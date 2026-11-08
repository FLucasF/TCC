# Pousada Maré Alta - Sistema de Reservas

Sistema web completo de gerenciamento de reservas para a Pousada Maré Alta, uma pequena pousada no litoral com 5 quartos.

## Stack Técnico

- **Java 21** com **Javalin 6.7.0** (sem Spring)
- **PostgreSQL 18** para persistência de dados
- **Maven** para build
- **Docker** para containerização (opcional)

## Estrutura de Quartos

| Quarto | Tipo | Capacidade | Diária |
|--------|------|-----------|--------|
| 101-102 | STANDARD | 2 pessoas | R$ 249,90 |
| 201-202 | DELUXE | 3 pessoas | R$ 389,90 |
| 301 | SUITE | 4 pessoas | R$ 579,90 |

**Alta Temporada** (+20% na diária):
- 15 de dezembro a 28 de fevereiro
- Mês de julho (inteiro)

## Planos de Tarifa

- **STANDARD**: Preço cheio, reembolso integral em cancelamento
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso em cancelamento

## Ciclo de Vida da Reserva

```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
   ↓                      ↓              ↓
CANCELLED (refund dep. do plano)
```

## API Endpoints

### Quartos
```
GET /rooms
```

### Reservas
```
POST   /reservations                          # Criar
GET    /reservations/{id}                     # Obter
POST   /reservations/{id}/confirmation        # Confirmar
POST   /reservations/{id}/check-in            # Check-in
POST   /reservations/{id}/check-out           # Check-out (retorna comprovante)
POST   /reservations/{id}/cancellation        # Cancelar
GET    /reservations/{id}/document            # Obter segunda via (recibo)
```

### Mensagens
```
GET /messages
```

### Relatórios (CSV)
```
GET /reports/occupancy?date=2027-07-10
GET /reports/revenue?month=2027-07
GET /reports/tourism-tax?month=2027-07      # Taxa turística: R$ 4,50/hóspede/noite
GET /reports/rate-plans?month=2027-07       # Análise por plano de tarifa
```

## Build & Execução

### Pré-requisitos
- Java 21
- Maven 3.9+
- PostgreSQL 18 (ou Docker)

### Build
```bash
mvn clean package
```

Gera: `/target/app.jar`

### Com Docker Compose
```bash
docker-compose up
```

A aplicação estará disponível em `http://localhost:7070`

### Manualmente
```bash
# Variáveis de ambiente obrigatórias:
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="pousada"
export DB_PASSWORD="senha"
export PORT=7070

java -jar target/app.jar
```

### Testes
```bash
mvn verify
```

### Teste da API
```bash
./test-api.sh
```

## Características Implementadas

✅ Gestão completa de reservas (CRUD)
✅ Validação de disponibilidade de quartos
✅ Cálculo de preços com:
  - Arredondamento banker's (meia para o par)
  - Detecção automática de alta temporada
  - Aplicação de descontos para NON_REFUNDABLE
✅ Sistema de confirmação por email (armazenado em banco)
✅ Recibos formatados (português)
✅ Cancelamento com regras de reembolso por plano
✅ 4 relatórios em CSV:
  - Ocupação (por noite)
  - Financeiro (por mês)
  - Taxa turística (municipal)
  - Análise de planos de tarifa
✅ Tratamento de erros HTTP padronizado
✅ Testes unitários

## Detalhes Técnicos

### Arredondamento
Todos os valores em centavos usam `RoundingMode.HALF_EVEN` (arredondamento banker's):
- Preço por noite
- Subtotal
- Desconto
- Total

### Formato de Datas
- **API**: ISO 8601 (`2027-07-10`)
- **Relatórios**: ISO 8601 (`2027-07-10`)
- **Comprovantes**: Brasileiro (`10/07/2027`)

### Detecção de Alta Temporada
- 15/12 a 28/02 (verão)
- Julho inteiro

Aplicada por noite, se qualquer noite cair em alta temporada durante a estadia.

### Relatórios CSV
- Codificação: UTF-8
- Separador: Vírgula
- Datas: ISO 8601
- Valores monetários: 2 casas decimais (ex: `599.76`)
- Cabeçalho: Título da pousada, nome do relatório, período

## Erros HTTP

| Status | Código | Cenário |
|--------|--------|---------|
| 400 | INVALID_REQUEST | Campo ausente, formato inválido, validação falhou |
| 404 | ROOM_NOT_FOUND | Quarto não existe |
| 404 | RESERVATION_NOT_FOUND | Reserva não existe |
| 409 | ROOM_UNAVAILABLE | Quarto já está reservado |
| 409 | OPERATION_NOT_ALLOWED | Ação não permitida para status atual |
| 422 | CAPACITY_EXCEEDED | Mais hóspedes que capacidade |

## Banco de Dados

Esquema auto-criado na primeira execução. Tabelas:
- `rooms` - Quartos estáticos
- `reservations` - Histórico completo de reservas
- `messages` - Mensagens enviadas (emails)

Índices criados para:
- Verificação de conflitos de reserva
- Filtros por datas

## Roadmap (Futuro)

Conforme requisitado pelo contador:
- [ ] Relatório de cancelamentos
- [ ] Relatório de hóspedes por estado
- [ ] Timestamp de geração em todos os relatórios
- [ ] Ajuste dinâmico de taxa turística (por ano)

## Notas de Implementação

- Sistema state-driven (ciclo de vida bem definido)
- Preços calculados uma única vez na criação (imutáveis na reserva)
- Refund nullable (null = não cancelada, 0 = sem refund)
- High season detectado por noite (não por período contíguo)
- Mensagens armazenadas em banco (pronto para integração com email real)
- CSV sem timestamps (conforme requisito inicial)
