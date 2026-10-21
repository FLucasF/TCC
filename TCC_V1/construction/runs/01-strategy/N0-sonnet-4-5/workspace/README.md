# Pousada Maré Alta - Sistema de Reservas

Sistema de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin.

## Requisitos

- Java 21
- Maven 3.x

## Compilação

```bash
mvn package
```

Isso gera o arquivo `target/reservas-1.0.0.jar` que pode ser executado independentemente.

## Execução

```bash
java -jar target/reservas-1.0.0.jar
```

O servidor inicia na porta configurada pela variável de ambiente `PORT`, ou 7070 se não estiver definida.

```bash
PORT=8080 java -jar target/reservas-1.0.0.jar
```

## Testes

```bash
mvn verify
```

## Quartos

- **101, 102**: STANDARD (2 pessoas, R$ 249,90/noite)
- **201, 202**: DELUXE (3 pessoas, R$ 389,90/noite)
- **301**: SUITE (4 pessoas, R$ 579,90/noite)

## Alta Temporada

20% de acréscimo nas diárias:
- 15 de dezembro até fim de fevereiro
- Julho inteiro

## Planos de Tarifa

### 1. STANDARD (Normal)
- Sem desconto
- Reembolso total em caso de cancelamento

### 2. NON_REFUNDABLE (Não Reembolsável)
- 15% de desconto sobre as diárias
- Sem reembolso em caso de cancelamento

### 3. CORPORATE (Convênio)
- Requer código de empresa válido (TECNOSUL, AGROVALE ou FARMAVIDA)
- Descontos: TECNOSUL 12%, AGROVALE 18%, FARMAVIDA 25%
- Reembolso total em caso de cancelamento

### 4. WEEKEND_PACKAGE (Pacote de Fim de Semana)
- Entrada na sexta, saída no domingo (exatamente 2 noites)
- Preço fixo: STANDARD R$ 520,00, DELUXE R$ 820,00, SUITE R$ 1.200,00
- Reembolso de 50% em caso de cancelamento

### 5. LONG_STAY (Estadia Longa)
- Mínimo 7 noites
- 10% de desconto sobre as diárias
- Alta temporada continua valendo nas noites aplicáveis
- Reembolso total em caso de cancelamento

## API Endpoints

### Quartos
```
GET /rooms
```

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

### Relatórios
```
GET /reports/occupancy?date=2027-07-10
GET /reports/revenue?month=2027-07
```

## Exemplo de Criação de Reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }'
```

## Códigos de Erro

- `INVALID_REQUEST` (400): Requisição inválida
- `ROOM_NOT_FOUND` (404): Quarto não encontrado
- `RESERVATION_NOT_FOUND` (404): Reserva não encontrada
- `ROOM_UNAVAILABLE` (409): Quarto indisponível nas datas
- `CAPACITY_EXCEEDED` (422): Número de hóspedes excede capacidade
- `OPERATION_NOT_ALLOWED` (409): Operação não permitida no status atual
