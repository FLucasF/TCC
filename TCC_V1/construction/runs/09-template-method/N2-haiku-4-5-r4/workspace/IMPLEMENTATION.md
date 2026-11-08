# Implementação - Sistema de Reservas Pousada Maré Alta

## Resumo

Sistema de gerenciamento de reservas completo para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Arquitetura

### Estrutura de Pacotes

```
com.pousada/
├── App.java                    # Entrada principal e rotas HTTP
├── Database.java               # Conexão e inicialização do banco de dados
├── GsonJsonMapper.java         # Configuração de serialização JSON
├── ApiException.java           # Exceção personalizada para a API
├── ErrorResponse.java          # Resposta de erro padrão
│
├── Models/
│   ├── Room.java               # Quarto
│   ├── Reservation.java        # Reserva
│   ├── Message.java            # Mensagem/Email
│   ├── ReservationDocument.java # Comprovante
│   └── Request/Response Classes
│
├── Enums/
│   ├── RoomType.java           # STANDARD, DELUXE, SUITE
│   ├── ReservationStatus.java  # PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED
│   └── RatePlan.java           # STANDARD, NON_REFUNDABLE
│
├── Services/
│   ├── RoomService.java        # Operações com quartos
│   ├── ReservationService.java # Operações com reservas (criar, confirmar, cancelar, etc)
│   ├── MessageService.java     # Operações com mensagens
│   └── ReportService.java      # Orquestração de relatórios
│
├── Reports/
│   ├── Report.java             # Template Method base
│   ├── OccupancyReport.java   # Relatório de ocupação
│   ├── RevenueReport.java      # Relatório de faturamento
│   ├── TourismTaxReport.java   # Relatório de taxa de turismo
│   └── RatePlansReport.java    # Relatório de planos de tarifa
│
└── Utilities/
    ├── DateUtils.java          # Formatação e cálculos com datas
    ├── Money.java              # Formatação de valores monetários
    └── PriceCalculator.java    # Cálculo de preços com alta temporada
```

## Padrões de Design

### Template Method (Relatórios)
- Classe abstrata `Report` define a estrutura de geração (título, cabeçalho, linhas, total)
- Cada tipo de relatório estende `Report` e implementa os passos específicos
- Novo relatório requer apenas uma nova classe, sem alterar código existente

## Banco de Dados

### Tabelas

**rooms**
- number (INT, PK)
- type (VARCHAR)
- capacity (INT)
- nightly_rate (NUMERIC)

**reservations**
- id (SERIAL, PK)
- room_number (INT, FK)
- guest_name (VARCHAR)
- guest_email (VARCHAR)
- guest_cpf (VARCHAR)
- check_in (DATE)
- check_out (DATE)
- guests (INT)
- rate_plan (VARCHAR)
- nights (INT)
- subtotal (NUMERIC)
- discount (NUMERIC)
- total (NUMERIC)
- status (VARCHAR)
- amount_paid (NUMERIC)
- refund (NUMERIC)

**messages**
- id (SERIAL, PK)
- channel (VARCHAR)
- recipient (VARCHAR)
- subject (VARCHAR)
- body (TEXT)
- created_at (TIMESTAMP)

## Cálculos

### Preço por Noite
1. Base: diária do quarto
2. Alta temporada: +20%
3. Arredondamento: HALF_EVEN (meio para o par)

### Desconto
- STANDARD: sem desconto
- NON_REFUNDABLE: 15% sobre subtotal

### Total da Reserva
- Subtotal - Desconto

### Taxa de Turismo
- R$ 4,50 por hóspede por noite

## Estados da Reserva

```
PENDING
  ↓
  └─→ CONFIRMED → CHECKED_IN → CHECKED_OUT
  ↓
  └─→ CANCELLED (antes do check-in)
```

**Regras:**
- PENDING → CONFIRMED: requer pagamento
- CONFIRMED → CHECKED_IN: check-in no dia
- CHECKED_IN → CHECKED_OUT: check-out no dia
- Cancelamento reembolsa no plano STANDARD, não reembolsa em NON_REFUNDABLE
- Reserva pendente não reembolsa nada (não tinha pagamento)

## API Response Codes

- 200 OK: Sucesso em GET
- 201 Created: Reserva criada com sucesso
- 400 Bad Request: Dados inválidos (INVALID_REQUEST)
- 404 Not Found: Recurso não encontrado (ROOM_NOT_FOUND, RESERVATION_NOT_FOUND)
- 409 Conflict: Conflito de estado (ROOM_UNAVAILABLE, OPERATION_NOT_ALLOWED)
- 422 Unprocessable Entity: Capacidade excedida (CAPACITY_EXCEEDED)

## Recursos Especiais

### Validações
- Check-out deve ser após check-in
- Número de hóspedes: 1 a capacidade do quarto
- Quarto não pode ter reservas sobrepostas (excluindo canceladas)
- CPF, email e nome são armazenados sem validação (conforme requisito)

### Formatações
- Datas JSON: yyyy-MM-dd
- Datas em texto: dd/MM/yyyy
- Valores monetários JSON: número com 2 casas (599.76)
- Valores monetários em texto: R$ 1.234,56 (com vírgula como separador decimal)
- CPF: armazenado conforme fornecido (ex: 111.222.333-44)

### Comprovante de Reserva
- Título: "Comprovante de reserva nº {id}"
- Hóspede com CPF
- Quarto com tipo
- Data de entrada e saída com número de diárias
- Diárias (subtotal)
- Desconto (se houver)
- Total

### Email de Confirmação
- Assunto: "Reserva confirmada - Pousada Maré Alta"
- Corpo: Saudação, dados da reserva com comprovante, instruções de horário
- Enviado automaticamente ao confirmar pagamento

## Testes

### Disponíveis

1. **test-api.sh** - Teste básico dos endpoints
2. **integration-test.sh** - Teste completo de um fluxo de reserva

### Execução

```bash
# Iniciar aplicação
bash run.sh

# Em outro terminal, executar testes
bash integration-test.sh
```

## Extensibilidade

### Adicionar Novo Quarto
Modificar apenas `Database.initializeRooms()` ou inserir manualmente no banco.

### Adicionar Novo Relatório
1. Estender classe `Report`
2. Implementar métodos abstratos
3. Registrar em `ReportService`

### Mudar Valor de Taxa de Turismo
Modificar constante `TAX_PER_GUEST_PER_NIGHT` em `TourismTaxReport.java`

### Alterar Formato de Relatórios
Modificar método `generate()` em `Report.java` (afeta todos)

## Dependências Principais

- io.javalin:javalin:6.7.0
- org.postgresql:postgresql:42.7.1
- com.zaxxer:HikariCP:5.1.0
- com.google.code.gson:gson:2.10.1
- org.slf4j:slf4j-simple:2.0.9

## Build e Deploy

```bash
# Compilar
mvn clean compile

# Testar
mvn test

# Empacotar
mvn clean package

# Executar
java -jar target/mare-alta.jar
```

O JAR é auto-executável e inclui todas as dependências.
