# Desenho: Serviço de Cálculo de Resumo de Compra

## O que muda de caso para caso

1. **Modalidades de entrega**: cada uma calcula frete diferente, tem prazo próprio e pode ter restrições
   - Novas modalidades entram toda semana
   
2. **Cupons**: cada cupom tem regra de desconto e condições de aplicação diferentes
   - Novos cupons sempre sendo criados pelo marketing
   
3. **Formas de pagamento**: cada uma aplica ajuste diferente (desconto, tarifa ou juros)
   - Número de parcelas varia por forma
   
4. **Níveis do clube**: cada nível tem conjunto próprio de benefícios
   - Estudando criar mais níveis

5. **Regiões**: cada uma tem sua porcentagem de seguro

## O que é igual

- Ordem do cálculo: produtos → frete → cupom → seguro → total → ajuste pagamento
  - **Nota**: Frete calculado antes do cupom porque FRETEGRATIS precisa do valor do frete
- Arredondamento "meio para o par" em cada etapa
- Estrutura de entrada/saída JSON
- Lógica de validação na ordem especificada

## Estrutura escolhida

**Strategy** para cada dimensão de variação:

- `ModalidadeEntrega`: interface + implementação por modalidade
- `Cupom`: interface + implementação por cupom  
- `FormaPagamento`: interface + implementação por forma
- `NivelClube`: enum com comportamento
- `Regiao`: enum com porcentagem

**Registries** para escolher entre os casos:
- Mapas que associam código (String/Enum) → implementação
- Sem sequências de if/else

**Por quê:** 
- Novas modalidades/cupons/formas entram sem mexer em código existente
- Cada comportamento fica isolado em um único lugar
- Escolha entre casos via lookup direto, não condicional
