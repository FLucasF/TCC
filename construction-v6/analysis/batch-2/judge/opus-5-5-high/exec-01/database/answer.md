# banco

## Visão geral

O serviço cria o esquema com SQL próprio, embutido no código e executado quando o serviço sobe (`src/main/java/com/shop/config/Database.java:42-98`, chamado em `src/main/java/com/shop/Main.java:22`). São quatro tabelas: `products`, `customers`, `orders` e `notifications`. O básico está certo. Toda tabela tem chave primária, as ligações guardadas em coluna têm chave estrangeira, o dinheiro está em `numeric(10,2)` e as colunas obrigatórias são `NOT NULL`.

Os problemas, do mais grave para o menos grave:

1. **O instante do pedido não guarda o fuso.** `orders.created_at` é `timestamp without time zone`, e a regra das 48 horas depende dele.
2. **Nenhuma das três regras que valem sempre está garantida no banco.** Não há `CHECK` no estoque, nem na quantidade, nem restrição única no id da transação. A trava contra o aviso de pagamento repetido fica até fora do banco, num `HashMap` na memória do serviço.
3. **Os itens do pedido estão numa coluna JSONB (`orders.items`).** Com isso, o banco não vê as ligações do item com o pedido e com o produto.
4. **Problemas menores:** falta índice na chave estrangeira `notifications.order_id`, o script de criação não tem versões, e os textos usam tamanhos escolhidos sem motivo do domínio.

## Item a item

**B1, chave primária.** As quatro tabelas têm chave primária (`database/database-schema.json`, restrições do tipo `p`):
- `customers_pkey (cpf)`
- `notifications_pkey (id)`
- `orders_pkey (id)`
- `products_pkey (sku)`

No código, elas estão em `Database.java:46`, `:56`, `:63` e `:76`. Nenhuma tabela fica sem chave. Segue a prática.
Conclusão: segue

**B2, as ligações.** Das ligações do enunciado, três estão guardadas em coluna, e as três têm chave estrangeira:
- pedido → cliente: `orders_customer_cpf_fkey`, `FOREIGN KEY (customer_cpf) REFERENCES customers(cpf)`;
- aviso → pedido: `notifications_order_id_fkey`;
- aviso → cliente: `notifications_customer_cpf_fkey`.

As três estão no `database/database-schema.json`; no código, em `Database.java:71`, `:81` e `:82`.

Item → pedido e item → produto não estão em coluna nenhuma: os itens ficam dentro de `orders.items` (`jsonb`, `Database.java:68`). Isso é avaliado no B8, não aqui.

O aviso de pagamento não tem tabela própria. O pedido só guarda o último id da transação, em `orders.last_payment_transaction_id` (`Database.java:70`), que não aponta para outra tabela. Não falta nenhuma chave estrangeira entre as colunas que existem.
Conclusão: segue

**B3, índices nas chaves estrangeiras.** Duas das três chaves estrangeiras têm índice:
- `orders.customer_cpf` tem `idx_orders_customer` (`Database.java:86`);
- `notifications.customer_cpf` tem `idx_notifications_customer` (`Database.java:89`).

`notifications.order_id` (`notifications_order_id_fkey`) não tem nenhum índice, chave primária ou restrição única que comece por ela. A lista `indexes` do `database/database-schema.json` só tem `notifications_pkey (id)` e `idx_notifications_customer (customer_cpf)` nessa tabela.
Conclusão: em parte

**B4, dinheiro em tipo exato.** As colunas de dinheiro são `numeric(10,2)`:
- `products.price` (`Database.java:48`);
- `customers.credit_balance` (`Database.java:58`).

Nenhuma é `real`, `double precision` ou `money`.

Os valores do resumo do pedido ficam dentro de `orders.summary` (`jsonb`, `Database.java:67`). O `CheckoutSummary` usa `BigDecimal` (`src/main/java/com/shop/dto/CheckoutSummary.java:6-15`), e o Jackson grava esses valores como números JSON. O `jsonb` guarda números como `numeric`, então eles também ficam exatos, mas sem tipo nem escala fixa no banco. Segue a prática.
Conclusão: segue

**B5, datas e horas com fuso.** `orders.created_at` é `timestamp without time zone` (`database/database-schema.json`; `Database.java:66`).

O código grava com `Timestamp.from(Instant.now())` (`src/main/java/com/shop/repository/OrderRepository.java:58`) e lê com `getTimestamp(...).toInstant()` (`OrderRepository.java:118`). O valor gravado é a hora local da JVM, e na leitura ele é lido de novo no fuso da JVM que leu. Com duas máquinas em fusos diferentes, ou numa troca de horário de verão, o instante muda, e essa coluna alimenta a regra das 48 horas (`src/main/java/com/shop/service/OrderService.java:106-107`).

O `paidAt` do aviso de pagamento nem chega a ser gravado. Como existe uma coluna de instante sem fuso, o item não segue.
Conclusão: não segue

**B6, obrigatórios em `NOT NULL`.** Pela foto, todas as colunas obrigatórias são `nullable: false`:
- `customers.cpf`, `email`, `credit_balance`;
- `orders.customer_cpf`, `status`, `created_at`, `summary`, `items`;
- `products.sku`, `name`, `price`, `weight_kg`, `stock`, `discontinued`;
- todas as colunas de `notifications`.

Só aceitam nulo `orders.tracking_code` e `orders.last_payment_transaction_id`, que só existem depois do envio e do pagamento. Está certo.

O problema está no que fica dentro do JSON. A régua cita como obrigatórios "os valores do resumo" e "a quantidade". Eles ficam dentro de `orders.summary` e `orders.items`, e o `NOT NULL` dessas colunas só garante que o documento existe. Um `{"productsSubtotal": null}` ou um item sem `quantity` seria aceito pelo banco.
Conclusão: em parte

**B7, regras invariáveis no banco.** O esquema não tem nenhuma restrição `c` nem `u` (`database/database-schema.json`, `constraints`). As três regras ficam só no código:

- **Estoque nunca negativo:** não há `CHECK (stock >= 0)` em `products.stock` (`Database.java:50`). A trava é o `WHERE sku = ? AND stock >= ?` do `UPDATE`, em `src/main/java/com/shop/repository/ProductRepository.java:45`, que lança erro quando não atualiza nenhuma linha (`:50-52`). É só código.
- **O mesmo aviso de pagamento não age duas vezes:** não há restrição única com o id da transação. `orders.last_payment_transaction_id` é uma coluna comum, sem restrição (`Database.java:70`). A resposta repetida vem de um `HashMap` na memória do processo (`OrderService.java:31`, `:84-92`), que se perde quando o serviço reinicia e não é compartilhado entre as duas máquinas. A baixa dupla é evitada pelo `SELECT ... FOR UPDATE` seguido da checagem da situação (`OrderService.java:97`, `:102`), que também é código.
- **Quantidade positiva:** a quantidade está dentro de `orders.items` (JSONB). O banco não tem como aplicar um `CHECK` a ela.
Conclusão: não segue

**B8, dado repetido sem motivo.** Os itens do pedido são guardados como uma lista JSON numa coluna (`orders.items jsonb`, `Database.java:68`, gravada em `OrderRepository.java:60`). A régua conta isso aqui: a ligação item → pedido e item → produto fica escondida do banco. Por isso não há chave estrangeira para `products`, nem `CHECK` na quantidade, nem consulta por produto.

O resto está bem:
- `orders.summary` é a foto do resumo no momento do registro, como o enunciado pede. Não é repetição.
- O e-mail fica só em `customers` (`Database.java:57`), não é copiado em outra tabela.
- O preço do produto não é copiado em outra tabela.
- `notifications.customer_cpf` poderia ser deduzido de `order_id` → `orders.customer_cpf`. Mas o cliente de um pedido não muda, e a régua trata o aviso como sendo de um pedido e de um cliente, então as duas versões não têm como divergir.

O único problema é a lista em JSON.
Conclusão: em parte

**B9, como o esquema é criado.** Não há Flyway nem Liquibase no `pom.xml`, nem `schema.sql` ou pasta `db/migration`. O esquema sai de uma lista de `CREATE TABLE IF NOT EXISTS` / `CREATE INDEX IF NOT EXISTS` dentro de `Database.createTables` (`Database.java:42-98`), executada a cada subida (`Main.java:22`). O catálogo inicial é carregado por um "conta e insere" (`Database.java:101-123`).

O ponto bom é que existe um mecanismo só. Os problemas são três:
- Não há versões. Com `IF NOT EXISTS`, uma coluna nova num banco que já existe nunca é aplicada.
- O enunciado diz que duas máquinas sobem contra o mesmo banco. Nesse caso, o `COUNT(*)` seguido do `INSERT` pode correr nas duas ao mesmo tempo, e uma delas falha com chave duplicada em `products_pkey`.
- Não há transação nem trava em volta da criação. O SQL é executado com autocommit.
Conclusão: em parte

**B10, tamanho dos textos.** Só `cpf varchar(11)` (`Database.java:56`, `:64`, `:79`) tem um tamanho com motivo do domínio.

Os outros tamanhos são escolhidos sem motivo:
- `sku varchar(50)` (`:46`);
- `name varchar(200)` (`:47`);
- `email varchar(255)` (`:57`);
- `status`/`type varchar(50)` (`:65`, `:77`, `:80`);
- `tracking_code`/`last_payment_transaction_id varchar(100)` (`:69-70`).

Não há `char(n)`, e não é o 255 em todas, mas nenhuma usa `text`.
Conclusão: em parte

## O que mudaria primeiro

1. **Trocar `orders.created_at` para `timestamptz` e guardar o `paidAt`, também como `timestamptz`.** É a coluna da regra das 48 horas. Hoje o instante depende do fuso de cada JVM, o que é um risco concreto com duas máquinas.
2. **Levar os itens para uma tabela `order_items`.** A chave primária seria `(order_id, sku)`, com chaves estrangeiras para `orders` e `products` e `CHECK (quantity > 0)`. Junto, criar `CHECK (stock >= 0)` em `products` e uma tabela de avisos de pagamento com `UNIQUE (order_id, transaction_id)`. Assim as três regras invariáveis passam a ser garantidas pelo banco, e a idempotência sai do `HashMap` em memória, que não funciona com duas máquinas nem depois de reiniciar.

## Dúvidas

- **B6:** todas as colunas obrigatórias são `NOT NULL`, e só os valores dentro do JSON (resumo e quantidade) ficam sem garantia. Marquei `em parte` porque a régua cita esses valores como obrigatórios. Quem contar só as colunas daria `segue`.
- **B8:** marquei `em parte`, e não `não segue`, porque o único problema é a lista de itens em JSONB. Não há dado repetido que muda (preço atual, e-mail) entre tabelas.
- **B4:** considerei o resumo em `jsonb` exato, porque o PostgreSQL guarda números de `jsonb` como `numeric`. Ainda assim, eles ficam sem escala fixa no banco.
- **A foto:** a régua cita `suite/database-schema.json`, mas a pasta de trabalho tem `database/database-schema.json`. Usei esse arquivo. Ele tem as quatro tabelas, então o serviço subiu.
