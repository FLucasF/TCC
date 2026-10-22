# Arquitetura - Pousada Maré Alta

## Estrutura de Pacotes

```
com.pousada.marealta
├── api/              # Respostas da API
│   └── ErrorResponse
├── db/               # Camada de banco de dados
│   ├── Database      # Inicialização e conexão
│   ├── RoomRepository
│   ├── ReservationRepository
│   └── MessageRepository
├── model/            # Modelos de dados
│   ├── Room
│   ├── Reservation
│   ├── Message
│   ├── Document
│   ├── RoomType
│   ├── RatePlan
│   ├── ReservationStatus
│   └── MessageStatus
├── service/          # Lógica de negócios
│   ├── ReservationService
│   ├── MessageService
│   ├── DocumentService
│   └── ReportService
├── util/             # Utilitários
│   ├── PriceCalculator
│   ├── EmailValidator
│   ├── DateFormatter
│   ├── MoneyFormatter
│   └── CpfFormatter
└── Application       # Ponto de entrada e rotas
```

## Fluxo de Dados

### Criação de Reserva
1. **Cliente** envia POST /reservations com dados
2. **Application** recebe e valida formato JSON
3. **ReservationService** valida regras de negócio:
   - Quarto existe?
   - Quarto tem capacidade?
   - Datas válidas?
   - Quarto disponível?
4. **PriceCalculator** calcula preços com alta temporada
5. **ReservationRepository** persiste no PostgreSQL
6. **Resposta** retorna reserva criada com status PENDING

### Confirmação de Reserva
1. **Cliente** envia POST /reservations/{id}/confirmation com amountPaid
2. **ReservationService** verifica se está em status PENDING
3. **MessageService** compõe email de confirmação
4. **EmailValidator** valida endereço do hóspede
5. Se válido: **MessageRepository** persiste email como SENT e cria cópia para arquivo
6. Se inválido: **MessageRepository** persiste como BLOCKED
7. Se modo teste: redireciona para testes@pousadamarealta.com.br
8. **ReservationRepository** atualiza status para CONFIRMED e amountPaid
9. **Resposta** retorna reserva confirmada

### Geração de Comprovante
1. **Cliente** requisita GET /reservations/{id}/document
2. **ReservationService** busca dados da reserva
3. **DocumentService** formata texto do comprovante com:
   - **CpfFormatter** formata CPF (111.222.333-44)
   - **DateFormatter** formata datas (10/03/2027)
   - **MoneyFormatter** formata valores (R$ 779,80)
4. **Resposta** retorna JSON com dados do comprovante

### Geração de Relatórios
1. **Cliente** requisita GET /reports/occupancy?date=2027-03-10
2. **ReportService** consulta reservas confirmadas/checkedIn/checkedOut para a data
3. **RoomRepository** busca informações dos quartos
4. **Resposta** retorna CSV com ocupação

## Componentes Principais

### PriceCalculator
- Calcula se é alta temporada (15/12-28/02 e julho)
- Aplica multiplicador de 1.20 em alta temporada
- Arredonda com HALF_EVEN (meio para o par)
- Calcula desconto NON_REFUNDABLE (15%)

### EmailValidator
- Valida formato: algo@dominio.com
- Rejeita: @dominio, algo@, algo@dominio., algo@@
- Rejeita valores null ou vazios

### MessageService
- Intercepta todos os emails antes de enviar
- Se modo teste: redireciona para testes@pousadamarealta.com.br
- Se email inválido: marca como BLOCKED
- Se válido: marca como SENT e cria cópia para arquivo (exceto em teste)

### ReservationService
- Orquestra toda a lógica de reservas
- Valida transições de estado
- Calcula preços via PriceCalculator
- Envia emails via MessageService
- Persiste via ReservationRepository

## Transições de Estado

```
PENDING ──confirm──> CONFIRMED ──check-in──> CHECKED_IN ──check-out──> CHECKED_OUT
  ↓
cancel (sem reembolso se foi pago)

CONFIRMED ──cancel──> CANCELLED (reembolsa se era STANDARD)
  ↓
check-in ──> ... (não pode mais cancelar)
```

## Banco de Dados

### Tabelas

#### rooms
- number (PK)
- type (STANDARD, DELUXE, SUITE)
- capacity (2, 3, 4)
- nightly_rate (249.90, 389.90, 579.90)

#### reservations
- id (BIGSERIAL PK)
- room_number (FK)
- guest_name, guest_email, guest_cpf
- check_in, check_out
- guests, rate_plan
- nights, subtotal, discount, total
- status, amount_paid, refund

#### messages
- id (BIGSERIAL PK)
- channel (EMAIL)
- recipient
- subject, body
- status (SENT, BLOCKED)

## Tratamento de Erros

### Status HTTP

- **201 Created**: Reserva criada com sucesso
- **400 Bad Request**: INVALID_REQUEST (campos faltando, formato inválido)
- **404 Not Found**: ROOM_NOT_FOUND, RESERVATION_NOT_FOUND
- **409 Conflict**: ROOM_UNAVAILABLE, OPERATION_NOT_ALLOWED
- **422 Unprocessable Entity**: CAPACITY_EXCEEDED

## Variáveis de Ambiente

| Variável | Obrigatória | Padrão | Uso |
|----------|-------------|--------|-----|
| DB_URL | Sim | - | jdbc:postgresql://... |
| DB_USER | Sim | - | usuário PostgreSQL |
| DB_PASSWORD | Sim | - | senha PostgreSQL |
| PORT | Não | 7070 | porta HTTP |
| HOTEL_TEST_MODE | Não | false | modo teste de emails |

## Segurança

- **SQL Injection**: Usamos PreparedStatement para todas as queries
- **Email Validation**: Validação rigorosa antes de enviar
- **Campos obrigatórios**: Validação de presença e tipo
- **Estados**: Validação de transições permitidas
- **Isolamento**: Cada contexto HTTP tem sua própria transação

## Performance

- Conexões reutilizadas via DriverManager (pool simples)
- Índices automáticos em chaves primárias
- Queries otimizadas com condições apropriadas
- Arredondamento feito em memória, não no DB
