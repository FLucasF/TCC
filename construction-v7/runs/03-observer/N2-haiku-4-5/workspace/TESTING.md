# Guia de Testes - Sistema de Reservas

## Testes Unitários

Execute todos os testes:

```bash
mvn test
```

Resultados esperados:
- **PricingServiceTest**: 9 testes
- **ReportServiceTest**: 8 testes  
- **ReservationServiceTest**: 11 testes
- **Total**: 28 testes, 0 falhas

## Teste de Integração Completo

Para um teste prático completo do sistema, execute:

```bash
mvn clean package
PORT=8080 java -jar target/reserva-system-1.0.0.jar &
SERVER_PID=$!
sleep 2
```

### 1. Listar Quartos

```bash
curl -X GET http://localhost:8080/rooms | jq .
```

Esperado: 5 quartos (101, 102, 201, 202, 301) com tipos e preços

### 2. Criar Reserva em Alta Temporada

```bash
curl -X POST http://localhost:8080/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@example.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 3,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq .
```

Esperado:
- Reserva ID 1
- Status: PENDING
- Subtotal: R$ 1.391,76 (2 × R$ 695,88)
- Desconto: R$ 208,76 (15%)
- Total: R$ 1.183,00

### 3. Confirmar Reserva

```bash
curl -X POST http://localhost:8080/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{}' | jq .
```

Esperado:
- Status: CONFIRMED
- amountPaid: R$ 1.183,00

### 4. Listar Mensagens Enviadas

```bash
curl -X GET http://localhost:8080/messages | jq .
```

Esperado: 3 mensagens (hóspede, governança, financeiro)

### 5. Check-in

```bash
curl -X POST http://localhost:8080/reservations/1/check-in \
  -H "Content-Type: application/json" \
  -d '{}' | jq .
```

Esperado: Status: CHECKED_IN

### 6. Check-out

```bash
curl -X POST http://localhost:8080/reservations/1/check-out \
  -H "Content-Type: application/json" \
  -d '{}' | jq .
```

Esperado: Comprovante com dados da reserva

### 7. Obter Segunda Via

```bash
curl -X GET http://localhost:8080/reservations/1/document | jq .
```

Esperado: Documento com mesmos dados

### 8. Criar Segunda Reserva (Fora de Alta Temporada)

```bash
curl -X POST http://localhost:8080/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Ana Silva",
    "guestEmail": "ana@example.com",
    "guestCpf": "55544433322",
    "checkIn": "2027-04-15",
    "checkOut": "2027-04-17",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq .
```

Esperado:
- Subtotal: R$ 499,80 (2 × R$ 249,90)
- Sem desconto (STANDARD)
- Total: R$ 499,80

### 9. Confirmar Segunda Reserva

```bash
curl -X POST http://localhost:8080/reservations/2/confirmation \
  -H "Content-Type: application/json" \
  -d '{}' | jq .
```

Esperado: 3 novas mensagens (total: 6)

### 10. Cancelar Segunda Reserva

```bash
curl -X POST http://localhost:8080/reservations/2/cancellation \
  -H "Content-Type: application/json" \
  -d '{}' | jq .
```

Esperado:
- Status: CANCELLED
- refund: R$ 499,80 (reembolso total para STANDARD)

### 11. Relatório de Ocupação (10/07/2027)

```bash
curl -X GET "http://localhost:8080/reports/occupancy?date=2027-07-10"
```

Esperado:
```
room,type,reservation,check_in,check_out
301,SUITE,1,2027-07-10,2027-07-12
total,1/5
```

### 12. Relatório de Receita (Julho/2027)

```bash
curl -X GET "http://localhost:8080/reports/revenue?month=2027-07"
```

Esperado:
```
reservation,room,check_in,check_out,status,amount
1,301,2027-07-10,2027-07-12,CHECKED_OUT,1183.00
total,,,,1183.00
```

### 13. Tentativa Inválida: Quarto Indisponível

```bash
curl -X POST http://localhost:8080/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "11111111111",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-11",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq .
```

Esperado: Erro 409 com ROOM_UNAVAILABLE

### 14. Tentativa Inválida: Capacidade Excedida

```bash
curl -X POST http://localhost:8080/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "11111111111",
    "checkIn": "2027-05-10",
    "checkOut": "2027-05-11",
    "guests": 5,
    "ratePlan": "STANDARD"
  }' | jq .
```

Esperado: Erro 422 com CAPACITY_EXCEEDED

## Casos de Teste de Sucesso

✅ Criar reserva STANDARD (reembolso total)  
✅ Criar reserva NON_REFUNDABLE (15% desconto)  
✅ Confirmar reserva (envia 3 emails)  
✅ Check-in e Check-out (com comprovante)  
✅ Cancelar reserva pendente (sem avisos)  
✅ Cancelar reserva confirmada (avisos + reembolso)  
✅ Relatório de ocupação (filtra datas corretas)  
✅ Relatório de receita (soma correta)  
✅ Segunda via de comprovante  
✅ Validação de capacidade  
✅ Validação de quarto indisponível  
✅ Validação de datas inválidas  

## Parar o Servidor

```bash
kill $SERVER_PID
```

## Verificação de Comprovante

Um exemplo de comprovante enviado por email:

```
Olá, Pedro Costa!

Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

Comprovante de reserva nº 1
Hóspede: Pedro Costa (CPF 987.654.321-00)
Quarto: 301 (SUITE)
Entrada: 10/07/2027 | Saída: 12/07/2027 | 2 diárias
Diárias: R$ 1.391,76
Desconto: R$ 208,76
Total: R$ 1.183,00

A entrada é a partir das 14h, e a saída, até as 12h.
Até breve!
```

## Cálculo de Exemplo

**Reserva em Alta Temporada com Desconto NON_REFUNDABLE**

- Quarto: 301 (SUITE)
- Diária base: R$ 579,90
- Datas: 10/07/2027 a 12/07/2027 (2 noites, julho = alta temporada)
- Multiplicador alta temporada: 1.20
- Diária com alta temporada: 579,90 × 1.20 = 695,88
- Subtotal: 695,88 × 2 = 1.391,76
- Desconto NON_REFUNDABLE: 1.391,76 × 0.15 = 208,76
- **Total: 1.391,76 - 208,76 = 1.183,00**

Arredondamento "meio para par" aplicado em cada etapa.
