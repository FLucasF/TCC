# o banco

## Visão geral

A foto do esquema (`database/database-schema.json`) está vazia: `tables`, `columns`, `constraints` e `indexes` vêm todos como `[]`. O serviço não subiu, e por isso a correção se baseia no código. Não há migração nem `schema.sql`. O esquema sairia do Hibernate, que o gera a partir das entidades JPA (`spring.jpa.hibernate.ddl-auto=create-drop`, `src/main/resources/application.properties:9`). O que digo sobre tipos e restrições é o que essas entidades pedem ao Hibernate, não algo conferido no banco.

**Pontos fortes:**
- As cinco tabelas têm chave primária.
- Preço e saldo usam `BigDecimal` com `precision`/`scale`, o que dá `numeric`.
- Os instantes usam `Instant`, que o Hibernate grava com fuso.
- As colunas obrigatórias estão marcadas com `nullable = false`.

**Problemas, do mais grave ao menos grave:**
1. **O esquema não sobrevive a um reinício.** O modo `create-drop` apaga as tabelas quando o serviço para e as recria quando ele sobe. Isso vai contra o "se o serviço reiniciar, nada pode se perder". Com duas máquinas no mesmo banco, cada uma que sobe recria o esquema da outra.
2. **Nenhuma das regras invariáveis está no banco.** Não há `CHECK` no estoque nem na quantidade, nem restrição única no id da transação. O código também não usa transação nem trava.
3. **Das ligações guardadas em coluna, só uma é chave estrangeira** (item → pedido), e essa coluna está mapeada duas vezes. A única chave estrangeira não tem índice.
4. **O resumo do pedido fica como JSON num `varchar(255)`.** Os valores em dinheiro dele não têm tipo numérico no banco, e na leitura passam por `Double`.

## Item a item

**B1, chave primária.** Sem foto, olhei o `@Id` de cada entidade. Todas as cinco tabelas têm:
- `customers(cpf)`: `src/main/java/com/example/domain/entity/Customer.java:13-15`
- `products(sku)`: `Product.java:13-15`
- `orders(id)`: `Order.java:15-16`
- `order_items(id)`: `OrderItem.java:10-11`
- `notifications(id)`: `Notification.java:12-13`

Nenhuma tabela fica sem chave.
Conclusão: segue

**B2, as ligações.** As ligações do enunciado, uma a uma:
- **O pedido é de um cliente.** O pedido guarda o CPF em `orders.customer_cpf`, como uma `String` comum (`Order.java:18-19`), sem `@ManyToOne` e sem chave estrangeira. Hoje nem daria para criar a chave: o cliente só é criado no pagamento (`CustomerService.java:35-38` chama `getOrCreateCustomer`), não no registro do pedido (`OrderService.java:54-64`).
- **O item é de um pedido.** O `@OneToMany` com `@JoinColumn(name = "order_id")` (`Order.java:37-38`) faz o Hibernate gerar uma chave estrangeira de `order_items.order_id` para `orders`. É a única ligação com chave. Mas a mesma coluna também está mapeada como campo comum, `orderId`, com `nullable = false` (`OrderItem.java:13-14`), e esse campo nunca recebe valor (o construtor em `OrderItem.java:25-29` não o preenche). O mapeamento duplo e o campo sempre nulo deixam essa ligação frágil.
- **O item é de um produto.** `order_items.sku` (`OrderItem.java:16-17`) é uma `String` sem chave estrangeira para `products`.
- **O aviso de pagamento é de um pedido.** Não tem tabela nem coluna de ligação própria. Fica nas colunas `paid_at` e `last_successful_transaction_id` do próprio pedido (`Order.java:31-35`), então não há chave a cobrar aqui.
- **O aviso ao cliente é de um pedido e de um cliente.** `notifications.order_id` e `notifications.customer_cpf` (`Notification.java:15-16`, `22-23`) são `String`s sem chave estrangeira.

Resultado: das cinco ligações guardadas em coluna, uma tem chave estrangeira.
Conclusão: não segue

**B3, índice nas chaves estrangeiras.** A única chave estrangeira gerada é `order_items.order_id`. Não há nenhum `@Index` nem `@Table(indexes = …)` no projeto (o `grep` por `Index` em `src/main/java` não encontra nada). A chave primária de `order_items` é `id`, não `order_id`. Logo, essa chave fica sem índice. Fora deste item: `notifications.customer_cpf`, filtrada em `NotificationRepository.java:11`, também não tem índice.
Conclusão: não segue

**B4, o dinheiro num tipo exato.**
- **Colunas próprias:** `products.price` e `customers.credit_balance` são `BigDecimal` com `precision = 10, scale = 2` (`Product.java:20-21`, `Customer.java:17-18`), o que dá `numeric(10,2)`. Está certo.
- **Valores do resumo:** subtotal, cupom, frete, seguro, ajuste, total final, parcela e crédito não têm coluna. Vão juntos num texto JSON (`Order.java:41-43`, `OrderSummaryConverter.java:17-34`), e a coluna sai `character varying(255)`, porque o conversor mapeia para `String`.
- **A leitura passa por `Double`:** o Jackson lê esse JSON como `Map` (`OrderSummaryConverter.java:47`), e sem configuração os números com casas decimais viram `Double`. Só depois voltam a `BigDecimal` via `toString()` (`:49-58`).

Não há `real`, `double precision` nem `money` em coluna nenhuma, então a regra de "um tipo inexato basta" não se aplica literalmente. Mas os valores em dinheiro do pedido não estão num tipo numérico do banco, e o caminho de leitura passa por ponto flutuante.
Conclusão: em parte

**B5, as datas com fuso.** Os três instantes são `java.time.Instant`:
- `orders.created_at` (`Order.java:28-29`)
- `orders.paid_at` (`Order.java:31-32`)
- `notifications.created_at` (`Notification.java:29-30`)

No Hibernate 6 em diante, o `Instant` é gravado por padrão como `timestamp(6) with time zone` no PostgreSQL. Não há `LocalDateTime` nem `java.util.Date` nas entidades. Sem foto, isso é o que o código pede, não o que o banco mostrou.
Conclusão: segue

**B6, o obrigatório é `NOT NULL`.**
- **Marcados como obrigatórios (`nullable = false`):**
  - no pedido: CPF, e-mail, situação, data de registro e resumo (`Order.java:18-29`, `41-43`);
  - no item: SKU e quantidade (`OrderItem.java:16-20`);
  - no produto: nome, preço, peso, estoque e fora de linha (`Product.java:17-30`);
  - no cliente: o saldo (`Customer.java:17`);
  - no aviso: todos os campos (`Notification.java:15-30`).
- **Ficam opcionais**, e o enunciado permite: `paid_at`, `tracking_code` e `last_successful_transaction_id` (`Order.java:31-35`, `45-46`).
- **Ressalva:** o resumo é `NOT NULL` como um bloco só. Os valores dentro do JSON não têm restrição individual no banco.
Conclusão: segue

**B7, as regras no banco.** Nenhuma das três regras está garantida no banco:
- **Estoque nunca negativo:** não há `CHECK` em `products.stock` (`Product.java:26-27`). A baixa é um ler, somar e gravar (`ProductService.java:22-28`), sem `@Transactional`, sem `@Lock` e sem `@Version` (o `grep` não encontra nenhum). Só o código cuida, e de forma frágil com duas máquinas.
- **Aviso de pagamento repetido:** o id da transação fica em `orders.last_successful_transaction_id` (`Order.java:34-35`), sem restrição única e sem tabela de pagamentos com chave na transação. A proteção é só a comparação em `OrderService.java:81-84` e a situação em `:86-88`.
- **Quantidade positiva:** não há `CHECK` em `order_items.quantity` (`OrderItem.java:19-20`).
Conclusão: não segue

**B8, dado repetido sem motivo.**
- `orders.customer_email` (`Order.java:21-22`) é a foto do momento da compra. Também é o único lugar do e-mail, porque `customers` não guarda e-mail.
- `notifications.customer_cpf` (`Notification.java:15-16`) daria para obter pelo pedido, mas o enunciado liga o aviso ao cliente, e o CPF não muda.
- O resumo em JSON (`Order.java:41-43`) é a foto pedida pelo negócio. É um valor, não uma lista de entidades, então não esconde nenhuma ligação. A questão do tipo já entrou no B4.
- Os itens têm tabela própria e não estão dentro do pedido.

Não achei coluna repetida que não seja foto do momento.
Conclusão: segue

**B9, como o esquema é criado.**
- **Mecanismo:** só o Hibernate, a partir das entidades. O `pom.xml` não tem Flyway nem Liquibase, não há `schema.sql` nem `db/migration`, e o catálogo inicial é gravado em Java (`src/main/java/com/example/infrastructure/DataInitializer.java:19-33`). Há um mecanismo só, mas não é uma migração versionada.
- **O modo:** é `create-drop` (`application.properties:9`). Ele apaga o esquema quando o serviço para e o recria do zero quando sobe, então pedidos, estoque e saldos se perdem a cada reinício. Com duas máquinas, a que sobe derruba as tabelas da outra.
- **O dialeto:** está fixo em `PostgreSQL12Dialect` (`application.properties:8`). Na seção de dúvidas falo da relação disso com o serviço não ter subido.
Conclusão: não segue

**B10, tamanho dos textos.**
- **Tamanhos com motivo:** o CPF com 11 caracteres em `customers`, `orders` e `notifications` (`Customer.java:14`, `Order.java:18`, `Notification.java:15`). O SKU com 10 (`Product.java:14`, `OrderItem.java:16`) é coerente entre as duas tabelas.
- **Tamanho arbitrário:** o e-mail com 100 (`Order.java:21`), menor que os 254 que um e-mail pode ter.
- **Padrão de 255 do JPA:** todos os ids UUID, `products.name`, as situações e tipos dos enums, `tracking_code`, `last_successful_transaction_id` e o próprio resumo em JSON. Pela minha conta, o JSON do exemplo 7 tem uns 237 caracteres, o que deixa pouca folga dentro dos 255.
Conclusão: em parte

## O que mudaria primeiro

1. **Trocar o `ddl-auto=create-drop` por migrações versionadas no Flyway**, com `ddl-auto=validate`, e colocar o catálogo inicial numa migração. É o que hoje impede os dados de durarem, e o que deixa as duas máquinas destruírem o banco uma da outra.
2. **Na mesma migração, colocar as garantias no banco:**
   - `CHECK (stock >= 0)` e `CHECK (quantity > 0)`;
   - restrição única no id da transação;
   - chaves estrangeiras de `orders.customer_cpf`, `order_items.sku`, `notifications.order_id` e `notifications.customer_cpf`, com índice.

   Isso exige criar o cliente já no registro do pedido. Também vale guardar os valores do resumo em colunas `numeric(12,2)` em vez do JSON.

## Dúvidas

- **Sem foto do banco.** Os tipos que cito (o `timestamp with time zone` do `Instant`, o `numeric(10,2)`, o `varchar(255)` do conversor, a chave estrangeira do `@JoinColumn`) são o que o Hibernate costuma gerar a partir dessas entidades, não algo visto no catálogo.
- **Por que o serviço não subiu.** Não sei, e decidir isso não é desta dimensão. Há dois candidatos:
  - o `PostgreSQL12Dialect` (`application.properties:8`), que pode não existir mais no Hibernate do Spring Boot 4;
  - o `order_id` mapeado duas vezes e sem valor em `OrderItem`.
- **B4.** Escolhi `em parte`, e não `não segue`. Não há coluna `real`, `double` ou `money`, mas o dinheiro do resumo fica como texto e é lido via `Double`. Li isso como fora da regra de "um tipo inexato basta", mas também como fora do "tipo exato" que o item pede.
