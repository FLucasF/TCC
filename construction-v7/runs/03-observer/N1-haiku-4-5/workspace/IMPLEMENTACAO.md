# Implementação do Sistema de Reservas - Pousada Maré Alta

## ✅ Requisitos Implementados

### 1. Estrutura de Quartos
- [x] 5 quartos com configurações distintas
- [x] Preços diários por tipo de quarto
- [x] Capacidade de hóspedes por quarto

### 2. Cálculo de Preços
- [x] Identificação automática de temporada alta (15 dez - 28 fev + julho)
- [x] Aumento de 20% em temporada alta
- [x] Plano NON_REFUNDABLE com desconto de 15%
- [x] Arredondamento banker's rounding (HALF_EVEN) em cada etapa

### 3. Fluxo de Reserva
- [x] Estados: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- [x] Cancelamento permitido em PENDING e CONFIRMED
- [x] Validações de capacidade e disponibilidade
- [x] Impedimento de duplas reservas na mesma noite

### 4. Sistema de Notificações Extensível
- [x] Notificação ao hóspede com comprovante
- [x] Notificação à governança com datas e número de hóspedes
- [x] Notificação ao financeiro com valor recebido
- [x] Notificações de cancelamento para governança e financeiro (quando há reembolso)
- [x] Arquitetura extensível para novos notificadores (restaurante, transfer, etc.)

### 5. Comprovante de Reserva
- [x] Formatação correta com dados da reserva
- [x] Suporte para descontos
- [x] Disponível para segunda via

### 6. Relatórios
- [x] Relatório de ocupação (CSV) por data
- [x] Relatório de receita (CSV) por mês
- [x] Cálculo correto de dinheiro recebido (pagamento - reembolso)

### 7. API REST
- [x] GET /rooms
- [x] POST /reservations
- [x] GET /reservations/{id}
- [x] POST /reservations/{id}/confirmation
- [x] POST /reservations/{id}/check-in
- [x] POST /reservations/{id}/check-out
- [x] POST /reservations/{id}/cancellation
- [x] GET /reservations/{id}/document
- [x] GET /messages
- [x] GET /reports/occupancy
- [x] GET /reports/revenue

### 8. Tratamento de Erros
- [x] 400 INVALID_REQUEST (dados inválidos)
- [x] 404 ROOM_NOT_FOUND
- [x] 404 RESERVATION_NOT_FOUND
- [x] 409 ROOM_UNAVAILABLE
- [x] 409 OPERATION_NOT_ALLOWED
- [x] 422 CAPACITY_EXCEEDED

### 9. Tecnologia
- [x] Java 21
- [x] Javalin 6.7.0
- [x] Maven com jar executável
- [x] Dados em memória
- [x] `mvn verify` passa com sucesso
- [x] 13 testes unitários passando

## 🏗️ Arquitetura

### Padrões Utilizados
- **MVC**: Controllers separam endpoints da lógica
- **Service Layer**: ReservationService, NotificationService, ReportService
- **Repository Pattern**: MessageRepository para dados
- **Observer Pattern**: NotificationService com handlers extensíveis
- **DTO Pattern**: Separação entre modelos internos e respostas da API

### Decisões de Design

1. **Notificadores como Handlers**: Permite adicionar novos notificadores sem modificar código existente. Basta criar uma nova classe que implemente `NotificationService.NotificationHandler` e registrá-la.

2. **Arredondamento em cada etapa**: Implementado em `MoneyUtil.round()` para garantir precisão nos cálculos monetários.

3. **Validação de temporada**: Centralizada em `DateUtil.isHighSeason()` para fácil manutenção.

4. **Sem persistência**: O design permite adicionar um banco de dados sem mudanças estruturais grandes - bastaria substituir `MessageRepository` e adicionar um layer de persistência.

## 🧪 Testes

```bash
# Executar todos os testes
mvn test

# Resultado
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
```

### Testes Unitários Cobertem
- ✅ Cálculos de preço (temporada alta, desconto)
- ✅ Transições de estado
- ✅ Validações (capacidade, disponibilidade)
- ✅ Sistema de notificações
- ✅ Extensibilidade para novos notificadores
- ✅ Reembolsos (STANDARD e NON_REFUNDABLE)

## 🚀 Como Começar

### 1. Build
```bash
mvn clean package
```

### 2. Execução
```bash
java -jar target/mare-alta.jar
```

### 3. Teste de API
```bash
# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João",
    "guestEmail": "joao@test.com",
    "guestCpf": "12345678900",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Confirmar
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 599.76}'
```

## 📋 Fluxo de Exemplo

```
Criar Reserva (Room 101, July 10-12, 2 guests)
├─ Subtotal: 249.90 * 1.2 * 2 = 599.76
└─ Status: PENDING

Confirmar Reserva (amountPaid: 599.76)
├─ Status: CONFIRMED
├─ Email ao hóspede: Comprovante
├─ Email à governança: "Reserva confirmada - quarto 101"
└─ Email ao financeiro: "Pagamento recebido - reserva 1"

Check-in
└─ Status: CHECKED_IN

Check-out
├─ Status: CHECKED_OUT
└─ Retorna: Documento da reserva

Ocupação (2027-07-10)
├─ Quarto 101: Ocupado (Res #1)
└─ Total: 1/5

Receita (2027-07)
├─ Res #1: 599.76
└─ Total: 599.76
```

## 🔧 Como Adicionar um Novo Notificador

Exemplo: Notificar o restaurante sobre chegadas de hóspedes

```java
class RestaurantNotificationHandler implements NotificationService.NotificationHandler {
    private MessageRepository messageRepository;
    
    @Override
    public boolean shouldHandle(NotificationService.ReservationEvent event) {
        return event == NotificationService.ReservationEvent.CONFIRMED;
    }
    
    @Override
    public void handle(Reservation reservation, RoomType roomType, Double refundAmount) {
        messageRepository.save(new Message(
            0,
            MessageChannel.EMAIL,
            "restaurante@pousadamarealta.com.br",
            "Novo hóspede chegando - " + reservation.getGuests() + " pessoas",
            "Entrada: " + reservation.getCheckIn() + 
            "\nHóspedes: " + reservation.getGuests()
        ));
    }
}
```

Depois, registrar na `NotificationService`:
```java
notificationService.registerHandler(new RestaurantNotificationHandler(messageRepository));
```

## ✨ Próximos Passos (Fora do Escopo)

- [ ] Persistência em banco de dados (PostgreSQL, MySQL)
- [ ] Autenticação e autorização
- [ ] Interface web para o gerente
- [ ] Sistema de pagamento integrado
- [ ] Envio de emails reais (SMTP)
- [ ] Logs estruturados (SLF4J com arquivo)
- [ ] Rate limiting e throttling
- [ ] Cache para consultas frequentes
