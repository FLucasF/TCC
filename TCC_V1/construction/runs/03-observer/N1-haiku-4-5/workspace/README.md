# Pousada Maré Alta - Sistema de Reservas

Sistema de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Requisitos

- Java 21
- Maven 3.9+
- PostgreSQL 18 (ou Docker)

## Rodando Localmente

### Com Docker Compose

```bash
docker-compose up
```

A aplicação estará disponível em `http://localhost:7070`

### Sem Docker

1. Configure um banco PostgreSQL 18:

```bash
createdb -U postgres pousada
```

2. Configure as variáveis de ambiente:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="sua_senha"
export PORT="7070"
```

3. Compile e rode:

```bash
mvn clean compile exec:java -Dexec.mainClass="com.pousada.Application"
```

Ou gere um JAR executável:

```bash
mvn clean package
java -jar target/app.jar
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Criar reserva
- `GET /reservations/{id}` - Buscar reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `POST /reservations/{id}/check-in` - Check-in
- `POST /reservations/{id}/check-out` - Check-out
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Lista de mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro

## Estrutura do Projeto

```
src/
  main/java/com/pousada/
    api/              # Endpoints HTTP
    database/         # Repositórios e conexão com BD
    event/            # Sistema de eventos
      listener/       # Listeners para notificações
    model/            # Modelos de dados
    service/          # Lógica de negócio
  resources/          # Recursos (se necessário)
```

## Build

```bash
mvn clean package
```

Gera `target/app.jar` - um arquivo JAR executável com todas as dependências.

## Testes

```bash
mvn test
```

## Arquitetura

### Sistema de Notificações

O sistema usa um padrão observer extensível para notificações:

1. Quando uma reserva é confirmada, um evento `ReservationConfirmedEvent` é disparado
2. Múltiplos listeners reagem ao evento:
   - `GuestNotificationListener` - Email para o hóspede
   - `HousekeepingNotificationListener` - Email para governança
   - `FinancialNotificationListener` - Email para financeiro

Para adicionar novos destinatários (restaurante, transfer, etc.), basta criar um novo listener e registrá-lo em `Application.java`.

### Cálculo de Preços

- Diárias são multiplicadas por 1.20 na alta temporada (15/12 a 28/02 e julho)
- Plano NON_REFUNDABLE oferece 15% de desconto
- Arredondamento "meio para o par" em cada etapa

### Validações

- Quarto não pode ter duas reservas no mesmo período
- Número de hóspedes não pode exceder a capacidade
- Operações são permitidas apenas conforme o status da reserva
