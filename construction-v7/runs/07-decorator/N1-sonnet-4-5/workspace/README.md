# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin.

## Requisitos

- Java 21 ou superior
- Maven 3.6 ou superior

## Compilação

```bash
mvn clean package
```

Isso irá gerar um JAR executável em `target/reservation-system-1.0.0.jar`.

## Execução

### Modo normal (produção)

```bash
java -jar target/reservation-system-1.0.0.jar
```

O sistema irá iniciar na porta 7070 (padrão) ou na porta especificada pela variável de ambiente `PORT`.

### Modo de teste

```bash
HOTEL_TEST_MODE=true java -jar target/reservation-system-1.0.0.jar
```

No modo de teste:
- E-mails são redirecionados para `testes@pousadamarealta.com.br`
- O assunto recebe o prefixo `[TESTE]`
- O corpo recebe uma primeira linha indicando o destinatário original
- Não há cópia para o arquivo

### Porta customizada

```bash
PORT=8080 java -jar target/reservation-system-1.0.0.jar
```

## Testes

```bash
mvn test
```

Para executar os testes de integração:

```bash
./test-api.sh
./test-test-mode.sh
```

## API

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out (retorna comprovante)
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens

- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro (CSV)

## Quartos da Pousada

- **101, 102** - STANDARD (2 pessoas) - R$ 249,90/noite
- **201, 202** - DELUXE (3 pessoas) - R$ 389,90/noite
- **301** - SUITE (4 pessoas) - R$ 579,90/noite

## Regras de Negócio

### Alta Temporada

- 15 de dezembro a fim de fevereiro
- Todo o mês de julho
- Acréscimo de 20% no valor da diária

### Planos de Tarifa

- **STANDARD**: Tarifa normal, reembolso total em caso de cancelamento
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso em caso de cancelamento

### Status da Reserva

1. **PENDING** - Reserva criada, aguardando pagamento
2. **CONFIRMED** - Reserva paga
3. **CHECKED_IN** - Hóspede já fez check-in
4. **CHECKED_OUT** - Hóspede já fez check-out
5. **CANCELLED** - Reserva cancelada

### Sistema de Mensagens

O sistema implementa três regras de mensagens:

1. **Validação de e-mail**: E-mails inválidos são bloqueados
   - Deve ter exatamente um `@`
   - Deve ter algo antes do `@`
   - O domínio (após `@`) deve conter pelo menos um ponto

2. **Cópia para arquivo**: Toda mensagem enviada gera uma cópia para `arquivo@pousadamarealta.com.br`
   - Não se aplica em modo de teste
   - Mensagens bloqueadas não geram cópia

3. **Modo de teste**: Quando `HOTEL_TEST_MODE=true`
   - Mensagens vão para `testes@pousadamarealta.com.br`
   - Assunto recebe prefixo `[TESTE]`
   - Primeira linha do corpo indica o destinatário original
   - Validação de e-mail continua ativa

## Exemplo de Uso

### Criar uma reserva

```bash
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
```

### Confirmar a reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

### Consultar mensagens enviadas

```bash
curl http://localhost:7070/messages
```

## Estrutura do Projeto

```
src/main/java/com/pousadamarealta/
├── App.java                    # Classe principal
├── controller/                 # Controllers da API
│   ├── MessageController.java
│   ├── ReportController.java
│   ├── ReservationController.java
│   └── RoomController.java
├── domain/                     # Modelos de domínio
│   ├── Message.java
│   ├── MessageChannel.java
│   ├── MessageStatus.java
│   ├── RatePlan.java
│   ├── Reservation.java
│   ├── ReservationStatus.java
│   ├── Room.java
│   └── RoomType.java
├── dto/                        # DTOs de request/response
│   ├── request/
│   └── response/
├── messaging/                  # Sistema de mensagens
│   ├── MessageProcessor.java
│   ├── OutgoingMessage.java
│   └── ProcessedMessage.java
├── service/                    # Serviços de negócio
│   ├── MessageService.java
│   ├── ReportService.java
│   ├── ReservationService.java
│   └── RoomService.java
└── util/                       # Utilitários
    ├── CpfFormatter.java
    ├── DateFormatter.java
    ├── MoneyFormatter.java
    └── PriceCalculator.java
```

## Arquitetura

O sistema foi organizado seguindo os princípios do CLAUDE.md:

- **Extensibilidade**: Novas regras de mensagem podem ser adicionadas facilmente no `MessageProcessor`
- **Responsabilidade única**: Cada classe tem uma responsabilidade bem definida
- **Sem acoplamento desnecessário**: Classes não conhecem detalhes que não são de sua responsabilidade

O sistema de mensagens foi projetado para ser facilmente extensível. Novas regras (como limite de mensagens por dia ou restrição de horário) podem ser adicionadas modificando apenas o `MessageProcessor`, sem afetar o resto do sistema.
