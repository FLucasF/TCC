# Instruções de Uso - Pousada Maré Alta

## Começando

### 1. Configurar o Banco de Dados

Use Docker Compose para iniciar o PostgreSQL:

```bash
docker-compose up -d
```

Isso vai iniciar um container PostgreSQL com:
- Database: `pousada`
- User: `pousada`
- Password: `pousada123`
- Port: `5432`

### 2. Compilar e Executar

```bash
# Compilar
mvn clean compile

# Empacotar
mvn clean package

# Executar com Docker
java -jar target/pousada.jar \
  -Denv:DB_URL="jdbc:postgresql://localhost:5432/pousada" \
  -Denv:DB_USER="pousada" \
  -Denv:DB_PASSWORD="pousada123" \
  -Denv:PORT="7070"

# Ou com variáveis de ambiente
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="pousada"
export DB_PASSWORD="pousada123"
export PORT=7070
java -jar target/pousada.jar
```

## Testando a API

Use o script de teste para fazer requisições básicas:

```bash
chmod +x test-api.sh
./test-api.sh http://localhost:7070
```

Ou faça requisições manualmente com curl:

### 1. Listar Quartos

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

### 2. Criar Reserva

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

Resposta:
```json
{
  "id": 1,
  "roomNumber": 101,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 499.80,
  "discount": 0,
  "total": 499.80,
  "status": "PENDING",
  "amountPaid": 0,
  "refund": null
}
```

### 3. Confirmar Reserva (Pagamento)

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 499.80}'
```

Isso envia e-mails para:
- Hóspede (joao@example.com): Confirmação com comprovante
- Governança (governanca@pousadamarealta.com.br): Aviso de nova reserva
- Financeiro (financeiro@pousadamarealta.com.br): Aviso de pagamento

### 4. Listar Mensagens Enviadas

```bash
curl http://localhost:7070/messages
```

### 5. Fazer Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in \
  -H "Content-Type: application/json" \
  -d '{}'
```

### 6. Fazer Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out \
  -H "Content-Type: application/json" \
  -d '{}'
```

Retorna o comprovante de check-out.

### 7. Obter Segunda Via do Comprovante

```bash
curl http://localhost:7070/reservations/1/document
```

### 8. Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation \
  -H "Content-Type: application/json" \
  -d '{}'
```

Se era confirmada com STANDARD, devolve o dinheiro.

### 9. Relatório de Ocupação

```bash
curl http://localhost:7070/reports/occupancy?date=2027-03-11
```

Retorna CSV:
```
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-03-10,2027-03-12
total,1/5
```

### 10. Relatório de Receita

```bash
curl http://localhost:7070/reports/revenue?month=2027-03
```

Retorna CSV:
```
reservation,room,check_in,check_out,status,amount
1,101,2027-03-10,2027-03-12,CHECKED_OUT,499.80
total,,,,,499.80
```

## Regras de Negócio

### Quartos

- **101 e 102**: STANDARD, 2 pessoas, R$ 249,90/noite
- **201 e 202**: DELUXE, 3 pessoas, R$ 389,90/noite  
- **301**: SUITE, 4 pessoas, R$ 579,90/noite

### Sazonalidade

20% de aumento em:
- 15 a 29 de dezembro
- Janeiro inteiro
- Fevereiro inteiro
- Julho inteiro

### Planos de Tarifa

- **STANDARD**: Preço normal, reembolsável 100% se cancelada
- **NON_REFUNDABLE**: 15% desconto, não reembolsável se cancelada

### Estados da Reserva

1. **PENDING**: Reserva criada, aguardando pagamento
2. **CONFIRMED**: Pagamento recebido
3. **CHECKED_IN**: Hóspede no quarto
4. **CHECKED_OUT**: Checkout realizado
5. **CANCELLED**: Cancelada (antes do check-in)

### Validações

- Check-in só em reservas CONFIRMED
- Check-out só em reservas CHECKED_IN
- Cancelamento só em PENDING ou CONFIRMED (antes do check-in)
- Um quarto não pode ter duas reservas na mesma noite
- Máximo de hóspedes limitado pela capacidade do quarto
- Check-out deve ser depois do check-in

## Sistema de Notificações Extensível

Para adicionar novos departamentos que precisam ser notificados:

1. Implemente `ReservationEventListener`
2. Registre em `Main.java`:

```java
ReservationEventPublisher.subscribe(new SeuNovoListener());
```

Eventos disponíveis:
- `onReservationConfirmed`: Quando reserva é confirmada com pagamento
- `onReservationCancelled`: Quando reserva é cancelada
- `onCheckedIn`: Quando hóspede faz check-in
- `onCheckedOut`: Quando hóspede faz check-out

## Parar o Sistema

Para parar o servidor e o banco de dados:

```bash
# Parar o servidor (Ctrl+C no terminal)

# Parar o Docker
docker-compose down
```

## Troubleshooting

### "DB_URL, DB_USER, and DB_PASSWORD must be set"
Configure as variáveis de ambiente antes de executar.

### "Could not connect to database"
Verifique se o PostgreSQL está rodando: `docker-compose ps`

### "Port 7070 is already in use"
Altere a variável `PORT` ou encerre o processo que está usando a porta.

## Notas

- Os e-mails não são enviados de verdade, ficam armazenados no banco de dados
- Para ver as mensagens enviadas, use: `curl http://localhost:7070/messages`
- Arredondamento é "meio para o par" (HALF_EVEN) em todas as operações monetárias
