# Como Adicionar Novos Interessados nas Notificações

O sistema foi projetado para ser facilmente extensível. Aqui está um exemplo de como adicionar notificações para o restaurante e o serviço de transfer.

## Exemplo: Notificação para o Restaurante

### 1. Criar o Listener

Crie um novo arquivo em `src/main/java/com/pousadamarealta/service/listeners/RestaurantNotificationListener.java`:

```java
package com.pousadamarealta.service.listeners;

import com.pousadamarealta.domain.events.EventListener;
import com.pousadamarealta.domain.events.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.util.Formatter;

public class RestaurantNotificationListener implements 
    EventListener<ReservationConfirmedEvent> {
    
    private static final String RESTAURANT_EMAIL = "restaurante@pousadamarealta.com.br";
    private final MessageService messageService;

    public RestaurantNotificationListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation r = event.getReservation();
        
        String subject = "Novo hóspede - café da manhã para " + r.getGuests();
        String body = String.format(
            "Uma reserva foi confirmada:\n\n" +
            "Reserva: %d\n" +
            "Entrada: %s\n" +
            "Saída: %s\n" +
            "Número de pessoas: %d\n\n" +
            "Favor preparar café da manhã.",
            r.getId(),
            Formatter.formatDate(r.getCheckIn()),
            Formatter.formatDate(r.getCheckOut()),
            r.getGuests()
        );
        
        messageService.sendEmail(RESTAURANT_EMAIL, subject, body);
    }
}
```

### 2. Registrar o Listener

No arquivo `Main.java`, adicione o registro do novo listener após os existentes:

```java
// Listeners existentes
eventPublisher.register(ReservationConfirmedEvent.class, 
    new GuestNotificationListener(messageService));
eventPublisher.register(ReservationConfirmedEvent.class, 
    new HousekeepingNotificationListener(messageService));
eventPublisher.register(ReservationConfirmedEvent.class, 
    new FinanceNotificationListener(messageService));

// NOVO: Listener do restaurante
eventPublisher.register(ReservationConfirmedEvent.class, 
    new RestaurantNotificationListener(messageService));
```

Pronto! Agora o restaurante será notificado automaticamente quando uma reserva for confirmada.

## Exemplo: Notificação para o Transfer

### 1. Criar o Listener

```java
package com.pousadamarealta.service.listeners;

import com.pousadamarealta.domain.events.EventListener;
import com.pousadamarealta.domain.events.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.util.Formatter;

public class TransferNotificationListener implements 
    EventListener<ReservationConfirmedEvent> {
    
    private static final String TRANSFER_EMAIL = "transfer@aeroportotransfer.com.br";
    private final MessageService messageService;

    public TransferNotificationListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation r = event.getReservation();
        
        String subject = "Possível cliente - " + Formatter.formatDate(r.getCheckIn());
        String body = String.format(
            "Chegada de hóspedes:\n\n" +
            "Data: %s\n" +
            "Passageiros: %d\n" +
            "Destino: Pousada Maré Alta\n" +
            "Contato: %s (%s)",
            Formatter.formatDate(r.getCheckIn()),
            r.getGuests(),
            r.getGuestName(),
            r.getGuestEmail()
        );
        
        messageService.sendEmail(TRANSFER_EMAIL, subject, body);
    }
}
```

### 2. Registrar o Listener

```java
// Adicione no Main.java junto com os outros
eventPublisher.register(ReservationConfirmedEvent.class, 
    new TransferNotificationListener(messageService));
```

## Removendo Notificações

Se um contrato terminar e você não quiser mais enviar notificações, basta **comentar ou remover** a linha de registro correspondente no `Main.java`. Não é necessário modificar mais nada.

## Eventos Disponíveis

Você pode criar listeners para qualquer evento:

- **ReservationConfirmedEvent**: Quando uma reserva é confirmada (pagamento)
- **ReservationCancelledEvent**: Quando uma reserva é cancelada
  - Use `event.wasConfirmed()` para saber se ela estava confirmada

## Criando Novos Eventos

Se precisar de novos eventos (ex: CheckInEvent, CheckOutEvent), siga este padrão:

1. Crie a classe do evento estendendo `ReservationEvent`
2. Publique o evento no serviço apropriado com `eventPublisher.publish(new SeuEvento(...))`
3. Crie listeners e registre-os no `Main.java`

## Vantagens desta Arquitetura

✅ **Sem modificar código existente**: Adicionar novos interessados não mexe na lógica de reservas  
✅ **Fácil de ativar/desativar**: Basta comentar uma linha  
✅ **Fácil de testar**: Cada listener pode ser testado independentemente  
✅ **Ordem não importa**: Os listeners são executados em sequência, mas são independentes  
✅ **Falha isolada**: Se um listener falhar, não afeta os outros  

