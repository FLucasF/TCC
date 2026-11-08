# Sistema de Reservas - Pousada Maré Alta

## Compilação e Execução

### Pré-requisitos
- Java 21
- PostgreSQL 18
- Maven 3.8+

### Compilação
```bash
mvn clean package
```

Isso gerará um JAR executável em `target/reservation-system.jar`.

### Executação
O sistema requer três variáveis de ambiente:
- `DB_URL`: URL de conexão do PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco
- `DB_PASSWORD`: Senha do banco
- `PORT` (opcional): Porta para o servidor (padrão: 7070)

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="senha"
export PORT="7070"

java -jar target/reservation-system.jar
```

O servidor estará disponível em `http://localhost:7070`.

## Testes

### Executar todos os testes
```bash
mvn test
```

### Verificar cobertura
```bash
mvn verify
```

## Estrutura do Projeto

- `src/main/java/com/pousadamarealta/`:
  - `domain/`: Modelos de domínio (Room, Reservation, etc.)
  - `database/`: DAOs e acesso a dados
  - `pricing/`: Implementação dos planos de tarifa (Strategy Pattern)
  - `service/`: Lógica de negócio (reservas, relatórios)
  - `api/`: Endpoints REST (Javalin)

## Endpoints da API

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Criar nova reserva
- `GET /reservations/{id}` - Obter dados da reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `POST /reservations/{id}/check-in` - Check-in
- `POST /reservations/{id}/check-out` - Check-out (retorna comprovante)
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Lista de e-mails enviados

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Ocupação de um dia (CSV)
- `GET /reports/revenue?month=YYYY-MM` - Receita de um mês (CSV)

## Planos de Tarifa

O sistema suporta 5 planos de tarifa com regras diferentes:

1. **STANDARD**: Sem desconto, com temporada alta (+20%)
2. **NON_REFUNDABLE**: 15% desconto, sem reembolso
3. **CORPORATE**: Desconto por empresa (TECNOSUL 12%, AGROVALE 18%, FARMAVIDA 25%), reembolso total
4. **WEEKEND_PACKAGE**: Sexta a domingo, preço fixo (R$ 520 standard, R$ 820 deluxe, R$ 1.200 suite), reembolso 50%
5. **LONG_STAY**: 7+ noites, 10% desconto, reembolso total

## Quartos

- **101, 102**: STANDARD (2 pessoas) - R$ 249,90/noite
- **201, 202**: DELUXE (3 pessoas) - R$ 389,90/noite
- **301**: SUITE (4 pessoas) - R$ 579,90/noite

## Temporada Alta

- Dezembro: 15 a 31
- Janeiro e Fevereiro: até 28
- Julho: mês inteiro

Durante a temporada alta, as diárias custam 20% a mais.

## Arquitetura

O projeto usa o padrão **Strategy** para implementação dos planos de tarifa. Cada plano é uma classe que implementa a interface `RatePlan` e pode ser facilmente estendido para novos planos sem modificar o código existente.

Todos os valores monetários são arredondados com **banker's rounding** (HALF_EVEN) para centavos em cada operação.
