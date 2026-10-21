# Pousada Maré Alta - Sistema de Reservas

Sistema de gestão de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin.

## Estrutura do Projeto

O sistema foi projetado seguindo as orientações do CLAUDE.md, com foco em extensibilidade e manutenção:

### Padrão Observer para Notificações

A funcionalidade principal implementada é o **sistema de notificações usando o padrão Observer**. Quando eventos importantes acontecem (confirmação ou cancelamento de reserva), múltiplos interessados são notificados automaticamente.

#### Por que Observer?

O enunciado deixava claro que:
- "E isso não vai parar por aí"
- "O restaurante já pediu para saber das confirmações"
- "O pessoal do transfer do aeroporto quer saber quem está chegando"
- "Vai ter cada vez mais gente querendo ser avisada, de mais coisas"
- "Quem é avisado vai mudar com o tempo: restaurante e transfer entram e saem conforme os contratos"

Isso indica uma **variação que vai continuar**, tornando o Observer a escolha natural.

#### Implementação

```
notification/
├── ReservationEvent.java          # Eventos (Confirmed, Cancelled)
├── ReservationListener.java       # Interface dos listeners
├── MessageSender.java             # Interface para envio
├── GuestNotifier.java            # Notifica o hóspede
├── HousekeepingNotifier.java     # Notifica a governança
└── FinanceNotifier.java          # Notifica o financeiro
```

O `ReservationService` recebe a lista de listeners no construtor e notifica todos quando um evento ocorre:

```java
private void notifyListeners(ReservationEvent event) {
    for (ReservationListener listener : listeners) {
        listener.onEvent(event);
    }
}
```

**Para adicionar um novo interessado** (ex: restaurante):
1. Criar `RestaurantNotifier implements ReservationListener`
2. Adicionar à lista de listeners em `Main.java`
3. **Pronto!** Nenhuma mudança no `ReservationService` ou nos listeners existentes

#### Exemplo de Novo Listener

```java
public final class RestaurantNotifier implements ReservationListener {
    private final MessageSender messageSender;
    
    public RestaurantNotifier(MessageSender messageSender) {
        this.messageSender = messageSender;
    }
    
    @Override
    public void onEvent(ReservationEvent event) {
        if (event instanceof ReservationEvent.Confirmed confirmed) {
            notifyBreakfast(confirmed.reservation());
        }
    }
    
    private void notifyBreakfast(Reservation reservation) {
        messageSender.send(
            MessageChannel.EMAIL,
            "restaurante@pousadamarealta.com.br",
            String.format("Café da manhã - %d pessoas em %s", 
                reservation.guests(), 
                formatDate(reservation.checkIn())),
            "..."
        );
    }
}
```

## Como Executar

### Compilar e Testar

```bash
mvn verify
```

### Executar o Sistema

```bash
java -jar target/pousada-mare-alta-1.0.0.jar
```

O servidor inicia na porta 7070 (ou na porta definida pela variável `PORT`).

### Testar as Notificações

```bash
# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "room_number": 201,
    "guest_name": "Carla Dias",
    "guest_email": "carla@example.com",
    "guest_cpf": "11122233344",
    "check_in": "2027-03-10",
    "check_out": "2027-03-12",
    "guests": 2,
    "rate_plan": "STANDARD"
  }'

# Confirmar reserva (dispara 3 notificações)
curl -X POST http://localhost:7070/reservations/1/confirmation

# Ver mensagens enviadas
curl http://localhost:7070/messages
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma reserva (status: PENDING)
- `GET /reservations/{id}` - Busca uma reserva
- `POST /reservations/{id}/confirmation` - Confirma reserva (envia 3 e-mails)
- `POST /reservations/{id}/cancellation` - Cancela reserva (envia até 2 e-mails)
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out (retorna comprovante)
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - Relatório financeiro (CSV)

## Regras de Negócio

### Quartos
- 101, 102: STANDARD (2 pessoas, R$ 249,90/noite)
- 201, 202: DELUXE (3 pessoas, R$ 389,90/noite)
- 301: SUITE (4 pessoas, R$ 579,90/noite)

### Alta Temporada (+20%)
- 15/dez até fim de fevereiro
- Julho inteiro

### Planos de Tarifa
- STANDARD: preço normal, reembolsável
- NON_REFUNDABLE: 15% de desconto, sem reembolso

### Notificações (Observer Pattern)

**Na confirmação** (3 e-mails):
1. **Hóspede**: comprovante completo
2. **Governança**: datas e número de hóspedes
3. **Financeiro**: valor recebido

**No cancelamento** (até 2 e-mails):
1. **Governança**: sempre notificada (datas liberadas)
2. **Financeiro**: só se houver reembolso (plano STANDARD)

## Tecnologias

- Java 21
- Javalin 6.7.0 (sem Spring)
- Maven
- JUnit 5 + AssertJ
- Jackson (JSON)
- Dados em memória

## Testes

18 testes implementados:
- 7 testes de pricing (cálculos, alta temporada, descontos)
- 4 testes de notificações (Observer pattern)
- 7 testes de integração da API

Executar com: `mvn test`
