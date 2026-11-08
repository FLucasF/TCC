# Como Executar o Sistema

## Opção 1: Com Docker (Recomendado)

### Passo 1: Iniciar o PostgreSQL
```bash
docker-compose up -d
```

### Passo 2: Compilar o projeto
```bash
mvn clean package
```

### Passo 3: Executar a aplicação
```bash
DB_URL="jdbc:postgresql://localhost:5432/pousada" \
DB_USER="pousada" \
DB_PASSWORD="pousada123" \
PORT=7070 \
java -jar target/mare-alta-1.0.0.jar
```

O servidor estará disponível em: `http://localhost:7070`

## Opção 2: Sem Docker

### Passo 1: Configurar PostgreSQL
- Instale PostgreSQL 18
- Crie um banco de dados chamado `pousada`
- Crie um usuário com permissões no banco

### Passo 2: Compilar e executar
```bash
mvn clean package

DB_URL="jdbc:postgresql://localhost:5432/pousada" \
DB_USER="seu_usuario" \
DB_PASSWORD="sua_senha" \
PORT=7070 \
java -jar target/mare-alta-1.0.0.jar
```

## Testando a API

### Com curl (opção 1 - manual)
```bash
# Listar quartos
curl http://localhost:7070/rooms

# Criar uma reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@email.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq '.'
```

### Com script de teste (opção 2 - automático)
```bash
chmod +x test-api.sh
./test-api.sh
```

Este script testará:
- GET /rooms
- POST /reservations (vários planos: STANDARD, CORPORATE, WEEKEND_PACKAGE, LONG_STAY)
- POST /reservations/{id}/confirmation
- POST /reservations/{id}/check-in
- POST /reservations/{id}/check-out
- GET /reservations/{id}/document
- GET /messages
- GET /reports/occupancy
- GET /reports/revenue

## Verificando os Dados

### Acessar o banco de dados
```bash
# Com psql
psql -h localhost -U pousada -d pousada

# Ver tabelas
\dt

# Ver reservas criadas
SELECT * FROM reservations;

# Ver mensagens (emails) enviadas
SELECT * FROM messages;
```

## Parar o servidor

- `Ctrl+C` no terminal onde o servidor está rodando

## Parar o Docker

```bash
docker-compose down
```

## Troubleshooting

### Erro de conexão com PostgreSQL
- Verifique se o container está rodando: `docker ps`
- Verifique as credenciais nas variáveis de ambiente
- Aguarde alguns segundos para o PostgreSQL iniciar completamente

### Erro "Port already in use"
- Mude a PORT: `PORT=8080 java -jar target/mare-alta-1.0.0.jar`

### Erro ao compilar
```bash
# Limpar cache e tentar novamente
mvn clean
mvn package
```
