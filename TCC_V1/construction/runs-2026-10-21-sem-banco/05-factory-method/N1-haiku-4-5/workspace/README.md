# Pousada Maré Alta - Sistema de Reservas

Sistema de reservas para a Pousada Maré Alta, uma pousada de 5 quartos no litoral.

## Construção

```bash
mvn clean package
```

Isso gera um arquivo JAR executável em `target/reservas.jar`.

## Execução

```bash
java -jar target/reservas.jar
```

O sistema escuta na porta definida pela variável de ambiente `PORT`, ou na porta 7070 se não definida:

```bash
PORT=8080 java -jar target/reservas.jar
```

## Testes

```bash
mvn verify
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma reserva
- `GET /reservations/{id}` - Obtém detalhes de uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Realiza check-in
- `POST /reservations/{id}/check-out` - Realiza check-out
- `GET /reservations/{id}/document` - Obtém documento (segunda via)

### Mensagens
- `GET /messages` - Lista mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-03-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-03` - Relatório financeiro (CSV)

## Arquitetura

- **Model**: Classes de domínio (Room, Reservation, Message, Document, GuestIdentification)
- **Service**: Lógica de negócio (PricingService, ReservationService, DocumentGenerator, ReportService)
- **Repository**: Armazenamento em memória
- **Main**: API REST com Javalin

A arquitetura foi organizada para que novos tipos de documentos ou casos de uso possam ser adicionados em um único lugar.
