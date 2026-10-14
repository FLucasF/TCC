# Configuração do Serviço de Pedidos

## Pré-requisitos

- Java 21 ou superior
- PostgreSQL 18 ou superior
- Maven 3.9 ou superior (opcional, pode usar `mvnw`)

## Instalação de Dependências

### Windows (WSL2)

```bash
# Instalar PostgreSQL via apt
sudo apt update
sudo apt install postgresql postgresql-contrib

# Iniciar o PostgreSQL
sudo service postgresql start

# Criar banco de dados
sudo -u postgres createdb loja_pedidos
```

### macOS (via Homebrew)

```bash
# Instalar PostgreSQL
brew install postgresql

# Iniciar o PostgreSQL
brew services start postgresql

# Criar banco de dados
createdb loja_pedidos
```

### Linux (Ubuntu/Debian)

```bash
# Instalar PostgreSQL
sudo apt update
sudo apt install postgresql postgresql-contrib

# Iniciar o PostgreSQL
sudo systemctl start postgresql

# Criar banco de dados
sudo -u postgres createdb loja_pedidos
```

## Variáveis de Ambiente

### Desenvolvimento Local

```bash
export DB_URL=jdbc:postgresql://localhost:5432/loja_pedidos
export DB_USER=postgres
export DB_PASSWORD=postgres
export TRANSLOG_URL=http://localhost:9999
```

### Docker

```bash
# Executar PostgreSQL em Docker
docker run --name loja-db -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=loja_pedidos -p 5432:5432 -d postgres:15

# Variáveis de ambiente
export DB_URL=jdbc:postgresql://localhost:5432/loja_pedidos
export DB_USER=postgres
export DB_PASSWORD=postgres
```

## Inicialização do Banco de Dados

O serviço cria automaticamente as tabelas na primeira execução usando o Hibernate com `ddl-auto: create`.

### Operações Manual (Opcional)

```sql
-- Conectar ao PostgreSQL
psql -U postgres -d loja_pedidos

-- Ver tabelas
\dt

-- Ver dados de uma tabela
SELECT * FROM produtos;

-- Consultar estoque
SELECT sku, nome, estoque FROM produtos;
```

## Inicialização do Catálogo

Ao iniciar o serviço, o arquivo `DataInitializer` cria automaticamente os produtos do catálogo:

- CAM-001: Camiseta básica
- TEN-001: Tênis corrida
- FON-001: Fone sem fio
- MEI-001: Meia esportiva
- JAQ-001: Jaqueta corta-vento
- MOC-001: Mochila urbana
- BON-001: Boné aba reta
- REL-001: Relógio digital (fora de linha)

## Compilação e Execução

### Via Maven

```bash
# Compilar
mvn clean compile

# Executar
mvn spring-boot:run

# Testes
mvn clean test

# Verificação completa
mvn clean verify
```

### Via JAR

```bash
# Criar JAR
mvn clean package

# Executar JAR
java -jar target/pedidos-1.0.0.jar
```

## Configuração da TransLog

A TransLog é simulada para testes. Você pode:

1. **Usar a URL padrão** (local)
   ```bash
   export TRANSLOG_URL=http://localhost:9999
   ```

2. **Implementar um mock**
   ```bash
   # Criar um servidor mock na porta 9999 que responda:
   # GET /cotacao?cep=01310100&pesoKg=1.5&servico=EXPRESSA
   # Response: { "valor": 35.50, "prazoDias": 3 }
   ```

3. **Usar stub para testes**
   - Os testes já incluem mocks do TransLogClient

## Verificação de Saúde

```bash
# Verificar se o serviço está rodando
curl http://localhost:8080/actuator/health 2>/dev/null || echo "Serviço não está respondendo"

# Obter informações da aplicação
curl http://localhost:8080/actuator/info 2>/dev/null
```

## Logs

Os logs são salvos em stdout por padrão. Para redirecionar:

```bash
mvn spring-boot:run > logs.txt 2>&1 &
```

Configuração de níveis em `application.yml`:

```yaml
logging:
  level:
    root: INFO
    com.loja.pedidos: DEBUG
```

## Troubleshooting

### Erro: "Cannot drop database. It is being accessed by other users"
```bash
# Terminar conexões ativas
psql -U postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'loja_pedidos';"

# Depois dropar o banco
dropdb loja_pedidos
```

### Erro: "Connection refused"
- Verificar se PostgreSQL está rodando: `sudo systemctl status postgresql`
- Verificar porta: `netstat -an | grep 5432`

### Erro: "Port 8080 already in use"
```bash
# Encontrar processo na porta 8080
lsof -i :8080

# Matar o processo (substituir PID)
kill -9 <PID>
```

## Profiling

Para análise de performance:

```bash
# Com Spring Boot Actuator (já incluído)
curl http://localhost:8080/actuator/metrics

# Ver tempo de requisições
curl http://localhost:8080/actuator/metrics/http.server.requests
```
