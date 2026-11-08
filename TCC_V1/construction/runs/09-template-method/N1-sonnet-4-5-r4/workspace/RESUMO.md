# Sistema de Reservas - Pousada Maré Alta

## ✅ Implementado

### Funcionalidades Principais

#### Gestão de Reservas
- [x] Criação de reservas com validação completa
- [x] Confirmação com pagamento
- [x] Check-in e check-out
- [x] Cancelamento com política de reembolso
- [x] Validação de disponibilidade (sem sobreposição)
- [x] Validação de capacidade do quarto

#### Cálculo de Preços
- [x] Alta temporada (15/12 a 28/02 + julho): +20%
- [x] Plano STANDARD: preço normal, reembolso total
- [x] Plano NON_REFUNDABLE: -15% desconto, sem reembolso
- [x] Arredondamento "meio para o par" (HALF_EVEN) em cada etapa
- [x] Cálculo correto de diárias por noite

#### Quartos
- [x] 5 quartos fixos pré-configurados:
  - 101, 102: STANDARD, 2 pessoas, R$ 249,90
  - 201, 202: DELUXE, 3 pessoas, R$ 389,90
  - 301: SUITE, 4 pessoas, R$ 579,90

#### Comprovantes e E-mails
- [x] Geração de comprovante formatado
- [x] E-mail de confirmação (armazenado internamente)
- [x] Segunda via de comprovante
- [x] Formatação de CPF (111.222.333-44)
- [x] Formatação de valores (R$ 1.234,56)
- [x] Formatação de datas (10/03/2027)

#### Relatórios CSV
Todos com estrutura comum: título + cabeçalho + linhas + total

1. **Ocupação** (`/reports/occupancy?date=YYYY-MM-DD`)
   - Reservas pagas que ocupam quartos na noite especificada
   - Colunas: room, type, reservation, check_in, check_out
   - Total: quartos ocupados / total de quartos

2. **Financeiro** (`/reports/revenue?month=YYYY-MM`)
   - Reservas que começam no mês e geraram receita
   - Colunas: reservation, room, check_in, check_out, status, amount
   - Total: soma de valores líquidos (pago - reembolsado)

3. **Taxa de Turismo** (`/reports/tourism-tax?month=YYYY-MM`)
   - R$ 4,50 por hóspede por noite
   - Reservas pagas que começam no mês
   - Colunas: reservation, room, check_in, check_out, guests, nights, tax
   - Total: soma das taxas

4. **Planos de Tarifa** (`/reports/rate-plans?month=YYYY-MM`)
   - Agrupamento por plano de tarifa
   - Colunas: rate_plan, reservations, amount
   - Total: quantidade e valor total

### API REST

#### Endpoints de Quartos
- `GET /rooms` - Lista todos os quartos

#### Endpoints de Reservas
- `POST /reservations` - Cria reserva (retorna 201)
- `GET /reservations/{id}` - Busca reserva
- `POST /reservations/{id}/confirmation` - Confirma e envia e-mail
- `POST /reservations/{id}/cancellation` - Cancela com política de reembolso
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out e retorna comprovante
- `GET /reservations/{id}/document` - Segunda via do comprovante

#### Endpoints de Mensagens
- `GET /messages` - Lista mensagens enviadas

#### Endpoints de Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD`
- `GET /reports/revenue?month=YYYY-MM`
- `GET /reports/tourism-tax?month=YYYY-MM`
- `GET /reports/rate-plans?month=YYYY-MM`

### Códigos de Erro
- `400 INVALID_REQUEST` - Dados inválidos, datas incorretas, JSON malformado
- `404 ROOM_NOT_FOUND` - Quarto não existe
- `404 RESERVATION_NOT_FOUND` - Reserva não existe
- `409 ROOM_UNAVAILABLE` - Quarto já reservado nas datas
- `409 OPERATION_NOT_ALLOWED` - Operação inválida para o status atual
- `422 CAPACITY_EXCEEDED` - Mais hóspedes que a capacidade

### Persistência
- PostgreSQL 18
- Inicialização automática do schema
- População automática dos quartos
- Três tabelas: rooms, reservations, messages

### Build e Deploy
- Java 21
- Javalin 6.7.0
- Maven com shade plugin
- JAR executável autônomo (~9MB)
- Configuração via variáveis de ambiente

## 🎯 Arquitetura

### Princípios Aplicados (CLAUDE.md)

1. **Cada novo caso entra em um lugar só**
   - Novos relatórios: apenas estender `ReportGenerator`
   - Novas regras de preço: apenas modificar `PricingService`
   - Novos estados de reserva: apenas adicionar no enum

2. **Sequências repetidas escritas uma vez**
   - Template para todos os relatórios CSV
   - Sequência de cálculo de preços centralizada
   - Validações de reserva em único ponto

3. **Organização onde a variação existe**
   - Relatórios: cada um define suas colunas únicas
   - Planos de tarifa: estratégia de desconto/reembolso
   - Quartos: dados fixos no banco, sem camada de abstração desnecessária

### Estrutura Modular
```
pricing/    - Cálculos isolados de preços
service/    - Orquestração de lógica de negócio
repository/ - Acesso a dados
report/     - Geração de relatórios com template comum
api/        - Controllers REST e DTOs
```

## 🧪 Qualidade

### Testes Implementados
- ✅ Testes unitários de cálculo de preços
- ✅ Testes de detecção de alta temporada
- ✅ Testes de geração de documentos
- ✅ Testes de integração do fluxo completo
- ✅ `mvn verify` passa com sucesso

### Documentação
- README.md: Como executar
- ARCHITECTURE.md: Decisões de design
- test-api.sh: Script de testes manuais
- docker-compose.yml: Infraestrutura local

## 🚀 Como usar

```bash
# Iniciar banco de dados
docker-compose up -d

# Compilar
mvn package

# Executar
java -jar target/pousada-reservas-1.0.0.jar

# Testar
./test-api.sh
```

## 📝 Notas de Implementação

### Decisões Técnicas

1. **JDBC direto sem ORM**: Modelo simples, queries diretas, performance previsível
2. **Arredondamento HALF_EVEN**: Padrão financeiro, evita viés
3. **Template Method para relatórios**: Facilita adição de novos relatórios
4. **Mensagens em memória**: Requisito atual, fácil substituir por serviço real
5. **Inicialização automática**: Banco self-service, sem scripts SQL separados

### Extensibilidade Futura

Já preparado para:
- Adicionar linha de data de geração nos relatórios (um ponto de mudança)
- Novos relatórios (relatório de cancelamentos, hóspedes por estado)
- Mudança no valor da taxa de turismo (constante facilmente modificável)
- Novos planos de tarifa (adicionar enum e regra em PricingService)

### Validações de Negócio

Todas implementadas:
- ✅ Check-out > check-in
- ✅ Hóspedes >= 1 e <= capacidade
- ✅ Sem sobreposição de reservas (exceto canceladas)
- ✅ Transições de estado válidas
- ✅ Política de cancelamento conforme plano
