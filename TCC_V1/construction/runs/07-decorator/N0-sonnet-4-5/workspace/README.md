# Sistema de Reservas - Pousada Maré Alta

Sistema de gestão de reservas para pousadas, com controle de quartos, hóspedes, pagamentos e relatórios.

## Características

- Gestão de 5 quartos (STANDARD, DELUXE, SUITE)
- Cálculo automático de preços com alta temporada (+20%)
- Planos de tarifa: normal e não reembolsável (-15%)
- Controle de status: pendente, confirmado, check-in, check-out, cancelado
- Envio de e-mails de confirmação com sistema de regras flexível
- Relatórios de ocupação e receita em CSV
- Modo de teste para desenvolvimento seguro

## Regras de Negócio

### Quartos
- 101, 102: STANDARD - 2 pessoas - R$ 249,90/noite
- 201, 202: DELUXE - 3 pessoas - R$ 389,90/noite
- 301: SUITE - 4 pessoas - R$ 579,90/noite

### Alta Temporada
- 15 de dezembro até final de fevereiro
- Mês de julho inteiro
- Acréscimo de 20% no valor da diária

### Planos de Tarifa
- **STANDARD**: Reembolso integral em caso de cancelamento
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso

### Sistema de Mensagens

O sistema implementa três regras críticas de e-mail:

1. **Validação de E-mail**: Mensagens para endereços inválidos são bloqueadas
2. **Cópia para Arquivo**: Todo e-mail enviado gera cópia para `arquivo@pousadamarealta.com.br`
3. **Modo de Teste**: Redireciona e-mails para `testes@pousadamarealta.com.br` quando `HOTEL_TEST_MODE=true`

## Requisitos

- Java 21
- PostgreSQL 18
- Maven 3.9+

## Configuração

O sistema usa variáveis de ambiente:

```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada
DB_USER=postgres
DB_PASSWORD=senha
PORT=7070
HOTEL_TEST_MODE=false
```

## Executar

### Desenvolvimento

```bash
# Compilar
mvn clean package

# Executar (requer PostgreSQL rodando)
java -jar target/reservation-system-1.0.0.jar
```

### Produção

```bash
export DB_URL=jdbc:postgresql://seu-servidor:5432/pousada
export DB_USER=usuario
export DB_PASSWORD=senha
export PORT=8080

java -jar target/reservation-system-1.0.0.jar
```

### Modo de Teste

```bash
export HOTEL_TEST_MODE=true
java -jar target/reservation-system-1.0.0.jar
```

## API

### Quartos

```http
GET /rooms
```

Retorna lista de quartos com número, tipo, capacidade e tarifa.

### Reservas

```http
POST /reservations
Content-Type: application/json

{
  "roomNumber": "201",
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
  "guestCpf": "11122233344",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

```http
GET /reservations/{id}
```

```http
POST /reservations/{id}/confirmation
POST /reservations/{id}/cancellation
POST /reservations/{id}/check-in
POST /reservations/{id}/check-out
GET /reservations/{id}/document
```

### Mensagens

```http
GET /messages
```

Lista todas as mensagens enviadas (status: SENT ou BLOCKED).

### Relatórios

```http
GET /reports/occupancy?date=2027-07-10
GET /reports/revenue?month=2027-07
```

Retornam CSV com charset UTF-8.

## Códigos de Erro

- **400 INVALID_REQUEST**: Dados inválidos ou ausentes
- **404 ROOM_NOT_FOUND**: Quarto não encontrado
- **404 RESERVATION_NOT_FOUND**: Reserva não encontrada
- **409 ROOM_UNAVAILABLE**: Quarto já reservado nas datas
- **409 OPERATION_NOT_ALLOWED**: Operação não permitida no status atual
- **422 CAPACITY_EXCEEDED**: Número de hóspedes excede capacidade

## Testes

```bash
# Executar testes (requer Docker para Testcontainers)
mvn verify
```

## Arquitetura

O sistema é construído com:

- **Javalin 6**: Framework web leve
- **PostgreSQL**: Persistência de dados
- **HikariCP**: Pool de conexões
- **Jackson**: Serialização JSON
- **Chain of Responsibility**: Sistema de regras de mensagens

### Estrutura de Pacotes

```
com.pousadamarealta
├── config/          - Configuração do banco de dados
├── controller/      - Endpoints HTTP
│   └── dto/         - Request/Response DTOs
├── messaging/       - Sistema de mensagens e regras
│   └── rules/       - Regras de processamento
├── model/           - Entidades de domínio
├── repository/      - Acesso a dados
├── service/         - Lógica de negócio
└── util/            - Utilitários (cálculo, formatação)
```

## Extensibilidade

O sistema de mensagens é facilmente extensível. Para adicionar novas regras:

1. Implemente a interface `MessageRule`
2. Adicione a regra em `MessagingService`
3. Configure condições (sempre, teste, produção)

Exemplo de regras futuras mencionadas:
- Limite de horário para envio
- Rate limiting por hóspede/dia
- Regras específicas por instalação
