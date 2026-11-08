# Pousada Maré Alta - Sistema de Reservas

Sistema de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL.

## Requisitos

- Java 21
- Maven 3.6+
- PostgreSQL 18

## Configuração

Configure as variáveis de ambiente para conexão com o banco de dados:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=seu_usuario
export DB_PASSWORD=sua_senha
export PORT=7070  # opcional, padrão é 7070
```

O sistema cria automaticamente as tabelas necessárias e popula os 5 quartos da pousada.

## Compilação

```bash
mvn package
```

## Execução

```bash
java -jar target/pousada-mare-alta-1.0.0.jar
```

## Testes

```bash
mvn test
```

## Planos de Tarifa

O sistema suporta 5 planos de tarifa:

- **STANDARD**: Plano normal, reembolso total
- **NON_REFUNDABLE**: 15% desconto, sem reembolso
- **CORPORATE**: Desconto corporativo (TECNOSUL 12%, AGROVALE 18%, FARMAVIDA 25%), reembolso total
- **WEEKEND_PACKAGE**: Pacote fim de semana (sexta→domingo), preço fixo, reembolso de 50%
- **LONG_STAY**: Mínimo 7 noites, 10% desconto, reembolso total

## API REST

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria nova reserva
- `GET /reservations/{id}` - Consulta reserva
- `POST /reservations/{id}/confirmation` - Confirma reserva
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Obtém comprovante (segunda via)

### Mensagens
- `GET /messages` - Lista mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro (CSV)
