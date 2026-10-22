# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Requisitos

- Java 21
- Maven 3.8+
- PostgreSQL 18

## Configuração

O sistema utiliza as seguintes variáveis de ambiente:

- `DB_URL`: URL de conexão JDBC com o PostgreSQL (padrão: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados (padrão: `postgres`)
- `DB_PASSWORD`: Senha do banco de dados (padrão: `postgres`)
- `PORT`: Porta HTTP do servidor (padrão: `7070`)

## Build

```bash
mvn package
```

Gera um JAR executável em `target/reservations-1.0.0.jar`.

## Execução

```bash
java -jar target/reservations-1.0.0.jar
```

Ou com variáveis de ambiente:

```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=postgres \
DB_PASSWORD=senha \
PORT=8080 \
java -jar target/reservations-1.0.0.jar
```

## Testes

```bash
mvn verify
```

Os testes de integração requerem um PostgreSQL rodando em `localhost:5432` com um banco chamado `pousada_test`.

## API

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens

- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro (CSV)

## Planos de Tarifa

1. **STANDARD**: Tarifa normal, reembolso total
2. **NON_REFUNDABLE**: 15% de desconto, sem reembolso
3. **CORPORATE**: Desconto por convênio (requer `companyCode`)
4. **WEEKEND_PACKAGE**: Pacote de fim de semana (sexta a domingo)
5. **LONG_STAY**: Estadia longa (7+ noites), 10% de desconto
