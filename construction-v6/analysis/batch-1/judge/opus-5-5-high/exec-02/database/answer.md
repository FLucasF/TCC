# banco

## Visão geral

O agente deixou o Hibernate gerar o esquema a partir de quatro entidades JPA (`customers`, `orders`, `products`, `notifications`). O que é coluna de verdade está bem feito. As quatro tabelas têm chave primária, o dinheiro está em `numeric`, os instantes em `timestamp with time zone`, e só aceitam nulo as colunas que só passam a existir depois (rastreio, transação, data do pagamento).

Os problemas, do mais grave para o menos grave:

1. **O esquema é recriado do zero a cada subida.** `ddl-auto: create-drop` apaga e recria as tabelas toda vez que o serviço sobe e as apaga quando ele para. Com isso, um reinício perde todos os pedidos, e nas duas máquinas de produção uma derruba os dados da outra quando sobe.
2. **Não existe nenhuma chave estrangeira.** O item do pedido não tem tabela: os itens e o resumo ficam em texto JSON dentro de `orders`.
3. **As três regras invariáveis do negócio não têm proteção nenhuma no banco.** Não há `CHECK` no estoque, não há restrição única na transação e a quantidade do item fica dentro do JSON.
4. Item menor: os textos seguem o `varchar(255)` padrão do JPA, com exceção do CPF.

A foto do esquema existe e tem tabelas; por isso, o que vem abaixo se baseia nela.

## Item a item

**B1, chave primária.** As quatro tabelas têm chave primária (`database/database-schema.json`, restrições `p`): `customers_pkey (cpf)`, `orders_pkey (id)`, `products_pkey (sku)` e `notifications_pkey (id)`. Como os itens não são tabela (ver B8), não existe tabela de item para avaliar. Ressalva menor: os ids de pedido e de aviso são UUID guardados em `character varying(255)`, em vez do tipo `uuid` (`domain/entity/Order.java:18-19`, `domain/entity/Notification.java:19-20`), mas isso não tira a chave.
Conclusão: segue

**B2, as ligações.** O esquema guarda três ligações em colunas, e nenhuma delas tem `FOREIGN KEY` (a foto só tem restrições `p` e `c`):
- o pedido é de um cliente: `orders.customer_cpf` → `customers.cpf`, sem chave (`domain/entity/Order.java:22`);
- o aviso é de um pedido: `notifications.order_id` → `orders.id`, sem chave (`domain/entity/Notification.java:30`);
- o aviso é de um cliente: `notifications.customer_cpf` → `customers.cpf`, sem chave (`domain/entity/Notification.java:23`).

As entidades usam `String` simples, sem `@ManyToOne`. Além disso, o cliente só é gravado quando o pagamento é confirmado (`service/OrderService.java:137-150`), não quando o pedido é registrado. Enquanto o pedido aguarda o pagamento, e no aviso de cancelamento de um pedido não pago (`service/OrderService.java:116`, `:254`), o `customer_cpf` aponta para uma linha que ainda não existe. Ou seja, o desenho nem aceitaria a chave estrangeira do jeito que está.

As ligações item → pedido e item → produto não estão em coluna nenhuma, porque ficam no JSON; isso é o B8. O aviso de pagamento também não tem tabela própria: o pedido guarda só `transaction_id` e `paid_at` na própria linha, então não há ligação a avaliar.
Conclusão: não segue

**B3, índice nas chaves estrangeiras.** Não existe nenhuma chave estrangeira (B2), então não há o que conferir. Mesmo assim, as colunas que fazem as ligações também não têm índice: a foto só tem os quatro índices das chaves primárias. Isso pesa em `notifications.customer_cpf`, que o código consulta por CPF para listar os avisos (`repository/NotificationRepository.java:10`).
Conclusão: não se aplica

**B4, dinheiro em tipo exato.** As colunas de dinheiro são `products.price` e `customers.credit_balance`, ambas `numeric(38,2)` na foto. Não há `real`, `double precision` nem `money`. A precisão 38 é o padrão do Hibernate para `BigDecimal`, não uma escolha do agente, mas o tipo é exato. Os valores do resumo (subtotal, frete, total, crédito etc.) não estão em coluna numérica: ficam dentro de `orders.summary_json` (`text`), escritos como números decimais em JSON. Não são um tipo inexato, mas o banco também não sabe que aquilo é dinheiro (ver B8).
Conclusão: segue

**B5, datas com fuso.** As três colunas de instante, `orders.created_at`, `orders.paid_at` e `notifications.created_at`, são `timestamp(6) with time zone` na foto, mapeadas de `Instant` (`domain/entity/Order.java:32`, `:44`; `domain/entity/Notification.java:37`). Não há nenhuma coluna `timestamp without time zone`.
Conclusão: segue

**B6, obrigatórios `NOT NULL`.**
- Todas as colunas reais que são obrigatórias estão `nullable: false` na foto: CPF, e-mail, situação, `created_at`, SKU, nome, preço, peso, estoque, fora de linha, saldo e o tipo e a situação do aviso.
- Aceitam nulo só `tracking_code`, `transaction_id` e `paid_at`, que só existem depois do envio ou do pagamento. Isso está correto.

O problema é o que não virou coluna. Os valores do resumo e o SKU e a quantidade de cada item ficam dentro de `summary_json` e `items_json` (`domain/entity/Order.java:35`, `:38`). O `NOT NULL` dessas colunas garante só que o texto existe, não que o `finalTotal` ou a `quantity` existam lá dentro. Uma parte do que a régua lista como obrigatório fica, portanto, sem essa garantia.
Conclusão: em parte

**B7, regras invariáveis no banco.**
- **Estoque nunca negativo:** `products.stock` é `bigint` sem `CHECK (stock >= 0)`. As únicas restrições `c` da foto são as que o Hibernate gera para os enums (`orders_status_check`, `notifications_status_check`, `notifications_type_check`). A trava existe só no código, que lê e depois grava (`service/OrderService.java:113`, `:133`), e não há `@Lock`, `@Version` nem consulta condicional.
- **Aviso de pagamento repetido:** `orders.transaction_id` não tem restrição única e não existe tabela de pagamentos com chave em `(order_id, transaction_id)`. Quem cuida disso é só a comparação no código (`service/OrderService.java:71-75`).
- **Quantidade positiva:** a quantidade vive em `items_json` (`text`), então não há como o banco pôr um `CHECK` nela. Só a validação do serviço protege essa regra.

Nenhuma das três regras é garantida pelo banco.
Conclusão: não segue

**B8, dado repetido / lista escondida.**
- **Os itens:** `orders.items_json` (`text`, `domain/entity/Order.java:38`; gravado em `service/OrderService.java:330-344`) guarda a lista inteira de itens como JSON. É exatamente o caso que a régua manda contar aqui. A ligação do item com o pedido e com o produto some do banco: não há chave, não há índice e não dá para perguntar ao banco quais pedidos têm um SKU.
- **O resumo:** `orders.summary_json` (`domain/entity/Order.java:35`) é a foto do momento que o enunciado pede, então não é repetição sem motivo. Mas é uma foto opaca: um bloco de texto, sem tipo, sem `NOT NULL` por campo e sem consulta possível.
- **O e-mail do pedido:** `orders.customer_email` é dado da compra, e `customers` nem tem e-mail, então não há duas versões que possam divergir.
- **O CPF do aviso:** `notifications.customer_cpf` repete o CPF do pedido. Como o CPF de um pedido não muda, as duas versões não divergem.

O problema real é a lista em JSON.
Conclusão: não segue

**B9, criação do esquema.** Um mecanismo só: o Hibernate a partir das entidades, com `spring.jpa.hibernate.ddl-auto: create-drop` (`src/main/resources/application.yml`). Não há Flyway nem Liquibase no `pom.xml`, nem `schema.sql` ou `db/migration`. A carga do catálogo é feita por um `CommandLineRunner` (`config/DataInitializer.java:58-129`).

Não ter dois mecanismos ao mesmo tempo está de acordo com a documentação. O problema é o modo escolhido:
- `create-drop` apaga e recria o esquema cada vez que o serviço sobe e o apaga quando ele para. Todo reinício perde pedidos, estoque e saldos, o que contraria diretamente "se o serviço reiniciar, nada pode se perder".
- Com as duas máquinas no mesmo banco, a subida de uma derruba as tabelas que a outra está usando.
- O `count() > 0` do `DataInitializer` mostra que o agente esperava dados persistentes, mas com esse modo a condição nunca chega a valer depois de uma reinicialização.
- Não há versionamento: nenhuma mudança de esquema pode ser migrada sem perder os dados.
Conclusão: não segue

**B10, tamanho dos textos.**
- O CPF tem tamanho escolhido por motivo do domínio, `character varying(11)` nas três tabelas (`domain/entity/Customer.java:18`, `Order.java:22`, `Notification.java:23`). É o que a régua pede.
- O SKU tem `varchar(20)` (`domain/entity/Product.java:18`), um tamanho escolhido mas sem regra do domínio por trás.
- Todo o resto está no `character varying(255)` padrão do JPA: `orders.id`, `notifications.id`, `notifications.order_id`, `status` e `type`, `customer_email`, `products.name`, `tracking_code` e `transaction_id`. Os ids deveriam ser `uuid`, e os demais, `text`.
- Não há `char(n)`. Os dois JSON são `text`.
Conclusão: em parte

## O que mudaria primeiro

1. **Trocar `ddl-auto: create-drop` por migrações versionadas do Flyway, com `ddl-auto: validate`.** É o único ponto em que o banco, do jeito que está, faz o serviço perder todos os dados, a cada reinício e a cada subida da segunda máquina. Ele vem antes de qualquer outra prática.
2. **Criar a tabela `order_items` e garantir as regras no banco.** A tabela teria `order_id` com FK para `orders`, `sku` com FK para `products`, `quantity` com `CHECK (quantity > 0)`, o preço unitário da compra e chave `(order_id, sku)`. Junto com ela:
   - as chaves estrangeiras de `orders.customer_cpf` e `notifications.order_id`, com índice, gravando o cliente já no registro do pedido;
   - `CHECK (stock >= 0)` em `products`;
   - uma restrição única em `(order_id, transaction_id)`.

   Isso dá ao banco as ligações e as três regras invariáveis que hoje só o código protege.

## Dúvidas

- **B3:** sem nenhuma chave estrangeira, li o item ao pé da letra e marquei `não se aplica`. A falta de índice nas colunas de ligação ficou registrada no texto, mas o peso dela está no B2.
- **B6:** contei os campos obrigatórios que estão dentro de `summary_json` e `items_json` como obrigatórios sem `NOT NULL`, e por isso dei `em parte`, mesmo com todas as colunas reais corretas. Uma leitura que considere só as colunas daria `segue`. Assim, o B6 em parte repete o problema que o B8 conta.
- **O caminho da foto:** a régua cita `suite/database-schema.json`, mas a pasta de trabalho só tem `database/database-schema.json`. Usei esse arquivo.
