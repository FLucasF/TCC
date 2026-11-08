# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Requisitos

- Java 21
- PostgreSQL 18
- Maven 3.8+

## Configuração

O sistema utiliza variáveis de ambiente para configuração:

- `DB_URL`: URL de conexão JDBC (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT`: Porta do servidor (padrão: 7070)

## Compilação

```bash
mvn clean package
```

Isso gera um JAR executável em `target/reservas-1.0-SNAPSHOT.jar`.

## Execução

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=seu_usuario
export DB_PASSWORD=sua_senha
export PORT=7070

java -jar target/reservas-1.0-SNAPSHOT.jar
```

## Estrutura

O sistema está organizado seguindo os princípios de design do CLAUDE.md:

- **domain**: Entidades de negócio (Room, Reservation, Message)
- **repository**: Acesso a dados com PostgreSQL
- **service**: Lógica de negócio
- **pricing**: Cálculo de preços e tarifas
- **report**: Geração de relatórios CSV (estrutura modular)
- **api**: Controllers REST

### Arquitetura de Relatórios

Todos os relatórios herdam de `CsvReport`, que define a estrutura comum:
- Linha de título com nome da pousada, tipo de relatório e período
- Linha de cabeçalho
- Linhas de dados
- Linha de total

Cada relatório específico implementa apenas suas particularidades (colunas, dados, cálculos).

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria nova reserva
- `GET /reservations/{id}` - Consulta reserva
- `POST /reservations/{id}/confirmation` - Confirma reserva
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `POST /reservations/{id}/check-in` - Realiza check-in
- `POST /reservations/{id}/check-out` - Realiza check-out
- `GET /reservations/{id}/document` - Obtém comprovante (segunda via)

### Mensagens
- `GET /messages` - Lista mensagens enviadas

### Relatórios (CSV)
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro
- `GET /reports/tourism-tax?month=2027-07` - Relatório de taxa de turismo
- `GET /reports/rate-plans?month=2027-07` - Relatório por plano de tarifa

## Regras de Negócio

### Quartos
- 101, 102: STANDARD (2 pessoas, R$ 249,90)
- 201, 202: DELUXE (3 pessoas, R$ 389,90)
- 301: SUITE (4 pessoas, R$ 579,90)

### Alta Temporada (+20%)
- 15 de dezembro a fim de fevereiro
- Julho inteiro

### Planos de Tarifa
- STANDARD: Preço normal, reembolsável
- NON_REFUNDABLE: 15% de desconto, não reembolsável

### Cálculos
Todos os valores monetários são arredondados com RoundingMode.HALF_EVEN (meio para o par) a cada passo.

### Estados da Reserva
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
       ↘ CANCELLED (apenas antes do check-in)

## Testes

```bash
mvn test
mvn verify
```
