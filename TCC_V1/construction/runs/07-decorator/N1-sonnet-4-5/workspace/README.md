# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para pousada, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Características

- **API REST** para criação e gerenciamento de reservas
- **Cálculo automático de preços** com regras de alta temporada e descontos
- **Sistema de mensagens** com regras configuráveis (validação de email, modo de teste, cópia para arquivo)
- **Relatórios CSV** de ocupação e receita
- **Arquitetura extensível** usando Chain of Responsibility para regras de negócio

## Requisitos

- Java 21
- Maven 3.8+
- PostgreSQL 18+ (ou usar Docker Compose fornecido)

## Configuração

O sistema requer as seguintes variáveis de ambiente:

- `DB_URL`: URL JDBC do PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT`: Porta do servidor (opcional, padrão: 7070)
- `HOTEL_TEST_MODE`: Se `true`, ativa o modo de teste (opcional)

## Build

```bash
mvn clean package
```

Isso gera um JAR executável em `target/reservas-1.0.0.jar`.

## Execução

### Com Docker Compose (Recomendado)

```bash
docker-compose up -d postgres
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=pousada
export DB_PASSWORD=pousada123
java -jar target/reservas-1.0.0.jar
```

### Com PostgreSQL existente

```bash
export DB_URL=jdbc:postgresql://seu-host:5432/seu-banco
export DB_USER=seu-usuario
export DB_PASSWORD=sua-senha
java -jar target/reservas-1.0.0.jar
```

## Testes

### Testes Unitários

```bash
mvn test
```

### Testes de Integração

Os testes de integração precisam de um banco PostgreSQL:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada_test
export DB_USER=pousada
export DB_PASSWORD=pousada123
mvn verify
```

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos disponíveis

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Obtém comprovante da reserva

### Mensagens

- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro

## Regras de Negócio

### Quartos

- 101, 102: STANDARD (2 pessoas, R$ 249,90/noite)
- 201, 202: DELUXE (3 pessoas, R$ 389,90/noite)
- 301: SUITE (4 pessoas, R$ 579,90/noite)

### Preços

- **Alta temporada** (15/dez a fim/fev + julho): +20% na diária
- **Plano NON_REFUNDABLE**: -15% no total
- Arredondamento "banker's rounding" (meio para o par) em cada passo

### Sistema de Mensagens

O sistema implementa três regras de mensagens de forma extensível:

1. **Validação de Email**: Bloqueia emails inválidos (sem @, sem ponto após @, etc)
2. **Modo de Teste**: Quando `HOTEL_TEST_MODE=true`, redireciona para `testes@pousadamarealta.com.br`
3. **Cópia para Arquivo**: Envia cópia para `arquivo@pousadamarealta.com.br` (exceto em modo de teste)

Novas regras podem ser adicionadas implementando a interface `MessageRule` e registrando no `MessageProcessor`.

## Estrutura do Projeto

```
src/main/java/com/pousadamarealta/
├── domain/              # Entidades de domínio e enums
├── messaging/           # Sistema de regras de mensagens
├── repository/          # Acesso ao banco de dados
├── service/             # Lógica de negócio
├── web/                 # Controllers REST
│   └── dto/            # DTOs de requisição/resposta
└── App.java            # Classe principal
```

## Design Patterns

- **Chain of Responsibility**: Sistema de regras de mensagens
- **Repository Pattern**: Acesso ao banco de dados
- **Service Layer**: Lógica de negócio isolada
- **DTO Pattern**: Separação entre domínio e API

## Licença

Proprietário - Pousada Maré Alta
