Já li tudo o que o item pede: a foto do esquema, o `Database.java` que cria as tabelas e os repositórios que gravam nelas. Segue a correção.

# Banco

## Visão geral

O esquema é criado à mão, por um script próprio com `CREATE TABLE IF NOT EXISTS` que roda quando o serviço sobe (`src/main/java/com/loja/infrastructure/Database.java:23-100`). São seis tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`, o obrigatório é `NOT NULL` e os itens ficam numa tabela própria, sem JSON. Esses são os pontos fortes.

Os problemas, do mais grave para o menos grave:

1. **Quase nenhuma ligação é chave estrangeira.** Das seis ligações do domínio, só `order_items.order_id → orders` tem `FOREIGN KEY`. O pedido não se liga ao cliente, o item não se liga ao produto, e os avisos de pagamento e os avisos ao cliente não se ligam a nada.
2. **Os instantes estão em `timestamp without time zone`.** É o caso de `orders.created_at` e `orders.paid_at`, e a regra das 48 horas depende desses dois valores.
3. **Nenhuma regra invariável tem `CHECK`.** A única que tem uma chave no banco é o aviso de pagamento repetido, com a chave `(order_id, transaction_id)`. Mas essa linha só é gravada depois que o estoque e o crédito já mudaram.
4. **O mecanismo que cria o esquema não é versionado**, e os textos têm tamanhos arbitrários (50 e 255).

## Item a item

**B1, chave primária.** Todas as seis tabelas têm chave primária (`database/database-schema.json`, as restrições `p`):

- `customers_pkey (cpf)`
- `notifications_pkey (id)`
- `order_items_pkey (order_id, sku)`
- `orders_pkey (id)`
- `payment_transactions_pkey (order_id, transaction_id)`
- `products_pkey (sku)`

A do item é composta pelo pedido e pelo SKU (`Database.java:74`), o que bate com a regra "o mesmo SKU duas vezes no carrinho é inválido". Segue a prática.
Conclusão: segue

**B2, as ligações.** As seis ligações do enunciado estão todas guardadas em colunas, mas só uma virou chave estrangeira: `order_items_order_id_fkey`, `FOREIGN KEY (order_id) REFERENCES orders(id)` (`Database.java:75`). As outras cinco não têm `FOREIGN KEY` (a foto não traz nenhuma outra restrição `f`):

- o pedido → o cliente: `orders.customer_cpf` (`Database.java:48`);
- o item → o produto: `order_items.sku` (`Database.java:72`);
- o aviso de pagamento → o pedido: `payment_transactions.order_id` (`Database.java:91`);
- o aviso ao cliente → o pedido: `notifications.order_id` (`Database.java:84`);
- o aviso ao cliente → o cliente: `notifications.customer_cpf` (`Database.java:82`).

Nada no código impede essas chaves. O cliente é gravado antes do pedido (`src/main/java/com/loja/services/OrderService.java:39-42`), e o catálogo é carregado ao subir (`Main.java:37`). Só uma das seis ligações é protegida pelo banco.
Conclusão: não segue

**B3, índice nas chaves estrangeiras.** A única chave estrangeira, `order_items.order_id`, é a primeira coluna da chave primária `order_items_pkey (order_id, sku)`, e o índice `order_items_pkey` está na foto. Então ela tem índice. Duas observações, que não mudam a conclusão porque essas colunas não são chaves estrangeiras:

- `orders.customer_cpf` e `notifications.customer_cpf` não têm índice nenhum. Mesmo assim, o código busca por elas: na regra do WELCOME10 (`OrderRepositoryImpl.java:173-177`) e na lista de avisos (`NotificationRepositoryImpl.java:220`).
- `payment_transactions.order_id` já está coberta pela chave primária dela.

Conclusão: segue

**B4, dinheiro em tipo exato.** Todas as colunas de dinheiro estão em `numeric(10,2)`:

- `products.price` e `customers.credit_balance`;
- em `orders`: `products_subtotal`, `coupon_discount`, `shipping_cost`, `insurance`, `payment_adjustment`, `final_total`, `installment_amount` e `next_purchase_credit`.

Não há `real`, `double precision` nem `money`. O peso (`weight_kg numeric(10,3)`) também é exato.

Uma ressalva fora do esquema: o código lê e grava esses valores como `double`, com `setDouble` e `getDouble` (`OrderRepositoryImpl.java:45-54` e `:105-114`, `CustomerRepositoryImpl.java:154,168`, `ProductRepositoryImpl.java:33,94`). O valor passa por `BigDecimal.valueOf(double)` (`Money.java:16-17`), que com 2 casas costuma acertar. Mesmo assim, o caminho exato seria `setBigDecimal`. O banco guarda certo.
Conclusão: segue

**B5, datas e horas com fuso.** `orders.created_at` e `orders.paid_at` são `timestamp without time zone` (na foto; `Database.java:51,54`, `TIMESTAMP`).

O código grava e lê com `Timestamp.from(Instant)` e `getTimestamp(...).toInstant()` (`OrderRepositoryImpl.java:44,128,158`). Isso converte pelo fuso padrão da JVM, e o código não fixa esse fuso em lugar nenhum (o grep por `TimeZone` e `user.timezone` não acha nada). O enunciado diz que o serviço roda em duas máquinas. Se os fusos delas forem diferentes, o mesmo `created_at` volta como instantes diferentes, e isso afeta a regra das 48 horas (`Order.java:101-103`).

Duas colunas de instante sem fuso. A régua diz que uma já basta.
Conclusão: não segue

**B6, o obrigatório é `NOT NULL`.** Na foto, são `nullable: false`:

- o CPF (`customers.cpf`, `orders.customer_cpf`, `notifications.customer_cpf`), o e-mail (`orders.customer_email`), a situação (`orders.status`) e a data de registro (`orders.created_at`);
- os onze campos do resumo em `orders`;
- em `products`: o SKU, o nome, o preço, o peso, o estoque e `discontinued`;
- a quantidade (`order_items.quantity`) e o saldo (`customers.credit_balance`, com padrão `0`);
- em `notifications`: `type`, `order_id` e `status`.

Só aceitam nulo `orders.tracking_code`, `orders.payment_transaction_id` e `orders.paid_at`. As três só existem depois do envio ou do pagamento. Segue a prática.
Conclusão: segue

**B7, as regras invariáveis no banco.**

- **O estoque nunca fica negativo:** `products.stock` é `integer` sem `CHECK` (não há nenhuma restrição `c` na foto). Só o código cuida disso (`Product.java:48-52`, `StockObserver.java:20-31`). O código ainda grava o estoque como um valor absoluto calculado em Java, `UPDATE products SET stock = ?` (`ProductRepositoryImpl.java:48`). Por isso, a falta de uma trava no banco pesa mais aqui.
- **A quantidade do item é positiva:** `order_items.quantity` é `integer` sem `CHECK`. Só a validação do código cuida disso.
- **O aviso de pagamento repetido não age duas vezes:** existe a chave primária `payment_transactions_pkey (order_id, transaction_id)`, que é o tipo de garantia que a régua aceita. Mas a linha só é inserida depois que o estoque baixou e o crédito entrou (`OrderService.java:84-88`). Cada passo usa uma conexão própria, sem uma transação em volta. Com dois avisos iguais ao mesmo tempo, os dois passam pela consulta `hasPaymentTransaction` (`OrderService.java:65`), os dois aplicam os efeitos, e a chave só barra a segunda inserção quando os efeitos já aconteceram. A chave grava a transação, mas não protege os efeitos.

Das três regras, uma tem estrutura no banco, e essa estrutura não cobre o efeito que a regra protege. As outras duas ficam só no código.
Conclusão: em parte

**B8, dado repetido entre tabelas.** Não há lista escondida em texto ou JSON: os itens estão em `order_items`. O que o pedido guarda é a foto do momento:

- os onze valores do resumo (`orders.products_subtotal` … `gift`), que o enunciado pede que não mudem;
- `orders.customer_email`, o dado do cliente naquela compra (a tabela `customers` nem guarda e-mail, então não há duas versões).

Duas repetições pequenas:

- `orders.payment_transaction_id` repete o id da transação que também está em `payment_transactions`. As duas gravações são separadas e sem transação (`OrderService.java:87-88`), então podem divergir se a segunda falhar.
- `notifications.customer_cpf` pode ser deduzido de `notifications.order_id → orders.customer_cpf`. Mas a régua trata o aviso como sendo "de um pedido e de um cliente", e a coluna é a chave da busca (`NotificationRepositoryImpl.java:220`). Por isso não a conto como repetição sem motivo.

Nada que crie duas versões de um dado que muda.
Conclusão: segue

**B9, como o esquema é criado.** Um script próprio em Java: `Database.initialize()` com `CREATE TABLE IF NOT EXISTS` (`Database.java:23-100`), chamado em `Main.java:34`. O `pom.xml` não tem Flyway nem Liquibase, e o projeto não tem `src/main/resources` (nem `schema.sql`, nem `db/migration`). Então há um mecanismo só, o que é bom.

Mas o mecanismo não é versionado. Com `IF NOT EXISTS`, uma coluna ou restrição nova nunca chega a um banco que já tem a tabela, e não há histórico do que foi aplicado.

Além disso, as duas máquinas sobem contra o mesmo banco sem nenhuma trava. O `CREATE TABLE IF NOT EXISTS` em paralelo pode falhar no PostgreSQL. A carga do catálogo também pode: ela conta as linhas e depois insere (`ProductRepositoryImpl.java:62-99`), então as duas máquinas podem inserir juntas, uma delas bate na `products_pkey` e cai ao subir. Uma ferramenta de migração resolveria as duas coisas.
Conclusão: em parte

**B10, tamanho dos textos.** Só o CPF tem um tamanho com motivo no domínio: `character varying(11)` em `customers.cpf`, `orders.customer_cpf` e `notifications.customer_cpf`. Os outros tamanhos são arbitrários:

- `character varying(50)` em `products.sku`, `orders.id`, `orders.status`, `order_items.order_id`, `order_items.sku`, `notifications.type`, `notifications.order_id`, `notifications.status` e `payment_transactions.order_id`;
- `character varying(255)` em `products.name`, `orders.customer_email`, `orders.tracking_code`, `orders.payment_transaction_id` e `payment_transactions.transaction_id`.

O id do pedido é um UUID (`OrderService.java:44`) guardado em `varchar(50)`, quando poderia ser o tipo `uuid` ou `text`. Não há `char(n)`.
Conclusão: em parte

## O que mudaria primeiro

1. **Criar as cinco chaves estrangeiras que faltam e envolver a confirmação do pagamento numa transação.** As chaves são `orders.customer_cpf → customers`, `order_items.sku → products`, `payment_transactions.order_id → orders`, `notifications.order_id → orders` e `notifications.customer_cpf → customers`, com índices em `orders.customer_cpf` e `notifications.customer_cpf`. Na transação, a linha de `payment_transactions` deve ser inserida primeiro, para que a chave primária barre o aviso repetido antes que ele baixe o estoque, junto com `CHECK (stock >= 0)` e `CHECK (quantity > 0)`. É isso que faz o banco proteger as ligações e as três regras que o enunciado diz que valem sempre.
2. **Trocar `created_at` e `paid_at` para `timestamptz` e passar o esquema para o Flyway.** Os instantes deixam de depender do fuso de cada máquina. O Flyway dá versões e trava a migração quando as duas máquinas sobem juntas.

## Dúvidas

- **No B7**, a régua aceita "uma chave primária ou única com o id da transação" como garantia, e o esquema tem essa chave. Só que ela é gravada depois dos efeitos e fora de uma transação com eles, então na prática não impede a dupla baixa. Contei a regra como parcialmente protegida, o que deu `em parte` no item. Se a leitura for que uma chave que não cobre os efeitos não conta, o item seria `não segue`.
- **No B9**, a régua não diz qual conclusão dar a um script próprio usado como mecanismo único. Dei `em parte`: segue o "um mecanismo só", mas não é a ferramenta versionada que a documentação recomenda.
