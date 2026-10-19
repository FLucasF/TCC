# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0.

## Arquitetura

O sistema utiliza o **Strategy Pattern** para gerenciar os diferentes tipos de documentos (Comprovante, Fatura e Booking Receipt), permitindo que novos tipos sejam adicionados facilmente sem modificar o código existente.

### Estrutura de Pacotes

- `br.com.mareaalta.domain` - Modelos de domínio e estratégias de geração de documentos
- `br.com.mareaalta.service` - Lógica de negócio
- `br.com.mareaalta.api` - Controllers REST e DTOs
- `br.com.mareaalta.App` - Ponto de entrada da aplicação

### Padrões de Design

**Strategy Pattern** para geradores de documento:
- `DocumentGenerator` - Interface da estratégia
- `CpfDocumentGenerator` - Gera comprovantes para hóspedes com CPF
- `CnpjDocumentGenerator` - Gera faturas para empresas com CNPJ
- `PassportDocumentGenerator` - Gera booking receipts para estrangeiros com passaporte

Para adicionar um novo tipo de documento (ex: agências de turismo), basta:
1. Criar uma nova classe implementando `DocumentGenerator`
2. Adicionar lógica de seleção no `ReservationService.determineDocumentGenerator()`

## Compilação

```bash
mvn clean package
```

Isso gerará o arquivo `target/pousada-1.0.0.jar`.

## Execução

```bash
java -jar target/pousada-1.0.0.jar
```

Por padrão, o servidor inicia na porta 7070. Para usar outra porta:

```bash
PORT=8080 java -jar target/pousada-1.0.0.jar
```

## Testes

```bash
mvn verify
```

Executa todos os testes de integração, incluindo:
- Testes de reservas com CPF, CNPJ e Passaporte
- Testes de geração de documentos
- Testes de relatórios CSV
- Testes de alta temporada e planos de tarifa

## API REST

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Obtém segunda via do documento

### Mensagens

- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro

## Regras de Negócio

### Tipos de Hóspede

1. **Pessoa com CPF**: Recebe comprovante em português
2. **Empresa com CNPJ**: Recebe fatura com ISS (5%) retido
3. **Estrangeiro com Passaporte**: Recebe booking receipt em inglês

### Alta Temporada

- 15 de dezembro até 28/29 de fevereiro
- Mês de julho inteiro
- Tarifa com acréscimo de 20%

### Planos de Tarifa

- **STANDARD**: Tarifa normal, reembolsável
- **NON_REFUNDABLE**: 15% de desconto, não reembolsável

### Estados da Reserva

PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
         ↓
     CANCELLED (apenas antes do check-in)

## Tecnologias

- Java 21
- Javalin 6.7.0
- Jackson 2.18.2
- JUnit 5.11.4
- REST Assured 5.5.0
- Maven 3.x
