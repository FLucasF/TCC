# Guia Rápido - Pousada Maré Alta

## Compilação e Testes

```bash
# Compilar o projeto
mvn clean package

# Rodar testes
mvn verify

# Gerar JAR executável
mvn clean package
```

## Execução com Docker

```bash
# Criar e iniciar os containers
docker-compose up

# Acessar a API em http://localhost:7070
```

## Execução Manual

```bash
# Iniciar PostgreSQL (necessário ter instalado)
# Assumindo que está rodando em localhost:5432

# Exportar variáveis de ambiente
export DB_URL="jdbc:postgresql://localhost:5432/reservas"
export DB_USER="postgres"
export DB_PASSWORD="sua_senha"
export PORT="7070"

# Executar o JAR
java -jar target/app.jar
```

## Testes da API

### 1. Verificar Quartos

```bash
curl http://localhost:7070/rooms | jq
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
  ...
]
```

### 2. Criar Reserva com CPF

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq
```

Resposta esperada (status 201):
```json
{
  "id": 1,
  "roomNumber": 201,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "11122233344",
  "companyCnpj": null,
  "companyName": null,
  "guestPassport": null,
  "guestCountry": null,
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 779.80,
  "discount": 0.00,
  "total": 779.80,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

### 3. Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation | jq
```

Depois disso, uma mensagem de e-mail será salva e poder ser consultada.

### 4. Verificar Mensagens Enviadas

```bash
curl http://localhost:7070/messages | jq
```

### 5. Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in | jq
```

### 6. Check-out (Obtém o Comprovante)

```bash
curl -X POST http://localhost:7070/reservations/1/check-out | jq
```

### 7. Segunda Via do Documento

```bash
curl http://localhost:7070/reservations/1/document | jq
```

### 8. Consultar Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

Resposta esperada (CSV):
```
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-07-10,2027-07-12
total,1/5
```

### 9. Consultar Receita

```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

Resposta esperada (CSV):
```
reservation,room,check_in,check_out,status,amount
1,201,2027-07-10,2027-07-12,CHECKED_OUT,779.80
total,,,,,779.80
```

## Criar Reserva com CNPJ

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 202,
    "guestName": "Ana Souza",
    "guestEmail": "ana@company.com",
    "companyCnpj": "12345678000190",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq
```

## Criar Reserva com Passaporte

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "John Smith",
    "guestEmail": "john@example.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq
```

## Testes de Erro

### Quarto não encontrado

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 999,
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq
```

Resposta esperada (status 404):
```json
{
  "error": "ROOM_NOT_FOUND",
  "message": "Room not found"
}
```

### Capacidade excedida

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 10,
    "ratePlan": "STANDARD"
  }' | jq
```

Resposta esperada (status 422):
```json
{
  "error": "CAPACITY_EXCEEDED",
  "message": "Number of guests exceeds room capacity"
}
```

## Estrutura do Banco de Dados

O sistema cria automaticamente as seguintes tabelas:

- `rooms`: Informações dos quartos (pré-carregados)
- `reservations`: Dados das reservas com todos os campos de identificação
- `messages`: Mensagens de e-mail enviadas

## Notas Importantes

1. **Alta Temporada**: 15/12 a 28/02 e julho inteiro recebem +20% na tarifa
2. **Planos**:
   - STANDARD: Sem desconto, 100% reembolsável
   - NON_REFUNDABLE: 15% de desconto, 0% reembolsável
3. **Arredondamento**: HALF_EVEN ("meio para o par") é usado em todos os cálculos
4. **Identificação**: Uma reserva deve ter EXATAMENTE um tipo de identificação (CPF, CNPJ ou Passaporte)
