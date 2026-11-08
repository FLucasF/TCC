# Pousada Maré Alta - Sistema de Reservas

Sistema de reservas para pequena pousada de litoral, construído em Java 21 com Javalin, PostgreSQL e Maven.

## Compilação e Execução

### Build
```bash
mvn clean package
```

Gera `target/mare-alta.jar` (arquivo auto-contido com todas as dependências).

### Execução
```bash
DB_URL="jdbc:postgresql://localhost:5432/pousada_mare_alta" \
DB_USER="postgres" \
DB_PASSWORD="sua_senha" \
PORT=7070 \
java -jar target/mare-alta.jar
```

Se `PORT` não for definido, padrão é 7070.

## Variáveis de Ambiente

- `DB_URL`: URL JDBC do PostgreSQL (obrigatório)
- `DB_USER`: Usuário do banco (obrigatório)
- `DB_PASSWORD`: Senha do banco (obrigatório)
- `PORT`: Porta HTTP (padrão: 7070)

## Arquitetura

### Organização para Extensibilidade

O sistema usa o padrão **Strategy** para planos de tarifa, garantindo que novos planos possam ser adicionados sem modificar código existente:

- `RatePlan`: Interface que cada plano implementa
- `RatePlanRegistry`: Registro central de todos os planos
- Cada plano (STANDARD, NON_REFUNDABLE, CORPORATE, WEEKEND_PACKAGE, LONG_STAY) é uma classe independente

Para adicionar um novo plano:
1. Crie uma classe que implemente `RatePlan`
2. Registre-a em `RatePlanRegistry`
3. Nenhuma outra mudança necessária

### Estrutura de Pacotes

```
src/main/java/com/pousada/
├── api/dto/           # DTOs para requisições/respostas
├── domain/            # Modelos de negócio e lógica
│   └── rateplans/     # Implementações de planos de tarifa
├── infra/             # Camada de dados (repositórios)
├── service/           # Lógica de negócio (serviços)
└── Application.java   # Configuração Javalin
```

## Planos de Tarifa Implementados

1. **STANDARD**: Plano normal, reembolso total
2. **NON_REFUNDABLE**: 15% desconto, sem reembolso
3. **CORPORATE**: Descontos por empresa (TECNOSUL 12%, AGROVALE 18%, FARMAVIDA 25%), reembolso total
4. **WEEKEND_PACKAGE**: Sexta-domingo (2 noites), preço fixo, 50% de reembolso
5. **LONG_STAY**: 7+ noites, 10% desconto, reembolso total

## Cálculos Financeiros

- Arredondamento: "meio para o par" (banker's rounding, `HALF_EVEN`)
- Moeda: Real (R$) com formatação brasileira (ponto como separador de milhar, vírgula para decimais)
- Alta temporada: 15-28 de dezembro, 1-28 de fevereiro, todo julho (20% surcharge)

## Validações Implementadas

- ✓ Hóspedes dentro da capacidade do quarto
- ✓ Datas de saída após entrada
- ✓ Compatibilidade de plano com datas/hóspedes
- ✓ Disponibilidade de quarto (sem conflitos)
- ✓ Transições de estado de reserva (PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT)
- ✓ Políticas de cancelamento por plano

## Testes

```bash
mvn test
```

Testes unitários validam cálculos de dinheiro com os exemplos do cliente.

## Banco de Dados

O sistema:
- Cria automaticamente as tabelas ao iniciar
- Insere os 5 quartos padrão
- Persiste todas as reservas e mensagens

Tabelas:
- `rooms`: Quartos da pousada
- `reservations`: Reservas (com todas as datas, valores, status)
- `messages`: Mensagens enviadas (para auditoria)

## API REST

Todos os endpoints retornam JSON. Valores monetários em números com 2 casas decimais.

### Exemplos de Requisição

```bash
# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Confirmar reserva
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 779.80}'

# Check-in
curl -X POST http://localhost:7070/reservations/1/check-in

# Check-out (retorna recibo)
curl -X POST http://localhost:7070/reservations/1/check-out

# Relatório de ocupação
curl http://localhost:7070/reports/occupancy?date=2027-03-10

# Relatório de receita
curl http://localhost:7070/reports/revenue?month=2027-03
```

## Nota sobre Design

O sistema foi estruturado seguindo as orientações do projeto:
- Cada novo plano de tarifa requer mudanças em um único lugar (implementação + registro)
- Sequências comuns de passos estão em classes base (`BaseRatePlan`)
- Organizações complexas foram evitadas onde havia apenas variações simples
