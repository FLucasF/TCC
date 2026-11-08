# Quick Start - Sistema de Reservas Pousada Maré Alta

## Instalação Rápida

### 1. Pré-requisitos
- Docker e Docker Compose
- Java 21 (para compilar)
- Maven 3.6+ (para compilar)

### 2. Compilar (opcional - JAR já está pronto)
```bash
mvn clean package
```

### 3. Iniciar PostgreSQL
```bash
docker-compose up -d postgres
```

Aguarde o banco estar pronto:
```bash
docker-compose exec postgres pg_isready -U pousada_user
```

### 4. Executar a Aplicação
```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=pousada_user
export DB_PASSWORD=pousada_pass
export PORT=7070

java -jar target/mare-alta.jar
```

A aplicação estará disponível em `http://localhost:7070`

## Testando a API

### 1. Listar Quartos
```bash
curl http://localhost:7070/rooms | jq .
```

### 2. Criar uma Reserva
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq .
```

Anote o `id` retornado (ex: 1)

### 3. Confirmar Pagamento
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 599.76}' | jq .
```

### 4. Check-in
```bash
curl -X POST http://localhost:7070/reservations/1/check-in \
  -H "Content-Type: application/json" | jq .
```

### 5. Check-out
```bash
curl -X POST http://localhost:7070/reservations/1/check-out \
  -H "Content-Type: application/json" | jq .
```

### 6. Ver Comprovante
```bash
curl http://localhost:7070/reservations/1/document | jq .
```

### 7. Ver Emails Enviados
```bash
curl http://localhost:7070/messages | jq .
```

### 8. Relatório de Ocupação
```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

### 9. Relatório de Faturamento
```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

### 10. Relatório de Taxa de Turismo
```bash
curl "http://localhost:7070/reports/tourism-tax?month=2027-07"
```

### 11. Relatório de Planos de Tarifa
```bash
curl "http://localhost:7070/reports/rate-plans?month=2027-07"
```

## Testes Automatizados

Execute o script de integração completa:
```bash
bash integration-test.sh
```

## Parar a Aplicação

```bash
docker-compose down
```

## Quartos Disponíveis

| Número | Tipo | Capacidade | Diária |
|--------|------|-----------|--------|
| 101 | STANDARD | 2 | R$ 249,90 |
| 102 | STANDARD | 2 | R$ 249,90 |
| 201 | DELUXE | 3 | R$ 389,90 |
| 202 | DELUXE | 3 | R$ 389,90 |
| 301 | SUITE | 4 | R$ 579,90 |

## Planos de Tarifa

- **STANDARD**: Cancelamento reembolsável
- **NON_REFUNDABLE**: 15% desconto, cancelamento não reembolsável

## Alta Temporada

- 15/12 a 28/02
- Julho inteiro
- Valor: +20% na diária

## Dicas

- Datas devem estar em formato `yyyy-MM-dd`
- Valores em JSON com 2 casas decimais (ex: 599.76)
- CPF no formato `111.222.333-44`
- Check-in/out são operações de estado, sem horários específicos

## Troubleshooting

### PostgreSQL não inicia
```bash
docker-compose logs postgres
```

### Porta 7070 já está em uso
```bash
export PORT=8080
java -jar target/mare-alta.jar
```

### Erro de conexão com banco de dados
- Verifique se PostgreSQL está rodando: `docker-compose ps`
- Aguarde alguns segundos e tente novamente
- Verifique variáveis de ambiente DB_URL, DB_USER, DB_PASSWORD
