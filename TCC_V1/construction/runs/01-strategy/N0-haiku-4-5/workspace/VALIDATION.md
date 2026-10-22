# Validação de Cálculos - Pousada Maré Alta

Este documento valida que os cálculos de pricing implementados estão corretos conforme as especificações.

## Exemplos Validados

### 1. Convênio TECNOSUL (CORPORATE) - Deluxe

**Entrada:** DELUXE, 10/03/2027 a 12/03/2027, TECNOSUL (12% desconto)

**Análise:**
- Março não está em temporada alta (temporada alta: 15/12-29/02, 01/01-28/02, julho)
- Noites: 2 (10/03 a 12/03)
- Tarifa: R$ 389,90/noite
- Subtotal: 389,90 × 2 = 779,80 (sem aumento)
- Desconto: 779,80 × 12% = 93,576 → **93,58** (arredondado para par)
- Total: 779,80 - 93,58 = **686,22** ✓

**Arredondamento HALF_EVEN verificação:**
- 93,576: o dígito a descartar é 6 (>5), arredonda para cima
- Mas 93,57 (ímpar em centésimos) vs 93,58 (par)
- Com HALF_EVEN, quando 6 > 5, arredonda para 93,58 ✓

### 2. Weekend Package (WEEKEND_PACKAGE) - Deluxe

**Entrada:** DELUXE, sexta 12/03/2027 a domingo 14/03/2027

**Análise:**
- Preço fixo DELUXE: R$ 820,00
- Sem desconto (preço já é fechado)
- Noites: 2
- Subtotal: R$ 820,00
- Total: R$ 820,00 ✓
- Se cancelado: refund = 820,00 ÷ 2 = R$ 410,00 ✓

**Validação de datas:**
- 12/03/2027 deve ser sexta-feira ✓
- 14/03/2027 deve ser domingo (checkout segunda 15/03) ✓

### 3. Long Stay (LONG_STAY) - Standard

**Entrada:** STANDARD, 01/03/2027 a 08/03/2027 (7 noites), LONG_STAY (10% desconto)

**Análise:**
- Março não está em temporada alta
- Noites: 7 (01/03 a 08/03)
- Tarifa: R$ 249,90/noite
- Subtotal: 249,90 × 7 = 1.749,30
- Desconto: 1.749,30 × 10% = 174,93
- Total: 1.749,30 - 174,93 = **1.574,37** ✓

### 4. Standard com Temporada Alta

**Entrada:** DELUXE, 10/07/2027 a 12/07/2027, STANDARD (sem desconto)

**Análise:**
- Julho é temporada alta (aumento 20%)
- Noites: 2 (10/07 a 12/07)
- Tarifa base: R$ 389,90
- Tarifa com temporada: 389,90 × 1,20 = 467,88
- Subtotal: 467,88 × 2 = 935,76
- Desconto: 0,00
- Total: 935,76

### 5. Non-Refundable

**Entrada:** STANDARD, 10/03/2027 a 12/03/2027, NON_REFUNDABLE (15% desconto)

**Análise:**
- Março não está em temporada alta
- Noites: 2
- Tarifa: R$ 249,90/noite
- Subtotal: 249,90 × 2 = 499,80
- Desconto: 499,80 × 15% = 74,97
- Total: 499,80 - 74,97 = 424,83
- Se cancelado: refund = R$ 0,00 (não reembolsável) ✓

## Validação de Arredondamento

O sistema usa arredondamento HALF_EVEN para centavos.

### Exemplos de Arredondamento

| Valor | Arredondado | Motivo |
|-------|-------------|--------|
| 93,575 | 93,58 | 5 → arredonda para par (8 é par) |
| 93,585 | 93,58 | 5 → arredonda para par (8 é par) |
| 93,565 | 93,56 | 5 → arredonda para par (6 é par) |
| 93,574 | 93,57 | 4 → arredonda para baixo |
| 93,576 | 93,58 | 6 → arredonda para cima |

## Validação de Temporada Alta

A temporada alta inclui:

1. **15 de dezembro até fim de fevereiro**
   - 15/12 a 31/12
   - 01/01 a 28/02 (ou 29/02 em bissextos)

2. **Julho inteiro**
   - 01/07 a 31/07

### Exemplo de Verificação

```
01/03/2027: não é temporada alta ✓
10/07/2027: é temporada alta (julho) ✓
15/12/2027: é temporada alta ✓
14/12/2027: não é temporada alta ✓
01/02/2027: é temporada alta ✓
01/03/2027: não é temporada alta ✓
```

## Validação de Planos de Tarifa

### Validações Implementadas

1. **CORPORATE**
   - ✓ Requer companyCode
   - ✓ Códigos válidos: TECNOSUL, AGROVALE, FARMAVIDA
   - ✓ Nega se código inválido (400 INVALID_REQUEST)

2. **WEEKEND_PACKAGE**
   - ✓ Requer entrada na sexta-feira
   - ✓ Requer saída no domingo (checkout segunda)
   - ✓ Exatamente 2 noites
   - ✓ Nega se não sexta-domingo (400 INVALID_REQUEST)

3. **LONG_STAY**
   - ✓ Requer mínimo 7 noites
   - ✓ Nega se menos de 7 noites (400 INVALID_REQUEST)

4. **STANDARD & NON_REFUNDABLE**
   - ✓ Aceitam qualquer data válida

## Validação de Reembolsos

| Plano | Pendente | Confirmado | Cancelado |
|-------|----------|-----------|-----------|
| STANDARD | R$ 0 | 100% | 100% |
| NON_REFUNDABLE | R$ 0 | 0% | 0% |
| CORPORATE | R$ 0 | 100% | 100% |
| WEEKEND_PACKAGE | R$ 0 | 50% | 50% |
| LONG_STAY | R$ 0 | 100% | 100% |

## Validação de Capacidade

| Quarto | Tipo | Capacidade |
|--------|------|-----------|
| 101, 102 | STANDARD | 2 |
| 201, 202 | DELUXE | 3 |
| 301 | SUITE | 4 |

- ✓ Rejeita se guests > capacity (422 CAPACITY_EXCEEDED)
- ✓ Rejeita se guests < 1 (400 INVALID_REQUEST)

## Validação de Disponibilidade

- ✓ Rejeita se quarto já tem reserva não-cancelada na mesma noite (409 ROOM_UNAVAILABLE)
- ✓ Conta reservas PENDING, CONFIRMED, CHECKED_IN
- ✓ Ignora CANCELLED e CHECKED_OUT

## Validação de Transições de Estado

### De PENDING
- ✓ confirmation → CONFIRMED
- ✓ cancellation → CANCELLED
- ✗ check-in (409 OPERATION_NOT_ALLOWED)
- ✗ check-out (409 OPERATION_NOT_ALLOWED)

### De CONFIRMED
- ✗ confirmation (409 OPERATION_NOT_ALLOWED)
- ✓ cancellation → CANCELLED
- ✓ check-in → CHECKED_IN
- ✗ check-out (409 OPERATION_NOT_ALLOWED)

### De CHECKED_IN
- ✗ confirmation (409 OPERATION_NOT_ALLOWED)
- ✗ cancellation (409 OPERATION_NOT_ALLOWED)
- ✗ check-in (409 OPERATION_NOT_ALLOWED)
- ✓ check-out → CHECKED_OUT

### De CHECKED_OUT
- ✗ Qualquer operação (status terminal)

### De CANCELLED
- ✗ Qualquer operação (status terminal)

## Validação de Emails

- ✓ Enviado na confirmação com corpo correto
- ✓ Armazenado na tabela messages
- ✓ Contém comprovante formatado
- ✓ Assunto: "Reserva confirmada - Pousada Maré Alta"

## Validação de Relatórios

### Occupancy Report

- ✓ CSV com headers: room,type,reservation,check_in,check_out
- ✓ Inclui apenas CONFIRMED, CHECKED_IN, CHECKED_OUT
- ✓ Conta ocupação apenas para noites com check-in <= data < check-out
- ✓ Linha "total" com formato: total,X/5

### Revenue Report

- ✓ CSV com headers: reservation,room,check_in,check_out,status,amount
- ✓ Inclui reservas que começam no mês especificado
- ✓ Calcula amount = amountPaid - refund
- ✓ Inclui CANCELLED com refund != null
- ✓ Linha "total" com suma de amount

## Validação de Formato

### Dates (ISO)
- Entrada: `YYYY-MM-DD` (ex: 2027-07-10)
- Saída em JSON: `YYYY-MM-DD`
- Saída em comprovante: `DD/MM/YYYY` (ex: 10/07/2027)

### Currency
- No JSON: `123.45` (ponto como decimal)
- No CSV: `123.45`
- No comprovante: `R$ 1.234,56` (ponto como milhar, vírgula como decimal)

## Testes de Erro

### 400 INVALID_REQUEST
- [ ] Campos obrigatórios ausentes
- [ ] Formato de data inválido
- [ ] guests < 1
- [ ] Plano de tarifa inválido
- [ ] CORPORATE sem companyCode
- [ ] CORPORATE com companyCode inválido
- [ ] WEEKEND_PACKAGE sem sexta-domingo
- [ ] LONG_STAY com < 7 noites

### 404 ROOM_NOT_FOUND
- [ ] GET /rooms para quarto inexistente
- [ ] POST /reservations com roomNumber inválido

### 404 RESERVATION_NOT_FOUND
- [ ] GET /reservations/{id} com ID inválido
- [ ] POST /reservations/{id}/* com ID inválido

### 409 ROOM_UNAVAILABLE
- [ ] POST /reservations com quarto ocupado

### 409 OPERATION_NOT_ALLOWED
- [ ] confirmation em CONFIRMED
- [ ] check-in em PENDING
- [ ] check-out em CONFIRMED
- [ ] cancellation em CHECKED_OUT

### 422 CAPACITY_EXCEEDED
- [ ] guests > roomCapacity
