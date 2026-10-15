# Desenho: resumo da compra

## O que muda de caso para caso
- Modalidade de entrega: fórmula de frete, prazo, limite de peso (motoboy até 5 kg).
- Cupom: desconto e condição para valer (MENOS50 exige subtotal >= 300).
- Nível do clube: percentual de crédito, frete (OURO zera), brinde (OURO com subtotal > 500).
- Forma de pagamento: ajuste (Pix -5%, boleto +3,49, cartão com/sem juros), parcelas permitidas, limite de valor (boleto até 1000).
- Região: só o percentual do seguro. A conta é a mesma, então é dado, não comportamento.

## O que é igual em todos os casos
- Validação dos itens e subtotal (soma de preço × quantidade, arredondado por linha).
- Peso total (sem arredondar).
- Seguro = percentual da região × subtotal, arredondado.
- Total do pedido = subtotal − desconto + frete + seguro.
- Arredondamento HALF_EVEN para centavos em cada etapa.
- Ordem das checagens de erro e formato da resposta/erro.

## Estrutura
- Cada caso que varia é um enum (`Entrega`, `Cupom`, `NivelClube`, `FormaPagamento`) com seu comportamento na própria constante. A escolha é uma busca por código, sem cadeia de ifs.
- `Regiao` é um enum só com o percentual.
- `ResumoCompraService` tem o fluxo fixo: valida na ordem da tabela de erros, calcula na ordem da spec e monta a resposta.
- Assinaturas pensando no caso mais exigente:
  - `Cupom.desconto(Pedido, frete)`: FRETEGRATIS precisa do frete; LEVE3PAGUE2 precisa dos itens.
  - `FormaPagamento.calcular(total, parcelas)` e `disponivel(total)`.
  - Campos de entrada do request são `String`, para que o serviço devolva o código de erro certo (um enum da Jackson lançaria erro genérico).
- Frete é calculado antes do desconto, porque FRETEGRATIS usa o frete. O frete não depende do cupom.
- `ResumoException(codigo)` é tratada no `TratadorErros` e devolve HTTP 400 com `{ "erro": CODIGO }`.

## Decisões em aberto (assumidas)
- Status HTTP de erro: 400 (a spec não definiu).
- LEVE3PAGUE2 não tem condição na spec: é sempre aplicável (desconto 0 se não houver 3 unidades).
- "Total do pedido" para o limite do boleto é o total da etapa 5 (antes do ajuste).
- Cupom vazio ou só espaços é tratado como ausente (o site pode mandar campo vazio).
- Frete é calculado antes do desconto (ver Estrutura); o resultado é o mesmo da ordem da spec.
