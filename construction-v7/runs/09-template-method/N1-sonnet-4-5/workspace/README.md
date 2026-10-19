# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Tecnologias

- Java 21
- Javalin 6.7.0
- Maven
- Jackson (JSON)

## Como executar

### Compilar

```bash
mvn clean package
```

### Executar

```bash
java -jar target/mare-alta-1.0.0.jar
```

O sistema sobe na porta 7070 por padrão. Para usar outra porta, defina a variável de ambiente `PORT`:

```bash
PORT=8080 java -jar target/mare-alta-1.0.0.jar
```

### Testes

```bash
mvn test
```

### Verificação completa

```bash
mvn verify
```

## API

### Quartos

**GET /rooms**
- Retorna todos os quartos disponíveis

### Reservas

**POST /reservations**
- Cria uma nova reserva
- Body: `{ roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan }`

**GET /reservations/{id}**
- Retorna os detalhes de uma reserva

**POST /reservations/{id}/confirmation**
- Confirma uma reserva pendente

**POST /reservations/{id}/cancellation**
- Cancela uma reserva

**POST /reservations/{id}/check-in**
- Faz o check-in de uma reserva confirmada

**POST /reservations/{id}/check-out**
- Faz o check-out de uma reserva com check-in
- Retorna o comprovante

**GET /reservations/{id}/document**
- Retorna a segunda via do comprovante

### Mensagens

**GET /messages**
- Retorna todas as mensagens enviadas

### Relatórios

**GET /reports/occupancy?date=YYYY-MM-DD**
- Relatório de ocupação para uma data específica

**GET /reports/revenue?month=YYYY-MM**
- Relatório financeiro mensal

**GET /reports/tourism-tax?month=YYYY-MM**
- Relatório de taxa de turismo mensal

**GET /reports/rate-plans?month=YYYY-MM**
- Relatório de planos de tarifa mensal

## Regras de negócio

### Quartos

- 101, 102: STANDARD (2 pessoas, R$ 249,90/noite)
- 201, 202: DELUXE (3 pessoas, R$ 389,90/noite)
- 301: SUITE (4 pessoas, R$ 579,90/noite)

### Temporadas

- Alta temporada: 15/dez a fim/fev e julho inteiro (+20% na diária)
- Temporada regular: demais períodos

### Planos de tarifa

- STANDARD: preço normal, reembolsável
- NON_REFUNDABLE: 15% de desconto, não reembolsável

### Fluxo de reserva

1. PENDING: reserva criada
2. CONFIRMED: pagamento realizado
3. CHECKED_IN: hóspede fez check-in
4. CHECKED_OUT: hóspede fez check-out
5. CANCELLED: reserva cancelada (possível de PENDING ou CONFIRMED)

## Arquitetura

O projeto segue uma arquitetura em camadas:

- **domain**: modelos, serviços e lógica de negócio
  - `model`: entidades do domínio
  - `service`: serviços de negócio
  - `pricing`: cálculos de preço
  - `validation`: validações de negócio
- **infrastructure**: repositórios em memória
- **api**: controllers e DTOs
- **report**: geradores de relatórios (padrão Strategy)

Novos relatórios podem ser adicionados criando uma classe que implementa `ReportGenerator` e registrando-a no `ReportService`.
