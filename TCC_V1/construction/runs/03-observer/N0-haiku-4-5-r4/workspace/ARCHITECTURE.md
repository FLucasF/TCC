# Arquitetura do Sistema

## Visão Geral

O sistema é uma aplicação web REST para gerenciamento de reservas, construída com padrões de camadas:

```
API Controllers (ApiRoutes)
        ↓
    Services (Business Logic)
        ↓
    Repositories (Data Access)
        ↓
    PostgreSQL Database
```

## Componentes Principais

### 1. Database Layer (`database/`)

**DatabaseConfig**: Gerencia conexões e inicializa esquema
- Cria tabelas automaticamente na primeira execução
- Popula dados iniciais de quartos
- Gerencia pool de conexões

**Tabelas**:
- `rooms`: Definição dos 5 quartos (estática)
- `reservations`: Todas as reservas com histórico completo
- `messages`: Log de e-mails enviados

### 2. Repository Layer (`repositories/`)

Acesso a dados com abstrações SQL:

**RoomRepository**:
- `findAll()`: Lista quartos disponíveis
- `findByNumber()`: Quarto específico

**ReservationRepository**:
- `save()`: Insere nova reserva com ID auto-gerado
- `findById()`: Busca por ID
- `update()`: Atualiza status, amountPaid, refund
- `findByRoomAndDateRange()`: Verificação de disponibilidade
- `findByMonthWithRevenue()`: Reservas com receita no relatório
- `findByDateAndConfirmed()`: Ocupação em uma data

**MessageRepository**:
- `save()`: Registra e-mail enviado
- `findAll()`: Lista histórico

### 3. Service Layer (`services/`)

Lógica de negócio e orquestração:

**PricingService**:
```
Responsabilidades:
- Detectar alta temporada (dez-fev, julho)
- Aplicar 20% no cálculo de diárias
- Calcular subtotal por período
- Aplicar desconto NON_REFUNDABLE (15%)
- Arredondar com HALF_EVEN em cada etapa
```

**ReservationService**:
```
Responsabilidades:
- Criar reserva (validação, cálculo de preço)
- Confirmar pagamento
- Check-in / Check-out
- Cancelar com reembolso correto
- Orquestrar notificações
```

**NotificationService**:
```
Responsabilidades:
- Gerar comprovante formatado
- Notificar hóspede (e-mail com comprovante)
- Notificar governança (entrada, saída, datas livres)
- Notificar financeiro (pagamentos, reembolsos)
- Extensível para novos notificadores
```

**ReportService**:
```
Responsabilidades:
- Gerar ocupação em CSV
- Gerar receita em CSV
- Filtrar apenas reservas com revenue > 0
```

### 4. API Layer (`api/`)

**ApiRoutes**: Endpoints REST
- Mapeamento de requisições para serviços
- Validação de entrada
- Conversão JSON/Objetos
- Tratamento centralizado de erros

## Fluxos Principais

### Criar Reserva

```
POST /reservations
    ↓
ApiRoutes.createReservation()
    ↓
ReservationService.createReservation()
    ├─ RoomRepository.findByNumber() → valida quarto existe
    ├─ ReservationService.checkRoomAvailability() → verifica datas
    ├─ PricingService.calculateSubtotal() → soma diárias com alta temporada
    ├─ PricingService.calculateDiscount() → aplica desconto se NON_REFUNDABLE
    └─ ReservationRepository.save() → insere com status PENDING
```

### Confirmar Reserva

```
POST /reservations/{id}/confirmation
    ↓
ReservationService.confirmReservation()
    ├─ Valida que está PENDING
    ├─ Atualiza status para CONFIRMED
    ├─ Registra amountPaid
    └─ NotificationService.notifyReservationConfirmed()
        ├─ Notifica hóspede (e-mail + comprovante)
        ├─ Notifica governança (datas, hóspedes)
        └─ Notifica financeiro (valor)
```

### Cancelar Reserva

```
POST /reservations/{id}/cancellation
    ↓
ReservationService.cancelReservation()
    ├─ Valida que não é CHECKED_IN/OUT/CANCELLED
    ├─ Se foi CONFIRMED:
    │  ├─ STANDARD: refund = amountPaid
    │  └─ NON_REFUNDABLE: refund = null
    ├─ Atualiza status para CANCELLED
    └─ NotificationService.notifyReservationCancelled()
        ├─ Se tinha refund: notifica governança (datas livres)
        └─ Se tem refund > 0: notifica financeiro (valor devolução)
```

## Modelo de Dados

### Reservation
```
id (PK)
roomNumber (FK)
guestName, guestEmail, guestCpf
checkIn, checkOut (datas)
guests (número)
ratePlan (STANDARD | NON_REFUNDABLE)
nights (calculado)
subtotal, discount, total (BigDecimal com 2 casas)
status (PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT | CANCELLED)
amountPaid (recebido do hóspede)
refund (devolução, null se nenhuma)
```

## Considerações de Design

### 1. Imutabilidade de Dados

- Quartos nunca mudam (apenas 5 hardcoded)
- Histórico de reservas nunca é deletado (soft-delete via CANCELLED)
- Mensagens nunca são modificadas (auditoria)

### 2. Transações

- Cada operação é atômica
- PostgreSQL garante ACID
- Sem transações explícitas (auto-commit por operação)

### 3. Extensibilidade

**Notificadores Novos**:
Para adicionar restaurante, transfer, etc:
1. Criar métodos em `NotificationService`
2. Chamar em `notifyReservationConfirmed()` e `notifyReservationCancelled()`
3. Registrar destinatários nos e-mails

Exemplo:
```java
private void notifyRestaurant(Reservation reservation, String type) throws Exception {
    String subject;
    String body;
    if ("confirmed".equals(type)) {
        subject = "Nova reserva confirmada";
        body = String.format("Hóspedes: %d, entrada: %s", 
            reservation.getGuests(), reservation.getCheckIn());
    }
    messageRepository.save("EMAIL", "restaurante@...", subject, body);
}
```

### 4. Segurança

- Sem autenticação (integração com sistema externo esperado)
- Sem autorização (endpoint público)
- Sem validação de CPF (apenas string)
- Sem rate limiting (esperado em gateway)
- Sem criptografia de sensíveis (esperado em TLS)

### 5. Performance

**Índices**:
- PK em reservations.id
- FK em reservations.room_number
- Índice em (room_number, check_in, check_out) para verificação de disponibilidade

**Queries Otimizadas**:
- Usa prepared statements (evita SQL injection)
- Usa índices para filtros de data
- Limita dados transferidos (campos necessários apenas)

## Tratamento de Erros

```
Exception → Try-Catch → ApiException | Generic Exception
              ↓                ↓
         errorResponse()    INVALID_REQUEST (400)
```

Códigos específicos:
- 400: INVALID_REQUEST (dados ruins)
- 404: ROOM_NOT_FOUND, RESERVATION_NOT_FOUND
- 409: ROOM_UNAVAILABLE, OPERATION_NOT_ALLOWED
- 422: CAPACITY_EXCEEDED

## Testes

- **Unitários**: PricingServiceTest valida cálculos
- **Manuais**: Scripts curl para fluxos completos
- **Integração**: Esperada contra PostgreSQL real

## Limitações Conhecidas

1. **Sem Migração de Schema**: Apenas SQL direto no Java
2. **Sem Cache**: Cada requisição acessa DB
3. **Sem Paginação**: Relatórios retornam tudo (okay para 5 quartos)
4. **Sem Soft-Delete**: CANCELLED é o único "delete"
5. **Sem Auditoria**: Não registra quem fez cada ação
6. **Sem Timeout**: Conexões podem ficar abertas indefinidamente

## Próximas Melhorias

1. Adicionar cache de quartos (dados estáticos)
2. Implementar autenticação JWT
3. Adicionar paginação em relatórios
4. Usar migrations (Flyway/Liquibase)
5. Adicionar logs estruturados
6. Implementar circuit breaker para BD
7. Adicionar validação de e-mail
8. Implementar fila para e-mails assincronos
