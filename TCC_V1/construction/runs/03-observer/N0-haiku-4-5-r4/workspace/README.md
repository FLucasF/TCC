# Sistema de Reservas - Pousada Maré Alta

## Visão Geral

Sistema completo de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

### Funcionalidades Principais

- **Gerenciamento de Reservas**: Criação, confirmação, check-in, check-out e cancelamento de reservas
- **Cálculo Dinâmico de Preços**: Considera alta temporada (dez-fev e julho) com aumento de 20%
- **Planos de Tarifa**: STANDARD (normal) e NON_REFUNDABLE (15% desconto)
- **Sistema de Notificações**: E-mails automáticos para hóspede, governança e financeiro
- **Relatórios**: Ocupação e receita em CSV
- **Arredondamento Monetário**: "Meio para o par" em todas operações

## Estrutura dos Quartos

- **Quartos 101-102** (STANDARD): 2 pessoas, R$ 249,90/noite
- **Quartos 201-202** (DELUXE): 3 pessoas, R$ 389,90/noite
- **Quarto 301** (SUITE): 4 pessoas, R$ 579,90/noite

## Compilação e Execução

### Requisitos
- Java 21
- Maven 3.9+
- PostgreSQL 18+

### Build

```bash
mvn clean package
```

Gera um arquivo JAR executável em `target/reservas-1.0.0.jar`.

### Execução

```bash
DB_URL="jdbc:postgresql://localhost:5432/pousada" \
DB_USER="postgres" \
DB_PASSWORD="senha" \
PORT=7070 \
java -jar target/reservas-1.0.0.jar
```

### Variáveis de Ambiente

- `DB_URL`: URL de conexão PostgreSQL (obrigatória)
- `DB_USER`: Usuário PostgreSQL (obrigatória)
- `DB_PASSWORD`: Senha PostgreSQL (obrigatória)
- `PORT`: Porta da aplicação (padrão: 7070)

## API

### Quartos

**GET /rooms**
```json
[
  {
    "number": 101,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  }
]
```

### Reservas

**POST /reservations**
```json
{
  "roomNumber": 101,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

Retorna: 201 com objeto de reserva

**GET /reservations/{id}**
Retorna detalhes da reserva

**POST /reservations/{id}/confirmation**
```json
{
  "amountPaid": 599.76
}
```
Confirma pagamento e envia notificações

**POST /reservations/{id}/check-in**
Registra entrada do hóspede

**POST /reservations/{id}/check-out**
Registra saída e retorna comprovante

**POST /reservations/{id}/cancellation**
Cancela reserva (com reembolso se aplicável)

**GET /reservations/{id}/document**
Segunda via do comprovante

### Mensagens

**GET /messages**
Lista todas as mensagens de e-mail enviadas

### Relatórios

**GET /reports/occupancy?date=2027-07-10**
Relatório de ocupação (CSV)

**GET /reports/revenue?month=2027-07**
Relatório financeiro (CSV)

## Tratamento de Erros

### Códigos HTTP

- `400 INVALID_REQUEST`: Dados inválidos ou faltando
- `404 ROOM_NOT_FOUND`: Quarto não existe
- `404 RESERVATION_NOT_FOUND`: Reserva não existe
- `409 ROOM_UNAVAILABLE`: Quarto indisponível nas datas
- `409 OPERATION_NOT_ALLOWED`: Operação não permitida para este estado
- `422 CAPACITY_EXCEEDED`: Mais hóspedes que a capacidade do quarto

## Sistema de Notificações

### Confirmação de Reserva

- **Hóspede**: Recebe e-mail com comprovante
- **Governança** (governanca@pousadamarealta.com.br): Data de entrada/saída e número de hóspedes
- **Financeiro** (financeiro@pousadamarealta.com.br): Valor do pagamento recebido

### Cancelamento

- **Governança**: Aviso das datas liberadas (se foi confirmada)
- **Financeiro**: Valor do reembolso (apenas se houver devolução)

### Extensibilidade

O sistema foi projetado para fácil adição de novos notificadores (restaurante, transfer, etc) através da classe `NotificationService`.

## Testes

```bash
mvn test
```

Executa testes unitários de preço e lógica de cálculo.

## Banco de Dados

O banco é inicializado automaticamente na primeira execução com:
- Tabelas para quartos, reservas e mensagens
- Índices para performance
- Seed dos dados de quartos

## Notas de Implementação

- Transações ACID garantidas pelo PostgreSQL
- Arredondamento consistente com `RoundingMode.HALF_EVEN`
- Datas em UTC via PostgreSQL
- Sem validação de CPF (apenas string)
- Sem autenticação (integração com sistema de autenticação externo esperada)
