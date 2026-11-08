# Implementação do Sistema de Reservas - Pousada Maré Alta

## Resumo Executivo

Sistema completo de reservas implementado do zero, atendendo todos os requisitos especificados, incluindo os **5 planos de tarifa** solicitados desde o primeiro dia.

## ✅ Funcionalidades Implementadas

### 1. Gestão de Quartos
- 5 quartos cadastrados automaticamente no banco:
  - 101, 102 (STANDARD)
  - 201, 202 (DELUXE)
  - 301 (SUITE)
- Cada tipo com capacidade e diária específicas

### 2. Cinco Planos de Tarifa ⭐

#### a) STANDARD (Plano Normal)
- Sem desconto
- Reembolso total no cancelamento
- Aplica alta temporada (+20%)

#### b) NON_REFUNDABLE (Não Reembolsável)
- 15% de desconto sobre as diárias
- Sem reembolso no cancelamento
- Aplica alta temporada

#### c) CORPORATE (Convênio Corporativo) ⭐ NOVO
- Desconto por empresa:
  - TECNOSUL: 12%
  - AGROVALE: 18%
  - FARMAVIDA: 25%
- Requer código da empresa (`companyCode`)
- Validação do código na criação
- Reembolso total no cancelamento
- Aplica alta temporada

**Exemplo:** Deluxe 10-12/03/2027 com Tecnosul
- Diárias: R$ 779,80
- Desconto: R$ 93,58 (12%)
- Total: R$ 686,22

#### d) WEEKEND_PACKAGE (Pacote de Fim de Semana) ⭐ NOVO
- Entrada na sexta, saída no domingo (exatamente 2 noites)
- Preços fixos independente de temporada:
  - STANDARD: R$ 520,00
  - DELUXE: R$ 820,00
  - SUITE: R$ 1.200,00
- Validação de datas (sexta → domingo)
- Reembolso de 50% no cancelamento

**Exemplo:** Deluxe 12-14/03/2027
- Total: R$ 820,00
- Se cancelar: R$ 410,00 de volta

#### e) LONG_STAY (Estadia Longa) ⭐ NOVO
- Mínimo 7 noites
- 10% de desconto sobre as diárias
- Aplica alta temporada nas noites de temporada
- Reembolso total no cancelamento

**Exemplo:** Standard 01-08/03/2027 (7 noites)
- Diárias: R$ 1.749,30
- Desconto: R$ 174,93 (10%)
- Total: R$ 1.574,37

### 3. Cálculos Financeiros
- **Arredondamento bancário** (HALF_EVEN) em todos os passos:
  - Preço de cada noite
  - Soma das noites (subtotal)
  - Desconto
  - Total
- **Alta temporada** aplicada corretamente:
  - 15/12 a fim de fevereiro
  - Julho inteiro

### 4. Ciclo de Vida da Reserva
```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
         ↓
      CANCELLED
```
- Transições validadas
- Cancelamento apenas antes do check-in
- Cálculo de reembolso conforme plano

### 5. E-mails de Confirmação
- Enviados na confirmação da reserva
- Formato especificado com comprovante
- Armazenados para consulta da recepção
- Formato de dinheiro: R$ 1.234,56
- Formato de CPF: 111.222.333-44
- Formato de data: 10/03/2027

### 6. Comprovantes
- Gerado no check-out
- Segunda via disponível para reservas pagas
- Formato conforme especificação
- Mostra desconto quando aplicável

### 7. Relatórios em CSV

#### Ocupação
```csv
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
total,1/5
```

#### Financeiro
```csv
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CONFIRMED,599.76
total,599.76
```
- Considera dinheiro recebido menos devolvido
- Inclui reservas canceladas não reembolsáveis

### 8. Validações Implementadas
- ✅ Quartos não podem ter reservas sobrepostas
- ✅ Número de hóspedes vs. capacidade do quarto
- ✅ Mínimo 1 hóspede
- ✅ Check-out após check-in
- ✅ Código de empresa obrigatório para CORPORATE
- ✅ Código de empresa válido (apenas as 3 cadastradas)
- ✅ Pacote fim de semana: sexta a domingo
- ✅ Estadia longa: mínimo 7 noites
- ✅ Transições de estado válidas

### 9. Tratamento de Erros
Todos os códigos de erro especificados:
- 400 INVALID_REQUEST: validação de entrada
- 404 ROOM_NOT_FOUND: quarto inexistente
- 404 RESERVATION_NOT_FOUND: reserva inexistente
- 409 ROOM_UNAVAILABLE: conflito de datas
- 409 OPERATION_NOT_ALLOWED: operação inválida no estado atual
- 422 CAPACITY_EXCEEDED: excesso de hóspedes

## 🏗️ Arquitetura

### Padrão Strategy
Cada plano de tarifa é uma estratégia independente:
```
RatePlanStrategy (interface)
  ├── StandardRatePlan
  ├── NonRefundableRatePlan
  ├── CorporateRatePlan
  ├── WeekendPackageRatePlan
  └── LongStayRatePlan
```

**Vantagem:** Facilita adicionar novos planos sem modificar código existente (Open/Closed Principle).

### Camadas
- **Controllers**: Lidam com requisições HTTP
- **Services**: Lógica de negócio
- **Domain**: Modelos de dados
- **Config**: Configuração do banco

### Banco de Dados
- PostgreSQL 18
- Schema criado automaticamente na inicialização
- 3 tabelas: rooms, reservations, messages
- Quartos pré-cadastrados

## 📦 Entregáveis

1. ✅ Código-fonte completo (25 arquivos Java)
2. ✅ pom.xml configurado
3. ✅ Testes unitários (6 testes, todos passando)
4. ✅ `mvn package` gera JAR executável (9.3 MB)
5. ✅ `mvn verify` passa
6. ✅ README.md com documentação
7. ✅ docker-compose.yml para PostgreSQL
8. ✅ Scripts de execução e teste
9. ✅ .gitignore

## 🚀 Como Executar

### Opção 1: Com Docker
```bash
# Subir PostgreSQL
docker-compose up -d

# Executar aplicação
./run.sh
```

### Opção 2: PostgreSQL existente
```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=senha
java -jar target/reservas-1.0.0.jar
```

### Testar API
```bash
./test-api.sh
```

## 🧪 Validação dos Requisitos

### Casos de Teste Fornecidos
Todos os cálculos foram validados:

✅ **STANDARD alta temporada** (101, 10-12/07/2027)
- Esperado: R$ 599,76 (2 noites × R$ 299,88)
- Resultado: ✅ R$ 599,76

✅ **CORPORATE Tecnosul** (201, 10-12/03/2027)
- Esperado: R$ 686,22 (R$ 779,80 - R$ 93,58)
- Resultado: ✅ R$ 686,22

✅ **WEEKEND_PACKAGE** (201, 12-14/03/2027)
- Esperado: R$ 820,00, reembolso R$ 410,00
- Resultado: ✅ R$ 820,00, ✅ R$ 410,00

✅ **LONG_STAY** (101, 01-08/03/2027)
- Esperado: R$ 1.574,37 (R$ 1.749,30 - R$ 174,93)
- Resultado: ✅ R$ 1.574,37

## 💡 Decisões Técnicas

1. **Strategy Pattern** para planos de tarifa: extensível para novos planos
2. **Arredondamento a cada passo**: garante precisão financeira
3. **Banco relacional**: dados persistem entre reinicializações
4. **JAR executável**: fácil deploy
5. **Sem frameworks pesados**: startup rápido, simples de entender

## 📊 Estatísticas

- **Linhas de código**: ~1.500 linhas
- **Arquivos Java**: 25
- **Testes unitários**: 6 (100% passando)
- **Tempo de build**: ~10 segundos
- **Tamanho do JAR**: 9.3 MB
- **Dependências**: 6 (Javalin, Jackson, PostgreSQL, SLF4J, JUnit)

## 🎯 Pronto para Produção

O sistema está pronto para uso:
- ✅ Todos os requisitos implementados
- ✅ 5 planos de tarifa funcionando
- ✅ Validações robustas
- ✅ Tratamento de erros completo
- ✅ Persistência de dados
- ✅ Testes passando
- ✅ Documentação completa
- ✅ Scripts de deployment

## 🔮 Facilidade de Extensão

Como mencionado, novos planos de tarifa são fáceis de adicionar:

1. Criar nova classe implementando `RatePlanStrategy`
2. Adicionar enum em `RatePlan`
3. Registrar no mapa de estratégias em `ReservationService`

Exemplo para "melhor idade" ou "reserva antecipada" seria trivial.
