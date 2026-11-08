# Revisão: construção, cenário 03

## Visão geral

O projeto tem 7 pacotes (`web`, `service`, `domain`, `events`, `notification`, `persistence` e a raiz com `Main` e `App`). Uma requisição entra em `web/Routes.java`, que lê o corpo com `RequestParser` e chama `ReservationService` ou `ReportService`. Cada operação do serviço abre uma transação (`persistence/Database.java:41-65`, com a conexão num `ThreadLocal`). As transições de estado e o reembolso ficam no record imutável `domain/Reservation.java`, o preço fica em `domain/Pricing.java` e o SQL fica nos repositórios de `persistence/`. Os dados ficam no PostgreSQL, em três tabelas que `persistence/Schema.java` cria quando o sistema sobe. Os avisos seguem o Observer. O serviço publica `ReservationConfirmed`/`ReservationCancelled` num `EventBus` (`service/ReservationService.java:70,81`), e três ouvintes (hóspede, governança e financeiro) são registrados na montagem (`App.java:31-34`). As 66 conferências do roteiro passaram, assim como os 18 testes, com 90% de cobertura de linhas. O defeito mais sério está no banco: o esquema é criado por `CREATE TABLE IF NOT EXISTS` no código, sem migrações, e não há `CHECK` nas colunas de situação nem no número de hóspedes.

## Arquitetura

**Números:** 7 pacotes, 16 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Javalin só em `web` e na raiz; JDBC e pool só em `persistence`. A maior classe é `ReservationRepository`, com 157 linhas.
**Nota:** bom

- **Borda HTTP fina.** Cada rota tem uma linha: lê, chama o serviço e escreve a resposta (`web/Routes.java:40-53`). A validação de formato fica separada em `web/RequestParser.java`. Os erros viram JSON num lugar só (`web/Routes.java:55-60`), com o status HTTP vindo do código do erro (`domain/PousadaException.java:5-6`).
- **Regras sem Javalin e sem SQL.** As transições e o reembolso ficam em `domain/Reservation.java:26-47`, o preço em `domain/Pricing.java:18-33`. O serviço só orquestra (`service/ReservationService.java:66-85`). Ele conhece `Database`, mas apenas para demarcar a transação.
- **Dados isolados.** Todo o SQL fica em `persistence/*Repository.java` e em `Schema.java`. A dependência `persistence -> notification` existe porque `MessageRepository` implementa a interface `MessageGateway` do pacote de notificação (`persistence/MessageRepository.java:13`). É uma inversão correta, não um vazamento.
- **Montagem num lugar só:** `App.java:22-42`. Não há `new` de repositório dentro dos serviços.
- **Único atalho:** a rota `/messages` chama o repositório e abre a transação ela mesma (`web/Routes.java:49`, com `Database` e `MessageRepository` injetados em `Routes.java:29-30`). É localizado e não leva regra nem SQL para a borda.

## Design de baixo nível

**Números:** 3 interessados pedidos (hóspede, governança, financeiro), os 3 como ouvintes do `EventBus` e nenhum fora da estrutura. Os 2 eventos (confirmação e cancelamento) passam pela estrutura, e nenhum aviso é montado no serviço.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern (Observer): acerto.**
- **O sujeito não conhece os ouvintes.** `ReservationService` só publica eventos (`service/ReservationService.java:70` na confirmação, `:81` no cancelamento). Ele não sabe quem é avisado nem monta texto de e-mail.
- **Lista que vem de fora.** O `EventBus` (`events/EventBus.java:6-17`) guarda a lista de `EventSubscriber` (`events/EventSubscriber.java:4-6`). O barramento chega ao serviço pelo construtor, e os ouvintes são registrados em `App.java:32-34`.
- **Um ouvinte por interessado, inclusive o hóspede.** `notification/GuestNotifier.java:17-25`, `notification/GovernanceNotifier.java:20-35` e `notification/FinanceNotifier.java:20-31`.
- **Cada regra no seu ouvinte.** "Sem reembolso, o financeiro não é avisado" fica no ouvinte do financeiro (`FinanceNotifier.java:26`).
- **Restaurante e transfer** entram com uma classe nova e uma linha em `App.java`.
- **Atomicidade.** Os avisos são gravados na mesma transação da mudança de estado (`ReservationService.java:67-72`): se um ouvinte falha, a confirmação volta atrás.

Dois pontos afastam o código do ideal, sem mudar a classe:
- **A regra do cancelamento ficou no serviço.** O serviço decide que só o cancelamento de uma reserva confirmada vira evento (`ReservationService.java:80`). Hoje está certo, mas um interessado futuro em cancelamentos de reservas pendentes exigiria mexer no serviço.
- **Filtro por `instanceof`.** Cada ouvinte recebe todos os eventos e filtra pelo tipo (`FinanceNotifier.java:21,26`). Com muitos tipos de evento, isso vira uma cadeia de `if`s em cada ouvinte.

**Exagero: contenção.**
- `DomainEvent` é uma interface-marcador com dois records.
- `MessageGateway` tem uma implementação só, mas separa a notificação da persistência, e o texto diz que os e-mails "por enquanto" não saem de verdade.
- `CreateReservation` é um record de comando.

Nada disso é cerimônia vazia: não há fábrica, builder nem camada que só repassa.

**O resto do código: bom.**
- **Tipos certos.** Dinheiro em `BigDecimal` com `HALF_EVEN` a cada passo (`domain/Money.java:12-14`, `domain/Pricing.java:22-31`), datas em `LocalDate`, e `enum` para a situação, o plano e o tipo de quarto.
- **Estado.** Os objetos são imutáveis (records), e a única coleção mutável é a `CopyOnWriteArrayList` do barramento.
- **Erros.** São tipados (`PousadaException` com código) e não há exceção engolida. O `catch (Exception)` da borda registra o erro em log antes de responder 500 (`web/Routes.java:57-60`).

Defeitos pequenos:
- **Construtor de 16 argumentos repetido em três lugares:** `domain/Reservation.java:61-62`, `persistence/ReservationRepository.java:53-55` e `service/ReservationService.java:56-58`. Um `withId` no record evitaria a cópia no repositório.
- **`orElseThrow()` sem mensagem** em `service/ReservationService.java:117`.

## Testes

**Números:** 18 testes rodaram, 0 falhas, 0 erros, 0 pulados; 18 métodos escritos (12 em `ApiTest`, 6 em `PricingTest`). Cobertura de 90,2% das linhas (472/523) e 81,7% dos ramos (103/126). `service`, `events` e `notification` têm 100% das linhas (`measures/tests.md`).
**Nota:** bom

Há uma mistura de testes de unidade (`PricingTest`) com testes pela API contra o PostgreSQL real, num schema temporário (`ApiTest.java:41-57`). Eles conferem as regras com valores:
- **Preço:** os limites da alta temporada (`PricingTest.java:17-25`), o exemplo de R$ 599,76 (`:28-33`), noites de temporadas diferentes (`:36-40`) e o "meio para o par" de 37,485 → 37,48 (`:43-48`).
- **Comprovante e formatação** (`:51-72`).
- **Recusas com status e código** (`ApiTest.java:120-132`), sobreposição e quarto liberado após cancelamento (`:134-143`), concorrência (`:145-159`), ciclo de vida e segunda via (`:241-261`) e os dois CSVs comparados por inteiro (`:271-304`).

A parte nova do cenário também está testada:
- O e-mail do hóspede é comparado por inteiro. Também são conferidos o destinatário e as datas da governança, o valor do financeiro e a ausência de novos e-mails numa confirmação recusada (`ApiTest.java:161-195`).
- Cancelamento de reserva pendente sem avisos (`:203-210`), reembolso avisado (`:212-226`) e reserva não reembolsável sem aviso ao financeiro (`:228-239`).

Pergunta da mutação:
- Se o ouvinte do financeiro avisasse com reembolso zero, ou se o serviço publicasse o cancelamento de reserva pendente, um teste falharia (`:209`, `:238`).
- Se o número de hóspedes sumisse do e-mail da governança, nenhum teste falharia. A asserção `contains("2")` (`ApiTest.java:187`) é satisfeita pelo "2027" das datas.
- O texto da governança no cancelamento só confere as datas.

`Main` não é coberto (0 de 8 ramos na raiz).

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(12,2)` nas 6 colunas e as datas em `date`. A chave estrangeira `reservations.room_number` tem índice, e há um único `CHECK` (`check_out > check_in`). Sem `CHECK` em `status`, `rate_plan`, `type` e `channel`. Esquema criado no código, com 0 arquivos de migração (`measures/database.md`).
**Nota:** com problemas

- **Esquema sem migrações.** É criado por `CREATE TABLE IF NOT EXISTS` em `persistence/Schema.java:18-53`, e esse comando não sabe acrescentar uma coluna numa tabela que já existe. A próxima mudança de esquema vai exigir `ALTER` à mão ou uma ferramenta de migração introduzida depois.
- **O que funciona.** O esquema é idempotente, protegido contra duas instâncias subindo juntas por `pg_advisory_xact_lock` (`Schema.java:17`), e os quartos são semeados com `ON CONFLICT DO NOTHING` (`:55`).
- **Restrições que faltam.** `status`, `rate_plan` e `type` são `VARCHAR` sem `CHECK` (`Schema.java:21,35,40`), e nada no banco impede `guests < 1` (`:34`). A validação existe só na aplicação (`web/RequestParser.java:49`).
- **Duas reservas na mesma noite.** O banco não tem `EXCLUDE`. O que impede a duplicação é uma trava de linha no quarto (`SELECT ... FOR UPDATE`, `persistence/RoomRepository.java:37-38`, usado em `service/ReservationService.java:45`) antes da checagem de sobreposição (`ReservationRepository.java:80-93`). A trava vale entre processos, e o roteiro confirmou 10 de 10 rodadas simultâneas corretas.
- **O código está correto:**
  - Todo SQL usa parâmetros. As concatenações que o script listou (`ReservationRepository.java:63,97-107`) só juntam constantes.
  - O pool é o HikariCP (`Database.java:20-30`).
  - Os recursos são fechados com try-with-resources em todos os repositórios.
  - A mudança de estado e as mensagens dos avisos são gravadas na mesma transação.
  - Os relatórios filtram no SQL, não em Java.

## Comportamento

**Números:** 66 de 66 conferências passaram, 0 falharam (`measures/behavior.md`):
- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- cenário 03: 15/15;
- duas reservas ao mesmo tempo: 2/2.

**Nota:** bom

Para o dono da pousada, tudo o que o texto descreve funciona pela API como ele pediu:
- os preços com temporada e arredondamento;
- as recusas com os códigos certos;
- o ciclo da reserva, as devoluções e os dois relatórios em CSV;
- as reservas e as mensagens continuam lá depois de reiniciar o sistema;
- na confirmação, a governança recebe "Reserva confirmada - quarto 201", com as datas e os hóspedes, e o financeiro recebe "Pagamento recebido - reserva 1", com R$ 779,80;
- no cancelamento de uma reserva confirmada, a governança e o financeiro (este só se houver reembolso) são avisados;
- o cancelamento de uma reserva pendente não avisa ninguém;
- duas reservas simultâneas para o mesmo quarto nunca são criadas juntas.

## Dúvidas

- A decisão de só publicar o cancelamento quando a reserva estava confirmada fica no serviço (`service/ReservationService.java:80`), e não num ouvinte. Hoje isso atende ao texto, e não mudei a classe por causa disso. Um interessado futuro em cancelamentos de reservas pendentes, porém, obrigaria a mexer no serviço.
