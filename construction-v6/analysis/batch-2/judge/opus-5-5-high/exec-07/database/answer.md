# Banco

## Visão geral

O agente montou o esquema à mão, numa lista de comandos `CREATE TABLE IF NOT EXISTS` em `src/main/java/shop/persistence/Schema.java`. Esses comandos rodam ao subir o serviço, protegidos por um `pg_advisory_lock` para que as duas máquinas não criem o esquema ao mesmo tempo. Por isso a foto mostra seis tabelas, todas com nome do domínio.

**O que está bom**
- Todas as tabelas têm chave primária.
- O dinheiro do catálogo e do saldo é `numeric(12,2)`.
- O instante do pedido é `timestamp with time zone`.
- Todos os textos são `text`.
- As colunas obrigatórias são `NOT NULL`.
- Duas regras do negócio estão protegidas no próprio banco: o `CHECK (stock >= 0)` e a chave primária `(order_id, transaction_id)` do aviso de pagamento.

**Os problemas, do mais grave ao menos grave**
1. **Faltam chaves estrangeiras.** Das seis ligações do domínio, só duas têm chave estrangeira. O item não aponta para o produto, e o aviso de pagamento e o aviso ao cliente não apontam para nada.
2. **A quantidade do item não tem `CHECK` no banco.** Só o código garante que ela é positiva.
3. **O esquema é criado por um script próprio, sem versão.** Com `IF NOT EXISTS`, uma mudança futura numa tabela que já existe não é aplicada.
4. **O resumo do pedido é guardado como JSON numa coluna `text`.** Com isso, o banco não vê o tipo nem a obrigatoriedade de cada valor.

## Item a item

**B1, chave primária.** As seis tabelas têm chave primária (`database/database-schema.json`, restrições `p`):
- `customers_pkey (cpf)`
- `notifications_pkey (seq)`
- `order_items_pkey (order_id, sku)`
- `orders_pkey (id)`
- `payment_notices_pkey (order_id, transaction_id)`
- `products_pkey (sku)`

Elas vêm de `src/main/java/shop/persistence/Schema.java:20`, `:28`, `:32`, `:45`, `:48` e `:60`. Em `notifications`, a chave é um `bigserial` que dá a ordem dos avisos, e o `id` exposto pela API tem restrição única (`notifications_id_key`). É uma escolha coerente. Segue a prática.
Conclusão: segue

**B2, as ligações.** O esquema guarda em coluna as seis ligações do enunciado. Só duas delas têm chave estrangeira na foto (restrições `f`):

| ligação | coluna | chave estrangeira |
|---|---|---|
| pedido → cliente | `orders.customer_cpf` | **tem**: `orders_customer_cpf_fkey` (`Schema.java:33`) |
| item → pedido | `order_items.order_id` | **tem**: `order_items_order_id_fkey` (`Schema.java:42`) |
| item → produto | `order_items.sku` | **falta** (`Schema.java:43`) |
| aviso de pagamento → pedido | `payment_notices.order_id` | **falta** (`Schema.java:57`) |
| aviso ao cliente → pedido | `notifications.order_id` | **falta** (`Schema.java:52`) |
| aviso ao cliente → cliente | `notifications.customer_cpf` | **falta** (`Schema.java:50`) |

Com isso, o banco aceita um item com um SKU que não está no catálogo, e avisos que apontam para um pedido ou um cliente que não existe. As duas ligações centrais estão protegidas, mas faltam quatro de seis, então o item fica mais perto de `não segue` do que de `segue`.
Conclusão: em parte

**B3, índice nas chaves estrangeiras.** As duas chaves estrangeiras que existem têm índice começando pela coluna delas:
- `orders.customer_cpf` está coberta por `orders_customer_idx (customer_cpf, status)` (`Schema.java:39`).
- `order_items.order_id` está coberta pela chave primária `order_items_pkey (order_id, sku)`.

As colunas que deveriam ser chave estrangeira e não são também já têm índice começando por elas: `notifications_customer_idx (customer_cpf, seq)` e `payment_notices_pkey (order_id, …)`. Assim, acrescentar as chaves faltantes não exigiria índices novos nessas duas tabelas. A exceção são `order_items.sku` e `notifications.order_id`, que não têm índice. Nenhuma chave existente está sem índice.
Conclusão: segue

**B4, dinheiro em tipo exato.** As colunas de dinheiro são `products.price numeric(12,2)` e `customers.credit_balance numeric(12,2)` (`Schema.java:22`, `:29`). Não há `real`, `double precision` nem `money` na foto.

Os valores em dinheiro do pedido ficam dentro do JSON em `orders.summary text`:
- Eles são gravados a partir de `BigDecimal` já arredondados (`checkout/Summary.java:8-17`, `:21-28`).
- São serializados por `PostgresStore.java:206` e lidos de volta como `BigDecimal` em `PostgresStore.java:253`.

Isso guarda o valor sem perda, mas como texto: o banco não sabe que aquilo é dinheiro. Volto a esse ponto em Dúvidas.
Conclusão: segue

**B5, datas com fuso.** A única coluna de instante é `orders.created_at`, do tipo `timestamp with time zone` (foto; `Schema.java:36`). O código grava em UTC (`PostgresStore.java:205`). O `paidAt` do aviso de pagamento não é guardado, então não há outra coluna de data a avaliar. Segue a prática.
Conclusão: segue

**B6, NOT NULL no obrigatório.** Na foto, todas as colunas obrigatórias têm `nullable: false`:
- o CPF, em `customers.cpf` e `orders.customer_cpf`;
- o e-mail (`orders.email`), a situação (`orders.status`), a data de registro (`orders.created_at`) e o resumo (`orders.summary`);
- o SKU e a quantidade (`order_items.sku`, `order_items.quantity`);
- o saldo (`customers.credit_balance`, com padrão 0);
- o nome, o preço, o peso, o estoque e o "fora de linha" do produto;
- todas as colunas de `notifications`.

Só duas colunas aceitam nulo, e as duas fazem sentido:
- `orders.tracking_code`, que só existe depois do envio;
- `payment_notices.refusal`, em que nulo quer dizer que o pagamento foi aceito.

Ressalva: os valores do resumo estão dentro do JSON de `orders.summary`, então o banco não obriga cada um deles. Ele só obriga que a coluna inteira não seja nula.
Conclusão: segue

**B7, regras invariáveis no banco.**
- **"Estoque nunca negativo"**: o banco garante, com `products_stock_check CHECK ((stock >= 0))` (`Schema.java:24`). O código também confere antes de baixar (`OrderService.java:156`).
- **"O mesmo aviso de pagamento não baixa o estoque nem dá o crédito duas vezes"**: o banco garante, com a chave primária `payment_notices_pkey (order_id, transaction_id)` (`Schema.java:60`). O registro do aviso é inserido na mesma transação que baixa o estoque e dá o crédito (`OrderService.java:65-92`). Se dois avisos iguais passassem pelo código ao mesmo tempo, o segundo `INSERT` violaria a chave e a transação inteira seria desfeita. Além disso, o código tranca o pedido com `FOR UPDATE` (`OrderService.java:66`).
- **"A quantidade do item é positiva"**: só o código cuida dela (`checkout/CheckoutCalculator.java:103`). A coluna `order_items.quantity integer NOT NULL` não tem `CHECK (quantity > 0)` (`Schema.java:44`; não há restrição `c` em `order_items` na foto).

Duas regras de três estão no banco.
Conclusão: em parte

**B8, dado repetido.**
- **Os itens** ficam numa tabela própria (`order_items`), com ligação ao pedido, e não como lista dentro do pedido.
- **O e-mail** só existe em `orders.email`; não há cópia em `customers`. Ele é o dado do cliente naquela compra, ou seja, uma foto do momento.
- **O resumo** em `orders.summary` é a foto do momento que o enunciado pede ("não muda mais"). Ele é um conjunto de valores, não uma lista que esconda uma ligação. Guardá-lo como JSON tira do banco o tipo e a obrigatoriedade de cada valor (ver B4 e B6), mas não repete dado de outra tabela.
- **`notifications.customer_cpf`** dá para tirar de `orders.customer_cpf` por meio de `order_id`. Mas o enunciado diz que o aviso é de um pedido e de um cliente, e o CPF de um pedido não muda, então as duas versões não têm como divergir.

Não há repetição sem motivo.
Conclusão: segue

**B9, criação do esquema.** O mecanismo é um script próprio:
- O `pom.xml` não tem Flyway, Liquibase nem Hibernate.
- Não há `schema.sql` nem pasta `db/migration`.
- O `Main.java:17` chama `Schema.init`, que roda a lista de DDL de `Schema.java:17-60`. Esse script trava o banco com `pg_advisory_lock` (`Schema.java:68`) e também carrega o catálogo inicial (`Schema.java:78-90`).

O lado bom: há um mecanismo só, e ele foi pensado para as duas máquinas. O lado fraco: não é uma ferramenta de migração versionada, e não há histórico de versões. Como todo comando é `CREATE TABLE IF NOT EXISTS`, uma mudança futura numa tabela que já existe é ignorada sem aviso: uma coluna nova, um `CHECK` novo, a "situação em separação" que o enunciado anuncia. Ela teria de ser feita à mão.
Conclusão: em parte

**B10, tamanho dos textos.** Todas as colunas de texto são `text`: `sku`, `name`, `cpf`, `email`, `status`, `type`, `id`, `summary`, `tracking_code`, `transaction_id` e `refusal`. Não há `character varying(n)` nem `char(n)` na foto. Segue a prática.
Conclusão: segue

## O que mudaria primeiro

1. **Criar as quatro chaves estrangeiras que faltam e o `CHECK (quantity > 0)`.** As chaves são `order_items.sku → products(sku)`, `payment_notices.order_id → orders(id)`, `notifications.order_id → orders(id)` e `notifications.customer_cpf → customers(cpf)`. Hoje só o caminho do código impede um item com um produto que não existe, ou um aviso de um pedido que não existe. São mudanças pequenas, e a maioria já tem índice (B3).
2. **Passar a criação do esquema para o Flyway.** A primeira migração seria o DDL atual mais as restrições acima. Isso faz as próximas mudanças anunciadas no enunciado chegarem aos bancos que já existem, coisa que o `IF NOT EXISTS` não faz. Seria também a hora de pensar em guardar o resumo em colunas `numeric`, em vez de JSON em `text`.

## Dúvidas

- **B4, o resumo em `text`.** Os oito valores em dinheiro do resumo estão em JSON dentro de uma coluna `text`. A régua aponta como problema os tipos inexatos (`real`, `double precision`, `money`), e o texto gravado a partir de `BigDecimal` não perde precisão. Por isso concluí `segue`, mas com a ressalva de que o banco não reconhece esses valores como dinheiro. Quem ler o B4 como "todo dinheiro em `numeric`" chegaria a `em parte`.
- **B2, a conclusão.** Escolhi `em parte` porque as duas ligações centrais têm chave estrangeira (pedido → cliente e item → pedido). Mas faltam quatro de seis, e uma leitura mais rígida daria `não segue`.
