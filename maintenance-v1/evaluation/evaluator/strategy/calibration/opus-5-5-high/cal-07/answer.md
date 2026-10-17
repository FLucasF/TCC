# Cenário 01

## Visão geral

O agente fez a mudança sem nenhum pattern. Os três planos novos entraram como constantes a mais no `enum` `RatePlan`, que segue sem comportamento. Os dois `switch` de comportamento da base passaram a cobrir os cinco casos. Entraram também um `if (ratePlan == WEEKEND_PACKAGE)` no cálculo e três `if (request.ratePlan() == …)` de validação no serviço. Para o próximo plano (melhor idade, 60 dias de antecedência) vai ser preciso mexer em quatro lugares de dois arquivos. Isso é justamente o que o pedido anunciava e o Strategy evitaria. O comportamento, porém, está todo certo: os três exemplos do financeiro conferem, as recusas voltam 400 `INVALID_REQUEST` e os planos antigos não mudaram. O problema mais sério é o desenho. Há ainda um ponto menor: o agente não escreveu nenhum teste para os planos novos.

## Item a item

**O pedido (pede o pattern).** `RatePlan` continua um `enum` só de nomes (`final/src/main/java/hotel/reservations/RatePlan.java:3-14`). O desconto ficou num `switch` de comportamento sobre o plano, agora com os cinco casos (`final/src/main/java/hotel/reservations/PriceCalculator.java:40-46`). Logo antes dele, um `if (ratePlan == RatePlan.WEEKEND_PACKAGE)` monta o preço fechado com outro `switch`, este sobre o tipo de quarto (`PriceCalculator.java:32-39`). Os descontos das empresas ficaram como mapa no `PriceCalculator` (`PriceCalculator.java:20-23`). O reembolso é o mesmo `switch` da base (`base/src/main/java/hotel/reservations/ReservationService.java`, método `cancel`), agora com os novos casos (`final/src/main/java/hotel/reservations/ReservationService.java:105-109`). As regras de quem pode usar cada plano ficaram em três `if` sobre o plano dentro de `ReservationService.create`: convênio em `ReservationService.java:51-56`, pacote em `:57-62` e estadia longa em `:63-66`. A validação do convênio ainda consulta `PriceCalculator.COMPANY_DISCOUNTS` diretamente (`:53`). Não existe interface, classe abstrata ou constante de `enum` com corpo próprio. Nenhum plano tem preço, validação e reembolso num lugar só dele, e o cálculo e o cancelamento seguem nomeando os planos. É exatamente o caso que as instruções chamam de omissão: os `switch` cresceram para os cinco casos e entraram `if (ratePlan == …)` no serviço e no cálculo.
Conclusão: omissão

**O exagero colateral.** Não há. A única coisa nova fora dos planos é o campo `companyCode`, que o pedido exigia: ele entra em `final/src/main/java/hotel/web/CreateReservationRequest.java:16`, em `final/src/main/java/hotel/reservations/NewReservation.java:6-7` e é repassado em `final/src/main/java/hotel/web/HttpApi.java:128-129`. O agente não criou interface, fábrica, builder nem camada nova.
Conclusão: contenção

**O comportamento.** Testei uma cópia da `final/`: compilei, subi na porta 7070 e chamei a API com `curl`.
- **Convênio.**
  - Tecnosul, deluxe 201 de 10/03 a 12/03/2027: subtotal 779,80, desconto 93,58, total 686,22, como no pedido.
  - Agrovale, standard, 1 noite: desconto 44,98 (18% de 249,90).
  - Farmavida, standard, 1 noite: desconto 62,48 (25% de 249,90 = 62,475, que arredonda meio para o par para 62,48).
  - Sem código, com `ACME` e com `tecnosul` em minúsculas: 400 `INVALID_REQUEST`.
- **Pacote.**
  - Deluxe de sexta 12/03 a domingo 14/03/2027: 820,00, sem desconto. Confirmei e cancelei: reembolso 410,00, como no pedido.
  - Standard em julho, que é alta temporada: 520,00. Suíte em julho: 1.200,00, e o cancelamento depois de pago devolveu 600,00.
  - Entrada num sábado, ou sexta com 3 noites: 400 `INVALID_REQUEST`.
- **Estadia longa.**
  - Standard de 01/03 a 08/03/2027: 1.749,30, desconto 174,93, total 1.574,37, como no pedido.
  - 7 noites em julho: 2.099,16 de diárias (a temporada vale) e desconto 209,92.
  - 6 noites: 400 `INVALID_REQUEST`.
- **Reembolsos.** Convênio e estadia longa devolveram tudo o que foi pago (686,22 e 1.574,37). Pacote pendente cancelado devolveu 0,00.
- **O que não devia mudar.**
  - `STANDARD` com `companyCode` enviado: o código é ignorado, desconto 0,00, e o cancelamento devolve 779,80.
  - `NON_REFUNDABLE` de 1 noite: desconto 37,48, reembolso 0,00.
  - Suíte `STANDARD` de 14/12 a 16/12/2027: 1.275,78, com a temporada a partir do dia 15.
  - Relatório de receita (2027-03 e 2027-07), ocupação, segunda via do comprovante e erro de campo faltando: tudo coerente.
- **Testes.** O `mvn -q verify` da cópia passou: 26 testes, 0 falhas. São exatamente os 26 da base, só ajustados para o parâmetro novo; o agente não escreveu nenhum teste para os planos novos.
- No fim, derrubei o serviço.

Conclusão: segue

## Dúvidas

Uma, pequena, que não muda nenhuma conclusão. As regras dos planos são conferidas depois da disponibilidade do quarto (`final/src/main/java/hotel/reservations/ReservationService.java:44-50` antes de `:51-66`). Assim, um pedido que fura a regra do plano num quarto ocupado recebe 409 `ROOM_UNAVAILABLE` em vez de 400. Testei uma `LONG_STAY` de 2 noites no quarto 102, já reservado, e veio 409. O pedido não define qual erro vem primeiro, então não contei isso como falha de comportamento. O README também não foi atualizado com os planos novos nem com o `companyCode`; isso fica de fora da classe.
