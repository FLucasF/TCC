# Sistema de Reservas - Pousada Maré Alta

Sistema completo de gestão de reservas para a Pousada Maré Alta, construído em Java 21 com Javalin 6.7.0.

## Recursos

### 1. Gerenciamento de Quartos
- 5 quartos: 101, 102 (STANDARD), 201, 202 (DELUXE), 301 (SUITE)
- Capacidades: 2, 3 e 4 hóspedes respectivamente
- Tarifas noturnas: R$ 249,90, R$ 389,90, R$ 579,90
- Sazonalidade: +20% de 15/12 a 29/02 e julho inteiro

### 2. Reservas
- Estados: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- Cancelamento: antes do check-in (reembolso total ou zero conforme plano)
- Planos de tarifa:
  - **STANDARD**: sem desconto, reembolso total no cancelamento
  - **NON_REFUNDABLE**: 15% de desconto, sem reembolso no cancelamento
- Arredondamento "meio para o par" em cada passo (noite, soma, desconto, total)

### 3. Comunicação por Email
- Validação de endereço: deve ter um @, algo antes, e domínio com ponto
- Mensagens bloqueadas aparecem na lista de mensagens como BLOCKED
- **Modo de teste** (HOTEL_TEST_MODE=true):
  - Todos os emails vão para `testes@pousadamarealta.com.br`
  - Assunto prefixado com `[TESTE]`
  - Primeira linha: "Destinatário original: " + email original
  - Sem cópia para arquivo em modo teste
- **Cópia para arquivo**: toda mensagem gera cópia para `arquivo@pousadamarealta.com.br`
  - Assunto prefixado com `[Cópia]`
  - Não criada se email bloqueado ou em modo teste

### 4. Relatórios
- **Ocupação** (`GET /reports/occupancy?date=YYYY-MM-DD`): quais quartos estão ocupados em uma noite
- **Receita** (`GET /reports/revenue?month=YYYY-MM`): reservas que começam em um mês
- Formato: CSV com charset utf-8

## Construção

```bash
# Compilar e testar
mvn verify

# Criar JAR executável
mvn package
```

## Execução

```bash
# Porta padrão (7070)
java -jar target/hotel-system-1.0.0.jar

# Porta customizada
PORT=8080 java -jar target/hotel-system-1.0.0.jar

# Modo de teste
HOTEL_TEST_MODE=true java -jar target/hotel-system-1.0.0.jar
```

## API

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Criar reserva
- `GET /reservations/{id}` - Obter reserva
- `POST /reservations/{id}/confirmation` - Confirmar (pagar)
- `POST /reservations/{id}/check-in` - Check-in
- `POST /reservations/{id}/check-out` - Check-out
- `POST /reservations/{id}/cancellation` - Cancelar
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Listar mensagens enviadas (com status SENT/BLOCKED)

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Ocupação de uma noite
- `GET /reports/revenue?month=2027-07` - Receita de um mês

## Exemplos

### Criar Reserva
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Confirmar Reserva
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 935.76}'
```

## Arquitetura

### Design Patterns
- **Strategy**: Planos de tarifa (STANDARD, NON_REFUNDABLE) e relatórios (ocupação, receita)
- **Chain of Responsibility**: Regras de email (validação, modo teste, cópia)

### Estrutura
- Modelos em `model/`
- Repositórios em memória em `repository/`
- Serviços em `service/`
- Handlers Javalin em `handler/`
- Utilitários em `util/`
- Estratégias em `tariff/` e `service/`
- Regras em `email/`

## Tecnologia
- Java 21
- Javalin 6.7.0
- Jackson (JSON)
- JUnit 5 (testes)
- Maven

## Dados
Todas as reservas e mensagens são armazenadas em memória. O sistema reinicia com estado limpo a cada inicialização.
