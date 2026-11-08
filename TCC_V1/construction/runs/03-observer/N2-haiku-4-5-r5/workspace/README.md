# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para uma pousada de 5 quartos, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Requisitos

- Java 21
- Maven
- PostgreSQL 18

## Configuração

### Variáveis de Ambiente

- `DB_URL`: URL de conexão PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT`: Porta onde o servidor atenderá (padrão: 7070)

## Compilação e Execução

### Compilar
```bash
mvn clean compile
```

### Empacotar
```bash
mvn clean package
```

### Executar testes
```bash
mvn test
```

### Executar aplicação
```bash
java -jar target/pousada.jar
```

## Arquitetura

O projeto é organizado em camadas:

- **Domain**: Entidades e enums do negócio
- **Infrastructure**: Camada de acesso aos dados (banco de dados)
- **Services**: Lógica de negócio e orquestração
- **API**: Controllers REST

### Sistema de Notificações

O sistema implementa um padrão Observer para notificações de eventos de reserva. Quando uma reserva é confirmada ou cancelada, listeners registrados são notificados automaticamente.

#### Listeners Implementados

- `HostelNotificationListener`: Envia e-mails para hóspede, governança e financeiro

#### Adicionando Novos Listeners

1. Implemente a interface `ReservationEventListener`
2. Registre em `Main.java`:
   ```java
   ReservationEventPublisher.subscribe(new SeuNovoListener());
   ```

## API

### Endpoints Principais

- `GET /rooms` - Lista todos os quartos
- `POST /reservations` - Cria nova reserva
- `GET /reservations/{id}` - Obtém detalhes da reserva
- `POST /reservations/{id}/confirmation` - Confirma pagamento
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `POST /reservations/{id}/check-in` - Registra check-in
- `POST /reservations/{id}/check-out` - Registra check-out
- `GET /reservations/{id}/document` - Obtém comprovante
- `GET /messages` - Lista mensagens enviadas
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro
