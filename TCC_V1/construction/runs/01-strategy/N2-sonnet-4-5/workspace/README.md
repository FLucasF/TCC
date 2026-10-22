# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta, construído com Java 21, Javalin 6.7.0 e PostgreSQL 18.

## Arquitetura

O sistema utiliza o **Strategy Pattern** para gerenciar os diferentes planos de tarifa, permitindo que novos planos sejam adicionados sem modificar o código existente.

### Planos de Tarifa Implementados

1. **STANDARD** - Plano normal com reembolso total
2. **NON_REFUNDABLE** - 15% de desconto, sem reembolso
3. **CORPORATE** - Convênio com empresas (TECNOSUL, AGROVALE, FARMAVIDA)
4. **WEEKEND_PACKAGE** - Pacote de fim de semana (sexta a domingo)
5. **LONG_STAY** - Estadia longa (7+ noites) com 10% de desconto

Para adicionar um novo plano de tarifa, basta:
1. Criar uma nova classe implementando `RatePlan`
2. Adicionar o enum em `RatePlanType`
3. Registrar no `RatePlanFactory`

## Requisitos

- Java 21
- Maven 3.6+
- PostgreSQL 18

## Configuração

O sistema utiliza variáveis de ambiente:

```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada
DB_USER=postgres
DB_PASSWORD=postgres
PORT=7070
```

## Build e Execução

```bash
# Compilar e testar
mvn clean verify

# Gerar JAR executável
mvn package

# Executar
java -jar target/reservations-1.0.0.jar
```

## API REST

### Quartos

- `GET /rooms` - Listar todos os quartos

### Reservas

- `POST /reservations` - Criar nova reserva
- `GET /reservations/{id}` - Consultar reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva
- `POST /reservations/{id}/check-in` - Fazer check-in
- `POST /reservations/{id}/check-out` - Fazer check-out
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens

- `GET /messages` - Listar mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - Relatório financeiro (CSV)

## Exemplo de Criação de Reserva

```json
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

Para plano CORPORATE, adicionar:
```json
{
  "companyCode": "TECNOSUL"
}
```

## Estrutura do Projeto

```
src/main/java/com/pousadamarealta/
├── domain/
│   ├── rateplan/          # Strategy Pattern para planos de tarifa
│   ├── Reservation.java
│   ├── Room.java
│   └── Message.java
├── repository/            # Acesso a dados (PostgreSQL)
├── service/              # Lógica de negócio
├── api/                  # Controladores REST
│   └── dto/             # DTOs para API
├── util/                 # Utilitários
└── infrastructure/       # Configuração de banco de dados
```

## Testes

Testes unitários para:
- Cálculo de preços e alta temporada
- Todos os planos de tarifa
- Regras de negócio de reservas

```bash
mvn test
```
