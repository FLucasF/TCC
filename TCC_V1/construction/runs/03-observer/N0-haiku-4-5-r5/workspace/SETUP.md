# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gerenciamento de reservas para pousada, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Requisitos

- Java 21+
- PostgreSQL 18+
- Maven 3.8+

## Build

```bash
mvn clean package
```

Isso gerará um JAR executável em `target/marealta-reservations-1.0.0.jar`.

## Execução

```bash
export DB_URL="jdbc:postgresql://localhost:5432/marealta"
export DB_USER="postgres"
export DB_PASSWORD="sua_senha"
export PORT=7070

java -jar target/marealta-reservations-1.0.0.jar
```

Se a variável `PORT` não for definida, o servidor iniciará na porta 7070.

## Banco de Dados

O sistema cria automaticamente as tabelas necessárias na primeira execução:
- `rooms`: Dados dos 5 quartos da pousada
- `reservations`: Histórico de reservas
- `messages`: Mensagens de notificação enviadas

## API Endpoints

### Quartos
- `GET /rooms`: Lista todos os quartos com tarifa

### Reservas
- `POST /reservations`: Cria uma nova reserva
- `GET /reservations/{id}`: Obtém detalhes de uma reserva
- `POST /reservations/{id}/confirmation`: Confirma uma reserva (pagamento recebido)
- `POST /reservations/{id}/cancellation`: Cancela uma reserva
- `POST /reservations/{id}/check-in`: Registra check-in
- `POST /reservations/{id}/check-out`: Registra check-out (devolve comprovante)
- `GET /reservations/{id}/document`: Segunda via do comprovante

### Notificações
- `GET /messages`: Lista todas as mensagens de notificação enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10`: Relatório de ocupação para um dia específico
- `GET /reports/revenue?month=2027-07`: Relatório financeiro para um mês específico

## Estrutura do Projeto

```
src/main/java/com/pousada/marealta/
├── Application.java                # Entrada principal
├── config/                         # Configuração de banco de dados
├── domain/                         # Modelos de domínio
├── repository/                     # Acesso a dados
├── service/                        # Lógica de negócio
├── controller/                     # Endpoints da API
├── notification/                   # Sistema de notificações (extensível)
└── util/                          # Utilitários
```

## Sistema de Notificações

O sistema é extensível para novos subscribers. Quando uma reserva é confirmada, notificações são enviadas para:
- **Hóspede**: E-mail com comprovante da reserva
- **Governança**: Alerta para preparar o quarto
- **Financeiro**: Confirmação do pagamento recebido

Quando uma reserva confirmada é cancelada:
- **Governança**: Alerta de quarto que ficou disponível
- **Financeiro**: Notificação de reembolso (se aplicável)

Para adicionar novos subscribers, implemente a interface `NotificationListener`.

## Cálculo de Preços

- **Base**: Valor de diária do quarto
- **Alta Temporada** (15/dez-fim fev, mês de julho): +20% na diária
- **Desconto NON_REFUNDABLE**: -15% no total da reserva
- **Arredondamento**: Meio para par (HALF_EVEN) em cada passo

## Testes

```bash
mvn test
```

Os testes unitários verificam o cálculo de preços em diferentes cenários (alta/baixa temporada, planos de tarifa, etc).
