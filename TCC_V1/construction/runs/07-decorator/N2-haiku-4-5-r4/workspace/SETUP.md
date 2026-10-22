# Setup - Pousada Maré Alta Sistema de Reservas

## Pré-requisitos

- Java 21
- PostgreSQL 18
- Maven 3.6+

## Configuração do PostgreSQL

1. Criar o banco de dados:
```sql
CREATE DATABASE pousada;
```

2. Conectar ao banco:
```sql
\c pousada
```

O sistema criará as tabelas automaticamente na primeira execução.

## Variáveis de Ambiente

### Linux/Mac
```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="sua_senha_aqui"
export PORT="7070"
# Opcional: export HOTEL_TEST_MODE="true"
```

### Windows (CMD)
```cmd
set DB_URL=jdbc:postgresql://localhost:5432/pousada
set DB_USER=postgres
set DB_PASSWORD=sua_senha_aqui
set PORT=7070
REM set HOTEL_TEST_MODE=true
```

### Windows (PowerShell)
```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/pousada"
$env:DB_USER="postgres"
$env:DB_PASSWORD="sua_senha_aqui"
$env:PORT="7070"
# $env:HOTEL_TEST_MODE="true"
```

## Compilação

```bash
mvn clean package
```

Isso gerará um arquivo `target/hotel-system.jar`.

## Execução

```bash
java -jar target/hotel-system.jar
```

Se configurado corretamente, você verá:
```
16:52:57.XXX [main] INFO com.pousadamarealta.Main - Server started on port 7070
```

## Teste Rápido

Com a aplicação rodando, teste um endpoint:

```bash
curl http://localhost:7070/rooms
```

Você deve receber uma lista dos 5 quartos em JSON.

## Modo de Teste

O sistema pode rodar em modo de teste. Neste modo:
- Todos os e-mails são redirecionados para `testes@pousadamarealta.com.br`
- O assunto começa com `[TESTE]`
- A primeira linha do corpo inclui "Destinatário original: "
- Não é feita a cópia para `arquivo@pousadamarealta.com.br`

Para ativar:
```bash
export HOTEL_TEST_MODE="true"
java -jar target/hotel-system.jar
```

## Estrutura da Aplicação

- `src/main/java/com/pousadamarealta/db/` - Gerenciamento de banco de dados
- `src/main/java/com/pousadamarealta/model/` - Modelos de dados
- `src/main/java/com/pousadamarealta/service/` - Lógica de negócio
- `src/main/java/com/pousadamarealta/handler/` - Endpoints da API
- `src/main/java/com/pousadamarealta/util/` - Utilitários

## Troubleshooting

### Erro: "Failed to connect to database"
- Verifique se PostgreSQL está rodando
- Valide as credenciais em DB_USER e DB_PASSWORD
- Confirme que a porta do PostgreSQL é 5432 (padrão)

### Erro: "Port already in use"
- A porta 7070 está em uso
- Mude a variável `PORT` para outra porta (ex: 8080)

### Erro: "INVALID_REQUEST"
- Verifique se os dados enviados estão no formato correto
- Datas devem estar no formato YYYY-MM-DD
- O corpo da requisição deve ser JSON válido

## Logs

Os logs são exibidos no console durante a execução. Para melhor controle, edite `src/main/resources/logback.xml`.

## Documentação Completa

Veja `README.md` para a documentação completa da API.
