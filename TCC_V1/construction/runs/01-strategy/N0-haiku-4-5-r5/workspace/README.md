# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta.

## Build

```bash
mvn clean package
```

Isso gera um JAR executável em `target/pousada-system-1.0.0.jar`.

## Execução

```bash
DB_URL="jdbc:postgresql://localhost:5432/pousada" \
DB_USER="postgres" \
DB_PASSWORD="password" \
PORT=7070 \
java -jar target/pousada-system-1.0.0.jar
```

As variáveis de ambiente:
- `DB_URL`: URL do PostgreSQL (obrigatória)
- `DB_USER`: Usuário do banco (obrigatória)
- `DB_PASSWORD`: Senha do banco (obrigatória)
- `PORT`: Porta do servidor (padrão: 7070)

## Recursos

### Quartos
- **101, 102**: STANDARD - 2 pessoas - R$ 249,90/noite
- **201, 202**: DELUXE - 3 pessoas - R$ 389,90/noite
- **301**: SUITE - 4 pessoas - R$ 579,90/noite

### Planos de Tarifa
1. **STANDARD**: Reembolso total
2. **NON_REFUNDABLE**: 15% desconto, sem reembolso
3. **CORPORATE**: Desconto por empresa (TECNOSUL 12%, AGROVALE 18%, FARMAVIDA 25%)
4. **WEEKEND_PACKAGE**: Preço fixo sexta a domingo
5. **LONG_STAY**: 10% desconto para 7+ noites

### Alta Temporada
- 15 de dezembro a 28 de fevereiro
- Mês de julho inteiro
- +20% no valor das diárias

## API

### Quartos
`GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Criar reserva
- `GET /reservations/{id}` - Obter reserva
- `POST /reservations/{id}/confirmation` - Confirmar pagamento
- `POST /reservations/{id}/cancellation` - Cancelar
- `POST /reservations/{id}/check-in` - Check-in
- `POST /reservations/{id}/check-out` - Check-out
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
`GET /messages` - Listar e-mails enviados

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Ocupação do dia
- `GET /reports/revenue?month=YYYY-MM` - Faturamento do mês
