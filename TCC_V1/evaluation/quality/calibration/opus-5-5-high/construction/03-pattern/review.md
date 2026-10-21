# Revisão: construção, cenário 03

## Visão geral

O projeto tem 8 pacotes, organizados por assunto: `hotel.web` (as rotas do Javalin e os DTOs), `hotel.reservations` (o serviço, a entidade, o cálculo de preço e o acesso ao banco das reservas), `hotel.rooms`, `hotel.messages`, `hotel.notifications`, `hotel.documents`, `hotel.reports` e `hotel` (a montagem e a conexão). Uma requisição entra em `HttpApi` (`src/main/java/hotel/web/HttpApi.java:61-91`), que lê o corpo, chama o `ReservationService`, e este chama `PriceCalculator` e `ReservationStore`, que guarda tudo no PostgreSQL com JDBC puro, o pool do Hikari e uma migração do Flyway (`src/main/resources/db/migration/V1__create_tables.sql`). A confirmação e o cancelamento avisam uma lista de `ReservationListener` montada em `Hotel` (`src/main/java/hotel/Hotel.java:29-32`): o hóspede, a governança e o financeiro. É um Observer direito. Os 29 testes passam, a cobertura de linhas é de 94,1% e o comportamento passou em 66 de 66 conferências. Não há nada de grave. Os pontos fracos são pequenos:
- um ciclo entre `documents` e `reservations`;
- os relatórios leem a tabela inteira e filtram em Java;
- a confirmação grava a reserva e as mensagens fora de uma transação;
- os testes dos avisos novos não conferem os destinatários, o corpo do e-mail da governança nem o valor do reembolso.

## Arquitetura

**Números:** 8 pacotes, 21 dependências entre eles e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), segundo `measures/dependencies.md`. O Javalin só aparece em `hotel.web` e `hotel`. O JDBC aparece em `hotel.reservations`, `hotel.rooms`, `hotel.messages` e `hotel`. A maior classe é `HttpApi`, com 170 linhas.
**Nota:** com problemas

- **A borda HTTP é fina.** Cada rota lê o pedido, chama o serviço e escreve a resposta (`src/main/java/hotel/web/HttpApi.java:62-90`). Os erros viram status num lugar só (`HttpApi.java:93-107`). A validação de presença dos campos fica na borda (`HttpApi.java:116-126`). As regras de negócio (datas, número de hóspedes, capacidade, disponibilidade) ficam no serviço (`src/main/java/hotel/reservations/ReservationService.java:26-43`).
- **As regras não conhecem o Javalin nem o SQL.** `ReservationService` só fala com `Rooms`, `ReservationStore` e `PriceCalculator`. O SQL fica isolado em três classes de acesso: `ReservationStore`, `Rooms` e `SentMessages`.
- **A montagem fica num lugar só.** É o `Hotel` (`src/main/java/hotel/Hotel.java:24-36`), sem `static` global e sem `new` de acesso a dados dentro do serviço.
- **O ciclo é o defeito.** `ReservationService` importa `hotel.documents.ReservationReceipt` (`ReservationService.java:3`, `:102-118`) para devolver o comprovante. Ao mesmo tempo, `ReservationReceipt` é construído a partir de `Reservation` (`src/main/java/hotel/documents/ReservationReceipt.java:3`, `:24`). O ciclo é pequeno e fica entre vizinhos. Ainda assim, mostra que o comprovante está no pacote errado ou que o serviço não deveria montá-lo.
- **Há um desvio menor.** `ReservationStore` mora no pacote das regras e é usado direto pelos relatórios (`src/main/java/hotel/reports/OccupancyReport.java:4`). É aceitável num sistema deste tamanho.

## Design de baixo nível

**Números:** há 3 interessados (o hóspede, a governança e o financeiro). Os 3 são ouvintes de `ReservationListener`, os 2 eventos (confirmado e cancelado) passam pela lista, e nenhum e-mail é montado no serviço.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** `ReservationListener` (`src/main/java/hotel/reservations/ReservationListener.java:6-15`) tem os dois eventos como métodos `default` vazios, então cada ouvinte só implementa o que lhe interessa. Isso é bom para o restaurante e o transfer, que vão entrar depois. O serviço recebe a lista de fora (`ReservationService.java:17-23`) e a percorre:
- na confirmação, em `ReservationService.java:63`;
- no cancelamento de uma reserva que estava confirmada, em `ReservationService.java:84-87`.

O serviço não sabe quem é avisado. Cada interessado é uma classe:
- o hóspede: `src/main/java/hotel/notifications/GuestConfirmationEmail.java:10-33`, o que mostra que o e-mail do hóspede também virou ouvinte;
- a governança: `HousekeepingNotice.java:10-35`;
- o financeiro: `FinanceNotice.java:10-35`, que decide sozinho não avisar quando não há reembolso (`:29`).

O registro é feito na montagem (`Hotel.java:29-32`). Acrescentar o restaurante é escrever uma classe e uma linha em `Hotel`. A lista é fixada na montagem (`List.copyOf`, `ReservationService.java:22`). Para "entram e saem conforme os contratos", isso pede mudar a montagem e reiniciar o sistema, o que é razoável aqui.

**O exagero.** Não há. `PriceCalculator`, `ReservationStore`, `Rooms` e `EmailSender` são classes concretas, sem interfaces de uma implementação só, sem fábricas e sem camadas que só repassam. A única interface é a do pattern.

**O resto do código** está limpo:
- `BigDecimal` com `HALF_EVEN` a cada passo (`src/main/java/hotel/reservations/PriceCalculator.java:33-52`);
- `LocalDate` e `YearMonth` para datas e meses;
- `enum` para a situação, o plano e o tipo de quarto;
- métodos curtos e nomes claros;
- `Reservation` com os mutadores restritos ao pacote (`Reservation.java:102-114`).

Os pontos menores são estes:
- `catch (Exception)` ao ler o corpo (`HttpApi.java:113`). Ele vira 400, o que está certo, mas pega qualquer coisa.
- A regra de devolução por plano fica num `switch` no serviço (`ReservationService.java:78-81`), enquanto o desconto por plano fica em `PriceCalculator` (`:26-29`). As duas regras do plano estão em lugares diferentes.
- O canal é o texto solto `"EMAIL"` (`src/main/java/hotel/messages/EmailSender.java:13`).
- Os ouvintes rodam depois que a reserva foi gravada (`ReservationService.java:61-63`). Se um deles lançar exceção, os outros não rodam e o cliente recebe 500, mas a reserva fica confirmada.

Nada disso exige mudança antes de aceitar.

## Testes

**Números:** 29 testes rodaram, com 0 falhas, 0 erros e 0 pulados. Há 29 métodos escritos. A cobertura é de 94,1% das linhas (506 de 538) e 87,7% dos ramos (107 de 122). `hotel.notifications` tem 33 de 33 linhas, `hotel.reports` tem 48 de 48 linhas e 22 de 22 ramos, e `hotel.web` tem 18 de 28 ramos. Os dados são de `measures/tests.md`. Os testes rodam contra o PostgreSQL de verdade, sem mocks, e 1 classe testa pela API HTTP.
**Nota:** com problemas

**A parte comum está bem conferida, com valores:**
- o preço com as bordas da alta temporada (14/15 de dezembro, 29 de fevereiro num ano bissexto, 30/06 e 01/07), em `src/test/java/hotel/reservations/PriceCalculatorTest.java:31-54`;
- o arredondamento meio para o par (37,485 vira 37,48), em `PriceCalculatorTest.java:57-63`;
- as devoluções por plano e o dinheiro que fica com a pousada, em `ReservationServiceTest.java:110-123`;
- as recusas pelos códigos de erro, em `ReservationServiceTest.java:54-63`;
- os dois relatórios comparados com o CSV inteiro, em `src/test/java/hotel/reports/ReportsTest.java:55-113`.

**A parte nova é conferida só em parte.** `src/test/java/hotel/notifications/TeamNoticesTest.java:47-76` confere os assuntos e a sequência dos avisos:
- a confirmação avisa a governança e o financeiro;
- o reembolso só sai quando há valor;
- uma reserva pendente cancelada não avisa ninguém.

O valor pago no e-mail do financeiro também é conferido (`:53`). Mas a pergunta da mutação deixa sem rede:
- **os destinatários:** trocar `governanca@...` ou `financeiro@...` (`HousekeepingNotice.java:12`, `FinanceNotice.java:12`) não faz nenhum teste falhar, porque nenhum confere o campo `to` dos avisos;
- **o corpo do aviso da governança:** apagar as datas ou a linha "Hóspedes" (`HousekeepingNotice.java:24-26`) ou as datas do cancelamento (`:32-33`) passa;
- **o valor do reembolso:** trocar `reservation.refund()` por outro valor no corpo (`FinanceNotice.java:31`) passa.

**Há uma falta menor.** O e-mail do hóspede é conferido com `contains` (`ReservationServiceTest.java:96-97`), e não pelo texto inteiro do enunciado.

## Banco

**Números:** 3 tabelas, cada uma com chave primária, e 1 migração do Flyway. Todo o dinheiro está em `numeric(10,2)`, as datas em `date`, e não há chave estrangeira sem índice. A conferência acusou 4 colunas de situação e de tipo em `text` sem CHECK: `reservations.status`, `reservations.rate_plan`, `rooms.type` e `messages.channel`. Não há transação explícita nem `FOR UPDATE`. Os dados são de `measures/database.md`.
**Nota:** com problemas

**O que está bem:**
- o esquema nasce de uma migração versionada (`src/main/resources/db/migration/V1__create_tables.sql`, aplicada pelo Flyway em `src/main/java/hotel/Database.java:28`);
- a reserva está ligada ao quarto por chave estrangeira (`V1__create_tables.sql:21`);
- há `CHECK` para as datas e para o número de hóspedes (`:27`, `:36`);
- a regra de não reservar o mesmo quarto duas vezes na mesma noite é protegida pelo próprio banco, com um `EXCLUDE USING gist` sobre `daterange` que ignora as canceladas (`:38-40`). A violação vira 409 (`ReservationStore.java:55-58`), e isso vale entre processos, não só dentro de um `synchronized`;
- o SQL é sempre parametrizado, e o que é concatenado é constante (`COLUMNS`, `ReservationStore.java:18`);
- as conexões vêm do Hikari, em try-with-resources.

**Os defeitos:**
- **Os relatórios leem a tabela inteira.** Os dois chamam `store.all()`, que traz todas as reservas da história (`ReservationStore.java:108-121`), e filtram em Java (`src/main/java/hotel/reports/OccupancyReport.java:25-30`, `RevenueReport.java:25-31`). A ocupação ainda faz uma consulta por linha para achar o quarto (`OccupancyReport.java:36`). A tabela cresce sem limite.
- **As mudanças de situação não são atômicas no banco.** O `UPDATE` não confere a situação anterior (`ReservationStore.java:67`), e a proteção contra duas confirmações ao mesmo tempo é só o `synchronized` do processo (`ReservationService.java:55`).
- **A confirmação não usa transação.** A gravação da reserva e as mensagens dos ouvintes usam conexões separadas e sem transação (`ReservationService.java:61-63`). Uma falha no meio deixa a reserva confirmada sem os avisos.
- **A situação, o plano e o tipo de quarto aceitam qualquer texto.** Não há CHECK sobre essas colunas (`V1__create_tables.sql:4`, `:28`, `:33`).

Nada disso perde dado hoje.

## Comportamento

**Números:** 66 de 66 conferências passaram, com 0 falhas (`measures/behavior.md`):
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 03: 15 de 15;
- duas reservas ao mesmo tempo: 2 de 2, ou seja, nas 10 rodadas sempre uma foi criada e a outra recusada com 409.

**Nota:** bom

Tudo o que o dono pediu funciona pela API:
- os preços com a alta temporada e o meio para o par;
- as recusas com os códigos certos;
- o ciclo da reserva, as devoluções e os dois relatórios em CSV;
- os dados, que continuam depois de reiniciar.

Na parte nova, a governança recebe os avisos de confirmação e de cancelamento com as datas e o número de hóspedes. O financeiro recebe o pagamento (R$ 779,80) e o reembolso, e não é avisado quando o cancelamento não reembolsável não tem nada a devolver. Uma reserva pendente cancelada não manda nada. O e-mail do hóspede continua chegando. Para o dono da pousada, o sistema faz o que o texto pede.

## Dúvidas

- **A lista de ouvintes é fixa depois de montada.** Ela é fixada em `Hotel` (`ReservationService.java:22`). O texto diz que quem é avisado "vai mudar com o tempo", conforme os contratos. Li isso como uma mudança na montagem, e não como cadastro em tempo de execução, e por isso mantive o acerto. Se o dono esperava ligar e desligar o restaurante sem mexer no código, faltaria uma configuração.
- **A nota do banco ficou na fronteira entre `bom` e `com problemas`.** Os defeitos são de carga e de robustez, não de dado errado. Dei `com problemas` pelas consultas que trazem a tabela inteira, que a skill põe nessa faixa.
