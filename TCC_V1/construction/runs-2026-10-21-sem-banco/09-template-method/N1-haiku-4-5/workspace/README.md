# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0.

## Recursos

- **Gerenciamento de quartos**: 5 quartos com tipos STANDARD, DELUXE e SUITE
- **Reservas**: Criação, confirmação, check-in, check-out e cancelamento
- **Preços dinâmicos**: Alta temporada (15 dez - 28 fev e julho) com +20% na diária
- **Planos de tarifa**: STANDARD (normal) e NON_REFUNDABLE (15% desconto)
- **Emails**: Confirmação de reserva enviada ao hóspede
- **Relatórios em CSV**:
  - Ocupação por noite
  - Receita por mês
  - Taxa de turismo (R$ 4,50/hóspede/noite)
  - Análise de planos de tarifa

## Como compilar

```bash
mvn clean package
```

O arquivo `target/mare-alta.jar` será gerado com todas as dependências incluídas.

## Como executar

```bash
# Porta padrão: 7070
java -jar target/mare-alta.jar

# Ou especificar uma porta via variável de ambiente
PORT=8080 java -jar target/mare-alta.jar
```

O servidor iniciará e estará disponível em `http://localhost:7070`.

## API REST

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma reserva
- `GET /reservations/{id}` - Obtém detalhes da reserva
- `POST /reservations/{id}/confirmation` - Confirma a reserva
- `POST /reservations/{id}/cancellation` - Cancela a reserva
- `POST /reservations/{id}/check-in` - Realiza o check-in
- `POST /reservations/{id}/check-out` - Realiza o check-out
- `GET /reservations/{id}/document` - Obtém segunda via do comprovante

### Mensagens
- `GET /messages` - Lista todos os e-mails enviados

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Ocupação em uma data (CSV)
- `GET /reports/revenue?month=2027-07` - Receita de um mês (CSV)
- `GET /reports/tourism-tax?month=2027-07` - Taxa de turismo de um mês (CSV)
- `GET /reports/rate-plans?month=2027-07` - Planos de tarifa de um mês (CSV)

## Quartos da Pousada

| Número | Tipo      | Capacidade | Diária (Baixa) | Diária (Alta) |
|--------|-----------|------------|----------------|---------------|
| 101    | STANDARD  | 2          | R$ 249,90      | R$ 299,88     |
| 102    | STANDARD  | 2          | R$ 249,90      | R$ 299,88     |
| 201    | DELUXE    | 3          | R$ 389,90      | R$ 467,88     |
| 202    | DELUXE    | 3          | R$ 389,90      | R$ 467,88     |
| 301    | SUITE     | 4          | R$ 579,90      | R$ 695,88     |

## Testes

```bash
mvn verify
```

Todos os testes unitários passarão, validando:
- Cálculo de preços e arredondamento
- Criação e gerenciamento de reservas
- Geração de relatórios
- Validações de erros

## Dados em memória

O sistema armazena todos os dados (reservas, mensagens) em memória. Ao reiniciar a aplicação, todos os dados serão perdidos.

## Estrutura do projeto

```
src/
├── main/java/com/pousada/
│   ├── Application.java           # Ponto de entrada
│   ├── api/
│   │   └── ApiController.java     # Endpoints REST
│   ├── domain/                    # Classes de domínio
│   │   ├── Message.java
│   │   ├── Reservation.java
│   │   ├── RatePlan.java
│   │   ├── ReservationStatus.java
│   │   ├── Room.java
│   │   └── RoomType.java
│   ├── service/                   # Lógica de negócio
│   │   ├── MessageRepository.java
│   │   ├── ReportService.java
│   │   ├── ReservationRepository.java
│   │   ├── ReservationService.java
│   │   └── RoomRepository.java
│   └── util/
│       └── PriceCalculator.java   # Cálculo de preços
└── test/java/com/pousada/
    └── (testes unitários)
```
