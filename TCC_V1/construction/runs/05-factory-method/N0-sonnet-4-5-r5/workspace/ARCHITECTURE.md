# Arquitetura do Sistema

## Visão Geral

O sistema foi desenvolvido seguindo uma arquitetura em camadas limpa e bem organizada:

```
┌─────────────────────────────────────┐
│     API REST (Javalin)              │
├─────────────────────────────────────┤
│     Controllers & DTOs              │
├─────────────────────────────────────┤
│     Services (Lógica de Negócio)    │
├─────────────────────────────────────┤
│     Repositories (Acesso a Dados)   │
├─────────────────────────────────────┤
│     PostgreSQL                      │
└─────────────────────────────────────┘
```

## Estrutura de Pacotes

### `br.com.maralta.model`
Modelos de domínio e enums:
- `Room`, `RoomType` - Quartos e tipos
- `Reservation`, `ReservationStatus`, `RatePlan` - Reservas
- `GuestIdentification` - Identificação do hóspede (CPF/CNPJ/Passaporte)
- `Document` - Documentos (comprovantes/faturas/receipts)
- `Message` - Mensagens de e-mail

### `br.com.maralta.repository`
Acesso ao banco de dados usando JDBC puro:
- `RoomRepository` - CRUD de quartos
- `ReservationRepository` - CRUD de reservas com queries especializadas
- `MessageRepository` - Armazenamento de mensagens

### `br.com.maralta.service`
Lógica de negócio:
- `PricingService` - Cálculo de preços, temporadas e descontos
- `DocumentService` - Geração de comprovantes (3 formatos)
- `EmailService` - Envio de e-mails (armazenamento)
- `ReservationService` - Orquestração do ciclo de vida da reserva
- `ReportService` - Geração de relatórios CSV

### `br.com.maralta.dto`
Data Transfer Objects para API:
- `CreateReservationRequest` - Entrada para criação de reserva
- `ReservationResponse` - Saída de consulta de reserva
- `RoomResponse` - Saída de consulta de quarto
- `ErrorResponse` - Formato de erro padronizado

### `br.com.maralta.util`
Utilitários:
- `SeasonUtils` - Verificação de alta temporada
- `MoneyUtils` - Arredondamento e formatação de valores
- `DatabaseInitializer` - Criação de schema e dados iniciais

### `br.com.maralta.exception`
- `ApiException` - Exceção customizada com códigos HTTP e de erro

## Decisões Técnicas

### 1. Arredondamento HALF_EVEN
Conforme especificado, todos os valores monetários são arredondados usando `RoundingMode.HALF_EVEN` (arredondamento bancário) a cada passo do cálculo.

### 2. Identificação de Hóspedes
Implementado usando composição com validação:
- Exatamente um tipo de identificação deve ser fornecido
- CPF → Comprovante em português
- CNPJ + Razão Social → Fatura com ISS (5%)
- Passaporte + País → Booking Receipt em inglês

### 3. Cálculo de Alta Temporada
```java
- 15/12 a 28/02: +20%
- Julho inteiro: +20%
```

### 4. Estados da Reserva
Máquina de estados bem definida:
```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
   ↓           ↓
CANCELLED   CANCELLED
```

### 5. Reembolso
- STANDARD: reembolso total
- NON_REFUNDABLE: sem reembolso
- Reservas PENDING canceladas não têm valor pago

### 6. Relatórios CSV
- **Ocupação**: Reservas pagas (CONFIRMED+) que ocupam a noite
  - `checkIn <= noite < checkOut`
- **Receita**: Reservas do mês com receita líquida
  - `amountPaid - refund`

### 7. Formatação de Valores
- JSON: números com 2 decimais (599.76)
- CSV: formato americano (599.76)
- Português: R$ 599,76
- Inglês: BRL 599.76

### 8. Non-Breaking Space
O `NumberFormat` brasileiro usa ` ` (non-breaking space). Convertido para espaço regular para facilitar testes e manipulação.

## Esquema do Banco de Dados

### Tabela `rooms`
```sql
CREATE TABLE rooms (
    number VARCHAR(10) PRIMARY KEY,
    type VARCHAR(20) NOT NULL
);
```

### Tabela `reservations`
```sql
CREATE TABLE reservations (
    id SERIAL PRIMARY KEY,
    room_number VARCHAR(10) NOT NULL,
    guest_name VARCHAR(255) NOT NULL,
    guest_email VARCHAR(255) NOT NULL,
    guest_cpf VARCHAR(20),
    company_cnpj VARCHAR(20),
    company_name VARCHAR(255),
    guest_passport VARCHAR(50),
    guest_country VARCHAR(100),
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    guests INTEGER NOT NULL,
    rate_plan VARCHAR(20) NOT NULL,
    nights INTEGER NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    discount DECIMAL(10,2) NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount_paid DECIMAL(10,2) NOT NULL DEFAULT 0,
    refund DECIMAL(10,2),
    FOREIGN KEY (room_number) REFERENCES rooms(number)
);
```

### Tabela `messages`
```sql
CREATE TABLE messages (
    id SERIAL PRIMARY KEY,
    channel VARCHAR(20) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(500),
    body TEXT NOT NULL
);
```

## Fluxo de uma Reserva

1. **Criação** (POST /reservations)
   - Valida dados de entrada
   - Verifica disponibilidade do quarto
   - Calcula preços (subtotal, desconto, total)
   - Salva no banco com status PENDING

2. **Confirmação** (POST /reservations/{id}/confirmation)
   - Atualiza status para CONFIRMED
   - Registra pagamento (amountPaid = total)
   - Gera documento
   - Envia e-mail de confirmação

3. **Check-in** (POST /reservations/{id}/check-in)
   - Atualiza status para CHECKED_IN

4. **Check-out** (POST /reservations/{id}/check-out)
   - Atualiza status para CHECKED_OUT
   - Retorna documento final

5. **Cancelamento** (POST /reservations/{id}/cancellation)
   - Atualiza status para CANCELLED
   - Calcula e registra reembolso conforme plano tarifário

## Testes

### Testes Unitários (17 testes)
- `PricingServiceTest`: Cálculo de preços e temporadas
- `SeasonUtilsTest`: Verificação de alta temporada
- `DocumentServiceTest`: Geração de documentos

### Testes de Integração (8 testes)
- `ReservationIT`: Testes end-to-end com PostgreSQL real
- Requer Docker (TestContainers)
- Desabilitado por padrão (`skipITs=true`)

## Performance

- Pool de conexões gerenciado pelo PostgreSQL driver
- Queries otimizadas com índices na chave primária
- Transações ACID garantidas pelo PostgreSQL
- JAR completo de 9.3MB (todas dependências incluídas)

## Segurança

- Validação de entrada em todos endpoints
- Prepared statements para prevenir SQL injection
- Códigos de erro padronizados sem exposição de stack traces
- Validação de estado antes de operações

## Extensibilidade

O sistema foi projetado para fácil extensão:

1. **Novos tipos de documento**: Adicionar método em `DocumentService`
2. **Novos tipos de quarto**: Adicionar em `RoomType` enum
3. **Novos planos tarifários**: Adicionar em `RatePlan` enum
4. **Novos relatórios**: Adicionar método em `ReportService`
5. **Novos canais de mensagem**: Estender `EmailService`

## Exemplo de Cálculo

Reserva STANDARD de 10/07/2027 a 12/07/2027 (2 noites em julho):

```
Noite 1 (10/07): R$ 249,90 × 1,20 = R$ 299,88
Noite 2 (11/07): R$ 249,90 × 1,20 = R$ 299,88
Subtotal: R$ 599,76
Desconto: R$ 0,00 (plano STANDARD)
Total: R$ 599,76
```

Mesma reserva com plano NON_REFUNDABLE:

```
Subtotal: R$ 599,76
Desconto: R$ 599,76 × 0,15 = R$ 89,96
Total: R$ 599,76 - R$ 89,96 = R$ 509,80
```

## Dependências Principais

- **Javalin 6.7.0**: Framework web minimalista
- **PostgreSQL Driver 42.7.4**: Driver JDBC
- **Jackson 2.18.2**: Serialização JSON
- **JUnit 5.11.4**: Testes unitários
- **TestContainers 1.20.4**: Testes de integração
