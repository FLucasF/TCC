# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para pousada, desenvolvido com Java 21 e Javalin.

## Arquitetura

O sistema foi construído seguindo princípios de design orientado a eventos:

- **Event-Driven Architecture**: Quando eventos importantes acontecem (confirmação, cancelamento), o sistema publica eventos que são consumidos por listeners independentes
- **Observer Pattern**: Novos interessados em notificações podem ser adicionados sem modificar o código existente
- **Repository Pattern**: Abstração da camada de persistência
- **Service Layer**: Lógica de negócio isolada dos controllers

### Por que Event-Driven?

O requisito deixou claro que o sistema de notificações vai crescer:
> "Vai ter cada vez mais gente querendo ser avisada, e de mais coisas"
> "Quem é avisado vai mudar com o tempo"

Com o padrão de eventos implementado, adicionar um novo destinatário (restaurante, transfer, etc.) é simplesmente criar uma nova classe `EventListener` e registrá-la no `EventPublisher`. Não é necessário modificar o código de confirmação ou cancelamento de reservas.

## Funcionalidades

- **Gestão de Quartos**: 5 quartos em 3 categorias (STANDARD, DELUXE, SUITE)
- **Reservas**: Criação, confirmação, cancelamento, check-in e check-out
- **Cálculo de Preços**: 
  - Alta temporada (15/dez-fev, julho inteiro): +20%
  - Plano não reembolsável: -15%
  - Arredondamento bancário (half-even)
- **Notificações por E-mail**:
  - Hóspede: comprovante de confirmação
  - Governança: avisos de confirmação e cancelamento
  - Financeiro: pagamentos e reembolsos
- **Relatórios CSV**:
  - Ocupação por data
  - Receita por mês

## Tecnologias

- Java 21
- Javalin 6.7.0 (web framework)
- PostgreSQL 18 (banco de dados)
- HikariCP (connection pool)
- Jackson (JSON)
- JUnit 5 + Testcontainers (testes)

## Requisitos

- Java 21+
- PostgreSQL 18+ (ou Docker para rodar via docker-compose)
- Maven 3.9+

## Desenvolvimento Local

### 1. Iniciar o banco de dados

```bash
docker-compose up -d
```

### 2. Compilar o projeto

```bash
mvn clean package
```

### 3. Executar a aplicação

```bash
java -jar target/reservas-1.0.0.jar
```

Ou com variáveis de ambiente customizadas:

```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=postgres \
DB_PASSWORD=postgres \
PORT=7070 \
java -jar target/reservas-1.0.0.jar
```

### 4. Testar os endpoints

```bash
# Listar quartos
curl http://localhost:7070/rooms

# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Confirmar reserva
curl -X POST http://localhost:7070/reservations/1/confirmation

# Ver mensagens enviadas
curl http://localhost:7070/messages
```

## Testes

### Testes Unitários

```bash
mvn test
```

### Testes de Integração

Os testes de integração usam Testcontainers e requerem Docker:

```bash
mvn verify
```

## API Reference

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria nova reserva (retorna 201)
- `GET /reservations/{id}` - Consulta reserva
- `POST /reservations/{id}/confirmation` - Confirma reserva
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out e retorna comprovante
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens

- `GET /messages` - Lista mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - Relatório financeiro (CSV)

## Códigos de Erro

| Código | HTTP | Descrição |
|--------|------|-----------|
| `INVALID_REQUEST` | 400 | Dados inválidos na requisição |
| `ROOM_NOT_FOUND` | 404 | Quarto não existe |
| `RESERVATION_NOT_FOUND` | 404 | Reserva não existe |
| `ROOM_UNAVAILABLE` | 409 | Quarto já reservado para as datas |
| `OPERATION_NOT_ALLOWED` | 409 | Operação não permitida no estado atual |
| `CAPACITY_EXCEEDED` | 422 | Mais hóspedes do que o quarto suporta |

## Estrutura do Projeto

```
src/main/java/com/pousadamarealta/
├── domain/
│   ├── model/          # Entidades de domínio
│   ├── enums/          # Enumerações
│   └── events/         # Eventos de domínio
├── service/
│   ├── event/          # Sistema de eventos e listeners
│   └── *Service.java   # Lógica de negócio
├── repository/         # Acesso a dados
├── controller/         # Controllers REST
├── dto/                # Request/Response objects
├── config/             # Configuração (Database)
└── App.java            # Main class
```

## Extensibilidade

### Adicionar um novo destinatário de notificações

1. Criar uma classe que implementa `EventListener`:

```java
public class RestaurantNotificationListener implements EventListener {
    @Override
    public void handle(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent) {
            // Notificar restaurante
        }
    }
}
```

2. Registrar no `App.java`:

```java
eventPublisher.subscribe(new RestaurantNotificationListener(messageRepository));
```

### Adicionar um novo tipo de evento

1. Criar a classe do evento implementando `ReservationEvent`
2. Publicar o evento no serviço apropriado usando `eventPublisher.publish()`
3. Criar ou atualizar listeners conforme necessário

## Licença

Propriedade da Pousada Maré Alta.
