# Sistema de Notificações da Pousada Maré Alta

## Como funciona

O sistema de notificações usa o padrão Observer para permitir que diferentes departamentos sejam notificados quando eventos importantes acontecem com as reservas.

## Departamentos notificados atualmente

### Governança (`governanca@pousadamarealta.com.br`)
- **Reserva confirmada:** Recebe número do quarto, datas de check-in/check-out e quantidade de hóspedes
- **Reserva cancelada:** Recebe apenas se a reserva estava confirmada, com as datas que ficaram livres

### Financeiro (`financeiro@pousadamarealta.com.br`)
- **Pagamento recebido:** Recebe o valor pago quando a reserva é confirmada
- **Reembolso:** Recebe apenas se houver valor a devolver no cancelamento

## Como adicionar um novo departamento

Para adicionar um novo departamento (restaurante, transfer, etc), siga estes passos:

1. Crie uma nova classe que implementa `ReservationEventListener`:

```java
package hotel.reservations;

import hotel.messages.EmailSender;

public final class RestaurantNotifier implements ReservationEventListener {

    private static final String RESTAURANT_EMAIL = "restaurante@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public RestaurantNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, int roomNumber) {
        // Envie o e-mail com as informações necessárias para o café da manhã
        String subject = "Nova reserva - café da manhã";
        String body = "Hóspedes: " + reservation.guests() + "\n"
                + "Check-in: " + TextFormat.date(reservation.checkIn()) + "\n";
        emailSender.send(RESTAURANT_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, int roomNumber, boolean wasConfirmed) {
        // Se quiser notificar sobre cancelamentos, implemente aqui
        // Senão, deixe vazio
    }
}
```

2. Registre o novo listener em `Hotel.java`:

```java
ReservationNotifier notifier = new ReservationNotifier();
notifier.addListener(new HousekeepingNotifier(emailSender));
notifier.addListener(new FinanceNotifier(emailSender));
notifier.addListener(new RestaurantNotifier(emailSender));  // <- Adicione aqui
```

3. Adicione o listener nos testes também (`ReservationServiceTest.java` e `ReportsTest.java`).

## Eventos disponíveis

- `onReservationConfirmed(reservation, roomNumber)`: Chamado quando uma reserva é confirmada (pagamento recebido)
- `onReservationCancelled(reservation, roomNumber, wasConfirmed)`: Chamado quando uma reserva é cancelada
  - `wasConfirmed`: indica se a reserva estava confirmada antes de ser cancelada

## Observações

- Os e-mails são enviados na ordem em que os listeners foram registrados
- Cada listener decide se quer ou não enviar e-mail baseado nas informações da reserva
- As mensagens enviadas ficam guardadas na tabela `messages` e podem ser consultadas em `/messages`
