# Revisão: construção, cenário 09

## Visão geral

O projeto é um serviço Javalin montado em `Main.java:24-80`: `api` (quatro controllers que leem a requisição e montam a
resposta como `Map`), `domain.*` (a entidade `Reservation` com as transições de estado, o `ReservationService`, o
`PricingCalculator` estático e os repositórios JDBC, que ficam nos mesmos pacotes do domínio), `report` (os quatro
relatórios CSV, subclasses de `ReportGenerator`), `database` (`DatabaseSetup`, que cria as tabelas com
`CREATE TABLE IF NOT EXISTS` e abre uma conexão nova por chamada com `DriverManager`) e `util` (datas e dinheiro). Uma
requisição vai de `ReservationController` → `ReservationService` → `ReservationRepository` → PostgreSQL, em três tabelas
(`rooms`, `reservations`, `messages`). As regras de preço, de situação e dos relatórios estão certas: com `roomNumber`
enviado como texto, todos os valores do roteiro saem exatos. O que há de mais sério está na borda e no banco.
`POST /reservations` faz `(String) body.get("roomNumber")` (`api/ReservationController.java:31`) e responde **500** para
o cliente do roteiro, que manda o número do quarto como número. Por isso 47 de 64 conferências falharam. Corpo que não é
JSON, falta de `guests` e plano inválido também dão 500. E nada impede duas reservas simultâneas do mesmo quarto: numa
chamada manual, 10 de 10 rodadas gravaram as duas.

## Arquitetura

**Números:** 9 pacotes, 21 dependências entre eles, 1 ciclo (`domain.pricing` ↔ `domain.reservation`). A maior classe é `ReservationController` (260 linhas). JDBC aparece em `api`, `report` e `domain.*` (por `SQLException`), e Javalin só em `api` e no `Main`.
**Nota:** com problemas

- A montagem fica num lugar só (`Main.java:34-49`), por construtor, sem `static` global. As regras (`Reservation.java:52-100`,
  `PricingCalculator.java`) não conhecem Javalin nem SQL, e o SQL fica nos repositórios, com parâmetros.
- O ciclo: `PricingCalculator` importa `RatePlan` do pacote `reservation` (`domain/pricing/PricingCalculator.java:3`), e
  `ReservationService` usa o `PricingCalculator` (`domain/reservation/ReservationService.java:6,49-51`). O ciclo é pequeno,
  mas mostra que o desconto do plano não tem um dono claro.
- A borda não é só fina. O `ReservationController` fala direto com o repositório e pula o serviço (`api/ReservationController.java:108,196`).
  A regra "segunda via só para reserva paga" mora no handler (`api/ReservationController.java:199`). A tradução de erro
  depende do texto da mensagem da exceção (`api/ReservationController.java:84-100`), o que acopla o HTTP às frases do serviço.
- O JDBC vaza para as camadas de cima: o serviço e os controllers declaram `throws SQLException`
  (`ReservationService.java:31`, `ReservationController.java:27`), e cada relatório embrulha `SQLException` em
  `RuntimeException` (`report/OccupancyReport.java:37-38` e iguais).
- Os repositórios moram nos pacotes do domínio (`domain/reservation/ReservationRepository.java`). Isso é aceitável neste tamanho.

## Design de baixo nível

**Números:** 4 de 4 relatórios passam pelo esqueleto `ReportGenerator.generate` (`report/ReportGenerator.java:9-30`), com 4 passos abstratos (`:32-35`). Nenhum relatório escreve título, cabeçalho ou total fora dele.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** É o Template Method como a referência descreve. A classe abstrata `ReportGenerator` tem o método-modelo
(título com o nome da pousada, o nome do relatório e o período, depois o cabeçalho, as linhas e o total), e
`OccupancyReport`, `RevenueReport`, `TourismTaxReport` e `RatePlanReport` preenchem só nome, colunas, linhas e total. Os
dois relatórios da parte comum também estão nele. A linha com a data que o dono quer no ano que vem entraria num lugar
só. Três coisas afastam do ideal:

- o `generate` não é `final` (`ReportGenerator.java:9`), então uma subclasse pode reescrever o esqueleto;
- os passos têm acoplamento temporal: `getTotalLine()` depende do campo que `getDataLines()` preencheu
  (`OccupancyReport.java:53`, `RevenueReport.java:58`, `RatePlanReport.java:68`, `TourismTaxReport.java:64`). Chamados
  fora de ordem, dão `NullPointerException`;
- a taxa refaz o filtro e o cálculo no total (`TourismTaxReport.java:64-67`).

**O exagero.** Não há interface com uma implementação só, fábrica ou camada que só repassa: contenção.

**O resto do código:**

- O corpo de `POST /reservations` é um `Map` com casts (`api/ReservationController.java:29-38`), em vez de um DTO
  validado. Um número no lugar de texto dá `ClassCastException`. A falta de `guests` dá `NullPointerException` na linha 37,
  antes da checagem de nulos da linha 41. Um plano inválido dá `IllegalArgumentException`, que é relançada (`:69,100`).
  Os três casos viram 500 no handler genérico (`Main.java:83-89`).
- Os erros são mapeados por `e.getMessage().equals(...)` (`ReservationController.java:84-100`), e
  `IllegalArgumentException` vira "reserva não encontrada" em cinco handlers (`:112,125,143,161,180`).
- `ReportController` repete quatro vezes o mesmo bloco de parse/400/CSV (`api/ReportController.java:19-127`).
- `OccupancyReport` fixa o total de quartos (`report/OccupancyReport.java:54`) e repete num `switch` o tipo de cada quarto
  (`:58-65`), uma cópia do que `RoomType` e a tabela `rooms` já sabem.
- `MoneyUtils` guarda `DecimalFormat` em `static final`, compartilhado entre as threads do Jetty, e `DecimalFormat` não
  é thread-safe (`util/MoneyUtils.java:11-12`).
- `formatCpf` supõe 11 dígitos (`domain/reservation/ReservationService.java:164-172`). Com um CPF curto, a confirmação
  grava `CONFIRMED` (`:75-76`), depois quebra ao montar o e-mail (`:79`) e responde 500. Conferi pela API: ficou
  `CONFIRMED`, sem e-mail.
- `Reservation` tem setter público para tudo, inclusive `setStatus` e `setAmountPaid`, o que permite contornar as transições (`Reservation.java:211-221`).
- `toResponse` busca o quarto no banco e não usa o resultado (`ReservationController.java:218-219`).
- `amountPaid` e `refund` saem como `0` em vez de `0.00` quando vêm de `BigDecimal.ZERO` (`Reservation.java:27,85`).

Tipos certos no resto: `BigDecimal` com `HALF_EVEN` (`MoneyUtils.java:15`), `LocalDate`, `enum` para situação, plano e tipo de quarto.

## Testes

**Números:** 25 testes rodaram, 0 falhas, 0 erros, 0 pulados; 25 métodos escritos em 4 classes. Cobertura de linhas 101 de 846 (11,9%), de ramos 32 de 134 (23,9%). O pacote `report` tem 0 de 130 linhas cobertas, `api` 0 de 230 e `domain.message` 0 de 54. Não há nenhum teste pela API nem contra o banco.
**Nota:** com problemas

- **O que está testado, com valores:** o preço da noite com e sem temporada e o exemplo do texto, 599,76
  (`PricingCalculatorTest.java:16-43`); o desconto de 15% com meio para o par, 89,96, e o total, 509,80 (`:46-66`); as
  bordas da alta temporada, 14 e 15/12 e 28/02 e 01/03 (`DateUtilsTest.java:32-40`); o arredondamento e o formato
  R$ 1.234,56 (`MoneyUtilsTest.java:12-30`); as transições e a devolução no cancelamento (`ReservationTest.java:13-98`).
  São asserções de valor, não só de `assertNotNull`.
- **O que ficou sem rede:** toda a parte nova do cenário. Não há teste de `ReportGenerator`, da linha de título, da taxa
  de turismo nem do relatório por plano. Se alguém trocar `4.50` por `5.40` (`TourismTaxReport.java:15`), tirar o filtro
  `isPaid` (`:47`), mudar `> 0` por `>= 0` no filtro do dinheiro que ficou (`RatePlanReport.java:41`, `RevenueReport.java:45`)
  ou apagar a linha de título (`ReportGenerator.java:13-15`), nenhum teste falha. O comprovante e o e-mail também não têm
  teste (`ReservationService.java:114-162`).
- Sem teste pela API, o 500 de `POST /reservations` com `roomNumber` numérico e o 500 com corpo inválido passaram despercebidos.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)` nas 5 colunas; datas em `date`. As conferências acusaram uma chave estrangeira sem índice (`reservations.room_number`), nenhum CHECK nas colunas de situação, plano, tipo e canal, nenhuma restrição que proteja uma regra, nenhum pool, 0 transações explícitas, 0 `FOR UPDATE`, e o esquema criado em código Java, sem migração.
**Nota:** ruim

- **O que está certo:** os tipos (`database/DatabaseSetup.java:44-49`), a chave estrangeira para o quarto (`:35`), o
  `NOT NULL` no que é obrigatório, o SQL todo com `?` e o try-with-resources em todos os repositórios. Os dados
  continuam depois de reiniciar: conferi pela API (a reserva 3 voltou `CHECKED_IN` e as mensagens estavam lá) e o
  roteiro também (as mensagens continuam).
- **O defeito sério:** a regra "um quarto não pode ter duas reservas na mesma noite" não tem proteção nenhuma. É um
  `SELECT COUNT(*)` (`ReservationRepository.java:108-140`) seguido de um `INSERT` em outra conexão
  (`ReservationService.java:43,68`), sem transação, trava ou restrição, nem mesmo um `synchronized`. Com o serviço de pé
  e `roomNumber` em texto, mandei 10 rodadas de dois pedidos simultâneos para o 202: **as 10 gravaram as duas reservas**
  (`201 201` em todas, e o banco ficou com 2 reservas ativas por noite). Para o dono, é quarto vendido duas vezes.
- A confirmação grava a situação e depois grava o e-mail, sem transação (`ReservationService.java:75-79`). Se o segundo
  passo falha, a reserva fica confirmada sem mensagem (o caso do CPF curto, acima).
- O esquema nasce de `CREATE TABLE IF NOT EXISTS` (`DatabaseSetup.java:24-62`) e não sabe acrescentar uma coluna numa
  tabela que já existe. O próximo pedido que mudar uma tabela vai exigir intervenção manual.
- Uma conexão nova a cada consulta, por `DriverManager` (`DatabaseSetup.java:77-79`), sem pool. A chave estrangeira
  `room_number` não tem índice, e o filtro por mês usa `EXTRACT` sobre `check_in` (`ReservationRepository.java:148-149`),
  que não aproveita índice.

## Comportamento

**Números:** 17 de 64 conferências passaram. Parte comum 8 de 46, dados depois de reiniciar 1 de 3, cenário 09 7 de 13, reservas simultâneas 1 de 2.
**Nota:** ruim

- **A causa da maioria das falhas:** o roteiro manda `"roomNumber": 201` (número), e o serviço responde **500
  INTERNAL_ERROR** em todo `POST /reservations` (`measures/behavior-service.log`: `ClassCastException ... at
  ReservationController.create(ReservationController.java:31)`). Sem reserva criada, caem em cascata a confirmação, o
  e-mail, o check-in, o check-out, os cancelamentos, a segunda via, as linhas dos relatórios e a persistência da reserva.
  O `GET /rooms` também devolve `number` como texto (`"101"`), e por isso falharam "o 201 comporta 3" e "a diária do 201".
  O texto não diz o tipo do número do quarto, mas um sistema que responde 500 a um número em vez de aceitá-lo, ou ao
  menos de recusá-lo com 400, quebra o cliente do site. Para o dono, do jeito que o site chama, **nenhuma reserva entra**.
- **Falhas que não dependem disso:** corpo que não é JSON e falta de campos dão 500 em vez de 400 INVALID_REQUEST
  (conferi à parte com `roomNumber` em texto: `isso nao e json` → 500, `{"roomNumber":"101"}` → 500); um `ratePlan` inválido também dá 500.
- **Com `roomNumber` em texto (chamadas manuais),** o resto funciona e bate com os valores do roteiro:
  - os preços, 899,64, 795,40, 695,88 e 212,42 com desconto 37,48;
  - as recusas 409, 422, 404 e 400 de datas e hóspedes;
  - o e-mail com o comprovante e a segunda via, com 409 para a pendente;
  - os quatro relatórios com a linha de título. O financeiro deu `total,2390.92`, contando a não reembolsável cancelada.
    A taxa deu `27.00`, `18.00` e `total,45.00`. Os planos deram `STANDARD,2,1595.52`, `NON_REFUNDABLE,1,795.40` e
    `total,3,2390.92`, com as duas linhas zeradas num mês vazio.

  Ficam pequenos desvios: `amountPaid` sai `0` e `refund` sai `0` em vez de `0.00` (o roteiro acusou o `0.00`).
- **Reservas simultâneas:** no roteiro, as 20 tentativas deram 500, então "nenhuma dupla" passou sem querer. Na chamada
  manual, as 10 rodadas criaram as duas (o defeito pesa na coluna Banco).

## Dúvidas

- O tipo de `roomNumber` e de `number`: o texto não diz se é número ou texto. Se o cliente mandasse texto, a parte comum
  e o cenário passariam quase inteiros. Mantive `ruim` porque o serviço responde 500 (e não aceita nem recusa com 400) a
  um JSON plausível, e porque, mesmo com texto, corpo inválido e campos faltando também dão 500.
- Banco entre `com problemas` e `ruim`: a skill põe a duplicação "protegida só dentro do processo" em `com problemas`.
  Aqui não há proteção nenhuma, e a duplicação aconteceu em 10 de 10 rodadas. Por isso dei `ruim`, pelo risco de dado errado.
- Testes entre `com problemas` e `ruim`: os testes que existem conferem valores de verdade, mas a parte nova do cenário
  tem cobertura zero e a cobertura total é 11,9%.
