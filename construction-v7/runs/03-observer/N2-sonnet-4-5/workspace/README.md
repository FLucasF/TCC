# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Tecnologias

- Java 21
- Javalin 6.7.0
- Maven
- Padrão Observer (GoF) para notificações

## Build e Execução

```bash
# Compilar e executar testes
mvn verify

# Gerar o JAR executável
mvn package

# Executar o sistema
java -jar target/reservations-1.0.0.jar

# Configurar porta (opcional, padrão: 7070)
PORT=8080 java -jar target/reservations-1.0.0.jar
```

## Arquitetura

O sistema utiliza o **padrão Observer** para gerenciar as notificações. Quando uma reserva é confirmada ou cancelada, diferentes departamentos são notificados automaticamente:

- **GuestNotificationObserver**: Envia email de confirmação para o hóspede
- **HousekeepingNotificationObserver**: Notifica a governança sobre reservas confirmadas/canceladas
- **FinanceNotificationObserver**: Notifica o financeiro sobre pagamentos e reembolsos

### Adicionar novos observadores

Para adicionar novos departamentos (restaurante, transfer, etc.), basta:

1. Criar uma nova classe que implementa `ReservationObserver`
2. Registrar o observer no `Application.java`:

```java
eventPublisher.attach(new RestaurantNotificationObserver(emailService));
```

Não é necessário modificar nenhuma outra parte do código. Essa organização garante que o sistema cresce sem quebrar o que já funciona.

## APIs Disponíveis

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Criar reserva
- `GET /reservations/{id}` - Consultar reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `POST /reservations/{id}/check-in` - Fazer check-in
- `POST /reservations/{id}/check-out` - Fazer check-out
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Listar emails enviados

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - Relatório financeiro (CSV)

## Exemplo de Uso

```bash
# Criar uma reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Confirmar a reserva (ID 1)
curl -X POST http://localhost:7070/reservations/1/confirmation

# Ver emails enviados
curl http://localhost:7070/messages
```

## Regras de Negócio

- Alta temporada: 15/dez a 28/fev e todo mês de julho (+20% no valor da diária)
- Plano não reembolsável: 15% de desconto no total
- Arredondamento: HALF_EVEN (meio para o par) a cada cálculo
- Cancelamento: plano STANDARD devolve tudo, NON_REFUNDABLE não devolve nada
