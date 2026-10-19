# Extensibilidade do Sistema de Notificações

O sistema foi projetado para facilitar a adição de novos destinatários de notificações sem modificar o código existente.

## Como funciona

Quando uma reserva é confirmada ou cancelada, o `ReservationService` publica um evento (`ReservationConfirmedEvent` ou `ReservationCancelledEvent`) através do `EventBus`. Listeners registrados no `EventBus` recebem esses eventos e decidem o que fazer.

## Adicionando um novo destinatário

### Exemplo: Notificar o restaurante

O restaurante pediu para ser notificado quando uma reserva for confirmada, para planejar o café da manhã.

**Passo 1:** Criar o listener

```java
package com.pousadamarealta.domain.listener;

import com.pousadamarealta.domain.event.Event;
import com.pousadamarealta.domain.event.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.service.MessageService;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class RestaurantNotificationListener implements Consumer<Event> {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String RESTAURANT_EMAIL = "restaurante@pousadamarealta.com.br";
    
    private final MessageService messageService;

    public RestaurantNotificationListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void accept(Event event) {
        if (event instanceof ReservationConfirmedEvent confirmedEvent) {
            sendBreakfastNotification(confirmedEvent.reservation());
        }
    }

    private void sendBreakfastNotification(Reservation reservation) {
        String body = String.format(
            "Data: %s%nNúmero de hóspedes: %d%nQuarto: %s",
            reservation.getCheckIn().format(DATE_FORMATTER),
            reservation.getGuests(),
            reservation.getRoomNumber()
        );
        
        messageService.sendEmail(
            RESTAURANT_EMAIL,
            String.format("Café da manhã - %s", reservation.getCheckIn().format(DATE_FORMATTER)),
            body
        );
    }
}
```

**Passo 2:** Registrar o listener em `Application.java`

```java
// Em Application.java, após criar o eventBus:

eventBus.subscribe(new GuestNotificationListener(messageService));
eventBus.subscribe(new HousekeepingNotificationListener(messageService));
eventBus.subscribe(new FinanceNotificationListener(messageService));
eventBus.subscribe(new RestaurantNotificationListener(messageService));  // <- Nova linha
```

Pronto! Agora o restaurante recebe um e-mail toda vez que uma reserva for confirmada.

## Vantagens desta abordagem

1. **Separação de responsabilidades**: O `ReservationService` não precisa saber quem deve ser notificado
2. **Fácil extensão**: Adicionar um novo destinatário é criar um arquivo e adicionar uma linha
3. **Sem modificação**: O código existente não precisa ser modificado
4. **Testável**: Cada listener pode ser testado isoladamente

## Outros exemplos de extensão

### Notificar transfer apenas no check-in

```java
public class TransferNotificationListener implements Consumer<Event> {
    @Override
    public void accept(Event event) {
        if (event instanceof ReservationCheckedInEvent checkedInEvent) {
            // Notificar transfer
        }
    }
}
```

### Múltiplos eventos no mesmo listener

```java
public class AuditListener implements Consumer<Event> {
    @Override
    public void accept(Event event) {
        if (event instanceof ReservationConfirmedEvent) {
            logConfirmation();
        } else if (event instanceof ReservationCancelledEvent) {
            logCancellation();
        }
    }
}
```

## Adicionando novos tipos de eventos

Se você precisar notificar sobre check-in ou check-out, basta:

1. Criar o evento (ex: `ReservationCheckedInEvent.java`)
2. Publicá-lo no `ReservationService` no método apropriado
3. Criar listeners que respondem a esse evento
