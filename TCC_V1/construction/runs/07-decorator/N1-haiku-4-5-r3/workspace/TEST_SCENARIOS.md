# Cenários de Teste - Pousada Maré Alta

Este documento detalha os principais cenários de teste que validam o funcionamento do sistema.

## Testes Unitários

### PriceCalculatorTest ✓
- `testHighSeasonJuly()` - Julho é alta temporada
- `testHighSeasonDecember()` - 15 a 31 de dezembro é alta temporada
- `testHighSeasonJanuary()` - Janeiro é alta temporada
- `testLowSeason()` - Outros meses não são alta temporada
- `testNightlyPrice()` - Preço é multiplicado por 1.20 em alta temporada
- `testRounding()` - Arredondamento HALF_EVEN funciona corretamente
- `testNonRefundableDiscount()` - Desconto de 15% é aplicado corretamente

### EmailValidatorTest ✓
- `testValidEmail()` - Aceita ana@gmail.com, user@example.co.uk, etc.
- `testInvalidEmail()` - Rejeita ana@gmail, ana.gmail.com, @gmail.com, etc.

---

## Cenários de Integração (Manual)

### 1. Fluxo Completo de Reserva Simples

**Setup**: 
- Quarto 101 (STANDARD, 2 pessoas, R$ 249,90)
- Período: 10/03/2027 a 12/03/2027 (2 noites, baixa temporada)
- Plano: STANDARD
- Email válido: carla@gmail.com

**Passos**:
1. ✓ POST /reservations → Reserva criada com status PENDING
   - subtotal: 499.80 (249.90 × 2)
   - discount: 0
   - total: 499.80
   - amountPaid: 0

2. ✓ GET /reservations/1 → Retorna reserva com status PENDING

3. ✓ POST /reservations/1/confirmation
   - Status muda para CONFIRMED
   - amountPaid = 499.80
   - Email enviado para carla@gmail.com
   - Cópia criada para arquivo@pousadamarealta.com.br

4. ✓ GET /messages → Lista 2 mensagens (original + cópia)
   - Mensagem original: SENT
   - Cópia: SENT

5. ✓ POST /reservations/1/check-in → Status muda para CHECKED_IN

6. ✓ POST /reservations/1/check-out
   - Status muda para CHECKED_OUT
   - Retorna comprovante do check-out

7. ✓ GET /reservations/1/document → Retorna segunda via do comprovante

---

### 2. Fluxo com Alta Temporada

**Setup**:
- Quarto 201 (DELUXE, 3 pessoas, R$ 389,90)
- Período: 10/07/2027 a 12/07/2027 (2 noites em julho = alta temporada)
- Plano: STANDARD

**Cálculo esperado**:
```
Preço por noite em alta temporada: 389.90 × 1.20 = 467.88
Subtotal: 467.88 × 2 = 935.76
Desconto: 0 (plano STANDARD)
Total: 935.76
```

**Validação**:
- POST /reservations retorna total: 935.76
- Confirmação e check-out funcionam normalmente

---

### 3. Fluxo com Desconto Non-Refundable

**Setup**:
- Quarto 201 (DELUXE, 3 pessoas)
- Período: 10/03/2027 a 12/03/2027 (2 noites, baixa temporada)
- Plano: NON_REFUNDABLE

**Cálculo esperado**:
```
Preço: 389.90 × 2 = 779.80
Desconto: 779.80 × 0.15 = 116.97
Total: 779.80 - 116.97 = 662.83
```

**Validação**:
- POST /reservations retorna:
  - subtotal: 779.80
  - discount: 116.97
  - total: 662.83

---

### 4. Cancelamento em Diferentes Estados

#### 4a. Cancelamento Pendente
**Setup**: Reserva pendente

**Resultado esperado**:
- Status → CANCELLED
- refund: null (nunca pagou)

#### 4b. Cancelamento Confirmado (STANDARD)
**Setup**: Reserva confirmada, plano STANDARD

**Resultado esperado**:
- Status → CANCELLED
- refund = amountPaid (reembolsa tudo)

#### 4c. Cancelamento Confirmado (NON_REFUNDABLE)
**Setup**: Reserva confirmada, plano NON_REFUNDABLE

**Resultado esperado**:
- Status → CANCELLED
- refund: 0 (não reembolsa)

#### 4d. Cancelamento após Check-in
**Setup**: Reserva em estado CHECKED_IN

**Resultado esperado**:
- Erro 409 OPERATION_NOT_ALLOWED

---

### 5. Validação de Email

#### 5a. Email Válido
**Setup**: 
```json
{
  "guestEmail": "ana@gmail.com",
  ...
}
```

**Resultado esperado**:
- Confirmação envia email SENT
- GET /messages mostra status SENT

#### 5b. Email Inválido (sem ponto)
**Setup**:
```json
{
  "guestEmail": "ana@gmail",
  ...
}
```

**Resultado esperado**:
- Confirmação persiste mensagem como BLOCKED
- GET /messages mostra status BLOCKED

#### 5c. Email Inválido (formato errado)
**Setup**: "ana.gmail.com" (sem @)

**Resultado esperado**:
- GET /messages mostra status BLOCKED

---

### 6. Modo de Teste (HOTEL_TEST_MODE=true)

**Setup**: HOTEL_TEST_MODE=true

#### 6a. Email Válido em Teste
**Setup**:
```json
{
  "guestEmail": "ana@gmail.com",
  ...
}
```

**Resultado esperado**:
- GET /messages retorna mensagem com:
  - to: "testes@pousadamarealta.com.br"
  - subject começa com "[TESTE]"
  - body começa com "Destinatário original: ana@gmail.com"
  - status: SENT
  - **SEM** cópia para arquivo

#### 6b. Email Inválido em Teste
**Setup**:
```json
{
  "guestEmail": "ana@gmail",
  ...
}
```

**Resultado esperado**:
- GET /messages retorna:
  - to: "ana@gmail" (email original, não redirecionado)
  - status: BLOCKED

---

### 7. Validação de Capacidade

#### 7a. Muito Poucos Hóspedes
**Setup**:
```json
{
  "guests": 0,
  ...
}
```

**Resultado esperado**:
- POST /reservations → 400 INVALID_REQUEST

#### 7b. Muitos Hóspedes
**Setup**:
```json
{
  "roomNumber": 101,  // STANDARD, capacidade 2
  "guests": 3,       // Muito!
  ...
}
```

**Resultado esperado**:
- POST /reservations → 422 CAPACITY_EXCEEDED

---

### 8. Validação de Datas

#### 8a. Data de Saída Antes da Entrada
**Setup**:
```json
{
  "checkIn": "2027-03-12",
  "checkOut": "2027-03-10"  // Antes!
  ...
}
```

**Resultado esperado**:
- POST /reservations → 400 INVALID_REQUEST

#### 8b. Data de Saída Igual à Entrada
**Setup**:
```json
{
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-10"  // Igual!
  ...
}
```

**Resultado esperado**:
- POST /reservations → 400 INVALID_REQUEST

---

### 9. Validação de Disponibilidade

#### 9a. Quarto Não Existe
**Setup**:
```json
{
  "roomNumber": 999,
  ...
}
```

**Resultado esperado**:
- POST /reservations → 404 ROOM_NOT_FOUND

#### 9b. Quarto Já Reservado
**Setup**:
1. Criar reserva confirmada para 10/03 a 12/03 no quarto 101
2. Tentar criar outra para 11/03 a 13/03 no mesmo quarto

**Resultado esperado**:
- Segunda requisição → 409 ROOM_UNAVAILABLE

#### 9c. Overlapping Não Inclusive
**Setup**:
1. Reserva 1: 10/03 a 12/03 (check-out 12/03)
2. Reserva 2: 12/03 a 14/03 (check-in 12/03)

**Resultado esperado**:
- Ambas devem ser aceitas (sem overlap, check-out é liberação do quarto)

---

### 10. Relatórios

#### 10a. Relatório de Ocupação
**Setup**:
- 10/03/2027: Reservas confirmadas no quarto 101 e 201
- Quartos: 101, 102, 201, 202, 301

**Request**: GET /reports/occupancy?date=2027-03-10

**Resposta esperada**:
```csv
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-03-10,2027-03-12
201,DELUXE,2,2027-03-10,2027-03-12
total,2/5
```

#### 10b. Relatório de Faturamento
**Setup**:
- Março/2027: 2 reservas confirmadas, 1 cancelada NON_REFUNDABLE (reembolso 0)

**Request**: GET /reports/revenue?month=2027-03

**Resposta esperada**:
```csv
reservation,room,check_in,check_out,status,amount
1,101,2027-03-10,2027-03-12,CONFIRMED,499.80
2,201,2027-03-10,2027-03-12,CONFIRMED,662.83
3,301,2027-03-05,2027-03-07,CANCELLED,450.0
total,,,,1612.63
```

---

## Cobertura de Testes

| Componente | Cobertura |
|-----------|-----------|
| PriceCalculator | 100% (7 testes) |
| EmailValidator | 100% (2 testes) |
| ReservationService | Lógica testável manualmente |
| MessageService | Lógica testável manualmente |
| ReportService | Lógica testável manualmente |
| Database | Cria tabelas corretamente |
| Repositórios | Operações CRUD funcionam |

## Como Executar Testes Manuais

1. Iniciar banco PostgreSQL:
```bash
docker run -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:18
```

2. Iniciar aplicação:
```bash
export DB_URL=jdbc:postgresql://localhost:5432/postgres
export DB_USER=postgres
export DB_PASSWORD=postgres
java -jar target/mare-alta.jar
```

3. Executar cenários com curl:
```bash
# Listar quartos
curl -X GET http://localhost:7070/rooms

# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{"roomNumber": 101, "guestName": "Ana", "guestEmail": "ana@gmail.com", "guestCpf": "11122233344", "checkIn": "2027-03-10", "checkOut": "2027-03-12", "guests": 2, "ratePlan": "STANDARD"}'

# ... outros cenários
```
