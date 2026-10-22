# Sistema de Reservas - Pousada Maré Alta

Sistema completo de gerenciamento de reservas para pousada pequena, construído em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Estrutura do Projeto

- **Modelos**: Quartos, Reservas, Mensagens, Comprovantes
- **Banco de Dados**: PostgreSQL com esquema automático
- **API REST**: Endpoints para reservas, check-in/out, relatórios
- **Lógica de Negócio**: Cálculo de preços com alta temporada, planos de tarifa, validação de emails

## Compilação

```bash
mvn clean verify
```

O comando `mvn package` gera um jar executável em `target/reservas-1.0.0.jar`.

## Execução

```bash
export DB_URL="jdbc:postgresql://localhost:5432/reservas"
export DB_USER="postgres"
export DB_PASSWORD="senha"
export PORT=7070
export HOTEL_TEST_MODE=true  # Opcional, para modo de teste

java -jar target/reservas-1.0.0.jar
```

### Variáveis de Ambiente

- `DB_URL`: URL de conexão do PostgreSQL (obrigatório)
- `DB_USER`: Usuário do banco (obrigatório)
- `DB_PASSWORD`: Senha do banco (obrigatório)
- `PORT`: Porta de execução (padrão: 7070)
- `HOTEL_TEST_MODE`: Modo de teste - emails vão para testes@pousadamarealta.com.br (padrão: false)

## Funcionalidades Principais

### Quartos
- 5 quartos com tipos diferentes (STANDARD, DELUXE, SUITE)
- Tarifa diária configurável
- Alta temporada: 20% acima (15 dez a 28 fev e julho)

### Reservas
- Estados: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- Cancelamento com reembolso conforme plano
- Planos: STANDARD (sem desconto) e NON_REFUNDABLE (15% desconto)

### Mensagens
- Validação de email
- Modo teste para desenvolvimento
- Cópia automática para arquivo
- Status: SENT ou BLOCKED

### Relatórios
- Ocupação por noite em CSV
- Receita por mês em CSV

## API Endpoints

- `GET /rooms` - Lista quartos
- `POST /reservations` - Criar reserva
- `GET /reservations/{id}` - Buscar reserva
- `POST /reservations/{id}/confirmation` - Confirmar
- `POST /reservations/{id}/check-in` - Check-in
- `POST /reservations/{id}/check-out` - Check-out
- `POST /reservations/{id}/cancellation` - Cancelar
- `GET /reservations/{id}/document` - Segunda via
- `GET /messages` - Listar mensagens
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório de receita
