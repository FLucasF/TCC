# Exemplo de Uso do Sistema

Este documento demonstra como o sistema calcula os valores e processa as reservas.

## Exemplo 1: Reserva Standard em Alta Temporação

**Dados da reserva:**
- Quarto: 101 (STANDARD - R$ 249,90 por noite)
- Período: 10/07/2027 a 12/07/2027 (2 noites em julho = alta temporada)
- Hóspedes: 2
- Plano: STANDARD (sem desconto)

**Cálculo:**
1. Diária base: R$ 249,90
2. Alta temporada (+20%): R$ 249,90 × 1,20 = R$ 299,88
3. Duas noites: R$ 299,88 + R$ 299,88 = R$ 599,76
4. Desconto: R$ 0,00
5. **Total: R$ 599,76**

**Taxa de turismo:**
- 2 hóspedes × 2 noites × R$ 4,50 = R$ 18,00

## Exemplo 2: Reserva Não Reembolsável Fora de Alta Temporada

**Dados da reserva:**
- Quarto: 201 (DELUXE - R$ 389,90 por noite)
- Período: 10/03/2027 a 12/03/2027 (2 noites em março = temporada normal)
- Hóspedes: 2
- Plano: NON_REFUNDABLE (15% de desconto)

**Cálculo:**
1. Diária base: R$ 389,90
2. Temporada normal: R$ 389,90 (sem acréscimo)
3. Duas noites: R$ 389,90 + R$ 389,90 = R$ 779,80
4. Desconto (15%): R$ 779,80 × 0,15 = R$ 116,97
5. **Total: R$ 779,80 - R$ 116,97 = R$ 662,83**

**Taxa de turismo:**
- 2 hóspedes × 2 noites × R$ 4,50 = R$ 18,00

## Exemplo 3: Suíte em Alta Temporada com Desconto

**Dados da reserva:**
- Quarto: 301 (SUITE - R$ 579,90 por noite)
- Período: 20/12/2027 a 23/12/2027 (3 noites, dec >= 15 = alta temporada)
- Hóspedes: 4
- Plano: NON_REFUNDABLE (15% de desconto)

**Cálculo:**
1. Diária base: R$ 579,90
2. Alta temporada (+20%): R$ 579,90 × 1,20 = R$ 695,88
3. Três noites: R$ 695,88 × 3 = R$ 2.087,64
4. Desconto (15%): R$ 2.087,64 × 0,15 = R$ 313,15 (com arredondamento HALF_EVEN)
5. **Total: R$ 2.087,64 - R$ 313,15 = R$ 1.774,49**

**Taxa de turismo:**
- 4 hóspedes × 3 noites × R$ 4,50 = R$ 54,00

## Fluxo Completo de uma Reserva

### 1. Criação (PENDING)
```bash
POST /reservations
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

Resposta: Status 201, reserva com `id=1` e `status=PENDING`

### 2. Confirmação (CONFIRMED)
```bash
POST /reservations/1/confirmation
```

O sistema:
- Muda o status para CONFIRMED
- Define `amountPaid = total` (R$ 599,76)
- Envia e-mail de confirmação com o comprovante
- Guarda a mensagem na lista de mensagens enviadas

### 3. Check-in (CHECKED_IN)
```bash
POST /reservations/1/check-in
```

Status muda para CHECKED_IN.

### 4. Check-out (CHECKED_OUT)
```bash
POST /reservations/1/check-out
```

Retorna o comprovante:
```
Comprovante de reserva nº 1
Hóspede: João Silva (CPF 123.456.789-01)
Quarto: 101 (STANDARD)
Entrada: 10/07/2027 | Saída: 12/07/2027 | 2 diárias
Diárias: R$ 599,76
Total: R$ 599,76
```

### 5. Segunda via
```bash
GET /reservations/1/document
```

Retorna os mesmos dados do comprovante em JSON.

## Cancelamento

### Plano STANDARD (com reembolso)
```bash
POST /reservations/1/cancellation
```

Se a reserva estava CONFIRMED:
- Status muda para CANCELLED
- `refund = amountPaid` (R$ 599,76)
- O dinheiro é devolvido

### Plano NON_REFUNDABLE (sem reembolso)
```bash
POST /reservations/2/cancellation
```

Se a reserva estava CONFIRMED:
- Status muda para CANCELLED
- `refund = 0`
- O dinheiro **não** é devolvido

## Relatórios

### Ocupação (10/07/2027)
```csv
Pousada Maré Alta,occupancy,2027-07-10
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
total,1/5
```

### Receita (Julho/2027)
```csv
Pousada Maré Alta,revenue,2027-07
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CHECKED_OUT,599.76
total,599.76
```

### Taxa de Turismo (Julho/2027)
```csv
Pousada Maré Alta,tourism-tax,2027-07
reservation,room,check_in,check_out,guests,nights,tax
1,101,2027-07-10,2027-07-12,2,2,18.00
total,18.00
```

### Planos de Tarifa (Julho/2027)
```csv
Pousada Maré Alta,rate-plans,2027-07
rate_plan,reservations,amount
STANDARD,1,599.76
NON_REFUNDABLE,0,0.00
total,1,599.76
```

## Validações e Erros

### Capacidade Excedida
```bash
POST /reservations
{
  "roomNumber": 101,
  "guests": 5,  # STANDARD suporta apenas 2
  ...
}
```

Resposta: 422 CAPACITY_EXCEEDED

### Quarto Indisponível
Tentar reservar o quarto 101 de 11/07 a 13/07 quando já existe uma reserva de 10/07 a 12/07:

Resposta: 409 ROOM_UNAVAILABLE

### Operação Não Permitida
Tentar fazer check-in de uma reserva PENDING:

Resposta: 409 OPERATION_NOT_ALLOWED

## Arredondamento HALF_EVEN

O sistema usa o arredondamento "para o par" (banker's rounding):
- 2.125 → 2.12 (arredonda para baixo, par)
- 2.135 → 2.14 (arredonda para cima, par)
- 2.145 → 2.14 (arredonda para baixo, par)
- 2.155 → 2.16 (arredonda para cima, par)

Isso minimiza o viés de arredondamento em grandes volumes de cálculos.
