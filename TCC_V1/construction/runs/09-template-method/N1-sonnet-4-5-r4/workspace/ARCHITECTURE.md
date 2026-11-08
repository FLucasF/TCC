# Arquitetura do Sistema

Este documento explica as decisões de arquitetura do sistema de reservas da Pousada Maré Alta.

## Princípios de Design

O sistema foi construído seguindo as orientações do CLAUDE.md:

### 1. Cada novo caso entra em um lugar só

Quando uma nova funcionalidade precisa ser adicionada, ela deve entrar em um único ponto do código, sem necessidade de modificar várias partes.

**Exemplo - Relatórios:**
Todos os relatórios compartilham a mesma estrutura (título + cabeçalho + linhas + total). A classe abstrata `ReportGenerator` implementa esse template, e cada tipo específico de relatório apenas define suas colunas e dados únicos.

```java
public abstract class ReportGenerator {
    public String generate(String period) {
        // Estrutura comum para todos os relatórios
        StringBuilder csv = new StringBuilder();
        csv.append(buildTitle(period)).append("\n");
        csv.append(buildHeader()).append("\n");
        // ... linhas de dados ...
        csv.append(buildTotal(period));
        return csv.toString();
    }
    
    // Cada relatório implementa apenas o que é único
    protected abstract String getReportName();
    protected abstract String buildHeader();
    protected abstract List<String> buildDataRows(String period);
    protected abstract String buildTotal(String period);
}
```

Benefício: Quando a gerência pedir para adicionar uma linha com a data de geração no rodapé de todos os relatórios, a mudança será feita uma única vez na classe `ReportGenerator`, e todos os relatórios herdarão automaticamente essa funcionalidade.

### 2. Sequências repetidas são escritas uma vez

Quando vários casos repetem a mesma sequência de passos, essa sequência é extraída para um método único.

**Exemplo - Cálculo de preços:**
O cálculo do total de uma reserva segue sempre a mesma sequência:
1. Calcular o preço de cada noite (considerando alta temporada)
2. Somar todas as noites (subtotal)
3. Calcular desconto (se aplicável)
4. Calcular total (subtotal - desconto)

Essa sequência está centralizada no `PricingService`:

```java
public class PricingService {
    public BigDecimal calculateNightPrice(Room room, LocalDate night) {
        // Considera alta temporada automaticamente
    }
    
    public BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        // Soma o preço de cada noite
    }
    
    public BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        // Aplica desconto conforme o plano
    }
    
    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        // Calcula o total final
    }
}
```

Cada passo é um método separado porque cada um tem sua própria regra de arredondamento.

### 3. Organização apenas onde a variação existe

Não criamos abstrações ou estruturas complexas para casos que não variam.

**Exemplo - Quartos da pousada:**
Os quartos são fixos e raramente mudam. Por isso, são simplesmente registrados no banco de dados na inicialização, sem necessidade de uma camada de configuração complexa.

**Contra-exemplo - Se tivéssemos seguido o caminho errado:**
Poderíamos ter criado um sistema de "regras de preço configuráveis" com XML ou banco de dados, mas isso seria sobre-engenharia para um caso que não varia na prática.

## Estrutura de Pacotes

```
br.com.marealta/
├── domain/          # Entidades de negócio
│   ├── Room
│   ├── Reservation
│   ├── Message
│   └── enums (RoomType, RatePlan, ReservationStatus)
│
├── repository/      # Acesso a dados
│   ├── RoomRepository
│   ├── ReservationRepository
│   └── MessageRepository
│
├── service/         # Lógica de negócio
│   ├── ReservationService    # Orquestra operações de reserva
│   ├── MessageService         # Gerencia envio de mensagens
│   └── DocumentService        # Gera comprovantes
│
├── pricing/         # Cálculos de preços isolados
│   ├── PricingService
│   └── SeasonCalculator
│
├── report/          # Geração de relatórios
│   ├── ReportGenerator       # Template abstrato
│   ├── OccupancyReport
│   ├── RevenueReport
│   ├── TourismTaxReport
│   └── RatePlansReport
│
├── api/             # Camada HTTP
│   ├── controller/
│   └── dto/
│
└── database/        # Setup do banco
    └── Database
```

## Casos de Uso e Extensibilidade

### Adicionando um novo relatório

1. Criar uma classe que estende `ReportGenerator`
2. Implementar os 4 métodos abstratos
3. Registrar no `ReportController`

**Não é necessário:** modificar estrutura de CSV, título, ou formato de saída.

### Adicionando uma nova regra de preço

1. Modificar apenas o método relevante em `PricingService`
2. Por exemplo, para adicionar desconto para estadias longas, modificar apenas `calculateDiscount`

**Não é necessário:** modificar o fluxo de criação de reservas ou outros cálculos.

### Mudando o formato de e-mail

1. Modificar apenas `MessageService.buildConfirmationEmail()`

**Não é necessário:** modificar a persistência ou o envio de mensagens.

## Regras de Negócio Implementadas

### Alta Temporada
- 15 de dezembro a 28 de fevereiro
- Julho inteiro
- Preço aumenta em 20%

### Planos de Tarifa
- **STANDARD**: Preço normal, reembolso total ao cancelar
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso

### Transições de Estado
```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
    ↓
CANCELLED
```

### Validações
- Quarto não pode ter duas reservas na mesma noite (exceto canceladas)
- Check-out deve ser depois do check-in
- Número de hóspedes entre 1 e capacidade do quarto
- Operações só podem ocorrer em estados válidos

## Arredondamento

Todos os valores monetários usam arredondamento "meio para o par" (HALF_EVEN):
- 0.5 arredonda para 0 (par mais próximo)
- 1.5 arredonda para 2 (par mais próximo)

Isso é implementado em `PricingService.round()` e aplicado em cada etapa do cálculo.

## Persistência

O sistema utiliza PostgreSQL com JDBC direto, sem ORM. Esta decisão foi tomada porque:
- O modelo de dados é simples e estável
- As queries são diretas e performáticas
- Evita complexidade desnecessária

O banco é inicializado automaticamente na primeira execução, criando:
- Tabelas necessárias
- População inicial dos quartos

## Testes

### Testes Unitários
- `PricingServiceTest`: Valida cálculos de preços
- `SeasonCalculatorTest`: Valida detecção de alta temporada
- `DocumentServiceTest`: Valida geração de comprovantes

### Testes de Integração
- `IntegrationTest`: Valida fluxos completos com banco de dados

Os testes de integração usam `Assumptions.assumeTrue()` para serem pulados automaticamente quando o banco não está disponível, permitindo que `mvn test` funcione mesmo sem infraestrutura.
