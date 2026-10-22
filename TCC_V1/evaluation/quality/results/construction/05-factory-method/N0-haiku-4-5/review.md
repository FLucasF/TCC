# Revisão: construção, cenário 05

## Visão geral

O projeto tem 7 pacotes (`api`, `service`, `model`, `database`, `exception`, `util` e a raiz) e cerca de 1.600 linhas.
Uma requisição entra num handler de `api` (por exemplo, `ReservationHandler.createReservation`), que lê o JSON à mão
com `JsonNode` e chama um serviço de `service`. O serviço tem as regras e também o SQL (JDBC puro, uma conexão nova do
`DriverManager` a cada chamada, `database/Database.java:17-18`). Os dados ficam no PostgreSQL, em 3 tabelas criadas
com `CREATE TABLE IF NOT EXISTS` ao subir (`database/Database.java:27-70`). A escolha do documento por tipo de hóspede
fica num método só, `DocumentService.generateDocument` (`service/DocumentService.java:19-55`).

O mais sério é que o sistema não funciona pela API. O `pom.xml` não traz o módulo `jackson-datatype-jsr310`, então
toda resposta que leva um `LocalDate` (a reserva e o documento) falha ao virar JSON. O `catch (Exception)` dos
handlers (`api/ReservationHandler.java:66-67`) transforma essa falha em `400 INVALID_REQUEST`. O resultado:
`POST /reservations` grava a reserva no banco e responde 400 ao site. O site nunca recebe o número da reserva, e
nenhuma reserva chega a ser confirmada, a fazer check-in ou check-out, nem gera e-mail ou documento. O roteiro passou
em 24 de 83 conferências. Além disso, o banco não protege o quarto contra duas reservas ao mesmo tempo: nas 10 rodadas
do roteiro, as duas reservas simultâneas foram gravadas. Os testes são praticamente inexistentes (1% das linhas).

## Arquitetura

**Números:** 7 pacotes, 10 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). `api` depende de
`database`, `exception`, `model` e `service`. `service` usa JDBC. A maior classe é `ReservationService`, com 364 linhas.
**Nota:** com problemas

- **O que está bom.** Não há ciclos. As regras não conhecem o Javalin: nenhum `Context` aparece em `service`, e o
  Javalin só aparece em `api` e na raiz.
- **Não há camada de dados.** O SQL mora nos serviços, ao lado das regras. Em `service/ReservationService.java`, o
  cálculo de preço (`:278-315`) e as regras de situação (`:121-202`) convivem com `INSERT`, `UPDATE` e `SELECT`
  escritos à mão (`:60-95`, `:131-138`, `:204-276`). O mesmo vale para `RoomService` e `MessageService`. Mudar a regra
  ou o banco mexe na mesma classe.
- **A montagem é espalhada.** `Application.java:30-33` passa o `Database` a cada handler, e cada handler monta os
  próprios serviços:
  - `api/ReservationHandler.java:25-29` cria `ReservationService`, `MessageService`, `DocumentService` e um segundo
    `RoomService`;
  - `api/ReportHandler.java:23-26` cria outro `ReservationService` e outro `RoomService`;
  - `service/ReservationService.java:23` faz `new RoomService(db)` por dentro.

  Por isso existe a dependência `api -> database`.
- **Regras na borda HTTP.**
  - O texto do e-mail de confirmação é montado no handler (`api/ReservationHandler.java:112-119`).
  - A regra "segunda via só para reserva paga" fica no handler (`api/ReservationHandler.java:200-204`).
  - O relatório de ocupação filtra as situações no handler (`api/ReportHandler.java:53`).
  - O valor que fica com a pousada (pago menos devolvido) é calculado no handler (`api/ReportHandler.java:101-104`).
  - O handler de confirmação aceita um `amountPaid` vindo do corpo (`api/ReservationHandler.java:93-105`), que é uma
    regra de negócio decidida na borda.

## Design de baixo nível

**Números:** 3 tipos de documento. Os 3, nos 3 lugares (e-mail na confirmação `api/ReservationHandler.java:110`,
check-out `:181`, segunda via `:206`), passam pela mesma escolha em `service/DocumentService.java:25-34`. Nenhum lugar
cria o documento à mão.
**Classe:** acerto
**Exagero:** contenção
**Nota:** ruim

**O pattern.** É uma fábrica concentrada: `generateDocument` decide entre comprovante, fatura e *booking receipt* pela
identificação da reserva (`service/DocumentService.java:25-34`). Os três pontos de saída pedem o documento a ela, e
nenhum deles pergunta qual é o tipo de hóspede. A referência aceita essa forma como acerto. Uma agência entraria como
mais um ramo nesse único lugar.

O que afasta da forma ideal:
- O documento devolvido é sempre a mesma classe `Document`. Só o título e o texto mudam: o que varia são três métodos
  privados que montam `String` (`:57-129`), não três produtos.
- A escolha pergunta qual campo não é nulo, e não um tipo de hóspede. A regra "exatamente uma identificação" fica em
  outro lugar (`service/ReservationService.java:317-338`), então as duas peças precisam andar juntas.
- Os três geradores repetem as linhas de quarto, datas, diárias e desconto (`:64-75`, `:90-101`, `:115-126`).

**O exagero.** Não há interface, fábrica ou camada sem necessidade. Contenção.

**O resto do código.** Os defeitos estão espalhados:
- **Erros mascarados.** Os sete handlers têm o mesmo `try/catch`, que transforma qualquer `Exception` em
  `400 INVALID_REQUEST` (`api/ReservationHandler.java:64-68`, `:81-85`, `:122-126` …;
  `api/ReportHandler.java:74-77`, `:123-126`). Foi isso que escondeu a falha de serialização que derruba o sistema. Um
  erro de banco também viraria "requisição inválida". A classe `ErrorResponse` está copiada em dois lugares
  (`api/ReservationHandler.java:247`, `api/ReportHandler.java:136`).
- **Texto solto no lugar de tipos.** A situação e o plano são `String` em todo o código (`model/Reservation.java:47`,
  `:62`; comparações em `service/ReservationService.java:127`, `:185`, `:190`). O plano não é validado: um `ratePlan`
  desconhecido é aceito e tratado como sem desconto (`service/ReservationService.java:297-303`).
- **Regra da alta temporada errada.** `isHighSeason` conta dezembro inteiro, quando o texto diz "a partir de 15 de
  dezembro", e deixa 29 de fevereiro de fora (`service/ReservationService.java:310-312`).
- **Dinheiro e números.** `new BigDecimal(body.get("amountPaid").asDouble())` (`api/ReservationHandler.java:95`)
  cria dinheiro a partir de `double` e deixa o cliente dizer quanto pagou. `getIntField` devolve 0 quando o campo falta
  (`api/ReservationHandler.java:228-233`), então um `roomNumber` ausente vira `404 ROOM_NOT_FOUND` em vez de 400.
- **Código morto e errado.** `MoneyUtils.formatBRL_EN` (`util/MoneyUtils.java:37-42`) trata o valor como centavos
  inteiros. `formatBRLRaw` também não é usado. `formatAmount` do CSV usa `String.format("%.2f")` com o locale da
  máquina (`api/ReportHandler.java:133`).
- **Duplicação.** A reserva é buscada e conferida (404) duas vezes, no handler e no serviço
  (`api/ReservationHandler.java:133-138` e `service/ReservationService.java:180-183`, o mesmo em check-in e
  check-out). O `SELECT` das 20 colunas está copiado três vezes (`service/ReservationService.java:102-106`, `:207-213`,
  `:234-241`).

## Testes

**Números:** 4 testes rodaram, 0 falharam; 4 métodos escritos. Cobertura de 8 de 772 linhas (1,0%) e 2 de 236 ramos
(0,8%). Nenhuma linha coberta em `api`, `service`, `model` ou `database` (`measures/tests.md`).
**Nota:** ruim

- `ReservationServiceTest.java:7-9` é `assertTrue(true)`.
- `MoneyUtilsTest.testCalculationExample` (`MoneyUtilsTest.java:23-37`) refaz o cálculo de 599,76 dentro do próprio
  teste, sem chamar o código do sistema. Se a regra da temporada ou do preço mudasse, ele continuaria passando.
- Os testes que de fato exercitam código conferem só o arredondamento e o `R$ 1.234,56` (`MoneyUtilsTest.java:10-21`).

Fica tudo sem teste:
- os preços, a temporada (o erro de dezembro passaria) e o desconto;
- as situações e o cancelamento com devolução;
- os códigos de erro e os relatórios;
- a parte nova inteira: a validação da identificação, a fatura com o ISS de 5% e o *booking receipt*.

Trocar `0.05` por `0.5` em `DocumentService.java:82` ou apagar o ramo da fatura não derrubaria nenhum teste. Não há
teste pela API nem contra o banco, e por isso ninguém viu que nenhuma reserva sai em JSON.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date`. As conferências acusaram (`measures/database.md`):
- a chave estrangeira `reservations(room_number)` sem índice;
- nenhum CHECK em `status`, `rate_plan`, `type` e `channel`;
- nenhuma restrição UNIQUE/EXCLUDE;
- nenhum pool;
- nenhuma transação explícita e nenhum `FOR UPDATE`.

**Nota:** ruim

**O que está bom.** Os tipos estão certos e todo o SQL usa parâmetros (`?`), sem dado de fora concatenado.

**Duas reservas no mesmo quarto.** Nada protege a regra "um quarto não pode ter duas reservas na mesma noite":
- a disponibilidade é um `SELECT COUNT(*)` numa conexão (`service/ReservationService.java:258-276`);
- o `INSERT` vai em outra conexão, sem transação nem trava (`:60-86`);
- o banco não tem restrição de exclusão.

Isso não é teórico. Depois do roteiro de comportamento, o banco `review_behavior_2` tinha 10 pares de reservas não
canceladas do quarto 301 nas mesmas noites (de 2030-02-02 a 2030-02-20): as 10 rodadas simultâneas gravaram as duas
reservas. A API respondeu 400 às duas, por causa da falha de JSON, e por isso o roteiro não acusou a gravação dupla. É
dado errado gravado: o quarto vendido duas vezes.

**Outros pontos.**
- O esquema é criado com `CREATE TABLE IF NOT EXISTS` no Java (`database/Database.java:27-70`), sem migrações. Uma
  coluna nova (o documento das agências, por exemplo) não chega a um banco que já existe.
- Não há pool: cada consulta abre uma conexão nova pelo `DriverManager` (`database/Database.java:17-18`). Uma
  confirmação abre umas cinco.

## Comportamento

**Números:** 24 de 83 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 17 de 46 |
| depois de reiniciar | 2 de 3 |
| o cenário 05 | 4 de 32 |
| duas reservas ao mesmo tempo | 1 de 2 |

**Nota:** ruim

**A falha de JSON.** Toda resposta que contém a reserva ou o documento falha com "Java 8 date/time type
`java.time.LocalDate` not supported by default" (a mensagem aparece no corpo do 400). Falta o
`jackson-datatype-jsr310` no `pom.xml` para os `LocalDate` de `model/Reservation.java:38` e `model/Document.java:41`.

**O que isso quer dizer para o dono da pousada.** O site não consegue criar uma reserva. Ele recebe 400, mas a reserva
fica gravada no banco como PENDING e ocupa o quarto (o `ROOM_UNAVAILABLE` seguinte é obtido por causa dessas reservas
fantasmas). Também não consegue consultá-la. Sem o número, nada mais anda:
- nenhuma confirmação, e portanto nenhum e-mail ("o hóspede recebe o e-mail": 0);
- nenhum check-in ou check-out, nenhuma segunda via, nenhum cancelamento;
- os relatórios saem vazios (`total,0/5` e `total,,,,0.00`).

**O cenário.** Nada da parte nova aparece pela API: nem comprovante, nem fatura, nem *booking receipt*. Só passaram as
4 recusas de identificação inválida (`service/ReservationService.java:317-338`), que acontecem antes de qualquer
serialização.

**O que funciona.** O que não depende de `LocalDate`: `GET /rooms`, as recusas 400/404/409/422 e o formato do mês.

**O que a leitura mostra.** Os preços calculados estão certos no banco (599,76 e 212,42 em `review_behavior_1`). Os
dados continuam depois de reiniciar: as 4 reservas e as mensagens estavam lá. A falha "a reserva continua lá" vem do
número que nunca chegou ao roteiro.

**Defeitos que o roteiro não alcançou, mas o código mostra:**
- a temporada de dezembro errada (`service/ReservationService.java:310`);
- `@JsonInclude(NON_NULL)` em `model/Reservation.java:8` e `model/Document.java:8`, que tira do JSON os campos que o
  texto pede como `null`.

## Dúvidas

- Classe do pattern: dei acerto porque a escolha está num lugar só e os três pontos de saída a usam. Mas é a forma
  mais fraca que a referência aceita: um método com `if/else` sobre campos nulos, que devolve sempre a mesma classe
  `Document` com texto diferente. Um revisor mais rígido poderia ler isso como fábrica pela metade.
- Sem a falha de JSON, não dá para saber pelo roteiro se os textos da fatura e do *booking receipt* sairiam exatamente
  como o texto pede. A leitura de `service/DocumentService.java:80-129` indica que sim (ISS arredondado meio para o
  par, `BRL #,##0.00`, "1 night"), mas isso não foi medido.
