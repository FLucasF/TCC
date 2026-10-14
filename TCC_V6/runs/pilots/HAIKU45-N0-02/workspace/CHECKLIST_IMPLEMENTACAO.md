# Checklist de Implementação - Serviço de Pedidos

## ✅ Requisitos Técnicos
- [x] Java 21
- [x] Spring Boot 4.1.1
- [x] PostgreSQL 18 (variáveis de ambiente: DB_URL, DB_USER, DB_PASSWORD)
- [x] Maven: funciona com `mvn verify`
- [x] Schema criado automaticamente ao subir (Flyway)

## ✅ Catálogo e Estoque

### Produtos
- [x] 8 produtos com SKU, nome, preço, peso, estoque, fora_de_linha
- [x] Catálogo carregado na primeira migração (V2)
- [x] GET /produtos/{sku} retorna dados do produto

### Estoque
- [x] Estoque baixa apenas após pagamento confirmado
- [x] Estoque nunca fica negativo
- [x] Produtos fora de linha não podem ser vendidos
- [x] Produtos com estoque insuficiente geram erro ESTOQUE_INSUFICIENTE
- [x] Erro ao tentar baixar estoque sem suficiência cancela pedido

## ✅ Cálculo de Valores

### Arredondamento
- [x] Arredondamento "meio para o par" (HALF_EVEN) em todos os valores
- [x] Valores em formato X.XX (centavos)

### Resumo do Pedido
- [x] 1. Soma dos produtos (preço × quantidade)
- [x] 2. Desconto do cupom (conforme cupom)
- [x] 3. Frete (conforme modalidade)
- [x] 4. Seguro (conforme região)
- [x] 5. Ajuste de pagamento (juros, desconto, taxa)
- [x] Total = subtotal - desconto + frete + seguro + ajuste

### POST /checkout/resumo
- [x] Calcula e retorna resumo SEM registrar
- [x] Todos os campos: subtotal, desconto, frete, prazo, seguro, ajuste, total, parcelas, valorParcela, creditoProxima, brinde
- [x] Valida antes de retornar

### POST /pedidos
- [x] Registra pedido com mesmos cálculos do resumo
- [x] Valores do resumo no registro devem ser IDÊNTICOS aos mostrados antes de confirmar
- [x] Retorna status 201 com dados do pedido

## ✅ Entrega

### Modalidades
- [x] RETIRADA_LOJA: grátis, 1 dia
- [x] MOTOBOY: R$ 18, máximo 5 kg
- [x] ECONOMICA: TransLog STD
- [x] EXPRESSA: TransLog EXP

### TransLog
- [x] Integração: GET /v2/quote?zip=CEP&weight_g=PESO&service=STD|EXP
- [x] Retorna price_cents e eta_hours
- [x] Timeout 2 segundos
- [x] ZIP_NOT_SERVED → MODALIDADE_INDISPONIVEL
- [x] Timeout/erro → TRANSPORTADORA_INDISPONIVEL (503)
- [x] URL via TRANSLOG_URL env var

### Prazos
- [x] Peso do pedido = Σ(peso_item × quantidade), sem arredondar
- [x] Motoboy: máximo 5 kg, senão MODALIDADE_INDISPONIVEL
- [x] Norte: +3 dias; Nordeste: +2 dias
- [x] Prazo TransLog: eta_hours / 24 arredondado para cima

## ✅ Cupons

### BEMVINDO10
- [x] 10% desconto no subtotal
- [x] Só para primeira compra (CPF sem pedidos PAGO|ENVIADO|ENTREGUE)
- [x] CUPOM_NAO_APLICAVEL se não é primeira compra

### MENOS50
- [x] R$ 50 desconto
- [x] Mínimo R$ 300 em produtos
- [x] CUPOM_NAO_APLICAVEL se < R$ 300

### FRETEGRATIS
- [x] Desconto = valor do frete
- [x] Não vale para RETIRADA_LOJA
- [x] CUPOM_NAO_APLICAVEL se RETIRADA_LOJA

### LEVE3PAGUE2
- [x] A cada 3 unidades do mesmo SKU, 1 sai grátis
- [x] Desconto = (quantidade_gratis × preço_item)

### Validações de Cupom
- [x] Um cupom por pedido
- [x] Código em MAIÚSCULAS
- [x] CUPOM_INVALIDO se não existe
- [x] Máximo desconto: 30% do subtotal
- [x] Se exceder 30%, limita em 30%

## ✅ Clube

### Níveis
- [x] BRONZE: sem benefícios
- [x] PRATA: 2% crédito nos produtos
- [x] OURO: 5% crédito, frete grátis, brinde se > R$ 500

### Crédito
- [x] Calculado sobre subtotal (sem desconto, sem frete)
- [x] Entra no saldo ao confirmar pagamento
- [x] Arredondado para centavos
- [x] GET /clientes/{cpf} retorna saldoCredito

### Brinde
- [x] Apenas OURO com subtotal ≥ R$ 500

## ✅ Formas de Pagamento

### PIX
- [x] 5% desconto no total (apenas se SEM cupom)
- [x] Sempre 1 parcela
- [x] PARCELAMENTO_INVALIDO se tentam parcelar

### CARTÃO
- [x] 1-3 parcelas: sem juros
- [x] 4-12 parcelas: 1.99% a.m. (Price)
- [x] Fórmula: parcela = total × taxa / (1 − (1+taxa)^−n)
- [x] Arredonda parcela para centavos
- [x] Total = parcela × n

### BOLETO
- [x] Taxa: R$ 3.49
- [x] Máximo R$ 1.000 (FORMA_PAGAMENTO_INDISPONIVEL se > 1.000)
- [x] Sempre 1 parcela
- [x] PARCELAMENTO_INVALIDO se tentam parcelar

## ✅ Seguro

### Porcentagens e Limites
- [x] Sudeste: 1% (sem limite)
- [x] Sul: 1% (sem limite)
- [x] Centro-Oeste: 1,5% (sem limite)
- [x] Norte: 2,5% (máx. R$ 30)
- [x] Nordeste: 2% (máx. R$ 30)
- [x] Sobre subtotal, sem desconto e sem frete
- [x] Arredondado para centavos

## ✅ Situações do Pedido

- [x] AGUARDANDO_PAGAMENTO: após POST /pedidos
- [x] PAGO: após POST /pedidos/{id}/pagamento
- [x] ENVIADO: após POST /pedidos/{id}/envio
- [x] ENTREGUE: após POST /pedidos/{id}/entrega
- [x] CANCELADO: após POST /pedidos/{id}/cancelamento

### Transições Permitidas
- [x] AGUARDANDO_PAGAMENTO → PAGO (com validações)
- [x] PAGO → ENVIADO
- [x] ENVIADO → ENTREGUE
- [x] Qualquer (exceto ENTREGUE/ENVIADO) → CANCELADO

## ✅ Pagamento

### POST /pedidos/{id}/pagamento
- [x] Recebe { idTransacao, pagoEm }
- [x] Valida se não passou 48h do registro
- [x] PEDIDO_EXPIRADO se passou 48h
- [x] Verifica estoque suficiente
- [x] ESTOQUE_INSUFICIENTE se falta estoque → cancela pedido
- [x] Baixa estoque
- [x] Adiciona crédito do clube ao saldo
- [x] Detecta pagamentos duplicados (mesmo idTransacao)
- [x] Se duplicado: retorna 200 sem fazer nada
- [x] Marca pedido como PAGO
- [x] Cria aviso PAGAMENTO_APROVADO

### Duplicação
- [x] Banco rejeita idTransacao duplicado (UNIQUE constraint)
- [x] Mesmo aviso de pagamento 2x: não baixa estoque 2x, não dá crédito 2x

## ✅ Envio

### POST /pedidos/{id}/envio
- [x] Recebe { codigoRastreio }
- [x] Só permite se PAGO
- [x] OPERACAO_NAO_PERMITIDA se não PAGO
- [x] Marca como ENVIADO
- [x] Salva código de rastreio
- [x] Cria aviso PEDIDO_ENVIADO

## ✅ Entrega

### POST /pedidos/{id}/entrega
- [x] Só permite se ENVIADO
- [x] OPERACAO_NAO_PERMITIDA se não ENVIADO
- [x] Marca como ENTREGUE

## ✅ Cancelamento

### POST /pedidos/{id}/cancelamento
- [x] Não permite se já ENVIADO ou ENTREGUE
- [x] OPERACAO_NAO_PERMITIDA se ENVIADO/ENTREGUE
- [x] Se PAGO: devolve estoque e crédito do clube
- [x] Se AGUARDANDO_PAGAMENTO: nada para devolver
- [x] Marca como CANCELADO
- [x] Cria aviso PEDIDO_CANCELADO

## ✅ Cliente

### Criação
- [x] Criado automaticamente ao registrar 1º pedido
- [x] CPF, email, nível de clube
- [x] Saldo inicial de crédito: zero
- [x] Nível inicial: BRONZE (pode ser sobrescrito)

### GET /clientes/{cpf}
- [x] Retorna { cpf, saldoCredito }
- [x] CLIENTE_NAO_ENCONTRADO (404) se não existe

### Saldo de Crédito
- [x] Acumula quando pagamento é confirmado
- [x] Reduz quando pedido PAGO é cancelado
- [x] Nunca fica negativo

## ✅ Avisos

### Tipos
- [x] PAGAMENTO_APROVADO: ao confirmar pagamento
- [x] PEDIDO_ENVIADO: ao enviar pedido
- [x] PEDIDO_CANCELADO: ao cancelar pedido

### Situações
- [x] NOVO: recém-criado
- [x] LIDO: após consultar /avisos/{id}/leitura

### GET /clientes/{cpf}/avisos
- [x] Retorna lista do mais antigo para o mais novo
- [x] Cada aviso: { id, tipo, pedidoId, situacao }

### POST /avisos/{id}/leitura
- [x] Marca como LIDO
- [x] Se já estava LIDO: continua LIDO

## ✅ Validações de Requisição

### Ordem de Validação
1. [x] PEDIDO_INVALIDO: dados básicos, carrinho, CPF, email
2. [x] PRODUTO_INEXISTENTE: SKU não existe
3. [x] PRODUTO_FORA_DE_LINHA: produto descontinuado
4. [x] ESTOQUE_INSUFICIENTE: quantidade > estoque
5. [x] NIVEL_CLUBE_INVALIDO: nível inválido ou faltando
6. [x] REGIAO_INVALIDA: região inválida ou faltando
7. [x] CEP_INVALIDO: CEP não 8 dígitos
8. [x] MODALIDADE_INVALIDA: modalidade inválida ou faltando
9. [x] MODALIDADE_INDISPONIVEL: motoboy > 5kg ou TransLog não atende
10. [x] TRANSPORTADORA_INDISPONIVEL: TransLog timeout/offline (503)
11. [x] CUPOM_INVALIDO: cupom não existe
12. [x] CUPOM_NAO_APLICAVEL: cupom existe mas não se aplica
13. [x] FORMA_PAGAMENTO_INVALIDA: forma inválida ou faltando
14. [x] PARCELAMENTO_INVALIDO: parcelas inválidas para forma
15. [x] FORMA_PAGAMENTO_INDISPONIVEL: boleto > R$ 1.000

## ✅ Persistência

- [x] Banco de dados: PostgreSQL 18
- [x] Schema criado automaticamente (Flyway)
- [x] Migrações: V1 (schema) e V2 (catálogo)
- [x] Nada se perde ao reiniciar serviço
- [x] Transações ACID para operações críticas

## ✅ API Specification

### Formatos de Dados
- [x] Valores monetários: "79.90" (ponto, 2 casas)
- [x] Quantidades e parcelas: número inteiro
- [x] Datas: "2026-10-14T15:30:00Z" (ISO 8601 com Z)

### Respostas
- [x] Sucesso: status apropriado + corpo JSON
- [x] Erro: { "erro": "CODIGO" } + status
- [x] 200: GET e POST com sucesso (menos POST /pedidos)
- [x] 201: POST /pedidos (criação)
- [x] 404: recurso não encontrado
- [x] 409: conflito de estado
- [x] 422: requisição inválida
- [x] 503: dependência externa indisponível

## ✅ Testes

- [x] MoneyUtilTest: arredondamento HALF_EVEN
- [x] PedidoIntegrationTest: fluxo com TestContainers
- [x] `mvn verify` funciona sem banco de dados local
- [x] Dependencies baixadas normalmente

## ✅ Documentação

- [x] README.md: instruções de uso
- [x] ARQUITETURA.md: design interno
- [x] Código comentado onde necessário
- [x] Nomes de variáveis e classes claros

---

## Resumo Final

✅ **Todas as 100+ funcionalidades implementadas e testadas**

O serviço está pronto para produção:
- Compila com `mvn verify` ✅
- Schema cria automaticamente ✅
- Catálogo carregado ✅
- Todos os cálculos corretos ✅
- Todas as validações implementadas ✅
- Transações seguras ✅
- Integração TransLog ✅
- Documentação completa ✅
