# Sistema de Reservas Pousada Maré Alta - Sumário de Entrega

## ✅ Completo e Testado

### Build & Compilação
- ✅ Java 21 + Javalin 6.7.0 + PostgreSQL 18
- ✅ Maven com maven-shade-plugin para JAR standalone
- ✅ Testes unitários passando (PriceCalculator, EmailValidator, MessageService)
- ✅ `mvn verify` passa sem erros
- ✅ `mvn package` gera `/workspace/target/app.jar` (27MB, standalone)

### Artefatos Gerados
```
/workspace/
├── target/app.jar                 # Executável (java -jar app.jar)
├── pom.xml                        # Configuração Maven
├── src/main/java/com/pousada/
│   ├── Application.java           # Entry point
│   ├── models/                    # Room, Reservation, Message, Receipt
│   ├── services/                  # ReservationService, MessageService, ReportService
│   ├── api/                       # ApiRoutes (todos os endpoints)
│   ├── db/                        # Database (JDBC + SQL)
│   └── utils/                     # PriceCalculator, EmailValidator
├── src/test/java/com/pousada/
│   ├── PriceCalculatorTest.java   # Testes de cálculo e arredondamento
│   ├── EmailValidatorTest.java    # Testes de validação de email
│   └── MessageServiceTest.java    # Testes de e-mail (bloqueio, arquivo, teste)
├── README.md                      # Documentação completa
├── INSTRUCTIONS.md                # Guia de execução
├── run.sh                         # Script para rodar o servidor
└── test-api.sh                    # Script para testar a API
```

### Funcionalidades Implementadas

#### 1. Gestão de Quartos ✅
- 5 quartos fixos com capacidades e tarifas definidas
- Endpoints retornam dados corretos

#### 2. Ciclo de Reservas ✅
- PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- CANCELLED (com reembolso condicionado ao plano)
- Validações: capacidade, datas, disponibilidade de quarto

#### 3. Cálculo de Preços ✅
- Base: tarifa do quarto
- Alta temporada: +20% (15/12-28/02 e julho)
- Arredondamento: meio-para-par em cada passo
- Planos: STANDARD (sem desconto, reembolsável) e NON_REFUNDABLE (15% desconto)
- Testes validam: exemplo do enunciado (2 noites em julho = R$ 599,76)

#### 4. Validação de E-mail ✅
- Válido: um @, algo antes, domínio com ponto (ex: user@example.com)
- Inválido: bloqueado, aparece na lista com status BLOCKED
- Testes: 6 casos válidos, 9 casos inválidos

#### 5. Gestão de E-mails (Crítico) ✅
**Regra 1: Validação**
- E-mail inválido → bloqueado (status BLOCKED, sem envio)
- E-mail válido → enviado (status SENT)

**Regra 2: Cópia para Arquivo**
- Cada e-mail enviado gera segunda mensagem para arquivo@pousadamarealta.com.br
- Assunto: `[Cópia] {original}`
- Corpo: igual ao original
- Não se cria cópia de bloqueados

**Regra 3: Modo Teste**
- Ativado com: `export HOTEL_TEST_MODE=true`
- Comportamento:
  - E-mail redireciona para testes@pousadamarealta.com.br
  - Assunto prefixado com `[TESTE]`
  - Body inclui linha: "Destinatário original: {email_real}"
  - Sem cópia para arquivo
  - E-mails inválidos ainda são bloqueados

#### 6. Comprovantes ✅
- Enviados automaticamente na confirmação
- Formato: número, hóspede, quarto, datas, noites, diárias, desconto (se houver), total
- Dinheiro: R$ X.XXX,XX (separador de milhar com ponto, decimal com vírgula)
- Datas: DD/MM/YYYY
- Segunda via via GET /reservations/{id}/document

#### 7. Relatórios em CSV ✅
- **Ocupação**: quartos ocupados em uma noite
  - Colunas: room, type, reservation, check_in, check_out
  - Formato data: 2027-07-10
  - Linha final: total,{ocupados}/5
- **Financeiro**: receita por mês
  - Colunas: reservation, room, check_in, check_out, status, amount
  - Formato data: 2027-07-10
  - Formato valor: 599.76
  - Cálculo: amount_paid - refund (NON_REFUNDABLE cancelado fica com pousada)

#### 8. API REST (Todos implementados) ✅
```
GET    /rooms
POST   /reservations
GET    /reservations/{id}
POST   /reservations/{id}/confirmation
POST   /reservations/{id}/cancellation
POST   /reservations/{id}/check-in
POST   /reservations/{id}/check-out
GET    /reservations/{id}/document
GET    /messages
GET    /reports/occupancy?date=2027-07-10
GET    /reports/revenue?month=2027-07
```

#### 9. Tratamento de Erros ✅
```
400 INVALID_REQUEST       - Falta campo, formato inválido, datas erradas, <1 hóspede
404 ROOM_NOT_FOUND        - Quarto não existe
404 RESERVATION_NOT_FOUND - Reserva não existe
409 ROOM_UNAVAILABLE      - Quarto já reservado nas datas
409 OPERATION_NOT_ALLOWED - Estado da reserva não permite a operação
422 CAPACITY_EXCEEDED     - Mais hóspedes que capacidade
```

#### 10. Banco de Dados ✅
- PostgreSQL 18
- Tabela: reservations (com todas as colunas necessárias)
- Auto-criação na primeira execução
- JDBC com HikariCP para pool de conexões

### Extensões Preparadas

A estrutura já suporta (sem ativar por padrão):
```java
messageService.blockTimeRange("22:00-07:00")     // Não enviar madrugada
messageService.setMaxPerGuestPerDay(5)            // Limite por hóspede
messageService.enableOnlyInTestMode()             // Ativa só em teste
messageService.enableOnlyInProduction()           // Ativa só em produção
messageService.enableAlways()                     // Sempre ativa
```

## Execução

### Compilar
```bash
mvn clean verify
```

### Executar
```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="pousada"
export DB_PASSWORD="senha"
export PORT="7070"

java -jar target/app.jar
```

### Testar
```bash
# Unitários
mvn test

# Integração (curl)
./test-api.sh

# Manual
curl http://localhost:7070/rooms
```

## Notas Técnicas

1. **MessageService**: Armazena em memória (lista), reseta em cada reinicialização
2. **Serialização**: Gson para JSON, sem marshalling de BigDecimal
3. **Sem Spring**: Javalin puro com JDBC direto
4. **Sem ORM**: SQL direto, queries manuais
5. **Imutabilidade**: Modelos usam construtor privado + with methods

## Pronto para Produção?

✅ **Funcional e Testado**
- Todos os requisitos implementados
- Testes unitários passando
- JAR standalone gerado

⚠️ **Considerações de produção** (não solicitadas):
- MessageService deveria persistir em banco (tabela messages)
- Implementar rate limiting
- Adicionar validação de input mais robusta
- Logging mais detalhado (já tem SLF4J+Simple)
- Tratamento de transações para operações críticas
- HTTPS em produção
- Autenticação/autorização

## Conclusão

Sistema completo e pronto para uso. Todos os requisitos foram implementados conforme especificação. O código segue as práticas solicitadas no CLAUDE.md:
- Organização para variações futuras
- Evita repetição
- Simplicidade sobre complexidade prematura
