# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para pousadas, desenvolvido em Java 21 com Javalin.

> **Sistema pronto para produção** com gestão completa de reservas, cálculo automático de preços com alta temporada, sistema de mensagens configurável com regras plugáveis e relatórios CSV.

## Características

- **Gestão de Quartos**: 5 quartos com diferentes categorias (STANDARD, DELUXE, SUITE)
- **Cálculo de Preços**: Preços dinâmicos com alta temporada e planos de tarifa
- **Fluxo de Reservas**: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- **Sistema de Mensagens Configurável**: Regras plugáveis para validação, teste e arquivamento
- **Relatórios**: CSV de ocupação e receita
- **API REST**: Endpoints completos para integração com site

## Sistema de Mensagens

O sistema implementa um pipeline configurável de regras para processamento de mensagens:

### Regras Implementadas

1. **Validação de E-mail**: Bloqueia e-mails inválidos (sem @, sem domínio, etc.)
2. **Modo de Teste**: Redireciona mensagens para endereço de teste quando `HOTEL_TEST_MODE=true`
3. **Cópia para Arquivo**: Gera cópia automática para arquivo@pousadamarealta.com.br

### Arquitetura Extensível

Novas regras podem ser adicionadas implementando a interface `MessageRule`:

```java
public interface MessageRule {
    ProcessingResult process(OutgoingMessage message);
}
```

Cada regra pode:
- Bloquear a mensagem
- Modificar destinatário, assunto ou corpo
- Gerar mensagens adicionais

## Configuração

### Variáveis de Ambiente

- `DB_URL`: URL JDBC do PostgreSQL (padrão: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco (padrão: `postgres`)
- `DB_PASSWORD`: Senha do banco (padrão: `postgres`)
- `PORT`: Porta do servidor (padrão: `7070`)
- `HOTEL_TEST_MODE`: Modo de teste (`true` ou ausente)

## Build e Execução

### Desenvolvimento Local

```bash
# Compilar e testar
mvn clean package

# Executar (precisa de PostgreSQL rodando)
java -jar target/reservas-1.0.0.jar

# Rodar testes unitários
mvn test

# Rodar testes de integração (requer Docker)
mvn verify
```

### Com Docker Compose (Recomendado)

```bash
# Subir tudo (PostgreSQL + API)
docker-compose up

# Apenas build
docker-compose build

# Parar
docker-compose down
```

### Configuração Mínima

```bash
# PostgreSQL via Docker
docker run -d \
  --name postgres \
  -e POSTGRES_DB=pousada \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:18

# Executar aplicação
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=postgres
java -jar target/reservas-1.0.0.jar
```

O sistema estará disponível em `http://localhost:7070`

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria reserva
- `GET /reservations/{id}` - Busca reserva
- `POST /reservations/{id}/confirmation` - Confirma reserva
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Busca comprovante (segunda via)

### Mensagens
- `GET /messages` - Lista mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro

## Detalhes Técnicos

### Regras de Negócio

- **Arredondamento**: HALF_EVEN (meio para par) em todos os cálculos monetários
- **Alta Temporada**: 15/dez a fim/fev + julho inteiro (20% adicional)
- **Desconto Não Reembolsável**: 15% sobre subtotal
- **Banco de Dados**: PostgreSQL 18 com inicialização automática de schema
- **Validação**: E-mail inválido é bloqueado (precisa ter @ e domínio com ponto)
- **Cancelamento**: STANDARD reembolsa 100%, NON_REFUNDABLE não reembolsa

### Arquitetura

```
com.pousadamarealta/
├── model/              # Entidades de domínio
├── dto/                # DTOs para API REST
├── repository/         # Acesso ao banco de dados
├── service/            # Lógica de negócio
├── controller/         # Endpoints REST
├── messaging/          # Sistema de mensagens
│   └── rules/          # Regras configuráveis
├── exception/          # Exceções customizadas
├── mapper/             # Conversão model ↔DTO
└── database/           # Inicialização do DB
```

### Stack Tecnológica

- **Java 21**: Records, Pattern Matching, Text Blocks
- **Javalin 6.7.0**: Framework web minimalista
- **PostgreSQL 18**: Banco de dados relacional
- **HikariCP 6.2.1**: Connection pooling
- **Jackson 2.18.2**: Serialização JSON
- **JUnit 5 + Testcontainers**: Testes unitários e de integração
- **Maven Shade**: Uber JAR executável

## Documentação Adicional

- [EXEMPLOS.md](EXEMPLOS.md) - Exemplos detalhados de uso da API com curl
- [Dockerfile](Dockerfile) - Build multi-stage para produção
- [docker-compose.yml](docker-compose.yml) - Orquestração completa

## Testes

### Cobertura

- ✅ **PricingServiceTest**: Cálculo de preços, alta temporada, descontos
- ✅ **MessageRulesTest**: Validação de e-mail, modo de teste, cópia para arquivo
- ✅ **ReservationIT**: Fluxo completo de reservas (requer Docker)

### Executar

```bash
# Apenas testes unitários (sem Docker)
mvn test

# Testes completos (requer Docker para Testcontainers)
mvn verify
```

## Arquitetura do Sistema de Mensagens

O sistema de mensagens é o diferencial deste projeto. Ele implementa um **pipeline de regras configuráveis** que permite adicionar, remover ou reordenar regras sem modificar o código core.

### Componentes

1. **OutgoingMessage**: Mensagem a ser enviada
2. **MessageRule**: Interface para regras de processamento
3. **ProcessingResult**: Resultado do processamento (bloqueada/permitida + mensagens adicionais)
4. **MessagePipeline**: Orquestrador das regras

### Regras Implementadas

- **EmailValidationRule**: Bloqueia e-mails inválidos
- **TestModeRule**: Redireciona para testes@pousadamarealta.com.br
- **ArchiveCopyRule**: Gera cópia para arquivo@pousadamarealta.com.br

### Extensibilidade

Para adicionar uma nova regra:

```java
public class NewRule implements MessageRule {
    @Override
    public ProcessingResult process(OutgoingMessage message) {
        // Sua lógica aqui
        return ProcessingResult.allow(message);
    }
}

// No MessageService:
builder.addRule(new NewRule());
```

## Licença

Este projeto foi desenvolvido para a Pousada Maré Alta como sistema proprietário de gestão de reservas.
