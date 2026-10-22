# Revisão: construção, cenário 03

## Visão geral

O agente montou o projeto em sete pacotes sob `br.com.marealta.reservas`. O pacote `api` cuida do HTTP (Javalin, Jackson). `service` tem a orquestração. `domain` guarda as regras e não depende de nada. `notification` tem os ouvintes da reserva, `persistence` o JDBC e `report` os CSV. O `Main` liga as peças. Uma requisição segue este caminho: `Api` (por exemplo `api/Api.java:43`), depois `ReservationService.confirm` (`service/ReservationService.java:65`). Esse método abre a transação (`persistence/Database.java:91`), trava a linha (`ReservationRepository.findForUpdate`) e aplica a mudança de situação em `Reservation.confirm` (`domain/Reservation.java:55`). Em seguida grava e avisa a lista de `ReservationListener` que recebeu do `Main` (`Main.java:42-47`). Os dados ficam no PostgreSQL, em três tabelas criadas por `CREATE TABLE IF NOT EXISTS` no código (`persistence/Database.java:11-44`). As mensagens "enviadas" também vão para o banco. Nada sério apareceu: as 66 conferências do comportamento passaram, inclusive as reservas simultâneas, e o Observer foi bem aplicado. Os defeitos são menores: um ciclo pequeno entre `persistence` e `notification`, o esquema sem migrações e sem `CHECK`, e testes que conferem os avisos novos só pelo assunto e pela contagem.

## Arquitetura

**Números:** 7 pacotes, 18 dependências entre eles, 1 ciclo (`persistence` ↔ `notification`). `domain` não depende de ninguém e é usado por 5 pacotes. Javalin só aparece em `api` e no `Main`, JDBC só em `persistence`.
**Nota:** com problemas

A borda HTTP é fina: cada rota lê a requisição, chama o serviço e escreve a resposta (`api/Api.java:39-59`). A validação da entrada fica isolada em `api/RequestParser.java`, e o formato do JSON em `api/Views.java`. As regras não conhecem nem Javalin nem SQL. O preço está em `domain/Quote.java:8-17` e `domain/Season.java:20-23`, as situações da reserva em `domain/Reservation.java:55-99` e a devolução em `domain/RatePlan.java:22-24`. O serviço só usa `Database.inTransaction` para marcar onde a transação começa e termina (`service/ReservationService.java:41`). Todo o SQL fica nos repositórios de `persistence`, e a montagem acontece num lugar só (`Main.java:34-49`).

O que pesa:
- **O ciclo:** `persistence/MessageRepository.java:3` importa `notification.Message`, e `notification/MessageLog.java:3` importa `persistence.MessageRepository`. O registro `Message` está no pacote errado; ele devia ficar no domínio ou na persistência. O ciclo é pequeno e fica entre dois pacotes vizinhos.
- **Um atalho na borda:** `GET /messages` vai direto da borda ao repositório (`api/Api.java:19,53-56`), sem passar por nenhuma regra.

Nenhuma classe é desproporcional: a maior é `ReservationRepository`, com 148 linhas.

## Design de baixo nível

**Números:** 3 interessados no total (hóspede, governança, financeiro), todos ouvintes. 4 casos de aviso (2 na confirmação, 2 no cancelamento), mais o e-mail do hóspede, e nenhum fora da estrutura. O serviço tem 0 linhas que montam e-mail.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern pedido (Observer):**
- **A interface:** `notification/ReservationListener.java:4-11` declara um ouvinte com dois eventos, `onConfirmed` e `onCancelled`. Os dois têm implementação vazia por padrão, então cada ouvinte só reage ao que lhe interessa.
- **Um ouvinte por interessado:**
  - `GuestConfirmationEmail` (`notification/GuestConfirmationEmail.java:13-22`): o e-mail do hóspede também virou ouvinte;
  - `HousekeepingNotifier` (`notification/HousekeepingNotifier.java:16-36`);
  - `FinanceNotifier` (`notification/FinanceNotifier.java:16-33`).
- **O serviço não conhece os interessados:** ele recebe a lista de fora (`service/ReservationService.java:26-31`) e só percorre os ouvintes (`service/ReservationService.java:66-76`). Os endereços entram pelo construtor, no `Main` (`Main.java:20-21,42-45`).
- **Os eventos:** `ReservationConfirmed` e `ReservationCancelled` são registros simples.
- **As condições de cada aviso** ficam dentro do próprio ouvinte, e não no serviço:
  - a governança ignora o cancelamento de reserva que não foi paga (`HousekeepingNotifier.java:29`);
  - o financeiro ignora quando não há o que devolver (`FinanceNotifier.java:27`).
- **Um interessado novo** (o restaurante, o transfer) só exige uma classe nova e uma linha na lista do `Main`. O serviço não muda.

Nada afasta o código do pattern. Um detalhe: os ouvintes rodam dentro da transação da confirmação (`service/ReservationService.java:106-112`). Se um ouvinte falhar, a confirmação é desfeita. Com as mensagens gravadas no mesmo banco, isso é até desejável. Mas, quando houver um envio de verdade ou um ouvinte externo, vai ser preciso decidir se a falha de um aviso pode barrar a confirmação.

**O exagero:** não há estrutura sem necessidade. `MessageLog` (`notification/MessageLog.java:6-18`) é um envoltório fino sobre o repositório, mas fixa o canal `EMAIL` e é o ponto onde um envio de verdade entraria. `NewReservation` é só o comando de entrada.

**O resto do código:**
- **Tipos:** os tipos são os certos. O dinheiro é `BigDecimal` com `HALF_EVEN` em cada passo (`domain/Money.java:19-21`, `domain/Quote.java:12,16`, `domain/RatePlan.java:19`). As datas são `LocalDate`, e situação, plano e tipo de quarto são `enum`.
- **Mudança de estado:** fica concentrada na entidade, com `requireStatus` (`domain/Reservation.java:94-99`).
- **Duplicação e nomes:** há poucos pontos menores:
  - a lista das situações "pagas" aparece em `domain/ReservationStatus.java:7-9` e de novo, como texto, no SQL (`persistence/ReservationRepository.java:99`);
  - a regra do "que ficou com a pousada" está em `domain/Reservation.java:90-92` e repetida no filtro SQL (`persistence/ReservationRepository.java:106`);
  - a formatação de data mora em `Money.brDate` (`domain/Money.java:34`), um nome que não diz o que faz;
  - `roomOf` usa `orElseThrow()` sem mensagem (`service/ReservationService.java:116`).

Nada disso dificulta mudar o código.

## Testes

**Números:** 10 testes rodaram, com 0 falhas, 0 erros e 0 pulados. São 10 métodos escritos: 6 de unidade em `PricingTest` e 4 pela API, contra o banco, em `ApiIntegrationTest`. Cobertura: linhas 498/554 (89,9%), ramos 76/98 (77,6%). O pacote `domain` tem 123/123 linhas cobertas, `service` 54/54 e `notification` 52/53.
**Nota:** com problemas

**O que está bem conferido:**
- **Preço, com valores:** temporada (`PricingTest.java:24-28`), limites da temporada (`PricingTest.java:31-37`), estadia que entra na alta (`PricingTest.java:40-43`), desconto com meio para o par, 116,97 (`PricingTest.java:46-51`).
- **Comprovante:** o texto inteiro do comprovante (`PricingTest.java:54-66`) e o formato do dinheiro (`PricingTest.java:69-72`).
- **Pela API:** as recusas com o código de erro (`ApiIntegrationTest.java:141-157`), o relatório de ocupação com a regra de quem entra e quem sai (`ApiIntegrationTest.java:162-165`) e o financeiro com o valor 509,80 (`ApiIntegrationTest.java:173-175`).
- **A parte nova:**
  - a confirmação deve gerar 3 mensagens, com os assuntos e os endereços da governança e do financeiro (`ApiIntegrationTest.java:100-107`);
  - o cancelamento de pendente não gera nada (`:119`);
  - o STANDARD confirmado gera 2 mensagens (`:126-128`);
  - o NON_REFUNDABLE gera 1 só (`:135`).

As contagens são boa rede: se alguém tirasse a guarda `refund > 0` do financeiro (`FinanceNotifier.java:27`) ou a guarda `wasPaid` da governança (`HousekeepingNotifier.java:29`), um teste falharia.

**O que fica sem rede:** o conteúdo dos avisos novos nunca é conferido.
- As asserções procuram trechos no JSON de todas as mensagens juntas, com `messages.toString().contains(...)` (`ApiIntegrationTest.java:102-107,125-128`). Por isso nem confirmam que o assunto "Pagamento recebido" foi para o endereço do financeiro.
- Se o `FinanceNotifier` escrevesse o total em vez do valor pago, ou um reembolso errado (`FinanceNotifier.java:20,31`), nenhum teste falharia. O mesmo vale se a governança deixasse de mandar as datas ou o número de hóspedes (`HousekeepingNotifier.java:20-22`).
- O corpo do e-mail do hóspede (`GuestConfirmationEmail.java:16-20`) também não é conferido.
- Nenhum teste cobre duas reservas simultâneas.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(12,2)` nas 6 colunas, as datas em `date`, e a única chave estrangeira tem índice. As conferências acusaram duas coisas:
- 0 restrições `CHECK`, `UNIQUE` ou `EXCLUDE`, e as colunas `status`, `rate_plan`, `type` e `channel` são `text` sem `CHECK`;
- o esquema é criado no código, em `Database.java`, com 0 arquivos de esquema e sem migrações.

No código: 0 pool de conexões, 1 transação explícita e 2 `FOR UPDATE`.
**Nota:** com problemas

**O que está certo:**
- **Tipos e chaves:** `persistence/Database.java:11-44` usa `NUMERIC(12,2)`, `DATE`, `NOT NULL` em tudo o que é obrigatório e `REFERENCES rooms(number)`, com o índice `(room_number, check_in)` (`:36`).
- **Reserva duplicada:** fica protegida no banco, e não no processo. A criação trava a linha do quarto com `SELECT ... FOR UPDATE` (`persistence/RoomRepository.java:66-77`) antes de procurar sobreposição (`persistence/ReservationRepository.java:81-94`), tudo na mesma transação (`service/ReservationService.java:41-58`). Isso vale mesmo com mais de um processo, e o roteiro confirmou com 10 rodadas sem duplicação.
- **SQL:** todo o SQL usa parâmetros `?`. O que entra concatenado são constantes, como `COLUMNS` (`ReservationRepository.java:19-20`).
- **Atomicidade:** as escritas da reserva e das mensagens andam juntas na mesma transação.

**Os defeitos:**
- **O esquema não evolui:** ele é criado com `CREATE TABLE IF NOT EXISTS` dentro do código (`persistence/Database.java:11-44,65`), sem migrações versionadas. A primeira coluna nova numa tabela que já existe vai exigir intervenção manual.
- **O banco não protege nenhuma regra por conta própria:**
  - não há `CHECK` em `status`, `rate_plan` nem `guests`, nem `check_out > check_in`;
  - não há restrição de exclusão para as noites; a regra depende só da trava no código.
- **Não há pool:** cada transação abre uma conexão nova com `DriverManager.getConnection` (`persistence/Database.java:95`). Basta neste tamanho, mas custa sob carga.

## Comportamento

**Números:** 66 de 66 conferências passaram, com 0 falhas:
- parte comum: 46/46;
- depois de reiniciar: 3/3;
- cenário 03: 15/15;
- reservas simultâneas: 2/2.

**Nota:** bom

Para o dono da pousada, o sistema faz o que o texto pede:
- **Preço:** cobra com a alta temporada e o arredondamento meio para o par (599,76 e o desconto de 37,48).
- **Recusas:** devolvem os códigos de erro combinados com o site.
- **Comprovante e e-mail do hóspede:** saem com o texto pedido.
- **Relatórios:** os dois batem, e a reserva não reembolsável cancelada continua contando no financeiro.
- **Reinício:** nada se perde ao reiniciar o serviço.
- **Os avisos novos:**
  - na confirmação, a governança recebe as datas e o número de hóspedes, e o financeiro o valor pago;
  - no cancelamento de reserva confirmada, a governança recebe as datas liberadas e o financeiro o reembolso;
  - o financeiro fica de fora quando não há o que devolver, e o cancelamento de pendente não avisa ninguém.
- **Reservas simultâneas:** dois pedidos ao mesmo tempo para o mesmo quarto nunca criam duas reservas.

## Dúvidas

Nenhuma que mude as notas. Na coluna de testes, a nota `com problemas` (e não `bom`) vem de os testes não conferirem o conteúdo dos avisos novos (valores, datas e hóspedes). As contagens e os assuntos conferidos cobrem boa parte da regra, então a nota fica perto do limite entre as duas.
