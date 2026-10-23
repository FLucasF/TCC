# Arquitetura do Sistema

## Estrutura Geral

O sistema segue uma arquitetura em camadas:

```
┌─────────────────────────────┐
│    Web Layer (Javalin)      │  ApiController
├─────────────────────────────┤
│    Service Layer            │  ReservationService, ReportService
├─────────────────────────────┤
│    Repository Layer         │  RoomRepository, ReservationRepository, MessageRepository
├─────────────────────────────┤
│    Domain Layer             │  Reservation, Room, GuestIdentification, etc.
├─────────────────────────────┤
│    Document Generation      │  DocumentGenerator (Template Method)
└─────────────────────────────┘
```

## Camadas

### 1. Domain Layer (`com.pousadamarealta.domain`)

Contém as entidades e regras de negócio centrais:

- **Room**: Representa um quarto da pousada
- **Reservation**: Entidade principal com regras de transição de estado e cálculo de preços
- **GuestIdentification**: Interface selada com 3 implementações (IndividualGuest, CorporateGuest, ForeignGuest)
- **Message**: Representa mensagens (e-mails) enviadas
- **Enums**: RoomType, ReservationStatus, RatePlan

#### Decisões de Design

**GuestIdentification como sealed interface:**
- Garante que apenas os 3 tipos conhecidos podem implementar a interface
- Facilita pattern matching em Java 21
- Torna explícito no código quais são os tipos de hóspedes suportados

**Reservation com lógica de negócio:**
- A classe Reservation encapsula todas as regras de transição de estado
- Cálculo de preços (alta temporada, descontos) está centralizado na entidade
- Arredondamento "meio para par" (HALF_EVEN) conforme especificado

### 2. Document Generation (`com.pousadamarealta.document`)

Implementa o **padrão Template Method** para geração de documentos:

```
DocumentGenerator (abstract)
    ├─ IndividualGuestDocumentGenerator
    ├─ CorporateGuestDocumentGenerator
    └─ ForeignGuestDocumentGenerator
```

**Por que Template Method?**

Conforme identificado na análise do CLAUDE.md:
- Todos os documentos seguem a mesma sequência: título → identificação → quarto → período → valores
- Cada tipo varia apenas em **como** formata cada parte
- Novos tipos de documentos (ex: agências) podem ser adicionados sem modificar código existente

**Sequência fixa (template method):**
1. formatTitle()
2. formatGuestIdentification()
3. formatRoom()
4. formatPeriod()
5. formatSubtotal()
6. formatDiscount() (opcional)
7. formatTotal()
8. formatAdditionalFields() (para campos específicos como ISS)

**Variações por tipo:**
- **Individual**: Português, formato BR de dinheiro (R$ 1.234,56) e data (dd/MM/yyyy)
- **Corporate**: Português + campo ISS retido (5%)
- **Foreign**: Inglês, formato US de dinheiro (BRL 1,234.56) e data ISO (yyyy-MM-dd)

### 3. Repository Layer (`com.pousadamarealta.repository`)

Camada de acesso a dados usando JDBC puro com PostgreSQL:

- **RoomRepository**: Gerencia os 5 quartos fixos
- **ReservationRepository**: CRUD de reservas + queries específicas (disponibilidade, relatórios)
- **MessageRepository**: Armazena e-mails enviados

**Decisões:**
- Uso de HikariCP para connection pooling
- Queries SQL diretas (sem ORM) para máximo controle
- Inicialização automática do schema na primeira execução

### 4. Service Layer (`com.pousadamarealta.service`)

Orquestra lógica de negócio e coordena repositórios:

- **ReservationService**: 
  - Valida regras de negócio (capacidade, disponibilidade)
  - Coordena transições de estado
  - Envia e-mails de confirmação
  - Gera documentos

- **ReportService**:
  - Gera relatórios CSV de ocupação e financeiro
  - Aplica regras de negócio para filtragem

**Exceções customizadas:**
Cada erro de negócio tem sua própria exceção:
- RoomNotFoundException
- ReservationNotFoundException
- RoomUnavailableException
- CapacityExceededException
- OperationNotAllowedException
- InvalidRequestException

### 5. Web Layer (`com.pousadamarealta.web`)

Controller REST usando Javalin:

- **ApiController**: Mapeia todos os endpoints
- Conversão automática JSON usando Jackson
- Tratamento de exceções com códigos HTTP apropriados

## Padrões de Código

### Princípios seguidos (conforme CLAUDE.md)

1. **Open/Closed para variações esperadas:**
   - Tipos de documentos: novos tipos entram como nova classe, sem modificar existentes
   - Cada caso (CPF, CNPJ, Passaporte) está em sua própria classe

2. **Organização para crescimento:**
   - "Para acrescentar o próximo caso, quantos lugares mudam?" → Um só (nova classe DocumentGenerator)
   - Sequência de geração escrita uma vez, casos variam apenas seus passos

3. **Sem estrutura para o que não foi pedido:**
   - Sem abstração prematura
   - Valores fixos (capacidade, diárias) são constantes nos enums
   - Apenas onde há variação real (documentos, estados) há estrutura

## Fluxo de uma Reserva

```
1. POST /reservations
   ↓
2. ReservationService.createReservation()
   ↓ valida room, capacidade, disponibilidade
   ↓
3. new Reservation(...) [status: PENDING]
   ↓ calcula preços (alta temporada, desconto)
   ↓
4. ReservationRepository.save()
   ↓
5. POST /reservations/{id}/confirmation
   ↓
6. Reservation.confirm() [status: CONFIRMED]
   ↓ amountPaid = total
   ↓
7. DocumentService.generateDocument()
   ↓ seleciona DocumentGenerator apropriado
   ↓
8. MessageRepository.save() [e-mail com documento]
   ↓
9. POST /reservations/{id}/check-in [status: CHECKED_IN]
   ↓
10. POST /reservations/{id}/check-out
    ↓
11. Reservation.performCheckOut() [status: CHECKED_OUT]
    ↓
12. DocumentService.generateDocument() [retorna documento final]
```

## Alta Temporada

Implementada na classe Reservation:

```java
private boolean isHighSeason(LocalDate date) {
    int month = date.getMonthValue();
    int day = date.getDayOfMonth();
    
    if (month == 12 && day >= 15) return true;  // 15/12 em diante
    if (month == 1 || month == 2) return true;  // Janeiro e Fevereiro
    if (month == 7) return true;                // Julho inteiro
    
    return false;
}
```

Cada noite é calculada individualmente, aplicando o acréscimo de 20% quando aplicável.

## Arredondamento Bancário

Todas as operações financeiras usam `RoundingMode.HALF_EVEN` (arredondamento "meio para par"):

- 2.125 → 2.12 (par)
- 2.135 → 2.14 (par)
- Minimiza viés estatístico em múltiplas operações
- Padrão bancário recomendado

## Extensibilidade Futura

### Novos tipos de hóspede (ex: Agência de Turismo)

1. Criar novo record em GuestIdentification:
```java
record TravelAgencyGuest(String cnpj, String agencyName, String commission) 
    implements GuestIdentification {}
```

2. Criar novo DocumentGenerator:
```java
class TravelAgencyDocumentGenerator extends DocumentGenerator {
    // implementar apenas os métodos de formatação específicos
}
```

3. Registrar no DocumentService:
```java
else if (guestId instanceof GuestIdentification.TravelAgencyGuest) {
    return travelAgencyGenerator;
}
```

**Pontos de mudança:** 3 arquivos apenas (GuestIdentification, novo Generator, DocumentService)

### Novos planos de tarifa

Adicionar valor no enum RatePlan:
```java
CORPORATE_AGREEMENT(new BigDecimal("0.20"));  // 20% desconto
```

A lógica de aplicação já está implementada em Reservation.

### Novos tipos de quarto

Adicionar no enum RoomType e inserir novos quartos via RoomRepository.
