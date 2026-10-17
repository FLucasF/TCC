# banco

## Visão geral

A foto do banco (`database/database-schema.json`) está vazia: `tables`, `columns`, `constraints` e `indexes` vêm sem nada. Isso quer dizer que o serviço não subiu, e por isso esta correção sai do código: das entidades JPA e da configuração. O motivo provável aparece no próprio mapeamento. `Order` tem o campo `installments` (`src/main/java/com/loja/orderservice/entity/Order.java:35`) e também embute o `OrderSummary`, que tem outro `installments` (`src/main/java/com/loja/orderservice/entity/OrderSummary.java:15`). Nenhum dos dois tem `@Column`, então os dois caem na mesma coluna `installments` da tabela `orders`, e o Hibernate costuma recusar isso ao subir ("column is duplicated in mapping"). Não rodei o serviço, então não confirmei. Nenhum teste sobe o contexto do Spring (não há `@SpringBootTest` em `src/test`), e o erro passou despercebido.

Sobre o desenho: o esquema sai do Hibernate. Os acertos são os tipos: o dinheiro é `BigDecimal`, que vira `numeric`, e os instantes são `Instant`, que vira `timestamp with time zone`. Os problemas, do mais grave para o menos grave:

1. O esquema é criado com `ddl-auto=create`, que apaga e recria as tabelas a cada subida. Isso perde os dados quando o serviço reinicia, e com duas máquinas uma derruba as tabelas da outra.
2. Das cinco ligações do domínio, só item → pedido tem chave estrangeira.
3. Nenhuma regra invariável está no banco.
4. Quase nada é `NOT NULL`.
5. Todo texto é `varchar(255)`.

## Item a item

**B1, chave primária.** Pelo código, as cinco tabelas têm chave primária:
- `customers(cpf)` (`entity/Customer.java:12-13`);
- `products(sku)` (`entity/Product.java:11-12`);
- `orders(id)`, UUID gerado (`entity/Order.java:12-14`);
- `order_items(id)`, identity (`entity/OrderItem.java:8-10`);
- `customer_notifications(id)` (`entity/CustomerNotification.java:9-11`).

Não há foto para confirmar o que o banco criou.
Conclusão: segue

**B2, as ligações.** As cinco ligações do enunciado estão todas guardadas em coluna. Só uma delas ganha chave estrangeira:
- **Item → pedido:** o `@OneToMany @JoinColumn(name = "order_id")` (`entity/Order.java:46-48`) faz o Hibernate gerar uma chave estrangeira de `order_items.order_id` para `orders`. A entidade do item também mapeia a mesma coluna como texto solto (`entity/OrderItem.java:12-13`).
- **Pedido → cliente:** `orders.customer_cpf` é um `String` comum, sem relacionamento (`entity/Order.java:16-17`). Falta a chave.
- **Item → produto:** `order_items.sku` é um `String` comum (`entity/OrderItem.java:15`). Falta a chave.
- **Aviso → pedido:** `customer_notifications.order_id` (`entity/CustomerNotification.java:19`). Falta a chave.
- **Aviso → cliente:** `customer_notifications.customer_cpf` (`entity/CustomerNotification.java:13-14`). Falta a chave.

O modelo nem aguentaria a chave para o cliente. O cliente só é gravado quando o pagamento é confirmado (`service/OrderService.java:226-233`), e não quando o pedido é registrado. Assim, o pedido e o aviso de cancelamento de um pedido que nunca foi pago apontam para um CPF que não existe em `customers`.
Conclusão: não segue

**B3, índice nas chaves estrangeiras.** A única chave estrangeira, `order_items.order_id`, não tem índice. Nenhuma entidade declara `@Table(indexes = ...)` ou `@Index`, e não há script de esquema. As colunas que fariam papel de chave (`orders.customer_cpf` e `customer_notifications.customer_cpf`) também não têm índice, e o código consulta por elas (`repository/OrderRepository.java`, `findCompletedOrdersByCpf`; `repository/CustomerNotificationRepository.java`, `findByCpf`).
Conclusão: não segue

**B4, dinheiro em tipo exato.** Todo o dinheiro é `BigDecimal`, sem `precision`/`scale`. No PostgreSQL o Hibernate gera `numeric(38,2)` para isso. Os campos são:
- `products.price` (`entity/Product.java:14`);
- `customers.credit_balance` (`entity/Customer.java:15`);
- os sete valores do resumo embutidos em `orders` (`entity/OrderSummary.java:8-17`).

Não há `real`, `double precision` nem `money`. O tipo vem do padrão do Hibernate, porque não há foto.
Conclusão: segue

**B5, instantes com fuso.** Os instantes são todos `java.time.Instant`:
- `orders.created_at` e `orders.paid_at` (`entity/Order.java:54-56`);
- `customers.created_at` (`entity/Customer.java:16`);
- `customer_notifications.created_at` (`entity/CustomerNotification.java:24`).

Desde a versão 6, o Hibernate grava `Instant` no PostgreSQL como `timestamp(6) with time zone`. Também aqui é o padrão do Hibernate, sem foto para conferir.
Conclusão: segue

**B6, obrigatório é `NOT NULL`.** Só as chaves primárias ficam `NOT NULL`. Todos os outros campos são tipos-objeto (`String`, `Integer`, `BigDecimal`, `Boolean`, enums), sem `nullable = false` nem `@NotNull`. Por isso aceitam nulo campos que todo registro tem:
- **`orders`:** `customer_cpf`, `customer_email`, `status`, `created_at`, `zip_code`, `region`, `shipping_method`, `payment_method`, `club_tier` e todos os valores do resumo (`entity/Order.java:16-54`, `entity/OrderSummary.java:8-18`);
- **`order_items`:** `order_id`, `sku` e `quantity` (`entity/OrderItem.java:12-16`);
- **`products`:** `name`, `price`, `weight_kg`, `stock` e `discontinued` (`entity/Product.java:13-17`);
- **`customers`:** `email` e `credit_balance` (`entity/Customer.java:14-15`);
- **`customer_notifications`:** `customer_cpf`, `type`, `order_id`, `status` e `created_at`.

Só `coupon`, `tracking_code`, `last_transaction_id` e `paid_at` deveriam poder ficar nulos.
Conclusão: não segue

**B7, regras invariáveis no banco.** Nenhuma das três regras tem garantia no banco:
- **Estoque nunca negativo:** `stock` é um `Integer` sem `CHECK` (`entity/Product.java:16`). A baixa lê, subtrai e grava sem trava (`service/OrderService.java:204-212`), então só a conferência do código (`service/OrderService.java:191-202`) segura a regra.
- **Aviso de pagamento repetido:** o id da transação fica em `orders.last_transaction_id` (`entity/Order.java:52`), sem restrição única e sem tabela de pagamentos com chave na transação. A idempotência é só a comparação em `service/OrderService.java:105`.
- **Quantidade positiva:** `order_items.quantity` não tem `CHECK` (`entity/OrderItem.java:16`).

Os enums gravados como texto talvez ganhem um `CHECK (... IN (...))` gerado pelo Hibernate. Isso não cobre nenhuma das três regras.
Conclusão: não segue

**B8, dado repetido entre tabelas.** Os itens ficam numa tabela própria, e não como JSON dentro do pedido (`entity/Order.java:46-48`). As repetições que existem têm motivo:
- O resumo embutido em `orders` e o `orders.customer_email` são a foto do momento que o enunciado pede.
- O `customer_notifications.customer_cpf` repete o cliente que já se acha pelo pedido, mas o enunciado põe o aviso ligado ao pedido e ao cliente.

Dentro da própria tabela `orders`, o número de parcelas aparece duas vezes (`entity/Order.java:35` e `entity/OrderSummary.java:15`). As duas cópias caem na mesma coluna, e esse é o provável erro de mapeamento da visão geral. Isso fica dentro de uma tabela só, não entre tabelas; vai nas dúvidas.
Conclusão: segue

**B9, como o esquema é criado.** Há um mecanismo só: o Hibernate gera o esquema a partir das entidades. O `pom.xml` não tem Flyway nem Liquibase, não há `schema.sql` nem `db/migration`, e o catálogo inicial é gravado por código (`service/CatalogService.java:269-283`). O modo, porém, é `spring.jpa.hibernate.ddl-auto=create` (`src/main/resources/application.properties:10`), que apaga e recria todas as tabelas a cada subida. Isso contraria o enunciado ("se o serviço reiniciar, nada pode se perder"). Com as duas máquinas no mesmo banco, a segunda a subir apaga as tabelas que a primeira está usando. Não há migração versionada.
Conclusão: não segue

**B10, tamanho dos textos.** Nenhum `String` tem `@Column(length = ...)` nem `columnDefinition`, então todos ficam com o `varchar(255)` padrão do JPA:
- dados com tamanho conhecido: `cpf` (11), `zip_code` (8) e `sku`;
- textos livres: `email`, `name`, `coupon`, `tracking_code` e `last_transaction_id`;
- os ids em UUID e os enums.

O 255 é arbitrário em todos.
Conclusão: não segue

## O que mudaria primeiro

1. **Trocar o `ddl-auto=create` por migrações versionadas.** Usar Flyway (ou Liquibase) com `ddl-auto=validate`. Esse é o problema mais grave: hoje cada reinício apaga os pedidos, o estoque e os saldos. O script de migração também é o lugar natural para declarar o que hoje falta: as chaves estrangeiras e seus índices, os `NOT NULL`, `CHECK (stock >= 0)`, `CHECK (quantity > 0)`, uma restrição única no id da transação e `text` (ou tamanhos do domínio) no lugar do 255.
2. **Ligar o pedido ao cliente de verdade.** Gravar o cliente quando o pedido é registrado, como pede o enunciado, e ter chaves estrangeiras de pedido, item e aviso para cliente, produto e pedido. Isso põe no banco as ligações que hoje só o código conhece. Junto, resolver o `installments` duplicado em `orders`, que provavelmente impede o serviço de subir.

## Dúvidas

- **Sem foto do banco.** Os tipos citados (`numeric(38,2)`, `timestamp(6) with time zone`, `varchar(255)`, a chave estrangeira de `order_items.order_id`) são os que o Hibernate costuma gerar para estes mapeamentos no PostgreSQL. Não estão confirmados pelo catálogo do banco.
- **Por que o serviço não subiu.** Atribuí a falha à coluna `installments` mapeada duas vezes em `orders`. É a explicação mais provável pela leitura, mas não rodei o serviço.
- **O `installments` duplicado no B8.** O item fala de repetição entre tabelas, e esta fica dentro de uma tabela só, por isso não pesou na conclusão. Ela entra como provável causa da falha na subida.
