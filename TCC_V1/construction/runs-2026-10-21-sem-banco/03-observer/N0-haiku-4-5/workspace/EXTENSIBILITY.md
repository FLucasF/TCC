# Guia de Extensibilidade

Este documento descreve como estender o sistema com novos notificadores e integrações.

## Sistema de Notificações Extensível

O sistema foi desenhado desde o início para aceitar novos destinatários de notificações sem modificar o código existente.

### Arquitetura Current

O sistema usa um padrão Observer baseado em eventos:

```
ReservationService.confirmReservation()
    ↓
    eventDispatcher.dispatch(ReservationConfirmedEvent)
    ↓
EventDispatcher itera sobre todos os listeners registrados
    ├→ GuestConfirmationNotifier
    ├→ GovernanceConfirmationNotifier
    ├→ FinancialConfirmationNotifier
    └→ ... (mais listeners podem ser adicionados)
```

### Adicionar um Novo Notificador: Passo a Passo

#### Cenário: Restaurante quer saber sobre confirmações

O restaurante precisa planejar o café da manhã com antecedência. Quando uma reserva é confirmada, ele recebe um email.

##### 1. Criar a Classe do Notificador

```java
// src/main/java/com/pousadamarealta/notification/RestaurantNotifier.java
package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.notification.event.ReservationConfirmedEvent;
import com.pousadamarealta.notification.event.ReservationEvent;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

public class RestaurantNotifier implements ReservationEventListener {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final MessageStore messageStore;
    private final AtomicLong messageIdCounter;

    public RestaurantNotifier(MessageStore messageStore, AtomicLong messageIdCounter) {
        this.messageStore = messageStore;
        this.messageIdCounter = messageIdCounter;
    }

    @Override
    public void onReservationEvent(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent confirmed) {
            Reservation res = confirmed.reservation();
            String body = String.format(
                "Novo hóspede confirmado:\n\n" +
                "Entrada: %s\n" +
                "Saída: %s\n" +
                "Hóspedes: %d\n" +
                "Quarto: %d\n",
                res.checkIn.format(DATE_FORMATTER),
                res.checkOut.format(DATE_FORMATTER),
                res.guests,
                res.roomNumber
            );

            Message msg = new Message(
                messageIdCounter.incrementAndGet(),
                "EMAIL",
                "restaurante@pousadamarealta.com.br",
                String.format("Novo hóspede - %s a %s",
                    res.checkIn.format(DATE_FORMATTER),
                    res.checkOut.format(DATE_FORMATTER)
                ),
                body
            );
            messageStore.addMessage(msg);
        }
    }
}
```

##### 2. Registrar no EventDispatcher (Main.java)

Na classe `Main`, adicione uma linha para registrar o novo notificador:

```java
public class Main {
    public static void main(String[] args) {
        // ... código existente ...
        
        // Registrar todos os notificadores
        eventDispatcher.subscribe(new GuestConfirmationNotifier(messageStore, messageIdCounter));
        eventDispatcher.subscribe(new GovernanceConfirmationNotifier(messageStore, messageIdCounter));
        eventDispatcher.subscribe(new FinancialConfirmationNotifier(messageStore, messageIdCounter));
        eventDispatcher.subscribe(new GovernanceCancellationNotifier(messageStore, messageIdCounter));
        eventDispatcher.subscribe(new FinancialCancellationNotifier(messageStore, messageIdCounter));
        
        // NOVA LINHA: Registrar restaurante
        eventDispatcher.subscribe(new RestaurantNotifier(messageStore, messageIdCounter));
        
        // ... resto do código ...
    }
}
```

##### 3. Compilar e Testar

```bash
mvn clean package
```

Pronto! Quando uma reserva for confirmada, o restaurante receberá automaticamente um email.

## Casos de Uso: Novos Notificadores

### Transfer do Aeroporto

```java
public class TransferNotifier implements ReservationEventListener {
    @Override
    public void onReservationEvent(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent confirmed) {
            // Notificar transfer sobre chegadas
            Reservation res = confirmed.reservation();
            // Enviar data de check-in para agendar pickup
        }
    }
}
```

### Limpeza (Housekeeping)

```java
public class HousekeepingNotifier implements ReservationEventListener {
    @Override
    public void onReservationEvent(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent confirmed) {
            // Preparar quarto
        } else if (event instanceof ReservationCancelledEvent cancelled) {
            // Cancelar limpeza
        }
    }
}
```

### Sistema de CRM/Marketing

```java
public class CRMNotifier implements ReservationEventListener {
    @Override
    public void onReservationEvent(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent confirmed) {
            // Adicionar hóspede ao banco de dados de marketing
            // Preparar follow-up pós-estadia
        }
    }
}
```

### Integração com API de Terceiros

```java
public class PaymentGatewayNotifier implements ReservationEventListener {
    private final PaymentGateway gateway;
    
    public PaymentGatewayNotifier(PaymentGateway gateway) {
        this.gateway = gateway;
    }
    
    @Override
    public void onReservationEvent(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent confirmed) {
            Reservation res = confirmed.reservation();
            // Registrar cobrança no gateway de pagamento
            gateway.chargeCard(res.id, res.total);
        }
    }
}
```

## Padrão de Implementação

Todos os notificadores devem seguir este padrão:

```java
public class NomeNotifier implements ReservationEventListener {
    private final [Dependências necessárias];

    public NomeNotifier([Dependências]) {
        this.[Dependências] = [Dependências];
    }

    @Override
    public void onReservationEvent(ReservationEvent event) {
        // Verificar tipo de evento
        if (event instanceof ReservationConfirmedEvent confirmed) {
            Reservation res = confirmed.reservation();
            // Implementar lógica para confirmação
        } else if (event instanceof ReservationCancelledEvent cancelled) {
            Reservation res = cancelled.reservation();
            // Implementar lógica para cancelamento
        }
    }
}
```

## Boas Práticas

### 1. Ser Defensivo
Sempre verificar os tipos de evento antes de processar:

```java
// ❌ ERRADO
Reservation res = (Reservation) event;

// ✅ CORRETO
if (event instanceof ReservationConfirmedEvent confirmed) {
    Reservation res = confirmed.reservation();
}
```

### 2. Não Lançar Exceções
Os notificadores não devem lançar exceções. Se algo der errado, fazer logging:

```java
try {
    // Implementação
} catch (Exception e) {
    System.err.println("Erro ao notificar: " + e.getMessage());
    // Em produção: usar um logger apropriado
}
```

### 3. Manter Rápido
Notificadores devem ser rápidos. Se precisar fazer trabalho pesado (chamar APIs externas), usar thread assincronas:

```java
// Usar thread pool para não bloquear
threadPool.submit(() -> {
    // Trabalho pesado aqui
});
```

### 4. Reutilizar Formatadores
Use os formatadores existentes para padronizar saída:

```java
// ✅ CORRETO: Usar DateTimeFormatter existente
String data = res.checkIn.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

// ❌ EVITAR: Criar formatadores novos
```

## Removendo Notificadores

Se um departamento não mais precisar de notificações, basta remover a linha de registro em `Main.java`:

```java
// Comentar ou remover a linha
// eventDispatcher.subscribe(new RestaurantNotifier(messageStore, messageIdCounter));
```

Nenhuma outra mudança é necessária!

## Testando Novos Notificadores

Para testar um novo notificador sem afetar o sistema:

1. Registrá-lo em uma branch de desenvolvimento
2. Compilar: `mvn clean package`
3. Rodar o servidor e fazer requisições de teste
4. Verificar os emails: `GET /messages`
5. Validar que os emails foram enviados para o destinatário correto

```bash
# Teste manual
curl -s http://localhost:7070/messages | jq '.[] | select(.to == "restaurante@pousadamarealta.com.br")'
```

## Futuro: Persistência de Notificações

Quando o sistema evoluir, considerar:

1. **Banco de dados para mensagens**: Ao invés de MessageStore em memória
2. **Filas de mensagens**: RabbitMQ, AWS SQS para processar notificações assincronamente
3. **Templates de Email**: Sistema de templates para emails padronizados
4. **Regras de Notificação**: Configuração de quem recebe o quê (pode ser ativado/desativado por departamento)
5. **Logging e Auditoria**: Registrar todas as notificações enviadas

## Integração com Sistemas Externos

O sistema está pronto para integrar com:

- **Mailgun/SendGrid**: Enviar emails de verdade
- **Slack**: Notificações em tempo real
- **WhatsApp Business**: Confirmações via WhatsApp
- **Calendário Google**: Sincronizar disponibilidade
- **Contabilidade**: Integrar com software de contabilidade
- **Booking.com/Airbnb**: Sincronizar com plataformas de OTA

Todos com o mesmo padrão: implementar `ReservationEventListener` e registrar no `EventDispatcher`.
