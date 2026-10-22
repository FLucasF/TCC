# Revisão: manutenção, cenário 01

## Visão geral

A base já era um serviço Javalin em sete pacotes. `hotel.web.HttpApi` lê o JSON e chama `hotel.reservations.ReservationService`, que usa `PriceCalculator` para o preço e `ReservationStore` (JDBC com HikariCP) para gravar no PostgreSQL. O esquema é criado por migrações do Flyway. O agente mexeu só onde o pedido mandava. Criou a interface `RatePlanPolicy` (preço com validação, e devolução) e cinco políticas em `RatePlanPolicies`, uma por plano. Cada constante do `enum RatePlan` leva a sua política no construtor. Os dois `switch` da base, o do desconto e o da devolução, foram trocados por chamadas à política. O `companyCode` vai do JSON até a coluna nova `company_code`, criada numa migração nova, a `V2`. Os 72 testes de comportamento passam, os 32 testes do projeto passam e a arquitetura não mudou. Não há nada sério. Há três defeitos pequenos: uma sobrecarga antiga de `calculate` que monta uma reserva falsa com `guest` nulo, o código da empresa gravado também nos planos que deviam ignorá-lo, e a validação do plano, que passou a rodar antes da conferência da capacidade.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base. Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base (`measures/dependencies-base.md`). `hotel.reservations` passou de 13 para 17 classes e de 508 para 682 linhas. A maior classe continua sendo `HttpApi`, com 171 linhas.
**Nota:** bom

- **Borda HTTP:** continua fina. A mudança em `src/main/java/hotel/web/HttpApi.java:128-130` só passa `body.companyCode()` adiante. `CreateReservationRequest.java:16` e `ReservationResponse.java:19,39` só ganharam o campo novo.
- **Regras:** as novas ficam em `hotel.reservations` (`RatePlanPolicies.java`, `RatePlanPolicy.java`), sem Javalin e sem JDBC. O jdeps confirma que `hotel.reservations` não usa Javalin.
- **SQL:** continua todo em `ReservationStore.java:19,32-44,133`.
- **Montagem:** as políticas são internas do pacote (`RatePlanPolicies` e `RatePlanPolicy` são package-private) e não exigiram mudar a montagem em `hotel`.
- **Ciclo:** veio da base (`ReservationService` usa `ReservationReceipt`, que lê `Reservation`), e o agente não o piorou.

## Design de baixo nível

**Números:** os 5 planos estão dentro da estrutura (2 da base e 3 novos), e nenhum ficou fora dela. Sobrou 0 `switch` ou `if` sobre o plano em `src/main`; o `grep` por `STANDARD` e `NON_REFUNDABLE` fora de `RatePlan` só acha `RoomType`. Há 1 ponto de seleção, o campo `policy` do enum. A interface tem 5 implementações.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** É um Strategy de verdade:

- **A interface:** `RatePlanPolicy.java:14,17` junta numa interface só o preço, que também recusa quando o plano não pode ser usado, e a devolução.
- **A escolha:** cada constante do enum recebe a sua política no construtor (`RatePlan.java:5-13,17-23`). A referência aceita isso de antemão como acerto.
- **Os dois planos da base:** também entraram na estrutura (`RatePlanPolicies.java:53-59`). O `switch` do desconto (base, `PriceCalculator.calculate`) e o da devolução (base, `ReservationService.cancel`) sumiram.
- **Quem usa não pergunta qual é o plano:** o cálculo só chama `request.ratePlan().policy().price(...)` (`PriceCalculator.java:25`), e o cancelamento só chama `reservation.ratePlan().policy().refund(...)` (`ReservationService.java:82`).
- **As validações ficaram dentro de cada plano:**
  - o código da empresa, em `RatePlanPolicies.java:66-70`;
  - a sexta ao domingo, em `RatePlanPolicies.java:85-90`;
  - as 7 noites, em `RatePlanPolicies.java:106-111`.
- **Um plano novo:** é uma política e uma constante, sem mexer no serviço.

**O exagero.** Não há. A interface tem cinco implementações, e não apareceu fábrica, builder nem camada de repasse. `Discounted` (`RatePlanPolicies.java:33-51`) é uma classe só, reaproveitada por três planos e estendida pela estadia longa (`:102-114`). Isso é economia, não cerimônia.

**O resto do código.** Está limpo, mas tem três defeitos localizados que um revisor exigente pediria para corrigir:

- **A sobrecarga antiga de `calculate`:** o agente manteve `calculate(room, checkIn, checkOut, ratePlan)` (`PriceCalculator.java:19-21`) só para os testes da base (`PriceCalculatorTest.java:21-68`). Ela monta um `NewReservation` falso com `guest` nulo, `guests` igual a 1 e `companyCode` nulo. Chamada com `CORPORATE`, ela sempre lança `INVALID_REQUEST`, o que é um contrato enganoso.
- **O código da empresa gravado em todos os planos:** o código é guardado e devolvido em qualquer plano (`Reservation.java:39`, `ReservationStore.java:44`, `ReservationResponse.java:39`), embora o pedido diga para ignorá-lo fora do convênio. Além disso, a busca faz `trim()` (`RatePlanPolicies.java:66`), mas o que se grava é o texto cru. Assim `" TECNOSUL "` é aceito e fica salvo com os espaços.
- **A ordem das conferências:** o cálculo do preço, que agora também valida o plano, subiu para antes da capacidade e da disponibilidade (`ReservationService.java:37`, contra a linha 46 da base). Para os planos novos, uma reserva que antes daria 422 ou 409 agora dá 400. É pequeno e não aparece no roteiro, mas muda a ordem dos erros sem necessidade.

Há também um detalhe de acoplamento: `PriceCalculator` chama a política, e a política recebe o calculador de volta (`PriceCalculator.java:25`, `RatePlanPolicy.java:14`). Esse ida-e-volta funciona, mas deixa as duas classes presas uma à outra.

O que está certo: o dinheiro é `BigDecimal` com `cents` (HALF_EVEN), as datas são `LocalDate`, as tabelas de preço e desconto são mapas imutáveis (`RatePlanPolicies.java:17-25`) e não há estado mutável novo.

## Testes

**Números:** 32 testes rodaram, com 0 falhas, 0 erros e 0 pulados. Há 32 métodos escritos. A base tinha 26, todos mantidos sem mudança, e o agente acrescentou 6 em `RatePlansTest`. A cobertura de linhas passou de 93,8% para 94,3% (517 de 548) e a de ramos de 87,1% para 87,9% (109 de 124). Em `hotel.reservations` estão cobertas 253 de 266 linhas e 57 de 60 ramos (`measures/tests.md`).
**Nota:** bom

Os testes novos rodam contra o banco, pelo serviço, e conferem as regras com valores:

- **Convênio:** os três descontos, com 93,58, 140,36 e 124,95 (`RatePlansTest.java:38-49`). A recusa sem código e com código desconhecido, e a devolução total de 686,22 (`:52-58`). O código guardado também é conferido (`:43`).
- **Pacote:** o preço fechado nos três tipos de quarto, inclusive na alta temporada, e a devolução de 410,00 (`:61-75`). A recusa de quinta a sábado, de sexta a segunda e de sexta a sábado (`:78-82`).
- **Estadia longa:** os valores do texto, 1749,30, 174,93 e 1574,37, a devolução total e a recusa com 6 noites (`:85-93`). A temporada mantida, com 1999,20 e 199,92 (`:96-101`).

A pergunta da mutação: trocar 0.12 por 0.21, `plusDays(2)` por outro valor, 0.5 na devolução ou `<` por `<=` no mínimo de noites faria algum teste falhar. Como as 7 noites são aceitas na linha 86 e as 6 recusadas na linha 92, a fronteira está coberta.

O que ficou sem rede:

- Nenhum teste passa o `companyCode` pela API. `HttpApiTest.java` não mudou, e o campo novo no JSON e o 400 pela HTTP só são conferidos pelo roteiro de comportamento.
- Nenhum teste confere que o código é ignorado nos outros planos.
- O `trim()` do código não é testado.

São lacunas pequenas, porque a regra está testada logo abaixo da borda.

## Banco

**Números:** 3 tabelas. A migração `V1__create_tables.sql` está igual à da base, e `V2__add_company_code.sql` é nova. As conferências não acusaram tabela sem chave primária, dinheiro fora de `numeric(10,2)`, chave estrangeira sem índice ou data em texto. `reservations.rate_plan` é `text` sem CHECK, o que já vinha da base. Não há transação explícita. A dupla reserva é protegida por `EXCLUDE USING gist`, também da base (`measures/database.md`).
**Nota:** bom

- **A migração:** o agente evoluiu o esquema do jeito certo, com uma migração nova que só acrescenta uma coluna anulável (`src/main/resources/db/migration/V2__add_company_code.sql:2`), sem editar a V1. Um banco que já roda o sistema sobe sem perder dados.
- **O SQL:** o `INSERT` e a leitura continuam com parâmetros (`ReservationStore.java:32-44`), e os índices foram renumerados corretamente. O roteiro mostra que os dados continuam depois de reiniciar.
- **A coluna `rate_plan`:** continua sem CHECK, vindo da base. Agora aceita cinco valores, e o agente não acrescentou a restrição. É aceitável, porque o enum do Java protege a escrita.
- **O código da empresa:** fica gravado também em reservas que não são de convênio (`ReservationStore.java:44`). É um dado sem uso, mas não errado. Esse ponto está contado no design.
- **Herdado da base:** `store.all()`, que traz a tabela inteira para os relatórios, já vinha da base e não foi tocado.

## Comportamento

**Números:** 72 de 72 conferências passaram, com 0 falhas (`measures/behavior.md`):
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 01: 21 de 21;
- reservas simultâneas: 2 de 2.

**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API, com os valores do financeiro:

- **Convênio:** a Tecnosul dá 779,80, 93,58 e 686,22. A Agrovale e a Farmavida também conferem. Sem código, ou com um código que não existe, a resposta é 400 `INVALID_REQUEST`, e o cancelamento devolve tudo.
- **Pacote:** o deluxe custa 820,00 e devolve 410,00. O standard custa 520,00, a suíte 1.200,00, e o preço é o mesmo em julho. As outras datas são recusadas com 400.
- **Estadia longa:** dá 1749,30, 174,93 e 1574,37, mantém a temporada (1754,30), recusa 6 noites e devolve tudo no cancelamento.
- **Planos antigos:** o STANDARD com `companyCode` ignora o código no preço, e o NON_REFUNDABLE continua com 15%.
- **O resto:** a parte comum segue igual, os dados continuam depois de reiniciar e as duas reservas ao mesmo tempo nunca são criadas juntas.

Fora do roteiro, notei pela leitura duas coisas que não mudam a nota:
- a reserva STANDARD devolve o `companyCode` que veio no JSON;
- a ordem dos erros mudou para os planos novos (400 antes de 422 e 409).

## Dúvidas

- O código da empresa é gravado e devolvido em qualquer plano (`ReservationStore.java:44`, `ReservationResponse.java:39`), e não só no convênio. É uma regra de plano pequena, fora da estrutura, que não muda a classe, como a referência prevê. Também não está claro se "pode ignorar" proíbe guardar o código ou só proíbe usá-lo no preço; o roteiro só confere o preço.
- A nota do design ficou entre `bom` e `com problemas`. Os defeitos são pequenos e localizados: a sobrecarga com reserva falsa, o código gravado em todos os planos e a mudança na ordem das conferências. Dei `com problemas` porque um revisor exigente pediria para tirar a sobrecarga com `guest` nulo antes de aceitar.
