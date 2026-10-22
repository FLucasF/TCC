# Pousada Maré Alta - Sistema de Reservas

Um sistema completo de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java com Javalin, PostgreSQL e Maven.

## Requisitos

- Java 21
- PostgreSQL 18+
- Maven 3.6+

## Configuração

O sistema requer as seguintes variáveis de ambiente:

- `DB_URL`: URL de conexão com PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT`: Porta da aplicação (padrão: 7070)
- `HOTEL_TEST_MODE`: Define o modo de teste (opcional, `true` para ativar)

## Build

```bash
mvn clean package
```

## Execução

```bash
java -jar target/hotel-system.jar
```

Ou com variáveis de ambiente:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="password"
export PORT="7070"
export HOTEL_TEST_MODE="false"

java -jar target/hotel-system.jar
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Obtém detalhes de uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva pendente
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Realiza o check-in
- `POST /reservations/{id}/check-out` - Realiza o check-out
- `GET /reservations/{id}/document` - Obtém a segunda via do comprovante

### Mensagens
- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - Relatório de receita (CSV)

## Funcionalidades

### Gerenciamento de Reservas
- Criar reservas com validação de disponibilidade
- Confirmar, cancelar, fazer check-in e check-out
- Cálculo automático de preços com sazonalidade
- Planos de tarifa (STANDARD e NON_REFUNDABLE)

### Sazonalidade
A alta temporada inclui:
- Mês de julho (inteiro)
- Dezembro a partir do dia 15
- Janeiro e fevereiro (inteiro)

Durante a alta temporada, o preço aumenta 20%.

### Planos de Tarifa
- **STANDARD**: Reembolso total em caso de cancelamento
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso em cancelamento

### E-mails
- Validação de endereço de e-mail (deve conter único @, domínio com pelo menos um ponto)
- Modo de teste: redireciona para testes@pousadamarealta.com.br
- Cópia automática para arquivo@pousadamarealta.com.br (apenas em modo normal)
- Bloqueio de e-mails com endereço inválido

### Arredondamento
Todos os cálculos monetários usam arredondamento "meio para o par" (HALF_EVEN).

### Relatórios
- **Ocupação**: Mostra quartos ocupados em uma data específica
- **Financeiro**: Mostra receita do mês, considerando pagamentos e reembolsos

## Estrutura do Banco de Dados

### Tabelas
- `rooms`: Informações dos quartos
- `reservations`: Dados das reservas
- `messages`: Mensagens enviadas

## Testes

```bash
mvn test
mvn verify
```

## Desenvolvimento

O projeto segue as seguintes práticas:
- Código organizado em pacotes separados (model, service, handler, db, util)
- Uso de prepared statements para evitar SQL injection
- Validação de entrada nos endpoints
- Tratamento de erros com códigos HTTP apropriados
- Logging estruturado

## Quartos Disponíveis

| Número | Tipo | Capacidade | Diária |
|--------|------|-----------|--------|
| 101 | STANDARD | 2 | R$ 249,90 |
| 102 | STANDARD | 2 | R$ 249,90 |
| 201 | DELUXE | 3 | R$ 389,90 |
| 202 | DELUXE | 3 | R$ 389,90 |
| 301 | SUITE | 4 | R$ 579,90 |

## Exemplo de Uso

```bash
# Listar quartos
curl http://localhost:7070/rooms

# Criar uma reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Confirmar uma reserva
curl -X POST http://localhost:7070/reservations/1/confirmation

# Fazer check-in
curl -X POST http://localhost:7070/reservations/1/check-in

# Fazer check-out
curl -X POST http://localhost:7070/reservations/1/check-out

# Ver mensagens enviadas
curl http://localhost:7070/messages

# Obter relatório de ocupação
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"

# Obter relatório de receita
curl "http://localhost:7070/reports/revenue?month=2027-07"
```
