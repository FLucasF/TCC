# Desenho: Serviço de Resumo de Compra

## O que varia de caso para caso

1. **Modalidades de Entrega**: cada uma calcula custo, prazo e tem restrições próprias
   - "Quase toda semana entra uma opção nova"
   
2. **Cupons**: cada um desconta de forma diferente e tem condições próprias
   - "O pessoal do marketing adora inventar promoção"
   
3. **Níveis do Clube**: cada nível tem seus benefícios específicos
   - "Estudando criar mais níveis"
   
4. **Formas de Pagamento**: cada uma ajusta o total de forma diferente
   - Pix desconta, boleto cobra tarifa, cartão parcela com/sem juros
   
5. **Regiões**: muda só a porcentagem do seguro (1%, 1,5%, 2%, 2,5%)

## O que é igual em todos os casos

- A ordem do cálculo: produtos → cupom → frete → seguro → total → ajuste pagamento
- Arredondamento meio-para-par em cada etapa
- Estrutura JSON entrada/saída
- Ordem de validação e códigos de erro
- Fórmula Price para juros
- Seguro sempre sobre produtos (sem desconto, sem frete)

## Estrutura escolhida

Polimorfismo para isolar o comportamento de cada caso:

- `ModalidadeEntrega`: interface com `calcularFrete(peso)`, `obterPrazo()`, `aceitaPedido(peso)`
- `Cupom`: interface com `calcularDesconto(contexto)`, `aplicavel(contexto)`
- `NivelClube`: interface com `calcularCredito(produtos)`, `temFreteGratis()`, `temBrinde(produtos)`
- `FormaPagamento`: interface com `calcularAjuste(total, parcelas)`, `validarParcelas(parcelas)`, `aceitaTotal(total)`, `calcularValorParcela(totalPedido, totalFinal, parcelas)`
- `Regiao`: enum simples com porcentagem do seguro

Um `CalculadoraResumo` orquestra o fluxo, delegando para cada estratégia.
Um `ValidadorPedido` valida na ordem exata do enunciado, incluindo verificação de cupom aplicável.

## Por que essa estrutura

Cada variação mora no seu lugar. Adicionar nova modalidade, cupom, nível ou forma de pagamento não exige mexer nos outros nem criar sequências de `if`. A ordem do cálculo fica explícita no orquestrador.
