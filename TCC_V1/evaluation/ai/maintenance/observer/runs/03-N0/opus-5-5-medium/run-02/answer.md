# Cenário 03

## Visão geral

O agente criou uma interface de ouvinte, `ReservationEventListener`, com dois eventos (confirmação e cancelamento) e duas implementações, a da governança e a do financeiro. As duas são registradas na montagem (`Hotel`), e o `ReservationService` percorre a lista nos dois eventos. A governança e o financeiro ficaram bem separados, e o serviço não monta nenhum dos e-mails deles. O problema sério é o hóspede: ele não virou ouvinte. A confirmação continua chamando o `EmailSender` direto, com o texto escrito pelo próprio serviço, e só depois percorre os ouvintes. Por isso o serviço ainda conhece um interessado concreto. As instruções do pattern classificam esse caso como malfeito.

## Como foi implementado, e por que o julgamento

**O projeto.** A aplicação Javalin está organizada em pacotes:
- `hotel.rooms`: quartos e catálogo;
- `hotel.reservations`: `ReservationService`, `Reservation`, `PriceCalculator`, `ReservationStore` em memória;
- `hotel.documents`: comprovante e `TextFormat`;
- `hotel.messages`: `EmailSender`, que só grava em `SentMessages`;
- `hotel.reports`: relatórios;
- `hotel.web`: `HttpApi`, com as rotas.

Uma requisição entra pela rota em `HttpApi`, que chama o `ReservationService`. O serviço valida o estado, altera a `Reservation` e manda os e-mails pelo `EmailSender`. As mensagens enviadas ficam na lista em memória `SentMessages`, que a API expõe em `/messages`. A classe `Hotel` monta as peças e sobe o servidor (`final/src/main/java/hotel/Hotel.java:21-33`).

A mudança acrescentou o pacote `hotel.notifications` com três arquivos:
- a interface `ReservationEventListener`, com `onReservationConfirmed` e `onReservationCancelled` (`final/src/main/java/hotel/notifications/ReservationEventListener.java:7-12`);
- o `HousekeepingListener`;
- o `FinanceListener`.

O serviço ganhou uma lista de ouvintes e o método `addListener` (`final/src/main/java/hotel/reservations/ReservationService.java:21`, `:31-33`). O `Hotel` cria um único `EmailSender` e registra os dois ouvintes (`final/src/main/java/hotel/Hotel.java:25-29`).

Nos testes, o agente ajustou o `ReservationServiceTest` para registrar os ouvintes e conferir 3 mensagens na confirmação. Também testou que cancelar uma reserva pendente não envia nada e que cancelar uma reserva confirmada com tarifa padrão avisa a governança e o financeiro. O `HttpApiTest` passou a conferir os três destinatários na confirmação.

**A parte do pattern.**
- Na confirmação, o serviço envia o e-mail do hóspede à mão (`ReservationService.java:76-80`, com o corpo em `confirmationEmailBody`, `:145-151`). Logo depois, percorre os ouvintes (`:81-84`).
- No cancelamento, o serviço guarda se a reserva estava confirmada (`:98`), calcula o reembolso, cancela, e só notifica os ouvintes se ela estava confirmada (`:104-109`).
- Cada ouvinte guarda o próprio endereço, assunto e texto. A governança fica em `HousekeepingListener.java:20-34`. O financeiro fica em `FinanceListener.java:20-35`, e a regra "só avisa se há algo a devolver" mora no próprio ouvinte (`:29`).

**O julgamento.** O desenho está perto do Observer: há uma interface de evento, uma implementação por departamento, e o registro é feito de fora, na montagem. O serviço não sabe nada da governança nem do financeiro, e o cancelamento também passa pela lista. Para incluir o restaurante ou o transfer, basta um novo ouvinte e uma linha no `Hotel`.

O que afasta o desenho do acerto é o hóspede. Ele é o interessado que já existia na base e continua com chamada direta no serviço. O serviço ainda guarda o `EmailSender` (`:20`, `:28`) e o texto do e-mail dele (`:145-151`). As instruções do pattern dizem expressamente que o e-mail do hóspede também deve ser um ouvinte, e que é malfeito quando o hóspede tem chamada direta e só a governança e o financeiro viraram ouvintes. É exatamente esse o caso.

No exagero colateral, não há nada fora do pattern. Não foi criada fábrica, builder nem camada de repasse. Há só um detalhe pequeno: o parâmetro `Room` dos eventos não é usado por nenhum ouvinte.

No comportamento, a conferência por leitura bate com o pedido.

## Item a item

**O pattern pedido.** A interface `ReservationEventListener` (`final/src/main/java/hotel/notifications/ReservationEventListener.java:7-12`) tem duas implementações, `HousekeepingListener` e `FinanceListener`. Elas são registradas em `final/src/main/java/hotel/Hotel.java:28-29`, e o serviço as percorre na confirmação (`final/src/main/java/hotel/reservations/ReservationService.java:82-84`) e no cancelamento (`:106-108`). Mas o e-mail do hóspede continua sendo enviado direto pelo serviço (`ReservationService.java:77-80`), com o texto montado ali mesmo (`:145-151`). Assim, a estrutura do Observer convive com o jeito antigo justamente no caso que já existia na base. As instruções do pattern classificam isso como malfeito.
Conclusão: malfeito

**O exagero colateral.** Não há. O agente criou só a interface e os dois ouvintes, que são a parte do pattern. Não há fábrica, builder ou camada que só repassa. O parâmetro `Room`, que nenhum ouvinte usa (`HousekeepingListener.java:21`, `FinanceListener.java:21`), é um detalhe e não estrutura a mais.
Conclusão: contenção

**O comportamento.** A conferência foi feita por leitura do código. Não subi o serviço nem rodei os testes.
- **Confirmação.** O e-mail do hóspede ficou idêntico ao da base: mesmo destinatário, assunto "Reserva confirmada - Pousada Maré Alta" e o mesmo corpo (`ReservationService.java:77-80`, `:145-151`).
- **Governança na confirmação.** Vai para `governanca@pousadamarealta.com.br` com o assunto "Reserva confirmada - quarto " + número. O corpo traz entrada, saída e número de hóspedes no formato dd/MM/yyyy (`HousekeepingListener.java:11`, `:22-25`).
- **Financeiro na confirmação.** Vai para `financeiro@pousadamarealta.com.br` com o assunto "Pagamento recebido - reserva " + id e o valor pago no formato R$ (`FinanceListener.java:12`, `:22-24`). O `amountPaid` já foi preenchido por `confirmPayment` antes do aviso (`ReservationService.java:75`).
- **Cancelamento de reserva confirmada.** A governança recebe "Reserva cancelada - quarto N" com as datas (`HousekeepingListener.java:30-33`). O financeiro recebe "Reembolso - reserva N" com o valor, mas só quando o reembolso é maior que zero (`FinanceListener.java:28-34`). Na tarifa não reembolsável o reembolso é zero (`ReservationService.java:101`), então o financeiro não é avisado.
- **Cancelamento de reserva pendente.** Não avisa ninguém (`ReservationService.java:98`, `:104`).
- **O resto do sistema.** O cálculo do reembolso e as transições de estado não mudaram.

Tudo o que conferi bate com o pedido.
Conclusão: segue

## Dúvidas

Nenhuma.
