# Guia de Setup - Pousada Maré Alta

## Setup do PostgreSQL

### Linux/WSL
```bash
# Instalar PostgreSQL
sudo apt-get install postgresql postgresql-contrib

# Iniciar o serviço
sudo service postgresql start

# Criar banco de dados
sudo -u postgres psql -c "CREATE DATABASE marealta;"
sudo -u postgres psql -c "ALTER USER postgres WITH PASSWORD 'postgres';"
```

### macOS (com Homebrew)
```bash
brew install postgresql
brew services start postgresql
psql postgres -c "CREATE DATABASE marealta;"
```

### Windows (Docker recomendado)
```bash
docker run --name postgres-marealta \
  -e POSTGRES_DB=marealta \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgres:18
```

## Variáveis de Ambiente

### Linux/macOS
```bash
export DB_URL="jdbc:postgresql://localhost:5432/marealta"
export DB_USER="postgres"
export DB_PASSWORD="postgres"
export PORT="7070"
export HOTEL_TEST_MODE="false"
```

### Windows (PowerShell)
```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/marealta"
$env:DB_USER="postgres"
$env:DB_PASSWORD="postgres"
$env:PORT="7070"
$env:HOTEL_TEST_MODE="false"
```

## Executar o Sistema

```bash
java -jar target/mare-alta.jar
```

A aplicação estará disponível em: `http://localhost:7070`

## Testes de Integração

### 1. Listar Quartos
```bash
curl -X GET http://localhost:7070/rooms \
  -H "Content-Type: application/json"
```

### 2. Criar Reserva
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Carla Dias",
    "guestEmail": "carla@gmail.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### 3. Confirmar Reserva
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{
    "amountPaid": 779.80
  }'
```

### 4. Check-in
```bash
curl -X POST http://localhost:7070/reservations/1/check-in \
  -H "Content-Type: application/json"
```

### 5. Check-out
```bash
curl -X POST http://localhost:7070/reservations/1/check-out \
  -H "Content-Type: application/json"
```

### 6. Ver Mensagens Enviadas
```bash
curl -X GET http://localhost:7070/messages \
  -H "Content-Type: application/json"
```

### 7. Relatório de Ocupação
```bash
curl -X GET "http://localhost:7070/reports/occupancy?date=2027-03-10" \
  -H "Content-Type: application/json"
```

### 8. Relatório Financeiro
```bash
curl -X GET "http://localhost:7070/reports/revenue?month=2027-03" \
  -H "Content-Type: application/json"
```

## Modo de Teste

Para testar o redirecionamento de emails:

```bash
export HOTEL_TEST_MODE=true
java -jar target/mare-alta.jar
```

Com esta flag ativa, todos os emails serão enviados para `testes@pousadamarealta.com.br` com o prefixo `[TESTE]` no assunto.

## Limpar o Banco de Dados

```bash
sudo -u postgres psql -d marealta -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```

Depois execute novamente a aplicação para recriar as tabelas.

## Troubleshooting

### Erro: "Connection refused"
- Verifique se o PostgreSQL está rodando
- Verifique se as variáveis de ambiente estão corretas

### Erro: "FATAL: database ... does not exist"
- Crie o banco de dados: `sudo -u postgres psql -c "CREATE DATABASE marealta;"`

### Porta 7070 já em uso
- Use outra porta: `export PORT=8080`

## Documentação Completa

Ver [README.md](README.md) para detalhes sobre a API e funcionalidades.
