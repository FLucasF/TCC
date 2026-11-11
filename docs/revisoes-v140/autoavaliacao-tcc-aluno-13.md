Autoavaliação feita com ferramenta de IA. A avaliação oficial é do orientador.

# Autoavaliação antes da entrega: artigo, versão 140 (rodada 13)

Este relatório aplica os critérios de revisão do professor Rodrigo Rebouças de Almeida, que são orientações dele para os seus orientandos e não critérios oficiais da disciplina, do curso ou da UFPB. O uso desta ferramenta é uso de IA e entra na declaração de uso do TCC (seção 8 do guia). O relatório não diz se o trabalho está aprovado e não reescreve o seu texto.

No total, são 1 obrigatório, 1 recomendado e 1 sugestão. Os pontos já decididos usam a numeração D1 a D21, para não se confundirem com os apontamentos.

## Pontos já decididos

Estes pontos foram decididos por você, já foram verificados ou dependem de você. Nenhum conta como pendência neste relatório, e nenhum apontamento abaixo pede lista no corpo do texto.

**Protegidos por decisão sua**

- D1. As Figuras 2, 5, 6, 7 e 8 ficam como imagem com fundo branco. Abri as cinco, e todas têm fundo branco.
- D2. A Seção 6.2 mantém uma figura por *pattern*.
- D3. As Figuras 9 e 10 ficam junto das Tabelas 7 e 8.
- D4. A declaração de uso de ferramentas de IA será escrita por último. O apoio à redação do artigo não entra na Tabela 1.
- D5. A coluna "como foi conferido" da Tabela 1 foi confirmada por você.
- D6. O título continua o mesmo, e o nome da H8 segue o pré-registro.
- D7. Os motivos que você deu estão mantidos: o Strategy por ser difundido e adequado ao domínio; os cinco *patterns* por cobrirem as três categorias do catálogo; o N3 fora da extensão; a segunda rodada do controle decidida antes do resultado da primeira; as 10 horas por causa da janela de 5 horas da cota; os três modelos da extensão; o Opus 5.5 como verificador.
- D8. As referências ficam só com revistas e eventos respeitados, sem *preprints*, com correções só de textos publicados, e a definição de IA vem de livro.
- D9. O registro é prosa impessoal em parágrafos encadeados, sem listas no corpo e sem rótulos em negrito no começo do parágrafo. As perguntas P1 a P8 ficam em parágrafo.
- D10. O Resumo fica entre 150 e 250 palavras, e o Abstract é tradução fiel. Contei 250 palavras em cada um.
- D11. A Introdução define cada termo para o leitor.
- D12. *Harness* segue o sentido de Galster et al., com o aviso sobre o pré-registro.
- D13. O uso só de modelos da Anthropic está declarado como limitação.

**Já verificado: não procede**

- D14. Tabela 11, *tokens* de saída do N2 da construção (38,9 mil): todas as células usam a soma dos segmentos. Somei os eventos `result` das transcrições, e as seis colunas conferem.

**Pendências que dependem de você**

- D15. Tabela comparativa dos trabalhos relacionados (Apêndice C do guia), um nome só para os níveis e "et al." nas referências.
- D16. Citar Page (1963) e Cohen (1960) depende de você ler ou aprovar. Itálico nas figuras.
- D17. O motivo de a hipótese "quem melhora não é quem exagera" e a proporção entre ler, escrever e executar não terem sido calculadas (OBJETIVO §4.5 e §4.6). O texto só registra o fato.
- D18. O aviso da API do OpenAlex pedindo o e-mail do usuário, de uma rodada anterior, não foi atendido. Nesta rodada não usei o OpenAlex, e as consultas ao Crossref foram feitas sem e-mail.
- D19. Yao et al. 2023 continua sem conferência por automação e precisa ser aberta por você.
- D20. O conteúdo de Kim (2025) e Kim (2026) não pôde ser confirmado: o Crossref traz só os metadados. Abra esses dois antes da entrega.
- D21. Na rodada 10, o agente baixou PDFs abertos de Ahmed, Wang e Ampatzoglou para conferir e os apagou depois, sem pedir antes. Nesta rodada, nenhum arquivo foi baixado, e nenhuma página ou API trouxe instrução dirigida a mim.

## Pontos a tratar

### Obrigatórios

O roteiro de revisão trata como obrigatório o resultado que não corresponde ao que os dados mostram.

| nº | ponto | onde |
|---|---|---|
| 1 | O texto relata uma execução em que o agente abriu a *skill* e terminou sem implementar, na construção. Os dados têm uma segunda, na manutenção, que entra nos números do comportamento citados no Resumo e na Conclusão. | Seção 6.3, P1; Seção 7, P4 e P6 |

### Recomendados

| nº | ponto | onde |
|---|---|---|
| 2 | A *skill* da extensão antecipa quase com as mesmas palavras a frase-isca dos controles negativos, e o texto só atribui esse efeito ao `CLAUDE.md`. | Seção 6.2; Seção 7, P2; Seção 8 |

### Sugestões

| nº | ponto | onde |
|---|---|---|
| 3 | O motivo do esforço *medium* no estudo principal não coincide com o registro de decisões. | Seção 5.1, terceiro parágrafo |

## Leitura macro

**1. Mapa das seções.** A Seção 1 apresenta conceitos, problema, objetivo e P1 a P8. A Seção 2 dá os conceitos e as métricas. A Seção 3 delimita a lacuna. A Seção 4 mostra fases, envolvidos, papel da IA, níveis e tecnologias. A Seção 5 traz o método, a proteção contra decisões posteriores e os resultados do estudo principal. A Seção 6 faz o mesmo para a extensão. A Seção 7 responde às oito perguntas. A Seção 8 traz as limitações, a Seção 9 conclui, e o Apêndice A reúne os dados por modelo. O parágrafo de organização corresponde a essa ordem.

**2. Linha do argumento.** Problema, objetivo, método, resultados e conclusão se encadeiam, e a passagem do estudo principal para a extensão está explicada na Seção 6.1.

**3. Resumo, Introdução e Conclusão.** Os três têm os mesmos números e a mesma cautela: a piora do Haiku 4.5, a exceção do Strategy na construção, a queda do comportamento na manutenção e a alta da estrutura desnecessária com a *skill*. O Abstract corresponde frase a frase ao Resumo. A queda do comportamento tem o componente tratado no apontamento 1.

**4. Números repetidos.** Refiz as contas a partir dos dados e somei as tabelas. Os números conferem. O apontamento 1 é uma omissão de caso, e nenhuma célula de tabela precisa mudar.

| números | fonte e regra | resultado |
|---|---|---|
| Tabelas 7, 8, 14 e 15 e Figuras 9 e 10 | 450 `review.json`; *pattern* certo = acerto + alternativa equivalente; soma de modelos, de *patterns* e de níveis | conferem |
| categorias: 27 e 30, 7 e 26, 32 e 15, 55 e 56, 9 e 14; 12, 8, 4 e 2; 45, 53 e 72; 48, 66 e 69; omissão 6, 1, 1 e malfeito 0, 7, 5 no Strategy | mesmos `review.json` | conferem |
| +18 e +8; 15 para 14 em 30; 14 e 12; 39 e 19; +25 e +14 | soma das células da Tabela 8 | conferem |
| 294 de 360, 54 de 90 e Tabela 16 | rodadas 2 a 5 contra a primeira, *pattern* certo ou não | conferem |
| Tabela 12; 88 de 90; 87 com comportamento bom; *skill* 20 de 30, 18 contidas | 90 `review.json` e `meta.json` | conferem |
| 430 de 450; 8, 7, 4 e 1; 11 falhas por Docker, 9 ruim e 2 com problemas | `build_exit_code`, `build.txt` e `tests-maven.log` | conferem |
| 59 e 16 nas reservas simultâneas; 0 de 225 na manutenção; 71 e 32 com nota ruim; 39 e 36, 36 e 39, 35 e 40 no banco; migrações | `behavior.md`, `database.md` e `review.json` | conferem |
| Tabelas 9 e 10; 4,87 e 3,60, 4,20 e 2,13, 0,33 e 0 | `oo-metrics.csv`, 449 execuções | conferem |
| Tabela 11 e US$ 380 | `meta.json` e `claude-output.jsonl` | conferem |
| 195 de 210; 30, 30, 29, 29, 27, 26, 24; kappa 0,53; 15 diferenças, todas um degrau mais brandas | 30 `review.json` do segundo verificador | conferem |
| 40 + 12 = 52; 58 revisões de calibração; 12 de 12 nos controles | texto e `summary-controls.md` | conferem |
| Tabelas 5, 6 e 13; US$ 346; 174 de 176; 6 projetos com LCOM de 37,2 ou mais no N0 | `resultados-V5-STRATEGY.md`, `metricas.csv` e `notas-V5-STRATEGY.csv` | conferem |
| Tabela 4, 28 vereditos; 10 e 7 pares em H3; Page 0,007 e abaixo de 0,001; perfis 7, 3 e 1; *tokens* 6 e 5; limites 36, 34 e 35 | `hipoteses-V5-STRATEGY.md` e OBJETIVO §4.1 | conferem |
| H27 com o subagente | `usage_by_model` em `meta.json`: Opus 4.6, 4.7 e 4.8 seguem com mais pares piores | veredito mantido |
| CBO vezes tipos: 22% a 46% e 3% a 27% | Tabelas 6 e 9 | conferem |
| 4 h e 10 h entre execuções; datas; *medium*; imagem e `harness_tree` únicos por nível; piloto de 1 em 5; 26 testes na base; 119 execuções da bancada; 122 arquivos no congelamento | `meta.json` das 540 execuções, base da manutenção, `construction-v3/runs` e `CONGELADO-V5.sha256` | conferem |

**5. Termos centrais.** *Harness*, configuração e orientações persistentes têm a relação explicada na Introdução. Os três sentidos de exagero e a estrutura desnecessária estão separados na Seção 6.2. A diferença de N1 e N2 entre os estudos está na Tabela 2.

**6. Proporção.** Sobre o texto numerado: Introdução 8%, Fundamentação 4%, Trabalhos relacionados 3%, Visão geral 8%, estudo principal 22%, extensão 29%, Discussão 15%, Limitações 8% e Conclusão 3%. O método é longo porque descreve dois estudos. Sem apontamento.

**7. Percurso de cada objetivo.** Ver a matriz. Nenhum objetivo some antes da Conclusão.

**8. Proveniência dos resultados.** Cada resultado tem etapa de origem: Semgrep, suíte, SonarQube, CK e transcrição (Seção 5.1); verificador, roteiro de comportamento, `pg_dump`, jdeps, JaCoCo, CK e registro do Claude Code (Seção 6.2); `mvn verify` (Seção 4); segundo verificador e calibração (Seção 6.2).

**9. Justificativa das escolhas.** A única discrepância encontrada entre o motivo declarado e o registro é a do apontamento 3. Os itens de D17 dependem de você.

## Apontamentos por critério

### Critério 1. Língua portuguesa e gramática

Sem apontamentos.

### Critério 2. Encadeamento lógico e coerência textual

Sem apontamentos.

### Critério 3. Terminologia e siglas

Sem apontamentos.

### Critério 4. Resumo e Abstract

Sem apontamentos.

### Critério 5. Introdução

Sem apontamentos.

### Critério 6. Coerência metodológica

**Apontamento 2. RECOMENDADA.**

- Problema: a *skill* escrita para a extensão responde quase com as mesmas palavras à frase que os controles negativos usam como isca, e o texto só registra esse efeito para o `CLAUDE.md`.
- Onde: Seção 6.2, "A frase é proposital, pois oferece ao agente um motivo para construir a estrutura que não foi solicitada". Seção 7, P2, "o `CLAUDE.md` afirma que uma mudança pontual pede uma mudança simples, o que quase descreve esses cenários". Seção 8, "O próprio `CLAUDE.md` orienta a não criar estrutura em mudanças pontuais". Seção 5.1, "um rascunho de *skill* própria repetia uma frase do enunciado, o que entregaria a resposta".
- Dado: `SKILL.md` de `design-patterns`, linha 22: "A vague "maybe one day" is not expected growth"; linha 94: "a hypothetical future that the requirements mention only as "maybe"". Cenário 04: "Talvez um dia a gente mande essa confirmação por WhatsApp". Cenário 10: "Talvez um dia a recepção peça o telefone". Cenário 08: "Talvez a gente troque a assinatura no Natal". Os cenários 02 e 06 usam "A gente já pensou em [...], mas por enquanto".
- Motivo: no N2, 18 das 20 execuções que abriram a *skill* ficaram contidas, e o leitor lê esse número como adesão ao `CLAUDE.md`. A *skill* nomeia a própria isca, o que é o mesmo tipo de proximidade que, na Seção 5.1, serviu de motivo para descartar uma *skill* própria. A contagem não muda; muda o que o N2 do controle permite concluir.
- Correção: acrescente a *skill* ao lado do `CLAUDE.md` na ressalva já existente da P2 ou da Seção 8, em uma oração.

**Apontamento 3. SUGESTÃO.**

- Problema: o texto dá a cota como motivo do esforço *medium* no estudo principal, e o registro traz outro motivo.
- Onde: Seção 5.1, terceiro parágrafo, "O esforço foi *medium*, escolhido para que todas as execuções coubessem na cota de uso".
- Dado: `construction-v3/DECISOES.md`, 24/09, "o Claude Code como vem": "`effort=medium`. *Por quê:* o efeito medido é o do harness somado ao Claude Code real". O motivo da cota aparece em 17/10 e vale para a extensão, onde a frase da Seção 6.2 está de acordo.
- Motivo: nenhum resultado depende disso, mas a frase atribui ao estudo principal um motivo que o registro não mostra.
- Correção: confirme qual foi o motivo no estudo principal e deixe o texto igual ao que você decidiu. Só você sabe se a cota também pesou.

### Critério 7. Apresentação da solução, resultados e conclusão

Cada resultado negativo faz parte do que o trabalho encontrou, e a conclusão só pode manter a cautela certa se o leitor souber quantas vezes ele ocorreu.

**Apontamento 1. OBRIGATÓRIA.**

- Problema: o texto relata como um caso único, na construção, a execução em que o agente abriu a *skill* e terminou sem implementar. Há um segundo caso na manutenção, que o texto não relata.
- Onde: Seção 6.3, P1, "Em uma rodada da construção do Strategy, o Haiku 4.5 abriu a *skill* com os planos de tarifa como argumento, anunciou que implementaria a solução e encerrou sem código". Seção 7, P4, "O risco reapareceu nas cinco rodadas, com a *skill* escrita para este trabalho, na execução do Haiku 4.5 que abriu a *skill* e encerrou sem código (Seção 6.3)". Seção 7, P6, "nesses dois modelos, o ganho em *pattern* veio acompanhado de alguma perda nessa dimensão".
- Dado: `TCC_V1/maintenance/runs/01-strategy/N2-haiku-4-5`. O `changes.patch` tem 0 bytes. O `meta.json` registra Read 13, Bash 4 e Skill 1, sem Write nem Edit. Na transcrição, o agente chama a *skill* com uma pergunta ("Devo criar uma abstração para cada plano, ou deixar o código acoplado ao enum RatePlan com switches?") e termina com "A resposta é **Strategy**", sem mudar o código. O `review.md` abre com "O agente não entregou nada", e todas as notas são ruim, com categoria omissão.
- Regra de cálculo: procurei, nas 540 execuções, as que têm `changes.patch` vazio ou nenhuma escrita de arquivo. A busca reproduz o caso que o texto já relata, na construção (`N2-haiku-4-5-r2`), e encontra só mais este. São 2 das 150 execuções do N2 com *pattern*: as duas do Haiku 4.5, as duas no Strategy, as duas com a *skill* chamada com um argumento.
- Motivo: a omissão muda a leitura em dois pontos. Primeiro, o risco aparece nas duas tarefas, e o texto o mostra só na construção, como execução única. Segundo, esse caso entra na queda do comportamento citada no Resumo e na Conclusão: é 1 dos 7 pontos de 61 para 54, 1 dos 5 de 59 para 54 e 1 dos 4 do Haiku 4.5 de 17 para 13. Ali a nota vem de não ter feito o pedido (15 das 21 conferências do cenário falham), e não de código que passou a errar, como a frase da P6 sugere. A direção da queda se mantém sem esse caso (61 para 55). As notas de arquitetura e banco dessa execução também são ruim por uma regra que a Seção 6.2 não descreve: o verificador recebe a instrução de dar "`ruim` onde não houver o que avaliar" (`evaluation/quality/agent/quality-reviewer.md`, linhas 113 e 114), embora outras execuções do mesmo cenário, que mantiveram essa base, recebam bom nessas dimensões.
- Correção: relate o caso da manutenção junto do caso da construção na Seção 6.3, ajuste a frase da P4 para os dois casos e diga, na P6 ou na Seção 6.3, que uma das quedas do comportamento vem dessa execução. Uma oração na Seção 6.2 pode dizer que a execução sem entrega recebe ruim nas dimensões. As tabelas não precisam mudar, e o total de 449 execuções com código continua certo, porque a base é código.

### Critério 8. Figuras, tabelas e gráficos

Sem apontamentos. Abri as dez figuras usadas no artigo. Os valores das Figuras 9 e 10 coincidem com as Tabelas 7 e 8, e os gráficos têm eixo e legenda. As figuras de código seguem D1.

### Critério 9. Inconsistências de conteúdo e trechos desconectados

Sem apontamentos além do que está no apontamento 1.

### Critério 10. Normas e referências

Sem apontamentos. As 43 referências da lista estão citadas, e todas as citações estão na lista.

- Conferidos no Crossref, com autor, ano, título principal e veículo iguais aos da lista: os 26 DOIs.
- Links que responderam com sucesso, sem baixar arquivo: Anthropic 2026a, 2026b e 2026c, Aniche, Grndlvl, JaCoCo, Semgrep, SonarSource e Poole e Mackworth.
- Sem DOI e não conferidas por automação nesta rodada: Brown, Vaswani, Yao, Fowler, Gamma, Martin, Meyer e Yourdon e Constantine. O Crossref só confirma metadados, de modo que o conteúdo citado não foi relido nesta rodada.
- Não há marcador pendente, parâmetro de rastreio nem citação direta de livro.

### Critério 11. Elementos obrigatórios da resolução

Sem apontamentos. A Tabela 3 traz tecnologia, versão e papel. O repositório abre, e os dados das execuções, as revisões do verificador e o pré-registro estão versionados.

## Matriz de rastreabilidade

| objetivo ou pergunta | onde é desenvolvido | etapa do método | resultado | resposta explícita | situação | ação pendente |
|---|---|---|---|---|---|---|
| Objetivo geral: avaliar o efeito de orientações persistentes na decisão de aplicar um *pattern*, na construção e na manutenção | Seções 5 e 6 | Seções 5.1 e 6.2 | Tabelas 5, 7 e 8 | Seção 9, dois primeiros parágrafos | atendido | nenhuma |
| P1. Uso do *pattern* esperado | Seções 5.3 e 6.3 | Semgrep e conferência humana (5.1); categoria do verificador (6.2) | Tabelas 4, 5 e 8; Figuras 9 e 10 | Seção 7, P1; Seção 9 | atendido | nenhuma |
| P2. Exagero, corretude e qualidade | Seções 5.3 e 6.3 | Semgrep, suíte e SonarQube (5.1); verificador (6.2) | Tabelas 4, 5, 7 e 14 | Seção 7, P2; Seção 9 | atendido | nenhuma |
| Controle negativo na manutenção (com a P2) | Seções 6.2 e 6.3 | Dois últimos parágrafos da Seção 6.2 | Tabela 12 | Seção 7, P2, segundo parágrafo; Seção 9 | atendido | ressalva sobre a *skill* (apontamento 2) |
| P3. Variação entre modelos | Seções 5.3 e 6.3 | H5, H19 e H20 (5.1); três modelos (6.2) | Tabelas 13 e 14 | Seção 7, P3; Seção 9 | atendido | nenhuma |
| P4. Componentes e custo | Seções 5.3 e 6.3 | Níveis e custo (5.1); níveis e registro de custo (6.2) | Tabelas 4, 5, 8 e 11 | Seção 7, P4; Seção 9 | parcial | relatar o segundo caso sem entrega (apontamento 1) |
| P5. Tipo de tarefa | Seção 6.3 | Duas tarefas (6.2) | Tabelas 7 e 8 | Seção 7, P5; Seção 9 | atendido | nenhuma |
| P6. Demais dimensões | Seção 6.3 | Notas do verificador (6.2) | Tabelas 7 e 14 | Seção 7, P6; Seção 9 | parcial | dizer que uma das quedas do comportamento vem da execução sem entrega (apontamento 1) |
| P7. Estabilidade | Seções 5.3 e 6.3 | Perfis (5.1); repetição da primeira rodada (6.2) | Seção 5.3; Tabela 16 | Seção 7, P7; Seção 9 | atendido | nenhuma |
| P8. Estrutura orientada a objetos | Seções 5.3 e 6.3 | CK (2, 5.1 e 6.2) | Tabelas 4, 6, 9 e 10 | Seção 7, P8; Seção 9 | atendido | nenhuma |

Nenhum resultado ficou sem objetivo.

## Checklist do guia (seção 10)

**Elementos obrigatórios da resolução**

- Introdução descreve problema e contexto: atendido.
- Metodologia descreve o processo e os envolvidos: atendido.
- Tecnologias com versão: atendido.
- Solução com artefatos e métricas: atendido.
- Link do repositório público: atendido.
- Processo e resultados da avaliação: atendido.

**Critério 1**

- Ortografia, acentuação, concordância e regência: atendido.
- Tempos verbais consistentes: atendido.
- Sem frases longas que encadeiam vários resultados: parcial. Restam frases longas nas Seções 5.1, 6.1 e 6.2; não encontrei nenhuma que leve a erro de leitura, e por isso não há apontamento.
- Sem declarações de importância sem fato: atendido.
- Sem gerúndio de comentário: atendido.
- "É" e "tem" em vez de "atua como": atendido.
- Afirmações de terceiros com fonte: atendido.
- Sem contrastes montados e fórmulas: atendido.
- Títulos com maiúscula só na primeira palavra e nos nomes próprios: atendido.
- Sem vírgula antes do "e" final e sem decalques: atendido.

**Critério 2**

- Cada seção prepara a seguinte: atendido.
- Termos definidos antes do uso: atendido.
- Objetivos retomados com o mesmo nome: atendido.
- Tamanho das seções: atendido.

**Critério 3**

- Um nome por conceito: atendido (o nome dos níveis está em D15).
- Nenhum nome para duas coisas: atendido.
- Siglas pouco conhecidas expandidas: atendido.
- Estrangeirismos em itálico: atendido no texto (as figuras estão em D16).

**Critério 4**

- Problema, objetivo, o que foi feito e resultado com número: atendido.
- Resumo se entende sozinho: atendido.
- Abstract é tradução fiel: atendido.

**Critério 5**

- Contexto, problema e relevância com fatos ou fontes: atendido.
- Objetivos claros, numerados e verificáveis: atendido.
- Objetivos correspondem ao que foi feito: atendido.
- Contexto institucional: atendido.
- Parágrafo de organização corresponde às seções: atendido.

**Critério 6**

- Cada etapa diz o que, como, com quê e quem: atendido.
- Sequência e dependências: atendido.
- Cada escolha tem motivo: parcial (apontamento 3; D17 depende de você).
- Todo resultado tem etapa de origem: atendido.
- Etapas e atividades batem: atendido.
- Versões informadas: atendido.

**Critério 7**

- Solução com métricas e artefatos: atendido.
- Resultados concretos e organizados pelas perguntas: parcial (apontamento 1).
- Resposta explícita para cada pergunta: atendido.
- Nenhum resultado desligado dos objetivos: atendido.
- Conclusão com resultados e as mesmas ressalvas: atendido.
- Limitações e trabalhos futuros específicos: parcial (apontamento 2).

**Critério 8**

- Número, título e fonte: atendido.
- Apresentação no texto com descrição: atendido.
- Gráficos com eixos, unidade e legenda: atendido.
- Código em texto, nenhum em imagem: decidido por você (D1), fora da contagem.
- Nenhuma imagem de código com fundo escuro: atendido.
- Mesma informação não repetida em tabela e figura: decidido por você (D3), fora da contagem.

**Critério 9**

- Mesmo número com o mesmo valor: atendido.
- Resumo, Introdução e Conclusão com a mesma cautela: atendido.
- Sem contradição entre seções: atendido. A diferença do apontamento 1 é entre o texto e os dados.
- Sem trechos genéricos: atendido.
- Sem itens citados uma vez e nunca mais: atendido.
- Estilo uniforme: atendido.

**Critério 10**

- Citações e lista batem: atendido.
- Cada DOI leva ao trabalho citado: atendido, pelos metadados.
- Sem marcador pendente: atendido.
- Citações diretas com página e links com data de acesso: atendido.
- Numeração e elementos do modelo: atendido.

**Critério 11**

- Tecnologias em tabela, com versão e papel: atendido.
- Link abre para qualquer pessoa: atendido.

**Revisão do texto inteiro e matriz**

- Leitura do texto inteiro: atendido nesta autoavaliação.
- Matriz sem célula "não encontrado": atendido.

**Uso de ferramentas de IA**

- Declaração de uso depois da Conclusão: decidido por você (D4), a escrever por último.
- IA como instrumento descrita na metodologia: atendido.
- Conferi todo conteúdo e referência sugeridos: depende de você nos itens D19 e D20.
