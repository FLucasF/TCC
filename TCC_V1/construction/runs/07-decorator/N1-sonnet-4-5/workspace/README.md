# Sistema de Reservas - Pousada Maré Alta

Sistema de gestão de reservas para a Pousada Maré Alta, com suporte a diferentes planos de tarifa, temporadas e **sistema configurável de regras de mensagens**.

## Características Principais

### Quartos
- **STANDARD** (101, 102): 2 pessoas, R$ 249,90/noite
- **DELUXE** (201, 202): 3 pessoas, R$ 389,90/noite
- **SUITE** (301): 4 pessoas, R$ 579,90/noite

### Planos de Tarifa
- **STANDARD**: Preço normal, reembolsável
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso

### Temporadas
- **Alta** (+20%): 15/dez a fim fev + julho completo
- **Baixa**: Demais períodos

### Sistema de Regras de Mensagens ⭐

O diferencial do sistema é sua arquitetura de **regras configuráveis** para envio de mensagens. Cada nova regra é apenas uma implementação de `MessageRule`, sem modificar código existente.

#### Regras Implementadas

1. **EmailValidationRule**: Valida formato de e-mail (usuario@dominio.com)
2. **ArchiveCopyRule**: Cria cópia para arquivo@pousadamarealta.com.br
3. **TestModeRedirectRule**: Redireciona para testes@pousadamarealta.com.br em modo de teste

#### Como Adicionar uma Nova Regra

```java
public class NoturnoBlockRule implements MessageRule {
    @Override
    public void apply(MessageRuleContext context) {
        LocalTime now = LocalTime.now();
        if (now.isAfter(LocalTime.of(22, 0)) || now.isBefore(LocalTime.of(6, 0))) {
            context.setStatus(MessageStatus.BLOCKED);
        }
    }
}
```

Depois, basta adicionar no `MessageProcessor`:

```java
List<MessageRule> rules = new ArrayList<>();
rules.add(new EmailValidationRule());
rules.add(new NoturnoBlockRule());  // Nova regra entra aqui
rules.add(new ArchiveCopyRule());
```

## Como Usar

### Compilar

```bash
mvn clean package
```

### Executar

```bash
# Modo produção (porta 7070, com cópia para arquivo)
java -jar target/reservas-1.0.0.jar

# Modo produção em porta customizada
PORT=8080 java -jar target/reservas-1.0.0.jar

# Modo de teste (redireciona e-mails para testes@pousadamarealta.com.br)
HOTEL_TEST_MODE=true java -jar target/reservas-1.0.0.jar
```

### Testes

```bash
# Todos os testes
mvn verify

# Apenas testes unitários
mvn test

# Apenas testes de integração
mvn failsafe:integration-test
```

## API REST

### Quartos

```bash
# Listar quartos
curl http://localhost:7070/rooms
```

### Reservas

```bash
# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Ana Silva",
    "guestEmail": "ana@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Consultar reserva
curl http://localhost:7070/reservations/1

# Confirmar reserva (envia e-mail)
curl -X POST http://localhost:7070/reservations/1/confirmation

# Check-in
curl -X POST http://localhost:7070/reservations/1/check-in

# Check-out (retorna comprovante)
curl -X POST http://localhost:7070/reservations/1/check-out

# Cancelar
curl -X POST http://localhost:7070/reservations/1/cancellation

# Segunda via do comprovante
curl http://localhost:7070/reservations/1/document
```

### Mensagens

```bash
# Listar mensagens enviadas
curl http://localhost:7070/messages
```

### Relatórios

```bash
# Ocupação de uma noite
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"

# Receita de um mês
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

## Códigos de Erro

- `400 INVALID_REQUEST`: Dados inválidos
- `404 ROOM_NOT_FOUND`: Quarto não existe
- `404 RESERVATION_NOT_FOUND`: Reserva não existe
- `409 ROOM_UNAVAILABLE`: Quarto ocupado nas datas
- `409 OPERATION_NOT_ALLOWED`: Operação não permitida no estado atual
- `422 CAPACITY_EXCEEDED`: Mais hóspedes que capacidade do quarto

## Arquitetura

O sistema segue os princípios do CLAUDE.md:
- **Novos casos entram em um lugar só**: Adicionar regra de mensagem = implementar `MessageRule`
- **Sequência única, variações localizadas**: `MessageProcessor` aplica todas as regras
- **Organização onde há variação futura**: Sistema de regras preparado para crescer

### Estrutura

```
com.pousadamarealta/
├── domain/              # Entidades e enums
├── messaging/           # Sistema de regras ⭐
│   ├── MessageRule.java
│   ├── MessageRuleContext.java
│   ├── MessageProcessor.java
│   └── rules/
│       ├── EmailValidationRule.java
│       ├── ArchiveCopyRule.java
│       └── TestModeRedirectRule.java
├── repository/          # Armazenamento em memória
├── service/             # Lógica de negócio
│   ├── PricingService.java
│   ├── ReservationService.java
│   ├── MessageService.java
│   └── ReportService.java
└── App.java            # API REST com Javalin
```

## Tecnologias

- Java 21
- Javalin 6.7.0
- Jackson (JSON)
- JUnit 5 + AssertJ
- Maven
