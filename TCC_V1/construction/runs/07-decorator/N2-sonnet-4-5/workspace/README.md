# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para pousada, com suporte a:
- Criação e gerenciamento de reservas
- Cálculo de preços com alta temporada e descontos
- Sistema de mensagens com regras plugáveis
- Relatórios de ocupação e financeiros

## Tecnologias

- Java 21
- Javalin 6.7.0
- PostgreSQL 18
- Maven

## Configuração

O sistema requer as seguintes variáveis de ambiente:

- `DB_URL`: URL JDBC do PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT` (opcional): Porta do servidor (padrão: 7070)
- `HOTEL_TEST_MODE` (opcional): Modo de teste (`true`/`false`, padrão: `false`)

## Build e Execução

```bash
# Compilar
mvn package

# Executar
java -jar target/reservations-1.0-SNAPSHOT.jar
```

## API

### Quartos

- `GET /rooms`: Lista todos os quartos

### Reservas

- `POST /reservations`: Cria uma nova reserva
- `GET /reservations/{id}`: Consulta uma reserva
- `POST /reservations/{id}/confirmation`: Confirma uma reserva
- `POST /reservations/{id}/cancellation`: Cancela uma reserva
- `POST /reservations/{id}/check-in`: Faz check-in
- `POST /reservations/{id}/check-out`: Faz check-out e retorna comprovante
- `GET /reservations/{id}/document`: Obtém segunda via do comprovante

### Mensagens

- `GET /messages`: Lista todas as mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=YYYY-MM-DD`: Relatório de ocupação
- `GET /reports/revenue?month=YYYY-MM`: Relatório financeiro

## Arquitetura

O sistema foi construído seguindo os princípios de design orientado a objetos:

### Regras de Mensagens

O sistema de mensagens utiliza um **pipeline de regras** (variante do padrão Chain of Responsibility) que permite adicionar novas regras de processamento sem modificar o código existente.

Cada regra (`MessageRule`) processa a mensagem sequencialmente e pode:
- Bloquear o envio (validação de e-mail)
- Modificar a mensagem (modo de teste)
- Gerar mensagens adicionais (cópia para arquivo)

As regras são configuradas na inicialização da aplicação, permitindo que diferentes instalações tenham combinações diferentes de regras.

### Cálculo de Preços

- Alta temporada: 15/dez a fim de fevereiro + julho inteiro (+20%)
- Plano não reembolsável: 15% de desconto
- Arredondamento bancário (HALF_EVEN) em cada etapa

### Transições de Estado

Reservas seguem o fluxo:
- PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- PENDING/CONFIRMED → CANCELLED

## Testes

```bash
mvn verify
```

Os testes de integração utilizam Testcontainers para criar um banco PostgreSQL temporário.
