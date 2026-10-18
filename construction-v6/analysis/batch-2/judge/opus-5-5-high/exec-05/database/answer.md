# banco

## Visão geral

O agente criou o esquema com um `schema.sql` próprio, que o `Database.java` executa ao subir, comando por comando. O script está quebrado. Nas tabelas `notifications` e `payment_tracking`, a ligação com o pedido foi escrita como `REFERENCES orders(id),`, sozinha, como elemento da tabela (`src/main/resources/schema.sql:34` e `:41`), e isso não é SQL válido no PostgreSQL. A execução para no `CREATE TABLE notifications`. Por isso a foto do banco tem só `customers`, `orders` e `products`. Não há tabela de avisos nem de controle de pagamento, e o `INSERT` do catálogo, que vem depois no script, também não roda. O serviço sobe pela metade e cai no `db.initialize()` (`App.java:15`).

No que foi criado há pontos bons: todas as tabelas têm chave primária, o dinheiro está em `numeric(10,2)`, os campos obrigatórios das colunas existentes são `NOT NULL` e a ligação pedido → cliente é uma chave estrangeira de verdade.

Os problemas, do mais grave ao menos grave:
1. O mecanismo de criação deixa o esquema incompleto: faltam duas das cinco tabelas e o catálogo não é carregado.
2. Os itens e o resumo do pedido estão guardados como JSON em colunas `text`. Assim o banco não liga o item ao pedido nem ao produto, e não consegue validar a quantidade.
3. Nenhuma das três regras invariáveis do negócio está garantida no banco.
4. Os instantes estão em `timestamp without time zone`.
5. A única chave estrangeira não tem índice.

## Item a item

**B1, chave primária.** As três tabelas criadas têm chave primária: `customers_pkey (cpf)`, `orders_pkey (id)` e `products_pkey (sku)` (`database/database-schema.json`, as restrições `p`). No script, as duas tabelas que não chegaram a ser criadas também declaram chave primária (`schema.sql:29`, `notifications.id`; `schema.sql:39`, `payment_tracking.order_id`). O `UNIQUE(id)` em `orders` e em `notifications` (`schema.sql:25`, `:35`) repete a chave primária à toa, mas o PostgreSQL o descarta (não há `orders_id_key` na foto).
Conclusão: segue

**B2, as ligações.** Ligação por ligação:
- **Pedido → cliente:** é chave estrangeira, `orders_cpf_fkey`, `FOREIGN KEY (cpf) REFERENCES customers(cpf)` (foto; `schema.sql:17`). O código grava o pedido antes de criar o cliente (`OrderService.java:41`, depois `:43-46`), então essa chave recusa o primeiro pedido de um CPF novo. Isso é comportamento, mas mostra que a chave existe e atua.
- **Item → pedido e item → produto:** nenhuma coluna guarda essas ligações, porque os itens ficam em `orders.items_json` (ver B8).
- **Aviso de pagamento → pedido:** `payment_tracking.order_id` (`schema.sql:39`) não existe no banco. A tabela não foi criada, porque a chave estrangeira foi escrita sem `FOREIGN KEY (order_id)` (`schema.sql:41`), e o código nem a usa (o controle fica num `HashMap`, `OrderService.java:16`).
- **Aviso ao cliente → pedido:** `notifications.order_id` (`schema.sql:31`) tem o mesmo erro de escrita (`schema.sql:34`), e a tabela não existe.
- **Aviso ao cliente → cliente:** `notifications.cpf` (`schema.sql:32`) não tem chave estrangeira nem no script.

Das quatro ligações guardadas em coluna, o banco garante uma. Das outras, duas foram escritas de um jeito que derrubou a criação das tabelas, e uma nem foi pedida.
Conclusão: em parte

**B3, índice nas chaves estrangeiras.** A única chave estrangeira do banco, `orders_cpf_fkey (cpf)`, não tem nenhum índice que comece por `cpf`: o único índice de `orders` é `orders_pkey (id)` (foto, `indexes`). O código consulta exatamente por essa coluna (`WHERE cpf = ?`, `OrderRepository.java:92` e `:109`). No script, `notifications.cpf`, também consultada por CPF (`NotificationRepository.java:303`), não teria índice.
Conclusão: não segue

**B4, dinheiro em tipo exato.** As colunas de dinheiro do banco estão em `numeric(10,2)`: `products.price` e `customers.credit_balance` (foto). Nenhuma está em `real`, `double precision` ou `money`. Os valores do resumo do pedido não têm coluna própria: ficam como texto dentro de `orders.summary_json` (`schema.sql:23`), gerados a partir de campos `Double` (`OrderSummary.java:6-15`). O código também lê e grava o saldo e o preço com `getDouble`/`setDouble` (`CustomerRepository.java:225`, `:253`; `ProductRepository.java:167`). Isso é problema do código, não do tipo da coluna (ver Dúvidas).
Conclusão: segue

**B5, datas e horas com fuso.** `orders.created_at` e `orders.paid_at` são `timestamp without time zone` (foto; `schema.sql:20-21`). São justamente as duas colunas da regra das 48 horas.
Conclusão: não segue

**B6, obrigatórios `NOT NULL`.**
- Nas colunas que existem, os obrigatórios são `NOT NULL`: o CPF e o saldo do cliente, o CPF, o e-mail, a situação e a data de registro do pedido, e todas as colunas do produto (foto, `nullable: false`). `paid_at` e `tracking_code` aceitam nulo, e isso está correto, porque só existem depois.
- No script, `notifications.order_id` aceita nulo (`schema.sql:31`), mas todo aviso do enunciado é de um pedido.
- O SKU e a quantidade do item e os valores do resumo, que o enunciado põe como obrigatórios, estão dentro de JSON (`schema.sql:23-24`). O banco não consegue exigir nenhum deles: só exige que o texto exista.

Conclusão: em parte

**B7, regras invariáveis no banco.**
- **Estoque nunca negativo:** `products.stock` não tem `CHECK` (a foto não tem nenhuma restrição `c`). Só o código cuida disso, com uma leitura (`OrderService.java:196-199`) seguida de um `UPDATE products SET stock = stock - ?` sem condição (`ProductRepository.java:37`).
- **Aviso de pagamento repetido:** o script previa `payment_tracking` com chave primária `order_id` e `UNIQUE(order_id, transaction_id)` (`schema.sql:39-42`), mas a tabela não existe e o código não a usa. A trava é um `HashMap` em memória (`OrderService.java:16`, `:65-71`).
- **Quantidade positiva:** o item está em JSON, então não há `CHECK` possível.

Nenhuma das três regras está no banco.
Conclusão: não segue

**B8, dado repetido.**
- Os itens estão guardados como lista JSON em `orders.items_json` (`schema.sql:24`; `OrderRepository.java:47`, `:136`). Isso esconde do banco uma entidade inteira do domínio e as duas ligações dela.
- O resumo em `orders.summary_json` (`schema.sql:23`) é uma foto do momento pedida pelo negócio, e por isso não é repetição. Mas ficou num único texto, sem tipo nem restrição.
- `orders.email` também é foto da compra (a tabela `customers` nem guarda e-mail).
- `notifications.cpf` (`schema.sql:32`) repete o CPF do pedido ligado. Como o CPF de um pedido não muda, o risco de divergir é pequeno.

O problema que pesa é o item em JSON.
Conclusão: não segue

**B9, criação do esquema.** O mecanismo é um só: um `schema.sql` próprio, sem Flyway nem Liquibase no `pom.xml` e sem versionamento. O `Database.java` o lê e o executa ao subir, partido por `;`, cada comando em autocommit e sem transação (`Database.java:26-37`). Por usar um mecanismo só, segue a recomendação. Mas faltam versão e transação, e o script quebrado mostra o custo disso: o esquema ficou pela metade no banco (três tabelas de cinco, segundo a foto) e o catálogo não foi carregado. Uma ferramenta de migração aplicaria o script numa transação e falharia por inteiro. Além disso, `CREATE TABLE IF NOT EXISTS` não oferece caminho para mudar o esquema depois, e as duas máquinas subindo ao mesmo tempo executariam o mesmo script em paralelo, sem nenhum controle.
Conclusão: não segue

**B10, tamanho dos textos.** Alguns tamanhos têm motivo: `cpf varchar(11)` (o CPF tem 11 números), e `id varchar(36)` é o tamanho de um UUID (`OrderService.java:234`), embora o tipo `uuid` fosse o mais adequado. Outros são arbitrários: `name` e `email` em `varchar(255)`, `status` e `tracking_code` em `varchar(50)` e `sku` em `varchar(20)` (foto). `summary_json` e `items_json` são `text`, mas deveriam ser outra estrutura, e não texto.
Conclusão: em parte

## O que mudaria primeiro

1. **Consertar a criação do esquema e aplicá-la de modo atômico.** Escrever as chaves estrangeiras como `FOREIGN KEY (order_id) REFERENCES orders(id)`, ou direto na coluna. O ideal é trocar o script próprio pelo Flyway, que aplica cada migração numa transação, guarda a versão e coordena as duas máquinas pelo travamento da tabela de histórico. Hoje o banco nem tem as tabelas de avisos e de pagamentos, nem o catálogo.
2. **Criar uma tabela de itens do pedido e deixar as regras no banco.** A tabela `order_items (order_id, sku, quantity)` teria chave estrangeira para `orders` e para `products` e `CHECK (quantity > 0)`. Além disso, `CHECK (stock >= 0)` em `products`, e um `UNIQUE (order_id, transaction_id)` numa tabela de pagamentos que o código realmente use. Com isso, as três regras invariáveis passam a valer também no banco, inclusive com as duas máquinas, que o `HashMap` em memória não cobre.

## Dúvidas

- **B4:** os valores do resumo não estão em `real` nem em `double precision`, e sim em texto JSON gerado a partir de `Double`. A régua aponta como problema só os tipos inexatos da coluna, então considerei que segue pelas colunas existentes e deixei o JSON para o B8. Uma leitura mais rígida daria "em parte".
- **B1, B2, B3, B6 e B10:** a foto não tem `notifications` nem `payment_tracking`. Julguei pelo banco real e usei o script para mostrar o que ele pedia e por que não foi criado. Que o `CREATE TABLE notifications` falha por erro de sintaxe, concluí lendo o script e vendo que a tabela não está na foto. Não rodei o script num PostgreSQL.
