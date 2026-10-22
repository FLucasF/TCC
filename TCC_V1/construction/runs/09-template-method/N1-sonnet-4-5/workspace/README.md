# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para pousadas, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Tecnologias

- Java 21
- Javalin 6.7.0
- PostgreSQL 18
- Maven

## Configuração

O sistema utiliza as seguintes variáveis de ambiente:

- `DB_URL`: URL de conexão JDBC do PostgreSQL (padrão: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados (padrão: `postgres`)
- `DB_PASSWORD`: Senha do banco de dados (padrão: `postgres`)
- `PORT`: Porta do servidor HTTP (padrão: `7070`)

## Compilação

```bash
mvn clean package
```

Isso gera o arquivo `target/reservations-1.0.0.jar`.

## Execução

```bash
java -jar target/reservations-1.0.0.jar
```

Com variáveis de ambiente customizadas:

```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=postgres \
DB_PASSWORD=senha123 \
PORT=8080 \
java -jar target/reservations-1.0.0.jar
```

## Testes

```bash
mvn verify
```

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria nova reserva
- `GET /reservations/{id}` - Consulta reserva
- `POST /reservations/{id}/confirmation` - Confirma reserva
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Obtém comprovante (segunda via)

### Mensagens

- `GET /messages` - Lista mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro
- `GET /reports/tourism-tax?month=2027-07` - Relatório de taxa de turismo
- `GET /reports/rate-plans?month=2027-07` - Relatório por plano de tarifa

## Estrutura do Projeto

O código segue os princípios de design orientado a objetos com foco em extensibilidade:

- **domain**: Modelos de domínio (Reservation, Room, Message)
- **infrastructure**: Repositórios e acesso a dados
- **service**: Lógica de negócio
- **service/report**: Sistema extensível de relatórios
- **api**: Handlers HTTP e DTOs

### Sistema de Relatórios

O sistema de relatórios foi projetado para ser facilmente extensível. Todos os relatórios compartilham a mesma estrutura base (título, cabeçalho, linhas, total) através da classe abstrata `Report`. Para adicionar um novo relatório:

1. Crie uma classe que estende `Report`
2. Implemente os métodos abstratos
3. Adicione o endpoint no `ApiHandlers` e `Main`

Mudanças que afetam todos os relatórios (como adicionar uma linha de rodapé com a data) são feitas uma única vez na classe base `Report`.

## Regras de Negócio

### Quartos

- 5 quartos: 101, 102 (STANDARD, 2 pessoas), 201, 202 (DELUXE, 3 pessoas), 301 (SUITE, 4 pessoas)
- Diárias: R$ 249,90 (STANDARD), R$ 389,90 (DELUXE), R$ 579,90 (SUITE)

### Alta Temporada

+20% nas diárias durante:
- 15 de dezembro a 28/29 de fevereiro
- Todo o mês de julho

### Planos de Tarifa

- **STANDARD**: Preço normal, reembolsável
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso ao cancelar

### Fluxo de Reserva

PENDING → CONFIRMED (com pagamento) → CHECKED_IN → CHECKED_OUT

Pode cancelar (CANCELLED) antes do check-in.

### Validações

- Não pode haver duas reservas não canceladas no mesmo quarto na mesma noite
- Número de hóspedes deve respeitar capacidade do quarto
- Mínimo de 1 hóspede
- Data de saída deve ser posterior à data de entrada
