# Sistema de Notificações

O sistema é projetado para ser extensível desde o primeiro dia, permitindo adicionar novos subscribers facilmente conforme novos contratos forem fechados.

## Subscribers Atuais

### 1. Hóspede (GuestNotificationListener)
Quando: Reserva é confirmada (pagamento recebido)
Formato: E-mail com comprovante da reserva

Exemplo:
```
Para: carla@email.com
Assunto: Reserva confirmada - Pousada Maré Alta

Olá, Carla Dias!

Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

Comprovante de reserva nº 7
Hóspede: Carla Dias (CPF 111.222.333-44)
Quarto: 201 (DELUXE)
Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
Diárias: R$ 779,80
Total: R$ 779,80

A entrada é a partir das 14h, e a saída, até as 12h.
Até breve!
```

### 2. Governança (GovernanceNotificationListener)
**Confirmação**: Quando reserva é confirmada
- Avisa para preparar o quarto
- Envia datas de entrada/saída e número de hóspedes

**Cancelamento**: Quando reserva confirmada é cancelada
- Avisa que o quarto ficou disponível
- Envia datas que foram liberadas

Exemplo (Confirmação):
```
Para: governanca@pousadamarealta.com.br
Assunto: Reserva confirmada - quarto 201

Entrada: 10/03/2027
Saída: 12/03/2027
Hóspedes: 2
```

Exemplo (Cancelamento):
```
Para: governanca@pousadamarealta.com.br
Assunto: Reserva cancelada - quarto 201

Entrada: 10/03/2027
Saída: 12/03/2027
```

### 3. Financeiro (FinanceNotificationListener)
**Confirmação**: Quando pagamento é recebido
- Avisa quanto foi recebido
- Vinculado ao número da reserva

**Cancelamento**: Quando reembolso é necessário
- Avisa do valor a ser devolvido
- Só envia se houver valor de reembolso

Exemplo (Confirmação):
```
Para: financeiro@pousadamarealta.com.br
Assunto: Pagamento recebido - reserva 7

Valor: R$ 779,80
```

Exemplo (Cancelamento):
```
Para: financeiro@pousadamarealta.com.br
Assunto: Reembolso - reserva 7

Valor: R$ 779,80
```

## Ciclo de Vida das Notificações

```
Reserva Criada (PENDING)
    ↓
    └─ Nenhuma notificação

Reserva Confirmada (CONFIRMED)
    ↓
    ├─ Hóspede: Recebe comprovante
    ├─ Governança: Aviso para preparar quarto
    └─ Financeiro: Confirmação de pagamento

Reserva com Check-in (CHECKED_IN)
    ↓
    └─ Nenhuma notificação automática

Reserva com Check-out (CHECKED_OUT)
    ↓
    └─ Nenhuma notificação automática

Reserva Cancelada (CANCELLED) - vindo de PENDING
    ↓
    └─ Nenhuma notificação (nunca foi confirmada)

Reserva Cancelada (CANCELLED) - vindo de CONFIRMED/CHECKED_IN/CHECKED_OUT
    ↓
    ├─ Governança: Aviso de quarto liberado
    └─ Financeiro: Aviso de reembolso (se houver)
```

## Como Adicionar Novo Subscriber

O cliente mencionou que o restaurante quer saber das confirmações e o transfer do aeroporto quer saber de chegadas. Aqui está como adicionar:

### Passo 1: Criar a classe do listener

```java
package com.pousada.marealta.notification;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.repository.MessageRepository;
import com.pousada.marealta.domain.Message.Channel;
import com.pousada.marealta.util.DateFormatter;

import java.sql.SQLException;

public class RestaurantNotificationListener implements NotificationListener {
    private static final String RESTAURANT_EMAIL = "restaurante@pousadamarealta.com.br";
    private final MessageRepository messageRepository;

    public RestaurantNotificationListener(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        try {
            String subject = "Café da manhã confirmado - " + reservation.getGuests() + 
                           " hóspede(s) em " + DateFormatter.format(reservation.getCheckIn());
            String body = String.format("""
                Novo hóspede(s) chegando no quarto %d.
                
                Hóspede: %s
                Entrada: %s
                Saída: %s
                Número de pessoas: %d
                """,
                reservation.getRoomNumber(),
                reservation.getGuestName(),
                DateFormatter.format(reservation.getCheckIn()),
                DateFormatter.format(reservation.getCheckOut()),
                reservation.getGuests()
            );
            messageRepository.save(Channel.EMAIL, RESTAURANT_EMAIL, subject, body);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to send restaurant notification", e);
        }
    }

    @Override
    public void onReservationCancelled(Reservation reservation) {
        // Restaurante não precisa ser notificado de cancelamentos
    }
}
```

### Passo 2: Registrar o listener

Em `Application.java`, após inicializar o NotificationService:

```java
NotificationService notificationService = NotificationService.getInstance();
notificationService.initialize(new MessageRepository());
notificationService.addListener(new RestaurantNotificationListener(new MessageRepository()));
```

### Passo 3: (Opcional) Criar listener para o transfer

```java
public class AirportTransferNotificationListener implements NotificationListener {
    private static final String TRANSFER_EMAIL = "transfer@empresa-parceira.com.br";
    private final MessageRepository messageRepository;

    public AirportTransferNotificationListener(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        try {
            String subject = "Chegada esperada - " + 
                           DateFormatter.format(reservation.getCheckIn()) + 
                           " - " + reservation.getGuests() + " pessoa(s)";
            
            String body = String.format("""
                Hóspede: %s (CPF: %s)
                Chegada prevista: %s (a partir das 14h)
                Quarto: %d
                Hóspedes: %d
                
                Contato: %s
                """,
                reservation.getGuestName(),
                reservation.getGuestCpf(),
                DateFormatter.format(reservation.getCheckIn()),
                reservation.getRoomNumber(),
                reservation.getGuests(),
                reservation.getGuestEmail()
            );
            
            messageRepository.save(Channel.EMAIL, TRANSFER_EMAIL, subject, body);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to send transfer notification", e);
        }
    }

    @Override
    public void onReservationCancelled(Reservation reservation) {
        try {
            String subject = "Transferência cancelada - " + reservation.getGuestName();
            String body = "Cancelamento de chegada para " + 
                         DateFormatter.format(reservation.getCheckIn());
            messageRepository.save(Channel.EMAIL, TRANSFER_EMAIL, subject, body);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to send transfer cancellation notification", e);
        }
    }
}
```

Depois registrar:
```java
notificationService.addListener(new AirportTransferNotificationListener(new MessageRepository()));
```

## Consultar Mensagens Enviadas

A recepção pode consultar todas as mensagens que foram enviadas:

```bash
curl http://localhost:7070/messages
```

Isso mostra:
- A quem foi enviado
- Quando foi enviado (via log de banco de dados)
- Conteúdo completo da mensagem
- Canal (EMAIL, e no futuro SMS, WhatsApp, etc)

## Mudanças Futuras

Quando um subscriber deixar de ser necessário ou será descontinuado:

1. **Remover do NotificationService**: Comentar ou remover a linha de `addListener()`
2. **Arquivar o código**: Manter a classe (em caso de auditoria)
3. **Sem perda de dados**: Mensagens antigas continuam no histórico

Exemplo:
```java
notificationService.initialize(new MessageRepository());
notificationService.addListener(new GuestNotificationListener(messageRepository));
notificationService.addListener(new GovernanceNotificationListener(messageRepository));
notificationService.addListener(new FinanceNotificationListener(messageRepository));
// notificationService.addListener(new RestaurantNotificationListener(messageRepository)); // Descontinuado
```

## Configuração em Banco de Dados

Para uma versão mais robusta futura, os subscribers poderiam ser configuráveis via banco:

```sql
CREATE TABLE notification_subscribers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    events TEXT NOT NULL, -- 'CONFIRMED,CANCELLED'
    active BOOLEAN DEFAULT true
);
```

Isso permitiria adicionar/remover subscribers sem recompilar o código.
