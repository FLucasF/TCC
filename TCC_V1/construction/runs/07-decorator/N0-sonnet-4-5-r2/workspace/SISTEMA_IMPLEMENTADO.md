# Sistema de Reservas - Pousada Maré Alta

## ✅ Implementação Completa

### 1. Modelo de Domínio

#### Quartos (Fixos)
- **101, 102**: STANDARD - 2 pessoas - R$ 249,90/noite
- **201, 202**: DELUXE - 3 pessoas - R$ 389,90/noite
- **301**: SUITE - 4 pessoas - R$ 579,90/noite

#### Alta Temporada (+20%)
- 15 de dezembro a 28 de fevereiro
- Mês de julho inteiro

#### Planos de Tarifa
- **STANDARD**: preço normal
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso

#### Estados da Reserva
```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
       ↘ CANCELLED
```

### 2. Funcionalidades Implementadas

#### API REST Completa
✅ `GET /rooms` - Listar quartos  
✅ `POST /reservations` - Criar reserva  
✅ `GET /reservations/{id}` - Consultar reserva  
✅ `POST /reservations/{id}/confirmation` - Confirmar (paga + envia e-mail)  
✅ `POST /reservations/{id}/cancellation` - Cancelar (com cálculo de reembolso)  
✅ `POST /reservations/{id}/check-in` - Check-in  
✅ `POST /reservations/{id}/check-out` - Check-out (retorna comprovante)  
✅ `GET /reservations/{id}/document` - Segunda via do comprovante  
✅ `GET /messages` - Listar mensagens enviadas  
✅ `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação (CSV)  
✅ `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro (CSV)

#### Cálculo de Preços
✅ Arredondamento "meio para o par" (HALF_EVEN) em cada etapa  
✅ Cálculo correto de noites  
✅ Preço por noite com multiplicador de alta temporada  
✅ Soma das noites arredondada  
✅ Desconto calculado sobre subtotal e arredondado  
✅ Total = subtotal - desconto (arredondado)

#### Validações
✅ Quarto não pode ter 2 reservas na mesma noite (exceto canceladas)  
✅ Número de hóspedes entre 1 e capacidade do quarto  
✅ Check-out deve ser após check-in  
✅ Operações só permitidas em estados apropriados  
✅ Comprovante só para reservas pagas

#### Erros HTTP Apropriados
✅ `400 INVALID_REQUEST` - Campos faltando, datas inválidas, formato incorreto  
✅ `404 ROOM_NOT_FOUND` - Quarto não existe  
✅ `404 RESERVATION_NOT_FOUND` - Reserva não existe  
✅ `409 ROOM_UNAVAILABLE` - Quarto já reservado nas datas  
✅ `409 OPERATION_NOT_ALLOWED` - Estado não permite a operação  
✅ `422 CAPACITY_EXCEEDED` - Mais hóspedes que a capacidade

### 3. Sistema de Mensagens (⭐ Parte Crítica)

#### Arquitetura Extensível
✅ Chain of Responsibility Pattern  
✅ Pipeline de processadores configurável  
✅ Contexto compartilhado entre regras  
✅ Suporte a mensagens adicionais (cópias)  
✅ Controle de fluxo (continuar/bloquear)

#### Regras Implementadas

**1. EmailValidationRule**
✅ Valida formato: `algo@dominio.com`  
✅ Requer pelo menos um ponto no domínio  
✅ Bloqueia mensagens inválidas (status BLOCKED)  
✅ Mensagens bloqueadas aparecem na lista para ação da recepção

**2. TestModeRule** (via `HOTEL_TEST_MODE=true`)
✅ Redireciona para `testes@pousadamarealta.com.br`  
✅ Adiciona prefixo `[TESTE]` no assunto  
✅ Adiciona linha `Destinatário original: ...` no corpo  
✅ Endereços inválidos continuam bloqueados

**3. ArchiveCopyRule**
✅ Cria cópia para `arquivo@pousadamarealta.com.br`  
✅ Adiciona prefixo `[Cópia]` no assunto  
✅ Mantém mesmo corpo da mensagem original  
✅ Não gera cópia em modo de teste  
✅ Não gera cópia de mensagens bloqueadas  
✅ Cópia aparece na lista logo após a mensagem original

#### Ordem de Execução
1️⃣ EmailValidationRule (bloqueia inválidos primeiro)  
2️⃣ TestModeRule (modifica antes de copiar)  
3️⃣ ArchiveCopyRule (copia versão já modificada)

#### Extensibilidade Demonstrada
📝 Documentação completa em `ARCHITECTURE.md`  
📝 Exemplos de novas regras (horário, limite por dia)  
📝 Instruções para adicionar regras  
📝 Padrão para configuração por instalação

### 4. Comprovante

✅ Formato exato conforme especificação:
```
Comprovante de reserva nº 7
Hóspede: Carla Dias (CPF 111.222.333-44)
Quarto: 201 (DELUXE)
Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
Diárias: R$ 779,80
Desconto: R$ 116,97
Total: R$ 662,83
```

✅ CPF formatado (111.222.333-44)  
✅ Datas formatadas (dd/MM/yyyy)  
✅ Valores em Real (R$ 1.234,56)  
✅ Singular/plural ("1 diária" / "2 diárias")  
✅ Linha de desconto só aparece quando há desconto

### 5. E-mail de Confirmação

✅ Assunto: "Reserva confirmada - Pousada Maré Alta"  
✅ Formato exato conforme especificação  
✅ Inclui comprovante no corpo  
✅ Informações de entrada (14h) e saída (12h)  
✅ Saudação personalizada com nome do hóspede

### 6. Relatórios CSV

#### Ocupação
✅ Cabeçalho: `room,type,reservation,check_in,check_out`  
✅ Apenas reservas pagas (CONFIRMED, CHECKED_IN, CHECKED_OUT)  
✅ Quem entra na noite conta, quem sai não conta  
✅ Ordenado por número do quarto  
✅ Linha total: `total,X/5`  
✅ Datas no formato ISO (2027-07-10)

#### Financeiro
✅ Cabeçalho: `reservation,room,check_in,check_out,status,amount`  
✅ Apenas reservas que começam no mês  
✅ Apenas reservas que deixaram dinheiro (amount_paid > 0 OR refund > 0)  
✅ Amount = amountPaid - refund (reserva cancelada não reembolsável conta)  
✅ Ordenado por data de entrada, depois por ID  
✅ Linha total: `total,VALOR`  
✅ Valores como 599.76 (sem formatação de moeda)

### 7. Persistência

✅ PostgreSQL 18  
✅ Criação automática de tabelas  
✅ Connection pool (HikariCP)  
✅ Transações apropriadas  
✅ Variáveis de ambiente para configuração  
✅ Dados persistem entre reinicializações

### 8. Tecnologias

✅ Java 21  
✅ Javalin 6.7.0 (sem Spring conforme solicitado)  
✅ Maven  
✅ PostgreSQL 18  
✅ Jackson para JSON  
✅ HikariCP para connection pooling  
✅ SLF4J para logging

### 9. Build e Deploy

✅ `mvn package` gera JAR executável  
✅ JAR com todas dependências (shaded)  
✅ Executa com `java -jar target/reservas-1.0.0.jar`  
✅ Porta configurável via variável `PORT` (default 7070)  
✅ Todas configurações via variáveis de ambiente

### 10. Testes

✅ Testes de integração completos  
✅ Cobertura de casos de sucesso  
✅ Cobertura de casos de erro  
✅ Testes de regras de e-mail  
✅ Testes de modo normal e modo de teste  
✅ Testes de relatórios  
✅ `mvn verify` executa tudo

### 11. Documentação

✅ `README.md` - Visão geral e setup  
✅ `ARCHITECTURE.md` - Design do sistema de mensagens  
✅ `EXAMPLES.md` - Exemplos práticos de uso  
✅ `.env.example` - Configuração de exemplo  
✅ Código bem organizado e comentado onde necessário

## 🎯 Destaques da Implementação

### 1. Sistema de Mensagens Extensível
A arquitetura permite adicionar regras futuras (ex: "não enviar de madrugada", "limite de mensagens por dia") sem modificar código existente. Cada instalação pode ter sua própria combinação de regras.

### 2. Cálculo de Preços Preciso
Arredondamento correto em cada etapa garante que os valores batem exatamente com o esperado, usando HALF_EVEN (banker's rounding).

### 3. Separação de Responsabilidades
- **Domain**: Modelos e interfaces
- **Infrastructure**: Implementações de repositórios
- **Service**: Lógica de negócio
- **Controller**: API REST
- **Messaging**: Sistema de regras extensível

### 4. Pronto para Produção
- Validações robustas
- Tratamento de erros apropriado
- Mensagens de erro claras
- Logs de sistema
- Configuração via ambiente
- Testes automatizados

## 🚀 Como Executar

### Requisitos
- Java 21
- PostgreSQL 18
- Maven 3.6+

### Setup Rápido

```bash
# 1. Iniciar PostgreSQL (Docker)
docker run --name pousada-db \
  -e POSTGRES_DB=pousada \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgres:18

# 2. Configurar variáveis
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=postgres
export PORT=7070
export HOTEL_TEST_MODE=false

# 3. Build
mvn clean package

# 4. Executar
java -jar target/reservas-1.0.0.jar
```

### Testar
```bash
# Ver quartos
curl http://localhost:7070/rooms | jq

# Criar reserva
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
  }' | jq

# Ver exemplos completos em EXAMPLES.md
```

## 📋 Checklist de Requisitos

### Funcionalidades Básicas
- [x] 5 quartos (101, 102 STANDARD / 201, 202 DELUXE / 301 SUITE)
- [x] Preços corretos (249.90 / 389.90 / 579.90)
- [x] Alta temporada (+20%)
- [x] Planos de tarifa (STANDARD / NON_REFUNDABLE -15%)
- [x] Arredondamento "meio para o par"
- [x] Estados da reserva (PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT / CANCELLED)
- [x] Regras de cancelamento e reembolso

### API REST
- [x] GET /rooms
- [x] POST /reservations
- [x] GET /reservations/{id}
- [x] POST /reservations/{id}/confirmation
- [x] POST /reservations/{id}/cancellation
- [x] POST /reservations/{id}/check-in
- [x] POST /reservations/{id}/check-out
- [x] GET /reservations/{id}/document
- [x] GET /messages
- [x] GET /reports/occupancy
- [x] GET /reports/revenue

### Sistema de Mensagens (⭐ Parte Crítica)
- [x] Validação de e-mail (algo@dominio.com)
- [x] Mensagens inválidas bloqueadas (status BLOCKED)
- [x] Cópia para arquivo@pousadamarealta.com.br
- [x] Prefixo [Cópia] no assunto da cópia
- [x] Modo de teste (HOTEL_TEST_MODE=true)
- [x] Redirecionamento para testes@pousadamarealta.com.br
- [x] Prefixo [TESTE] no assunto
- [x] Linha "Destinatário original:" no corpo
- [x] Sem cópia em modo de teste
- [x] Arquitetura extensível para novas regras

### Outros
- [x] Comprovante formatado corretamente
- [x] E-mail de confirmação
- [x] Relatórios CSV corretos
- [x] PostgreSQL 18
- [x] Java 21
- [x] Javalin 6.7.0 (sem Spring)
- [x] Maven
- [x] JAR executável
- [x] Testes de integração
- [x] Documentação completa

## ✨ Conclusão

O sistema está **100% funcional** e **pronto para uso**. Todas as funcionalidades solicitadas foram implementadas, com ênfase especial no sistema extensível de regras de mensagens, que permite fácil adição de novas regras conforme a pousada crescer.

O código está bem estruturado, testado e documentado, seguindo boas práticas de desenvolvimento e preparado para evoluir conforme novas necessidades surgirem.
