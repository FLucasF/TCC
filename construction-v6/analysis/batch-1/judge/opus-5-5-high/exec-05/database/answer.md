# o banco

## Visão geral

O agente deixou o Hibernate criar o esquema a partir das entidades JPA (`ddl-auto=update`). Nenhuma tabela foi escrita à mão e não há migração versionada. O básico das colunas está certo: as cinco tabelas têm chave primária, todo valor em dinheiro é `numeric(10,2)`, o único instante guardado (`orders.created_at`) é `timestamp with time zone`, e o resumo do pedido fica gravado no próprio pedido como foto do momento. Os problemas, do mais grave ao menos grave:

- **As ligações entre as tabelas quase não existem no banco.** De cinco ligações do domínio, só uma tem chave estrangeira (o item aponta para o pedido). O pedido não aponta para o cliente, o item não aponta para o produto e o aviso não aponta nem para o pedido nem para o cliente.
- **Nenhuma das três regras invariáveis está protegida no banco.** Não há `CHECK` para o estoque não ficar negativo nem para a quantidade ser positiva, e nenhuma restrição única impede que o mesmo aviso de pagamento seja processado duas vezes.
- **Faltam índices.** A única chave estrangeira não tem índice, e as colunas `customer_cpf`, que o código usa para buscar, também não.
- **Problemas menores:**
  - `order_items.order_id` aceita nulo;
  - o esquema é criado pelo `ddl-auto=update`, em vez de uma ferramenta de migração;
  - todos os textos ficaram com o `varchar(255)` padrão do JPA.

## Item a item

**B1, chave primária.** As cinco tabelas têm chave primária (`database/database-schema.json`, as restrições `p`):
- `customers_pkey (cpf)`;
- `products_pkey (sku)`;
- `orders_pkey (id)`;
- `order_items_pkey (id)`;
- `notifications_pkey (id)`.

As três últimas são identidade gerada pelo banco (`GenerationType.IDENTITY` em `src/main/java/com/loja/orderservice/entity/Order.java:15`, `OrderItem.java:10` e `Notification.java:12`). O CPF e o SKU são chaves naturais, que o enunciado trata como identificadores. Segue a prática.
Conclusão: segue

**B2, as ligações.** Das cinco ligações que o esquema guarda em coluna, só uma tem chave estrangeira:
- **O item é de um pedido:** `order_items.order_id` tem a chave estrangeira `fkbioxgbv59vetrxe0ejfubep1w`, `FOREIGN KEY (order_id) REFERENCES orders(id)`, que vem do `@JoinColumn(name = "order_id")` em `Order.java:31-33`. Tem.
- **O pedido é de um cliente:** `orders.customer_cpf` é um `String` solto (`Order.java:18-19`), sem chave estrangeira para `customers(cpf)`. Falta. O código grava o cliente antes do pedido (`src/main/java/com/loja/orderservice/service/OrderService.java:65-69`), então a chave caberia sem mudar a ordem das gravações.
- **O item é de um produto:** `order_items.sku` (`OrderItem.java:13-14`) não tem chave estrangeira para `products(sku)`. Falta.
- **O aviso é de um pedido:** `notifications.order_id` (`Notification.java:22-23`) não tem chave estrangeira para `orders(id)`. Falta.
- **O aviso é de um cliente:** `notifications.customer_cpf` (`Notification.java:15-16`) não tem chave estrangeira para `customers(cpf)`. Falta.

O aviso de pagamento não tem tabela própria. Dele só fica o `orders.last_transaction_id`, na própria linha do pedido (`Order.java:62`). Por isso não há chave estrangeira a cobrar ali (o efeito disso aparece no B7). A foto mostra uma única restrição `f` no banco inteiro.
Conclusão: não segue

**B3, índices nas chaves estrangeiras.** A única chave estrangeira, `order_items.order_id`, não tem índice. Os índices de `order_items` se resumem a `order_items_pkey (id)` (`database/database-schema.json`, `indexes`). O custo aparece em todo pedido lido, porque os itens vêm junto (`fetch = FetchType.EAGER`, `Order.java:31`) e são procurados por `order_id`.

As colunas que deveriam ser chaves estrangeiras também não têm índice, e o código busca por elas:
- `notifications.customer_cpf`, na busca `findByCustomerCpfOrderByIdAsc` (`src/main/java/com/loja/orderservice/repository/NotificationRepository.java:12`);
- `orders.customer_cpf`, na contagem `countByCustomerCpfAndStatusIn` da regra do WELCOME10 (`src/main/java/com/loja/orderservice/service/CheckoutCalculator.java:307`).
Conclusão: não segue

**B4, dinheiro em tipo exato.** Todas as colunas de dinheiro são `numeric(10,2)`:
- `products.price`;
- `customers.credit_balance`;
- os nove valores do resumo em `orders`: `products_subtotal`, `coupon_discount`, `shipping_cost`, `insurance`, `payment_adjustment`, `final_total`, `installment_amount` e `next_purchase_credit`.

Não há `real`, `double precision` nem `money` (`database/database-schema.json`, `columns`). No Java são `BigDecimal` com `precision = 10, scale = 2` (`OrderSummary.java:10-38`, `Product.java:16-17`, `Customer.java:13-14`). Segue a prática.
Conclusão: segue

**B5, datas e horas com fuso.** O único instante guardado é `orders.created_at`, do tipo `timestamp(6) with time zone`, mapeado de um `Instant` (`Order.java:28-29`). O `paidAt` do aviso de pagamento não é gravado, então não existe uma segunda coluna de instante a conferir. Não há nenhuma coluna `timestamp without time zone`.
Conclusão: segue

**B6, o obrigatório é NOT NULL.** Quase todas as colunas obrigatórias são `NOT NULL`:
- **Os dados de compra em `orders`:** `customer_cpf`, `customer_email`, `status`, `created_at`, `zip_code`, `region`, `shipping_method`, `payment_method` e `club_tier`, além de todos os campos do resumo.
- **`products`:** todas as colunas.
- **`customers`:** `credit_balance`.
- **`order_items`:** `sku` e `quantity`.
- **`notifications`:** todas as colunas.

Aceitam nulo só as colunas que podem faltar: `orders.coupon` (o cupom é opcional), `orders.tracking_code` (só existe depois do envio) e `orders.last_transaction_id` (só existe depois do pagamento).

A exceção é **`order_items.order_id`, que aceita nulo** (`"nullable": true`), embora todo item seja de um pedido. A causa é o `@OneToMany` unidirecional com `@JoinColumn` sem `nullable = false` (`Order.java:31-33`). Nesse mapeamento o Hibernate grava o item sem pedido e só depois preenche o `order_id`, e o banco aceita um item órfão.
Conclusão: em parte

**B7, as regras invariáveis no banco.** Nenhuma das três está no banco:
- **O estoque nunca fica negativo.** `products.stock` é `integer NOT NULL` sem `CHECK`: as restrições de `products` se resumem a `products_pkey`. Quem cuida da regra é o código: ele trava a linha com `PESSIMISTIC_WRITE` (`src/main/java/com/loja/orderservice/repository/ProductRepository.java:16-18`) e compara o estoque com a quantidade antes de baixar (`OrderService.java:203-210`). A regra fica só no código.
- **O mesmo aviso de pagamento não é processado duas vezes.** O id da transação vai para `orders.last_transaction_id`, uma coluna comum, sem restrição única e sem uma tabela de pagamentos com chave `(order_id, transaction_id)`. A repetição é barrada por um `if` (`OrderService.java:90-93`) sobre um pedido lido sem trava (`findById`, `OrderService.java:87`), e o pedido não tem `@Version`. Pela leitura (não medi isso), dois avisos iguais que chegassem juntos nas duas máquinas passariam os dois pelo `if`. O banco não impediria a segunda baixa de estoque nem o segundo crédito. A regra fica só no código.
- **A quantidade do item é positiva.** `order_items.quantity` não tem `CHECK (quantity > 0)`. Só a validação da entrada cuida disso.

Como ponto positivo, o Hibernate criou `CHECK`s de domínio para todas as colunas de enum, como `orders_status_check`, `orders_region_check` e `notifications_type_check`. Mas nenhum deles cobre as três regras.
Conclusão: não segue

**B8, repetição sem motivo.** Não encontrei repetição de dado que muda:
- **Cliente:** do cliente o pedido guarda só `customer_cpf` e `customer_email`. O e-mail não está em `customers`, então não há duas versões dele, e no pedido ele é o dado daquela compra.
- **Resumo e dados de compra:** `orders` guarda os valores do resumo e o que foi escolhido na compra (região, CEP, entrega, cupom, pagamento, nível do clube). É a foto do momento que o enunciado pede.
- **Itens:** viram linhas de `order_items`, não JSON.
- **Preço do produto:** `products.price` não é copiado em lugar nenhum.

O único dado derivável é `notifications.customer_cpf`, que também se obtém por `notifications.order_id → orders.customer_cpf`. Mas o enunciado define o aviso como "de um pedido e de um cliente", e o CPF de um pedido não muda depois do registro. Não vejo risco de divergência que conte aqui.

Uma observação fora da régua: o item não guarda o preço unitário da compra (`OrderItem.java:13-17`). Não é repetição; é uma foto do momento que ficou incompleta, porque só o subtotal do pedido fica gravado.
Conclusão: segue

**B9, como o esquema é criado.** O Hibernate cria o esquema a partir das entidades, com `spring.jpa.hibernate.ddl-auto=update` (`src/main/resources/application.properties:8`):
- o `pom.xml` não tem Flyway nem Liquibase;
- não existe `schema.sql` nem pasta `db/migration`;
- a carga do catálogo é um `CommandLineRunner` em Java (`src/main/java/com/loja/orderservice/config/DataInitializer.java:17-47`), não um script.

O ponto bom é que há um mecanismo só, sem misturar dois. O ponto fraco é que esse mecanismo não é a ferramenta versionada que a documentação recomenda. O `update` não tem histórico, não remove nem altera coluna com segurança, e roda ao mesmo tempo nas duas máquinas de produção quando elas sobem.

Os testes usam outro banco e outro modo (H2 com `create-drop`, `src/test/resources/application-test.properties:1-6`). Então o esquema que a suíte do projeto exercita não é o do PostgreSQL.
Conclusão: em parte

**B10, tamanho dos textos.** Todas as colunas de texto são `character varying(255)`, o padrão do JPA:
- os CPFs (`customers.cpf`, `orders.customer_cpf`, `notifications.customer_cpf`), que têm 11 dígitos;
- `orders.zip_code`, que tem 8 dígitos;
- os SKUs, `name`, `customer_email`, `tracking_code` e `last_transaction_id`;
- todas as colunas de enum.

Nenhuma coluna usa `text`, e nenhum tamanho foi escolhido por motivo do domínio. É o caso fraco que a régua descreve, o 255 em todas.
Conclusão: não segue

## O que mudaria primeiro

1. **Passar o esquema para uma migração Flyway.** O `V1__init.sql` traria:
   - as chaves estrangeiras que faltam: `orders.customer_cpf → customers`, `order_items.sku → products`, `notifications.order_id → orders` e `notifications.customer_cpf → customers`;
   - `order_items.order_id NOT NULL`;
   - índices em `order_items.order_id`, `orders.customer_cpf` e `notifications.customer_cpf`.

   Junto, trocaria o `ddl-auto` para `validate`. Isso resolve B2, B3, B6 e B9 de uma vez e para de depender do `update` rodando nas duas máquinas.
2. **Proteger as regras invariáveis no banco.**
   - `CHECK (stock >= 0)` em `products` e `CHECK (quantity > 0)` em `order_items`.
   - Uma tabela `payments` com chave única `(order_id, transaction_id)`, ou ao menos `UNIQUE` no id da transação. Assim um aviso repetido falha no banco mesmo que dois cheguem juntos nas duas máquinas.

## Dúvidas

- **Onde está a foto do esquema.** A régua cita `suite/database-schema.json`, mas o arquivo está em `database/database-schema.json`, como diz a tabela do guia. Usei esse.
- **O aviso de pagamento no B2.** Ele não tem tabela; dele só fica o `last_transaction_id` na linha do pedido. Li que não há chave estrangeira a cobrar e levei a falta de garantia contra repetição para o B7.
- **`notifications.customer_cpf` no B8.** Como o enunciado liga o aviso também ao cliente, não contei essa coluna como repetição sem motivo, embora o CPF seja derivável pelo pedido.
- **A corrida no pagamento repetido (B7).** É uma leitura do código, não um teste. Usei-a só para mostrar que a proteção é só do código, não para dizer que o serviço erra.
- **B9 como `em parte`.** Pesei que há um mecanismo só, o que segue a documentação, mas não é a ferramenta de migração versionada que ela recomenda.
