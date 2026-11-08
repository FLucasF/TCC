# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL.

## Tecnologias

- Java 21
- Javalin 6.7.0 (framework web)
- PostgreSQL 18 (banco de dados)
- Maven (gerenciamento de dependências e build)

## Estrutura do Projeto

O projeto segue uma organização que facilita a adição de novos casos sem modificar o código existente:

- **domain**: Entidades de negócio (Room, Reservation, Message)
- **api**: Controllers REST
- **report**: Sistema de relatórios com estrutura reutilizável
- **database**: Configuração e setup do banco de dados
- **util**: Utilitários (cálculos monetários, formatação de datas)

### Sistema de Relatórios

Todos os relatórios CSV compartilham a mesma estrutura (título, cabeçalho, linhas, total) através da classe base `ReportGenerator`. Cada relatório específico fornece apenas seus dados próprios, facilitando a adição de novos relatórios ou modificações globais no formato.

## Compilação e Execução

### Compilar

```bash
mvn package
```

Este comando gera um JAR executável em `target/pousada-mare-alta-1.0.0.jar`.

### Executar

```bash
java -jar target/pousada-mare-alta-1.0.0.jar
```

### Configuração

O sistema usa variáveis de ambiente para configuração:

- `DB_URL`: URL de conexão JDBC ao PostgreSQL (padrão: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados (padrão: `postgres`)
- `DB_PASSWORD`: Senha do banco de dados (padrão: `postgres`)
- `PORT`: Porta HTTP do servidor (padrão: `7070`)

Exemplo:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=senha123
export PORT=8080
java -jar target/pousada-mare-alta-1.0.0.jar
```

### Banco de Dados

O sistema cria automaticamente as tabelas necessárias e insere os 5 quartos da pousada na primeira execução. Não é necessário criar o schema manualmente.

## Testes

```bash
mvn test
```

ou

```bash
mvn verify
```

## API REST

### Quartos

**GET /rooms**
- Lista todos os quartos da pousada
- Retorna: array de objetos com `number`, `type`, `capacity`, `nightlyRate`

### Reservas

**POST /reservations**
- Cria uma nova reserva
- Body: `roomNumber`, `guestName`, `guestEmail`, `guestCpf`, `checkIn`, `checkOut`, `guests`, `ratePlan`
- Retorna: objeto da reserva criada (status 201)

**GET /reservations/{id}**
- Consulta uma reserva específica
- Retorna: objeto da reserva

**POST /reservations/{id}/confirmation**
- Confirma uma reserva pendente
- Retorna: reserva atualizada

**POST /reservations/{id}/cancellation**
- Cancela uma reserva
- Retorna: reserva atualizada

**POST /reservations/{id}/check-in**
- Faz check-in de uma reserva confirmada
- Retorna: reserva atualizada

**POST /reservations/{id}/check-out**
- Faz check-out de uma reserva
- Retorna: comprovante da reserva

**GET /reservations/{id}/document**
- Obtém segunda via do comprovante
- Retorna: comprovante (somente para reservas pagas)

### Mensagens

**GET /messages**
- Lista todas as mensagens enviadas pelo sistema
- Retorna: array de objetos com `id`, `channel`, `to`, `subject`, `body`

### Relatórios

Todos os relatórios retornam CSV com charset UTF-8.

**GET /reports/occupancy?date=YYYY-MM-DD**
- Relatório de ocupação para uma data específica

**GET /reports/revenue?month=YYYY-MM**
- Relatório financeiro mensal

**GET /reports/tourism-tax?month=YYYY-MM**
- Relatório de taxa de turismo mensal

**GET /reports/rate-plans?month=YYYY-MM**
- Relatório de planos de tarifa mensal

## Regras de Negócio

### Quartos
- 101, 102: STANDARD (2 pessoas, R$ 249,90/noite)
- 201, 202: DELUXE (3 pessoas, R$ 389,90/noite)
- 301: SUITE (4 pessoas, R$ 579,90/noite)

### Alta Temporada
- 15 de dezembro a fim de fevereiro
- Julho inteiro
- Acréscimo de 20% no valor da diária

### Planos de Tarifa
- STANDARD: preço normal, reembolsável
- NON_REFUNDABLE: 15% de desconto, não reembolsável

### Estados da Reserva
1. PENDING (inicial)
2. CONFIRMED (após pagamento)
3. CHECKED_IN (após check-in)
4. CHECKED_OUT (após check-out)
5. CANCELLED (cancelada)

### Arredondamento
Todos os valores monetários usam arredondamento "banker's rounding" (HALF_EVEN) com 2 casas decimais.

## Códigos de Erro

- `INVALID_REQUEST` (400): Dados inválidos na requisição
- `ROOM_NOT_FOUND` (404): Quarto não encontrado
- `RESERVATION_NOT_FOUND` (404): Reserva não encontrada
- `ROOM_UNAVAILABLE` (409): Quarto já reservado
- `OPERATION_NOT_ALLOWED` (409): Operação não permitida no estado atual
- `CAPACITY_EXCEEDED` (422): Número de hóspedes excede capacidade

## Desenvolvimento

O projeto foi desenvolvido seguindo os princípios descritos no CLAUDE.md:
- Código organizado para que novos casos entrem em um único lugar
- Reutilização de sequências comuns entre casos semelhantes
- Abstração aplicada somente onde a variação é explícita e contínua
