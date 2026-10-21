# Arquitetura do Sistema

## Visão Geral

O sistema é construído com uma arquitetura limpa baseada em camadas, com suporte para um sistema de notificações baseado em eventos que permite adicionar novos destinatários sem modificar o código core.

```
┌─────────────────────────────────────┐
│        API Controllers              │
│  (RoomController, ReservationCtrl)  │
└──────────────┬──────────────────────┘
               │
┌──────────────┴──────────────────────┐
│         Service Layer               │
│  (ReservationService, RoomService)  │
└──────────────┬──────────────────────┘
               │
┌──────────────┴──────────────────────┐
│   Domain & Events                   │
│  (Reservation, Room, Events)        │
└──────────────┬──────────────────────┘
               │
┌──────────────┴──────────────────────┐
│  Event Listeners (Notificadores)    │
│  (Async event-driven notifications) │
└─────────────────────────────────────┘
```

## Sistema de Eventos e Notificações

O sistema de notificações é completamente extensível através de um padrão observer/pub-sub:

### Componentes Principais

1. **EventDispatcher**: Gerencia inscrição e despache de eventos
   - Subscribers se registram para eventos
   - Ao ocorrer um evento, todos os subscribers são notificados

2. **ReservationEvent**: Interface sealed com dois eventos:
   - `ReservationConfirmedEvent`: Disparado quando uma reserva é confirmada
   - `ReservationCancelledEvent`: Disparado quando uma reserva é cancelada

3. **ReservationEventListener**: Interface que define o contrato para listeners
   - `onReservationEvent(ReservationEvent event)`: Processador de eventos

### Notificadores Implementados

```
ReservationEventListener (interface)
├── GuestConfirmationNotifier
│   └── Envia email de confirmação para o hóspede
├── GovernanceConfirmationNotifier
│   └── Notifica governança quando reserva é confirmada
├── FinancialConfirmationNotifier
│   └── Notifica financeiro quando pagamento é recebido
├── GovernanceCancellationNotifier
│   └── Notifica governança quando reserva é cancelada
└── FinancialCancellationNotifier
    └── Notifica financeiro quando reembolso é necessário
```

### Como Adicionar Novos Notificadores

Para adicionar um novo notificador (ex: restaurante, transfer), basta:

1. Criar uma classe que implemente `ReservationEventListener`:

```java
public class RestaurantNotifier implements ReservationEventListener {
    @Override
    public void onReservationEvent(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent confirmed) {
            // Notificar restaurante sobre novo hóspede
            Reservation res = confirmed.reservation();
            // ... implementação
        }
    }
}
```

2. Registrar no EventDispatcher na classe Main:

```java
eventDispatcher.subscribe(new RestaurantNotifier(messageStore, messageIdCounter));
```

Nenhuma outra mudança é necessária no código existente!

## Estrutura de Pacotes

```
com.pousadamarealta/
├── Main.java                          # Entry point
├── domain/                            # Entidades de domínio
│   ├── Room.java
│   ├── RoomType.java
│   ├── Reservation.java
│   ├── ReservationStatus.java
│   ├── RatePlan.java
│   └── Message.java
├── service/                           # Lógica de negócio
│   ├── RoomService.java
│   ├── ReservationService.java
│   ├── PricingService.java
│   └── ReportService.java
├── api/                               # Controladores e DTOs
│   ├── RoomController.java
│   ├── ReservationController.java
│   ├── MessageController.java
│   ├── ReportController.java
│   └── dto/
│       ├── CreateReservationRequest.java
│       ├── ReservationResponse.java
│       ├── DocumentResponse.java
│       ├── MessageResponse.java
│       └── ErrorResponse.java
├── notification/                      # Sistema de eventos
│   ├── EventDispatcher.java
│   ├── MessageStore.java
│   ├── ReservationEventListener.java
│   ├── GuestConfirmationNotifier.java
│   ├── GovernanceConfirmationNotifier.java
│   ├── FinancialConfirmationNotifier.java
│   ├── GovernanceCancellationNotifier.java
│   ├── FinancialCancellationNotifier.java
│   └── event/
│       ├── ReservationEvent.java (sealed interface)
│       ├── ReservationConfirmedEvent.java
│       └── ReservationCancelledEvent.java
└── util/
    └── ComprovantFormatter.java
```

## Fluxo de Dados

### Criação e Confirmação de Reserva

```
POST /reservations
    ↓
ReservationController.createReservation()
    ↓
ReservationService.createReservation()
    ├── Valida entrada
    ├── Calcula preços (PricingService)
    ├── Verifica disponibilidade
    └── Armazena em HashMap
    
POST /reservations/{id}/confirmation
    ↓
ReservationService.confirmReservation()
    ├── Valida status
    ├── Atualiza status para CONFIRMED
    └── Dispara ReservationConfirmedEvent
            ↓
    EventDispatcher.dispatch(event)
        ├── GuestConfirmationNotifier
        ├── GovernanceConfirmationNotifier
        └── FinancialConfirmationNotifier
            └── Cada notificador adiciona mensagens ao MessageStore
```

### Cálculo de Preços

```
ReservationService.createReservation()
    ↓
PricingService.calculateNightlyRate()
    ├── Detecta alta temporada
    └── Aplica multiplicador 1.20 se necessário
    ↓
PricingService.calculateSubtotal()
    └── Soma preços diários (arredonda cada noite)
    ↓
PricingService.calculateDiscount()
    └── Aplica 15% se NON_REFUNDABLE
    ↓
PricingService.calculateTotal()
    └── Subtotal - Desconto (com arredondamento)
```

## Decisões de Design

### 1. Records vs Classes
- **Records**: Utilizados para DTOs imutáveis (Room, Message, DocumentResponse)
- **Classes**: Utilizadas para entidades de domínio (Reservation) que necessitam métodos de cópia
- **Interfaces Sealed**: Utilizadas para eventos para type safety

### 2. Armazenamento em Memória
- HashMap<Long, Reservation> para lookups rápidos por ID
- ArrayList em MessageStore para manter ordem de mensagens
- AtomicLong para IDs sequenciais thread-safe

### 3. Tratamento de Erros
- Exceções de negócio (ReservationException) com código de erro
- Mapeamento em ReservationController para status HTTP apropriados
- Validação em camada de controlador + serviço

### 4. Notificações Assincronas (Simuladas)
- Notificadores são chamados sincronamente no dispatch
- Emails são armazenados em memória (MessageStore)
- Sistema pronto para ser expandido com fila assincronas (RabbitMQ, Kafka) no futuro

## Extensibilidade Futura

### Adicionar Novos Notificadores
1. Implementar ReservationEventListener
2. Registrar no EventDispatcher em Main

### Persistência de Dados
1. Substituir HashMap por JPA (Hibernate)
2. Adicionar banco de dados (PostgreSQL, MySQL)
3. Sem mudanças necessárias na lógica de negócio

### Envio Real de Emails
1. Implementar EmailService
2. Integrar com SendGrid, AWS SES, etc
3. Adicionar fila assincronas (sem bloquear requisições)

### Múltiplas Pousadas
1. Adicionar tenantId às entidades
2. Isolamento de dados por tenant
3. Suporte a taxas e horários customizados por pousada

## Testes

A arquitetura permite testes unitários isolados:
- Mock de dependências (RoomService, MessageStore)
- Testes de eventos sem efeitos colaterais
- Testes de cálculos de preço com cases edge

```bash
mvn test  # (quando testes forem adicionados)
```
