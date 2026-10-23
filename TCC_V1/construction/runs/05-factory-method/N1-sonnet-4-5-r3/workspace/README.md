# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Requisitos

- Java 21
- Maven 3.6+
- PostgreSQL 18 (ou use o Docker Compose incluído)

## Configuração e Execução

### 1. Iniciar o banco de dados

```bash
docker-compose up -d
```

### 2. Compilar o projeto

```bash
mvn package
```

### 3. Executar o sistema

```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=pousada \
DB_PASSWORD=pousada123 \
java -jar target/reservas-1.0.0.jar
```

O sistema estará disponível em `http://localhost:7070`

Para usar uma porta diferente, defina a variável de ambiente `PORT`.

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Realiza check-in
- `POST /reservations/{id}/check-out` - Realiza check-out e retorna comprovante
- `GET /reservations/{id}/document` - Obtém segunda via do comprovante

### Mensagens

- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro

## Exemplo de Criação de Reserva

### Reserva com CPF

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Reserva com CNPJ (Empresa)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Ana Souza",
    "guestEmail": "ana@tecnosul.com",
    "companyCnpj": "12.345.678/0001-90",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Reserva com Passaporte (Estrangeiro)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "301",
    "guestName": "John Smith",
    "guestEmail": "john@example.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 3,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

## Testes

```bash
mvn test
```

## Verificação Completa

```bash
mvn verify
```

## Arquitetura

O sistema foi desenvolvido seguindo os princípios do CLAUDE.md:

- **Organização por tipo de identificação**: Cada tipo de hóspede (CPF, CNPJ, Passaporte) possui sua própria classe que sabe gerar o documento apropriado. Novos tipos podem ser adicionados sem modificar os existentes.

- **Separação de responsabilidades**:
  - `model`: Entidades de domínio e lógica de negócio
  - `repository`: Acesso a dados
  - `service`: Regras de negócio e orquestração
  - `controller`: Endpoints da API
  - `api`: DTOs de request/response

## Regras de Negócio

- **Alta temporada**: 15/12 a fim de fevereiro + julho inteiro (+20% no preço)
- **Planos de tarifa**:
  - STANDARD: sem desconto, reembolsável
  - NON_REFUNDABLE: 15% de desconto, não reembolsável
- **Arredondamento**: Banker's rounding (meio para o par) em todos os cálculos
- **Estados da reserva**: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT (CANCELLED a qualquer momento antes do check-in)
