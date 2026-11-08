# Quick Start

## Pré-requisitos

- Java 21+
- PostgreSQL 18+ em execução

## 1. Preparar Banco de Dados

```bash
# Criar banco de dados
createdb pousada_mare_alta

# Ou, se preferir outro nome, ajuste o DB_URL abaixo
```

## 2. Executar Aplicação

### No repositório (desenvolvimento):

```bash
mvn clean package

# Linux/Mac
DB_URL="jdbc:postgresql://localhost:5432/pousada_mare_alta" \
DB_USER="postgres" \
DB_PASSWORD="sua_senha" \
PORT=7070 \
java -jar target/reservas-1.0.0.jar

# Windows PowerShell
$env:DB_URL="jdbc:postgresql://localhost:5432/pousada_mare_alta"
$env:DB_USER="postgres"
$env:DB_PASSWORD="sua_senha"
$env:PORT="7070"
java -jar target/reservas-1.0.0.jar
```

### Esperado:
```
Pousada Maré Alta API listening on port 7070
```

## 3. Testar com curl

### Listar quartos:
```bash
curl http://localhost:7070/rooms | jq
```

### Criar reserva:
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
  }' | jq
```

### Confirmar reserva (usar ID retornado):
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 599.76}' | jq
```

### Ver mensagens de e-mail:
```bash
curl http://localhost:7070/messages | jq
```

## 4. Informações de Teste

### Preços Base (sem alta temporada)
- Quarto STANDARD: R$ 249,90/noite
- Quarto DELUXE: R$ 389,90/noite
- Quarto SUITE: R$ 579,90/noite

### Alta Temporada (+20%)
- 15 dez a 28 fev
- Julho

### Reserva Exemplo
- Check-in: 10/07/2027 (alta temporada)
- Check-out: 12/07/2027 (2 noites)
- Quarto STANDARD (101/102): 249,90 × 1,20 × 2 = **R$ 599,76**

## 5. Documentação Completa

- `README.md` - Visão geral e API
- `TESTING.md` - Testes manuais com curl
- `ARCHITECTURE.md` - Design e componentes
- `QUICKSTART.md` - Este arquivo

## Troubleshooting

### "DB_URL, DB_USER, and DB_PASSWORD environment variables are required"
→ Certifique-se de setar as 3 variáveis de ambiente

### "Connection refused"
→ Verifique se PostgreSQL está rodando:
```bash
psql -U postgres -c "SELECT 1"
```

### "database pousada_mare_alta does not exist"
→ Crie o banco:
```bash
createdb pousada_mare_alta
```

### Porta já em uso
→ Use outra porta:
```bash
PORT=8080 java -jar target/reservas-1.0.0.jar
```

## Desenvolvedor

Para modificar o código:

```bash
# Compilar
mvn compile

# Testar
mvn test

# Build completo
mvn clean package

# Limpar
mvn clean
```

## Exemplo Completo

```bash
#!/bin/bash

# Setup
createdb pousada_test 2>/dev/null || true

# Executar
DB_URL="jdbc:postgresql://localhost:5432/pousada_test" \
DB_USER="postgres" \
DB_PASSWORD="postgres" \
PORT=7070 \
java -jar target/reservas-1.0.0.jar &

sleep 2

# Testar
echo "=== QUARTOS ==="
curl -s http://localhost:7070/rooms | jq '.[] | {number, type, capacity}'

echo -e "\n=== CRIAR RESERVA ==="
RESP=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Teste",
    "guestEmail": "teste@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-08-10",
    "checkOut": "2027-08-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
  
RID=$(echo $RESP | jq '.id')
echo "Reserva ID: $RID"
echo $RESP | jq '{status, total}'

echo -e "\n=== CONFIRMAR ==="
curl -s -X POST http://localhost:7070/reservations/$RID/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 499.80}' | jq '{status, amountPaid}'

echo -e "\n=== MENSAGENS ==="
curl -s http://localhost:7070/messages | jq 'length'

# Cleanup
pkill -f "java -jar"
dropdb pousada_test 2>/dev/null || true
```
