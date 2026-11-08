# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta, um pequeno hotel com 5 quartos no litoral.

## Requisitos

- Java 21
- Maven 3.8+
- PostgreSQL 18

## Configuração

### Variáveis de Ambiente

O sistema requer as seguintes variáveis de ambiente:

- `DB_URL`: URL de conexão do PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do PostgreSQL
- `DB_PASSWORD`: Senha do PostgreSQL
- `PORT`: Porta de execução (padrão: 7070)

### Exemplo de Inicialização

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="password"
export PORT=7070

java -jar target/mare-alta-1.0.0.jar
```

## Build

```bash
mvn clean package
```

O arquivo JAR será gerado em `target/mare-alta-1.0.0.jar`.

## API

### Quartos

#### GET /rooms
Retorna a lista de todos os quartos.

**Resposta:**
```json
[
  {
    "number": 101,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  },
  ...
]
```

### Reservas

#### POST /reservations
Cria uma nova reserva.

**Body:**
```json
{
  "roomNumber": 101,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

**Resposta (201):**
```json
{
  "id": 1,
  "roomNumber": 101,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 499.80,
  "discount": 0.00,
  "total": 499.80,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

#### GET /reservations/{id}
Obtém os detalhes de uma reserva.

#### POST /reservations/{id}/confirmation
Confirma uma reserva pendente (realiza o pagamento).

**Body:**
```json
{
  "amountPaid": 499.80
}
```

#### POST /reservations/{id}/check-in
Registra o check-in de um hóspede.

#### POST /reservations/{id}/check-out
Registra o check-out de um hóspede e retorna o comprovante.

#### POST /reservations/{id}/cancellation
Cancela uma reserva (antes do check-in).

#### GET /reservations/{id}/document
Obtém uma segunda via do comprovante de uma reserva confirmada.

### Mensagens

#### GET /messages
Retorna a lista de mensagens (emails) enviadas.

**Resposta:**
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "joao@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "..."
  }
]
```

### Relatórios

#### GET /reports/occupancy?date=2027-07-10
Retorna relatório de ocupação para um dia específico.

**Formato CSV:**
```
Pousada Maré Alta,occupancy,2027-07-10
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
total,1/5
```

#### GET /reports/revenue?month=2027-07
Retorna relatório financeiro para um mês.

**Formato CSV:**
```
Pousada Maré Alta,revenue,2027-07
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CHECKED_OUT,499.80
total,,,,,499.80
```

#### GET /reports/tourism-tax?month=2027-07
Retorna relatório da taxa de turismo para um mês (R$ 4,50 por hóspede por noite).

#### GET /reports/rate-plans?month=2027-07
Retorna análise de receita por plano de tarifa.

## Estrutura do Projeto

```
├── src/
│   ├── main/
│   │   ├── java/com/pousada/
│   │   │   ├── Main.java - Ponto de entrada da aplicação
│   │   │   ├── api/ - Controllers da API REST
│   │   │   ├── config/ - Configuração do banco de dados
│   │   │   ├── model/ - Entidades (Room, Reservation, etc)
│   │   │   ├── repository/ - Acesso aos dados
│   │   │   ├── service/ - Lógica de negócio
│   │   │   ├── exception/ - Exceções customizadas
│   │   │   └── util/ - Utilitários (preços, datas)
│   │   └── resources/ - Recursos da aplicação
│   └── test/
│       └── java/com/pousada/ - Testes unitários
├── pom.xml - Configuração Maven
└── README.md - Este arquivo
```

## Regras de Negócio

### Quartos
- **101 e 102 (STANDARD)**: 2 pessoas máximo, R$ 249,90/noite
- **201 e 202 (DELUXE)**: 3 pessoas máximo, R$ 389,90/noite
- **301 (SUITE)**: 4 pessoas máximo, R$ 579,90/noite

### Alta Temporada
- 15 de dezembro a 28/29 de fevereiro
- Julho inteiro
- Acréscimo de 20% na diária

### Planos de Tarifa
- **STANDARD**: Sem desconto, reembolsável
- **NON_REFUNDABLE**: 15% de desconto, não reembolsável

### Estados da Reserva
1. **PENDING**: Reserva criada, aguardando pagamento
2. **CONFIRMED**: Pagamento realizado
3. **CHECKED_IN**: Hóspede fez o check-in
4. **CHECKED_OUT**: Hóspede fez o check-out
5. **CANCELLED**: Reserva cancelada

### Cálculo de Preço
- Preço por noite = tarifa do quarto × (1,20 se alta temporada)
- Subtotal = soma dos preços das noites
- Desconto = subtotal × taxa do plano (se aplicável)
- Total = subtotal - desconto
- Arredondamento: "meio para o par" em cada passo

## Testes

```bash
mvn test
```

## Notas Importantes

- O banco de dados é criado automaticamente na primeira execução
- Emails são armazenados em um banco de dados, não são enviados de verdade
- Cada reserva tem um ID único que serve como número do comprovante
- Relatórios são retornados em CSV com charset UTF-8
