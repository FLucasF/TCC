# banco

## Visão geral

Quem cria o esquema é um script próprio: `CREATE TABLE IF NOT EXISTS` escritos à mão em `Database.migrate()` (`src/main/java/com/loja/database/Database.java:24-100`), rodados toda vez que o serviço sobe. São cinco tabelas (`products`, `customers`, `orders`, `order_items`, `notifications`), e a foto em `database/database-schema.json` bate com o script.

Os pontos fortes:
- toda tabela tem chave primária;
- todo valor em dinheiro está em `numeric(10,2)`;
- as colunas obrigatórias são quase todas `NOT NULL`;
- os itens do pedido ficam numa tabela própria, com chave estrangeira para o pedido e para o produto;
- o pedido guarda a foto do resumo.

Os problemas, do mais grave para o menos grave:
1. **Os instantes não guardam o fuso.** `orders.created_at`, `orders.paid_at` e `notifications.created_at` são `timestamp without time zone`, e a regra das 48 horas depende delas.
2. **O banco não garante nenhuma das três regras invariáveis.** A foto não tem nenhuma restrição `c` (CHECK) nem `u` (única).
3. **Nenhuma das quatro chaves estrangeiras tem índice.**
4. **`notifications.order_id` não tem chave estrangeira e aceita nulo**, embora todo aviso seja de um pedido.
5. **O esquema não é versionado.**

## Item a item

**B1, chave primária.** As cinco tabelas têm chave primária (`database/database-schema.json`, as restrições `p`):
- `customers_pkey (cpf)`
- `products_pkey (sku)`
- `orders_pkey (id)`
- `order_items_pkey (id)`
- `notifications_pkey (id)`

A do item é uma chave artificial `SERIAL` (`Database.java:78`). Como não há nenhuma restrição única em `(order_id, sku)`, o banco aceitaria o mesmo SKU duas vezes num pedido, coisa que o enunciado proíbe. Isso é um detalhe; o item pede chave primária, e todas têm.
Conclusão: segue

**B2, as ligações.** As ligações do enunciado, uma a uma:
- **pedido → cliente:** tem a chave `orders_cpf_fkey` (`FOREIGN KEY (cpf) REFERENCES customers(cpf)`).
- **item → pedido:** tem a chave `order_items_order_id_fkey`.
- **item → produto:** tem a chave `order_items_sku_fkey`.
- **aviso de pagamento → pedido:** fica guardado no próprio pedido (`orders.paid_at` e `orders.payment_transaction_id`, `Database.java:71-72`). Não há coluna que aponte para outra tabela, então não falta chave.
- **aviso ao cliente → cliente:** tem a chave `notifications_cpf_fkey`.
- **aviso ao cliente → pedido:** **falta a chave estrangeira.** A coluna `notifications.order_id` (`character varying(36)`, `Database.java:90`) aponta para `orders.id`, mas a foto não traz nenhuma restrição `f` nela. O banco aceita um aviso de um pedido que não existe.

Cinco das seis ligações estão protegidas; a do aviso com o pedido não está.
Conclusão: em parte

**B3, índice nas chaves estrangeiras.** Em `indexes` só aparecem os índices das chaves primárias (`customers_pkey`, `notifications_pkey`, `order_items_pkey`, `orders_pkey`, `products_pkey`), e nenhum deles começa por uma coluna de chave estrangeira. As quatro chaves estão sem índice:
- **`orders.cpf`:** é a busca do WELCOME10 (`CheckoutService.java:324`).
- **`order_items.order_id`:** é a busca de todo `GET /orders/{id}` (`OrderService.java:112`).
- **`order_items.sku`.**
- **`notifications.cpf`:** é a busca da lista de avisos (`NotificationService.java:28`).

As três consultas citadas filtram exatamente por essas colunas.
Conclusão: não segue

**B4, dinheiro em tipo exato.** Todas as colunas de dinheiro são `numeric(10,2)`:
- `products.price`;
- `customers.credit_balance`;
- em `orders`: `products_subtotal`, `coupon_discount`, `shipping_cost`, `insurance`, `payment_adjustment`, `final_total`, `installment_amount` e `next_purchase_credit`.

Nenhuma é `real`, `double precision` ou `money`. O peso também é exato, `numeric(5,2)`. O código Java lê e grava esses valores como `double` (`OrderService.java:32-41`, `CustomerService.java:47`, `Database.java:130`). Isso é um risco de cálculo, de outra dimensão; o que o banco guarda é exato.
Conclusão: segue

**B5, instantes com fuso.** As três colunas de instante são `timestamp without time zone` (`Database.java:53`, `:71` e `:92`):
- `orders.created_at`;
- `orders.paid_at`;
- `notifications.created_at`.

O enunciado trabalha com instantes em UTC (`2026-10-14T15:30:00Z`) e com a regra das 48 horas, que compara `paidAt` com `createdAt` (`PaymentService.java:50-54`). Sem o fuso na coluna, o instante gravado depende do fuso da sessão de quem grava e de quem lê. A régua já dá `não segue` para uma coluna nessa situação; aqui são três.
Conclusão: não segue

**B6, obrigatórios `NOT NULL`.** Estão `NOT NULL`:
- o CPF (`orders.cpf`, `customers.cpf`, `notifications.cpf`);
- `orders.status` e `orders.created_at`;
- todos os valores do resumo, inclusive `delivery_days`, `installments` e `gift`;
- `order_items.sku` e `order_items.quantity`;
- `products.stock`, `price`, `name` e `weight_kg`;
- `customers.credit_balance`, com padrão `0.00`;
- `notifications.type` e `notifications.status`.

Aceitam nulo, corretamente, o que só existe depois: `orders.tracking_code`, `orders.paid_at`, `orders.payment_transaction_id`, e também `orders.coupon`, que é opcional.

A exceção é **`notifications.order_id`, que aceita nulo** (`nullable: true`, `Database.java:90`). O enunciado diz que todo aviso tem `orderId`, e o código sempre o preenche (`NotificationService.java:20`), mas o banco não exige.
Conclusão: em parte

**B7, regras invariáveis no banco.** A foto não tem nenhuma restrição do tipo `c` nem `u`. As três regras ficam só no código:
- **Estoque nunca negativo.** Não há `CHECK (stock >= 0)` em `products.stock`. A trava é um "confere e depois baixa" no código: lê o estoque em `PaymentService.java:62` e faz `UPDATE products SET stock = stock + ?` sem condição em `ProductService.java:30`.
- **Aviso de pagamento repetido.** Não há restrição única com o id da transação. `orders.payment_transaction_id` é uma coluna solta e anulável. Quem impede a segunda baixa de estoque e o segundo crédito é só a checagem da situação do pedido em `PaymentService.java:43-48`.
- **Quantidade do item positiva.** Não há `CHECK (quantity > 0)` em `order_items.quantity`. A validação está só em `CheckoutService`.

Isso não diz que o serviço erra; diz que o banco não protege nenhuma das três regras.
Conclusão: não segue

**B8, dado repetido sem motivo.** O `email` aparece em duas tabelas, `customers.email` e `orders.email` (`Database.java:42` e `:51`):
- **`orders.email`** guarda o e-mail informado naquela compra (`OrderService.java:29`). É dado do cliente daquela compra, ou seja, uma foto do momento.
- **`customers.email`** só é gravado quando o cliente é criado (`CustomerService.java:20-25`), nunca é atualizado e nenhum ponto do código o lê para decidir nada. É uma cópia que pode envelhecer, mas não chega a ser duas versões de um dado em uso.

As colunas `region`, `shipping_method`, `payment_method`, `club_tier` e `coupon` do pedido são dados da compra, e os valores do resumo são a foto que o enunciado pede. Não há lista guardada como texto ou JSON: os itens têm tabela própria e os avisos também.

Uma observação fora do que este item pune: o item não guarda o preço na hora da compra. A foto fica só nos totais do resumo.
Conclusão: segue

**B9, como o esquema é criado.** O `pom.xml` não tem Flyway, Liquibase nem Hibernate, e não há nenhum arquivo `.sql`. O esquema sai de um script próprio, os `CREATE TABLE IF NOT EXISTS` de `Database.java:28-94`, chamado em `Database.init` (`Database.java:21`) ao subir (`App.java:30`). É um mecanismo só, o que a documentação recomenda. Mas:
- **Não há versionamento.** O `IF NOT EXISTS` nunca altera uma tabela que já existe, então qualquer mudança futura no esquema não chega a um banco já criado.
- **Não há trava para duas máquinas subindo juntas.** O enunciado diz que são duas máquinas no mesmo banco, e nada no script impede as duas de criarem as tabelas e semearem o catálogo ao mesmo tempo. O semeio checa `COUNT(*)` e depois insere (`Database.java:104-118`), o que pode duplicar a tentativa de carga.

Uma ferramenta de migração versionada resolveria as duas coisas.
Conclusão: em parte

**B10, tamanho dos textos.** Nenhuma coluna é `text`. Quanto aos tamanhos:
- **Têm motivo do domínio:** o CPF com 11 (`customers.cpf`, `orders.cpf`, `notifications.cpf`) e os ids com 36, que é o tamanho de um UUID em texto (o tipo `uuid` seria o natural).
- **São arbitrários:** `character varying(255)` em `products.name`, `customers.email` e `orders.email`; e números escolhidos sem regra do domínio em `sku` (20), `status` (30 e 10), `type` (30), `tracking_code` (50), `coupon` (50), `payment_transaction_id` (100), `region`, `shipping_method` e `payment_method` (20) e `club_tier` (10).

Nenhuma coluna é `char(n)`. Alguns tamanhos têm motivo, mas a maioria é arbitrária.
Conclusão: em parte

## O que mudaria primeiro

1. **Trocar as três colunas de instante para `timestamptz` e pôr as regras no banco.** Os instantes são `orders.created_at`, `orders.paid_at` e `notifications.created_at`. As regras seriam:
   - `CHECK (stock >= 0)` em `products`;
   - `CHECK (quantity > 0)` em `order_items`;
   - uma restrição única em `orders.payment_transaction_id`, ou uma tabela de avisos de pagamento com chave em `(order_id, transaction_id)`.

   A regra das 48 horas, o estoque e a idempotência do pagamento são as regras que o enunciado mais destaca, e hoje só o código as protege, com o serviço rodando em duas máquinas no mesmo banco.
2. **Completar as ligações e passar a versionar o esquema.** Pôr a chave estrangeira e o `NOT NULL` em `notifications.order_id`, e criar índices em `orders(cpf)`, `order_items(order_id)`, `order_items(sku)` e `notifications(cpf)`. Levar o script para Flyway, para que as próximas mudanças do esquema cheguem aos bancos que já existem.

## Dúvidas

- **B9 ficou como `em parte`.** A régua pede que se diga qual é o mecanismo, mas não diz qual conclusão dar a um script próprio usado sozinho. Escolhi `em parte`: é um mecanismo só, como a documentação recomenda, mas não é versionado e não tem trava para duas máquinas subindo juntas.
- **Uma provável falha no primeiro boot, que não pesa na nota.** Em `Database.java:117`, `conn.commit()` é chamado numa conexão do Hikari, que por padrão está em autocommit. Pela leitura, o driver do PostgreSQL recusa esse `commit`. No primeiro boot, as tabelas seriam criadas e os produtos gravados (cada insert com o próprio commit), e então a subida falharia. Não rodei o serviço, porque esta dimensão não roda; a foto tem as tabelas, então o esquema foi criado de algum jeito. Quem confirma isso é a dimensão de comportamento.
