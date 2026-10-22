# Pousada Maré Alta - Sistema de Reservas

Sistema de reservas para Pousada Maré Alta desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Construindo

```bash
mvn clean package
```

## Executando

Defina as variáveis de ambiente necessárias:

```bash
export PORT=7070
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="user"
export DB_PASSWORD="password"
export HOTEL_TEST_MODE=false  # ou "true" para modo teste
```

E execute:

```bash
java -jar target/mare-alta.jar
```

## Testando

```bash
mvn verify
```

## Endpoints

- `GET /rooms` - Lista os quartos
- `POST /reservations` - Cria nova reserva
- `GET /reservations/{id}` - Obtém dados da reserva
- `POST /reservations/{id}/confirmation` - Confirma a reserva
- `POST /reservations/{id}/cancellation` - Cancela a reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out e retorna comprovante
- `GET /reservations/{id}/document` - Obtém segunda via do comprovante
- `GET /messages` - Lista mensagens enviadas
- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro (CSV)

## Funcionalidades

- 5 quartos com tipos e capacidades diferentes
- Cálculo de preços com alta temporada (20% mais caro)
- Dois planos de tarifa (STANDARD e NON_REFUNDABLE com 15% desconto)
- Validação de e-mail e bloqueio de mensagens inválidas
- Modo teste com desvio de e-mails
- Cópia automática de mensagens para arquivo
- Relatórios de ocupação e financeiro em CSV
- Arredondamento "meio para o par" (HALF_EVEN) para todos os valores monetários
