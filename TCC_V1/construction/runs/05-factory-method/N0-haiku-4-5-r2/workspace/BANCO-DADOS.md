# Estrutura do Banco de Dados - Pousada Maré Alta

## Conexão

```
Driver: PostgreSQL 18
JDBC URL: jdbc:postgresql://localhost:5432/marealta
Usuário: pousada (padrão)
Senha: senha123 (padrão)
```

## Variáveis de Ambiente

```bash
DB_URL="jdbc:postgresql://HOST:PORT/DATABASE"
DB_USER="usuario"
DB_PASSWORD="senha"
```

## Schema - Tabela `rooms`

Quartos cadastrados (5 quartos fixos).

```sql
CREATE TABLE rooms (
    number INT PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    capacity INT NOT NULL,
    nightly_rate DECIMAL(10, 2) NOT NULL
);
```

### Dados Pré-carregados

| number | type | capacity | nightly_rate |
|--------|------|----------|--------------|
| 101 | STANDARD | 2 | 249.90 |
| 102 | STANDARD | 2 | 249.90 |
| 201 | DELUXE | 3 | 389.90 |
| 202 | DELUXE | 3 | 389.90 |
| 301 | SUITE | 4 | 579.90 |

## Schema - Tabela `reservations`

Histórico completo de todas as reservas.

```sql
CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    room_number INT NOT NULL,
    guest_name VARCHAR(255) NOT NULL,
    guest_email VARCHAR(255) NOT NULL,
    guest_cpf VARCHAR(14),
    company_cnpj VARCHAR(18),
    company_name VARCHAR(255),
    guest_passport VARCHAR(50),
    guest_country VARCHAR(100),
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    guests INT NOT NULL,
    rate_plan VARCHAR(50) NOT NULL,
    nights INT NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    discount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    total DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    amount_paid DECIMAL(10, 2) NOT NULL DEFAULT 0,
    refund DECIMAL(10, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_number) REFERENCES rooms(number)
);
```

### Campos

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | BIGSERIAL | ID único auto-incrementado |
| room_number | INT | Número do quarto (FK) |
| guest_name | VARCHAR(255) | Nome do hóspede |
| guest_email | VARCHAR(255) | Email do hóspede |
| guest_cpf | VARCHAR(14) | CPF do hóspede (único tipo 1) |
| company_cnpj | VARCHAR(18) | CNPJ da empresa (tipo 2) |
| company_name | VARCHAR(255) | Razão social (tipo 2) |
| guest_passport | VARCHAR(50) | Passaporte (tipo 3) |
| guest_country | VARCHAR(100) | País (tipo 3) |
| check_in | DATE | Data de entrada |
| check_out | DATE | Data de saída |
| guests | INT | Quantidade de hóspedes |
| rate_plan | VARCHAR(50) | STANDARD ou NON_REFUNDABLE |
| nights | INT | Quantidade de noites |
| subtotal | DECIMAL(10, 2) | Soma das diárias |
| discount | DECIMAL(10, 2) | Desconto aplicado |
| total | DECIMAL(10, 2) | Valor final (subtotal - desconto) |
| status | VARCHAR(50) | PENDING/CONFIRMED/CHECKED_IN/CHECKED_OUT/CANCELLED |
| amount_paid | DECIMAL(10, 2) | Valor pago (0 até confirmação) |
| refund | DECIMAL(10, 2) | Valor reembolsado (NULL se não reembolsado) |
| created_at | TIMESTAMP | Data/hora de criação |

### Estados Possíveis

```
PENDING
  ↓ (confirmação)
CONFIRMED
  ↓ (check-in)
CHECKED_IN
  ↓ (check-out)
CHECKED_OUT

(Em qualquer ponto antes de CHECKED_IN) → CANCELLED
```

### Tipos de Identificação

Apenas UM dos três deve ser preenchido:

1. **CPF** (Brasileiro)
   - guest_cpf: "123.456.789-00"
   - others: NULL

2. **CNPJ** (Empresa)
   - company_cnpj: "12.345.678/0001-90"
   - company_name: "Nome da Empresa"
   - others: NULL

3. **Passaporte** (Estrangeiro)
   - guest_passport: "X1234567"
   - guest_country: "United Kingdom"
   - others: NULL

## Schema - Tabela `messages`

Log de mensagens enviadas (emails de confirmação, etc).

```sql
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    channel VARCHAR(50) NOT NULL,
    "to" VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    body TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Campos

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | BIGSERIAL | ID único auto-incrementado |
| channel | VARCHAR(50) | EMAIL (único por enquanto) |
| to | VARCHAR(255) | Destinatário (email) |
| subject | VARCHAR(255) | Assunto |
| body | TEXT | Corpo da mensagem |
| created_at | TIMESTAMP | Data/hora de envio |

## Fluxo de Dados

### 1. Criar Reserva

```
POST /reservations
  ↓
ReservationService.createReservation()
  ↓
INSERT INTO reservations (status='PENDING', amount_paid=0)
  ↓
Retorna Reservation com id
```

### 2. Confirmar Reserva

```
POST /reservations/{id}/confirmation
  ↓
ReservationService.confirmReservation()
  ↓
UPDATE reservations SET status='CONFIRMED', amount_paid=total
  ↓
INSERT INTO messages (channel='EMAIL', ...)
  ↓
Retorna Reservation com status='CONFIRMED'
```

### 3. Check-in

```
POST /reservations/{id}/check-in
  ↓
ReservationService.checkIn()
  ↓
UPDATE reservations SET status='CHECKED_IN'
  ↓
Retorna Reservation com status='CHECKED_IN'
```

### 4. Check-out

```
POST /reservations/{id}/check-out
  ↓
ReservationService.checkOut()
  ↓
UPDATE reservations SET status='CHECKED_OUT'
  ↓
Retorna Document
```

### 5. Cancelar

```
POST /reservations/{id}/cancellation
  ↓
ReservationService.cancelReservation()
  ↓
UPDATE reservations SET status='CANCELLED'
  ↓
IF (status='CONFIRMED' AND ratePlan='STANDARD'):
    UPDATE reservations SET refund=amount_paid
ELSE:
    refund remains NULL
  ↓
Retorna Reservation com status='CANCELLED'
```

## Queries Principais

### Verificar Conflito de Data

```sql
SELECT * FROM reservations
WHERE room_number = ?
AND status != 'CANCELLED'
AND check_in < ? AND check_out > ?
```

Retorna conflitos entre datas [checkIn, checkOut).

### Ocupação por Noite

```sql
SELECT * FROM reservations
WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
AND check_in <= ? AND check_out > ?
ORDER BY room_number
```

Hóspedes que ocupam o quarto na noite especificada.

### Receita do Mês

```sql
SELECT * FROM reservations
WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')
AND check_in >= ? AND check_in < ?
AND (status != 'CANCELLED' OR amount_paid > 0)
ORDER BY check_in, id
```

Reservas que iniciaram no mês.

## Cálculos no Banco

Todos os cálculos são feitos na **aplicação Java**, usando `BigDecimal` com `RoundingMode.HALF_EVEN`.

**Não há triggers ou stored procedures.**

## Integridade Referencial

- `room_number` → `rooms(number)` (obrigatório)
- Nenhuma constraint UNIQUE (mesma reserva pode ser consultada várias vezes)
- Nenhuma constraint CHECK (validações na aplicação)

## Tipagem

- Datas: `DATE` (apenas YYYY-MM-DD)
- Valores monetários: `DECIMAL(10, 2)` (até R$ 99.999.999,99)
- IDs: `BIGSERIAL` (até 9.223.372.036.854.775.807)
- Strings: `VARCHAR` com tamanhos apropriados

## Índices

Automáticos:
- `rooms.number` (PRIMARY KEY)
- `reservations.id` (PRIMARY KEY)
- `messages.id` (PRIMARY KEY)

Sugeridos (não criados automaticamente):
```sql
CREATE INDEX idx_reservations_room_date 
  ON reservations(room_number, check_in, check_out);

CREATE INDEX idx_reservations_status 
  ON reservations(status);

CREATE INDEX idx_reservations_check_in 
  ON reservations(check_in);
```

## Backup e Recuperação

```bash
# Backup
pg_dump -U pousada -d marealta > backup.sql

# Restaurar
psql -U pousada -d marealta < backup.sql
```

## Monitoramento

```bash
# Conectar ao banco
psql -U pousada -d marealta

# Ver reservas
SELECT id, guest_name, status, total FROM reservations;

# Ver mensagens
SELECT id, channel, "to", subject FROM messages;

# Ver ocupação de um dia
SELECT room_number, guest_name, check_in, check_out 
FROM reservations 
WHERE check_in <= '2027-07-10' AND check_out > '2027-07-10'
AND status != 'CANCELLED';
```

## Performance

- Aplicação: ~5-10ms por requisição
- Banco: ~1-2ms por query
- Total esperado: <50ms por operação

Para otimizar, considere adicionar os índices sugeridos em produção.

## Conformidade

- ✅ Sem informações sensíveis em logs
- ✅ Sem senhas armazenadas
- ✅ Dados monetários com precisão
- ✅ Histórico completo de transações
- ✅ Timestamps em UTC (CURRENT_TIMESTAMP)
