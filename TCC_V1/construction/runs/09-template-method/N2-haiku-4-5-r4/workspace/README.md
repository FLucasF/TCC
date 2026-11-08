# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta, uma pequena pousada no litoral com 5 quartos.

## Requisitos

- Java 21
- Maven 3.6+
- PostgreSQL 18 ou superior

## Como Compilar

```bash
mvn clean package
```

Isso gerará um JAR executável em `target/mare-alta.jar`.

## Como Executar

### Usando Docker Compose para o PostgreSQL

1. Inicie o banco de dados:
```bash
docker-compose up -d postgres
```

2. Aguarde o banco estar pronto:
```bash
docker-compose exec postgres pg_isready -U pousada_user
```

3. Execute a aplicação:
```bash
java -jar target/mare-alta.jar
```

Variáveis de ambiente obrigatórias:
- `DB_URL`: Conexão JDBC (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco (ex: `pousada_user`)
- `DB_PASSWORD`: Senha do banco (ex: `pousada_pass`)
- `PORT`: Porta da aplicação (padrão: 7070)

Exemplo:
```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=pousada_user
export DB_PASSWORD=pousada_pass
export PORT=7070
java -jar target/mare-alta.jar
```

## API Endpoints

### Quartos

**GET /rooms**
- Retorna lista de todos os quartos
- Resposta: Array de objetos com `number`, `type`, `capacity`, `nightlyRate`

### Reservas

**POST /reservations**
- Cria uma nova reserva
- Body: `roomNumber`, `guestName`, `guestEmail`, `guestCpf`, `checkIn` (yyyy-MM-dd), `checkOut` (yyyy-MM-dd), `guests`, `ratePlan` (STANDARD ou NON_REFUNDABLE)
- Resposta: 201 com objeto da reserva

**GET /reservations/{id}**
- Obtém detalhes de uma reserva
- Resposta: Objeto da reserva

**POST /reservations/{id}/confirmation**
- Confirma uma reserva pendente
- Body: `amountPaid`
- Resposta: Objeto da reserva atualizado

**POST /reservations/{id}/cancellation**
- Cancela uma reserva pendente ou confirmada
- Resposta: Objeto da reserva atualizado

**POST /reservations/{id}/check-in**
- Realiza check-in de uma reserva confirmada
- Resposta: Objeto da reserva atualizado

**POST /reservations/{id}/check-out**
- Realiza check-out de uma reserva em check-in
- Resposta: Comprovante (documento)

**GET /reservations/{id}/document**
- Obtém comprovante de uma reserva confirmada, com check-in ou check-out
- Resposta: Documento/comprovante

### Mensagens

**GET /messages**
- Lista todas as mensagens enviadas
- Resposta: Array de objetos com `id`, `channel`, `to`, `subject`, `body`

### Relatórios

**GET /reports/occupancy?date=2027-07-10**
- Gera relatório de ocupação para uma data específica
- Formato: CSV

**GET /reports/revenue?month=2027-07**
- Gera relatório de faturamento para um mês
- Formato: CSV

**GET /reports/tourism-tax?month=2027-07**
- Gera relatório de taxa de turismo para um mês
- Formato: CSV

**GET /reports/rate-plans?month=2027-07**
- Gera relatório de planos de tarifa para um mês
- Formato: CSV

## Estrutura de Quartos

| Número | Tipo | Capacidade | Diária |
|--------|------|-----------|--------|
| 101-102 | STANDARD | 2 | R$ 249,90 |
| 201-202 | DELUXE | 3 | R$ 389,90 |
| 301 | SUITE | 4 | R$ 579,90 |

## Planos de Tarifa

- **STANDARD**: Plano normal, cancelamento reembolsável
- **NON_REFUNDABLE**: 15% de desconto, cancelamento não reembolsável

## Alta Temporada

- 15 de dezembro a 28 de fevereiro
- Mês de julho inteiro
- Valor: +20% na diária

## Estados da Reserva

1. **PENDING**: Reserva criada, aguardando pagamento
2. **CONFIRMED**: Pagamento recebido
3. **CHECKED_IN**: Hóspede realizou check-in
4. **CHECKED_OUT**: Hóspede realizou check-out
5. **CANCELLED**: Reserva cancelada

## Testes

Execute o script de teste após iniciar a aplicação:

```bash
bash test-api.sh
```

## Notas de Implementação

- Valores monetários são arredondados com modo "HALF_EVEN"
- Emails de confirmação são salvos no banco de dados
- Banco de dados é inicializado automaticamente na primeira execução
- Comprovantes estão disponíveis apenas para reservas confirmadas ou com check-in/out
