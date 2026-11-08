# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Características Principais

### Sistema de Notificações Extensível

O sistema implementa um padrão de eventos (Observer Pattern) que torna fácil adicionar novos interessados nas notificações:

- **Quando uma reserva é confirmada:**
  - Hóspede recebe comprovante por e-mail
  - Governança é notificada sobre o quarto, datas e número de hóspedes
  - Financeiro é notificado sobre o pagamento recebido

- **Quando uma reserva confirmada é cancelada:**
  - Governança é notificada sobre as datas liberadas
  - Financeiro é notificado sobre reembolso (se houver)

**Para adicionar novos interessados** (restaurante, transfer, etc.), basta criar um novo listener e registrá-lo no EventPublisher. Não é necessário modificar o código central do sistema.

### Regras de Negócio

- 5 quartos: 101, 102 (STANDARD), 201, 202 (DELUXE), 301 (SUITE)
- Preços base: STANDARD R$ 249,90, DELUXE R$ 389,90, SUITE R$ 579,90
- Alta temporada (15/dez-fev + julho): +20%
- Plano não reembolsável: 15% de desconto
- Arredondamento "meio para o par" (banker's rounding)

## Como Usar

### Pré-requisitos

- Java 21
- Maven 3.6+
- Docker e Docker Compose (para desenvolvimento)

### Build

```bash
mvn clean package
```

### Executar

Com Docker Compose (recomendado para desenvolvimento):

```bash
chmod +x run.sh
./run.sh
```

Ou manualmente:

```bash
# Inicie o PostgreSQL
docker-compose up -d postgres

# Configure as variáveis de ambiente
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="postgres"
export PORT="7070"

# Execute o JAR
java -jar target/reservation-system-1.0.0.jar
```

### Testes

```bash
# Certifique-se de que o sistema está rodando
./run.sh

# Em outro terminal, execute os testes
mvn verify
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria nova reserva
- `GET /reservations/{id}` - Consulta reserva
- `POST /reservations/{id}/confirmation` - Confirma reserva (pagamento)
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out (retorna comprovante)
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - Relatório financeiro (CSV)

## Exemplo de Uso

```bash
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

## Arquitetura

O sistema utiliza uma arquitetura em camadas:

- **Domain**: Modelos e eventos de domínio
- **Repository**: Acesso aos dados
- **Service**: Lógica de negócio
- **Web**: Controllers e DTOs REST
- **Event Listeners**: Sistema extensível de notificações

O **EventPublisher** desacopla a lógica de negócio das notificações, tornando o sistema fácil de estender.

## Licença

Copyright © 2026 Pousada Maré Alta
