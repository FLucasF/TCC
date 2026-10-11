# Desenho: Serviço de Resumo de Compra

## O que muda de caso para caso

1. **Cupons**: cada cupom calcula desconto de forma diferente
   - BEMVINDO10: 10% sobre produtos
   - MENOS50: R$ 50 fixo (condição: produtos ≥ R$ 300)
   - FRETEGRATIS: desconto = frete (será zero se clube já der frete grátis)
   - LEVE3PAGUE2: a cada 3 itens iguais, 1 grátis

2. **Modalidades de entrega**: custo, prazo e restrições diferentes
   - ECONOMICA: R$ 12 + R$ 2/kg, 7 dias
   - EXPRESSA: R$ 25 + R$ 4,50/kg, 2 dias
   - RETIRADA_LOJA: grátis, 1 dia
   - MOTOBOY: R$ 18, 0 dias (máx 5 kg)

3. **Níveis do clube**: benefícios diferentes
   - BRONZE: nada
   - PRATA: 2% crédito
   - OURO: 5% crédito + frete grátis + brinde se produtos > R$ 500

4. **Regiões**: porcentagem de seguro varia
   - SUDESTE/SUL: 1%
   - CENTRO_OESTE: 1,5%
   - NORTE: 2,5%
   - NORDESTE: 2%

5. **Formas de pagamento**: ajuste diferente
   - PIX: 5% desconto
   - BOLETO: +R$ 3,49 (não aceita > R$ 1.000)
   - CARTAO: até 3x sem juros, 4-12x com juros 1,99% a.m.

## O que é igual

- Ordem do cálculo: produtos → frete → cupom → seguro → total → ajuste pagamento
  (frete antes do cupom porque FRETEGRATIS precisa do frete já calculado)
- Arredondamento "meio para o par" em cada etapa
- Validações na ordem especificada
- Estrutura JSON de entrada/saída

## Estrutura escolhida

**Strategy pattern** para cada dimensão de variação:
- Interface `Cupom` com método `calcularDesconto(contexto)`
- Interface `ModalidadeEntrega` com métodos `calcularFrete(pesoKg)`, `obterPrazo()`, `estaDisponivel(pesoKg)`
- Interface `NivelClube` com métodos para crédito, frete grátis, brinde
- Enum `Regiao` com porcentagem de seguro
- Interface `FormaPagamento` com métodos para calcular ajuste e validar disponibilidade

**Por quê:**
- Cada caso fica isolado em uma classe
- Adicionar novos cupons/modalidades/níveis não mexe nos existentes
- Não há cascata de ifs para escolher o comportamento
- Registro/factory mapeia código → estratégia

**Orquestração:**
- `ResumoService` executa o cálculo na ordem correta
- Validações antes de calcular (ordem do erro importa)
- Arredondamento usando `RoundingMode.HALF_EVEN`
