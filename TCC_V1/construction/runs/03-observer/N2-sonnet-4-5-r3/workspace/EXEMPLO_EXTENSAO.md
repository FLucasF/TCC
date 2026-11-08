# Exemplo: Adicionando Notificação para o Restaurante

Este documento mostra como adicionar um novo departamento para receber notificações quando uma reserva é confirmada, sem modificar o código existente de confirmação de reservas.

## Passo 1: Criar o Listener

Crie um arquivo `src/main/java/com/pousadamarealta/notification/RestaurantNotificationListener.java`:

```java
package com.pousadamarealta.notification;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.util.Formatter;

public final class RestaurantNotificationListener implements ReservationEventListener {
    private final MessageService messageService;

    public RestaurantNotificationListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, RoomType roomType) {
        String body = String.format(
            "Entrada: %s | %d %s para café da manhã",
            Formatter.formatDate(reservation.checkIn()),
            reservation.guests(),
            reservation.guests() == 1 ? "pessoa" : "pessoas"
        );
        
        messageService.send(
            MessageChannel.EMAIL,
            "restaurante@pousadamarealta.com.br",
            String.format("Nova reserva - %d café(s) da manhã", reservation.guests()),
            body
        );
    }

    @Override
    public void onReservationCancelled(Reservation reservation, RoomType roomType, boolean wasPreviouslyConfirmed) {
        // Restaurante não precisa saber de cancelamentos
    }
}
```

## Passo 2: Registrar o Listener

Edite `src/main/java/com/pousadamarealta/Main.java` e adicione o novo listener à lista:

```java
List<ReservationEventListener> listeners = List.of(
    new GuestNotificationListener(messageService),
    new HousekeepingNotificationListener(messageService),
    new FinanceNotificationListener(messageService),
    new RestaurantNotificationListener(messageService)  // <-- NOVA LINHA
);
```

Pronto! Agora quando uma reserva for confirmada, o restaurante será notificado automaticamente.

## O que NÃO precisou mudar

- ✅ Nenhuma linha em `ReservationService.confirmReservation()`
- ✅ Nenhuma linha em `ApiServer`
- ✅ Nenhuma linha nos listeners existentes
- ✅ Nenhuma linha nos testes

## Removendo um Listener

Para deixar de notificar um departamento (ex: quando o contrato com o transfer acaba), basta remover a linha correspondente da lista de listeners em `Main.java`. Nada mais precisa ser tocado.

## Por que isso funciona?

O padrão **Observer** separa:
- **O que acontece** (reserva confirmada) 
- **Quem precisa saber** (lista de listeners)

O `ReservationService` só sabe que há uma lista de interessados e notifica todos eles. Não conhece os detalhes de cada departamento nem precisa ser modificado quando a lista muda.

Isso segue o princípio do CLAUDE.md: "para acrescentar o próximo caso parecido, quantos lugares do código teriam de mudar?" 

Resposta: apenas um (a lista de listeners).
