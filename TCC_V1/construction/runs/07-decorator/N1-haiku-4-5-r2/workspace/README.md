# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, construído em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Recursos Principais

### Gestão de Quartos
- **5 quartos fixos** com tarifas e capacidades pré-definidas
- STANDARD (101, 102): 2 pessoas, R$ 249,90/noite
- DELUXE (201, 202): 3 pessoas, R$ 389,90/noite
- SUITE (301): 4 pessoas, R$ 579,90/noite

### Dinâmica de Preços
- **Alta temporada**: +20% na diária (15/12 a 28/02 e julho inteiro)
- **Planos de tarifa**: STANDARD (reembolsável) e NON_REFUNDABLE (15% desconto)
- **Arredondamento**: Meio-para-par em cada passo (noite, subtotal, desconto, total)

### Ciclo de Reservas
```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
           ↓
        CANCELLED (com reembolso ou não, conforme plano)
```

### Gestão Inteligente de E-mails
1. **Validação rigorosa**: Um @ + algo antes + domínio com ponto (ex: user@example.com)
2. **E-mails inválidos**: Bloqueados automaticamente e listados para ação manual
3. **Cópia para arquivo**: Cada e-mail gera cópia em arquivo@pousadamarealta.com.br
4. **Modo teste**: Via `HOTEL_TEST_MODE=true`
   - Redireciona para testes@pousadamarealta.com.br
   - Adiciona `[TESTE]` no assunto
   - Inclui endereço original no corpo
   - Desativa cópia para arquivo

### Comprovantes e Confirmações
- Enviados automaticamente ao confirmar pagamento
- Formato padronizado com número, hóspede, quarto, datas e valores
- Segunda via disponível a qualquer momento para confirmadas/checked-in/checked-out

### Relatórios em CSV
1. **Ocupação**: Quartos ocupados em uma noite específica
   - Formato: room, type, reservation, check_in, check_out
2. **Financeiro**: Receita mensais por reserva
   - Formato: reservation, room, check_in, check_out, status, amount
   - Cálculo: amount_paid - refund

## Instalação e Execução

### Pré-requisitos
- Java 21+
- PostgreSQL 18+
- Maven 3.6+

### Build
```bash
mvn clean package
```

Gera: `target/app.jar` (27MB, standalone)

### Executar
```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="pousada"
export DB_PASSWORD="senha"
export PORT="7070"
export HOTEL_TEST_MODE="false"

java -jar target/app.jar
```

Ou usando o script:
```bash
./run.sh
```

### Docker (exemplo)
```bash
docker run -d --name postgres \
  -e POSTGRES_DB=pousada \
  -e POSTGRES_USER=pousada \
  -e POSTGRES_PASSWORD=senha \
  -p 5432:5432 \
  postgres:18

export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="pousada"
export DB_PASSWORD="senha"

java -jar target/app.jar
```

## API REST

### Quartos
```bash
GET /rooms
```
Response: Array de quartos com número, tipo, capacidade e tarifa

### Reservas - Criar
```bash
POST /reservations
Content-Type: application/json

{
  "roomNumber": 201,
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
  "guestCpf": "111.222.333-44",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

Response: 201 Created com dados da reserva

### Reservas - Obter
```bash
GET /reservations/{id}
```

### Reservas - Confirmar
```bash
POST /reservations/{id}/confirmation
Content-Type: application/json

{
  "amountPaid": 779.80
}
```

Automático: Envia comprovante por e-mail

### Reservas - Check-in
```bash
POST /reservations/{id}/check-in
```

### Reservas - Check-out
```bash
POST /reservations/{id}/check-out
```

Response: Comprovante de saída

### Reservas - Cancelar
```bash
POST /reservations/{id}/cancellation
```

Reembolso conforme plano (STANDARD: total, NON_REFUNDABLE: 0)

### Reservas - Segunda Via
```bash
GET /reservations/{id}/document
```

Disponível após confirmação

### Mensagens Enviadas
```bash
GET /messages
```

Response: Array com status (SENT ou BLOCKED), incluindo cópias para arquivo

### Relatórios
```bash
GET /reports/occupancy?date=2027-07-10
GET /reports/revenue?month=2027-07
```

Response: CSV com charset utf-8

## Tratamento de Erros

### 400 Bad Request
- `INVALID_REQUEST`: Falta de campo, formato inválido, datas fora de ordem, <1 hóspede

### 404 Not Found
- `ROOM_NOT_FOUND`: Quarto não existe
- `RESERVATION_NOT_FOUND`: Reserva não existe

### 409 Conflict
- `ROOM_UNAVAILABLE`: Quarto já reservado nas datas
- `OPERATION_NOT_ALLOWED`: Operação incompatível com estado da reserva

### 422 Unprocessable Entity
- `CAPACITY_EXCEEDED`: Mais hóspedes que capacidade do quarto

## Testes

```bash
mvn verify
```

Inclui:
- **PriceCalculatorTest**: Cálculos com arredondamento meio-para-par, alta temporada, descontos
- **EmailValidatorTest**: Validação de e-mail em vários cenários
- **MessageServiceTest**: E-mails, modo teste, cópia para arquivo, bloqueio

## Estrutura do Código

```
src/main/java/com/pousada/
├── Application.java          # Entry point
├── models/
│   ├── Room.java            # Quartos (imutável)
│   ├── Reservation.java     # Reservas
│   ├── Message.java         # Mensagens enviadas
│   └── Receipt.java         # Comprovantes formatados
├── services/
│   ├── ReservationService.java  # Lógica de negócio de reservas
│   ├── MessageService.java      # Gestão de e-mails com regras
│   └── ReportService.java       # Geração de relatórios CSV
├── api/
│   └── ApiRoutes.java       # Endpoints REST
├── db/
│   └── Database.java        # Pool de conexões e queries
└── utils/
    ├── PriceCalculator.java # Cálculo de preços
    └── EmailValidator.java  # Validação de e-mail
```

## Decisões Técnicas

### Armazenamento
- **PostgreSQL**: Banco relacional simples com uma tabela (reservations)
- **HikariCP**: Pool de conexões para melhor performance
- **JDBC direto**: Sem ORM, mais controle e simplicidade

### Cálculo de Preços
- Arredondamento RoundingMode.HALF_UP em cada passo conforme especificação
- Cálculo por noite individual para permitir preços diferentes por noite (alta/baixa temporada)

### Validação de E-mail
- Simples mas rigorosa: um @ + algo antes + domínio com ponto
- Não usa regex complexas, apenas lógica clara e testável

### Modo Teste
- Simples: flag HOTEL_TEST_MODE booleano
- Estrutura pronta para extensões futuras (horário, limite por hóspede, etc.)

## Extensões Futuras

A estrutura `MessageService` já suporta:
- Bloqueio de envios em horários específicos (ex: 22h-7h)
- Limite de mensagens por hóspede por dia
- Ativação condicional (sempre, só teste, só produção)

Exemplos de uso futuro (após adicionar configuração):

```java
messageService.shouldBlockTime("22:00-07:00")  // Não enviar madrugada
messageService.setDailyLimitPerGuest(5)         // Max 5 e-mails/hóspede/dia
messageService.enableOnlyInTestMode()           // Regra específica do modo teste
```

## Licença

Desenvolvido para Pousada Maré Alta - 2026
