# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido com Java 21, Javalin 6.7.0 e PostgreSQL 18.

## Características

✅ **Gerenciamento de Reservas**
- Criar, confirmar, cancelar reservas
- Check-in e check-out
- Cálculo automático de preços com alta temporada
- Planos de tarifa (Standard e Non-Refundable)
- Comprovantes e segunda via

✅ **Sistema de Notificações Extensível**
- E-mails para hóspede, governança e financeiro
- Fácil adicionar novos departamentos
- Registro de todas as mensagens enviadas

✅ **Relatórios em CSV**
- Ocupação por noite
- Receita por mês

✅ **Persistência em Banco de Dados**
- PostgreSQL 18 com pool de conexões
- Criação automática de schema
- Transações seguras

## Estrutura

```
src/main/java/com/pousada/marealta/
├── App.java                    # Aplicação principal com endpoints
├── db/                         # Acesso ao banco de dados
│   ├── Database.java
│   ├── RoomRepository.java
│   ├── ReservationRepository.java
│   └── MessageRepository.java
├── model/                      # Classes de domínio
│   ├── Room.java
│   ├── Reservation.java
│   ├── Message.java
│   └── Document.java
├── enums/
│   ├── RoomType.java
│   ├── ReservationStatus.java
│   ├── RatePlan.java
│   └── MessageChannel.java
├── service/                    # Lógica de negócio
│   ├── ReservationService.java
│   └── ReportService.java
├── notification/               # Sistema de notificações
│   ├── ReservationEvent.java
│   ├── ReservationNotifier.java
│   ├── NotificationManager.java
│   └── notifiers/
│       ├── GuestNotifier.java
│       ├── HousekeepingNotifier.java
│       └── FinanceNotifier.java
├── api/                        # Requisições e respostas HTTP
│   ├── ApiError.java
│   ├── request/
│   └── response/
└── util/                       # Utilitários
    ├── PricingCalculator.java
    ├── Formatter.java
    └── DocumentGenerator.java
```

## Requisitos

- Java 21
- Maven 3.8+
- PostgreSQL 18+

## Compilação

```bash
mvn clean package
```

Gera um JAR em `target/mare-alta-reservas-1.0.0.jar`.

## Execução

```bash
export DB_URL="jdbc:postgresql://localhost:5432/mare_alta"
export DB_USER="postgres"
export DB_PASSWORD="senha"
export PORT=7070

java -jar target/mare-alta-reservas-1.0.0.jar
```

### Variáveis de Ambiente

- `DB_URL` - String de conexão PostgreSQL (obrigatória)
- `DB_USER` - Usuário do banco (obrigatória)
- `DB_PASSWORD` - Senha do banco (obrigatória)
- `PORT` - Porta HTTP (opcional, padrão: 7070)

## API

### Quartos
```
GET /rooms
```
Retorna lista de quartos com número, tipo, capacidade e tarifa.

### Reservas

Criar:
```
POST /reservations
Content-Type: application/json

{
  "roomNumber": 201,
  "guestName": "João Silva",
  "guestEmail": "joao@email.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD"  // ou NON_REFUNDABLE
}
```

Obtém:
```
GET /reservations/{id}
```

Confirma:
```
POST /reservations/{id}/confirmation
```

Cancela:
```
POST /reservations/{id}/cancellation
```

Check-in:
```
POST /reservations/{id}/check-in
```

Check-out (retorna comprovante):
```
POST /reservations/{id}/check-out
```

Segunda via do comprovante:
```
GET /reservations/{id}/document
```

### Mensagens
```
GET /messages
```
Lista todas as mensagens enviadas (hóspede, governança, financeiro).

### Relatórios

Ocupação:
```
GET /reports/occupancy?date=2027-07-10
```
Retorna CSV com quartos ocupados naquela noite.

Receita:
```
GET /reports/revenue?month=2027-07
```
Retorna CSV com reservas e receita do mês.

## Quartos

| Número | Tipo      | Capacidade | Tarifa    | Alta Temp |
|--------|-----------|------------|-----------|-----------|
| 101    | STANDARD  | 2 pessoas  | R$ 249,90 | R$ 299,88 |
| 102    | STANDARD  | 2 pessoas  | R$ 249,90 | R$ 299,88 |
| 201    | DELUXE    | 3 pessoas  | R$ 389,90 | R$ 467,88 |
| 202    | DELUXE    | 3 pessoas  | R$ 389,90 | R$ 467,88 |
| 301    | SUITE     | 4 pessoas  | R$ 579,90 | R$ 695,88 |

Alta temporada: 15/12 a 28/02 e julho (20% acréscimo).

## Planos de Tarifa

- **STANDARD**: Tarifa normal, reembolso total em caso de cancelamento
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso em caso de cancelamento

## Cálculo de Preços

1. Calcula tarifa de cada noite (base + 20% se alta temporada)
2. Arredonda para centavos (meio para par)
3. Soma todas as noites
4. Aplica desconto se NON_REFUNDABLE (15%)
5. Obtém total final

Exemplo: 2 noites de STANDARD em julho (01/07 a 03/07)
- Noite 1 (01/07): R$ 249,90 × 1,20 = R$ 299,88
- Noite 2 (02/07): R$ 249,90 × 1,20 = R$ 299,88
- Subtotal: R$ 599,76
- Se STANDARD: Total = R$ 599,76
- Se NON_REFUNDABLE: Desconto = R$ 89,96, Total = R$ 509,80

## Sistema de Notificações

Quando uma reserva é confirmada, e-mails são enviados para:
- **Hóspede**: Confirmação com comprovante
- **Governança** (governanca@pousadamarealta.com.br): Datas de entrada/saída e número de hóspedes
- **Financeiro** (financeiro@pousadamarealta.com.br): Valor pago

Quando uma reserva confirmada é cancelada:
- **Governança**: Notificação de cancelamento
- **Financeiro**: Notificação de reembolso (se aplicável)

### Adicionar Novo Notificador

1. Criar classe em `notification/notifiers/` implementando `ReservationNotifier`
2. Implementar `handleEvent(ReservationEvent event)`
3. Registrar em `App.initializeNotifiers()` chamando `NotificationManager.registerNotifier()`

## Testes

```bash
mvn verify
```

Executa testes de unidade para cálculo de preços e formatação.

## Script de Teste

```bash
./test.sh
```

Testa todos os endpoints da API (requer aplicação rodando).

## Banco de Dados

Tabelas criadas automaticamente:
- `rooms` - Quartos
- `reservations` - Reservas com histórico completo
- `messages` - Mensagens enviadas

## Tratamento de Erros

| Erro                  | HTTP | Código                 |
|----------------------|------|------------------------|
| Falta de campo       | 400  | INVALID_REQUEST        |
| Quarto não existe    | 404  | ROOM_NOT_FOUND         |
| Reserva não existe   | 404  | RESERVATION_NOT_FOUND  |
| Quarto indisponível  | 409  | ROOM_UNAVAILABLE       |
| Operação não permitida| 409  | OPERATION_NOT_ALLOWED  |
| Capacidade excedida  | 422  | CAPACITY_EXCEEDED      |

## Exemplo de Uso

```bash
# 1. Listar quartos
curl http://localhost:7070/rooms | jq

# 2. Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Silva",
    "guestEmail": "maria@email.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-13",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq

# 3. Confirmar reserva
curl -X POST http://localhost:7070/reservations/1/confirmation | jq

# 4. Verificar mensagens
curl http://localhost:7070/messages | jq

# 5. Check-in
curl -X POST http://localhost:7070/reservations/1/check-in | jq

# 6. Check-out
curl -X POST http://localhost:7070/reservations/1/check-out | jq

# 7. Relatório de ocupação
curl http://localhost:7070/reports/occupancy?date=2027-07-11

# 8. Relatório financeiro
curl http://localhost:7070/reports/revenue?month=2027-07
```

## Desenvolvimento

Para adicionar funcionalidades:

1. **Novo tipo de quarto**: Adicione a enum em `RoomType.java`
2. **Novo plano de tarifa**: Adicione em `RatePlan.java`
3. **Novo status de reserva**: Adicione em `ReservationStatus.java`
4. **Novo departamento notificado**: Crie notificador em `notification/notifiers/`
5. **Novo relatório**: Adicione método em `ReportService.java` e endpoint em `App.java`

Todas as operações seguem a lógica de não mexer no que já funciona: mudanças são adicionadas como novos arquivos e registros, não modificações ao código existente.

## Licença

Desenvolvido para Pousada Maré Alta.
