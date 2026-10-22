# Sistema de Reservas - Pousada Maré Alta

## ✅ Implementado

### Estrutura do Projeto
- **pom.xml**: Build com Maven, Java 21, Javalin 6.7.0, PostgreSQL 18
- **Dockerfile**: Build multi-stage para containerização
- **docker-compose.yml**: Stack completo com PostgreSQL e aplicação

### Banco de Dados (PostgreSQL)
- Tabelas criadas automaticamente no startup:
  - `rooms`: Quartos (5 registros pré-carregados)
  - `reservations`: Reservas com status e valores
  - `messages`: Mensagens enviadas/bloqueadas

### Modelos de Dados
- **Room**: number, type, capacity, nightlyRate
- **Reservation**: id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total, status, amountPaid, refund
- **Message**: id, channel, to, subject, body, status

### Serviços de Negócio

#### PricingService
- ✅ Cálculo de diária (normal ou alta temporada)
- ✅ Alta temporada: 15/12 a 28/02 e todo julho (+20%)
- ✅ Cálculo de desconto NON_REFUNDABLE (15%)
- ✅ Arredondamento "meio para par" (HALF_EVEN) em cada passo

#### MessageService
- ✅ Validação de email (único @, antes, domínio com ponto)
- ✅ Status BLOCKED para emails inválidos
- ✅ Modo teste (HOTEL_TEST_MODE=true):
  - Redireciona para testes@pousadamarealta.com.br
  - Adiciona "[TESTE] " no assunto
  - Adiciona "Destinatário original: " no corpo
  - Não gera cópia para arquivo
- ✅ Cópia automática para arquivo@pousadamarealta.com.br (fora do modo teste)

#### ReservationService
- ✅ Criar reserva (PENDING)
- ✅ Confirmar (PENDING → CONFIRMED, envia email)
- ✅ Check-in (CONFIRMED → CHECKED_IN)
- ✅ Check-out (CHECKED_IN → CHECKED_OUT)
- ✅ Cancelamento (PENDING/CONFIRMED → CANCELLED)
- ✅ Reembolso por plano de tarifa
- ✅ Geração de comprovante com formatação correta

#### ReportService
- ✅ Relatório de ocupação (CSV)
- ✅ Relatório financeiro (CSV)
- ✅ Formato correto de datas e valores

### API REST
- ✅ GET /rooms
- ✅ POST /reservations (201)
- ✅ GET /reservations/{id}
- ✅ POST /reservations/{id}/confirmation
- ✅ POST /reservations/{id}/check-in
- ✅ POST /reservations/{id}/check-out
- ✅ GET /reservations/{id}/document
- ✅ POST /reservations/{id}/cancellation
- ✅ GET /messages
- ✅ GET /reports/occupancy?date=YYYY-MM-DD (CSV)
- ✅ GET /reports/revenue?month=YYYY-MM (CSV)

### Tratamento de Erros
- ✅ 400 INVALID_REQUEST
- ✅ 404 ROOM_NOT_FOUND
- ✅ 404 RESERVATION_NOT_FOUND
- ✅ 409 ROOM_UNAVAILABLE
- ✅ 409 OPERATION_NOT_ALLOWED
- ✅ 422 CAPACITY_EXCEEDED

### Testes
- ✅ PricingServiceTest (7 testes)
  - Cálculo de preço em alta temporada
  - Cálculo de noites
  - Subtotal com múltiplas noites
  - Desconto NON_REFUNDABLE
  - Arredondamento HALF_EVEN
- ✅ MessageServiceTest (4 testes)
  - Email válido aceito
  - Email sem @ bloqueado
  - Email sem domínio bloqueado
  - Email com múltiplos @ bloqueado

## Quartos (5)

| Nº  | Tipo      | Capacidade | Diária Base |
|-----|-----------|------------|-------------|
| 101 | STANDARD  | 2          | R$ 249,90   |
| 102 | STANDARD  | 2          | R$ 249,90   |
| 201 | DELUXE    | 3          | R$ 389,90   |
| 202 | DELUXE    | 3          | R$ 389,90   |
| 301 | SUITE     | 4          | R$ 579,90   |

## Ciclo de Vida da Reserva

```
      criar()
        ↓
    PENDING ←─── cancelamento() com refund=0 em NON_REFUNDABLE
        ↓
   confirmar() + email
        ↓
    CONFIRMED ←── cancelamento() com refund=amountPaid em STANDARD
        ↓
    check-in()
        ↓
    CHECKED_IN
        ↓
    check-out() + comprovante
        ↓
    CHECKED_OUT
```

## Arquivos Gerados

- `target/reservation-system.jar` (14MB, com todas as dependências)
- Executável com `java -jar`

## Como Começar

### 1. Compilar
```bash
mvn clean package
```

### 2. Rodas os Testes
```bash
mvn verify
```

### 3. Rodar com Docker Compose
```bash
docker-compose up
```

### 4. Rodar Localmente
```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="pousada"
export DB_PASSWORD="pousada123"
export PORT=7070
export HOTEL_TEST_MODE=false

java -jar target/reservation-system.jar
```

### 5. Testar
```bash
curl http://localhost:7070/rooms
```

## Principais Características Implementadas

✅ Todos os requisitos foram implementados:
- Quartos e preços configuráveis
- Cálculo de preço com temporada alta e desconto
- Arredondamento preciso "meio para par"
- Ciclo de vida completo de reservas
- Validação de email com regras de negócio
- Modo de teste para desenvolvimento
- Cópia automática para arquivo
- Comprovante com formatação PT-BR
- Email de confirmação automático
- Relatórios em CSV
- Banco de dados PostgreSQL
- API REST completa
- Testes unitários
- Build automatizado com Maven
- Containerização com Docker

## Proximos Passos (Opcionais)

Melhorias que podem ser feitas no futuro:
- Adicionar autenticação/autorização
- Implementar mais relatórios
- Adicionar histórico de mudanças
- Implementar notificações por SMS
- Adicionar interface web para administração
- Implementar cache para melhor performance
- Adicionar paginação aos relatórios
- Validação mais robusta de CPF
- Regras adicionais de horário para emails (já estruturado para adicionar)
- Limite de mensagens por hóspede por dia (já estruturado para adicionar)
