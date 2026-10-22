# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Instalação

### Pré-requisitos
- Java 21
- PostgreSQL 18
- Maven 3.8+

### Variáveis de Ambiente

```bash
# Banco de dados (obrigatórios)
export DB_URL=jdbc:postgresql://localhost:5432/marealta
export DB_USER=postgres
export DB_PASSWORD=password

# Servidor (opcional, padrão: 7070)
export PORT=7070

# Modo de teste (opcional)
export HOTEL_TEST_MODE=true
```

### Build e execução

```bash
# Compilar e executar testes
mvn clean verify

# Gerar JAR executável
mvn clean package

# Executar
java -jar target/mare-alta.jar
```

## Quartos

- **101, 102**: STANDARD (2 pessoas) - R$ 249,90/noite
- **201, 202**: DELUXE (3 pessoas) - R$ 389,90/noite  
- **301**: SUITE (4 pessoas) - R$ 579,90/noite

## Tarifa

### Alta Temporada
- 15 de dezembro a 28 de fevereiro
- Mês de julho
- Acréscimo de 20% na diária

### Planos de Tarifa
- **STANDARD**: Sem desconto, reembolso total em caso de cancelamento
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso em caso de cancelamento

### Arredondamento
Todos os valores são arredondados para centavos com o método "meio para o par" (HALF_EVEN) em cada etapa: preço de cada noite, soma das noites, desconto e total.

## API

### Quartos
```
GET /rooms
```

### Reservas
```
POST /reservations
  - roomNumber, guestName, guestEmail, guestCpf
  - checkIn, checkOut (formato: YYYY-MM-DD)
  - guests, ratePlan (STANDARD ou NON_REFUNDABLE)
  
GET /reservations/{id}

POST /reservations/{id}/confirmation
  - amountPaid

POST /reservations/{id}/cancellation

POST /reservations/{id}/check-in

POST /reservations/{id}/check-out

GET /reservations/{id}/document (segunda via)
```

### Mensagens
```
GET /messages
```

### Relatórios
```
GET /reports/occupancy?date=YYYY-MM-DD
GET /reports/revenue?month=YYYY-MM
```

## Estados da Reserva

- **PENDING**: Recém-criada, aguardando confirmação
- **CONFIRMED**: Confirmada com pagamento recebido
- **CHECKED_IN**: Hóspede já fez check-in
- **CHECKED_OUT**: Hóspede realizou check-out
- **CANCELLED**: Cancelada (antes do check-in)

## Validações de Email

O sistema valida endereços de email segundo as regras:
- Deve ter exatamente um caractere `@`
- Deve ter algo antes do `@`
- Deve ter um domínio após o `@` com pelo menos um ponto no meio

### Modo de Teste (HOTEL_TEST_MODE=true)
- Todos os emails são redirecionados para `testes@pousadamarealta.com.br`
- O assunto recebe o prefixo `[TESTE]`
- O corpo recebe a primeira linha: `Destinatário original: <email_original>`
- Não gera cópia para arquivo

### Modo Normal
- Emails válidos são enviados normalmente
- Emails bloqueados aparecem com status BLOCKED na lista de mensagens
- Cada email enviado gera uma cópia automática para `arquivo@pousadamarealta.com.br` com prefixo `[Cópia]`

## Testes

```bash
mvn test
```

Os testes cobrem:
- Cálculo de preços e arredondamento
- Validação de emails
- Estados e transições de reservas
