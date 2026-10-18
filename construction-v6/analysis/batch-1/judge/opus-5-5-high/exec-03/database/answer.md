# banco

## Visão geral

A foto do banco veio vazia: `database/database-schema.json` tem `tables: []`. O serviço não chegou a criar nada no PostgreSQL, então esta correção foi feita pelo código, pela migração `src/main/resources/db/migration/V1__Initial_schema.sql` e pelas entidades JPA.

A causa mais provável é que o Flyway nunca rodou. O `pom.xml:55-63` traz só `flyway-core` e `flyway-database-postgresql`. No Spring Boot 4.1.1, a configuração automática do Flyway fica num módulo separado, `spring-boot-flyway`, que o BOM declara (`spring-boot-dependencies-4.1.1.pom:1693`) e que o `spring-boot-autoconfigure-4.1.1.jar` não traz. Sem ela, nenhuma migração roda, e o `ddl-auto: validate` (`src/main/resources/application.yml:6`) recusa o banco vazio na subida.

O desenho que o agente escreveu é razoável:
- todas as tabelas têm chave primária;
- o dinheiro está em `NUMERIC(10, 2)`;
- o esquema é criado por um mecanismo só, uma migração versionada;
- o pedido guarda uma foto do resumo e do preço de cada item.

Os problemas, do mais grave para o menos grave:
1. **Fuso nas datas.** Os instantes estão em `TIMESTAMP` sem fuso (`created_at`, `paid_at`).
2. **Regras do negócio só no código.** Nenhuma das três regras invariáveis tem garantia no banco: não há `CHECK` no estoque nem na quantidade, e não há restrição única no `transaction_id`.
3. **Resumo aceita nulo.** As onze colunas do resumo do pedido aceitam nulo.
4. **Chaves estrangeiras faltando.** O aviso ao cliente não tem chave estrangeira para o pedido nem para o cliente.
5. **Índice faltando.** A chave estrangeira `order_items.sku` não tem índice.

## Item a item

**B1, chave primária.** Não há foto, então a resposta vem da migração. As cinco tabelas têm chave primária:
- `products.sku` (`src/main/resources/db/migration/V1__Initial_schema.sql:2`);
- `customers.cpf` (`:11`);
- `orders.id BIGSERIAL` (`:17`);
- `order_items.id BIGSERIAL` (`:46`);
- `notifications.id BIGSERIAL` (`:54`).

As entidades batem com isso: `@Id` em `model/Product.java:9`, `Customer.java:9`, `Order.java:13`, `OrderItem.java:9` e `Notification.java:20`.
Conclusão: segue

**B2, as ligações.** As ligações que o esquema guarda em coluna:

| ligação | coluna | chave estrangeira |
|---|---|---|
| pedido → cliente | `orders.customer_cpf` | tem, `REFERENCES customers(cpf)` (`V1__Initial_schema.sql:18`) |
| item → pedido | `order_items.order_id` | tem, `REFERENCES orders(id)` (`:47`) |
| item → produto | `order_items.sku` | tem, `REFERENCES products(sku)` (`:48`) |
| aviso ao cliente → pedido | `notifications.order_id` | **falta** (`:57`) |
| aviso ao cliente → cliente | `notifications.customer_cpf` | **falta** (`:55`) |

A entidade também guarda as duas ligações do aviso como valores soltos: `Long orderId` e `String customerCpf` (`model/Notification.java:25-33`).

O aviso de pagamento não tem tabela própria. Ele é guardado nas colunas `transaction_id` e `paid_at` do próprio pedido (`:30-31`), então não há chave faltando ali.

Há também um conflito entre a chave estrangeira e o código. A chave em `orders.customer_cpf` está certa, mas o cliente só é criado no pagamento (`src/main/java/com/loja/order/service/OrderService.java:212-215`). O registro do pedido salva o pedido sem criar o cliente antes (`OrderService.java:142-178`). Num PostgreSQL com essa migração aplicada, o primeiro pedido de um CPF novo provavelmente seria recusado pela chave estrangeira. Isso não muda a conclusão do item, mas a suíte de comportamento deve ver a falha.
Conclusão: em parte

**B3, índice nas chaves estrangeiras.**
- `orders.customer_cpf` tem `idx_orders_customer` (`V1__Initial_schema.sql:62`).
- `order_items.order_id` tem `idx_order_items_order` (`:64`).
- `order_items.sku` → `products(sku)` (`:48`) **não tem índice** que comece por essa coluna.

`notifications.order_id` tem um índice que começa por ela, `idx_notifications_order_type (order_id, type)` (`:66`). Esse índice já serviria se a chave estrangeira do B2 fosse criada.
Conclusão: em parte

**B4, o dinheiro num tipo exato.** Todo valor em dinheiro está em `NUMERIC(10, 2)`:
- `products.price` (`V1__Initial_schema.sql:4`);
- `customers.credit_balance` (`:13`);
- as colunas de dinheiro do resumo em `orders` (`:32-34`, `:36-38`, `:40-41`);
- `order_items.price` (`:50`).

As entidades usam `BigDecimal` (`model/OrderSummary.java:8-17`, `Product.java:17`, `Customer.java:17`). Não há `real`, `double precision` nem `money`. O peso está em `NUMERIC(10, 6)` (`:5`), que também é exato.
Conclusão: segue

**B5, as datas e horas com fuso.** As três colunas de instante usam `TIMESTAMP`, que no PostgreSQL é `timestamp without time zone`:
- `orders.created_at` (`V1__Initial_schema.sql:21`);
- `orders.paid_at` (`:31`);
- `notifications.created_at` (`:59`).

No código, esses campos são `Instant` (`model/Order.java:29`, `:63`, `Notification.java:40`). A regra das 48 horas compara `created_at` com o relógio (`Order.java:198-199`). Se o fuso da sessão mudar, o instante gravado fica ambíguo. A régua diz que uma coluna de instante sem fuso já basta para `não segue`.
Conclusão: não segue

**B6, o obrigatório é `NOT NULL`.**

O que está certo como `NOT NULL`:
- o CPF e o e-mail (`V1__Initial_schema.sql:11-12`, `:18-19`);
- a situação e a data de registro (`:20-21`);
- o SKU, o nome, o preço, o peso, o estoque e o `discontinued` do produto (`:2-7`);
- o saldo (`:13`);
- a quantidade e o preço do item (`:48-50`);
- todas as colunas do aviso (`:55-59`).

O que fica nulo com razão, por só existir depois: `tracking_code`, `transaction_id`, `paid_at` e `coupon` (`:22`, `:29-31`).

O problema: as **onze colunas do resumo aceitam nulo** (`:32-42`). São elas `products_subtotal`, `coupon_discount`, `shipping_cost`, `delivery_days`, `insurance`, `payment_adjustment`, `final_total`, `installments_summary`, `installment_amount`, `next_purchase_credit` e `gift`. O enunciado diz que todo pedido registrado tem um resumo que não muda, e a régua cita explicitamente os valores do resumo como obrigatórios.
Conclusão: em parte

**B7, as regras invariáveis no banco.**
- **Estoque nunca negativo.** `stock INTEGER NOT NULL` não tem `CHECK (stock >= 0)` (`V1__Initial_schema.sql:6`). Quem cuida disso é só o código: `checkStock` e `decreaseStock` com trava pessimista (`OrderService.java:404-419`, `repository/ProductRepository.java:10-12`).
- **Aviso de pagamento repetido.** O `transaction_id` fica numa coluna do pedido, sem restrição única (`:30`). A proteção contra repetição é só do código: a comparação em `OrderService.java:192-194` e a checagem da situação em `:196-198`, sob a trava do pedido (`repository/OrderRepository.java:11-13`).
- **Quantidade positiva.** `quantity INTEGER NOT NULL` não tem `CHECK (quantity > 0)` (`:49`).

O banco não garante nenhuma das três.
Conclusão: não segue

**B8, dado repetido sem motivo.**

O que é foto do momento, pedida pelo negócio:
- o pedido guarda os valores do resumo (`V1__Initial_schema.sql:32-42`);
- o item guarda o preço da compra (`:50`), preenchido com o preço da hora em `OrderService.java:156-159`;
- o pedido guarda os dados daquela compra: e-mail, região, CEP, nível e forma de pagamento (`:19-28`).

O `orders.customer_email` repete o `customers.email`, mas é o dado do cliente naquela compra. É também a fonte de onde o cliente é criado depois (`OrderService.java:214`).

O `notifications.customer_cpf` pode ser deduzido do pedido. Mesmo assim, o enunciado diz que o aviso é de um cliente, e o CPF não muda, então as duas versões não divergem.

Não há lista guardada como texto ou JSON. Os itens estão na tabela `order_items`.

Há uma repetição menor dentro da mesma tabela: `orders.installments` (`:27`) e `orders.installments_summary` (`:39`) guardam o mesmo número. Não é repetição entre tabelas.
Conclusão: segue

**B9, como o esquema é criado.** O mecanismo escolhido é uma ferramenta de migração versionada, o Flyway:
- as migrações `db/migration/V1__Initial_schema.sql` e `V2__Initial_catalog.sql`;
- `spring.flyway.enabled: true` (`src/main/resources/application.yml:17-19`);
- o Hibernate só em `validate` (`:6`), então não há um segundo mecanismo criando tabelas.

O perfil `src/test/resources/application-test.yml:10-14` juntaria `create-drop` e Flyway, o que a documentação desaconselha. Mas nenhum teste ativa esse perfil: não há `@ActiveProfiles` nem `@SpringBootTest` em `src/test`.

O problema é que o mecanismo escolhido não está ligado. O `pom.xml:55-63` não traz `spring-boot-starter-flyway`, e no Boot 4.1.1 a configuração automática do Flyway não está no `spring-boot-autoconfigure` (conferido no jar do repositório Maven local). Na prática nada cria o esquema, e o `validate` derruba a subida. Isso bate com a foto vazia.

Há ainda uma divergência entre a entidade e a migração. O campo embutido `OrderSummary.installments` (`model/OrderSummary.java:15`) não tem `@AttributeOverride`. Pela nomeação padrão ele cairia na coluna `installments`, a mesma de `Order.installments` (`model/Order.java:49-50`), e a coluna `installments_summary` da migração (`:39`) ficaria sem mapeamento.

A escolha é a recomendada, mas não funciona como está.
Conclusão: em parte

**B10, tamanho dos textos.**

Com tamanho justificado pelo domínio:
- o CPF tem `VARCHAR(11)` (`V1__Initial_schema.sql:11`, `:18`, `:55`);
- o CEP tem `VARCHAR(8)` (`:25`).

Com tamanho arbitrário:
- `VARCHAR(255)` em `products.name`, `customers.email`, `orders.customer_email`, `tracking_code` e `transaction_id` (`:3`, `:12`, `:19`, `:22`, `:30`);
- `VARCHAR(30)` em todos os códigos de situação, região, entrega, pagamento e nível (`:20`, `:23-24`, `:26`, `:28`, `:56`, `:58`);
- `VARCHAR(50)` no cupom (`:29`);
- `VARCHAR(20)` no SKU (`:2`).

Não há `char(n)`.
Conclusão: em parte

## O que mudaria primeiro

1. **Ligar o Flyway e acertar os tipos.** Trocar `flyway-core` por `spring-boot-starter-flyway` e mudar os três `TIMESTAMP` para `TIMESTAMPTZ`. Sem a primeira mudança, o banco simplesmente não existe. A segunda fecha o único item que a régua reprova por uma coluna só. Junto com isso, convém mapear `installments_summary` com `@AttributeOverride` para a entidade bater com a migração.
2. **Pôr as regras e as ligações no banco.** Isso significa:
   - `CHECK (stock >= 0)` e `CHECK (quantity > 0)`;
   - uma restrição única que torne o pagamento idempotente no próprio banco;
   - `NOT NULL` nas colunas do resumo;
   - chaves estrangeiras de `notifications` para `orders` e `customers`;
   - um índice em `order_items.sku`.

   Para as chaves estrangeiras funcionarem, o cliente precisa ser criado no registro do pedido, como o enunciado pede, e não só no pagamento.

## Dúvidas

- **Por que a foto veio vazia.** O motivo de o serviço não subir foi deduzido do código, não observado. A falta do módulo do Flyway no Boot 4 está conferida no BOM e no jar. Há outras causas que podem ter contribuído, e não verifiquei nenhuma rodando o serviço:
  - a coluna `installments` duplicada no mapeamento de `Order`;
  - o `validate` do Hibernate recusar `timestamp` para campos `Instant`.
- **O idempotente no B7.** Uma restrição única só em `transaction_id` não reproduz exatamente a regra "mesmo pedido e mesmo `transactionId`". Li o item como: o banco não tem nenhuma garantia desse tipo, e a proteção é só do código.
- **O `notifications.customer_cpf` no B8.** A coluna pode ser deduzida pelo pedido. Não contei como repetição sem motivo, porque o enunciado define o aviso como "de um pedido e de um cliente" e o CPF não muda.
