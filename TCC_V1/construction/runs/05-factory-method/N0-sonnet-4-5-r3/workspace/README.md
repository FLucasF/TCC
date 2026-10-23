# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Tecnologias

- Java 21
- Javalin 6.7.0
- PostgreSQL 18
- Maven

## Configuração

O sistema utiliza as seguintes variáveis de ambiente:

- `DB_URL`: URL de conexão do PostgreSQL (padrão: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados (padrão: `postgres`)
- `DB_PASSWORD`: Senha do banco de dados (padrão: `postgres`)
- `PORT`: Porta do servidor (padrão: `7070`)

## Build

Para compilar o projeto:

```bash
mvn clean package
```

Isso gera um JAR executável em `target/marealta-1.0.0.jar`.

## Execução

```bash
java -jar target/marealta-1.0.0.jar
```

O sistema criará automaticamente as tabelas necessárias no banco de dados na primeira execução.

## Testes

```bash
mvn verify
```

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Realiza check-in
- `POST /reservations/{id}/check-out` - Realiza check-out e retorna comprovante
- `GET /reservations/{id}/document` - Obtém segunda via do comprovante

### Mensagens

- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação por data
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro por mês

## Estrutura do Projeto

```
src/
├── main/
│   └── java/
│       └── br/com/pousada/marealta/
│           ├── App.java                   # Classe principal
│           ├── controller/                # Controladores REST
│           ├── model/                     # Modelos de domínio
│           ├── repository/                # Acesso a dados
│           ├── service/                   # Lógica de negócio
│           ├── dto/                       # DTOs
│           ├── exception/                 # Exceções customizadas
│           └── util/                      # Utilitários
└── test/
    └── java/
        └── br/com/pousada/marealta/       # Testes unitários
```

## Funcionalidades

- **5 Quartos**: 101, 102 (STANDARD), 201, 202 (DELUXE), 301 (SUITE)
- **Alta Temporada**: 15/12 a 28/02 e julho (+20% nas diárias)
- **Planos de Tarifa**: STANDARD e NON_REFUNDABLE (15% desconto)
- **Tipos de Hóspede**: 
  - Pessoa física (CPF) → Comprovante em português
  - Empresa (CNPJ) → Fatura com ISS
  - Estrangeiro (Passaporte) → Booking receipt em inglês
- **Workflow**: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT (ou CANCELLED)
- **E-mails**: Confirmação enviada ao confirmar reserva
- **Relatórios**: Ocupação diária e financeiro mensal em CSV
