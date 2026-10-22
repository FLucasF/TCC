# Pousada Maré Alta - Sistema de Reservas

## ✅ Sistema Completo e Funcional

O sistema foi implementado com todas as funcionalidades solicitadas e está pronto para uso.

### 🏨 Funcionalidades Implementadas

**Quartos (5 total):**
- 101, 102: STANDARD (2 pessoas, R$ 249,90/noite)
- 201, 202: DELUXE (3 pessoas, R$ 389,90/noite)
- 301: SUITE (4 pessoas, R$ 579,90/noite)

**Alta Temporada (+20%):**
- 15/12 até fim de fevereiro
- Julho inteiro

**Planos de Tarifa (5 planos):**
1. **STANDARD**: Tarifa normal, reembolso total
2. **NON_REFUNDABLE**: 15% desconto, sem reembolso
3. **CORPORATE**: Convênios com empresas
   - TECNOSUL: 12% desconto
   - AGROVALE: 18% desconto
   - FARMAVIDA: 25% desconto
4. **WEEKEND_PACKAGE**: Sexta a domingo (2 noites)
   - STANDARD: R$ 520,00
   - DELUXE: R$ 820,00
   - SUITE: R$ 1.200,00
   - Reembolso de 50% no cancelamento
5. **LONG_STAY**: 7+ noites, 10% desconto, reembolso total

**Ciclo de Vida da Reserva:**
- PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- Cancelamento possível antes do check-in (CANCELLED)

**Notificações:**
- E-mail de confirmação (armazenado no banco)
- Comprovante em texto formatado
- Segunda via disponível via API

**Relatórios:**
- Ocupação por data (CSV)
- Financeiro por mês (CSV)

### 🛠️ Tecnologias

- **Java 21**
- **Javalin 6.7.0** (sem Spring)
- **PostgreSQL 18**
- **Maven** para build
- **JUnit 5** e **AssertJ** para testes

### 📦 Build e Execução

**Compilar e empacotar:**
```bash
mvn package
```

**Executar:**
```bash
java -jar target/reservations-1.0.0.jar
```

**Com variáveis de ambiente:**
```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=postgres \
DB_PASSWORD=senha \
PORT=8080 \
java -jar target/reservations-1.0.0.jar
```

**Testar:**
```bash
mvn verify
```

Para rodar os testes com Docker:
```bash
./run-tests.sh
```

### 📝 Estrutura do Código

```
src/main/java/com/pousadamareaalta/
├── App.java                    # Classe principal
├── api/                        # Camada REST
│   ├── ApiController.java      # Rotas e handlers
│   ├── ApiMapper.java          # Conversão de DTOs
│   ├── ErrorResponse.java      # Respostas de erro
│   └── dto/                    # Objetos de transferência
├── exception/                  # Exceções customizadas
├── model/                      # Modelos de domínio
│   ├── Room.java
│   ├── Reservation.java
│   ├── Message.java
│   └── ...
├── pricing/                    # Estratégias de precificação
│   ├── RatePlanStrategy.java  # Interface
│   ├── StandardRatePlan.java
│   ├── CorporateRatePlan.java
│   ├── WeekendPackageRatePlan.java
│   ├── LongStayRatePlan.java
│   └── ...
├── repository/                 # Acesso ao banco
│   ├── Database.java           # Inicialização do schema
│   ├── ReservationRepository.java
│   └── MessageRepository.java
└── service/                    # Lógica de negócio
    ├── ReservationService.java
    ├── RoomService.java
    ├── MessageService.java
    └── ReportService.java
```

### 🧪 Testes

**Testes Unitários:**
- `PricingTest.java`: Valida todos os cálculos de preço conforme especificado
  - Alta temporada
  - Todos os planos de tarifa
  - Arredondamento (half-to-even)

**Testes de Integração:**
- `ReservationSystemIT.java`: Testa todos os endpoints da API
  - Criação de reservas
  - Confirmação e cancelamento
  - Check-in e check-out
  - Relatórios
  - Tratamento de erros

Todos os testes estão passando ✅

### 🎯 Validações Implementadas

- ✅ Número de hóspedes entre 1 e capacidade do quarto
- ✅ Check-out depois do check-in
- ✅ Sem dupla reserva do mesmo quarto
- ✅ Validações específicas por plano:
  - CORPORATE: código de empresa obrigatório e válido
  - WEEKEND_PACKAGE: sexta a domingo (exatamente 2 noites)
  - LONG_STAY: mínimo 7 noites
- ✅ Transições de estado válidas
- ✅ Cancelamento só antes do check-in

### 💰 Cálculos Validados

Todos os exemplos fornecidos foram testados e confirmados:

1. **Tecnosul DELUXE 10-12/03/2027:**
   - Diárias: R$ 779,80
   - Desconto: R$ 93,58
   - Total: R$ 686,22 ✓

2. **Pacote DELUXE 12-14/03/2027:**
   - Total: R$ 820,00
   - Reembolso: R$ 410,00 ✓

3. **Long Stay STANDARD 01-08/03/2027:**
   - Diárias: R$ 1.749,30
   - Desconto: R$ 174,93
   - Total: R$ 1.574,37 ✓

4. **Alta temporada STANDARD 10-12/07/2027:**
   - Total: R$ 599,76 ✓

### 📚 Documentação

- `README.md`: Instruções gerais
- `API_EXAMPLES.md`: Exemplos de uso de todos os endpoints
- `RESUMO.md`: Este arquivo

### 🔄 Próximos Passos (se necessário)

O sistema está pronto para produção. Para evoluções futuras:

1. **Novos planos de tarifa**: Basta criar uma nova classe implementando `RatePlanStrategy` e adicionar ao enum `RatePlan`
2. **Envio real de e-mails**: Substituir `MessageService` por implementação SMTP
3. **Interface web**: O backend está pronto para receber chamadas do frontend
4. **Autenticação**: Adicionar JWT ou sessões conforme necessário

### 🚀 Pronto para Deploy

O sistema está completamente funcional e validado. Basta:

1. Configurar o PostgreSQL em produção
2. Definir as variáveis de ambiente
3. Executar o JAR

**O sistema implementa 100% das especificações fornecidas e está pronto para uso!**
