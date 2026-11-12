Autoavaliação feita com ferramenta de IA. A avaliação oficial é do orientador.

Os critérios usados aqui são as orientações de revisão do professor Rodrigo Rebouças de Almeida para os seus orientandos, e não critérios oficiais da disciplina de TCC, do curso ou da UFPB; quando houver diferença, valem a resolução e as normas oficiais. O uso desta autoavaliação é uso de ferramenta de IA e entra na declaração de uso de IA do TCC (seção 8 do guia).

Texto avaliado: `docs/artigo-v5-e-extensao.md`, versão 138. Como o Markdown não tem páginas, cada apontamento é localizado por seção, parágrafo (§, contado a partir do início da seção) e um trecho curto.

---

## 1. Pontos a tratar

Os quatro primeiros são tratados pelo roteiro de revisão como obrigatórios. Os demais vêm em ordem de peso.

| nº | nível | critério | ponto | onde |
|---|---|---|---|---|
| 1 | OBRIGATÓRIA | 7 e 9 | O texto diz que a forma literal do Decorator apareceu "nas dez rodadas do Sonnet 5.5 e em uma do Sonnet 4.5" no contexto da construção, e a Tabela 15 mostra 5 e 0 na construção | Seção 7, P4; Tabela 15 |
| 2 | OBRIGATÓRIA | 6 | Regras de decisão incompletas: o veredito "inconclusiva" (7 hipóteses) não é definido; o limiar de 36 em 55 não tem motivo; o "mínimo estipulado" das medidas binárias não é informado | Seção 5.1, §4; Tabela 4 |
| 3 | OBRIGATÓRIA | 6 | A segunda revisão (Fable 5.1, 30 execuções, 195 de 210, kappa) e a primeira rodada de calibração (50 de 52) aparecem só nas Limitações, sem etapa no método | Seção 8, §5; Seção 6.2, §5 |
| 4 | OBRIGATÓRIA | 8 | Código em imagem nas Figuras 2, 5, 6, 7 e 8 (ver nota sobre o acordo com o orientador) | Seções 2 e 6.2 |
| 5 | RECOMENDADA | 2 e 7 | A Conclusão não retoma a P7 (estabilidade) nem a parte de custo da P4 | Seção 9 |
| 6 | RECOMENDADA | 9 | A Conclusão omite a queda do comportamento na manutenção, que a Discussão apresenta como custo | Seção 9, §2; Seção 7, P6 |
| 7 | RECOMENDADA | 3 | "Estrutura desnecessária" usada na Conclusão para os controles negativos, contra a definição da Seção 6.2 | Seção 9, §2; Seção 6.2, §6 |
| 8 | RECOMENDADA | 3 | "Acerto" designa a categoria e também a soma acerto + alternativa equivalente | Seções 6.3 e 7 |
| 9 | RECOMENDADA | 3 | "Pacote" designa o projeto de uma execução e também o pacote Java | Seções 4, 5.3 e 6.2 |
| 10 | RECOMENDADA | 9 | A P8 da Introdução diz que só a coesão tem hipótese; a Seção 5.1 e a Discussão dizem que o CBO e a LCOM têm | Seção 1, §9; Seção 5.1, §3; Seção 7, P8 |
| 11 | RECOMENDADA | 9 | "Os 11 modelos Claude disponíveis" e, na frase seguinte, "O Fable foi excluído" | Seção 5.1, §3 |
| 12 | RECOMENDADA | 4 | O Resumo não diz que a extensão usou três modelos e um verificador de IA, e a última frase não diz a qual estudo se refere | Resumo e Abstract |
| 13 | RECOMENDADA | 6 | Sem motivo declarado: cinco rodadas no estudo principal e o valor da margem de −7 (13%) | Seção 5.1, §3 e §4 |
| 14 | RECOMENDADA | 6 | Medidas definidas só nos Resultados: a estabilidade (P7) e o teste de reserva simultânea | Seção 6.3, §3; Seção 6.2 |
| 15 | RECOMENDADA | 10 | Citações diretas de livro sem página (Fowler 2018; Martin 2003) | Seção 1, §4; Seção 2, §5 |
| 16 | RECOMENDADA | 1 | Frases longas com várias informações encadeadas | Seção 4, §6; Seção 5.1, §3; Seção 6.2, §1 |
| 17 | RECOMENDADA | 11 e guia, seção 8 | O texto declara a IA usada na pesquisa (Tabela 1), mas não diz se houve IA na redação e na revisão do texto | Seção 4, §6 e Tabela 1 |
| 18 a 25 | SUGESTÃO | vários | Ver os apontamentos por critério | |

Contagem: 4 OBRIGATÓRIAS, 13 RECOMENDADAS e 8 SUGESTÕES.

---

## 2. Leitura macro

### 2.1 Mapa das seções

| seção | o que afirma ou entrega | o que prepara |
|---|---|---|
| Resumo / Abstract | Objetivo, os dois estudos, números principais (220, 67% para 93%, 450, nove de dez, 88 de 90) | |
| 1. Introdução | LLM, agente, *harness*, os três mecanismos de configuração, *pattern* e exagero, construção e manutenção, relevância, problema, objetivo geral, P1 a P8, contribuições, organização | Conceitos de OO para ler os resultados |
| 2. Fundamentação | Encapsulamento do que varia, polimorfismo, aberto/fechado, acoplamento e coesão, métricas CK | As medidas da P8 |
| 3. Trabalhos relacionados | Kim, Kjellberg, Zisa, Ampatzoglou, arquivos de contexto, Kim 2026; a lacuna | O desenho dos dois estudos |
| 4. Visão geral do processo | Fases (Fig. 3), desenho (Fig. 4), repositório, envolvidos, IA por etapa (Tab. 1), níveis (Tab. 2), tecnologias (Tab. 3), isolamento e *scripts* | Estudo principal |
| 5. Estudo principal | Método, 28 hipóteses (Tab. 4), proteção contra decisões posteriores, resultados (Tab. 5 e 6) | Os pontos em aberto que motivam a extensão |
| 6. Extensão | Evolução do desenho, método, verificador e calibração, controles negativos, resultados (Tab. 7 a 12, Fig. 9 e 10) | Discussão |
| 7. Discussão | Responde P1 a P8 e discute a trajetória | Limitações e Conclusão |
| 8. Limitações | Ameaças gerais, por estudo, da avaliação por IA, da execução e das métricas | |
| 9. Conclusão | Síntese dos dois estudos, recomendação prática, trabalhos futuros | |
| Apêndice A | Tabelas 13 a 16 por modelo | |

O parágrafo de organização (Seção 1, último §) anuncia exatamente as Seções 2 a 9 e o Apêndice A, na ordem em que aparecem. A Seção 7 se chama "Discussão" e é anunciada como a seção que "responde às perguntas de pesquisa", o que corresponde ao conteúdo. Não há seção anunciada que falte nem seção não anunciada.

### 2.2 Linha do argumento

A sequência problema, objetivo, método, resultado e conclusão se fecha nos dois estudos. Os pontos em que um conceito aparece antes de ser definido estão no apontamento 18 (quartetos, Semgrep e pacotes na Seção 4, N3 na Tabela 1). O veredito "inconclusiva" é usado na Tabela 4 sem definição (apontamento 2). A conclusão decorre dos resultados; as diferenças de cautela estão nos apontamentos 6 e 12.

### 2.3 Resumo, Introdução e Conclusão

Os três prometem e afirmam o mesmo nos números centrais (11 modelos, 220 execuções, 67% para 93%, nove de dez combinações, exceção do Strategy na construção, 450 e 90 execuções, 88 de 90). A cautela sobre o Haiku 4.5 aparece nos três. As diferenças: o Resumo omite que a extensão vale só para três modelos e usa avaliação por IA (apontamento 12); a Conclusão não retoma a P7 nem o custo da P4 (apontamento 5) e omite a queda do comportamento (apontamento 6).

### 2.4 Números repetidos

Conferi cada número que aparece em mais de um lugar e somei as tabelas com desdobramento. Os que fecham:

- Tabela 13 (estudo principal por modelo): N0 soma 37 de 55 (67%), N1 e N2 somam 51 (93%), N3 soma 54 (98%), como na Tabela 5. O saldo H1 dos seis modelos fora do teto soma +14 ("14 pares melhores e nenhum pior", Seção 5.3, §2). O saldo H3 soma +3 ("saldo de +3 em 55", Seção 5.3, §1). Haiku 4.5 com 0+1+1+4 = 6 ("6 acertos em 20") e +3 do N2 ao N3 ("3 pares melhores em 5").
- Exagero no seguro: 0%, 2%, 5% e 0% correspondem a 0, 1, 3 e 0 em 55, coerente com "apenas 1 dos 55 pares" (H2) e "de 3 execuções em 55 para nenhuma" (H26).
- Custo: Tabela 5 × 55 dá US$ 345,95 ("US$ 346"); Tabela 11 × 75 dá US$ 380,25 ("US$ 380").
- Tabela 8 soma, por nível e tarefa, 41, 53, 60, 19, 33 e 58, como na Tabela 7 e na Figura 9; a Figura 10 repete a Tabela 8 sem diferença.
- Tabela 14 fecha com a Tabela 7 em todas as colunas (*pattern*, *design*, arquitetura, testes, banco, comportamento) e com a estrutura desnecessária do texto (4, 5, 9 e 0, 1, 2). Os números por modelo da Seção 6.3, §2 (22 para 25, 11 para 25, 14 para 22, 5 para 20, 5 para 13, 3 para 13) batem com ela.
- Tabela 15 soma 4, 10, 10 na construção e 3, 5, 8 na manutenção, como a linha do Decorator na Tabela 8.
- Tabela 10 soma 224 execuções na construção e 225 na manutenção (449), e acerto + alternativa dá 154 e 110, iguais às somas da Tabela 7. As médias ponderadas da Tabela 10 (interfaces, métodos abstratos, tipos, WMC) reproduzem as médias da Tabela 9.
- Tabela 16 soma 96, 103 e 95 por nível e 89, 96 e 109 por modelo (294 de 360, 82%), e 54 combinações estáveis de 90; o Sonnet 5.5 com orientação tem 9 + 10 = 19 de 20.
- "+18 contra +8" (P1 e Limitações), "+14 e +12", "+39 e +19" (P5), "+25 contra +14" (P4), "69 de 75" = 19 + 25 + 25, "20 das 30, 18 delas contidas", "195 de 210" = 30+30+29+29+27+26+24, "15 diferenças" e "52 notas" = 20+20+4+4+4 estão corretos. Os intervalos "22% a 46%" e "3% a 27%" da P8 se reproduzem com os valores das Tabelas 6 e 9.
- 450 = 90 + 144 + 216; 540 revisões = 450 + 90; 32 calibrações = 26 + 6.

A única divergência numérica encontrada é a do apontamento 1:

- Seção 7, P4: "No Decorator da construção, o número de acertos não mudou (10 de 15 no N1 e no N2), mas a forma mudou: a forma literal do Decorator apareceu com a *skill*, nas dez rodadas do Sonnet 5.5 e em uma do Sonnet 4.5".
- Tabela 15, construção, N2: Sonnet 5.5 "5 / 0" e Sonnet 4.5 "0 / 5". As dez rodadas do Sonnet 5.5 e a uma do Sonnet 4.5 só aparecem somando a manutenção (Sonnet 5.5 "5 / 0"; Sonnet 4.5 "1 / 1").

### 2.5 Termos centrais

Mantêm nome e sentido: configuração, nível (N0 a N3, com a ressalva explícita da Tabela 2), ponto do clube, ponto do seguro, teto, quarteto, controle negativo, contenção, verificador, *harness* (com a nota sobre o uso no pré-registro). Problemas: "acerto" (apontamento 8), "pacote" (apontamento 9), "estrutura desnecessária" (apontamento 7), "inconclusiva" sem definição (apontamento 2), "arquivos" contra "classes" na H8 (apontamento 19).

### 2.6 Proporção

A proporção é adequada. A fundamentação é curta e serve às medidas; método e resultados dos dois estudos são as partes mais longas; a Discussão é proporcional às oito perguntas. As Limitações são longas, mas cada parágrafo trata de uma ameaça concreta do trabalho.

### 2.7 Percurso de cada objetivo

Está na matriz da parte 4. Nenhuma pergunta some antes da Discussão; a P7 e o custo da P4 não chegam à Conclusão.

### 2.8 Proveniência dos resultados

| resultado | etapa de origem |
|---|---|
| Acerto nos pontos e exagero no seguro (Tab. 5, 13) | Semgrep e conferência humana (Seção 4, §6; 5.1, §4) |
| Suíte de aceitação | 21 casos, validados com 17 versões defeituosas (5.1, §4) |
| Complexidade, linhas, duplicação | SonarQube (5.1, §4) |
| CK (Tab. 6, 9, 10) | CK, *commit* 8a1ef916 (Seção 2, §6; 6.2, §5) |
| *Tokens*, custo, tempo, turnos (Tab. 5, 11) | Transcrição do Claude Code (Seção 4, último §) |
| Abertura da *skill* e uso do revisor | Transcrição e `extract-meta.mjs` / `run_one_scenario.py` (5.1, §5) |
| Teste de Page | Descrito em 5.1, §5 |
| Categoria do *pattern* e notas (Tab. 7, 8, 14, 15) | Verificador e calibração (6.2, §4 e §5) |
| Controles negativos (Tab. 12) | 6.2, §6 e §7 |
| Estabilidade (Tab. 16) | Cinco rodadas (6.2, §5); a medida não é definida no método (apontamento 14) |
| Reserva duas vezes com pedidos simultâneos (6.3, §3) | O método só menciona "um roteiro de chamadas à API por cenário" (apontamento 14) |
| Segunda revisão, kappa e primeira calibração 50 de 52 (Seção 8, §5) | Sem etapa no método (apontamento 3) |
| Constantes de *enum* com corpo próprio e classes anônimas (Tab. 6) | Contagem não descrita (apontamento 24) |
| Vereditos de *tokens* com o subagente incluído (Seção 8, último §) | Recálculo não descrito (apontamento 24) |

### 2.9 Justificativa das escolhas

Têm motivo declarado: os 11 modelos e a exclusão do Fable, o esforço *medium*, o quarteto simultâneo, a ordem sorteada, o enunciado na voz do dono, a troca de Spring por Javalin, a inclusão do banco, a troca da *skill*, a frase acrescentada aos enunciados, os três modelos da extensão, o Opus 5.5 como verificador, a ordem das repetições, as duas rodadas dos controles, as quatro execuções refeitas, a instrução da *skill* no primeiro item na manutenção, as cinco rodadas da extensão (nas Limitações). Sem motivo: o número de rodadas do estudo principal, o valor −7, o limiar de 36 em 55, o mínimo das binárias (não informado), a regra de 40 de 44 da conferência humana (pré-registrada, sem motivo no texto), a escolha do Fable 5.1 e de 30 execuções para a segunda revisão.

---

## 3. Apontamentos por critério

### Critério 1. Língua portuguesa e gramática

O texto não tem erros de ortografia, concordância ou regência que eu tenha encontrado, e o tempo verbal é coerente (método no passado, discussão no presente). Não encontrei marcadores pendentes nem resíduos de conversa.

**16. RECOMENDADA. Frases longas.**
- Onde: Seção 4, §6 ("O autor supervisionou essas etapas por meio do congelamento do pré-registro antes da primeira execução, da autorização das rodadas, do registro das decisões no repositório, das conferências automáticas de cada lote, como o *script* `verify.mjs`, que recusa…"); Seção 5.1, §3 ("…escolhido para que todas as execuções coubessem na cota de uso, em um contêiner."); Seção 6.2, §1 ("No N2 da construção, a instrução de usar a *skill* é o último item do arquivo; no N2 da manutenção, ela foi deslocada para o primeiro item, depois de execuções preliminares, anteriores às relatadas aqui, em que…").
- Motivo: cada frase encadeia várias informações, e o leitor precisa reler para saber a que se liga cada oração. Na Seção 5.1, "em um contêiner" fica solto no fim e parece explicar a cota.
- Correção: uma informação principal por frase. Exemplo com a sua frase:
  - Antes: "…com esforço *medium*, parâmetro do Claude Code que regula o grau de raciocínio do modelo, escolhido para que todas as execuções coubessem na cota de uso, em um contêiner."
  - Depois: "Cada execução rodou em um contêiner, com esforço *medium*, o parâmetro do Claude Code que regula o grau de raciocínio do modelo. Esse esforço foi escolhido para que todas as execuções coubessem na cota de uso."

**23. SUGESTÃO. Fórmulas de ênfase e contraste montado.**
- Onde: Seção 7, P2 ("A extensão recomenda cautela"; "Os números são pequenos, mas a direção não é a de ausência de custo"); Seção 7, §9 ("Por fim, cabe considerar a trajetória do estudo"); Seção 7, P1 ("A exceção merece registro, por tratar-se do…").
- Motivo: a extensão não recomenda nada, quem recomenda é o autor a partir dos dados; o "não é a de ausência de custo" diz por negação o que pode ser dito de forma direta.
- Correção: afirme o fato. Exemplo: "Os números são pequenos, mas apontam para um custo: a estrutura desnecessária aumentou com a *skill*."

### Critério 2. Encadeamento lógico e coerência textual

**5. RECOMENDADA. Perguntas que não chegam à Conclusão.**
- Onde: Seção 9. A P7 (estabilidade) é respondida na Seção 7 ("uma execução isolada não basta"), mas a Conclusão não a menciona. A P4 pergunta "a que custo"; a Discussão responde (US$ 1,98 no N3 contra US$ 1,47 no N2; US$ 0,66 para US$ 0,72 na manutenção), e a Conclusão não traz o custo monetário nem o resultado do N3.
- Motivo: cada pergunta anunciada na Introdução é um compromisso com o leitor. Como a resposta explícita existe na Discussão, a rastreabilidade se cumpre, mas quem lê só Resumo e Conclusão não sabe o que o trabalho concluiu sobre estabilidade e custo.
- Correção: acrescente à Conclusão uma frase com a resposta da P7 e uma com o custo e o efeito do N3, com os números que a Discussão já tem.

**18. SUGESTÃO. Termos usados antes de serem definidos.**
- Onde: Seção 4, §6 ("conferiu a classificação do Semgrep em 44 pacotes") e Seção 4, último § ("disparam os quartetos"), antes de Semgrep, pacote e quarteto serem apresentados na Seção 5.1; Tabela 1 ("subagente revisor do N3"), antes da Tabela 2 definir os níveis; Seção 4, §5 ("o uso de um teste de tendência"), definido só em 5.1, §5.
- Motivo: o leitor encontra o termo sem contexto e precisa adivinhar.
- Correção: uma remissão curta ("descrita na Seção 5.1") ou a mudança da Tabela 2 para antes da Tabela 1.

### Critério 3. Terminologia e siglas

As siglas do domínio (CBO, LCOM, WMC, DIT, LLM, API) são expandidas. Nada a apontar nas siglas.

**7. RECOMENDADA. "Estrutura desnecessária" com outro sentido na Conclusão.**
- Onde: Seção 6.2, §6 define: "de estrutura desnecessária, a estrutura sem necessidade no restante do código da extensão; e de contenção, nos controles negativos, a mudança pequena". Seção 9, §2: "nos controles negativos da manutenção, a estrutura desnecessária foi rara: 88 de 90 execuções mantiveram a mudança pequena".
- Motivo: você mesmo separou os três termos para que o leitor não os confunda; na Conclusão, o termo da construção passa a designar o resultado dos controles.
- Correção: use "contenção" ou "exagero" para os controles, como na Seção 6.2.

**8. RECOMENDADA. "Acerto" com dois sentidos.**
- Onde: Seção 6.2, §4 define "acerto" como uma das quatro categorias, distinta de "alternativa equivalente". Na Seção 7, "acertos" passa a significar acerto + alternativa: P1 ("de 19 para 33 e 58 acertos em 75"); P4 ("No Decorator da construção, o número de acertos não mudou (10 de 15 no N1 e no N2)"), quando, na categoria acerto, a Tabela 15 mostra 0 no N1 e 5 no N2.
- Motivo: quando o mesmo nome designa duas coisas, o leitor não sabe de qual se fala; no Decorator, as duas leituras dão resultados opostos.
- Correção: reserve "acerto" para a categoria e use "*pattern* certo" (o termo das Tabelas 8 e das Figuras 9 e 10) para a soma.

**9. RECOMENDADA. "Pacote" com dois sentidos.**
- Onde: Seção 4, §6 e Seção 5.3, §1 ("44 pacotes sorteados", "adotada para os 220 pacotes"), em que pacote é o projeto entregue por uma execução; Seção 6.2, §4 e Tabela 3 ("dependências e ciclos entre pacotes, com o jdeps"), em que pacote é o pacote Java.
- Motivo: o mesmo nome para duas coisas no mesmo texto.
- Correção: chame o primeiro de "projeto" (como nas Tabelas 6 e 9, "CBO médio do projeto") e deixe "pacote" para o Java.

**19. SUGESTÃO. Nomes diferentes para a mesma hipótese ou regra.**
- Onde: Tabela 4, H8 ("gera mais arquivos", medida "número de classes"); Seção 5.1, §4 chama a quarta regra de "hipótese do modelo", e a Tabela 4 a chama de "comparação entre modelos".
- Correção: padronize "classes" na H8 e um só nome para a regra.

### Critério 4. Resumo/Abstract

O Resumo tem objetivo, método e resultado, e os números batem com o texto. O Abstract corresponde ao Resumo.

**12. RECOMENDADA. Resumo sem a cautela da extensão.**
- Onde: Resumo, frases 3 a 5 ("Em extensão exploratória (450 execuções), o uso correto aumentou…"; "O ganho concentrou-se nos modelos que não acertavam sem orientação."). O mesmo no Abstract.
- Motivo: a Seção 6.2, §1 diz que as conclusões da extensão "valem para essa faixa, e não para os 11 modelos do estudo principal", e a avaliação da extensão foi feita por um verificador de IA. O Resumo não informa nem uma coisa nem outra, e o leitor que lê só o Resumo atribui à extensão o alcance do estudo principal. A última frase, colocada depois dos números da extensão, parece valer para os dois estudos; na manutenção da extensão, o Sonnet 5.5, que está no teto no estudo principal, teve um dos maiores ganhos (de 11 para 25 em 25, Tabela 14).
- Correção: diga, na frase da extensão, que ela usou três modelos e um verificador de IA calibrado, e diga a qual estudo se refere a última frase.

### Critério 5. Introdução

A Introdução apresenta problema, contexto, relevância com números, objetivo geral e as perguntas P1 a P8, que correspondem ao que foi feito. O AYTY não é mencionado, de modo que o ponto do contexto institucional não se aplica. O apontamento sobre a P8 está no critério 9 (apontamento 10).

### Critério 6. Coerência metodológica

O método descreve o que foi feito, como e com quais ferramentas e versões (Tabela 3), e a ordem das etapas (Figura 3). Os envolvidos estão identificados: autor, orientador e agentes de IA, com modelo e esforço por etapa (Seção 4, §5 e §6, Tabela 1). As etapas do método aparecem nos resultados.

**2. OBRIGATÓRIA. Regras de decisão incompletas.**
- Onde: Seção 5.1, §4 e Tabela 4.
  - O método define "apoiada", "contrariada" e "não apoiada", mas a Tabela 4 dá o veredito "inconclusiva" a sete hipóteses (H8, H15, H16, H21, H24, H27, H28) sem que o texto diga quando ele se aplica. Na H16, que é de alteração, o método diz que a alteração que não aparece se chama "não apoiada", e a tabela diz "inconclusiva".
  - "nas medidas contínuas, quando ao menos 36 dos 55 pares vão na mesma direção": o número 36 não tem motivo. Desse limiar dependem os vereditos de H9 e H10, que sustentam a resposta da P8 sobre acoplamento e coesão.
  - "quando há ao menos 5 pares não empatados e o lado maior atinge o mínimo estipulado no pré-registro": o mínimo não é informado.
- Motivo: o roteiro trata como obrigatória a falta de motivo quando a escolha muda a leitura do resultado, e o critério de avaliação é o caso típico. Sem a definição de "inconclusiva" e sem os valores, o leitor não consegue conferir sete vereditos e não sabe por que H9 e H10 não foram apoiadas.
- Correção: defina "inconclusiva" na lista de regras, informe o mínimo das medidas binárias e diga, em uma oração com "porque", de onde vem o 36.

**3. OBRIGATÓRIA. Segunda revisão e primeira calibração sem etapa no método.**
- Onde: Seção 8, §5 ("Antes, uma primeira rodada teve 50 de 52; depois dela, os testes de duas soluções foram completados…"; "o Claude Fable 5.1 … revisou às cegas 30 execuções sorteadas, e 195 de 210 notas coincidiram"; "kappa de Cohen de 0,53"). A Tabela 1 lista o "segundo verificador (30 revisões)". A Seção 6.2, §5 diz só que "As 52 notas coincidiram com as esperadas".
- Motivo: são resultados (concordância, kappa, primeira calibração) cuja coleta não está na metodologia. O roteiro trata resultado sem etapa de origem como obrigatório. A descrição parcial que está nas Limitações não diz como as 30 execuções foram sorteadas (com semente?), por que o Fable 5.1 e por que 30, nem como o kappa foi calculado. A Seção 6.2 também dá a impressão de calibração em uma só rodada, o que as Limitações corrigem.
- Correção: descreva na Seção 6.2 as duas rodadas de calibração e a segunda revisão (quem, quantas, como sorteadas, por que esse modelo e esse número, que medida de concordância), e deixe nas Limitações só a leitura do que ela significa.

**13. RECOMENDADA. Escolhas sem motivo.**
- Onde: Seção 5.1, §3 ("Cada quarteto foi repetido em cinco rodadas") e §4 ("não é inferior a −7 em 55 pares, limite de 13% dos pares fixado no pré-registro, porque, mesmo sem efeito da configuração, alguns pares diferem por acaso").
- Motivo: o motivo das cinco rodadas aparece só para a extensão, nas Limitações ("decorreu do custo"). Na margem, o texto explica por que existe uma margem, mas não por que ela é 7. Como o saldo da H3 foi +3, positivo, a escolha da margem não muda a leitura; por isso o nível é recomendado.
- Correção: uma oração com "porque" para cada escolha.

**14. RECOMENDADA. Medidas definidas só nos Resultados.**
- Onde: Seção 6.3, §3: "O resultado da primeira rodada, em termos de *pattern* certo ou não, repetiu-se em 294 das 360 rodadas novas" e "59 das 225 execuções de construção permitiram que o mesmo quarto fosse reservado duas vezes quando dois pedidos chegavam simultaneamente, e 16 não chegaram a esse teste".
- Motivo: a P7 é uma pergunta de pesquisa, e a medida que a responde (comparar cada rodada nova com a primeira) só é definida no resultado. O teste de pedidos simultâneos é um resultado de comportamento sem descrição no método, que fala apenas em "um roteiro de chamadas à API por cenário".
- Correção: defina a medida de estabilidade na Seção 6.2 e diga que o roteiro de comportamento inclui o teste de reserva simultânea.

**24. SUGESTÃO. Contagens sem procedimento descrito.**
- Onde: Tabela 6, "constantes de *enum* com corpo próprio e classes anônimas" (a Seção 2 cita só tipos e métodos abstratos); Seção 8, último § ("os vereditos das hipóteses de *tokens* permanecem os mesmos quando esse consumo é incluído"); Seção 6.2, §7 ("as 12 notas coincidiram"), sem dizer quais são as duas notas por solução.
- Correção: uma oração em cada caso dizendo como a contagem foi feita.

### Critério 7. Apresentação da solução, resultados e conclusão

Os resultados são concretos, com números, tabelas e figuras, e o processo de avaliação (suíte, Semgrep com conferência humana, verificador calibrado) está descrito. As limitações são específicas do trabalho, e os trabalhos futuros decorrem dos resultados (Strategy na construção, N3 na extensão, controle na construção, leitura humana das notas).

**1. OBRIGATÓRIA. Número da Discussão diferente da tabela.**
- Onde: Seção 7, P4, e Tabela 15 (as duas citações estão na parte 2.4).
- Motivo: a frase está no contexto do "Decorator da construção" e atribui a ele números que só existem somando construção e manutenção. Um número que não bate com a própria tabela faz o leitor duvidar dos dois.
- Correção: diga que a forma literal apareceu nas cinco rodadas do Sonnet 5.5 na construção, ou deixe claro que as dez rodadas e a do Sonnet 4.5 somam as duas tarefas.

**6. RECOMENDADA. Conclusão com menos ressalvas que a Discussão.**
- Onde: Seção 7, P6: "O comportamento … cai na manutenção (61, 59 e 54 em 75), de modo que o ganho em *pattern* veio acompanhado de alguma perda nessa dimensão". Seção 9, §2: "Nas demais dimensões de qualidade, o modelo e o ponto de partida pesaram mais que a orientação. Quanto aos custos, na construção com a *skill*, a estrutura desnecessária subiu…".
- Motivo: a Conclusão lista os custos e deixa de fora um custo que a Discussão aponta. A conclusão mantém as mesmas ressalvas da discussão.
- Correção: inclua a queda do comportamento na manutenção entre os custos.

### Critério 8. Figuras, tabelas e gráficos

Todas as figuras (1 a 10) e tabelas (1 a 16) têm número, título e fonte, e todas são referenciadas no texto com descrição do que mostram. Os gráficos (Figuras 9 e 10) têm legenda de nível, título que identifica o eixo vertical e categorias no eixo horizontal, e os valores conferem com as Tabelas 7 e 8. Nenhuma imagem de código tem fundo preto.

**4. OBRIGATÓRIA. Código em imagem.**
- Onde: Figura 2 (Strategy, `fig-strategy-intellij-claro.png`, Seção 2), Figura 5 (Observer), Figura 6 (Factory Method), Figura 7 (Decorator) e Figura 8 (Template Method), na Seção 6.2.
- Motivo: o roteiro diz que trechos de código não devem aparecer como imagem, e sim como texto em bloco com fonte monoespaçada, porque em imagem o código não pode ser copiado, pesquisado nem lido por leitores de tela, e perde nitidez na impressão.
- Correção pelo roteiro: substituir cada figura por bloco de código em texto.
- Nota: você relata um acordo com o orientador para manter as cinco figuras como imagem com fundo branco. Como o roteiro escrito ainda pede texto, registre esse acordo onde for adequado e confirme com o orientador; a decisão é dele.

**20. SUGESTÃO. Estatística diferente entre tabelas comparadas.**
- Onde: Tabela 6 usa mediana para WMC e CBO ("WMC da maior classe, mediana"; "CBO médio do projeto, mediana"); Tabela 9 usa média para as mesmas medidas, embora a Seção 6.2, §5 diga "com as mesmas regras de resumo". A conta da P8 ("multiplicando o CBO médio pelos tipos") multiplica, no estudo principal, uma mediana por uma média.
- Correção: diga que "regras de resumo" se refere ao resumo por projeto, e registre na P8 que a conta usa mediana no estudo principal e média na extensão.

**21. SUGESTÃO. Legenda da Figura 4.**
- Onde: Seção 4, §2: "os blocos escuros indicam o que varia em cada um deles, isto é, a configuração".
- Motivo: na extensão também variam a tarefa e o modelo, como a própria figura mostra.
- Correção: "o que é manipulado como tratamento" no lugar de "o que varia".

### Critério 9. Inconsistências de conteúdo

Os dois estudos são mantidos separados com cuidado, e as afirmações sobre o estudo pré-registrado e sobre a extensão exploratória têm graus de cautela diferentes e coerentes.

**10. RECOMENDADA. Alcance das hipóteses da P8.**
- Onde: Seção 1, §9: "em polimorfismo, tamanho das classes, acoplamento e coesão, esta com hipótese apenas no estudo principal (P8)". Seção 5.1, §3: "apenas o CBO e a LCOM integram as hipóteses do pré-registro". Seção 7, P8: "é descritiva, salvo quanto ao CBO e à LCOM do estudo principal".
- Motivo: "esta" retoma só a coesão; as outras duas passagens dizem que acoplamento e coesão têm hipótese (H9 e H10).
- Correção: na Introdução, "estas duas com hipótese apenas no estudo principal", ou forma equivalente.

**11. RECOMENDADA. Número de modelos disponíveis.**
- Onde: Seção 5.1, §3: "Participaram do estudo os 11 modelos Claude disponíveis no Claude Code em 10/10/2026 … O Fable foi excluído em razão do custo". Seção 8, §5: "o Claude Fable 5.1".
- Motivo: se o Fable estava disponível e foi excluído, os disponíveis eram mais de 11.
- Correção: "11 dos modelos Claude disponíveis" ou "os modelos disponíveis, exceto o Fable".

**22. SUGESTÃO. Arquitetura na P6.**
- Onde: Seção 7, P6: "arquitetura, testes e banco oscilam sem direção clara" e "os ganhos aparecem no *pattern* e no *design* do restante do código, mas não na arquitetura". Tabela 7: arquitetura na manutenção 58, 57 e 68.
- Motivo: na manutenção, o N2 tem 10 execuções boas a mais que o N0, o que o leitor pode ler como ganho.
- Correção: mencione esse valor e diga por que não o lê como efeito (por exemplo, o N1 não acompanha).

### Critério 10. Normas e referências

Todas as citações do texto estão na lista e todas as referências da lista são citadas. A numeração das seções e subseções é consistente. Não há marcadores pendentes nem links com parâmetro de rastreio.

Conferi os 26 DOIs da lista no Crossref: todos resolvem para o trabalho citado, com título, primeiro autor e ano iguais aos da lista. O de Arcuri e Briand tem publicação *online* em 2012 e impressa em 2014, volume 24(3), páginas 219-250, como na lista. O de Vokáč et al. resolve pelo doi.org para a Springer. Não conferi: as referências sem DOI (Brown et al. 2020, Vaswani et al. 2017, Yao et al. 2023, os livros e as páginas de ferramentas e da Anthropic) e o link do repositório público. Abra-os antes de entregar.

**15. RECOMENDADA. Citação direta de livro sem página.**
- Onde: Seção 1, §4: Fowler (2018) "descreve como "generalidade especulativa""; Seção 2, §5: Martin (2003) "o enuncia como "aberto para extensão, fechado para modificação"".
- Motivo: o roteiro pede a página nas citações diretas de livro, para que o leitor encontre o trecho.
- Correção: acrescente a página, ou transforme em citação indireta sem aspas.

**25. SUGESTÃO. Recomendação vaga atribuída a uma fonte.**
- Onde: Seção 8, §6: "sendo a recomendação geral um número muito alto de repetições" (Arcuri e Briand 2014).
- Correção: confira no artigo o número que os autores recomendam e use-o no lugar de "muito alto".

### Critério 11. Elementos obrigatórios da resolução

- Introdução com problema e contexto: presente (Seção 1).
- Descrição do processo e dos envolvidos: presente (Seção 4 e Figura 3; autor, orientador e IA por etapa).
- Tecnologias: presente (Tabela 3, com versão e papel).
- Apresentação do que foi feito, com métricas e artefatos: presente (bancada, *scripts*, enunciados, Tabelas 5 a 16, Figuras 1 a 10).
- Avaliação descrita: presente (suíte, Semgrep e conferência humana, verificador e calibração), com a lacuna do apontamento 3.
- Repositório público: presente (nota da Seção 4, https://github.com/FLucasF/TCC). Não abri o link; confira que está acessível.

**17. RECOMENDADA. Declaração de uso de IA na redação.**
- Onde: Seção 4, §6 e Tabela 1 declaram a IA usada no desenho, no disparo, na coleta e na avaliação. O texto não diz se ferramentas de IA foram usadas na escrita ou na revisão do artigo.
- Motivo: a seção 8 do guia pede a declaração de uso de IA, e esta autoavaliação já é um uso que entra nela.
- Correção: confira o que a seção 8 do guia exige e acrescente a declaração no lugar indicado.

---

## 4. Matriz de rastreabilidade

| objetivo ou pergunta | onde é desenvolvido | etapa do método que gera os dados | resultado que atende | resposta explícita | chega à Conclusão | situação |
|---|---|---|---|---|---|---|
| Objetivo geral: avaliar o efeito de orientações persistentes na construção e na manutenção | Seções 5 e 6 | 5.1 e 6.2 | Tabelas 5, 7, 8 e 12 | Seção 9, §1 e §2 | sim | atendido |
| P1: uso do *pattern* esperado | 5.1, 5.3, 6.2, 6.3 | Semgrep com conferência humana (5.1); categoria do verificador (6.2) | Tabelas 5, 8, 13; Figuras 9 e 10 | Seção 7, P1: "aponta para o aumento do uso correto" | sim | atendido; corrigir o termo "acerto" (apontamento 8) |
| P2: exagero e corretude | 5.1, 5.3, 6.2, 6.3 | Semgrep no seguro, suíte, SonarQube; estrutura desnecessária do verificador | Tabelas 4 (H2, H3, H7, H8, H23, H26), 5, 14 | Seção 7, P2: "O código torna-se, assim, menos complexo e maior" | sim | atendido |
| Controle negativo da manutenção (tratado com a P2) | 6.2, 6.3 | 6.2, §6 e §7 | Tabela 12 | Seção 7, §3 | sim | atendido; termo na Conclusão (apontamento 7) |
| P3: variação entre modelos | 5.3, 6.3 | pareamento por modelo e rodada (5.1); notas por modelo (6.2) | Tabelas 13 e 14 | Seção 7, P3: "O efeito concentra-se nos modelos que não acertam sem orientação" | sim | atendido |
| P4: contribuição de cada componente e custo | 5.3, 6.3 | níveis cumulativos, transcrição (custo), abertura da *skill* | Tabelas 4 (H21 a H28), 5, 11, 15 | Seção 7, P4: "o `CLAUDE.md` concentra a maior parte do ganho" e custos do N3 e do N2 | em parte: sem custo nem N3 | parcial (apontamentos 1 e 5) |
| P5: tipo de tarefa | 6.3 | tarefas de construção e manutenção (6.2) | Tabelas 7 e 8; Figuras 9 e 10 | Seção 7, P5: "aumento maior na manutenção somente quando a *skill* é acrescentada" | sim | atendido |
| P6: demais dimensões de qualidade | 6.3 | *skills* e *scripts* do verificador por dimensão (6.2) | Tabelas 7 e 14 | Seção 7, P6: "os ganhos aparecem no *pattern* e no *design* do restante do código, mas não na arquitetura" | em parte: sem a queda do comportamento | parcial (apontamentos 6 e 22) |
| P7: estabilidade entre rodadas | 6.3 | cinco rodadas (6.2); a medida não está no método | Seção 6.3, §3; Tabela 16 | Seção 7, P7: "o que mostra que uma execução isolada não basta" | não | parcial (apontamentos 5 e 14) |
| P8: estrutura orientada a objetos | Seção 2, 5.3, 6.3 | CK (Seção 2; 5.1; 6.2) | Tabelas 6, 9 e 10 | Seção 7, P8: "A orientação substitui a condicional pelo polimorfismo" | sim | atendido; alcance da hipótese (apontamento 10) |

Não há resultado sem objetivo: as dimensões da Tabela 7 servem à P6, os custos à P4, as métricas CK à P8, e a calibração e a segunda revisão sustentam a validade da avaliação.

---

## 5. Checklist do guia (seção 10)

Não tive acesso ao PDF do guia. Os itens abaixo seguem os critérios do roteiro, que o checklist do guia resume. Confira com o PDF se algum item do guia ficou de fora.

| item | situação | observação |
|---|---|---|
| Introdução com problema e contexto | atendido | Seção 1 |
| Objetivos ou perguntas claros e compatíveis com o que foi feito | atendido | P1 a P8; ajuste na P8 (apontamento 10) |
| Processo descrito, em ordem, com ferramentas e versões | atendido | Seção 4, Figura 3, Tabela 3 |
| Envolvidos identificados (aluno, orientador, outros) | atendido | Seção 4, §5 e §6; Tabela 1 |
| Breve descrição das tecnologias | atendido | Tabela 3 |
| Apresentação do que foi desenvolvido, com métricas e artefatos | atendido | Bancada, *scripts*, Tabelas 5 a 16 |
| Processo e resultados da avaliação descritos | parcial | Segunda revisão e primeira calibração fora do método (apontamento 3) |
| Repositório público com link | atendido | Link presente; acesso não conferido |
| Todo resultado com etapa de origem no método | parcial | Apontamentos 3 e 14 |
| Escolhas de método justificadas | parcial | Apontamentos 2 e 13 |
| Cada objetivo com resultado e resposta explícita | atendido | Matriz; todas têm resposta na Seção 7 |
| Objetivos retomados até a Conclusão | parcial | P7 e custo da P4 (apontamento 5) |
| Resumo com objetivo, método e resultado | atendido | Cautela da extensão (apontamento 12) |
| Resumo, Introdução e Conclusão com os mesmos números e a mesma cautela | parcial | Apontamentos 6 e 12 |
| Mesmos números em todo o texto | parcial | Uma divergência (apontamento 1) |
| Termos consistentes | parcial | Apontamentos 7, 8, 9 e 19 |
| Conclusão com resultados concretos, sem frases genéricas | atendido | Seção 9 |
| Limitações e trabalhos futuros específicos | atendido | Seções 8 e 9 |
| Figuras e tabelas numeradas, com título, fonte e descrição no texto | atendido | Figuras 1 a 10, Tabelas 1 a 16 |
| Gráficos com legenda e eixos identificados | atendido | Figuras 9 e 10 |
| Código como texto, sem imagem de fundo preto | pendente | Figuras 2, 5, 6, 7 e 8 em imagem, com fundo branco; acordo relatado com o orientador (apontamento 4) |
| Citações e referências correspondentes | atendido | Todas citadas e listadas |
| DOIs e links conferidos | parcial | 26 DOIs conferidos; links e referências sem DOI não conferidos |
| Citação direta de livro com página | pendente | Apontamento 15 |
| Sem marcadores pendentes nem parâmetros de rastreio | atendido | Nenhum encontrado |
| Numeração das seções consistente | atendido | Seções 1 a 9, Apêndice A |
| Declaração de uso de IA | parcial | IA na pesquisa declarada; IA na redação não (apontamento 17) |
| Redação clara, sem frases longas e fórmulas de ênfase | parcial | Apontamentos 16 e 23 |
