# Sistema de Reservas - Pousada Maré Alta

Sistema de gestão de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Funcionalidades

- **Gestão de Quartos**: 5 quartos (2 Standard, 2 Deluxe, 1 Suíte)
- **Reservas**: Criação, confirmação, check-in, check-out e cancelamento
- **Planos de Tarifa**: Standard e Não Reembolsável (15% de desconto)
- **Alta Temporada**: Acréscimo de 20% nas diárias (15/dez a fim de fev + julho)
- **Notificações**: E-mails de confirmação armazenados no sistema
- **Relatórios CSV**:
  - Ocupação por data
  - Receita mensal
  - Taxa de turismo
  - Análise por plano de tarifa

## Requisitos

- Java 21
- Maven 3.6+
- PostgreSQL 18

## Compilação

```bash
mvn clean package
```

O comando gera um JAR executável em `target/mare-alta-1.0.0.jar`.

## Execução

Configure as variáveis de ambiente e execute:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/marealba"
export DB_USER="postgres"
export DB_PASSWORD="postgres"
export PORT=7070  # opcional, padrão é 7070

java -jar target/mare-alta-1.0.0.jar
```

## Banco de Dados

O sistema cria automaticamente as tabelas necessárias na primeira execução. Para facilitar o desenvolvimento, use Docker Compose:

```bash
docker-compose up -d
```

Isso inicia um PostgreSQL na porta 5432 com o banco `marealba_test`.

## API REST

### Quartos

**GET /rooms** - Lista todos os quartos

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

**POST /reservations** - Cria uma reserva

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

**GET /reservations/{id}** - Consulta uma reserva

**POST /reservations/{id}/confirmation** - Confirma uma reserva pendente

**POST /reservations/{id}/cancellation** - Cancela uma reserva

**POST /reservations/{id}/check-in** - Faz check-in de uma reserva confirmada

**POST /reservations/{id}/check-out** - Faz check-out e retorna o comprovante

**GET /reservations/{id}/document** - Consulta a segunda via do comprovante

### Mensagens

**GET /messages** - Lista todos os e-mails enviados

### Relatórios

Todos os relatórios são retornados em formato CSV com charset UTF-8.

**GET /reports/occupancy?date=2027-07-10** - Relatório de ocupação por data

**GET /reports/revenue?month=2027-07** - Relatório financeiro mensal

**GET /reports/tourism-tax?month=2027-07** - Relatório de taxa de turismo

**GET /reports/rate-plans?month=2027-07** - Relatório por plano de tarifa

## Regras de Negócio

### Cálculo de Preços

1. O preço de cada noite é calculado com base na diária do quarto
2. Se a data estiver em alta temporada, aplica-se 20% de acréscimo
3. O subtotal é a soma das noites (arredondado a cada passo)
4. O desconto de 15% é aplicado sobre o subtotal (plano NON_REFUNDABLE)
5. O total final é subtotal - desconto
6. Todos os valores usam arredondamento "half even" (para o par mais próximo)

### Estados da Reserva

- **PENDING**: Reserva criada, aguardando confirmação
- **CONFIRMED**: Reserva paga, aguardando check-in
- **CHECKED_IN**: Hóspede presente na pousada
- **CHECKED_OUT**: Estadia finalizada
- **CANCELLED**: Reserva cancelada

### Cancelamento

- Plano STANDARD: reembolso total se cancelado antes do check-in
- Plano NON_REFUNDABLE: sem reembolso
- Não é possível cancelar após check-in

### Validações

- Check-out deve ser posterior ao check-in
- Número de hóspedes entre 1 e a capacidade do quarto
- Quarto não pode ter duas reservas na mesma noite (exceto canceladas)

## Testes

Execute os testes com:

```bash
mvn verify
```

## Estrutura do Projeto

```
src/main/java/com/pousada/marealba/
├── Main.java                 # Ponto de entrada
├── config/
│   └── Database.java         # Configuração do banco
├── model/                    # Modelos de domínio
├── repository/               # Acesso a dados
├── service/                  # Lógica de negócio
├── controller/               # Endpoints REST
├── exception/                # Tratamento de erros
└── util/                     # Utilitários
```

## Códigos de Erro

- **400 INVALID_REQUEST**: Requisição inválida (campos faltando, datas inválidas, etc.)
- **404 ROOM_NOT_FOUND**: Quarto não encontrado
- **404 RESERVATION_NOT_FOUND**: Reserva não encontrada
- **409 ROOM_UNAVAILABLE**: Quarto já reservado para o período
- **409 OPERATION_NOT_ALLOWED**: Operação não permitida pelo estado da reserva
- **422 CAPACITY_EXCEEDED**: Número de hóspedes excede capacidade do quarto

## Licença

Propriedade da Pousada Maré Alta.
