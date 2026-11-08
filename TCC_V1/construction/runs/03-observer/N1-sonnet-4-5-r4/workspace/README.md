# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Requisitos

- Java 21
- Maven 3.x
- PostgreSQL 18

## Compilação

```bash
mvn package
```

O comando gera um JAR executável em `target/reservas-1.0.0.jar`.

## Execução

Configure as variáveis de ambiente:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=senha
export PORT=7070  # opcional, padrão 7070
```

Execute o sistema:

```bash
java -jar target/reservas-1.0.0.jar
```

## Testes

```bash
mvn verify
```

## API

O sistema expõe os seguintes endpoints:

- `GET /rooms` - Lista os quartos
- `POST /reservations` - Cria uma reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out e retorna comprovante
- `GET /reservations/{id}/document` - Segunda via do comprovante
- `GET /messages` - Lista mensagens enviadas
- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro

## Arquitetura

O sistema foi desenvolvido com foco em extensibilidade, especialmente no sistema de notificações. Utiliza o padrão Observer/Event Publisher para permitir que novos destinatários de notificações sejam adicionados sem modificar o código existente.

### Estrutura

- **Domain**: Modelos de negócio, serviços e eventos
- **Repository**: Persistência em PostgreSQL
- **API**: Controllers REST e DTOs
- **Events**: Sistema extensível de notificações

Para adicionar um novo listener de eventos (ex: restaurante, transfer), basta criar uma classe que implemente `EventPublisher.ReservationConfirmedListener` ou `EventPublisher.ReservationCancelledListener` e registrá-la no EventPublisher.
