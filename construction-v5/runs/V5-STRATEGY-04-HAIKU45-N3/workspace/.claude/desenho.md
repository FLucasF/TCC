# Desenho: Serviço de Resumo de Checkout

## O que varia de caso para caso

1. **Modalidade de entrega**: fórmula de frete, prazo, limitações (ex: motoboy até 5kg)
2. **Cupom**: tipo de desconto aplicado
3. **Forma de pagamento**: ajuste final (desconto Pix, taxa boleto, juros cartão)
4. **Nível de clube**: vantagens (frete grátis, desconto, brinde)
5. **Região**: taxa de seguro

## O que é igual em todos os casos

- Cálculo de subtotal (soma de produtos)
- Ordem de cálculo: subtotal → desconto cupom → frete → seguro → ajuste pagamento
- Arredondamento (banker's rounding) em cada etapa
- Validações em ordem prescrita

## Estrutura escolhida

- **Enums encapsulam cada caso**: ModalidadeEntrega, Cupom, FormaPagamento, NivelClube, Regiao
  - Cada enum tem métodos que implementam sua lógica (ex: `ModalidadeEntrega.calcularFrete()`)
  - Evita sequências de condições e coloca comportamento de cada caso num lugar só

- **ResumoCheckoutService orquestra o cálculo** em etapas discretas, usando os enums

- **ValidadorRequisicao** executa validações em ordem, retorna primeiro erro

- **ArredondadorBanqueiro** centraliza arredondamento "meio para o par"

- **DTOs** para entrada e saída, seguindo nomes e formatos exatos do anexo técnico

## Por que essa estrutura

- Quando novos cupons, entregas ou pagamentos surgem, cada um vira um item no enum respectivo
- Lógica de cada caso fica isolada, fácil de manter
- Cálculo principal fica legível, sem muita condição
