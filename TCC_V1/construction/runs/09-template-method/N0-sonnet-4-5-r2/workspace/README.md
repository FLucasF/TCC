# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Requisitos

- Java 21
- PostgreSQL 18 (para produção)
- Maven 3.8+

## Configuração

O sistema utiliza as seguintes variáveis de ambiente:

- `DB_URL`: URL JDBC do banco PostgreSQL (padrão: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco (padrão: `postgres`)
- `DB_PASSWORD`: Senha do banco (padrão: `postgres`)
- `PORT`: Porta HTTP do servidor (padrão: `7070`)

## Build

```bash
mvn clean package
```

Isso gera o JAR executável em `target/reservas-1.0.0.jar`.

## Execução

```bash
java -jar target/reservas-1.0.0.jar
```

Com variáveis de ambiente customizadas:

```bash
DB_URL=jdbc:postgresql://db.example.com:5432/pousada \
DB_USER=pousada \
DB_PASSWORD=senha123 \
PORT=8080 \
java -jar target/reservas-1.0.0.jar
```

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos disponíveis

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Obtém comprovante (segunda via)

### Mensagens

- `GET /messages` - Lista mensagens enviadas

### Relatórios (CSV)

- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro
- `GET /reports/tourism-tax?month=2027-07` - Relatório de taxa de turismo
- `GET /reports/rate-plans?month=2027-07` - Relatório por plano de tarifa

## Testes

```bash
mvn test
```

## Quartos da Pousada

- **101, 102**: STANDARD - 2 pessoas - R$ 249,90/noite
- **201, 202**: DELUXE - 3 pessoas - R$ 389,90/noite
- **301**: SUITE - 4 pessoas - R$ 579,90/noite

## Regras de Negócio

### Alta Temporada
- 15 de dezembro até fim de fevereiro
- Todo o mês de julho
- Acréscimo de 20% sobre a diária

### Planos de Tarifa
- **STANDARD**: Plano normal, com reembolso em caso de cancelamento
- **NON_REFUNDABLE**: Desconto de 15%, sem reembolso

### Status da Reserva
1. **PENDING**: Criada, aguardando pagamento
2. **CONFIRMED**: Paga e confirmada
3. **CHECKED_IN**: Check-in realizado
4. **CHECKED_OUT**: Check-out realizado
5. **CANCELLED**: Cancelada

### Taxa de Turismo
R$ 4,50 por hóspede por noite (para relatórios)

## Estrutura do Projeto

```
src/main/java/com/pousadamarealta/
├── Main.java                      # Aplicação principal
├── controller/                    # Controllers (rotas HTTP)
├── model/                         # Entidades de domínio
├── repository/                    # Acesso a dados
├── service/                       # Lógica de negócio
├── report/                        # Geração de relatórios CSV
└── util/                          # Utilitários
```
