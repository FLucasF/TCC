# Documentação da API - Pousada Maré Alta

## Base URL
`http://localhost:7070` (ou a porta configurada)

## Formato de Datas
- **Formato de entrada**: `yyyy-MM-dd` (ex: `2027-07-10`)
- **Formato de saída**: `dd/MM/yyyy` (ex: `10/07/2027`)
- **Formato ISO para relatórios**: `yyyy-MM-dd` (ex: `2027-07-10`)

## Formato de Valores Monetários
- **JSON**: Número com 2 casas decimais (ex: `599.76`)
- **CSV**: Número com ponto como separador decimal (ex: `599.76`)
- **Exibição**: Com símbolo R$ e formatação brasileira (ex: `R$ 599,76`)

## Endpoints

### Quartos

#### GET /rooms
Retorna todos os quartos disponíveis.

**Resposta (200 OK):**
```json
[
  {
    "number": 101,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  },
  ...
]
```

---

### Reservas

#### POST /reservations
Cria uma nova reserva (status = PENDING).

**Body:**
```json
{
  "roomNumber": 101,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

**Resposta (201 CREATED):**
```json
{
  "id": 1,
  "roomNumber": 101,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 599.76,
  "discount": 0.00,
  "total": 599.76,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

**Erros:**
- `400 INVALID_REQUEST`: Campo faltando, datas inválidas, hóspedes < 1
- `404 ROOM_NOT_FOUND`: Quarto não existe
- `409 ROOM_UNAVAILABLE`: Quarto indisponível para as datas
- `422 CAPACITY_EXCEEDED`: Mais hóspedes que a capacidade do quarto

---

#### GET /reservations/{id}
Retorna os dados da reserva.

**Resposta (200 OK):**
```json
{
  "id": 1,
  "roomNumber": 101,
  "guestName": "João Silva",
  ...
}
```

**Erros:**
- `404 RESERVATION_NOT_FOUND`: Reserva não existe

---

#### POST /reservations/{id}/confirmation
Confirma uma reserva pendente (muda status para CONFIRMED).
Envia e-mail de confirmação para o hóspede.

**Body:**
```json
{
  "amountPaid": 599.76
}
```

**Resposta (200 OK):**
```json
{
  "id": 1,
  "status": "CONFIRMED",
  "amountPaid": 599.76,
  ...
}
```

**Erros:**
- `404 RESERVATION_NOT_FOUND`: Reserva não existe
- `409 OPERATION_NOT_ALLOWED`: Reserva não está pendente

---

#### POST /reservations/{id}/cancellation
Cancela uma reserva (muda status para CANCELLED).

**Body:** (vazio)

**Resposta (200 OK):**
```json
{
  "id": 1,
  "status": "CANCELLED",
  "refund": 599.76,
  ...
}
```

**Erros:**
- `404 RESERVATION_NOT_FOUND`: Reserva não existe
- `409 OPERATION_NOT_ALLOWED`: Reserva não pode ser cancelada (já fez check-out ou foi cancelada)

**Reembolso:**
- Plano STANDARD: 100% do valor pago
- Plano NON_REFUNDABLE: 0% (sem reembolso)
- Reserva PENDING: Sem reembolso (nada foi pago)

---

#### POST /reservations/{id}/check-in
Realiza o check-in da reserva (muda status para CHECKED_IN).

**Body:** (vazio)

**Resposta (200 OK):**
```json
{
  "id": 1,
  "status": "CHECKED_IN",
  ...
}
```

**Erros:**
- `404 RESERVATION_NOT_FOUND`: Reserva não existe
- `409 OPERATION_NOT_ALLOWED`: Reserva não está confirmada

---

#### POST /reservations/{id}/check-out
Realiza o check-out da reserva (muda status para CHECKED_OUT).
Retorna o comprovante.

**Body:** (vazio)

**Resposta (200 OK):**
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  "guestName": "João Silva",
  "guestCpf": "12345678901",
  "roomNumber": 101,
  "roomType": "STANDARD",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "nights": 2,
  "subtotal": 599.76,
  "discount": 0.00,
  "total": 599.76
}
```

**Erros:**
- `404 RESERVATION_NOT_FOUND`: Reserva não existe
- `409 OPERATION_NOT_ALLOWED`: Reserva não fez check-in

---

#### GET /reservations/{id}/document
Retorna a segunda via do comprovante (disponível apenas para reservas confirmadas, com check-in ou com check-out).

**Resposta (200 OK):**
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  ...
}
```

**Erros:**
- `404 RESERVATION_NOT_FOUND`: Reserva não existe
- `409 OPERATION_NOT_ALLOWED`: Comprovante não disponível para este status

---

### Mensagens

#### GET /messages
Retorna todas as mensagens enviadas (e-mails).

**Resposta (200 OK):**
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "joao@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, João Silva!\n\nSua reserva na Pousada Maré Alta está confirmada...\n\nA entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!"
  },
  ...
]
```

---

### Relatórios

#### GET /reports/occupancy?date=2027-07-10
Retorna o relatório de ocupação do dia em CSV.
Mostra quais quartos estão ocupados naquela noite.

**Resposta (200 OK):**
```
Pousada Maré Alta,occupancy,2027-07-10
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
total,1/5
```

**Parâmetros:**
- `date`: Data no formato `yyyy-MM-dd` (obrigatório)

---

#### GET /reports/revenue?month=2027-07
Retorna o relatório financeiro do mês em CSV.
Mostra as reservas que começam no mês e o dinheiro que ficou com a pousada.

**Resposta (200 OK):**
```
Pousada Maré Alta,revenue,2027-07
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CONFIRMED,599.76
total,,,,,599.76
```

**Parâmetros:**
- `month`: Mês no formato `yyyy-MM` (obrigatório)

---

#### GET /reports/tourism-tax?month=2027-07
Retorna o relatório de taxa de turismo do mês em CSV.
R$ 4,50 por hóspede por noite.

**Resposta (200 OK):**
```
Pousada Maré Alta,tourism-tax,2027-07
reservation,room,check_in,check_out,guests,nights,tax
1,101,2027-07-10,2027-07-12,2,2,18.00
total,,,,,27.00
```

**Parâmetros:**
- `month`: Mês no formato `yyyy-MM` (obrigatório)

---

#### GET /reports/rate-plans?month=2027-07
Retorna o relatório de planos de tarifa do mês em CSV.
Compara a receita entre os planos STANDARD e NON_REFUNDABLE.

**Resposta (200 OK):**
```
Pousada Maré Alta,rate-plans,2027-07
rate_plan,reservations,amount
STANDARD,1,599.76
NON_REFUNDABLE,0,0.00
total,1,599.76
```

**Parâmetros:**
- `month`: Mês no formato `yyyy-MM` (obrigatório)

---

## Códigos de Erro

| Status | Código | Significado |
|--------|--------|-------------|
| 400 | INVALID_REQUEST | Requisição inválida (campos faltando, datas inválidas, etc) |
| 404 | ROOM_NOT_FOUND | Quarto não encontrado |
| 404 | RESERVATION_NOT_FOUND | Reserva não encontrada |
| 409 | ROOM_UNAVAILABLE | Quarto já reservado para as datas |
| 409 | OPERATION_NOT_ALLOWED | Operação não permitida para o status atual |
| 422 | CAPACITY_EXCEEDED | Número de hóspedes excede a capacidade |

---

## Fluxo Típico de Reserva

1. **POST /reservations** → Cria reserva (PENDING)
2. **POST /reservations/{id}/confirmation** → Confirma (CONFIRMED, envia e-mail)
3. **POST /reservations/{id}/check-in** → Check-in (CHECKED_IN)
4. **POST /reservations/{id}/check-out** → Check-out (CHECKED_OUT, retorna comprovante)

---

## Quartos Disponíveis

| Número | Tipo | Capacidade | Diária |
|--------|------|-----------|--------|
| 101 | STANDARD | 2 | R$ 249,90 |
| 102 | STANDARD | 2 | R$ 249,90 |
| 201 | DELUXE | 3 | R$ 389,90 |
| 202 | DELUXE | 3 | R$ 389,90 |
| 301 | SUITE | 4 | R$ 579,90 |

**Nota:** Na alta temporada (15 dez - 28 fev e julho inteiro), as diárias aumentam 20%.

---

## Planos de Tarifa

| Plano | Desconto | Reembolso |
|-------|----------|-----------|
| STANDARD | 0% | 100% |
| NON_REFUNDABLE | 15% | 0% |
