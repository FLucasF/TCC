# Síntese das três revisões da v53

Os relatórios completos estão nesta pasta: `deep-review.md`, `paper-review-system.md` e `research-superpowers.md`. Cada um
seguiu o método de um plugin, leu só o artigo e não viu os outros. A coluna "nos dados" foi conferida depois, no repositório.

**Vereditos:** Deep Review, revisão maior; research-superpowers, revisão maior; paper-review-system, *borderline*, 3 de 5
(ponderada 3,3). Os três foram feitos por IA da mesma empresa que os modelos estudados, e não substituem um avaliador humano.
Eles dizem que quase tudo se corrige com a redação e com dados já coletados.

## A. Problemas confirmados nos dados, do mais grave ao menos grave

| # | Problema | Revisores | O que os dados mostram | Correção proposta |
|---|---|---|---|---|
| 1 | O efeito "mais forte na manutenção" compara `CLAUDE.md` diferentes | os 3 | O texto do `CLAUDE.md` é outro em cada tarefa: na manutenção diz "reorganize primeiro" e "vale também para os casos que já existem", e a linha da *skill* foi para o primeiro item só no N2 da manutenção (21/10). Só com o arquivo, o ganho é +14 contra +12; a diferença vem do N2 (+39 contra +19). A *skill* foi aberta igualmente nas duas tarefas (69 de 75). | Descrever a diferença no método, declarar a limitação e reduzir a frase do Resumo e da Conclusão. |
| 2 | Os ganhos do N1 vêm sobretudo dos dois cenários favorecidos | Deep Review | De N0 a N1, Decorator e Template Method somam +18; os outros três, +8. São os dois cenários que as precisões do `CLAUDE.md` aproximam, e o texto declara o favorecimento, mas não o tamanho. | Dar os números na Discussão e nas Limitações. |
| 3 | "Nove das dez" só vale de N0 a N2, e o Strategy na construção não é explicado pelo teto | os 3 | De N0 a N1, o Strategy vai de 15 para 14 em 30. Sem o Sonnet 5.5, a construção fica em 4, 2 e 4 em 10. | Dizer no Resumo que é de N0 a N2 e citar a exceção; tirar a explicação pelo teto. |
| 4 | Só um dos quatro pontos do Strategy é relatado | Deep Review, paper-review | Os outros também sobem. Entrega: 78, 96, 96 e 98%. Cupom: 65, 93, 93 e 100%. Pagamento: 55, 91, 95 e 98%. Esses três não tiveram conferência humana. | Acrescentar à Tabela 1, como medidas secundárias. |
| 5 | O controle negativo é afirmado com força demais | os 3 | 30 das 88 são do N0. No N2, a *skill* foi aberta em 20 de 30 (18 de 20 contidas). E o `CLAUDE.md` diz que "uma mudança pontual" pede "uma mudança simples", o que quase descreve esses cenários. | Trocar "não leva o agente" por "não se observou" e declarar o texto do arquivo como limitação. |
| 6 | "Calibrado" no Resumo promete mais do que a calibração cobriu | os 3 | As 128 execuções com malfeito ou alternativa equivalente (de 449) estão em classes que a calibração não testou. | Qualificar no Resumo. |
| 7 | "Oscilam sem direção clara" não vale para o comportamento na manutenção | os 3 | A Tabela 3 mostra 61, 59 e 54. | Relatar a queda. |
| 8 | A *skill* pública do estudo principal não foi testada | Deep Review, research-superpowers | Ela não foi aberta em nenhuma das 110 execuções, e "não acrescentou efeito" sugere que foi avaliada. Na extensão, falta a taxa de abertura: 19 de 25 (Haiku 4.5), 25 de 25 (dois Sonnet), nas duas tarefas. | Dizer que a *skill* não foi usada; informar a taxa da extensão. |
| 9 | "O `CLAUDE.md` responde pela maior parte" depende da ordem dos níveis | os 3 | Na manutenção, a *skill* soma +25 e o arquivo, +14. | Restringir ao estudo principal. |
| 10 | O *commit* não prova a data | research-superpowers, paper-review | A data de um *commit* pode ser definida por quem o faz. | Trocar "permite verificar a data" por "permite conferir o conteúdo registrado". |
| 11 | "Sugerem o mecanismo" é em parte circular | os 3 | As métricas redescrevem a troca de condicionais, e a complexidade conta condicionais. | Trocar por "são compatíveis com". |
| 12 | A duplicação só aparece na Discussão | research-superpowers, paper-review | O 1,2% para 2,5% é um resultado sem lugar na Seção 4.3. | Mover para os Resultados. |
| 13 | A Figura 2 mostra só um multiplicador mudando | paper-review | O exemplo usa 1,2 e 0,9, o caso que a Seção 4.1 chama de exagero. | Trocar o exemplo por planos com regras diferentes. |

## B. Falsos alarmes (não mudam nada, mas o texto pode evitar a confusão)

- **"Três a mais" contra +7 da Tabela 1.** São medidas diferentes. A hipótese usa os pontos da suíte (10 pares melhores, 7 piores, saldo +3), e a Tabela 1, a suíte inteira aprovada. Basta dizer qual é a medida.
- **"Sem a direção de nenhum efeito" contra "a métrica que mais mudou".** São ensaios diferentes: o primeiro é o V4, e o segundo, o EXT (V3). Basta nomear o ensaio na Seção 7.

## C. Decisões suas

- Publicar o texto dos `CLAUDE.md` e da *skill* em apêndice ou apontar a pasta do repositório. Os três revisores sentiram falta.
- Relatar o RFC ou retirar da Seção 4.1.
- Acrescentar uma tabela por modelo e o cruzamento de classe por nível.
- Critério numérico da regra "de alteração": confira no pré-registro antes de escrever, porque o artigo não o traz.

## D. O que os revisores não verificaram

A existência e os metadados das referências, a fidelidade das citações às fontes e a literatura mais recente. Dentro do texto, as 40 referências e as citações batem nos dois sentidos.
