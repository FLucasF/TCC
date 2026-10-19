# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Construir e executar

```bash
# Compilar e gerar o JAR
mvn package

# Executar o servidor
java -jar target/reservas-1.0.0.jar

# Especificar porta customizada
PORT=8080 java -jar target/reservas-1.0.0.jar
```

O servidor inicia na porta 7070 por padrão.

## Executar testes

```bash
mvn verify
```

## API

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria nova reserva
- `GET /reservations/{id}` - Consulta reserva
- `POST /reservations/{id}/confirmation` - Confirma reserva
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens

- `GET /messages` - Lista mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro

## Exemplo de uso

```bash
# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Carla Dias",
    "guestEmail": "carla@email.com",
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

## Notificações

O sistema envia automaticamente notificações por e-mail quando:

- **Reserva confirmada:**
  - Hóspede recebe comprovante
  - Governança é notificada para preparar o quarto
  - Financeiro é notificado do pagamento recebido

- **Reserva cancelada (se já estava confirmada):**
  - Governança é notificada que o quarto ficou livre
  - Financeiro é notificado do reembolso (se aplicável)

Para adicionar novos destinatários ou tipos de notificação, basta criar um novo listener e registrá-lo no `EventBus` em `Application.java`.
