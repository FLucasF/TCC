# Configuração do Banco de Dados

## Instalação do PostgreSQL

### Linux (Ubuntu/Debian)
```bash
sudo apt-get update
sudo apt-get install postgresql postgresql-contrib
sudo service postgresql start
```

### macOS (com Homebrew)
```bash
brew install postgresql@18
brew services start postgresql@18
```

### Windows
Baixe e execute o instalador de https://www.postgresql.org/download/windows/

## Criar Banco de Dados

```bash
sudo -u postgres psql

# No prompt do psql:
CREATE USER pousada WITH PASSWORD 'pousada123';
CREATE DATABASE pousada OWNER pousada;
GRANT ALL PRIVILEGES ON DATABASE pousada TO pousada;
\q
```

## Iniciar a Aplicação

1. Copie `.env.example` para `.env` e ajuste as credenciais se necessário:
```bash
cp .env.example .env
```

2. Compile a aplicação:
```bash
mvn clean package
```

3. Execute:
```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="pousada"
export DB_PASSWORD="pousada123"
export PORT=7070
java -jar target/mare-alta-1.0.0.jar
```

A aplicação irá:
1. Conectar ao banco de dados
2. Criar as tabelas automaticamente (se não existirem)
3. Inserir os 5 quartos da pousada
4. Iniciar o servidor na porta 7070

## Testando a Conexão

```bash
curl http://localhost:7070/rooms
```

Se retornar os 5 quartos em JSON, a aplicação está funcionando corretamente.

## Docker Compose (Opcional)

Para facilitar, você pode usar Docker Compose:

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:18
    environment:
      POSTGRES_USER: pousada
      POSTGRES_PASSWORD: pousada123
      POSTGRES_DB: pousada
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  app:
    build: .
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/pousada
      DB_USER: pousada
      DB_PASSWORD: pousada123
      PORT: 7070
    ports:
      - "7070:7070"
    depends_on:
      - postgres

volumes:
  postgres_data:
```

Salve como `docker-compose.yml` e execute:
```bash
docker-compose up
```
