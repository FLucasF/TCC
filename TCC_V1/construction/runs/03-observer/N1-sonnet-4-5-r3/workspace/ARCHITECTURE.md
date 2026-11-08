# Arquitetura do Sistema

## Visão Geral

O sistema foi construído seguindo os princípios definidos no CLAUDE.md, com foco especial na extensibilidade do sistema de notificações.

## Sistema de Eventos (Event-Driven Architecture)

O requisito principal era que **novos departamentos e eventos pudessem ser adicionados sem modificar código existente**.

### Como funciona

1. **Eventos de Domínio** (`domain.events`)
   - `ReservationConfirmed`: Disparado quando uma reserva é confirmada
   - `ReservationCancelled`: Disparado quando uma reserva é cancelada (com flag indicando se estava confirmada)

2. **Event Publisher** (`EventPublisher`)
   - Gerencia o registro de listeners para cada tipo de evento
   - Distribui eventos para todos os listeners interessados

3. **Event Listeners** (`service.listeners`)
   - Cada listener reage a um evento específico e executa uma ação
   - Listeners atuais:
     - `GuestConfirmationListener`: Envia e-mail de confirmação ao hóspede
     - `HousekeepingConfirmationListener`: Notifica governança sobre nova reserva
     - `FinanceConfirmationListener`: Notifica financeiro sobre pagamento
     - `HousekeepingCancellationListener`: Notifica governança sobre cancelamento
     - `FinanceCancellationListener`: Notifica financeiro sobre reembolso

### Adicionando Novos Departamentos

Para adicionar notificações para o restaurante, por exemplo:

```java
// 1. Criar o listener
public class RestaurantConfirmationListener implements EventListener<ReservationConfirmed> {
    private static final String RESTAURANT_EMAIL = "restaurante@pousadamarealta.com.br";
    
    private final NotificationService notificationService;

    public RestaurantConfirmationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void handle(ReservationConfirmed event) {
        Reservation reservation = event.reservation();
        
        String subject = "Nova reserva - café da manhã";
        String body = String.format(
            "Entrada: %s | %d %s",
            DocumentFormatter.formatDate(reservation.getCheckIn()),
            reservation.getGuests(),
            reservation.getGuests() == 1 ? "hóspede" : "hóspedes"
        );
        
        notificationService.sendEmail(RESTAURANT_EMAIL, subject, body);
    }
}

// 2. Registrar o listener na Application.java
eventPublisher.subscribe(ReservationConfirmed.class, 
    new RestaurantConfirmationListener(notificationService));
```

**Pronto!** Nenhuma linha de código existente foi modificada. O sistema existente continua funcionando exatamente como antes.

### Adicionando Novos Eventos

Para adicionar notificações de check-in, por exemplo:

```java
// 1. Criar o novo evento
public record ReservationCheckedIn(Reservation reservation) implements DomainEvent {
}

// 2. Publicar o evento no ReservationService
public Reservation checkIn(Long id) {
    // ... código existente ...
    
    eventPublisher.publish(new ReservationCheckedIn(reservation));
    
    return reservation;
}

// 3. Criar listeners que reagem ao novo evento
public class TransportConfirmationListener implements EventListener<ReservationCheckedIn> {
    @Override
    public void handle(ReservationCheckedIn event) {
        // Notificar serviço de transfer
    }
}
```

## Fluxo de uma Confirmação de Reserva

```
1. Cliente → POST /reservations/{id}/confirmation
2. ReservationController → ReservationService.confirmReservation()
3. Reserva muda para CONFIRMED
4. EventPublisher.publish(new ReservationConfirmed(reservation))
5. EventPublisher distribui para todos os listeners registrados:
   ├─ GuestConfirmationListener → envia e-mail ao hóspede
   ├─ HousekeepingConfirmationListener → envia e-mail à governança
   └─ FinanceConfirmationListener → envia e-mail ao financeiro
6. Cada listener executa independentemente
7. Response retorna ao cliente
```

## Princípios Aplicados

1. **Open/Closed Principle**: Sistema aberto para extensão (novos listeners) mas fechado para modificação (código existente não muda)

2. **Single Responsibility**: Cada listener tem uma única responsabilidade bem definida

3. **Separation of Concerns**: Lógica de negócio (confirmar reserva) separada das notificações

4. **Loose Coupling**: Componentes comunicam-se através de eventos, não dependências diretas

## Estrutura de Pacotes

```
com.pousadamarealta
├── domain                    # Entidades e lógica de domínio
│   ├── events               # Eventos de domínio
│   ├── Room, Reservation
│   ├── RoomType, RatePlan, ReservationStatus
│   ├── PriceCalculator
│   └── DocumentFormatter
├── repository               # Persistência
│   ├── interfaces (RoomRepository, ReservationRepository, MessageRepository)
│   └── implementações JDBC
├── service                  # Lógica de negócio
│   ├── ReservationService   # Orquestra operações e publica eventos
│   ├── NotificationService  # Envia notificações
│   ├── ReportService        # Gera relatórios
│   └── listeners            # Listeners de eventos
├── api                      # Controllers REST
│   ├── controllers
│   └── dto
└── infrastructure           # Configuração
    └── DatabaseInitializer
```

## Benefícios da Abordagem

1. **Extensibilidade**: Adicionar novos departamentos não requer modificar código testado
2. **Testabilidade**: Cada listener pode ser testado independentemente
3. **Manutenibilidade**: Fácil entender o que acontece quando um evento ocorre
4. **Escalabilidade**: No futuro, eventos podem ser movidos para um message broker (RabbitMQ, Kafka)
5. **Rastreabilidade**: Todas as notificações ficam registradas no banco de dados
