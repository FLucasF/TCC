# Sistema de Reservas - Pousada Maré Alta

Sistema de reservas para a Pousada Maré Alta, com notificações automáticas para diferentes departamentos.

## Requisitos

- Java 21
- PostgreSQL 18
- Maven 3.x

## Configuração

O sistema usa variáveis de ambiente para configuração:

- `DB_URL`: URL JDBC do PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT`: Porta do servidor (padrão: 7070)

## Compilação

```bash
mvn clean package
```

Isso gera um JAR executável em `target/reservas-1.0.0.jar`.

## Execução

### Com Docker Compose (banco de dados local)

1. Inicie o PostgreSQL:
```bash
docker-compose up -d
```

2. Execute a aplicação:
```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=postgres \
DB_PASSWORD=postgres \
java -jar target/reservas-1.0.0.jar
```

### Execução direta

```bash
java -jar target/reservas-1.0.0.jar
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos disponíveis

### Reservas
- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva (envia notificações)
- `POST /reservations/{id}/cancellation` - Cancela uma reserva (envia notificações se necessário)
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - Relatório financeiro (CSV)

## Sistema de Notificações

O sistema utiliza uma arquitetura baseada em eventos para enviar notificações automáticas:

### Quando uma reserva é confirmada:
1. **Hóspede** recebe comprovante por e-mail
2. **Governança** recebe notificação com datas e número de hóspedes
3. **Financeiro** recebe notificação com valor recebido

### Quando uma reserva confirmada é cancelada:
1. **Governança** recebe notificação com datas liberadas
2. **Financeiro** recebe notificação de reembolso (se aplicável)

### Adicionando novos departamentos

Para adicionar novos departamentos que devem ser notificados, basta criar um novo listener em `event/listeners/` e registrá-lo no `Main.java`. Não é necessário modificar a lógica de negócio.

Exemplo:
```java
// Criar RestaurantNotificationListener.java
eventPublisher.subscribe(ReservationConfirmedEvent.class, 
    new RestaurantNotificationListener(notificationService));
```

## Testes

```bash
mvn test        # Testes unitários
mvn verify      # Testes de integração
```

## Regras de Negócio

### Quartos
- 101, 102: STANDARD (2 pessoas) - R$ 249,90/noite
- 201, 202: DELUXE (3 pessoas) - R$ 389,90/noite
- 301: SUITE (4 pessoas) - R$ 579,90/noite

### Alta Temporada (+20%)
- 15 de dezembro a 28/29 de fevereiro
- Julho inteiro

### Planos de Tarifa
- STANDARD: preço normal, reembolsável
- NON_REFUNDABLE: 15% de desconto, não reembolsável

### Fluxo de Estados
```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
    ↓
CANCELLED (só antes do check-in)
```
