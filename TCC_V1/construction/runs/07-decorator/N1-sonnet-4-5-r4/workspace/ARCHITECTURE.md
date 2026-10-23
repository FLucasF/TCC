# Arquitetura do Sistema

## Visão Geral

O sistema foi projetado seguindo os princípios SOLID e o padrão de arquitetura em camadas, com atenção especial à extensibilidade conforme as orientações do CLAUDE.md.

## Estrutura de Pacotes

```
com.pousadamarealta
├── controller/          # Endpoints REST
├── database/           # Gerenciamento de conexão
├── messaging/          # Sistema de regras de mensagens
├── model/             # Entidades de domínio
├── repository/        # Acesso a dados
└── service/           # Regras de negócio
```

## Componentes Principais

### Models

Entidades de domínio com lógica específica:

- **Room**: Representa um quarto da pousada
- **Reservation**: Contém lógica de cálculo de preços e alta temporada
- **Message**: Representa uma mensagem enviada
- **ReservationDocument**: DTO para comprovantes

### Repositories

Camada de persistência com PostgreSQL:

- **RoomRepository**: Quartos (em memória, dados fixos)
- **ReservationRepository**: CRUD de reservas
- **MessageRepository**: CRUD de mensagens

### Services

Lógica de negócio centralizada:

- **ReservationService**: Orquestra todo o fluxo de reservas
- **MessageService**: Aplica pipeline de regras às mensagens
- **ReportService**: Gera relatórios CSV

### Controllers

Endpoints REST que delegam para os services:

- **RoomController**
- **ReservationController**
- **MessageController**
- **ReportController**

## Sistema de Mensagens Extensível

O design do sistema de mensagens foi feito para atender ao requisito: *"cada regra dessas pode valer sempre, só no modo de teste ou só fora dele. E cada instalação do sistema pode querer outra combinação"*.

### Padrão Chain of Responsibility

```
Message → Pipeline → [Rule1] → [Rule2] → [Rule3] → Result
```

### Componentes

1. **MessageRule** (interface)
   - Contrato que toda regra deve implementar
   - Método: `void apply(MessageRuleContext context)`

2. **MessageRuleContext**
   - Transporta estado entre regras
   - Permite bloquear mensagem
   - Permite adicionar mensagens adicionais (cópia)

3. **MessageRulePipeline**
   - Aplica regras em sequência
   - Retorna lista de mensagens resultantes

4. **MessageRulePipelineFactory**
   - Cria pipeline com regras apropriadas
   - Configuração centralizada

### Regras Implementadas

- **EmailValidationRule**: Valida formato do e-mail
- **TestModeRule**: Redireciona para teste
- **ArchiveCopyRule**: Cria cópia para arquivo

### Adicionando Nova Regra

Para adicionar uma nova regra (ex: limite de mensagens por dia):

1. Criar classe implementando `MessageRule`:

```java
public class DailyLimitRule implements MessageRule {
    @Override
    public void apply(MessageRuleContext context) {
        // Lógica da regra
    }
}
```

2. Registrar no `MessageRulePipelineFactory`:

```java
rules.add(new DailyLimitRule());
```

**Não é necessário modificar código existente.**

## Cálculo de Preços

O cálculo de preços segue o arredondamento "meio para o par" (banker's rounding) em cada etapa:

1. Calcular preço de cada noite (base × 1.20 se alta temporada)
2. Arredondar cada noite
3. Somar todas as noites (subtotal)
4. Arredondar subtotal
5. Calcular desconto (subtotal × taxa)
6. Arredondar desconto
7. Calcular total (subtotal - desconto)
8. Arredondar total

Implementação em `Reservation.calculateSubtotal()` e `Reservation.calculateDiscount()`.

## Alta Temporada

Definição:
- Julho completo
- 15 de dezembro até fim de fevereiro

Implementação em `Reservation.isHighSeason()`.

## Máquina de Estados

```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
    ↓          ↓
CANCELLED  CANCELLED
```

Validações em `ReservationService` garantem transições corretas.

## Banco de Dados

### Tabelas

**reservations**
- id (serial)
- room_number
- guest_name, guest_email, guest_cpf
- check_in, check_out
- guests
- rate_plan
- status
- amount_paid
- refund

**messages**
- id (serial)
- channel
- recipient
- subject
- body
- status

### Migração

Tabelas são criadas automaticamente na inicialização via `DatabaseManager`.

## Princípios Aplicados

### Do CLAUDE.md

1. **"Para acrescentar o próximo caso, quantos lugares teriam de mudar?"**
   - Nova regra de mensagem: apenas adicionar classe
   - Novo tipo de quarto: apenas dados (enum + constantes)
   - Nova transição de status: apenas um método

2. **"Quando vários casos repetem a sequência"**
   - Pipeline de regras usa mesma sequência para todas
   - Cálculo de preço usa mesma estrutura para todos os casos

3. **"Faça essa organização só onde a variação existe"**
   - Regras de mensagem: variação explícita no enunciado
   - Quartos: valores fixos, sem abstração desnecessária

### SOLID

- **Single Responsibility**: Cada classe tem uma responsabilidade clara
- **Open/Closed**: Extensível (regras) sem modificar existente
- **Liskov Substitution**: Todas as regras são intercambiáveis
- **Interface Segregation**: Interface minimalista (apenas `apply`)
- **Dependency Inversion**: Services dependem de abstrações (repositories)
