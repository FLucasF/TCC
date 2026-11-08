# Arquitetura do Sistema

## Visão Geral

O sistema segue uma arquitetura em camadas com separação clara de responsabilidades:

```
┌─────────────────────────────────────────┐
│           API REST (Javalin)            │
│  ErrorResponse, ApiMapper, RestApi      │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│            Services                     │
│  ReservationService, MessageService     │
│  DocumentService, PricingService        │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│         Repositories                    │
│  RoomRepository, ReservationRepository  │
│  MessageRepository                      │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│          PostgreSQL                     │
└─────────────────────────────────────────┘
```

## Camadas

### 1. Domain (Modelo de Domínio)

Entidades puras sem dependências externas:

- **Room**: Representa um quarto da pousada
- **Reservation**: Representa uma reserva com todos os cálculos
- **Document**: Representa um comprovante de reserva
- **Message**: Representa uma mensagem enviada
- **Enums**: RoomType, RatePlan, ReservationStatus

### 2. Repository (Acesso a Dados)

Camada de persistência usando JDBC puro:

- **Database**: Gerencia conexões (HikariCP) e migrations
- **RoomRepositoryImpl**: CRUD de quartos + inicialização
- **ReservationRepositoryImpl**: CRUD de reservas com queries especializadas
- **MessageRepositoryImpl**: CRUD de mensagens

### 3. Service (Lógica de Negócio)

Serviços que implementam as regras de negócio:

- **PricingService**: Calcula preços, considera alta temporada, aplica descontos
- **DocumentService**: Gera comprovantes
- **MessageService**: Formata e envia mensagens
- **ReservationService**: Orquestra todo o fluxo de reservas

### 4. Report (Relatórios)

Implementação do padrão **Template Method** para relatórios CSV:

```java
abstract class CsvReport {
    // Método template: define a estrutura fixa
    public final String generate() {
        título → cabeçalho → dados → total
    }
    
    // Métodos abstratos: cada relatório implementa
    protected abstract String getReportName();
    protected abstract String getPeriod();
    protected abstract String buildHeaderLine();
    protected abstract List<String> buildDataLines();
    protected abstract String buildTotalLine();
}
```

Relatórios concretos:
- **OccupancyReport**: Ocupação por noite
- **RevenueReport**: Receita por mês
- **TourismTaxReport**: Taxa de turismo
- **RatePlanReport**: Análise por plano de tarifa

### 5. API (Camada REST)

- **RestApi**: Configura endpoints e exception handlers
- **ApiMapper**: Converte entre JSON e objetos de domínio
- **ErrorResponse**: Formato padronizado de erros

## Decisões de Design

### Template Method para Relatórios

**Problema**: Todos os relatórios seguem a mesma estrutura (título → cabeçalho → dados → total), mas diferem nos detalhes. No futuro, todos precisarão incluir data de geração.

**Solução**: Template Method permite:
1. Estrutura fixa definida uma vez na classe base
2. Cada relatório implementa apenas o que varia
3. Mudanças na estrutura geral afetam todos de uma vez
4. Novos relatórios entram facilmente

**Como adicionar um novo relatório**:
```java
public class CancellationsReport extends CsvReport {
    @Override protected String getReportName() { return "cancellations"; }
    @Override protected String getPeriod() { return month.toString(); }
    @Override protected String buildHeaderLine() { /* colunas */ }
    @Override protected List<String> buildDataLines() { /* dados */ }
    @Override protected String buildTotalLine() { /* total */ }
}
```

### Arredondamento HALF_EVEN

Todos os cálculos monetários usam `RoundingMode.HALF_EVEN` (banker's rounding):
- Minimiza viés acumulativo
- Padrão IEEE 754
- Execução a cada passo do cálculo

### Separação de Responsabilidades

- **PricingService**: Apenas cálculos, sem persistência
- **ReservationService**: Orquestração, validações, fluxo de estados
- **Repositories**: Apenas SQL, sem lógica de negócio

### Exceptions como Controle de Fluxo

Exceptions específicas mapeiam para códigos HTTP:
```java
RoomNotFoundException → 404 ROOM_NOT_FOUND
RoomUnavailableException → 409 ROOM_UNAVAILABLE
CapacityExceededException → 422 CAPACITY_EXCEEDED
```

## Extensibilidade

### Adicionar Nova Taxa (ex: Taxa de Limpeza)

1. Criar `CleaningTaxReport extends CsvReport`
2. Implementar os 5 métodos abstratos
3. Adicionar endpoint em `RestApi`

### Mudar Estrutura de Todos os Relatórios

Exemplo: adicionar data de geração ao final

```java
abstract class CsvReport {
    public final String generate() {
        StringBuilder csv = new StringBuilder();
        csv.append(buildTitleLine()).append("\n");
        csv.append(buildHeaderLine()).append("\n");
        for (String line : buildDataLines()) {
            csv.append(line).append("\n");
        }
        csv.append(buildTotalLine()).append("\n");
        csv.append("Gerado em,").append(LocalDate.now()).append("\n"); // NOVA LINHA
        return csv.toString();
    }
}
```

Uma mudança, todos os relatórios atualizados.

### Adicionar Novo Estado de Reserva

1. Adicionar em `ReservationStatus`
2. Atualizar transições em `ReservationService`
3. Ajustar queries de relatórios se necessário

## Testes

- **PricingServiceTest**: Valida cálculos de preço e alta temporada
- Testes unitários isolados, sem dependências externas
- Para testes de integração, usar Testcontainers com PostgreSQL

## Performance

- **Connection pooling**: HikariCP com pool de 10 conexões
- **Queries preparadas**: Proteção contra SQL injection
- **Índices implícitos**: Primary keys nas tabelas

## Segurança

- Validação de entrada em `ApiMapper`
- Prepared statements previnem SQL injection
- Validações de estado em `ReservationService`
