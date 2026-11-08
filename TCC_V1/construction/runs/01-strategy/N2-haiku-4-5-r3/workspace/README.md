# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Requisitos

- Java 21
- PostgreSQL 18
- Maven 3.9+

## Compilação e Empacotamento

```bash
mvn clean package
```

O comando gera um JAR executável em `target/mare-alta-1.0.0.jar`.

## Execução

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="usuario"
export DB_PASSWORD="senha"
export PORT=7070

java -jar target/mare-alta-1.0.0.jar
```

Se a variável `PORT` não for definida, a aplicação usa a porta 7070 por padrão.

## Testes

```bash
mvn test
```

## Arquitetura

- **Strategy Pattern**: Implementado para os planos de tarifa (STANDARD, NON_REFUNDABLE, CORPORATE, WEEKEND_PACKAGE, LONG_STAY)
- **DAO Pattern**: Acesso aos dados através de Data Access Objects
- **REST API**: Endpoints RESTful para operações de reservas

## Estrutura do Projeto

```
src/main/java/com/pousada/
├── Application.java           # Ponto de entrada
├── api/                       # Handlers HTTP
├── database/                  # DAOs e conexão
├── model/                     # Entidades
├── rateplan/                  # Implementações de planos de tarifa
├── service/                   # Lógica de negócio
└── util/                      # Utilitários
```

## Funcionalidades

- ✅ Criação de reservas com validação
- ✅ Confirmação de reservas
- ✅ Check-in e Check-out
- ✅ Cancelamento com reembolso
- ✅ Comprovantes de reserva
- ✅ Envio de emails de confirmação
- ✅ Relatórios de ocupação (CSV)
- ✅ Relatórios financeiros (CSV)
- ✅ Múltiplos planos de tarifa com regras específicas
- ✅ Arredondamento financeiro (banker's rounding)
- ✅ Cálculo de preços com alta temporada

## Endpoints da API

- `GET /rooms` - Lista quartos
- `POST /reservations` - Cria reserva
- `GET /reservations/{id}` - Obtém reserva
- `POST /reservations/{id}/confirmation` - Confirma reserva
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `POST /reservations/{id}/check-in` - Check-in
- `POST /reservations/{id}/check-out` - Check-out
- `GET /reservations/{id}/document` - Segunda via de comprovante
- `GET /messages` - Lista mensagens enviadas
- `GET /reports/occupancy?date=YYYY-MM-DD` - Ocupação por dia
- `GET /reports/revenue?month=YYYY-MM` - Receita por mês
