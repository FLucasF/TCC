Autoavaliação feita com ferramenta de IA. A avaliação oficial é do orientador.

# Autoavaliação antes da entrega: artigo, versão 139 (r4)

Arquivo revisado: `docs/artigo-v5-e-extensao.md` (versão 139), com as dez figuras de `docs/figuras` abertas como imagem. Os critérios usados são orientações do professor Rodrigo Rebouças de Almeida para os orientandos dele, e não critérios oficiais da disciplina de TCC, do curso ou da UFPB; quando houver diferença, valem a resolução e as normas oficiais. O uso desta revisão é uso de ferramenta de IA e entra na declaração de uso do TCC (seção 8 do guia).

Os números das tabelas foram somados entre si e conferidos contra os dados do repositório (`construction-v5/analysis`, `TCC_V1/evaluation/quality/results`, `TCC_V1/*/runs/*/meta.json` e `claude-output.jsonl`). Os 26 DOIs foram conferidos na API do Crossref e os 23 links foram abertos.

## Pontos já decididos pelo autor

Estes pontos não contam como pendência neste relatório.

1. As figuras de código (Figuras 2, 5, 6, 7 e 8) ficam como imagem, com fundo branco, como combinado com o orientador. As imagens conferidas têm fundo branco. O apontamento 8.1 trata só do conteúdo do código da Figura 8, não do formato.
2. A declaração de uso de ferramentas de IA será escrita por último pelo autor.
3. As Figuras 9 e 10 ficam ao lado das Tabelas 7 e 8, mesmo repetindo os dados.
4. O nome da hipótese H8 ("gera mais arquivos", medida pelo número de classes) segue o pré-registro congelado.
5. A coluna "como foi conferido" da Tabela 1 foi conferida e confirmada pelo autor.
6. O título continua o mesmo.

## Pontos a tratar

Contagem: 2 obrigatórios, 15 recomendados e 19 sugestões. O roteiro de revisão trata os dois primeiros como obrigatórios. Os recomendados estão em ordem de peso.

Obrigatórios:

- **6.1** A estabilidade da extensão é medida contra a primeira rodada sem motivo declarado, e essa rodada é diferente das outras (data e uso na escolha dos *patterns* repetidos).
- **6.2** O verificador dos controles negativos foi informado de que o cenário era um controle, sem motivo declarado, e o resultado de 88 de 90 depende dessa leitura.

Recomendados, por peso:

- **9.1** A Discussão afirma com menos cautela que o Resumo e a Conclusão (exagero no ponto do seguro e mecanismo da P8).
- **7.1** A recomendação prática da Conclusão deixa de fora a queda do comportamento na manutenção com a *skill* e a ressalva da calibração.
- **7.2** A resposta à P7 junta dois estudos que apontam em direções diferentes, com medidas diferentes, sem dizer isso.
- **3.1** "Acerto" e "certo" têm sentidos diferentes nos dois estudos, e o Resumo e a P1 comparam os dois sob o mesmo nome.
- **9.2** O artigo diz que o pagamento é um ponto que pede o Strategy; o pré-registro diz que o enunciado não dá motivo para separar as formas de pagamento.
- **6.5** Não está dito quem escreveu os instrumentos (as 17 versões defeituosas, as soluções de referência, os exemplos degradados).
- **3.2** "Cinco dimensões" designa conjuntos diferentes na Introdução, na Seção 6.2 e na Tabela 7, e a frase das contribuições sugere uma calibração mais ampla do que a feita.
- **5.1** As perguntas P1 a P8 estão num único parágrafo, escritas como "busca-se verificar como", e não como perguntas numeradas e localizáveis.
- **6.6** A medida de semelhança entre rodadas do estudo principal só é definida nos resultados.
- **2.1** O segundo parágrafo da Seção 5.3 mistura P3, P7, P4, H25, H26, H7 e custo.
- **6.4** A escolha de N1 × N0 como comparação das hipóteses principais não tem motivo no texto.
- **6.3** As falhas de ambiente (contêiner sem Docker) contam na nota de testes sem motivo explícito.
- **1.1** Frases longas encadeiam vários resultados (Conclusão, Seção 5.3, Seção 8).
- **7.5** Um valor da Tabela 11 (tokens de saída, N2 da construção) não foi reproduzido a partir dos dados brutos.
- **1.2** Os tempos verbais dos resultados mudam entre a Seção 5.3 (passado) e a Seção 6.3 e a Discussão (presente).

## Leitura macro

### 1. Mapa das seções

| seção | o que entrega | o que prepara |
|---|---|---|
| Resumo e Abstract | problema, objetivo, os dois estudos, números principais (67% para 93%, 9 de 10, 88 de 90, 4 para 9) | |
| 1. Introdução | LLM, agente, *harness*, os três mecanismos (`CLAUDE.md`, *skill*, processo), *patterns*, exagero, construção e manutenção, relevância, problema, objetivo geral, P1 a P8, contribuições, organização | as perguntas e os conceitos usados no resto |
| 2. Fundamentação | encapsulamento do que varia, polimorfismo, aberto/fechado, acoplamento e coesão, métricas do CK, complexidade cognitiva | as medidas das Seções 5 e 6 e a leitura da P8 |
| 3. Trabalhos relacionados | Kim, Kjellberg, Zisa, Ampatzoglou, Chatlatanagulchai, Arabat e Sayagh, Jiang e Nam, Kim (2026), e a lacuna | a diferença do trabalho: decisão não pedida, níveis de configuração |
| 4. Visão geral do processo | fases (Figura 3), desenho (Figura 4), repositório, papéis do autor e do orientador, IA (Tabela 1), níveis (Tabela 2), tecnologias (Tabela 3), infraestrutura | os dois estudos |
| 5. Estudo principal | método, regras de decisão, 28 hipóteses (Tabela 4), proteção contra decisões posteriores, resultados (Tabelas 5 e 6) | P1 a P4, P7 e P8 |
| 6. Extensão | motivação e mudanças do desenho, método (verificador, calibração, rodadas, controles), resultados (Tabelas 7 a 12, Figuras 9 e 10) | P1 a P8 |
| 7. Discussão | uma resposta por pergunta, de P1 a P8, e a trajetória do estudo | a Conclusão |
| 8. Limitações | ameaças gerais, do estudo principal, da extensão, do controle, do verificador, da execução e das métricas | trabalhos futuros |
| 9. Conclusão | retomada do objetivo, resultados por pergunta, recomendação prática, trabalhos futuros | |
| Apêndice A | Tabelas 13 a 16 por modelo | |

O parágrafo de organização da Introdução corresponde às seções que existem, na mesma ordem, e anuncia o Apêndice A. Não há seção anunciada que falte nem seção que não tenha sido anunciada.

### 2. Linha do argumento

A sequência problema, objetivo, método, resultado e conclusão se fecha: a pergunta da Introdução ("como a configuração do *harness* ... muda a forma como ele aplica *design patterns*") é atendida pelos dois estudos, e a Discussão responde pergunta por pergunta. Os conceitos da Seção 2 são usados depois (polimorfismo e métricas na P8, aberto/fechado na P8, complexidade cognitiva em H4). Os pontos fracos do encadeamento estão nos apontamentos 2.1 (resultados do estudo principal sem organização por pergunta), 2.2 (termos usados antes da definição) e 7.2 (P7).

### 3. Resumo, Introdução e Conclusão

Os três prometem e afirmam a mesma coisa e usam os mesmos números (220, 450, 90, 67% para 93%, nove de dez, 88 de 90, 4 para 9 em 75). O Resumo e a Conclusão mantêm a ressalva sobre o Haiku 4.5 e usam "sem alteração detectada" para o exagero. A Discussão é que fica mais forte em dois pontos (apontamento 9.1). O Resumo termina com uma frase verdadeira por construção (apontamento 4.1).

### 4. Números repetidos

Conferidos e iguais em todos os lugares em que aparecem:

- Tabela 7, linha do *pattern* (41, 53, 60; 19, 33, 58) = soma da Tabela 8 por tarefa e nível = soma da Tabela 14 por tarefa e nível = Figuras 9 e 10. As demais linhas da Tabela 7 também fecham com a soma da Tabela 14 (*design* do restante, arquitetura, testes, banco, comportamento, estrutura desnecessária 4, 5, 9 e 0, 1, 2).
- Tabela 10: 126 + 28 + 29 + 41 = 224 e 99 + 11 + 60 + 55 = 225 (449 com código); acerto + alternativa = 154 na construção e 110 na manutenção, iguais à soma da Tabela 7.
- Tabela 13: 37, 51, 51 e 54 acertos em 55 dão 67%, 93%, 93% e 98% (Tabela 5); saldos H1 somam +14 (14 pares melhores, nenhum pior); saldos H3 somam +3.
- Tabela 15: soma igual às linhas do Decorator da Tabela 8; 11 execuções na categoria acerto com a *skill* (10 do Sonnet 5.5 e 1 do Sonnet 4.5).
- Tabela 16: 89, 96 e 109 por modelo; 96, 103 e 95 por nível; 294 de 360 (81,7%, arredondado para 82%); 54 de 90 combinações.
- Tabela 12: 30, 30 e 28; 88 de 90.
- Custos: 55 × (1,32 + 1,52 + 1,47 + 1,98) = US$ 346; 75 × (0,96 + 1,06 + 1,07 + 0,60 + 0,66 + 0,72) = US$ 380.
- Discussão P1: +18 (Decorator e Template Method) e +8 (outros três); 15 para 14 no Strategy; 4, 2 e 4 em 10 sem o Sonnet 5.5.
- Discussão P8: CBO × tipos refeito com as Tabelas 6 e 9: +22% a +46% no estudo principal e +3% a +27% na extensão.
- Seção 8: 195 de 210 notas iguais, kappa de 0,53 no *design* e as 15 diferenças sempre um degrau mais brandas, refeitos a partir dos arquivos `review.json` dos dois verificadores.
- Seção 6.3: 59 reservas duplicadas e 16 execuções sem o teste na construção; nota ruim de comportamento em 71 (Haiku 4.5) e 32 (Sonnet 4.5); *skill* aberta em 69 de 75 nas duas tarefas e em 20 de 30 nos controles, todos refeitos a partir dos dados.
- Tabela 4: os 28 vereditos coincidem com `construction-v5/analysis/hipoteses-V5-STRATEGY.md` ("contrariada" nas hipóteses de alteração aparece como "não apoiada", como o texto declara).

Diferença encontrada: Tabela 11, *tokens* de saída do N2 da construção (apontamento 7.5).

Não conferidos contra os dados: as Tabelas 6 e 9 e as colunas de métricas da Tabela 10, as porcentagens de frete, cupom e pagamento da Tabela 5, as 17 versões defeituosas, as 119 execuções da bancada e as 11 execuções sem Docker.

### 5. Termos centrais

| termo | situação |
|---|---|
| configuração, orientação, orientações persistentes | sinônimos declarados na Introdução, mas alternados ("níveis de orientação" no Resumo, "níveis de configuração" na Introdução): apontamento 3.3 |
| *harness* | definido na Introdução; o texto avisa que o pré-registro usa o termo em outro sentido |
| exagero, estrutura desnecessária, contenção | distinguidos na Seção 6.2; uso coerente no restante |
| acerto, certo, *pattern* certo, categoria acerto | sentidos diferentes nos dois estudos: apontamento 3.1 |
| dimensão | conjuntos diferentes: apontamento 3.2 |
| teto | definido no fim do parágrafo em que já foi usado: apontamento 2.2 |
| lote, agente | cada um designa mais de uma coisa: apontamento 3.4 |
| P1 a P8 | o mesmo código do começo ao fim; a Conclusão põe a queda do comportamento na P2, e a Discussão, na P6: apontamento 7.3 |

### 6. Proporção

Palavras de texto corrido, sem tabelas, de 14.464 no total da Introdução à Conclusão: Introdução 11%, Fundamentação 5%, Trabalhos relacionados 4%, método (Seções 4, 5.1, 5.2, 6.1 e 6.2) 37%, resultados (5.3 e 6.3) 14%, Discussão 16%, Limitações 9%, Conclusão 4%. O Resumo tem 207 palavras e o Abstract, 210, dentro de 150 a 250.

A Fundamentação e os Trabalhos relacionados ficam abaixo da faixa do guia, mas entregam o que é usado depois. O método fica acima, o que corresponde ao papel dele num trabalho experimental, em que o "desenvolvimento" é o próprio experimento. As Limitações têm cerca de cinco vezes a "meia página" do guia, mas são específicas do trabalho. Não há apontamento de proporção.

### 7. Percurso de cada objetivo

Está na matriz de rastreabilidade, no fim deste relatório. Todas as perguntas têm desenvolvimento, etapa de método, resultado e resposta na Seção 7 e na Seção 9; a P7 tem resposta parcial (apontamento 7.2).

### 8. Proveniência dos resultados

| resultado | etapa de origem |
|---|---|
| acerto nos pontos do clube, frete, cupom e pagamento; exagero no seguro (Tabelas 5 e 13) | Semgrep, Seção 5.1; conferência humana em 44 execuções |
| suíte inteira, casos aprovados, *build* (Tabela 5, H3, H11 a H14) | suíte de aceitação, Seção 5.1 |
| complexidade, linhas, duplicação, *code smells* | SonarQube, Seção 5.1 |
| Tabela 6, Tabelas 9 e 10 | CK, Seções 2, 5.1 e 6.2 |
| custo, *tokens*, tempo, turnos (Tabelas 5 e 11) | transcrição do Claude Code, Seções 4 e 5.1 |
| abertura da *skill* e uso do revisor | transcrição, `extract-meta.mjs` e `run_one_scenario.py`, Seção 5.1 |
| teste de Page | Seção 5.1 |
| perfis distintos do Semgrep entre rodadas (P7, estudo principal) | a etapa só é nomeada ("semelhança entre rodadas"); a medida aparece nos resultados: apontamento 6.6 |
| Tabelas 7, 8, 14 e 15 | verificador de IA, Seção 6.2 |
| estabilidade (Tabela 16) | Seção 6.2, comparação com a primeira rodada: apontamento 6.1 |
| reservas simultâneas, migrações | roteiro de comportamento e dimensão do banco, Seção 6.2 |
| Tabela 12 | controles negativos, Seção 6.2: apontamento 6.2 |
| concordância com o segundo verificador (Seção 8) | Seção 6.2 |
| "o Haiku 4.5 a abriu em apenas 1 de 5 vezes" (Seção 6.2) | "execuções preliminares", sem a fase nomeada: apontamento 6.7 |

Não encontrei resultado sem nenhuma etapa de origem.

### 9. Justificativa das escolhas

Com motivo declarado: o Strategy, a *skill* pública, os 11 modelos e a exclusão do Fable, as cinco rodadas, o esforço *medium*, os quartetos simultâneos, os limites de 7 em 55, 36 em 55 e 6,3%, a regra de saída de 40 em 44, a troca do Spring pelo Javalin, o banco de dados, a *skill* própria, a frase acrescentada a três enunciados, a exclusão do N3 na extensão, os três modelos, o Opus 5.5 como verificador, a ordem das repetições, as 30 revisões do segundo verificador, a segunda rodada dos controles, as quatro execuções refeitas.

Sem motivo declarado: a comparação com a primeira rodada na estabilidade (6.1), o verificador informado do controle (6.2), as falhas de ambiente contadas na nota de testes (6.3, motivo só sugerido), N1 × N0 como comparação principal (6.4).

## Apontamentos por critério

### Critério 1. Língua portuguesa e redação

Frases longas, com vários resultados encadeados, obrigam o leitor a reler. Um texto técnico fica mais claro com uma informação principal por frase.

**1.1 RECOMENDADA. Frases longas que encadeiam vários resultados.**
Onde: Seção 9, segundo parágrafo, frase "Quanto aos custos, o comportamento caiu na manutenção ... (P2)", com três resultados (comportamento, estrutura desnecessária, controles); Seção 5.3, segundo parágrafo, frase "Quanto às demais questões descritivas, ... queda em 5", com dois resultados ligados por ponto e vírgula; Seção 8, quinto parágrafo, frase "O número de cinco rodadas ... sendo de ao menos 1.000 a recomendação geral ...".
Motivo: na Conclusão, a parte que mais leitores leem sozinha, o leitor precisa separar três achados de uma frase só.
Correção: divida cada frase em uma por resultado. Exemplo com a sua frase:
Antes: "Quanto aos custos, o comportamento caiu na manutenção (de 61 para 54 execuções boas em 75), e, na construção com a *skill*, a estrutura desnecessária, também fora da calibração, subiu de 4 para 9 execuções em 75, ..."
Depois: "Na manutenção, o comportamento caiu de 61 para 54 execuções boas em 75. Na construção com a *skill*, a estrutura desnecessária, também fora da calibração, subiu de 4 para 9 execuções em 75."

**1.2 RECOMENDADA. Tempo verbal dos resultados.**
Onde: a Seção 5.3 relata no passado ("passou de 0 para 5", "teve efeito apoiado"); a Seção 6.3 relata no presente ("Do N0 ao N2, o *pattern* certo aumenta em nove das dez combinações", "Os métodos abstratos aumentam com a orientação"); a Discussão, P2, também ("A corretude não piora, e a complexidade diminui").
Motivo: o guia (seção 6) pede passado para o que foi observado e presente para o que se conclui; a mistura faz o leitor hesitar entre fato medido e conclusão geral.
Correção: passe para o passado os resultados observados das Seções 6.3 e 7 e deixe no presente só a interpretação.

**1.3 SUGESTÃO. Vírgula antes do "e" final em enumerações.**
Onde: Seção 1, contribuições: "sobre o que esperar delas, e sobre o custo que podem trazer"; Seção 6.3: "de 4,20 e 2,13 no segundo, e de apenas 0,33 e 0 no Decorator".
Motivo: é sintaxe do inglês quando o "e" fecha uma enumeração de complementos.
Correção: retire a vírgula antes do "e" nesses dois trechos.

**1.4 SUGESTÃO. Construção quebrada.**
Onde: Seção 1, nono parágrafo: "O estudo principal tem pré-registro, isto é, com o protocolo e as regras de decisão fixados antes da coleta dos dados".
Motivo: depois de "tem pré-registro", o "isto é, com" não completa a frase.
Correção: Antes: "O estudo principal tem pré-registro, isto é, com o protocolo e as regras de decisão fixados antes da coleta dos dados". Depois: "O estudo principal tem pré-registro, isto é, o protocolo e as regras de decisão foram fixados antes da coleta dos dados".

Não encontrei declarações de importância sem fato, gerúndio de comentário, "atua como", "conta com", contrastes montados, fórmulas de enchimento, títulos com maiúscula em todas as palavras nem campos pendentes.

### Critério 2. Encadeamento lógico e coerência textual

**2.1 RECOMENDADA. Resultados do estudo principal sem organização por pergunta.**
Onde: Seção 5.3, segundo parágrafo, de "O ganho concentrou-se nos modelos fora do teto" até "O lote custou US$ 346 em equivalente de API".
Motivo: o parágrafo junta P3 (modelos fora do teto), P2 (corretude do Haiku 4.5), P1 (Page), P4 (H21, H23, revisor), P7 (perfis), custo (*tokens*), H25, H26 e H7. O guia (4.10) pede os resultados organizados pelos objetivos ou perguntas, para o leitor saber a qual pergunta cada um responde. A Seção 6.3 já faz isso melhor, com parágrafos por assunto.
Correção: quebre o parágrafo em blocos por pergunta (por exemplo, um para P3, um para P4 e o N3, um para P7) e abra cada bloco dizendo a pergunta.

**2.2 SUGESTÃO. Termos usados antes da definição.**
Onde: Seção 5.1, quarto parágrafo: "a maioria dos modelos fora do teto melhora" aparece antes de "Considera-se 'teto' o modelo que ...", no fim do mesmo parágrafo; a Figura 1, na Introdução, já usa "nível N0", e a Figura 4 usa N0 a N3 antes da Tabela 2, que define os níveis.
Motivo: o leitor encontra o termo antes da definição.
Correção: mova a definição de teto para o começo do parágrafo e cite, na Introdução, que os níveis vão de N0 a N3 (o N0 sem configuração).

**2.3 SUGESTÃO. Trabalhos relacionados sem tabela comparativa.**
Onde: Seção 3.
Motivo: a seção compara sete trabalhos em vários critérios (agente ou não, *pattern* nomeado ou não, orientação persistente, controle negativo, manutenção, modelos de uma ou mais empresas); o guia (4.6 e Apêndice C) sugere uma tabela para o leitor ver as diferenças de uma vez.
Correção: considere uma tabela com esses critérios, incluindo a linha "este trabalho".

### Critério 3. Terminologia e siglas

Em texto técnico, cada conceito tem um nome, e esse nome não muda. Quando o mesmo nome designa duas coisas, o leitor não sabe de qual se fala.

**3.1 RECOMENDADA. "Acerto" e "certo" com sentidos diferentes nos dois estudos.**
Onde: no estudo principal, "acerto no ponto do clube" (Tabela 4) e "ponto do clube certo" (Tabela 5) são a classificação do Semgrep pelos dois critérios da Seção 5.1; na extensão, "*pattern* certo soma as categorias acerto e alternativa equivalente" (Seção 6.2), e "categoria acerto" é só uma delas. O Resumo junta os dois sob "uso correto" ("o uso correto aumentou ... em nove de dez combinações"), e a Discussão, P1, compara "o ponto do clube passou de 67% para 93%" com "de 19 para 33 e 58 execuções com o *pattern* certo".
Motivo: o leitor entende que a mesma medida subiu nos dois estudos, quando os critérios são diferentes (no estudo principal, uma alternativa equivalente não contaria como certa).
Correção: defina "acerto" na Seção 5.1 (os dois critérios do Semgrep) e diga, na P1 da Discussão, em uma frase, que as duas medidas não são iguais.

**3.2 RECOMENDADA. "Cinco dimensões" designa conjuntos diferentes.**
Onde: Seção 1, contribuições: "um verificador de qualidade em cinco dimensões, calibrado em casos de resposta conhecida na categoria do *pattern*, no comportamento, na arquitetura, nos testes e no banco"; Seção 6.2: "As cinco dimensões de qualidade receberam a nota bom, com problemas ou ruim" (o *design* do restante, a arquitetura, os testes, o banco e o comportamento, como na P6 da Introdução); Tabela 7: "Execuções boas por dimensão", com o *pattern* como uma das linhas.
Motivo: pela frase das contribuições, o leitor entende que as cinco dimensões foram calibradas, mas a dimensão do *design* do restante não foi (Seção 6.2: "A calibração não cobriu o *design* do restante do código nem a estrutura desnecessária").
Correção: liste as cinco dimensões uma vez, na Seção 6.2, e, nas contribuições, diga que a calibração cobriu quatro delas e a categoria do *pattern*. No título da Tabela 7, use "por dimensão e categoria do *pattern*" ou equivalente.

**3.3 SUGESTÃO. Dois nomes para os níveis.**
Onde: Resumo: "quatro níveis de orientação"; Seção 1: "quatro níveis de configuração"; Tabela 2: N0 "sem configuração"; Seção 6.3 e Discussão: "sem orientação".
Motivo: a Introdução declara que "configuração" e "orientações persistentes" são a mesma coisa, mas a alternância continua nos rótulos dos níveis.
Correção: escolha um termo para os níveis ("nível de configuração", por exemplo) e use-o no Resumo, nas tabelas e na Discussão.

**3.4 SUGESTÃO. "Lote" e "agente" com mais de um sentido.**
Onde: "lote descartado" (Seção 4); "O lote custou US$ 346" (Seção 5.3, estudo principal); "lotes pequenos" (Seção 6.2, disparo da extensão). "Agente" é o objeto estudado em todo o texto, mas a Seção 6.2 diz "revisado ... por um agente, o Claude Opus 5.5" e "com o mesmo agente" para o verificador.
Correção: use "estudo principal" no lugar de "o lote" na Seção 5.3 e "verificador" no lugar de "agente" na Seção 6.2.

**3.5 SUGESTÃO. "*Tokens* de entrada" sem definição.**
Onde: Seção 5.1 ("O custo foi medido pelos *tokens*") e Tabela 11.
Motivo: os valores incluem a leitura e a escrita de *cache* (num `meta.json` da extensão, `input_tokens` é 361 e `cache_read_input_tokens` é 2.195.678), e o texto não diz isso, embora explique que o custo depende do *cache*.
Correção: diga em uma oração o que a medida soma.

As siglas pouco conhecidas estão expandidas (LCOM, CBO, WMC, DIT, CCAE, DCX) e os estrangeirismos estão em itálico.

### Critério 4. Resumo e Abstract

O Resumo tem problema, objetivo, método e resultado com números, e o Abstract é tradução fiel dele.

**4.1 SUGESTÃO. Última frase do Resumo verdadeira por construção.**
Onde: Resumo, última frase: "No estudo principal, o ganho concentrou-se nos modelos que não acertavam sem orientação, os únicos com espaço para melhorar." A mesma ideia está na Seção 5.3 e na Conclusão.
Motivo: um modelo no teto não pode melhorar, então o ganho só pode aparecer nos outros; a própria frase reconhece isso. O achado que os dados mostram é outro: os seis modelos fora do teto melhoraram (14 pares melhores, nenhum pior), o maior ganho foi do mais fraco no N0 (H5) e nenhum modelo no teto piorou (H19).
Correção: troque a frase por esse achado, com o número.

**4.2 SUGESTÃO. Termos do Resumo sem definição.**
Onde: "*skills*" e "estrutura desnecessária" aparecem sem explicação, ao passo que "exagero" e "controles negativos" são explicados; "o Haiku 4.5" aparece sem que o leitor saiba que é um dos 11 modelos e o mais fraco no conjunto.
Correção: acrescente poucas palavras para cada um (por exemplo, "o modelo mais fraco, Haiku 4.5").

### Critério 5. Introdução

A Introdução tem contexto, problema, relevância com fontes (He et al., Hou et al., Galster et al.), objetivo geral, contribuições e organização correta. O contexto institucional (TCC da Licenciatura da UFPB, orientador identificado na Seção 4) está dado; o texto não menciona o AYTY.

**5.1 RECOMENDADA. Perguntas de pesquisa difíceis de localizar.**
Onde: Seção 1, parágrafo "Os objetivos específicos, formulados como perguntas de pesquisa (P1 a P8), são os seguintes", com as oito perguntas numa sequência de frases do tipo "busca-se verificar como a configuração muda ... (P1) e o exagero e a corretude do código (P2)".
Motivo: o texto diz que são perguntas, mas não as escreve como perguntas; duas delas dividem uma frase, e o leitor não acha cada uma pelo código. "Como ... muda" também não diz o que conta como resposta. O guia (4.4) pede perguntas numeradas e formuladas de modo que os dados consigam respondê-las.
Correção: escreva cada pergunta numa frase própria, em forma de pergunta, com o estudo que a responde e o tipo de evidência (confirmatória, exploratória ou descritiva). Exemplo do padrão com a sua frase:
Antes: "busca-se verificar como a configuração muda o uso do *pattern* esperado (P1)".
Depois: "P1. A configuração aumenta o uso do *pattern* esperado? (confirmatória no estudo principal, exploratória na extensão)".

### Critério 6. Coerência metodológica

A metodologia descreve o que foi feito, como, com quais ferramentas e com quem, em ordem (Figura 3), e identifica o autor, o orientador e os modelos de IA (Seção 4 e Tabela 1). Os apontamentos abaixo tratam de escolhas sem motivo e de origens pouco claras.

O leitor precisa entender por que cada coisa foi feita daquele jeito. Quando a escolha afeta o resultado (a comparação, o critério de avaliação, os dados que contam), uma escolha sem motivo faz o leitor duvidar do resultado. Em geral, uma oração com "para" ou "porque" resolve.

**6.1 OBRIGATÓRIA. Estabilidade medida contra a primeira rodada, sem motivo.**
Onde: Seção 6.2, quinto parágrafo: "A estabilidade foi medida comparando cada rodada nova com a primeira da mesma combinação, quanto ao *pattern* certo ou não."
Motivo: o roteiro de revisão trata como obrigatória a falta de motivo quando a escolha muda a leitura do resultado, e a comparação escolhida muda: os 82% da Seção 6.3, da Discussão e da Conclusão ("uma execução isolada não basta") dependem de qual rodada é a referência. Essa rodada não é igual às outras: pelos `meta.json`, 87 das 90 execuções dela são de 21/10, ao passo que as repetições são de 22/10, 01/11 e 02/11; e foi pelo resultado dela que se escolheu quais *patterns* repetir primeiro (mesmo parágrafo). O texto não dá a data da primeira rodada.
Correção: diga por que a primeira rodada é a referência ("comparamos com a primeira rodada para ..."), informe a data dela e diga se a diferença de datas pode pesar. As 54 de 90 combinações com o mesmo resultado nas cinco rodadas, que você já relata, não dependem da referência e podem acompanhar o número.

**6.2 OBRIGATÓRIA. Verificador informado de que o cenário era um controle, sem motivo.**
Onde: Seção 6.2, último parágrafo: "O mesmo verificador da extensão, informado de que o cenário era um controle e às cegas quanto ao modelo e ao nível, classificou cada execução em contenção ... ou em exagero". A Seção 8 repete o fato como limitação ("O verificador sabe que o cenário é um controle").
Motivo: o roteiro trata como obrigatória a falta de motivo quando a escolha do critério de avaliação muda a leitura do resultado. Um avaliador que sabe que o pedido não justifica o *pattern* pode tender a classificar a mudança como contida, e o resultado de 88 de 90, que sustenta parte da resposta à P2 e da Conclusão, depende dessa classificação. O texto registra o risco, mas não diz por que o verificador precisou saber.
Correção: escreva, na Seção 6.2, o motivo (por exemplo, se as categorias de contenção e exagero só existem para os controles, diga isso) e, se for o caso, o que reduz o risco (os critérios definidos antes, a calibração com soluções de resposta conhecida).

**6.3 RECOMENDADA. Falhas de ambiente contadas na nota de testes.**
Onde: Seção 6.2, terceiro parágrafo: "O contêiner do agente não dispunha de Docker. ... A *skill* de testes pede ao verificador que distinga as falhas do código das do ambiente, mas as falhas contam na nota; dessas 11 execuções, 9 receberam a nota ruim nos testes e 2, com problemas."
Motivo: são 11 das 225 execuções da construção, numa dimensão em que a construção tem 11, 9 e 6 notas bom (Tabela 7). O texto sugere um motivo ("em vez de usar o endereço fornecido pelo enunciado"), mas não diz que foi esse o motivo de contar a falha, nem por que o contêiner não tinha Docker.
Correção: declare as duas razões em uma oração cada.

**6.4 RECOMENDADA. N1 × N0 como comparação principal sem motivo no texto.**
Onde: Seção 5.1, parágrafo da Tabela 4: "As cinco primeiras são as principais, isto é, as que respondem diretamente à pergunta do pré-registro".
Motivo: o pré-registro dá o motivo ("o `CLAUDE.md` contra nenhum harness, o contraste mais limpo", `OBJETIVO.md`, seção 4), mas o artigo não; e a Discussão, P4, diz que a concentração do ganho no `CLAUDE.md` "decorre também da ordem cumulativa dos níveis", o que torna a escolha relevante para a leitura.
Correção: traga o motivo do pré-registro para a Seção 5.1 em uma oração.

**6.5 RECOMENDADA. Autoria dos instrumentos não identificada.**
Onde: Seção 5.1: "verificada com 17 versões defeituosas escritas manualmente"; Seção 6.2: "As dez soluções de referência" e "Seis exemplos deliberadamente degradados"; Tabela 1: "apoio ao desenho dos instrumentos e às bancadas | Opus 5 e Opus 5.5".
Motivo: a resolução exige identificar quem fez o quê. Como a Tabela 1 registra IA no desenho dos instrumentos, "escritas manualmente" não diz se foi o autor ou o modelo com revisão do autor, e o leitor não sabe quem escreveu as respostas de referência da calibração.
Correção: diga, para a suíte, as versões defeituosas, as regras do Semgrep, as soluções de referência e os exemplos degradados, quem escreveu e quem conferiu.

**6.6 RECOMENDADA. Medida da semelhança entre rodadas definida só nos resultados.**
Onde: Seção 5.1: "Outras quatro questões registradas são apenas descritivas: a semelhança entre rodadas, ..."; a medida só aparece na Seção 5.3: "as cinco rodadas produziram menos perfis distintos de classificação do Semgrep no N1 do que no N0".
Motivo: o método precisa dizer como o dado foi produzido; o leitor só descobre a medida (e que ela compara apenas N1 e N0) ao ler o resultado.
Correção: descreva a medida na Seção 5.1 em uma frase.

**6.7 SUGESTÃO. Origem de um número usado como justificativa.**
Onde: Seção 6.2: "em execuções preliminares, anteriores às relatadas aqui, o Haiku 4.5 a abriu em apenas 1 de 5 vezes".
Correção: diga de qual fase vêm essas execuções (a fase intermediária da Seção 6.1, se for o caso), para o número ter origem descrita.

**6.8 SUGESTÃO. Resumos pré-registrados que não aparecem nem são apontados.**
Onde: o pré-registro (`construction-v5/OBJETIVO.md`, seções 4.1 e 4.10) prevê o tamanho do efeito publicado "ao lado de cada hipótese", uma nota de 0 a 100 com variantes de peso e a coluna que mede se o cegamento da leitura humana vazou. O artigo não traz nenhum dos três, e o último parágrafo da Seção 8 só remete ao repositório "as demais métricas do CK e os resultados por execução".
Correção: acrescente a essa frase que esses resumos pré-registrados estão no repositório, ou relate o resultado da verificação do cegamento, que apoia a conferência humana.

### Critério 7. Solução, resultados e conclusão

A solução (o experimento e a bancada) está descrita com artefatos (Figuras 1, 3 e 4, Tabelas 1 a 3) e métricas de dimensão (220, 450 e 90 execuções, 28 hipóteses, 52 notas de calibração). Cada pergunta tem resposta explícita na Seção 7, e as limitações e os trabalhos futuros são específicos.

A conclusão não pode afirmar mais do que os resultados sustentam: se a discussão apresenta um efeito com ressalvas, a recomendação mantém as mesmas ressalvas.

**7.1 RECOMENDADA. Recomendação prática sem parte dos dados.**
Onde: Seção 9, quarto parágrafo: "um arquivo curto de princípios ... concentrou o ganho no estudo principal ... Na manutenção, uma *skill* com instrução de uso acrescentou mais execuções com o *pattern* certo que o arquivo, mas, na construção, veio acompanhada de mais estrutura desnecessária."
Motivo: o guia (4.13) diz que a recomendação prática precisa refletir tudo o que os dados mostraram. Ficaram de fora: (a) na manutenção, o comportamento caiu de 59 para 54 execuções boas em 75 do N1 para o N2 (Tabela 7), justamente onde a *skill* é recomendada; (b) a ressalva da Discussão, P2, de que a estrutura desnecessária "vem de uma nota que a calibração não cobriu"; (c) a piora do Haiku 4.5 na corretude com o arquivo. Além disso, "curto" não tem apoio: o texto não informa o tamanho do `CLAUDE.md`.
Correção: inclua (a), (b) e (c) na recomendação, com a mesma cautela da Discussão, e informe o tamanho do arquivo ou retire "curto".

**7.2 RECOMENDADA. Resposta à P7 sem conciliar os dois estudos.**
Onde: Seção 7, P7: "as rodadas ficaram mais parecidas entre si com o `CLAUDE.md` em 7 dos 11 modelos" (estudo principal) e "A variação acompanha o modelo ..., e não o nível (96, 103 e 95 de 120 no N0, N1 e N2)" (extensão); a Conclusão só traz a extensão.
Motivo: os dois achados apontam em direções diferentes quanto ao efeito do nível, e as medidas são diferentes (perfis distintos de cinco pontos, N1 contra N0, no estudo principal; acordo com a primeira rodada, em três níveis, na extensão). Sem dizer isso, o leitor não sabe qual é a resposta.
Correção: diga em uma frase que as medidas diferem e qual é a resposta à P7 considerando as duas.

**7.3 SUGESTÃO. Mesma queda atribuída a perguntas diferentes.**
Onde: Seção 9, segundo parágrafo: "Quanto aos custos, o comportamento caiu na manutenção ... (P2)"; Seção 7, P6: "O comportamento ... cai na manutenção ..., de modo que o ganho em *pattern* veio acompanhado de alguma perda nessa dimensão."
Correção: use na Conclusão a mesma pergunta da Discussão (P6), ou explique por que o comportamento entra também na P2.

**7.5 RECOMENDADA. Valor da Tabela 11 não reproduzido.**
Onde: Tabela 11, *tokens* de saída do N2 da construção: 38,9 mil.
Motivo: a Tabela 1 diz que os "números do texto [foram] recalculados a partir dos dados brutos". Somando `modelUsage` do evento final de `claude-output.jsonl`, fonte que reproduz exatamente os 2,29 milhões de entrada desse nível e as demais células da construção, o valor é 39,1 mil; pelos `meta.json`, é 38,4 mil (e a entrada, 2,24 milhões). As outras células da Tabela 11 conferem.
Correção: recalcule a célula pela mesma fonte das demais e registre no método qual é essa fonte. A diferença é pequena e não muda a leitura.

(O número 7.4 não foi usado: o ponto foi incluído em 4.1.)

### Critério 8. Figuras, tabelas e gráficos

Todas as figuras e tabelas têm número, título e fonte, e todas são apresentadas no texto com descrição. Os gráficos (Figuras 9 e 10) têm eixo rotulado ("execuções"), escala e legenda. Os pontos 1 e 3 já decididos cobrem o código em imagem e a repetição entre tabela e figura.

**8.1 SUGESTÃO. Figura 8: o exemplo do Template Method perde o título de cada relatório.**
Onde: Figura 8. Sem o *pattern*, cada relatório tem o seu título (`titulo("occupancy")`, `titulo("revenue")`); com o *pattern*, `gerar` chama `titulo()` sem parâmetro, e o comentário diz "titulo() fica em Relatorio".
Motivo: com um único `titulo()` na classe base, todos os relatórios teriam o mesmo título, o que não corresponde ao lado esquerdo nem à frase da Seção 6.2 ("cada um define o próprio conteúdo").
Correção: torne o título um passo que a subclasse define, ou passe o título à classe base.

**8.2 SUGESTÃO. Figura 2 usa `double` para preço.**
Onde: Figura 2, `double preco(String plano, double diaria, int noites)`; Seção 6.2: "dinheiro em ponto flutuante ... dão ruim".
Motivo: o próprio verificador do trabalho penaliza dinheiro em ponto flutuante, e um leitor atento nota o contraste.
Correção: use `BigDecimal` no exemplo ou diga na legenda que o tipo foi simplificado.

**8.3 SUGESTÃO. Título da Tabela 9.**
Onde: título "médias de 75 execuções por nível", mas a última linha é "LCOM média do projeto, mediana".
Correção: ajuste o título (por exemplo, "médias, salvo a LCOM, em mediana").

**8.4 SUGESTÃO. Tabelas do apêndice citadas só entre parênteses.**
Onde: o Apêndice A diz "dados por modelo citados na Seção 7", mas as Tabelas 13 a 16 são citadas primeiro nas Seções 5.3 e 6.3, e sempre entre parênteses ("(Tabela 13)", "(Tabela 14)").
Correção: corrija a frase do apêndice e diga, na primeira citação de cada tabela, o que ela mostra.

### Critério 9. Inconsistências de conteúdo

Uma contradição entre duas seções faz o leitor duvidar das duas, e a mesma afirmação precisa ter a mesma cautela no Resumo, nos resultados e na conclusão.

**9.1 RECOMENDADA. A Discussão afirma com menos cautela que o Resumo e a Conclusão.**
Onde (primeiro par): Resumo, "sem alteração detectada no uso do *pattern* onde ele não se justificava (exagero)", e Conclusão, "sem alteração detectada no exagero"; contra a Discussão, P2: "No estudo principal, o `CLAUDE.md` não altera o exagero no ponto do seguro". Com 1 par não empatado em 55 (Seção 5.3), a regra binária da Seção 5.1 exige ao menos 5 para apoiar; o dado não permite dizer que não há efeito, só que ele não foi detectado.
Onde (segundo par): Discussão, P8: "A orientação substitui a condicional pelo polimorfismo, como a Seção 2 prevê"; contra a Conclusão: "As métricas ..., em sua maioria descritivas e analisadas depois dos resultados, são compatíveis com um mecanismo, que em parte apenas redescreve a mudança de *design*".
Correção: use na Discussão a mesma formulação do Resumo e da Conclusão nos dois casos.

**9.2 RECOMENDADA. O ponto do pagamento, no artigo e no pré-registro.**
Onde: Seção 5.1: "O texto contém quatro pontos em que a variação pede o *pattern* Strategy: o frete, o cupom, a forma de pagamento e o nível do clube"; `construction-v5/OBJETIVO.md`, seção 6: "O P3 (pagamento) não tem frase de crescimento no enunciado, ao contrário do P1, do P2 e do P4: um `switch` sobre as 3 formas conta como erro no Semgrep sem que o enunciado dê motivo para separá-las."
Motivo: o artigo apresenta o pagamento como ponto que pede o Strategy, e a Tabela 5 relata 55% de acerto no N0 nesse ponto; o pré-registro diz que um código sem *pattern* ali não está errado pelo enunciado. O leitor do artigo lê os 55% como erro.
Correção: diga na Seção 5.1 que o ponto do pagamento não tem frase de crescimento e que, por isso, ele é só secundário (o artigo já diz que é secundário, mas por outro motivo, a falta de conferência humana).

**9.3 SUGESTÃO. Momento da decisão das duas rodadas do controle.**
Onde: Seção 6.2: "a decisão foi tomada antes de o resultado da primeira rodada ser conhecido"; Seção 8: "a decisão de realizar duas rodadas foram tomados no último dia de execução, depois de conhecidos os resultados dos cenários com *pattern*".
Motivo: as duas frases são compatíveis (uma fala da primeira rodada do controle, a outra dos cenários com *pattern*), mas, lidas separadamente, parecem se contradizer.
Correção: escreva "primeira rodada do controle" na Seção 6.2.

Não encontrei trechos genéricos desligados do trabalho, funcionalidades citadas uma vez e nunca mais, nem mudança de estilo.

### Critério 10. Normas e referências

As 43 referências da lista são citadas no texto, e todas as citações do texto estão na lista. Os 26 DOIs foram abertos na API do Crossref e levam ao trabalho citado, com os mesmos autores, ano, título e veículo (entre eles os de 2026: Ampatzoglou et al., Arabat e Sayagh, Chatlatanagulchai et al., Galster et al., He et al., Jiang e Nam, Kim, Kjellberg et al., Liu et al. e Zisa et al.). Os 23 endereços (rodapés e referências) abriram com HTTP 200, incluindo o repositório público e as pastas citadas nele (`construction-v5/experiment/harnesses`, `TCC_V1/construction/experiment/harnesses`, `TCC_V1/maintenance/experiment/harnesses`, `TCC_V1/evaluation/quality/skills`, `construction-v5/infra/scripts`). Os *commits* citados existem (54e13fc9 no repositório do trabalho, 85e94a3 no da *skill* pública e 8a1ef916 no do CK). Não há marcador pendente, link com parâmetro de rastreio nem citação direta de livro. Não conferi o conteúdo das referências sem DOI (livros, Brown et al., Vaswani et al., Yao et al.) nem os números citados de cada artigo (por exemplo, os 2.853 repositórios de Galster et al.).

**10.1 SUGESTÃO. Fonte da descrição geral do Claude Code.**
Onde: Seção 1: "ferramenta da empresa Anthropic que recebe um pedido em linguagem natural e altera o projeto com autonomia [Anthropic 2026a]".
Motivo: Anthropic (2026a) é a página sobre a memória e o `CLAUDE.md` ("How Claude remembers your project"), que sustenta a frase seguinte do texto, mas não a descrição geral da ferramenta; a página geral está só no rodapé.
Correção: cite a página geral da documentação como referência para essa frase.

**10.2 SUGESTÃO. Autores na lista de referências.**
Onde: Brown et al., Chatlatanagulchai et al., Hou et al., Liu et al., Vaswani et al., Xi et al. e Yao et al. aparecem com "et al." na lista; as demais referências trazem todos os autores.
Correção: confira a regra do modelo da SBC e use a mesma forma em toda a lista.

### Critério 11. Elementos obrigatórios da resolução

As tecnologias estão na Tabela 3, com versão e papel (a versão do Docker das execuções anteriores a 09/11 não foi registrada, o que o próprio texto declara). O link do repositório público está no texto (Seção 4 e Seção 5.1) e abre sem login. Não há apontamento neste critério.

## Matriz de rastreabilidade

| objetivo / pergunta | onde é desenvolvido | etapa do método que gera os dados | resultado (seção, tabela) | resposta explícita (onde) | situação | ação pendente |
|---|---|---|---|---|---|---|
| Objetivo geral: avaliar o efeito de orientações persistentes sobre a decisão de aplicar um *pattern*, na construção e na manutenção | Seções 5 e 6 | Seções 5.1 e 6.2 | Tabelas 5, 7, 8 e 12 | Seção 9, primeiro e segundo parágrafos | atendido | nenhuma |
| P1. Uso do *pattern* esperado | Seções 5.3 e 6.3 | Semgrep com conferência humana (5.1); verificador de IA (6.2) | Tabelas 5, 8, 13, 14 e 15; Figuras 9 e 10 | Seção 7, P1 ("aponta para o aumento do uso correto"); Seção 9 | atendido | 3.1 (medidas diferentes sob o mesmo nome) |
| P2. Exagero e corretude | Seções 5.3 e 6.3 | suíte, Semgrep, SonarQube (5.1); estrutura desnecessária e controles (6.2) | Tabela 4 (H2, H3, H7, H8, H11 a H15, H23, H24, H26), Tabelas 5, 12 e 14 | Seção 7, P2 e parágrafo do controle; Seção 9 | atendido, com cautela diferente | 9.1, 6.2 |
| P3. Variação entre modelos | Seções 5.3 e 6.3 | regras H5, H19 e H20 (5.1); três modelos (6.2) | Tabelas 13 e 14 | Seção 7, P3; Seção 9, primeiro parágrafo | atendido | 4.1 |
| P4. Contribuição de cada componente e custo | Seções 5.3 e 6.3 | níveis cumulativos e custo pela transcrição (4, 5.1, 6.2) | Tabelas 4 (H16 a H18, H21 a H28), 5 e 11 | Seção 7, P4; Seção 9, terceiro e quarto parágrafos | atendido | 6.4, 7.1 |
| P5. Efeito do tipo de tarefa | Seção 6.3 | construção e manutenção (6.2) | Tabelas 7 e 8; Figuras 9 e 10 | Seção 7, P5; Seção 9, segundo parágrafo | atendido, com a confusão do texto do `CLAUDE.md` declarada | nenhuma |
| P6. Demais dimensões de qualidade | Seção 6.3 | verificador, cinco dimensões (6.2) | Tabelas 7 e 14 | Seção 7, P6; Seção 9, segundo parágrafo | atendido | 3.2, 7.3 |
| P7. Estabilidade entre rodadas | Seções 5.3 e 6.3 | questão descritiva (5.1, medida só nos resultados); comparação com a primeira rodada (6.2) | texto da Seção 5.3; Tabela 16 | Seção 7, P7; Seção 9, terceiro parágrafo (só extensão) | parcial | 6.1, 6.6, 7.2 |
| P8. Estrutura orientada a objetos | Seções 5.3 e 6.3 | CK (2, 5.1, 6.2) | Tabelas 6, 9 e 10; H9 e H10 | Seção 7, P8; Seção 9, segundo parágrafo | atendido, com cautela diferente | 9.1 |
| Controle negativo da manutenção (tratado com a P2) | Seção 6.3 | Seção 6.2, últimos dois parágrafos | Tabela 12 | Seção 7, parágrafo do controle; Seção 9, segundo parágrafo | atendido | 6.2 |

Não há resultado sem objetivo: os resultados das Seções 5.3 e 6.3 se ligam a alguma pergunta (o custo e a abertura da *skill* à P4; as reservas simultâneas e as migrações à P6).

## Checklist do guia (seção 10)

### Elementos obrigatórios da resolução

| item | situação |
|---|---|
| A Introdução descreve o problema e o contexto | atendido |
| A metodologia descreve o processo e identifica os envolvidos | parcial (6.5) |
| Há descrição das tecnologias, com versão | atendido (Tabela 3) |
| A solução é apresentada com artefatos e métricas de dimensão | atendido |
| O texto traz o link do repositório público | atendido (abre e as pastas citadas existem) |
| Se houve avaliação, o processo e os resultados estão descritos | atendido |

### Critério 1. Língua portuguesa e redação

| item | situação |
|---|---|
| Sem erros de ortografia, acentuação, concordância ou regência | parcial (1.4) |
| Tempos verbais consistentes | parcial (1.2) |
| Sem frases longas que encadeiam vários resultados | parcial (1.1) |
| Sem declarações de importância sem fato | atendido |
| Sem gerúndio de comentário no fim da frase | atendido |
| "É" e "tem" em vez de "atua como", "serve como", "conta com" | atendido |
| Toda afirmação atribuída a terceiros cita a fonte | atendido |
| Sem contrastes montados nem fórmulas de enchimento | atendido |
| Títulos com maiúscula só na primeira palavra e em nomes próprios | atendido |
| Sem vírgula antes do "e" final nem decalques do inglês | parcial (1.3) |

### Critério 2. Encadeamento

| item | situação |
|---|---|
| Cada seção prepara a seguinte, sem saltos | parcial (2.1) |
| Todo termo técnico é definido antes de ser usado | parcial (2.2) |
| Cada objetivo é retomado e chega à Conclusão com o mesmo nome | parcial (7.3) |
| O tamanho de cada seção corresponde ao seu papel | atendido (ver leitura macro, item 6) |

### Critério 3. Terminologia e siglas

| item | situação |
|---|---|
| Cada conceito tem um nome só | parcial (3.1, 3.3) |
| Nenhum nome designa duas coisas | parcial (3.1, 3.2, 3.4) |
| Siglas pouco conhecidas expandidas | atendido |
| Estrangeirismos em itálico | atendido |

### Critério 4. Resumo e Abstract

| item | situação |
|---|---|
| Problema, objetivo, o que foi feito e resultado com número | atendido |
| O resumo se entende sozinho | parcial (4.2) |
| O Abstract é a tradução fiel do resumo | atendido |

### Critério 5. Introdução

| item | situação |
|---|---|
| Contexto, problema e relevância com fatos ou fontes | atendido |
| Objetivos ou perguntas claros, numerados e verificáveis | parcial (5.1) |
| Os objetivos correspondem ao que foi feito | atendido |
| Contexto institucional explicado | atendido |
| O parágrafo de organização corresponde às seções | atendido |

### Critério 6. Metodologia

| item | situação |
|---|---|
| Cada etapa diz o que, como, com quais ferramentas e quem | parcial (6.5) |
| Sequência e dependências das etapas claras | atendido |
| Cada escolha tem motivo declarado | pendente (6.1, 6.2) e parcial (6.3, 6.4) |
| Todo resultado tem etapa de origem | parcial (6.6, 6.7) |
| Toda etapa aparece nos resultados, e toda atividade está no método | atendido (a releitura não feita está declarada) |
| Versões das ferramentas informadas | atendido |

### Critério 7. Solução, resultados e conclusão

| item | situação |
|---|---|
| A solução é descrita com métricas e artefatos | atendido |
| Resultados concretos e organizados pelas perguntas | parcial (2.1) |
| Cada pergunta tem resposta explícita | parcial (7.2, na P7) |
| Não há resultado desligado dos objetivos | atendido |
| A conclusão traz os resultados e mantém as ressalvas | parcial (7.1, 9.1) |
| Limitações e trabalhos futuros específicos | atendido |

### Critério 8. Figuras, tabelas e código

| item | situação |
|---|---|
| Toda figura e tabela tem número, título e fonte | atendido |
| Toda figura e tabela é apresentada com descrição | parcial (8.4) |
| Todo gráfico tem eixos com rótulo e legenda | atendido |
| Código em texto, nenhum em imagem | decidido com o orientador (ponto 1) |
| Nenhuma imagem de código com fundo escuro | atendido |
| A mesma informação não se repete em tabela e figura | decidido pelo autor (ponto 3) |

### Critério 9. Consistência de conteúdo

| item | situação |
|---|---|
| O mesmo número tem o mesmo valor em todo o texto | atendido; uma célula da Tabela 11 não confere com os dados brutos (7.5) |
| Resumo, Introdução e Conclusão com a mesma cautela | parcial (9.1, pela Discussão) |
| Sem contradição entre seções | parcial (9.2, 9.3) |
| Sem trechos genéricos sem ligação com o trabalho | atendido |
| Sem funcionalidades ou etapas citadas uma vez só | atendido |
| Estilo uniforme | atendido |

### Critério 10. Normas e referências

| item | situação |
|---|---|
| Toda citação está na lista, e toda referência é citada | atendido |
| Cada DOI leva ao trabalho citado | atendido (26 de 26) |
| Sem marcador pendente | atendido |
| Citações diretas de livro com página; links com data de acesso | atendido |
| Numeração das seções consistente e elementos do modelo | atendido |

### Critério 11. Tecnologias e repositório

| item | situação |
|---|---|
| Tecnologias em tabela, com versão e papel | atendido |
| O link do repositório está no texto e abre para qualquer pessoa | atendido |

### Revisão do texto inteiro e matriz

| item | situação |
|---|---|
| Leitura do texto inteiro (seção 7 do guia) | atendido nesta autoavaliação |
| Matriz completa, sem célula "não encontrado" | atendido (P7 parcial) |

### Uso de ferramentas de IA

| item | situação |
|---|---|
| Declaração de uso depois da Conclusão | pendente por decisão do autor (ponto 2) |
| IA usada como instrumento descrita na metodologia | atendido (Tabela 1, Seção 6.2) |
| Todo conteúdo, código e referência sugeridos foram conferidos | não verificável por esta revisão; as referências e os números conferidos aqui estão listados acima |
