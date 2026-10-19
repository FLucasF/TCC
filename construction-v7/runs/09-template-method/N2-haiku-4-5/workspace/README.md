# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0.

## Construir e Executar

```bash
# Compilar
mvn clean package

# Executar (porta padrão 7070)
java -jar target/app.jar

# Executar em porta específica
PORT=8080 java -jar target/app.jar
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos com capacidade e diária

### Reservas
- `POST /reservations` - Criar nova reserva (PENDING)
- `GET /reservations/{id}` - Obter detalhes da reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva (PENDING → CONFIRMED)
- `POST /reservations/{id}/check-in` - Check-in (CONFIRMED → CHECKED_IN)
- `POST /reservations/{id}/check-out` - Check-out e gerar comprovante (CHECKED_IN → CHECKED_OUT)
- `POST /reservations/{id}/cancellation` - Cancelar reserva (PENDING/CONFIRMED → CANCELLED)
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Lista de e-mails enviados

### Relatórios (CSV)
- `GET /reports/occupancy?date=YYYY-MM-DD` - Ocupação de uma noite
- `GET /reports/revenue?month=YYYY-MM` - Financeiro do mês
- `GET /reports/tourism-tax?month=YYYY-MM` - Taxa de turismo (R$ 4,50/hóspede/noite)
- `GET /reports/rate-plans?month=YYYY-MM` - Performance dos planos de tarifa

## Regras de Negócio

### Quartos
- 101, 102: STANDARD - 2 pessoas - R$ 249,90/noite
- 201, 202: DELUXE - 3 pessoas - R$ 389,90/noite
- 301: SUITE - 4 pessoas - R$ 579,90/noite

### Temporadas
- **Alta**: 15 dez - 28 fev + julho inteiro (20% acréscimo)
- **Normal**: resto do ano

### Planos de Tarifa
- **STANDARD**: preço normal, reembolso total em cancelamento
- **NON_REFUNDABLE**: 15% desconto, sem reembolso em cancelamento

### Cálculo de Preço
1. Preço por noite = diária × (1,20 se alta temporada, senão 1,00)
2. Subtotal = soma das noites
3. Desconto = subtotal × 15% se NON_REFUNDABLE, senão 0
4. Total = subtotal - desconto
5. Arredondamento: "meio para par" (banker's rounding) a cada passo

### Estados da Reserva
- **PENDING** - Criada, não paga
- **CONFIRMED** - Paga
- **CHECKED_IN** - Hospede entrou
- **CHECKED_OUT** - Hospede saiu
- **CANCELLED** - Cancelada

### Reembolso em Cancelamento
- STANDARD confirmada: reembolso total
- NON_REFUNDABLE: sem reembolso
- PENDING (não paga): sem reembolso

## Validações

- Menos de 1 hóspede ou mais que a capacidade do quarto: `422 CAPACITY_EXCEEDED`
- Quarto já reservado na mesma noite: `409 ROOM_UNAVAILABLE`
- Operação não permitida no estado atual: `409 OPERATION_NOT_ALLOWED`
- Quarto inexistente: `404 ROOM_NOT_FOUND`
- Reserva inexistente: `404 RESERVATION_NOT_FOUND`
- Datas ou formato inválido: `400 INVALID_REQUEST`

## Exemplo de Uso

```bash
# Listar quartos
curl http://localhost:7070/rooms

# Criar reserva
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

# Confirmar reserva
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 779.80}'

# Check-in
curl -X POST http://localhost:7070/reservations/1/check-in

# Check-out e obter comprovante
curl -X POST http://localhost:7070/reservations/1/check-out

# Gerar relatórios
curl "http://localhost:7070/reports/revenue?month=2027-03"
curl "http://localhost:7070/reports/tourism-tax?month=2027-03"
```

## Implementação

- **29 arquivos Java** estruturados em:
  - `model/` - Entidades (Room, Reservation, Message, etc)
  - `service/` - Lógica de negócio
  - `controller/` - Endpoints
  - `dto/` - Data transfer objects
  - `exception/` - Exceções customizadas

- **Dados em memória** - Não persistidos entre reinicializações
- **Sem Spring** - Javalin puro
- **Maven** - Build com assembly JAR executável
