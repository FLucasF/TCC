# Guia de Setup - Pousada Maré Alta

## Pré-requisitos

- Java 21
- Maven 3.9+
- PostgreSQL 18+ (ou Docker)
- Docker e Docker Compose (opcional, para ambiente containerizado)

## Instalação Local

### 1. Clonar/Preparar o projeto

```bash
cd /workspace
```

### 2. Configurar o Banco de Dados PostgreSQL

#### Opção A: Com Docker

Crie um container PostgreSQL:

```bash
docker run -d \
  --name marealta-db \
  -e POSTGRES_USER=marealta \
  -e POSTGRES_PASSWORD=marealta123 \
  -e POSTGRES_DB=marealta \
  -p 5432:5432 \
  postgres:18-alpine
```

Aguarde alguns segundos para o banco estar pronto.

#### Opção B: PostgreSQL instalado localmente

Crie um banco de dados:

```bash
createdb -U postgres marealta
```

Se precisar de um usuário específico:

```sql
CREATE USER marealta WITH PASSWORD 'marealta123';
ALTER ROLE marealta CREATEDB;
GRANT ALL PRIVILEGES ON DATABASE marealta TO marealta;
```

### 3. Build do Projeto

```bash
mvn clean verify
```

Isso irá compilar o código, rodar os testes e gerar o JAR.

### 4. Rodar a Aplicação

```bash
export DB_URL="jdbc:postgresql://localhost:5432/marealta"
export DB_USER="marealta"
export DB_PASSWORD="marealta123"
export PORT=7070

java -jar target/marealta.jar
```

A aplicação estará disponível em `http://localhost:7070`

## Instalação com Docker Compose

### 1. Build e start

```bash
docker-compose up --build
```

Isso irá:
- Criar e rodar um container PostgreSQL
- Build a aplicação Java
- Rodar a aplicação

A aplicação estará disponível em `http://localhost:7070`

### 2. Parar os serviços

```bash
docker-compose down
```

Para apagar o volume do banco:

```bash
docker-compose down -v
```

## Verificação de Saúde

Teste se a API está funcionando:

```bash
curl http://localhost:7070/rooms
```

Deve retornar uma lista dos 5 quartos da pousada.

## Variáveis de Ambiente

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `DB_URL` | URL JDBC do PostgreSQL | Obrigatória |
| `DB_USER` | Usuário do banco de dados | Obrigatória |
| `DB_PASSWORD` | Senha do banco de dados | Obrigatória |
| `PORT` | Porta para rodar a API | 7070 |

## Estrutura de Diretórios

```
.
├── src/
│   ├── main/
│   │   ├── java/com/pousada/marealta/
│   │   └── resources/
│   └── test/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── README.md
├── SETUP.md
└── EXAMPLES.md
```

## Troubleshooting

### Erro de conexão ao banco de dados

Verifique se as variáveis de ambiente estão corretas:

```bash
echo "DB_URL: $DB_URL"
echo "DB_USER: $DB_USER"
echo "DB_PASSWORD: $DB_PASSWORD"
```

### Porta já em uso

Se a porta 7070 já está em uso, mude a variável de ambiente:

```bash
export PORT=8080
java -jar target/marealta.jar
```

### Build falha com erro de compilação

Certifique-se de ter Java 21:

```bash
java -version
```

Se não tiver, instale Java 21:

```bash
# macOS
brew install openjdk@21

# Ubuntu/Debian
sudo apt install openjdk-21-jdk

# Outros sistemas
# Acesse: https://www.oracle.com/java/technologies/downloads/
```

### PostgreSQL não inicia com Docker

Se receber erro de permissão, tente:

```bash
docker-compose down -v
docker-compose up --build
```

## Banco de Dados

### Tabelas criadas automaticamente

O sistema cria automaticamente as tabelas necessárias:

- `rooms` - Informações dos quartos
- `reservations` - Dados das reservas
- `messages` - E-mails enviados

### Quartos pré-configurados

Os 5 quartos são inseridos automaticamente na primeira execução:

| Número | Tipo | Capacidade | Diária |
|--------|------|-----------|--------|
| 101 | STANDARD | 2 | R$ 249,90 |
| 102 | STANDARD | 2 | R$ 249,90 |
| 201 | DELUXE | 3 | R$ 389,90 |
| 202 | DELUXE | 3 | R$ 389,90 |
| 301 | SUITE | 4 | R$ 579,90 |

## Logs

Os logs são exibidos no console. Para alterar o nível de log, edite `src/main/resources/logback.xml`.

## Testes

Execute os testes:

```bash
# Apenas testes unitários
mvn test

# Todos os testes (incluindo integração)
mvn verify
```

## Limpeza

Para remover artefatos de build:

```bash
mvn clean
```

Para remover tudo incluindo o banco de dados (Docker):

```bash
docker-compose down -v
```

## Próximos Passos

1. Leia [README.md](README.md) para documentação completa
2. Consulte [EXAMPLES.md](EXAMPLES.md) para exemplos de uso da API
3. Configure suas variáveis de ambiente
4. Rode a aplicação
5. Teste os endpoints com os exemplos fornecidos
