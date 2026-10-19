# Sistema de Reservas - Pousada Maré Alta

Um sistema completo de gerenciamento de reservas para uma pequena pousada no litoral, desenvolvido em Java 21 com Javalin 6.7.0.

## Compilação e Deploy

### Compilar e executar testes

```bash
mvn clean verify
```

### Gerar JAR executável

```bash
mvn clean package
```

### Executar o servidor

```bash
java -jar target/mare-alta.jar
```

Porta padrão: 7070. Para usar outra porta, configure a variável de ambiente PORT:

```bash
PORT=8080 java -jar target/mare-alta.jar
```

### Modo de teste

Para redirecionar e-mails para um endereço de teste:

```bash
HOTEL_TEST_MODE=true java -jar target/mare-alta.jar
```

## Recursos Implementados

### Quartos
- 5 quartos: 101, 102 (STANDARD 2 pessoas), 201, 202 (DELUXE 3 pessoas), 301 (SUITE 4 pessoas)
- Diárias: R$ 249,90, R$ 389,90, R$ 579,90 respectivamente
- Alta temporada: 15/12 a 29/02 e todo julho (+20% na diária)

### Reservas
- Estados: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT (ou CANCELLED)
- Planos de tarifa: STANDARD e NON_REFUNDABLE (15% desconto)
- Arredondamento: "meio para o par" em cada passo

### Validações
- E-mail inválido: bloqueado na mensagem de confirmação
- Quarto indisponível: não permite double booking
- Capacidade do quarto: não ultrapassa limite de hóspedes

### E-mails
- Confirmação automaticamente enviada ao hóspede
- Cópia para arquivo@pousadamarealta.com.br (em produção)
- Modo de teste: redirecionamento para testes@pousadamarealta.com.br
- E-mails bloqueados aparecem na lista com status BLOCKED

### Relatórios
- Ocupação: quartos ocupados em uma noite específica (CSV)
- Receita: reservas pagas em um mês específico (CSV)

## API

### GET /rooms
Retorna lista de quartos disponíveis.

### POST /reservations
Cria uma nova reserva. Corpo esperado:
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

### GET /reservations/{id}
Retorna dados da reserva.

### POST /reservations/{id}/confirmation
Confirma reserva. Corpo esperado:
```json
{"amountPaid": 779.80}
```

### POST /reservations/{id}/cancellation
Cancela reserva.

### POST /reservations/{id}/check-in
Realiza check-in.

### POST /reservations/{id}/check-out
Realiza check-out e retorna comprovante.

### GET /reservations/{id}/document
Retorna comprovante (segunda via).

### GET /messages
Retorna lista de mensagens enviadas (incluindo bloqueadas).

### GET /reports/occupancy?date=2027-07-10
Retorna relatório de ocupação em CSV.

### GET /reports/revenue?month=2027-07
Retorna relatório de receita em CSV.

## Exemplos de Uso

### Criar uma reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

### Confirmar reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 509.80}'
```

### Gerar relatório de ocupação

```bash
curl -s http://localhost:7070/reports/occupancy?date=2027-07-10
```

### Gerar relatório de receita

```bash
curl -s http://localhost:7070/reports/revenue?month=2027-07
```

## Estrutura do Projeto

```
src/main/java/com/pousada/marealta/
├── App.java                 # Aplicação principal com rotas
├── model/                   # Modelos de domínio
│   ├── Room
│   ├── Reservation
│   ├── Message
│   ├── RoomType
│   ├── ReservationStatus
│   ├── RatePlan
│   └── ReservationDocument
├── service/                 # Lógica de negócio
│   ├── RoomManager
│   ├── ReservationService
│   ├── MessageService
│   ├── PricingService
│   └── ReportService
├── api/                     # API REST
│   ├── converter/           # Conversores de modelos
│   └── dto/                 # Data Transfer Objects
└── util/                    # Utilitários
    ├── MoneyUtil
    ├── EmailValidator
    ├── DateUtil
    └── DocumentFormatter
```

## Dados em Memória

O sistema armazena todas as reservas e mensagens em memória. Ao reiniciar a aplicação, todos os dados são perdidos. Para persistência, seria necessário adicionar um banco de dados.
