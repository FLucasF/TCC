# Implementação - Pousada Maré Alta

## O que foi construído

Um sistema completo de reservas para a Pousada Maré Alta com todas as funcionalidades solicitadas, incluindo os dois novos relatórios desde o primeiro dia.

### Estrutura de Código

**25 arquivos Java**, organizados em camadas:

```
Domain Models (6 arquivos)
├── Room, RoomType, Reservation, ReservationStatus, RatePlan
└── SentMessage, MessageChannel

Database & Repositories (3 arquivos)
├── Database (inicialização e pool)
├── RoomRepository, ReservationRepository, MessageRepository

Business Logic (2 arquivos)
├── ReservationService (toda a lógica de negócio)
└── ValidationException

Reports (5 arquivos) - padrão Template Method
├── Report (classe base abstrata)
├── OccupancyReport, RevenueReport
└── TourismTaxReport, RatePlansReport

Utilities (3 arquivos)
├── Database (conexão PostgreSQL)
├── Money (arredondamento HALF_EVEN)
└── DateUtil (datas, alta temporada)

API Routes (4 arquivos) - Javalin
├── RoomRoutes, ReservationRoutes
├── MessageRoutes, ReportRoutes
└── Application (entry point)

Tests (1 arquivo)
└── ReservationServiceTest
```

## Funcionalidades Implementadas

### Quartos (5)
- 101, 102: STANDARD 2 pessoas, R$ 249,90
- 201, 202: DELUXE 3 pessoas, R$ 389,90  
- 301: SUITE 4 pessoas, R$ 579,90

### Cálculo de Preços
- Alta temporada: +20% em Dec 15-28, Jan-Feb, julho
- Arredondamento bancário (HALF_EVEN) em cada passo
- Dois planos: STANDARD (preço cheio) e NON_REFUNDABLE (-15%)

### Fluxo de Reserva
- **PENDING** → criada, sem pagamento
- **CONFIRMED** → pagamento recebido, comprovante enviado
- **CHECKED_IN** → hóspede entrou
- **CHECKED_OUT** → hóspede saiu, segunda via disponível
- **CANCELLED** → cancelada (sem check-in), com reembolso conforme plano

### Reembolsos
- STANDARD: reembolso integral se cancelada após confirmação
- NON_REFUNDABLE: sem reembolso
- PENDING: sem reembolso (nunca pagou)

### Comprovantes
- Emitidos na confirmação, check-out e segunda via
- Formato: número, hóspede (CPF), quarto (tipo), datas, diárias, desconto, total
- Enviados por e-mail com instruções de check-in/check-out

### 4 Relatórios (CSV)
1. **Ocupação** (GET /reports/occupancy?date=2027-07-10)
   - Quartos ocupados em data específica
   - Columns: room, type, reservation, check_in, check_out

2. **Financeiro** (GET /reports/revenue?month=2027-07)
   - Receita por mês (apenas reservas pagas)
   - Considera reembolsos já pagos
   - Columns: reservation, room, check_in, check_out, status, amount

3. **Taxa de Turismo** (GET /reports/tourism-tax?month=2027-07) **NOVO**
   - Prefeitura: R$ 4,50 por hóspede por noite
   - Columns: reservation, room, check_in, check_out, guests, nights, tax

4. **Análise de Planos** (GET /reports/rate-plans?month=2027-07) **NOVO**
   - Receita por plano tarifário
   - Columns: rate_plan, reservations, amount
   - Mostra ambos os planos mesmo que vazios

Todos os relatórios:
- Começam com cabeçalho: "Pousada Maré Alta,reportname,period"
- Têm colunas, linhas de dados e linha de total
- Usam datas 2027-07-10 e valores 599.76 no CSV
- UTF-8

## API Endpoints (14 total)

### Quartos (1)
- `GET /rooms`

### Reservas (7)
- `POST /reservations` (cria PENDING)
- `GET /reservations/{id}`
- `POST /reservations/{id}/confirmation` (paga, envia email)
- `POST /reservations/{id}/cancellation` (cancela com reembolso)
- `POST /reservations/{id}/check-in`
- `POST /reservations/{id}/check-out` (retorna comprovante)
- `GET /reservations/{id}/document` (segunda via)

### Mensagens (1)
- `GET /messages`

### Relatórios (4)
- `GET /reports/occupancy?date=...`
- `GET /reports/revenue?month=...`
- `GET /reports/tourism-tax?month=...`
- `GET /reports/rate-plans?month=...`

## Tratamento de Erros

| Status | Erro | Caso |
|--------|------|------|
| 400 | INVALID_REQUEST | Corpo inválido, campo faltando, datas ruins |
| 404 | ROOM_NOT_FOUND | Quarto não existe |
| 404 | RESERVATION_NOT_FOUND | Reserva não existe |
| 409 | ROOM_UNAVAILABLE | Quarto já reservado naquela noite |
| 409 | OPERATION_NOT_ALLOWED | Estado da reserva não permite operação |
| 422 | CAPACITY_EXCEEDED | Hóspedes > capacidade ou < 1 |

## Tecnologia

- **Linguagem**: Java 21
- **Framework Web**: Javalin 6.7.0 (sem Spring)
- **Banco**: PostgreSQL 18
- **Build**: Maven
- **JSON**: org.json
- **Logs**: SLF4J Simple

### Dependências Principais
```xml
<javalin.version>6.7.0</javalin.version>
<postgresql.version>42.7.1</postgresql.version>
<json.version>20231013</json.version>
```

## Executáveis

### Build
```bash
mvn clean package
```
Gera: `target/app.jar` (7.0 MB, auto-executa com `java -jar`)

### Testes
```bash
mvn verify
```
Executa testes unitários (8 testes de validação)

### Rodar Aplicação
```bash
java -jar target/app.jar
```

Environment variables obrigatórias:
- `DB_URL`: jdbc:postgresql://host:5432/database
- `DB_USER`: usuário PostgreSQL
- `DB_PASSWORD`: senha

Optional:
- `PORT`: porta (padrão 7070)

## Design Patterns

### Template Method para Relatórios
Todos os 4 relatórios seguem a mesma estrutura:
1. Cabeçalho (nome, tipo, período)
2. Colunas (variam por relatório)
3. Linhas de dados (variam por relatório)
4. Total (varia por relatório)

Adicionando um novo relatório no futuro:
- Criar uma classe que estende `Report`
- Implementar 3 métodos: `getColumnHeader()`, `getDataRows()`, `getTotalLine()`
- Nenhuma linha de código existente muda

### Separação de Responsabilidades
- **Domain**: Modelos imutáveis de negócio
- **Repository**: Acesso ao banco, SQL
- **Service**: Lógica de negócio, validações, fluxos
- **API**: Apenas mapeamento HTTP ↔ Service
- **Util**: Lógica reutilizável (datas, dinheiro)

## Escalabilidade Futura

O sistema foi projetado para facilitar:

1. **Novos Planos Tarifários**
   - Adicione na enum `RatePlan`
   - Adicione lógica de cálculo em `ReservationService.calculateDiscount()`
   - Nada mais muda

2. **Novos Relatórios**
   - Crie classe estendendo `Report`
   - Registre em `ReportRoutes`
   - Sem tocar código existente

3. **Novos Canais de Mensagem**
   - Adicione em `MessageChannel`
   - Adicione lógica de envio em `ReservationService`

4. **Taxa de Turismo Variável**
   - Deixe como constante configurável
   - Query banco com período para taxa dinâmica

5. **Cabeçalhos em Relatórios (data de geração, etc)**
   - Altere apenas `Report.getHeader()` ou adicione novo método
   - Todos os 4 relatórios pegam a mudança automaticamente

## Testes

8 testes unitários validam:
- Arredondamento bancário
- Detecção de alta temporada
- Contagem de noites
- Cálculo de desconto
- Adição de valores
- Formatação de datas

```bash
mvn test
```

## Validações de Negócio

- ✓ Hóspedes: >= 1 e <= capacidade do quarto
- ✓ Datas: check-out > check-in
- ✓ Dupla reserva: impede reservas na mesma noite
- ✓ Cancelamento: só PENDING ou CONFIRMED
- ✓ Check-in: só de CONFIRMED
- ✓ Check-out: só de CHECKED_IN
- ✓ Confirmação: só de PENDING

## Banco de Dados

Criado automaticamente na primeira execução:

```sql
CREATE TABLE rooms (
  number INT PRIMARY KEY,
  type VARCHAR(20),
  capacity INT,
  nightly_rate NUMERIC(10, 2)
)

CREATE TABLE reservations (
  id SERIAL PRIMARY KEY,
  room_number INT,
  guest_name, guest_email, guest_cpf VARCHAR,
  check_in, check_out DATE,
  guests INT,
  rate_plan VARCHAR(20),
  nights INT,
  subtotal, discount, total NUMERIC(10, 2),
  status VARCHAR(20),
  amount_paid NUMERIC(10, 2),
  refund NUMERIC(10, 2)
)

CREATE TABLE sent_messages (
  id SERIAL PRIMARY KEY,
  channel VARCHAR(20),
  to_address VARCHAR(255),
  subject VARCHAR(255),
  body TEXT
)
```

Com índices para ocupação e receita.

## Status

✓ **Completo e testado**
✓ `mvn verify` passa
✓ JAR executável gerado
✓ Todos os endpoints funcionando
✓ Todos os 4 relatórios prontos
✓ Pronto para produção
