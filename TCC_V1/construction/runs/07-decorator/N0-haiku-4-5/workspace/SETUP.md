# Guia de Setup e Desenvolvimento

## Requisitos

- Java 21+
- Maven 3.6+
- PostgreSQL 18+
- Docker (opcional, para facilitar setup do PostgreSQL)

## Setup Rápido

### 1. Preparar o banco de dados

**Opção A: Com Docker (recomendado)**

```bash
docker-compose up -d
```

Isso inicia PostgreSQL na porta 5432 com credenciais padrão:
- User: `postgres`
- Password: `password`
- Database: `pousada`

Opcional: acesse pgAdmin em http://localhost:5050 para gerenciar o banco visualmente.

**Opção B: PostgreSQL local**

Certifique-se que PostgreSQL está rodando e crie o banco:

```sql
CREATE DATABASE pousada;
```

### 2. Compilar o projeto

```bash
mvn clean package
```

Isso compila o código, roda os 20 testes unitários e gera o JAR em `target/mare-alta.jar`.

### 3. Configurar variáveis de ambiente

Copie `.env.example` para `.env` e ajuste conforme necessário:

```bash
cp .env.example .env
```

Edite `.env` com suas credenciais do PostgreSQL.

### 4. Executar o servidor

**Opção A: Script de conveniência**

```bash
./run.sh
```

**Opção B: Direto com Java**

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=password
export PORT=7070

java -jar target/mare-alta.jar
```

Servidor inicia em http://localhost:7070

## Desenvolvimento

### Estrutura do projeto

```
src/main/java/com/pousada/
├── Application.java              # Ponto de entrada
├── api/
│   ├── ApiController.java       # Endpoints REST
│   ├── ApiMapper.java           # Mapeamento modelo <-> DTO
│   └── dto/                     # Objetos de transferência de dados
├── database/
│   └── Database.java            # Conexão e inicialização do BD
├── models/                      # Entidades do domínio
├── repositories/                # Acesso aos dados
├── services/                    # Lógica de negócio
└── utils/                       # Funções auxiliares

src/test/java/com/pousada/
├── EmailValidatorTest.java
├── MoneyTest.java
├── DateUtilTest.java
└── CpfFormatterTest.java
```

### Adicionar nova funcionalidade

1. **Adicionar testes** em `src/test/java`
2. **Implementar lógica** (models, services, repositories)
3. **Expor via API** (ApiController, DTOs)
4. **Rodar testes**: `mvn test`
5. **Compilar**: `mvn clean package`

### Fazer mudanças no banco

O schema é criado automaticamente na primeira execução em `Database.initialize()`.

Para fazer alterações estruturais:
1. Modifique `Database.java` com os novos `CREATE TABLE` ou `ALTER TABLE`
2. Delete o banco: `docker-compose down -v` (ou no PostgreSQL: `DROP DATABASE pousada`)
3. Recompile e execute

### Rodar testes unitários

```bash
mvn test
```

Ou com cobertura:

```bash
mvn verify
```

### Verificar erros de compilação

```bash
mvn compile
```

## Modo de Teste

Para testar sem enviar e-mails reais:

```bash
HOTEL_TEST_MODE=true ./run.sh
```

Todos os e-mails serão redirecionados para `testes@pousadamarealta.com.br`.

## Desabilitar Cópia para Arquivo

Para não fazer cópia automática para `arquivo@pousadamarealta.com.br`:

```bash
HOTEL_ARCHIVE_DISABLED=true ./run.sh
```

## Debugging

### Logs da aplicação

Os logs aparecem no stdout. Para aumentar o nível:

Edite `src/main/resources/simplelogger.properties` (crie se não existir):
```properties
org.slf4j.simpleLogger.defaultLogLevel=debug
```

### Testar endpoints

Use `curl` ou Postman. Exemplos:

```bash
# Listar quartos
curl http://localhost:7070/rooms | jq

# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Teste",
    "guestEmail": "teste@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq
```

Veja `EXAMPLE_API.md` para mais exemplos.

## Troubleshooting

### Erro: "Connection refused"

PostgreSQL não está rodando. Inicie com:
```bash
docker-compose up -d
```

### Erro: "column ... does not exist"

Schema não foi criado. Verifique que `Database.initialize()` é chamado na inicialização.

### Erro: "Email validation failed"

Verifique que `HOTEL_TEST_MODE=true` ou confira o domínio da válido (deve ter um ponto).

## Deploy em Produção

1. Compilar com otimizações:
   ```bash
   mvn clean package -DskipTests -Dmaven.compiler.optimize=true
   ```

2. Configurar variáveis de ambiente no servidor

3. Usar um gerenciador de processos (systemd, supervisor)

4. Configurar SSL/TLS com proxy reverso (nginx, Apache)

5. Monitorar logs e performance

## Performance

- O JAR (~7.2MB) é auto-contido com todas as dependências
- PostgreSQL com índices automáticos em chaves primárias
- Conexões reutilizadas via DriverManager (sem pool por enquanto)
- Arredondamento HALF_EVEN em todas operações monetárias

## Licença

Desenvolvido para Pousada Maré Alta.
