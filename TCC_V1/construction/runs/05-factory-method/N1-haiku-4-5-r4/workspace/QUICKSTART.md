# QuickStart - Pousada Maré Alta

## Preparação Rápida

### 1. Iniciar PostgreSQL (com Docker)

```bash
docker-compose up -d
```

Se não tiver Docker, pode instalar o PostgreSQL manualmente:
- User: `pousada`
- Password: `pousada123`
- Database: `pousada_mare_alta`
- Host: `localhost`
- Port: `5432`

### 2. Build do Sistema

```bash
mvn clean package
```

### 3. Executar o Sistema

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada_mare_alta"
export DB_USER="pousada"
export DB_PASSWORD="pousada123"
export PORT=7070

java -jar target/reservation-system.jar
```

Ou use o script:

```bash
./scripts/start-local.sh
```

O sistema estará disponível em `http://localhost:7070`

## Testar a API

### Listar Quartos

```bash
curl http://localhost:7070/rooms | jq .
```

### Criar uma Reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Silva",
    "guestEmail": "maria@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq .
```

Nota o `id` da resposta para os próximos passos.

### Confirmar uma Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 779.80}'
```

### Fazer Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

### Fazer Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out | jq .
```

### Listar Mensagens (e-mails enviados)

```bash
curl http://localhost:7070/messages | jq .
```

### Gerar Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

### Gerar Relatório Financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

## Script de Teste Completo

Execute o script de teste que cria várias reservas e testa todos os endpoints:

```bash
./examples/test-api.sh
```

## Tipos de Identificação

### 1. Pessoa Física (CPF)

Gera: **Comprovante de reserva** (português)

```json
{
  "guestCpf": "123.456.789-00"
}
```

### 2. Empresa (CNPJ)

Gera: **Fatura de hospedagem** (português) com ISS retido

```json
{
  "companyCnpj": "12.345.678/0001-90",
  "companyName": "Empresa Ltda"
}
```

### 3. Estrangeiro (Passaporte)

Gera: **Booking receipt** (inglês)

```json
{
  "guestPassport": "X1234567",
  "guestCountry": "United Kingdom"
}
```

## Parar o Sistema

```bash
# Parar a aplicação (Ctrl+C no terminal)

# Parar o PostgreSQL
docker-compose down
```

## Próximos Passos

- Leia `README.md` para documentação completa
- Veja `pom.xml` para dependências usadas
- Explore o código em `src/main/java/com/pousadamarealta/`

## Troubleshooting

### "Connection refused" para o banco

Verifique se o PostgreSQL está rodando:

```bash
docker-compose ps
```

Se não estiver, inicie:

```bash
docker-compose up -d
```

### Porta 5432 já em uso

Mude a porta no `docker-compose.yml`:

```yaml
ports:
  - "5433:5432"  # Mude de 5432 para 5433
```

E na URL de conexão:

```bash
export DB_URL="jdbc:postgresql://localhost:5433/pousada_mare_alta"
```

### Porta 7070 já em uso

Mude a porta do servidor:

```bash
export PORT=8080
java -jar target/reservation-system.jar
```
