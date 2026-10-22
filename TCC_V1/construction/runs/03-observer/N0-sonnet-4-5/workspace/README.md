# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Arquitetura

O sistema foi projetado com uma arquitetura orientada a eventos para facilitar a extensibilidade do sistema de notificações:

- **Event-Driven Architecture**: Quando eventos importantes acontecem (confirmação, cancelamento), são publicados através de um `EventPublisher`
- **Observer Pattern**: Listeners específicos (governança, financeiro, hóspede) se inscrevem nos eventos de interesse
- **Facilmente extensível**: Novos departamentos (restaurante, transfer) podem ser adicionados simplesmente criando novos listeners e registrando-os no `Main.java`

### Componentes principais

- **Domain**: Entidades de negócio (Room, Reservation, Message)
- **Repository**: Acesso a dados com PostgreSQL
- **Service**: Lógica de negócio (precificação, reservas, relatórios)
- **Events**: Sistema de eventos e listeners para notificações
- **API**: Controllers REST com Javalin

## Requisitos

- Java 21
- Maven 3.6+
- PostgreSQL 18

## Configuração

O sistema lê as seguintes variáveis de ambiente:

- `DB_URL`: JDBC URL do PostgreSQL (padrão: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco (padrão: `postgres`)
- `DB_PASSWORD`: Senha do banco (padrão: `postgres`)
- `PORT`: Porta HTTP (padrão: `7070`)

## Como executar

### 1. Iniciar o banco de dados

```bash
docker run -d \
  --name pousada-db \
  -e POSTGRES_DB=pousada \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:18
```

### 2. Compilar o projeto

```bash
mvn clean package
```

### 3. Executar o sistema

```bash
java -jar target/reservations-1.0.0.jar
```

O sistema estará disponível em `http://localhost:7070`.

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
- `GET /reservations/{id}/document` - Obtém comprovante (segunda via)

### Mensagens
- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro

## Sistema de Notificações

O sistema notifica automaticamente os departamentos relevantes quando eventos importantes acontecem:

### Confirmação de reserva
- **Hóspede**: Recebe email de confirmação com comprovante
- **Governança**: Recebe email com datas e número de hóspedes
- **Financeiro**: Recebe email com valor recebido

### Cancelamento de reserva confirmada
- **Governança**: Recebe email com datas liberadas
- **Financeiro**: Recebe email com valor de reembolso (se houver)

### Adicionando novos listeners

Para adicionar um novo departamento às notificações, basta:

1. Criar uma classe que implementa `EventListener<ReservationConfirmedEvent>` ou `EventListener<ReservationCancelledEvent>`
2. Registrar o listener em `Main.java`:

```java
eventPublisher.subscribe(ReservationConfirmedEvent.class, 
    new RestaurantNotificationListener(messageRepository));
```

## Testes

```bash
mvn test
mvn verify
```

## Quartos cadastrados

- **101, 102**: STANDARD (2 pessoas) - R$ 249,90/noite
- **201, 202**: DELUXE (3 pessoas) - R$ 389,90/noite  
- **301**: SUITE (4 pessoas) - R$ 579,90/noite

## Alta Temporada

Período de alta temporada (20% adicional na diária):
- 15 de dezembro a 28/29 de fevereiro
- Julho inteiro

## Planos de Tarifa

- **STANDARD**: Cancelamento com reembolso total
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso
