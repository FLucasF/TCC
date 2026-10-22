# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, um pequeno hotel no litoral com 5 quartos.

## Requisitos

- Java 21
- Maven 3.8+
- PostgreSQL 18+

## Instalação e Compilação

```bash
mvn clean package
```

Isto gerará um JAR executável em `target/app.jar`.

## Execução

### Com Docker (recomendado para testes)

```bash
docker run --name pousada-db -e POSTGRES_PASSWORD=senha123 -e POSTGRES_DB=pousada -p 5432:5432 -d postgres:18
```

### Executar a Aplicação

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=senha123
export PORT=7070

java -jar target/app.jar
```

Se as variáveis de ambiente não forem definidas, a aplicação usa:
- `PORT`: 7070 (padrão)
- Banco de dados: variáveis obrigatórias

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Obtém detalhes de uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva (com pagamento)
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Realiza check-in
- `POST /reservations/{id}/check-out` - Realiza check-out
- `GET /reservations/{id}/document` - Obtém segunda via do documento

### Mensagens
- `GET /messages` - Lista mensagens enviadas (confirmação de reservas)

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - Relatório financeiro (CSV)

## Estrutura de Dados

### Quartos
- 101, 102: STANDARD (2 pessoas, R$ 249,90/noite)
- 201, 202: DELUXE (3 pessoas, R$ 389,90/noite)
- 301: SUITE (4 pessoas, R$ 579,90/noite)

### Temporadas
- Alta temporada: 15 dez - 28 fev e mês de julho (acréscimo de 20%)

### Planos de Tarifa
- STANDARD: sem desconto
- NON_REFUNDABLE: 15% de desconto (não reembolsável)

### Tipos de Identificação
1. CPF - Pessoa física
2. CNPJ + Razão Social - Pessoa jurídica
3. Passaporte + País - Estrangeiros

## Exemplos de Requisição

### Criar Reserva (CPF)
```json
POST /reservations
{
  "guestName": "Carla Dias",
  "guestEmail": "carla@email.com",
  "guestCpf": "11122233344",
  "roomNumber": 201,
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

### Criar Reserva (CNPJ)
```json
POST /reservations
{
  "guestName": "Ana Souza",
  "guestEmail": "ana@empresa.com",
  "companyCnpj": "12345678000190",
  "companyName": "Tecnosul Ltda",
  "roomNumber": 201,
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 1,
  "ratePlan": "STANDARD"
}
```

### Criar Reserva (Passaporte)
```json
POST /reservations
{
  "guestName": "John Smith",
  "guestEmail": "john@email.com",
  "guestPassport": "X1234567",
  "guestCountry": "United Kingdom",
  "roomNumber": 201,
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 1,
  "ratePlan": "STANDARD"
}
```

### Confirmar Reserva
```json
POST /reservations/{id}/confirmation
{
  "amountPaid": 779.80
}
```

Sem `amountPaid`, usa o total da reserva.

## Testes

```bash
mvn test
mvn verify
```

## Arredondamento Monetário

Todos os valores monetários são arredondados com o método "meio para o par" (HALF_EVEN/banker's rounding) a cada passo de cálculo.

## Documentos Gerados

Três tipos de documentos são gerados conforme o tipo de identificação:

1. **Comprovante de Reserva** (CPF) - em português
2. **Fatura de Hospedagem** (CNPJ) - em português, com ISS retido 5%
3. **Booking Receipt** (Passaporte) - em inglês

Os documentos são enviados por email na confirmação e estão disponíveis via API após o check-out ou segunda via.
