# Sistema de Reservas - Pousada Maré Alta

Sistema de reservas para a Pousada Maré Alta, com suporte a múltiplos planos de tarifa.

## Requisitos

- Java 21
- Maven 3.6+

## Compilação

```bash
mvn clean package
```

## Execução

```bash
java -jar target/pousada-mare-alta-1.0.0.jar
```

O sistema sobe na porta configurada na variável de ambiente `PORT`, ou 7070 por padrão.

## Testes

```bash
mvn verify
```

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Obtém uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Realiza check-in
- `POST /reservations/{id}/check-out` - Realiza check-out e retorna comprovante
- `GET /reservations/{id}/document` - Obtém segunda via do comprovante

### Mensagens

- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=yyyy-MM-dd` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=yyyy-MM` - Relatório financeiro (CSV)

## Planos de Tarifa

### STANDARD
Plano normal. Reembolso total em caso de cancelamento.

### NON_REFUNDABLE
15% de desconto sobre as diárias. Sem reembolso.

### CORPORATE
Desconto corporativo por empresa conveniada:
- TECNOSUL: 12%
- AGROVALE: 18%
- FARMAVIDA: 25%

Requer campo `companyCode` na criação. Reembolso total em caso de cancelamento.

### WEEKEND_PACKAGE
Pacote de fim de semana com preço fixo:
- STANDARD: R$ 520,00
- DELUXE: R$ 820,00
- SUITE: R$ 1.200,00

Entrada na sexta, saída no domingo (exatamente 2 noites). Reembolso de 50% em caso de cancelamento.

### LONG_STAY
Estadia longa (mínimo 7 noites). 10% de desconto sobre as diárias. Reembolso total em caso de cancelamento.

## Arquitetura

O sistema utiliza o **Strategy Pattern** para os planos de tarifa, permitindo que novos planos sejam adicionados sem modificar o código existente. Cada plano de tarifa é uma implementação de `RatePlanStrategy` com suas próprias regras de validação, cálculo de preço e reembolso.

### Estrutura de Pacotes

```
com.pousada
├── domain          # Entidades do domínio (Room, Reservation, Message, etc.)
├── rateplan        # Strategies dos planos de tarifa
├── service         # Lógica de negócio
├── controller      # Controladores REST
└── exception       # Exceções de negócio
```
