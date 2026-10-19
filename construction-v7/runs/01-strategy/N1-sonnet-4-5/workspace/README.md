# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido com Java 21 e Javalin 6.7.0.

## Requisitos

- Java 21
- Maven 3.6+

## Compilação

```bash
mvn clean package
```

## Execução

```bash
java -jar target/reservas-1.0-SNAPSHOT.jar
```

Por padrão, o sistema atende na porta 7070. Para alterar a porta:

```bash
PORT=8080 java -jar target/reservas-1.0-SNAPSHOT.jar
```

## Testes

```bash
mvn test
```

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Obtém segunda via do comprovante

### Mensagens

- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro

## Planos de Tarifa

O sistema suporta os seguintes planos:

- **STANDARD**: Plano normal sem desconto
- **NON_REFUNDABLE**: 15% de desconto, sem devolução no cancelamento
- **CORPORATE**: Convênio com empresas (Tecnosul 12%, Agrovale 18%, Farmavida 25%)
- **WEEKEND_PACKAGE**: Pacote de fim de semana (sexta a domingo)
- **LONG_STAY**: Estadia longa (7+ noites com 10% de desconto)

## Estrutura do Projeto

- `model/`: Classes de domínio (Room, Reservation, Message, etc.)
- `service/`: Lógica de negócio
- `controller/`: Controllers da API REST
- `rateplan/`: Implementação dos planos de tarifa (Strategy pattern)
- `dto/`: Data Transfer Objects para a API
- `util/`: Utilitários (formatação de datas, dinheiro, CPF)

## Arquitetura

O sistema foi projetado seguindo o princípio de extensibilidade. Novos planos de tarifa podem ser adicionados facilmente:

1. Criar uma nova classe implementando a interface `RatePlan`
2. Adicionar o enum correspondente em `RatePlanType`
3. Registrar o plano em `RatePlanFactory`

Isso permite que novos planos sejam adicionados sem modificar código existente.
