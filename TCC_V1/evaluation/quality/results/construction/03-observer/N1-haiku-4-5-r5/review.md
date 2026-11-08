# Revisão: construção, cenário 03

## Visão geral

O projeto é um serviço Javalin com 11 pacotes: `App` (que monta as rotas e também é a única "montagem"), `service`
(regras de reserva e relatórios), `db` (JDBC puro com HikariCP, esquema criado por `CREATE TABLE IF NOT EXISTS` em
`db/Database.java:27-65`), `notification` (interface `ReservationNotifier`, evento `ReservationEvent`, registro estático
`NotificationManager`) com três ouvintes em `notification/notifiers`, além de `model`, `enums`, `util` e `api`. Uma
requisição passa pelo handler em `App.java` (que lê o corpo com Gson e às vezes já consulta o repositório), vai a um
método estático de `ReservationService`, que chama métodos estáticos dos repositórios; os dados ficam no PostgreSQL. O
pedido do cenário (avisar governança e financeiro, e mais gente no futuro) foi resolvido com um Observer de verdade, e
os 15 itens do cenário passam. O mais sério está na parte comum: a checagem de disponibilidade só olha reservas
CONFIRMED/CHECKED_IN/CHECKED_OUT (`service/ReservationService.java:34-37`) e a confirmação não confere de novo, então
duas reservas pendentes do mesmo quarto e da mesma noite são criadas e as duas podem ser confirmadas e pagas (verifiquei
pela API: reservas 1 e 2 do quarto 101 em 05/12/2027, as duas CONFIRMED, e o relatório de ocupação mostrando
`total,2/5` com um único quarto). Além disso, todo o mês de dezembro é cobrado como alta temporada
(`util/PricingCalculator.java:21`, quando a alta começa em 15/12): uma noite no 101 em 05/12/2027 saiu R$ 299,88 em vez
de R$ 249,90. Os testes cobrem 5,8% das linhas e não tocam nas regras de reserva nem nos avisos.

## Arquitetura

**Números:** 11 pacotes, 28 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Javalin só no pacote
raiz; JDBC e pool só em `db`. Maior classe: `App.java`, 300 linhas.
**Nota:** com problemas

O sentido das dependências é bom e não há ciclo, mas a borda HTTP não é fina e não há montagem de verdade:

- Os handlers falam direto com o repositório e carregam regra: `App.java:96`, `:112`, `:155`, `:163`, `:189`, `:201`
  chamam `ReservationRepository`/`RoomRepository`; a regra de quando existe a segunda via mora no handler
  (`App.java:196`, "pago maior que zero"), e o valor pago na confirmação é decidido no handler
  (`App.java:119`, passa `reservation.getTotal()` ao serviço). O comprovante é montado no handler, duas vezes
  (`App.java:164-176` e `App.java:202-214`).
- Não há injeção: serviços, repositórios e o `NotificationManager` são todos `static`
  (`service/ReservationService.java:16`, `db/ReservationRepository.java:13`, `notification/NotificationManager.java:7`),
  e o pool é um campo estático global (`db/Database.java:10`). A única coisa montada no `main` são os ouvintes
  (`App.java:270-274`).
- `util` depende de `db`: `util/DocumentGenerator.java:10` busca o quarto no banco no meio da formatação do
  comprovante, e os ouvintes também gravam direto pelo `MessageRepository`
  (`notification/notifiers/GuestNotifier.java:19`).
- As regras (`service`) não importam Javalin nem `java.sql`, o que é o lado bom.

## Design de baixo nível

**Números:** 3 interessados na estrutura (hóspede, governança, financeiro), 0 fora dela; 2 eventos (CONFIRMED,
CANCELLED), os dois passando pela mesma lista de ouvintes.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

O pattern: `ReservationNotifier` (`notification/ReservationNotifier.java:3-5`) é a interface de ouvinte, com uma
implementação por interessado: `GuestNotifier`, `HousekeepingNotifier` e `FinanceNotifier`. Eles são registrados na
montagem (`App.java:270-274`), e o serviço só anuncia o evento, sem conhecer destinos nem textos:
`service/ReservationService.java:67` na confirmação e `:124-126` no cancelamento (só de reserva confirmada, que é a regra
do texto). O e-mail do hóspede também virou ouvinte, e a regra "sem devolução não avisa o financeiro" fica no próprio
ouvinte (`notifiers/FinanceNotifier.java:28`). Acrescentar o restaurante ou o transfer é uma classe nova e uma linha no
`main`. O que afasta um pouco: a lista é um `static` global mutável (`NotificationManager.java:7-11`), e não algo que o
serviço recebe de fora; e um ouvinte que falha derruba a operação depois que o status já foi gravado (veja "Banco").

Exagero: nada relevante. A única estrutura a mais é `model/Document.java`, que só carrega os campos até
`DocumentResponse`, pequena demais para contar.

O resto do código tem defeitos espalhados pela borda HTTP:

- todos os handlers têm `catch (Exception e)` que transforma qualquer falha (inclusive do banco) em
  `400 INVALID_REQUEST` com a mensagem interna (`App.java:60-62`, `:88-90`, `:104-106`, ... `:258-260`);
- os códigos de erro viajam como texto na mensagem de `IllegalArgumentException` (`ReservationService.java:23`, `:27`,
  `:40`...) e são traduzidos por um `switch` de strings (`App.java:276-299`);
- dinheiro sai como `String` via `String.format("%.2f")` (`api/response/ReservationResponse.java:34-40`,
  `DocumentResponse.java:29-31`, `App.java:58`), o que contradiz o texto (número) e depende do locale da JVM;
  `Formatter.formatMoneyCSV` remenda isso com `replace(',', '.')` (`util/Formatter.java:26`);
- a mesma validação repetida (hóspedes < 1 em `App.java:71`, `ReservationService.java:30` e `:141`), o comprovante
  montado duas vezes (`App.java:164-176`/`:202-214`);
- `isHighSeason` lê o dia e não usa (`util/PricingCalculator.java:18`), que é exatamente a origem do erro de dezembro;
  código morto (`Formatter.unformatCPF`, `Reservation` com setters nunca usados);
- duas bibliotecas de JSON (Gson para ler, Jackson do Javalin para escrever).

Os tipos do domínio estão certos (`BigDecimal` com `HALF_EVEN` a cada passo, `LocalDate`, `enum`s), o que mantém a
nota fora de `ruim`.

## Testes

**Números:** 12 testes rodaram, 0 falharam, 12 métodos escritos (2 classes); cobertura de linhas 44 de 756 (5,8%) e de
ramos 16 de 191 (8,4%). `service`, `db`, `notification` e `notification.notifiers`: 0%. Nenhum teste pela API ou
contra o banco (`measures/tests.md`).
**Nota:** ruim

Só há testes de `Formatter` (`FormatterTest.java`) e de `PricingCalculator` (`PricingCalculatorTest.java`). Esses
conferem valores de verdade (599,76 em `PricingCalculatorTest.java:94`, desconto 89,96 em `:102`, total 509,80 em
`:114`), mas é tudo. Nada testa a parte nova do cenário (nenhum dos três ouvintes, nem que a pendente cancelada não
avisa ninguém, nem que o financeiro fica de fora quando não há devolução): se alguém apagasse o `if` de
`FinanceNotifier.java:28`, ou trocasse o endereço da governança, nenhum teste falharia. Também ficam sem rede a máquina
de estados, o cancelamento e a devolução, a disponibilidade do quarto, os códigos de erro e os dois relatórios. O teste
de temporada confere 15/12 (`PricingCalculatorTest.java:61-62`), mas nenhum dia de 1 a 14 de dezembro, e por isso o
erro de dezembro passou.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro em `numeric(10,2)`,
datas em `date`. Conferências que acusaram: a chave estrangeira `reservations(room_number)` sem índice; nenhum CHECK
nas colunas de situação, plano, tipo e canal; nenhuma restrição UNIQUE/EXCLUDE; 0 transações explícitas, 0
`FOR UPDATE`; 2 SQL montados com `+` (`measures/database.md`).
**Nota:** com problemas

- O esquema nasce de `CREATE TABLE IF NOT EXISTS` dentro do código (`db/Database.java:27-65`), sem migrações: a primeira
  coluna nova vai exigir mexer à mão num banco que já tem dados.
- Nada protege a regra de um quarto por noite: nem restrição (um `EXCLUDE` sobre `daterange`), nem trava, nem
  transação. O roteiro concorrente criou as duas reservas nas 10 rodadas. O furo sequencial (pendentes não contam) é
  do serviço e pesa em "Comportamento".
- Escritas que deviam andar juntas estão separadas: o cancelamento grava a devolução e depois o status em dois
  comandos com autocommit (`service/ReservationService.java:119-120`); a confirmação grava o status
  (`ReservationService.java:64`) e só depois os ouvintes gravam as mensagens, sem transação. Se a gravação de uma mensagem
  falhar, a reserva fica CONFIRMED e o cliente recebe 400.
- O SQL concatenado não recebe dado de fora: em `db/ReservationRepository.java:82` entram nomes de `enum`, e em
  `db/RoomRepository.java:34` um `int`. Não é injeção, mas foge do padrão `?` usado no resto.
- O relatório financeiro filtra por `amount_paid > 0` (`ReservationRepository.java:165`), e não pelo que ficou com a
  pousada (pago menos devolvido), e o de ocupação consulta o banco uma vez por quarto (`ReportService.java:21-22`).
  Pesa pouco com cinco quartos.
- O lado bom: pool HikariCP, try-with-resources em todas as conexões, tipos certos, `NOT NULL` no que é obrigatório e
  os dados que sobrevivem ao reinício (3 de 3).

## Comportamento

**Números:** 61 de 66 conferências passaram. Parte comum: 42 de 46; dados depois de reiniciar: 3 de 3; cenário 03: 15
de 15; duas reservas ao mesmo tempo: 1 de 2 (nas 10 rodadas, as duas reservas foram criadas) (`measures/behavior.md`).
**Nota:** ruim

Os avisos do cenário funcionam inteiros: o hóspede, a governança com as datas e os hóspedes, o financeiro com o valor
pago, o cancelamento com as datas liberadas e o reembolso, nenhum aviso ao financeiro sem devolução e nenhum aviso na
pendente cancelada. O que pesa, para o dono da pousada, está na parte comum:

- **Venda dupla do quarto.** "Quarto ocupado numa das noites" respondeu 201 em vez de 409, e as 10 rodadas concorrentes
  criaram as duas reservas. A causa é `ReservationService.java:34-37`: reservas pendentes não bloqueiam o quarto,
  contrariando o texto ("contando só as que não foram canceladas"), e confirmar não confere de novo. Pela API, confirmei
  duas reservas pagas do 101 na mesma noite de 05/12/2027.
- **Cobrança errada de 1 a 14 de dezembro** (fora do roteiro, verificado pela API): uma noite no 101 em 05/12/2027
  custou R$ 299,88 em vez de R$ 249,90 (`util/PricingCalculator.java:21`).
- **Relatório de ocupação errado** (fora do roteiro, verificado): ele conta reservas pendentes, que ainda não foram pagas
  (`ReservationRepository.java:140`, que só exclui CANCELLED), e conta reservas em vez de quartos
  (`ReportService.java:30`). A noite da venda dupla saiu como `total,2/5` com um quarto só.
- **Dinheiro como texto no JSON** (`"amountPaid":"0.00"` em vez de `0.00`): o site recebe strings onde o combinado era
  número (`ReservationResponse.java:34-40`).
- **Segunda via de reserva não paga** responde 404 RESERVATION_NOT_FOUND em vez de 409 OPERATION_NOT_ALLOWED
  (`App.java:196-198`). Pior: como a regra é "pago > 0", uma reserva NON_REFUNDABLE **cancelada** emite comprovante
  (verifiquei: reserva 3 cancelada, `GET /reservations/3/document` deu 200), o que o texto não permite.
- **Financeiro:** a reserva STANDARD cancelada com devolução total aparece com `0.00` (`ReservationRepository.java:165`),
  e o texto pede só as que deixaram dinheiro. O total ficou certo (599,76), mas na forma `total,,,,,599.76`. Essa forma
  é uma escolha defensável para um CSV, e ela sozinha não pesaria.

As falhas atingem as regras centrais do negócio: vender o mesmo quarto duas vezes e cobrar errado em dezembro. Por
isso a nota é `ruim`, mesmo com o cenário inteiro passando.

## Dúvidas

- A classe do pattern: o serviço não recebe a lista de ouvintes de fora, chama um registro estático global
  (`NotificationManager.java:7-17`) que o `main` preenche. Os interessados são registrados na montagem e o serviço não
  os conhece, então dei `acerto`. Um leitor mais estrito poderia ver aí um defeito de montagem, que eu contei na
  arquitetura e na nota de design.
- A nota de design ficou entre `com problemas` e `ruim`, por causa do `static` em tudo e do `catch (Exception)` em todos
  os handlers. Ficou em `com problemas` porque os tipos do domínio estão certos e os defeitos se concentram na borda HTTP.
- A falta de qualquer proteção no banco contra a venda dupla poderia puxar "Banco" para `ruim`. Contei o furo
  sequencial (as pendentes que não contam) no comportamento, para não pesar o mesmo defeito em duas colunas.
