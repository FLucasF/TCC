---
tags: [tcc, experimento, avaliacao, gabarito]
confidencial: true
---

# Gabarito do avaliador: pontos de Strategy no prompt

> [!warning] Nunca entra no container
> Este arquivo não pode ficar em `experimento/harness/`, `experimento/prompt/` nem em nenhuma pasta copiada para as execuções. O `.dockerignore` é lista branca e já barra `avaliacao/` do contexto de build.

## Visão geral

| Ponto | Onde está no prompt | Dificuldade | O que varia | Comportamentos por variação |
|---|---|---|---|---|
| **P1: Entrega** | Seção "Entrega" | 🟢 Fácil | Opção de entrega | Custo, prazo, disponibilidade |
| **P2: Cupons** | Seção "Cupons" | 🟡 Média | Regra do cupom | Desconto e condição de aplicação |
| **P3: Pagamento** | Espalhado: FAQ + "Observações do financeiro" + tabela de erros | 🔴 Difícil | Forma de pagamento | Ajuste do valor, parcelamento permitido, cálculo de parcela, disponibilidade |

## P1: Entrega (fácil)

**Por que é fácil**
- Apresentada numa **tabela única** com as variações lado a lado.
- Frase explícita de mudança: *"Quase toda semana entra uma opção nova de entrega, cada uma com seu jeito de cobrar, seu prazo e suas limitações."*

**Solução esperada**
- Abstração de entrega com: cálculo do frete, prazo e verificação de disponibilidade.
- Uma implementação por opção (`ECONOMICA`, `EXPRESSA`, `RETIRADA_LOJA`, `MOTOBOY`).
- Seleção pelo código recebido, sem condicional espalhada.

**Sinais de solução sem Strategy**
- `switch`/`if` por modalidade no serviço de checkout.
- `enum` com `switch` interno em vários métodos (preço, prazo, disponibilidade).

> [!note] Enum com comportamento
> Um `enum` em que **cada constante sobrescreve métodos abstratos** é uma forma válida de Strategy. Decidir **antes** de avaliar se aceita (recomendado: aceitar e anotar em observações).

## P2: Cupons (média)

**Por que é média**
- As variações aparecem como **exemplos concretos de cupons**, não como "tipos de regra". O modelo precisa perceber que cada cupom é um **algoritmo diferente**: percentual, valor fixo com condição, frete grátis, leve-pague.
- A pista de mudança é fraca: *"O pessoal do marketing adora inventar promoção."*
- Os algoritmos têm entradas diferentes: `LEVE3PAGUE2` olha itens; `FRETEGRATIS` olha frete; `MENOS50` tem condição.

**Solução esperada**
- Abstração de cupom com: verificar se é aplicável e calcular o desconto (recebendo contexto suficiente: itens, subtotal, frete).
- Uma implementação por regra.
- Registro dos cupons disponíveis sem condicional por código.

**Solução intermediária aceitável (parcial)**
- Implementações por regra genérica (ex.: "percentual", "valor fixo") parametrizadas, com os cupons como dados. É até melhor para cupons novos com a mesma regra. Contar como **correto** se a seleção não usa condicional por código.

**Sinais de solução sem Strategy**
- `switch (cupom)` com a lógica de cada cupom dentro.

## P3: Pagamento (difícil)

**Por que é difícil**
- **Não existe tabela nem seção própria.** As regras estão **espalhadas** em três lugares:
  1. FAQ: Pix 5%, cartão até 3× sem juros e de 4× a 12× com juros, tarifa do boleto, Pix e boleto à vista.
  2. Observações do financeiro: fórmula Price, regra sem juros, arredondamento do Pix, limite do boleto.
  3. Tabela de erros: parcelamento e disponibilidade.
- Misturada com itens irrelevantes (troca, moeda).
- **Nenhuma frase** sobre novas formas de pagamento.
- Cada regra isolada parece "só um `if`".

**Por que Strategy é justificado mesmo sem pista de mudança**
- Cada forma de pagamento tem **vários comportamentos diferentes ao mesmo tempo**: ajuste do total, validação de parcelas, cálculo da parcela e disponibilidade. Com condicionais, a mesma decisão por forma de pagamento se repete em vários pontos.

**Solução esperada**
- Abstração de forma de pagamento com: validar parcelas, verificar disponibilidade, calcular total final e parcela.
- Implementações `PIX`, `CARTAO`, `BOLETO`.

**Sinais de solução sem Strategy**
- `if (formaPagamento == PIX)` repetido em validação, cálculo e disponibilidade.

> [!question] Decidir antes de avaliar
> Com só 3 formas de pagamento e nenhuma pista de mudança, uma solução com condicionais **concentrada em um único lugar** é defensável. Proposta: anotar `forma` como `switch`/`ifs`, e registrar em observações se a condicional está concentrada (1 lugar) ou espalhada (vários lugares). Isso permite análise qualitativa sem mudar a regra. O desfecho primário não depende disto: ele é a contagem de arquivos.

## O desfecho: aplicar o teste de extensão por ponto

> [!warning] Alterado em 22/09/2026, e isto muda o hash deste arquivo
> Esta seção se chamava "Rubrica: aplicar por ponto" e trazia a tabela de
> preenchimento C1–C6. A rubrica saiu do desenho em 21/09/2026 (plano.md §14.4)
> e o arquivo dela foi removido, então esta seção mandava executar um
> procedimento que não existe mais.
>
> O hash anterior era `06813eb530435427`. O novo está na §7 do pré-registro, com
> os dois lado a lado.

O desfecho primário é a **contagem do teste de extensão**, aplicada
separadamente para P1, P2 e P3. O procedimento está em
`avaliacao/testes-extensao/README.md` e no plano §14.5.

| Pacote | P1 arq. alterados | P2 arq. alterados | P3 arq. alterados |
|---|---|---|---|

Resultado esperado a observar:
- **Dificuldade:** estender P1 custa menos que P2, e P2 menos que P3, nas duas condições.
- **Efeito do harness:** maior em P2 e P3 do que em P1 (onde o modelo puro já tende a acertar).

## Testes de extensão (um por ponto)

| Ponto | Extensão | Regra |
|---|---|---|
| P1 | Nova entrega `DRONE` | R$ 30,00 fixo; prazo 0; só até 2 kg |
| P2 | Novo cupom `DEZOFF` | R$ 10,00 de desconto nos produtos, sem condição |
| P3 | Nova forma `CARTEIRA_DIGITAL` | 2% de desconto no total; só à vista; indisponível acima de R$ 500,00 |

Registrar para cada um: arquivos criados, arquivos existentes alterados e linhas alteradas em arquivos existentes.

## Casos reservados para a suíte escondida (não aparecem no prompt)

Valores conferidos com arredondamento "meio para o par".

**Caso E5**: Livro 59,90 × 1 (0,50 kg), `EXPRESSA`, cupom `FRETEGRATIS`, `PIX`
→ subtotal 59,90 · frete 27,25 · cupom 27,25 · total do pedido 59,90 · Pix 5% = 2,995 → **3,00** · ajuste −3,00 · total final **56,90** · 1× de 56,90
*(testa o arredondamento "meio para o par" e o FRETEGRATIS)*

**Caso E6**: Fone 199,90 × 2 (0,25 kg), `MOTOBOY`, cupom `MENOS50`, `CARTAO` em 12×
→ total do pedido 367,80 · parcela **34,76** · total final **417,12** · ajuste **49,32**

**Casos de erro sugeridos**
- `MOTOBOY` com 5,01 kg → `MODALIDADE_INDISPONIVEL`
- `MOTOBOY` com exatamente 5,00 kg → sucesso
- `MENOS50` com subtotal 299,99 → `CUPOM_NAO_APLICAVEL`; com 300,00 → sucesso
- `BOLETO` com total do pedido 1.000,00 → sucesso; 1.000,01 → `FORMA_PAGAMENTO_INDISPONIVEL`
- `PIX` com `parcelas: 2` → `PARCELAMENTO_INVALIDO`
- `CARTAO` com `parcelas: 13` e com `parcelas: 0` → `PARCELAMENTO_INVALIDO`
- Cupom `bemvindo10` (minúsculo) → `CUPOM_INVALIDO`
- Precedência: carrinho vazio **e** modalidade inválida → `PEDIDO_INVALIDO`
- `LEVE3PAGUE2` com 2 unidades → desconto 0,00; com 6 unidades → 2 grátis
