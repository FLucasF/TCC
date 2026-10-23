# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Características

- **Gestão de quartos**: 5 quartos (2 STANDARD, 2 DELUXE, 1 SUITE)
- **Reservas completas**: criação, confirmação, check-in, check-out e cancelamento
- **Tipos de hóspede**: pessoa física (CPF), empresa (CNPJ) e estrangeiro (passaporte)
- **Documentos personalizados**: comprovante, fatura de hospedagem e booking receipt
- **Alta temporada**: cálculo automático com acréscimo de 20%
- **Planos de tarifa**: STANDARD e NON_REFUNDABLE (15% de desconto)
- **Relatórios**: ocupação por data e receita por mês (formato CSV)
- **E-mails**: envio de confirmação (armazenados em banco)

## Arquitetura

O sistema utiliza o padrão **Strategy** para geração de documentos:
- `IndividualDocumentGenerator`: gera comprovantes para pessoas físicas
- `CorporateDocumentGenerator`: gera faturas para empresas (com ISS 5%)
- `InternationalDocumentGenerator`: gera booking receipts em inglês para estrangeiros

Adicionar novos tipos de documento (ex: agências de turismo) requer apenas:
1. Criar nova classe implementando `DocumentGenerator`
2. Registrar na `DocumentGeneratorFactory`

## Requisitos

- Java 21
- PostgreSQL 18 (ou compatível)
- Maven 3.6+

## Configuração

Configure as variáveis de ambiente:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=postgres
export PORT=7070
```

Se não configuradas, o sistema usa os valores padrão acima.

## Build e Execução

```bash
# Compilar e gerar JAR
mvn clean package

# Executar
java -jar target/reservas-1.0.0.jar
```

O sistema cria automaticamente as tabelas e insere os 5 quartos no primeiro boot.

## API REST

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria reserva
- `GET /reservations/{id}` - Consulta reserva
- `POST /reservations/{id}/confirmation` - Confirma reserva (envia e-mail)
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out (retorna documento)
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `GET /reservations/{id}/document` - Segunda via do documento

### Mensagens
- `GET /messages` - Lista e-mails enviados

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Ocupação do dia (CSV)
- `GET /reports/revenue?month=2027-07` - Receita do mês (CSV)

## Exemplo: Criar Reserva

```json
POST /reservations
{
  "roomNumber": "101",
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "111.222.333-44",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

Para empresa:
```json
{
  "companyCnpj": "12.345.678/0001-90",
  "companyName": "Tecnosul Ltda",
  ...
}
```

Para estrangeiro:
```json
{
  "guestPassport": "X1234567",
  "guestCountry": "United Kingdom",
  ...
}
```

## Testes

```bash
# Testes unitários
mvn test

# Verificação completa
mvn verify
```

14 testes unitários cobrem:
- Cálculo de preços com alta/baixa temporada
- Descontos por plano de tarifa
- Transições de status da reserva
- Geração de documentos personalizados
- Padrão Strategy para diferentes tipos de hóspede

## Estrutura do Projeto

```
src/main/java/com/pousadamarealta/
├── domain/           # Entidades de negócio
├── document/         # Strategy pattern para documentos
├── repository/       # Acesso a dados
├── service/          # Lógica de negócio
├── api/              # Controllers REST
└── database/         # Migração e configuração do BD
```

## Códigos de Erro

- `INVALID_REQUEST` (400): Requisição inválida
- `ROOM_NOT_FOUND` (404): Quarto não encontrado
- `RESERVATION_NOT_FOUND` (404): Reserva não encontrada
- `ROOM_UNAVAILABLE` (409): Quarto já reservado nas datas
- `OPERATION_NOT_ALLOWED` (409): Operação não permitida no status atual
- `CAPACITY_EXCEEDED` (422): Número de hóspedes excede capacidade do quarto
