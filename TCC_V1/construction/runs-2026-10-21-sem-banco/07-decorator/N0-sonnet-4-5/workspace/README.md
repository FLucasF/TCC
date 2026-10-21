# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas desenvolvido em Java 21 com Javalin 6.7.0.

## Funcionalidades

### Gestão de Quartos
- 5 quartos com diferentes categorias (STANDARD, DELUXE, SUITE)
- Capacidade e preços configurados
- Alta temporada com acréscimo de 20%

### Gestão de Reservas
- Criação de reservas com validações
- Confirmação de pagamento
- Check-in e check-out
- Cancelamento com política de reembolso
- Planos de tarifa: STANDARD e NON_REFUNDABLE (15% desconto)

### Sistema de Mensagens com Regras Configuráveis
O sistema implementa um pipeline de regras para processamento de mensagens, destacando:

#### Modo de Produção
1. **Validação de e-mail**: Bloqueia mensagens para endereços inválidos
2. **Cópia para arquivo**: Todas as mensagens enviadas geram cópia para `arquivo@pousadamarealta.com.br`

#### Modo de Teste (`HOTEL_TEST_MODE=true`)
1. **Validação de e-mail**: Mantém bloqueio de endereços inválidos
2. **Redirecionamento**: Mensagens vão para `testes@pousadamarealta.com.br` com prefixo `[TESTE]`
3. **Sem cópia para arquivo**: Não gera cópias em modo de teste

### Relatórios
- **Ocupação**: Mostra quartos ocupados em uma data específica (CSV)
- **Financeiro**: Receita do mês com todas as reservas pagas (CSV)

## Build e Execução

### Compilar
```bash
mvn clean package
```

### Executar
```bash
# Modo normal
java -jar target/reservas-1.0.0.jar

# Modo de teste
HOTEL_TEST_MODE=true java -jar target/reservas-1.0.0.jar

# Porta customizada
PORT=8080 java -jar target/reservas-1.0.0.jar
```

### Testes
```bash
mvn verify
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria nova reserva
- `GET /reservations/{id}` - Consulta reserva
- `POST /reservations/{id}/confirmation` - Confirma pagamento
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro (CSV)

## Arquitetura

### Padrão de Mensagens
O sistema utiliza o padrão Pipeline para processar mensagens, permitindo adicionar novas regras facilmente:

```java
MessageRule -> EmailValidationRule
            -> TestModeRule (modo teste)
            -> ArchiveCopyRule (modo produção)
```

Cada instalação pode configurar diferentes combinações de regras sem alterar o código.

## Tecnologias
- Java 21
- Javalin 6.7.0
- Jackson para JSON
- JUnit 5 e AssertJ para testes
- Maven para build
