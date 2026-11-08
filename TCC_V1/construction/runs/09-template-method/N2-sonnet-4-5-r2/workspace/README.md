# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Tecnologias

- Java 21
- Javalin 6.7.0
- PostgreSQL 18
- Maven

## Compilação

```bash
mvn package
```

O comando gera um JAR executável em `target/pousada-reservas-1.0.0.jar`.

## Execução

### Com Docker Compose (recomendado para testes)

```bash
docker-compose up
```

O sistema estará disponível em `http://localhost:7070`.

### Manual

1. Configure um banco PostgreSQL 18

2. Execute o sistema passando as variáveis de ambiente:

```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=postgres \
DB_PASSWORD=postgres \
PORT=7070 \
java -jar target/pousada-reservas-1.0.0.jar
```

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria uma reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out e retorna comprovante
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens

- `GET /messages` - Lista mensagens enviadas

### Relatórios (CSV)

- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro
- `GET /reports/tourism-tax?month=2027-07` - Relatório de taxa de turismo
- `GET /reports/rate-plans?month=2027-07` - Relatório por plano de tarifa

## Exemplo de Uso

```bash
# Criar uma reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Confirmar a reserva
curl -X POST http://localhost:7070/reservations/1/confirmation

# Gerar relatório de ocupação
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

## Estrutura do Código

O projeto segue uma arquitetura em camadas:

- **domain**: Entidades de domínio (Room, Reservation, Message, Document)
- **repository**: Acesso a dados com JDBC
- **service**: Lógica de negócio
- **report**: Relatórios CSV usando Template Method pattern
- **api**: Camada REST com Javalin

### Template Method Pattern

Os relatórios CSV foram implementados usando o padrão Template Method. A classe abstrata `CsvReport` define a estrutura fixa (título → cabeçalho → dados → total), e cada relatório concreto implementa apenas as partes que variam:

- `OccupancyReport` - Ocupação por noite
- `RevenueReport` - Receita por mês
- `TourismTaxReport` - Taxa de turismo
- `RatePlanReport` - Análise por plano de tarifa

Isso permite que mudanças na estrutura geral (como adicionar data de geração no futuro) afetem todos os relatórios de uma vez, e que novos relatórios sejam adicionados com facilidade.

## Regras de Negócio

- **Quartos**: 5 quartos fixos (101, 102 STANDARD; 201, 202 DELUXE; 301 SUITE)
- **Alta temporada**: 15/dez a fim de fev + julho completo (+20% na diária)
- **Planos de tarifa**:
  - STANDARD: preço normal, reembolsável
  - NON_REFUNDABLE: 15% de desconto, não reembolsável
- **Estados da reserva**: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
  - Cancelamento (CANCELLED) apenas antes do check-in
- **Arredondamento**: HALF_EVEN (meio para o par) em cada passo do cálculo
