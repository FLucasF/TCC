# Documentação de Implementação - Sistema de Reservas Pousada Maré Alta

## Visão Geral

Sistema completo de gerenciamento de reservas para pousada pequena no litoral, construído em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Estrutura do Projeto

```
src/main/java/com/pousada/marealta/
├── Application.java                      # Aplicação principal
├── domain/                               # Modelos de domínio
│   ├── Room.java
│   ├── RoomType.java
│   ├── Reservation.java
│   ├── RatePlan.java
│   ├── ReservationStatus.java
│   ├── Message.java
│   ├── MessageChannel.java
│   └── MessageStatus.java
├── service/                              # Lógica de negócio
│   ├── ReservationService.java
│   ├── PriceCalculator.java
│   └── ReportService.java
├── infrastructure/
│   ├── persistence/                      # Persistência em BD
│   │   ├── Database.java
│   │   ├── RoomRepository.java
│   │   ├── ReservationRepository.java
│   │   └── MessageRepository.java
│   └── messaging/                        # Sistema de mensagens
│       ├── MessagePolicy.java            # Interface de políticas
│       ├── MessageContext.java
│       ├── MessagePolicyResult.java
│       ├── EmailValidationPolicy.java
│       ├── TestModePolicy.java
│       └── MessageSender.java
├── api/                                  # Controllers/Endpoints
│   ├── RoomController.java
│   ├── ReservationController.java
│   ├── MessageController.java
│   └── ReportController.java
└── util/                                 # Utilitários
    ├── MoneyRounder.java
    ├── MoneyFormatter.java
    ├── DateFormatter.java
    ├── EmailValidator.java
    └── SeasonCalculator.java
```

## Quartos

### Tipos e Configuração

| Número | Tipo     | Capacidade | Diária    |
|--------|----------|------------|-----------|
| 101    | STANDARD | 2 pessoas  | R$ 249,90 |
| 102    | STANDARD | 2 pessoas  | R$ 249,90 |
| 201    | DELUXE   | 3 pessoas  | R$ 389,90 |
| 202    | DELUXE   | 3 pessoas  | R$ 389,90 |
| 301    | SUITE    | 4 pessoas  | R$ 579,90 |

Quartos inseridos automaticamente na inicialização do banco de dados.

## Preço e Temporada

### Cálculo de Preço

1. Para cada noite da reserva, calcula-se o valor base ajustado pela temporada
2. Cada valor de noite é arredondado individualmente
3. Os valores são somados (com arredondamento)
4. Aplica-se o desconto (com arredondamento)
5. Calcula-se o total (com arredondamento)

Arredondamento utiliza o método HALF_EVEN (meios para pares), conforme especificado.

### Alta Temporada

- 15 de dezembro até 28 de fevereiro: +20%
- Mês de julho inteiro: +20%

Fora desses períodos: preço normal

## Planos de Tarifa

- **STANDARD**: Sem desconto. Cancelamento com reembolso integral.
- **NON_REFUNDABLE**: 15% de desconto. Cancelamento sem reembolso.

## Estados da Reserva

1. **PENDING**: Criada, aguardando pagamento
2. **CONFIRMED**: Confirmada com pagamento, comprovante enviado
3. **CHECKED_IN**: Hóspede realizou check-in
4. **CHECKED_OUT**: Hóspede realizou check-out
5. **CANCELLED**: Cancelada (com ou sem reembolso conforme plano)

### Transições de Estado

```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
  ↓                      ↓
CANCELLED            CANCELLED
```

- PENDING pode ir para CONFIRMED ou CANCELLED
- CONFIRMED pode ir para CHECKED_IN ou CANCELLED
- CHECKED_IN pode ir para CHECKED_OUT ou CANCELLED (apenas por erro)
- CHECKED_OUT e CANCELLED são estados finais

## Sistema de Mensagens

### Validação de E-mail

Endereço válido deve ter:
- Exatamente um `@`
- Algo antes do `@`
- Domínio com pelo menos um ponto (ex: gmail.com)

E-mails inválidos ficam com status BLOCKED na base de dados.

### Políticas de Mensagem

Sistema extensível de políticas implementado via padrão Strategy:

1. **EmailValidationPolicy**: Valida formato do e-mail
2. **TestModePolicy**: Redireciona para modo teste se ativado

Políticas podem ser adicionadas dinamicamente via `MessageSender.addPolicy()`.

### Modo de Teste (HOTEL_TEST_MODE=true)

- Todas as mensagens são redirecionadas para `testes@pousadamarealta.com.br`
- Assunto recebe prefixo `[TESTE] `
- Primeira linha do corpo: "Destinatário original: {email_original}"
- Não gera cópia para arquivo

### Cópia para Arquivo (HOTEL_TEST_MODE=false)

- Mensagens SENT geram cópia automática para `arquivo@pousadamarealta.com.br`
- Cópia tem assunto com prefixo `[Cópia] `
- Cópia aparece como mensagem separada na lista

## Comprovante de Reserva

Formato de texto:

```
Comprovante de reserva nº {id}
Hóspede: {nome} (CPF {cpf})
Quarto: {número} ({tipo})
Entrada: {data} | Saída: {data} | {noites} diária{s}
Diárias: {subtotal}
[Desconto: {desconto}]
Total: {total}
```

Enviado por e-mail ao confirmar a reserva.
Disponível para segunda via em GET /reservations/{id}/document apenas para reservas confirmadas.

## Comprovante de Confirmação por E-mail

```
Olá, {nome}!

Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

{comprovante}

A entrada é a partir das 14h, e a saída, até as 12h.
Até breve!
```

## Relatórios

### Ocupação (CSV)

Endpoint: `GET /reports/occupancy?date=2027-07-10`

```csv
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-07-10,2027-07-12
301,SUITE,2,2027-07-10,2027-07-15
total,2/5
```

Mostra:
- Reservas CONFIRMED, CHECKED_IN ou CHECKED_OUT
- Na data especificada (quem entra naquela noite conta, quem sai não)
- Ordenadas por número de quarto

### Financeiro (CSV)

Endpoint: `GET /reports/revenue?month=2027-07`

```csv
reservation,room,check_in,check_out,status,amount
1,201,2027-07-10,2027-07-12,CONFIRMED,599.76
2,301,2027-07-10,2027-07-15,CHECKED_OUT,999.99
total,,,,,1599.75
```

Mostra:
- Reservas que começam no mês especificado
- Status que deixaram dinheiro com a pousada (CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED)
- Dinheiro líquido = pagamento - reembolso
- Ordenadas por data de entrada, depois por ID

## Validações

- Quarto deve existir (ROOM_NOT_FOUND)
- Hóspedes: 1 ≤ guests ≤ capacidade (CAPACITY_EXCEEDED se exceder)
- Datas: checkOut > checkIn (INVALID_REQUEST se inválido)
- Disponibilidade: quarto não pode ter outra reserva no mesmo período (ROOM_UNAVAILABLE)
- Operações: apenas transições válidas de estado (OPERATION_NOT_ALLOWED)

## Persistência em Banco de Dados

### Tabelas

#### rooms
```sql
CREATE TABLE rooms (
    number INTEGER PRIMARY KEY,
    type VARCHAR(50) NOT NULL
)
```

#### reservations
```sql
CREATE TABLE reservations (
    id SERIAL PRIMARY KEY,
    room_number INTEGER NOT NULL,
    guest_name VARCHAR(255) NOT NULL,
    guest_email VARCHAR(255) NOT NULL,
    guest_cpf VARCHAR(20) NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    guests INTEGER NOT NULL,
    rate_plan VARCHAR(50) NOT NULL,
    nights INTEGER NOT NULL,
    subtotal NUMERIC(10, 2) NOT NULL,
    discount NUMERIC(10, 2) NOT NULL,
    total NUMERIC(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0,
    refund NUMERIC(10, 2),
    FOREIGN KEY (room_number) REFERENCES rooms(number)
)
```

#### messages
```sql
CREATE TABLE messages (
    id SERIAL PRIMARY KEY,
    channel VARCHAR(50) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(50) NOT NULL
)
```

Criadas automaticamente na inicialização se não existirem.

## Endpoints da API

### Quartos

```
GET /rooms
```

Retorna lista com todos os quartos disponíveis.

### Reservas

```
POST /reservations
GET /reservations/{id}
POST /reservations/{id}/confirmation
POST /reservations/{id}/cancellation
POST /reservations/{id}/check-in
POST /reservations/{id}/check-out
GET /reservations/{id}/document
```

### Mensagens

```
GET /messages
```

Lista todas as mensagens enviadas e bloqueadas.

### Relatórios

```
GET /reports/occupancy?date={date}
GET /reports/revenue?month={month}
```

## Decisões de Design

### Arquitetura em Camadas

- **domain**: Modelos imutáveis de negócio
- **service**: Lógica de negócio centralizada
- **infrastructure**: Detalhe técnico (BD, mensagens)
- **api**: Apenas conversão HTTP ↔ objetos

### Padrão Strategy para Políticas de Mensagem

Permite extensibilidade sem modificar código existente. Novas políticas podem ser adicionadas sem tocar em ReservationService ou ReservationController.

### Imutabilidade de Modelos

Todas as classes de domínio são imutáveis. Transições de estado criam novas instâncias, facilitando auditoria e prevenindo bugs.

### Separação de Responsabilidades

- ReservationService: Lógica de negócio pura
- ReservationRepository: Apenas persistência
- MessageSender: Apenas envio com políticas

Cada classe tem uma razão única para mudar.

## Tratamento de Erros

### Códigos HTTP

- 201: Recurso criado (POST /reservations)
- 400: Requisição inválida (INVALID_REQUEST)
- 404: Recurso não encontrado (ROOM_NOT_FOUND, RESERVATION_NOT_FOUND)
- 409: Conflito (ROOM_UNAVAILABLE, OPERATION_NOT_ALLOWED)
- 422: Entidade não processável (CAPACITY_EXCEEDED)
- 500: Erro interno (INTERNAL_ERROR)

### Formato de Erro

```json
{
  "error": "CODIGO",
  "message": "Descrição do erro"
}
```

## Testes

Testes unitários para:
- MoneyRounder (arredondamento HALF_EVEN)
- EmailValidator (validação de e-mail)
- SeasonCalculator (períodos de alta temporada)

Executar com: `mvn test`

## Compilação e Execução

```bash
# Compilar
mvn clean compile

# Testar
mvn test

# Empacotar
mvn clean package

# Executar
export DB_URL="jdbc:postgresql://localhost:5432/marealta"
export DB_USER="marealta"
export DB_PASSWORD="password123"
export PORT="7070"
export HOTEL_TEST_MODE="false"
java -jar target/mare-alta.jar
```

## Próximas Evoluções (Sugestões)

1. **Limite de mensagens por hóspede/dia**: Implementar como nova MessagePolicy
2. **Não mandar e-mails à noite**: Implementar como nova MessagePolicy
3. **Autenticação na API**: Adicionar JWT/OAuth2
4. **Dashboard de administração**: Interface web para gerência
5. **Integração com gateway de pagamento**: Validar pagamentos
6. **Notificações em tempo real**: WebSocket para atualizações
7. **Gestão de temporada customizável**: Interface para definir períodos
