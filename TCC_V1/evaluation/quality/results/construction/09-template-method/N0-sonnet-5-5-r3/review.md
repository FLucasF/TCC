# Revisão: construção, cenário 09

## Visão geral

O projeto tem seis pacotes: `web`, `service`, `report`, `domain`, `db` e a raiz com `App` e `Config`. Uma requisição entra numa rota de uma linha em `web/Api.java:36-52`. O corpo do POST é validado em `web/RequestParser.java`. Depois a rota chama `ReservationService`, que abre uma transação (`db/Database.java:98`), usa os repositórios JDBC (`db/*Repository.java`) e as regras dos objetos de domínio (`domain/Quote.java`, `domain/Reservation.java`). Os dados ficam no PostgreSQL, em três tabelas criadas por um `CREATE TABLE IF NOT EXISTS` dentro do código (`db/Database.java:17-58`). Os quatro relatórios implementam `report/Report.java`. Cada um devolve um `ReportTable`, e um único `ReportRenderer` escreve o CSV (`report/ReportRenderer.java:23-30`).

Não encontrei defeito sério. As 64 conferências do roteiro passaram, e os 8 testes do agente também. O que há para apontar:
- o esquema não tem migrações versionadas e nenhuma restrição CHECK;
- falta um teste para a regra "só as reservas pagas" da taxa de turismo;
- há algumas sobras de código (código morto, um tipo reinventado).

## Arquitetura

**Números:** 6 pacotes, 13 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin só aparece na raiz e em `web`. O JDBC aparece em `db` e, como tipo `Connection`, em `service` e `report`. A maior classe é `ReservationService`, com 141 linhas.
**Nota:** bom

- **Borda HTTP fina:** cada rota só lê o `id` ou o corpo, chama o serviço e escreve o JSON (`web/Api.java:36-45`). As rotas dos relatórios são registradas num laço sobre o catálogo (`web/Api.java:47-52`), sem regra nenhuma dentro.
- **Regras sem Javalin e sem SQL:** `ReservationService` não conhece o `Context`. Ele recebe um `Connection` só para repassar a transação aos repositórios (`service/ReservationService.java:45-57`, `:124-132`). Todo o SQL fica em `db/` (`db/ReservationRepository.java`, `db/RoomRepository.java`, `db/MessageRepository.java`). O preço e as transições de situação ficam no domínio (`domain/Quote.java:11-31`, `domain/Reservation.java:24-47`).
- **Montagem num lugar só:** tudo é criado e ligado em `App.start` (`App.java:28-45`), sem `static` global. A taxa de turismo é injetada por configuração (`Config.java:13,19`; `App.java:39`).

Ressalvas pequenas, que não mudam a nota:
- `ApiException`, que carrega o status HTTP, mora em `service` (`service/ApiException.java:7`). Por causa dela, `report` passa a depender de `service` só para lançar o erro 400 (`report/PeriodKind.java:3,26`).
- O domínio conhece o Jackson por uma anotação (`domain/Receipt.java:3,31`).
- `Report.build` recebe um `Connection` (`report/Report.java:15`). Assim, os relatórios ficam presos ao JDBC, mas continuam sem SQL próprio: tudo passa por `ReservationRepository` e `RoomRepository`.

## Design de baixo nível

**Números:** 4 relatórios, e os 4 passam pela mesma estrutura: interface `Report`, `ReportTable`, `ReportRenderer.render` e `ReportCatalog.csv`. Nenhum relatório fica fora dela, e nenhum escreve a própria linha de título.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** bom

**O pattern.** A solução é por composição, e não por classe abstrata.
- O esqueleto (título, cabeçalho, linhas, total) está num lugar só, em `report/ReportRenderer.java:23-30`. A linha de título está em `:25`, e o comentário em `:8-11` reserva esse lugar para a futura "linha com a data".
- O caminho completo de um relatório também é um só: validar o período, abrir a transação, montar a tabela e escrever o CSV (`report/ReportCatalog.java:25-29`).
- Cada relatório só preenche as partes que variam: o nome, o tipo de período, as colunas, as linhas e o total (`report/OccupancyReport.java:30-42`, `report/RevenueReport.java:24-36`, `report/TourismTaxReport.java:28-42`, `report/RatePlanReport.java:27-39`). Isso vale também para os dois relatórios que o texto já descrevia.
- A seleção comum "reservas que começam no mês / que deixaram dinheiro" está em `report/MonthlyReservations.java:16-24`. Por isso o relatório financeiro e o de planos contam as mesmas reservas.
- Um relatório novo é uma classe e uma linha em `App.java:36-40`. A rota sai sozinha (`web/Api.java:47`).

O que afasta do acerto: não há método-modelo `final` numa classe abstrata. Cada relatório ainda escreve o próprio laço e soma o próprio total. A referência aceita isso como alternativa equivalente: um escritor genérico que recebe as partes que variam, com os quatro relatórios passando por ele.

**O exagero.** Não há estrutura sem necessidade. `PeriodKind` (`report/PeriodKind.java:9-38`) se justifica, porque há dois tipos de período (data e mês). `ReportTable` é um record de dados. Não há fábrica, builder nem camada que só repassa.

**O resto do código.** Um revisor exigente aceitaria o código, com pequenas observações:
- **Tipos certos:** `BigDecimal` com HALF_EVEN em cada passo (`domain/Money.java:15-17`, `domain/Quote.java:18-24`), `LocalDate` para datas e `enum` para situação, plano, tipo e canal. O reembolso fica no próprio plano (`domain/RatePlan.java:6-7`; `domain/Reservation.java:34-38`).
- **Pequenas sobras:**
  - `ReportCatalog.get` nunca é chamado (`report/ReportCatalog.java:31-35`), nem `ReservationStatus.ACTIVE` (`domain/ReservationStatus.java:13`).
  - A busca de sobreposição repete o texto `'CANCELLED'` em vez de usar o enum (`db/ReservationRepository.java:77`).
  - A interface privada `Rule` reinventa `Predicate` (`service/ReservationService.java:105-107`), e `UnaryOperator` aparece com o nome completo (`:110`).
  - A cópia do record de 16 campos se repete em `domain/Reservation.java:25,30,36` e em `db/ReservationRepository.java:46`.
  - `PeriodKind.normalize` pergunta `this == DATE` em vez de deixar o comportamento em cada constante (`report/PeriodKind.java:29`).
- **Erros:** nenhuma exceção é engolida. O `catch (Exception)` geral registra a pilha e responde 500 (`web/Api.java:55-58`).

## Testes

**Números:** 8 testes rodaram, 0 falhas, 0 erros, 0 pulados; 8 métodos escritos. A cobertura é de 92,3% das linhas (434 de 470) e 71,3% dos ramos (87 de 122). Por pacote: `domain` 61/61 linhas, `report` 107/110, `service` 68/69 (`measures/tests.md`).
**Nota:** com problemas

Os testes passam e conferem a maior parte das regras com valores:
- **Domínio:** o preço na alta temporada (599,76, em `DomainTest.java:28-33`), os limites da temporada (`:36-44`), o desconto meio-para-o-par 37,48 / 212,42 (`:47-52`), os formatos de dinheiro (`:55-59`), o texto exato do comprovante (`:62-74`) e o esqueleto do CSV, com escape de vírgula (`:77-81`).
- **Ponta a ponta, contra o PostgreSQL de `DB_URL`, num esquema descartável** (`ApiTest.java:116-134`):
  - os códigos 409, 422, 400 e 404 (`ApiTest.java:170-176`);
  - o e-mail (`:183-187`);
  - a ocupação exata, com título e `total,1/5` (`:189-193`);
  - a taxa 18,00 (`:195-197`);
  - as devoluções nos três casos de cancelamento (`:211-223`);
  - o relatório de planos inteiro, com o STANDARD zerado e a NON_REFUNDABLE cancelada contando 985,83 (`:225-229`).

O que fica sem rede:
- **Pergunta da mutação:** se alguém apagasse o filtro `if (!r.isPaid()) continue;` da taxa de turismo (`report/TourismTaxReport.java:32`), nenhum teste falharia. No mês testado não há reserva pendente nem cancelada (`ApiTest.java:195-197`). O mesmo vale para o filtro `isPaid` da ocupação (`report/OccupancyReport.java:34`).
- A ordem por data de entrada e número, no financeiro e na taxa, não é conferida com mais de uma reserva.
- A taxa só é conferida por um trecho da linha (`contains(",2,2,18.00\n")`, em `ApiTest.java:196`).
- Os dois testes de API são longos, com dezenas de asserções em sequência (`ApiTest.java:157-207`). A primeira falha esconde as demais.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(12,2)` e as datas em `date`. A chave estrangeira de `reservations.room_number` tem índice. Não há nenhuma restrição CHECK, UNIQUE ou EXCLUDE: as colunas de situação, plano, tipo e canal são `varchar` sem CHECK. O esquema é criado por `CREATE TABLE` no código, sem arquivo de migração (`measures/database.md`).
**Nota:** com problemas

Os dados são guardados certo e sobrevivem ao reinício (3/3 no roteiro). O SQL usa parâmetros: os trechos montados com `+` só juntam constantes (`db/ReservationRepository.java:19-20,65,99`; `db/RoomRepository.java:15`). O pool é o HikariCP (`db/Database.java:62-70`), as conexões ficam em try-with-resources e cada operação roda numa transação (`db/Database.java:98-112`).

A regra "um quarto, uma reserva por noite" é protegida no banco, e não só dentro do processo: a criação trava a linha do quarto com `SELECT ... FOR UPDATE` antes de procurar sobreposição (`db/RoomRepository.java:36-41`; `service/ReservationService.java:48-51`). As 10 rodadas simultâneas do roteiro passaram.

Os defeitos:
- **Esquema sem migrações:** o esquema é uma constante com `CREATE TABLE IF NOT EXISTS` (`db/Database.java:17-58`), aplicada a cada subida (`:73-96`). Ela não sabe acrescentar uma coluna numa tabela que já existe, e o próximo pedido que mudar o banco vai precisar de migração manual.
- **Nenhuma restrição protege as regras:**
  - nada impede `guests < 1`, `check_out <= check_in` ou uma situação fora da lista (`db/Database.java:30-38`);
  - a ausência de sobreposição depende só do código, sem `EXCLUDE`.
- Os relatórios filtram as pagas em Java, sobre todas as reservas do mês (`report/MonthlyReservations.java:22-23`, `report/TourismTaxReport.java:32`). Para o tamanho da pousada, isso não pesa.

## Comportamento

**Números:** 64 de 64 conferências passaram, nenhuma falhou (`measures/behavior.md`):

| parte do roteiro | passaram |
|---|---|
| parte comum | 46 de 46 |
| dados depois de reiniciar | 3 de 3 |
| cenário 09 (os relatórios) | 13 de 13 |
| reservas simultâneas | 2 de 2 |

**Nota:** bom

Pela API, tudo o que o texto descreve funciona com os valores que ele dá:
- o preço da alta temporada (599,76) e o desconto meio-para-o-par (37,48);
- as recusas, com os códigos certos;
- a confirmação com o e-mail e o comprovante;
- o check-in e o check-out;
- as devoluções nos três casos de cancelamento;
- os dois relatórios originais.

Na parte nova, os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>". A taxa de turismo cobra R$ 4,50 por hóspede por noite, só das reservas pagas, e devolve total 0.00 num mês vazio. O relatório de planos mostra o STANDARD e o NON_REFUNDABLE mesmo sem reservas e conta a não reembolsável cancelada. Para o dono da pousada: o sistema faz o que ele pediu, não perde dados ao reiniciar e não aceita duas reservas do mesmo quarto feitas ao mesmo tempo.

## Dúvidas

- **A classe do design:** classifiquei como "alternativa equivalente" porque não há classe abstrata com método-modelo; a estrutura é uma interface mais um renderizador comum. Cada relatório ainda escreve o próprio laço e soma o próprio total, mas o esqueleto do CSV (título, cabeçalho, linhas, total) e o caminho de geração estão num lugar só, para os quatro relatórios. Por isso não considerei "malfeito".
- **A nota dos testes:** fica entre "bom" e "com problemas". Escolhi "com problemas" porque o filtro de reservas pagas da taxa de turismo, que é parte nova do cenário, não tem teste que falhe se for removido.
