# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Requisitos

- Java 21
- PostgreSQL 18
- Maven

## Configuração

Configure as seguintes variáveis de ambiente:

- `DB_URL`: URL JDBC do PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT`: Porta do servidor (opcional, padrão: 7070)

## Compilar e executar

```bash
mvn package
java -jar target/pousada-mare-alta-1.0.0.jar
```

## Testes

```bash
mvn verify
```

## API Endpoints

- `GET /rooms` - Lista os quartos
- `POST /reservations` - Cria uma reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Segunda via do comprovante
- `GET /messages` - Lista mensagens enviadas
- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro

## Planos de Tarifa

- **STANDARD**: Plano normal, reembolso total
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso
- **CORPORATE**: Desconto por empresa (TECNOSUL 12%, AGROVALE 18%, FARMAVIDA 25%), reembolso total
- **WEEKEND_PACKAGE**: Pacote sexta a domingo, preço fixo, reembolso de 50%
- **LONG_STAY**: 7+ noites, 10% de desconto, reembolso total
