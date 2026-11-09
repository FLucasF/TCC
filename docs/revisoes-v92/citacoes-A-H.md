# Conferência das citações A-H do artigo v92

Artigo conferido: `J:\TCC\docs\versoes\artigo-v92.md` (não alterado). Referências cobertas: Ahmed 2025, Ampatzoglou 2026, Aniche 2015, Anthropic 2026, Arabat 2026, Arcuri 2014, Basili 1996, Boehm 1976, Brown 2020, Campbell 2018, Chatlatanagulchai 2026, Chidamber 1994, Ernst 2023, Fowler 2018, Galster 2026, Gamma 1995, Grndlvl 2026, He 2026, Hou 2024.

## Resultado

| classificação | ocorrências |
|---|---|
| FIEL | 25 |
| PARCIAL | 4 |
| SEM SUPORTE | 0 |
| FONTE AUSENTE | 7 |
| **total** | **36** |

Cada linha é uma ocorrência no corpo do artigo; a ocorrência de Ampatzoglou na Seção 3 foi dividida porque suas afirmações têm classificações diferentes. A coluna "onde" traz a página ou a seção e um resumo meu, sem transcrição da fonte. A menção à Anthropic na Seção 7 ("um único fornecedor, a Anthropic") nomeia a empresa e não é citação.

## Tabela de ocorrências

| nº | seção (linha) | trecho do artigo | referência | classificação | onde está na fonte |
|---|---|---|---|---|---|
| 1 | 1 (l. 24) | LLMs treinados com muito texto, geram texto a partir de *prompt*, inclusive código | Hou 2024 | FIEL | Seção 1: aprendem de grandes corpora e geram texto e código; seção 5.1: *prompts* como entrada |
| 2 | 1 (l. 24) | baseados na arquitetura Transformer (agrupada com Vaswani, fora deste lote) | Brown 2020 | FIEL | Seção 2.1, p. 8: mesma arquitetura do GPT-2, camadas do Transformer; introdução: linha dos modelos Transformer |
| 3 | 1 (l. 24-25) | tarefas novas especificadas só por texto, sem ajuste de parâmetros | Brown 2020 | FIEL | Resumo, p. 1: GPT-3 aplicado sem atualização de gradiente ou *fine-tuning*, tarefas dadas por texto. Observação: o artigo generaliza do GPT-3 para "esses modelos", sem distorcer |
| 4 | 1 (l. 32-34) | Claude Code recebe pedido em linguagem natural e altera o projeto com autonomia | Anthropic 2026 | FONTE AUSENTE | Páginas da documentação não salvas; `fundamentacao-teorica.md` traz só resumo |
| 5 | 1 (l. 36-37) | camadas em volta do modelo que conduzem o ciclo, montam a entrada e oferecem ferramentas: *harness* | Galster 2026 | FIEL | P. 11 e nota 2 da versão ACM: camadas que conduzem o laço, montam o contexto e expõem as ferramentas |
| 6 | 1 (l. 39-41) | `CLAUDE.md` carregado a cada sessão, tratado como contexto, não como regra imposta | Anthropic 2026 | FONTE AUSENTE | Página *How Claude remembers your project* não salva |
| 7 | 1 (l. 41-42) | *skill*: descrição sempre disponível, conteúdo carregado só quando usada | Anthropic 2026 | FONTE AUSENTE | Página *Extend Claude with skills* não salva |
| 8 | 1 (l. 42-44) | subagente: agente separado, contexto próprio, recebe tarefa delegada | Anthropic 2026 | FONTE AUSENTE | Página *Create custom subagents* não salva. Galster (p. 11) confirma o contexto próprio, mas não é a fonte citada |
| 9 | 1 (l. 45-47) | em repositórios públicos, contexto predomina; *skills* 5,5% e subagentes 4,6% | Galster 2026 | PARCIAL | Fig. 1, p. 12: 158 e 131 de 2.853 repositórios; p. 16: 90,6% com contexto. Os percentuais estão certos, mas o denominador é o de repositórios que já usam ferramentas de agente |
| 10 | 1 (l. 56-57) | solução conhecida e nomeada para problema recorrente de projeto em OO | Gamma 1995 | FIEL | P. 3 e glossário, p. 360: nomeia e explica um desenho geral para problema recorrente em OO |
| 11 | 1 (l. 59-61) | não aplicar indiscriminadamente; a flexibilidade custa indireção | Gamma 1995 | FIEL | P. 31, fim da seção 1.8 |
| 12 | 1 (l. 61-62) | "generalidade especulativa": flexibilidade para necessidades que talvez não surjam | Fowler 2018 | FIEL | Cap. 3, *Speculative Generality*. O nome é atribuído por Fowler a Brian Foote; a frase diz "descreve", o que está correto |
| 13 | 1 (l. 68-69) | manutenção: modifica software existente preservando funções principais | Boehm 1976 | FIEL | Seção VII.A, p. 1235: definição dada pelo autor |
| 14 | 1 (l. 72-74) | 806 repositórios com Cursor; velocidade por cerca de dois meses; alertas e complexidade cognitiva persistem | He 2026 | FIEL | Tabela 1, seção 3.1 (806); seção 4.1.1 (ganho só nos dois primeiros meses); seção 4.1.2 (persistência); seção 3.2.1 (complexidade é a cognitiva) |
| 15 | 1 (l. 75-76) | em 395 artigos, apenas 0,92% tratam de projeto | Hou 2024 | PARCIAL | Seção 6.1, Fig. 10 e Tabela 10, p. 1:26-27: 0,92% = 4 de 436 registros de tarefa, não 4 de 395 artigos |
| 16 | 1 (l. 76-78) | faltam estudos controlados sobre configurações mais ricas | Galster 2026 | FIEL | Conclusão, p. 19: estudos controlados devem comparar configuração mais rica com só arquivos de contexto |
| 17 | 1 (l. 84-85) | pré-registro: protocolo e regras fixados antes da coleta | Ernst 2023 | FIEL | Seção de motivação (p. 4 do preprint): protocolo com hipóteses, coleta e análise registrado antes do estudo; p. 8: compromisso com a análise antes de ver os dados |
| 18 | 2 (l. 115-116) | a parte que muda fica num objeto próprio, separada da estável | Gamma 1995 | PARCIAL | P. 29: encapsular o conceito que varia; Tabela 1.2, p. 30, lista os cinco patterns. A fonte não diz "objeto próprio" como regra geral, e no Template Method a variação fica em subclasses |
| 19 | 2 (l. 120-121) | em vez de perguntar o caso, chama-se um método e cada caso responde | Fowler 2018 | FIEL | *Replace Conditional with Polymorphism* (272), motivação |
| 20 | 2 (l. 140-141) | pattern sem necessidade: flexibilidade que custa indireção | Gamma 1995 | FIEL | P. 31: só aplicar quando a flexibilidade for necessária |
| 21 | 2 (l. 143-144) | CK implementa versões das métricas, com regras próprias | Aniche 2015 | FONTE AUSENTE | README e código do commit 8a1ef916 não salvos na leitura; há só o `.jar` em `construction-v5/evaluation/tools/ck`. A anotação antiga leu o README do ramo master, não esse commit |
| 22 | 2 (l. 144-146) | definições de CBO, LCOM (original, piso zero), WMC, DIT | Chidamber 1994 | FIEL | Lido na imagem do PDF: WMC p. 482, DIT p. 483, CBO p. 486, LCOM p. 488 |
| 23 | 2 (l. 147) | no commit 8a1ef916, o CBO conta as dependências da classe | Aniche 2015 | FONTE AUSENTE | Texto do commit não salvo |
| 24 | 3 (l. 163-164) | refatoração para patterns e princípios; cruzamento com dez júniores | Ampatzoglou 2026 | FIEL | Resumo; seção 1; seção 3.2: 10 júniores, AB/BA |
| 25 | 3 (l. 164-165) | LLM aumenta correção da implementação; sem LLM melhor na identificação do problema | Ampatzoglou 2026 | FIEL | Resumo (resultado qualitativo). O arquivo salvo termina no título da seção 4 |
| 26 | 3 (l. 165) | na escolha da prática, não houve diferença | Ampatzoglou 2026 | FONTE AUSENTE | Seção 4 não está no texto salvo; o único registro é a anotação antiga, que não vale como conferência |
| 27 | 3 (l. 167-168) | ponto em que o pattern seria exagero: ausente naquele estudo | Ampatzoglou 2026 | FIEL | Seção 3.2: tarefas construídas para aplicar uma prática; nada sobre controle negativo no texto salvo (seções 1 a 3) |
| 28 | 3 (l. 170-171) | 2.303 arquivos de 1.925 repositórios | Chatlatanagulchai 2026 | FIEL | Resumo; seção 1 |
| 29 | 3 (l. 171-172) | descreve o conteúdo, sem experimento sobre qualidade do código | Chatlatanagulchai 2026 | FIEL | Perguntas de pesquisa são de estrutura, manutenção e conteúdo; p. 26 sugere estudos controlados como trabalho futuro |
| 30 | 3 (l. 172-173) | 148 projetos antes e depois; resultado misto em PRs; sem medir o projeto do código | Arabat 2026 | FIEL | Resumo e RQ1: 27,7% sobem ao menos 20%, 26,35% caem; métricas de RQ2 e RQ3 não incluem desenho |
| 31 | 4.1 (l. 203) | *skill* pública de patterns | Grndlvl 2026 | FIEL | Não está na pasta de leitura, mas a cópia está em `construction-v5/experiment/third-party/gof-patterns/ORIGEM.md`, com autor, repositório e commit 85e94a3 |
| 32 | 7 (l. 394) | substituir todos os avaliadores humanos é arriscado | Ahmed 2025 | FIEL | Seção V: risco em substituir todos; substituir um avaliador é aceitável em parte dos casos |
| 33 | 7 (l. 396) | abaixo do piso de dez repetições sugerido para algoritmos randomizados | Arcuri 2014 | PARCIAL | Diretrizes finais: ao menos 1.000 execuções por artefato; o mínimo de 10 vale quando há muitos artefatos e pouco tempo (seção 10.1 e lista de diretrizes) |
| 34 | 7 (l. 398) | LCOM zero não indica coesão máxima | Chidamber 1994 | FIEL | Nota 28, p. 488 |
| 35 | 7 (l. 398) | LCOM sem relação significativa com falhas; 180 classes C++ de alunos | Basili 1996 | FIEL | P. 756: insignificante em todos os casos; p. 753-754: alunos de graduação e pós, 180 classes em C++ |
| 36 | 7 (l. 398) | complexidade cognitiva conta as condicionais por construção | Campbell 2018 | FIEL | Seção 2.2, p. 57: incrementos para `if`, `switch` e outros desvios |

## Correções propostas para as ocorrências PARCIAL

**Nº 9 (Introdução, l. 45-47).** Sem o denominador, o leitor entende 5,5% de todos os repositórios públicos, mas a base são os 2.853 que já usam alguma ferramenta de agente.

> Em 2.853 repositórios públicos que já usam ferramentas de agente, os arquivos de contexto predominam (90,6%), enquanto *skills* (5,5%) e subagentes (4,6%) ainda são pouco adotados [Galster et al. 2026].

Os dois percentuais são calculados a partir das contagens da fonte (158 e 131 de 2.853); a fonte não os imprime nessa forma.

**Nº 15 (Introdução, l. 75-76).** O número está certo, mas a base é de registros de tarefa (436), e não os 395 artigos.

> Ao mesmo tempo, o projeto de software recebe pouca atenção: em uma revisão de 395 artigos sobre LLMs em engenharia de software, essa atividade responde por apenas 0,92% das tarefas registradas, 4 de 436 [Hou et al. 2024].

Cabe ainda lembrar que as quatro tarefas de Hou são recuperação de interfaces, prototipação rápida, síntese de especificação e desenho de sistema, e não design orientado a objetos. A revisão vai até janeiro de 2024.

**Nº 18 (Seção 2, l. 115-116).** A fonte fala em encapsular o conceito que varia; "num objeto próprio" não vale para o Template Method, em que a variação fica em subclasses, e a frase seguinte inclui os cinco patterns.

> **Encapsular o que varia.** A parte que muda de um caso para outro fica separada da parte estável, em um objeto ou em uma subclasse própria [Gamma et al. 1995].

**Nº 33 (Seção 7, l. 396).** Dez é o mínimo admitido numa situação específica; a recomendação geral é muito maior, o que torna a limitação mais séria do que a frase sugere.

> O número de cinco rodadas por combinação decorreu do custo, e não de um cálculo de poder, e fica abaixo até do mínimo de dez repetições que Arcuri e Briand (2014) admitem quando há muitos artefatos e pouco tempo de execução, sendo de ao menos 1.000 a recomendação geral para algoritmos randomizados; por isso, a extensão descreve as diferenças, sem teste de significância.

## FONTE AUSENTE: o que falta conferir

- **Anthropic 2026 (nº 4, 6, 7, 8).** Salvar as três páginas citadas (`/memory`, `/skills`, `/sub-agents`). A afirmação mais específica, e que mais precisa da conferência, é a do `CLAUDE.md` "tratado como contexto, e não como regra imposta".
- **Aniche 2015 (nº 21, 23).** Conferir o README ou o código do commit 8a1ef916, e não o ramo master.
- **Ampatzoglou 2026 (nº 26).** Conseguir a seção 4.1 do artigo publicado. O arquivo salvo foi cortado em 50.000 de 97.689 caracteres, exatamente no título "4. Results". A anotação antiga registra ausência de diferença na escolha da prática (p = 0,811), mas não foi possível verificá-la na fonte.

## Observações sobre as referências

- **Confirmados no texto salvo:** Ampatzoglou (JSS 238, 112862), Arabat (MSR '26, 5 páginas, compatível com 969-973), Basili (751-761), Boehm (início em 1226), Campbell (57-58), Chidamber (476-493, 18 páginas), Galster (AIware '26, DOI e páginas 11 a 20; o título da versão ACM é o da lista, e o preprint v5 mudou para *Harness Engineering*), Gamma (copyright 1995).
- **Lidos em preprint; volume e páginas da lista não são confirmáveis no texto salvo:** Ahmed (arXiv v2), Arcuri (versão dos autores), Brown (arXiv v4), Chatlatanagulchai (arXiv v2), Ernst (arXiv v1), He (arXiv v3), Hou (arXiv v6). Os números citados foram conferidos nessas versões. O cabeçalho do artigo diz que preprints ficam de fora, o que vale para o que é citado na lista, mas as conferências destes dependem dos preprints.
- **He 2026:** a lista dá 181-193 (13 páginas) e o arXiv v3 declara 19 páginas. Provavelmente é o apêndice do preprint; vale confirmar nos anais.
- **He 2026, nuance:** com controles de tamanho do projeto (modelos GMM, seção 4.2), o Cursor não tem efeito significativo sobre os alertas, embora tenha sobre a complexidade. A frase do artigo trata da persistência do aumento e continua correta.
- **Fowler 2018:** o exemplar salvo traz copyright 2019 (catálogo da Library of Congress de 2018). O ano de 2018 é o usual para a 2ª edição, mas convém mantê-lo de forma consciente.

## Cobertura

Lidos na íntegra: Campbell 2018 e os trechos integrais, em imagem, das páginas 482, 483, 486 e 488 de Chidamber 1994 (PDF escaneado). Nos demais, fiz busca por termos, incluindo sinônimos em inglês, e li o trecho em volta de cada achado. As anotações de `vereditos/` serviram só para localizar trechos.
