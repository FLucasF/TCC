# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para pousada com funcionalidades de reserva, check-in/check-out, notificações por e-mail e relatórios.

## Características

- **5 quartos**: 101, 102 (STANDARD), 201, 202 (DELUXE), 301 (SUITE)
- **Preços dinâmicos**: Alta temporada (15/dez a fim/fev + julho) com acréscimo de 20%
- **Planos de tarifa**: STANDARD e NON_REFUNDABLE (15% de desconto)
- **Sistema extensível de regras de e-mail**:
  - Validação de endereços de e-mail
  - Modo de teste que redireciona e-mails
  - Cópia automática para arquivo
  - Arquitetura preparada para novas regras (limite de envios, horários, etc.)

## Tecnologias

- Java 21
- Javalin 6.7.0
- PostgreSQL 18
- Maven

## Configuração

### Variáveis de Ambiente

```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada
DB_USER=postgres
DB_PASSWORD=postgres
PORT=7070
HOTEL_TEST_MODE=false
```

### Iniciar o banco de dados (Docker)

```bash
docker run --name pousada-db \
  -e POSTGRES_DB=pousada \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgres:18
```

### Build e execução

```bash
# Compilar
mvn clean package

# Executar
java -jar target/reservas-1.0.0.jar

# Ou diretamente
mvn clean compile exec:java -Dexec.mainClass="com.pousadamarealta.Application"
```

### Testes

```bash
# Executar testes de integração
mvn verify
```

## API Endpoints

### Quartos
- `GET /rooms` - Listar todos os quartos

### Reservas
- `POST /reservations` - Criar reserva
- `GET /reservations/{id}` - Consultar reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `POST /reservations/{id}/check-in` - Fazer check-in
- `POST /reservations/{id}/check-out` - Fazer check-out
- `GET /reservations/{id}/document` - Obter comprovante (segunda via)

### Mensagens
- `GET /messages` - Listar mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - Relatório financeiro (CSV)

## Regras de Negócio

### Preços
- Arredondamento "meio para o par" (HALF_EVEN) em cada etapa
- Alta temporada: 15/dez a 28/fev + 1/jul a 31/jul

### Estados da Reserva
```
PENDING -> CONFIRMED -> CHECKED_IN -> CHECKED_OUT
       \-> CANCELLED
```

### Cancelamentos
- STANDARD: reembolso total
- NON_REFUNDABLE: sem reembolso

### Sistema de Mensagens

O sistema implementa um pipeline extensível de processamento de mensagens com três regras ativas:

1. **EmailValidationRule**: Valida formato do e-mail (algo@dominio.com)
2. **TestModeRule**: Em modo de teste, redireciona para testes@pousadamarealta.com.br
3. **ArchiveCopyRule**: Cria cópia para arquivo@pousadamarealta.com.br (exceto em modo de teste)

Para adicionar novas regras, implemente a interface `MessageProcessor` e adicione ao `MessageService`.

## Estrutura do Projeto

```
src/main/java/com/pousadamarealta/
├── domain/
│   ├── model/          # Entidades e enums
│   ├── repository/     # Interfaces de repositório
│   └── exception/      # Exceções personalizadas
├── infrastructure/     # Implementações de repositório e DB
├── messaging/          # Sistema de regras de mensagens
│   └── rules/          # Regras específicas
├── service/            # Lógica de negócio
├── controller/         # Controllers REST
│   └── dto/            # DTOs de request/response
└── Application.java    # Classe principal
```
