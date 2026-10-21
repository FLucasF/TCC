# Resumo do Sistema - Pousada Maré Alta

## ✅ Sistema Completo e Funcionando

### Requisitos Implementados

#### 1. Core do Negócio
- ✅ 5 quartos (STANDARD, DELUXE, SUITE) com capacidades e preços
- ✅ Cálculo de preços com alta temporada (+20%)
- ✅ Planos de tarifa (STANDARD e NON_REFUNDABLE com 15% desconto)
- ✅ Arredondamento "meio para o par" (banker's rounding)
- ✅ Ciclo de vida da reserva: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- ✅ Cancelamento com regras de reembolso
- ✅ Validações (capacidade, disponibilidade, datas)

#### 2. API REST (Javalin 6.7.0)
- ✅ GET /rooms
- ✅ POST /reservations (cria PENDING)
- ✅ GET /reservations/{id}
- ✅ POST /reservations/{id}/confirmation
- ✅ POST /reservations/{id}/cancellation
- ✅ POST /reservations/{id}/check-in
- ✅ POST /reservations/{id}/check-out
- ✅ GET /reservations/{id}/document (segunda via)
- ✅ GET /messages (lista e-mails enviados)
- ✅ GET /reports/occupancy?date=...
- ✅ GET /reports/revenue?month=...
- ✅ Tratamento de erros (400, 404, 409, 422)

#### 3. **Sistema de Notificações (Observer Pattern)** ⭐

**Requisito chave:** "E isso não vai parar por aí... vai ter cada vez mais gente querendo ser avisada"

##### Na Confirmação (3 e-mails):
1. **Hóspede** → comprovante completo
2. **Governança** → datas e número de hóspedes
3. **Financeiro** → valor recebido

##### No Cancelamento (até 2 e-mails):
1. **Governança** → sempre (datas liberadas)
2. **Financeiro** → só se houver reembolso (STANDARD)

##### Estrutura do Observer:
```
notification/
├── ReservationEvent.java          # Eventos (sealed interface)
├── ReservationListener.java       # Interface dos observers
├── MessageSender.java             # Abstração de envio
├── GuestNotifier.java            # Listener: hóspede
├── HousekeepingNotifier.java     # Listener: governança
└── FinanceNotifier.java          # Listener: financeiro
```

##### Como Funciona:
- `ReservationService` recebe lista de listeners no construtor
- Quando confirma/cancela, dispara evento para todos
- Cada listener decide se reage e como
- Adicionar novo listener = criar classe + registrar em Main

#### 4. Relatórios (CSV)
- ✅ Ocupação por data (quartos ocupados naquela noite)
- ✅ Financeiro por mês (receita das reservas do mês)
- ✅ Formato CSV correto com charset utf-8

#### 5. Build e Deploy
- ✅ Java 21
- ✅ Maven com `mvn package` gerando JAR executável
- ✅ Porta configurável (env PORT ou 7070)
- ✅ `mvn verify` passa (18 testes, 0 falhas)
- ✅ JAR executável: `java -jar target/pousada-mare-alta-1.0.0.jar`

## 🎯 Decisões de Design

### Por que Observer?

Seguindo as orientações do CLAUDE.md:
> "Antes de escrever cada parte, pergunte: para acrescentar o próximo caso parecido com este, quantos lugares do código teriam de mudar?"

O enunciado deixava claro:
- **Variação existente:** 3 tipos de notificação na confirmação, 2 no cancelamento
- **Variação futura garantida:** restaurante, transfer, "cada vez mais gente", "entram e saem conforme contratos"

**Sem Observer:** adicionar restaurante = editar `ReservationService`  
**Com Observer:** adicionar restaurante = criar `RestaurantNotifier` + 1 linha em `Main`

### Aplicado Completamente

Conforme design-patterns skill:
- ✅ "Move every case into the structure" → GuestNotifier, HousekeepingNotifier e FinanceNotifier são listeners
- ✅ "Keep one place that chooses" → `Main.java` monta a lista
- ✅ "Source only announces" → `ReservationService` apenas chama `notifyListeners()`
- ✅ "Adding the next case" → criar classe + registrar, sem mexer no service

## 📊 Testes

```
18 testes, 0 falhas

Pricing (7 testes):
  ✓ Cálculo baixa temporada
  ✓ Alta temporada (julho, dezembro, janeiro, fevereiro)
  ✓ Desconto NON_REFUNDABLE
  ✓ Arredondamento correto

Notificações (4 testes):
  ✓ Confirmação envia 3 e-mails
  ✓ Cancelamento de confirmada envia 2 e-mails
  ✓ Cancelamento de pendente não envia nada
  ✓ NON_REFUNDABLE cancelado não notifica financeiro

API (7 testes):
  ✓ Listar quartos
  ✓ Criar e confirmar reserva
  ✓ Erros: room not found, capacity exceeded, room unavailable
  ✓ Relatórios de ocupação e financeiro
```

## 🚀 Como Usar

### Executar
```bash
mvn package
java -jar target/pousada-mare-alta-1.0.0.jar
```

### Testar
```bash
# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{"room_number": 201, "guest_name": "Carla", ...}'

# Confirmar (dispara Observer)
curl -X POST http://localhost:7070/reservations/1/confirmation

# Ver e-mails enviados
curl http://localhost:7070/messages | jq
```

## 📁 Estrutura do Código

```
src/main/java/com/pousadamarealta/
├── Main.java                      # Configuração e wiring
├── domain/                        # Entidades e enums
│   ├── Room.java
│   ├── Reservation.java
│   ├── Message.java
│   └── ...
├── pricing/
│   └── PricingService.java       # Cálculo de preços
├── notification/                  # ⭐ Observer Pattern
│   ├── ReservationEvent.java
│   ├── ReservationListener.java
│   ├── GuestNotifier.java
│   ├── HousekeepingNotifier.java
│   └── FinanceNotifier.java
├── repository/                    # Dados em memória
│   ├── RoomRepository.java
│   ├── ReservationRepository.java
│   └── MessageRepository.java
├── service/
│   ├── ReservationService.java   # Core business (usa Observer)
│   └── ReportService.java
└── api/
    ├── ApiController.java        # Endpoints REST
    └── ErrorResponse.java
```

## 🎓 Demonstração do Observer

Veja `OBSERVER_PATTERN.md` para:
- Arquitetura detalhada
- Fluxo de execução
- Código simplificado
- Como adicionar novos listeners
- Comparação: com vs sem Observer

## ✨ Próximos Passos (Futuro)

Para adicionar restaurante:
1. Criar `RestaurantNotifier implements ReservationListener`
2. Em `Main.java`, adicionar à lista: `List.of(..., restaurante)`
3. Pronto!

Nenhuma mudança em:
- ReservationService
- GuestNotifier, HousekeepingNotifier, FinanceNotifier
- Reservation, API, repositórios

O Observer garante que cada novo caso **entra em um lugar só**.
