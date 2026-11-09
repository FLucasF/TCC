# Síntese das três revisões da v71

Os relatórios completos estão nesta pasta: `deep-review.md`, `paper-review-system.md` e `research-superpowers.md`. Cada um
seguiu o método de um plugin, leu só o artigo e não viu os outros nem a rodada anterior. A coluna "nos dados" foi conferida
depois, no repositório.

| método | v53 | v71 |
|---|---|---|
| Deep Review | revisão maior, 3 itens críticos | aceitar com revisões maiores, nenhum crítico, "aprovável como TCC após os ajustes" |
| paper-review-system | *borderline*, 3/5 (ponderada 3,3) | *weak accept*, 3,5/5 |
| research-superpowers | revisão maior | revisão maior; nenhum item exige novas execuções |

A comparação é aproximada, porque cada rodada foi feita por um agente novo. Os três são IA da mesma empresa dos modelos
estudados, e não substituem um avaliador humano.

## A. Problemas confirmados, do mais grave ao menos grave

| # | Problema | Revisores | O que os dados mostram | Correção proposta |
|---|---|---|---|---|
| 1 | O ganho do Decorator sem a *skill* é todo de "alternativa equivalente", classe que a calibração não testou | os 3 | Construção: N0 4 e N1 10 alternativas, nenhum acerto; N2 5 e 5. Manutenção: N0 3 e N1 5, nenhum acerto; N2 2 e 6. | Dizer isso na P4 e nas Limitações; definir "alternativa equivalente" no método. |
| 2 | "Ponto mais difícil" não bate com a Tabela 1 | os 3 | No N0, pagamento tem 55%, cupom 65% e clube 67%. O clube é o mais complexo por afetar três regras, não o de menor acerto. A inconsistência ficou visível com as linhas acrescentadas na v57. | Trocar por "o ponto de maior complexidade" no Resumo, na 4.1 e na Conclusão. |
| 3 | "Na mesma direção nos dois estudos" contradiz a P1 | os 3 | No Strategy, o único *pattern* comum, a construção vai de 9 para 7 no N1. | Restringir a frase, citando a exceção. |
| 4 | "Não se observou aumento" contra 30, 30 e 28 | os 3 | A frase é da v58 e está errada: há duas execuções a mais com exagero no N2. | Dizer que o exagero ficou raro (2 em 90, ambos no N2). |
| 5 | A P2 diz que a *skill* não altera o exagero, e a P4, que ela não foi aplicada | Deep Review, research-superpowers | As 110 execuções não a abriram. | Dizer que N2 contra N1 não testa a *skill*. |
| 6 | O Resumo omite perdas | os 3 | Haiku 4.5 piora nos 5 pares; o comportamento na manutenção cai de 61 para 54; a construção com a *skill* vai de 4 para 9 em exagero. | Citar ao menos a piora do Haiku 4.5 em "sem piorar a correção". |
| 7 | "Aumento maior na manutenção" ignora o ponto de partida | os 3 | 19 e 41 no N0; 58 e 60 no N2. As tarefas convergem. | Dizer que a manutenção parte de um nível menor e chega perto da construção. |
| 8 | "O efeito veio dos seis modelos" é quase mecânico | Deep Review, research-superpowers | Os outros cinco já estavam no teto. | Dizer que o ganho só poderia aparecer neles. |
| 9 | A ameaça do autor que conhece o critério só é declarada na extensão | os 3 | O `CLAUDE.md` do estudo principal também foi escrito por quem conhece o critério. | Passar para as ameaças gerais. |
| 10 | "Ela não favorece nenhum deles" ignora a interação | os 3 | A frase acrescentada aos enunciados pode reforçar o efeito da orientação. | Reduzir a afirmação. |
| 11 | "Analisadas depois" não vale para CBO e LCOM | os 3 | Essas duas são pré-registradas. | Trocar por "em sua maioria descritivas". |
| 12 | O Sonnet 4.5 também não teve comportamento bom na construção | research-superpowers | Haiku 4.5: 0 de 75. Sonnet 4.5: 0 de 75 (43 com problemas, 32 ruim). O texto só cita o Haiku. | Citar os dois. |
| 13 | "Dimensões ligadas a ela" não se sustenta | os 3 | A arquitetura na construção vai de 16 a 9 a 16. | Restringir ao design do restante do código. |
| 14 | O controle negativo não está nos objetivos da Introdução | research-superpowers, paper-review | A lista P1 a P8 não o menciona. | Acrescentar uma frase. |
| 15 | "Cinco hipóteses principais" não são nomeadas | os 3 | São desenho, correção, qualidade, modelo e exagero. | Nomear na 4.3. |
| 16 | "Exagero" designa medidas diferentes | Deep Review | Ponto do seguro, estrutura no restante do código e controle negativo. | Dizer uma vez qual é qual. |
| 17 | Referências fora de ordem | paper-review | Aniche deveria vir antes de Anthropic. | Inverter. |

## B. Falsos alarmes (basta esclarecer o texto)

- **O N2 da manutenção mistura configurações?** Não. As 75 execuções do N2 têm o mesmo *hash* de configuração, inclusive as do piloto oficial. O piloto em que o Haiku 4.5 abriu a *skill* em 1 de 5 veio antes delas. Era a pergunta que mais podia derrubar o resultado.
- **O exagero no N3 foi apoiado com menos de 5 pares.** A regra dessa comparação é direcional; a dos 5 pares vale só para as hipóteses sem direção.
- **"Quatro perguntas" contra "sem conferência humana".** As quatro são do clube e do seguro (localização, seleção, forma e proporção); frete, cupom e pagamento é que não tiveram conferência.
- **A regra de desempate de 09/10 veio depois do ensaio.** O ensaio do V4 foi decidido em 10/10, depois da regra.

## C. Sugestões que dependem de você

- Tabela com todas as hipóteses e tabela por modelo.
- Separar "acerto" e "alternativa equivalente" na Tabela 3.
- Declarar que uma só tarefa e uma só linguagem limitam a generalização.
- Intervalos de confiança nas proporções.

## D. O que continua sem verificar

A existência das obras, os DOIs, os números atribuídos às fontes e as citações ausentes.
