# Sistema de Reservas - Pousada Maré Alta

Sistema completo de gerenciamento de reservas para pousada, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL.

## Funcionalidades

### Quartos
- 5 quartos: 101, 102 (STANDARD), 201, 202 (DELUXE), 301 (SUITE)
- Capacidades: 2, 3 e 4 pessoas respectivamente
- Diárias: R$ 249,90, R$ 389,90 e R$ 579,90

### Precificação
- Alta temporada (15/dez-fim/fev + julho): +20%
- Planos de tarifa:
  - STANDARD: preço normal
  - NON_REFUNDABLE: 15% de desconto, sem reembolso

### Fluxo de Reservas
- Estados: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- Cancelamento (CANCELLED) permitido antes do check-in
- Política de reembolso conforme plano de tarifa

### Notificações
- E-mail de confirmação com comprovante
- Armazenamento em memória para consulta posterior

### Relatórios CSV
1. **Ocupação**: Quartos ocupados em uma noite específica
2. **Receita**: Reservas por mês com valores
3. **Taxa de Turismo**: R$ 4,50 por hóspede/noite
4. **Planos de Tarifa**: Comparativo de receita por plano

## API REST

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria nova reserva
- `GET /reservations/{id}` - Consulta reserva
- `POST /reservations/{id}/confirmation` - Confirma e processa pagamento
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out e gera comprovante
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Lista mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD`
- `GET /reports/revenue?month=YYYY-MM`
- `GET /reports/tourism-tax?month=YYYY-MM`
- `GET /reports/rate-plans?month=YYYY-MM`

## Tecnologias

- Java 21
- Javalin 6.7.0
- PostgreSQL 18
- Maven
- H2 Database (testes)

## Configuração

Variáveis de ambiente:
- `DB_URL`: jdbc:postgresql://host:port/database
- `DB_USER`: usuário do banco
- `DB_PASSWORD`: senha do banco
- `PORT`: porta do servidor (padrão: 7070)

## Build e Execução

```bash
# Build
mvn clean package

# Execução
java -jar target/reservations-1.0.0.jar

# Testes
mvn verify
```

## Arquitetura

```
src/main/java/com/pousadamarealta/
├── model/          # Entidades de domínio
├── repository/     # Acesso a dados
├── service/        # Lógica de negócio
├── web/            # Controllers HTTP
├── db/             # Gerenciamento de banco
├── dto/            # Data Transfer Objects
└── exception/      # Exceções customizadas
```

## Regras de Negócio

- Arredondamento "half to even" em todas as operações monetárias
- Validação de conflitos de reserva
- Validação de capacidade do quarto
- Cálculo automático de noites, subtotal, desconto e total
- Histórico de mensagens enviadas
- Suporte a PostgreSQL e H2 (testes)
