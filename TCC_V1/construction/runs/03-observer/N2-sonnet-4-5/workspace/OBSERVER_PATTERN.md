# Padrão Observer - Sistema de Notificações

## Arquitetura

```
┌─────────────────────────────────────────────────────────────────┐
│                      ReservationService                          │
│  (não conhece os listeners, apenas notifica quando algo acontece)│
└────────────────┬────────────────────────────────────────────────┘
                 │
                 │ notifyListeners(event)
                 │
                 ▼
      ┌──────────────────────┐
      │ ReservationListener  │ ◄──── Interface
      │  onEvent(event)      │
      └──────────────────────┘
               △
               │ implements
               │
    ┌──────────┼──────────┬──────────┐
    │          │          │          │
    ▼          ▼          ▼          ▼
┌─────┐   ┌─────┐   ┌─────┐   ┌─────┐
│Guest│   │House│   │Finan│   │...  │
│Noti │   │keep │   │ce   │   │futu │
│fier │   │ing  │   │Noti │   │ro   │
└─────┘   └─────┘   └─────┘   └─────┘
```

## Fluxo de Execução

### Confirmação de Reserva

```
Cliente API
    │
    │ POST /reservations/1/confirmation
    │
    ▼
ApiController
    │
    │ reservationService.confirm(1)
    │
    ▼
ReservationService
    │
    │ reservation.confirm()
    │ notifyListeners(new Confirmed(reservation))
    │
    ├─────────────┬─────────────┬─────────────┐
    │             │             │             │
    ▼             ▼             ▼             ▼
GuestNotifier  Housekeeping  Finance    (futuros)
    │          Notifier      Notifier
    │             │             │
    │             │             │
    ▼             ▼             ▼
MessageRepository (envia e-mails)
```

## Código Simplificado

### 1. Evento
```java
public sealed interface ReservationEvent {
    Reservation reservation();
    
    record Confirmed(Reservation reservation) 
        implements ReservationEvent {}
}
```

### 2. Interface do Listener
```java
public interface ReservationListener {
    void onEvent(ReservationEvent event);
}
```

### 3. Listener Concreto
```java
public final class FinanceNotifier implements ReservationListener {
    private final MessageSender messageSender;
    
    @Override
    public void onEvent(ReservationEvent event) {
        if (event instanceof ReservationEvent.Confirmed confirmed) {
            notifyPayment(confirmed.reservation());
        }
    }
    
    private void notifyPayment(Reservation r) {
        messageSender.send(
            MessageChannel.EMAIL,
            "financeiro@pousadamarealta.com.br",
            "Pagamento recebido - reserva " + r.id(),
            "Valor: " + formatMoney(r.amountPaid())
        );
    }
}
```

### 4. Fonte (não conhece os listeners)
```java
public final class ReservationService {
    private final List<ReservationListener> listeners;
    
    public ReservationService(..., List<ReservationListener> listeners) {
        this.listeners = List.copyOf(listeners);
    }
    
    public Reservation confirm(int id) {
        Reservation reservation = findById(id);
        reservation.confirm();
        notifyListeners(new ReservationEvent.Confirmed(reservation));
        return reservation;
    }
    
    private void notifyListeners(ReservationEvent event) {
        for (ReservationListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
```

### 5. Configuração (onde os listeners são montados)
```java
public class Main {
    public static void main(String[] args) {
        MessageRepository messageRepository = new MessageRepository();
        
        // Cria os listeners
        GuestNotifier guestNotifier = new GuestNotifier(messageRepository);
        HousekeepingNotifier housekeeping = new HousekeepingNotifier(messageRepository);
        FinanceNotifier finance = new FinanceNotifier(messageRepository);
        
        // Monta a lista (é aqui que se adiciona/remove listeners)
        List<ReservationListener> listeners = List.of(
            guestNotifier,
            housekeeping,
            finance
            // RestaurantNotifier quando necessário
            // TransferNotifier quando necessário
        );
        
        // Injeta no serviço
        ReservationService service = new ReservationService(
            ..., listeners
        );
    }
}
```

## Adicionando um Novo Listener

Para adicionar notificação para o restaurante (café da manhã):

### 1. Criar a Classe
```java
public final class RestaurantNotifier implements ReservationListener {
    private final MessageSender messageSender;
    
    public RestaurantNotifier(MessageSender messageSender) {
        this.messageSender = messageSender;
    }
    
    @Override
    public void onEvent(ReservationEvent event) {
        if (event instanceof ReservationEvent.Confirmed confirmed) {
            notifyBreakfast(confirmed.reservation());
        }
    }
    
    private void notifyBreakfast(Reservation r) {
        messageSender.send(
            MessageChannel.EMAIL,
            "restaurante@pousadamarealta.com.br",
            String.format("Café da manhã - reserva %d", r.id()),
            String.format(
                "Preparar café para %d pessoas em %s",
                r.guests(),
                formatDate(r.checkIn())
            )
        );
    }
}
```

### 2. Registrar em Main.java
```java
RestaurantNotifier restaurant = new RestaurantNotifier(messageRepository);

List<ReservationListener> listeners = List.of(
    guestNotifier,
    housekeeping,
    finance,
    restaurant  // ← apenas adiciona aqui
);
```

### 3. Pronto!
- ✅ Nenhuma mudança em `ReservationService`
- ✅ Nenhuma mudança nos listeners existentes
- ✅ Nenhuma mudança na API
- ✅ Um lugar só modificado: a configuração em `Main`

## Benefícios

### Extensibilidade
Adicionar novos interessados (transfer, limpeza, manutenção) é trivial: uma classe nova + uma linha de registro.

### Separação de Responsabilidades
- `ReservationService`: gerencia reservas, não conhece detalhes de notificação
- Cada `Notifier`: sabe quando e como notificar seu destinatário
- `Main`: decide quem está ativo no momento

### Testabilidade
Cada listener pode ser testado isoladamente. O serviço pode ser testado com listeners mock.

### Manutenibilidade
Se o financeiro mudar de e-mail ou regra de notificação, mexe-se apenas em `FinanceNotifier`.

## Comparação: Com vs Sem Observer

### ❌ Sem Observer (hard-coded)
```java
public Reservation confirm(int id) {
    reservation.confirm();
    
    // Hóspede
    String receipt = buildReceipt(reservation);
    messageRepo.send(EMAIL, reservation.email(), "Confirmada", receipt);
    
    // Governança
    messageRepo.send(EMAIL, "governanca@...", "Confirmada - " + ..., ...);
    
    // Financeiro
    messageRepo.send(EMAIL, "financeiro@...", "Pagamento - " + ..., ...);
    
    // TODO: adicionar restaurante quando contratar
    // TODO: adicionar transfer quando contratar
    
    return reservation;
}
```

**Problemas:**
- Adicionar novo interessado = editar `ReservationService`
- Lógica de formatação de cada e-mail no serviço
- Difícil testar isoladamente
- Cresce a cada novo requisito

### ✅ Com Observer
```java
public Reservation confirm(int id) {
    reservation.confirm();
    notifyListeners(new ReservationEvent.Confirmed(reservation));
    return reservation;
}
```

**Vantagens:**
- Serviço estável, não muda quando adicionam interessados
- Cada notificação em sua própria classe
- Fácil testar cada listener
- Configuração centralizada em um lugar só
