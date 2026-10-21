# Pousada Maré Alta - Sistema de Reservas

Sistema de gestão de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0.

## Compilação e Execução

### Compilar o projeto

```bash
mvn clean package
```

### Executar o sistema

```bash
java -jar target/mare-alta-1.0.0.jar
```

Por padrão, o sistema inicia na porta 7070. Para usar outra porta, defina a variável de ambiente `PORT`:

```bash
PORT=8080 java -jar target/mare-alta-1.0.0.jar
```

### Executar os testes

```bash
mvn verify
```

## API REST

### Quartos

#### GET /rooms
Lista todos os quartos disponíveis.

**Resposta:**
```json
[
  {
    "number": "101",
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  }
]
```

### Reservas

#### POST /reservations
Cria uma nova reserva.

**Corpo da requisição com CPF:**
```json
{
  "roomNumber": "101",
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "123.456.789-00",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

**Corpo da requisição com CNPJ (empresa):**
```json
{
  "roomNumber": "201",
  "guestName": "Ana Souza",
  "guestEmail": "ana@example.com",
  "companyName": "Tecnosul Ltda",
  "companyCnpj": "12.345.678/0001-90",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

**Corpo da requisição com Passaporte (estrangeiro):**
```json
{
  "roomNumber": "202",
  "guestName": "John Smith",
  "guestEmail": "john@example.com",
  "guestPassport": "X1234567",
  "guestCountry": "United Kingdom",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "NON_REFUNDABLE"
}
```

**Planos de tarifa:**
- `STANDARD`: Tarifa normal, com direito a reembolso
- `NON_REFUNDABLE`: 15% de desconto, sem direito a reembolso

#### GET /reservations/{id}
Consulta uma reserva pelo ID.

#### POST /reservations/{id}/confirmation
Confirma uma reserva (registra pagamento e envia e-mail de confirmação).

#### POST /reservations/{id}/check-in
Realiza o check-in de uma reserva confirmada.

#### POST /reservations/{id}/check-out
Realiza o check-out e retorna o documento/comprovante.

**Resposta:**
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  "guestName": "João Silva",
  "guestCpf": "123.456.789-00",
  "companyName": null,
  "companyCnpj": null,
  "guestPassport": null,
  "guestCountry": null,
  "roomNumber": "101",
  "roomType": "STANDARD",
  "checkIn": "10/03/2027",
  "checkOut": "12/03/2027",
  "nights": 2,
  "subtotal": "499,80",
  "discount": null,
  "total": "499,80",
  "text": "Comprovante de reserva nº 1\nHóspede: João Silva (CPF 123.456.789-00)\n..."
}
```

#### GET /reservations/{id}/document
Retorna a segunda via do documento (comprovante/fatura/receipt).

#### POST /reservations/{id}/cancellation
Cancela uma reserva. Reembolso depende do plano de tarifa.

### Mensagens

#### GET /messages
Lista todas as mensagens enviadas pelo sistema (e-mails de confirmação).

### Relatórios

#### GET /reports/occupancy?date=2027-07-10
Relatório de ocupação para uma data específica (formato CSV).

**Exemplo de resposta:**
```csv
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
201,DELUXE,2,2027-07-10,2027-07-11
total,2/5
```

#### GET /reports/revenue?month=2027-07
Relatório financeiro para um mês específico (formato CSV).

**Exemplo de resposta:**
```csv
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CHECKED_OUT,599.76
2,201,2027-07-15,2027-07-16,CONFIRMED,467.88
total,1067.64
```

## Regras de Negócio

### Quartos
- **101, 102**: STANDARD, até 2 pessoas, R$ 249,90/noite
- **201, 202**: DELUXE, até 3 pessoas, R$ 389,90/noite
- **301**: SUITE, até 4 pessoas, R$ 579,90/noite

### Alta Temporada
- 15 de dezembro a fim de fevereiro
- Todo o mês de julho

Durante a alta temporada, o valor da diária aumenta 20%.

### Tipos de Identificação
- **CPF**: Pessoa física brasileira → gera Comprovante de reserva
- **CNPJ**: Empresa → gera Fatura de hospedagem (com ISS retido de 5%)
- **Passaporte**: Estrangeiro → gera Booking receipt (em inglês)

### Status da Reserva
1. **PENDING**: Criada mas não confirmada
2. **CONFIRMED**: Confirmada e paga
3. **CHECKED_IN**: Check-in realizado
4. **CHECKED_OUT**: Check-out realizado
5. **CANCELLED**: Cancelada

### Transições Permitidas
- PENDING → CONFIRMED (confirmação)
- CONFIRMED → CHECKED_IN (check-in)
- CHECKED_IN → CHECKED_OUT (check-out)
- PENDING → CANCELLED (cancelamento)
- CONFIRMED → CANCELLED (cancelamento)

## Códigos de Erro

- **400 INVALID_REQUEST**: Requisição inválida (campos faltando, formato incorreto)
- **404 ROOM_NOT_FOUND**: Quarto não encontrado
- **404 RESERVATION_NOT_FOUND**: Reserva não encontrada
- **409 ROOM_UNAVAILABLE**: Quarto já reservado para as datas solicitadas
- **409 OPERATION_NOT_ALLOWED**: Operação não permitida no status atual da reserva
- **422 CAPACITY_EXCEEDED**: Número de hóspedes excede a capacidade do quarto

## Arquitetura

O projeto segue princípios de design orientado a objetos e está organizado da seguinte forma:

- **domain**: Entidades do domínio (Room, Reservation, GuestIdentification, etc.)
- **repository**: Repositórios in-memory para persistência
- **service**: Lógica de negócio
- **controller**: Endpoints da API REST
- **dto**: Objetos de transferência de dados para requisições e respostas

### Extensibilidade

O sistema foi projetado para facilitar a adição de novos tipos de identificação de hóspedes (como agências de turismo). A interface `GuestIdentification` define o contrato e cada tipo de identificação implementa sua própria lógica de formatação e geração de documento, seguindo o princípio Open/Closed.

## Tecnologias

- Java 21
- Javalin 6.7.0
- Jackson (JSON)
- Maven
- JUnit 5

## Limitações

- Dados armazenados em memória (não persistem após reinicialização)
- E-mails não são enviados de verdade, apenas registrados internamente
