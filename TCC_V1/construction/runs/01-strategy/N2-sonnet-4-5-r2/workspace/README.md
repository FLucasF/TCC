# Sistema de Reservas - Pousada Maré Alta

Sistema de gestão de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Requisitos

- Java 21
- PostgreSQL 18
- Maven 3.8+

## Configuração

O sistema requer as seguintes variáveis de ambiente:

- `DB_URL` - URL do banco PostgreSQL no formato `jdbc:postgresql://host:port/database`
- `DB_USER` - Usuário do banco de dados
- `DB_PASSWORD` - Senha do banco de dados
- `PORT` - Porta HTTP (opcional, padrão: 7070)

## Compilação

```bash
mvn clean package
```

## Execução

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="senha"
export PORT=7070

java -jar target/reservas-1.0.0.jar
```

O sistema iniciará na porta especificada e criará automaticamente as tabelas necessárias no banco de dados.

## Planos de Tarifa

O sistema suporta 5 planos de tarifa:

1. **STANDARD** - Plano normal com tarifa cheia e reembolso total
2. **NON_REFUNDABLE** - 15% de desconto, sem reembolso
3. **CORPORATE** - Desconto corporativo (12%, 18% ou 25% conforme empresa)
4. **WEEKEND_PACKAGE** - Pacote de fim de semana (sexta a domingo) com preço fixo e reembolso de 50%
5. **LONG_STAY** - Estadia longa (7+ noites) com 10% de desconto e reembolso total

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria nova reserva
- `GET /reservations/{id}` - Consulta reserva
- `POST /reservations/{id}/confirmation` - Confirma reserva
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out e retorna comprovante
- `GET /reservations/{id}/document` - Obtém segunda via do comprovante

### Mensagens

- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=yyyy-MM-dd` - Relatório de ocupação para uma data
- `GET /reports/revenue?month=yyyy-MM` - Relatório financeiro de um mês

## Arquitetura

O sistema foi desenvolvido seguindo o **Strategy Pattern** para os planos de tarifa, permitindo adicionar novos planos sem modificar código existente. Cada plano implementa a interface `RatePlan` com métodos para:

- Validação das regras do plano
- Cálculo do subtotal
- Cálculo do desconto
- Cálculo do reembolso

## Estrutura do Projeto

```
src/main/java/com/pousadamarealta/
├── domain/
│   ├── model/              # Entidades de domínio
│   └── service/            # Serviços de negócio
│       └── rateplan/       # Implementações dos planos de tarifa
├── infrastructure/
│   ├── database/           # Repositórios e acesso a dados
│   └── web/                # Controllers e DTOs HTTP
└── Application.java        # Ponto de entrada da aplicação
```
