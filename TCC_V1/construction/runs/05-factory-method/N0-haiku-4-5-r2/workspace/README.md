# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Requisitos

- Java 21
- Maven 3.9+
- PostgreSQL 18+
- Docker (opcional, para executar o PostgreSQL)

## Compilação

```bash
mvn clean package
```

Gera um JAR executável em `target/mare-alta-1.0.0.jar`

## Execução

### 1. Iniciar PostgreSQL (via Docker)

```bash
docker run --name postgres-marealta \
  -e POSTGRES_USER=pousada \
  -e POSTGRES_PASSWORD=senha123 \
  -e POSTGRES_DB=marealta \
  -p 5432:5432 \
  postgres:18
```

### 2. Executar o Sistema

```bash
java -jar target/mare-alta-1.0.0.jar
```

Com variáveis de ambiente customizadas:

```bash
DB_URL="jdbc:postgresql://localhost:5432/marealta" \
DB_USER="pousada" \
DB_PASSWORD="senha123" \
PORT=7070 \
java -jar target/mare-alta-1.0.0.jar
```

O sistema estará disponível em `http://localhost:7070`

## Testes

```bash
mvn verify
```

Executa os testes de unidade (pricing)

## API

### Quartos

```
GET /rooms
```

Retorna a lista de quartos com capacidade e diárias.

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

#### Tipos de Identificação

A reserva pode ser feita de três formas:

**1. CPF (Brasileiro Individual)**
```json
{
  "guestCpf": "111.222.333-44",
  ...
}
```
Retorna: Comprovante de Reserva

**2. CNPJ (Empresa)**
```json
{
  "companyCnpj": "12.345.678/0001-90",
  "companyName": "Tecnosul Ltda",
  ...
}
```
Retorna: Fatura de Hospedagem com ISS retido

**3. Passaporte (Estrangeiro)**
```json
{
  "guestPassport": "X1234567",
  "guestCountry": "United Kingdom",
  ...
}
```
Retorna: Booking Receipt em inglês

### Mensagens

```
GET /messages
```

Lista de mensagens enviadas (emails de confirmação, etc)

### Relatórios

```
GET /reports/occupancy?date=2027-07-10
GET /reports/revenue?month=2027-07
```

Retorna dados em formato CSV com charset utf-8.

## Funcionalidades

### Tarifas

- **Alta Temporada**: 15 dez - 28 fev e julho inteiro (+20% na diária)
- **Plano STANDARD**: Preço cheio, reembolso integral em caso de cancelamento
- **Plano NON_REFUNDABLE**: 15% de desconto, sem reembolso em cancelamento

### Estados da Reserva

1. **PENDING**: Criada, aguardando pagamento
2. **CONFIRMED**: Pagamento recebido
3. **CHECKED_IN**: Hóspede já está na pousada
4. **CHECKED_OUT**: Hóspede saiu
5. **CANCELLED**: Cancelada (antes do check-in apenas)

### Documentos

- **Comprovante de Reserva** (CPF): português, com CPF do hóspede
- **Fatura de Hospedagem** (CNPJ): português, com ISS 5% retido
- **Booking Receipt** (Passaporte): inglês, com formato internacionalizado

Os documentos são:
- Enviados por email na confirmação
- Disponíveis no check-out
- Disponíveis como segunda via via `/document`

### Arredondamento

Usa banker's rounding (round-half-to-even) em todos os valores monetários:
- Preço de cada noite
- Soma das noites
- Desconto
- Total
- ISS retido

## Estrutura do Banco de Dados

- `rooms`: 5 quartos pré-cadastrados
- `reservations`: histórico completo de reservas
- `messages`: log de mensagens enviadas

O schema é criado automaticamente na primeira execução.

## Desenvolvimento

### Estrutura do Projeto

```
src/
├── main/
│   └── java/
│       └── com/pousada/
│           ├── App.java
│           ├── api/
│           ├── db/
│           ├── model/
│           ├── repository/
│           ├── service/
│           └── util/
└── test/
    └── java/
        └── com/pousada/
```

### Adicionar Novos Endpoints

1. Criar classe em `api/`
2. Usar `ReservationService` para lógica
3. Chamar `setup()` em `App.java`

## Troubleshooting

**Erro de conexão com banco**: Verifique se PostgreSQL está rodando e as variáveis de ambiente estão corretas.

**Conflito de porta**: Altere a variável `PORT`.

**Erro de arredondamento**: Use `BigDecimal` com `RoundingMode.HALF_EVEN` sempre.
