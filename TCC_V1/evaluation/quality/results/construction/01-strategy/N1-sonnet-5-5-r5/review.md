# Revisão: construção, cenário 01

## Visão geral

O agente montou o projeto do zero em cinco pacotes: `pousada` (a montagem, `App`), `pousada.web` (as rotas do Javalin e
a leitura do corpo), `pousada.service` (os casos de uso e os relatórios), `pousada.domain` (a reserva, o dinheiro, a
temporada, os quartos e os planos de tarifa) e `pousada.persistence` (o pool Hikari, o esquema e os repositórios JDBC).
Uma requisição passa por `Api` (`src/main/java/pousada/web/Api.java:31-43`), que valida o formato com `RequestParser`
e chama `ReservationService`. O serviço abre uma transação (`Database.inTransaction`,
`src/main/java/pousada/persistence/Database.java:70-84`), pede a regra ao domínio (`Reservation.confirm/cancel/...`,
`RatePlan.quote`) e grava pelos repositórios. Os dados ficam em duas tabelas do PostgreSQL (`reservations` e `messages`),
e os quartos e os planos ficam no código. Cada um dos cinco planos de tarifa é uma implementação de `RatePlan`, um
Strategy limpo, e o roteiro de comportamento passou inteiro (72 de 72). O que há de mais sério é pouco: os testes
ficam só no domínio (30,5% das linhas, nada no serviço, nos repositórios nem na API), e o banco é criado com
`CREATE TABLE IF NOT EXISTS` no código, sem migração e sem nenhuma restrição além das chaves primárias.

## Arquitetura

**Números:** 5 pacotes, 8 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O `pousada.domain`
(19 classes, 510 linhas) não depende de nenhum outro pacote. O Javalin só aparece em `pousada` e `pousada.web`, e o
JDBC em `pousada.persistence` e `pousada.service`. A maior classe é `ReservationRepository`, com 152 linhas.
**Nota:** bom

- **A borda HTTP é fina.** Cada rota é uma linha que lê, chama o serviço e escreve a resposta
  (`src/main/java/pousada/web/Api.java:30-43`). Os erros viram JSON num lugar só (`Api.java:45-49`). A validação de
  formato (o JSON, as datas, os inteiros, a saída depois da entrada) fica em `RequestParser`
  (`src/main/java/pousada/web/RequestParser.java:18-47`), que não tem nenhuma regra de negócio.
- **As regras ficam no domínio, sem Javalin e sem SQL.** As transições de situação estão em
  `src/main/java/pousada/domain/Reservation.java:26-44`, o preço em `RatePlan.quote` (`RatePlan.java:31-35`) e a
  temporada em `Season.java:14-24`.
- **O acesso ao banco está isolado nos repositórios.** Todo o SQL mora em `ReservationRepository` e `MessageRepository`.
  O serviço só demarca a transação e passa a `Connection` adiante (`ReservationService.java:47-57`, `108-117`). Isso
  explica o "JDBC" do pacote `service` no jdeps: ele usa `Connection` e `SQLException`, mas não monta SQL.
- **A montagem fica quase toda em `App.main`** (`src/main/java/pousada/App.java:9-15`). A exceção são os dois
  repositórios, que o serviço cria por dentro (`ReservationService.java:28-29`). Como eles não guardam estado, o custo
  é pequeno.
- Os catálogos `Rooms` e `RatePlans` são `static`, mas imutáveis (`Rooms.java:8-13`, `RatePlans.java:8-13`). Isso é
  razoável num sistema em que, segundo o texto, os quartos quase nunca mudam.

Nada aqui que um revisor exigente bloquearia. O que dá para pedir é pequeno: injetar os repositórios em vez de criá-los
dentro do serviço.

## Design de baixo nível

**Números:** os 5 planos (STANDARD, NON_REFUNDABLE, CORPORATE, WEEKEND_PACKAGE, LONG_STAY) estão todos na estrutura,
cada um numa classe que implementa `RatePlan`, e nenhum fica fora dela. Há 0 `switch` e 0 `if` sobre o plano fora das
implementações (busca por `switch` e pelos códigos dos planos em `src/main/java`). A seleção acontece num ponto só,
`RatePlans.find` (`RatePlans.java:18-20`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** A interface `RatePlan` (`src/main/java/pousada/domain/RatePlan.java:10-36`) reúne as três coisas que o
texto diz que cada plano traz: quem pode usar (`validate`), o preço (`subtotal` e `discount`) e a devolução (`refund`).
O roteiro comum do preço é um método default, `quote`, que valida, soma as noites e aplica o desconto. Os dois planos
da parte comum também são classes: `StandardPlan.java:5-15` e `NonRefundablePlan.java:5-22`. Os três novos guardam as
suas regras dentro da própria classe:

- o convênio, com o mapa de empresas e a recusa sem código válido (`CorporatePlan.java:8-28`);
- o pacote, com a sexta e as duas noites, o preço fechado por tipo de quarto, sem temporada, e a devolução de metade
  (`WeekendPackagePlan.java:9-35`);
- a estadia longa, com o mínimo de 7 noites e os 10% (`LongStayPlan.java:7-30`).

Quem usa a estrutura não pergunta qual é o plano. A criação chama `plan.quote(booking)`
(`ReservationService.java:42-45`), e o cancelamento chama `RatePlans.get(ratePlan).refund(amountPaid)`
(`Reservation.java:43`). Um plano novo é uma classe nova e uma linha em `RatePlans.ALL`.

**O exagero.** Não há estrutura sem uso. `Booking`, `Quote` e `CreateReservation` são records que carregam dados de
verdade. `Database.Work` existe porque a lambda precisa lançar `SQLException`. A única interface do projeto,
`RatePlan`, tem cinco implementações. Não há fábrica, builder nem camada que só repasse.

**O resto do código** está limpo:

- o dinheiro é `BigDecimal`, arredondado com HALF_EVEN num lugar só (`Money.java:20-27`);
- as datas são `LocalDate`, e a situação é um `enum` com `isPaid` (`ReservationStatus.java:3-9`);
- a reserva é um record imutável, e cada transição devolve uma cópia (`Reservation.java:26-67`);
- os métodos são curtos, sem condições aninhadas;
- os erros de negócio usam um tipo próprio com status e código (`DomainException.java:4-29`), e o `catch (Exception)`
  da borda registra a pilha antes de responder 500 (`Api.java:46-49`).

Há três observações menores, que não bloqueiam:

- o plano fica na reserva como `String` (`Reservation.java:17`) e é resolvido de novo pelo registro estático no
  cancelamento (`Reservation.java:43`);
- a chamada ao construtor de 16 argumentos se repete em `Reservation.with` (`Reservation.java:64-67`),
  `ReservationRepository.withId` (`ReservationRepository.java:147-151`) e `ReservationService.create`
  (`ReservationService.java:53-56`);
- `Rooms.find(...).orElseThrow()` se repete em `ReservationService.java:119-121` e `Reports.java:23`.

## Testes

**Números:** 14 testes rodaram, com 0 falhas, 0 erros e 0 pulados. Há 14 métodos de teste em 2 arquivos. A cobertura
é de 122 de 400 linhas (30,5%) e de 35 de 96 ramos (36,5%). Por pacote, `pousada.domain` tem 122 de 126 linhas e 35 de
40 ramos, enquanto `service`, `persistence`, `web` e `App` têm 0 (`measures/tests.md`). Todos os testes são de
unidade: nenhum chama a API ou o banco, e nenhum usa mocks.
**Nota:** com problemas

Os testes que existem são bons e conferem valores (`src/test/java/pousada/domain/RatePlanTest.java`):

- o STANDARD de julho dá 599,76 (`RatePlanTest.java:22-25`);
- o NON_REFUNDABLE dá desconto de 116,97 (`RatePlanTest.java:27-30`);
- o convênio da Tecnosul dá 93,58 de desconto e 686,22 de total (`RatePlanTest.java:32-35`);
- o convênio sem código ou com um código que não existe é recusado (`RatePlanTest.java:37-42`);
- o pacote sai 820,00 no deluxe e 1.200,00 na suíte em julho, e devolve 410,00 (`RatePlanTest.java:44-49`);
- o pacote de quinta e o de 3 noites são recusados (`RatePlanTest.java:51-55`);
- a estadia longa dá 1.574,37, e com 6 noites é recusada (`RatePlanTest.java:57-61`);
- a devolução de cada plano e as bordas da temporada (`RatePlanTest.java:63-78`).

`ReservationTest.java` confere o ciclo de vida e as recusas de transição (`ReservationTest.java:17-24`), a devolução no
cancelamento (`ReservationTest.java:26-32`), o texto inteiro do e-mail (`ReservationTest.java:34-48`) e o formato
R$ 1.234,56 (`ReservationTest.java:55-58`).

A pergunta da mutação mostra boa rede no que está testado. Trocar os 12% da Tecnosul, os 10% da estadia longa, os 50%
da devolução do pacote ou o `< 7` por `<= 7` faz algum teste falhar. Ficam sem rede:

- os percentuais da Agrovale (18%) e da Farmavida (25%);
- o preço do pacote no standard (R$ 520,00);
- tudo o que mora fora do domínio: a recusa de quarto ocupado (a consulta de sobreposição em
  `ReservationRepository.java:30-42`), os dois relatórios em CSV (`Reports.java:19-46` e os filtros SQL em
  `ReservationRepository.java:80-92`), os códigos e status HTTP de erro, a criação do esquema e a trava de
  concorrência.

O texto não pedia testes, e a iniciativa foi boa no domínio. Ainda assim, não há nenhum teste pela API nem contra o
banco, e o sistema só se confere por inteiro de fora.

## Banco

**Números:** 2 tabelas (`reservations`, com 16 colunas, e `messages`, com 5). As duas têm chave primária. As 5 colunas
de dinheiro são `numeric(12,2)` e as datas são `date`. A conferência acusou 0 chaves estrangeiras, 0 CHECK e 0
UNIQUE/EXCLUDE, e `status`, `rate_plan` e `channel` são `text` sem CHECK. O esquema é criado por `CREATE TABLE` no
código, com 0 arquivos de migração. Os 4 trechos de SQL montados com `+` só juntam constantes (`measures/database.md`).
**Nota:** com problemas

O que está certo:

- os tipos estão certos (`Database.java:18-45`) e o obrigatório é `NOT NULL`;
- todo o SQL usa parâmetros `?`. Os trechos concatenados são constantes: `PAID` em `ReservationRepository.java:19`
  e o `FOR UPDATE` em `ReservationRepository.java:70`;
- as conexões vêm de um pool Hikari (`Database.java:49-57`) e voltam por try-with-resources;
- cada operação roda numa transação com rollback (`Database.java:70-84`). Na confirmação, a mudança de situação e a
  gravação do e-mail andam juntas (`ReservationService.java:64-71`);
- a regra de não ter duas reservas na mesma noite é protegida por uma trava consultiva do PostgreSQL por quarto,
  dentro da transação (`ReservationRepository.java:22-27`, chamada em `ReservationService.java:48`). Ela vale entre
  processos, não só dentro de um, e o roteiro de concorrência passou;
- as transições leem a linha com `FOR UPDATE` (`ReservationService.java:108-117`).

Os defeitos:

- **O esquema não evolui.** Ele nasce de um `CREATE TABLE IF NOT EXISTS` embutido no código (`Database.java:18-45`,
  `59-68`), sem migrações versionadas. A primeira coluna nova numa tabela que já existe vai exigir uma mudança à mão.
- **O banco não protege nenhuma regra.** Não há tabela de quartos, nem chave estrangeira de `room_number`
  (`Database.java:21`). Não há CHECK em `status`, em `rate_plan` ou em `guests >= 1`, e nada no esquema impede a
  sobreposição: a proteção depende de todo escritor passar pela trava do código.

## Comportamento

**Números:** 72 de 72 conferências passaram, e nenhuma falhou (`measures/behavior.md`):

- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 01 (os planos novos): 21 de 21;
- duas reservas ao mesmo tempo: 2 de 2, nas 10 rodadas.
**Nota:** bom

O sistema faz o que o dono da pousada pediu. Os valores do financeiro conferem:

- Tecnosul: 779,80, 93,58 e 686,22;
- Agrovale e Farmavida: 18% e 25%;
- o pacote: 520,00, 820,00 e 1.200,00, inclusive em julho, e devolve 410,00 quando é cancelado depois de pago;
- a estadia longa: 1.574,37, e 1.754,30 com noites de temporada.

As recusas também conferem: convênio sem código ou com código desconhecido, pacote fora de sexta a domingo e estadia
com 6 noites voltam 400 INVALID_REQUEST. O `companyCode` é ignorado nos outros planos. A parte comum funciona inteira:
preço e arredondamento, códigos de erro, e-mail com o comprovante, check-in, check-out, devoluções e os dois
relatórios. As reservas sobrevivem ao reinício, e duas reservas simultâneas do mesmo quarto nunca saem as duas.

## Dúvidas

Nenhuma que mude as notas. Fica uma escolha que o texto não fechava: um `ratePlan` desconhecido responde 400
INVALID_REQUEST (`ReservationService.java:42-43`). É razoável, e o roteiro não confere esse caso.
