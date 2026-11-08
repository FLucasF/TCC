# Documentação da Implementação

## Visão Geral

Sistema completo de reservas para a Pousada Maré Alta, construído seguindo os princípios do CLAUDE.md:

1. **Organização modular**: Novos casos similares entram em um lugar só
2. **Reutilização**: Sequências comuns escritas uma vez
3. **Evolução controlada**: Estrutura preparada apenas onde a variação existe

## Estrutura do Código

### 1. Domain (Domínio)

Entidades de negócio puras, sem dependências de frameworks:

- `RoomType`: Enum com capacidade e diária base de cada tipo
- `Room`: Representa um quarto específico
- `RatePlan`: Enum para planos de tarifa (STANDARD, NON_REFUNDABLE)
- `ReservationStatus`: Estados da reserva (PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT ou CANCELLED)
- `Reservation`: Entidade principal com toda a lógica de transição de estados
- `MessageChannel`: Enum para canais de comunicação (EMAIL)
- `Message`: Mensagem enviada ao hóspede

### 2. Pricing (Precificação)

`PricingCalculator`: Centraliza toda a lógica de cálculo de preços:
- Detecção de alta temporada (15/dez a fim/fev + julho)
- Cálculo por noite com multiplicador de alta temporada
- Cálculo de subtotal (soma das noites)
- Cálculo de desconto (15% para NON_REFUNDABLE)
- Arredondamento consistente (HALF_EVEN) em cada passo

**Princípio aplicado**: Um lugar só para toda a lógica de precificação. Mudanças futuras (nova regra de alta temporada, novo desconto) entram aqui.

### 3. Repository (Persistência)

Camada de acesso a dados com PostgreSQL:

- `DatabaseManager`: Gerencia conexões (HikariCP) e inicializa o schema
- `RoomRepository`: Gerencia os 5 quartos (inicializa automaticamente)
- `ReservationRepository`: CRUD de reservas + queries específicas (conflitos, por data, por mês)
- `MessageRepository`: Armazena mensagens enviadas

**Decisão técnica**: O schema é criado automaticamente na primeira execução. Os quartos são pré-populados se o banco estiver vazio.

### 4. Service (Serviços de Negócio)

Orquestram as regras de negócio:

- `ReservationService`: Criação e transições de estado das reservas
  - Validações (capacidade, disponibilidade, datas)
  - Cálculo de preços via PricingCalculator
  - Transições de estado (confirm, checkIn, checkOut, cancel)
  
- `DocumentService`: Geração de comprovantes formatados
  - Formatação de dinheiro (R$ 1.234,56)
  - Formatação de datas (dd/MM/yyyy)
  - Formatação de CPF (111.222.333-44)
  
- `MessageService`: Envio de mensagens (e-mails)
  - Composição do e-mail de confirmação
  - Integração com MessageRepository

- `ReportService`: Coordena a geração dos 4 relatórios

### 5. Report (Relatórios)

**Estrutura modular** - aplicação direta do princípio do CLAUDE.md:

```
CsvReport (classe abstrata)
├── OccupancyReport
├── RevenueReport
├── TourismTaxReport
└── RatePlanReport
```

`CsvReport` define a sequência comum a todos os relatórios:
1. Linha de título: "Pousada Maré Alta,{nome-relatorio},{periodo}"
2. Linha de cabeçalho
3. Linhas de dados
4. Linha de total

Cada relatório específico implementa apenas:
- `getReportName()`: Nome no título
- `getPeriod()`: Período formatado
- `getColumnNames()`: Colunas
- `getDataRows()`: Dados
- `getTotalRow()`: Linha de total

**Benefício**: Quando o contador pediu para adicionar a taxa de turismo e o relatório de planos, foram criadas apenas as classes específicas. A estrutura comum (título, formato CSV) foi herdada. No futuro, se todos os relatórios precisarem de uma linha de data de geração, essa mudança entra em `CsvReport.generate()` e afeta todos automaticamente.

### 6. API (Controllers REST)

Controllers mapeiam requests HTTP para os serviços:

- `RoomController`: Lista quartos
- `ReservationController`: CRUD + operações de reserva + comprovante
- `MessageController`: Lista mensagens
- `ReportController`: Gera relatórios CSV

Tratamento de erros com códigos específicos:
- `INVALID_REQUEST` (400)
- `ROOM_NOT_FOUND`, `RESERVATION_NOT_FOUND` (404)
- `ROOM_UNAVAILABLE`, `OPERATION_NOT_ALLOWED` (409)
- `CAPACITY_EXCEEDED` (422)

## Relatórios Implementados

### 1. Ocupação (occupancy)
- Filtra: reservas pagas que ocupam o quarto na data
- Ordena: por número do quarto
- Total: "total,X/5"

### 2. Financeiro (revenue)
- Filtra: reservas que começam no mês E deixaram dinheiro (amountPaid - refund > 0)
- Ordena: por check-in, depois por ID
- Total: soma do dinheiro líquido

### 3. Taxa de Turismo (tourism-tax)
- Filtra: reservas pagas que começam no mês
- Calcula: R$ 4,50 × hóspedes × noites
- Total: soma das taxas

### 4. Planos de Tarifa (rate-plans)
- Agrupa: por RatePlan (sempre mostra STANDARD e NON_REFUNDABLE)
- Filtra: reservas do mês que deixaram dinheiro
- Total: quantidade total + soma total

## Cálculo de Preços - Exemplo

Reserva no quarto 101 (STANDARD, R$ 249,90) de 10/07/2027 a 12/07/2027:

1. **Noite 1** (10/07): R$ 249,90 × 1.20 = R$ 299,88 (alta temporada - julho)
2. **Noite 2** (11/07): R$ 249,90 × 1.20 = R$ 299,88
3. **Subtotal**: R$ 299,88 + R$ 299,88 = R$ 599,76
4. **Desconto** (se NON_REFUNDABLE): R$ 599,76 × 0.15 = R$ 89,96
5. **Total**: R$ 599,76 - R$ 89,96 = R$ 509,80

Todos os passos usam `setScale(2, RoundingMode.HALF_EVEN)`.

## Estados e Transições

```
PENDING
  ├─ confirm() → CONFIRMED (amountPaid = total)
  │                 └─ checkIn() → CHECKED_IN
  │                                   └─ checkOut() → CHECKED_OUT
  └─ cancel() → CANCELLED (refund = amountPaid se STANDARD, senão 0)

CONFIRMED
  ├─ checkIn() → CHECKED_IN
  └─ cancel() → CANCELLED
```

Regras:
- Cancelamento só antes do check-in
- Cada transição valida o estado atual
- Refund é calculado no cancelamento baseado no ratePlan

## Validações

1. **Criação de Reserva**:
   - Quarto existe
   - Data de saída > data de entrada
   - Número de hóspedes entre 1 e capacidade do quarto
   - Quarto disponível (sem conflito de datas com reservas não-canceladas)

2. **Transições de Estado**:
   - Confirm: reserva PENDING
   - Check-in: reserva CONFIRMED
   - Check-out: reserva CHECKED_IN
   - Cancel: reserva PENDING ou CONFIRMED

## E-mail de Confirmação

Enviado automaticamente quando a reserva é confirmada:

- Destinatário: e-mail do hóspede
- Assunto: "Reserva confirmada - Pousada Maré Alta"
- Corpo: Saudação + comprovante + informações de check-in/out

As mensagens ficam armazenadas no banco (tabela `messages`) e podem ser consultadas via `/messages`.

## Banco de Dados

Schema criado automaticamente:

1. **rooms**: número, tipo, capacidade, diária
2. **reservations**: todos os campos da reserva
3. **messages**: canal, destinatário, assunto, corpo

Inicialização:
- Se `rooms` estiver vazia, os 5 quartos são inseridos automaticamente

## Testes

- `PricingCalculatorTest`: Valida cálculos de preço, alta temporada, descontos
- `CsvReportTest`: Valida estrutura comum dos relatórios

Total: 8 testes passando

## Execução

```bash
# Compilar
mvn clean package

# Executar
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=senha
export PORT=7070

java -jar target/reservas-1.0-SNAPSHOT.jar
```

O JAR gerado (9.3MB) contém todas as dependências (fat jar via maven-shade-plugin).

## Próximos Passos Possíveis

Baseado nas menções do dono da pousada:

1. **Novos relatórios**: Cancelamentos e hóspedes por estado
   - Criar `CancellationReport` e `GuestsByStateReport` herdando de `CsvReport`
   - Mudanças em um lugar só

2. **Mudança no valor da taxa de turismo**:
   - Alterar constante `TAX_PER_GUEST_PER_NIGHT` em `TourismTaxReport`
   - Ou criar sistema de configuração se precisar de histórico

3. **Data de geração nos relatórios**:
   - Adicionar no método `generate()` de `CsvReport`
   - Todos os relatórios herdam automaticamente

A arquitetura está preparada para essas evoluções sem quebrar o que já funciona.
