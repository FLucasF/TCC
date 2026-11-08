# Revisão: manutenção, cenário 01

## Visão geral

O projeto (vindo da base) tem sete pacotes: `hotel` (a montagem, `Hotel.java:25`), `hotel.web` (as rotas do Javalin em
`HttpApi`), `hotel.reservations` (o serviço, o cálculo do preço, o plano de tarifa e o `ReservationStore` com o JDBC),
`hotel.rooms`, `hotel.documents`, `hotel.messages` e `hotel.reports`. Uma requisição entra em `HttpApi`, vira um
`NewReservation` (`HttpApi.java:128-130`), passa pelo `ReservationService.create` (`ReservationService.java:27-49`),
que pede ao plano para validar (`:34`) e ao `PriceCalculator` o preço (`:47`), e é gravada no PostgreSQL por
`ReservationStore` (esquema por migrações Flyway). A mudança do agente é pequena e focada: transformou o `enum RatePlan`
num Strategy (cada constante com o seu corpo para preço, validação e devolução), criou o `enum Company` com os
descontos do convênio, acrescentou o `companyCode` do pedido até o banco com uma migração nova (`V2__add_company_code.sql`)
e escreveu 7 testes novos. Os 72 pontos do roteiro de comportamento passam. Não há defeito sério; o único ponto a
observar é que o `companyCode` é gravado e devolvido também nos planos que não são o convênio.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), igual à base
(`measures/dependencies-base.md`: mesmos 7 pacotes, mesmas 17 dependências, mesmo ciclo). `hotel.reservations` passou de
13 para 17 classes e de 508 para 646 linhas; a maior classe continua `HttpApi.java` (171 linhas, 170 na base).
**Nota:** bom

O agente não criou dependência nova entre pacotes: `RatePlan` importa `hotel.rooms.Room` (`RatePlan.java:3`), e
`hotel.reservations -> hotel.rooms` já existia na base. As regras novas ficaram no pacote das regras, sem Javalin e sem
SQL (`RatePlan.java`, `Company.java`); a borda HTTP só ganhou o repasse do campo (`HttpApi.java:129-130`,
`CreateReservationRequest.java:16`, `ReservationResponse.java:19,39`); o SQL novo ficou no `ReservationStore`
(`ReservationStore.java:31-33,44,132-133`). A montagem continua num lugar só (`Hotel.java:25`). O ciclo
`documents` ↔ `reservations` veio da base e o agente não o piorou.

## Design de baixo nível

**Números:** 5 planos (os 2 da base e os 3 novos), os 5 com o comportamento dentro da própria constante do `enum`
(STANDARD pelos padrões da classe, `RatePlan.java:82-93`; os outros quatro com corpo próprio, `RatePlan.java:15-79`);
0 `switch`/`if` sobre o plano fora do `enum` (os dois `switch` da base, em `PriceCalculator.calculate` e
`ReservationService.cancel`, foram removidos: `PriceCalculator.java:26`, `ReservationService.java:83`). Os três pontos
de variação (preço, validação, devolução) estão no plano.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

O pattern: o `enum` como estratégia, que a referência aceita como acerto. `RatePlan` declara os três comportamentos com
o padrão do plano normal (`validate` vazio, `price` sem desconto, `refund` devolvendo tudo, `RatePlan.java:82-93`) e cada
constante sobrescreve só o que muda: NON_REFUNDABLE o preço e a devolução (`:15-25`), CORPORATE a validação e o preço
(`:27-40`), WEEKEND_PACKAGE os três (`:42-65`), LONG_STAY a validação e o preço (`:67-79`). Quem usa pede ao plano sem
nomear nenhum: o cálculo (`PriceCalculator.java:25-26`, que entrega ao plano o preço noite a noite com a temporada), a
criação (`ReservationService.java:34`) e o cancelamento (`ReservationService.java:83`). Os dois planos da base entraram
na estrutura também. O `switch (room.type())` em `RatePlan.java:53-57` escolhe o preço fechado por tipo de quarto, não
o plano, e não afasta a classe. O próximo plano (melhor idade, 60 dias de antecedência) é uma constante nova sem mexer
em outro lugar.

O exagero: nada além do necessário. `Company` (`Company.java`) é um `enum` simples com o desconto e uma busca pelo
código; `Price.withDiscount` (`Price.java:7-9`) tira a repetição do "subtotal menos desconto" dos três planos com
desconto. Não há interface com uma implementação só, fábrica ou camada de repasse.

O resto do código: dinheiro em `BigDecimal` com o mesmo `cents` (meio para o par) da base em cada passo; datas em
`LocalDate`; o erro de plano é o `HotelException(INVALID_REQUEST)` que a API já traduz (`RatePlan.java:95-97`).
Pontos menores, que não pedem mudança antes de aceitar: as taxas viraram literais dentro dos métodos
(`new BigDecimal("0.15")`, `"0.10"`, `"0.5"` em `RatePlan.java:18,63,77`), onde a base tinha uma constante com nome
(`NON_REFUNDABLE_DISCOUNT`, removida de `PriceCalculator.java`); `Company.find` roda duas vezes por reserva corporativa
(`RatePlan.java:30,37`); e o `companyCode` passa sem filtro para qualquer plano (`HttpApi.java:129-130` →
`Reservation.java:39` → `ReservationStore.java:44` → `ReservationResponse.java:39`), o que contradiz o próprio comentário
de `NewReservation.java:5` ("it is null otherwise"): um STANDARD que mande `companyCode` tem o preço certo, mas grava e
devolve o código.

## Testes

**Números:** 33 testes rodaram, 0 falhas, 0 erros, 0 pulados; 33 métodos escritos (a base tinha 26, todos mantidos e
passando). Cobertura de linhas 94,4% (520 de 551; base 93,8%), de ramos 88,2% (112 de 127; base 87,1%);
`hotel.reservations` com 256 de 269 linhas e 60 de 63 ramos.
**Nota:** bom

Os 7 testes novos conferem a parte nova com valores, e todos os exemplos do financeiro estão lá: Tecnosul 779,80 /
93,58 / 686,22 (`PriceCalculatorTest.java:82-92`), pacote deluxe 820,00 em alta temporada e 520,00 / 1.200,00 nos outros
quartos (`:95-108`), estadia longa 1.749,30 / 174,93 / 1.574,37 (`:111-118`) e com a temporada valendo (`:121-128`). No
serviço, contra o banco: o convênio sem código, com código em minúsculas e com código desconhecido é recusado com
`INVALID_REQUEST`, o código volta do banco e a devolução é total (`ReservationServiceTest.java:170-181`); o pacote
recusado numa quinta, com 3 noites e com 1 noite, e devolvendo 410,00 (`:184-197`); a estadia longa recusada com 6
noites e aceita com 7 (a fronteira), devolvendo tudo (`:200-209`). Os testes da base foram só adaptados à assinatura nova
do `calculate`, sem perder asserção.

A pergunta da mutação: trocar 0.12 por 0.21, 0.10 por 0.01, 0.5 por 1, `FRIDAY` por outro dia ou `plusDays(7)` por
`plusDays(6)` derruba algum teste. Ficam com rede fraca: os 18% e 25% são conferidos só lendo a constante do `enum`
(`PriceCalculatorTest.java:90-91`), não num preço calculado (trocar o desconto do CORPORATE para um valor fixo de 12%
ainda falharia pelo teste do código, mas o uso do desconto da empresa certa só é conferido pela Tecnosul); e o campo
`companyCode` pela API (o JSON, o 400 pela HTTP) não tem teste, porque `HttpApiTest` não foi tocado. São lacunas pequenas.

## Banco

**Números:** 3 tabelas, todas com chave primária; 2 migrações Flyway (a `V1__create_tables.sql` da base igual, a
`V2__add_company_code.sql` nova); dinheiro todo em `numeric(10,2)`; nenhuma chave estrangeira sem índice; nenhuma data em
texto; colunas de situação/plano sem CHECK (`reservations.rate_plan`, `status`, `rooms.type`, `messages.channel`, todas
da base); o EXCLUDE que impede duas reservas na mesma noite continua (`V1__create_tables.sql:39`).
**Nota:** bom

O agente evoluiu o esquema do jeito certo: uma migração nova, sem editar a da base (`V2__add_company_code.sql:2`, um
`ALTER TABLE ... ADD COLUMN company_code text`, que funciona num banco que já tem reservas, porque a coluna é nula para
elas). O SQL novo é com parâmetro (`ReservationStore.java:31-33,44`) e a leitura usa a coluna nova
(`ReservationStore.java:132-133`); os `+` que o script lista só concatenam constantes. A coluna `company_code` não tem
CHECK nem ligação com o plano (nada impede, no banco, um CORPORATE sem código ou um STANDARD com código), e o
`rate_plan` sem CHECK veio da base; nenhum dos dois perde nem corrompe dado, porque o código só grava planos do `enum` e
valida o código do convênio antes de gravar.

## Comportamento

**Números:** 72 de 72 conferências passaram: parte comum 46 de 46, dados depois de reiniciar 3 de 3, cenário 01 21 de
21, duas reservas ao mesmo tempo 2 de 2.
**Nota:** bom

Nada falhou. Para o dono da pousada: os três planos novos cobram e devolvem o que o financeiro conferiu (686,22 no
convênio da Tecnosul, 639,44 e 584,85 na Agrovale e na Farmavida, 820,00 no pacote deluxe com 410,00 de volta, 520,00
e 1.200,00 nos outros quartos, 820,00 também em julho, 1.574,37 na estadia longa e 1.754,30 com noites de temporada);
as recusas pedidas saem como 400 `INVALID_REQUEST` (convênio sem código ou com código desconhecido, pacote fora de
sexta a domingo, estadia com 6 noites); o `companyCode` não muda o preço do STANDARD; e o normal e o não reembolsável
seguem como antes. O que o roteiro não confere: o código mandado num plano que não é o convênio é guardado e aparece na
resposta (ver o design); o preço sai certo, então não pesa aqui.

## Dúvidas

- O texto diz que, nos outros planos, o `companyCode` "pode ignorar". O agente ignora no preço, mas grava e devolve o
  código (`HttpApi.java:129-130`, `ReservationStore.java:44`, `ReservationResponse.java:39`). Li isso como um detalhe
  menor, que não muda a classe nem a nota; um revisor mais rígido poderia pedir que o código só fosse guardado no
  convênio.
