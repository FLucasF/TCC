# Arquitetura do Sistema

## Visão Geral

Este documento explica as decisões arquiteturais do sistema de reservas da Pousada Maré Alta, com foco especial no sistema de notificações extensível.

## Problema Principal

O proprietário deixou claro que o sistema de notificações vai evoluir constantemente:

> "Vai ter cada vez mais gente querendo ser avisada, e de mais coisas. E quem é avisado vai mudar com o tempo: o restaurante e o transfer entram e saem conforme os contratos que a gente fecha."

### Solução Inadequada (Anti-Pattern)

```java
// ❌ NÃO FAZER - Viola Open/Closed Principle
public void confirmReservation(Long id) {
    Reservation reservation = repository.findById(id);
    reservation.confirm();
    repository.save(reservation);
    
    // Enviar e-mail para hóspede
    emailService.sendToGuest(reservation);
    
    // Enviar e-mail para governança
    emailService.sendToHousekeeping(reservation);
    
    // Enviar e-mail para financeiro
    emailService.sendToFinance(reservation);
    
    // ⚠️ Quando adicionar restaurante, preciso mexer AQUI
    // emailService.sendToRestaurant(reservation);
    
    // ⚠️ Quando adicionar transfer, preciso mexer AQUI DE NOVO
    // emailService.sendToTransfer(reservation);
}
```

**Problemas:**
- Cada novo destinatário requer modificar o método `confirmReservation`
- Viola o princípio Open/Closed (aberto para extensão, fechado para modificação)
- Alto acoplamento entre reservas e notificações
- Dificulta testes unitários

## Solução Implementada: Event-Driven Architecture

### Padrão Observer

```
┌─────────────────────────────────────────────────────────────┐
│                    ReservationService                        │
│  ┌────────────────────────────────────────────────────┐    │
│  │ confirmReservation(id)                             │    │
│  │  1. reservation.confirm()                          │    │
│  │  2. repository.save(reservation)                   │    │
│  │  3. eventPublisher.publish(                        │    │
│  │       new ReservationConfirmedEvent(reservation)   │    │
│  │    )                                               │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────┬───────────────────────────────────┘
                          │ publica evento
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                     EventPublisher                           │
│  Notifica todos os listeners registrados                     │
└─────────────┬──────────────┬──────────────┬─────────────────┘
              │              │              │
              ↓              ↓              ↓
   ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
   │    Guest     │  │ Housekeeping │  │   Finance    │
   │ Notification │  │ Notification │  │ Notification │
   │   Listener   │  │   Listener   │  │   Listener   │
   └──────────────┘  └──────────────┘  └──────────────┘
         │                  │                  │
         ↓                  ↓                  ↓
   Envia e-mail      Envia e-mail      Envia e-mail
   para hóspede      para governança   para financeiro
```

### Código Implementado

```java
// ✅ Código atual - Segue Open/Closed Principle
public class ReservationService {
    private final EventPublisher eventPublisher;
    
    public Reservation confirmReservation(Long id) {
        Reservation reservation = repository.findById(id);
        reservation.confirm();
        repository.save(reservation);
        
        // Publica um evento - não sabe quem vai processar
        eventPublisher.publish(new ReservationConfirmedEvent(reservation));
        
        return reservation;
    }
}
```

### Como Funciona

#### 1. Eventos de Domínio

```java
// Interface marcadora
public interface ReservationEvent {
    Reservation getReservation();
}

// Evento específico
public record ReservationConfirmedEvent(Reservation reservation) 
    implements ReservationEvent {
}

public record ReservationCancelledEvent(
    Reservation reservation, 
    boolean wasConfirmed
) implements ReservationEvent {
}
```

#### 2. Event Publisher (Subject)

```java
public class EventPublisher {
    private final List<EventListener> listeners = new ArrayList<>();

    public void subscribe(EventListener listener) {
        listeners.add(listener);
    }

    public void publish(ReservationEvent event) {
        for (EventListener listener : listeners) {
            listener.handle(event);
        }
    }
}
```

#### 3. Event Listeners (Observers)

```java
public interface EventListener {
    void handle(ReservationEvent event);
}
```

#### 4. Listeners Concretos

Cada interessado implementa sua própria lógica:

```java
public class GuestNotificationListener implements EventListener {
    @Override
    public void handle(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent) {
            sendConfirmationEmail((ReservationConfirmedEvent) event);
        }
    }
    
    private void sendConfirmationEmail(ReservationConfirmedEvent event) {
        // Lógica específica para e-mail do hóspede
    }
}
```

```java
public class HousekeepingNotificationListener implements EventListener {
    @Override
    public void handle(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent) {
            notifyConfirmation(event);
        } else if (event instanceof ReservationCancelledEvent e) {
            if (e.wasConfirmed()) {
                notifyCancellation(e);
            }
        }
    }
}
```

```java
public class FinanceNotificationListener implements EventListener {
    @Override
    public void handle(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent) {
            notifyPayment(event);
        } else if (event instanceof ReservationCancelledEvent e) {
            if (e.wasConfirmed() && hasRefund(e.getReservation())) {
                notifyRefund(e);
            }
        }
    }
}
```

#### 5. Registro de Listeners (App.java)

```java
EventPublisher eventPublisher = new EventPublisher();

// Registra os listeners atuais
eventPublisher.subscribe(new GuestNotificationListener(...));
eventPublisher.subscribe(new HousekeepingNotificationListener(...));
eventPublisher.subscribe(new FinanceNotificationListener(...));

// No futuro, adicionar novos é só uma linha:
// eventPublisher.subscribe(new RestaurantNotificationListener(...));
// eventPublisher.subscribe(new TransferNotificationListener(...));
```

## Vantagens da Arquitetura

### 1. Extensibilidade
Adicionar o restaurante como destinatário:

```java
// Nova classe - NÃO precisa modificar código existente
public class RestaurantNotificationListener implements EventListener {
    @Override
    public void handle(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent) {
            // Avisar sobre café da manhã
        }
    }
}

// No App.java - apenas UMA linha adicional
eventPublisher.subscribe(new RestaurantNotificationListener(...));
```

### 2. Baixo Acoplamento
- `ReservationService` não conhece os listeners
- Listeners não conhecem uns aos outros
- Mudanças em um listener não afetam outros

### 3. Testabilidade
Fácil testar isoladamente:

```java
@Test
void shouldPublishEventOnConfirmation() {
    TestEventListener listener = new TestEventListener();
    eventPublisher.subscribe(listener);
    
    service.confirmReservation(1L);
    
    assertEquals(1, listener.events.size());
    assertTrue(listener.events.get(0) instanceof ReservationConfirmedEvent);
}
```

### 4. Responsabilidade Única
Cada listener tem apenas uma responsabilidade:
- `GuestNotificationListener`: comunicação com hóspede
- `HousekeepingNotificationListener`: comunicação com governança
- `FinanceNotificationListener`: comunicação com financeiro

### 5. Manutenibilidade
Se mudar a regra de quando avisar o financeiro (ex: só valores acima de R$ 100), mexemos apenas em `FinanceNotificationListener`.

## Cenários de Evolução

### Adicionar novo tipo de evento

```java
// 1. Criar o evento
public record CheckInCompletedEvent(Reservation reservation) 
    implements ReservationEvent {
}

// 2. Publicar no lugar apropriado (ReservationService)
public void checkIn(Long id) {
    reservation.checkIn();
    repository.save(reservation);
    eventPublisher.publish(new CheckInCompletedEvent(reservation));
}

// 3. Listeners interessados respondem ao novo evento
public class HousekeepingNotificationListener implements EventListener {
    @Override
    public void handle(ReservationEvent event) {
        // ... código existente ...
        
        if (event instanceof CheckInCompletedEvent) {
            notifyRoomReady((CheckInCompletedEvent) event);
        }
    }
}
```

### Remover um destinatário

Quando o contrato com o transfer acabar:

```java
// App.java - apenas comentar ou remover a linha
// eventPublisher.subscribe(new TransferNotificationListener(...));
```

Nenhum outro código precisa mudar.

### Adicionar canal de notificação (SMS, WhatsApp)

```java
public class WhatsAppNotificationListener implements EventListener {
    @Override
    public void handle(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent) {
            sendWhatsApp(event.getReservation());
        }
    }
}
```

## Outras Decisões Arquiteturais

### Repository Pattern
Abstrai o acesso a dados, facilitando testes e possível troca de banco.

### Service Layer
Centraliza a lógica de negócio, mantendo controllers finos.

### Arredondamento Half-Even
Usado em sistemas financeiros para evitar viés de arredondamento:
```java
amount.setScale(2, RoundingMode.HALF_EVEN)
```

### Cálculo de Alta Temporada
Implementado como método privado no serviço, permitindo mudanças futuras sem afetar a API.

## Conclusão

A arquitetura baseada em eventos garante que o sistema pode crescer com a pousada, permitindo adicionar novos destinatários e tipos de notificações sem modificar código existente. Isso reduz bugs, facilita testes e torna o sistema mais manutenível a longo prazo.
