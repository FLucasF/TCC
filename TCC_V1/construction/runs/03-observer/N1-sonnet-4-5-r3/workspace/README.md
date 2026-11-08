# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Requisitos

- Java 21
- PostgreSQL 18
- Maven

## Configuração

O sistema requer as seguintes variáveis de ambiente:

- `DB_URL`: URL de conexão JDBC (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT` (opcional): Porta do servidor (padrão: 7070)

## Compilação

```bash
mvn package
```

## Execução

```bash
java -jar target/reservas-1.0.0.jar
```

Ou com variáveis de ambiente:

```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=postgres \
DB_PASSWORD=senha \
PORT=8080 \
java -jar target/reservas-1.0.0.jar
```

## Testes

Testes unitários:
```bash
mvn test
```

Verificação completa (inclui testes de integração):
```bash
mvn verify
```

**Nota**: Os testes de integração requerem acesso a um banco PostgreSQL configurado via variáveis de ambiente.

## Teste Manual

Para testar a API localmente:

1. Suba o PostgreSQL com Docker:
```bash
docker-compose up -d
```

2. Execute o sistema:
```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=postgres \
DB_PASSWORD=postgres \
java -jar target/reservas-1.0.0.jar
```

3. Em outro terminal, execute o script de testes:
```bash
./test-api.sh
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
- `POST /reservations/{id}/check-out` - Faz check-out e retorna comprovante
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens

- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro

## Arquitetura

O sistema utiliza uma arquitetura orientada a eventos para gerenciar notificações:

- **Domain Events**: Eventos de domínio (`ReservationConfirmed`, `ReservationCancelled`)
- **Event Listeners**: Listeners que reagem aos eventos e enviam notificações
- **Event Publisher**: Distribui eventos para os listeners registrados

Esta abordagem permite adicionar novos departamentos e tipos de notificações sem modificar o código existente, apenas adicionando novos listeners.

### Estrutura de Pacotes

- `domain`: Entidades, value objects e eventos de domínio
- `domain.events`: Sistema de eventos
- `repository`: Interfaces e implementações de persistência
- `service`: Lógica de negócio e listeners de eventos
- `api`: Controllers REST e DTOs
- `infrastructure`: Configuração e inicialização do banco de dados

## Notificações

Quando uma reserva é confirmada, o sistema envia notificações para:
- Hóspede: e-mail de confirmação
- Governança: aviso de nova reserva
- Financeiro: aviso de pagamento recebido

Quando uma reserva confirmada é cancelada:
- Governança: aviso de cancelamento
- Financeiro: aviso de reembolso (apenas se houver valor a devolver)

Reservas pendentes canceladas não geram notificações.
