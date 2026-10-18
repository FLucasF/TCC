# o banco

## Visão geral

O agente criou o esquema com um script próprio de `CREATE TABLE IF NOT EXISTS`, rodado em `Database.initializeSchema()` quando o serviço sobe. São seis tabelas normalizadas de forma razoável: produtos, clientes, pedidos, itens, avisos e uma tabela de transações de pagamento. O que está melhor: todas as tabelas têm chave primária, o dinheiro está em `numeric`, as colunas obrigatórias são `NOT NULL`, e o aviso de pagamento repetido é barrado pelo próprio banco, com a chave primária `(order_id, transaction_id)`. Os problemas, do mais grave para o menos grave:

1. **Não há nenhuma chave estrangeira.** As seis ligações do domínio ficam só no código.
2. **Todos os instantes são `timestamp without time zone`**, inclusive o `created_at` do pedido, que é a base da regra das 48 horas.
3. **Faltam CHECKs.** O banco não garante o estoque não negativo nem a quantidade positiva.
4. **Os textos usam tamanhos arbitrários**, e o esquema não tem uma ferramenta de migração versionada.

## Item a item

**B1, chave primária.** As seis tabelas têm chave primária (`database/database-schema.json`, as restrições do tipo `p`): `customers_pkey (cpf)`, `notifications_pkey (id)`, `order_items_pkey (order_id, sku)`, `orders_pkey (id)`, `payment_transactions_pkey (order_id, transaction_id)` e `products_pkey (sku)`. Na tabela de itens, a chave composta `(order_id, sku)` também impede que o mesmo SKU apareça duas vezes no mesmo pedido (`src/main/java/com/loja/repository/Database.java:73`).
Conclusão: segue

**B2, as ligações.** A foto não tem nenhuma restrição do tipo `f`. Todas as ligações que o esquema guarda em coluna estão sem chave estrangeira:
- `orders.customer_cpf` → `customers`
- `order_items.order_id` → `orders`
- `order_items.sku` → `products`
- `payment_transactions.order_id` → `orders`
- `notifications.order_id` → `orders`
- `notifications.customer_cpf` → `customers`

O DDL confirma que nenhuma tabela declara `REFERENCES` (`Database.java:49-95`). Ou seja, o banco aceita um item de um pedido que não existe ou de um SKU que não está no catálogo. Quem garante a ligação é só a ordem das gravações no `OrderService`.
Conclusão: não segue

**B3, índice nas chaves estrangeiras.** Como não existe chave estrangeira, avaliei as colunas de ligação do B2. Duas estão cobertas, porque são a primeira coluna de uma chave primária: `order_items.order_id` (`order_items_pkey`) e `payment_transactions.order_id` (`payment_transactions_pkey`). As outras quatro não têm índice nenhum (a foto só tem os índices das chaves primárias):
- `orders.customer_cpf`, que é consultada em `OrderRepository.java:121` (`hasCompletedOrders`, a regra do WELCOME10);
- `notifications.customer_cpf`, consultada em `NotificationRepository.java:33`;
- `notifications.order_id`;
- `order_items.sku`.
Conclusão: em parte

**B4, dinheiro em tipo exato.** As colunas de dinheiro estão em `numeric`: `products.price` e `customers.credit_balance` são `numeric(10,2)`. Não há `real`, `double precision` nem `money` na foto. Os valores do resumo não ficam em colunas próprias, e sim no texto JSON `orders.summary_json`. Lá eles são `BigDecimal` dos dois lados (`domain/CheckoutSummary.java:6-15`), então são exatos na ida e na volta, mas o banco não sabe o tipo deles. A carga do catálogo passa por `double` no código (`ProductRepository.java:29`), mas converte com `new BigDecimal(item[2].toString())` (`ProductRepository.java:42`), o que dá o valor exato.
Conclusão: segue

**B5, datas e horas com fuso.** São três colunas de instante, todas `timestamp without time zone` na foto:
- `orders.created_at`, que é a base da regra das 48 horas (`OrderService.java:108`);
- `payment_transactions.paid_at`;
- `notifications.created_at`, cujo padrão `CURRENT_TIMESTAMP` é convertido para o fuso da sessão.

No DDL elas são `TIMESTAMP` sem mais nada (`Database.java:61, 84, 92`). O código grava com `Timestamp.from(Instant)` (`OrderRepository.java:39, 136`). Nesse caminho o driver converte o instante pelo fuso padrão da JVM, então o valor só volta certo se as duas máquinas de produção estiverem no mesmo fuso, e fica ambíguo na mudança de horário de verão. A régua diz que uma coluna já basta para este resultado.
Conclusão: não segue

**B6, obrigatórios `NOT NULL`.** Pela foto, todas as colunas que o enunciado trata como obrigatórias são `nullable: false`:
- o CPF e o e-mail (em `customers` e em `orders`), `status`, `created_at`, `region`, `shipping_method`, `payment_method`, `club_tier` e `summary_json`;
- `sku`, `quantity`, `stock`, `price`, `weight_kg`, `discontinued` e `credit_balance`;
- o `type`, o `status` e o `order_id` dos avisos, e o `paid_at` da transação.

Só aceitam nulo as colunas que podem faltar de verdade: `orders.coupon` (o cupom é opcional), `orders.tracking_code` (antes do envio) e `orders.payment_transaction_id` (antes do pagamento). Uma ressalva: os valores do resumo estão dentro de `summary_json`. A coluna é `NOT NULL`, mas o banco não garante cada valor do resumo separadamente.
Conclusão: segue

**B7, regras invariáveis no banco.** Uma das três regras está garantida no banco; as outras duas, só no código:
- **Estoque nunca negativo:** a foto não tem nenhuma restrição do tipo `c`, e `products.stock` é só `INTEGER NOT NULL` (`Database.java:36`). A trava está no código, no `UPDATE ... WHERE sku = ? AND stock >= ?` (`ProductRepository.java:99`). Ela funciona, mas um `CHECK (stock >= 0)` não existe.
- **Aviso de pagamento repetido:** essa o banco garante. A chave primária `payment_transactions_pkey (order_id, transaction_id)`, junto com o `INSERT ... ON CONFLICT DO NOTHING` (`OrderRepository.java:132`), impede a segunda gravação. O código só baixa o estoque e dá o crédito quando a inserção foi nova (`OrderService.java:96-101`).
- **Quantidade positiva:** `order_items.quantity` também é só `INTEGER NOT NULL`, sem `CHECK (quantity > 0)` (`Database.java:72`).
Conclusão: em parte

**B8, dado repetido.** Três pontos:
- **Fotos do momento, que o negócio pede:** `orders.customer_email` (o dado do cliente daquela compra) e `orders.summary_json` (o resumo, que "não muda mais"). O resumo é um objeto único de valores, não uma lista de entidades, então guardá-lo em JSON não esconde nenhuma ligação. Os itens estão numa tabela própria, como deveriam.
- **Repetição real, mas pequena:** `orders.payment_transaction_id` repete a transação que já está em `payment_transactions`. A tabela de transações guarda também as recusadas, então a coluna serve para marcar qual transação foi aceita. Mesmo assim, são duas fontes que podem divergir. Ela é gravada em `OrderService.java:140` e não é lida em nenhuma regra (fora o mapeamento em `OrderRepository.java:173`).
- **Fora do escopo deste item, mas vale registrar:** o item não guarda o preço unitário da compra. Não é repetição; é uma foto que falta.
Conclusão: segue

**B9, criação do esquema.** O `pom.xml` não tem Flyway, Liquibase nem Hibernate. O esquema vem de um script próprio no código, que roda ao subir: `new Database(...)` (`Application.java:26`) chama `initializeSchema()` (`Database.java:28-99`), que executa `CREATE TABLE IF NOT EXISTS`. Há um mecanismo só, o que está certo, mas sem versão. Como as tabelas são criadas só se não existirem, uma mudança de coluna nunca chega a um banco que já existe. Além disso, as duas máquinas sobem contra o mesmo banco sem nenhuma trava entre elas. A carga do catálogo tem o mesmo problema: ela confere se a tabela está vazia e só depois insere (`ProductRepository.java:17-48`), e as duas máquinas podem fazer isso ao mesmo tempo. Os testes usam o mesmo script contra o H2 (`src/test/java/com/loja/CheckoutServiceTest.java:30`).
Conclusão: em parte

**B10, tamanhos dos textos.** Dois tamanhos têm motivo no domínio: `cpf` `character varying(11)` e `zip_code` `character varying(8)`. Os outros são arbitrários:
- `name` e `email` com 255 (o padrão do JPA);
- os ids com 50, embora sejam UUIDs de 36 caracteres (`OrderService.java:44`), que caberiam no tipo `uuid`;
- `sku` e `coupon` com 50, `tracking_code` e `transaction_id` com 100;
- os códigos fixos com 10, 20 e 30.

Não há `char(n)`.
Conclusão: em parte

## O que mudaria primeiro

1. **Declarar as chaves estrangeiras e indexar as colunas que apontam.** Com `REFERENCES` nas seis ligações do B2 e índices em `orders.customer_cpf`, `notifications.customer_cpf`, `notifications.order_id` e `order_items.sku`, o banco passa a garantir as ligações em vez de depender da ordem das gravações, e as consultas por CPF ganham um índice.
2. **Trocar os três `TIMESTAMP` por `timestamptz` e acrescentar `CHECK (stock >= 0)` e `CHECK (quantity > 0)`.** A regra das 48 horas deixa de depender do fuso da JVM de cada máquina, e as duas regras invariáveis que só o código protege passam a ser protegidas também pelo banco. O lugar natural para essas mudanças é uma migração versionada (Flyway), o que resolve também o B9.

## Dúvidas

- **B3 sem nenhuma chave estrangeira.** Ao pé da letra, o item não se aplicaria. Escolhi avaliar as colunas de ligação do B2, porque é isso que a prática quer proteger, e concluí "em parte". Isso pesa a falta das chaves duas vezes, uma no B2 e outra no B3. Se o critério for literal, a conclusão seria "não se aplica".
- **B6 e o resumo em JSON.** Considerei suficiente que `summary_json` seja `NOT NULL`, porque guardar o resumo como foto é permitido. O banco, porém, não garante cada valor do resumo separadamente.
