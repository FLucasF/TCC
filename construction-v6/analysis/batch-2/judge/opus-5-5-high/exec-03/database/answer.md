# o banco

## Visão geral

O serviço cria o esquema com um script próprio em Java: uma lista de `CREATE TABLE IF NOT EXISTS` que roda ao subir, protegida por um `pg_advisory_lock` (`src/main/java/shop/persistence/postgres/PostgresStore.java:15-75`). São cinco tabelas: `products`, `customers`, `orders`, `payments` e `notifications`.

Os pontos fortes:
- Todas as tabelas têm chave primária.
- Os valores em dinheiro são `numeric(12,2)` e o instante do pedido é `timestamptz`.
- Todas as colunas obrigatórias são `NOT NULL`.
- Os textos são `text`.
- Duas das três regras invariáveis estão garantidas no próprio banco: o `CHECK (stock >= 0)`, e a chave primária `(order_id, transaction_id)` em `payments` contra o aviso de pagamento repetido.

Os problemas, do mais grave ao menos grave:
1. Os itens do pedido não são uma tabela. A lista inteira fica num texto JSON dentro de `orders` (`items_json`). Isso esconde do banco as ligações item→pedido e item→produto, e também a regra da quantidade positiva. O resumo também é JSON (`summary_json`).
2. Das quatro ligações que o esquema guarda em coluna, só uma tem chave estrangeira (`orders.cpf`). Ficaram sem chave `payments.order_id`, `notifications.order_id` e `notifications.cpf`.
3. O esquema não é versionado: o `IF NOT EXISTS` cria, mas não sabe evoluir uma tabela que já existe.

## Item a item

**B1, chave primária.** As cinco tabelas têm chave primária (`database/database-schema.json`, restrições `p`):
- `customers_pkey (cpf)`
- `notifications_pkey (seq)`
- `orders_pkey (id)`
- `payments_pkey (order_id, transaction_id)`
- `products_pkey (sku)`

Em `notifications`, a chave é um `bigserial` e o id público tem `UNIQUE (id)` (`notifications_id_key`; `PostgresStore.java:46-47`). O `seq` serve para ordenar os avisos (`PgNotifications.java:205` no arquivo, a consulta `ORDER BY seq`). Nenhuma tabela fica sem chave.
Conclusão: segue

**B2, as ligações.** Das ligações do enunciado, o esquema guarda quatro em coluna:
- pedido→cliente: `orders.cpf` tem `orders_cpf_fkey`, `FOREIGN KEY (cpf) REFERENCES customers(cpf)` (`PostgresStore.java:31`). Tem a chave.
- aviso de pagamento→pedido: `payments.order_id` não tem `FOREIGN KEY` (`PostgresStore.java:39-43`; no JSON, `payments` só tem a restrição `p`).
- aviso ao cliente→pedido: `notifications.order_id` não tem `FOREIGN KEY` (`PostgresStore.java:50`).
- aviso ao cliente→cliente: `notifications.cpf` não tem `FOREIGN KEY` (`PostgresStore.java:48`).

As ligações item→pedido e item→produto não ficam em coluna nenhuma, porque os itens estão em `orders.items_json`. Isso não é chave faltando: é o B8.

Resultado: uma ligação com chave e três sem. Nada no banco impede um aviso ou um pagamento que aponte para um pedido que não existe.
Conclusão: não segue

**B3, índice nas chaves estrangeiras.** A única chave estrangeira, `orders.cpf`, tem índice próprio: `orders_cpf_idx ON orders(cpf)` (`PostgresStore.java:37`; índice `orders_cpf_idx` no JSON). Das colunas que deveriam ter chave estrangeira:
- `notifications.cpf` já tem `notifications_cpf_idx`.
- `payments.order_id` já é a primeira coluna da chave primária.
- `notifications.order_id` não tem índice.

Para a chave que existe, a prática está cumprida.
Conclusão: segue

**B4, dinheiro em tipo exato.**
- `products.price` e `customers.credit_balance` são `numeric(12,2)` (JSON, `columns`).
- Os valores do resumo vão em `orders.summary_json`. Eles saem de campos `BigDecimal` (`domain/Summary.java:5-8`) serializados em texto, o que guarda o valor decimal exato. Não há `real`, `double precision` nem `money`.
- `products.weight_kg` (`numeric(10,3)`) é peso, não dinheiro.
Conclusão: segue

**B5, datas com fuso.** O único instante guardado é `orders.created_at`, que é `timestamp with time zone` (JSON; `PostgresStore.java:33`). O código grava esse valor em UTC (`PgOrders.java:36`). O `paidAt` do aviso de pagamento não é guardado (`payments` não tem coluna de data), então não há outro instante a avaliar.
Conclusão: segue

**B6, obrigatórios `NOT NULL`.** Todas as colunas obrigatórias são `nullable: false`: `cpf`, `status`, `created_at`, `sku`, `name`, `price`, `stock`, `credit_balance`, e os `type`/`status`/`order_id` dos avisos. Só aceitam nulo:
- `orders.tracking_code`, que só existe depois do envio;
- `payments.error_code`, onde nulo quer dizer "pagamento aceito" (`OrderService.java:42,56`).

As duas exceções são legítimas. Há uma ressalva: os valores do resumo e a quantidade e o SKU de cada item ficam dentro de JSON. O banco só garante que o texto inteiro não é nulo, e não cada valor. A causa disso é tratada no B8. O e-mail não é guardado em lugar nenhum (só é conferido em `service/CheckoutCalculator.java:85`), então não entra aqui.
Conclusão: segue

**B7, regras invariáveis no banco.**
- O estoque nunca negativo é garantido pelo banco: `products_stock_check`, `CHECK ((stock >= 0))` (`PostgresStore.java:22`).
- O aviso de pagamento repetido também é garantido pelo banco: `payments_pkey PRIMARY KEY (order_id, transaction_id)`. O registro do pagamento é gravado na mesma transação que baixa o estoque e dá o crédito (`OrderService.java:50-56`, e `:67` na recusa). Com isso, uma segunda gravação do mesmo aviso violaria a chave e desfaria a transação inteira.
- A quantidade positiva só é garantida pelo código (`CheckoutCalculator`). A quantidade fica dentro de `orders.items_json`, onde nenhum `CHECK` alcança.
Conclusão: em parte

**B8, dado repetido entre tabelas.**
- `orders.items_json` (`PostgresStore.java:35`, tipo `text`) guarda a lista inteira de itens como JSON (`PgOrders.java:38`). Isso fere a primeira forma normal: um valor não atômico com uma relação 1:N dentro de uma coluna. O banco não enxerga nem o pedido nem o produto de cada item, e não pode ligar o SKU a `products`.
- `orders.summary_json` é a foto do resumo pedida pelo enunciado, então não é repetição sem motivo. Mas também fica num texto opaco, em vez de colunas.
- Nenhum dado que muda (o preço atual, o e-mail) aparece repetido em outra tabela.
- `notifications.cpf` repete o `cpf` do pedido. Esse valor não muda, e o enunciado diz que o aviso é do pedido e do cliente, então não há divergência possível.

A lista em JSON, que é a entidade filha central do pedido, decide o item.
Conclusão: não segue

**B9, como o esquema é criado.** O `pom.xml` não tem Flyway, Liquibase, Hibernate ou JPA. Não há `schema.sql` nem pasta `db/migration`. O esquema sai de um script próprio, a lista `SCHEMA` (`PostgresStore.java:15-52`), executado por `migrate()` ao subir (`App.java:26`), com um advisory lock para as duas máquinas não correrem juntas (`PostgresStore.java:64-70`).

Isso dá um mecanismo só, e não dois em conflito. Mas não é uma ferramenta de migração versionada: não há histórico de versões, e `CREATE TABLE IF NOT EXISTS` não aplica mudanças numa tabela que já existe. A documentação recomenda o Flyway ou o Liquibase como mecanismo de mais alto nível.
Conclusão: em parte

**B10, tamanho dos textos.** Todas as colunas de texto são `text` (JSON: `cpf`, `id`, `status`, `name`, `sku`, `transaction_id`, etc.). Não há `character varying(255)` nem `char(n)`.
Conclusão: segue

## O que mudaria primeiro

1. **Criar a tabela `order_items`.** Ela teria `order_id` com FK para `orders`, `sku` com FK para `products`, a `quantity` com `CHECK (quantity > 0)`, e a chave `(order_id, sku)`. Isso substitui o `items_json`, devolve ao banco as duas ligações do item e leva para o banco a terceira regra invariável (B2, B7, B8). Dentro da mesma mudança, valeria trocar o `summary_json` por colunas `numeric` `NOT NULL` no próprio pedido.
2. **Pôr as chaves estrangeiras que faltam.** São elas: `payments.order_id → orders(id)`, `notifications.order_id → orders(id)` e `notifications.cpf → customers(cpf)`, mais um índice em `notifications.order_id`. Fazer isso por uma migração versionada (Flyway) resolveria também a evolução do esquema (B9).

## Dúvidas

- A régua aponta a foto em `suite/database-schema.json`, mas na pasta ela está em `database/database-schema.json`, como diz o guia. Usei essa.
- No B6, a falta de `NOT NULL` nos valores dentro do JSON tem a mesma causa do B8. Para não contar duas vezes, avaliei o B6 pelas colunas que existem e deixei o peso do JSON no B8.
- No B8, decidi `não segue`, e não `em parte`. O motivo é que a única falha (os itens em JSON) quebra a primeira forma normal justamente na entidade filha central do pedido. O resto do item segue a prática.
