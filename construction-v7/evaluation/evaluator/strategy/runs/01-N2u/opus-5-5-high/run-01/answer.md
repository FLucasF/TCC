# Cenário 01

## Visão geral

O agente montou um Strategy de verdade para os planos de tarifa. Há uma interface `RatePlan` com três operações (validar, calcular o preço, calcular a devolução) e uma classe para cada um dos cinco planos, o normal e o não reembolsável inclusive. Um único `switch` na fábrica escolhe a classe. A validação, o preço e a devolução de cada plano moram na classe dele, e o cancelamento pede a devolução ao plano sem saber qual é. O problema mais sério está no convênio. A interface não leva o código da empresa para o cálculo do preço, e o agente contornou isso no serviço: ali o código pergunta se o plano é `CORPORATE` e faz `instanceof CorporateRatePlan` para chamar um método que só essa classe tem. O `calculatePrice` do contrato, chamado nesse plano, devolve desconto zero. Quem usa a estrutura continua perguntando qual é o plano concreto, por isso a classe é `malfeito`. As regras da parte do fim funcionam todas pela API. Já a parte comum tem falhas reais: janeiro e fevereiro não contam como alta temporada, uma saída antes da entrada é aceita e vários erros de dados viram 500.

## Como foi implementado, e por que o julgamento

**O projeto.** O código fica em `com.pousada` e se divide em quatro pacotes:

- `api`: controladores Javalin e DTOs em `record`;
- `service`: `ReservationService`, `RoomService`, `MessageService` e `ReportService`;
- `domain`: `Reservation`, `Room`, os enums `RoomType`, `RatePlanType` e `ReservationStatus`, `PriceCalculation`, `ValidationResult`, e o subpacote `rateplan`;
- `util`: datas, dinheiro e o formatador do comprovante e do e-mail.

`Application.main` monta tudo à mão, sem injeção (`Application.java:26-34`). Ele também registra as rotas (`Application.java:44-57`) e traduz exceções em `{error, message}` (`Application.java:59-92`).

Uma criação de reserva segue este caminho. `ReservationController.createReservation` desserializa o corpo num `CreateReservationRequest` e chama `ReservationService.createReservation` (`ReservationController.java:22-34`). O serviço busca o quarto, conta as noites e verifica hóspedes e capacidade (`ReservationService.java:29-40`). Depois pede o plano à fábrica, valida pelo plano, confere a disponibilidade e calcula o preço (`ReservationService.java:42-58`).

Os dados ficam só na memória, num `HashMap` dentro do serviço (`ReservationService.java:16`). Os quartos estão fixos em `RoomService.java:13-19`, e as mensagens enviadas ficam numa lista em `MessageService.java:11`. O jar sobe sozinho graças ao shade com `mainClass` (`pom.xml`), e a porta vem de `PORT` (`Application.java:98-101`).

O agente escreveu só uma classe de teste, `RatePlanTest`, com 4 testes de unidade sobre os planos. Os testes conferem os quatro exemplos do enunciado (599,76, 686,22, 820/410 e 1.574,37). Não há teste de API.

**A parte do fim.**

- **O contrato.** `RatePlan` declara `validate(checkIn, checkOut, nights, companyCode)`, `calculatePrice(room, checkIn, checkOut, nights)` e `calculateRefund(amountPaid)` (`RatePlan.java:12-16`).
- **STANDARD.** `StandardRatePlan` não tem restrição de uso e devolve tudo. Ele guarda a soma das noites com temporada num método protegido, `calculateSubtotal` (`StandardRatePlan.java:33-50`), que três subclasses reaproveitam por herança.
- **NON_REFUNDABLE.** `NonRefundableRatePlan` dá 15% e devolve zero (`NonRefundableRatePlan.java:16`, `:24`).
- **LONG_STAY.** `LongStayRatePlan` exige 7 noites, dá 10% e devolve tudo (`LongStayRatePlan.java:15`, `:25`, `:33`).
- **WEEKEND_PACKAGE.** `WeekendPackageRatePlan` exige 2 noites, de sexta a domingo, cobra um preço fechado por tipo de quarto num mapa e devolve metade (`WeekendPackageRatePlan.java:16-20`, `:23-37`, `:50`).
- **CORPORATE.** `CorporateRatePlan` valida o código numa tabela (`CorporateRatePlan.java:14-31`) e devolve tudo (`:49-51`). O preço com desconto, porém, está num método fora do contrato, `calculatePriceWithCompany` (`:38-46`). O `calculatePrice` da interface chama esse método com `companyCode = null` e, portanto, com 0% (`:34-36`).
- **A escolha.** `RatePlanFactory.create` tem o único `switch`, e ele só escolhe a implementação (`RatePlanFactory.java:8-14`). As instruções aceitam isso.
- **O cancelamento.** Ele é polimórfico: `RatePlanFactory.create(reservation.getRatePlanType()).calculateRefund(...)` (`ReservationService.java:91-92`).
- **A criação.** O desvio está aqui: `if (ratePlanType == RatePlanType.CORPORATE && ratePlan instanceof CorporateRatePlan corporatePlan)` chama `calculatePriceWithCompany`, e todos os outros planos passam pelo `calculatePrice` do contrato (`ReservationService.java:54-58`).

**O raciocínio.** Muita coisa aproxima o desenho do acerto. Os cinco planos estão na estrutura, a validação de cada um (sexta a domingo, 7 noites, código da empresa) está dentro do plano e não em `if` no serviço, a devolução é polimórfica, e a seleção acontece num único ponto. O que o afasta é que o contrato não basta para um dos planos. O serviço nomeia o plano e desce ao tipo concreto para pedir o preço, que é exatamente o caso das instruções em que "quem usa a estrutura ainda pergunta qual é o caso concreto". As consequências são concretas:

- `CorporateRatePlan.calculatePrice`, chamado pela interface, cobra sem desconto;
- o próprio teste do agente precisa chamar o método específico (`RatePlanTest.java:38`);
- um plano futuro que precise de outro dado da reserva (a data da reserva, para os 60 dias de antecedência, ou a idade, para a melhor idade) vai cair no mesmo padrão: mudar a assinatura ou acrescentar outro `if`/`instanceof` no serviço.

Por isso a classe é `malfeito`, e não `acerto`.

Quanto ao exagero colateral, fora dos planos não há interface com uma implementação só, fábrica, builder nem camada que só repassa. A `RatePlanFactory` é o ponto de seleção do próprio pattern. Os serviços têm lógica real, e os DTOs são o mapeamento normal para o JSON.

No comportamento, a parte do fim confere toda pela API. A parte comum falha em vários pontos:

- **A temporada.** `DateUtils.isHighSeason` monta o intervalo de 15 de dezembro do ano da data até fevereiro do ano seguinte, e por isso janeiro e fevereiro nunca contam como temporada (`DateUtils.java:16-25`).
- **As datas.** Não há verificação de saída depois da entrada (`ReservationService.java:32-36`).
- **Campos que faltam.** Viram NPE e caem no 500 genérico (`Application.java:83-91`).
- **O dinheiro no texto.** O comprovante usa `NumberFormat` de moeda pt-BR (`ReceiptFormatter.java:14`, `:64-65`), que põe um espaço não separável (U+00A0) depois de "R$".

## Item a item

**O enunciado (pede o pattern).** A interface `RatePlan` (`RatePlan.java:10-17`) tem uma implementação por plano, os cinco inclusive (`StandardRatePlan.java:12`, `NonRefundableRatePlan.java:11`, `CorporateRatePlan.java:12`, `WeekendPackageRatePlan.java:14`, `LongStayRatePlan.java:11`). A seleção acontece num `switch` só de escolha (`RatePlanFactory.java:8-14`). A validação é delegada (`ReservationService.java:44-47`) e a devolução também (`ReservationService.java:91-92`). Mas o cálculo do preço na criação nomeia o plano e testa o tipo concreto (`ReservationService.java:54-58`), porque o contrato não leva o código da empresa e o preço com desconto do convênio vive fora da interface (`CorporateRatePlan.java:34-46`). A estrutura existe e convive com o jeito sem ela, porque o cliente ainda pergunta qual é o plano.
Conclusão: malfeito

**O exagero colateral.** Não há. Fora dos planos, os serviços (`ReportService`, `RoomService`, `MessageService`) têm lógica própria, os controladores fazem o trabalho HTTP e os DTOs (`api/dto/*.java`) fazem o mapeamento do JSON. Não há interface com uma implementação só, fábrica, builder nem camada que só repassa. A `RatePlanFactory` faz parte do pattern.
Conclusão: contenção

**O comportamento.** O build (`mvn -q package -DskipTests`) gerou `target/mare-alta-1.0.0.jar`. Subi o serviço com `PORT=7070` e chamei a API com `curl`.

A parte do fim confere:

- **Convênio.** TECNOSUL num deluxe de 10/03 a 12/03/2027 deu 779,80 / 93,58 / 686,22. AGROVALE deu 140,36 de desconto (18%) e FARMAVIDA, numa suíte, deu 289,95 (25%). Sem código, com `ACME` e com `tecnosul` minúsculo, a resposta foi 400 `INVALID_REQUEST`.
- **Pacote.** Deluxe de 12/03 a 14/03 saiu 820,00 e, pago e cancelado, devolveu 410,00. Standard em julho saiu 520,00 (sem temporada) e a suíte, 1.200,00. Sábado→segunda e quinta→domingo deram 400.
- **Estadia longa.** Standard de 01/03 a 08/03 deu 1.749,30 / 174,93 / 1.574,37. De 28/06 a 05/07 a temporada valeu nas noites de julho (1.949,22 / 194,92 / 1.754,30). Com 6 noites, 400.
- **Devoluções.** Convênio e estadia longa devolveram tudo, o não reembolsável devolveu 0, e o pacote pendente cancelado devolveu 0,00. O `companyCode` num STANDARD foi ignorado.

A parte comum confere em boa parte:

- o STANDARD de 10/07 a 12/07/2027 deu 599,76;
- conflito de quarto deu 409, capacidade deu 422, quarto inexistente deu 404, e uma transição inválida deu 409;
- a segunda via de uma reserva pendente foi recusada e o check-out devolveu o comprovante;
- o e-mail saiu com o assunto e o texto pedidos;
- os CSVs vieram com cabeçalho, `total,1/5`, ordem certa, o não reembolsável cancelado contando no financeiro e `text/csv; charset=utf-8`.

Mas falha em vários pontos:

- **Temporada.** Um STANDARD de 10/01 a 12/01/2027 deu 499,80, quando o certo era 599,76, e 10/02/2027 deu 249,90 sem o acréscimo (`DateUtils.java:16-25`).
- **Datas da estadia.** Uma saída antes da entrada foi criada com `nights: -2` e total 0,00, e uma saída igual à entrada também foi aceita.
- **Campos e formatos.** Sem `guests`, sem `ratePlan`, sem `checkIn`, com data `01/09/2027` ou com plano inexistente, a resposta foi 500 `INTERNAL_ERROR`. Sem `guestName`, a reserva foi criada com nome nulo.
- **Dinheiro no comprovante.** O valor aparece como `R$\u00A0820,00`, com espaço não separável.
- **Duas casas no JSON e no CSV.** `amountPaid` aparece como `0` antes da confirmação, o `refund` do não reembolsável como `0`, e o financeiro de um mês vazio termina em `total,0`.

O `mvn -q verify` da cópia passou, com 4 testes, 0 falhas e 0 erros. Derrubei o serviço no fim e confirmei que a porta 7070 ficou fechada.
Conclusão: em parte

## Dúvidas

- A gravidade do `instanceof` decidiu a classe. Ele é um só, localizado e restrito ao preço do convênio, e todo o resto do Strategy está correto. Mesmo assim, é exatamente o caso que as instruções descrevem como `malfeito` (o cliente pergunta qual é o caso concreto), e ele deixa o `calculatePrice` do contrato errado para esse plano. Por isso não dei `acerto`.
- `ReservationService.java:63` guarda o `companyCode` em toda reserva, qualquer que seja o plano. Como o código não é usado fora do convênio, não muda nada observável nem a classe.
- Os erros da temporada (janeiro e fevereiro) também afetam a estadia longa nesses meses, porque ela reaproveita `calculateSubtotal`. Não testei uma estadia longa em janeiro, mas pela leitura do código o resultado seria o mesmo erro.
