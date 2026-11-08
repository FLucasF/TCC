# Schema do Banco de Dados

O sistema cria automaticamente as tabelas na primeira execução. Este documento descreve o schema.

## Tabela: rooms

Armazena os dados dos quartos da pousada.

```sql
CREATE TABLE rooms (
    number INTEGER PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    capacity INTEGER NOT NULL,
    nightly_rate NUMERIC(10, 2) NOT NULL
);
```

Dados pré-inseridos:
- **101**: STANDARD, 2 pessoas, R$ 249,90
- **102**: STANDARD, 2 pessoas, R$ 249,90
- **201**: DELUXE, 3 pessoas, R$ 389,90
- **202**: DELUXE, 3 pessoas, R$ 389,90
- **301**: SUITE, 4 pessoas, R$ 579,90

## Tabela: reservations

Armazena os dados das reservas.

```sql
CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    room_number INTEGER NOT NULL,
    guest_name VARCHAR(255) NOT NULL,
    guest_email VARCHAR(255) NOT NULL,
    guest_cpf VARCHAR(14) NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    guests INTEGER NOT NULL,
    rate_plan VARCHAR(20) NOT NULL,
    nights INTEGER NOT NULL,
    subtotal NUMERIC(10, 2) NOT NULL,
    discount NUMERIC(10, 2) NOT NULL,
    total NUMERIC(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0,
    refund NUMERIC(10, 2),
    FOREIGN KEY (room_number) REFERENCES rooms(number)
);
```

### Campos
- **id**: Identificador único da reserva (auto-incrementado)
- **room_number**: Número do quarto (FK para rooms)
- **guest_name**: Nome completo do hóspede
- **guest_email**: E-mail do hóspede
- **guest_cpf**: CPF do hóspede (11 dígitos sem formatação)
- **check_in**: Data de entrada
- **check_out**: Data de saída
- **guests**: Número de hóspedes
- **rate_plan**: Plano de tarifa (STANDARD ou NON_REFUNDABLE)
- **nights**: Número de diárias
- **subtotal**: Valor total das diárias (sem desconto)
- **discount**: Valor do desconto aplicado
- **total**: Valor final (subtotal - desconto)
- **status**: Status da reserva (PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED)
- **amount_paid**: Valor pago (zero enquanto PENDING)
- **refund**: Valor reembolsado (null enquanto não cancelada)

## Tabela: messages

Armazena todas as mensagens (e-mails) enviadas pelo sistema.

```sql
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    channel VARCHAR(20) NOT NULL,
    "to" VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL
);
```

### Campos
- **id**: Identificador único da mensagem (auto-incrementado)
- **channel**: Canal de envio (atualmente apenas EMAIL)
- **to**: Endereço de e-mail do destinatário
- **subject**: Assunto do e-mail
- **body**: Corpo do e-mail

## Índices Implícitos

O PostgreSQL cria automaticamente índices nas chaves primárias. Para melhorar performance em queries frequentes, considere adicionar:

```sql
-- Buscar reservas por quarto e data
CREATE INDEX idx_reservations_room_date ON reservations(room_number, check_in, check_out);

-- Buscar reservas por e-mail
CREATE INDEX idx_reservations_guest_email ON reservations(guest_email);

-- Buscar mensagens por destinatário
CREATE INDEX idx_messages_to ON messages("to");

-- Buscar reservas por status
CREATE INDEX idx_reservations_status ON reservations(status);
```

## Verificações de Integridade

### Validações no Banco
- **Foreign Key**: room_number sempre aponta para um quarto existente
- **Dates**: check_out > check_in é validado na aplicação (não no DB)
- **Capacity**: número de guests ≤ capacity do quarto (validado na aplicação)

### Validações na Aplicação
- CPF com exatamente 11 dígitos
- Nome e e-mail não vazios
- Plano de tarifa válido
- Status de reserva válido
- Operações respeitam a máquina de estados de status

## Consultas Úteis

### Reservas confirmadas para um período
```sql
SELECT * FROM reservations
WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
AND check_in <= '2027-07-15'
AND check_out > '2027-07-15'
ORDER BY room_number;
```

### Receita de um mês
```sql
SELECT 
    SUM(amount_paid - COALESCE(refund, 0)) as total_revenue
FROM reservations
WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')
AND EXTRACT(YEAR FROM check_in) = 2027
AND EXTRACT(MONTH FROM check_in) = 7
AND amount_paid > 0;
```

### Quartos ocupados em uma data
```sql
SELECT 
    r.room_number,
    rooms.type,
    r.guest_name,
    r.check_in,
    r.check_out
FROM reservations r
JOIN rooms ON r.room_number = rooms.number
WHERE r.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
AND r.check_in <= '2027-07-15'
AND r.check_out > '2027-07-15'
ORDER BY r.room_number;
```

### E-mails enviados para um hóspede
```sql
SELECT * FROM messages
WHERE "to" = 'carla@example.com'
ORDER BY id DESC;
```

## Backup e Restauração

### Backup completo
```bash
pg_dump -U usuario -h localhost pousada > backup.sql
```

### Restauração
```bash
psql -U usuario -h localhost pousada < backup.sql
```

### Backup apenas de dados
```bash
pg_dump -U usuario -h localhost --data-only pousada > backup_data.sql
```
