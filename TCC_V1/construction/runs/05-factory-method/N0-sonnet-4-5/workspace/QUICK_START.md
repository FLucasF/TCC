# Guia de Início Rápido - Pousada Maré Alta

## Opção 1: Docker Compose (Mais Fácil)

```bash
# Build e start
mvn clean package -DskipTests
docker-compose up

# A API estará disponível em http://localhost:7070
```

## Opção 2: Local com PostgreSQL

### Passo 1: Iniciar PostgreSQL
```bash
docker run -d \
  --name postgres-marealte \
  -e POSTGRES_DB=marealte \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:18
```

### Passo 2: Executar Aplicação
```bash
# Build
mvn clean package -DskipTests

# Executar
export DB_URL=jdbc:postgresql://localhost:5432/marealte
export DB_USER=postgres
export DB_PASSWORD=postgres
java -jar target/pousada-mare-alta-1.0.0.jar
```

## Testar a API

### 1. Listar Quartos
```bash
curl http://localhost:7070/rooms | jq
```

### 2. Criar Reserva com CPF
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@email.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq
```

### 3. Confirmar Reserva (substitua {id} pelo ID retornado)
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation | jq
```

### 4. Ver Email Enviado
```bash
curl http://localhost:7070/messages | jq
```

### 5. Check-in
```bash
curl -X POST http://localhost:7070/reservations/1/check-in | jq
```

### 6. Check-out (recebe o comprovante)
```bash
curl -X POST http://localhost:7070/reservations/1/check-out | jq '.text' -r
```

### 7. Segunda Via do Documento
```bash
curl http://localhost:7070/reservations/1/document | jq '.text' -r
```

## Exemplos de Reservas

### Com CNPJ (Empresa)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Ana Costa",
    "guestEmail": "ana@email.com",
    "companyCnpj": "12.345.678/0001-90",
    "companyName": "Empresa ABC Ltda",
    "checkIn": "2027-03-15",
    "checkOut": "2027-03-17",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Com Passaporte (Estrangeiro)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "301",
    "guestName": "John Smith",
    "guestEmail": "john@email.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-03-20",
    "checkOut": "2027-03-22",
    "guests": 3,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

### Com Desconto (NON_REFUNDABLE)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Maria Santos",
    "guestEmail": "maria@email.com",
    "guestCpf": "987.654.321-00",
    "checkIn": "2027-04-01",
    "checkOut": "2027-04-05",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

### Alta Temporada (Julho)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Carlos Pereira",
    "guestEmail": "carlos@email.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

## Relatórios

### Ocupação por Data
```bash
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

### Receita por Mês
```bash
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

## Script de Teste Completo

Execute o script de teste que cria várias reservas e testa todos os fluxos:

```bash
./test-api.sh
```

## Testes Automatizados

```bash
# Testes unitários
mvn test

# Testes de integração (requer Docker)
mvn verify
```

## Parar e Limpar

### Docker Compose
```bash
docker-compose down -v
```

### PostgreSQL Local
```bash
docker stop postgres-marealte
docker rm postgres-marealte
```

## Documentos Gerados

O sistema gera três tipos de documentos:

1. **Comprovante** (CPF) - em português com R$
2. **Fatura** (CNPJ) - em português com R$ e ISS 5%
3. **Booking Receipt** (Passaporte) - em inglês com BRL

Todos são gerados automaticamente no check-out e podem ser consultados via segunda via.

## Portas Utilizadas

- **7070**: API REST
- **5432**: PostgreSQL (quando rodando local)

## Variáveis de Ambiente

- `DB_URL`: JDBC URL do PostgreSQL (padrão: jdbc:postgresql://localhost:5432/marealte)
- `DB_USER`: Usuário do banco (padrão: postgres)
- `DB_PASSWORD`: Senha do banco (padrão: postgres)
- `PORT`: Porta da aplicação (padrão: 7070)
