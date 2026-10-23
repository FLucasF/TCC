# Arquitetura do Sistema

## Visão Geral

O sistema foi organizado seguindo as orientações do CLAUDE.md, com foco em:
- Novos casos devem entrar num lugar só, sem mexer no que já funciona
- Sequências repetidas são escritas uma vez só
- Organização focada nas variações que realmente existem

## Estrutura de Pacotes

```
com.pousada/
├── domain/              # Lógica de negócio e entidades
├── infrastructure/      # Acesso a dados (PostgreSQL)
├── service/            # Serviços de aplicação
├── api/                # Controllers e DTOs REST
└── Application.java    # Ponto de entrada
```

## Decisões de Design

### 1. Tipos de Identificação (Sealed Interface)

Para suportar CPF, CNPJ e Passaporte (e futuras agências de turismo), foi criada uma **sealed interface** `GuestIdentification`:

```java
public sealed interface GuestIdentification {
    record Cpf(String cpf) implements GuestIdentification {}
    record Cnpj(String cnpj, String companyName) implements GuestIdentification {}
    record Passport(String passport, String country) implements GuestIdentification {}
}
```

**Benefício**: Novos tipos de identificação entram num lugar só (novo record), e o pattern matching do Java garante que todos os casos sejam tratados.

### 2. Geração de Documentos (Strategy Pattern)

Cada tipo de identificação gera seu próprio documento através de `DocumentGenerator`:

```java
DocumentGenerator generator = DocumentGenerator.forIdentification(identification);
String document = generator.generate(reservation);
```

**Benefício**: 
- Novo tipo de documento = nova classe implementando `DocumentGenerator`
- A lógica de escolha do gerador está isolada no método factory
- Nenhum controller ou service precisa conhecer os detalhes de formatação

### 3. Cálculo de Preços (Classe Utilitária)

A lógica de cálculo (alta temporada, desconto, arredondamento) está concentrada em `PricingCalculator`:

```java
BigDecimal subtotal = PricingCalculator.calculateSubtotal(baseRate, checkIn, checkOut);
BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, ratePlan);
BigDecimal total = PricingCalculator.calculateTotal(subtotal, discount);
```

**Benefício**: A sequência de cálculo está num lugar só. Mudanças nas regras de preço afetam apenas esta classe.

### 4. Estado da Reserva (Máquina de Estados)

As transições de estado estão encapsuladas na classe `Reservation`:

```java
reservation.confirm();   // PENDING → CONFIRMED
reservation.checkIn();   // CONFIRMED → CHECKED_IN
reservation.checkOut();  // CHECKED_IN → CHECKED_OUT
reservation.cancel();    // PENDING/CONFIRMED → CANCELLED
```

**Benefício**: As regras de transição ficam na entidade, não espalhadas pelos serviços.

## Camadas

### Domain (Núcleo do Sistema)

- **Entidades**: `Room`, `Reservation`, `Message`
- **Value Objects**: `GuestIdentification` (sealed interface)
- **Enums**: `RoomType`, `RatePlan`, `ReservationStatus`
- **Lógica de Negócio**: `PricingCalculator`, `DocumentGenerator`

### Infrastructure (Persistência)

- **Database**: Gerencia conexão e inicialização do schema
- **Repositories**: `RoomRepository`, `ReservationRepository`, `MessageRepository`
- Isolam o acesso ao PostgreSQL usando JDBC puro

### Service (Casos de Uso)

- **ReservationService**: Orquestra criação, confirmação, check-in/out, cancelamento
- **ReportService**: Gera relatórios CSV de ocupação e receita
- Coordena domain e infrastructure, aplica validações

### API (Interface REST)

- **Controllers**: `RoomController`, `ReservationController`, `MessageController`, `ReportController`
- **DTOs**: Isolam a representação JSON da estrutura interna
- **Exception Handling**: Converte exceções de domínio em códigos HTTP e mensagens de erro

## Extensibilidade

### Para adicionar novo tipo de identificação:

1. Adicionar novo record em `GuestIdentification`
2. Criar nova classe `XXXDocumentGenerator implements DocumentGenerator`
3. Adicionar case no switch de `DocumentGenerator.forIdentification`
4. Adicionar campos no banco de dados (tabela `reservations`)

**Nenhuma outra classe precisa mudar.**

### Para adicionar novo plano de tarifa:

1. Adicionar enum em `RatePlan` com a taxa de desconto
2. A lógica de cálculo já funciona automaticamente

### Para adicionar novo canal de mensagem:

A estrutura já suporta: apenas mudar o canal ao criar a `Message`.

## Testes

- **Unitários**: `PricingCalculatorTest` valida cálculos de preço
- Todos os testes passam com `mvn test`
- Build completo com `mvn verify` passa sem erros

## Banco de Dados

O schema é criado automaticamente na inicialização:

- **rooms**: 5 quartos pré-cadastrados
- **reservations**: Todas as informações da reserva
- **messages**: Histórico de mensagens enviadas

Campos `guest_cpf`, `company_cnpj`, `company_name`, `guest_passport`, `guest_country` são nullable - apenas um grupo é preenchido por reserva.
