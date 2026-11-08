# Sistema de Reservas - Pousada Maré Alta

Sistema de gestão de reservas para a Pousada Maré Alta.

## Requisitos

- Java 21
- PostgreSQL 18
- Maven 3.x

## Compilação

```bash
mvn clean package
```

## Execução

Configure as variáveis de ambiente:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=postgres
export PORT=7070
```

Execute o JAR:

```bash
java -jar target/reservas-1.0.0.jar
```

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Obtém segunda via do comprovante

### Mensagens

- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro
- `GET /reports/tourism-tax?month=2027-07` - Relatório de taxa de turismo
- `GET /reports/rate-plans?month=2027-07` - Relatório de planos de tarifa

## Estrutura do Projeto

- `src/main/java/com/pousadamarealta/`
  - `model/` - Modelos de domínio
  - `repository/` - Acesso a dados
  - `service/` - Lógica de negócio
  - `handler/` - Controllers HTTP
  - `dto/` - Data Transfer Objects
  - `database/` - Gerenciamento do banco de dados

## Regras de Negócio

### Quartos

- 101, 102: STANDARD (2 pessoas, R$ 249,90)
- 201, 202: DELUXE (3 pessoas, R$ 389,90)
- 301: SUITE (4 pessoas, R$ 579,90)

### Alta Temporada (20% a mais)

- 15 de dezembro até fim de fevereiro
- Todo o mês de julho

### Planos de Tarifa

- STANDARD: preço normal, reembolso total ao cancelar
- NON_REFUNDABLE: 15% de desconto, sem reembolso

### Estados da Reserva

- PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- CANCELLED (apenas de PENDING ou CONFIRMED)
