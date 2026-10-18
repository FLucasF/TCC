# Banco

## Visão geral

Quem cria o esquema é o Hibernate, com `ddl-auto=update` a partir das entidades JPA. O agente não escolheu nada além dos tipos Java, e por isso o banco tem só o que o JPA põe por padrão. Os pontos fortes vêm sobretudo desses padrões e de escolhas certas de tipo Java. Toda tabela tem chave primária. O dinheiro está em `numeric`, porque o agente usou `BigDecimal`. O instante do registro guarda o fuso, porque o agente usou `Instant`. O resumo do pedido fica guardado como foto do momento, em colunas do próprio pedido. Os problemas, do mais sério para o menos sério:

1. Das ligações do domínio, só uma é chave estrangeira (item → pedido). Pedido → cliente, item → produto e aviso → pedido/cliente são colunas soltas.
2. Nenhuma das três regras invariáveis do negócio está no banco: estoque nunca negativo, aviso de pagamento repetido sem efeito e quantidade positiva.
3. Quase todas as colunas obrigatórias aceitam nulo. Só ficaram `NOT NULL` as chaves, as de `@Version` e as de tipo primitivo Java.
4. A única chave estrangeira não tem índice.
5. O esquema não tem migração versionada.
6. Todos os textos são `character varying(255)`.

## Item a item

**B1, chave primária.** As cinco tabelas do domínio têm chave primária (`database/database-schema.json`, restrições `p`):
- `customers_pkey (cpf)`
- `notifications_pkey (id)`
- `order_items_pkey (id)`
- `orders_pkey (id)`
- `products_pkey (sku)`

Elas vêm dos `@Id` em `src/main/java/com/loja/domain/Customer.java:9`, `Notification.java:8`, `OrderItem.java:8`, `Order.java:12` e `Product.java:9`. Nenhuma tabela está sem chave.
Conclusão: segue

**B2, as ligações.** As ligações que o esquema guarda em coluna, uma a uma:
- **Item → pedido:** tem chave estrangeira, `fkbioxgbv59vetrxe0ejfubep1w`, `FOREIGN KEY (order_id) REFERENCES orders(id)`. Ela vem do `@ManyToOne` em `OrderItem.java:15-17`.
- **Pedido → cliente:** `orders.customer_cpf` não tem chave estrangeira para `customers(cpf)`. É um `String` simples em `Order.java:16`. O cliente é criado na mesma transação, antes de o pedido ser gravado (`service/OrderService.java:54-57`), então a chave seria viável.
- **Item → produto:** `order_items.sku` não tem chave estrangeira para `products(sku)` (`OrderItem.java:12`).
- **Aviso → pedido:** `notifications.order_id` não tem chave estrangeira (`Notification.java:17`, um `Long`).
- **Aviso → cliente:** `notifications.customer_cpf` não tem chave estrangeira (`Notification.java:12`).
- **Aviso de pagamento → pedido:** não há tabela de avisos de pagamento. O `transactionId` fica numa coluna do próprio pedido, `orders.payment_transaction_id` (`Order.java:40`), então não há chave faltando aqui.

Uma chave estrangeira em cinco ligações guardadas em coluna.
Conclusão: não segue

**B3, índice nas chaves estrangeiras.** A única chave estrangeira, `order_items.order_id`, não tem índice. Em `order_items`, o único índice é `order_items_pkey`, sobre `(id)`. O serviço lê os itens de cada pedido por essa coluna (`@OneToMany(mappedBy = "order")`, `Order.java:23`), então a falta pesa.

Fora do item, mas no mesmo sentido: também não têm índice `orders.customer_cpf` e `notifications.customer_cpf`. Seriam chaves estrangeiras se o B2 fosse corrigido, e são consultadas pelo serviço:
- `existsByCustomerCpfAndStatusIn` em `service/CheckoutService.java:239`, para o WELCOME10;
- `findByCustomerCpfOrderByIdAsc` em `service/NotificationService.java:28`, para a lista de avisos.

Conclusão: não segue

**B4, dinheiro em tipo exato.** Todas as colunas de dinheiro são `numeric(38,2)` na foto:
- `products.price`;
- `customers.credit_balance`;
- no pedido: `orders.products_subtotal`, `coupon_discount`, `shipping_cost`, `insurance`, `payment_adjustment`, `final_total`, `installment_amount` e `next_purchase_credit`.

Nas entidades, todas são `BigDecimal` (`Order.java:28-37`, `Product.java:14`, `Customer.java:12`). Não há `real`, `double precision` nem `money`. A precisão `(38,2)` é o padrão do Hibernate para `BigDecimal`, e não uma escolha do agente, mas é um tipo exato com centavos.

Uma observação fora do item: `products.weight_kg` também ficou `numeric(38,2)`. As escala 2 cabe nos pesos do catálogo de hoje, mas cortaria um peso como 0,125 kg.
Conclusão: segue

**B5, datas com fuso.** A única coluna de instante é `orders.created_at`, do tipo `timestamp(6) with time zone` na foto. Ela vem de um `Instant` (`Order.java:21`). O `paidAt` do aviso de pagamento não é guardado em coluna nenhuma, então não há outra coluna de data para avaliar. Nenhuma coluna está como `timestamp without time zone`.
Conclusão: segue

**B6, `NOT NULL` no que é obrigatório.** Na foto, só são `NOT NULL` as chaves primárias, as colunas `version` e as que vêm de tipo primitivo Java:
- `orders.delivery_days`, `installments` e `gift`;
- `order_items.quantity`;
- `products.stock` e `discontinued`.

Aceitam nulo, embora o enunciado diga que todo registro as tem:
- **`orders`:** `customer_cpf`, `status`, `created_at` e todos os valores do resumo (`products_subtotal`, `coupon_discount`, `shipping_cost`, `insurance`, `payment_adjustment`, `final_total`, `installment_amount`, `next_purchase_credit`);
- **`order_items`:** `sku` e `order_id`;
- **`products`:** `name`, `price` e `weight_kg`;
- **`customers`:** `credit_balance`, o saldo;
- **`notifications`:** `customer_cpf`, `order_id`, `type` e `status`.

Não há `@Column(nullable = false)` nem `@NotNull` em nenhuma entidade (o `grep` por `nullable` não acha nada). Aceitam nulo com razão `orders.tracking_code` (não existe antes do envio) e `orders.payment_transaction_id` (não existe antes do pagamento).
Conclusão: não segue

**B7, regras invariáveis no banco.**
- **Estoque nunca negativo:** não há `CHECK (stock >= 0)` em `products`. As únicas restrições `c` da foto são as que o Hibernate gera para os enums (`orders_status_check`, `notifications_status_check`, `notifications_type_check`). A trava fica só no código: o `if (stock < quantity)` em `Product.java:64-68`, com o `PESSIMISTIC_WRITE` de `repository/ProductRepository.java`.
- **Aviso de pagamento repetido:** não há restrição única com o id da transação. `orders.payment_transaction_id` não tem `UNIQUE`, e não existe tabela de pagamentos processados. Quem cuida disso é o código:
  - um `HashMap` em memória (`service/OrderService.java:29`, `71-80`);
  - a conferência da situação do pedido (`OrderService.java:82-89`);
  - o `@Version` do pedido (`Order.java:42`).

  O `HashMap` some quando o serviço reinicia e não é compartilhado entre as duas máquinas. Isso pode ser um problema de comportamento, mas aqui conta só que o banco não protege a regra.
- **Quantidade positiva:** `order_items.quantity` é `integer NOT NULL`, sem `CHECK (quantity > 0)`. A validação fica só na entrada, no código.

Nenhuma das três regras está garantida no banco.
Conclusão: não segue

**B8, dado repetido sem motivo.**
- **Os valores do resumo** (`orders.products_subtotal` … `gift`, `Order.java:28-38`) são a foto do momento que o enunciado pede.
- **`notifications.customer_cpf`** pode ser deduzido do pedido (`orders.customer_cpf`). Mas o enunciado põe o aviso como de um pedido e de um cliente, e o CPF é a chave do cliente e não muda. Por isso não há risco de duas versões divergirem.
- **Nada que muda é copiado:** nem o preço atual do produto, nem o nome, nem o e-mail. O e-mail, aliás, não é guardado em lugar nenhum.
- **Nenhuma lista vai como texto ou JSON:** os itens têm tabela própria, `order_items`.

Não encontrei dado repetido sem motivo.
Conclusão: segue

**B9, como o esquema é criado.** O Hibernate gera o esquema a partir das entidades, com `spring.jpa.hibernate.ddl-auto=update` (`src/main/resources/application.properties:8`). O `pom.xml` não tem Flyway nem Liquibase, e não há `schema.sql` nem pasta `db/migration` (o `find` por `*.sql` não acha nada). O catálogo inicial é gravado por um `CommandLineRunner` (`config/DataInitializer.java:14-41`), não por uma migração. Os testes usam H2 com `create-drop` (`src/test/resources/application.properties:8`), então o esquema que os testes veem não é o do PostgreSQL.

O lado bom é que só um mecanismo cria o esquema. O fraco é que não é a ferramenta de migração versionada que a documentação recomenda. O `update` não renomeia nem apaga colunas e não acrescenta restrições a tabelas que já existem. E, com duas máquinas subindo ao mesmo tempo, as duas rodam DDL sobre o mesmo banco.
Conclusão: em parte

**B10, tamanho dos textos.** Todas as colunas de texto da foto são `character varying(255)`, o padrão do JPA:
- `customers.cpf`;
- `products.sku` e `products.name`;
- `order_items.sku`;
- `orders.customer_cpf`, `status`, `tracking_code` e `payment_transaction_id`;
- `notifications.customer_cpf`, `type` e `status`.

Nenhuma tem tamanho escolhido por motivo do domínio (o CPF teria 11) nem é `text`.
Conclusão: não segue

## O que mudaria primeiro

1. **Migrações versionadas no Flyway no lugar do `ddl-auto=update`, escrevendo o DDL à mão.** É a mudança que destrava quase todo o resto, porque no mesmo script entram:
   - as chaves estrangeiras que faltam (`orders.customer_cpf`, `order_items.sku`, `notifications.order_id`, `notifications.customer_cpf`), com índice em cada uma;
   - `NOT NULL` nas colunas obrigatórias;
   - `CHECK (stock >= 0)` e `CHECK (quantity > 0)`;
   - os tipos de texto escolhidos.

   Também resolve a corrida de DDL entre as duas máquinas e permite gravar o catálogo inicial numa migração.
2. **Pôr a idempotência do pagamento no banco.** Uma tabela de avisos de pagamento com chave primária ou restrição única em `(order_id, transaction_id)` e com o resultado da primeira resposta. Hoje essa regra depende de um `HashMap` em memória, que não sobrevive a um reinício nem é compartilhado entre as duas máquinas.

## Dúvidas

- **Aviso de pagamento → pedido (B2):** como o esquema guarda o `transactionId` numa coluna do próprio pedido, entendi que essa ligação está guardada dentro de `orders` e não é chave faltando. A falta de uma tabela de pagamentos entrou no B7.
- **`notifications.customer_cpf` (B8):** tratei a coluna como a ligação aviso → cliente que a régua tira do enunciado, e não como repetição. O CPF é imutável e não pode divergir.
- **`notifications.order_id` obrigatório (B6):** entendi que é obrigatório porque o enunciado põe `orderId` em todo aviso.
