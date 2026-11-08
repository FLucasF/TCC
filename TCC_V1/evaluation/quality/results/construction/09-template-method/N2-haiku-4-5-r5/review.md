# Revisão: construção, cenário 09

## Visão geral

O projeto tem seis pacotes: `com.pousada` (o `App`, com as rotas Javalin), `service` (regras: reserva, preço, temporada, comprovante), `report` (os quatro CSV), `repository` (JDBC), `database` (o pool Hikari e o `schema.sql`) e `domain`. Uma requisição entra numa lambda do `App`, que lê o corpo com Gson, chama um método `static` de `ReservationService`, que chama métodos `static` dos repositórios, que usam o `DataSource` guardado num campo `static` (`Database.java:12`). Os dados ficam no PostgreSQL, com o esquema criado por `CREATE TABLE IF NOT EXISTS`, mas os quartos são lidos de um mapa em memória (`RoomRepository.java:12-20`). O defeito mais sério: o `pom.xml` não traz o `jackson-databind`, e todas as respostas JSON (`ctx.json`, inclusive a do tratador de erros, `App.java:46`) saem como 500. Assim a API não funciona: só 12 de 64 conferências passaram. Um segundo problema: as regras rodam antes de a resposta falhar, então as reservas são gravadas mesmo com o cliente recebendo 500. Sem trava nem restrição no banco, duas reservas simultâneas do mesmo quarto foram gravadas nas 10 rodadas que testei à mão. Os relatórios seguem o Template Method pedido.

## Arquitetura

**Números:** 6 pacotes, 11 dependências entre eles, nenhum ciclo. Javalin só em `com.pousada`, JDBC só em `database` e `repository`. A maior classe é `App.java`, com 278 linhas.
**Nota:** com problemas

As camadas existem e as dependências vão no sentido certo: borda → serviço/relatórios → repositório → banco, sem ciclo. As regras não conhecem Javalin nem SQL (`ReservationService.java`, `PricingService.java`). Os problemas:
- **Não há montagem.** Tudo é `static`: `ReservationService.createReservation` (`ReservationService.java:12`), os repositórios (`ReservationRepository.java:14`) e o `Database` com `dataSource` estático mutável (`Database.java:12`, `:33`). Nenhuma peça é criada e ligada num lugar, e nenhuma pode ser trocada num teste.
- **A borda HTTP não é fina.**
  - O comprovante é montado inteiro duas vezes dentro dos handlers (`App.java:134-148` e `App.java:159-173`).
  - A regra "segunda via só para reserva paga" está no handler (`App.java:155`), não no serviço.
  - Os handlers vão direto ao repositório (`App.java:134`, `App.java:159`).
- **Os relatórios buscam dados no construtor** chamando o repositório estático (`RevenueReport.java:19`, `OccupancyReport.java:19-29`). Por isso não dá para gerar um relatório sem banco.

## Design de baixo nível

**Números:** 4 de 4 relatórios passam pelo esqueleto `Report.generate()`, com 4 passos abstratos. Nenhum relatório fica fora dele. A linha de título ("Pousada Maré Alta," + nome + período) está copiada nas 4 subclasses.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** `Report.java:10-25` é o método-modelo: título, cabeçalho, linhas e total, sempre nessa ordem e montados num lugar só. Os passos `buildTitle`, `buildHeader`, `buildDataLines` e `buildTotalLine` são abstratos (`Report.java:27-30`). As quatro subclasses preenchem só esses passos: `OccupancyReport`, `RevenueReport`, `TourismTaxReport` e `RatePlanReport`. Isso inclui os dois relatórios da parte comum, e as quatro rotas usam o tipo `Report` (`App.java:188`, `204`, `220`, `236`). A linha com a data, que vem no ano que vem, entra em `generate()` e vale para todos.

O que afasta do ideal:
- `generate()` não é `final` (`Report.java:10`).
- O prefixo do título está copiado em cada subclasse (`OccupancyReport.java:35`, `RevenueReport.java:27`, `TourismTaxReport.java:29`, `RatePlanReport.java:28`). Se o formato do título mudar, são quatro lugares a mexer. O esqueleto devia receber só o nome e o período.
- A base tem estado mutável protegido sem uso (`title` e `dataLines`, `Report.java:7-8`) e um método morto (`Report.java:32-34`).
- A seleção das reservas do mês está copiada nos três relatórios mensais (`RevenueReport.java:19-22`, `TourismTaxReport.java:21-24`, `RatePlanReport.java:20-23`).

**Exagero.** Não há interfaces, fábricas nem camadas que só repassam. `Money` e `PricingResult` fazem trabalho de verdade.

**O resto do código.**
- **Duplicação:**
  - o JSON do comprovante, duas vezes (`App.java:135-147` e `App.java:160-172`);
  - o cálculo "pago menos devolvido", quatro vezes (`RevenueReport.java:41-44` e `:60-63`, `RatePlanReport.java:47-50` e `:68-71`);
  - a taxa, duas vezes (`TourismTaxReport.java:43-46` e `:63-66`).
- **Tipos soltos:**
  - situação comparada por texto (`OccupancyReport.java:25`) e passada como `String` (`RevenueReport.java:21`);
  - tipo do quarto como `String` (`Room.java:7`);
  - dinheiro convertido para `double` na saída (`App.java:275-276`), o que perde as 2 casas: 0.00 sai 0.0.
- **Estado:** campos públicos em todo o domínio (`Reservation.java:7-22`) e `static` mutável (`Database.java:12`).
- **Erros:**
  - `catch (Exception)` transforma qualquer falha em 400 INVALID_REQUEST (`App.java:89-90`, `114-115`), inclusive uma falha do banco ou da serialização;
  - os relatórios fazem o mesmo, e a mensagem diz "Invalid date format" para qualquer erro (`App.java:191-192`);
  - `findById` devolve `null` (`ReservationRepository.java:70`).

São defeitos localizados num código pequeno e legível, por isso `com problemas`, não `ruim`.

## Testes

**Números:** 13 testes rodaram, 0 falharam, 13 métodos escritos. Cobertura de 41 de 726 linhas (5,6%) e de 16 de 190 ramos (8,4%). Os pacotes `report`, `repository`, `com.pousada` e `database` estão em 0%. Nenhum teste chama a API ou o banco.
**Nota:** ruim

Só há dois arquivos: `SeasonalityCalculatorTest` (9 testes da temporada) e `PricingServiceTest` (4 testes de preço).
- Os testes de preço da alta temporada calculam o esperado com o próprio `Money` do código (`PricingServiceTest.java:37-39`, `:66-70`), em vez de conferir R$ 599,76. Um erro no `Money` passaria.
- O teste do desconto não reembolsável confere valores fixos (`PricingServiceTest.java:52-54`). Esse é bom.
- Ficam sem teste: as situações da reserva, o cancelamento e a devolução, as recusas e os códigos de erro, o comprovante, o e-mail, os quatro relatórios (a parte nova do cenário) e a API.

A pergunta da mutação, na parte nova:
- trocar R$ 4,50 por 5,40 (`TourismTaxReport.java:14`): nenhum teste falha;
- tirar o `NON_REFUNDABLE` cancelado da conta: nenhum teste falha (o código já faz isso errado, veja Comportamento);
- apagar a linha de título de `Report.generate()`: nenhum teste falha.

O `mvn verify` passa justamente porque nada sobe o servidor. Um único teste pela API teria mostrado que nenhuma resposta JSON funciona.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Dinheiro em `numeric(10,2)` (6 colunas, todas ok) e datas em `date`. A chave estrangeira `reservations.room_number` tem índice. Nenhum CHECK, UNIQUE ou EXCLUDE. Nenhuma transação explícita, nenhum `FOR UPDATE`. O SQL montado com `+` só usa constantes (`RoomRepository.java:26-28`).
**Nota:** ruim

O básico está certo: tipos, chaves, `NOT NULL`, SQL com `?`, pool Hikari e try-with-resources em todos os acessos. Os problemas:
- **Nada impede a reserva dupla.** A criação lê os conflitos e depois insere, sem transação nem trava (`ReservationService.java:32-43`), e o banco não tem restrição de sobreposição (`schema.sql:8-26`).
  - Testei à mão: 10 rodadas de dois `POST /reservations` simultâneos para o quarto 202 e as mesmas noites. As 10 rodadas gravaram as duas reservas (`count = 2` para cada data).
  - É dado errado no banco: o mesmo quarto vendido duas vezes.
- **Escritas que deviam andar juntas vão separadas.** Confirmar faz dois `UPDATE` sem transação (`ReservationService.java:56-57`); cancelar também (`ReservationService.java:76`, `:86`).
- **O esquema não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` (`Database.java:59-77`), sem migrações, e não saberá acrescentar uma coluna.
- **A tabela `rooms` é escrita e nunca lida.** Os quartos vêm de um mapa estático (`RoomRepository.java:36-42`), então o banco e o código podem divergir.
- **Sem CHECK nas colunas de situação e de plano** (`schema.sql:17-18`).

## Comportamento

**Números:** 12 de 64 conferências passaram:
- parte comum: 4 de 46;
- reiniciar: 1 de 3;
- cenário 09: 6 de 13;
- reservas simultâneas: 1 de 2.

**Nota:** ruim

**A causa principal.** O projeto não tem o `jackson-databind`, e o Javalin 6 precisa de um mapeador de JSON para `ctx.json`. O log (`measures/behavior-service.log`) repete "It looks like you don't have an object mapper configured". Isso quebra:
- `GET /rooms`, que dá 500;
- a criação da reserva, que dá 500;
- todos os códigos de erro (404, 409, 422, 400), que saem 500, porque o tratador também usa `ctx.json` (`App.java:46`, `:54`);
- as mensagens, a confirmação, o check-in e o check-out.

Para o dono da pousada, o site não consegue fazer nem mostrar nenhuma reserva. Os relatórios em CSV respondem, mas vazios: o roteiro não descobre o número das reservas e não consegue confirmá-las.

**Os efeitos que verifiquei à mão.** Com o serviço de pé, a reserva é gravada mesmo com o 500, e os relatórios saem certos para ela:
- ocupação `101,STANDARD,1,2027-07-10,2027-07-12` e `total,1/5`;
- taxa de 2×2×4,50 = `18.00`;
- planos `STANDARD,1,599.76` e `NON_REFUNDABLE,0,0.00`.

O site, porém, recebe erro e tenta de novo.

O "OK" das reservas simultâneas ("nenhuma com as duas criadas") é falso: o roteiro só viu dois 500. No banco, as duas foram gravadas nas 10 rodadas (veja Banco).

**Defeitos que o roteiro não alcançou, por causa do 500, e que a leitura mostra:**
- A confirmação exige `amountPaid` no corpo (`App.java:105-107`), e o texto diz que o pago é o total. Uma confirmação sem corpo dá 400, e o hóspede poderia "pagar" qualquer valor.
- Cancelar uma reserva pendente ou não reembolsável deixa `refund` como `null`, não 0.00 (`ReservationService.java:85-87`).
- O financeiro e o de planos deixam de fora a reserva NON_REFUNDABLE cancelada, porque buscam só CONFIRMED, CHECKED_IN e CHECKED_OUT (`RevenueReport.java:19-22`, `RatePlanReport.java:20-23`). O texto diz que ela continua contando. O contador veria menos dinheiro do que ficou com a pousada.
- A segunda via sai para reserva cancelada (`App.java:155` só recusa a pendente).
- O comprovante escreve o dinheiro sem separador de milhar, "R$ 1234,56" (`DocumentService.java:11`, `:35`). O texto pede "R$ 1.234,56".
- O dinheiro no JSON passa por `double` (`App.java:275-276`) e não sai com 2 casas.

## Dúvidas

- **Banco, `ruim` ou `com problemas`.** A skill do banco põe "a duplicação protegida só dentro do processo" em `com problemas`. Aqui não há proteção nenhuma, e a duplicação foi verificada no banco nas 10 rodadas. Por isso dei `ruim`, pelo risco de dado errado.
- **Design, classe.** Dei `acerto` porque os quatro relatórios passam pelo esqueleto. Mesmo assim, o prefixo do título está copiado nas quatro subclasses, e o método-modelo não é `final`.
- **Defeitos que nenhum roteiro executou.** Os de comportamento listados por leitura (confirmação, `refund`, cancelada no financeiro, milhar no comprovante) não foram executados pelo roteiro, por causa do 500 geral. O do financeiro também não foi testado à mão: vem só da leitura do código.
