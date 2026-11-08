# Revisão: manutenção, cenário 01

## Visão geral

O projeto vem da base e o agente não mexeu na forma dele. Os pacotes são `hotel` (a montagem), `hotel.web` (Javalin), `hotel.reservations` (as regras e o acesso ao banco), `hotel.rooms`, `hotel.messages`, `hotel.documents` e `hotel.reports`. Uma requisição entra por `hotel/web/HttpApi.java`, que lê o JSON e monta um `NewReservation` (`HttpApi.java:109-136`). Daí vai para `ReservationService`, que valida, pede o preço ao `PriceCalculator` e grava pelo `ReservationStore` (JDBC, PostgreSQL, migrações do Flyway). Para os três planos novos, o agente acrescentou casos aos `switch` sobre o `RatePlan` que já existiam e criou mais dois `switch`. Ficaram quatro lugares que decidem pelo plano, em duas classes: `PriceCalculator.java:39-42` e `:52-58`, e `ReservationService.java:53-74` e `:125-129`. A regra de cada plano (preço, quem pode usar, devolução) está espalhada, e o texto avisa que logo vêm mais planos. Isso é exatamente o que o pedido queria evitar, e é o ponto mais sério. O comportamento está todo certo (72 de 72), os 50 testes passam e o banco evoluiu com uma migração nova.

## Arquitetura

**Números:** 7 pacotes, 17 dependências e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais aos da base (`measures/dependencies-base.md`). `hotel.reservations` passou de 508 para 611 linhas. `ReservationService` passou de 138 para 182 linhas e agora é a maior classe.
**Nota:** bom

O agente respeitou as camadas que já existiam. A borda HTTP só lê o campo novo e o repassa (`hotel/web/HttpApi.java:135`, `hotel/web/CreateReservationRequest.java:16`). As regras de quem pode usar cada plano ficaram no serviço, que não conhece Javalin nem SQL (`hotel/reservations/ReservationService.java:52-91`). O SQL da coluna nova ficou no `ReservationStore` (`ReservationStore.java:19`, `:32`, `:50`, `:133`). O ciclo `documents` ↔ `reservations` já existia na base e o agente não o piorou. Há um acoplamento novo, mas pequeno e dentro do mesmo pacote: o serviço consulta a tabela de descontos do calculador para validar o código da empresa (`ReservationService.java:59` lê `PriceCalculator.CORPORATE_DISCOUNTS`). A lista de empresas mora numa classe e quem a usa para validar está em outra. Isso pesa mais no design do que na arquitetura. O serviço cresceu cerca de 30%, mas continua num tamanho razoável.

## Design de baixo nível

**Números:** dos 5 planos, nenhum tem comportamento próprio. `RatePlan` continua um `enum` sem corpo (`RatePlan.java:3-14`). Há 4 `switch` sobre o plano: a escolha do cálculo (`PriceCalculator.java:39-42`), o desconto (`PriceCalculator.java:52-58`), a validação (`ReservationService.java:53-74`) e a devolução (`ReservationService.java:125-129`). Os 5 casos estão fora de qualquer estrutura.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto pede, sem dizer o nome, o Strategy: cada plano com o seu preço, a sua regra de uso e a sua devolução num lugar só dele, porque "o comercial vive inventando plano". O agente fez o contrário. O `switch` do desconto e o da devolução, que a base já tinha, cresceram para cinco casos (`PriceCalculator.java:52-58`, `ReservationService.java:125-129`). Surgiram mais dois `switch` de comportamento: um que desvia o pacote para outro cálculo (`PriceCalculator.java:39-42`) e um de validação no serviço (`ReservationService.java:53-74`). A referência classifica isso como omissão: são `switch` sobre o plano no preço, no cancelamento e na validação. Um plano para a melhor idade, por exemplo, obriga a mexer em pelo menos três métodos de duas classes. Os sintomas aparecem no próprio código. O `switch` do desconto tem um caso morto, `case WEEKEND_PACKAGE -> cents(BigDecimal.ZERO); // handled separately` (`PriceCalculator.java:57`), que só existe porque o `switch` precisa cobrir todos os casos. A JaCoCo confirma que essa linha nunca roda. E a regra do convênio está repartida entre a tabela de descontos (`PriceCalculator.java:25-28`) e a validação que a consulta (`ReservationService.java:59`).

**O exagero.** Não há estrutura sem necessidade: o agente não criou interface, fábrica nem camada nova. Contenção.

**O resto do código.** São defeitos localizados:
- O `null` está tratado de duas maneiras. O código da empresa é `Optional<String>` em `NewReservation.java:19`, mas vira `String` nulo em `Reservation.java:39`, em `ReservationService.java:47` e no calculador. A sobrecarga pública `calculate(room, in, out, ratePlan)` passa `null` adiante (`PriceCalculator.java:35`). Com `CORPORATE`, isso cai em `CORPORATE_DISCOUNTS.get(null)` (`PriceCalculator.java:55`), e o `Map.of` lança `NullPointerException`. Hoje nada chega lá sem passar pela validação do serviço, mas o calculador depende de alguém ter validado antes.
- `calculateWeekendPackage` recebe `checkIn` e `checkOut` e não usa nenhum dos dois, e escreve 2 noites fixas (`PriceCalculator.java:62-64`).
- O import `java.time.DayOfWeek` não é usado (`PriceCalculator.java:7`).
- Há nomes completos no meio do código em vez de imports: `java.time.temporal.ChronoUnit` e `java.time.DayOfWeek` (`ReservationService.java:67`, `:78-82`) e `java.util.Optional` (`HttpApi.java:135`).
- O cálculo das noites está repetido em `ReservationService.java:67` e `:80`, e também no laço do calculador.
- A metade da devolução é um número solto: `new BigDecimal("0.5")` em `ReservationService.java:128`, enquanto os outros percentuais são constantes nomeadas.
- O código da empresa é gravado em qualquer plano (`HttpApi.java:135`, `ReservationStore.java:50`). O pedido diz que, fora do convênio, ele pode ser ignorado. O preço o ignora, mas o dado fica guardado.

`BigDecimal` com arredondamento em centavos e `LocalDate` continuam usados direito.

## Testes

**Números:** 50 testes rodaram, 0 falharam, 0 foram pulados e há 50 métodos escritos. A base tinha 26, então o agente acrescentou 24, todos os 26 da base continuam e nenhum foi enfraquecido. Cobertura: 94,2% das linhas (524/556) e 87,9% dos ramos (123/140). Na base eram 93,8% e 87,1%. Em `hotel.reservations`: 257/271 linhas e 69/74 ramos.
**Nota:** com problemas

**O que está bem.** Os testes novos conferem a maior parte da parte nova com valores exatos:
- os três descontos do convênio, inclusive o exemplo do financeiro, R$ 779,80 / R$ 93,58 / R$ 686,22 (`PriceCalculatorTest.java:78-121`);
- os três preços do pacote (`PriceCalculatorTest.java:123-163`);
- a estadia longa, R$ 1.749,30 / R$ 174,93 / R$ 1.574,37 (`PriceCalculatorTest.java:165-177`);
- as recusas do convênio sem código e com código inválido (`ReservationServiceTest.java:164-175`);
- a devolução de metade, R$ 410,00 (`ReservationServiceTest.java:245-257`), e a devolução integral no convênio e na estadia longa (`:259-285`);
- os mesmos casos pela API (`HttpApiTest.java:124-201`).

A pergunta da mutação passa nos pontos principais. Trocar 0.12 por 0.21 derruba `corporateTecnosulDiscount`. Trocar `< 7` por `<= 7` derruba `longStayWithSevenOrMoreNightsCreatesReservation`, porque 7 noites são aceitas no teste.

**O que ficou sem rede:**
- **As 2 noites do pacote.** `weekendPackageMustBeExactlyTwoNights` (`ReservationServiceTest.java:206-211`) usa as mesmas datas de `weekendPackageMustEndOnSunday` (`:198-203`): sexta 12/03 a segunda 15/03. A recusa acontece na regra do domingo, e a regra das 2 noites nunca roda. A JaCoCo marca `ReservationService.java:89` como nunca executada. Se alguém apagar `ReservationService.java:88-90`, uma sexta até o domingo da semana seguinte (9 noites) passa a ser aceita e nenhum teste falha. O nome do teste promete uma conferência que ele não faz.
- **A alta temporada na estadia longa.** O texto diz que ela "continua valendo", mas todos os testes de `LONG_STAY` são em março, fora da temporada (`PriceCalculatorTest.java:165`, `ReservationServiceTest.java:233`). O pacote em alta temporada, com o preço fechado, também não tem teste.
- **O código ignorado fora do convênio.** Nenhum teste confere isso.
- **Uma asserção fraca.** No teste HTTP, o código inválido só confere o status, sem o código de erro (`HttpApiTest.java:136-138`).

## Banco

**Números:** 3 tabelas, todas com chave primária; o dinheiro em `numeric(10,2)`; nenhuma chave estrangeira sem índice; nenhuma data guardada como texto. Há 2 migrações: a `V1__create_tables.sql` está igual à da base, e a `V2__add_company_code.sql` é nova. As conferências só acusaram `rate_plan` e `status` sem CHECK, o que já vinha da base. A restrição `EXCLUDE` contra duas reservas na mesma noite continua lá.
**Nota:** bom

O agente fez a mudança do jeito certo: uma migração nova, sem editar a da base, que acrescenta uma coluna opcional (`src/main/resources/db/migration/V2__add_company_code.sql:1`). Um banco que já roda o sistema sobe sem problema. O SQL novo usa parâmetros (`ReservationStore.java:31-33`, `:50`), e a leitura devolve o código como `Optional` (`ReservationStore.java:133`). Duas observações pequenas, que não mudam a nota:
- nenhuma restrição no banco amarra o convênio ao código (algo como `rate_plan <> 'CORPORATE' OR company_code IS NOT NULL`), e essa regra fica só no Java;
- a coluna recebe o código também nas reservas de outros planos (`ReservationStore.java:50`), o que guarda um dado sem significado.

`rate_plan` em texto sem CHECK já vinha da base.

## Comportamento

**Números:** 72 de 72 conferências passaram:
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 01: 21 de 21;
- reservas simultâneas: 2 de 2.
**Nota:** bom

Para o dono da pousada, tudo o que o pedido descreve funciona pela API.
- **Convênio:** os três descontos estão certos, e o exemplo da Tecnosul dá R$ 686,22. A reserva sem código, ou com um código que não existe, é recusada com 400 `INVALID_REQUEST`. O cancelamento devolve tudo.
- **Pacote:** R$ 520,00 / R$ 820,00 / R$ 1.200,00, inclusive em julho. A reserva de sábado a segunda e a de sexta a segunda são recusadas. O cancelamento devolve R$ 410,00.
- **Estadia longa:** dá R$ 1.574,37, a temporada é aplicada (28/06 a 05/07: R$ 1.754,30) e a estadia de 6 noites é recusada.
- **O resto:** o código da empresa é ignorado no plano normal, e o normal e o não reembolsável continuam como antes.

Os dados sobrevivem ao reinício, e as reservas simultâneas não duplicam.

## Dúvidas

- **Testes, `com problemas` ou `bom`.** A maior parte das regras novas está conferida com os valores do texto. Duas ficaram sem rede: as 2 noites do pacote, cujo teste repete as datas de outro, e a temporada na estadia longa. Escolhi `com problemas` porque um revisor exigente pediria para corrigir o teste que não confere o que o nome diz.
- **Banco e o código fora do convênio.** Gravar o código da empresa nas reservas de outros planos não aparece no comportamento (o desconto é ignorado). Pode ser lido como escolha razoável ou como dado inútil. Não pesou na nota.
