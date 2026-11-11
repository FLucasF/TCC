Autoavaliação feita com ferramenta de IA. A avaliação oficial é do orientador.

# Autoavaliação antes da entrega: artigo, versão 140 (rodada 15)

Esta autoavaliação segue os critérios de revisão do professor Rodrigo Rebouças de Almeida (DCX/UFPB). Eles são orientações dele para os seus orientandos, e não critérios oficiais da disciplina de TCC, do curso ou da UFPB; quando houver diferença, valem a resolução de TCC e as normas oficiais. O uso desta skill é uso de ferramenta de IA e entra na declaração de uso do TCC (seção 8 do guia).

Arquivo revisado: `artigo-v140.md` (versão de 11/11, depois das correções da checagem contra os dados), com as 10 figuras abertas em PNG. Nenhum arquivo do artigo nem dos dados foi alterado. Não usei a rede: os números foram refeitos a partir dos arquivos em `J:\TCC`, e as referências foram conferidas pelos metadados já registrados no projeto.

## 1. Pontos já decididos (não contam como pendência)

**Protegidos por decisão sua.** Estes pontos não entram na contagem, mesmo quando o roteiro trataria de outro jeito.

- As Figuras 2, 5, 6, 7 e 8 mostram código como imagem, com fundo branco. O roteiro pede código em texto; a escolha é sua e está registrada.
- Uma figura por *pattern* na Seção 6.2.
- As Figuras 9 e 10 ficam junto das Tabelas 7 e 8, mesmo repetindo os dados.
- A declaração de uso de IA será escrita por você no fim; o apoio à redação do artigo fica só nessa seção, e não na Tabela 1.
- A coluna "como foi conferido" da Tabela 1 foi confirmada por você, e o Fable tem linha própria (revisão dos textos dos controles, sem versão nem esforço registrados, sem revisão do autor), aprovada em 11/11.
- Título mantido; nome da H8 segue o pré-registro.
- Motivos dados por você: Strategy difundido e adequado ao domínio; cinco *patterns* para as três categorias do GoF; N3 fora da extensão; segunda rodada do controle decidida antes do resultado da primeira; diferença de até 10 horas pela janela de 5 horas da cota; três modelos da extensão (pior, mediano e melhor no estudo anterior); Opus 5.5 como verificador (capaz e com custo viável); esforço *medium* pela cota.
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

- Sugestões não aplicadas de rodadas anteriores: tabela comparativa dos trabalhos relacionados, um nome só para os níveis, "et al." nas referências.
- Citar Page (1963) e Cohen (1960) depende de você ler e aprovar; itálico nas figuras.
- O motivo de duas análises do pré-registro (OBJETIVO §4.5 e §4.6) não terem sido calculadas; o texto já registra o fato na Seção 8.
- O conteúdo de Yao et al. (2023), Kim (2025) e Kim (2026) precisa ser aberto por você.
- Avisos de rodadas anteriores: o pedido de e-mail da API do OpenAlex, que não foi atendido, e os PDFs abertos baixados e apagados na rodada 10.

## 2. Pontos a tratar

Nesta rodada, nenhum ponto é tratado pelo roteiro como obrigatório. Encontrei um ponto recomendado e duas sugestões. Os três são pequenos e não mudam a direção de nenhum resultado.

- **Apontamento 2 (RECOMENDADA).** Na Discussão da P4, a frase "o `CLAUDE.md` não alterou o tempo de API nem os turnos" afirma mais do que as hipóteses H17 e H18 sustentam.
- **Apontamento 1 (SUGESTÃO).** A regra "a execução que não entrega nada recebe ruim onde não há o que avaliar" (Seção 6.2) não descreve o que aconteceu na execução da manutenção que não mudou nenhuma linha, que recebeu ruim em todas as dimensões.
- **Apontamento 3 (SUGESTÃO).** A Seção 2 dá a entender que os métodos abstratos foram contados nos dois estudos, mas só a extensão os mede.

## 3. Leitura macro

**1. Mapa das seções.** A Introdução apresenta o contexto, o problema, o objetivo geral, as oito perguntas, as contribuições e a organização do texto. A Seção 2 define os conceitos e as métricas usados depois (encapsulamento do que varia, polimorfismo, aberto/fechado, acoplamento e coesão, CK, complexidade cognitiva). A Seção 3 delimita a lacuna. A Seção 4 mostra as fases, o desenho, o repositório, os envolvidos, a IA usada (Tabela 1), os níveis (Tabela 2), as tecnologias (Tabela 3) e o isolamento das execuções. A Seção 5 traz o método, a proteção contra decisões posteriores e os resultados do estudo principal. A Seção 6 traz a motivação da extensão, o método, o verificador, os controles e os resultados. A Seção 7 responde P1 a P8, a Seção 8 traz as limitações, a Seção 9 a conclusão, e o Apêndice A os dados por modelo. O parágrafo de organização corresponde a essas seções, na mesma ordem.

**2. Linha do argumento.** Problema, objetivo, perguntas, método, resultados e respostas formam uma sequência. Os termos são definidos antes do uso: exagero na Introdução; teto, saldo, ponto do clube e ponto do seguro na Seção 5.1; categorias do *pattern* e *pattern* certo na Seção 6.2; estrutura desnecessária, contenção e exagero dos controles na Seção 6.2.

**3. Resumo, Introdução e Conclusão.** Os três falam da mesma coisa, com os mesmos números (67% para 93%; 220, 450 e 90 execuções; 19 para 58 e 41 para 60; 61 para 54; 88 de 90; 4 para 9) e a mesma cautela (piora do Haiku 4.5 na corretude, exceção do Strategy na construção, notas fora da calibração, controle no teto). A Conclusão não afirma mais do que a Discussão.

**4. Números repetidos.** Refiz as contas a partir dos dados, e não só das tabelas do texto:

- Tabela 5, a partir de `construction-v5/analysis/notas-V5-STRATEGY.csv`: frete 78, 96, 96, 98%; cupom 65, 93, 93, 100%; pagamento 55, 91, 95, 98%; clube 67, 93, 93, 98%; suíte inteira 71, 84, 80, 87%. Linhas, complexidade, custo e exagero conferem com `resultados-V5-STRATEGY.md`.
- Tabela 6, a partir de `construction-v5/evaluation/metrics/V5-STRATEGY/metricas.csv`: todas as células conferem (tipos 22,0 a 30,2; interfaces; constantes com corpo e anônimas; WMC; CBO e LCOM em mediana; percentil 90 na posição 50 de 55; DIT). Seis projetos com LCOM média de 37,2 ou mais no N0, um no N2 e nenhum no N1 e no N3: confere.
- Tabelas 4 e 13 e o texto da Seção 5.3, contra `hipoteses-V5-STRATEGY.md`: os 28 vereditos; H3 com 10 pares melhores e 7 piores; Haiku 4.5 com −5 em H3; saldos de H1 (+1, +2, +3, +2, +1, +5; 14 melhores e 0 piores); H23 com 2 pares, ambos com exagero no N2; H25 decidida pelo Haiku 4.5 (3 de 5); H26 com 3 melhores e 0 piores; perfis distintos (7 menos, 3 iguais, 1 mais); sinal dos *tokens* (6 e 5); Page (p = 0,007 e p < 0,001).
- *Tokens* do revisor: refiz H27 com o consumo total de `usage_by_model` nos `meta.json`. O Opus 4.6, o 4.7 e o 4.8 continuam com mais pares piores, e a hipótese segue inconclusiva, como o texto diz.
- *Skill* do estudo principal: nenhuma das 110 execuções do N2 e do N3 chamou a ferramenta de *skills* nem leu os arquivos da *skill* (só um `ls` da pasta, no Opus 5.5).
- Tabela 7 é a soma da Tabela 14 em todas as células; a linha *pattern* é a soma da Tabela 8 (41, 53, 60 e 19, 33, 58); as Figuras 9 e 10 mostram os mesmos valores.
- Categorias do verificador, a partir de `TCC_V1/evaluation/quality/results/summary.md`: 225 acertos, 39 alternativas, 89 malfeitos e 97 omissões; os totais da Tabela 10 (126, 28, 29, 41 e 99, 11, 60, 55); 45, 53, 72 e 48, 66, 69 com o malfeito; os 26 malfeitos do N0 da manutenção (12, 8, 4, 2); o Observer da manutenção (27 malfeitos em 45); as 12 execuções com malfeito no Strategy da construção.
- Tabela 10, cruzando `summary.md` com `oo-metrics.csv`: todas as médias conferem. Médias de métodos abstratos no N2 por *pattern* (4,87 e 3,60; 4,20 e 2,13; 0,33 e 0): conferem.
- Tabela 9, a partir de `oo-metrics.csv`: todas as células conferem; a conta aproximada do CBO vezes os tipos dá +22% a +46% no estudo principal e +3% a +27% na extensão.
- Primeira rodada da extensão: o Decorator teve o menor número de execuções com o *pattern* certo (7 de 18) e o Factory Method o maior (16 de 18), como o texto diz ao justificar a ordem das repetições.
- O Sonnet 5.5 no Factory Method da manutenção: 0 de 5 no N0 e 5 de 5 no N1 e no N2. Comportamento bom por modelo e nível na manutenção: 16, 17, 13; 20, 17, 16; 25, 25, 25. Comportamento ruim na construção: 71 (Haiku 4.5) e 32 (Sonnet 4.5).
- Segundo verificador, recalculado com `second_verifier.py agreement`: 195 de 210; 30, 30, 29, 29, 27, 26, 24 por coluna; kappa de 0,53 no *design*, o mais baixo; as 15 diferenças são todas um degrau mais brandas.
- Tabela 16 soma 89, 96, 109 por modelo, 96, 103, 95 por nível, 294 de 360 e 54 combinações estáveis. Tabela 12 soma 30, 30 e 28. Tabela 11 dá US$ 380 nas 450 execuções.
- Outros fatos do método: `CLAUDE.md` do N1 com 14 linhas; 122 *hashes* no `CONGELADO-V5.sha256`; *commit* 54e13fc9 de 10/10; base da manutenção com 26 testes e uma migração; Javalin 6.7.0 e Flyway 12.4.0 no `pom.xml`; Claude Code 2.1.288 nas transcrições da extensão; *scripts* citados presentes nas pastas indicadas.

**5. Termos centrais.** "Configuração", "orientações persistentes", "nível", "teto", "saldo", "ponto do clube", "ponto do seguro", "exagero", "*pattern* certo", "categoria acerto", "estrutura desnecessária" e "contenção" mantêm o mesmo nome e o mesmo sentido. Onde o mesmo nome poderia confundir (exagero no estudo principal e nos controles; N1 e N2 nos dois estudos; certo no Semgrep e no verificador), o texto avisa a diferença (Tabela 2, Seção 6.2 e Discussão da P1). A expressão "não apoiada" é definida na Seção 5.1; o apontamento 2 trata de um trecho que vai além dela.

**6. Proporção.** Método e resultados ocupam a maior parte do texto; a Fundamentação e os Trabalhos relacionados são curtos e usados depois.

**7. Percurso de cada objetivo.** As oito perguntas e o controle negativo têm método, resultado e resposta explícita (matriz na seção 6).

**8. Proveniência dos resultados.** Cada resultado tem etapa de origem: suíte, Semgrep, conferência humana, SonarQube e CK (Seção 5.1); verificador, roteiro de comportamento, JaCoCo, jdeps e `pg_dump` (Seção 6.2); controles (Seção 6.2); custo, *tokens*, tempo e turnos do registro do Claude Code (Seções 5.1 e 6.2); abertura da *skill* pela transcrição (Seção 5.1); estabilidade (Seções 5.1 e 6.2); segundo verificador (Seção 6.2).

**9. Justificativa das escolhas.** As escolhas que mudam a leitura dos resultados têm motivo declarado: amostra de modelos, número de rodadas, limites das regras de decisão, critérios das notas, exclusão do N3 na extensão, ordem das repetições, ausência de Docker nos contêineres, segunda rodada do controle, verificador ciente do controle, testes não pedidos na construção, migrações exigidas na construção.

## 4. Apontamentos por critério

**Critério 1. Língua portuguesa e redação.** Sem apontamentos. Não encontrei erro de ortografia, concordância ou regência, marcador pendente nem os padrões de escrita do roteiro.

**Critério 2. Encadeamento.** Sem apontamentos. As perguntas P1 a P8 são retomadas com o mesmo código nas Seções 5.3, 6.3, 7 e 9.

**Critério 3. Terminologia e siglas.** Sem apontamentos.

**Critério 4. Resumo e Abstract.** Sem apontamentos. Todos os números do Resumo conferem com as tabelas e com os dados, e o Abstract traduz o Resumo frase a frase.

**Critério 5. Introdução.** Sem apontamentos. O texto não menciona o AYTY, então o ponto do roteiro sobre o laboratório não se aplica.

**Critério 6. Coerência metodológica.**

**Apontamento 1 (SUGESTÃO).** Regra da execução sem entrega diferente do que foi aplicado.

- *Onde:* Seção 6.2, segundo parágrafo depois da Figura 8, "a execução que não entrega nada recebe ruim onde não há o que avaliar"; e Seção 6.3, parágrafo da P1, "a execução recebeu ruim em todas as dimensões".
- *Motivo:* na execução da manutenção do Strategy (N2, Haiku 4.5, primeira rodada) nenhuma linha mudou, mas a base continuou lá, com arquitetura, migração e 26 testes que poderiam ser avaliados. O `review.json` dessa execução tem ruim em arquitetura, *design*, testes, banco e comportamento. A instrução do verificador (`agent/quality-reviewer.md`) trata "nenhuma linha mudada" como não entrega, o que explica a nota, mas o texto da 6.2 descreve uma regra mais estreita ("onde não há o que avaliar"). O efeito é de uma execução em 75 no N2 da manutenção e não muda a direção de nenhuma comparação da Tabela 7; a Discussão da P6 já cita essa execução no comportamento.
- *Correção:* deixe a regra da 6.2 igual à que foi aplicada, dizendo que, na manutenção, a execução sem nenhuma linha mudada também recebeu ruim em todas as dimensões.

**Critério 7. Solução, resultados e conclusão.** Sem apontamentos. Cada pergunta tem resposta explícita na Seção 7, e a Conclusão repete os números e as ressalvas da Discussão.

**Critério 8. Figuras, tabelas e código.** Sem apontamentos fora dos pontos protegidos. As 10 figuras e as 16 tabelas têm número, título e fonte e são apresentadas no texto. As Figuras 9 e 10 têm eixo com rótulo, escala e legenda, e os valores conferem com as Tabelas 7 e 8.

**Critério 9. Consistência de conteúdo.**

**Apontamento 2 (RECOMENDADA).** Afirmação de ausência de efeito mais forte do que as hipóteses H17 e H18.

- *Onde:* Seção 7, quarta pergunta (P4), segundo parágrafo, "o `CLAUDE.md` não alterou o tempo de API nem os turnos (H17 e H18)".
- *Motivo:* a Seção 5.1 diz que, nas hipóteses de alteração, "não apoiada" significa que "a alteração prevista não apareceu", e a Tabela 4 usa essa expressão. Nos dados, os pares se dividiram (lado maior com 30 e 31 de 55, `hipoteses-V5-STRATEGY.md`), mas as médias subiram do N0 para o N1: turnos de 31,0 para 35,3 e tempo de API de 5,8 para 6,2 minutos (`resultados-V5-STRATEGY.md`, seção 6). Dizer que o arquivo "não alterou" essas medidas afirma ausência de efeito, o que a regra não mostra. Em outros pontos o texto usa a cautela certa: "sem alteração detectada" no exagero (H2) e "não houve alteração consistente entre os pares" no acoplamento e na coesão (H9 e H10).
- *Correção:* use na P4 a mesma formulação da Tabela 4 e da Seção 5.3, falando em alteração não apoiada ou não detectada entre os pares.

**Apontamento 3 (SUGESTÃO).** Métricas atribuídas aos dois estudos, medidas só em um.

- *Onde:* Seção 2, parágrafo sobre o CK, "Somam-se as contagens de tipos (classes, interfaces, *enums*, *records*) e de métodos abstratos, que mostram o uso de polimorfismo, e, no estudo principal, as de constantes de *enum* com corpo próprio e de classes anônimas".
- *Motivo:* a frase põe os métodos abstratos entre as medidas dos dois estudos e restringe ao estudo principal só as constantes de *enum* e as classes anônimas. O `metricas.csv` do estudo principal não tem coluna de métodos abstratos, a Tabela 6 não tem essa linha, e a Discussão da P8 diz "métodos abstratos (na extensão, sobretudo com a *skill*)". O leitor que procurar essa medida no estudo principal não a encontra.
- *Correção:* indique na frase da Seção 2 que os métodos abstratos são contados na extensão.

**Critério 10. Normas e referências.** Sem apontamentos. As 43 referências da lista são todas citadas, e todas as citações do texto estão na lista. Sem rede, conferi os DOIs pelos metadados do Crossref registrados no projeto em 10/11 (`doi-check.txt`): 25 dos 26 levam ao trabalho citado, com o mesmo primeiro autor, ano e título. O DOI de Vokáč et al. (2004) não foi resolvido nessa consulta; o formato é o usual dos artigos da Kluwer em *Empirical Software Engineering*, então não há indício de erro, mas abra esse DOI no navegador antes de entregar. Não puderam ser conferidas por DOI as referências sem DOI (Brown et al. 2020, Vaswani et al. 2017, Yao et al. 2023, os livros e as páginas de ferramentas). Os links têm data de acesso e não têm parâmetro de rastreio, e não há citação direta de livro.

**Critério 11. Tecnologias e repositório.** Sem apontamentos. A Tabela 3 traz nome, versão e papel de cada tecnologia, e o link do repositório (https://github.com/FLucasF/TCC) está no texto. Sem rede, não testei se o link abre nesta rodada; a rodada 14 registrou resposta 200.

**Contagem.** OBRIGATÓRIA: 0. RECOMENDADA: 1 (apontamento 2). SUGESTÃO: 2 (apontamentos 1 e 3).

## 5. Matriz de rastreabilidade

| Objetivo / pergunta | Onde é desenvolvido | Etapa do método que gera os dados | Resultado (seção, tabela) | Resposta explícita (onde) | Situação | Ação pendente |
|---|---|---|---|---|---|---|
| Objetivo geral: efeito de orientações persistentes sobre a decisão de aplicar um *pattern*, na construção e na manutenção | Seções 5 e 6 | Os dois estudos (5.1 e 6.2) | 5.3 e 6.3 | Seção 9, primeiro e segundo parágrafos | atendido | nenhuma |
| P1. Uso do *pattern* esperado | 5.3 e 6.3 | Semgrep e conferência humana (5.1); categoria do verificador (6.2) | Tabelas 5, 7, 8 e 13; Figuras 9 e 10 | Seção 7, P1; Seção 9 | atendido | nenhuma |
| P2. Exagero, corretude e qualidade | 5.3 e 6.3 | Semgrep no ponto do seguro, suíte, SonarQube (5.1); estrutura desnecessária e controles (6.2) | Tabelas 4, 5, 7, 12 e 14 | Seção 7, P2; Seção 9 | atendido | nenhuma |
| Controle negativo da manutenção (com a P2) | 6.2 e 6.3 | Cenários de controle e verificador ciente do controle (6.2) | Tabela 12 | Seção 7, parágrafo do controle; Seção 9 | atendido | nenhuma |
| P3. Variação entre modelos | 5.3 e 6.3 | Pares por modelo (5.1); resultados por modelo (6.2) | Tabelas 13 e 14 | Seção 7, P3; Seção 9 | atendido | nenhuma |
| P4. Contribuição de cada componente e custo | 5.3 e 6.3 | Comparações N2 × N1 e N3 × N2, transcrição e registro de custo (5.1 e 6.2) | Tabelas 4, 5 e 11 | Seção 7, P4; Seção 9 | atendido | ajustar a cautela de um trecho (apontamento 2) |
| P5. Tipo de tarefa | 6.3 | Duas tarefas com os mesmos níveis e modelos (6.2) | Tabelas 7 e 8; Figura 10 | Seção 7, P5; Seção 9 | atendido | nenhuma |
| P6. Demais dimensões de qualidade | 6.3 | Verificador com uma *skill* por dimensão (6.2) | Tabelas 7 e 14 | Seção 7, P6; Seção 9 | atendido | opcional: regra da execução sem entrega (apontamento 1) |
| P7. Estabilidade | 5.3 e 6.3 | Perfis do Semgrep por rodada (5.1); repetição da primeira rodada (6.2) | Seção 5.3; Tabela 16 | Seção 7, P7; Seção 9 | atendido | nenhuma |
| P8. Estrutura orientada a objetos | 5.3 e 6.3 | CK nos dois estudos (Seção 2, 5.1 e 6.2) | Tabelas 6, 9 e 10 | Seção 7, P8; Seção 9 | atendido | opcional: descrição das métricas (apontamento 3) |

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
| Estrangeirismos em itálico | atendido |

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
| Objetivo geral e perguntas claros, numerados e verificáveis | atendido |
| Os objetivos correspondem ao que foi feito | atendido |
| Contexto institucional explicado | atendido (TCC da Licenciatura, UFPB; orientador na Seção 4) |
| Parágrafo de organização corresponde às seções | atendido |

**Critério 6. Metodologia**

| item | situação |
|---|---|
| Cada etapa diz o que, como, com quais ferramentas e quem | atendido |
| Sequência e dependências claras | atendido (Figura 3 e Seção 6.2) |
| Cada escolha tem motivo declarado | atendido |
| Todo resultado tem etapa de origem | atendido |
| Toda etapa aparece nos resultados, e toda atividade está no método | parcial (apontamento 1: regra da execução sem entrega) |
| Versões das ferramentas informadas | atendido |

**Critério 7. Solução, resultados e conclusão**

| item | situação |
|---|---|
| Solução descrita com métricas e artefatos | atendido |
| Resultados concretos e organizados pelas perguntas | atendido |
| Cada pergunta tem resposta explícita | atendido |
| Nenhum resultado desligado dos objetivos | atendido |
| A conclusão mantém as ressalvas da discussão | atendido |
| Limitações e trabalhos futuros específicos | atendido |

**Critério 8. Figuras, tabelas e código**

| item | situação |
|---|---|
| Toda figura e tabela tem número, título e fonte | atendido |
| Toda figura e tabela é apresentada no texto | atendido |
| Gráficos com eixos, rótulos e legenda | atendido |
| Código em texto, nenhum em imagem | pendente pelo roteiro; decisão sua, protegida (Figuras 2, 5 a 8) |
| Nenhuma imagem de código com fundo escuro | atendido (fundo branco) |
| A mesma informação não aparece em tabela e figura | pendente pelo roteiro; decisão sua, protegida (Figuras 9 e 10) |

**Critério 9. Consistência de conteúdo**

| item | situação |
|---|---|
| O mesmo número tem o mesmo valor em todo o texto | atendido |
| Resumo, Introdução e Conclusão com a mesma cautela | atendido |
| Sem contradição entre seções | parcial (apontamentos 2 e 3) |
| Sem trechos genéricos sem ligação com o trabalho | atendido |
| Sem etapas citadas uma vez e nunca mais | atendido |
| Estilo uniforme | atendido |

**Critério 10. Normas e referências**

| item | situação |
|---|---|
| Toda citação está na lista, e toda referência é citada | atendido |
| Abri cada DOI e ele leva ao trabalho citado | parcial (25 de 26 conferidos pelos metadados; abra o de Vokáč et al. 2004) |
| Sem marcador pendente | atendido |
| Citações diretas de livro com página; links com data de acesso | atendido |
| Numeração consistente e elementos do modelo | atendido |

**Critério 11. Tecnologias e repositório**

| item | situação |
|---|---|
| Tecnologias em tabela, com versão e papel | atendido |
| Link do repositório no texto e aberto a qualquer pessoa | atendido (não testado nesta rodada, sem rede) |

**Revisão do texto inteiro e matriz**

| item | situação |
|---|---|
| Leitura do texto inteiro | atendido |
| Matriz completa, sem "não encontrado" | atendido |

**Uso de ferramentas de IA**

| item | situação |
|---|---|
| Declaração de uso depois da Conclusão | pendente; decisão sua de escrever por último |
| IA usada como instrumento está na metodologia | atendido (Seção 4, Tabela 1 e Seção 6.2) |
| Conferi todo conteúdo que uma ferramenta sugeriu | parcial; os dois ajustes do Fable nos controles foram aplicados sem sua revisão, e o texto declara isso (decisão sua, protegida) |
