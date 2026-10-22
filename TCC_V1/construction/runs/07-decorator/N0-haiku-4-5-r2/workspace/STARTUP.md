# Como iniciar o sistema

## Pré-requisitos

- Java 21+
- Maven 3.8+
- Docker e Docker Compose (opcional, para banco de dados)
- curl e jq (para testar a API)

## Setup do banco de dados

### Opção 1: Docker Compose (recomendado)

```bash
docker-compose up -d postgres
```

Isso inicia um PostgreSQL 18 com as credenciais:
- DB: pousada
- User: pousada_user
- Password: pousada_pass
- Host: localhost
- Port: 5432

### Opção 2: PostgreSQL instalado localmente

```bash
createdb pousada
```

E configure o usuário/senha conforme necessário.

## Compilação

```bash
mvn clean package
```

## Execução

### Modo produção

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="pousada_user"
export DB_PASSWORD="pousada_pass"
export PORT=7070
export HOTEL_TEST_MODE=false

java -jar target/mare-alta.jar
```

### Modo teste (testes de e-mail)

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="pousada_user"
export DB_PASSWORD="pousada_pass"
export PORT=7070
export HOTEL_TEST_MODE=true

java -jar target/mare-alta.jar
```

A aplicação estará disponível em http://localhost:7070

## Testando a API

### 1. Script automático

```bash
./test-api.sh
```

### 2. Testando endpoints manualmente

#### Listar quartos
```bash
curl http://localhost:7070/rooms | jq
```

#### Criar reserva
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Maria Silva",
    "guestEmail": "maria@gmail.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq
```

#### Confirmar reserva
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 599.76}' | jq
```

#### Fazer check-in
```bash
curl -X POST http://localhost:7070/reservations/1/check-in \
  -H "Content-Type: application/json" \
  -d '{}' | jq
```

#### Fazer check-out
```bash
curl -X POST http://localhost:7070/reservations/1/check-out \
  -H "Content-Type: application/json" \
  -d '{}' | jq
```

#### Relatório de ocupação
```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-11"
```

#### Relatório financeiro
```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

#### Listar mensagens enviadas
```bash
curl http://localhost:7070/messages | jq
```

## Modo teste de e-mails

Quando `HOTEL_TEST_MODE=true`:

1. Todos os e-mails são redirecionados para `testes@pousadamarealta.com.br`
2. O assunto é prefixado com `[TESTE]`
3. A primeira linha do corpo contém "Destinatário original: {email}"
4. Não gera cópia para arquivo

Você pode verificar as mensagens com:
```bash
curl http://localhost:7070/messages | jq '.[] | select(.to == "testes@pousadamarealta.com.br")'
```

## Limpeza

### Parar o PostgreSQL
```bash
docker-compose down
```

### Remover volume de dados
```bash
docker-compose down -v
```

## Troubleshooting

### Erro de conexão com banco de dados
- Verifique se o PostgreSQL está rodando
- Verifique as variáveis de ambiente (DB_URL, DB_USER, DB_PASSWORD)
- Verifique se a porta 5432 está aberta

### Erro de compilação
```bash
mvn clean compile
```

### Erro de testes
```bash
mvn clean test
```

## Estrutura do projeto

```
├── pom.xml                 # Configuração Maven
├── README.md               # Documentação da API
├── STARTUP.md              # Este arquivo
├── docker-compose.yml      # Configuração Docker
├── test-api.sh             # Script de teste da API
├── src/
│   ├── main/java/
│   │   └── com/pousada/marealta/
│   │       ├── Main.java                 # Aplicação principal
│   │       ├── api/                      # Classes de API
│   │       ├── db/                       # Camada de banco de dados
│   │       ├── models/                   # Modelos de dados
│   │       ├── services/                 # Serviços de negócio
│   │       └── util/                     # Utilidades
│   └── test/java/
│       └── com/pousada/marealta/
│           ├── EmailValidatorTest.java
│           └── PricingTest.java
└── target/                 # Saída da compilação (gerado)
```
