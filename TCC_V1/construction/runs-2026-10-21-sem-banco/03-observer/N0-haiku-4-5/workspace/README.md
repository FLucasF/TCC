# Sistema de Reservas - Pousada Maré Alta

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, com suporte para criação de reservas, confirmação, check-in/check-out, cancelamento e geração de relatórios.

## Características

- **5 Quartos**: STANDARD (101, 102), DELUXE (201, 202), SUITE (301)
- **Planos de Tarifa**: STANDARD (normal) e NON_REFUNDABLE (15% desconto)
- **Alta Temporada**: 15 de dezembro até 28 de fevereiro e julho inteiro (20% de acréscimo)
- **Sistema de Notificações Extensível**: Emails automáticos para hóspede, governança e financeiro
- **Relatórios em CSV**: Ocupação e financeiro
- **Armazenamento em Memória**: Dados persistem enquanto o servidor está rodando

## Como Executar

```bash
java -jar target/reservas-1.0.0.jar
```

O servidor inicia na porta 7070 (ou na porta especificada pela variável de ambiente PORT).

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria nova reserva
- `GET /reservations/{id}` - Obtém detalhes da reserva
- `POST /reservations/{id}/confirmation` - Confirma reserva (paga)
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out (retorna comprovante)
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Lista emails enviados (para debug)

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Ocupação para uma data (CSV)
- `GET /reports/revenue?month=2027-07` - Financeiro para um mês (CSV)

## Exemplo de Uso

```bash
# Criar uma reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
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

# Fazer check-in
curl -X POST http://localhost:7070/reservations/1/check-in

# Fazer check-out
curl -X POST http://localhost:7070/reservations/1/check-out

# Ver relatório de ocupação
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"

# Ver relatório financeiro
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

## Fluxo de Reserva

1. **PENDING** → Hóspede cria reserva (sem pagamento)
2. **CONFIRMED** → Hóspede paga (email de confirmação enviado)
3. **CHECKED_IN** → Hóspede faz check-in
4. **CHECKED_OUT** → Hóspede faz check-out (comprovante emitido)
5. **CANCELLED** → Reserva cancelada (reembolso se aplicável)

## Sistema de Notificações

O sistema envia emails (simulados) automaticamente para:

- **Hóspede**: Quando a reserva é confirmada (com comprovante)
- **Governança**: Quando reserva é confirmada (para preparar quarto) ou cancelada (para liberar datas)
- **Financeiro**: Quando pagamento é recebido ou reembolso é necessário

O sistema foi desenhado para permitir adicionar novos destinatários facilmente sem modificar o código core.

## Detalhes Técnicos

- **Java 21**
- **Javalin 6.7.0** (sem Spring)
- **Maven** para build
- Dados armazenados em memória (HashMap/ArrayList)
- Arredondamento "meio para o par" em todos os cálculos financeiros

## Teste

```bash
mvn verify
```

Compila, testa e gera JAR executável em `target/reservas-1.0.0.jar`.
