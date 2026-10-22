# Revisão: construção, cenário 01

## Visão geral

O projeto (`projeto/`) é um serviço Javalin com 10 pacotes em camadas. O pacote `api` tem os controllers, o
`ExceptionHandler` e os DTOs. O `service` tem `ReservationService`, `MessageService` e `ReportService`. O `domain` tem
`Reservation` com a máquina de estados e os enums. O `domain.rateplan` tem uma estratégia por plano de tarifa. O
`repository` faz JDBC puro sobre um pool Hikari e o `infrastructure.Database` cria as tabelas com
`CREATE TABLE IF NOT EXISTS`. A montagem fica toda em `App.java:27-44`.

Uma criação de reserva segue este caminho: `ReservationController.createReservation` valida a entrada, chama
`ReservationService.createReservation` (`service/ReservationService.java:26`), que confere a capacidade e o conflito
de datas. Depois `Reservation.create` pede o plano a `RatePlanFactory`, e o plano valida e calcula o preço
(`domain/Reservation.java:53-84`). Por fim, o `ReservationRepository` grava a reserva no PostgreSQL. Os dados ficam no
banco e sobrevivem a um reinício (3/3).

O Strategy que o texto pede foi bem feito: os cinco planos são classes, e todos os 21 testes do cenário passam.

O problema mais sério é que nada impede duas reservas do mesmo quarto ao mesmo tempo. O serviço consulta o conflito
e depois insere, sem transação, sem trava e sem restrição no banco. No roteiro, as duas reservas foram criadas em 9 de
10 rodadas.

Há também dois erros de dado nas saídas:
- o comprovante (e com ele o e-mail) e o relatório de ocupação mostram o plano de tarifa no lugar do tipo do quarto
  (`util/DocumentFormatter.java:23`, `service/ReportService.java:27`);
- o dinheiro do comprovante sai com um espaço não separável ("R$ 599,76").

## Arquitetura

**Números:** 10 pacotes e 24 dependências entre eles. Há 2 ciclos, `domain` ↔ `domain.rateplan` e `domain` ↔ `util`
(`measures/dependencies.md`). Javalin aparece só em `api` e em `App`; JDBC só em `repository`, `infrastructure` e
`App`. A maior classe é `ReservationRepository`, com 242 linhas.
**Nota:** com problemas

O que está bom:
- **A borda HTTP é fina.** Os handlers leem a requisição, chamam o serviço e escrevem o DTO
  (`api/ReservationController.java:47-70`). Os erros viram JSON num lugar só, com `app.exception`
  (`App.java:69-76`, `api/ExceptionHandler.java`).
- **Os serviços não conhecem o Javalin nem o SQL** (`service/ReservationService.java`).
- **O acesso ao banco fica isolado** nos três repositórios, com o `DataSource` injetado.
- **A montagem fica num lugar só** (`App.java:27-44`).

O que impede o `bom`:
- **Dois ciclos de pacote.** `domain.Reservation` importa `rateplan.RatePlan`, `RatePlanFactory` e
  `util.PriceCalculator` (`domain/Reservation.java:3-6`). Já `rateplan` e `util` importam `domain.Room`, `RoomType` e
  `Reservation` (`domain/rateplan/RatePlan.java:3`, `util/PriceCalculator.java:3`, `util/DocumentFormatter.java:3`).
  O `util` mistura uma regra de domínio (o preço por noite e a temporada) com formatação de apresentação (o
  comprovante).
- **Uma regra no handler.** A regra "segunda via só de reserva paga" mora no controller
  (`api/ReservationController.java:75-76`), e não no serviço.
- **Um controller pula a camada de regras.** `RoomController` fala direto com o repositório
  (`api/RoomController.java:10`). É um caso pequeno.
- **Estado global.** `Database` guarda o pool num `static` mutável (`infrastructure/Database.java:10`).

São misturas localizadas, mas os ciclos são dois.

## Design de baixo nível

**Números:** os 5 planos estão na estrutura, 0 fora dela. Há uma interface `RatePlan` com 4 operações (validar, diárias,
desconto e devolução) e 5 implementações, escolhidas por um mapa `RatePlanType → Supplier`
(`domain/rateplan/RatePlanFactory.java:9-23`). Não há nenhum `switch` ou `if` sobre o plano fora das estratégias.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** Cada plano, os dois do ponto de partida inclusive, tem o seu preço, a sua validação e a sua devolução
numa classe só dele:
- o pacote de fim de semana valida sexta a domingo e tem preço fechado por tipo de quarto
  (`domain/rateplan/WeekendPackageRatePlan.java:22-48`);
- a estadia longa exige 7 noites (`LongStayRatePlan.java:14-19`);
- o convênio guarda a tabela de empresas (`CorporateRatePlan.java:13-30`);
- o não reembolsável devolve zero (`NonRefundableRatePlan.java`).

O cálculo e o cancelamento pedem ao plano sem nomeá-lo (`domain/Reservation.java:57-63` e `:113-114`). O único
ponto que conhece os planos pela constante é o mapa de seleção, que a referência aceita. Um plano novo é uma classe e
uma linha no mapa (mais a constante do `enum`).

O que afasta um pouco do ideal, sem mudar a classe:
- **Um parâmetro que só um plano usa.** A assinatura comum `validate(checkIn, checkOut, companyCode)`
  (`RatePlan.java:8`) põe na interface o dado de um plano só.
- **Acoplamento temporal.** `CorporateRatePlan` guarda o código num campo mutável dentro de `validate`
  (`CorporateRatePlan.java:19,29`), e `calculateDiscount` depende disso (`:39`). Chamado sem `validate` antes, ele
  falha com `NullPointerException`.
- **O código da empresa não é persistido.** Na hora do cancelamento, o plano é recriado sem ele
  (`domain/Reservation.java:113`). Hoje isso não importa, porque a devolução do convênio não usa o código.

**O exagero.** Não há estrutura sem necessidade. A fábrica é o ponto de seleção do próprio Strategy, os DTOs são
`record`s finos e não há interfaces de serviço ou de repositório com uma implementação só.

**O resto do código.** Os tipos estão certos: `BigDecimal` com `HALF_EVEN` em cada passo (`util/PriceCalculator.java:20-27`)
e `LocalDate`. Os defeitos que um revisor apontaria:
- **O tipo errado no comprovante.** `DocumentFormatter.java:23` escreve `getRatePlanType()` onde o texto pede o tipo do
  quarto, e sai "Quarto: 201 (CORPORATE)". Confirmei chamando a API.
- **O tipo errado no relatório.** `ReportService.java:27` repete o erro na coluna `type` do relatório de ocupação, e
  sai `201,CORPORATE,...`.
- **O formatador de moeda.** O `NumberFormat` de moeda é `static` e compartilhado entre as threads do Jetty, e
  `NumberFormat` não é thread-safe (`DocumentFormatter.java:14`, `:55`). Ele também produz `R$` seguido de U+00A0, e
  não o espaço comum do texto. Além disso, o construtor `new Locale(...)` está obsoleto (`:12`).
- **Zeros sem as 2 casas.** `BigDecimal.ZERO` com escala 0 aparece como desconto e como valor pago
  (`StandardRatePlan`, `Reservation.java` no `create`), e o JSON sai `"discount":0` e `"amountPaid":0`.
- **Duas constantes fixas.** A ocupação fixa o total de quartos em `"/5"` (`ReportService.java:34`). A lista de colunas
  do `SELECT` está copiada 4 vezes em `ReservationRepository.java`.

São defeitos localizados, mas dois deles põem dado errado na saída.

## Testes

**Números:** 20 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 20 métodos escritos em 7 classes. A cobertura é
de 18,6% das linhas (140 de 753) e 17,6% dos ramos (26 de 148). Os pacotes `api`, `service`, `repository` e
`infrastructure` têm 0%. O pacote `domain.rateplan` tem 44 de 52 linhas (`measures/tests.md`). Não há nenhum teste
pela API nem contra o banco.
**Nota:** com problemas

O que os testes conferem:
- **Testes de unidade com valores, sobre os planos novos.** Tecnosul 779,80 e 93,58
  (`CorporateRatePlanTest.java:14-26`); estadia longa 1.749,30 e 174,93 (`LongStayRatePlanTest.java:14-26`); pacote
  820,00 e devolução de 410,00 (`WeekendPackageRatePlanTest.java:14-34`).
- **As recusas.** Código ausente e código inválido (`CorporateRatePlanTest.java:29-48`), menos de 7 noites e o pacote
  fora de sexta ou fora de 2 noites.
- **A temporada de julho**, com 599,76 (`PriceCalculatorTest.java:24-32`).
- **Parte da máquina de estados** (`ReservationTest.java`).

A pergunta da mutação deixa sem rede:
- trocar 0.18 ou 0.25 da Agrovale e da Farmavida (só a Tecnosul é testada);
- trocar a devolução integral do convênio ou da estadia longa por outra regra (`calculateRefund` delas não é
  testado);
- mudar os limites da temporada (15/12, fim de fevereiro);
- mudar os preços do pacote standard e suíte;
- mexer em check-in, check-out e cancelamento pendente.

Nada testa o comprovante, o e-mail, os relatórios, os códigos de erro HTTP ou o conflito de datas. Por isso os dois
erros de saída (o plano no lugar do tipo do quarto e o espaço não separável) e a reserva duplicada passaram sem que
nenhum teste falhasse.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date` (`measures/database.md`).

O que as conferências acusaram:
- a chave estrangeira `reservations.room_number` não tem índice;
- não há nenhum CHECK em `status`, `rate_plan_type`, `room_type` e `channel`;
- não há nenhuma restrição UNIQUE ou EXCLUDE;
- há 0 transações explícitas e 0 `FOR UPDATE`.

O esquema é criado no código, sem migrações.
**Nota:** com problemas

O que está certo:
- os tipos;
- o SQL sempre com `?` e try-with-resources sobre o pool Hikari (`repository/*.java`);
- a reserva ligada ao quarto por chave estrangeira (`infrastructure/Database.java:36`);
- os dados persistem depois de reiniciar.

Os defeitos:
- **Nada protege contra reserva duplicada.** `ReservationService.java:35` consulta os conflitos
  (`ReservationRepository.java:121`) e depois insere numa outra conexão (`ReservationService.java:45`), sem
  transação, trava ou restrição. Nem um `synchronized` existe. O roteiro do comportamento criou as duas reservas em 9
  de 10 rodadas simultâneas.
- **O esquema não evolui.** `CREATE TABLE IF NOT EXISTS` (`Database.java:25-75`) não sabe acrescentar uma coluna
  quando a próxima mudança precisar.
- **A confirmação não é atômica.** A reserva e o e-mail são gravados em duas operações sem transação
  (`ReservationService.java:55-57`). Se o `INSERT` da mensagem falhar, a reserva fica confirmada sem o e-mail
  registrado.
- **Uma informação da reserva não fica guardada.** O `companyCode` do convênio não é guardado.

Não há dinheiro em ponto flutuante nem SQL montado com dado de fora.

## Comportamento

**Números:** 66 de 72 conferências passaram (`measures/behavior.md`):
- a parte comum: 41 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 01: 21 de 21;
- duas reservas ao mesmo tempo: 1 de 2.

**Nota:** com problemas

**O cenário está todo certo.** Passaram:
- Tecnosul 686,22, Agrovale e Farmavida;
- as recusas do convênio;
- o pacote a 820,00 / 520,00 / 1.200,00, sem temporada, e a devolução de 410,00;
- a estadia longa a 1.574,37, inclusive com noites de temporada;
- a recusa com 6 noites;
- o `companyCode` ignorado nos outros planos.

As falhas, uma a uma:
- **"O 201 comporta 3" e "a diária do 201 é 389.90"** não pesam. O serviço devolve `number` como texto (`"201"`) e o
  roteiro procura o número 201. Chamei `GET /rooms`, e a capacidade 3 e a diária 389.90 estão certas. O texto não
  fixa o tipo do campo.
- **`amountPaid` sai `0`, e não `0.00`**, assim como `discount` sai `0` no plano normal. O texto pede "número com 2
  casas". É um erro pequeno de formato.
- **A segunda via de uma reserva pendente responde 400 `INVALID_REQUEST`**, quando o texto diz que uma operação que a
  situação da reserva não permite é 409 `OPERATION_NOT_ALLOWED`. O site vai tratar o caso como erro de dados.
- **O comprovante do e-mail não traz "Total: R$ 599,76".** Traz `R$` seguido de um espaço não separável (U+00A0),
  então o texto não é o que o dono descreveu. Fora do roteiro, conferi pela API mais dois erros:
  - a linha "Quarto: 201 (CORPORATE)" mostra o plano no lugar do tipo do quarto, no e-mail e no comprovante;
  - o relatório de ocupação traz `201,CORPORATE,...` na coluna `type`.

  O hóspede e o contador recebem informação errada.
- **Duas reservas ao mesmo tempo.** Em 9 de 10 rodadas, as duas reservas do mesmo quarto e das mesmas noites foram
  criadas. Para o dono, isso é overbooking real quando dois hóspedes reservam pelo site no mesmo instante. O defeito
  é do banco e foi contado naquela coluna.

## Dúvidas

- **A nota do banco.** A falta total de proteção contra reserva duplicada (9 de 10 rodadas) é um risco de dado errado,
  e poderia justificar `ruim`. Fiquei com `com problemas` porque a rubrica da coluna põe a duplicação mal protegida
  nessa faixa e o resto do esquema está correto.
- **O formato do campo `number` dos quartos.** O texto não diz se é número ou texto. Não pesei as duas falhas que vêm
  disso.
