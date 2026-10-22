# Cenário 03

## Visão geral

O agente criou uma interface de ouvinte, `ReservationEventListener`, com dois eventos (confirmada e cancelada). A governança e o financeiro viraram dois ouvintes separados, registrados na montagem do sistema (`Hotel`), e o serviço percorre a lista sem saber quem está nela. A estrutura do Observer existe e cobre a confirmação e o cancelamento. O problema mais sério é que o hóspede, que era o único interessado da base, ficou de fora dela: o `ReservationService.confirm` continua montando e mandando o e-mail do hóspede direto e só depois avisa os ouvintes. Há então um interessado tratado à mão ao lado de dois tratados pelo pattern, e é exatamente o caso que as instruções chamam de malfeito. Não há estrutura sobrando, e as regras pedidas conferem pela leitura do código.

## Como foi implementado, e por que o julgamento

**O projeto.** É uma aplicação Javalin pequena, dividida em pacotes:
- `hotel.web`: `HttpApi` com as rotas, entre elas `POST /reservations/{id}/confirmation` e `/cancellation` (`web/HttpApi.java:70-73`), mais os DTOs de requisição e resposta.
- `hotel.reservations`: o domínio. `ReservationService` cuida do ciclo de vida da reserva; `Reservation`, `PriceCalculator` e `ReservationStore` guardam as reservas em memória num mapa.
- `hotel.rooms`: os quartos, em `Rooms`, também em memória.
- `hotel.documents`: o comprovante (`ReservationReceipt`) e a formatação de valores e datas (`TextFormat`).
- `hotel.messages`: o `EmailSender`, que não envia nada de verdade e só registra a mensagem em `SentMessages`, exposta em `GET /messages`.
- `hotel.reports`: os relatórios.

Uma requisição sai da rota em `HttpApi`, chama o método do `ReservationService`, que confere o estado da reserva, muda a `Reservation` e, na confirmação, manda os e-mails. A classe `Hotel` monta tudo no construtor e sobe o servidor (`Hotel.java:21-33`).

Os testes do agente ficam em `ReservationServiceTest`. Ele mudou as contagens para 3 mensagens na confirmação e criou três testes de cancelamento: confirmada standard, pendente e não reembolsável (`src/test/java/hotel/reservations/ReservationServiceTest.java`). Também ajustou o `HttpApiTest` para esperar os três destinatários.

**A parte do pattern.**
- `ReservationEventListener` (`reservations/ReservationEventListener.java:4-9`) tem dois métodos, `onReservationConfirmed` e `onReservationCancelled`.
- `GovernanceListener` (`reservations/GovernanceListener.java:20-36`) monta os dois e-mails da governança.
- `FinanceListener` (`reservations/FinanceListener.java:17-31`) monta os do financeiro e só manda o de reembolso quando o valor é maior que zero (`FinanceListener.java:27`).
- O serviço guarda uma `List<ReservationEventListener>` (`reservations/ReservationService.java:20`), que é preenchida de fora por `addListener` (`ReservationService.java:30-32`). O `Hotel` registra os dois ouvintes (`Hotel.java:28-29`).
- Na confirmação, o serviço avisa todos os ouvintes (`ReservationService.java:80`). No cancelamento, só avisa se a reserva estava confirmada (`ReservationService.java:94`, `100-102`).
- O e-mail do hóspede, porém, continua no próprio serviço: a chamada a `emailSender.send` para `reservation.guest().email()` está em `ReservationService.java:76-79`, e o texto continua em `confirmationEmailBody` (`ReservationService.java:138-144`). O serviço também segue recebendo o `EmailSender` no construtor (`ReservationService.java:19`, `27`) só para isso.

**O raciocínio.** O que aproxima o desenho do acerto:
- a interface tem uma implementação por interessado;
- o serviço não conhece a governança nem o financeiro;
- os ouvintes são registrados na montagem;
- o cancelamento também passa pela lista.

O que o afasta: as instruções pedem que "o e-mail do hóspede também é um ouvinte" e listam como malfeito o caso "quando o hóspede tem chamada direta e só a governança e o financeiro viraram ouvintes". É o que está no código. O interessado que já existia na base não foi para a estrutura, e o serviço ainda sabe que o hóspede precisa ser avisado e como avisá-lo. Por isso a classe é malfeito, e não acerto. Também não é omissão, porque a lista de ouvintes existe e os dois interessados novos passam por ela.

No exagero colateral, não pesou nada: a interface tem duas implementações e é o próprio ponto de extensão, e não foram criadas fábrica, builder nem camada de repasse.

No comportamento, conferi por leitura os destinatários, os assuntos, os corpos, a condição do reembolso e o silêncio no cancelamento de reserva pendente. Tudo confere.

## Item a item

**O pattern pedido.** O agente fez um Observer de verdade para a governança e o financeiro:
- a interface fica em `reservations/ReservationEventListener.java:4-9`;
- os ouvintes ficam em `reservations/GovernanceListener.java:9` e `reservations/FinanceListener.java:8`;
- a lista e o `addListener` ficam em `reservations/ReservationService.java:20` e `30-32`;
- o registro na montagem fica em `Hotel.java:28-29`;
- o aviso na confirmação fica em `ReservationService.java:80`, e o do cancelamento em `ReservationService.java:100-102`.

O hóspede, porém, continua com chamada direta em `ReservationService.java:76-79`, com o corpo montado no próprio serviço (`ReservationService.java:138-144`). Assim, um dos interessados é tratado à mão ao lado da estrutura, que é o caso de malfeito previsto nas instruções.
Conclusão: malfeito

**O exagero colateral.** Não há. As únicas peças novas são a interface de ouvinte e as duas implementações, ou seja, a própria estrutura do pattern. `Hotel` e `ReservationService` ganharam só o registro e a lista, sem fábrica, builder nem camada que só repassa.
Conclusão: contenção

**O comportamento.** A conferência foi por leitura do código, sem subir o serviço nem chamar a API, como pede esta correção.
- **Confirmação.** O e-mail do hóspede ficou igual ao da base, com o mesmo destinatário, o assunto "Reserva confirmada - Pousada Maré Alta" e o mesmo corpo (`ReservationService.java:76-79`, `138-144`).
- **Governança na confirmação.** Vai para `governanca@pousadamarealta.com.br` com o assunto "Reserva confirmada - quarto N", as datas de entrada e saída em `dd/MM/yyyy` e o número de hóspedes (`GovernanceListener.java:22-26`).
- **Financeiro na confirmação.** Vai para `financeiro@pousadamarealta.com.br` com o assunto "Pagamento recebido - reserva N" e o valor pago em `R$ 1.234,56` (`FinanceListener.java:18-20`, `documents/TextFormat.java:17-19`).
- **Cancelamento de reserva confirmada.** A governança recebe "Reserva cancelada - quarto N" com as datas (`GovernanceListener.java:31-35`). O financeiro recebe "Reembolso - reserva N" com o valor, só se o reembolso for maior que zero (`FinanceListener.java:26-30`). Uma reserva não reembolsável tem reembolso zero (`ReservationService.java:97`) e não avisa o financeiro.
- **Cancelamento de reserva pendente.** Não avisa ninguém (`ReservationService.java:94`, `100`).
- **O resto do sistema.** As regras de recusa e de reembolso não mudaram.

Conclusão: segue

## Dúvidas

Nenhuma.
