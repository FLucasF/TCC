# Guia de Desenvolvimento

## Setup Local para Testes

### 1. Iniciar PostgreSQL com Docker

```bash
docker run -d \
  --name pousada-db \
  -e POSTGRES_USER=pousada \
  -e POSTGRES_PASSWORD=senha \
  -e POSTGRES_DB=pousada \
  -p 5432:5432 \
  postgres:18-alpine
```

### 2. Build e Executar

```bash
# Build do projeto
mvn clean package

# Rodar a aplicação
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="pousada"
export DB_PASSWORD="senha"
export PORT=7070

java -jar target/reserva-system.jar
```

A aplicação estará disponível em `http://localhost:7070`.

### 3. Testar com curl

#### Listar quartos
```bash
curl -s http://localhost:7070/rooms | jq
```

#### Criar uma reserva
```bash
RESERVATION=$(curl -s -X POST http://localhost:7070/reservations \
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
  }')

echo $RESERVATION | jq

RES_ID=$(echo $RESERVATION | jq '.id')
echo "Reserva ID: $RES_ID"
```

#### Confirmar reserva
```bash
curl -s -X POST http://localhost:7070/reservations/$RES_ID/confirmation | jq
```

#### Verificar e-mail enviado
```bash
curl -s http://localhost:7070/messages | jq
```

#### Check-in
```bash
curl -s -X POST http://localhost:7070/reservations/$RES_ID/check-in | jq
```

#### Check-out (retorna comprovante)
```bash
curl -s -X POST http://localhost:7070/reservations/$RES_ID/check-out | jq
```

#### Obter segunda via do comprovante
```bash
curl -s http://localhost:7070/reservations/$RES_ID/document | jq
```

#### Gerar relatórios
```bash
# Ocupação em uma data
curl -s "http://localhost:7070/reports/occupancy?date=2027-07-10" | head -20

# Receita mensal
curl -s "http://localhost:7070/reports/revenue?month=2027-07" | head -20

# Taxa de turismo
curl -s "http://localhost:7070/reports/tourism-tax?month=2027-07" | head -20

# Análise de planos
curl -s "http://localhost:7070/reports/rate-plans?month=2027-07" | head -20
```

## Casos de Teste Importantes

### 1. Cálculo de Preço em Alta Temporada
```bash
# July 10-12, 2027 (alta temporada, 2 noites)
# STANDARD room: 249.90 × 1.20 = 299.88 por noite
# Total: 599.76
```

### 2. Desconto Non-Refundable
```bash
# 599.76 × 0.15 = 89.96 de desconto
# Total: 509.80
```

### 3. Conflito de Datas
```bash
# Criar primeira reserva: 10/07 a 12/07
# Tentar criar segunda: 11/07 a 13/07 → ERRO ROOM_UNAVAILABLE
```

### 4. Cancelamento
- STANDARD paga: reembolso total
- NON_REFUNDABLE paga: sem reembolso
- PENDING: nada para reembolsar

### 5. Documentos
- Disponível só para CONFIRMED, CHECKED_IN, CHECKED_OUT
- Impossível para PENDING ou CANCELLED

## Estrutura de Commits

O projeto usa as seguintes convenções:

```bash
git commit -m "feat: adiciona endpoint de relatório"
git commit -m "fix: corrige cálculo de desconto"
git commit -m "test: adiciona testes de preço"
git commit -m "refactor: simplifica lógica de validação"
```

## Performance

O sistema foi projetado para:
- Suportar centenas de reservas
- Gerar relatórios em segundos
- Validações em tempo real
- Sem cache (dados sempre frescos do banco)

Para escala maior, considere:
- Índices em PostgreSQL nas colunas de data e status
- Cache de quartos (mudam raramente)
- Particionamento de tabela de reservas por ano

## Problemas Comuns

### Erro: "DB_URL environment variable not set"
```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="pousada"
export DB_PASSWORD="senha"
```

### Erro: "Connection refused"
PostgreSQL não está rodando. Inicie com Docker ou o serviço local.

### Erro: "23505 unique violation"
Tentou inserir reserva com ID que já existe. IDs são auto-incrementados.

### Erro: "ROOM_UNAVAILABLE" quando parece disponível
Verifique se há reserva CONFIRMED/CHECKED_IN/CHECKED_OUT no período.
CANCELLED e PENDING não bloqueiam.

## Debugging

### Ver logs de SQL
```bash
export JDBC_DEBUG=true
```

### Verificar estado do banco
```sql
psql -U pousada -d pousada -c "SELECT COUNT(*) FROM reservations;"
```

### Limpar dados para novo teste
```sql
DELETE FROM messages;
DELETE FROM reservations;
ALTER SEQUENCE reservations_id_seq RESTART WITH 1;
```

## Próximas Funcionalidades (Não Implementadas)

- Relatório de cancelamentos
- Relatório de hóspedes por estado
- Data de geração dos relatórios
- Atualização de taxa de turismo (configurável)
- API de atualização de preços sazonais
- Integração com sistema de pagamento real
- Autenticação/autorização
- Histórico de alterações
