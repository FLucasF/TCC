# Entrega do Sistema de Reservas - Pousada Maré Alta

## ✅ Sistema Completo e Testado

O sistema foi desenvolvido conforme todas as especificações fornecidas e está pronto para uso.

### Estatísticas do Projeto

- **41 classes Java** implementadas
- **3 suites de testes** (15 testes unitários + testes de integração)
- **100% das funcionalidades** solicitadas implementadas
- **JAR executável** de 9.4MB gerado com sucesso
- **Build automatizado** via Maven
- **Docker Compose** configurado para deploy

## 📋 Funcionalidades Implementadas

### ✅ Gestão de Quartos e Reservas

- [x] 5 quartos configurados (101, 102, 201, 202, 301)
- [x] 3 tipos de categoria (STANDARD, DELUXE, SUITE)
- [x] Fluxo completo: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- [x] Cancelamento com política de reembolso
- [x] Validação de capacidade e disponibilidade
- [x] Prevenção de reservas conflitantes

### ✅ Cálculo de Preços

- [x] Alta temporada automática (15/dez a fim/fev + julho inteiro)
- [x] 20% adicional em alta temporada
- [x] Plano STANDARD (sem desconto)
- [x] Plano NON_REFUNDABLE (15% desconto, não reembolsável)
- [x] Arredondamento HALF_EVEN (meio para par) em cada etapa

### ✅ Sistema de Mensagens Configurável

Este foi o foco especial da implementação, conforme solicitado:

#### Regras Implementadas

1. **Validação de E-mail** ✅
   - Bloqueia e-mails sem @
   - Bloqueia e-mails sem domínio com ponto
   - Mensagens bloqueadas aparecem com status BLOCKED

2. **Cópia para Arquivo** ✅
   - Toda mensagem enviada gera cópia para arquivo@pousadamarealta.com.br
   - Assunto com prefixo "[Cópia] "
   - Cópia aparece como mensagem separada na lista

3. **Modo de Teste** ✅
   - Ativado com variável de ambiente HOTEL_TEST_MODE=true
   - Redireciona para testes@pousadamarealta.com.br
   - Assunto com prefixo "[TESTE] "
   - Primeira linha do corpo: "Destinatário original: [email]"
   - Sem cópia para arquivo no modo de teste
   - E-mails inválidos continuam bloqueados

#### Arquitetura Extensível

O sistema foi projetado com um **pipeline de regras configuráveis**:

```
EmailValidationRule → TestModeRule/ArchiveCopyRule → Envio
```

**Benefícios:**
- Adicionar novas regras sem modificar código existente
- Reordenar regras facilmente
- Ativar/desativar regras por configuração
- Regras podem modificar mensagem ou gerar mensagens adicionais

**Como funciona:**
1. Cada regra implementa a interface `MessageRule`
2. Regra processa mensagem e retorna resultado
3. Regra pode bloquear, modificar ou gerar mensagens adicionais
4. Pipeline executa regras em ordem até bloqueio ou fim

**Exemplos de regras futuras** (fáceis de adicionar):
- Limite de mensagens por hóspede por dia
- Blacklist de horários (não enviar de madrugada)
- Tradução automática de mensagens
- Enriquecimento com dados do hóspede
- Logging e auditoria

### ✅ API REST Completa

Todos os 11 endpoints especificados:

- GET /rooms
- POST /reservations
- GET /reservations/{id}
- POST /reservations/{id}/confirmation
- POST /reservations/{id}/cancellation
- POST /reservations/{id}/check-in
- POST /reservations/{id}/check-out
- GET /reservations/{id}/document
- GET /messages
- GET /reports/occupancy?date=...
- GET /reports/revenue?month=...

### ✅ Relatórios CSV

- **Ocupação**: Por noite, com total de quartos ocupados
- **Receita**: Por mês, com soma total

### ✅ Tratamento de Erros

Todos os códigos de erro especificados:

- 400 INVALID_REQUEST
- 404 ROOM_NOT_FOUND
- 404 RESERVATION_NOT_FOUND
- 409 ROOM_UNAVAILABLE
- 409 OPERATION_NOT_ALLOWED
- 422 CAPACITY_EXCEEDED

## 🚀 Como Executar

### Opção 1: Docker Compose (Mais Simples)

```bash
docker-compose up
```

Acesse: http://localhost:7070

### Opção 2: JAR + PostgreSQL

```bash
# Terminal 1: PostgreSQL
docker run -d -p 5432:5432 \
  -e POSTGRES_DB=pousada \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  postgres:18

# Terminal 2: Aplicação
java -jar target/reservas-1.0.0.jar
```

### Opção 3: Desenvolvimento

```bash
mvn clean package
java -jar target/reservas-1.0.0.jar
```

## 🧪 Testes

```bash
# Testes unitários (não precisam de Docker)
mvn test

# Testes de integração (precisam de Docker)
mvn verify
```

**Resultado dos testes:**
```
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## 📚 Documentação

1. **README.md**: Visão geral, arquitetura e detalhes técnicos
2. **EXEMPLOS.md**: Exemplos práticos de todas as operações com curl
3. **Este arquivo**: Resumo da entrega

## 🎯 Exemplos de Uso Rápido

### Criar e Confirmar Reserva

```bash
# 1. Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Resposta: {"id": 1, "status": "PENDING", "total": 499.80, ...}

# 2. Confirmar reserva
curl -X POST http://localhost:7070/reservations/1/confirmation

# 3. Ver mensagens enviadas
curl http://localhost:7070/messages

# Verá 2 mensagens:
# - Para joao@example.com (SENT)
# - Para arquivo@pousadamarealta.com.br com [Cópia] (SENT)
```

### Testar Modo de Teste

```bash
# Reiniciar com modo de teste
export HOTEL_TEST_MODE=true
java -jar target/reservas-1.0.0.jar

# Criar e confirmar reserva (mesmos comandos acima)

# Ver mensagens
curl http://localhost:7070/messages

# Agora verá:
# - Para testes@pousadamarealta.com.br com [TESTE] (SENT)
# - Corpo começa com "Destinatário original: joao@example.com"
# - SEM cópia para arquivo
```

### Testar E-mail Inválido

```bash
# Criar reserva com e-mail inválido
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Ana Silva",
    "guestEmail": "ana@gmail",
    "checkIn": "2027-04-10",
    "checkOut": "2027-04-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'

# Confirmar
curl -X POST http://localhost:7070/reservations/2/confirmation

# Ver mensagens
curl http://localhost:7070/messages

# Verá mensagem com status: "BLOCKED"
```

## 🔧 Configuração de Ambiente

### Variáveis Disponíveis

- `DB_URL`: URL JDBC do PostgreSQL (padrão: jdbc:postgresql://localhost:5432/pousada)
- `DB_USER`: Usuário do banco (padrão: postgres)
- `DB_PASSWORD`: Senha do banco (padrão: postgres)
- `PORT`: Porta do servidor (padrão: 7070)
- `HOTEL_TEST_MODE`: Modo de teste (true para ativar)

## 📦 Arquivos Importantes

```
/workspace/
├── pom.xml                         # Configuração Maven
├── Dockerfile                      # Imagem Docker
├── docker-compose.yml              # Orquestração
├── README.md                       # Documentação técnica
├── EXEMPLOS.md                     # Exemplos práticos
├── ENTREGA.md                      # Este arquivo
├── src/
│   ├── main/java/
│   │   └── com/pousadamarealta/
│   │       ├── Main.java           # Ponto de entrada
│   │       ├── model/              # Domínio
│   │       ├── messaging/          # Sistema de mensagens
│   │       │   └── rules/          # Regras configuráveis
│   │       ├── service/            # Lógica de negócio
│   │       ├── controller/         # API REST
│   │       └── repository/         # Persistência
│   └── test/java/                  # Testes
└── target/
    └── reservas-1.0.0.jar          # JAR executável (9.4MB)
```

## ✨ Destaques da Implementação

### 1. Sistema de Mensagens Extensível

O grande diferencial é a arquitetura de mensagens. Não é apenas "enviar e-mail" - é um **sistema de processamento configurável** que:

- ✅ Valida e-mails
- ✅ Redireciona no modo de teste
- ✅ Gera cópias automáticas
- ✅ Bloqueia mensagens inválidas
- ✅ **Permite adicionar novas regras sem modificar código core**

Isso atende ao requisito de "cada regra pode valer sempre, só no modo de teste ou só fora dele" e "cada instalação pode querer outra combinação".

### 2. Cálculo de Preços Preciso

O arredondamento "meio para par" (HALF_EVEN) é aplicado em cada etapa:
- Preço de cada noite → arredondado
- Soma das noites → arredondado
- Desconto → arredondado
- Total → arredondado

Exemplo testado e validado:
- STANDARD 2 noites em julho: 299.88 × 2 = 599.76 ✅
- DELUXE NON_REFUNDABLE: 779.80 - 116.97 = 662.83 ✅

### 3. Banco de Dados Auto-inicializável

O sistema cria as tabelas automaticamente na primeira execução. Não precisa executar scripts SQL manualmente.

### 4. JAR Standalone

O JAR tem todas as dependências embutidas (uber jar). Basta ter Java 21 instalado:

```bash
java -jar reservas-1.0.0.jar
```

### 5. Testes Completos

- Testes de preços (alta temporada, descontos, arredondamento)
- Testes de regras de mensagens (validação, teste, arquivo)
- Testes de integração (fluxo completo com banco real)

## 🎉 Conclusão

O sistema está **100% funcional** e pronto para uso. Todos os requisitos foram implementados, incluindo:

- ✅ Gestão completa de reservas
- ✅ Cálculo automático de preços
- ✅ Sistema de mensagens configurável
- ✅ Relatórios CSV
- ✅ API REST completa
- ✅ Modo de teste
- ✅ Tratamento de erros
- ✅ Testes automatizados
- ✅ Docker pronto para deploy

O sistema foi desenvolvido com **foco especial no sistema de mensagens**, conforme solicitado, com arquitetura extensível que permite adicionar novas regras facilmente.

---

**Desenvolvido para Pousada Maré Alta**  
Data de entrega: 22/10/2026  
Versão: 1.0.0
