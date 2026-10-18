# banco

## Visão geral

O agente montou o esquema com uma única migração do Flyway escrita à mão (`V1__init.sql`), sem JPA, e acesso por `JdbcClient`. A escolha dos tipos e das garantias de coluna está bem feita. Todo dinheiro está em `numeric`, o único instante do domínio está em `timestamp with time zone`, e todas as colunas obrigatórias são `NOT NULL`. O banco também protege duas das três regras invariáveis: o estoque nunca negativo tem um `CHECK`, e o aviso de pagamento repetido esbarra na chave primária `(order_id, transaction_id)`.

O problema mais sério é que **não existe nenhuma chave estrangeira**. As seis ligações do domínio estão guardadas em colunas, mas nenhuma é declarada como chave estrangeira, então o banco aceita itens, avisos e eventos de pagamento que apontam para pedidos ou produtos inexistentes. Os problemas menores são estes: a quantidade positiva do item só é conferida no código, e os textos misturam tamanhos com motivo (36 para UUID) com tamanhos arbitrários (255 no CPF, que tem 11 dígitos).

## Item a item

**B1, chave primária.** As seis tabelas do domínio têm chave primária (`database/database-schema.json`, restrições do tipo `p`):
- `products_pkey (sku)`
- `customers_pkey (cpf)`
- `orders_pkey (id)`
- `order_items_pkey (order_id, "position")`, composta
- `payment_events_pkey (order_id, transaction_id)`, composta
- `notifications_pkey (seq)`, uma identidade, com `notifications_id_key UNIQUE (id)` para o id público

Isso está em `src/main/resources/db/migration/V1__init.sql:2,21,27,52,59,63-64`. Segue a prática.
Conclusão: segue

**B2, as ligações.** Na foto, a lista `constraints` não tem nenhuma restrição do tipo `f`. As seis ligações do enunciado estão em colunas, e nenhuma tem chave estrangeira:
- o pedido é de um cliente: `orders.cpf` → `customers.cpf` (`V1__init.sql:28`);
- o item é de um pedido: `order_items.order_id` → `orders.id` (`V1__init.sql:48`);
- o item é de um produto: `order_items.sku` → `products.sku` (`V1__init.sql:50`);
- o aviso de pagamento é de um pedido: `payment_events.order_id` → `orders.id` (`V1__init.sql:56`);
- o aviso ao cliente é de um pedido: `notifications.order_id` → `orders.id` (`V1__init.sql:67`);
- o aviso ao cliente é de um cliente: `notifications.cpf` → `customers.cpf` (`V1__init.sql:65`).

A integridade dessas ligações depende só da ordem das chamadas no código. Por exemplo, `customers.createIfAbsent` roda fora da transação que insere o pedido (`src/main/java/br/com/loja/pedidos/order/OrderService.java:58-61`). O banco não impede um item com SKU inexistente nem um aviso de um pedido que não existe.
Conclusão: não segue

**B3, índice nas chaves estrangeiras.** Não há chave estrangeira (ver B2), então a pergunta não tem objeto. Só como informação, olhando as colunas de ligação:
- já estão cobertas como primeira coluna de algum índice: `order_items.order_id` (`order_items_pkey`), `payment_events.order_id` (`payment_events_pkey`), `orders.cpf` (`idx_orders_cpf_status`) e `notifications.cpf` (`idx_notifications_cpf`);
- ficariam sem índice se viessem as chaves: `order_items.sku` e `notifications.order_id`.
Conclusão: não se aplica

**B4, dinheiro em tipo exato.** Todas as colunas de dinheiro são `numeric` (`database/database-schema.json`, `columns`):
- `products.price numeric(12,2)`;
- `customers.credit_balance numeric(14,2)`;
- em `orders`, também `numeric(14,2)`: `products_subtotal`, `coupon_discount`, `shipping_cost`, `insurance`, `payment_adjustment`, `final_total`, `installment_amount` e `next_purchase_credit`.

Nenhuma coluna é `real`, `double precision` ou `money`. O peso também é exato (`products.weight_kg numeric(10,3)`).
Conclusão: segue

**B5, datas com fuso.** O único instante do domínio guardado é `orders.created_at`, que é `timestamp with time zone` e `NOT NULL` (`V1__init.sql:31`; na foto, `orders.created_at`). O código grava esse instante em UTC (`OrderRepository.java:36`). O `paidAt` não é guardado, porque `payment_events` não tem coluna de data. A única coluna `timestamp without time zone` da foto é `flyway_schema_history.installed_on`, da ferramenta de migração, e fica fora do item.
Conclusão: segue

**B6, obrigatórios `NOT NULL`.** Na foto, todas as colunas das tabelas do domínio têm `"nullable": false`, inclusive:
- o CPF e o e-mail (em `customers`, `orders` e `notifications`);
- `orders.status` e `orders.created_at`;
- todos os valores do resumo;
- `products.sku`, `stock` e `discontinued`;
- `order_items.quantity`;
- `customers.credit_balance`, que também tem `DEFAULT 0`;
- `notifications.type` e `notifications.status`.

A única coluna que aceita nulo é `orders.tracking_code` (`V1__init.sql:32`), que pelo enunciado só existe depois do envio. Segue a prática.
Conclusão: segue

**B7, regras invariáveis no banco.**
- **Estoque nunca negativo:** o banco garante, com `products_stock_check CHECK ((stock >= 0))` (`V1__init.sql:6`; na foto, a restrição `c`). A trava do código (`ProductRepository.java:43`, `AND stock >= :q`) fica como uma segunda proteção.
- **O mesmo aviso de pagamento não pode baixar o estoque nem dar o crédito duas vezes:** o banco garante com `payment_events_pkey PRIMARY KEY (order_id, transaction_id)` (`V1__init.sql:59`). O registro do evento (`OrderService.java:137`) roda na mesma transação que a baixa do estoque e o crédito (`OrderService.java:70,135-139`). Assim, um segundo aviso igual que escapasse da trava do código falharia na chave e desfaria a transação inteira.
- **Quantidade do item positiva:** só o código cuida, em `CheckoutService.java:106` (`item.quantity() <= 0`). A coluna `order_items.quantity` não tem `CHECK (quantity > 0)` (`V1__init.sql:51`).

O banco protege duas das três regras.
Conclusão: em parte

**B8, dado repetido sem motivo.**
- Os itens estão numa tabela própria (`order_items`), não em JSON, e os avisos também têm tabela própria.
- Os valores do resumo no pedido (`V1__init.sql:33-43`) e o `orders.email` são a foto do momento que o enunciado pede.
- O `customers.email` é gravado uma vez no primeiro pedido (`CustomerRepository.java:23-26`) e nunca é lido (`CustomerRepository.java:33` só lê `cpf` e `credit_balance`). Por isso não chega a criar duas versões em uso.
- O `notifications.cpf` daria para tirar de `notifications.order_id` → `orders.cpf`. Mas o CPF é a chave do cliente e não muda, e a régua trata o aviso como sendo de um pedido e de um cliente.

Nenhuma dessas colunas cria duas versões de um dado que muda.
Conclusão: segue

**B9, como o esquema é criado.** Só o Flyway cria o esquema:
- `spring-boot-starter-flyway` e `flyway-database-postgresql` no `pom.xml:29-36`;
- a migração versionada `src/main/resources/db/migration/V1__init.sql`, que também carrega o catálogo inicial;
- a foto tem a tabela `flyway_schema_history`.

Não há JPA nem Hibernate (o projeto usa `spring-boot-starter-jdbc`, `pom.xml:25-28`), não há `ddl-auto` nem `schema.sql`, e o `application.properties` não tem nenhuma configuração `spring.sql.init`. É um mecanismo só, o recomendado.
Conclusão: segue

**B10, tamanho dos textos.** Não há nenhuma coluna `text` nem `char(n)`. Os tamanhos se dividem assim:
- **com motivo:** os ids UUID em `varchar(36)` (`orders.id`, `order_items.order_id`, `payment_events.order_id`, `notifications.id` e `notifications.order_id`). O tipo `uuid` seria mais preciso, mas o tamanho tem razão.
- **arbitrários, 255:** `customers.cpf`, `orders.cpf` e `notifications.cpf` (o CPF tem 11 dígitos), além de `customers.email`, `orders.email`, `products.name`, `orders.tracking_code` e `payment_events.transaction_id`.
- **sem motivo visível no domínio:** `products.sku` e `order_items.sku` em `varchar(64)`, `orders.status` em `varchar(32)`, `notifications.type` em `varchar(32)`, `payment_events.outcome` em `varchar(64)` e `notifications.status` em `varchar(8)`.

Não é o padrão de 255 em tudo, mas a maior parte dos tamanhos é arbitrária.
Conclusão: em parte

## O que mudaria primeiro

1. **Declarar as seis chaves estrangeiras:**
   - `orders.cpf` → `customers`;
   - `order_items.order_id` → `orders` e `order_items.sku` → `products`;
   - `payment_events.order_id` → `orders`;
   - `notifications.order_id` → `orders` e `notifications.cpf` → `customers`.

   Junto, criar índices em `order_items.sku` e `notifications.order_id`. Hoje toda a integridade das ligações depende só do código. Para isso, o `createIfAbsent` do cliente precisa continuar rodando antes da inserção do pedido, como já roda.
2. **Acrescentar `CHECK (quantity > 0)` em `order_items`**, para que a terceira regra invariável também fique no banco. De quebra, trocar os `varchar(255)` arbitrários por `text`, ou por `varchar(11)` no CPF.

## Dúvidas

- **B3:** sem nenhuma chave estrangeira, li o item como "não se aplica" e deixei a falta para o B2. Comentei a cobertura das colunas de ligação só como informação.
- **B8:** tratei `customers.email` como dado do cliente, não como repetição de `orders.email`, porque nunca é lido. Tratei `notifications.cpf` como aceitável, porque o CPF é imutável e a régua diz que o aviso é de um pedido e de um cliente.
- **Caminho da foto:** a régua cita `suite/database-schema.json`, mas na pasta a foto está em `database/database-schema.json`. Usei esse arquivo, que tem as tabelas, então o serviço subiu.
