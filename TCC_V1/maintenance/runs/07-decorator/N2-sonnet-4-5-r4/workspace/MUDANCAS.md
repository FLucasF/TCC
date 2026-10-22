# Mudanças no Sistema de Mensagens

## Resumo

Implementadas três novas regras de processamento de mensagens usando o padrão Chain of Responsibility, permitindo flexibilidade para diferentes instalações e modos de operação.

## Funcionalidades Implementadas

### 1. Validação de E-mail
- **Regra:** `EmailValidationRule`
- **Comportamento:** Bloqueia mensagens com endereços inválidos
- **Critérios de validação:**
  - Deve ter exatamente um `@`
  - Deve ter conteúdo antes do `@`
  - Deve ter um domínio com pelo menos um ponto após o `@` (ex: `gmail.com`)
- **Resultado:** Mensagens bloqueadas aparecem com `status: "BLOCKED"` na lista

### 2. Modo de Teste
- **Regra:** `TestModeRedirectRule`
- **Ativação:** Variável de ambiente `HOTEL_TEST_MODE=true`
- **Comportamento:**
  - Redireciona mensagens válidas para `testes@pousadamarealta.com.br`
  - Adiciona `[TESTE]` no início do assunto
  - Adiciona `"Destinatário original: <email>"` na primeira linha do corpo
  - Mensagens bloqueadas mantêm o endereço original
  - Não gera cópia para arquivo

### 3. Cópia para Arquivo
- **Regra:** `ArchiveCopyRule`
- **Ativação:** Apenas em modo produção (quando `HOTEL_TEST_MODE` não é `true`)
- **Comportamento:**
  - Cria cópia de toda mensagem enviada para `arquivo@pousadamarealta.com.br`
  - Adiciona `[Cópia]` no início do assunto
  - Mantém o mesmo corpo da mensagem original
  - Mensagens bloqueadas não geram cópia
  - A cópia aparece como mensagem separada na lista, logo após a original

## Mudanças na API

### GET /messages
Agora retorna campo adicional `status` para cada mensagem:
- `"SENT"`: Mensagem enviada com sucesso
- `"BLOCKED"`: Mensagem bloqueada (e-mail inválido)

Exemplo de resposta:
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "ana@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Ana!...",
    "status": "SENT"
  },
  {
    "id": 2,
    "channel": "EMAIL",
    "to": "arquivo@pousadamarealta.com.br",
    "subject": "[Cópia] Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Ana!...",
    "status": "SENT"
  }
]
```

## Estrutura Técnica

### Padrão de Design: Chain of Responsibility
A solução usa o padrão Chain of Responsibility para processar mensagens através de uma sequência de regras:

1. **`MessageRule`** (interface): Define o contrato para regras de processamento
2. **`MessageRuleChain`**: Orquestra a execução sequencial das regras
3. **Regras concretas:**
   - `EmailValidationRule`
   - `TestModeRedirectRule`
   - `ArchiveCopyRule`

### Vantagens da Arquitetura
- **Extensível:** Adicionar novas regras (horário, limite diário) significa criar uma nova classe sem modificar as existentes
- **Configurável:** Diferentes instalações podem ter combinações diferentes de regras
- **Testável:** Cada regra pode ser testada isoladamente
- **Ordem importa:** Validação → Modo Teste → Cópia para Arquivo

## Mudanças no Banco de Dados

### Nova Migração: V2__add_message_status.sql
```sql
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
```

## Arquivos Criados/Modificados

### Novos Arquivos
- `src/main/java/hotel/messages/OutgoingMessage.java`
- `src/main/java/hotel/messages/MessageRule.java`
- `src/main/java/hotel/messages/MessageRuleChain.java`
- `src/main/java/hotel/messages/EmailValidationRule.java`
- `src/main/java/hotel/messages/TestModeRedirectRule.java`
- `src/main/java/hotel/messages/ArchiveCopyRule.java`
- `src/main/resources/db/migration/V2__add_message_status.sql`
- `src/test/java/hotel/messages/MessageRulesTest.java`

### Arquivos Modificados
- `src/main/java/hotel/messages/Message.java` - Adicionado campo `status`
- `src/main/java/hotel/messages/EmailSender.java` - Integrado com `MessageRuleChain`
- `src/main/java/hotel/messages/SentMessages.java` - Suporte ao campo `status`
- `src/main/java/hotel/Hotel.java` - Montagem da cadeia de regras baseada no modo
- Arquivos de teste atualizados para nova assinatura

## Testes
- **Total de testes:** 34 (8 novos testes específicos para regras de mensagens)
- **Cobertura:** Validação, modo teste, modo produção, combinações, casos de borda
- **Resultado:** ✅ Todos os testes passando
