# Guia de Extensibilidade - Sistema de Notificações

O sistema foi desenhado para ser facilmente extensível. Quando novos departamentos ou serviços precisarem ser notificados sobre eventos de reservas, você só precisa:

## 1. Criar um novo Listener

Crie uma classe que implemente `ReservationEventListener`:

```java
package com.pousada.event.listener;

import com.pousada.database.MessageRepository;
import com.pousada.event.ReservationCancelledEvent;
import com.pousada.event.ReservationConfirmedEvent;
import com.pousada.event.ReservationEventListener;
import com.pousada.model.Reservation;
import com.pousada.service.FormattingService;

public class RestaurantNotificationListener implements ReservationEventListener {
  private static final String RESTAURANT_EMAIL = "restaurante@pousadamarealta.com.br";

  @Override
  public void onReservationConfirmed(ReservationConfirmedEvent event) {
    Reservation reservation = event.getReservation();
    String subject = "Novo hóspede chegando - " + FormattingService.formatDate(reservation.checkIn);
    String body = 
        "Data de entrada: " + FormattingService.formatDate(reservation.checkIn) + "\n" +
        "Hóspedes: " + reservation.guests + "\n" +
        "Quarto: " + reservation.roomNumber;

    MessageRepository.save("EMAIL", RESTAURANT_EMAIL, subject, body);
  }

  @Override
  public void onReservationCancelled(ReservationCancelledEvent event) {
    // Implementar se necessário
  }
}
```

## 2. Registrar o Listener

Em `Application.java`, basta adicionar uma linha no método `main`:

```java
EventDispatcher.subscribe(new RestaurantNotificationListener());
EventDispatcher.subscribe(new AirportTransferNotificationListener());
EventDispatcher.subscribe(new ConciergeNotificationListener());
```

Isso é tudo! O novo listener será automaticamente notificado quando:
- Uma reserva for confirmada: `onReservationConfirmed()` será chamado
- Uma reserva for cancelada: `onReservationCancelled()` será chamado

## 3. Adicionar Informações no Evento (se necessário)

Se precisar de dados adicionais no evento, você pode estender as classes de evento. Por exemplo, adicionar informações do quarto:

```java
public class ReservationConfirmedEvent extends ReservationEvent {
  private final BigDecimal amountPaid;
  private final Room room;  // Novo campo

  public ReservationConfirmedEvent(Reservation reservation, BigDecimal amountPaid, Room room) {
    super(reservation);
    this.amountPaid = amountPaid;
    this.room = room;
  }

  public Room getRoom() {
    return room;
  }
}
```

Depois atualize onde o evento é criado (em `ReservationService.java`):

```java
Optional<Room> room = RoomRepository.findByNumber(updated.roomNumber);
EventDispatcher.dispatchConfirmed(new ReservationConfirmedEvent(updated, res.total, room.get()));
```

## Exemplos de Novos Listeners

### Transfer do Aeroporto

```java
public class AirportTransferNotificationListener implements ReservationEventListener {
  private static final String TRANSFER_EMAIL = "transfer@pousadamarealta.com.br";

  @Override
  public void onReservationConfirmed(ReservationConfirmedEvent event) {
    Reservation reservation = event.getReservation();
    String subject = "Passageiro chegando - " + FormattingService.formatDate(reservation.checkIn);
    String body = 
        "Nome: " + reservation.guestName + "\n" +
        "CPF: " + reservation.guestCpf + "\n" +
        "Chegada: " + FormattingService.formatDate(reservation.checkIn);

    MessageRepository.save("EMAIL", TRANSFER_EMAIL, subject, body);
  }

  @Override
  public void onReservationCancelled(ReservationCancelledEvent event) {
    // Avisa se era confirmada
    if (event.wasConfirmed()) {
      Reservation reservation = event.getReservation();
      String subject = "Transferência cancelada - " + reservation.guestName;
      String body = "Hóspede " + reservation.guestName + " cancelou reserva";

      MessageRepository.save("EMAIL", "transfer@pousadamarealta.com.br", subject, body);
    }
  }
}
```

### Sistema de Limpeza Automática

```java
public class CleaningScheduleListener implements ReservationEventListener {
  private static final String CLEANING_API = "https://sistema-limpeza.local/api";

  @Override
  public void onReservationConfirmed(ReservationConfirmedEvent event) {
    Reservation reservation = event.getReservation();
    // Chamar API de agendamento de limpeza
    scheduleCheckoutCleaning(reservation.roomNumber, reservation.checkOut);
  }

  @Override
  public void onReservationCancelled(ReservationCancelledEvent event) {
    Reservation reservation = event.getReservation();
    // Cancelar agendamento de limpeza
    cancelCheckoutCleaning(reservation.roomNumber, reservation.checkOut);
  }

  private void scheduleCheckoutCleaning(int roomNumber, LocalDate checkoutDate) {
    // Implementar chamada à API
  }

  private void cancelCheckoutCleaning(int roomNumber, LocalDate checkoutDate) {
    // Implementar chamada à API
  }
}
```

### Concierge (Reservas de Restaurante/Passeios)

```java
public class ConciergeNotificationListener implements ReservationEventListener {
  private static final String CONCIERGE_EMAIL = "concierge@pousadamarealta.com.br";

  @Override
  public void onReservationConfirmed(ReservationConfirmedEvent event) {
    Reservation reservation = event.getReservation();
    String subject = "Novo hóspede para concierge - " + reservation.guestName;
    String body = 
        "Nome: " + reservation.guestName + "\n" +
        "Período: " + FormattingService.formatDate(reservation.checkIn) + 
        " a " + FormattingService.formatDate(reservation.checkOut) + "\n" +
        "Hóspedes: " + reservation.guests + "\n" +
        "Quarto: " + reservation.roomNumber;

    MessageRepository.save("EMAIL", CONCIERGE_EMAIL, subject, body);
  }

  @Override
  public void onReservationCancelled(ReservationCancelledEvent event) {
    // Notificar cancelamento
  }
}
```

## Padrão Observer Flexível

Este design segue o padrão Observer, oferecendo:

- **Desacoplamento**: Cada listener é independente
- **Extensibilidade**: Adicione listeners sem modificar código existente
- **Testabilidade**: Cada listener pode ser testado isoladamente
- **Manutenibilidade**: Lógica de negócio separada por domínio

## Próximos Passos

Quando novos serviços forem contratados:

1. Crie o listener no pacote `com.pousada.event.listener`
2. Implemente os métodos `onReservationConfirmed` e `onReservationCancelled`
3. Registre em `Application.java`
4. Nenhuma alteração em outras partes do código é necessária
