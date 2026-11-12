Autoavaliação feita com ferramenta de IA. A avaliação oficial é do orientador.

# Autoavaliação antes da entrega: artigo, versão 140 (rodada 16)

Esta autoavaliação segue os critérios de revisão do professor Rodrigo Rebouças de Almeida (DCX/UFPB). Eles são orientações dele para os seus orientandos, e não critérios oficiais da disciplina de TCC, do curso ou da UFPB; quando houver diferença, valem a resolução de TCC e as normas oficiais. Se o seu orientador adotar outros critérios, valem os dele. O uso desta skill é uso de ferramenta de IA e entra na declaração de uso do TCC (seção 8 do guia).

Arquivo revisado: `artigo-v140.md`, versão de 11/11 já com as três correções da rodada 15 (H17 e H18 na Discussão da P4, regra da execução sem entrega na Seção 6.2 e métodos abstratos só na extensão, na Seção 2). Li o texto inteiro, as 16 tabelas e as 10 figuras em PNG. Não alterei nenhum arquivo do artigo nem dos dados e não usei a rede. Conferi os números somando as tabelas entre si e recalculando a partir dos arquivos em `J:\TCC`; as referências foram conferidas pelos metadados do Crossref já gravados no projeto (`scratchpad/refcheck/crossref.json`). Nenhum arquivo lido trazia instruções dirigidas a mim.

## 1. Pontos já decididos (não contam como pendência)

**Protegidos por decisão sua.** Estes pontos ficam fora da contagem, mesmo quando o roteiro trataria de outro jeito.

- As Figuras 2, 5, 6, 7 e 8 mostram código como imagem, com fundo branco. O roteiro pede código em texto; a escolha é sua e está registrada. Conferi que nenhuma tem fundo escuro.
- Uma figura por *pattern* na Seção 6.2.
- As Figuras 9 e 10 ficam junto das Tabelas 7 e 8, mesmo repetindo os dados. Conferi que os valores das duas figuras são os das tabelas.
- A declaração de uso de IA será escrita por você no fim; o apoio à redação do artigo fica só nessa seção, e não na Tabela 1.
- A coluna "como foi conferido" da Tabela 1 foi confirmada por você, e o Fable tem linha própria, aprovada em 11/11.
- Título mantido e nome da H8 conforme o pré-registro. Comentário sobre o título, fora da contagem: "sua extensão à manutenção" descreve só uma das duas tarefas da extensão, que também tem a construção com cinco *patterns*; se o título ainda estiver em discussão, vale considerar esse ponto.
- Motivos dados por você: Strategy difundido e adequado ao domínio; cinco *patterns* para as três categorias do GoF; N3 fora da extensão; segunda rodada do controle decidida antes do resultado da primeira; diferença de horas entre execuções pela janela de 5 horas da cota; três modelos da extensão (pior, mediano e melhor no estudo anterior); Opus 5.5 como verificador (capaz e com custo viável); esforço *medium* pela cota.
- Referências só de revistas e eventos respeitados, sem *preprints*; definição de IA por Poole e Mackworth.
- Registro acadêmico em prosa, sem listas no corpo e sem rótulos em negrito; P1 a P8 em parágrafo. Este relatório não pede listas no corpo do texto.
- Resumo de 150 a 250 palavras (contei 249) e Abstract como tradução fiel (contei 250).
- A Introdução define cada termo para o leitor.
- *Harness* no sentido de Galster et al., com o aviso da diferença para o pré-registro.
- Só modelos da Anthropic, declarado como limitação.

**Já verificado: não procede.**

- Tabela 11, *tokens* de saída do N2 da construção (38,9 mil): todas as células usam a soma dos segmentos.
- Conferências de 11/11 já aplicadas: H14 conta casos de borda aprovados; a calibração tem 42 revisões (26 + 10 + 6); 3 das 17 versões defeituosas vêm de erro observado; o efeito possivelmente nulo do esforço no Haiku 4.5 tem registro no plano do projeto.

**Pendências que dependem de você (já registradas, fora da contagem).**

- Sugestões não aplicadas de rodadas anteriores: tabela comparativa dos trabalhos relacionados (Seção 3), um nome só para os níveis, "et al." nas referências.
- Citar Page (1963) e Cohen (1960) depende de você ler e aprovar; itálico de *pattern* dentro das figuras (os títulos das Figuras 9 e 10 e os eixos ainda trazem "pattern" sem itálico).
- O motivo de duas análises do pré-registro (OBJETIVO §4.5 e §4.6) não terem sido calculadas; o texto já registra o fato na Seção 8.
- O conteúdo de Yao et al. (2023), Kim (2025) e Kim (2026) precisa ser aberto por você.
- Avisos de rodadas anteriores: o pedido de e-mail da API do OpenAlex, que não foi atendido, e os PDFs abertos baixados e apagados na rodada 10.

## 2. Pontos a tratar

Nesta rodada não encontrei nenhum ponto novo para tratar: nenhum obrigatório, nenhum recomendado e nenhuma sugestão. Todos os números que conferi batem com as tabelas e com os dados brutos, não achei contradição entre seções nem afirmação sem apoio, e as três correções da rodada 15 entraram no texto sem criar inconsistência nova. Os itens da seção 1 continuam dependendo de você.

## 3. Leitura macro

**1. Mapa das seções.** A Introdução apresenta IA, LLM, agente, *harness*, os três mecanismos de configuração, *patterns*, exagero, construção e manutenção, a relevância, o problema, o objetivo geral, as oito perguntas, as contribuições e a organização do texto. A Seção 2 dá os conceitos e as métricas usados depois (encapsulamento do que varia, polimorfismo, aberto/fechado, acoplamento e coesão, CK, complexidade cognitiva e duplicação). A Seção 3 delimita a lacuna. A Seção 4 mostra as fases (Figura 3), o desenho (Figura 4), o repositório, os envolvidos, a IA usada (Tabela 1), os níveis (Tabela 2), as tecnologias (Tabela 3) e o isolamento das execuções. A Seção 5 traz o método, a proteção contra decisões posteriores e os resultados do estudo principal; a Seção 6, a motivação da extensão, o método, o verificador, os controles e os resultados; a Seção 7 responde P1 a P8; a Seção 8 traz as limitações; a Seção 9, a conclusão; e o Apêndice A, os dados por modelo. O parágrafo de organização (fim da Introdução) corresponde a essas seções, na mesma ordem.

**2. Linha do argumento.** Problema, objetivo, perguntas, método, resultados e respostas formam uma sequência. Os termos aparecem definidos antes do uso: exagero na Introdução; teto, saldo, ponto do clube e ponto do seguro na Seção 5.1; regras de decisão e "não apoiada" na Seção 5.1; categorias do *pattern*, *pattern* certo, estrutura desnecessária, contenção e exagero dos controles na Seção 6.2.

**3. Resumo, Introdução e Conclusão.** Os três falam da mesma coisa, com os mesmos números (67% para 93%; 220, 450 e 90 execuções; 19 para 58 e 41 para 60; 61 para 54; 88 de 90; 4 para 9) e a mesma cautela (piora do Haiku 4.5 na corretude, exceção do Strategy na construção, notas fora da calibração, controle no teto, descrição sem teste de significância na extensão). A Conclusão não afirma mais do que a Discussão.

**4. Números repetidos.** Refiz as contas a partir das tabelas e dos dados:

- Tabela 7 é a soma da Tabela 14 em todas as 36 células e confere com `TCC_V1/evaluation/quality/results/summary.md`. A linha *pattern* é a soma da Tabela 8 (41, 53, 60 e 19, 33, 58), e as Figuras 9 e 10 mostram os mesmos valores.
- Categorias do *pattern* por tarefa e nível, derivadas do texto da Seção 6.3 e da Tabela 10: construção 32/9/7/27, 39/14/13/9 e 55/5/9/6 (acerto, alternativa, malfeito, omissão); manutenção 15/4/26/30, 28/5/20/22 e 56/2/14/3. As somas fecham em 75 por nível, os totais batem com a Tabela 10 (126, 28, 29, 41 mais a execução sem código; 99, 11, 60, 55) e com os números 45, 53, 72 e 48, 66, 69 do texto.
- Strategy na construção, contado no `summary.md`: 9, 7 e 9 certos; malfeito 0, 7 e 5; omissão 6, 1 e 1; sem o Sonnet 5.5, 4, 2 e 4 em 10. Confere.
- Médias ponderadas da Tabela 10 por categoria reproduzem as médias da Tabela 9 em tipos, interfaces, métodos abstratos, WMC e CBO, nas duas tarefas (por exemplo, CBO da construção 3,377 e 3,376).
- Conta aproximada do CBO vezes os tipos: +22% a +46% no estudo principal e +3% a +27% na extensão. Confere.
- Tabela 13 reproduz as Tabelas 4 e 5: acertos no ponto do clube 37, 51, 51 e 54 em 55 (67%, 93%, 93% e 98%); saldos de H1 somam +14, e os de H3, +3. Os saldos, os perfis distintos (7 menos, 3 iguais, 1 mais), o sinal dos *tokens* (6 e 5), H23 (2 pares), H26 (3 pares, Haiku 4.5 e Sonnet 4.5) e o teste de Page (p = 0,007 e p < 0,001) conferem com `construction-v5/analysis/hipoteses-V5-STRATEGY.md`; H17 e H18 têm 30 e 31 pares do lado maior.
- Conferência humana: 174 de 176 respostas, com as duas discordâncias na forma do ponto do seguro (`conferencia-V5-STRATEGY.md`).
- Tabela 16 soma 89, 96, 109 por modelo e 96, 103, 95 por nível, total de 294 em 360 (82%) e 54 combinações estáveis. Tabela 12 soma 30, 30 e 28.
- Custos: 55 × (1,32 + 1,52 + 1,47 + 1,98) = US$ 346 no estudo principal; 75 × soma da linha de custo da Tabela 11 = US$ 380 na extensão.
- Contagens recalculadas nos `meta.json` e nas medidas: falhas do `mvn verify` 8, 7 e 4 na construção e 1 no N1 da manutenção (430 de 450); 11 delas com "Could not find a valid Docker environment", todas do Sonnet 4.5, com nota de testes ruim em 9 e com problemas em 2; *skill* aberta 19, 25 e 25 por modelo no N2 das duas tarefas e 0, 10 e 10 nos controles; reservas simultâneas: 59 execuções da construção com as duas reservas criadas, 16 sem chegar ao teste e nenhuma na manutenção; controles com 2 exageros (Sonnet 5.5, temporada, N2) e comportamento não bom em 3 execuções do Haiku 4.5 (crianças e assinatura); diferença máxima de horário de 4,0 horas entre níveis do mesmo modelo e 9,9 horas entre modelos do mesmo cenário e rodada; datas das rodadas como na Seção 6.2; razão de custo do segundo verificador de 3,04 nas 30 revisões; esforço *medium* nas 220 execuções do estudo principal e Claude Code 2.1.288 no `meta.json`.
- As duas execuções que encerraram sem código (construção e manutenção do Strategy, Haiku 4.5, N2) estão nos `meta.json` e nas transcrições como o texto descreve.
- Calibração: o `cases.json` tem 10 soluções de referência revisadas nas duas tarefas, 6 exemplos degradados e 6 soluções dos controles (contidas de 02, 04, 08 e 10; com exagero de 02 e 10; nenhuma do 06, crianças), como a Seção 6.2 diz.

**5. Termos centrais.** "Configuração", "orientações persistentes", "nível", "teto", "saldo", "ponto do clube", "ponto do seguro", "exagero", "*pattern* certo", "categoria acerto", "estrutura desnecessária" e "contenção" mantêm nome e sentido. Onde o mesmo nome poderia confundir (exagero no estudo principal e nos controles; N1 e N2 nos dois estudos; certo no Semgrep e no verificador), o texto avisa (Tabela 2, Seção 6.2 e Discussão da P1).

**6. Proporção.** Método e resultados ocupam a maior parte do texto; Fundamentação e Trabalhos relacionados são curtos e usados depois.

**7. Percurso de cada objetivo.** O objetivo geral, as oito perguntas e o controle negativo da manutenção têm método, resultado e resposta explícita (matriz na seção 6 deste relatório).

**8. Proveniência dos resultados.** Cada resultado tem etapa de origem: suíte, Semgrep, conferência humana, SonarQube e CK (Seção 5.1); verificador, roteiro de comportamento, JaCoCo, jdeps e `pg_dump` (Seção 6.2); controles (Seção 6.2); custo, *tokens*, tempo e turnos pelo registro do Claude Code (Seções 4, 5.1 e 6.2); abertura da *skill* e chamada do revisor pela transcrição (Seção 5.1); estabilidade (Seções 5.1 e 6.2); segundo verificador (Seção 6.2); falhas por falta de Docker (Seção 6.2).

**9. Justificativa das escolhas.** As escolhas que mudam a leitura dos resultados têm motivo declarado: modelos dos dois estudos, número de rodadas, limites das regras de decisão, critérios das notas, exclusão do N3 na extensão, ordem das repetições, contêineres sem Docker, segunda rodada do controle, verificador ciente do controle, testes não pedidos na construção, migrações exigidas na construção, posição da instrução da *skill* na manutenção.

## 4. Apontamentos por critério

**Critério 1. Língua portuguesa e redação.** Sem apontamentos. Não encontrei erro de ortografia, concordância ou regência, marcador pendente nem os padrões de escrita do roteiro.

**Critério 2. Encadeamento.** Sem apontamentos. As perguntas P1 a P8 são retomadas com o mesmo código nas Seções 5.3, 6.3, 7 e 9, e cada seção prepara a seguinte.

**Critério 3. Terminologia e siglas.** Sem apontamentos.

**Critério 4. Resumo e Abstract.** Sem apontamentos. Os números do Resumo conferem com as tabelas e com os dados, e o Abstract traduz o Resumo frase a frase.

**Critério 5. Introdução.** Sem apontamentos. O texto não menciona o AYTY, então o ponto do roteiro sobre o laboratório não se aplica.

**Critério 6. Coerência metodológica.** Sem apontamentos. As descrições do `CLAUDE.md` e da *skill* nas Seções 5.1, 6.1, 6.2 e 8 conferem com os arquivos em `construction-v5/experiment/harnesses` e `TCC_V1/*/experiment/harnesses` (por exemplo, a instrução de usar a *skill* no último item da construção e no primeiro da manutenção, e o "maybe one day" da *skill*). As regras de decisão da Seção 5.1 conferem com o OBJETIVO §4.1 (36 de 55, 5 pares não empatados e 6,3%).

**Critério 7. Solução, resultados e conclusão.** Sem apontamentos. Cada pergunta tem resposta explícita na Seção 7, e a Conclusão repete os números e as ressalvas da Discussão.

**Critério 8. Figuras, tabelas e código.** Sem apontamentos fora dos pontos protegidos. As 10 figuras e as 16 tabelas têm número, título e fonte e são apresentadas no texto com o que mostram. As Figuras 9 e 10 têm eixo com rótulo, escala e legenda.

**Critério 9. Consistência de conteúdo.** Sem apontamentos.

**Critério 10. Normas e referências.** Sem apontamentos. As 43 referências da lista são todas citadas, e todas as citações do texto estão na lista. Pelos metadados do Crossref gravados no projeto, os 26 DOIs levam ao trabalho citado, com o mesmo primeiro autor, título, veículo e páginas ou número de artigo; isso inclui Vokáč et al. (2004), que a rodada 15 não tinha conseguido resolver. Não puderam ser conferidas por DOI as referências sem DOI (Brown et al. 2020, Vaswani et al. 2017, Yao et al. 2023, os livros e as páginas de ferramentas), e, sem rede, não conferi números tirados do conteúdo dos artigos (como os 806 repositórios de He et al. e os 2.853 de Galster et al.). Os links têm data de acesso e não têm parâmetro de rastreio, e não há citação direta de livro.

**Critério 11. Tecnologias e repositório.** Sem apontamentos. A Tabela 3 traz nome, versão e papel de cada tecnologia, e o link do repositório (https://github.com/FLucasF/TCC) está no texto. Sem rede, não testei se o link abre nesta rodada.

**Contagem.** OBRIGATÓRIA: 0. RECOMENDADA: 0. SUGESTÃO: 0.

## 5. Matriz de rastreabilidade

| Objetivo / pergunta | Onde é desenvolvido | Etapa do método que gera os dados | Resultado (seção, tabela) | Resposta explícita (onde) | Situação | Ação pendente |
|---|---|---|---|---|---|---|
| Objetivo geral: efeito de orientações persistentes sobre a decisão de aplicar um *pattern*, na construção e na manutenção | Seções 5 e 6 | Os dois estudos (5.1 e 6.2) | 5.3 e 6.3 | Seção 9, primeiro e segundo parágrafos | atendido | nenhuma |
| P1. Uso do *pattern* esperado | 5.3 e 6.3 | Semgrep e conferência humana (5.1); categoria do verificador (6.2) | Tabelas 5, 7, 8 e 13; Figuras 9 e 10 | Seção 7, P1; Seção 9 | atendido | nenhuma |
| P2. Exagero, corretude e qualidade | 5.3 e 6.3 | Semgrep no ponto do seguro, suíte, SonarQube (5.1); estrutura desnecessária (6.2) | Tabelas 4, 5, 7 e 14 | Seção 7, P2; Seção 9 | atendido | nenhuma |
| Controle negativo da manutenção (com a P2) | 6.2 e 6.3 | Cenários de controle e verificador ciente do controle (6.2) | Tabela 12 | Seção 7, parágrafo do controle; Seção 9 | atendido | nenhuma |
| P3. Variação entre modelos | 5.3 e 6.3 | Pares por modelo (5.1); resultados por modelo (6.2) | Tabelas 13 e 14 | Seção 7, P3; Seção 9 | atendido | nenhuma |
| P4. Contribuição de cada componente e custo | 5.3 e 6.3 | Comparações N2 × N1 e N3 × N2, transcrição e registro de custo (5.1 e 6.2) | Tabelas 4, 5 e 11 | Seção 7, P4; Seção 9 | atendido | nenhuma |
| P5. Tipo de tarefa | 6.3 | Duas tarefas com os mesmos níveis e modelos (6.2) | Tabelas 7 e 8; Figura 10 | Seção 7, P5; Seção 9 | atendido | nenhuma |
| P6. Demais dimensões de qualidade | 6.3 | Verificador com uma *skill* por dimensão (6.2) | Tabelas 7 e 14 | Seção 7, P6; Seção 9 | atendido | nenhuma |
| P7. Estabilidade | 5.3 e 6.3 | Perfis do Semgrep por rodada (5.1); repetição da primeira rodada (6.2) | Seção 5.3; Tabela 16 | Seção 7, P7; Seção 9 | atendido | nenhuma |
| P8. Estrutura orientada a objetos | 5.3 e 6.3 | CK nos dois estudos (Seções 2, 5.1 e 6.2) | Tabelas 6, 9 e 10 | Seção 7, P8; Seção 9 | atendido | nenhuma |

## 6. Checklist da seção 10 do guia

**Elementos obrigatórios da resolução**

| item | situação |
|---|---|
| A Introdução descreve o problema e o contexto | atendido |
| A metodologia descreve o processo e identifica os envolvidos | atendido (Seção 4 e Tabela 1) |
| Há uma descrição das tecnologias usadas, com versão | atendido (Tabela 3) |
| A solução é apresentada com artefatos e métricas | atendido |
| O texto traz o link do repositório público | atendido |
| Se houve avaliação, o processo e os resultados estão descritos | atendido |

**Critério 1. Língua portuguesa e redação**

| item | situação |
|---|---|
| Sem erros de ortografia, acentuação, concordância ou regência | atendido |
| Tempos verbais consistentes | atendido |
| Sem frases longas que encadeiam vários resultados | atendido |
| Sem declarações de importância sem fato | atendido |
| Sem gerúndio de comentário no fim da frase | atendido |
| "É" e "tem" em vez de "atua como", "serve como", "conta com" | atendido |
| Toda afirmação atribuída a terceiros cita a fonte | atendido |
| Sem contrastes montados nem fórmulas de enchimento | atendido |
| Títulos com maiúscula só na primeira palavra e nos nomes próprios | atendido |
| Sem vírgula antes do "e" final nem decalques do inglês | atendido |

**Critério 2. Encadeamento**

| item | situação |
|---|---|
| Cada seção prepara a seguinte | atendido |
| Todo termo técnico é definido antes de ser usado | atendido |
| Cada pergunta chega à Conclusão com o mesmo nome | atendido |
| O tamanho de cada seção corresponde ao seu papel | atendido |

**Critério 3. Terminologia e siglas**

| item | situação |
|---|---|
| Cada conceito tem um nome só | atendido |
| Nenhum nome designa duas coisas diferentes | atendido (o texto avisa onde "exagero", "certo", N1 e N2 mudam de sentido) |
| Siglas pouco conhecidas expandidas | atendido |
| Estrangeirismos em itálico | parcial (no texto, atendido; dentro das figuras, pendência sua já registrada) |

**Critério 4. Resumo e Abstract**

| item | situação |
|---|---|
| Problema, objetivo, o que foi feito e resultado com número | atendido |
| O resumo se entende sozinho | atendido |
| O Abstract é a tradução fiel do resumo | atendido |

**Critério 5. Introdução**

| item | situação |
|---|---|
| Contexto, problema e relevância com fatos ou fontes | atendido |
| Objetivo geral e perguntas claros, numerados e verificáveis | atendido (P1 a P8) |
| Os objetivos correspondem ao que o trabalho fez | atendido |
| Contexto institucional explicado | não se aplica (sem laboratório ou cliente; o vínculo com o curso e o orientador está declarado) |
| O parágrafo de organização corresponde às seções | atendido |

**Critério 6. Metodologia**

| item | situação |
|---|---|
| Cada etapa diz o que, como, com quais ferramentas e quem | atendido |
| Sequência e dependências das etapas claras | atendido (Figura 3 e Seções 5.2, 6.1 e 6.2) |
| Cada escolha do método tem motivo declarado | atendido |
| Todo resultado tem etapa de origem | atendido |
| Toda etapa aparece nos resultados, e toda atividade está na metodologia | atendido |
| Versões das ferramentas informadas | atendido (Tabela 3; a versão do Docker das execuções anteriores a 09/11 não foi registrada, e o texto diz isso) |

**Critério 7. Solução, resultados e conclusão**

| item | situação |
|---|---|
| A solução é descrita com métricas e artefatos | atendido |
| Resultados concretos e organizados pelas perguntas | atendido |
| Cada pergunta tem resposta explícita | atendido |
| Não há resultado desligado dos objetivos | atendido |
| A conclusão traz os resultados e mantém as ressalvas | atendido |
| Limitações e trabalhos futuros específicos | atendido |

**Critério 8. Figuras, tabelas e código**

| item | situação |
|---|---|
| Toda figura e tabela tem número, título e fonte | atendido |
| Toda figura e tabela é apresentada no texto, com descrição | atendido |
| Todo gráfico tem eixos com rótulo e unidade, e legenda | atendido (Figuras 9 e 10) |
| Todo trecho de código está em texto, nenhum em imagem | decidido por você (Figuras 2, 5, 6, 7 e 8 em imagem; ponto protegido) |
| Nenhuma imagem tem fundo preto ou escuro com código | atendido (fundo branco) |
| A mesma informação não aparece repetida em tabela e figura | decidido por você (Figuras 9 e 10 junto das Tabelas 7 e 8; ponto protegido) |

**Critério 9. Consistência de conteúdo**

| item | situação |
|---|---|
| O mesmo número tem o mesmo valor em todas as partes | atendido |
| Resumo, Introdução e Conclusão com a mesma cautela | atendido |
| Não há contradição entre seções | atendido |
| Não há trechos genéricos sem ligação com o trabalho | atendido |
| Não há itens citados uma vez e nunca mais | atendido |
| Estilo uniforme | atendido |

**Critério 10. Normas e referências**

| item | situação |
|---|---|
| Toda citação está na lista, e toda referência é citada | atendido (43 de 43) |
| Cada DOI leva ao trabalho citado | atendido pelos metadados do Crossref já gravados (26 de 26); abra no navegador antes de entregar, já que nesta rodada não houve rede |
| Não há marcador pendente | atendido |
| Citações diretas de livro com página e links com data de acesso | atendido |
| Numeração das seções consistente e elementos do modelo | atendido |

**Critério 11. Tecnologias e repositório**

| item | situação |
|---|---|
| Tecnologias em tabela, com versão e papel | atendido (Tabela 3) |
| Link do repositório no texto e aberto | atendido no texto; o acesso não foi testado nesta rodada |

**Revisão do texto inteiro e matriz**

| item | situação |
|---|---|
| Leitura do texto inteiro | atendido |
| Matriz completa, sem célula "não encontrado" | atendido |

**Uso de ferramentas de IA**

| item | situação |
|---|---|
| A declaração de uso está depois da Conclusão | pendente (você escreve por último; ponto decidido) |
| IA usada como instrumento está descrita na metodologia | atendido (Seção 4, Tabela 1 e Seção 6.2) |
| Todo conteúdo, código e referência sugeridos por ferramenta foram conferidos | parcial (Yao et al. 2023, Kim 2025 e Kim 2026 ainda dependem da sua leitura; os ajustes do Fable nos controles foram aplicados sem sua revisão, como o texto declara) |
