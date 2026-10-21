# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Tecnologias

- Java 21
- Javalin 6.7.0
- Maven

## Build

```bash
mvn clean package
```

## Execução

```bash
# Porta padrão (7070)
java -jar target/reservas-1.0-SNAPSHOT.jar

# Porta customizada
PORT=8080 java -jar target/reservas-1.0-SNAPSHOT.jar
```

## Testes

```bash
mvn verify
```

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria nova reserva
- `GET /reservations/{id}` - Consulta reserva
- `POST /reservations/{id}/confirmation` - Confirma reserva
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens

- `GET /messages` - Lista mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro

## Regras de Negócio

### Quartos

- 101, 102: STANDARD (2 pessoas, R$ 249,90/noite)
- 201, 202: DELUXE (3 pessoas, R$ 389,90/noite)
- 301: SUITE (4 pessoas, R$ 579,90/noite)

### Preços

- Alta temporada (15/12 a fim de fevereiro + julho): +20%
- Plano NON_REFUNDABLE: -15% sobre o total

### Estados da Reserva

- PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- CANCELLED (apenas antes do check-in)

### Notificações

Quando uma reserva é **confirmada**, são enviados e-mails para:
- Hóspede: confirmação com comprovante
- Governança: datas e número de hóspedes
- Financeiro: valor pago

Quando uma reserva confirmada é **cancelada**, são enviados e-mails para:
- Governança: datas liberadas
- Financeiro: valor a devolver (se houver)

## Arquitetura

O sistema utiliza uma arquitetura orientada a eventos para garantir extensibilidade. Novos departamentos podem ser facilmente adicionados registrando novos handlers no `EventService`:

```java
eventService.register(ReservationConfirmedEvent.class, new RestaurantNotificationHandler(messageService));
```

Não é necessário modificar a lógica central do `ReservationService` para adicionar novas notificações.
