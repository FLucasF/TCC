# Exemplo de Uso - Sistema de E-mails

## Cenário 1: Modo Normal (Produção)

### Reserva com E-mail Válido

**Request:**
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Ana Silva",
    "guestEmail": "ana@gmail.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-08-10",
    "checkOut": "2027-08-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

curl -X POST http://localhost:7070/reservations/1/confirmation
```

**GET /messages retorna:**
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "ana@gmail.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Ana Silva!...",
    "status": "SENT"
  },
  {
    "id": 2,
    "channel": "EMAIL",
    "to": "arquivo@pousadamarealta.com.br",
    "subject": "[Cópia] Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Ana Silva!...",
    "status": "SENT"
  }
]
```

### Reserva com E-mail Inválido

**Request:**
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@gmail",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-09-10",
    "checkOut": "2027-09-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

curl -X POST http://localhost:7070/reservations/2/confirmation
```

**GET /messages retorna:**
```json
[
  {
    "id": 3,
    "channel": "EMAIL",
    "to": "pedro@gmail",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Pedro Costa!...",
    "status": "BLOCKED"
  }
]
```

**Observação:** Mensagem bloqueada não gera cópia para arquivo. A recepção deve ligar para o hóspede.

---

## Cenário 2: Modo de Teste

### Iniciar em Modo de Teste

```bash
HOTEL_TEST_MODE=true java -jar target/hotel.jar
```

### Reserva com E-mail Válido

**Request:**
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "555.666.777-88",
    "checkIn": "2027-10-10",
    "checkOut": "2027-10-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

curl -X POST http://localhost:7070/reservations/3/confirmation
```

**GET /messages retorna:**
```json
[
  {
    "id": 4,
    "channel": "EMAIL",
    "to": "testes@pousadamarealta.com.br",
    "subject": "[TESTE] Reserva confirmada - Pousada Maré Alta",
    "body": "Destinatário original: carla@example.com\nOlá, Carla Dias!...",
    "status": "SENT"
  }
]
```

**Observação:** 
- E-mail redirecionado para `testes@pousadamarealta.com.br`
- Assunto com prefixo `[TESTE]`
- Corpo com linha adicional indicando destinatário original
- **Não gera cópia para arquivo**

### Reserva com E-mail Inválido (em Modo de Teste)

**Request:**
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 202,
    "guestName": "João Lima",
    "guestEmail": "joao.exemplo.com",
    "guestCpf": "999.888.777-66",
    "checkIn": "2027-11-10",
    "checkOut": "2027-11-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

curl -X POST http://localhost:7070/reservations/4/confirmation
```

**GET /messages retorna:**
```json
[
  {
    "id": 5,
    "channel": "EMAIL",
    "to": "joao.exemplo.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, João Lima!...",
    "status": "BLOCKED"
  }
]
```

**Observação:** Mesmo em modo de teste, e-mails inválidos são bloqueados e mantêm o endereço original.

---

## Resumo dos Comportamentos

| Situação | Modo Normal | Modo de Teste |
|----------|-------------|---------------|
| E-mail válido | Enviado + Cópia arquivo | Redirecionado para testes@ |
| E-mail inválido | Bloqueado | Bloqueado |
| Cópia para arquivo | Sim (se não bloqueado) | Não |
| Prefixo assunto | Nenhum / [Cópia] | [TESTE] |
| Destinatário original | N/A | Adicionado ao corpo |
