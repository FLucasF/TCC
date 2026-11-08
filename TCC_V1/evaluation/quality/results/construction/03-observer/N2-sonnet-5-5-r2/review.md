# Revisão: construção, cenário 03

## Visão geral

O projeto tem seis pacotes: `web` (Javalin e Jackson), `app` (serviços e catálogo de quartos), `domain` (reserva, preço, planos, comprovante), `notification` (ouvintes e mensagens), `persistence` (JDBC) e a raiz (`Main` e `Application`, onde as peças são montadas). Uma requisição como `POST /reservations/{id}/confirmation` entra em `web/Api.java:62` e chama `ReservationService.confirm` (`app/ReservationService.java:54`). Ali, dentro de uma transação, a reserva é lida com `FOR UPDATE`, muda de situação no domínio (`domain/Reservation.java:75`), é gravada, e depois a lista de ouvintes recebida de fora é percorrida (`app/ReservationService.java:56`). Os dados ficam no PostgreSQL, em duas tabelas (`reservation` e `message`), criadas com `CREATE TABLE IF NOT EXISTS` no código (`persistence/Database.java:32-61`). Os quartos não estão no banco: ficam fixos em `app/RoomCatalog.java:13-18`. O roteiro de comportamento passou inteiro (66 de 66) e os 18 testes passam. Nada é grave; os pontos mais sérios são estes: um ciclo entre `app` e `persistence`, causado por `RoomCatalog` estar no pacote errado; o esquema sem migrações e sem nenhuma restrição (CHECK, chave estrangeira); e o conteúdo dos e-mails da governança e do financeiro sem teste.

## Arquitetura

**Números:** 6 pacotes e 14 dependências entre eles. Há 1 ciclo, entre `br.com.marealta.app` e `br.com.marealta.persistence`. O Javalin aparece só em `web` e na raiz, e o JDBC só em `persistence` e na raiz (`measures/dependencies.md`).
**Nota:** com problemas

- **A borda HTTP é fina.** Cada rota lê a requisição, chama o serviço e escreve a resposta (`web/Api.java:57-74`). A leitura e a validação do corpo ficam separadas, em `web/ReservationRequestParser.java:22-47`, e o JSON de saída em `web/Views.java`. Os códigos de erro viram status HTTP num só lugar (`web/Api.java:23-29,50-51`).
- **As regras não conhecem Javalin nem SQL.** Preço e temporada estão em `domain/Pricing.java:18-42`, as transições de situação e a devolução em `domain/Reservation.java:75-95`, e desconto e devolução por plano em `domain/RatePlan.java`.
- **O SQL está todo em `persistence`** (`ReservationRepository`, `MessageStore`, `Database`).
- **A montagem fica num lugar só**, `Application.java:24-40`, e é ali que se decide quem é avisado (`Application.java:32-35`).
- **Defeito: o ciclo `app` ↔ `persistence`.** `persistence/ReservationRepository.java:3,18,112` usa `app.RoomCatalog` para reconstruir o quarto. Ao mesmo tempo, `app` depende de `persistence` (`app/ReservationService.java:10-11`, `app/ReportService.java:5`). O catálogo de quartos é dado de domínio e está no pacote errado: dentro de `domain`, ou passado como função, o ciclo sumiria. É um ciclo pequeno entre vizinhos, mas é novo, porque o projeto foi feito do zero.
- **Menores:**
  - O serviço depende de `Database` para abrir a transação (`app/ReservationService.java:39,78`). Não monta SQL, então é aceitável.
  - A rota `/messages` vai direto a `persistence.MessageStore`, sem passar por `app` (`web/Api.java:8,71`).
  - Nenhuma classe é muito maior que as outras: a maior tem 153 linhas (`domain/Reservation.java`).

## Design de baixo nível

**Números:**
- Na confirmação, os 3 interessados (hóspede, governança e financeiro) são ouvintes, cada um numa classe.
- No cancelamento, os 2 interessados são ouvintes.
- Nenhum e-mail é montado no serviço.
- O serviço não pergunta qual é o caso.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O Observer está aplicado como a referência pede, e por isso a classe é acerto:
- Existe a interface `notification/ReservationListener.java:6-13`, com `onConfirmed` e `onCancelled` (métodos `default`, para cada ouvinte escolher o evento que lhe interessa).
- Há uma implementação por interessado, e o hóspede também é ouvinte:
  - `notification/GuestNotifier.java:15-22`;
  - `notification/HousekeepingNotifier.java:17-35`;
  - `notification/FinanceNotifier.java:17-32`.
- O serviço recebe a lista de fora (`app/ReservationService.java:27-33`) e só a percorre: na confirmação em `app/ReservationService.java:56` e no cancelamento em `:61`. Ele não conhece destinatários, assuntos nem textos.
- O registro é feito na montagem (`Application.java:32-35`). Pôr o restaurante ou o transfer é escrever uma classe nova e acrescentar uma linha ali, sem mexer no serviço.
- Quem decide se o fato interessa é o ouvinte:
  - a governança ignora o cancelamento sem pagamento (`notification/HousekeepingNotifier.java:28`);
  - o financeiro ignora quando não há nada a devolver (`notification/FinanceNotifier.java:26`).
- A saída das mensagens está atrás de `notification/MessageSender.java`, e assim `notification` não depende de `persistence`.

**O exagero.** Não há estrutura sem necessidade:
- `MessageSender` tem uma implementação de produção (`persistence/MessageStore.java:9`). Mesmo assim, os testes a trocam por `sent::add` (`NotificationTest.java:23-25`) e ela inverte a dependência entre os pacotes, então se justifica.
- `Database.RowMapper` e `Database.Binder` (`persistence/Database.java:15-23`) são interfaces funcionais usadas com lambdas.
- Não há fábricas, builders nem camadas que só repassam.

**O resto do código** está bem tipado: `BigDecimal` com `HALF_EVEN` (`domain/Money.java:20-22`), `LocalDate`, e `enum` para situação, plano, tipo de quarto e canal. O preço por plano está polimórfico no próprio `RatePlan` (`domain/RatePlan.java:6-35`). Há, porém, alguns defeitos localizados que um revisor exigente apontaria:
- **Nome enganoso:** a formatação de datas mora em `Money.format(LocalDate)` (`domain/Money.java:32-35`), e os ouvintes formatam datas com `Money.format(r.checkInDate())` (`notification/HousekeepingNotifier.java:20-21,32-33`).
- **Validação duplicada:** "saída depois da entrada" e "ao menos um hóspede" aparecem em `web/ReservationRequestParser.java:40-45` e de novo em `domain/Reservation.java:50-55`, com as mesmas mensagens e os mesmos códigos.
- **Estado mutável exposto:** `Reservation.assignId` é público (`domain/Reservation.java:133-135`), e o `id` pode ser trocado por qualquer um depois de criado. O `restore` tem 16 parâmetros posicionais (`domain/Reservation.java:67-73`).
- **Condição redundante:** `!r.hasPayment() ||` em `notification/FinanceNotifier.java:26` não muda nada, porque uma devolução maior que zero já implica pagamento. A checagem `hasPayment()` aparece repetida nos dois ouvintes de funcionários.
- **Erros:** o `catch (Exception)` geral só faz `printStackTrace` e responde 500 (`web/Api.java:52-55`). A causa não se perde, mas não há log de verdade.
- **Ouvintes dentro da transação:** eles rodam dentro da transação que trava a reserva (`app/ReservationService.java:77-84`). Hoje isso é uma virtude, porque a mensagem gravada e a confirmação andam juntas. Mas, quando o envio for real (um SMTP lento ou com falha), vai prender a trava da linha ou desfazer a confirmação. Vale uma nota para o próximo passo.

## Testes

**Números:** 18 testes rodaram, com 0 falhas, 0 erros e 0 pulados, e há 18 métodos escritos (`measures/tests.md`). A cobertura é de 94,4% das linhas (475 de 503) e 78,6% dos ramos (77 de 98). O pacote `app` tem 100% de linhas e de ramos; `domain`, 142 de 143 linhas; `notification`, 43 de 44. Os testes estão em três níveis: domínio puro, ouvintes com um enviador falso e a API contra o PostgreSQL de `DB_URL`, num schema próprio (`ApiTest.java:32-48`).
**Nota:** com problemas

**O que está bem testado**, com valores:
- **A parte comum:**
  - a alta temporada, com 599.76 (`DomainTest.java:30-34`);
  - as fronteiras da temporada, incluindo 14/12, 15/12, 29/02 e 01/03 (`DomainTest.java:37-44`);
  - o "meio para o par", com 37.48 (`DomainTest.java:47-52`);
  - as transições de situação e as recusas (`DomainTest.java:55-66`);
  - a devolução por plano (`DomainTest.java:69-85`);
  - o texto do comprovante, comparado inteiro (`DomainTest.java:102-113`);
  - "1 diária", o desconto e o separador de milhar (`DomainTest.java:116-125`);
  - os códigos de erro pela API (`ApiTest.java:102-120`);
  - os relatórios em CSV, comparados inteiros (`ApiTest.java:157-174`);
  - a NON_REFUNDABLE cancelada continuando no relatório financeiro (`ApiTest.java:205-208`).
- **A parte nova:**
  - os assuntos das três mensagens da confirmação e o corpo inteiro do e-mail do hóspede (`NotificationTest.java:38-61`);
  - o cancelamento STANDARD pago (`NotificationTest.java:63-70`);
  - o NON_REFUNDABLE sem aviso ao financeiro (`NotificationTest.java:72-79`);
  - a pendente sem aviso nenhum (`NotificationTest.java:81-87`);
  - de novo pela API, com os destinatários (`ApiTest.java:145-151,178-203`).

**O que fica sem rede.** Nenhum teste confere o corpo dos e-mails da governança e do financeiro. Os testes olham só assuntos e destinatários (`NotificationTest.java:34-36,44-45,69`; `ApiTest.java:146-150`). Pela pergunta da mutação, todos estes defeitos passariam:
- apagar a linha "Hóspedes: " (`notification/HousekeepingNotifier.java:22`);
- trocar a data de saída pela de entrada (`:21`);
- escrever o valor pago errado no aviso ao financeiro (`notification/FinanceNotifier.java:20`);
- escrever o valor errado no reembolso (`:30`).

Justamente o que o texto do cenário pede nesses e-mails (as datas, o número de hóspedes e os valores) não tem teste. Também não há teste de duas reservas simultâneas: a trava `pg_advisory_xact_lock` (`persistence/ReservationRepository.java:71-73`) não é exercitada. Por fim, `ApiTest` se desliga sozinho quando falta `DB_URL` (`ApiTest.java:35`). Aqui ele rodou, mas num `mvn verify` sem banco metade da rede some em silêncio.

## Banco

**Números:** o serviço subiu contra um banco vazio e criou 2 tabelas, `message` e `reservation`, as duas com chave primária. As conferências do script (`measures/database.md`) deram:
- dinheiro: as 5 colunas em `numeric(12,2)`, ok;
- datas: em `date`, ok;
- chaves estrangeiras: 0;
- CHECK: 0, e `status`, `rate_plan` e `channel` são `text` sem CHECK;
- UNIQUE ou EXCLUDE: 0;
- SQL montado com `+`: 2 candidatos, os dois concatenando só a constante `COLUMNS`.
**Nota:** com problemas

**O que está certo:**
- Os tipos estão certos: dinheiro com 2 casas e datas em `date` (`persistence/Database.java:40-50`). Todas as colunas obrigatórias são `NOT NULL`; só `refund` aceita nulo, de propósito.
- Todo o SQL usa parâmetros `?`. As duas concatenações (`persistence/ReservationRepository.java:61,66,89,100`) juntam só a constante `COLUMNS` (`:14-15`).
- As conexões são fechadas com try-with-resources (`persistence/Database.java:67,125`).
- A transação junta a leitura com `FOR UPDATE`, a gravação e as mensagens (`app/ReservationService.java:77-84`).
- A regra "um quarto, uma reserva por noite" está protegida entre processos: `pg_advisory_xact_lock` por quarto mais a checagem de sobreposição, na mesma transação (`app/ReservationService.java:39-47`, `persistence/ReservationRepository.java:71-85`). O roteiro de concorrência confirmou: 10 de 10 rodadas com uma reserva só.
- Os dados continuam depois de reiniciar o serviço.

**Os defeitos:**
- **O esquema não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` no código (`persistence/Database.java:32-61`), sem nenhuma migração. A primeira coluna nova que um pedido futuro trouxer não vai aparecer num banco que já existe.
- **Não há tabela de quartos nem chave estrangeira.** `room_number` é um `INT` solto (`persistence/Database.java:36`). Os quartos vivem só em `app/RoomCatalog.java:13-18`, e um número inválido gravado no banco só é descoberto na leitura (`persistence/ReservationRepository.java:112`).
- **O banco não protege nenhuma regra.** Não há CHECK em `status`, `rate_plan`, `guests > 0` nem `check_out > check_in`, nem uma `EXCLUDE` contra sobreposição. A integridade depende inteira da aplicação.
- **Não há pool de conexões.** `Main.java:11` usa `PGSimpleDataSource`, que abre uma conexão física nova a cada `getConnection` (`persistence/Database.java:67,125`). O script contou "pool: 1 arquivo", mas isso vem do nome `DataSource`, não de um pool de verdade.
- **Uma carga, só para registro:** `GET /messages` traz a tabela inteira, sem limite (`persistence/MessageStore.java:27-31`). Para o volume de uma pousada, não pesa.

## Comportamento

**Números:** 66 de 66 conferências passaram, e nenhuma falhou (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário 03 | 15 de 15 |
| duas reservas ao mesmo tempo | 2 de 2 |
**Nota:** bom

Para o dono da pousada, tudo o que o texto descreve funciona pela API:
- o preço com a alta temporada e o "meio para o par" (599.76, 37.48 e 212.42);
- os códigos de erro;
- a confirmação com o e-mail do hóspede e o comprovante;
- o check-in, o check-out e a segunda via;
- as devoluções por plano;
- os dois relatórios em CSV, inclusive a NON_REFUNDABLE cancelada continuando no financeiro.

Na parte nova, a governança recebe "Reserva confirmada - quarto 201", com as datas e os 2 hóspedes, e o financeiro recebe "Pagamento recebido - reserva 1", com R$ 779,80. No cancelamento de uma reserva confirmada, os dois são avisados, com as datas e o valor a devolver. O financeiro não é avisado quando não há o que devolver, e uma reserva pendente cancelada não manda nada. Os dados sobrevivem ao reinício, e duas reservas simultâneas do mesmo quarto nunca são criadas juntas.

## Dúvidas

- **Testes:** fiquei entre `bom` e `com problemas`. A cobertura é alta e as regras da parte comum estão conferidas com valores. Dei `com problemas` porque o conteúdo dos e-mails da governança e do financeiro, que é a parte nova do cenário, não tem nenhum teste. Um revisor que aceite conferir só os assuntos daria `bom`.
- **Design de baixo nível:** a nota `com problemas` vem de defeitos pequenos e localizados: o nome `Money.format(LocalDate)`, a validação duplicada e o `assignId` público. Um revisor mais tolerante daria `bom`.
- **Banco:** guardar os quartos fixos no código, sem tabela, pode ser defendido, porque o texto diz que "os quartos quase nunca mudam". Mas isso deixa a reserva sem chave estrangeira.
