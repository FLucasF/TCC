# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Requisitos

- Java 21
- PostgreSQL 18+
- Maven 3.6+

## Configuração

O sistema utiliza variáveis de ambiente para configuração:

- `DB_URL`: URL de conexão JDBC do PostgreSQL (padrão: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados (padrão: `postgres`)
- `DB_PASSWORD`: Senha do banco de dados (padrão: `postgres`)
- `PORT`: Porta do servidor HTTP (padrão: `7070`)

## Como executar

### Com Docker Compose

```bash
docker-compose up -d
mvn package
java -jar target/pousada-reservas-1.0.0.jar
```

### Sem Docker (PostgreSQL já instalado)

```bash
# Configure as variáveis de ambiente
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=seu_usuario
export DB_PASSWORD=sua_senha

# Compile e execute
mvn package
java -jar target/pousada-reservas-1.0.0.jar
```

## Estrutura do Projeto

O projeto segue princípios de design orientado a objetos, organizando o código para que:
- Cada novo caso entre em um lugar só
- Sequências repetidas são escritas uma vez
- Variações existem apenas onde necessárias

### Pacotes

- `domain`: Entidades de domínio (Room, Reservation, Message)
- `repository`: Acesso a dados
- `service`: Lógica de negócio
- `pricing`: Cálculos de preços e tarifas
- `report`: Geração de relatórios CSV
- `api`: Controllers e DTOs da API REST

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Busca uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Obtém comprovante

### Mensagens
- `GET /messages` - Lista mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro
- `GET /reports/tourism-tax?month=YYYY-MM` - Relatório de taxa de turismo
- `GET /reports/rate-plans?month=YYYY-MM` - Relatório por plano de tarifa

## Testes

```bash
mvn test
```

## Build

```bash
mvn package
```

O comando gera um JAR executável em `target/pousada-reservas-1.0.0.jar`.
