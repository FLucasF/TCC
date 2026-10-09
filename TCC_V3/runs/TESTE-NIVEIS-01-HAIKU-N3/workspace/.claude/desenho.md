# Desenho: Serviço de Resumo de Compra

## O que muda de caso para caso

### 1. Cálculo de Frete (por modalidade)
- `ECONOMICA`: R$ 12,00 + R$ 2,00/kg
- `EXPRESSA`: R$ 25,00 + R$ 4,50/kg
- `RETIRADA_LOJA`: Grátis (R$ 0,00)
- `MOTOBOY`: R$ 18,00 fixo (só até 5 kg)

### 2. Cálculo de Cupom (por tipo)
- `BEMVINDO10`: 10% do subtotal
- `MENOS50`: R$ 50,00 (mínimo R$ 300,00 de subtotal)
- `FRETEGRATIS`: Valor igual ao frete
- `LEVE3PAGUE2`: A cada 3 unidades de um item, 1 sai grátis

### 3. Ajuste de Pagamento (por forma)
- `PIX`: -5% do total do pedido
- `CARTAO`: 0% até 3x (sem juros), 1,99% a.m. de 4 a 12x (tabela Price)
- `BOLETO`: +R$ 3,49 taxa

### 4. Vantagens de Clube (por nível)
- `BRONZE`: Nenhuma
- `PRATA`: 2% de crédito sobre subtotal
- `OURO`: 5% de crédito sobre subtotal + frete grátis + brinde se subtotal > R$ 500,00

### 5. Seguro (por região)
- Percentual sobre subtotal: `SUDESTE` 1%, `SUL` 1%, `CENTRO_OESTE` 1,5%, `NORTE` 2,5%, `NORDESTE` 2%

## O que é igual em todos os casos

- Ordem de cálculo: sempre produto → cupom → frete → seguro → pagamento
- Arredondamento: sempre "meio para o par" (banker's rounding) em cada etapa
- Validações: sempre na mesma ordem (definida nos codes de erro)
- Estrutura de resposta: sempre os mesmos campos

## Estrutura escolhida

**Padrão Strategy** para cada aspecto variável:
- `FreteCalculador`: interface com implementações para cada modalidade
- `CupomCalculador`: interface com implementações para cada cupom
- `PagamentoCalculador`: interface com implementações para cada forma
- `ClubeCalculador`: interface com implementações para cada nível
- `SeguroCalculador`: calcula por região

**Orquestrador central** (`ResumoCheckoutService`):
- Valida entrada
- Delegua cálculos aos calculadores específicos
- Aplica arredondamentos
- Monta resposta

## Por que

A primeira razão é evitar grandes cadeias de `if-else` ou `switch-case` espalhadas. Cada regra de negócio fica isolada em um lugar, facilitando manutenção futura quando novas modalidades, cupons, formas de pagamento ou níveis de clube forem criados.

A segunda é atender o caso mais exigente na assinatura: `ResumoCheckoutService` precisa aceitar qualquer combinação de entrada sem precisar saber os detalhes de como cada parte é calculada.
