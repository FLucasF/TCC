# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Requisitos

- Java 21
- PostgreSQL 18
- Maven

## Compilação

```bash
mvn clean package
```

Isto gerará um JAR executável em `target/reservations-1.0.0.jar`.

## Execução

Configure as variáveis de ambiente:

- `DB_URL`: URL de conexão JDBC ao PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT`: Porta do servidor (opcional, padrão: 7070)

Execute o JAR:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=senha
export PORT=7070

java -jar target/reservations-1.0.0.jar
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Realiza check-in
- `POST /reservations/{id}/check-out` - Realiza check-out
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Lista mensagens enviadas

### Relatórios (CSV)
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro
- `GET /reports/tourism-tax?month=2027-07` - Relatório de taxa de turismo
- `GET /reports/rate-plans?month=2027-07` - Relatório por plano de tarifa

## Quartos Disponíveis

- **101, 102**: STANDARD - 2 pessoas - R$ 249,90/noite
- **201, 202**: DELUXE - 3 pessoas - R$ 389,90/noite
- **301**: SUITE - 4 pessoas - R$ 579,90/noite

## Regras de Negócio

### Alta Temporada
- 15 de dezembro até fim de fevereiro
- Todo o mês de julho
- Acréscimo de 20% na diária

### Planos de Tarifa
- **STANDARD**: Preço normal, com reembolso
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso

### Estados da Reserva
- **PENDING**: Reserva criada, aguardando pagamento
- **CONFIRMED**: Reserva confirmada (paga)
- **CHECKED_IN**: Check-in realizado
- **CHECKED_OUT**: Check-out realizado
- **CANCELLED**: Reserva cancelada
