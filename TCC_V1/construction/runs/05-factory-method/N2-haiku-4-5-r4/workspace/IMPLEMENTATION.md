# Decisões de Implementação - Sistema de Reservas Pousada Maré Alta

## Arquitetura

O sistema foi construído seguindo a arquitetura em camadas:

- **API Layer** (`api/`): Endpoints REST com Javalin
- **Service Layer** (`service/`): Lógica de negócio
- **Repository Layer** (`repository/`): Acesso a dados
- **Domain Layer** (`domain/`): Modelos de negócio
- **Utilities** (`util/`): Funções auxiliares

## Escolhas Tecnológicas

### Banco de Dados

- **PostgreSQL 18**: Utiliza tipos de dados nativos (DATE, NUMERIC)
- **HikariCP**: Pool de conexões para melhor performance
- **Schema**: Todas as tabelas criadas automaticamente na inicialização

### Validação e Tratamento de Erros

- **Validação em camada única**: O serviço valida regras de negócio, a API apenas formata respostas
- **Erros padronizados**: Todos os erros seguem o padrão `{ "error": "CODE", "message": "..." }`

### Arredondamento Monetário

- **RoundingMode.HALF_EVEN**: Implementa a regra "meia para o par" conforme especificado
- **Arredondamento em cascata**: Cada passo (preço/noite, subtotal, desconto, total) é arredondado individualmente

### Documentos

A geração de documentos foi organizada em três métodos na classe `DocumentService`:
- `generateReceiptPT()`: Comprovante em português (para hóspedes com CPF)
- `generateInvoice()`: Fatura em português (para empresas com CNPJ)
- `generateBookingReceipt()`: Booking receipt em inglês (para estrangeiros com passaporte)

Cada tipo de documento recebe exatamente as informações que precisa. Para adicionar um novo tipo no futuro (agências de turismo):
1. Adicionar novo método em `DocumentService`
2. Adicionar novos campos em `Reservation`
3. Atualizar validação em `ReservationService`
4. Atualizar `DocumentService.generateDocumentResponse()` para selecionar o tipo correto

## Transições de Estado

As reservas seguem um fluxo de estado rigoroso:

```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
   ↓
CANCELLED (de PENDING ou CONFIRMED)
```

Transições inválidas retornam `409 OPERATION_NOT_ALLOWED`.

## Cancelamentos e Reembolsos

A lógica de refund foi implementada em `ReservationService.cancel()`:

- **PENDING → CANCELLED**: Sem pagamento anterior, refund = 0
- **CONFIRMED (STANDARD) → CANCELLED**: Refund total do amountPaid
- **CONFIRMED (NON_REFUNDABLE) → CANCELLED**: Refund = 0

O refund é calculado ANTES de mudar o status para evitar erros de lógica.

## Relatórios

### Relatório de Ocupação

- Filtra reservas com status CONFIRMED, CHECKED_IN ou CHECKED_OUT
- Uma reserva ocupa o quarto na noite se `check_in <= data < check_out`
- Retorna CSV com colunas: `room, type, reservation, check_in, check_out`

### Relatório Financeiro

- Filtra reservas que INICIARAM no mês especificado (não que terminaram)
- Calcula receita como: `amountPaid - (refund ou 0)`
- Incluindo cancelamentos não-reembolsáveis que geram receita
- Retorna CSV com colunas: `reservation, room, check_in, check_out, status, amount`

## Validações

### Identificação do Hóspede

Exatamente um dos três campos deve ser fornecido:
- `guestCpf`: Número de CPF (formato: "123.456.789-00")
- `companyCnpj` + `companyName`: CNPJ e razão social
- `guestPassport` + `guestCountry`: Passaporte e país

Validação em `ReservationService.createReservation()` retorna `400 INVALID_REQUEST` se violado.

### Disponibilidade de Quartos

- Consulta BD para conflitos em `findByRoomAndDateRange()`
- Usa lógica de intervalo: `check_in < checkOut AND check_out > checkIn`
- Ignora reservas canceladas
- Retorna `409 ROOM_UNAVAILABLE` se há conflito

### Capacidade do Quarto

- `guests <= room.capacity`
- `guests >= 1`
- Retorna `422 CAPACITY_EXCEEDED` se violado

### Datas

- `checkOut > checkIn` (deve haver pelo menos uma noite)
- Formato ISO: `YYYY-MM-DD`
- Retorna `400 INVALID_REQUEST` se inválido

## Mensagens

As mensagens são armazenadas em tabela do BD com campos:
- `id`: Auto-incrementado
- `channel`: Sempre "EMAIL" (no estado atual)
- `recipient`: Endereço de e-mail
- `subject`: Assunto da mensagem
- `body`: Corpo em texto puro com newlines `\n`

A API expõe `/messages` para consulta (útil para recepção verificar comprovantes enviados).

## Extensibilidade Futura

### Agências de Turismo

Quando agências de turismo forem integradas, será necessário:

1. Adicionar novo tipo de identificação (ex: `agencyId`)
2. Novo método em `DocumentService` para gerar documento da agência
3. Atualizar `DocumentService.generateDocumentResponse()` para selecionar o tipo

A arquitetura permite isso sem modificar a lógica de negócio de reservas, preços ou estados.

### Novos Tipos de Quarto

Para adicionar novo tipo de quarto:
1. Inserir diretamente no BD (a enum RoomType pode ser expandida se houver muitos novos tipos)
2. Sem alterações no código

### Política de Refund Customizável

Atualmente hardcoded em `ReservationService.cancel()`. Para customizar por tipo de quarto ou período:
1. Adicionar coluna `refund_policy` em `reservations`
2. Consultar tabela `refund_policies` na lógica de cancelamento

## Testes

Os testes unitários cobrem:
- `PriceCalculatorTest`: Cálculos de preço em temporada normal e alta, com desconto
- `SeasonUtilTest`: Detecção correta de alta temporada (julho, dez 15-28, fevereiro)
- `FormattingUtilTest`: Formatação de valores em português e inglês, datas

Faltam testes de integração com banco de dados (seriam executados contra BD real ou container Docker).

## Configuração de Deployment

### Variáveis de Ambiente

```bash
DB_URL="jdbc:postgresql://host:5432/database"
DB_USER="usuario"
DB_PASSWORD="senha"
PORT=8080  # opcional, padrão é 7070
```

### Inicialização do Banco

Automática na primeira execução:
1. Cria tabelas se não existirem
2. Insere 5 quartos padrão (101, 102, 201, 202, 301)

### Build e Execução

```bash
mvn clean package   # Gera target/reservas.jar
java -jar target/reservas.jar  # Executa
```

O JAR é "fat JAR" com todas as dependências incluídas.
