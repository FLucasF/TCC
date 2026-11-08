# Revisão: construção, cenário 01

## Visão geral

O projeto tem seis pacotes: `handler` (rotas do Javalin), `service` (regras e SQL), `database` (pool Hikari e
`CREATE TABLE`), `domain` (entidades e enums), `util` e o `Application`. Uma requisição entra num método estático de
um handler (`Application.java:33-49`), que lê o corpo com Gson, chama um serviço e escreve a resposta com
`ctx.json(...)`. O serviço faz a regra e o SQL juntos e grava no PostgreSQL (`ReservationService.java:57-86`). Por
baixo, a conta está certa: os preços dos cinco planos saíram corretos no banco quando chamei a API à mão. O
problema mais sério é que o projeto não declara nenhum mapeador JSON. O Javalin precisa do `jackson-databind` para
`ctx.json`, e o `pom.xml` só traz o Gson (`pom.xml:44-48`). Por isso toda resposta em JSON, inclusive as de erro,
sai como **500**. O log do serviço (`measures/behavior-service.log`) mostra "It looks like you don't have an object
mapper configured". Na prática, o site não consegue usar o sistema: só 6 de 72 conferências do comportamento
passaram. Há ainda outros defeitos. A confirmação exige do cliente um `amountPaid`, em vez de usar o total. A
criação não tem proteção contra duas reservas ao mesmo tempo. O texto pede o Strategy e não há nenhum: os planos
estão em três `switch`. E só há dois testes.

## Arquitetura

**Números:** 6 pacotes, 9 dependências entre eles, nenhum ciclo. O Javalin aparece só em `com.pousada` e `handler`,
e o JDBC só em `database` e `service`. As maiores classes são `ReservationService` (362 linhas) e
`ReservationHandler` (356 linhas).
**Nota:** com problemas

As dependências seguem a direção certa (handler → service → database), sem ciclo, e o Javalin não entra nos
serviços. Mas as camadas estão misturadas em dois pontos:

- **O handler tem regra de negócio.** O comprovante e o e-mail de confirmação são montados no handler, com a
  formatação de dinheiro, datas e CPF (`handler/ReservationHandler.java:235-291`). Também é o handler que decide
  quando existe segunda via (`ReservationHandler.java:203`), e decide pelo valor pago, não pela situação da
  reserva. A montagem do comprovante aparece duas vezes, igual (`ReservationHandler.java:167-180` e `209-222`).
  Por isso o handler tem quase o tamanho do serviço.
- **O serviço monta SQL.** O acesso ao banco não está isolado: os serviços montam o SQL direto
  (`service/ReservationService.java:57-86`, `service/ReportService.java:77-117`). O mapeamento da linha para a
  reserva está copiado nos dois arquivos (`ReservationService.java:327-349`, `ReportService.java:119-139`).

A montagem não está num lugar só. O `Database` é estático e global (`database/Database.java:11`). O
`RoomService` guarda os quartos num `HashMap` estático (`service/RoomService.java:14`). Cada handler cria o seu
serviço num campo estático (`ReservationHandler.java:19`). Os serviços de relatório e de mensagens são só
métodos estáticos.

## Design de baixo nível

**Números:** dos 5 planos, nenhum está numa estrutura própria. O comportamento dos planos fica em 3 `switch`
sobre `RatePlan` no serviço: o preço (`ReservationService.java:261-267`), a validação (`208-226`) e a devolução
(`199-205`). `RatePlan` é um enum sem comportamento (`domain/RatePlan.java:3-9`).
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto avisa que "o comercial vive inventando plano" e que cada plano tem o seu preço, as suas
regras de uso e a sua devolução. A resposta esperada era o Strategy. O agente fez o contrário: um plano novo
obriga a mexer em três `switch` diferentes de `ReservationService`:

- `calculatePrice` (`ReservationService.java:261-267`), que chama cinco métodos privados, um por plano
  (`270-325`);
- `validateRatePlanRules`, com os casos de sexta a domingo, das 7 noites e do código da empresa
  (`ReservationService.java:209-225`);
- `calculateRefund` (`ReservationService.java:199-205`).

Os dados dos planos também ficam soltos no serviço: os descontos das empresas (`ReservationService.java:20-24`) e
os preços do pacote (`26-30`). Não há interface, classe por plano nem enum com corpo. É **omissão**.

**O exagero.** Não há estrutura sem necessidade: nenhuma interface, fábrica ou camada que só repassa. É
**contenção**.

**O resto do código.**

- **Duplicação.** O laço de preço com temporada está copiado em `calculateStandardPrice` e
  `calculateLongStayPrice` (`ReservationService.java:273-280` e `311-318`). O mapeamento da linha do banco e a
  montagem do comprovante também estão copiados, como dito na arquitetura.
- **Erros como texto.** Os erros são `IllegalArgumentException` com o código do erro na mensagem
  (`ReservationService.java:37,41,45,51,...`). O handler traduz a mensagem num `switch` de strings
  (`ReservationHandler.java:318-344`). Qualquer outra exceção vira 400 "Invalid request" sem log
  (`ReservationHandler.java:50-53`; `ReportHandler.java:30-33`), e isso esconde a causa. É o caso do `NullPointerException`
  na confirmação sem corpo (`ReservationHandler.java:81-83`).
- **A regra mistura os casos.** `guests < 1` vira `CAPACITY_EXCEEDED` (`ReservationService.java:40-42`), mas o
  texto pede `INVALID_REQUEST` para esse caso.
- **Dinheiro.** O dinheiro sai em `double`: `doubleValue()` no JSON (`ReservationHandler.java:305-311`) e
  `%.2f` com `doubleValue()` no CSV (`ReportService.java:56-69`). Com `double`, 520,00 sairia como `520.0`. O
  formato do CSV também depende do locale da JVM. Na formatação em reais falta o separador de milhar
  (`ReservationHandler.java:279-281`): o e-mail que gerei à mão traz "Diárias: R$ 1749,30" no lugar de
  "R$ 1.749,30".
- **Estado e código morto.** Há estado estático mutável (`RoomService.java:14`, `Database.java:11`). Há também
  código sem uso: `MoneyRounder.multiply/divide` (`util/MoneyRounder.java:11-17`) e o `gson` em `RoomHandler.java:11`.

Os tipos de domínio estão certos: `BigDecimal` com `HALF_EVEN` em cada passo (`MoneyRounder.java:8`), `LocalDate`
para as datas e enums para a situação e o plano. Os defeitos são vários, mas cada um fica num lugar só.

## Testes

**Números:** 2 testes rodaram, nenhum falhou; 2 métodos de teste escritos. A cobertura é de 10 de 781 linhas
(1,3%) e 10 de 196 ramos (5,1%). Só o pacote `util` é tocado. Não há teste pela API nem contra o banco.
**Nota:** ruim

O único arquivo, `src/test/java/com/pousada/ReservationTest.java`, confere duas coisas: as datas da alta
temporada (`ReservationTest.java:15-22`) e o arredondamento de 249,90 × 1,2 (`ReservationTest.java:25-29`).
Nada confere os planos, as situações da reserva, o cancelamento, a devolução, os erros, os relatórios ou o
e-mail. A parte nova do cenário também fica sem teste: os 12/18/25% do convênio, o preço fechado do pacote e a
metade devolvida, as 7 noites e os 10% da estadia longa. Se alguém trocasse `0.5` por `1` na devolução do pacote
(`ReservationService.java:203`), ou `< 7` por `<= 7` (`ReservationService.java:221`), nenhum teste falharia.
Também não há nenhum teste que suba o serviço e chame a API, que é o que pegaria o 500 de todas as respostas JSON.
É uma coluna quase ausente.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária, e o dinheiro está todo em
`numeric(10,2)`. O script acusou estes pontos:

- a chave estrangeira `reservations(room_number) → rooms` não tem índice;
- não há `CHECK` em `status`, `rate_plan`, `type` e `channel`;
- não há nenhuma restrição que proteja uma regra (UNIQUE, EXCLUDE ou CHECK);
- há 0 transações explícitas e 0 `FOR UPDATE`.

**Nota:** com problemas

O que está certo: os tipos (datas em `date`, dinheiro em `numeric(10,2)`, `Database.java:30-56`), a reserva
ligada ao quarto por chave estrangeira (`Database.java:41`), o SQL sempre com parâmetros, as conexões devolvidas
por try-with-resources e o pool Hikari (`Database.java:13-23`). No teste à mão, os dados ficaram no banco com os
valores certos.

Os defeitos:

- **O esquema não evolui.** Ele é criado no código com `CREATE TABLE IF NOT EXISTS` (`Database.java:29-68`),
  sem migrações. A próxima coluna, como a de um plano novo, não vai chegar a um banco que já existe.
- **Nada impede reserva dupla.** A criação confere a disponibilidade (`ReservationService.java:50`, consulta em
  `239-253`) e depois insere com outra conexão (`ReservationService.java:57-79`). Não há transação, trava nem
  restrição de exclusão. Dois pedidos ao mesmo tempo podem gravar duas reservas do mesmo quarto na mesma noite.
  O roteiro não conseguiu provar nem desmentir isso, porque as duas chamadas voltaram 500.
- **Falta índice na chave estrangeira.** O `room_number`, usado em toda consulta de disponibilidade, não tem
  índice.

## Comportamento

**Números:** 6 de 72 conferências passaram.

| parte | passaram |
|---|---|
| a parte comum | 4 de 46 |
| depois de reiniciar | 1 de 3 |
| cenário 01 (os planos novos) | 0 de 21 |
| reservas ao mesmo tempo | 1 de 2 |

**Nota:** ruim

**Toda resposta JSON volta 500.** `GET /rooms`, a criação e todas as respostas de erro voltam 500 com a página
do Javalin pedindo o `jackson-databind` (`measures/behavior.md`; `pom.xml` sem Jackson). Os erros que deviam
ser 400, 404, 409 ou 422 também saem como 500, porque a resposta de erro também é escrita com `ctx.json`. Só
funcionam os relatórios CSV, que usam `ctx.result`.

As falhas em cadeia vêm daí. A criação devolve 500 e o roteiro não recebe o `id`. Por isso confirmar,
check-in, check-out, cancelar e a segunda via respondem 404 de rota, e os relatórios saem vazios. As 21
conferências do cenário falham pelo mesmo motivo.

**A regra por baixo está certa.** Chamei a API à mão e conferi no banco: o Tecnosul deu 779,80 / 93,58 /
686,22; o pacote deluxe deu 820,00 e devolveu 410,00 no cancelamento; a estadia longa deu 1.749,30 / 174,93 /
1.574,37; a estadia longa com 2 noites foi recusada. Mas o hóspede e o site não veem nada disso, só o 500.

**Outros defeitos, que apareceriam mesmo com o JSON consertado:**

- a confirmação sem corpo dá erro (`ReservationHandler.java:81-89`). Com corpo, grava como pago o valor que o
  cliente mandar, e não o total da reserva, como o texto pede;
- a segunda via de uma reserva pendente responde 404, e não 409 `OPERATION_NOT_ALLOWED`
  (`ReservationHandler.java:203-206`). Uma reserva paga e depois cancelada ainda tem segunda via;
- cancelar uma reserva pendente deixa o `refund` em `null`, e não `0.00` (`ReservationService.java:195-197`);
- zero hóspedes responde 422 (`ReservationService.java:40`);
- o financeiro lista a STANDARD cancelada e devolvida com valor 0,00 (`ReportService.java:103`). No teste à mão
  ela apareceu como `1,101,...,CANCELLED,0.00`, mas não deixou dinheiro com a pousada;
- o comprovante não tem separador de milhar.

Para o dono da pousada: do jeito que foi entregue, o site não consegue reservar nem ver quartos, e não recebe
nenhuma mensagem de erro que preste.

## Dúvidas

- A conferência de "duas reservas ao mesmo tempo" deu 1 de 2 só porque as duas chamadas voltaram 500. Ela não
  mostra se o sistema impede ou não a reserva dupla. Pela leitura do código (consulta e inserção separadas, sem
  transação nem restrição), a reserva dupla é possível. Não a reproduzi.
- O texto não diz qual código usar na segunda via de reserva não paga. O roteiro espera 409, e o projeto dá 404.
  Pesou pouco diante do 500 geral.
