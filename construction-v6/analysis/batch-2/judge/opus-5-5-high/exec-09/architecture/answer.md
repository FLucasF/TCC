# a arquitetura

## Visão geral

O projeto é organizado por camada: `api` (as rotas e os objetos da requisição), `services`, `database`, `models` e `utils`. Toda a regra de negócio está em métodos estáticos. O ponto mais forte é o cálculo: o resumo e o pedido passam pelo mesmo `CheckoutService.calculateSummary`. Os problemas, do mais grave ao mais leve:

1. **A regra de negócio conhece a web.** Os serviços recebem os objetos de entrada da API (`api.CheckoutRequest`, `api.PaymentRequest`) e devolvem códigos HTTP dentro de `ValidationError` e `PaymentResult`.
2. **O controller decide o negócio.** `Routes` decide e executa sozinho o envio, a entrega e o cancelamento: confere a situação do pedido, devolve o estoque, estorna o crédito e gera o aviso.
3. **O acesso ao banco é só em parte isolado.** Os serviços de produto, cliente, aviso e pedido fazem na prática o papel de DAO, mas o `CheckoutService`, a principal classe de cálculo, monta SQL e usa JDBC direto.

O único ciclo do relatório (`api` ↔ `services`) fecha por tipos de dado, e por isso é leve.

## Item a item

**A1, a regra de negócio não conhece a web?** O relatório não acha tipos do Javalin fora de `App` e `api.Routes`. Os objetos da API do projeto, porém, entram na regra de negócio, como mostra a dependência `services -> api` do relatório:
- `services/CheckoutService.java:3` importa `com.loja.api.CheckoutRequest`. A validação (`:17`) e o cálculo (`:199`, `:281`, `:337`) recebem esse objeto de requisição e trabalham com ele.
- `services/OrderService.java:3` e `:11`: `createOrder(CheckoutRequest req, ...)` lê a requisição campo a campo (`:28-47`, `:51`).
- `services/PaymentService.java:3` e `:32`: `processPayment(String, PaymentRequest)`, com `paidAt` ainda como texto da requisição, convertido em `:50`.

Os códigos HTTP também moram no negócio:
- `CheckoutService.ValidationError` tem um campo `httpStatus` (`CheckoutService.java:460-468`), preenchido com 422 e 503 (`:149`).
- `PaymentService.PaymentResult` tem um campo `httpStatus` (`PaymentService.java:14-30`), com 404, 422 e 409 (`:35`, `:40`, `:47`, `:58`, `:66`).

O controller só repassa o código (`api/Routes.java:33`, `:48`, `:81`). As classes de negócio que conhecem a web são `CheckoutService`, `OrderService` e `PaymentService`.
Conclusão: não segue

**A2, os ciclos entre pacotes são acoplamento real?** O relatório traz um grupo, `api` ↔ `services`.
- O sentido `api -> services` é o normal: `api.Routes` chama os seis serviços.
- O círculo fecha por `services -> api`, em três dependências: `CheckoutService -> api.CheckoutRequest`, `OrderService -> api.CheckoutRequest` e `PaymentService -> api.PaymentRequest`.

`CheckoutRequest` e `PaymentRequest` são só dados, classes com campos públicos e sem método nenhum (`api/CheckoutRequest.java`, `api/PaymentRequest.java`). Nenhum serviço chama comportamento de `api`. O laço fecha por tipo de dado e poderia ser desfeito mudando esses tipos de pacote ou traduzindo-os no controller. É leve como ciclo, embora seja o mesmo vazamento que derruba o A1.
Conclusão: em parte

**A3, o controller só traduz?** Não. Em três handlers, `Routes` toma as decisões do ciclo do pedido e mexe direto no estado:
- **Envio** (`api/Routes.java:90-121`): decide a regra "só envia pedido pago" (`:107`), grava o rastreio e a nova situação (`:113-114`) e cria o aviso `ORDER_SHIPPED` (`:116-117`).
- **Entrega** (`:123-143`): decide "só entrega pedido enviado" (`:133`) e muda a situação (`:139`).
- **Cancelamento** (`:145-175`): decide quando pode cancelar (`:155`). Num pedido pago, devolve o estoque item a item (`ProductService.updateStock`, `:164-166`) e estorna o crédito do clube (`CustomerService.updateCreditBalance`, `:167`). Depois muda a situação (`:170`) e gera o aviso (`:171`).

São duas recusas de negócio, três mudanças de situação, uma devolução de estoque e um estorno de crédito, tudo no controller e sem serviço de pedido por trás. Por contraste, `processPayment` (`:74-88`), `checkoutSummary` e `createOrder` (`:28-58`) só chamam o serviço e traduzem o resultado, como deveria ser.
Conclusão: não segue

**A4, o acesso ao banco está isolado?** O isolamento existe em parte:
- **O que funciona como acesso a dados.** `ProductService`, `CustomerService` e `NotificationService` só fazem SQL: cada método é uma consulta ou um `UPDATE` (`services/ProductService.java:18-46`, `services/CustomerService.java:9-51`, `services/NotificationService.java:12-72`). Mesmo com o nome de serviço, fazem o papel de DAO. `PaymentService` não tem SQL e fala com os dados por esses métodos.
- **O que não funciona.** `CheckoutService`, a classe do cálculo e das regras dos cupons, monta SQL e usa JDBC direto em `hasCompletedOrder` (`services/CheckoutService.java:323-335`): pega uma `Connection` do `Database.getDataSource()`, prepara a consulta sobre `orders` e lê o `ResultSet`.
- **Um caso misto.** `OrderService` mistura uma regra (criar o cliente no primeiro pedido, `OrderService.java:15`) com o SQL de inserir e ler os pedidos (`:17-125`), na mesma classe.

Fora do banco, o cliente HTTP da TransLog também mora dentro da classe de cálculo (`CheckoutService.java:388-418`, e `System.getenv("TRANSLOG_URL")` em `:133` e `:357`). Isso não entra na conclusão deste item, mas é outra integração sem isolamento.
Conclusão: em parte

**A5, um cálculo só para o resumo e para o pedido?** Sim. Os dois fluxos fazem a mesma sequência:
- `Routes.checkoutSummary` (`api/Routes.java:30` e `:38`) e `Routes.createOrder` (`:45` e `:53`) chamam `CheckoutService.validateCheckoutRequest` e depois `CheckoutService.calculateSummary`.
- O pedido grava o resumo que recebe (`OrderService.java:32-42`), sem recalcular nada.

Nenhuma etapa é refeita só num dos fluxos. Há repetição dentro do próprio `CheckoutService`, mas ela afeta os dois fluxos do mesmo jeito:
- o subtotal é somado em `:164-169` e em `:200-205`;
- o peso é somado em `:122-126`, `:138-142` e `:340-344`;
- a condição de cada cupom aparece em `validateCouponApplicability` (`:171-184`) e de novo em `calculateCouponDiscount` (`:284-308`);
- a TransLog é consultada na validação (`:147`), no cálculo (`:365`) e mais uma vez no FREESHIPPING (`:303`);
- a validação do boleto roda o cálculo inteiro (`:191`).

Isso é risco de manutenção, mas não é uma segunda cópia do cálculo para um dos fluxos.
Conclusão: segue

**A6, como os pacotes estão organizados?** Por camada. `api` tem as rotas e os objetos da requisição e da resposta (5 classes). `services` tem a regra de negócio misturada com o acesso a dados (6). `database` tem o pool e a criação das tabelas (1). `models` tem as entidades e os enums (7). `utils` tem o arredondamento (1). `App` fica no pacote raiz (`architecture/architecture-report.txt`, "Packages").
Conclusão: não se aplica

## O que mudaria primeiro

1. **Tirar as transições do pedido do controller.** Levaria o envio, a entrega e o cancelamento (a regra de cada transição, a devolução de estoque, o estorno de crédito e o aviso, hoje em `Routes.java:90-175`) para um serviço de pedido, com o mesmo formato que `PaymentService` já usa. O ciclo de vida do pedido fica hoje espalhado entre o controller e o serviço. Sem essa mudança, a futura situação "em separação" ou os novos efeitos do cancelamento teriam de ser escritos dentro do handler HTTP.
2. **Separar a regra de negócio da API.** Faria o controller traduzir `CheckoutRequest` e `PaymentRequest` para tipos do negócio (um comando de checkout com os enums já convertidos e um `Instant` no pagamento). Faria os serviços devolverem um código de recusa do negócio, deixando a escolha do código HTTP para o `Routes`. Isso resolve o A1 e desfaz o ciclo do A2 de uma vez. Junto, levaria `hasCompletedOrder` e o cliente da TransLog para fora do `CheckoutService`.

## Dúvidas

- **No A4, se os `*Service` com SQL são acesso a dados ou negócio.** O nome diz serviço, mas pela regra de julgar pelo que o código faz, tratei `ProductService`, `CustomerService` e `NotificationService` como DAOs. Por isso a conclusão ficou em `em parte` e não em `não segue`. O que pesa é o `CheckoutService`, que é negócio e usa JDBC direto. Se esses três contassem como negócio, o item seria `não segue`.
- **No A6, a palavra da conclusão.** O item é só descritivo, e nenhuma das palavras permitidas descreve uma organização. Usei `não se aplica` para não sugerir um juízo de bom ou ruim.
