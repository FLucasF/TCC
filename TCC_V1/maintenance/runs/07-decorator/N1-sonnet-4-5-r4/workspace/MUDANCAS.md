# Mudanças no Sistema de E-mails

## Resumo

Implementadas três regras para o envio de e-mails da pousada, com uma arquitetura extensível para facilitar a adição de novas regras no futuro.

## Funcionalidades Implementadas

### 1. Validação de E-mail
- E-mails inválidos são bloqueados (não enviados)
- Aparecem na lista de mensagens com `status: BLOCKED`
- Validação: deve ter exatamente um @, algo antes dele, e um domínio com pelo menos um ponto
- Exemplos bloqueados: `ana@gmail`, `ana.gmail.com`, `@gmail.com`, `ana@@gmail.com`

### 2. Cópia para Arquivo (Modo Normal)
- Toda mensagem enviada gera uma cópia para `arquivo@pousadamarealta.com.br`
- Assunto da cópia: `[Cópia] ` + assunto original
- Mesmo texto da mensagem original
- Mensagens bloqueadas não geram cópia
- A cópia aparece na lista de mensagens logo após a original

### 3. Modo de Teste
- Ativado pela variável de ambiente `HOTEL_TEST_MODE=true`
- Redireciona mensagens para `testes@pousadamarealta.com.br`
- Assunto: `[TESTE] ` + assunto original
- Primeira linha do corpo: `Destinatário original: <endereço>`
- Não gera cópia para arquivo
- E-mails inválidos continuam bloqueados (mantém endereço original)

### 4. Campo Status na API
- Todas as mensagens no `GET /messages` incluem o campo `status`
- Valores possíveis: `SENT` ou `BLOCKED`

## Estrutura Técnica

A implementação usa um padrão de regras encadeadas que facilita adicionar novas validações no futuro:

- **EmailRule**: interface para regras de processamento
- **OutgoingMessage**: mensagem sendo processada (pode ser modificada)
- **RuleResult**: resultado do processamento (pode incluir mensagens adicionais)

Regras atuais:
1. `EmailValidationRule` - bloqueia e-mails inválidos
2. `TestModeRule` - redireciona para teste (só em modo de teste)
3. `ArchiveCopyRule` - gera cópia para arquivo (só em modo normal)

Para adicionar novas regras (horário, limite por dia, etc.), basta criar uma nova classe que implementa `EmailRule` e adicioná-la em `EmailSender.buildRules()`.

## Banco de Dados

Nova migração `V2__add_message_status.sql` adiciona a coluna `status` na tabela `messages`.

## Testes

35 testes passando, incluindo:
- Validação de e-mails inválidos
- Cópia para arquivo em modo normal
- Redirecionamento em modo de teste
- Verificação do campo status na API
- Todos os testes existentes ajustados para as novas regras
