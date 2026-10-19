# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta, com funcionalidades para criação, confirmação, check-in, check-out e cancelamento de reservas, além de um sistema extensível de notificações.

## Características Principais

### Quartos
- 101, 102: STANDARD (2 pessoas) - R$ 249,90/noite
- 201, 202: DELUXE (3 pessoas) - R$ 389,90/noite
- 301: SUITE (4 pessoas) - R$ 579,90/noite

### Regras de Negócio
- **Alta Temporada** (acréscimo de 20%): 15/dez a fim de fevereiro e todo mês de julho
- **Planos de Tarifa**:
  - STANDARD: preço normal, reembolso total se cancelado
  - NON_REFUNDABLE: 15% de desconto, sem reembolso se cancelado
- **Arredondamento**: HALF_EVEN (banker's rounding) em todas as etapas de cálculo

### Sistema de Notificações
Sistema extensível baseado em eventos que envia notificações automáticas:

**Quando uma reserva é confirmada:**
- Hóspede: recebe comprovante por email
- Governança (governanca@pousadamarealta.com.br): informações sobre preparação do quarto
- Financeiro (financeiro@pousadamarealta.com.br): valor do pagamento recebido

**Quando uma reserva confirmada é cancelada:**
- Governança: datas que ficaram livres
- Financeiro: valor a reembolsar (apenas se houver reembolso)

O sistema é preparado para novos destinatários (restaurante, transfer, etc.) através da adição de novos `NotificationHandler`.

## Compilação e Execução

### Requisitos
- Java 21
- Maven 3.6+

### Compilar e Testar
```bash
mvn verify
```

### Gerar JAR Executável
```bash
mvn package
```

### Executar
```bash
java -jar target/reservas-1.0.0.jar
```

O servidor inicia na porta configurada pela variável de ambiente `PORT`, ou 7070 por padrão.

## API REST

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria nova reserva (status: PENDING)
- `GET /reservations/{id}` - Consulta reserva
- `POST /reservations/{id}/confirmation` - Confirma reserva (CONFIRMED) e envia notificações
- `POST /reservations/{id}/check-in` - Realiza check-in (CHECKED_IN)
- `POST /reservations/{id}/check-out` - Realiza check-out (CHECKED_OUT) e retorna comprovante
- `POST /reservations/{id}/cancellation` - Cancela reserva (CANCELLED) e envia notificações
- `GET /reservations/{id}/document` - Obtém segunda via do comprovante

### Mensagens
- `GET /messages` - Lista todas as mensagens de notificação enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação em CSV
- `GET /reports/revenue?month=2027-07` - Relatório financeiro em CSV

## Arquitetura

### Padrão de Notificações
O sistema utiliza um padrão de eventos e handlers:

```
ReservationEvent (sealed interface)
  ├─ ReservationConfirmedEvent
  └─ ReservationCancelledEvent

NotificationHandler (interface)
  ├─ GuestNotificationHandler
  ├─ HousekeepingNotificationHandler
  └─ FinanceNotificationHandler
```

Para adicionar novos tipos de notificação:
1. Criar novo `NotificationHandler`
2. Registrar no `NotificationService`
3. O handler receberá automaticamente todos os eventos relevantes

### Estrutura do Código
```
com.pousadamarealta/
├── domain/           # Entidades de domínio (Room, Reservation, enums)
├── service/          # Lógica de negócio e orquestração
├── notification/     # Sistema de notificações extensível
├── controller/       # Controladores REST
├── dto/              # Data Transfer Objects
└── Application.java  # Ponto de entrada
```

## Desenvolvimento

### Adicionar Novo Tipo de Notificação

Exemplo: notificar o restaurante sobre novas reservas confirmadas.

1. Criar handler:
```java
public class RestaurantNotificationHandler implements NotificationHandler {
    @Override
    public List<NotificationMessage> handle(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent confirmed) {
            // criar e retornar mensagem
        }
        return Collections.emptyList();
    }
}
```

2. Registrar no `NotificationService`:
```java
handlers.add(new RestaurantNotificationHandler());
```

### Adicionar Novo Tipo de Evento

Para notificar sobre check-out:
1. Criar `ReservationCheckedOutEvent`
2. Emitir evento no `ReservationService`
3. Handlers interessados implementam lógica para o novo evento
