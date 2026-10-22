# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Funcionalidades

- **Gerenciamento de Reservas**: criar, confirmar, cancelar, check-in e check-out
- **Cálculo de Preços**: tarifa de alta temporada (20%), plano de tarifa não reembolsável (15% desconto)
- **Sistema de Mensagens**: envio de e-mails com validação, modo de teste e cópia para arquivo
- **Relatórios**: ocupação diária e receita mensal em CSV
- **API REST**: endpoints para todas as operações

## Stack Técnico

- Java 21
- Javalin 6.7.0 (framework web)
- PostgreSQL 18 (banco de dados)
- Maven (build)
- Gson (JSON)

## Compilação

```bash
mvn clean verify
```

Isso gera um JAR autoexecutável em `target/hotel-system.jar`.

## Execução

### Pré-requisitos

- PostgreSQL 18 rodando
- Java 21+

### Variáveis de Ambiente

- `DB_URL`: conexão PostgreSQL (padrão: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: usuário do banco (padrão: `postgres`)
- `DB_PASSWORD`: senha do banco (padrão: vazio)
- `PORT`: porta do servidor (padrão: `7070`)
- `HOTEL_TEST_MODE`: modo de teste (padrão: `false`)

### Iniciar o Servidor

```bash
java -jar target/hotel-system.jar
```

O servidor estará disponível em `http://localhost:7070`.

## API Endpoints

### Quartos
- `GET /rooms` - Listar todos os quartos

### Reservas
- `POST /reservations` - Criar reserva
- `GET /reservations/{id}` - Obter detalhes da reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `POST /reservations/{id}/check-in` - Check-in
- `POST /reservations/{id}/check-out` - Check-out
- `GET /reservations/{id}/document` - Obter comprovante (segunda via)

### Mensagens
- `GET /messages` - Listar todas as mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Ocupação diária (CSV)
- `GET /reports/revenue?month=2027-07` - Receita mensal (CSV)

## Estrutura do Projeto

```
src/main/java/com/pousada/marealta/
├── App.java                    # Entrada principal
├── api/                        # Endpoints REST
├── config/                     # Configuração e banco de dados
├── domain/                     # Classes de domínio
├── messaging/                  # Sistema de processamento de mensagens
│   ├── MessageData.java
│   ├── MessageProcessor.java
│   ├── MessageProcessorChain.java
│   └── processors/             # Processadores (Chain of Responsibility)
├── repository/                 # Acesso a dados
└── service/                    # Lógica de negócio
```

## Padrões de Design

O sistema usa o padrão **Chain of Responsibility** para o processamento de mensagens, permitindo adicionar novas regras de e-mail sem modificar código existente:

1. **EmailValidationProcessor**: valida endereço de e-mail
2. **TestModeProcessor**: redireciona e-mails em modo de teste
3. **ArchiveCopyProcessor**: gera cópia para arquivo

## Testes

```bash
mvn test
```

Os testes cobrem:
- Cálculo de preços (alta temporada, desconto, arredondamento)
- Processamento de mensagens (validação, modo teste, bloqueio)

## Exemplo de Requisição

### Criar Reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678900",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

### Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amount": "509.80"}'
```

## Notas Técnicas

- As datas são armazenadas no formato ISO (YYYY-MM-DD)
- Os valores monetários usam arredondamento "meio para o par" (HALF_EVEN)
- O modo de teste (`HOTEL_TEST_MODE=true`) redireciona todos os e-mails para `testes@pousadamarealta.com.br`
- E-mails para o arquivo são enviados para `arquivo@pousadamarealta.com.br` (apenas em produção)
- Mensagens com endereço de e-mail inválido são bloqueadas automaticamente

## Autoria

Sistema desenvolvido para Pousada Maré Alta.
