# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Compilação

```bash
mvn clean package
```

O processo gera um arquivo JAR executável em `target/mare-alta-1.0.0.jar`.

## Execução

### Variáveis de Ambiente Requeridas

- `DB_URL`: URL de conexão PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT`: Porta da aplicação (padrão: 7070)
- `HOTEL_TEST_MODE`: Define modo teste (opcional, padrão: false)

### Iniciar a aplicação

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="senha"
export PORT=7070
java -jar target/mare-alta-1.0.0.jar
```

## Modo Teste

Para ativar o modo teste (redireciona emails para testes@pousadamarealta.com.br):

```bash
export HOTEL_TEST_MODE=true
```

## Endpoints da API

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Criar nova reserva
- `GET /reservations/{id}` - Obter detalhes da reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva e enviar comprovante
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `POST /reservations/{id}/check-in` - Fazer check-in
- `POST /reservations/{id}/check-out` - Fazer check-out
- `GET /reservations/{id}/document` - Obter segunda via do comprovante

### Mensagens
- `GET /messages` - Lista mensagens enviadas (com status SENT ou BLOCKED)

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - Relatório financeiro (CSV)

## Testes

```bash
mvn verify
```

Executa todos os testes da aplicação. Os testes cobrem:
- Validação de emails
- Cálculo de preços com temporada alta/baixa
- Cálculo de descontos
