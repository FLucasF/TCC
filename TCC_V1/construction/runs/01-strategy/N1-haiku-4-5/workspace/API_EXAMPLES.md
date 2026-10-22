# Exemplos de Uso da API

## Requisitos para Testar

- PostgreSQL 18 rodando localmente
- cURL ou Postman
- Java 21

## Setup

1. Criar banco de dados:
```bash
createdb mare_alta
```

2. Definir variáveis de ambiente:
```bash
export DB_URL="jdbc:postgresql://localhost:5432/mare_alta"
export DB_USER="postgres"
export DB_PASSWORD="sua_senha"
export PORT=7070
```

3. Iniciar o servidor:
```bash
java -jar target/mare-alta-system-1.0.0.jar
```

## Exemplos de Requisições

### 1. Listar Quartos

```bash
curl -X GET http://localhost:7070/rooms
```

Resposta esperada:
```json
[
  {
    "number": 101,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  },
  {
    "number": 102,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  },
  {
    "number": 201,
    "type": "DELUXE",
    "capacity": 3,
    "nightlyRate": 389.90
  },
  {
    "number": 202,
    "type": "DELUXE",
    "capacity": 3,
    "nightlyRate": 389.90
  },
  {
    "number": 301,
    "type": "SUITE",
    "capacity": 4,
    "nightlyRate": 579.90
  }
]
```

### 2. Criar Reserva STANDARD

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

Resposta esperada: Código 201 com os dados da reserva

### 3. Criar Reserva com Convênio Corporativo

```bash
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
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }'
```

### 4. Criar Reserva Não-Reembolsável

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-06-15",
    "checkOut": "2027-06-17",
    "guests": 1,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

### 5. Criar Reserva Pacote Fim de Semana

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@example.com",
    "guestCpf": "55566677788",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }'
```

Note: A data de entrada DEVE ser uma sexta-feira e a data de saída, um domingo, totalizando 2 noites.

### 6. Criar Reserva Estadia Longa

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Ana Costa",
    "guestEmail": "ana@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 2,
    "ratePlan": "LONG_STAY"
  }'
```

Note: Mínimo 7 noites requeridas.

### 7. Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{
    "amountPaid": 599.76
  }'
```

### 8. Fazer Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

### 9. Fazer Check-out (Retorna Comprovante)

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

### 10. Obter Comprovante (Segunda Via)

```bash
curl -X GET http://localhost:7070/reservations/1/document
```

### 11. Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

### 12. Listar Mensagens Enviadas

```bash
curl -X GET http://localhost:7070/messages
```

### 13. Relatório de Ocupação

```bash
curl -X GET "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

Retorna CSV:
```
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
201,DELUXE,2,2027-07-09,2027-07-15
total,2/5
```

### 14. Relatório de Receita

```bash
curl -X GET "http://localhost:7070/reports/revenue?month=2027-07"
```

Retorna CSV:
```
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CHECKED_OUT,599.76
2,201,2027-07-09,2027-07-15,CONFIRMED,2879.40
total,,,,,3479.16
```

## Testes com Dados Reais

### Cenário 1: Reserva Padrão na Alta Temporada

Data: 10/07/2027 a 12/07/2027 (2 noites na alta temporada, julho)
Quarto: 101 STANDARD (R$ 249,90/noite)

Cálculo esperado:
- Noite 1 (10/07): R$ 249,90 × 1.20 = R$ 299,88
- Noite 2 (11/07): R$ 249,90 × 1.20 = R$ 299,88
- Subtotal: R$ 599,76
- Desconto: R$ 0,00
- Total: R$ 599,76

### Cenário 2: Convênio Tecnosul

Data: 10/03/2027 a 12/03/2027 (2 noites, fora de alta temporada)
Quarto: 201 DELUXE (R$ 389,90/noite)
Empresa: TECNOSUL (12% desconto)

Cálculo esperado:
- Noite 1: R$ 389,90
- Noite 2: R$ 389,90
- Subtotal: R$ 779,80
- Desconto (12%): R$ 93,58
- Total: R$ 686,22

### Cenário 3: Pacote Fim de Semana - Deluxe

Data: 12/03/2027 (sexta) a 14/03/2027 (domingo)
Quarto: 201 DELUXE
Plano: WEEKEND_PACKAGE

Cálculo esperado:
- Subtotal: R$ 820,00 (preço fixo)
- Desconto: R$ 0,00
- Total: R$ 820,00

Se cancelar:
- Reembolso: R$ 410,00 (50%)

### Cenário 4: Estadia Longa - Standard

Data: 01/03/2027 a 08/03/2027 (7 noites)
Quarto: 101 STANDARD (R$ 249,90/noite)
Plano: LONG_STAY (10% desconto)

Cálculo esperado:
- 7 noites × R$ 249,90 = R$ 1.749,30
- Desconto (10%): R$ 174,93
- Total: R$ 1.574,37

## Codigos de Erro

### 400 INVALID_REQUEST
- Campo obrigatório faltando
- Formato de data ou valor incorreto
- Empresa inválida para convênio
- Fim de semana inválido para o pacote
- Menos de 7 noites para estadia longa

### 404 ROOM_NOT_FOUND
- Número do quarto não existe

### 404 RESERVATION_NOT_FOUND
- ID da reserva não existe

### 409 ROOM_UNAVAILABLE
- Quarto já tem outra reserva para as mesmas datas

### 409 OPERATION_NOT_ALLOWED
- Tentar confirmar reserva que não está PENDING
- Tentar fazer check-in de reserva que não está CONFIRMED
- Tentar fazer check-out de reserva que não está CHECKED_IN
- Tentar cancelar reserva que já foi feito check-in ou check-out

### 422 CAPACITY_EXCEEDED
- Número de hóspedes maior que a capacidade do quarto
