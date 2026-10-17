# banco: PLANTED-01

## Visão geral

O agente escreveu o esquema à mão, num `schema.sql` executado pela inicialização SQL do Spring
(`spring.sql.init.mode=always`), e acessa o banco com `JdbcClient`, sem JPA. O resultado é um esquema
enxuto e bem pensado na maior parte: seis tabelas, todas com chave primária; o item do pedido e o aviso de
pagamento com chaves compostas que fazem sentido; o resumo do pedido guardado como uma foto do momento, em
`numeric(12,2)`; um `CHECK` na quantidade; e o aviso de pagamento repetido barrado pela chave primária
`(order_id, transaction_id)`. Os problemas, em ordem de gravidade: (1) o valor final do pedido, o
`final_total`, é guardado em `double precision`, o único valor em dinheiro num tipo inexato; (2) o
`created_at` do pedido, que é a referência da regra das 48 horas, é `timestamp without time zone`, enquanto o
`paid_at`, com que ele é comparado, tem fuso; (3) o item do pedido guarda o `sku` sem chave estrangeira para
o produto; (4) o "estoque nunca negativo" só é garantido pelo código, sem `CHECK` no banco; (5) a chave
estrangeira `notification.order_id` não tem índice. Menores: o esquema não é versionado (não há Flyway nem
Liquibase) e há quatro colunas em `varchar(255)`.

A foto do banco existe e tem as seis tabelas (`suite/database-schema.json`); ela confere com o
`src/main/resources/schema.sql` em todos os pontos abaixo.

## Item a item

**B1, chave primária.** As seis tabelas têm chave primária (`suite/database-schema.json`, restrições `p`):
`product_pkey (sku)`, `customer_pkey (cpf)`, `customer_order_pkey (id)`, `notification_pkey (id)`, e duas
compostas, `order_item_pkey (order_id, line_no)` e `payment_notice_pkey (order_id, transaction_id)`
(`src/main/resources/schema.sql:2`, `:11`, `:16`, `:44`, `:51`, `:55`). As chaves naturais (SKU, CPF) e a
composta do item são escolhas razoáveis para o domínio. Segue a prática.

**B2, as ligações.** Das ligações do enunciado, o esquema guarda cinco em colunas, e quatro têm chave
estrangeira (`suite/database-schema.json`, restrições `f`):

- pedido → cliente: `customer_order_cpf_fkey`, `customer_order(cpf) → customer(cpf)` (`schema.sql:17`). Segue.
- item → pedido: `order_item_order_id_fkey` (`schema.sql:40`). Segue.
- aviso de pagamento → pedido: `payment_notice_order_id_fkey` (`schema.sql:48`). Segue.
- aviso ao cliente → pedido e → cliente: `notification_order_id_fkey` e `notification_cpf_fkey`
  (`schema.sql:57-58`). Segue.
- **item → produto: falta.** A coluna `order_item.sku` (`schema.sql:42`) aponta para `product.sku`, mas não há
  `FOREIGN KEY`; na foto, a tabela `order_item` só tem a `order_item_order_id_fkey`. O banco aceita um item com
  um SKU que não existe no catálogo, e permite apagar um produto que tem itens vendidos. Hoje quem impede isso
  é só a validação do checkout (`UNKNOWN_PRODUCT`) e o fato de o serviço não apagar produtos. Não segue.

**B3, índice nas chaves estrangeiras.** Das cinco chaves estrangeiras, quatro têm um índice que começa pela
coluna da chave (`suite/database-schema.json`, `indexes`):

- `customer_order.cpf`: `customer_order_cpf_status (cpf, status)` (`schema.sql:37`). Este índice ainda serve à
  consulta da primeira compra (`order/OrderRepository.java:123`).
- `order_item.order_id`: a chave primária `order_item_pkey (order_id, line_no)`.
- `payment_notice.order_id`: a chave primária `payment_notice_pkey (order_id, transaction_id)`.
- `notification.cpf`: `notification_cpf (cpf, seq)` (`schema.sql:63`), que também serve à listagem em ordem
  (`notification/NotificationRepository.java:36`).
- **`notification.order_id`: sem índice.** Nenhum índice da tabela `notification` começa por `order_id`. Hoje
  nenhuma consulta filtra os avisos por pedido, mas cada exclusão ou mudança de chave em `customer_order`
  obriga o banco a varrer `notification` inteira para conferir a chave, que é o caso que a documentação cita.

Não segue em um ponto (`notification_order_id_fkey`).

**B4, dinheiro num tipo exato.** Quase todo o dinheiro está em `numeric(12,2)`: `product.price`,
`customer.credit_balance` e os valores do resumo no pedido (`products_subtotal`, `coupon_discount`,
`shipping_cost`, `insurance`, `payment_adjustment`, `installment_amount`, `next_purchase_credit`). A exceção é
**`customer_order.final_total`, `double precision`** (`schema.sql:30`; na foto, `"type": "double precision"`).
É justamente o valor final, o que o cliente paga, e o enunciado conta que já houve divergência entre o resumo
e o pedido registrado. O código lê a coluna com `getBigDecimal` e arredonda para centavos
(`order/OrderRepository.java:144`, `:167-168`), o que esconde o erro de representação na leitura, mas o valor
guardado no banco é binário e inexato, e qualquer soma ou relatório feito em SQL sobre essa coluna herda o
erro. É também inconsistente com as outras dez colunas de dinheiro. Não segue.

**B5, datas com fuso.** As duas colunas de instante têm tipos diferentes (`suite/database-schema.json`):
`customer_order.paid_at` é `timestamp with time zone` (`schema.sql:23`), segue; **`customer_order.created_at`
é `timestamp without time zone`** (`schema.sql:20`), não segue. Isso pesa porque o `created_at` é a ponta de
partida da regra das 48 horas (`order/OrderService.java:87`) e é devolvido ao site no formato com fuso. O
código grava o instante como `OffsetDateTime` em UTC (`order/OrderRepository.java:45`, `:171-173`) e lê como
`OffsetDateTime` (`:154`), ou seja, ele trata a coluna como se guardasse um instante, mas a coluna não guarda o
fuso. **Fica em dúvida** se isso já dá um valor errado: o PostgreSQL converte o valor com fuso para o horário
local da sessão ao gravar numa coluna sem fuso, e o driver JDBC interpreta o valor sem fuso de um jeito próprio
ao ler; se o fuso da sessão não for UTC, o instante lido pode ficar deslocado, e com ele a regra das 48 horas.
O projeto não fixa o fuso da sessão em lugar nenhum (nenhum `timezone` no `src/` nem no `pom.xml`). Sem rodar,
não dá para afirmar o deslocamento; dá para afirmar que a coluna depende de uma convenção que o banco não guarda.

**B6, NOT NULL no que é obrigatório.** Todas as colunas obrigatórias são `NOT NULL` na foto: o CPF e o e-mail do
pedido, a situação, o `created_at`, os onze campos do resumo, o SKU e a quantidade do item, os campos do
produto (inclusive `stock`), o saldo do cliente e os campos dos dois tipos de aviso. As únicas colunas que
aceitam nulo são as que só existem depois: `customer_order.tracking_code` (até o envio),
`payment_transaction_id` e `paid_at` (até o pagamento) (`schema.sql:21-23`). Segue a prática.

**B7, regras invariáveis no banco.**

- *Estoque nunca negativo:* **só no código.** Não há `CHECK (stock >= 0)` em `product` (a foto não tem nenhuma
  restrição `c` nessa tabela; `schema.sql:6`). A trava está no `InventoryService.take`, que lê o estoque com
  `SELECT ... FOR UPDATE` e só baixa se todos os itens têm saldo (`catalog/InventoryService.java:28-37`,
  `catalog/ProductRepository.java:47-60`). A trava é bem feita, mas a baixa em si é um `UPDATE ... SET stock =
  stock + :delta` sem condição (`catalog/ProductRepository.java:57`): qualquer outro caminho que chame esse
  método sem a leitura travada deixaria o estoque ficar negativo, e o banco aceitaria. Não segue.
- *O mesmo aviso de pagamento não baixa o estoque nem dá o crédito duas vezes:* **garantido no banco.** A chave
  primária `payment_notice_pkey (order_id, transaction_id)` (`schema.sql:51`) impede dois registros do mesmo
  aviso, e o registro é inserido na mesma transação que baixa o estoque e soma o crédito
  (`order/OrderService.java:74`, `:90-96`; o crédito é síncrono, `customer/ClubCreditListener.java:20-22`). Se
  dois avisos iguais passassem pela checagem do código ao mesmo tempo, a segunda inserção violaria a chave e a
  transação inteira, com a baixa e o crédito, seria desfeita. Segue. (Um aviso com *outro* `transactionId` num
  pedido já pago é barrado só pela checagem de situação no código, `order/OrderService.java:85`, mas essa não é
  a regra do item.)
- *Quantidade do item positiva:* **garantido no banco**, `order_item_quantity_check`, `CHECK ((quantity > 0))`
  (`schema.sql:43`). Segue.

Duas de três regras estão no banco; falta a do estoque.

**B8, dado repetido.** O pedido guarda os onze valores do resumo e o e-mail da compra
(`schema.sql:18`, `:24-34`): é a foto do momento que o enunciado pede ("calculado quando o pedido foi
registrado e não muda mais"), e o e-mail não se repete em `customer`, que só tem CPF e saldo. Não há lista
guardada como texto ou JSON: itens, avisos de pagamento e avisos ao cliente estão em tabelas próprias. Dois
pontos de repetição, ambos leves:

- `customer_order.payment_transaction_id` (`schema.sql:22`) repete o `transaction_id` do aviso aceito em
  `payment_notice`. É gravado (`order/OrderRepository.java:87`) e nunca lido; são duas fontes para o mesmo
  fato. Como não muda depois de gravado, o risco de divergir é baixo, mas a coluna é dispensável.
- `notification.cpf` (`schema.sql:57`) é derivável de `notification.order_id → customer_order.cpf`. Aqui há
  motivo: a régua trata o aviso como ligado ao pedido *e* ao cliente, e a coluna serve à listagem por CPF com o
  índice `notification_cpf`. Mas o banco não garante que o CPF do aviso é o mesmo do pedido (seria preciso uma
  chave estrangeira composta `(order_id, cpf)`); o código grava os dois do mesmo `Order`, e o CPF do pedido não
  muda.

Segue a prática, com a ressalva da coluna `payment_transaction_id`.

**B9, como o esquema é criado.** Um mecanismo só: a inicialização SQL do Spring Boot, com
`spring.sql.init.mode=always` (`src/main/resources/application.properties:3`), que roda o `schema.sql` e o
`data.sql` a cada subida. O `pom.xml` não tem Flyway, Liquibase nem JPA (só `spring-boot-starter-jdbc`,
`pom.xml:29-32`), então não há `ddl-auto` concorrendo. Os scripts são idempotentes (`CREATE TABLE IF NOT
EXISTS`; o catálogo com `INSERT ... WHERE NOT EXISTS`, `data.sql:2-17`), o que preserva o estoque entre
reinícios. O ponto fraco é que não é uma migração versionada: um `CREATE TABLE IF NOT EXISTS` não altera uma
tabela que já existe, então qualquer mudança futura no esquema (uma coluna nova, um `CHECK` novo) não chega a um
banco já criado sem um script manual. Segue a recomendação de um mecanismo só, mas não usa a ferramenta de
migração que a documentação chama de mais alto nível.

**B10, tamanho dos textos.** A maioria dos tamanhos tem motivo do domínio: `cpf varchar(11)`, os ids
`varchar(36)` (o tamanho de um UUID em texto; o tipo `uuid` seria ainda melhor), `email varchar(320)` (o
máximo de um endereço de e-mail), e os códigos de situação e tipo em `varchar(32)`/`varchar(16)`. O
`sku varchar(64)` é um limite folgado, sem motivo explícito. Ficam em `varchar(255)` arbitrário quatro colunas
(`suite/database-schema.json`): `product.name`, `customer_order.tracking_code`,
`customer_order.payment_transaction_id` e `payment_notice.transaction_id`. Não há `char(n)`. Item menor;
segue em parte.

## O que mudaria primeiro

1. **Trocar `final_total` para `numeric(12,2)` e `created_at` para `timestamp with time zone`.** São as duas
   colunas mais importantes do pedido (o que o cliente paga e o início do prazo de 48 horas) e as duas únicas
   que fogem do padrão que o próprio esquema adota nas colunas vizinhas. Hoje elas só funcionam porque o código
   arredonda na leitura e grava sempre em UTC.
2. **Pôr no banco as garantias que faltam:** `CHECK (stock >= 0)` em `product` e
   `FOREIGN KEY (sku) REFERENCES product (sku)` em `order_item`, mais um índice em `notification (order_id)`.
   O estoque nunca negativo é uma das regras que o enunciado diz que valem sempre, e o `CHECK` protege a regra
   mesmo se um caminho novo do código esquecer a trava.
