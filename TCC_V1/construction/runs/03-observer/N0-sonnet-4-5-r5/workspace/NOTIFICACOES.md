# Sistema de Notificações - Pousada Maré Alta

Este documento descreve o sistema de notificações implementado usando arquitetura orientada a eventos.

## Arquitetura

O sistema utiliza o **padrão Observer/Pub-Sub** para desacoplar a lógica de negócio das notificações:

```
ReservationService (publica evento)
         ↓
   EventPublisher
         ↓
    ┌────┴────┬─────────┐
    ↓         ↓         ↓
GuestListener HousekeepingListener FinanceListener
    ↓         ↓         ↓
NotificationService (persiste mensagens)
```

### Vantagens

1. **Desacoplamento**: A lógica de reservas não precisa conhecer os departamentos
2. **Extensibilidade**: Novos departamentos podem ser adicionados sem modificar código existente
3. **Rastreabilidade**: Todas as notificações são armazenadas no banco de dados
4. **Facilidade de teste**: Cada listener pode ser testado independentemente

## Eventos

### ReservationConfirmedEvent

Disparado quando uma reserva é confirmada (transição de PENDING → CONFIRMED).

**Listeners que respondem:**
- `GuestNotificationListener` - Envia comprovante ao hóspede
- `HousekeepingNotificationListener` - Notifica governança
- `FinanceNotificationListener` - Notifica financeiro

**Exemplo de e-mail ao hóspede:**
```
Para: joao@example.com
Assunto: Reserva confirmada - Pousada Maré Alta

Olá, João Silva!

Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

Comprovante de reserva nº 1
Hóspede: João Silva (CPF 123.456.789-01)
Quarto: 101 (STANDARD)
Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
Diárias: R$ 499,80
Total: R$ 499,80

A entrada é a partir das 14h, e a saída, até as 12h.
Até breve!
```

**Exemplo de e-mail à governança:**
```
Para: governanca@pousadamarealta.com.br
Assunto: Reserva confirmada - quarto 101

Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 hóspedes
```

**Exemplo de e-mail ao financeiro:**
```
Para: financeiro@pousadamarealta.com.br
Assunto: Pagamento recebido - reserva 1

R$ 499,80
```

### ReservationCancelledEvent

Disparado quando uma reserva é cancelada.

**Listeners que respondem:**
- `HousekeepingCancellationListener` - Notifica governança (só se estava CONFIRMED)
- `FinanceCancellationListener` - Notifica financeiro (só se houver reembolso)

**Exemplo de e-mail à governança:**
```
Para: governanca@pousadamarealta.com.br
Assunto: Reserva cancelada - quarto 101

Entrada: 10/03/2027 | Saída: 12/03/2027
```

**Exemplo de e-mail ao financeiro (plano STANDARD):**
```
Para: financeiro@pousadamarealta.com.br
Assunto: Reembolso - reserva 1

R$ 499,80
```

**Nota:** Se o plano for NON_REFUNDABLE, o financeiro **não** é notificado, pois não há reembolso.

## Adicionando Novo Departamento

Para adicionar um novo departamento (exemplo: restaurante), siga estes passos:

### 1. Criar o Listener

```java
package com.pousadamarealta.event.listeners;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.event.EventListener;
import com.pousadamarealta.event.ReservationConfirmedEvent;
import com.pousadamarealta.service.NotificationService;

import java.time.format.DateTimeFormatter;

public class RestaurantNotificationListener implements EventListener<ReservationConfirmedEvent> {
    
    private final NotificationService notificationService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public RestaurantNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation reservation = event.getReservation();
        
        String subject = "Nova reserva - café da manhã";
        String body = String.format(
            "Entrada: %s | %d %s", 
            reservation.getCheckIn().format(dateFormatter),
            reservation.getGuests(),
            reservation.getGuests() == 1 ? "pessoa" : "pessoas"
        );

        notificationService.sendEmail("restaurante@pousadamarealta.com.br", subject, body);
    }
}
```

### 2. Registrar no Main.java

```java
eventPublisher.subscribe(ReservationConfirmedEvent.class, 
    new RestaurantNotificationListener(notificationService));
```

Pronto! Agora o restaurante será notificado automaticamente de todas as novas reservas.

## Testando as Notificações

1. Inicie o sistema
2. Crie e confirme uma reserva
3. Consulte `GET /messages` para ver todas as notificações enviadas

## Futuros Aprimoramentos

- **Transfer**: Notificar serviço de transfer quando houver reservas
- **Marketing**: Enviar pesquisa de satisfação após check-out
- **SMS**: Adicionar canal SMS além de e-mail
- **WhatsApp**: Integrar com WhatsApp Business API
- **Notificações em tempo real**: WebSocket para notificações instantâneas no frontend
