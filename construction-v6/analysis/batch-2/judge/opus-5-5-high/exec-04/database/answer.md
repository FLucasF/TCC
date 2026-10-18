# banco

## Visão geral

O agente escreveu o esquema à mão, num método Java que roda `CREATE TABLE IF NOT EXISTS` toda vez que o serviço sobe (`src/main/java/com/clothing/Database.java:37-119`). Não usa ferramenta de migração. São seis tabelas: `products`, `customers`, `orders`, `order_items`, `notifications` e `payment_transactions`.

**Pontos fortes:**
- Toda tabela tem chave primária.
- Todo dinheiro está em `numeric(10,2)`.
- O que é obrigatório está como `NOT NULL`.
- Os itens e os avisos estão em tabelas próprias, não guardados como JSON.
- O resumo do pedido fica gravado como foto do momento em que o pedido foi registrado.

**Problemas, do mais grave ao menos grave:**
1. Todas as sete colunas de data e hora são `timestamp without time zone`. Só isso já faz o B5 não seguir a prática.
2. Faltam duas chaves estrangeiras do domínio: a do pedido para o cliente e a do item para o produto.
3. Das três regras que valem sempre, o banco só protege uma, e pela metade. A restrição única sobre a transação existe, mas o código engole o erro dela e baixa o estoque mesmo assim. Não há `CHECK` nem no estoque nem na quantidade.
4. Duas chaves estrangeiras não têm índice. Uma delas é `order_items.order_id`, lida em toda consulta de pedido.
5. O esquema não é versionado.

## Item a item

**B1, chave primária.** As seis tabelas têm chave primária (`database/database-schema.json`, restrições `p`):
- `customers_pkey (cpf)`
- `products_pkey (sku)`
- `orders_pkey (id)`
- `order_items_pkey (id)`
- `notifications_pkey (id)`
- `payment_transactions_pkey (id)`

O CPF e o SKU são chaves naturais, os demais são `bigserial`. Nenhuma tabela fica sem chave.
Conclusão: segue

**B2, as ligações.** São seis as ligações que o esquema guarda em coluna:

| ligação | coluna | chave estrangeira |
|---|---|---|
| item → pedido | `order_items.order_id` | sim: `order_items_order_id_fkey` |
| aviso de pagamento → pedido | `payment_transactions.order_id` | sim: `payment_transactions_order_id_fkey` |
| aviso ao cliente → pedido | `notifications.order_id` | sim: `notifications_order_id_fkey` |
| aviso ao cliente → cliente | `notifications.customer_cpf` | sim: `notifications_customer_cpf_fkey` |
| pedido → cliente | `orders.customer_cpf` | **não** (`Database.java:60`) |
| item → produto | `order_items.sku` | **não** (`Database.java:87`) |

Nas duas que faltam, a coluna aponta para `customers.cpf` e `products.sku`, mas não há `REFERENCES`. Nada impedia a chave: o cliente é criado antes do pedido (`CheckoutService.java:218-219`), e o produto existe no catálogo semeado. O banco aceitaria um pedido de um CPF que não existe ou um item com SKU inventado.
Conclusão: em parte

**B3, índices nas chaves estrangeiras.**

| chave estrangeira | índice | situação |
|---|---|---|
| `notifications.customer_cpf` | `idx_notifications_cpf` | tem |
| `payment_transactions.order_id` | o índice único `payment_transactions_order_id_key` | tem |
| `order_items.order_id` | nenhum (o único índice da tabela é `order_items_pkey (id)`) | **falta** |
| `notifications.order_id` | nenhum | **falta** |

A falta em `order_items.order_id` pesa: toda leitura de pedido busca os itens por essa coluna (`OrderRepository.java:100`). Já `idx_orders_cpf` indexa `orders.customer_cpf`, que nem é chave estrangeira (B2).
Conclusão: em parte

**B4, tipo do dinheiro.** Todas as colunas de dinheiro são `numeric(10,2)`:
- `products.price`
- `customers.credit_balance`
- em `orders`: `products_subtotal`, `coupon_discount`, `shipping_cost`, `insurance`, `payment_adjustment`, `final_total` e `next_purchase_credit`

Não há `real`, `double precision` nem `money`. O código lê esses valores como `BigDecimal` a partir do texto (`OrderRepository.java:86-95`).
Conclusão: segue

**B5, fuso nas datas e horas.** Todas as colunas de instante são `timestamp without time zone` (`database/database-schema.json`):
- `customers.created_at`
- `notifications.created_at`
- em `orders`: `created_at`, `paid_at`, `shipped_at` e `delivered_at`
- `payment_transactions.paid_at`

O código grava `Timestamp.from(Instant)` (`OrderRepository.java:45, 137, 163`) e lê de volta com `toInstant()` (`:110, :190`). Sem fuso na coluna, o instante fica dependendo do fuso da JVM. Isso importa aqui porque o enunciado põe o serviço em duas máquinas que usam o mesmo banco, e a regra das 48 horas compara `created_at` com o `paidAt` do banco (`CheckoutService.java:223-226`). Os `DEFAULT CURRENT_TIMESTAMP` (`Database.java:54, 99`) também guardam a hora local da sessão, não um instante.
Conclusão: não segue

**B6, NOT NULL.** Tudo o que o enunciado diz que sempre existe está como `NOT NULL`:
- o CPF e o e-mail do cliente, e o saldo (`credit_balance`)
- a situação, a data de registro e os onze valores do resumo em `orders` (`Database.java:60-77`)
- o SKU e a quantidade do item
- o preço, o peso, o estoque e o `discontinued` do produto
- o tipo e a situação do aviso
- o `transaction_id` e o `paid_at` do pagamento

Aceitam nulo apenas `tracking_code`, `paid_at`, `shipped_at` e `delivered_at` em `orders`, que só existem mais tarde no ciclo do pedido.
Conclusão: segue

**B7, regras invariáveis no banco.**
- **Estoque nunca negativo:** só o código cuida. `products.stock` é `integer NOT NULL` sem `CHECK` (`Database.java:45`; não há restrição `c` na foto). O `UPDATE products SET stock = stock - ?` não tem guarda (`OrderRepository.java:200`). A conferência é uma leitura prévia, feita em outra conexão (`OrderRoutes.java:152-160`).
- **Quantidade positiva:** só o código cuida. `order_items.quantity` não tem `CHECK` (`Database.java:88`).
- **Aviso repetido não baixa o estoque duas vezes:** o banco tem `UNIQUE (transaction_id)` e `UNIQUE (order_id)` em `payment_transactions`. Mas a proteção não chega ao estoque nem ao crédito:
  - `recordPayment` captura a `SQLException` da violação, registra no log e segue em frente (`OrderRepository.java:165-167`);
  - o serviço então baixa o estoque e dá o crédito do mesmo jeito (`CheckoutService.java:233-243`);
  - cada passo usa sua própria conexão, com autocommit ligado (`Database.java:29`).

  Na prática, o que segura a repetição é a checagem de situação no código (`OrderRoutes.java:136, 142`). A restrição única existe, mas o banco não impede a segunda baixa.

Das três regras, uma tem restrição no banco, e mesmo essa o código contorna.
Conclusão: não segue

**B8, dado repetido.** O pedido guarda os valores do resumo, a forma de entrega, a de pagamento, a região e o nível do clube (`Database.java:62-75`). É a foto do momento que o enunciado pede. Não há preço atual do produto nem e-mail do cliente copiados em outra tabela, e nenhuma lista guardada como texto ou JSON.

Há duas repetições pequenas:
- **`notifications.customer_cpf`** dá para tirar de `notifications.order_id → orders.customer_cpf`. Mas a própria régua conta "o aviso é de um cliente" como uma ligação do domínio, por isso não pesa.
- **`orders.paid_at` e `payment_transactions.paid_at`** têm o mesmo nome e guardam coisas diferentes. A primeira é o relógio do servidor (`OrderRepository.java:137`); a segunda é o `paidAt` informado pelo banco (`:163`). A de `orders` nunca é lida (o `SELECT` em `:74-76` não a inclui). É mais uma coluna confusa do que uma cópia que possa divergir.
Conclusão: segue

**B9, criação do esquema.** É um script próprio. O `pom.xml` não tem Flyway nem Liquibase, não há JPA nem pasta `db/migration`, e não há `schema.sql`. É um único mecanismo, o que segue a recomendação. Mas não é versionado:
- com `CREATE TABLE IF NOT EXISTS` (`Database.java:39-119`), qualquer mudança futura numa tabela que já existe não será aplicada, e não há histórico do que já rodou;
- a carga do catálogo é um "conta, e se der zero insere" sem trava (`Database.java:131-144`). Com as duas máquinas subindo ao mesmo tempo sobre o banco vazio, uma delas pode falhar na chave primária ao subir. Não testei isso; é leitura do código.
Conclusão: em parte

**B10, tamanho dos textos.** A maior parte tem tamanho escolhido de propósito:
- `cpf varchar(11)`, que segue o domínio;
- os códigos de enumeração (`status varchar(20)`, `club_tier varchar(10)`, `type varchar(30)`, `status varchar(10)`), que cabem nos códigos do enunciado.

O padrão arbitrário aparece em outros lugares:
- `products.name` e `customers.email` são `varchar(255)`;
- `tracking_code varchar(50)` e `transaction_id varchar(100)` (`Database.java:76, 108`) são identificadores de terceiros, que o serviço não controla. Um `transactionId` maior que 100 faria o `INSERT` falhar em silêncio, porque `recordPayment` engole o erro.

Não há `char(n)`.
Conclusão: em parte

## O que mudaria primeiro

1. **Trocar as sete colunas de instante para `timestamptz`.** A regra das 48 horas e o serviço rodando em duas máquinas dependem de instantes sem ambiguidade, e hoje eles dependem do fuso da JVM.
2. **Pôr as regras e as ligações no banco.** Seriam `CHECK (stock >= 0)`, `CHECK (quantity > 0)` e as chaves estrangeiras `orders.customer_cpf → customers` e `order_items.sku → products`. Além disso, a gravação do pagamento, a baixa do estoque e o crédito deveriam ir numa só transação, que para se a inserção em `payment_transactions` violar a restrição única. Assim o banco garante de fato o que hoje só o código garante. O ideal é fazer isso numa ferramenta de migração versionada (B9).

## Dúvidas

- **B7:** o banco tem a restrição única sobre `transaction_id`, mas o código anula o efeito dela. Li a pergunta como "o banco protege a regra do estoque e do crédito?" e respondi que não. Assim o item fica `não segue`, mesmo existindo a restrição.
- **B8:** `notifications.customer_cpf` pode ser tirado do pedido. Não contei como repetição porque a régua lista essa ligação como do domínio.
