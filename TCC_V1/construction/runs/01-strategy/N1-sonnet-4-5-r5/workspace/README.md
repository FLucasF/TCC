# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Requisitos

- Java 21
- Maven
- PostgreSQL 18

## Configuração

Configure as variáveis de ambiente:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=postgres
export PORT=7070
```

## Build

```bash
mvn clean package
```

## Executar

```bash
java -jar target/pousada-reservas-1.0.0.jar
```

## Testes

```bash
mvn verify
```

## API Endpoints

- `GET /rooms` - Lista todos os quartos
- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Busca uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Obtém o comprovante
- `GET /messages` - Lista mensagens enviadas
- `GET /reports/occupancy?date=yyyy-MM-dd` - Relatório de ocupação
- `GET /reports/revenue?month=yyyy-MM` - Relatório financeiro

## Planos de Tarifa

- **STANDARD**: Plano normal, reembolso total
- **NON_REFUNDABLE**: 15% desconto, sem reembolso
- **CORPORATE**: Desconto por empresa (TECNOSUL 12%, AGROVALE 18%, FARMAVIDA 25%)
- **WEEKEND_PACKAGE**: Sexta a domingo, preço fixo
- **LONG_STAY**: 7+ noites, 10% desconto
