# Pousada Maré Alta - Sistema de Reservas

Sistema de reservas completo para pequena pousada no litoral, com suporte a múltiplos planos de tarifa e relatórios gerenciais.

## Características

- **5 Quartos**: 2 Standard, 2 Deluxe, 1 Suite
- **5 Planos de Tarifa**:
  - STANDARD: tarifa normal
  - NON_REFUNDABLE: 15% desconto, sem reembolso
  - CORPORATE: convênio com empresas (Tecnosul 12%, Agrovale 18%, Farmavida 25%)
  - WEEKEND_PACKAGE: pacote fixo de fim de semana (sexta → domingo)
  - LONG_STAY: 7+ noites com 10% desconto
- **Preços com Alta Temporada**: 20% acima de 15/12-28/02 e julho
- **Arredondamento**: Banker's rounding ("meio para o par")
- **Relatórios**: Ocupação por data e receita por mês

## Compilação

```bash
mvn package
```

O JAR executável será gerado em `target/pousada.jar`.

## Execução

Variáveis de ambiente obrigatórias:
- `DB_URL`: String de conexão PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco
- `DB_PASSWORD`: Senha do banco

Variável opcional:
- `PORT`: Porta (padrão: 7070)

```bash
java -jar target/pousada.jar
```

## Exemplo de Uso

```bash
# Listar quartos
curl http://localhost:7070/rooms

# Criar reserva (Standard, 2 noites em julho)
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Maria Silva",
    "guestEmail": "maria@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'

# Confirmar reserva (id 1, pagamento R$ 600,00)
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amount": 600.00}'

# Relatório de ocupação
curl http://localhost:7070/reports/occupancy?date=2027-07-10

# Relatório de receita
curl http://localhost:7070/reports/revenue?month=2027-07
```

## Estrutura da API

### Endpoints

- `GET /rooms` - Lista todos os quartos
- `POST /reservations` - Cria nova reserva
- `GET /reservations/{id}` - Obtém detalhes da reserva
- `POST /reservations/{id}/confirmation` - Confirma pagamento
- `POST /reservations/{id}/check-in` - Efetua check-in
- `POST /reservations/{id}/check-out` - Efetua check-out
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `GET /reservations/{id}/document` - Obtém segunda via do comprovante
- `GET /messages` - Lista mensagens enviadas
- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=YYYY-MM` - Relatório de receita (CSV)

### Códigos de Erro

- `400 INVALID_REQUEST`: Dados inválidos (falta de campo, formato errado, etc)
- `404 ROOM_NOT_FOUND`: Quarto não existe
- `404 RESERVATION_NOT_FOUND`: Reserva não existe
- `409 ROOM_UNAVAILABLE`: Quarto não está disponível nas datas
- `409 OPERATION_NOT_ALLOWED`: Operação não permitida no estado atual
- `422 CAPACITY_EXCEEDED`: Número de hóspedes excede capacidade

## Arquitetura

O sistema usa o padrão **Strategy** para os planos de tarifa, permitindo fácil adição de novos planos sem modificar código existente. Cada plano implementa:
- Validação de requisitos
- Cálculo de preço
- Percentual de reembolso

Dados persistidos em PostgreSQL. Mensagens de e-mail armazenadas para auditoria (não são enviadas realmente).

## Testes

```bash
mvn verify
```

Testes cobrem utilidades de data, arredondamento de valores e validações.
