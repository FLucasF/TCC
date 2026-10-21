# Sistema de Reservas - Pousada Maré Alta

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin.

## Compilação

```bash
mvn clean package
```

Gera um JAR executável em `target/reservas-1.0-SNAPSHOT.jar`.

## Execução

```bash
java -jar target/reservas-1.0-SNAPSHOT.jar
```

O servidor inicia na porta 7070 por padrão. Para alterar:

```bash
PORT=8080 java -jar target/reservas-1.0-SNAPSHOT.jar
```

## Modo de Teste

Em modo de teste, todos os emails são redirecionados para `testes@pousadamarealta.com.br`:

```bash
HOTEL_TEST_MODE=true java -jar target/reservas-1.0-SNAPSHOT.jar
```

## API REST

### Quartos

```bash
GET /rooms
```

Retorna lista de quartos disponíveis com capacidade e diária.

### Reservas

```bash
# Criar reserva
POST /reservations
Content-Type: application/json
{
  "roomNumber": 101,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD"  // ou NON_REFUNDABLE
}

# Obter detalhes
GET /reservations/{id}

# Confirmar pagamento
POST /reservations/{id}/confirmation
Content-Type: application/json
{
  "amountPaid": 599.76
}

# Check-in
POST /reservations/{id}/check-in

# Check-out
POST /reservations/{id}/check-out

# Cancelar
POST /reservations/{id}/cancellation

# Segunda via do comprovante
GET /reservations/{id}/document
```

### Mensagens

```bash
GET /messages
```

Lista todas as mensagens enviadas (SENT ou BLOCKED).

### Relatórios

```bash
# Ocupação por dia
GET /reports/occupancy?date=2027-07-10

# Receita por mês
GET /reports/revenue?month=2027-07
```

## Características

### Preços
- STANDARD: R$ 249,90 (2 pessoas), R$ 389,90 (3 pessoas), R$ 579,90 (4 pessoas)
- Alta temporada (+20%): 15-28 fevereiro, 1-31 julho, 15-31 dezembro
- Plano NON_REFUNDABLE: 15% desconto

### Validação de Emails
- Formato obrigatório: `usuario@dominio.com`
- Um único `@`, algo antes e depois com ponto
- Emails inválidos ficam bloqueados (BLOCKED)

### Modo de Teste
- Redireciona emails para testes@pousadamarealta.com.br
- Adiciona "[TESTE] " no assunto
- Inclui "Destinatário original: " no corpo

### Sistema de Mensagens
- Cópia automática para arquivo@pousadamarealta.com.br (modo normal)
- Validação rigorosa de formato de email
- Status SENT ou BLOCKED

## Testes

```bash
mvn verify
```

Executa todos os testes unitários.

## Estrutura

```
src/
├── main/java/com/pousadamarealta/
│   ├── Application.java              # Entrada principal
│   ├── api/ApiRoutes.java            # Rotas REST
│   ├── domain/                        # Modelos de domínio
│   │   ├── Room, Reservation, Message
│   │   └── Enums: RatePlan, ReservationStatus, MessageStatus
│   ├── service/                       # Lógica de negócio
│   │   ├── RoomService
│   │   ├── ReservationService
│   │   ├── MessageService
│   │   └── ReportService
│   └── util/                          # Utilitários
│       ├── PriceCalculator
│       ├── DateUtil, EmailValidator
│       └── MoneyUtil
└── test/java/                         # Testes unitários
```

## Notas

- Dados são armazenados apenas em memória (sem persistência)
- Implementação segue as orientações do CLAUDE.md
- Sistema pronto para expansão com novos recursos
