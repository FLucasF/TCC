# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Compilação

```bash
mvn clean package
```

## Execução

O sistema requer as seguintes variáveis de ambiente:

- `DB_URL`: URL de conexão JDBC ao PostgreSQL (formato: `jdbc:postgresql://host:port/database`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT`: Porta para o servidor HTTP (opcional, padrão: 7070)

```bash
java -jar target/reservations-1.0.0.jar
```

## Arquitetura

O projeto segue os princípios do CLAUDE.md, utilizando padrões de projeto para facilitar a extensão:

### Template Method - Identificação de Hóspedes

A geração de documentos (comprovantes, faturas, booking receipts) utiliza o padrão Template Method. Para adicionar um novo tipo de identificação (ex: agência de turismo):

1. Criar uma nova classe que estende `GuestIdentification`
2. Implementar os métodos abstratos para formatar cada seção do documento
3. O novo tipo entra automaticamente no sistema sem modificar código existente

**Localização**: `com.pousadamaralta.domain.identification`

### Strategy - Planos de Tarifa

O cálculo de descontos e reembolsos utiliza o padrão Strategy. Para adicionar um novo plano (ex: EARLY_BIRD):

1. Criar uma nova classe que implementa `RatePlan`
2. Implementar `calculateDiscount()` e `calculateRefund()`
3. Registrar no `RatePlanFactory`

**Localização**: `com.pousadamaralta.domain.rateplan`

## API

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out e retorna o documento
- `GET /reservations/{id}/document` - Segunda via do documento

### Mensagens

- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro (CSV)

## Banco de Dados

O sistema cria automaticamente as tabelas necessárias no PostgreSQL e popula os 5 quartos da pousada.
