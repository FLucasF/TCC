# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Requisitos

- Java 21
- Maven 3.x

## Compilar

```bash
mvn clean package
```

## Executar

```bash
java -jar target/hotel-reservations-1.0-SNAPSHOT.jar
```

O sistema inicia na porta 7070 por padrão. Para usar uma porta diferente:

```bash
PORT=8080 java -jar target/hotel-reservations-1.0-SNAPSHOT.jar
```

## Modo de Teste

Para ativar o modo de teste (redireciona emails para testes@pousadamarealta.com.br):

```bash
HOTEL_TEST_MODE=true java -jar target/hotel-reservations-1.0-SNAPSHOT.jar
```

## Testes

```bash
mvn test
```

## Verificação Completa

```bash
mvn verify
```

## Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Busca uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Obtém o comprovante (segunda via)

### Mensagens

- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - Relatório financeiro (CSV)

## Arquitetura

O sistema utiliza os seguintes padrões de design:

### Chain of Responsibility

Usado para processar mensagens através de uma cadeia de handlers:

1. **EmailValidationHandler** - Valida o formato do email
2. **TestModeHandler** - Redireciona emails em modo de teste
3. **ArchiveCopyHandler** - Cria cópia para o arquivo

Esta abordagem permite adicionar novas regras de processamento (como limites de envio, horários permitidos, etc.) sem modificar o código existente. Cada handler é independente e pode ser adicionado ou removido facilmente.

### Organização do Código

- **domain** - Entidades e lógica de negócio
- **messaging** - Sistema de mensagens com Chain of Responsibility
- **repository** - Repositórios em memória
- **web** - Controllers e DTOs

## Quartos da Pousada

- 101, 102: STANDARD (2 pessoas, R$ 249,90/noite)
- 201, 202: DELUXE (3 pessoas, R$ 389,90/noite)
- 301: SUITE (4 pessoas, R$ 579,90/noite)

## Alta Temporada

20% de acréscimo nas seguintes datas:

- Todo o mês de julho
- 15 de dezembro até o fim de fevereiro

## Planos de Tarifa

- **STANDARD** - Sem desconto, reembolso total em caso de cancelamento
- **NON_REFUNDABLE** - 15% de desconto, sem reembolso em caso de cancelamento
