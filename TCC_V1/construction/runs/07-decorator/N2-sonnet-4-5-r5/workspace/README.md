# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas desenvolvido em Java 21 com Javalin 6.7.0.

## Requisitos

- Java 21
- Maven 3.6+
- PostgreSQL 18

## Compilação

```bash
mvn clean package
```

O comando gera um JAR executável em `target/reservas-1.0.0.jar`.

## Execução

```bash
java -jar target/reservas-1.0.0.jar
```

### Variáveis de Ambiente

- `DB_URL`: URL JDBC do PostgreSQL (padrão: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco (padrão: `postgres`)
- `DB_PASSWORD`: Senha do banco (padrão: `postgres`)
- `PORT`: Porta da aplicação (padrão: `7070`)
- `HOTEL_TEST_MODE`: Ativa modo de teste (`true` ou `false`, padrão: `false`)

Exemplo:

```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=admin \
DB_PASSWORD=senha123 \
PORT=8080 \
java -jar target/reservas-1.0.0.jar
```

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos disponíveis

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out (retorna comprovante)
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `GET /reservations/{id}/document` - Obtém segunda via do comprovante

### Mensagens

- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação para uma data
- `GET /reports/revenue?month=2027-07` - Relatório financeiro para um mês

## Funcionalidades Principais

### Quartos

- 101, 102: STANDARD (2 pessoas, R$ 249,90/noite)
- 201, 202: DELUXE (3 pessoas, R$ 389,90/noite)
- 301: SUITE (4 pessoas, R$ 579,90/noite)

### Alta Temporada

- 15 de dezembro a 28/29 de fevereiro
- Todo o mês de julho
- Acréscimo de 20% na diária

### Planos de Tarifa

- **STANDARD**: Tarifa normal com reembolso total em cancelamento
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso

### Sistema de Mensagens

O sistema usa o padrão **Chain of Responsibility** para processar mensagens com as seguintes regras:

1. **Validação de Email**: Bloqueia mensagens com endereços inválidos
2. **Cópia para Arquivo**: Cria cópia para `arquivo@pousadamarealta.com.br` (modo produção)
3. **Modo Teste**: Redireciona para `testes@pousadamarealta.com.br` (quando `HOTEL_TEST_MODE=true`)

Novas regras podem ser adicionadas implementando a interface `MessageRule` e registrando-as no `MessagePipeline`.

## Arquitetura

O sistema foi desenvolvido seguindo as orientações do `CLAUDE.md`:

- **Chain of Responsibility** para o sistema de mensagens, permitindo que novas regras sejam adicionadas sem modificar código existente
- Separação clara entre camadas (domain, service, database, api)
- Validações e regras de negócio centralizadas nos serviços
- Arredondamento bancário (HALF_EVEN) para valores monetários

## Testes

```bash
mvn test
```

Os testes incluem:
- Cálculo de preços com alta temporada
- Descontos para planos não-reembolsáveis
- Validação de emails
- Regras do pipeline de mensagens
- Modo de teste

## Exemplo de Uso

### Criar uma Reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
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
curl -X POST http://localhost:7070/reservations/1/confirmation
```

Ao confirmar, o sistema envia um email de confirmação com o comprovante.
