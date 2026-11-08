# Revisão: construção, cenário 03

## Visão geral

O projeto tem 9 pacotes debaixo de `br.com.pousadamarealta`: `api` (controllers Javalin e `ErrorHandler`), `api.dto`
(records de entrada e de saída), `service` (`ReservationService`, `ReportService` e as exceções), `domain`
(`Reservation` com o preço, as transições e o texto do comprovante, mais os enums), `events` e `events.listeners` (o
Observer: `EventPublisher`, dois eventos e três ouvintes), `repository` (JDBC puro com `PreparedStatement`) e
`config`. Uma requisição entra pelo controller (`api/ReservationsController.java:28`), que valida e chama o serviço; o
serviço aplica a regra, grava pelo repositório e publica um evento (`service/ReservationService.java:79`); os ouvintes
gravam as mensagens na tabela `messages`. Os dados ficam no PostgreSQL, em três tabelas criadas por
`CREATE TABLE IF NOT EXISTS` no código (`repository/Database.java:31-68`). O Observer está bem feito. O problema mais
sério está no banco: a coluna `guest_cpf` é `VARCHAR(11)` (`repository/Database.java:44`), e um CPF escrito como o
enunciado o mostra ("111.222.333-44") estoura a coluna e o `POST /reservations` devolve 500. Por isso o roteiro de
comportamento passa em só 16 de 66 conferências. Além disso, nada impede que o mesmo quarto seja reservado duas vezes
para as mesmas noites quando os pedidos chegam juntos (nem restrição no banco, nem transação, nem trava), e o
relatório financeiro mostra reservas canceladas com devolução total, que não deixaram dinheiro.

## Arquitetura

**Números:** 9 pacotes, 19 dependências entre eles, nenhum ciclo. Javalin só em `api` e na montagem; JDBC só em
`repository`, `config` e na montagem; `service`, `domain` e `events` não usam biblioteca nenhuma. Maior classe:
`domain/Reservation.java`, com 251 linhas.
**Nota:** com problemas

As camadas existem e as dependências vão para o lado certo (`api -> service -> repository -> domain`, sem ciclo). Toda
a montagem fica num lugar só, `Application.java:31-62`, e os ouvintes são registrados ali (`Application.java:39-42`).
As regras não conhecem o Javalin nem o SQL. Os defeitos estão em lugares bem definidos:

- A borda HTTP faz conta de dinheiro. O check-out e a segunda via recalculam diárias, desconto e total no próprio
  handler (`api/ReservationsController.java:107-112` e `:125-130`). O handler da segunda via também repete a regra de
  "só reserva paga" (`api/ReservationsController.java:119-123`), que já existe em `ReservationService.getDocument`
  (`service/ReservationService.java:129`).
- No check-out, o handler chama `reservationService.getDocument(id)` e joga o resultado fora
  (`api/ReservationsController.java:105`). O comprovante sai de dois caminhos diferentes.
- A validação do pedido (campos que faltam, saída depois da entrada) fica no controller
  (`api/ReservationsController.java:32-48`), e o "pelo menos um hóspede" fica no serviço
  (`service/ReservationService.java:36`). A regra da reserva válida está partida em dois lugares.
- Os controllers falam direto com o repositório para buscar o quarto depois de cada operação
  (`api/ReservationsController.java:61,77,84,91,98`), e `MessagesController`/`RoomsController` também usam o
  repositório sem passar pelo serviço. Num sistema deste tamanho isso é aceitável para leitura, mas é a razão do
  cálculo repetido na borda.
- O domínio formata texto: o comprovante e o formato de dinheiro e de data moram em `domain/Reservation.java:150-185`,
  e os ouvintes os usam (`events/listeners/FinanceNotifier.java:33`).

## Design de baixo nível

**Números:** 3 interessados (hóspede, governança, financeiro) e 2 eventos (confirmação, cancelamento). Os 3
interessados são ouvintes separados registrados na montagem; nenhum e-mail é montado fora deles. O cálculo
subtotal/desconto/total aparece em 5 lugares.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

O pattern é o que a referência chama de acerto. Há uma interface de ouvinte (`events/ReservationEventListener.java:3-5`)
e uma implementação por interessado: `GuestNotifier`, `HousekeepingNotifier` e `FinanceNotifier`. O e-mail do hóspede
também é um ouvinte (`events/listeners/GuestNotifier.java:14`). O serviço recebe de fora o `EventPublisher`
(`service/ReservationService.java:20-25`), que percorre a lista (`events/EventPublisher.java:13-17`), e só anuncia o
fato (`service/ReservationService.java:79` e `:99`). Ele não sabe quem é avisado nem monta e-mail. O cancelamento
também passa pela estrutura. A regra "pendente cancelada não avisa ninguém" e a regra "sem devolução não avisa o
financeiro" ficam nos ouvintes (`events/listeners/HousekeepingNotifier.java:38`,
`events/listeners/FinanceNotifier.java:40-46`). Para acrescentar o restaurante ou o transfer, basta uma classe nova e
uma linha em `Application.java`.

O que afasta um pouco do ideal: cada ouvinte descobre o tipo do evento com `instanceof`
(`events/listeners/FinanceNotifier.java:23-27`, `HousekeepingNotifier.java:21-25`, `GuestNotifier.java:25-27`). Com
mais eventos, cada ouvinte vai ganhar um `else if`. O evento de cancelamento leva `wasConfirmed`, uma decisão que cada
ouvinte repete. Isso não tira o caso da estrutura.

Exagero: não há. As duas interfaces (`ReservationEvent`, `ReservationEventListener`) são justamente o ponto de
variação. `Database` e `DataSourceConfig` são finos, mas não chegam a ser uma camada que só repassa.

O resto do código tem defeitos em lugares definidos:

- **Duplicação do cálculo de dinheiro:** subtotal, depois desconto, depois `subtract` aparece em
  `api/dto/ReservationDTO.java:27-29`, `api/ReservationsController.java:108-110` e `:126-128`,
  `service/ReservationService.java:134-136` e `events/listeners/GuestNotifier.java:34-36`. Isso apesar de existir
  `Reservation.calculateTotal` (`domain/Reservation.java:101`). O total nunca é gravado; é sempre recalculado com a
  diária atual.
- **Regra de situação em dobro:** o serviço confere a situação (`service/ReservationService.java:68,87,107,118`) e o
  domínio confere de novo com `IllegalStateException` (`domain/Reservation.java:108,115,122,129`).
- **Formatação que depende do locale da máquina:** `String.format("R$ %,.2f")` com troca de caracteres
  (`domain/Reservation.java:161`) e `String.format("%.2f")` no CSV (`service/ReportService.java:65,70`) não fixam o
  `Locale`. Numa JVM em pt_BR o CSV sairia "599,76" e o comprovante "R$ 1,234.56".
- **Código morto:** `Reservation.wasConfirmed()` não tem uso (`domain/Reservation.java:245-250`), e o parâmetro
  `excludeId` de `hasConflict` sempre recebe `null` (`service/ReservationService.java:44`).
- **Erros:** toda `SQLException` vira um `RuntimeException` genérico (`repository/ReservationRepository.java:118`
  etc.), e não há tratador para eles: um corpo que não é JSON ou um CPF longo chega ao cliente como 500 "Server
  Error". O `catch (IllegalArgumentException)` do controller assume que é sempre "plano inválido"
  (`api/ReservationsController.java:68-70`).
- **Tipos:** `BigDecimal` com `HALF_EVEN` a cada passo (`domain/Reservation.java:67,93,98,104`), `LocalDate` e enums
  para situação, plano e tipo, tudo certo. `formatCpf` só formata se tiver exatamente 11 caracteres
  (`domain/Reservation.java:151`).

## Testes

**Números:** 15 testes rodaram, 0 falhas, 0 erros, 0 pulados; 15 métodos escritos (6 de unidade em `PricingTest`, 9 de
integração com H2 em `ReservationFlowIT` e `ReportServiceIT`). Cobertura de 60,6% das linhas (493/813) e de 49,4% dos
ramos (85/172). `api`, `api.dto` e a montagem têm 0%; `events.listeners` tem 74/74 linhas.
**Nota:** com problemas

O que está testado com valores: o preço da alta temporada e o arredondamento (299,88 e 599,76 em
`PricingTest.java:30,49`), a virada de 14 para 15/12 (`PricingTest.java:93-94`), o desconto do NON_REFUNDABLE
(116,97 e 662,83 em `PricingTest.java:71-72`), o formato de dinheiro e de CPF, a ocupação e o financeiro básicos
(`ReportServiceIT.java:188-213`), o choque de datas e a capacidade (`ReservationFlowIT.java:425,441`).

Na parte nova (o Observer), os testes conferem só o destinatário e um pedaço do assunto. `ReservationFlowIT.java:316-322`
procura `contains("Reserva confirmada")` e `contains("Pagamento recebido")`, sem o número do quarto ou da reserva, e
nenhum teste lê o corpo de uma mensagem. A pergunta da mutação: se `FinanceNotifier.java:33` mandasse o subtotal no
lugar do pago, ou se `HousekeepingNotifier.java:55` deixasse de mostrar o número de hóspedes, ou se o assunto perdesse
o `+ reservation.getRoomNumber()` (`HousekeepingNotifier.java:30`), nenhum teste falharia. O e-mail do hóspede também
não é conferido (nem o texto, nem o comprovante).

O que ficou sem teste: as recusas por situação (confirmar duas vezes, check-in de pendente), o check-in e o check-out,
a segunda via, os códigos de erro HTTP (não há nenhum teste pela API), e a reserva STANDARD cancelada no financeiro,
que está errada (veja Comportamento).

Os testes rodam em H2 (`ReservationFlowIT.java:268-270`), não no PostgreSQL de `DB_URL`, e usam o CPF sem pontuação
(`"11122233344"`). Por isso o defeito que derruba o sistema (CPF em `VARCHAR(11)`) passou sem ser visto. O código de
produção ainda ganhou um desvio só para o H2 (`repository/Database.java:28-29`).

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)`, as datas em `date`, e há 1 chave estrangeira (`reservations.room_number -> rooms`), sem índice. Não
há nenhum CHECK, UNIQUE ou EXCLUDE. São 0 transações explícitas e 0 `FOR UPDATE`. O esquema é criado por
`CREATE TABLE` no código, sem migração. O script conta "pool: 1 arquivo", mas o que ele encontrou é o
`PGSimpleDataSource` (`config/DataSourceConfig.java:10`), que não é pool: abre uma conexão nova a cada chamada.
**Nota:** ruim

- **Um tipo errado que derruba o sistema:** `guest_cpf VARCHAR(11)` (`repository/Database.java:44`). O enunciado
  escreve o CPF como "111.222.333-44" (14 caracteres). O roteiro manda esse formato e o PostgreSQL recusa todas as
  inserções com "value too long for type character varying(11)" (`measures/behavior-service.log:65`). O serviço não
  valida nem normaliza o CPF antes de gravar, e o erro vira 500.
- **Reserva em dobro:** a proteção contra duas reservas na mesma noite é só um `SELECT COUNT(*)` antes do `INSERT`
  (`service/ReservationService.java:44`, `repository/ReservationRepository.java:186-217`), sem transação, sem trava e
  sem restrição no banco. Ela não vale nem dentro do mesmo processo. Subi o serviço eu mesmo com o CPF sem pontuação e
  mandei dois pedidos simultâneos para o quarto 301: em 9 de 9 rodadas com datas livres, os dois pedidos foram
  criados com 201. Isso é dado errado: o quarto fica vendido duas vezes.
- **Escritas que deviam andar juntas:** a confirmação grava a reserva (`service/ReservationService.java:77`) e depois
  cada ouvinte grava a sua mensagem numa conexão própria (`events/listeners/FinanceNotifier.java:36`). Se uma falhar,
  a reserva fica confirmada sem os avisos, e o cliente recebe 500.
- **O esquema não evolui:** `CREATE TABLE IF NOT EXISTS` (`repository/Database.java:31-64`) não sabe acrescentar uma
  coluna numa tabela que já existe, e o próximo pedido vai precisar mudar o banco à mão.
- **Detalhes menores:** a chave estrangeira não tem índice. `status`, `rate_plan` e `type` não têm CHECK. Os
  `Statement`/`ResultSet` de `Database.java:66-80` e os `ResultSet` dos repositórios
  (`repository/ReservationRepository.java:175,211`) não estão em try-with-resources; dependem do fechamento da
  conexão.
- **O que está certo:** todo o SQL usa `?` com parâmetros, e nada é montado com dado de fora. As conexões são
  devolvidas com try-with-resources. Os dados persistem: as mensagens continuam depois de reiniciar o serviço
  (`measures/behavior.md`).

## Comportamento

**Números:** 16 de 66 conferências passaram. Parte comum: 12 de 46. Dados depois de reiniciar: 1 de 3. Cenário 03: 2
de 15. Reservas simultâneas: 1 de 2 (em todas as rodadas, as duas chamadas deram 500).
**Nota:** ruim

Quase todas as falhas vêm de uma causa só: `POST /reservations` responde 500 quando o CPF chega com pontos e traço,
como no enunciado (veja Banco). Sem reserva criada, tudo o que depende dela falha em cascata: confirmação, e-mails,
check-in e check-out, cancelamentos, relatórios, os dados depois de reiniciar e todo o cenário 03. As únicas
conferências do cenário que passaram ("o financeiro não é avisado", "pendente cancelada não manda nada") passam
porque nada foi mandado. Para o dono da pousada, isso quer dizer que o site não consegue criar reserva se mandar o CPF
do jeito comum.

Para saber se o resto funciona, subi o serviço e chamei a API com o CPF só com dígitos. Assim, a reserva sai com 779,80
e é confirmada com o pago igual ao total. Saem os três e-mails da confirmação: o do hóspede, com o texto e o
comprovante exatos; o da governança, "Reserva confirmada - quarto 201", com 10/03/2027, 12/03/2027 e "2 hóspedes"; e o
do financeiro, "Pagamento recebido - reserva 1", com R$ 779,80. Cancelar uma reserva STANDARD confirmada manda
"Reserva cancelada - quarto 201" com as datas e "Reembolso - reserva 1" com R$ 779,80. Ou seja, o Observer funciona;
quem impede o uso é o CPF.

Outras falhas reais, fora da cascata:

- **Corpo que não é JSON:** responde 500 "Server Error", e não 400 INVALID_REQUEST. Não há tratador para o erro do
  Jackson (`api/ErrorHandler.java`).
- **Financeiro:** a reserva STANDARD cancelada com devolução total aparece no relatório com 0,00
  (`repository/ReservationRepository.java:256` filtra por `amount_paid > 0 OR refund > 0`). O enunciado pede só as
  que "deixaram dinheiro com a pousada". Confirmei chamando a API: `1,201,2027-03-10,2027-03-12,CANCELLED,0.00`.
- **Duas reservas ao mesmo tempo:** com o CPF sem pontuação, os dois pedidos foram criados em 9 de 9 rodadas (veja
  Banco). Isso pesa na coluna do banco, mas também aparece numa chamada que o texto descreve.
- **Ponto que o texto deixava em aberto:** "o 201 comporta 3" e "a diária do 201 é 389.90" falharam porque
  `number` sai como texto ("201") e o roteiro procura o número 201. Os dois valores estão certos na resposta
  (`"capacity":3,"nightlyRate":389.90`). O texto não diz o tipo de `number`, então isso não pesa.
- **Id fora do formato:** um id não numérico (`/reservations/abc`) responde 400 no formato do Javalin, não no
  `{error, message}` pedido. Ficou fora do roteiro e não pesa.

## Dúvidas

- O enunciado não diz em que formato o `guestCpf` chega no `POST /reservations`; ele só mostra o CPF formatado no
  comprovante. Tratei como defeito sério porque o formato com pontos é o mais provável, porque o 500 não é uma recusa
  com código, e porque o próprio sistema formata o CPF no comprovante (`domain/Reservation.java:150-158`), supondo que
  ele chega cru. Quem entender que o texto fixava o CPF só com dígitos daria `com problemas` ao comportamento (o resto
  funciona) e manteria `ruim` no banco pela reserva em dobro.
- A reserva em dobro foi medida por mim, com o CPF sem pontuação, porque o roteiro não chegou a criar reservas. O
  código (`repository/ReservationRepository.java:186-217` sem transação nem restrição) confirma a medida.
