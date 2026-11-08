# Exemplos de Uso da API

## 1. Listar Quartos

```bash
curl http://localhost:7070/rooms
```

Resposta:
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

## 2. Criar Reserva - Plano STANDARD

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

## 3. Criar Reserva - Plano CORPORATE (Tecnosul)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }'
```

Resultado: diárias R$ 779,80, desconto R$ 93,58, total R$ 686,22

## 4. Criar Reserva - Plano WEEKEND_PACKAGE

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 202,
    "guestName": "Carlos Dias",
    "guestEmail": "carlos@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 3,
    "ratePlan": "WEEKEND_PACKAGE"
  }'
```

Resultado: total R$ 820,00 (preço fixo)

## 5. Criar Reserva - Plano LONG_STAY

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Ana Costa",
    "guestEmail": "ana@example.com",
    "guestCpf": "55566677788",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 1,
    "ratePlan": "LONG_STAY"
  }'
```

Resultado: diárias R$ 1.749,30, desconto R$ 174,93, total R$ 1.574,37

## 6. Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

Isso:
- Muda o status para CONFIRMED
- Registra o pagamento
- Envia email de confirmação (mock)

## 7. Fazer Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

## 8. Fazer Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

Retorna o comprovante formatado.

## 9. Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

O reembolso depende do plano:
- STANDARD: 100%
- NON_REFUNDABLE: 0%
- CORPORATE: 100%
- WEEKEND_PACKAGE: 50%
- LONG_STAY: 100%

## 10. Buscar Reserva

```bash
curl http://localhost:7070/reservations/1
```

## 11. Obter Comprovante (Segunda Via)

```bash
curl http://localhost:7070/reservations/1/document
```

Disponível apenas para reservas pagas (CONFIRMED, CHECKED_IN ou CHECKED_OUT).

## 12. Listar Mensagens Enviadas

```bash
curl http://localhost:7070/messages
```

## 13. Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

Retorna CSV:
```csv
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-03-10,2027-03-12
202,DELUXE,2,2027-03-12,2027-03-14
total,2/5
```

## 14. Relatório Financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

Retorna CSV:
```csv
reservation,room,check_in,check_out,status,amount
1,201,2027-03-10,2027-03-12,CONFIRMED,686.22
2,202,2027-03-12,2027-03-14,CANCELLED,820.00
total,1506.22
```

## Códigos de Erro

- 400 INVALID_REQUEST: Dados inválidos ou regras do plano não atendidas
- 404 ROOM_NOT_FOUND: Quarto não existe
- 404 RESERVATION_NOT_FOUND: Reserva não existe
- 409 ROOM_UNAVAILABLE: Quarto já reservado nas datas
- 409 OPERATION_NOT_ALLOWED: Operação não permitida no status atual
- 422 CAPACITY_EXCEEDED: Mais hóspedes do que o quarto comporta

## Empresas Conveniadas (CORPORATE)

- TECNOSUL: 12% de desconto
- AGROVALE: 18% de desconto
- FARMAVIDA: 25% de desconto
