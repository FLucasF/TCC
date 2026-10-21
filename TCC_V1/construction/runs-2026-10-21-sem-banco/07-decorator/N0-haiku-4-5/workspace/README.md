# Sistema de Reservas - Pousada Maré Alta

Um sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0.

## Compilação e Execução

### Compilar
```bash
mvn clean verify
```

### Executar
```bash
java -jar target/reservas-1.0.0.jar
```

O servidor inicia na porta especificada pela variável de ambiente `PORT`, ou na porta 7070 por padrão.

### Modo de Teste
```bash
HOTEL_TEST_MODE=true java -jar target/reservas-1.0.0.jar
```

Em modo de teste, todos os emails são redirecionados para `testes@pousadamarealta.com.br` com o assunto prefixado com `[TESTE]`.

## Características

### Quartos Disponíveis
- **101, 102**: STANDARD (até 2 pessoas) - R$ 249,90/noite
- **201, 202**: DELUXE (até 3 pessoas) - R$ 389,90/noite
- **301**: SUITE (até 4 pessoas) - R$ 579,90/noite

### Temporadas
- **Alta** (15/12 a 29/02 e julho inteiro): +20% no valor da diária
- **Normal**: preço base

### Planos de Tarifa
- **STANDARD**: preço normal
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso em caso de cancelamento

### Estados da Reserva
- PENDING: criada, aguardando confirmação
- CONFIRMED: pagamento recebido
- CHECKED_IN: hóspede fez o check-in
- CHECKED_OUT: hóspede fez o check-out
- CANCELLED: cancelada

## API

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Obtém detalhes de uma reserva
- `POST /reservations/{id}/confirmation` - Confirma a reserva (pagamento)
- `POST /reservations/{id}/check-in` - Realiza o check-in
- `POST /reservations/{id}/check-out` - Realiza o check-out (retorna comprovante)
- `GET /reservations/{id}/document` - Obtém segunda via do comprovante
- `POST /reservations/{id}/cancellation` - Cancela a reserva

### Mensagens
- `GET /messages` - Lista todas as mensagens de email enviadas

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=YYYY-MM` - Relatório de receita (CSV)

## Sistema de Emails

### Validação
Um email é considerado válido quando:
- Contém exatamente um `@`
- Tem algum conteúdo antes do `@`
- Tem um domínio após o `@` com pelo menos um ponto (ex: `user@example.com`)

Emails inválidos são marcados como BLOCKED e não geram cópia para arquivo.

### Modo Normal
- Email de confirmação enviado para o hóspede
- Cópia automática para `arquivo@pousadamarealta.com.br` com assunto prefixado com `[Cópia] `

### Modo Teste
- Email redirecionado para `testes@pousadamarealta.com.br`
- Assunto prefixado com `[TESTE] `
- Primeira linha adicionada: `Destinatário original: <email_original>`
- Sem cópia para arquivo

## Exemplo de Uso

### Criar uma reserva
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Confirmar uma reserva
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

### Fazer check-in
```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

### Fazer check-out (retorna comprovante)
```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

### Obter relatório de ocupação
```bash
curl http://localhost:7070/reports/occupancy?date=2027-03-10
```

## Estrutura do Projeto

```
src/main/java/com/pousadamarealta/
├── model/              # Entidades do domínio
│   ├── Room.java
│   ├── Reservation.java
│   ├── Message.java
│   └── Enums
├── service/            # Lógica de negócio
│   ├── ReservationService.java
│   ├── MessageService.java
│   └── ReportService.java
├── dto/                # Data Transfer Objects
├── util/               # Utilitários
│   ├── Money.java
│   ├── DateUtils.java
│   └── EmailValidator.java
└── Main.java           # Aplicação Javalin

src/test/java/         # Testes unitários
```

## Tecnologias

- **Java 21**
- **Javalin 6.7.0** - Framework web
- **Jackson** - Serialização JSON
- **JUnit 5** - Testes
- **Maven** - Build

## Arredondamento Monetário

Todos os valores monetários são arredondados para centavos usando o método HALF_EVEN (banker's rounding):
- Preço de cada noite
- Soma das noites (subtotal)
- Desconto aplicado
- Total final

## Dados em Memória

Atualmente, todas as reservas e mensagens são armazenadas em memória. Se o sistema for reiniciado, todos os dados são perdidos. Para persistência, seria necessário integrar um banco de dados.
