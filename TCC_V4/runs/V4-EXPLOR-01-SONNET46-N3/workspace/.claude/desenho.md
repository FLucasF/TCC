# Desenho — Serviço de Checkout

## O que muda de caso para caso

- **Modalidade de entrega**: fórmula de frete, prazo, restrições (ex.: motoboy ≤ 5 kg)
- **Cupom**: lógica de desconto, condição de aplicabilidade
- **Nível do clube**: taxa de crédito, isenção de frete, brinde
- **Forma de pagamento**: fórmula de ajuste, restrições (boleto ≤ 1000, parcelas)
- **Seguro**: apenas a taxa muda por região — mesma fórmula

## O que é igual em todos os casos

- Pipeline de cálculo (ordem fixa)
- Arredondamento banker's rounding (HALF_EVEN) a cada etapa
- Sequência de validação de erros (10 passos em ordem fixa)
- Estrutura da resposta JSON

## Estrutura escolhida

Cada dimensão variável → interface com implementações concretas + registro (Map<String,Impl>).
O serviço central resolve o caso pelo registro, sem if/else de seleção.

- `ModalidadeEntrega`: interface → Economica, Expressa, RetiradaLoja, Motoboy
- `Cupom`: interface → CupomBemVindo10, CupomMenos50, CupomFreteGratis, CupomLeve3Pague2
- `NivelClube`: interface → NivelBronze, NivelPrata, NivelOuro
- `FormaPagamento`: interface → PagamentoPix, PagamentoCartao, PagamentoBoleto
- `Seguro`: classe estática com mapa região→taxa (só a taxa varia)

Registros são Maps populados staticamente, método `buscar(codigo)` lança a exceção correta.

## Por quê

Toda nova modalidade, cupom ou nível é uma nova classe + uma linha no mapa: zero mudança
no serviço central, zero novo if/else. Atende a variabilidade descrita sem criar estrutura
para variação imaginada (ex.: não há abstração genérica de "regra de frete por volume").
