# banco

## Visão geral

O agente escreveu o esquema à mão num `schema.sql`, que o Spring executa a cada subida (`spring.sql.init.mode=always`). Não usou JPA nem ferramenta de migração. A foto do banco (`database/database-schema.json`) bate coluna a coluna com o script. No geral o esquema é cuidadoso:
- as seis tabelas têm chave primária;
- quase todas as ligações são chaves estrangeiras;
- o aviso de pagamento tem chave primária `(order_id, transaction_id)`, gravada na mesma transação que baixa o estoque, então o banco também protege contra o aviso repetido;
- a quantidade do item tem `CHECK`;
- as obrigatórias são `NOT NULL`;
- o resto do dinheiro está em `numeric(12,2)`.

Os problemas, do mais grave ao mais leve:
1. O valor final do pedido está em `double precision`, um tipo inexato.
2. A data de registro do pedido, que é a base da regra das 48 horas, está em `timestamp without time zone`, enquanto a data do pagamento está com fuso.
3. O estoque não tem `CHECK (stock >= 0)` no banco.
4. O item do pedido não tem chave estrangeira para o produto.
5. O `order_id` do aviso ao cliente não tem índice.
6. O esquema é um script não versionado, com `CREATE TABLE IF NOT EXISTS`.
7. Há alguns tamanhos de texto arbitrários.

## Item a item

**B1, chave primária.** As seis tabelas do domínio têm chave primária (`database/database-schema.json`, as restrições `p`):
- `customer_pkey (cpf)`;
- `customer_order_pkey (id)`;
- `notification_pkey (id)`;
- `order_item_pkey (order_id, line_no)`, composta (`src/main/resources/schema.sql:44`);
- `payment_notice_pkey (order_id, transaction_id)` (`schema.sql:51`);
- `product_pkey (sku)`.

Nenhuma tabela fica sem chave. Segue a prática.
Conclusão: segue

**B2, as ligações.** As ligações do enunciado que o esquema guarda em coluna:
- pedido → cliente: `customer_order_cpf_fkey`, `FOREIGN KEY (cpf) REFERENCES customer(cpf)`. Tem.
- item → pedido: `order_item_order_id_fkey`. Tem.
- item → produto: a coluna `order_item.sku` (`schema.sql:42`, `sku VARCHAR(64) NOT NULL`) aponta para `product.sku`, mas a foto não tem restrição `f` nela. **Falta.**
- aviso de pagamento → pedido: `payment_notice_order_id_fkey`. Tem.
- aviso ao cliente → pedido e → cliente: `notification_order_id_fkey` e `notification_cpf_fkey`. Tem.

Das seis ligações, cinco são chaves estrangeiras. A do item com o produto fica só a cargo do código: a validação do checkout recusa o SKU desconhecido.
Conclusão: em parte

**B3, índice nas chaves estrangeiras.** Para cada chave estrangeira, procurei na foto (`indexes`) um índice que começa pela coluna dela:
- `customer_order.cpf`: `customer_order_cpf_status (cpf, status)` (`schema.sql:37`). Coberta.
- `order_item.order_id`: `order_item_pkey (order_id, line_no)`. Coberta.
- `payment_notice.order_id`: `payment_notice_pkey (order_id, transaction_id)`. Coberta.
- `notification.cpf`: `notification_cpf (cpf, seq)` (`schema.sql:63`). Coberta.
- `notification.order_id`: **sem índice**. O único outro índice da tabela começa por `cpf`, e a chave primária é `id`.

Quatro de cinco estão cobertas. Falta a de `notification.order_id`, que pesa quando se apaga ou se mexe num `customer_order` e o banco precisa varrer `notification`.
Conclusão: em parte

**B4, dinheiro em tipo exato.** Quase todo o dinheiro está em `numeric(12,2)`:
- `product.price` e `customer.credit_balance`;
- em `customer_order`: `products_subtotal`, `coupon_discount`, `shipping_cost`, `insurance`, `payment_adjustment`, `installment_amount` e `next_purchase_credit`.

A exceção é **`customer_order.final_total`, que é `double precision`** (foto, coluna `final_total`; `schema.sql:30`). O código grava um `BigDecimal` (`src/main/java/com/loja/orders/order/OrderRepository.java:52`) e, na leitura, arredonda de volta para centavos (`OrderRepository.java:144` e `:167-168`, `Money.round`). Isso esconde o erro na resposta da API, mas o valor guardado é binário e inexato. Qualquer conta feita no próprio banco (uma soma de faturamento, uma comparação com `products_subtotal - coupon_discount + ...`) vai trabalhar com um número aproximado. Pela régua, uma coluna de dinheiro em tipo inexato basta.
Conclusão: não segue

**B5, datas com fuso.** `customer_order.paid_at` é `timestamp with time zone` (`schema.sql:23`), mas **`customer_order.created_at` é `timestamp without time zone`** (foto; `schema.sql:20`). É justamente o instante de registro, a base da regra das 48 horas (`src/main/java/com/loja/orders/order/OrderService.java:87`, que compara `createdAt` com `paidAt`).

O código tenta contornar: grava o valor como `OffsetDateTime` em UTC (`OrderRepository.java:45` e `:171-173`) e lê com `getObject(..., OffsetDateTime.class)` (`OrderRepository.java:154`). Mas o banco não sabe que o valor está em UTC. Isso depende da conversão do driver e do fuso da sessão (veja as Dúvidas). Além disso, as duas colunas de instante da mesma tabela usam tipos diferentes. Pela régua, uma coluna de instante sem fuso basta.
Conclusão: não segue

**B6, obrigatórios `NOT NULL`.** Na foto, só três colunas aceitam nulo: `customer_order.tracking_code`, `payment_transaction_id` e `paid_at`. As três só existem depois do envio ou do pagamento, então o nulo é legítimo. Todas as obrigatórias são `NOT NULL`:
- o CPF (`customer.cpf`, `customer_order.cpf`, `notification.cpf`);
- o e-mail, a situação e a data de registro do pedido;
- os onze valores do resumo;
- o SKU, a quantidade e o `line_no` do item;
- o nome, o preço, o peso, o estoque e o `discontinued` do produto;
- o saldo do cliente;
- o tipo, a situação e o pedido do aviso;
- o `outcome` do aviso de pagamento.

Segue a prática.
Conclusão: segue

**B7, regras invariáveis no banco.**
- **O estoque nunca negativo:** a foto não tem restrição `c` em `product`. `stock` é só `INTEGER NOT NULL` (`schema.sql:6`). A regra é garantida apenas pelo código: ele trava as linhas com `SELECT ... FOR UPDATE` (`src/main/java/com/loja/orders/catalog/ProductRepository.java:98`), confere antes de baixar (`src/main/java/com/loja/orders/catalog/InventoryService.java:31-33`) e depois faz o `UPDATE stock = stock + :delta` (`ProductRepository.java:107`). Um `UPDATE` direto, ou outro caminho do código, deixaria o estoque negativo sem o banco reclamar.
- **O aviso de pagamento repetido:** o banco garante. `payment_notice_pkey (order_id, transaction_id)` e a gravação do aviso (`OrderService.java:94`) acontecem na mesma transação (`@Transactional`, `OrderService.java:74`) que baixa o estoque (`:90`) e dispara o crédito (`:96`). Se dois avisos iguais passassem pela checagem ao mesmo tempo, a segunda inserção violaria a chave e desfaria a baixa e o crédito junto.
- **A quantidade positiva:** o banco garante, com `order_item_quantity_check`, `CHECK ((quantity > 0))` (`schema.sql:43`).

Duas das três regras estão no banco. A do estoque fica só no código.
Conclusão: em parte

**B8, dado repetido.**
- Não há listas guardadas como texto ou JSON: os itens, os avisos de pagamento e os avisos ao cliente têm tabelas próprias.
- Os valores do resumo e o `email` em `customer_order` são a foto do momento pedida pelo enunciado. Repare que `customer` nem tem e-mail.
- Há duas repetições pequenas, ambas de dados que não mudam:
  - `notification.cpf` dá para tirar de `notification.order_id → customer_order.cpf`. O CPF de um pedido nunca muda, e a régua trata o aviso ao cliente como ligado aos dois.
  - `customer_order.payment_transaction_id` repete a linha `ACCEPTED` de `payment_notice`. As duas são gravadas juntas (`OrderRepository.java:87` e `:114`), e o `payment_transaction_id`, assim como o `paid_at`, nunca é lido pelo código (só aparece no `markPaid`).

Como nada disso pode divergir com o tempo, não é a repetição que o item condena. Uma observação fora da repetição: `order_item` não guarda o preço unitário da hora da compra, então o detalhe por item só se reconstrói com o preço atual do produto, que pode ter mudado. Isso é uma foto que falta, não um dado repetido.
Conclusão: segue

**B9, como o esquema é criado.** O mecanismo é um `schema.sql` próprio (`src/main/resources/schema.sql`), seguido de `data.sql` com o catálogo, executados pelo `spring.sql.init.mode=always` (`src/main/resources/application.properties:3`). O `pom.xml` não tem Flyway, Liquibase nem JPA: só `spring-boot-starter-jdbc`. Então não há `ddl-auto`, e o mecanismo é um só, o que a documentação recomenda.

Mas não é uma ferramenta de migração versionada. Como o script usa `CREATE TABLE IF NOT EXISTS` (`schema.sql:1`, `:10`, `:15`...), qualquer mudança futura numa tabela que já existe (uma coluna nova, o `CHECK` do estoque, a troca de `final_total` para `numeric`) nunca chega a um banco já criado. E não fica registrado em que versão o esquema está.
Conclusão: em parte

**B10, tamanho dos textos.** Os tamanhos, coluna a coluna:
- com motivo do domínio: `cpf varchar(11)` (os 11 dígitos) e `email varchar(320)` (o limite usual de endereço de e-mail);
- ids com 36: `customer_order.id`, `notification.id`, `order_item.order_id` e `payment_notice.order_id` são UUIDs em texto. O tipo `uuid` seria o natural, mas o 36 tem motivo;
- arbitrários: `product.name varchar(255)`, `tracking_code varchar(255)`, `payment_transaction_id varchar(255)`, `payment_notice.transaction_id varchar(255)`, `sku varchar(64)`, e os `status`/`type`/`outcome` em `varchar(32)` ou `varchar(16)`.

Não há `char(n)`. A mistura é de alguns tamanhos justificados com vários arbitrários. Os 255 do id da transação e do código de rastreio, que vêm de fora, são exatamente o padrão que a régua aponta.
Conclusão: em parte

## O que mudaria primeiro

1. **Trocar `final_total` para `numeric(12,2)` e `created_at` para `timestamp with time zone`.** São os dois itens que derrubam a dimensão sozinhos. As duas colunas são o centro do pedido: o valor cobrado, e o instante de que depende a regra das 48 horas. A correção é pequena e deixa a tabela coerente com as colunas vizinhas, que já estão certas.
2. **Pôr no banco o que hoje só o código garante:** `CHECK (stock >= 0)` em `product` e `FOREIGN KEY (sku) REFERENCES product(sku)` em `order_item` (com índice em `notification.order_id`). Para essas mudanças chegarem a um banco que já existe, isso pede trocar o `schema.sql` com `IF NOT EXISTS` por migrações versionadas (Flyway).

## Dúvidas

- **O `created_at` sem fuso.** Não verifiquei se, na prática, o valor desloca. Isso depende de como o driver do PostgreSQL converte um `OffsetDateTime` gravado numa coluna sem fuso (usa o fuso da sessão, que vem do fuso padrão da JVM) e de como ele lê de volta (`OrderRepository.java:154`). Se a JVM roda em UTC, o valor volta igual. Se não roda, o registro pode voltar deslocado e mexer na regra das 48 horas. Isso é coisa da suíte de comportamento. Aqui o item conta só o tipo da coluna, que é o que a régua pede.
- **B8.** Li a régua como condenando a repetição de dados que **mudam**. Por isso não rebaixei o item pelo `notification.cpf` nem pelo `payment_transaction_id` duplicado, que são imutáveis. Só registrei os dois.
- **B9.** A régua pede só que eu diga qual é o mecanismo. Escolhi `em parte` porque é um mecanismo só (o que a documentação pede), mas não é a ferramenta de migração versionada que ela recomenda.
