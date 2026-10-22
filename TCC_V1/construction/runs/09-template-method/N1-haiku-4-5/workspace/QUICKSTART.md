# Quick Start - Pousada Maré Alta

## Usando Docker Compose (Recomendado)

1. Compile o projeto:
   ```bash
   mvn clean package
   ```

2. Inicie os serviços:
   ```bash
   docker-compose up
   ```

   Isso iniciará:
   - PostgreSQL em `localhost:5432`
   - Aplicação em `localhost:7070`

## Sem Docker

### Pré-requisitos
- Java 21+
- PostgreSQL 18+

### Passos

1. Configure o PostgreSQL:
   ```bash
   createdb mare_alta
   ```

2. Compile o projeto:
   ```bash
   mvn clean package
   ```

3. Execute a aplicação:
   ```bash
   DB_URL=jdbc:postgresql://localhost:5432/mare_alta \
   DB_USER=postgres \
   DB_PASSWORD=sua_senha \
   PORT=7070 \
   java -jar target/mare-alta.jar
   ```

## Testando a API

Use o script de teste incluído:
```bash
bash test-api.sh
```

Ou use curl manualmente:
```bash
curl http://localhost:7070/rooms | jq
```

## Estrutura do Banco

O banco é criado automaticamente na primeira execução com as tabelas:
- `rooms` - Quartos da pousada
- `reservations` - Reservas
- `messages` - Mensagens enviadas

## Endpoints Principais

- `GET /rooms` - Lista quartos
- `POST /reservations` - Cria reserva
- `POST /reservations/{id}/confirmation` - Confirma
- `POST /reservations/{id}/check-in` - Check-in
- `POST /reservations/{id}/check-out` - Check-out
- `GET /reports/*` - Relatórios em CSV

Veja README.md para documentação completa.
