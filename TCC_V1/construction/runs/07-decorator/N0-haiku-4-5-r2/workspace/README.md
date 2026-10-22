# Pousada Maré Alta - Sistema de Reservas

Sistema completo de reservas para a Pousada Maré Alta, com API REST, gerenciamento de e-mails, relatórios e modo de teste.

## Características

- **API REST** para gerenciamento de quartos, reservas, mensagens e relatórios
- **Precificação dinâmica** com suporte a alta temporada (15+ dezembro, janeiro, fevereiro, julho)
- **Validação de e-mail** com tratamento de endereços inválidos
- **Modo teste** (HOTEL_TEST_MODE) para desenvolvimento seguro
- **Cópia para arquivo** com prefixo [Cópia]
- **Relatórios** de ocupação e financeiro em CSV
- **Banco PostgreSQL** com schema automático
- **Testes unitários** abrangentes

## Requisitos

- Java 21+
- PostgreSQL 18+
- Maven 3.8+

## Compilação

```bash
mvn clean compile
mvn package
```

O arquivo JAR será gerado em `target/mare-alta.jar`.

## Execução

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="senha"
export PORT=7070
export HOTEL_TEST_MODE=false

java -jar target/mare-alta.jar
```

## Variáveis de Ambiente

- `DB_URL`: URL JDBC do PostgreSQL (obrigatório)
- `DB_USER`: Usuário do banco de dados (obrigatório)
- `DB_PASSWORD`: Senha do banco de dados (obrigatório)
- `PORT`: Porta de execução (padrão: 7070)
- `HOTEL_TEST_MODE`: Ativar modo teste "true" ou "false" (padrão: false)

## API

### Quartos
- `GET /rooms` - Lista os 5 quartos da pousada

### Reservas
- `POST /reservations` - Cria nova reserva (status PENDING)
- `GET /reservations/{id}` - Retorna dados da reserva
- `POST /reservations/{id}/confirmation` - Confirma reserva (status CONFIRMED)
- `POST /reservations/{id}/cancellation` - Cancela reserva (status CANCELLED)
- `POST /reservations/{id}/check-in` - Realiza check-in (status CHECKED_IN)
- `POST /reservations/{id}/check-out` - Realiza check-out (status CHECKED_OUT)
- `GET /reservations/{id}/document` - Retorna comprovante

### Mensagens
- `GET /messages` - Lista mensagens enviadas (status SENT) e bloqueadas (status BLOCKED)

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Ocupação para uma data (CSV)
- `GET /reports/revenue?month=2027-07` - Receita para um mês (CSV)

## Estrutura de Quartos

| Quarto | Tipo | Capacidade | Diária |
|--------|------|-----------|--------|
| 101-102 | STANDARD | 2 | R$ 249,90 |
| 201-202 | DELUXE | 3 | R$ 389,90 |
| 301 | SUITE | 4 | R$ 579,90 |

## Alta Temporada

- **Dezembro** (a partir do dia 15)
- **Janeiro** (todo mês)
- **Fevereiro** (todo mês)
- **Julho** (todo mês)

Durante alta temporada, a diária sofre acréscimo de 20%.

## Planos de Tarifa

- **STANDARD**: Cancelamento com devolução 100%
- **NON_REFUNDABLE**: Desconto de 15%, cancelamento sem devolução

## E-mails

### Validação
Endereços são bloqueados se:
- Não contiverem exatamente um `@`
- Não tiverem caracteres antes do `@`
- Não tiverem domínio válido com pelo menos um ponto

### Modo Teste (HOTEL_TEST_MODE=true)
- Mensagens são redirecionadas para `testes@pousadamarealta.com.br`
- Assunto é prefixado com `[TESTE]`
- Primeira linha do corpo contém "Destinatário original: {email}"
- Sem cópia para arquivo

### Modo Produção
- Cópia automática para `arquivo@pousadamarealta.com.br`
- Assunto da cópia prefixado com `[Cópia]`
- Endereços bloqueados aparecem como BLOCKED

## Testes

```bash
mvn test
```

Testes unitários para validação de e-mails, cálculo de preços e lógica de alta temporada.
