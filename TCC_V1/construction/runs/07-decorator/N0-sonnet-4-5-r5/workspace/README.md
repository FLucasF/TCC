# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL.

## Características Principais

### Sistema Extensível de Regras de Mensageria

O destaque do sistema é sua **arquitetura extensível de regras para processamento de mensagens**, projetada desde o primeiro dia para suportar diferentes configurações e novos requisitos sem modificar o código core.

#### Como Funciona

O sistema utiliza um **Pipeline de Mensagens** que processa cada mensagem através de uma cadeia de regras:

```java
MessagePipeline → ValidateEmailRule → TestModeRedirectRule → ArchiveCopyRule → Mensagem Final
```

Cada regra pode:
- **Validar** e bloquear mensagens
- **Modificar** destinatário, assunto ou corpo
- **Criar cópias** adicionais da mensagem

#### Regras Implementadas

1. **ValidateEmailRule**: Valida endereços de e-mail e bloqueia inválidos
2. **TestModeRedirectRule**: Redireciona mensagens em modo de teste
3. **ArchiveCopyRule**: Cria cópia para arquivo (configurável)

#### Extensibilidade

Para adicionar novas regras (ex: limite de mensagens por dia, horário de envio):

1. Implemente a interface `MessageRule`
2. Adicione a regra ao pipeline em `App.java`
3. Configure quando ela deve ser aplicada (sempre, só teste, só produção)

Exemplo de nova regra:

```java
public class NoNightTimeRule implements MessageRule {
    @Override
    public List<Message> process(Message message) {
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.of(6, 0)) || now.isAfter(LocalTime.of(22, 0))) {
            message.setStatus(MessageStatus.BLOCKED);
        }
        return Collections.singletonList(message);
    }
}
```

## Compilação e Execução

### Requisitos

- Java 21
- Maven 3.6+
- PostgreSQL 18

### Compilar

```bash
mvn clean package
```

### Executar

```bash
# Com PostgreSQL local
export DB_URL="jdbc:postgresql://localhost:5432/marealta"
export DB_USER="postgres"
export DB_PASSWORD="senha"
export PORT=7070

java -jar target/marealta-1.0.0.jar
```

### Modo de Teste

```bash
export HOTEL_TEST_MODE=true
java -jar target/marealta-1.0.0.jar
```

No modo de teste:
- Mensagens são redirecionadas para `testes@pousadamarealta.com.br`
- Cópia para arquivo é desabilitada
- E-mails inválidos continuam sendo bloqueados

## API REST

### Quartos

**GET /rooms**
Lista todos os quartos disponíveis.

### Reservas

**POST /reservations**
Cria uma nova reserva (status PENDING).

**GET /reservations/{id}**
Consulta uma reserva.

**POST /reservations/{id}/confirmation**
Confirma uma reserva pendente (envia e-mail).

**POST /reservations/{id}/check-in**
Realiza check-in de reserva confirmada.

**POST /reservations/{id}/check-out**
Realiza check-out e retorna comprovante.

**POST /reservations/{id}/cancellation**
Cancela uma reserva (com política de reembolso).

**GET /reservations/{id}/document**
Obtém segunda via do comprovante.

### Mensagens

**GET /messages**
Lista todas as mensagens enviadas (incluindo bloqueadas).

### Relatórios

**GET /reports/occupancy?date=2027-07-10**
Relatório de ocupação para uma noite específica (CSV).

**GET /reports/revenue?month=2027-07**
Relatório financeiro para um mês (CSV).

## Regras de Negócio

### Quartos

- 101, 102: STANDARD (2 pessoas) - R$ 249,90
- 201, 202: DELUXE (3 pessoas) - R$ 389,90
- 301: SUITE (4 pessoas) - R$ 579,90

### Alta Temporada (+20%)

- 15/12 até fim de fevereiro
- Mês de julho inteiro

### Planos de Tarifa

- **STANDARD**: Preço normal, reembolsável
- **NON_REFUNDABLE**: 15% desconto, não reembolsável

### Cálculo de Preços

- Arredondamento "meio para o par" (banker's rounding) em cada etapa
- Preço de cada noite → soma → desconto → total

### Fluxo de Estados

```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
    ↓
CANCELLED
```

### E-mails

- Enviados na confirmação da reserva
- Validação de formato do endereço
- Sistema extensível de regras de processamento

## Testes

```bash
mvn test
```

O projeto inclui testes para:
- Cálculo de preços e alta temporada
- Sistema de regras de mensageria
- Validação de e-mails
- Pipeline de processamento

## Arquitetura

```
com.pousada.marealta/
├── domain/              # Entidades de negócio
├── repository/          # Acesso a dados
├── service/             # Lógica de negócio
├── messaging/           # Sistema extensível de regras
│   ├── MessageRule      # Interface para novas regras
│   ├── MessagePipeline  # Orquestrador de regras
│   └── rules/           # Regras implementadas
├── controller/          # Endpoints REST
├── dto/                 # Objetos de transferência
└── config/              # Configuração e banco
```

## Licença

Propriedade da Pousada Maré Alta.
