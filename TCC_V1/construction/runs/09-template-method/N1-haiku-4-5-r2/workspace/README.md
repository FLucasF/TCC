# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido com Java 21, Javalin 6.7.0 e PostgreSQL 18.

## Requisitos

- Java 21+
- PostgreSQL 18+
- Maven 3.8+

## Configuração

### 1. Variáveis de Ambiente

Configure as seguintes variáveis de ambiente:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=sua_senha
export PORT=7070
```

Ou copie o arquivo `.env.example` e customize:

```bash
cp .env.example .env
```

### 2. Banco de Dados

Crie o banco de dados PostgreSQL:

```bash
createdb pousada
```

O sistema cria automaticamente as tabelas na primeira execução.

## Build e Execução

### Build

```bash
mvn clean package
```

Isso gera um JAR executável em `target/pousada.jar`.

### Executar

```bash
java -jar target/pousada.jar
```

O servidor inicia na porta 7070 (ou conforme configurado em `PORT`).

## Testes

Executar todos os testes:

```bash
mvn test
```

Executar com verificação completa (testes + build):

```bash
mvn verify
```

## API Endpoints

### Quartos

```
GET /rooms
```

Retorna a lista de quartos disponíveis.

### Reservas

```
POST /reservations
GET /reservations/{id}
POST /reservations/{id}/confirmation
POST /reservations/{id}/cancellation
POST /reservations/{id}/check-in
POST /reservations/{id}/check-out
GET /reservations/{id}/document
```

### Mensagens

```
GET /messages
```

Retorna lista de mensagens enviadas (comprovantes por email).

### Relatórios

```
GET /reports/occupancy?date=2027-07-10
GET /reports/revenue?month=2027-07
GET /reports/tourism-tax?month=2027-07
GET /reports/rate-plans?month=2027-07
```

Todos os relatórios retornam CSV com charset UTF-8.

## Estrutura do Projeto

```
src/
├── main/java/com/pousadamarealta/
│   ├── Application.java                 # Ponto de entrada
│   ├── handler/                         # Controladores REST
│   ├── model/                           # Entidades (Room, Reservation, Message)
│   ├── repository/                      # Acesso ao banco de dados
│   ├── service/                         # Lógica de negócio
│   ├── infrastructure/                  # Configuração de banco de dados
│   └── util/                            # Utilitários (cálculos, formatação)
└── test/java/com/pousadamarealta/      # Testes unitários
```

## Características

- ✅ Gerenciamento de 5 quartos (2 STANDARD, 2 DELUXE, 1 SUITE)
- ✅ Sistema de reservas com múltiplos status
- ✅ Cálculo automático de preços com alta temporada
- ✅ Suporte a planos de tarifa (STANDARD e NON_REFUNDABLE)
- ✅ Arredondamento HALF_EVEN para valores monetários
- ✅ Geração de comprovantes
- ✅ Envio automático de emails de confirmação
- ✅ 4 relatórios em CSV: ocupação, financeiro, taxa de turismo e planos de tarifa
- ✅ Validação completa de inputs
- ✅ Testes unitários

## Exemplo de Uso

Criar uma reserva:

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

Confirmar a reserva:

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{
    "amountPaid": 599.76
  }'
```

Obter relatório de ocupação:

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

## Script de Teste

Executar teste manual completo:

```bash
bash test.sh
```

(Requer `curl` e `jq` instalados)

## Notas Importantes

- O comprovante é gerado automaticamente quando uma reserva é confirmada
- Os comprovantes também estão disponíveis via GET /reservations/{id}/document
- Todos os valores monetários usam arredondamento HALF_EVEN (meio para o par)
- Alta temporada: 15 de dezembro até 28/29 de fevereiro + mês de julho
- A taxa de turismo é R$ 4,50 por hóspede por noite
- Relatórios listam apenas reservas pagas (CONFIRMED, CHECKED_IN, CHECKED_OUT)
