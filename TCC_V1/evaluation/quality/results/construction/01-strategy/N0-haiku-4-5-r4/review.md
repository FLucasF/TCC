# Revisão: construção, cenário 01

## Visão geral

O projeto tem sete pacotes: `api` (rotas do Javalin), `service` (regras e SQL), `rateplan` (os planos de tarifa),
`db` (o pool Hikari e o `CREATE TABLE`), `entity`, `util` e a classe `Application`. Uma requisição entra num handler
estático (`api/ReservationEndpoints.java`), que lê o JSON à mão, cria um `ReservationService` com `new` e devolve um
`LinkedHashMap`. O serviço escreve o SQL ele mesmo, por JDBC, no PostgreSQL. Os planos de tarifa formam um Strategy
correto: uma interface, cinco implementações e um mapa que escolhe. O problema mais sério é que **o sistema não sobe**.
As rotas usam a sintaxe `:id`, abandonada desde o Javalin 4, e o Javalin 6.7.0 lança `IllegalArgumentException` ao
registrar `GET /reservations/:id` (`src/main/java/com/pousada/api/ReservationEndpoints.java:55`, chamado de
`Application.java:19`). O `main` só trata `SQLException` (`Application.java:27`), e o processo morre antes do
`app.start`. Por isso nenhuma conferência do comportamento chegou a rodar. Há um segundo defeito, escondido pelo
primeiro: `getReservation` seleciona `rm.type` mas lê a coluna `room_type` (`ReservationService.java:89-92` e `:205`).
Num experimento numa cópia em `/tmp` (só `:id` trocado por `{id}`), toda criação e toda leitura de reserva deram 500
(`The column name room_type was not found`), embora o preço gravado no banco estivesse certo (779,80 / 93,58 / 686,22).
Os testes são 3 métodos sobre a classe `Money`, com 0,9% de cobertura de linhas.

## Arquitetura

**Números:** 7 pacotes, 10 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). `api` → `service` →
`db`/`rateplan`/`entity`/`util`. JDBC aparece em `service` e em `db`, e Javalin só em `api` e na raiz. A maior classe
é `ReservationEndpoints` (243 linhas), seguida de `ReservationService` (223).
**Nota:** com problemas

As dependências apontam na direção certa, e as regras não conhecem o Javalin (nenhum `Context` em `service` nem em
`rateplan`). Os planos ficam num pacote que não depende de banco nem de HTTP (`rateplan` → `entity`, `util`). Os
problemas:

- **Não há camada de acesso a dados.** Todo serviço monta e executa o seu SQL, misturado com a regra:
  `ReservationService.java:53-83` (insert), `:111-117`, `:138-144`, `:187-199` (disponibilidade),
  `ReportService.java:19-45` e `:54-93`, `RoomService.java:13-28`. O pacote `db` só guarda o pool e o esquema.
- **A montagem está espalhada e usa estado global estático.** `Database` é um singleton estático
  (`db/Database.java:11`, `:104`). Cada handler cria o serviço a cada requisição
  (`ReservationEndpoints.java:39`, `:58`, `:77`…), o serviço cria os seus colaboradores por dentro
  (`ReservationService.java:19`, `:119`, `ReportService.java:13`), e as rotas são registradas por métodos estáticos.
  Não existe um lugar onde as peças são ligadas.
- **Há regra nos handlers.** A decisão de quais situações dão direito à segunda via fica no handler
  (`ReservationEndpoints.java:143-148`). O handler do check-out compõe o comprovante chamando o `MessageService`
  (`:121-124`). O mapeamento de código de erro para status HTTP depende de strings que o serviço põe na mensagem da
  `IllegalArgumentException` (`:205-224`).

A mistura é de serviço com SQL e de handler com um pedaço de regra. As camadas existem, mas mal separadas.

## Design de baixo nível

**Números:** os 5 planos (STANDARD, NON_REFUNDABLE, CORPORATE, WEEKEND_PACKAGE, LONG_STAY) estão na estrutura e
nenhum fica fora dela. Há 0 `switch`/`if` sobre o nome do plano fora de `rateplan`, e um único ponto de seleção
(`RatePlanFactory.getPlan`). O cálculo das diárias está copiado igual em 4 classes.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** `rateplan/RatePlan.java:6-13` define a interface com `isValid`, `calculatePrice`, `getRefundAmount` e
`getName`. Cada um dos cinco planos, inclusive os dois do ponto de partida, tem a sua classe com o preço, a validação e
a devolução:

- o convênio, com os 12/18/25% e a recusa do código ausente ou desconhecido, em `CorporatePlan.java:11-20`;
- o pacote, sexta + 2 dias com preço fechado e metade devolvida, em `WeekendPackagePlan.java:11-15`, `:18-31` e `:41-42`;
- a estadia longa, com 7 noites ou mais e 10%, em `LongStayPlan.java:11-20`;
- o não reembolsável, com 15% e devolução zero, em `NonRefundablePlan.java`.

A escolha é feita por um mapa (`RatePlanFactory.java:8-23`). O serviço só pergunta ao plano, sem nomeá-lo
(`ReservationService.java:37-44`, `:51`, `:135-136`). Um plano novo é uma classe nova e uma linha no `register`. A
única regra de plano fora do plano é o `companyCode`, gravado em qualquer plano (`ReservationEndpoints.java:30`,
`ReservationService.java:69`): é pequena e não muda a classe.

**O exagero.** A `RatePlanFactory` é o ponto de seleção que o pattern pede, e o `PriceCalculation` é um record de
resultado. Não há interface, fábrica nem camada sobrando. Há código morto (`RatePlanFactory.isValidPlan` e
`getAllPlans`, `:26-32`; `DateUtil.isWeekend` e `nextSunday`, `util/DateUtil.java:30-36`;
`Database.getDataSource`, `:100`), mas código morto não é estrutura.

**O resto do código:**

- **Duplicação.** O método `calculateSubtotal`, com a temporada, está copiado igual em `StandardPlan.java:23-35`,
  `NonRefundablePlan.java:23-35`, `CorporatePlan.java:31-43` e `LongStayPlan.java:24-36`. Também é igual em todas o
  `guests >= 1 && checkIn.isBefore(checkOut)`, que o serviço já confere (`ReservationService.java:29-35`). Uma mudança
  na temporada mexe em quatro arquivos.
- **Tipos.** Situação, plano e tipo de quarto são `String` soltas (`ReservationService.java:107`, `:131`, `:154`,
  `:173`), sem `enum`. O percentual do convênio é `Double` (`CorporatePlan.java:11`). O dinheiro sai no JSON por
  `Double.parseDouble(String.format("%.2f", …))` (`ReservationEndpoints.java:172-177`, `:192-194`), que depende da
  locale padrão da JVM: numa máquina em pt_BR, `"779,80"` não é lido como double e a resposta quebra. O mesmo
  `String.format("%.2f")` sem locale aparece no CSV (`ReportService.java:97`). O dinheiro do comprovante usa
  `Locale.GERMANY` com `%.2f` (`MessageService.java:95-97`), que dá `1749,30` e não o `1.749,30` pedido.
- **Estado.** As entidades têm campos públicos (`entity/Reservation.java:7-24`, `entity/Room.java`), e o `DataSource`
  é um `static` mutável (`Database.java:11`).
- **Erros.** Os códigos de erro viajam como texto na mensagem de `IllegalArgumentException`
  (`ReservationService.java:26`, `:30`, `:39`…). `catch (Exception e)` transforma qualquer falha, inclusive um bug,
  em 400 `INVALID_REQUEST` com a mensagem interna (`ReservationEndpoints.java:50-52`, `:84-86`). O mesmo bloco de
  `catch` está repetido em cada rota.

São defeitos localizados, que um revisor pediria para corrigir. A parte do pattern está limpa.

## Testes

**Números:** 3 testes rodaram, com 0 falhas, 0 erros e 0 pulados; 3 métodos escritos, num arquivo só
(`src/test/java/com/pousada/MoneyTest.java`). A cobertura é de 6 de 696 linhas (0,9%) e 0 de 185 ramos (0,0%). Os
pacotes `rateplan`, `service`, `api` e `db` estão com 0% (`measures/tests.md`). Não há nenhum teste pela API nem
contra o banco.
**Nota:** ruim

Os três testes conferem valores, mas só contas soltas da classe `Money`: 249,90 × 1,2 = 299,88
(`MoneyTest.java:10-15`), 12% de 779,80 = 93,58 (`:17-22`) e 299,88 + 299,88 = 599,76 (`:24-29`). Nenhum teste toca
um plano de tarifa, a temporada por data, a máquina de situações, o cancelamento e a devolução, os códigos de erro ou
os relatórios. Na pergunta da mutação, trocar 12 por 21 em `CorporatePlan.java:12`, `FRIDAY` por `SATURDAY` em
`WeekendPackagePlan.java:23`, `>= 7` por `> 7` em `LongStayPlan.java:13` ou `0.5` por `1` em
`WeekendPackagePlan.java:42` não faria nenhum teste falhar. O pior é que o `mvn verify` passa com um jar que não sobe:
nenhum teste chega a registrar as rotas, que é onde o sistema quebra. Na prática, os testes estão quase ausentes.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date`. As conferências acusaram uma chave estrangeira sem índice
(`reservations.room_number` → `rooms`), as colunas de situação, plano, tipo e canal sem CHECK, nenhuma restrição
UNIQUE/EXCLUDE/CHECK, 0 transações explícitas, 0 `FOR UPDATE` e 9 trechos de SQL montados com `+`
(`measures/database.md`). O script registra que **o serviço não subiu contra um banco vazio**.
**Nota:** ruim

- **O serviço não sobe.** As tabelas chegam a ser criadas, porque o `Database.initialize()` roda antes das rotas, mas
  logo depois o processo cai no registro de `/reservations/:id` (`ReservationEndpoints.java:55`). Nenhum dado chega a
  entrar pela API. Mesmo com as rotas corrigidas, a leitura de reserva quebraria em toda chamada por causa de
  `rs.getString("room_type")` sobre um `SELECT` que traz `rm.type` (`ReservationService.java:89-92`, `:205`).
- **O esquema não evolui.** É `CREATE TABLE IF NOT EXISTS` dentro do Java (`Database.java:37-77`), sem migração.
  Uma coluna nova numa tabela que já existe nunca seria aplicada.
- **Nada protege a regra de uma reserva por quarto e noite.** A disponibilidade é um `SELECT COUNT(*)`
  (`ReservationService.java:187-199`) feito numa conexão. O `INSERT` vem depois, em outra conexão
  (`:59-75`), em autocommit (`Database.java:27`). Não há transação, trava nem restrição `EXCLUDE`. Dois pedidos
  simultâneos podem criar duas reservas para o mesmo quarto. A confirmação também grava o pagamento e a mensagem em
  conexões separadas, sem transação (`ReservationService.java:111-120`).
- **SQL montado com `+`.** O que entra é `int`, `LocalDate` ou `YearMonth` já convertido
  (`ReservationService.java:92`, `:191-194`; `ReportService.java:25-26`, `:58-59`; `RoomService.java:35`), então não
  há injeção hoje. Mesmo assim é a prática errada ao lado de `PreparedStatement` usado em outros pontos.
- **O que está certo.** Há um pool (Hikari, `Database.java:22-29`), conexões em try-with-resources, a chave
  estrangeira para o quarto (`Database.java:49`) e `NOT NULL` no que é obrigatório.

## Comportamento

**Números:** nenhuma conferência rodou: 0 passaram e 0 falharam. O roteiro registrou "não deu para testar (o serviço
não subiu contra um banco vazio)" (`measures/behavior.md`). O log (`measures/behavior-service.log`) mostra
`IllegalArgumentException: Path '/reservations/:id' invalid - Javalin 4 switched from ':param' to '{param}'`
em `ReservationEndpoints.java:55`.
**Nota:** ruim

Para o dono da pousada, o sistema entregue não funciona: o `java -jar` termina na hora e o site não tem a quem chamar.
Ninguém reserva, a recepção não faz check-in e o contador não tira relatório. O experimento numa cópia fora do
projeto mostra que corrigir só as rotas não bastaria. Os quartos aparecem, mas criar ou ler uma reserva devolve 500
(`room_type`, `ReservationService.java:205`), mesmo com a reserva já gravada no banco. Há outros desvios do texto que
o roteiro teria pegado:

- A confirmação exige um `amountPaid` no corpo (`ReservationEndpoints.java:75`), quando o texto diz que o pago é o
  total da reserva. Sem corpo, a resposta é 400.
- O comprovante escreve o dinheiro sem separador de milhar (`MessageService.java:96`): R$ 1749,30 em vez de
  R$ 1.749,30.
- O financeiro lista o cancelamento no plano normal com 0,00, porque filtra por `amount_paid > 0` e não pelo que ficou
  com a pousada (`ReportService.java:61`).
- Duas reservas simultâneas para o mesmo quarto e as mesmas noites podem passar juntas (ver Banco).

## Dúvidas

- Os valores do cenário (686,22, 820,00 e 410,00, 1.574,37) não puderam ser conferidos pela API. Pela leitura, as
  contas dos planos estão certas (`Money.java` usa HALF_EVEN a cada passo). No experimento, o convênio Tecnosul gravou
  779,80 / 93,58 / 686,22 no banco. Os outros exemplos não foram executados.
- O experimento com as rotas corrigidas foi feito numa cópia em `/tmp`, apagada depois. Ele não entra nos números e
  serve só para dizer o que o defeito da subida escondia.
- Na nota do design, entre `com problemas` e `ruim`: os defeitos (duplicação, tipos soltos, erros por texto,
  formatação dependente de locale) aparecem em vários arquivos. Ficou `com problemas` porque a parte central, os
  planos, está bem estruturada e fácil de mudar.
