# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Requisitos

- Java 21
- Maven 3.x
- PostgreSQL 18

## Configuração

O sistema requer as seguintes variáveis de ambiente:

- `DB_URL`: URL JDBC do PostgreSQL (formato: `jdbc:postgresql://host:port/database`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT` (opcional): Porta para o servidor HTTP (padrão: 7070)

## Compilação

```bash
mvn package
```

Isso gera um JAR executável em `target/maralta-1.0-SNAPSHOT.jar`.

## Execução

```bash
java -jar target/maralta-1.0-SNAPSHOT.jar
```

O sistema iniciará na porta configurada (padrão 7070) e criará automaticamente as tabelas no banco de dados.

## Testes

```bash
mvn test
```

Ou para executar verificação completa:

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
- `POST /reservations/{id}/check-out` - Realiza check-out
- `GET /reservations/{id}/document` - Obtém segunda via do documento

### Mensagens
- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro

## Tipos de Hóspede

O sistema suporta três tipos de hóspede, cada um com seu próprio documento:

### Pessoa Física
- Campos: `guestCpf`
- Documento: Comprovante de reserva em português

### Empresa
- Campos: `companyCnpj`, `companyName`
- Documento: Fatura de hospedagem com ISS retido (5%)

### Estrangeiro
- Campos: `guestPassport`, `guestCountry`
- Documento: Booking receipt em inglês

## Arquitetura

O sistema utiliza o padrão Strategy para gerenciar os diferentes tipos de hóspede, permitindo que novos tipos (como agências de turismo) sejam adicionados facilmente sem modificar o código existente.
