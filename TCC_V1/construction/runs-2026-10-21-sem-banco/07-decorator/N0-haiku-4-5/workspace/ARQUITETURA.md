# Arquitetura do Sistema de Reservas

## Visão Geral

O sistema é construído com arquitetura em camadas:

```
┌─────────────────────────────────────┐
│         HTTP API (Javalin)          │
├─────────────────────────────────────┤
│    Controllers / Route Handlers      │
├─────────────────────────────────────┤
│         Service Layer               │
│  ┌──────────┐  ┌──────────┐        │
│  │Reservation│  │ Message  │        │
│  │ Service   │  │ Service  │        │
│  └──────────┘  └──────────┘        │
├─────────────────────────────────────┤
│         Model Layer                 │
│  ┌──────────┬──────────┬───────┐  │
│  │Room      │Reservation│Message│  │
│  └──────────┴──────────┴───────┘  │
├─────────────────────────────────────┤
│    Utilities & Helpers              │
│  ┌──────────┬──────────┬─────────┐ │
│  │Money     │DateUtils │EmailVal.│ │
│  └──────────┴──────────┴─────────┘ │
└─────────────────────────────────────┘
```

## Pacotes e Classes

### `com.pousadamarealta.model`
Entidades de domínio imutáveis (após criação).

- **Room**: Informações do quarto (número, tipo, capacidade, preço/noite)
- **Reservation**: Registro completo da reserva
  - Campos imutáveis: ID, hóspede, datas, quarto, tipo de tarifa, cálculos
  - Campos mutáveis: status, valor pago, reembolso
- **Message**: Mensagem de email enviada ou bloqueada
- **Enums**:
  - `RoomType`: STANDARD, DELUXE, SUITE
  - `ReservationStatus`: PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED
  - `RatePlan`: STANDARD, NON_REFUNDABLE
  - `MessageChannel`: EMAIL (extensível para SMS, etc)
  - `MessageStatus`: SENT, BLOCKED

### `com.pousadamarealta.service`

#### ReservationService
Lógica completa de reservas:
- Criação e validação de reservas
- Transições de estado (confirm, check-in, check-out, cancel)
- Cálculo de preços com descontos
- Verificação de disponibilidade de quartos
- Geração de comprovantes
- Envio de emails via MessageService

**Métodos principais:**
- `createReservation()`: Cria nova reserva, valida dados
- `confirmReservation()`: Confirma pagamento, envia email
- `checkIn()`: Marca hóspede como entrada
- `checkOut()`: Marca hóspede como saída
- `cancelReservation()`: Cancela com reembolso baseado no plano
- `generateComprovante()`: Gera texto do comprovante
- `isRoomUnavailable()`: Verifica conflitos de datas

#### MessageService
Gerenciamento de emails com as três camadas de proteção:
- Validação de endereço de email
- Modo teste (redirecionamento)
- Cópia para arquivo

**Métodos principais:**
- `sendEmail()`: Envia email com todas as regras
- `getMessages()`: Retorna histórico

#### ReportService
Geração de relatórios em CSV:
- Ocupação por data
- Receita por mês

**Métodos principais:**
- `generateOccupancyReport()`: Relatório de quartos ocupados
- `generateRevenueReport()`: Relatório de receita

### `com.pousadamarealta.dto`
Data Transfer Objects para JSON:

- **CreateReservationRequest**: Entrada do POST /reservations
- **ReservationResponse**: Saída padrão de operações de reserva
- **RoomResponse**: Saída de GET /rooms
- **MessageResponse**: Saída de GET /messages
- **ComprovanteResponse**: Estrutura de comprovante (não usado atualmente, poderia ser usado no JSON futuro)
- **ErrorResponse**: Estrutura de erro padrão

### `com.pousadamarealta.util`

#### Money
Manipulação de valores monetários com arredondamento HALF_EVEN:
```java
Money.round(249.90 * 1.2)         // 299.88
Money.applyPercentageDiscount(600, 15)  // 90
Money.subtract(600, 90)           // 510.00
```

#### DateUtils
Operações com datas:
```java
DateUtils.isHighSeason(LocalDate)  // Verifica se é alta temporada
DateUtils.daysInRange(from, to)    // Calcula noites entre datas
```

#### EmailValidator
Validação de formato de email:
```java
EmailValidator.isValid("user@example.com")  // true
EmailValidator.isValid("user@gmail")        // false (sem ponto no domínio)
```

### `com.pousadamarealta.Main`
Aplicação Javalin com todos os endpoints.

## Fluxo de Dados

### Criar e Confirmar Reserva

```
1. POST /reservations
   ↓
   ReservationService.createReservation()
   - Valida quarto existe
   - Verifica capacidade
   - Verifica datas válidas (saída > entrada)
   - Verifica disponibilidade do quarto
   - Calcula noites, subtotal, desconto, total
   ↓
   Reservation criada com status PENDING
   ↓
   Retorna 201 ReservationResponse

2. POST /reservations/{id}/confirmation
   ↓
   ReservationService.confirmReservation()
   - Verifica status == PENDING
   - Muda para CONFIRMED
   - Define amountPaid
   ↓
   MessageService.sendEmail()
   - Valida email
   - Se inválido → BLOCKED
   - Se válido → cria 2 mensagens (hóspede + arquivo)
   - Se em modo teste → redireciona para testes@...
   ↓
   Retorna ReservationResponse com status CONFIRMED
```

### Cancelar Reserva com Reembolso

```
POST /reservations/{id}/cancellation
   ↓
   ReservationService.cancelReservation()
   ↓
   if status != (PENDING ou CONFIRMED) → erro
   ↓
   if PENDING → sem reembolso (não pagou nada)
   if CONFIRMED:
     - if ratePlan == STANDARD → refund = amountPaid
     - if ratePlan == NON_REFUNDABLE → refund = null
   ↓
   status = CANCELLED
   refund = valor a devolver
   ↓
   Retorna ReservationResponse
```

## Armazenamento de Dados

### Em Memória (Atual)

Mantém dois maps principais:

```java
// ReservationService
List<Reservation> reservations;           // Todas as reservas criadas
Map<Integer, Room> rooms;                 // Quartos (5 fixos)
long reservationIdCounter = 1;            // Próximo ID

// MessageService
List<Message> messages;                   // Todas as mensagens
long messageIdCounter = 1;                // Próximo ID de mensagem
```

### Implicações

- Dados perdidos ao reiniciar o servidor
- Nenhuma persistência
- Escalável apenas localmente
- Ideal para desenvolvimento/testes

### Migração Futura para BD

Seria necessário:
1. Criar DAO layer (Data Access Objects)
2. Adicionar dependência JPA/Hibernate ou JDBC
3. Manter as mesmas interfaces de Service
4. Migração de dados nula (primeiro startup)

## Cálculo de Preços - Exemplo

### Reserva: Quarto 101 (STANDARD) de 10/03/2027 a 12/03/2027

```
Base: R$ 249,90 por noite
Temporada: Normal (março não é alta)

Noite 1 (10/03): 249,90 (normal)
Noite 2 (11/03): 249,90 (normal)

Subtotal = 249,90 + 249,90 = 499,80

Plano STANDARD: sem desconto
Desconto = 0

Total = 499,80
```

### Reserva: Quarto 201 (DELUXE) de 10/07/2027 a 12/07/2027

```
Base: R$ 389,90 por noite
Temporada: ALTA (julho)

Noite 1 (10/07): 389,90 × 1,2 = 467,88
Noite 2 (11/07): 389,90 × 1,2 = 467,88

Subtotal = 467,88 + 467,88 = 935,76

Plano NON_REFUNDABLE: 15% desconto
Desconto = 935,76 × 0,15 = 140,364... → arredonda para 140,36

Total = 935,76 - 140,36 = 795,40
```

## Estados da Reserva - Diagrama

```
        ┌────────────────┐
        │    PENDING     │
        │ (Criada, não   │
        │  confirmada)   │
        └────────┬───────┘
                 │
         ╔═══════╩════════╗
         ║                ║
         V                V
    CONFIRMED        CANCELLED
    (Pagou)         (Cancelada)
         │
         V
    CHECKED_IN
    (Entrada)
         │
         V
    CHECKED_OUT
    (Saída)


Estados válidos para operações:
- Confirm: PENDING → CONFIRMED
- CheckIn: CONFIRMED → CHECKED_IN
- CheckOut: CHECKED_IN → CHECKED_OUT
- Cancel: PENDING ou CONFIRMED → CANCELLED
- GetDocument: CONFIRMED, CHECKED_IN, ou CHECKED_OUT
```

## Validações

### Validação de Email

```
Válido:
✓ Contém exatamente 1 @
✓ Tem conteúdo antes de @
✓ Domínio tem pelo menos 1 ponto

Exemplos:
✓ user@example.com
✓ nome.sobrenome@empresa.co.uk
✗ user (sem @)
✗ user@gmail (sem ponto em domínio)
✗ @example.com (sem conteúdo antes de @)
✗ user@@example.com (dois @)
```

### Validação de Reserva

```
✗ Quarto não existe → ROOM_NOT_FOUND (404)
✗ Saída ≤ Entrada → INVALID_REQUEST (400)
✗ Guests < 1 ou > capacidade → CAPACITY_EXCEEDED (422)
✗ Quarto ocupado nas datas → ROOM_UNAVAILABLE (409)
✗ Operação ilegal para status → OPERATION_NOT_ALLOWED (409)
```

## Extensibilidade

### Adicionar Novo Campo em Reservation

1. Adicionar campo na classe `Reservation`
2. Adicionar no construtor
3. Adicionar getter
4. Atualizar `ReservationService.createReservation()`
5. Atualizar `ReservationResponse`
6. Atualizar testes

### Adicionar Novo Tipo de Quartos

1. Adicionar ao enum `RoomType`
2. Adicionar à inicialização em `ReservationService.initializeRooms()`
3. Atualizar testes e documentação

### Adicionar Novo Endpoint

1. Adicionar método em `Main.java`
2. Implementar lógica em Service apropriado
3. Criar DTOs se necessário
4. Adicionar testes
5. Documentar API

## Performance

### Operações O(n)

- Buscar reserva por ID: O(n) - lista simples
- Verificar disponibilidade: O(n*d) - onde d = dias
- Gerar relatório: O(n) - percorre todas as reservas

### Otimizações Possíveis

1. Índice de reservas por quarto e data (HashMap)
2. Cache de disponibilidade por quarto
3. Paginação de mensagens
4. Query builder para relatórios

## Segurança

### Implementado

✓ Validação de entrada (datas, formato)
✓ Validação de email (previne spam)
✓ Modo teste (previne envio para clientes reais)
✓ Redirecionamento de emails em teste
✓ Arredondamento correto de valores

### Não Implementado

- Autenticação/Autorização
- Rate limiting
- HTTPS/TLS
- Validação de CPF
- Criptografia de dados sensíveis
- Auditoria de alterações

## Testes

Total: 20 testes em 5 classes

```
MoneyTest (2 testes)
- Arredondamento HALF_EVEN
- Cálculo de desconto

EmailValidatorTest (2 testes)
- Emails válidos
- Emails inválidos

MessageServiceTest (4 testes)
- Email válido enviado + cópia
- Email inválido bloqueado
- Modo teste redirecionamento
- Modo teste email inválido

ReservationServiceTest (10 testes)
- Criação básica
- Pricing alta temporada
- Plano NON_REFUNDABLE
- Confirmação
- Quarto indisponível
- Capacidade excedida
- Cancelamento com/sem reembolso
- Check-in/Check-out

ReportServiceTest (2 testes)
- Relatório de ocupação
- Relatório de receita
```

## Logs

O sistema usa SLF4J com slf4j-simple.

Logs principais:
- Startup do Javalin
- Requisições HTTP (configurável)
- Erros e exceções (automáticos)

Para debug:
```bash
java -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG -jar target/reservas-1.0.0.jar
```
