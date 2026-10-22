# Implementação do Sistema de Reservas - Pousada Maré Alta

## Resumo

Sistema completo e funcional de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Arquitetura

### Estrutura de Pacotes

- `com.pousada.marealta` - Aplicação principal
- `com.pousada.marealta.model` - Modelos de dados (Room, Reservation, Message)
- `com.pousada.marealta.db` - Configuração e inicialização do banco de dados
- `com.pousada.marealta.service` - Lógica de negócio
  - `RoomService` - Gerenciamento de quartos
  - `ReservationService` - Gerenciamento de reservas
  - `MessageService` - Gerenciamento e envio de mensagens
  - `ReportService` - Geração de relatórios
- `com.pousada.marealta.routes` - Endpoints HTTP
  - `RoomRoutes`
  - `ReservationRoutes`
  - `MessageRoutes`
  - `ReportRoutes`
- `com.pousada.marealta.util` - Utilitários
  - `PriceCalculator` - Cálculos de tarifa com alta/baixa temporada
  - `EmailValidator` - Validação de endereços de email
- `com.pousada.marealta.exception` - Exceções customizadas

### Banco de Dados

Tabelas criadas automaticamente na inicialização:
- `rooms` - Quartos da pousada (5 registros)
- `reservations` - Histórico de reservas
- `messages` - Mensagens enviadas

## Funcionalidades Implementadas

### 1. Gerenciamento de Quartos
- GET /rooms: Lista todos os 5 quartos com tipo, capacidade e tarifa

### 2. Fluxo de Reservas
- **Criar (POST /reservations)**: 
  - Validação de quarto, datas, capacidade
  - Cálculo automático de preços com temporada
  - Estado inicial: PENDING
  
- **Confirmar (POST /reservations/{id}/confirmation)**:
  - Registra pagamento
  - Envia comprovante por email
  - Estado: PENDING → CONFIRMED

- **Check-in (POST /reservations/{id}/check-in)**:
  - Estado: CONFIRMED → CHECKED_IN

- **Check-out (POST /reservations/{id}/check-out)**:
  - Retorna comprovante
  - Estado: CHECKED_IN → CHECKED_OUT

- **Cancelar (POST /reservations/{id}/cancellation)**:
  - Processado antes do check-in
  - STANDARD: reembolso integral
  - NON_REFUNDABLE: sem reembolso
  - Estado: → CANCELLED

### 3. Cálculos Financeiros
- **Temporada Alta**: 15/12 a 28/02 e julho
  - Acréscimo de 20% na diária
- **Desconto NON_REFUNDABLE**: 15% do subtotal
- **Arredondamento**: "Meio para o par" (HALF_EVEN) em cada etapa

### 4. Gerenciamento de Mensagens

**Regras Implementadas:**
1. **Validação de Email**:
   - Exatamente 1 @
   - Algo antes do @
   - Domínio com pelo menos 1 ponto
   - Status: SENT ou BLOCKED

2. **Cópia para Arquivo**:
   - Automática para arquivo@pousadamarealta.com.br
   - Assunto prefixado com "[Cópia]"
   - Apenas para emails válidos em modo normal

3. **Modo Teste (HOTEL_TEST_MODE=true)**:
   - Redirecionamento para testes@pousadamarealta.com.br
   - Assunto prefixado com "[TESTE]"
   - Primeira linha do body: "Destinatário original: [email]"
   - Sem cópia para arquivo

### 5. Relatórios em CSV

**Ocupação** (`GET /reports/occupancy?date=2027-07-10`):
- Reservas pagas naquela noite
- Colunas: room, type, reservation, check_in, check_out
- Total de quartos ocupados vs. disponíveis

**Financeiro** (`GET /reports/revenue?month=2027-07`):
- Reservas que começam naquele mês
- Colunas: reservation, room, check_in, check_out, status, amount
- Amount = valor pago - reembolso
- NON_REFUNDABLE cancelada continua contando com amount_paid

### 6. Comprovantes

**Formato Padronizado:**
```
Comprovante de reserva nº X
Hóspede: Nome (CPF XXX.XXX.XXX-XX)
Quarto: 201 (DELUXE)
Entrada: DD/MM/YYYY | Saída: DD/MM/YYYY | N diárias
Diárias: R$ XXX,XX
[Desconto: R$ XXX,XX]
Total: R$ XXX,XX
```

**Email de Confirmação:**
- Assunto: "Reserva confirmada - Pousada Maré Alta"
- Inclui comprovante completo
- Informações sobre horários de check-in/check-out

## Validações de Entrada

| Erro | Status | Condição |
|------|--------|----------|
| INVALID_REQUEST | 400 | Falta campo, JSON inválido, datas inválidas, < 1 hóspede |
| ROOM_NOT_FOUND | 404 | Quarto não existe |
| RESERVATION_NOT_FOUND | 404 | Reserva não existe |
| ROOM_UNAVAILABLE | 409 | Quarto já reservado |
| OPERATION_NOT_ALLOWED | 409 | Operação não permitida no estado atual |
| CAPACITY_EXCEEDED | 422 | Mais hóspedes que capacidade do quarto |

## Testes

**Teste Unitários Implementados:**
- EmailValidator: 8 testes (validação de email)
- PriceCalculator: 7 testes (temporadas, cálculos, descontos)

**Execução:**
```bash
mvn verify  # 15 testes, 100% sucesso
```

## Configuração

**Variáveis de Ambiente:**
- `DB_URL`: jdbc:postgresql://host:port/database
- `DB_USER`: usuário do banco
- `DB_PASSWORD`: senha do banco
- `PORT`: porta da aplicação (padrão: 7070)
- `HOTEL_TEST_MODE`: true/false (padrão: false)

## Compilação e Deploy

**Compilação:**
```bash
mvn clean package
# Gera: target/mare-alta-1.0.0.jar (9.1 MB)
```

**Execução:**
```bash
java -jar target/mare-alta-1.0.0.jar
```

**Docker:**
```bash
docker build -t mare-alta .
docker run -e DB_URL=... -e DB_USER=... -e DB_PASSWORD=... -p 7070:7070 mare-alta
```

## Padrões de Código

- Sem Spring: apenas Javalin para HTTP
- Java 21: syntaxe moderno, records não usados por simplicidade
- PostgreSQL nativo: sem ORM, queries SQL diretas
- Segurança: validação de entrada em endpoints críticos
- Testes: JUnit 4 para testes unitários

## Notas de Implementação

1. **Cálculo de Preço**: Cada noite é calculada individualmente com sua tarifa (alta/baixa), permitindo reservas que cruzem períodos de temporada.

2. **Emails**: Não são enviados de verdade. Ficam armazenados em `messages` para consulta via API GET /messages.

3. **Banco de Dados**: Criado automaticamente na primeira execução. Não requer scripts de inicialização manual.

4. **Transações**: SQL executado diretamente sem transactions explícitas (auto-commit). Para produção, adicionar transações.

5. **Performance**: Sem índices especializados. Para produção, criar índices em `room_number`, `check_in`, `status`.
