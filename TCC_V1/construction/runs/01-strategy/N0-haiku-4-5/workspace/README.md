# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Requisitos

- Java 21+
- PostgreSQL 18+
- Maven 3.6+

## Instalação e Execução

### 1. Build

```bash
mvn clean package
```

Isso gera um arquivo `target/mare-alta-1.0.0.jar` que pode ser executado como um uber JAR (com todas as dependências incluídas).

### 2. Configuração do Banco de Dados

Defina as variáveis de ambiente:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="sua_senha"
export PORT=7070
```

### 3. Execução

```bash
java -jar target/mare-alta-1.0.0.jar
```

O sistema:
- Criará automaticamente o schema PostgreSQL na primeira execução
- Inicializará os 5 quartos da pousada
- Iniciará o servidor na porta especificada (padrão: 7070)

## Arquitetura

### Bancos de Dados

- **rooms**: Tabela de quartos (imutável, pré-carregada)
- **reservations**: Tabela de reservas com todos os dados
- **messages**: Tabela de mensagens de email enviadas

### Quartos

```
101, 102: STANDARD    - 2 pessoas - R$ 249,90/noite
201, 202: DELUXE      - 3 pessoas - R$ 389,90/noite
301:      SUITE       - 4 pessoas - R$ 579,90/noite
```

### Planos de Tarifa

#### 1. STANDARD
- Sem desconto
- Reembolso total em caso de cancelamento
- Temporada alta (15/12 a 28/02 e julho): +20% nas diárias

#### 2. NON_REFUNDABLE
- 15% de desconto
- Sem reembolso em cancelamento
- Temporada alta: +20% nas diárias

#### 3. CORPORATE
- Desconto conforme a empresa:
  - TECNOSUL: 12%
  - AGROVALE: 18%
  - FARMAVIDA: 25%
- Reembolso total em caso de cancelamento
- Requer campo `companyCode`
- Temporada alta: +20% nas diárias

#### 4. WEEKEND_PACKAGE
- Entrada: sexta-feira, Saída: domingo (exatamente 2 noites)
- Preço fixo (sem variação de temporada):
  - STANDARD: R$ 520,00
  - DELUXE: R$ 820,00
  - SUITE: R$ 1.200,00
- Reembolso de 50% em caso de cancelamento

#### 5. LONG_STAY
- Mínimo 7 noites
- 10% de desconto
- Reembolso total em caso de cancelamento
- Temporada alta: +20% nas diárias

### Estados da Reserva

```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
    ↓
CANCELLED (antes do check-in)
```

### Arredondamento

Todos os valores monetários usam arredondamento "meio para par" (HALF_EVEN) para centavos.

## API REST

### GET /rooms

Retorna lista de quartos com todas as informações.

```json
[
  {
    "number": 101,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  }
]
```

### POST /reservations

Cria uma nova reserva (status inicial: PENDING).

**Request:**
```json
{
  "roomNumber": 101,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "123.456.789-00",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "companyCode": "TECNOSUL"  // Opcional, apenas para CORPORATE
}
```

**Response (201 Created):**
```json
{
  "id": 7,
  "roomNumber": 101,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "123.456.789-00",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 549.80,
  "discount": 0.00,
  "total": 549.80,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

### GET /reservations/{id}

Retorna os dados de uma reserva.

### POST /reservations/{id}/confirmation

Confirma uma reserva pendente. Requer PENDING status.

- Define status para CONFIRMED
- Define amountPaid para o total da reserva
- Envia email de confirmação ao hóspede
- Retorna a reserva atualizada

### POST /reservations/{id}/cancellation

Cancela uma reserva. Requer PENDING ou CONFIRMED status.

- Define status para CANCELLED
- Calcula reembolso conforme o plano de tarifa
- Define refund para o valor a devolver
- Retorna a reserva atualizada

### POST /reservations/{id}/check-in

Faz check-in de uma reserva. Requer CONFIRMED status.

- Define status para CHECKED_IN
- Retorna a reserva atualizada

### POST /reservations/{id}/check-out

Faz check-out de uma reserva. Requer CHECKED_IN status.

- Define status para CHECKED_OUT
- Retorna o comprovante (recibo)

**Response:**
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 7,
  "guestName": "João Silva",
  "guestCpf": "123.456.789-00",
  "roomNumber": 101,
  "roomType": "STANDARD",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "nights": 2,
  "subtotal": 549.80,
  "discount": 0.00,
  "total": 549.80
}
```

### GET /reservations/{id}/document

Retorna a segunda via do comprovante. Disponível para reservas CONFIRMED, CHECKED_IN ou CHECKED_OUT.

### GET /messages

Retorna lista de todas as mensagens de email enviadas.

```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "joao@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "..."
  }
]
```

### GET /reports/occupancy?date=YYYY-MM-DD

Relatório de ocupação em formato CSV para uma data específica.

```
room,type,reservation,check_in,check_out
101,STANDARD,7,2027-07-10,2027-07-12
total,1/5
```

### GET /reports/revenue?month=YYYY-MM

Relatório financeiro em formato CSV para um mês específico.

```
reservation,room,check_in,check_out,status,amount
7,101,2027-07-10,2027-07-12,CHECKED_OUT,549.80
total,,,,-,549.80
```

## Respostas de Erro

### 400 Bad Request - INVALID_REQUEST
- Campos obrigatórios ausentes
- Formato de data inválido
- Menos de 1 hóspede
- Plano de tarifa inválido
- Empresa inexistente (CORPORATE)
- Datas inválidas para WEEKEND_PACKAGE
- Menos de 7 noites para LONG_STAY

### 404 Not Found
- `ROOM_NOT_FOUND`: Quarto inexistente
- `RESERVATION_NOT_FOUND`: Reserva inexistente

### 409 Conflict
- `ROOM_UNAVAILABLE`: Quarto ocupado nessas datas
- `OPERATION_NOT_ALLOWED`: Transição de estado não permitida

### 422 Unprocessable Entity - CAPACITY_EXCEEDED
- Número de hóspedes excede capacidade do quarto

## Exemplos de Uso

### Teste Rápido com cURL

```bash
# Listar quartos
curl http://localhost:7070/rooms

# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "987.654.321-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-13",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Confirmar reserva
curl -X POST http://localhost:7070/reservations/1/confirmation

# Check-in
curl -X POST http://localhost:7070/reservations/1/check-in

# Check-out
curl -X POST http://localhost:7070/reservations/1/check-out

# Ver mensagens
curl http://localhost:7070/messages

# Relatório de ocupação
curl http://localhost:7070/reports/occupancy?date=2027-07-10

# Relatório financeiro
curl http://localhost:7070/reports/revenue?month=2027-07
```

## Testes

```bash
mvn verify
```

## Notas de Implementação

### Padrão de Arredondamento

Todos os cálculos monetários usam `BigDecimal` com arredondamento HALF_EVEN:
- Cada noite é arredondada individualmente
- A subtotal é a soma das noites
- O desconto é arredondado
- O total é arredondado

### Cálculo de Diárias em Temporada Alta

A temporada alta inclui:
- 15 de dezembro a 28/29 de fevereiro
- Mês de julho inteiro

Durante esse período, a diária sofre um aumento de 20%.

### Validações de Plano de Tarifa

Cada plano de tarifa valida:
- **CORPORATE**: Requer companyCode válido
- **WEEKEND_PACKAGE**: Deve ser exatamente sexta-domingo
- **LONG_STAY**: Mínimo de 7 noites

### Transições de Estado

```
PENDING:
  ✓ confirmation → CONFIRMED
  ✓ cancellation → CANCELLED

CONFIRMED:
  ✓ check-in → CHECKED_IN
  ✓ cancellation → CANCELLED

CHECKED_IN:
  ✓ check-out → CHECKED_OUT

CHECKED_OUT:
  (terminal)

CANCELLED:
  (terminal)
```

## Estrutura de Diretórios

```
/workspace
├── pom.xml
├── src/main/java/com/pousada/
│   ├── Application.java
│   ├── api/
│   │   ├── RoomAPI.java
│   │   ├── ReservationAPI.java
│   │   ├── MessageAPI.java
│   │   └── ReportAPI.java
│   ├── db/
│   │   ├── Database.java
│   │   ├── Room.java
│   │   ├── Reservation.java
│   │   ├── Receipt.java
│   │   └── Message.java
│   ├── service/
│   │   ├── RoomService.java
│   │   ├── ReservationService.java
│   │   ├── MessageService.java
│   │   ├── ReceiptService.java
│   │   ├── RatePlanCalculator.java
│   │   └── Validator.java
│   └── util/
│       ├── PriceCalculator.java
│       ├── DateFormatter.java
│       └── SeasonUtils.java
└── target/
    └── mare-alta-1.0.0.jar
```

## Licença

Desenvolvido para Pousada Maré Alta.
