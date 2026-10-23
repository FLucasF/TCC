# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin.

## Requisitos

- Java 21
- Maven 3.9+
- PostgreSQL 18

## Compilação

```bash
mvn package
```

O comando gera um JAR executável em `target/reservas-1.0.0.jar`.

## Execução

Configure as variáveis de ambiente para conexão com o banco PostgreSQL:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="senha"
export PORT=7070  # opcional, padrão é 7070
```

Execute o JAR:

```bash
java -jar target/reservas-1.0.0.jar
```

O servidor iniciará na porta especificada (ou 7070 por padrão).

## Estrutura do Sistema

### Quartos

A pousada possui 5 quartos fixos:
- 101 e 102: STANDARD (2 pessoas, R$ 249,90/noite)
- 201 e 202: DELUXE (3 pessoas, R$ 389,90/noite)
- 301: SUITE (4 pessoas, R$ 579,90/noite)

### Alta Temporada

Período de 15 de dezembro a fim de fevereiro + mês de julho inteiro (+20% no valor da diária).

### Planos de Tarifa

- STANDARD: tarifa normal
- NON_REFUNDABLE: 15% de desconto, sem reembolso em caso de cancelamento

### Tipos de Hóspedes

O sistema suporta três tipos de hóspedes, cada um com seu documento específico:

1. **Pessoa Física (CPF)**: Recebe comprovante em português
2. **Pessoa Jurídica (CNPJ)**: Recebe fatura de hospedagem com ISS retido (5%)
3. **Estrangeiro (Passaporte)**: Recebe booking receipt em inglês

## API REST

### Quartos

**GET /rooms**
- Lista todos os quartos disponíveis

### Reservas

**POST /reservations**
```json
{
  "roomNumber": 101,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "123.456.789-00",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

Para empresas, substitua `guestCpf` por:
```json
{
  "companyCnpj": "12.345.678/0001-90",
  "companyName": "Tecnosul Ltda",
  ...
}
```

Para estrangeiros:
```json
{
  "guestPassport": "X1234567",
  "guestCountry": "United Kingdom",
  ...
}
```

**GET /reservations/{id}**
- Consulta uma reserva

**POST /reservations/{id}/confirmation**
- Confirma uma reserva (muda status de PENDING para CONFIRMED)

**POST /reservations/{id}/cancellation**
- Cancela uma reserva

**POST /reservations/{id}/check-in**
- Realiza check-in (muda status de CONFIRMED para CHECKED_IN)

**POST /reservations/{id}/check-out**
- Realiza check-out e retorna o documento (comprovante/fatura/receipt)

**GET /reservations/{id}/document**
- Obtém segunda via do documento (disponível apenas para reservas pagas)

### Mensagens

**GET /messages**
- Lista todos os e-mails enviados pelo sistema

### Relatórios

**GET /reports/occupancy?date=2027-07-10**
- Relatório de ocupação em formato CSV para uma data específica

**GET /reports/revenue?month=2027-07**
- Relatório financeiro em formato CSV para um mês específico

## Design Pattern: Template Method

O sistema utiliza o padrão Template Method para geração de documentos. A estrutura comum de todos os documentos (comprovante, fatura, booking receipt) está definida na classe abstrata `DocumentGenerator`, enquanto as classes concretas implementam apenas as variações específicas de cada tipo:

- `IndividualGuestDocumentGenerator`: Gera comprovante em português para pessoas físicas
- `CorporateGuestDocumentGenerator`: Gera fatura com ISS retido para empresas
- `ForeignGuestDocumentGenerator`: Gera booking receipt em inglês para estrangeiros

Essa abordagem garante que novos tipos de documentos (como para agências de turismo) possam ser adicionados facilmente, implementando apenas as diferenças sem alterar o código existente.

## Testes

Execute os testes unitários:

```bash
mvn test
```

Execute verificação completa (compile + test + package):

```bash
mvn verify
```
