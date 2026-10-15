# Desenho: Serviço de Resumo de Compra

## O que muda de caso para caso

1. **Cálculo do frete**: cada modalidade tem fórmula própria (fixo, por kg, grátis) e
   restrições (motoboy até 5kg)
2. **Desconto do cupom**: cada cupom tem regra própria (porcentagem, fixo, condicional,
   por quantidade) e pode ter requisitos mínimos
3. **Benefícios do clube**: cada nível tem vantagens diferentes (crédito, frete grátis,
   brinde)
4. **Ajuste de pagamento**: cada forma calcula diferente (desconto, tarifa, juros) e
   pode ter restrições (boleto até R$ 1000)
5. **Taxa de seguro**: só a porcentagem muda por região, o cálculo é o mesmo

## O que é igual em todos os casos

- Ordem de cálculo (produtos → cupom → frete → seguro → total → pagamento)
- Arredondamento (HALF_EVEN/"meio para o par" em cada etapa)
- Validações na ordem definida
- Estrutura de entrada/saída JSON

## Estrutura escolhida

Usar polimorfismo/estratégia para isolar o comportamento de cada caso em seu próprio lugar:

- **Modalidade de entrega**: interface `ModalidadeEntrega` com método `calcularFrete(pesoKg)`
  e `verificarDisponibilidade(pesoKg)`. Cada modalidade é uma classe.
  
- **Cupom**: interface `Cupom` com métodos `calcularDesconto(itens, frete)` e
  `verificarAplicavel(itens)`. Cada cupom é uma classe.
  
- **Nível clube**: enum `NivelClube` com métodos que retornam porcentagem de crédito,
  se zera frete e se ganha brinde.
  
- **Forma de pagamento**: interface `FormaPagamento` com `calcularAjuste(total, parcelas)`
  e `verificarDisponibilidade(total, parcelas)`. Cada forma é uma classe.
  
- **Região**: enum `Regiao` com a porcentagem de seguro.

Escolher entre os casos será via factory ou registry (map de código → implementação),
não sequência de if/switch. O serviço orquestra o fluxo fixo, delegando cálculos
variáveis para as estratégias.

## Por quê

- Novas transportadoras/cupons/níveis/formas: adiciona classe, não modifica if/switch
- Cada regra de negócio mora em um lugar só
- Fácil testar cada caso isoladamente
- A ordem de cálculo e validações fica clara no serviço principal
