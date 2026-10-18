# banco

## Visão geral

O agente montou o esquema à mão, com SQL versionado pelo Flyway (`V1__schema.sql`, `V2__catalog_seed.sql`). A foto do banco corresponde exatamente às migrações. O esquema tem pontos fortes claros:
- toda tabela tem chave primária;
- o dinheiro está em `numeric(12,2)` e o instante do pedido em `timestamp with time zone`;
- as colunas obrigatórias são `NOT NULL`;
- todas as chaves estrangeiras declaradas têm índice;
- duas das três regras invariáveis estão no próprio banco: o `CHECK (stock >= 0)` e a chave primária `(order_id, transaction_id)` nos avisos de pagamento.

Os problemas, do mais grave para o menos grave:
1. Faltam duas chaves estrangeiras: o item do pedido não aponta para o produto, e o aviso ao cliente não aponta para o pedido.
2. A quantidade do item não tem `CHECK`.
3. Sete colunas de texto usam `varchar(255)`, entre elas o CPF, que o enunciado diz ter 11 dígitos.
4. O resumo do pedido fica num único `text` com JSON. Isso é uma foto do momento pedida pelo negócio, mas o banco não sabe o tipo nem a obrigatoriedade de nenhum dos onze valores.

## Item a item

**B1, chave primária.** As seis tabelas do domínio têm chave primária (`database/database-schema.json`, restrições `p`):
- `products_pkey (sku)`;
- `customers_pkey (cpf)`;
- `orders_pkey (id)`;
- `order_items_pkey (order_id, sku)`, composta;
- `payment_notices_pkey (order_id, transaction_id)`, composta;
- `notifications_pkey (seq)`.

Não há tabela sem chave.
Conclusão: segue

**B2, as ligações.** Ligações que o esquema guarda em coluna, conferidas uma a uma contra a foto:
- pedido → cliente: `orders.cpf`, com `orders_cpf_fkey` (`V1__schema.sql:17`). Tem.
- item → pedido: `order_items.order_id`, com `order_items_order_id_fkey` (`V1__schema.sql:27`). Tem.
- **item → produto: `order_items.sku` não tem `FOREIGN KEY`** (`V1__schema.sql:28`; a foto só tem a chave estrangeira para `orders`). Um item pode citar um SKU que não existe no catálogo.
- aviso de pagamento → pedido: `payment_notices.order_id`, com `payment_notices_order_id_fkey` (`V1__schema.sql:35`). Tem.
- aviso ao cliente → cliente: `notifications.cpf`, com `notifications_cpf_fkey` (`V1__schema.sql:44`). Tem.
- **aviso ao cliente → pedido: `notifications.order_id` não tem `FOREIGN KEY`** (`V1__schema.sql:46`; não há restrição `f` sobre essa coluna na foto).

Das seis ligações, quatro têm chave estrangeira e duas não têm.
Conclusão: em parte

**B3, índice nas chaves estrangeiras.** As quatro chaves estrangeiras que existem têm um índice que começa pela coluna delas:
- `orders.cpf`: `idx_orders_cpf (cpf, status)` (`V1__schema.sql:24`);
- `order_items.order_id`: a chave primária `(order_id, sku)`;
- `payment_notices.order_id`: a chave primária `(order_id, transaction_id)`;
- `notifications.cpf`: `idx_notifications_cpf (cpf, seq)` (`V1__schema.sql:49`).

Fora do item, só uma observação: as duas ligações sem chave estrangeira do B2 (`order_items.sku` e `notifications.order_id`) também não têm índice.
Conclusão: segue

**B4, dinheiro em tipo exato.** As colunas de dinheiro que existem são `products.price` e `customers.credit_balance`, ambas `numeric(12,2)` (foto, `columns`). Não há `real`, `double precision` nem `money`.

Os valores do resumo (subtotal, desconto, frete, total, parcela, crédito) não têm coluna própria. Ficam em `orders.summary_json`, do tipo `text`, gravados a partir de um `record` de `BigDecimal` (`main/java/br/com/loja/pedidos/checkout/Summary.java:5-16`, `OrderRepository.java:37`). O texto decimal preserva os dígitos e é lido de volta como `BigDecimal` (`OrderRepository.java:96`), então não há perda por ponto flutuante. Só que, para o banco, esses valores não são números. Trato isso no B8; aqui não pesa, porque nenhuma coluna usa tipo inexato.
Conclusão: segue

**B5, datas com fuso.** O único instante do domínio guardado em coluna é `orders.created_at`, do tipo `timestamp with time zone` (foto; `V1__schema.sql:20`), gravado como `OffsetDateTime` em UTC (`OrderRepository.java:35`). A coluna `installed_on`, do tipo `timestamp without time zone`, é da `flyway_schema_history` e fica fora do item.

O `paidAt` do aviso de pagamento não é guardado em coluna nenhuma (`payment_notices` só tem `order_id`, `transaction_id` e `error_code`). Por isso não há coluna sem fuso.
Conclusão: segue

**B6, NOT NULL no obrigatório.** As colunas obrigatórias estão todas com `nullable: false` na foto:
- o SKU, o nome, o preço, o peso, o estoque e o `discontinued` do produto;
- o CPF e o saldo do cliente;
- o CPF, o e-mail, a situação, o `created_at` e o resumo do pedido;
- o pedido, o SKU e a quantidade do item;
- o pedido e o `transaction_id` do aviso de pagamento;
- o id, o CPF, o tipo, o pedido e a situação do aviso ao cliente.

Só duas colunas aceitam nulo, e as duas com motivo:
- `orders.tracking_code`, que só existe depois do envio;
- `payment_notices.error_code`, cujo nulo significa "aviso aceito" (`OrderRepository.java:82`).

Ressalva: o `NOT NULL` de `summary_json` só garante que o texto existe. O banco não garante que cada um dos onze valores do resumo esteja presente.
Conclusão: segue

**B7, regras no próprio banco.**
- *Estoque nunca negativo*: o banco garante, com `products_stock_check CHECK ((stock >= 0))` (foto; `V1__schema.sql:6`). A baixa é um `stock = stock + ?` (`ProductRepository.java:34`), e o `CHECK` barra qualquer resultado negativo, mesmo que a verificação do código falhe.
- *Aviso de pagamento repetido não baixa o estoque nem dá o crédito duas vezes*: o banco garante, com a chave primária `payment_notices_pkey (order_id, transaction_id)`. O registro do aviso é gravado na mesma transação que baixa o estoque e dá o crédito (`OrderService.java:84-91`, com `@Transactional` na classe em `OrderService.java:26`). Um segundo `INSERT` do mesmo par viola a chave e desfaz a transação inteira, porque `DuplicateKeyException` não está no `noRollbackFor`.
- *Quantidade do item positiva*: **o banco não garante**. `order_items.quantity` é `INTEGER NOT NULL`, sem `CHECK (quantity > 0)` (`V1__schema.sql:29`; não há restrição `c` em `order_items` na foto). Só a validação do código cuida dessa regra.

Duas das três regras estão no banco.
Conclusão: em parte

**B8, dado repetido.**
- `orders.email` não repete nada: o cliente não tem coluna de e-mail (`customers` tem só `cpf` e `credit_balance`). É o dado do cliente naquela compra, uma foto do momento.
- `orders.summary_json` é o resumo que o enunciado manda congelar: também é foto do momento, e não uma lista nem uma ligação escondida. Os itens ficam numa tabela própria (`order_items`), e os avisos também (`notifications`, `payment_notices`).
- O item não guarda o preço da compra, mas isso não é repetição: o preço só aparece consolidado no resumo congelado.
- Há uma redundância pequena em `notifications.cpf`: ela é deduzível de `notifications.order_id → orders.cpf`. O enunciado trata o aviso como sendo "de um pedido e de um cliente", e o CPF é chave imutável, então o risco de divergir é baixo. Mesmo assim, como `order_id` não tem chave estrangeira (B2), nada garante que os dois concordem. Hoje o código grava os dois a partir do mesmo pedido (`main/java/br/com/loja/pedidos/notification/CustomerNotificationListener.java:20,25,30`).
- Uma observação fora da pergunta: guardar o resumo como `text` em vez de onze colunas tipadas (ou `jsonb`) tira do banco o tipo e o `NOT NULL` de cada valor.

Não encontrei dado que muda repetido sem motivo.
Conclusão: segue

**B9, criação do esquema.** Um mecanismo só: o Flyway.
- O `pom.xml` tem `spring-boot-starter-flyway` e `flyway-database-postgresql`.
- O `application.properties:5` tem `spring.flyway.enabled=true`.
- As migrações versionadas ficam em `db/migration`: `V1__schema.sql` cria o esquema e `V2__catalog_seed.sql` carrega o catálogo.
- Não há JPA nem Hibernate no projeto (o acesso é por `JdbcClient`), não há `ddl-auto` nem `schema.sql`; `grep` por `jpa`, `ddl-auto` e `sql.init` não encontra nada.
- A foto mostra a `flyway_schema_history`, prova de que foi o Flyway que criou o esquema.

Os testes de `mvn verify` rodam as mesmas migrações num H2 em modo PostgreSQL (`test/java/br/com/loja/pedidos/PedidosApiTest.java:61`), e não um segundo mecanismo.
Conclusão: segue

**B10, tamanho dos textos.** O SQL foi escrito à mão, então os tamanhos foram escolhidos pelo agente, não herdados do JPA. Há tamanhos com motivo:
- `varchar(36)` para os ids UUID;
- `varchar(8)` para `NEW`/`READ`;
- `varchar(32)` para as situações;
- `varchar(64)` para o SKU e o `error_code`.

Mas há `character varying(255)` arbitrário em sete colunas (foto): `customers.cpf`, `orders.cpf`, `notifications.cpf`, `orders.email`, `orders.tracking_code`, `payment_notices.transaction_id` e `products.name`. O caso mais claro é o CPF, que pelo enunciado tem 11 números e ganhou 255 nas três tabelas. Não há `char(n)` nem coluna `text`, fora o `summary_json`.
Conclusão: em parte

## O que mudaria primeiro

1. **As duas chaves estrangeiras que faltam, com índice:** `order_items.sku → products(sku)` e `notifications.order_id → orders(id)`. São ligações centrais do domínio. Hoje um item pode apontar para um produto inexistente, e um aviso para um pedido inexistente (ou de outro cliente que não o da coluna `cpf`), sem o banco reclamar.
2. **`CHECK (quantity > 0)` em `order_items`,** que fecha a terceira regra invariável no banco. Junto, trocaria os `varchar(255)` por `text`, ou por um tamanho do domínio, como `varchar(11)` (ou `char`-free com `CHECK`) para o CPF.

## Dúvidas

- **O resumo em JSON num `text`.** Pela régua, o B8 só conta como problema uma *lista* guardada em JSON. O resumo é um registro fixo de valores, que o enunciado manda congelar. Li isso como foto do momento e não como repetição, então não pesou no B8 nem no B4. Registrei a perda de tipo e de `NOT NULL` como ressalva nesses itens.
- **A redundância `notifications.cpf` × `notifications.order_id`.** O B2 diz que o aviso é "de um pedido e de um cliente", então tratei o CPF no aviso como ligação pedida pelo domínio, e não como repetição sem motivo.
