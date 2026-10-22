# Arquitetura do Sistema

## Visão Geral

O sistema foi projetado seguindo as orientações do CLAUDE.md, com foco em **extensibilidade** e **organização para mudanças futuras**.

## Princípios de Design Aplicados

### 1. "Acrescentar o próximo caso num lugar só"

**Sistema de Relatórios**

Todos os relatórios (ocupação, financeiro, taxa de turismo, planos de tarifa) compartilham a mesma estrutura através da classe abstrata `Report`:

```
Report (abstrata)
├── OccupancyReport
├── RevenueReport
├── TourismTaxReport
└── RatePlansReport
```

**Estrutura comum (definida uma vez):**
- Linha de título: "Pousada Maré Alta,{nome},{período}"
- Linha de cabeçalho com nomes das colunas
- Linhas de dados
- Linha de total

**O que cada relatório específico define:**
- Nome do relatório
- Período formatado
- Colunas específicas
- Lógica de filtragem dos dados
- Cálculo do total

**Benefício:** Quando for necessário adicionar a linha de data no final de todos os relatórios (mencionado no enunciado), a mudança será feita **uma única vez** na classe `Report`, afetando automaticamente todos os 4 relatórios existentes e qualquer novo relatório futuro.

### 2. "Escrever a sequência uma vez só"

**Fluxo de Reserva**

A máquina de estados da reserva está centralizada na classe `Reservation`:

```java
PENDING → confirm() → CONFIRMED
        ↓                ↓
    cancel()         checkIn()
        ↓                ↓
   CANCELLED      CHECKED_IN
                       ↓
                  checkOut()
                       ↓
                 CHECKED_OUT
```

Cada transição:
- Valida o estado atual
- Executa a lógica específica
- Atualiza o estado

**Cálculo de Preços**

O cálculo do total de uma reserva segue sempre a mesma sequência:

1. Calcular preço base de cada noite
2. Aplicar acréscimo de alta temporada (se aplicável)
3. Arredondar cada noite
4. Somar as noites (subtotal)
5. Calcular desconto (se aplicável)
6. Subtrair desconto do subtotal
7. Retornar total

Essa sequência está em `Reservation.calculateTotal()` e é reutilizada em todos os lugares que precisam do total.

### 3. "Organizar só onde a variação existe"

**Alta Temporada**

O enunciado deixa claro que as regras de alta temporada podem mudar. Por isso, a lógica está isolada no método `isHighSeason(LocalDate)`, que pode ser facilmente modificado sem afetar o restante do código.

**Taxa de Turismo**

A prefeitura muda o valor todo ano. O valor está em `Reservation.calculateTourismTax()` como uma constante que pode ser facilmente parametrizada quando necessário (ex: ler de configuração ou banco de dados).

**Tipos de Quarto**

Os quartos "quase nunca mudam", então estão em uma estrutura simples (enum `RoomType` + inicialização no banco). Não há complexidade desnecessária.

## Estrutura de Pacotes

```
com.pousadamarealta
├── domain                    # Modelos de domínio
│   ├── RoomType             # Enum: tipos de quarto
│   ├── RatePlan             # Enum: planos de tarifa
│   ├── ReservationStatus    # Enum: status da reserva
│   ├── Room                 # Modelo: quarto
│   ├── Reservation          # Modelo: reserva (com toda lógica de cálculo)
│   └── Message              # Modelo: mensagem enviada
│
├── infrastructure            # Acesso a dados
│   ├── Database             # Gerenciamento de conexão e inicialização
│   ├── RoomRepository       
│   ├── ReservationRepository
│   └── MessageRepository
│
├── service                   # Lógica de negócio
│   ├── ReservationService   # Orquestração de reservas
│   ├── NotificationService  # Envio de mensagens
│   ├── ReportService        # Coordenação de relatórios
│   └── report               # Sistema de relatórios
│       ├── Report           # Classe base abstrata
│       ├── OccupancyReport
│       ├── RevenueReport
│       ├── TourismTaxReport
│       └── RatePlansReport
│
├── api                       # Camada HTTP
│   ├── ApiHandlers          # Handlers dos endpoints
│   ├── ErrorResponse        # DTO de erro
│   └── dto                  # DTOs de request/response
│       ├── RoomResponse
│       ├── ReservationResponse
│       ├── CreateReservationRequest
│       ├── DocumentResponse
│       └── MessageResponse
│
└── Main                      # Inicialização da aplicação
```

## Pontos de Extensão

### 1. Adicionar um Novo Relatório

Exemplo: Relatório de cancelamentos

```java
public class CancellationReport extends Report {
    // Implementar apenas os métodos abstratos
    @Override
    protected String getReportName() {
        return "cancellations";
    }
    
    @Override
    protected String buildHeader() {
        return "reservation,room,guest,cancelled_at,refund";
    }
    
    // ... resto da implementação
}
```

Adicionar no `ReportService` e criar o endpoint. **Nenhuma mudança** nos outros relatórios.

### 2. Adicionar um Novo Status

Se for necessário adicionar um status (ex: NO_SHOW), basta:

1. Adicionar em `ReservationStatus`
2. Adicionar método de transição em `Reservation`
3. Adicionar endpoint em `ApiHandlers`

A lógica de relatórios, cálculos e validações continua funcionando.

### 3. Mudar Regra de Alta Temporada

Toda a lógica está em `Reservation.isHighSeason(LocalDate)`. Mudar apenas esse método afeta automaticamente todos os cálculos de preço.

### 4. Parametrizar Taxa de Turismo

Atual:
```java
BigDecimal taxPerGuestPerNight = new BigDecimal("4.50");
```

Futuro (quando a taxa mudar anualmente):
```java
BigDecimal taxPerGuestPerNight = getTaxForYear(date.getYear());
```

### 5. Adicionar Linha de Data nos Relatórios

Quando for necessário adicionar a data de geração, alterar apenas `Report.generate()`:

```java
public String generate() {
    StringBuilder csv = new StringBuilder();
    csv.append(buildTitle()).append("\n");
    csv.append(buildHeader()).append("\n");
    
    List<String> dataLines = buildDataLines();
    for (String line : dataLines) {
        csv.append(line).append("\n");
    }
    
    csv.append(buildTotal()).append("\n");
    csv.append("generated_at,").append(LocalDate.now());  // NOVA LINHA
    
    return csv.toString();
}
```

Todos os 4 relatórios (ou 10, ou 20 futuros) ganham automaticamente a nova linha.

## Decisões Técnicas

### BigDecimal e Arredondamento

Todo dinheiro usa `BigDecimal` com `RoundingMode.HALF_EVEN` (arredondamento "meio para o par"), conforme especificado.

### Validações Centralizadas

Validações de negócio estão nos modelos de domínio (ex: `Reservation.confirm()` valida o status). Validações de requisição estão em `ReservationService`.

### Repositórios Simples

Usam JDBC direto, sem ORM. Simples e explícito para um sistema desse porte.

### Sem Comentários Desnecessários

O código é auto-explicativo através de nomes claros de métodos e classes. Comentários são usados apenas onde a lógica de negócio não é óbvia (ex: regras de alta temporada).

## Testes

### Testes Unitários

- `ReservationTest`: Testa toda a lógica de negócio da reserva
- `ReportTest`: Testa geração de relatórios

**Rodam sem dependências externas** (sem banco de dados).

### Testes de Integração

- `ReservationSystemIT` (renomeado para `.manual`): Testa o fluxo completo da API

**Requer PostgreSQL rodando**.

## Rodando o Sistema

```bash
# Compilar e empacotar
mvn clean package

# Executar
java -jar target/reservations-1.0.0.jar

# Com variáveis customizadas
DB_URL=jdbc:postgresql://host:5432/db \
DB_USER=user \
DB_PASSWORD=pass \
PORT=8080 \
java -jar target/reservations-1.0.0.jar
```

## Futuras Extensões Previstas

O enunciado menciona explicitamente que vão pedir:

1. ✅ **Mais relatórios** (cancelamentos, hóspedes por estado)
   - Estrutura extensível pronta
   
2. ✅ **Mudança no valor da taxa de turismo**
   - Valor isolado em um lugar
   
3. ✅ **Linha de data nos relatórios**
   - Mudança afetará todos de uma vez

Todas essas mudanças foram antecipadas no design e terão impacto mínimo quando necessárias.
