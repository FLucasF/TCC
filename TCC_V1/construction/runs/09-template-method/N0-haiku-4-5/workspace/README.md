# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta, construído com Java 21, Javalin e PostgreSQL.

## Compilação

O projeto usa Maven para compilação. Para gerar o JAR executável:

```bash
mvn clean package
```

## Execução

O JAR gerado em `target/mare-alta-1.0.0.jar` é executável e auto-contido:

```bash
java -jar target/mare-alta-1.0.0.jar
```

## Configuração

O sistema requer as seguintes variáveis de ambiente:

- `DB_URL`: URL JDBC do PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT`: Porta para o servidor (padrão: 7070)

Exemplo:
```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="senha123"
export PORT="7070"

java -jar target/mare-alta-1.0.0.jar
```

## Funcionalidades

- **Gerenciamento de Reservas**: Criar, confirmar, cancelar, fazer check-in e check-out
- **Precificação Dinâmica**: Cálculo automático de preços com base na sazonalidade
- **Planos de Tarifa**: STANDARD e NON_REFUNDABLE com desconto de 15%
- **Relatórios em CSV**: Ocupação, receita, taxa de turismo e análise de planos tarifários
- **E-mail de Confirmação**: Envio de comprovantes de reserva (armazenados localmente)
- **Persistência em Banco de Dados**: PostgreSQL 18

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Criar nova reserva
- `GET /reservations/{id}` - Obter dados da reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva (com pagamento)
- `POST /reservations/{id}/check-in` - Realizar check-in
- `POST /reservations/{id}/check-out` - Realizar check-out
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `GET /reservations/{id}/document` - Obter segunda via do comprovante

### Mensagens
- `GET /messages` - Listar e-mails enviados

### Relatórios (CSV)
- `GET /reports/occupancy?date=2027-07-10` - Ocupação por data
- `GET /reports/revenue?month=2027-07` - Receita mensal
- `GET /reports/tourism-tax?month=2027-07` - Taxa de turismo
- `GET /reports/rate-plans?month=2027-07` - Análise de planos tarifários

## Testes

Executar testes com:

```bash
mvn test
```

## Estrutura do Projeto

```
src/
├── main/java/com/pousada/
│   ├── Application.java           - Entrada da aplicação
│   ├── Database.java              - Gerenciamento de conexão e schema
│   ├── RouteConfig.java           - Configuração de rotas HTTP
│   ├── model/
│   │   ├── Room.java
│   │   ├── Reservation.java
│   │   ├── ReservationStatus.java
│   │   ├── RatePlan.java
│   │   └── Message.java
│   ├── repository/
│   │   ├── RoomRepository.java
│   │   ├── ReservationRepository.java
│   │   └── MessageRepository.java
│   └── service/
│       ├── ReservationService.java
│       ├── PricingCalculator.java
│       ├── DocumentService.java
│       ├── EmailService.java
│       └── ReportService.java
└── test/java/com/pousada/
    └── PricingCalculatorTest.java
```

## Regras de Negócio

### Sazonalidade
- **Alta Temporada**: 15 de dezembro até 28/29 de fevereiro + todo o mês de julho
- **Multiplicador**: 20% de aumento no valor da diária

### Planos de Tarifa
- **STANDARD**: Sem desconto, reembolsável completamente em caso de cancelamento
- **NON_REFUNDABLE**: 15% de desconto, não reembolsável

### Quartos
- 101, 102 (STANDARD): 2 pessoas, R$ 249,90
- 201, 202 (DELUXE): 3 pessoas, R$ 389,90
- 301 (SUITE): 4 pessoas, R$ 579,90

### Estados da Reserva
1. PENDING - Reserva criada, aguardando confirmação
2. CONFIRMED - Reserva confirmada e paga
3. CHECKED_IN - Hóspede realizou check-in
4. CHECKED_OUT - Hóspede realizou check-out
5. CANCELLED - Reserva cancelada

### Arredondamento
Todos os valores monetários usam arredondamento "meio para o par" (HALF_EVEN) em cada operação.
