# Pousada Maré Alta - Sistema de Reservas

Sistema de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin.

## Compilação e Execução

### Compilar o projeto
```bash
mvn package
```

### Executar o servidor
```bash
java -jar target/reservas-1.0-SNAPSHOT.jar
```

O servidor inicia na porta definida pela variável de ambiente `PORT`, ou na porta 7070 se não definida.

### Executar os testes
```bash
mvn verify
```

## Arquitetura

O sistema foi desenvolvido seguindo os padrões de design GoF:

### Strategy Pattern
- **Planos de Tarifa**: Cada plano (STANDARD, NON_REFUNDABLE) é uma estratégia com sua lógica de desconto e reembolso
- **Relatórios**: Cada relatório é uma estratégia que implementa a interface `ReportGenerator`, facilitando a adição de novos relatórios

### Estrutura do Projeto
```
src/main/java/com/pousadamarealta/
├── domain/              # Modelos de domínio
│   ├── Room, Reservation, Message
│   ├── RatePlan (interface) e implementações
│   └── SeasonCalculator
├── repository/          # Repositórios em memória
├── service/            # Lógica de negócio
│   └── report/         # Estratégias de relatório
├── api/
│   ├── controller/     # Controllers REST
│   ├── dto/           # DTOs da API
│   └── mapper/        # Conversores domain ↔ DTO
└── App.java           # Classe principal
```

## API REST

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma e envia e-mail
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out e retorna comprovante
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Lista mensagens enviadas

### Relatórios (CSV)
- `GET /reports/occupancy?date=2027-07-10` - Ocupação por noite
- `GET /reports/revenue?month=2027-07` - Receita por mês
- `GET /reports/tourism-tax?month=2027-07` - Taxa de turismo por mês
- `GET /reports/rate-plans?month=2027-07` - Análise por plano de tarifa

## Regras de Negócio

### Alta Temporada
- 15 de dezembro a fim de fevereiro
- Julho inteiro
- Acréscimo de 20% na diária

### Planos de Tarifa
- **STANDARD**: Sem desconto, com reembolso
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso

### Taxa de Turismo
- R$ 4,50 por hóspede por noite
- Valor configurável em `TourismTaxReportGenerator`

### Arredondamento
- Bankers Rounding (HALF_EVEN) em todas as operações monetárias
- Arredondamento a cada passo do cálculo

## Extensibilidade

### Adicionar novo relatório
1. Criar classe que implementa `ReportGenerator`
2. Registrar no `ReportService`
3. Adicionar endpoint no `ReportController`

### Adicionar novo plano de tarifa
1. Adicionar valor no enum `RatePlanType`
2. Criar classe que implementa `RatePlan`
3. Registrar no `RatePlanFactory`

### Alterar taxa de turismo
Editar a constante `TAX_PER_GUEST_PER_NIGHT` em `TourismTaxReportGenerator`.
