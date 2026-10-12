# Desenho: serviço de resumo de checkout

## O que muda de caso para caso

- **Modalidade de entrega**: cálculo do frete (fórmula diferente), prazo e limitação de disponibilidade (ex: motoboy ≤ 5 kg).
- **Cupom**: cálculo do desconto (percentual, valor fixo, igual ao frete, por unidades grátis) e condição de aplicabilidade.
- **Nível do clube**: se tem frete grátis, taxa de crédito, condição de brinde.
- **Forma de pagamento**: cálculo do total final e da parcela, tarifa/desconto, validação de disponibilidade.
- **Seguro por região**: só a taxa percentual muda; a fórmula é a mesma em todas.

## O que é igual em todos os casos

- Pipeline de cálculo (subtotal → desconto cupom → frete → seguro → total → ajuste pagamento).
- Arredondamento HALF_EVEN a 2 casas em cada etapa.
- Ordem de validação de erros (1–10 conforme spec).
- Crédito do clube = taxa × subtotalProdutos; seguro = taxa × subtotalProdutos.

## Estrutura escolhida

- Interface + implementação para cada ponto de variação: `ModalidadeEntrega`, `Cupom`, `NivelClube`, `FormaPagamento`.
- Registry por código (Map<String, T>) em cada pacote: evita cadeias de if/else ao escolher entre casos.
- `SeguroRegiao` como enum com campo de taxa: só a taxa varia, sem necessidade de polimorfismo.
- `CheckoutService` orquestra o pipeline na ordem definida, delegando cada etapa à implementação correta.
- Exceções de negócio (`CheckoutException` com `ErroCodigo`) capturadas no controller para resposta padronizada.

## Por que essa estrutura

- Cada comportamento variável mora num lugar só (sua classe de implementação).
- Adicionar entrega nova = criar uma classe que implementa `ModalidadeEntrega` e registrá-la; zero mudança no pipeline.
- Adicionar nível novo = criar uma classe `NivelClube`; idem para cupom e forma de pagamento.
- O pipeline do serviço não toca em if/else para escolher a modalidade, o cupom, o nível ou o pagamento.
