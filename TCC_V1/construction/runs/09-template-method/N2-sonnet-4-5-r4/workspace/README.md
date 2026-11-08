# Pousada Maré Alta - Sistema de Reservas

Sistema de gestão de reservas para a Pousada Maré Alta.

## Compilação

```bash
mvn clean package
```

## Execução

```bash
java -jar target/mare-alta-1.0.0.jar
```

## Configuração

O sistema utiliza variáveis de ambiente:

- `DB_URL`: URL de conexão JDBC PostgreSQL (padrão: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco (padrão: `postgres`)
- `DB_PASSWORD`: Senha do banco (padrão: `postgres`)
- `PORT`: Porta do servidor HTTP (padrão: `7070`)

## Endpoints da API

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria nova reserva
- `GET /reservations/{id}` - Consulta reserva
- `POST /reservations/{id}/confirmation` - Confirma reserva
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Obtém segunda via do comprovante

### Mensagens
- `GET /messages` - Lista mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro
- `GET /reports/tourism-tax?month=2027-07` - Relatório de taxa de turismo
- `GET /reports/rate-plans?month=2027-07` - Relatório por plano de tarifa

## Estrutura do Projeto

- **domain**: Modelos de domínio (Room, Reservation, Message, etc.)
- **repository**: Acesso ao banco de dados
- **service**: Lógica de negócio
- **report**: Geração de relatórios CSV (padrão Template Method)
- **api**: Controllers REST e DTOs
- **config**: Configuração do banco de dados

## Padrões de Projeto

O sistema utiliza o padrão **Template Method** para geração de relatórios, facilitando a adição de novos tipos de relatório sem modificar o código existente. Cada relatório herda de `ReportGenerator` e implementa os métodos específicos.
