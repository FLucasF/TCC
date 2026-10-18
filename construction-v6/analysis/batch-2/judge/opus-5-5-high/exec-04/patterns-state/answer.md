# padrões — State

## Visão geral

O agente não usou o padrão State. `OrderStatus` (`src/main/java/com/clothing/model/OrderStatus.java:3-17`) é um enum que só dá nome às cinco situações. Ele não diz quais operações cada situação permite nem para onde ela vai. O único código dele, o `fromString`, traduz o texto para a constante, e isso não conta. Todas as decisões sobre a situação ficam em `if`s dentro das rotas do Javalin (`OrderRoutes.java`), mais algumas no `CheckoutService` e uma num SQL do `CustomerRepository`. O problema mais sério é o P7: criar a situação "em separação" que o enunciado anuncia obrigaria a mexer em pelo menos quatro lugares fora do tipo da situação. O segundo é o remendo do cancelamento: a diferença entre cancelar um pedido pago e um não pago é um `if (status == PAID)` dentro da rota. Nos avisos ao cliente, o agente acertou: usou um campo de texto e um `UPDATE`, sem estrutura a mais.

## Item a item

**P7, as decisões sobre a situação moram na própria situação?** Nenhuma decisão está dentro de `OrderStatus`. O tipo só tem as constantes (`OrderStatus.java:4-8`) e a tradução do texto (`OrderStatus.java:10-17`), que não contam. As decisões que ficam fora dele:
- **Pagar:** a rota exige `AWAITING_PAYMENT` (`OrderRoutes.java:136`).
- **Enviar:** a rota exige `PAID` (`OrderRoutes.java:201`).
- **Entregar:** a rota exige `SHIPPED` (`OrderRoutes.java:231`).
- **Cancelar:** a rota recusa um conjunto que não cita todas as situações, `SHIPPED || DELIVERED` (`OrderRoutes.java:260`). Como a regra mora fora do tipo, o `CANCELLED` ficou de fora: um pedido já cancelado pode ser cancelado de novo.
- **Destino de cada transição:** quem escolhe é sempre quem chama: `PAID` em `CheckoutService.java:234`, `SHIPPED` em `OrderRoutes.java:208`, `DELIVERED` em `OrderRoutes.java:237` e `CANCELLED` em três lugares diferentes (`OrderRoutes.java:155`, `OrderRoutes.java:279`, `CheckoutService.java:227`).
- **O que muda em cada transição:** também mora em quem chama. A coluna de data de cada passo vai como texto solto: `"paid_at"`, `"shipped_at"`, `"delivered_at"` (`CheckoutService.java:234`, `OrderRoutes.java:208`, `OrderRoutes.java:237`).
- **Situações que contam como compra:** para o WELCOME10, o conjunto `status IN ('PAID', 'SHIPPED', 'DELIVERED')` está no SQL (`service/CustomerRepository.java:85`).

Com uma situação "em separação", teriam de mudar `OrderRoutes.java:201`, `:260` e `:268` e `CustomerRepository.java:85`, e não o tipo da situação. A situação é só um rótulo, com as decisões em `if`s nos serviços e no controller.
Conclusão: omissão

**P7-remendo, cancelar um pago e cancelar um não pago.** A diferença está num `if (order.status() == OrderStatus.PAID)` dentro da rota de cancelamento (`OrderRoutes.java:268-277`). Esse bloco devolve o estoque (`:269-271`) e tira o crédito do saldo (`:273-276`). A situação não diz se há o que devolver: quem decide é o controller. Não existe estrutura de situação onde o remendo pudesse ter ficado de fora; a regra foi escrita direto no `if` que o enunciado induz. Por isso a classificação é omissão, e não malfeito. Os outros dois cancelamentos (o pagamento atrasado, em `CheckoutService.java:225-229`, e a falta de estoque, em `OrderRoutes.java:152-160`) repetem por conta própria o par "muda para `CANCELLED` + cria o aviso".
Conclusão: omissão

**P7-exagero, a situação do aviso.** O status do aviso é um `String` no record (`model/Notification.java:11`). Ele nasce `'NEW'` no `INSERT` (`service/CustomerRepository.java:158`) e passa a `'READ'` com um `UPDATE notifications SET status = 'READ'` (`service/CustomerRepository.java:146`). A regra "se já estava lido, continua lido" sai de graça desse `UPDATE`. Não há enum, classe por situação nem máquina de estados para o aviso. É um campo com uma troca simples.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto deste pattern não encontrei estrutura sem necessidade. O projeto não tem nenhuma interface nem classe abstrata (a busca por `interface`/`abstract`/`implements` não acha nada, e o único `extends` é a exceção em `service/TransLogClient.java:69`). Os repositórios são classes concretas criadas com `new` (`OrderRoutes.java:15-18`). `CheckoutService.registerOrder` (`CheckoutService.java:216-220`) faz três coisas, então não é uma camada que só repassa.
Conclusão: contenção

## O que mudaria primeiro

1. **Pôr as transições em `OrderStatus`.** Cada constante diria o que permite e para onde vai (`canPay()`, `canShip()`, `canCancel()`, `next(...)`, ou um conjunto de destinos por constante), e as rotas só perguntariam ao status atual. Isso tira os quatro `if`s de `OrderRoutes.java` (`:136`, `:201`, `:231`, `:260`). Com isso, a situação "em separação" passaria a ser uma constante nova, sem mexer nas rotas. De quebra, também se corrigiria o `CANCELLED` que hoje pode ser cancelado de novo.
2. **Pôr na situação a resposta "há o que devolver?"** (por exemplo, `PAID.refundsOnCancel()`). Essa resposta substituiria o `if (status == PAID)` de `OrderRoutes.java:268`. O mesmo vale para o conjunto "conta como compra" do SQL de `CustomerRepository.java:85`: ele viraria uma pergunta à situação (`countsAsPurchase()`), e o SQL seria montado a partir das situações que respondem sim.

## Dúvidas

- Contei como decisão sobre a situação o filtro `status IN ('PAID', 'SHIPPED', 'DELIVERED')` (`CustomerRepository.java:85`). Ele está num SQL e não no código Java, mas é um conjunto que não cita todas as situações e decide uma regra de negócio (a primeira compra), e por isso entra no P7.
- No remendo, escolhi `omissão` em vez de `malfeito`, porque não há estrutura de situação de que a regra tenha ficado de fora.
