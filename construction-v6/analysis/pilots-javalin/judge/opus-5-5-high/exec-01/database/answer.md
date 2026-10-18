# banco

## Visão geral

O agente não usou JPA. Ele escreveu o DDL à mão numa lista de comandos `CREATE TABLE IF NOT EXISTS` (`src/main/java/shop/db/Schema.java:13-36`), que roda ao subir o serviço com uma trava do PostgreSQL (`pg_advisory_lock`) para as duas instâncias não criarem o esquema ao mesmo tempo. Os tipos estão bem escolhidos: dinheiro em `numeric(12,2)`, o instante em `timestamp with time zone`, e todo campo obrigatório é `NOT NULL`. O banco protege duas das três regras invariáveis: o estoque, com um `CHECK (stock >= 0)`, e o pagamento repetido, com a chave primária `(order_id, transaction_id)`. O problema mais sério são as ligações. Das cinco ligações que o esquema guarda, só uma é chave estrangeira (`order_items.order_id`). O pedido não aponta para o cliente, o item não aponta para o produto, e os avisos de pagamento e os avisos ao cliente não apontam para nada. Os problemas menores:
- a quantidade do item não tem `CHECK`;
- o esquema é criado por um script próprio, sem versão;
- a maioria dos textos tem um tamanho arbitrário (`varchar(255)`, o CPF em `varchar(32)`);
- o resumo do pedido fica num JSON dentro de uma coluna `text`.

## Item a item

**B1, chave primária.** As seis tabelas têm chave primária (`database/database-schema.json`, as restrições `p`): `customers_pkey (cpf)`, `orders_pkey (id)`, `products_pkey (sku)`, `notifications_pkey (seq)`, `order_items_pkey (order_id, line_no)` e `payment_events_pkey (order_id, transaction_id)`. Os avisos ao cliente têm uma chave substituta `seq` (identity) e o `id` público único (`notifications_id_key`) (`Schema.java:33`). Segue a prática.
Conclusão: segue

**B2, as ligações.** A foto tem uma única restrição `f`: `order_items_order_id_fkey`, `FOREIGN KEY (order_id) REFERENCES orders(id)` (`Schema.java:27`). As outras ligações do enunciado estão guardadas em colunas, mas sem chave estrangeira:
- o pedido é de um cliente: `orders.cpf` não aponta para `customers` (`Schema.java:22`);
- o item é de um produto: `order_items.sku` não aponta para `products` (`Schema.java:28`);
- o aviso de pagamento é de um pedido: `payment_events.order_id` não aponta para `orders` (`Schema.java:30`);
- o aviso ao cliente é de um pedido e de um cliente: `notifications.order_id` e `notifications.cpf` não apontam para nada (`Schema.java:34`).

Nada impedia essas chaves. O cliente é gravado antes do pedido, na mesma transação (`src/main/java/shop/app/OrderService.java:55-56`), e os produtos existem desde a carga inicial. Faltam quatro chaves estrangeiras, incluindo a ligação central pedido → cliente. A única que existe não basta para a dimensão seguir a prática.
Conclusão: não segue

**B3, índice nas chaves estrangeiras.** A única chave estrangeira, `order_items.order_id`, tem índice: ela é a primeira coluna da chave primária `order_items_pkey (order_id, line_no)`. Entre as ligações que não têm chave estrangeira, `orders.cpf` (`idx_orders_cpf_status`), `notifications.cpf` (`idx_notifications_cpf`) e `payment_events.order_id` (a chave primária) também têm índice. `order_items.sku` e `notifications.order_id` não têm. Isso pesaria se as chaves que faltam no B2 fossem criadas, mas não é uma chave estrangeira sem índice.
Conclusão: segue

**B4, dinheiro em tipo exato.** `products.price` e `customers.credit_balance` são `numeric(12,2)` (foto, `columns`). Nenhuma coluna é `real`, `double precision` ou `money`. Os valores do resumo não têm coluna própria: vão como texto JSON em `orders.summary` (`text`). Esse JSON sai de campos `BigDecimal` (`src/main/java/shop/domain/Summary.java:5-8`), gravados por extenso (`WRITE_BIGDECIMAL_AS_PLAIN`, `src/main/java/shop/domain/Json.java:21`) e lidos de volta como `BigDecimal` (`Dao.java:177`). A gravação é exata, mas o banco não sabe que aquilo é dinheiro.
Conclusão: segue

**B5, datas com fuso.** A única coluna de instante é `orders.created_at`, do tipo `timestamp with time zone` (foto; `Schema.java:23`), gravada em UTC (`Dao.java:147`). O `paidAt` só é usado para comparar com o prazo e não é guardado (`OrderService.java:69-79`). Não há coluna `timestamp without time zone`.
Conclusão: segue

**B6, obrigatórios `NOT NULL`.** Na foto, toda coluna tem `nullable: false`, menos `orders.tracking_code`, que só existe depois do envio e pode mesmo ser nula. O CPF, o e-mail, a situação, a data de registro, o SKU, a quantidade, o saldo, o preço, o estoque e o fora de linha são todos `NOT NULL`. Uma ressalva: os valores do resumo só são obrigatórios em bloco (`orders.summary text NOT NULL`). O banco não impede um resumo sem `finalTotal`, por exemplo. Isso vem de o resumo ser guardado como JSON (B8), e não de um campo obrigatório que aceita nulo.
Conclusão: segue

**B7, regras invariáveis no banco.**
- **O estoque nunca fica negativo:** o banco garante. Há `products_stock_check`, `CHECK ((stock >= 0))` (foto; `Schema.java:16`). A baixa é um `UPDATE ... SET stock = stock + ?` (`Dao.java:77`), que esse `CHECK` recusaria se o estoque ficasse negativo.
- **O aviso de pagamento repetido não baixa o estoque nem dá o crédito duas vezes:** o banco garante. A chave primária `payment_events_pkey (order_id, transaction_id)` é gravada na mesma transação que a baixa do estoque e o crédito (`OrderService.java:65-98`, `recordPaymentOutcome` na linha 97). Se dois avisos iguais correrem ao mesmo tempo, o segundo `INSERT` viola a chave e a transação inteira volta. O código também trava a linha do pedido (`FOR UPDATE`, `Dao.java:166`).
- **A quantidade do item é positiva:** só o código cuida. `order_items.quantity` é `integer NOT NULL`, sem `CHECK` (foto: a única restrição `c` é a do estoque). A trava está em `CheckoutService.java:109-110`.

Duas das três regras estão garantidas no banco.
Conclusão: em parte

**B8, dado repetido.** O e-mail aparece em `orders.email` e em `customers.email`. No pedido, ele é a foto da compra, o que o enunciado permite. Em `customers`, ele é gravado só no primeiro pedido (`ON CONFLICT (cpf) DO NOTHING`, `Dao.java:94`), nunca é atualizado e nunca é lido (`Dao.java:108` só lê `cpf, credit_balance`). É uma cópia do e-mail do primeiro pedido que vai ficando velha. É menor, mas não tem motivo. `notifications.cpf` dá para tirar de `orders.cpf` pelo `order_id`, mas o enunciado diz que o aviso é de um pedido e de um cliente, então leio as duas colunas como as duas ligações pedidas. Os itens e os avisos estão em tabelas próprias, não em JSON. O resumo em JSON (`orders.summary text`, `Dao.java:148`) é um objeto só, não uma lista, e é a foto do momento que o enunciado pede. Ele não esconde ligação nenhuma, mas tira do banco os tipos e o `NOT NULL` de cada valor (B4, B6).
Conclusão: segue

**B9, criação do esquema.** O `pom.xml` não tem Flyway, Liquibase nem JPA/Hibernate, e não há `src/main/resources` nem `db/migration`. Um script próprio cria o esquema: a lista `DDL` em `Schema.java:13-36`, chamada por `Main.java:43,55` ao subir, com novas tentativas e trava entre instâncias. A carga do catálogo também está ali (`Schema.java:78-95`). É um mecanismo só, o que segue a recomendação. Mas não tem versão: o `CREATE TABLE IF NOT EXISTS` não muda uma tabela que já existe, então uma coluna nova (para a situação "em separação" ou um novo tipo de aviso, por exemplo) não chegaria a um banco de produção já criado. A documentação aponta as ferramentas de migração versionada como o caminho de mais alto nível.
Conclusão: em parte

**B10, tamanho dos textos.** Na foto há:
- **255 sem motivo do domínio:** `character varying(255)` em `products.name`, `customers.email`, `orders.email`, `orders.tracking_code` e `payment_events.transaction_id`;
- **tamanho que não é o do domínio:** o CPF em `varchar(32)` nas três tabelas, quando o enunciado diz 11 números; o SKU em `varchar(64)`;
- **com motivo:** `varchar(36)` nos ids UUID (o tipo `uuid` seria mais exato) e as situações em `varchar(32)`/`varchar(8)`.

Só o `summary` é `text`. Não há `char(n)`. O agente escolheu tamanhos coluna a coluna, mas a maioria é arbitrária.
Conclusão: em parte

## O que mudaria primeiro

1. **Criar as quatro chaves estrangeiras que faltam,** cada uma com índice: `orders.cpf → customers`, `order_items.sku → products`, `payment_events.order_id → orders`, e `notifications.order_id → orders` e `notifications.cpf → customers`. Hoje o banco aceita um item de um produto que não existe ou um aviso de um pedido que não existe, e essa é a falha mais grave da dimensão.
2. **Passar a criar o esquema com uma ferramenta de migração versionada (Flyway)** e já incluir o `CHECK (quantity > 0)` em `order_items`. As mudanças que o enunciado já anuncia (situação nova, avisos novos) passariam a chegar ao banco que já existe, e a terceira regra invariável ficaria garantida no banco.

## Dúvidas

- No **B2**, há uma chave estrangeira e quatro faltando. Escolhi `não segue` e não `em parte`, porque a maioria das ligações, incluindo a central pedido → cliente, não está garantida.
- No **B8**, li `notifications.cpf` como a ligação do aviso ao cliente que o enunciado pede, e não como repetição de `orders.cpf`. Li o resumo em JSON como a foto do momento, e não como uma lista escondida, porque ele não guarda nenhuma ligação. O custo dessa escolha aparece no B4 e no B6.
- No **B10**, os tamanhos estão misturados: há os UUIDs e as situações com motivo, e há o 255 e o CPF 32 sem motivo. Por isso escolhi `em parte` e não `não segue`.
