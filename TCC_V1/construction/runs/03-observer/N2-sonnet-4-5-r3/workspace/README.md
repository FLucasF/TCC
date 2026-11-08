# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Arquitetura

O sistema foi construído seguindo o padrão **Observer** para as notificações de eventos de reservas. Quando uma reserva muda de estado (confirmada ou cancelada), múltiplos interessados são notificados automaticamente:

- **Hóspede**: recebe e-mail de confirmação com comprovante
- **Governança**: recebe notificação sobre quartos a preparar ou liberados
- **Financeiro**: recebe notificação sobre pagamentos recebidos ou reembolsos

Para adicionar um novo interessado (como restaurante ou transfer), basta:
1. Criar uma nova classe que implementa `ReservationEventListener`
2. Registrá-la na lista de listeners em `Main.java`

Não é necessário modificar o código de confirmação ou cancelamento de reservas.

## Funcionalidades

### Quartos
- 5 quartos com diferentes categorias (STANDARD, DELUXE, SUITE)
- Capacidades e tarifas específicas por quarto
- Tarifa 20% maior na alta temporada (15/dez a fim/fev e todo julho)

### Reservas
- Criação de reservas com validação de disponibilidade
- Dois planos de tarifa:
  - **STANDARD**: permite reembolso total em caso de cancelamento
  - **NON_REFUNDABLE**: 15% de desconto, sem reembolso
- Fluxo de estados: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- Cancelamento possível antes do check-in

### Notificações
- E-mails armazenados no banco de dados (simulação de envio)
- Notificações automáticas via padrão Observer
- Múltiplos destinatários por evento

### Relatórios
- **Ocupação**: mostra reservas pagas para uma data específica
- **Financeiro**: mostra receita de reservas que começam em um mês

## Requisitos

- Java 21+
- PostgreSQL 18+
- Maven 3.9+

## Configuração

O sistema usa variáveis de ambiente para configuração:

```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada
DB_USER=postgres
DB_PASSWORD=senha
PORT=7070
```

Se as variáveis não forem fornecidas, usa os valores padrão acima.

## Build

```bash
mvn clean package
```

Gera o arquivo `target/reservations-1.0.0.jar` pronto para execução.

## Execução

```bash
java -jar target/reservations-1.0.0.jar
```

O servidor inicia na porta configurada (padrão 7070).

## API

### Quartos

**GET /rooms**
```json
[
  {
    "number": 101,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  }
]
```

### Reservas

**POST /reservations**
```json
{
  "roomNumber": 201,
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
  "guestCpf": "11122233344",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

**GET /reservations/{id}**

**POST /reservations/{id}/confirmation** - Confirma reserva

**POST /reservations/{id}/cancellation** - Cancela reserva

**POST /reservations/{id}/check-in** - Faz check-in

**POST /reservations/{id}/check-out** - Faz check-out e retorna comprovante

**GET /reservations/{id}/document** - Obtém 2ª via do comprovante

### Mensagens

**GET /messages**
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "carla@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "..."
  }
]
```

### Relatórios

**GET /reports/occupancy?date=2027-07-10** - CSV de ocupação

**GET /reports/revenue?month=2027-07** - CSV financeiro

## Testes

Testes unitários:
```bash
mvn test
```

Testes de integração (requer Docker):
```bash
mvn verify -DskipITs=false
```

## Estrutura do Código

```
src/main/java/com/pousadamarealta/
├── domain/              # Modelos de domínio
├── repository/          # Acesso a dados (PostgreSQL)
├── service/             # Lógica de negócio
├── notification/        # Listeners do padrão Observer
├── api/                 # Controllers e DTOs
├── util/                # Formatação e utilitários
└── Main.java           # Ponto de entrada e wiring
```

## Decisões de Design

### Padrão Observer
Escolhido para desacoplar a lógica de reservas das notificações. Cada novo departamento que precisa ser notificado entra como um listener independente, sem modificar o código existente.

### Arredondamento
Todos os valores monetários são arredondados para centavos usando HALF_EVEN (bankers' rounding) a cada passo do cálculo.

### Banco de Dados
Esquema criado automaticamente na inicialização. Os 5 quartos são inseridos se o banco estiver vazio.

### Validações
- Datas: check-out deve ser após check-in
- Capacidade: número de hóspedes não pode exceder capacidade do quarto
- Disponibilidade: não permite reservas conflitantes
- Estados: transições de estado validadas

## Erros da API

| Código | Status | Descrição |
|--------|--------|-----------|
| INVALID_REQUEST | 400 | Requisição inválida |
| ROOM_NOT_FOUND | 404 | Quarto não encontrado |
| RESERVATION_NOT_FOUND | 404 | Reserva não encontrada |
| ROOM_UNAVAILABLE | 409 | Quarto indisponível nas datas |
| OPERATION_NOT_ALLOWED | 409 | Operação não permitida no estado atual |
| CAPACITY_EXCEEDED | 422 | Hóspedes excedem capacidade |
