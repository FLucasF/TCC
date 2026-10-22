# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Requisitos

- Java 21
- Maven 3.6+
- PostgreSQL 18 (ou Docker)

## Compilação

```bash
mvn clean package
```

Isso gera um JAR executável em `target/mare-alta-reservas-1.0.0.jar`.

## Execução com Docker

### 1. Iniciar PostgreSQL

```bash
docker-compose up -d
```

### 2. Executar a aplicação

```bash
DB_URL="jdbc:postgresql://localhost:5432/pousada" \
DB_USER="pousada" \
DB_PASSWORD="pousada123" \
PORT=7070 \
java -jar target/mare-alta-reservas-1.0.0.jar
```

Ou use o script facilitador:

```bash
chmod +x run.sh
./run.sh
```

## Configuração

A aplicação lê as seguintes variáveis de ambiente:

- `DB_URL`: URL do banco PostgreSQL (padrão: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco (padrão: `postgres`)
- `DB_PASSWORD`: Senha do banco (padrão: vazio)
- `PORT`: Porta do servidor (padrão: `7070`)
- `HOTEL_TEST_MODE`: Modo de teste para e-mails (padrão: `false`)

## API REST

### Quartos

**GET /rooms**
- Lista todos os quartos disponíveis

### Reservas

**POST /reservations**
- Criar nova reserva
- Body: `{ roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan }`
- Retorna: 201 com a reserva criada

**GET /reservations/{id}**
- Obter detalhes de uma reserva

**POST /reservations/{id}/confirmation**
- Confirmar uma reserva (mudar status de PENDING para CONFIRMED)
- Body: `{ amount }` (valor pago)
- Envia e-mail de confirmação ao hóspede

**POST /reservations/{id}/cancellation**
- Cancelar uma reserva
- Devolve o valor pago se aplicável

**POST /reservations/{id}/check-in**
- Fazer check-in (CONFIRMED → CHECKED_IN)

**POST /reservations/{id}/check-out**
- Fazer check-out (CHECKED_IN → CHECKED_OUT)
- Retorna comprovante

**GET /reservations/{id}/document**
- Obter segunda via do comprovante (apenas para reservas confirmadas ou com check-in/out)

### Mensagens

**GET /messages**
- Lista todas as mensagens enviadas com status (SENT ou BLOCKED)

### Relatórios

**GET /reports/occupancy?date=2027-07-10**
- Relatório de ocupação em CSV para uma data

**GET /reports/revenue?month=2027-07**
- Relatório de receita em CSV para um mês

## Sistema de Mensagens

O sistema valida e processa e-mails com as seguintes regras:

1. **Validação de e-mail**: Valida formato básico (user@domain.com)
2. **Modo de teste**: Se `HOTEL_TEST_MODE=true`, redireciona para `testes@pousadamarealta.com.br`
3. **Cópia para arquivo**: Cria cópia para `arquivo@pousadamarealta.com.br` (exceto em modo teste)
4. **Bloqueio**: E-mails inválidos são marcados como BLOCKED

## Testes

```bash
mvn test
```

## Estrutura de Preços

- **STANDARD** (101, 102): R$ 249,90/noite, até 2 pessoas
- **DELUXE** (201, 202): R$ 389,90/noite, até 3 pessoas
- **SUITE** (301): R$ 579,90/noite, até 4 pessoas

### Alta Temporada (20% extra)
- 15 de dezembro até fim de fevereiro
- Julho inteiro

### Planos de Tarifa
- **STANDARD**: Sem desconto, reembolsável 100%
- **NON_REFUNDABLE**: 15% desconto, não reembolsável

## Arredondamento

Todos os valores em reais são arredondados para centavos usando o método "meio para par" (HALF_EVEN), em cada passo:
1. Taxa noturna
2. Subtotal (soma das noites)
3. Desconto
4. Total (subtotal - desconto)

## Código de Exemplos

### Criar Reserva
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Confirmar Reserva
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{ "amount": "599.76" }'
```

## Licença

Propriedade da Pousada Maré Alta
