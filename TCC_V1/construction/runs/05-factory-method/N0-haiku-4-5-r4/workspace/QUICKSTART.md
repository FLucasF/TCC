# Quick Start - Pousada Maré Alta

## Pré-requisitos

- PostgreSQL 18 instalado e rodando
- Java 21 instalado
- Maven 3.6+ instalado
- curl para testar a API

## 1. Preparar o Banco de Dados

```bash
# Conectar ao PostgreSQL
psql -U postgres

# Criar database
CREATE DATABASE marealta;

# Sair (ctrl+d ou \q)
```

## 2. Build do Projeto

```bash
cd /workspace

# Compilar
mvn clean compile

# Gerar jar executável
mvn package -DskipTests

# Verificar se foi criado
ls -lh target/mare-alta-1.0.0.jar
```

## 3. Executar o Servidor

```bash
# Definir variáveis de ambiente
export DB_URL="jdbc:postgresql://localhost:5432/marealta"
export DB_USER="postgres"
export DB_PASSWORD="sua_senha"
export PORT="7070"

# Executar
java -jar target/mare-alta-1.0.0.jar
```

Se tudo correr bem, você verá algo como:
```
16:45:20.123 [main] INFO com.pousada.marealta.database.Database - Database initialized successfully
16:45:20.456 [main] INFO com.pousada.marealta.App - Server started on port 7070
```

## 4. Testar a API

Em outro terminal, execute:

### Listar Quartos
```bash
curl -s http://localhost:7070/rooms | jq .
```

Resultado esperado (5 quartos):
```json
[
  {"number": 101, "type": "STANDARD", "capacity": 2, "nightlyRate": 249.90},
  {"number": 102, "type": "STANDARD", "capacity": 2, "nightlyRate": 249.90},
  {"number": 201, "type": "DELUXE", "capacity": 3, "nightlyRate": 389.90},
  {"number": 202, "type": "DELUXE", "capacity": 3, "nightlyRate": 389.90},
  {"number": 301, "type": "SUITE", "capacity": 4, "nightlyRate": 579.90}
]
```

### Criar Reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Maria Silva",
    "guestEmail": "maria@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq .
```

Resultado: Uma reserva PENDING com id 1, total R$ 499,80

### Confirmar Reserva (Pagar)

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 499.80}' | jq .
```

Resultado: Reserva agora está CONFIRMED

### Verificar E-mails Enviados

```bash
curl -s http://localhost:7070/messages | jq '.[0]'
```

Você verá o e-mail de confirmação com o comprovante dentro!

### Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in \
  -H "Content-Type: application/json" \
  -d '{}' | jq .
```

### Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out \
  -H "Content-Type: application/json" \
  -d '{}' | jq .
```

Resultado: Documento de check-out com o comprovante formatado!

### Obter Segunda Via

```bash
curl -s http://localhost:7070/reservations/1/document | jq .
```

### Relatório de Ocupação

```bash
curl -s "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

Resultado (CSV):
```
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-03-10,2027-03-12
total,1/5
```

### Relatório Financeiro

```bash
curl -s "http://localhost:7070/reports/revenue?month=2027-03"
```

Resultado (CSV):
```
reservation,room,check_in,check_out,status,amount
1,101,2027-03-10,2027-03-12,CHECKED_OUT,499.80
total,,,,499.80
```

## 5. Testar com CNPJ (Empresa)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Santos",
    "guestEmail": "joao@empresa.com.br",
    "companyCnpj": "12345678000190",
    "companyName": "Empresa Ltda",
    "checkIn": "2027-05-01",
    "checkOut": "2027-05-03",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq .
```

Resultado: Reserva 2 com total R$ 779,80

Confirme e verifique que o e-mail contém uma **FATURA** (em vez de comprovante):
- Mostra empresa com CNPJ
- Inclui "ISS retido (5%): R$ 38,99"

## 6. Testar com Passaporte (Estrangeiro)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "John Smith",
    "guestEmail": "john@example.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq .
```

Resultado: Reserva 3 
- Noites: 2 × R$ 695,88 (alta temporada) = R$ 1.391,76
- Desconto (15%): R$ 208,76
- Total: R$ 1.183,00

Confirme e verifique que o e-mail contém um **BOOKING RECEIPT** (em inglês):
- Em formato internacional
- Valores em "BRL 1,183.00"
- Check-in/out em YYYY-MM-DD

## 7. Testar Cancelamento

```bash
# Criar uma reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{...}' > /tmp/res.json

# Confirmar
curl -X POST http://localhost:7070/reservations/4/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 1000.00}'

# Cancelar (STANDARD plan = reembolso total)
curl -X POST http://localhost:7070/reservations/4/cancellation \
  -H "Content-Type: application/json" \
  -d '{}' | jq .

# Ver refund
curl -s http://localhost:7070/reservations/4 | jq '.refund'
```

## 8. Parar o Servidor

No terminal onde ele está rodando:
```
Ctrl+C
```

## Pronto! 🎉

Você já testou:
- ✅ Criar reserva (CPF, CNPJ, Passaporte)
- ✅ Confirmar (pagar)
- ✅ Check-in e Check-out
- ✅ Documentos (Comprovante, Fatura, Booking Receipt)
- ✅ E-mails
- ✅ Segunda via
- ✅ Relatórios
- ✅ Cancelamento

## Próximos Passos

1. Ver exemplos mais completos em `EXAMPLES.md`
2. Ler implementação em `IMPLEMENTATION_SUMMARY.md`
3. Integrar com o frontend (site)

## Troubleshooting

**Erro de conexão com banco:**
```
DB_URL, DB_USER ou DB_PASSWORD incorretos
```

**Porta 7070 já em uso:**
```
export PORT="8080"  # usar outra porta
```

**Erro ao criar database:**
```
psql -U postgres -c "CREATE DATABASE marealta;"
```

Divirta-se! 🚀
