Autoavaliação feita com ferramenta de IA. A avaliação oficial é do orientador.

# Autoavaliação antes da entrega: artigo, versão 140 (rodada 14)

Esta autoavaliação segue os critérios de revisão do professor Rodrigo Rebouças de Almeida (DCX/UFPB). Eles são orientações dele para os seus orientandos, e não critérios oficiais da disciplina de TCC, do curso ou da UFPB; quando houver diferença, valem a resolução de TCC e as normas oficiais. O uso desta skill é uso de ferramenta de IA e entra na declaração de uso do TCC (seção 8 do guia).

Arquivo revisado: `artigo-v140.md`, com as 10 figuras abertas em PNG. Nenhum arquivo do artigo foi alterado.

## 1. Pontos já decididos (não contam como pendência)

Os pontos abaixo foram decididos por você ou já foram conferidos em rodadas anteriores. Eles não entram na contagem de apontamentos.

**Protegidos por decisão sua.**

- As Figuras 2, 5, 6, 7 e 8 mostram código como imagem, com fundo branco. O roteiro pede código em texto, mas a escolha é sua e está registrada.
- Uma figura por *pattern* na Seção 6.2.
- As Figuras 9 e 10 repetem os dados das Tabelas 7 e 8. O roteiro pede para não repetir a mesma informação em tabela e figura, mas a escolha é sua.
- A declaração de uso de IA será escrita por você no fim; o apoio à redação não entra na Tabela 1.
- A coluna "como foi conferido" da Tabela 1 foi confirmada por você. Registro aqui uma observação, sem contar como apontamento: a linha "apoio ao desenho dos instrumentos e às bancadas" diz "revisão do autor", e a Seção 6.2 diz que os dois ajustes de redação dos controles negativos, propostos pelo Claude Fable, foram aplicados "sem revisão do autor". O trecho da Seção 6.2 é específico e honesto, e a Seção 8 retoma o ponto; a leitura do resultado não muda. Só você decide se quer marcar essa exceção.
- Título mantido; nome da H8 segue o pré-registro.
- Motivos já dados por você: Strategy difundido e adequado ao domínio; cinco *patterns* para as três categorias do GoF; N3 fora da extensão; segunda rodada do controle decidida antes do resultado da primeira; diferença de até 10 horas por causa da janela de 5 horas da cota; três modelos da extensão (pior, mediano e melhor); Opus 5.5 como verificador; esforço *medium* pela cota.
- Referências só de revistas e eventos; sem *preprints*; definição de IA por Poole e Mackworth.
- Registro acadêmico em prosa, sem listas no corpo e sem rótulos em negrito; P1 a P8 em parágrafo. Este relatório não pede listas no corpo do texto.
- Resumo de 150 a 250 palavras (está no limite) e Abstract como tradução fiel.
- *Harness* no sentido de Galster et al., com o aviso da diferença para o pré-registro.
- Só modelos da Anthropic, declarado como limitação.

**Já verificado: não procede.**

- Tabela 11, *tokens* de saída do N2 da construção (38,9 mil): todas as células usam a soma dos segmentos.

**Pendências que dependem de você (já registradas).**

- Sugestões não aplicadas de rodadas anteriores: tabela comparativa dos trabalhos relacionados, um nome só para os níveis, "et al." nas referências.
- Citar Page (1963) e Cohen (1960) depende de você ler e aprovar; itálico nas figuras.
- Motivo de duas análises do pré-registro (OBJETIVO §4.5 e §4.6) não terem sido calculadas; o texto já registra o fato na Seção 8.
- O conteúdo de Yao et al. (2023), Kim (2025) e Kim (2026) precisa ser aberto por você; a automação não confirma o que o texto atribui a eles.
- Avisos de rodadas anteriores (pedido de e-mail da API do OpenAlex, que não foi atendido; PDFs abertos baixados e apagados na rodada 10).

## 2. Pontos a tratar

Nesta rodada não encontrei nenhum ponto que o roteiro trate como obrigatório, nenhum recomendado e nenhuma sugestão.

Esse resultado vem de conferência, e não de leitura rápida. Somei todas as tabelas e refiz as contas do texto contra os dados brutos (detalhe na leitura macro, item 4). Não encontrei número que divirja, contradição entre seções nem afirmação sem apoio. Os pontos de cautela que rodadas anteriores levantaram (calibração que não cobre o *design* do restante e a estrutura desnecessária, a regra de empate da H5, a exceção do Strategy na construção, o controle negativo no teto) aparecem com a mesma cautela no Resumo, na Discussão, nas Limitações e na Conclusão.

## 3. Leitura macro

**1. Mapa das seções.** A Introdução apresenta o problema, o objetivo geral, as oito perguntas e a organização do texto. A Seção 2 define os conceitos e as métricas usados depois (encapsulamento, polimorfismo, aberto/fechado, CK, complexidade cognitiva). A Seção 3 delimita a lacuna. A Seção 4 mostra as fases, o desenho, os envolvidos, a IA usada, os níveis e as tecnologias. A Seção 5 traz método, proteção contra decisões posteriores e resultados do estudo principal. A Seção 6 traz a motivação da extensão, o método, o verificador, os controles e os resultados. A Seção 7 responde P1 a P8. A Seção 8 traz as limitações, a Seção 9 a conclusão, e o Apêndice A os dados por modelo. O parágrafo de organização da Introdução corresponde a essas seções, na mesma ordem. A seção chamada "Discussão" (7) aparece no parágrafo de organização como "responde às perguntas de pesquisa", o que é o mesmo papel.

**2. Linha do argumento.** Problema (efeito das orientações persistentes sobre a decisão de usar *pattern* não nomeado), objetivo geral, perguntas, método, resultados e respostas formam uma sequência. Os termos são definidos antes do uso: exagero na Introdução, teto e saldo na Seção 5.1, categorias do *pattern* e *pattern* certo na Seção 6.2, estrutura desnecessária, contenção e exagero dos controles na Seção 6.2.

**3. Resumo, Introdução e Conclusão.** Os três falam da mesma coisa, com os mesmos números (67% para 93%; 220 e 450 execuções; 19 para 58 e 41 para 60; 61 para 54; 88 de 90; 4 para 9) e a mesma cautela (piora do Haiku 4.5 na corretude, exceção do Strategy na construção, notas fora da calibração). A Conclusão não afirma mais do que a Discussão.

**4. Números repetidos.** Conferi contra os registros:

- Tabela 8 soma a linha *pattern* da Tabela 7 (41, 53, 60 e 19, 33, 58), e a Tabela 14 reproduz todas as células da Tabela 7 pela soma dos três modelos. As Figuras 9 e 10 mostram os mesmos valores.
- Ganho de N0 a N1: Decorator e Template Method somam +18, e os outros três somam +8. Strategy nas duas tarefas: 15 para 14 em 30.
- Categorias do verificador recalculadas dos `review.json`: N0 da construção 32, 9, 7, 27; N2 da construção 55, 5, 9, 6; N0 da manutenção 15, 4, 26, 30; N2 da manutenção 56, 2, 14, 3. Daí saem 45, 53, 72 e 48, 66, 69 com o malfeito, e os totais da Tabela 10 (126, 28, 29, 41 e 99, 11, 60, 55, com a execução sem código fora).
- Estrutura desnecessária 4, 5, 9 e 0, 1, 2; comportamento ruim na construção 71 (Haiku 4.5) e 32 (Sonnet 4.5); 59 reservas duplas e 16 execuções sem esse teste; nenhuma reserva dupla na manutenção.
- Tabela 15 fecha com a Tabela 8 no Decorator (4, 10, 10 e 3, 5, 8) e com as 11 execuções da categoria acerto.
- Tabela 16 soma 89, 96, 109 por modelo, 96, 103, 95 por nível, 294 de 360 (82%) e 54 combinações estáveis.
- Tabela 9 é reproduzida pelo `oo-metrics.csv` em todas as células. A conta aproximada do CBO vezes os tipos dá +22% a +46% no estudo principal e +3% a +27% na extensão, como no texto.
- Tabela 11: médias por nível iguais às dos `meta.json` (0,96, 1,06, 1,07 e 0,60, 0,66, 0,72) e soma de US$ 380,26. Estudo principal: US$ 346,16.
- *Skill* aberta em 19, 25, 25 no N2 de cada tarefa (69 de 75) e em 20 de 30 nos controles.
- Controles: 88 de 90 contidos, 87 com comportamento bom (as três exceções do Haiku 4.5, cenários das crianças e da assinatura) e `mvn verify` em 90 de 90.
- Segundo verificador: 195 de 210 notas iguais (30, 30, 29, 29, 27, 26, 24), kappa de 0,53 no *design*, e as 15 diferenças são todas um degrau mais brandas.
- Estudo principal, contra `resultados-V5-STRATEGY.md` e `hipoteses-V5-STRATEGY.md`: Tabela 5, Tabela 13, saldos de H1 e H3 (10 melhores e 7 piores), perfis distintos (7 menos, 3 iguais, 1 mais), sinal dos *tokens* (6 e 5), Page (p = 0,007 e p < 0,001), exagero por modelo (H2, H23, H26) e os vereditos de H27 e H28.
- Regras de decisão: 36 de 55 dá 3,0% nos dois lados, 35 dá 5,8%; 5 de 5 dá 6,25%; 7 de 55 e 2 de 15 dão cerca de 13%.
- Datas e horários dos `meta.json`: primeira rodada em 21 e 22/10; Factory Method e Decorator repetidos em 22/10; os outros três em 01 e 02/11; controles em 09/11; até 4,0 horas entre níveis do mesmo modelo e até 9,9 horas no cenário e rodada.

**5. Termos centrais.** "Configuração", "orientações persistentes", "nível", "teto", "saldo", "ponto do clube", "ponto do seguro", "exagero", "*pattern* certo", "categoria acerto", "estrutura desnecessária" e "contenção" mantêm o mesmo nome e o mesmo sentido. Onde o mesmo nome poderia confundir (exagero no estudo principal e nos controles; N1 e N2 nos dois estudos; certo no Semgrep e no verificador), o texto avisa a diferença (Seção 6.2, Tabela 2 e Discussão da P1).

**6. Proporção.** Método e resultados ocupam a maior parte do texto; a Fundamentação e os Trabalhos relacionados são curtos e usados depois. A Introdução é longa, mas define termos que você decidiu definir ali.

**7. Percurso de cada objetivo.** As oito perguntas e o controle negativo têm método, resultado e resposta explícita (matriz na seção 6).

**8. Proveniência dos resultados.** Cada resultado tem etapa de origem: suíte, Semgrep, conferência humana, SonarQube e CK (Seção 5.1); verificador, roteiro de comportamento, JaCoCo, jdeps e `pg_dump` (Seção 6.2); controles (Seção 6.2); custo e *tokens* do registro do Claude Code (Seções 5.1 e 6.2); abertura da *skill* pela transcrição (Seção 5.1); estabilidade (Seções 5.1 e 6.2); segundo verificador (Seção 6.2).

**9. Justificativa das escolhas.** As escolhas que mudam a leitura dos resultados têm motivo declarado: amostra de modelos, número de rodadas, limites das regras de decisão, critérios das notas, exclusão do N3 na extensão, ordem das repetições, Docker fora dos contêineres, segunda rodada do controle, verificador ciente do controle, testes não pedidos na construção, migrações exigidas na construção.

## 4. Apontamentos por critério

**Critério 1. Língua portuguesa e redação.** Sem apontamentos. Não encontrei erro de ortografia, concordância ou regência, marcador pendente nem os padrões de escrita do roteiro. Há frases longas na Seção 6.2, mas elas não tornam ambígua a leitura de nenhum resultado.

**Critério 2. Encadeamento.** Sem apontamentos. Cada seção usa o que a anterior preparou, e as perguntas P1 a P8 são retomadas com o mesmo código nas Seções 5.3, 6.3, 7 e 9.

**Critério 3. Terminologia e siglas.** Sem apontamentos. As siglas do domínio (CBO, LCOM, WMC, DIT, LLM, API) são expandidas na primeira ocorrência.

**Critério 4. Resumo e Abstract.** Sem apontamentos. O Resumo tem problema, objetivo, método e resultados com números, todos iguais aos das tabelas, e o Abstract traduz o Resumo sem acrescentar nem cortar informação.

**Critério 5. Introdução.** Sem apontamentos. Contexto e problema têm fontes; o objetivo geral e as oito perguntas estão numerados e correspondem ao que foi feito; o contexto institucional (TCC da Licenciatura, UFPB) e o papel do orientador estão explicados (Seção 4). O texto não menciona o AYTY, então o ponto do roteiro sobre o laboratório não se aplica.

**Critério 6. Coerência metodológica.** Sem apontamentos. As etapas têm o que, como, com que ferramenta e quem; os envolvidos (autor, orientador, leitor humano, agentes de IA) estão identificados na Seção 4 e na Tabela 1. Todo resultado tem etapa de origem, e os desvios do pré-registro estão declarados (releitura não feita, duas análises não calculadas, *tokens* do revisor).

**Critério 7. Solução, resultados e conclusão.** Sem apontamentos. Os resultados são concretos e organizados pelas perguntas; cada pergunta tem resposta explícita na Seção 7; a Conclusão repete os números e as ressalvas da Discussão; limitações e trabalhos futuros são específicos do trabalho.

**Critério 8. Figuras, tabelas e código.** Sem apontamentos fora dos pontos protegidos. As 10 figuras e as 16 tabelas têm número, título e fonte e são apresentadas no texto antes de aparecer, com descrição do que mostram. Os gráficos (Figuras 9 e 10) têm eixo com rótulo, escala e legenda, e os valores conferem com as Tabelas 7 e 8. As imagens de código têm fundo branco.

**Critério 9. Consistência de conteúdo.** Sem apontamentos. Não encontrei contradição entre seções nem afirmação mais forte em um trecho do que em outro. A observação sobre a Tabela 1 e a Seção 6.2 está na seção 1, porque envolve a coluna protegida.

**Critério 10. Normas e referências.** Sem apontamentos. Todas as citações do texto estão na lista, e todas as referências da lista são citadas. Conferi pelos metadados do Crossref os 26 DOIs da lista: todos resolvem para o trabalho citado, com o mesmo primeiro autor, ano, título, veículo, volume e páginas. Arcuri e Briand aparece no Crossref com 2012 (publicação *online*), e o ano 2014 da lista é o do fascículo 24(3), o que está correto. Não puderam ser conferidas por DOI as referências sem DOI (Brown et al. 2020, Vaswani et al. 2017, Yao et al. 2023, os livros e as páginas de ferramentas); os links têm data de acesso e não têm parâmetro de rastreio. Não há citação direta de livro.

**Critério 11. Tecnologias e repositório.** Sem apontamentos. A Tabela 3 traz nome, versão e papel de cada tecnologia, e o link do repositório (https://github.com/FLucasF/TCC) respondeu com código 200.

## 5. Contagem

| classificação | quantidade |
|---|---|
| OBRIGATÓRIA | 0 |
| RECOMENDADA | 0 |
| SUGESTÃO | 0 |

## 6. Matriz de rastreabilidade

| objetivo / pergunta | onde é desenvolvido | etapa do método que gera os dados | resultado (seção, tabela) | resposta explícita (onde) | situação | ação pendente |
|---|---|---|---|---|---|---|
| Objetivo geral: avaliar o efeito de orientações persistentes sobre a decisão de aplicar um *pattern*, na construção e na manutenção | Seções 5 e 6 | Seções 5.1 e 6.2 | Seções 5.3 e 6.3 | Seção 9, primeiro e segundo parágrafos | atendido | nenhuma |
| P1. Uso do *pattern* esperado | Seções 5.3 e 6.3 | Semgrep e conferência humana (5.1); verificador (6.2) | Tabelas 5, 7, 8, 13, 14; Figuras 9 e 10 | Seção 7, P1; Seção 9 | atendido | nenhuma |
| P2. Exagero, corretude e qualidade | Seções 5.3 e 6.3 | suíte, Semgrep e SonarQube (5.1); verificador e controles (6.2) | Tabelas 4, 5, 7, 12, 14 | Seção 7, P2; Seção 9 | atendido | nenhuma |
| P3. Variação entre modelos | Seções 5.3 e 6.3 | regra de comparação entre modelos (5.1); escolha dos três modelos (6.2) | Tabelas 13 e 14 | Seção 7, P3; Seção 9 | atendido | nenhuma |
| P4. Componentes e custo | Seções 5.3 e 6.3 | hipóteses de custo e registro do Claude Code (5.1, 6.2) | Tabelas 4, 5, 11 | Seção 7, P4; Seção 9 | atendido | nenhuma |
| P5. Tipo de tarefa em cinco *patterns* | Seção 6.3 | verificador nas duas tarefas (6.2) | Tabelas 7 e 8; Figura 10; categorias na Seção 6.3 | Seção 7, P5; Seção 9 | atendido | nenhuma |
| P6. Demais dimensões de qualidade | Seção 6.3 | cinco dimensões do verificador (6.2) | Tabelas 7 e 14 | Seção 7, P6; Seção 9 | atendido | nenhuma |
| P7. Estabilidade entre rodadas | Seções 5.3 e 6.3 | perfis distintos (5.1); repetição da primeira rodada (6.2) | Tabela 16 e Seção 5.3 | Seção 7, P7; Seção 9 | atendido | nenhuma |
| P8. Estrutura orientada a objetos | Seções 5.3 e 6.3 | CK (Seção 2, 5.1, 6.2) | Tabelas 6, 9, 10 | Seção 7, P8; Seção 9 | atendido | nenhuma |
| Controle negativo da extensão (tratado com a P2) | Seção 6.3 | cinco cenários de controle e verificador (6.2) | Tabela 12 | Seção 7, P2 (segundo parágrafo); Seção 9 | atendido | nenhuma |

Não há resultado sem objetivo: os dados descritivos do CK, do custo e da estabilidade estão ligados à P8, à P4 e à P7.

## 7. Checklist da seção 10 do guia

**Elementos obrigatórios da resolução**

- A Introdução descreve o problema e o contexto: atendido.
- A metodologia descreve o processo e identifica os envolvidos: atendido (Seção 4 e Tabela 1).
- Há descrição das tecnologias, com versão: atendido (Tabela 3).
- A solução é apresentada com artefatos e métricas de dimensão: atendido (Figuras 1 a 8; 220, 450 e 90 execuções; métricas do CK).
- O texto traz o link do repositório público: atendido.
- Se houve avaliação, o processo e os resultados estão descritos: atendido (verificador, calibração, segundo verificador, conferência humana).

**Critério 1. Língua portuguesa e redação**

- Sem erros de ortografia, acentuação, concordância ou regência: atendido.
- Tempos verbais consistentes: atendido.
- Sem frases longas que encadeiam vários resultados: atendido (há frases longas na Seção 6.2, sem ambiguidade).
- Sem declarações de importância sem fato: atendido.
- Sem gerúndio de comentário: atendido.
- "É" e "tem" no lugar de "atua como": atendido.
- Afirmação de terceiros com fonte: atendido.
- Sem contrastes montados nem fórmulas de enchimento: atendido.
- Títulos com maiúscula só na primeira palavra: atendido.
- Sem vírgula antes do "e" final nem decalques: atendido.

**Critério 2. Encadeamento**

- Cada seção prepara a seguinte: atendido.
- Todo termo técnico definido antes do uso: atendido.
- Cada pergunta retomada com o mesmo nome até a Conclusão: atendido.
- Tamanho de cada seção corresponde ao papel: atendido.

**Critério 3. Terminologia e siglas**

- Cada conceito tem um nome só: atendido.
- Nenhum nome designa duas coisas sem aviso: atendido.
- Siglas pouco conhecidas expandidas: atendido.
- Estrangeirismos em itálico: atendido.

**Critério 4. Resumo e Abstract**

- Problema, objetivo, o que foi feito e resultado com número: atendido.
- Resumo se entende sozinho: atendido.
- Abstract é tradução fiel: atendido.

**Critério 5. Introdução**

- Contexto, problema e relevância com fontes: atendido.
- Objetivo geral e perguntas claros, numerados e verificáveis: atendido.
- Objetivos correspondem ao que foi feito: atendido.
- Contexto institucional explicado: atendido.
- Parágrafo de organização corresponde às seções: atendido.

**Critério 6. Metodologia**

- Cada etapa diz o que, como, com que ferramenta e quem: atendido.
- Sequência das etapas clara: atendido (Figura 3 e Seção 6.2).
- Cada escolha com motivo declarado: atendido.
- Todo resultado com etapa de origem: atendido.
- Toda etapa aparece nos resultados, e toda atividade está na metodologia: atendido.
- Versões das ferramentas informadas: atendido.

**Critério 7. Solução, resultados e conclusão**

- Solução descrita com métricas e artefatos: atendido.
- Resultados concretos e organizados pelas perguntas: atendido.
- Cada pergunta com resposta explícita: atendido.
- Nenhum resultado desligado dos objetivos: atendido.
- Conclusão com resultados e as mesmas ressalvas: atendido.
- Limitações e trabalhos futuros específicos: atendido.

**Critério 8. Figuras, tabelas e código**

- Toda figura e tabela com número, título e fonte: atendido.
- Toda figura e tabela apresentada com descrição: atendido.
- Todo gráfico com eixos, rótulo e legenda: atendido.
- Todo trecho de código em texto: decidido por você (Figuras 2, 5, 6, 7 e 8 ficam como imagem).
- Nenhuma imagem de código com fundo escuro: atendido (fundo branco).
- A mesma informação não aparece em tabela e figura: decidido por você (Figuras 9 e 10 com as Tabelas 7 e 8).

**Critério 9. Consistência de conteúdo**

- O mesmo número tem o mesmo valor em todo o texto: atendido.
- Resumo, Introdução e Conclusão com a mesma cautela: atendido.
- Sem contradição entre seções: atendido.
- Sem trechos genéricos: atendido.
- Sem funcionalidade, tecnologia ou etapa citada uma vez só: atendido.
- Estilo uniforme: atendido.

**Critério 10. Normas e referências**

- Toda citação na lista e toda referência citada: atendido.
- Cada DOI leva ao trabalho citado: atendido (26 de 26 pelo Crossref; obras sem DOI não conferidas por esse meio).
- Sem marcador pendente: atendido.
- Citações diretas de livro com página e links com data de acesso: atendido (não há citação direta; todos os links têm data).
- Numeração das seções consistente e elementos do modelo: atendido.

**Critério 11. Tecnologias e repositório**

- Tecnologias em tabela com versão e papel: atendido.
- Link do repositório no texto e acessível: atendido.

**Revisão do texto inteiro e matriz**

- Leitura do texto inteiro: atendido.
- Matriz completa, sem "não encontrado": atendido.

**Uso de ferramentas de IA**

- Declaração de uso depois da Conclusão: pendente por decisão sua (você escreve por último).
- IA como instrumento do trabalho descrita na metodologia: atendido (Tabela 1 e Seções 4, 6.2).
- Todo conteúdo, código e referência sugeridos por ferramenta conferidos: parcial (falta você abrir Yao et al. 2023, Kim 2025 e Kim 2026, já registrado nas pendências).
