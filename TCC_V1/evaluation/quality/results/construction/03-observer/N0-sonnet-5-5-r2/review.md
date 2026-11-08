# Revisão: construção, cenário 03

## Visão geral

O projeto tem seis pacotes: `web` (a classe `App`, com as rotas do Javalin), `service` (os serviços de reserva e de relatórios e as interfaces de acesso a dados e de transação), `domain` (registros e enums: `Reservation`, `Room`, `PricingCalculator`, `Money`, `Receipt`), `events` (o evento, a interface de ouvinte e um ouvinte por interessado), `infra` (o JDBC com HikariCP e o esquema) e a raiz (`Main`, onde tudo é montado). Uma requisição passa assim: a rota em `web/App.java:40` lê o corpo, `ReservationService` abre uma transação (`Database.run`, `infra/Database.java:112`), trava a linha do quarto ou da reserva com `FOR UPDATE`, aplica a regra do registro imutável `Reservation` (`domain/Reservation.java:24-45`), grava pelo repositório e publica o evento. Os ouvintes gravam as mensagens na mesma transação. Os dados ficam em três tabelas do PostgreSQL, criadas por `CREATE TABLE IF NOT EXISTS` no código. O roteiro de comportamento passou inteiro (66 de 66), e o Observer está aplicado direito. Não há defeito sério. Os problemas são menores: um ciclo entre `service` e `events`, o esquema criado sem migrações nem `CHECK`, e testes que não chegam no SQL de verdade.

## Arquitetura

**Números:** 6 pacotes, 12 dependências entre eles, 1 ciclo (`service` ↔ `events`), de `measures/dependencies.md`. O Javalin aparece só em `web` e na raiz, e o JDBC só em `infra`. A maior classe é `JdbcReservationRepository`, com 169 linhas.
**Nota:** com problemas

A borda HTTP é fina. Cada rota só converte a entrada, chama o serviço e escreve a resposta (`web/App.java:39-49`), e o tratamento de erro fica num lugar só (`web/App.java:51-55`). As regras não conhecem nem Javalin nem JDBC. `ReservationService` recebe as interfaces `Transactions`, `RoomRepository`, `ReservationRepository` e `MessageStore` (`service/ReservationService.java:25-31`), e a regra de situação e devolução mora no domínio (`domain/Reservation.java:24-45`). O SQL fica isolado em `infra`. A montagem acontece num lugar só, `Main.java:32-49`, inclusive a lista de quem é avisado (`Main.java:41-44`). Não há classe grande demais.

O defeito é o ciclo entre `service` e `events`. O serviço depende do pacote de eventos (`service/ReservationService.java:13-14`), e `events/StoredMessageSender.java:4` depende de `service.MessageStore`. A interface de gravação das mensagens ficou no pacote do serviço, e a classe que só repassa para ela ficou no pacote dos eventos. O ciclo é pequeno, entre dois pacotes vizinhos, mas a responsabilidade está no lugar errado. Há também um detalhe menor: o status HTTP está dentro do domínio (`domain/ErrorCode.java:4-9`, usado em `web/App.java:60`).

## Design de baixo nível

**Números:** 3 interessados (hóspede, governança, financeiro) e 2 eventos (confirmação e cancelamento). Os 3 interessados estão na estrutura de ouvintes e nenhum está fora dela, e o serviço não monta nenhum e-mail.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

O pattern pedido é o Observer, e ele está aplicado. `ReservationListener` (`events/ReservationListener.java:7-13`) tem um método por evento, com implementação vazia por padrão. Há uma implementação por interessado: `GuestNotifier` (`events/GuestNotifier.java:15-23`), `HousekeepingNotifier` (`events/HousekeepingNotifier.java:18-39`) e `FinanceNotifier` (`events/FinanceNotifier.java:18-34`). O e-mail do hóspede também é um ouvinte. `ReservationEvents` recebe a lista de fora e percorre todos os ouvintes (`events/ReservationEvents.java:9-20`), e a lista é montada em `Main.java:41-44`. O serviço só anuncia o fato (`service/ReservationService.java:76` e `:86`) e não sabe quem é avisado. Os dois eventos passam pela estrutura, e o cancelamento leva junto a situação anterior (`events/ReservationEvent.java:16-21`). Assim, a decisão "a reserva pendente não avisa ninguém" e "sem devolução não avisa o financeiro" fica dentro de cada ouvinte (`events/HousekeepingNotifier.java:30`, `events/FinanceNotifier.java:28`). Para incluir o restaurante ou o transfer, basta uma classe nova e uma linha em `Main`. Um evento novo pede mexer no `switch` de `ReservationEvents.java:15-18` e num método `default`. O custo é pequeno, e o `switch` sobre a interface `sealed` é verificado pelo compilador.

Exagero: as interfaces de repositório, de transação e de `MessageStore` têm uma segunda implementação, a de memória dos testes (`src/test/.../InMemory.java:149-215`), então não sobram. A única estrutura discutível é `MessageSender` com uma única `StoredMessageSender`, que só repassa (`events/StoredMessageSender.java:13-16`). É pequena, e o texto adianta que os e-mails "por enquanto" não saem de verdade. Não a contei como exagero (ver Dúvidas).

O resto do código está bem. Dinheiro em `BigDecimal` com `HALF_EVEN` a cada passo (`domain/Money.java:16-18`, `domain/PricingCalculator.java:20-37`). Datas em `LocalDate`, enums para situação, plano, tipo e canal. Registros imutáveis com transições que validam a situação (`domain/Reservation.java:24-45`). Nenhum `static` mutável. Exceções do JDBC embrulhadas sem serem engolidas. Pontos menores: o registro `Reservation` tem 16 campos posicionais, e o construtor aparece repetido em `domain/Reservation.java:48-49` e `:58-59` e chamado com 16 argumentos em `service/ReservationService.java:60-62`, o que facilita trocar campos de lugar. Além disso, `Receipt.formatDate` serve de utilitário de data para os ouvintes (`events/HousekeepingNotifier.java:22-23`).

## Testes

**Números:** 14 testes rodaram, 0 falhas, 0 erros, 0 pulados, e há 14 métodos escritos. Cobertura de 59,9% das linhas (308/514) e 70,0% dos ramos (84/120). Por pacote: `domain` 86/86, `service` 95/95, `events` 60/61 e `infra` 0/184 (de `measures/tests.md`).
**Nota:** com problemas

Os testes conferem as regras com valores. Estão cobertos o exemplo da alta temporada (`PricingTest.java:254-259`), o meio para o par em 37,485 → 37,48 (`PricingTest.java:268-271`), a fronteira da temporada (`PricingTest.java:244-252`, `:274-278`) e o formato R$ 1.234,56 (`PricingTest.java:280-285`). Também estão a máquina de estados com os códigos de erro (`ReservationFlowTest.java:408-425`), os dois relatórios comparados linha a linha (`ReservationFlowTest.java:427-449`) e a borda HTTP com os códigos de status (`HttpApiTest.java:47-105`). A parte nova também tem teste: o e-mail do hóspede é comparado inteiro, e os assuntos e valores da governança e do financeiro são conferidos na confirmação (`ReservationFlowTest.java:337-367`) e no cancelamento, nos três casos: pendente sem aviso, normal com dois avisos e não reembolsável só com a governança (`ReservationFlowTest.java:378-406`). Teste de mutação: inverter o `wasConfirmed` (`events/HousekeepingNotifier.java:30`) ou trocar `<= 0` por `< 0` em `events/FinanceNotifier.java:28` faz um teste falhar (`ReservationFlowTest.java:384` e `:404`). Já apagar a linha "Hóspedes" do aviso da governança (`events/HousekeepingNotifier.java:24`) não derruba nenhum teste, porque `ReservationFlowTest.java:361` só olha as datas. O corpo do aviso de cancelamento também não é conferido, só o assunto (`ReservationFlowTest.java:393`).

A lacuna maior é que nenhum teste toca o banco: `infra` tem 0% de cobertura. A sobreposição de noites, a ocupação e o filtro do mês estão no SQL (`infra/JdbcReservationRepository.java:92`, `:108-109`, `:117`). Os testes conferem uma cópia dessas regras escrita em Java no `InMemory` (`InMemory.java:187-203`), não o SQL que roda de verdade. A trava contra reserva duplicada também fica sem teste. Trocar `check_out > ?` por `>=` na consulta de ocupação passaria em todos os testes.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Dinheiro em `numeric(12,2)` nas 6 colunas e datas em `date`. A única chave estrangeira tem índice. Nenhuma restrição CHECK, UNIQUE ou EXCLUDE, e as 4 colunas de situação e tipo (`status`, `rate_plan`, `type`, `channel`) estão em `varchar` sem CHECK. O esquema é criado por `CREATE TABLE` no código, com 0 arquivos de migração (de `measures/database.md`).
**Nota:** com problemas

Os tipos estão certos e o código que fala com o banco está bem feito. Todo SQL usa parâmetros `?`, e os trechos concatenados são só constantes (`infra/JdbcReservationRepository.java:21-24`, `:80-117`). Há pool HikariCP (`infra/Database.java:65-73`), conexões em try-with-resources e uma transação por operação com rollback (`infra/Database.java:112-136`). Por isso a mudança de situação e as mensagens dos ouvintes são gravadas juntas. A reserva duplicada é evitada travando a linha do quarto com `FOR UPDATE` dentro da transação (`service/ReservationService.java:50`, `infra/JdbcRoomRepository.java:44`). A trava vale entre processos, e o roteiro confirmou isso: nenhuma das 10 rodadas simultâneas criou as duas reservas.

Os defeitos:
- O esquema é criado por `CREATE TABLE IF NOT EXISTS` num vetor de textos (`infra/Database.java:14-60`), sem migrações versionadas. O próximo pedido que mexer numa coluna não tem como evoluir um banco que já tem dados.
- O banco não protege nenhuma regra: não há CHECK em `status`/`rate_plan`/`guests`/`check_out > check_in` nem EXCLUDE de sobreposição. A proteção depende toda da trava no código.
- A carga inicial dos quartos faz `ON CONFLICT ... DO UPDATE` a cada vez que o sistema sobe (`infra/Database.java:58-59`), e isso apaga sem aviso qualquer ajuste feito à mão nos quartos.

## Comportamento

**Números:** 66 de 66 conferências passaram, nenhuma falhou. Parte comum 46/46, reinício 3/3, cenário 03 15/15, reservas simultâneas 2/2 (de `measures/behavior.md`).
**Nota:** bom

Tudo o que o texto descreve funcionou pela API a partir de um banco vazio:
- os preços com temporada e meio para o par;
- as recusas com os códigos certos;
- a confirmação com o e-mail e o comprovante, o check-in, o check-out e a segunda via;
- as devoluções nos dois planos;
- os dois relatórios em CSV.

Os dados continuaram lá depois de reiniciar o sistema. Na parte nova, a governança e o financeiro recebem os avisos certos na confirmação e no cancelamento, com datas, número de hóspedes e valores. O financeiro não é avisado quando não há nada a devolver, e a reserva pendente cancelada não avisa ninguém. Duas reservas simultâneas para o mesmo quarto nunca entraram juntas. Para o dono da pousada, o sistema faz o que ele pediu.

## Dúvidas

- `MessageSender` tem uma única implementação, `StoredMessageSender`, que só repassa para `MessageStore` (`events/StoredMessageSender.java:13-16`), e nenhum teste a substitui. Pelo critério estrito ("uma camada que só repassa"), isso poderia contar como exagero. Contei como contenção porque a peça é mínima e o texto diz que os e-mails "por enquanto" não saem de verdade. É a mesma peça que causa o ciclo entre `events` e `service`.
- Na nota de testes, a cobertura das regras em Java é quase total e a parte nova está bem conferida. Dei `com problemas` porque as regras que estão no SQL e a trava contra duplicação não têm teste. Se a ausência de teste contra o banco pesar pouco numa construção em que o texto não pedia testes, a nota poderia ser `bom`.
