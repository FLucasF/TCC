# banco

## Visão geral

O agente criou o esquema com um `schema.sql` próprio, que o Spring roda a cada vez que o serviço sobe (`spring.sql.init.mode=always`). O Hibernate fica só na validação (`ddl-auto=validate`). A foto do banco bate coluna por coluna com esse script. O esquema é limpo nos fundamentos:
- as seis tabelas têm chave primária;
- as seis ligações do domínio viraram chaves estrangeiras;
- todo dinheiro está em `numeric(10,2)`;
- o que é obrigatório é `NOT NULL`;
- as três regras invariáveis estão no próprio banco: `CHECK (stock >= 0)`, `CHECK (quantity > 0)` e `UNIQUE (order_id)` no pagamento.

Os problemas, do mais grave para o menos grave:
1. **Os três instantes estão sem fuso.** As colunas são `timestamp without time zone`, e o código usa `LocalDateTime`, o que deixa a regra das 48 horas ambígua.
2. **O esquema é criado por um script sem versão,** com `CREATE TABLE IF NOT EXISTS`, e há uma cópia paralela para o H2 nos testes.
3. **Duas chaves estrangeiras não têm índice.**
4. **O nível do clube está repetido em `customers`.** Essa cópia é gravada uma vez e nunca é atualizada nem lida.
5. **Os tamanhos dos textos são, em boa parte, arbitrários.**

## Item a item

**B1, chave primária.** As seis tabelas têm chave primária (`database/database-schema.json`, restrições `p`):
- `customers_pkey (cpf)`;
- `products_pkey (sku)`;
- `orders_pkey (id)`;
- `order_items_pkey (id)`;
- `order_payments_pkey (id)`;
- `notifications_pkey (id)`.

Nenhuma tabela ficou sem chave. Uma observação que não muda a conclusão: o item tem uma chave artificial `BIGSERIAL` (`src/main/resources/schema.sql:48`) e nenhuma restrição única em `(order_id, sku)`, embora o enunciado proíba o mesmo SKU duas vezes no carrinho.
Conclusão: segue

**B2, as ligações.** Todas as ligações que o enunciado descreve estão guardadas em coluna e têm chave estrangeira (`database/database-schema.json`, restrições `f`):

| ligação | chave estrangeira |
|---|---|
| pedido → cliente | `orders_customer_cpf_fkey` |
| item → pedido | `order_items_order_id_fkey` |
| item → produto | `order_items_sku_fkey` |
| aviso de pagamento → pedido | `order_payments_order_id_fkey` |
| aviso ao cliente → pedido | `notifications_order_id_fkey` |
| aviso ao cliente → cliente | `notifications_customer_cpf_fkey` |

Nenhuma coluna que aponta para outra tabela ficou sem `FOREIGN KEY` (`src/main/resources/schema.sql:44`, `:52-53`, `:61`, `:71-72`).
Conclusão: segue

**B3, o índice das chaves estrangeiras.** Das seis chaves, quatro têm um índice que começa pela coluna da chave:
- `orders.customer_cpf`, com `idx_orders_customer`;
- `order_items.order_id`, com `idx_order_items_order`;
- `order_payments.order_id`, com o índice único `order_payments_order_id_key`;
- `notifications.customer_cpf`, com `idx_notifications_customer`.

Duas não têm índice nenhum em `database/database-schema.json`, `indexes`:
- `order_items.sku`, da chave `order_items_sku_fkey`;
- `notifications.order_id`, da chave `notifications_order_id_fkey`.

O script cria só os quatro índices (`src/main/resources/schema.sql:75-78`).
Conclusão: em parte

**B4, dinheiro.** Todas as colunas de dinheiro são `numeric(10,2)`:
- `products.price`;
- `customers.credit_balance`;
- as oito colunas de valores do pedido (`products_subtotal`, `coupon_discount`, `shipping_cost`, `insurance`, `payment_adjustment`, `final_total`, `installment_amount`, `next_purchase_credit`).

A fonte é `database/database-schema.json`, `columns` (e `src/main/resources/schema.sql:4`, `:13`, `:30-38`). As entidades usam `BigDecimal` (`src/main/java/com/loja/domain/Order.java:41-64`). Não há `real`, `double precision` nem `money`. O peso também é exato (`weight_kg numeric(10,3)`).
Conclusão: segue

**B5, datas e horas.** As três colunas de instante são `timestamp without time zone` (`database/database-schema.json`):
- `orders.created_at`;
- `order_payments.paid_at`;
- `notifications.created_at`.

O script as cria assim em `src/main/resources/schema.sql:42`, `:60` e `:70`. O código confirma a ambiguidade. As entidades e o DTO usam `LocalDateTime` (`src/main/java/com/loja/domain/Order.java:72`, `src/main/java/com/loja/domain/OrderPayment.java:20`, `src/main/java/com/loja/dto/PaymentRequest.java:7`), então o `Z` de `paidAt` se perde. Já o registro grava `LocalDateTime.now()`, que é a hora local da JVM (`src/main/java/com/loja/service/OrderService.java:86`). A regra das 48 horas compara esses dois valores sem fuso (`src/main/java/com/loja/service/OrderService.java:298-300`). Com a máquina fora de UTC, a diferença do fuso entra na conta. Também não há nada no código que explique um padrão de UTC.
Conclusão: não segue

**B6, NOT NULL.** Tudo o que o enunciado torna obrigatório é `NOT NULL` na foto:
- o CPF (`customers.cpf`, `orders.customer_cpf`, `notifications.customer_cpf`);
- o e-mail, a situação, `created_at` e os valores do resumo do pedido;
- o SKU, o nome, o preço, o peso, o estoque e `discontinued` do produto;
- o saldo (`credit_balance`);
- a quantidade do item;
- `transaction_id` e `paid_at` do pagamento;
- `type`, `status` e `order_id` do aviso.

Só duas colunas aceitam nulo, e as duas podem: `orders.coupon` (o cupom é opcional) e `orders.tracking_code` (só existe depois do envio). A fonte é `database/database-schema.json`, `nullable`.
Conclusão: segue

**B7, as regras invariáveis no banco.**
- **Estoque nunca negativo:** é garantido por `products_stock_check`, `CHECK ((stock >= 0))` (`src/main/resources/schema.sql:6`).
- **Quantidade positiva:** é garantida por `order_items_quantity_check`, `CHECK ((quantity > 0))` (`src/main/resources/schema.sql:51`).
- **O aviso repetido não baixa o estoque nem dá o crédito duas vezes:** o banco tem `order_payments_order_id_key`, `UNIQUE (order_id)` (`src/main/resources/schema.sql:58`). A gravação do pagamento acontece na mesma transação da baixa do estoque e do crédito (`src/main/java/com/loja/state/AwaitingPaymentState.java:44-47`, dentro do `@Transactional` de `src/main/java/com/loja/service/OrderService.java:115`). Imagine dois avisos iguais concorrendo, mesmo em máquinas diferentes. Cada um tenta inserir uma linha com o mesmo `order_id`, e a restrição única derruba o segundo, que desfaz a própria baixa. A chave é o pedido e não o `transaction_id`. Isso é mais restritivo (um pagamento por pedido) e cobre o caso pedido.

Como bônus, há também `customers_credit_balance_check` (`credit_balance >= 0`).
Conclusão: segue

**B8, dados repetidos.** As listas estão em tabelas próprias (`order_items`, `notifications`, `order_payments`), sem JSON nem texto embutido. Os valores do resumo, `customer_email`, `club_tier`, `region`, `coupon` etc. guardados em `orders` (`src/main/resources/schema.sql:19-39`) são a foto da compra que o enunciado pede.

A repetição sem motivo é `customers.club_tier` (`src/main/resources/schema.sql:14`). É gravado só quando o cliente é criado (`src/main/java/com/loja/service/OrderService.java:54-57`) e nunca é atualizado nem lido: o único uso é o getter/setter em `src/main/java/com/loja/domain/Customer.java:38-39`. O nível chega a cada compra e fica em `orders.club_tier`. Assim, `customers.club_tier` vira uma segunda versão, congelada no primeiro pedido, que diverge assim que o cliente muda de nível. `customers.email` tem o mesmo comportamento: é gravado uma vez e nunca é atualizado. Mas ele é o dado de cadastro do cliente, e a cópia por compra está em `orders.customer_email`, então pesa menos.

O par `notifications.customer_cpf` e `notifications.order_id` é redundante (o CPF já sai do pedido). Mas as duas ligações estão no enunciado, e o CPF de um pedido não muda, então não há como divergir.
Conclusão: em parte

**B9, a criação do esquema.** Um mecanismo só cria o esquema: o script `src/main/resources/schema.sql`, rodado pelo Spring a cada vez que o serviço sobe (`src/main/resources/application.properties:10-11`). O Hibernate fica em `ddl-auto=validate` (`src/main/resources/application.properties:8`), que confere sem criar. Não há Flyway nem Liquibase no `pom.xml`, nem pasta `db/migration`.

O mecanismo é único, mas é um script próprio sem versão:
- com `CREATE TABLE IF NOT EXISTS`, qualquer mudança futura numa tabela que já existe não é aplicada;
- as duas máquinas rodam o script a cada subida;
- os testes usam uma cópia separada para o H2 (`src/test/resources/schema-h2.sql`, `src/test/resources/application-test.properties:9`), que já difere em sintaxe (`AUTO_INCREMENT`, `MERGE`) e pode se afastar da versão de produção.

Não chega a usar dois mecanismos ao mesmo tempo, mas também não usa a ferramenta de migração versionada que a documentação recomenda.
Conclusão: em parte

**B10, o tamanho dos textos.** Não há `char(n)`, e não é o 255 em todas as colunas. Os tamanhos se dividem assim:

| tipo de tamanho | colunas |
|---|---|
| motivo do domínio | `cpf varchar(11)`, `zip_code varchar(8)` |
| arbitrário | `sku varchar(50)`, `name` e `email varchar(255)`, `transaction_id varchar(100)`, `tracking_code varchar(50)` |
| `varchar(50)` para guardar um UUID | `orders.id`, gerado em `src/main/java/com/loja/service/OrderService.java:60` |
| códigos fixos com limite qualquer | `status`, `region`, `type` com 10, 20 e 30 |

A fonte é `database/database-schema.json`. Os tamanhos arbitrários criam limites que o negócio não pediu: um id de transação do banco com mais de 100 caracteres seria recusado. Para o id do pedido, o tipo `uuid` seria o natural.
Conclusão: em parte

## O que mudaria primeiro

1. **Trocar os três instantes para `timestamptz` e o código para `Instant`/`OffsetDateTime`.** É o único item que não segue, e afeta diretamente a regra das 48 horas: hoje ela compara a hora local da JVM com um `paidAt` que perdeu o `Z`.
2. **Passar a criação do esquema para o Flyway,** com uma migração inicial versionada que inclua os índices de `order_items.sku` e `notifications.order_id`. Assim ele evolui com segurança nas duas máquinas e a cópia `schema-h2.sql` deixa de existir.

## Dúvidas

- **B7:** a restrição única está em `order_id`, e não no `transaction_id` que a régua usa como exemplo. Contei como garantia do banco, porque ela impede uma segunda linha de pagamento para o mesmo pedido. Como essa linha é gravada na mesma transação da baixa do estoque e do crédito, a repetição não passa.
- **B8:** considerei `customers.club_tier` repetição sem motivo, porque o nível chega a cada compra e a cópia do cadastro nunca é atualizada nem lida. Não contei `customers.email` como problema, porque é o dado de cadastro do cliente, e a foto da compra está em `orders.customer_email`.
- **B9:** a régua não diz se um `schema.sql` próprio, como mecanismo único, segue ou não. Dei `em parte`: há um mecanismo só, como a documentação pede, mas não é a ferramenta de migração versionada que ela recomenda.
