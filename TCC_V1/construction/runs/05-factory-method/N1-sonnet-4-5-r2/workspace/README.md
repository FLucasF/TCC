# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Requisitos

- Java 21
- PostgreSQL 18
- Maven 3.8+

## Configuração

O sistema requer as seguintes variáveis de ambiente:

- `DB_URL`: URL de conexão JDBC para o PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT`: Porta para o servidor HTTP (opcional, padrão: 7070)

## Compilação

```bash
mvn clean package
```

Este comando irá:
- Compilar o código
- Executar os testes
- Gerar um JAR executável em `target/mare-alta-1.0.0.jar`

## Execução

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=usuario
export DB_PASSWORD=senha
export PORT=7070

java -jar target/mare-alta-1.0.0.jar
```

O sistema iniciará e criará automaticamente as tabelas necessárias no banco de dados.

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos disponíveis

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Realiza check-in
- `POST /reservations/{id}/check-out` - Realiza check-out e retorna documento
- `GET /reservations/{id}/document` - Obtém segunda via do documento

### Mensagens

- `GET /messages` - Lista todas as mensagens enviadas (e-mails)

### Relatórios

- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação para uma data (CSV)
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro para um mês (CSV)

## Tipos de Identificação

O sistema suporta três tipos de identificação de hóspedes:

1. **CPF** (pessoa física) - gera Comprovante de Reserva em português
2. **CNPJ** (empresa) - gera Fatura de Hospedagem com ISS
3. **Passaporte** (estrangeiro) - gera Booking Receipt em inglês

## Quartos

- 101, 102: STANDARD (2 pessoas, R$ 249,90/noite)
- 201, 202: DELUXE (3 pessoas, R$ 389,90/noite)
- 301: SUITE (4 pessoas, R$ 579,90/noite)

## Alta Temporada

Alta temporada (acréscimo de 20% na diária):
- 15 de dezembro a 28/29 de fevereiro
- Julho inteiro

## Planos de Tarifa

- `STANDARD`: tarifa normal, reembolso total em caso de cancelamento
- `NON_REFUNDABLE`: 15% de desconto, sem reembolso em caso de cancelamento

## Estrutura do Projeto

```
src/main/java/br/com/pousada/
├── Application.java           # Classe principal
├── api/                       # Controllers HTTP
├── domain/                    # Modelo de domínio
├── exception/                 # Exceções customizadas
├── repository/               # Acesso a dados
├── service/                  # Lógica de negócio
└── util/                     # Utilitários
```

## Testes

Execute os testes com:

```bash
mvn test
```

ou para executar tudo incluindo testes de integração:

```bash
mvn verify
```
