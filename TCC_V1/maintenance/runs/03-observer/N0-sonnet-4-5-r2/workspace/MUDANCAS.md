# Mudanças Implementadas - Sistema de Notificações

## O que foi feito

Implementei um sistema de notificações extensível que avisa automaticamente a governança e o financeiro quando reservas são confirmadas ou canceladas.

### Notificações implementadas

#### Quando uma reserva é confirmada:
- **Governança** recebe e-mail em `governanca@pousadamarealta.com.br`
  - Assunto: "Reserva confirmada - quarto 201" (com o número do quarto)
  - Conteúdo: datas de check-in e check-out, quantidade de hóspedes
  
- **Financeiro** recebe e-mail em `financeiro@pousadamarealta.com.br`
  - Assunto: "Pagamento recebido - reserva 7" (com o número da reserva)
  - Conteúdo: valor pago

#### Quando uma reserva confirmada é cancelada:
- **Governança** recebe e-mail:
  - Assunto: "Reserva cancelada - quarto 201"
  - Conteúdo: datas que ficaram livres
  
- **Financeiro** recebe e-mail (somente se houver reembolso):
  - Assunto: "Reembolso - reserva 7"
  - Conteúdo: valor a devolver
  - Nota: Cancelamento de NON_REFUNDABLE não notifica o financeiro (não há reembolso)

#### Quando uma reserva pendente é cancelada:
- Ninguém é notificado (a reserva nunca foi confirmada)

### Arquitetura

Implementei um padrão **Observer/Event-driven** que facilita adicionar novos departamentos no futuro:

- **Eventos**: `ReservationConfirmed` e `ReservationCancelled`
- **Listeners**: `HousekeepingNotifier` e `FinanceNotifier`
- **Publisher**: `ReservationEventPublisher` que gerencia os listeners

Para adicionar novos departamentos (como restaurante ou transfer):
1. Criar um novo listener implementando `ReservationEventListener`
2. Registrá-lo no `Hotel.java` junto com os outros

### Arquivos criados

```
src/main/java/hotel/notifications/
├── ReservationEvent.java              # Interface base para eventos
├── ReservationConfirmed.java          # Evento de confirmação
├── ReservationCancelled.java          # Evento de cancelamento
├── ReservationEventListener.java      # Interface para listeners
├── ReservationEventPublisher.java     # Gerenciador de eventos
├── HousekeepingNotifier.java          # Notificador da governança
└── FinanceNotifier.java               # Notificador do financeiro
```

### Arquivos modificados

- `ReservationService.java`: Publica eventos ao confirmar/cancelar reservas
- `Hotel.java`: Configura os notificadores
- Testes: Atualizados para considerar as novas mensagens

### Testes

Criei `NotificationTest.java` que verifica:
- Notificações enviadas na confirmação
- Notificações enviadas no cancelamento com reembolso
- Notificações enviadas no cancelamento sem reembolso (NON_REFUNDABLE)
- Nenhuma notificação no cancelamento de reserva pendente

Todos os testes existentes continuam passando, e os novos testes verificam o comportamento correto das notificações.

## Próximos passos (quando necessário)

Para adicionar o restaurante ou transfer, basta criar novos listeners seguindo o mesmo padrão:

```java
public final class RestaurantNotifier implements ReservationEventListener {
    @Override
    public void onEvent(ReservationEvent event) {
        if (event instanceof ReservationConfirmed confirmed) {
            // enviar e-mail para restaurante@pousadamarealta.com.br
        }
    }
}
```

E registrar no `Hotel.java`:
```java
eventPublisher.register(new RestaurantNotifier(emailSender));
```
