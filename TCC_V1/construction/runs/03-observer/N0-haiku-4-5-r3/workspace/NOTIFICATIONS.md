# Sistema Extensível de Notificações

O sistema de notificações da Pousada Maré Alta foi projetado para ser facilmente extensível, permitindo adicionar novos canais e destinatários sem modificar o código existente.

## Arquitetura

### Componentes Principais

1. **NotificationService**: Gerencia o envio de notificações e a lista de handlers registrados
2. **NotificationHandler**: Interface que define como tratar eventos de reserva
3. **DefaultNotificationHandler**: Implementação padrão que envia emails para governança e financeiro
4. **Notification**: Classe que representa uma notificação (canal, destinatário, assunto, corpo)

### Fluxo de Funcionamento

```
Reserva é confirmada
        ↓
ReservationService.confirmReservation()
        ↓
NotificationService.notifyReservationConfirmed()
        ↓
Para cada NotificationHandler registrado:
  └→ handler.onReservationConfirmed(reservation)
        ↓
DefaultNotificationHandler.onReservationConfirmed()
        ↓
Envia emails para:
  - governanca@pousadamarealta.com.br
  - financeiro@pousadamarealta.com.br
  - hospede@email.com
```

## Adicionando Novos Handlers

Para adicionar um novo handler (por exemplo, para o restaurante):

### 1. Criar uma nova classe implementando NotificationHandler

```java
public class RestaurantNotificationHandler implements NotificationHandler {
    private final NotificationService notificationService;

    public RestaurantNotificationHandler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        String subject = String.format("Reserva confirmada - %d hóspedes",
            reservation.getGuests());
        String body = String.format(
            "Hóspede: %s\nData de entrada: %s\nQuantidade de pessoas: %d",
            reservation.getGuestName(),
            Formatters.formatDate(reservation.getCheckIn()),
            reservation.getGuests());

        Notification notification = new Notification(
            MessageChannel.EMAIL,
            "restaurante@pousadamarealta.com.br",
            subject,
            body
        );
        notificationService.send(notification);
    }

    @Override
    public void onReservationCancelled(Reservation reservation) {
        // Opcional: avisar sobre cancelamento
    }
}
```

### 2. Registrar o handler na inicialização

Em `Main.java`, após criar a `NotificationService`:

```java
NotificationService notificationService = new NotificationService(messageDAO);
notificationService.registerHandler(new DefaultNotificationHandler(notificationService));
notificationService.registerHandler(new RestaurantNotificationHandler(notificationService));
// Adicione quantos handlers forem necessários
```

## Adicionando Novos Canais de Notificação

Para adicionar um novo canal de notificação (por exemplo, SMS ao invés de apenas EMAIL):

### 1. Estender o enum MessageChannel

```java
public enum MessageChannel {
    EMAIL,
    SMS
}
```

### 2. Usar em um handler

```java
public class TransferNotificationHandler implements NotificationHandler {
    private final NotificationService notificationService;

    public TransferNotificationHandler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        // Enviar SMS para o transfer do aeroporto
        String body = String.format(
            "Novo hóspede chegando: %s. Data: %s",
            reservation.getGuestName(),
            Formatters.formatDate(reservation.getCheckIn()));

        Notification notification = new Notification(
            MessageChannel.SMS,
            "+55-11-99999-9999",
            "Novo hóspede",
            body
        );
        notificationService.send(notification);
    }

    @Override
    public void onReservationCancelled(Reservation reservation) {
        // Notificar cancelamento, se necessário
    }
}
```

## Exemplo: Restaurante e Transfer

Para adicionar notificações para restaurante e transfer:

```java
// Em Main.java
NotificationService notificationService = new NotificationService(messageDAO);

notificationService.registerHandler(new DefaultNotificationHandler(notificationService));
notificationService.registerHandler(new RestaurantNotificationHandler(notificationService));
notificationService.registerHandler(new AirportTransferNotificationHandler(notificationService));

// Os handlers serão notificados automaticamente quando:
// - Uma reserva for confirmada
// - Uma reserva for cancelada
```

## Tabela de Mensagens

Todas as notificações enviadas ficam armazenadas na tabela `messages`:

```sql
SELECT * FROM messages;

id | channel | to_address                           | subject                      | body
```

Isso permite:
- Auditoria de todas as notificações enviadas
- Consulta da recepção sobre o que foi comunicado
- Reprodução de mensagens perdidas
- Verificação de conformidade e histórico

## Boas Práticas

1. **Idempotência**: Um handler deve ser capaz de ser chamado múltiplas vezes sem efeitos colaterais indesejados
2. **Tratamento de Erros**: Exceções em um handler não devem impedir outros handlers de serem executados
3. **Logging**: Sempre registre as ações importantes (NotificationService já faz isso)
4. **Configurabilidade**: Considere usar variáveis de ambiente para emails de destinatários

## Configuração por Ambiente

Para tornar os emails configuráveis:

```java
// Em um handler
String governanceEmail = System.getenv("GOVERNANCE_EMAIL");
if (governanceEmail == null) {
    governanceEmail = "governanca@pousadamarealta.com.br";
}

Notification notification = new Notification(
    MessageChannel.EMAIL,
    governanceEmail,
    subject,
    body
);
```

## Testes

Ao testar novos handlers, você pode:

1. Verificar a tabela `messages` para validar que as notificações foram armazenadas
2. Mockar o `NotificationService` em testes unitários
3. Consultar `GET /messages` para ver todas as mensagens enviadas

```bash
curl http://localhost:7070/messages | jq '.[].subject'
```
