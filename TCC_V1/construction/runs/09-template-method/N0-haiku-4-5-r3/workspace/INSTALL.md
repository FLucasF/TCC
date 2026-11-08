# Guia de Instalação - Pousada Maré Alta

## Pré-requisitos

- Java 21 ou superior
- PostgreSQL 18 ou superior
- Maven 3.6+

## Instalação do PostgreSQL

### Linux (Ubuntu/Debian)
```bash
sudo apt-get update
sudo apt-get install postgresql postgresql-contrib
sudo service postgresql start
```

### macOS (com Homebrew)
```bash
brew install postgresql
brew services start postgresql
```

### Windows
Baixe o instalador em: https://www.postgresql.org/download/windows/

## Configuração do Banco de Dados

1. Crie um novo usuário e banco de dados:

```bash
psql -U postgres

postgres=# CREATE USER marea_alta WITH PASSWORD 'sua_senha';
postgres=# CREATE DATABASE marea_alta OWNER marea_alta;
postgres=# GRANT ALL PRIVILEGES ON DATABASE marea_alta TO marea_alta;
postgres=# \q
```

2. Teste a conexão:

```bash
psql -U marea_alta -d marea_alta -h localhost
```

## Build do Projeto

```bash
cd /workspace
mvn clean package
```

Isso vai gerar o arquivo `target/marea-alta.jar`

## Executar o Servidor

### Opção 1: Com variáveis de ambiente

```bash
export DB_URL="jdbc:postgresql://localhost:5432/marea_alta"
export DB_USER="marea_alta"
export DB_PASSWORD="sua_senha"
export PORT="8080"

java -jar target/marea-alta.jar
```

### Opção 2: Em uma única linha

```bash
DB_URL="jdbc:postgresql://localhost:5432/marea_alta" \
DB_USER="marea_alta" \
DB_PASSWORD="sua_senha" \
PORT="8080" \
java -jar target/marea-alta.jar
```

## Verificar que o Servidor Está Rodando

```bash
curl http://localhost:8080/rooms
```

Você deve receber um JSON com os 5 quartos da pousada.

## Testes

Execute todos os testes:

```bash
mvn verify
```

## Script de Teste da API

Existe um script `test-api.sh` que faz testes completos da API:

```bash
chmod +x test-api.sh
./test-api.sh
```

(Certifique-se de que o servidor está rodando antes de executar o script)

## Solução de Problemas

### Erro: "Unable to connect to PostgreSQL"

- Verifique se o PostgreSQL está rodando
- Verifique as credenciais no arquivo env
- Verifique a URL JDBC

### Erro: "Database already exists"

- O banco foi criado corretamente na primeira execução
- Você pode deletar e recriar se necessário:

```bash
psql -U postgres

postgres=# DROP DATABASE marea_alta;
postgres=# CREATE DATABASE marea_alta OWNER marea_alta;
```

### Porta já está em uso

Se a porta 8080 (ou a que você configurou) já está em uso, altere a variável `PORT`:

```bash
PORT="8081" java -jar target/marea-alta.jar
```

## Estrutura do Banco de Dados

Após executar, as seguintes tabelas são criadas automaticamente:

- `rooms`: Quartos da pousada
- `reservations`: Reservas dos hóspedes
- `messages`: E-mails enviados

Não é necessário executar nenhum script SQL manualmente.
