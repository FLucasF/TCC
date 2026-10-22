# Pousada Maré Alta - Sistema de Reservas

Sistema completo de reservas para a Pousada Maré Alta, construído com Java 21, Javalin 6.7.0 e PostgreSQL 18.

## Configuração

### Variáveis de Ambiente Necessárias

- `DB_URL`: URL de conexão com o PostgreSQL (ex: `jdbc:postgresql://localhost:5432/marealta`)
- `DB_USER`: Usuário do PostgreSQL
- `DB_PASSWORD`: Senha do PostgreSQL
- `PORT`: Porta para o servidor (padrão: 7070)

### Exemplo de Inicialização

```bash
export DB_URL="jdbc:postgresql://localhost:5432/marealta"
export DB_USER="postgres"
export DB_PASSWORD="senha"
export PORT="8080"

java -jar target/mare-alta-1.0.0.jar
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Criar nova reserva
- `GET /reservations/{id}` - Obter detalhes da reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva (pagar)
- `POST /reservations/{id}/check-in` - Fazer check-in
- `POST /reservations/{id}/check-out` - Fazer check-out
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `GET /reservations/{id}/document` - Obter segunda via do comprovante

### Mensagens
- `GET /messages` - Listar mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - Relatório financeiro (CSV)

## Quartos Disponíveis

| Número | Tipo | Capacidade | Diária |
|--------|------|-----------|---------|
| 101    | STANDARD | 2 | R$ 249,90 |
| 102    | STANDARD | 2 | R$ 249,90 |
| 201    | DELUXE | 3 | R$ 389,90 |
| 202    | DELUXE | 3 | R$ 389,90 |
| 301    | SUITE | 4 | R$ 579,90 |

## Identificação de Hóspedes

A reserva pode ser criada com três tipos de identificação:

1. **CPF** (para pessoa física): `guestCpf`
2. **CNPJ** (para empresa): `companyCnpj` + `companyName`
3. **Passaporte** (para estrangeiros): `guestPassport` + `guestCountry`

Apenas um tipo de identificação deve ser fornecido.

## Estados da Reserva

1. **PENDING** - Reserva criada, aguardando pagamento
2. **CONFIRMED** - Reserva paga
3. **CHECKED_IN** - Hóspede fez check-in
4. **CHECKED_OUT** - Hóspede fez check-out
5. **CANCELLED** - Reserva cancelada

## Preço

- **Alta Temporada** (15 dez a 28 fev e julho): +20% no valor da diária
- **Planos**:
  - STANDARD: sem desconto
  - NON_REFUNDABLE: 15% de desconto (não reembolsável)
- **Arredondamento**: "meio para o par" em cada etapa (preço da noite, subtotal, desconto, total)

## Compilação e Testes

```bash
# Compilar
mvn clean compile

# Executar testes
mvn clean test

# Gerar jar (maven-shade para jar auto-executável)
mvn clean package

# Verificar tudo (com testes)
mvn clean verify
```

## Banco de Dados

O sistema cria automaticamente todas as tabelas necessárias ao iniciar:

- `rooms` - Quartos disponíveis
- `reservations` - Dados das reservas
- `messages` - Mensagens enviadas (e-mails)

## Documentos Gerados

### Comprovante de Reserva (CPF)
Emitido quando a reserva é confirmada com identificação por CPF.

### Fatura de Hospedagem (CNPJ)
Emitida quando a reserva é confirmada com identificação por CNPJ, incluindo ISS retido (5%).

### Booking Receipt (Passaporte)
Emitido quando a reserva é confirmada com identificação por passaporte, em inglês.

Todos os documentos são enviados por e-mail na confirmação e disponíveis para segunda via após pagamento.
