# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Requisitos

- Java 21
- Maven 3.6+
- PostgreSQL 18

## Compilação e Empacotamento

```bash
# Compilar
mvn clean compile

# Executar testes
mvn test

# Gerar JAR executável
mvn clean package

# Executar testes de verificação
mvn clean verify
```

## Execução

A aplicação requer as seguintes variáveis de ambiente:

- `DB_URL`: URL de conexão JDBC do PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do usuário
- `PORT` (opcional): Porta para servidor (padrão: 7070)

```bash
# Executar o JAR
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="senha"
export PORT=7070

java -jar target/pousada.jar
```

## API Endpoints

### Quartos

- **GET /rooms** - Lista todos os quartos

### Reservas

- **POST /reservations** - Cria nova reserva
  - Corpo: `{ roomNumber, guestName, guestEmail, checkIn, checkOut, guests, ratePlan, (guestCpf | (companyCnpj + companyName) | (guestPassport + guestCountry)) }`
  - Retorna: Reserva (201)

- **GET /reservations/{id}** - Obtém detalhes da reserva

- **POST /reservations/{id}/confirmation** - Confirma reserva e envia e-mail
  - Corpo: `{ amountPaid }`

- **POST /reservations/{id}/check-in** - Realiza check-in

- **POST /reservations/{id}/check-out** - Realiza check-out e retorna comprovante

- **POST /reservations/{id}/cancellation** - Cancela reserva

- **GET /reservations/{id}/document** - Obtém segunda via do documento

### Mensagens

- **GET /messages** - Lista todas as mensagens enviadas (e-mails)

### Relatórios

- **GET /reports/occupancy?date=2027-07-10** - Relatório de ocupação em CSV

- **GET /reports/revenue?month=2027-07** - Relatório financeiro em CSV

## Estrutura do Banco de Dados

O sistema cria automaticamente as seguintes tabelas:

- `rooms` - Quartos da pousada
- `reservations` - Reservas
- `messages` - Mensagens enviadas

## Tipos de Hóspede e Documentos

### 1. Pessoa com CPF
- Identificação: `guestCpf`
- Documento: Comprovante de Reserva em português

### 2. Empresa com CNPJ
- Identificação: `companyCnpj` + `companyName`
- Documento: Fatura de Hospedagem em português

### 3. Hóspede estrangeiro com Passaporte
- Identificação: `guestPassport` + `guestCountry`
- Documento: Booking Receipt em inglês

## Preços e Tarifas

### Tipos de Quarto
- **101, 102 (STANDARD)**: 2 pessoas, R$ 249,90/noite
- **201, 202 (DELUXE)**: 3 pessoas, R$ 389,90/noite
- **301 (SUITE)**: 4 pessoas, R$ 579,90/noite

### Planos de Tarifa
- **STANDARD**: Sem desconto, totalmente reembolsável
- **NON_REFUNDABLE**: 15% desconto, não reembolsável

### Alta Temporada
- Julho inteiro
- 15 de dezembro a fim de fevereiro
- Acréscimo de 20% nas diárias durante alta temporada

## Estados da Reserva

1. **PENDING** - Reserva criada, aguardando confirmação
2. **CONFIRMED** - Pagamento recebido
3. **CHECKED_IN** - Hóspede fez check-in
4. **CHECKED_OUT** - Hóspede fez check-out
5. **CANCELLED** - Reserva cancelada

## Cálculo de Preços

- Cada noite é calculada com arredondamento "meio para o par"
- Subtotal = soma das diárias
- Desconto = 15% do subtotal (apenas para NON_REFUNDABLE)
- Total = subtotal - desconto

## Reembolsos

- **STANDARD CONFIRMED**: 100% reembolsável
- **NON_REFUNDABLE**: 0% reembolsável (não reembolsável)
- **PENDING (qualquer plano)**: 0% (sem pagamento)

## Exemplo de Requisição

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```
