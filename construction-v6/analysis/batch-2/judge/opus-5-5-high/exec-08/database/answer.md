# o banco

## Visão geral

O serviço cria o esquema com um script próprio: uma lista de `CREATE TABLE IF NOT EXISTS` dentro de `JdbcStore`, rodada ao subir com uma trava consultiva (`pg_advisory_lock`) para que as duas máquinas não criem o esquema ao mesmo tempo. Os tipos foram bem escolhidos: o dinheiro é `numeric(14,2)`, os instantes são `timestamptz` e os textos são `text`, sem `varchar(255)` por padrão. Toda tabela tem chave primária. O estoque tem `CHECK (stock >= 0)`, e o aviso de pagamento tem chave primária `(order_id, transaction_id)`. O problema mais sério são as ligações: das seis que o esquema guarda em coluna, só uma é chave estrangeira (`order_items.order_id`). O cliente do pedido, o produto do item, o pedido do aviso de pagamento e o pedido e o cliente do aviso ao cliente não têm `FOREIGN KEY`. Há mais três problemas. O resumo do pedido fica num único `text` com JSON, então o banco não garante nem tipa os valores dele. A quantidade do item não tem `CHECK`. E não há ferramenta de migração versionada.

## Item a item

**B1, chave primária.** As seis tabelas têm chave primária (`database/database-schema.json`, as restrições `p`): `customers_pkey (cpf)`, `notifications_pkey (id)`, `order_items_pkey (order_id, "position")`, `orders_pkey (id)`, `payment_events_pkey (order_id, transaction_id)` e `products_pkey (sku)`. A chave do item é composta pelo pedido e pela posição (`src/main/java/shop/JdbcStore.java:64`). Detalhe menor: o id do pedido é um UUID guardado em `text` (`JdbcStore.java:47`, gerado em `OrderService.java:34`), quando o PostgreSQL tem o tipo `uuid`. Segue a prática.
Conclusão: segue

**B2, as ligações.** O esquema guarda seis ligações do domínio em colunas, e só uma tem `FOREIGN KEY`:
- item → pedido: `order_items.order_id` tem `order_items_order_id_fkey` (`JdbcStore.java:60`). Tem a chave.
- pedido → cliente: `orders.cpf` (`JdbcStore.java:48`) não tem chave estrangeira para `customers(cpf)`.
- item → produto: `order_items.sku` (`JdbcStore.java:62`) não tem chave estrangeira para `products(sku)`.
- aviso de pagamento → pedido: `payment_events.order_id` (`JdbcStore.java:68`) não tem chave estrangeira para `orders(id)`.
- aviso ao cliente → pedido: `notifications.order_id` (`JdbcStore.java:79`) não tem chave estrangeira.
- aviso ao cliente → cliente: `notifications.cpf` (`JdbcStore.java:77`) não tem chave estrangeira.

Nada no código impediria essas chaves. O cliente é inserido antes do pedido na mesma transação (`OrderService.java:37-38`), e o catálogo é carregado ao subir, antes de qualquer item (`JdbcStore.java:108-120`). Uma ligação em seis é a exceção, não a prática.
Conclusão: não segue

**B3, índice nas chaves estrangeiras.** A única chave estrangeira, `order_items.order_id`, é a primeira coluna da chave primária `order_items_pkey (order_id, "position")`, que cria um índice único. Então ela tem índice. As ligações que deveriam ser chaves estrangeiras ficam assim:
- Têm índice que começa por elas: `orders.cpf` (`orders_cpf_status_idx (cpf, status)`), `notifications.cpf` (`notifications_cpf_idx (cpf, id)`) e `payment_events.order_id` (a primeira coluna da chave primária).
- Ficariam sem índice se virassem chave estrangeira: `order_items.sku` e `notifications.order_id`.

Pelo que o item pergunta, que são as chaves estrangeiras que existem, segue.
Conclusão: segue

**B4, dinheiro em tipo exato.** O preço (`products.price`) e o saldo (`customers.credit_balance`) são `numeric(14,2)` (`database/database-schema.json`, `columns`; `JdbcStore.java:34,43`). O peso, que não é dinheiro, é `numeric(10,3)`. Nenhuma coluna é `real`, `double precision` ou `money`. Os valores do resumo (subtotal, desconto, frete, seguro, total, parcela, crédito) não têm coluna própria: ficam no JSON da coluna `orders.summary` (`text`, `JdbcStore.java:52`), gravados com `WRITE_BIGDECIMAL_AS_PLAIN` (`Json.java:10`) e lidos de volta como `BigDecimal` (`Models.java:24-26`). A representação é exata, porque não passa por ponto flutuante. Mas o banco não vê esses valores como números: não dá para somá-los nem conferi-los em SQL.
Conclusão: segue

**B5, datas e horas com fuso.** As duas colunas de instante, `orders.created_at` e `orders.paid_at`, são `timestamp with time zone` (`database/database-schema.json`; `JdbcStore.java:51,54`). O código grava em UTC por `OffsetDateTime` (`JdbcStore.java:157-159`). Não há coluna `timestamp without time zone`.
Conclusão: segue

**B6, obrigatórios `NOT NULL`.** Todas as colunas obrigatórias aceitam só valores não nulos:
- produtos: `sku`, `name`, `price`, `weight_kg`, `stock` e `discontinued`;
- clientes: `cpf`, `email` e `credit_balance`;
- pedidos: `cpf`, `email`, `status`, `created_at` e `summary`;
- itens: `order_id`, `position`, `sku` e `quantity`;
- avisos ao cliente: `cpf`, `type`, `order_id` e `status`;
- avisos de pagamento: `http_status`.

O que só existe depois é nulo, como deve: `tracking_code`, `paid_at` e `transaction_id` em `orders`, e `error_code` em `payment_events` (nulo quer dizer sucesso, `Models.java:21`). A falha está nos valores do resumo, que a régua cita como obrigatórios. Eles não são colunas: o `NOT NULL` vale para o texto JSON inteiro (`JdbcStore.java:52`), e um `'{}'` ou um `finalTotal` nulo dentro dele passaria pelo banco. Fora o resumo, tudo está certo.
Conclusão: em parte

**B7, regras invariáveis no banco.**
- **Estoque nunca negativo.** O banco garante, com `products_stock_check`, `CHECK ((stock >= 0))` (`JdbcStore.java:36`). O código também confere antes de baixar, com trava de linha (`OrderEffects.java:24-31`).
- **Aviso de pagamento repetido não baixa o estoque nem dá o crédito duas vezes.** Há a chave primária `payment_events_pkey (order_id, transaction_id)` (`JdbcStore.java:72`), que é o que a régua pede. Uma ressalva: a gravação usa `ON CONFLICT (order_id, transaction_id) DO NOTHING` (`JdbcStore.java:370`). Assim, uma repetição que escapasse da conferência não abortaria a transação. A proteção efetiva é a trava do pedido (`OrderService.java:52`), a busca do registro anterior (`OrderService.java:57`) e a conferência da situação (`OrderService.java:63`). A chave impede o registro duplicado, mas não desfaz uma baixa feita em dobro.
- **Quantidade do item positiva.** Só o código cuida disso (`PricingService.java:81`): `order_items.quantity` é `integer NOT NULL`, sem `CHECK (quantity > 0)` (`JdbcStore.java:63`; nenhuma restrição `c` em `order_items` na foto).

O banco garante duas das três regras.
Conclusão: em parte

**B8, dado repetido sem motivo.**
- O `orders.email` repete o e-mail do cliente, mas é uma foto da compra: `customers.email` nunca é atualizado (`ON CONFLICT DO NOTHING`, `JdbcStore.java:319`), e o pedido guarda o e-mail informado naquela compra.
- O `notifications.cpf` dá para tirar do pedido (`orders.cpf`), mas o CPF de um pedido não muda, então as duas versões não divergem. Além disso, o aviso é de um cliente pelo enunciado.
- O `orders.transaction_id` e o `payment_events.transaction_id` não são o mesmo dado. Um é o pagamento aceito, o outro é o registro de cada aviso recebido, inclusive os recusados.
- Os itens estão em tabela própria, não em JSON (`JdbcStore.java:59-65`).
- O resumo em JSON numa só coluna (`orders.summary`) é a foto pedida pelo negócio, e não é uma lista nem uma ligação escondida. Ele fere a atomicidade da primeira forma normal (o efeito aparece no B4 e no B6), mas não é dado repetido entre tabelas.
Conclusão: segue

**B9, como o esquema é criado.** É um script próprio: a lista `SCHEMA` de `CREATE TABLE/INDEX IF NOT EXISTS` (`JdbcStore.java:29-82`), executada por `initSchema()` ao subir (`JdbcStore.java:98-129`), com `pg_advisory_lock` para as duas máquinas. Há um mecanismo só: não há `schema.sql`, pasta `db/migration`, Flyway, Liquibase nem Hibernate no `pom.xml`. Mas não é uma ferramenta de migração versionada. Não existe histórico de versões, e o `IF NOT EXISTS` não muda uma tabela que já existe: uma coluna nova incluída nessa lista nunca chegaria a um banco já criado. Isso pesa num enunciado que avisa de mudanças frequentes, como a situação "em separação".
Conclusão: em parte

**B10, tamanho dos textos.** Todas as colunas de texto são `text` (`database/database-schema.json`): `sku`, `name`, `cpf`, `email`, `status`, `type`, `order_id`, `tracking_code`, `transaction_id` e `error_code`. Não há `character varying(255)` nem `char(n)`.
Conclusão: segue

## O que mudaria primeiro

1. **Criar as cinco chaves estrangeiras que faltam**: `orders.cpf` e `notifications.cpf` para `customers`, `order_items.sku` para `products`, e `payment_events.order_id` e `notifications.order_id` para `orders`. Junto, criar índices em `order_items.sku` e `notifications.order_id`. Hoje o banco aceita itens de produtos que não existem e avisos de pedidos que não existem, e só o código impede isso.
2. **Passar o esquema para o Flyway** e, na mesma leva, guardar o resumo em colunas `numeric(14,2) NOT NULL` no próprio `orders`, além de acrescentar `CHECK (quantity > 0)` em `order_items`. Assim o esquema pode evoluir com versões, e os valores do resumo passam a ter tipo e obrigatoriedade garantidos pelo banco.

## Dúvidas

- **O resumo em JSON.** A régua só conta como problema no B8 as listas guardadas em JSON. Li o resumo, que é um conjunto de valores e não uma lista, como foto do momento: não é repetição. Registrei o custo dele onde a régua o alcança: no B4, como observação (a representação é exata, então segue), e no B6, onde a régua cita "os valores do resumo" como obrigatórios e o banco não garante cada um (em parte).
- **A idempotência no B7.** Contei a chave primária `(order_id, transaction_id)` como a garantia que a régua pede, apesar do `ON CONFLICT DO NOTHING`, que tira dela o poder de abortar uma repetição. O ponto está registrado no texto do item.
- **O B3.** Respondi pela única chave estrangeira que existe. As ligações sem chave já contam no B2, e não contei de novo a falta de índice nas que viriam a existir.
