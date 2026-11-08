# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin em camadas: `hotel.web.HttpApi` recebe a requisição e chama `hotel.reservations.ReservationService`, que guarda os dados no PostgreSQL por meio de `ReservationStore`, `Rooms` e `SentMessages` (JDBC puro, esquema criado pelo Flyway). As peças são montadas em `hotel.Hotel`. O agente criou o pacote `hotel.notifications` com uma interface de ouvinte (`ReservationEventListener`), três ouvintes (hóspede, governança e financeiro) e um `ReservationNotificationManager` que guarda a lista e avisa cada ouvinte. Os ouvintes são registrados em `Hotel.java:30-33`, e o serviço passou a só anunciar a confirmação e o cancelamento (`ReservationService.java:64`, `:85-88`). É um Observer bem feito: o e-mail do hóspede ficou igual ao da base, as 66 conferências do roteiro passaram e os 30 testes também. O defeito mais sério é de arquitetura: `ReservationService` depende do `ReservationNotificationManager`, uma classe concreta do pacote novo, e os ouvintes dependem de `Reservation`. Isso cria um ciclo novo entre `hotel.reservations` e `hotel.notifications`.

## Arquitetura

**Números:** 8 pacotes (a base tinha 7) e 22 dependências entre eles (a base tinha 17). São 3 ciclos: `documents↔reservations` (já existia na base), `reservations↔notifications` (novo) e `documents→reservations→notifications` (novo, porque os dois ciclos se juntam). `hotel.notifications` tem 5 classes e 139 linhas e não usa Javalin nem JDBC. `ReservationService` caiu de 138 para 131 linhas.
**Nota:** com problemas

O que está bom: a montagem continua num lugar só. Os ouvintes são criados e registrados em `Hotel.java:28-36` e entregues ao serviço pelo construtor (`ReservationService.java:17-24`). O serviço não sabe mais quem é avisado. O pacote novo não toca em HTTP nem em SQL; ele grava as mensagens pelo `EmailSender`, que já existia. A borda HTTP e o acesso ao banco não mudaram.

O problema: o agente pôs a interface do ouvinte e o gerenciador dentro de `hotel.notifications` (`ReservationEventListener.java:6`, `ReservationNotificationManager.java:8`). O serviço passou a importar a classe concreta desse pacote (`ReservationService.java:4`, `:15`), e os ouvintes importam `hotel.reservations.Reservation` (`GovernanceListener.java:5`, `FinanceListener.java:5`, `ReservationEventListener.java:3`). Com isso, as regras de reserva dependem de quem é notificado, e quem é notificado depende das regras: é o ciclo `reservations↔notifications` que `measures/dependencies.md` acusa. Ele some se a interface de ouvinte (e a lista) ficar em `hotel.reservations` e só as implementações ficarem em `notifications`. É um ciclo novo entre dois pacotes vizinhos, localizado e fácil de corrigir. O ciclo `documents↔reservations` veio da base.

## Design de baixo nível

**Números:** há 3 interessados (hóspede, governança e financeiro) e 2 eventos (confirmação e cancelamento). Todos os 3 interessados estão na estrutura de ouvintes, inclusive o hóspede, que já existia na base, e nenhum ficou fora. Nenhum e-mail é montado no serviço.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

O pattern pedido:
- **A interface:** `ReservationEventListener` (`ReservationEventListener.java:6-11`) tem os dois eventos, e cada interessado tem uma implementação: `GuestConfirmationListener`, `GovernanceListener` e `FinanceListener`.
- **O serviço não conhece quem é avisado:** ele chama `notifyConfirmed` (`ReservationService.java:64`) e `notifyCancelled` (`ReservationService.java:85-88`), e o gerenciador percorre a lista (`ReservationNotificationManager.java:16-26`).
- **O hóspede também virou ouvinte:** o texto do e-mail saiu do serviço para `GuestConfirmationListener.java:18-36`, sem mudanças em relação à base.
- **Cada regra no ouvinte certo:** "só avisar o financeiro se houver algo a devolver" fica no ouvinte do financeiro (`FinanceListener.java:26`). "Cancelar uma reserva pendente não avisa ninguém" é uma condição sobre o evento e fica no serviço (`ReservationService.java:78`, `:85`), o que é sensato.
- **O registro:** fica na montagem (`Hotel.java:31-33`). Para incluir o restaurante ou o transfer, basta uma classe nova e uma linha ali.

O que afasta um pouco do ideal:
- `GuestConfirmationListener.onReservationCancelled` é um método vazio (`GuestConfirmationListener.java:27-29`), porque a interface não tem métodos `default`. Com mais interessados, cada um que só se importa com um evento vai repetir esse método vazio.
- O serviço depende da classe concreta do gerenciador, e não de uma abstração (é a origem do ciclo de arquitetura).

O exagero: não há. O `ReservationNotificationManager` é o próprio sujeito do Observer (a lista e o laço), não uma camada que só repassa. Não há fábricas nem builders.

O resto do código está bom, e o que o agente acrescentou é curto e legível. Pequenos defeitos:
- um `import java.math.BigDecimal` sem uso (`FinanceListener.java:7`);
- os endereços da governança e do financeiro repetidos como texto solto dentro de cada método (`GovernanceListener.java:22`, `:30`; `FinanceListener.java:21`, `:29`);
- o parâmetro `Room` que dois dos três ouvintes não usam.

Também não há proteção entre os ouvintes: uma exceção num deles interrompe os seguintes e faz a chamada responder erro com a reserva já confirmada (`ReservationNotificationManager.java:17-19`). A base tinha o mesmo risco com o seu único e-mail.

## Testes

**Números:**
- **Final:** 30 testes rodaram (0 falhas, 0 erros, 0 pulados), com 30 métodos escritos. Cobertura de linhas de 94,4% (525 de 556) e de ramos de 88,1% (111 de 126). `hotel.notifications` está 100% coberto (49 de 49 linhas, 6 de 6 ramos).
- **Base:** 26 testes, 93,8% das linhas e 87,1% dos ramos.

**Nota:** bom

O agente escreveu 4 testes novos em `ReservationServiceTest`, contra o banco e com valores:
- **Confirmação** (`ReservationServiceTest.java:120-151`): os destinatários, os assuntos ("Reserva confirmada - quarto 101", "Pagamento recebido - reserva N"), as datas "10/03/2027" e "11/03/2027", "Hóspedes: 1" e "R$ 249,90".
- **Cancelamento de uma reserva confirmada** (`:170-200`): o assunto "Reserva cancelada - quarto 102", as datas, "Reembolso - reserva N" e o valor.
- **Cancelamento de uma reserva pendente** (`:203-219`): nenhuma mensagem.
- **Cancelamento de uma NON_REFUNDABLE** (`:222-245`): a governança é avisada e o financeiro não (4 mensagens, e não 5).

O agente também ajustou `HttpApiTest.java:81-84` para conferir as 3 mensagens pela API. Os 26 testes da base continuam passando. O agente não apagou nenhum deles. A única asserção que mudou foi a quantidade de mensagens em `HttpApiTest` (de 1 para 3), e a mudança segue o pedido.

A pergunta da mutação:
- **Trocar `signum() > 0` por `>= 0`** (`FinanceListener.java:26`): o teste NON_REFUNDABLE falha. A regra tem rede.
- **Tirar o `if (wasConfirmed)`** (`ReservationService.java:85`): o teste da reserva pendente falha. A regra tem rede.
- **Trocar `refund()` por `amountPaid()` no e-mail de reembolso** (`FinanceListener.java:28`): nenhum teste falha, porque no plano STANDARD os dois valores são iguais (R$ 249,90 em `:199`).

Duas fragilidades pequenas:
- As asserções dependem da ordem dos e-mails (`sent.get(1)`, `sent.get(2)` em `:140`, `:147`; `HttpApiTest.java:83-84`), embora o texto diga que a ordem não importa.
- A montagem do serviço com os três ouvintes está copiada em quatro testes (`:121-127`, `:171-177`, `:204-210`, `:223-229`).

## Banco

**Números:**
- **Esquema:** 3 tabelas, criadas por 1 migração Flyway (`V1__create_tables.sql`), que está igual à da base.
- **Conferências sem nada a acusar:** nenhuma tabela sem chave primária; todo o dinheiro em `numeric(10,2)`; nenhuma chave estrangeira sem índice; nenhuma data guardada como texto.
- **Restrições:** há um `EXCLUDE USING gist` que impede duas reservas do mesmo quarto na mesma noite.
- **Sem CHECK:** `status`, `rate_plan`, `type` e `channel`.
- **Código:** 0 transações explícitas e 0 `FOR UPDATE`.

**Nota:** bom

O pedido não precisava de mudança no banco, e o agente não mexeu nele. Os avisos novos são gravados na tabela `messages` pelo `EmailSender`/`SentMessages`, que já existiam, e continuam lá depois de reiniciar (conferência do roteiro). As colunas de situação sem CHECK e a falta de transação entre a atualização da reserva e a gravação das mensagens vêm da base. Com três mensagens por confirmação em vez de uma, uma falha no meio deixaria a reserva confirmada com parte dos avisos gravados (`ReservationService.java:62-64`), mas essa estrutura já existia e não é um defeito novo do esquema.

## Comportamento

**Números:** 66 de 66 conferências passaram:
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 03: 15 de 15;
- reservas simultâneas: 2 de 2.

**Nota:** bom

Tudo o que o dono pediu funciona pela API:
- **Na confirmação:** o hóspede continua recebendo o e-mail de antes. A governança recebe "Reserva confirmada - quarto 201", com 10/03/2027, 12/03/2027 e 2 hóspedes. O financeiro recebe "Pagamento recebido - reserva 1", com R$ 779,80.
- **No cancelamento de uma reserva confirmada:** a governança recebe "Reserva cancelada - quarto 201", com as datas, e o financeiro recebe "Reembolso - reserva 1", com R$ 779,80.
- **Plano NON_REFUNDABLE:** só a governança é avisada.
- **Reserva pendente:** o cancelamento não manda nada.

Nada do que já funcionava quebrou (preços, recusas, check-in/check-out, relatórios, dados depois de reiniciar e as reservas simultâneas).

## Dúvidas

O texto diz que os interessados "entram e saem conforme os contratos". O agente resolveu isso com o registro na montagem (`Hotel.java:31-33`), o que exige mudar o código e reiniciar o serviço. Não há `removeListener` nem configuração. Pela referência do pattern, o registro na montagem é o esperado, mas não está claro se o dono esperava poder ligar e desligar um interessado sem mexer no código.
