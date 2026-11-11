Autoavaliação feita com ferramenta de IA. A avaliação oficial é do orientador.

# Autoavaliação antes da entrega: artigo, versão 140 (rodada 7)

Este relatório aplica os critérios de revisão do professor Rodrigo Rebouças de Almeida, que são orientações dele para os seus orientandos e não critérios oficiais da disciplina, do curso ou da UFPB. O uso desta ferramenta é uso de IA e entra na declaração de uso do TCC (seção 8 do guia). O relatório não diz se o trabalho está aprovado e não reescreve o seu texto. Quando um ponto aparece como OBRIGATÓRIA, isso significa que o roteiro de revisão o trata como obrigatório.

Os apontamentos têm número único, na ordem da seção "Apontamentos por critério". No total, são 1 obrigatório, 6 recomendados e 16 sugestões.

## Pontos já decididos

Estes pontos foram decididos por você, já foram verificados ou dependem de você. Nenhum conta como pendência neste relatório, e nenhum apontamento abaixo pede lista no corpo do texto.

**Decididos**

1. As figuras de código (Figuras 2, 5, 6, 7 e 8) ficam como imagem com fundo branco. Abri as cinco, e todas têm fundo branco.
2. A Seção 6.2 mantém uma figura por *pattern*.
3. As Figuras 9 e 10 ficam junto das Tabelas 7 e 8.
4. A declaração de uso de ferramentas de IA será escrita por último. O apoio à redação do artigo não entra na Tabela 1.
5. A coluna "como foi conferido" da Tabela 1 foi confirmada por você.
6. O título continua o mesmo, e o nome da H8 segue o pré-registro.
7. Os motivos que você já deu estão mantidos: o Strategy por ser difundido e adequado ao domínio; os cinco *patterns* por cobrirem as três categorias do catálogo; o N3 fora da extensão; a segunda rodada do controle decidida antes do resultado da primeira; a diferença de horário por causa da janela da cota.
8. As referências ficam só com revistas e eventos respeitados, sem *preprints*, com correções só de textos publicados, e a definição de IA vem de livro.
9. O registro é prosa impessoal em parágrafos encadeados, sem listas no corpo e sem rótulos em negrito no começo do parágrafo. As perguntas P1 a P8 ficam em parágrafo.
10. O Resumo fica entre 150 e 250 palavras, e o Abstract é tradução fiel. Contei 249 e 250 palavras.
11. A Introdução define cada termo para o leitor.
12. *Harness* segue o sentido de Galster et al., com o aviso sobre o pré-registro.
13. O uso só de modelos da Anthropic está declarado como limitação.

**Já verificado: não procede**

14. Tabela 11, *tokens* de saída do N2 da construção (38,9 mil): todas as células usam a soma dos segmentos. Não reabri o ponto.

**Pendências que dependem de você**

15. Tabela comparativa dos trabalhos relacionados (Apêndice C do guia), um nome só para os níveis e "et al." nas referências.
16. Citar Page (1963) e Cohen (1960) depende de você ler ou aprovar. Itálico nas Figuras 1, 3, 4, 9 e 10.
17. O motivo de a hipótese "quem melhora não é quem exagera" e a proporção entre ler, escrever e executar não terem sido calculadas (OBJETIVO §4.5 e §4.6). O texto só registra o fato.
18. O aviso da API do OpenAlex pedindo o e-mail do usuário, da rodada anterior, não foi atendido. Nesta rodada não usei o OpenAlex, e nenhuma página ou API trouxe instrução dirigida a mim.

## Pontos a tratar

### Obrigatório

O roteiro de revisão trata este ponto como obrigatório, porque muda a leitura de um resultado.

| nº | ponto | onde |
|---|---|---|
| 18 | A recomendação sobre a *skill* na manutenção cita a queda do comportamento (59 para 54) e deixa de fora a queda maior dos testes (37 para 25) e a alta da arquitetura (57 para 68), que aparecem nos três modelos. | Seções 7 (P6) e 9 |

### Recomendados, por peso

| nº | ponto | onde |
|---|---|---|
| 20 | As duas exceções do controle são lidas como coerentes com a alta da estrutura desnecessária na construção, mas ocorrem no único modelo que ficou em zero nessa medida. | Seção 7, parágrafo após a P2; Tabela 14 |
| 14 | O Resumo afirma ganho maior na manutenção sem a ressalva que a Discussão e a Conclusão fazem. | Resumo e Abstract; Seções 7 e 9 |
| 19 | A H5 foi apoiada pela regra de empate, e a resposta da P3 não diz que o outro modelo empatado teve o menor saldo fora do teto. | Seções 5.3 e 7; Tabela 13 |
| 15 | A descrição das 12 execuções com malfeito vem do relatório escrito do verificador, e o método não descreve esse relatório. | Seções 6.2, 6.3 e 7 |
| 22 | A frase nova sobre o pagamento dá a entender que só esse ponto é secundário e sem conferência humana. | Seções 5.1 e 5.3 |
| 2 | "A mesma etapa" passou a apontar para a etapa de desenho dos instrumentos. | Seção 5.1 |

## Leitura macro

### Mapa das seções

| seção | o que entrega | o que prepara |
|---|---|---|
| Resumo e Abstract | problema, objetivo, os dois estudos e os resultados principais | a leitura do trabalho |
| 1. Introdução | contexto, *harness*, *patterns*, relevância, objetivo geral, P1 a P8, contribuições e organização | as definições e o compromisso das perguntas |
| 2. Fundamentação | encapsulamento, polimorfismo, aberto/fechado, acoplamento, coesão e métricas | a leitura das Tabelas 6, 9 e 10 |
| 3. Trabalhos relacionados | oito trabalhos e a lacuna | a diferença do desenho experimental |
| 4. Visão geral do processo | fases, desenhos, repositório, envolvidos, IA (Tabela 1), níveis (Tabela 2), tecnologias (Tabela 3) | os dois estudos |
| 5. Estudo principal | método, regras, 28 hipóteses, proteção contra decisões posteriores e resultados | os pontos em aberto da Seção 6.1 |
| 6. Extensão | evolução do desenho, método, calibração, controles e resultados | a Discussão |
| 7. Discussão | resposta a P1 a P8, comparação com os trabalhos e trajetória | Limitações e Conclusão |
| 8. Limitações | ameaças por estudo, avaliação e métricas | a cautela da Conclusão |
| 9. Conclusão | resultados, recomendação prática e trabalhos futuros | fim |
| Apêndice A | Tabelas 13 a 16 | apoio às Seções 5.3, 6.3 e 7 |

O parágrafo de organização corresponde às seções e ao apêndice que existem, na mesma ordem.

### Linha do argumento

A sequência é coerente: o problema leva às perguntas, o estudo principal mede a decisão de usar um *pattern*, as suas lacunas motivam a extensão, e a Discussão responde na ordem das perguntas. As duas falhas que afetam o argumento estão na leitura das demais dimensões com a *skill* (18) e na ligação entre o controle e a construção (20).

### Resumo, Introdução e Conclusão

Os três prometem o mesmo objeto, e os números que coincidem têm o mesmo valor. A cautela difere no ganho maior na manutenção, que o Resumo afirma sem ressalva (14). A Conclusão mantém as ressalvas da Discussão, mas a recomendação prática usa só parte dos dados (18).

### Números repetidos

Somei as tabelas e recalculei a partir dos dados o que indico abaixo. Tudo fechou, e nenhum apontamento desta rodada é de valor errado.

| conferência | fonte e regra | resultado |
|---|---|---|
| Tabelas 7, 8, 12, 14 e 15 | `review.json` das 540 revisões; *pattern* certo = acerto + alternativa equivalente; dimensão = nota bom | conferem em todas as células |
| Tabela 16 e 294 de 360, 54 de 90 | `review.json`, repetição comparada com a rodada 1 | conferem |
| Categorias: 27 e 30; 7 e 26; 32 e 15; 55 e 56; 9 e 14; 45, 53, 72; 48, 66, 69; 12, 8, 4 e 2 | `design_class` em `review.json` | conferem |
| Strategy na construção: omissão 6, 1, 1; malfeito 0, 7, 5; 4, 2, 4 em 10 | `review.json` | conferem |
| 12 execuções com malfeito, 11 com um só ponto fora | os 12 `review.md` | confere |
| Figuras 9 e 10 contra Tabelas 7 e 8 | leitura das imagens | conferem |
| Tabelas 9 e 10; contagens de 449 execuções | `oo-metrics.csv`; categoria de `review.json` | conferem |
| Conta do CBO pelos tipos (22% a 46%; 3% a 27%) | Tabelas 6 e 9, pela regra declarada | confere |
| Custo da extensão, US$ 380; US$ 346 no principal | `cost_usd` em `meta.json`; `resultados-V5-STRATEGY.md` | conferem (380,26; 346,16) |
| Abertura da *skill*: 69 de 75, 19, 25, 25; 20 de 30 nos controles | `skill_opened` em `meta.json` | confere |
| `mvn verify`: 430 de 450; 8, 7, 4 e 1; 90 de 90 | `build_exit_code` em `meta.json` | confere |
| Reservas simultâneas: 59 e 16 em 225; nenhuma na manutenção | `behavior.md`, linha "nenhuma com as duas reservas criadas" | confere (18 Haiku 4.5, 41 Sonnet 4.5) |
| Comportamento ruim em 71 e 32; banco 39/36, 36/39, 35/40 | `review.json` | conferem |
| Uma execução com arquivos de migração e serviço sem subir | `database.md` | confere |
| Datas e diferenças de até 4 h e 10 h | `start` em `meta.json` | conferem (4,01 h; 9,91 h) |
| Controles: 88 de 90, 87 com bom, três do Haiku 4.5; quatro refeitas | `review.json`, `runs-2026-11-09-maven-429` | conferem |
| Piloto: 1 de 5 | `skill_opened`, `runs-2026-10-21-sem-banco` | confere |
| Segundo verificador: 195 de 210, 30/30/29/29/27/26/24, kappa 0,53, 15 diferenças sempre mais brandas, razão de custo 3 | `second-verifier` contra `review.json` | conferem |
| Calibração: 26 + 26 + 6 = 58 | pastas de `calibration` | confere |
| Tabelas 4, 5 e 13, saldo H3 (10 e 7), H23, H26, Page 0,007, perfis 7, 3 e 1, *tokens* 6 e 5 modelos | `resultados-V5-STRATEGY.md`, `hipoteses-V5-STRATEGY.md` | conferem; os 28 vereditos batem |
| Vereditos de *tokens* com o subagente | `usage_by_model` em `meta.json`, regras do §4.1 | conferem: H16 35 de 55 (inconclusiva), H22 e H27 sem mudança |
| 36 de 55, 5 de 5, 8 de 9, 13% | binomial e §4.1 do `OBJETIVO.md` | conferem |
| 119 execuções, 122 arquivos, 14 linhas, 21 casos, 17 versões, 26 testes | `meta.json` da bancada, `CONGELADO-V5.sha256`, `CLAUDE.md`, READMEs, testes da base | conferem |
| Versões: Javalin, Flyway, Maven, JaCoCo, imagem | `pom.xml`, Dockerfiles, `meta.json` | conferem; Semgrep e SonarQube não reabri |

Não recalculei nesta rodada a Tabela 6, que a rodada 6 conferiu em `ck/class.csv`.

### Termos centrais

| termo | situação |
|---|---|
| configuração, orientação, orientações persistentes, *harness* | nomes declarados na Introdução; ponto já decidido 15 |
| acerto, certo, *pattern* certo | sentidos diferentes nos dois estudos, com aviso nas Seções 6.2 e 7 |
| exagero, estrutura desnecessária, contenção | distinguidos na Seção 6.2 |
| corretude, comportamento | diferença declarada na P2 |
| instrução de usar a *skill* | aparece com outro nome uma vez; ver 13 |
| piloto, fase intermediária | ver 17 |

### Proporção

Contei as palavras do corpo, incluindo as tabelas (18.657 palavras). Introdução: 8,5%. Fundamentação: 3,8%. Trabalhos relacionados: 3,1%. Método e descrição do que foi construído (Seções 4, 5.1, 5.2, 6.1 e 6.2): 39,8%. Resultados (5.3 e 6.3): 19,2%. Discussão: 14,5%. Limitações: 7,7%. Conclusão: 3,3%. A Discussão fica acima da faixa do guia, o que se explica por responder oito perguntas, e parte dela pode sair (5). Fundamentação e Trabalhos relacionados ficam abaixo, ponto já na sua lista.

### Percurso de cada objetivo

O percurso está na matriz de rastreabilidade, no fim deste relatório. As oito perguntas têm resposta explícita na Seção 7.

### Proveniência dos resultados

| resultado | etapa de origem | situação |
|---|---|---|
| acerto nos pontos e exagero no ponto do seguro | Seção 5.1 | descrito |
| suíte, *build* | Seção 5.1 | descrito; referência ambígua em 2 |
| complexidade, duplicação, linhas, *code smells* | Seção 5.1 | descrito |
| CK nos dois estudos | Seções 2, 5.1 e 6.2 | descrito |
| *tokens*, tempo, turnos, custo | Seções 4, 5.1 e 6.2 | descrito |
| perfis distintos, abertura da *skill*, revisor | Seção 5.1 | descrito |
| categoria do *pattern*, dimensões, estrutura desnecessária | Seção 6.2 | descrito |
| descrição qualitativa dos malfeitos e da interface `Season` | relatório escrito do verificador | origem não descrita; ver 15 |
| reservas simultâneas, `mvn verify` | Seções 4 e 6.2 | descrito |
| estabilidade, segundo verificador, controles negativos | Seção 6.2 | descrito |
| *tokens* com o subagente | Seção 8 | citado, sem a etapa; ver 16 |

### Justificativa das escolhas

Têm motivo declarado: o Strategy, os cinco *patterns*, o Javalin, o banco, o esforço *medium*, o Fable fora, cinco rodadas, o quarteto, os limites das regras, o N1 contra o N0 nas principais, o N3 fora da extensão, a posição da instrução da *skill*, os três modelos, o Opus 5.5, as 30 revisões, as duas rodadas de controle, a exigência de migrações, o Semgrep, o SonarQube e o CK. Não encontrei escolha de método sem motivo que mude a leitura de um resultado.

## Apontamentos por critério

### Critério 1. Língua portuguesa e redação

Não encontrei erros de ortografia, acentuação ou concordância, nem marcadores pendentes, fórmulas de ênfase, gerúndio de comentário ou resíduos de conversa.

**1. SUGESTÃO. Frases longas que encadeiam números ou definições.**
Onde: restam 37 frases com mais de 50 palavras. As que mais pedem releitura são: Seção 6.2, "A arquitetura foi avaliada pelas dependências" (76 palavras); Seção 6.2, "Os três níveis de cada cenário e rodada ficavam lado a lado" (68); Seção 6.3, "Por modelo (P3), as execuções com o *pattern* certo" (62).
Motivo: o guia pede uma informação principal por frase, e aqui o leitor precisa voltar para saber a que medida cada número pertence.
Correção: quebre essas três em frases dentro do mesmo parágrafo, sem usar lista.

### Critério 2. Encadeamento e coerência

**2. RECOMENDADA. "A mesma etapa" mudou de referente.**
Onde: Seção 5.1, "A suíte, essas versões e as regras do Semgrep foram escritas com apoio de IA, na etapa de desenho dos instrumentos (Tabela 1). Entre os 21 casos [...]. A mesma etapa registrou se o projeto compila e se o serviço sobe".
Problema: antes da frase acrescentada nesta versão, "a mesma etapa" era a aferição pela suíte. Agora a única etapa nomeada é a de desenho, que não registra *build* de nenhuma execução. O registro é feito pelo `acceptance.sh`, ao rodar a suíte.
Motivo: H13 usa o *build* como medida, e o leitor precisa saber quando ele foi registrado.
Correção: antes: "A mesma etapa registrou se o projeto compila". Depois: "A execução da suíte registrou também se o projeto compila".

**3. SUGESTÃO. Tabela citada antes de ser apresentada.**
Onde: Seção 6.3, P1, "é a execução sem código da Tabela 9"; a Tabela 9 é apresentada só na P8.
Correção: diga que a execução não escreveu código e deixe a remissão à Tabela 9 para a legenda, que já traz essa informação. Motivo: o guia pede que cada tabela seja apresentada antes de aparecer.

**4. SUGESTÃO. Última frase do parágrafo da P1.**
Onde: Seção 6.3, depois de "A Figura 10 apresenta os dados da Tabela 8", vem "Somente de N0 a N1, o Decorator e o Template Method somaram +18".
Problema: a frase é sobre o arquivo sozinho e fica entre a chamada da figura e a figura.
Correção: leve-a para antes da chamada da Figura 10.

**5. SUGESTÃO. O que sobra no parágrafo da trajetória.**
Onde: Seção 7, "Por fim, quanto à trajetória do estudo".
Problema: "O lote descartado foi abandonado antes da análise confirmatória" repete a Seção 5.2. A lista das mudanças repete a Seção 6.1 e o começo do parágrafo da extensão na Seção 8. A exceção do Strategy já está na P1.
Correção: mantenha só o que é síntese dos dois estudos, nas duas últimas frases sobre o aumento e a contenção. Motivo: a Discussão é a seção mais acima da faixa do guia.

**6. SUGESTÃO. Repetições na Seção 8.**
Onde: Seção 8, parágrafo "Quanto à execução da extensão": "as repetições do Factory Method e do Decorator são de 22/10" e "A escolha de quais repetir primeiro foi feita depois de ver a primeira rodada"; "Os contêineres não dispunham de Docker".
Problema: as datas e o Docker já estão na Seção 6.2, e a escolha aparece também na trajetória da Seção 7.
Correção: fique com a consequência para a validade e retire a descrição que o método já deu.

**7. SUGESTÃO. Comparação com números que o texto não mostra.**
Onde: Seção 8, "Os números absolutos também não se comparam com os da fase intermediária".
Problema: o artigo não apresenta resultados da fase intermediária, e a Seção 4 já diz que os números das fases preparatórias não são somados.
Correção: retire a frase.

**8. SUGESTÃO. Parêntese sem função.**
Onde: Seção 5.2, "119 execuções (62 até 06/10)".
Problema: o leitor não sabe o que aconteceu em 06/10, e a data não é usada depois.
Correção: retire o parêntese.

**9. SUGESTÃO. Detalhe de custo do segundo verificador.**
Onde: Seção 6.2, "cada revisão desse modelo custou cerca de 3,5 vezes a do Opus 5.5; nas 30 revisões, a razão foi de cerca de 3 vezes".
Problema: o número previsto justifica as 30 revisões; o realizado, acrescentado depois, não muda nada na leitura.
Correção: retire a oração depois do ponto e vírgula.

**10. SUGESTÃO. Última frase sobre o CK.**
Onde: Seção 2, "Na versão usada da ferramenta (*commit* 8a1ef916), o CBO conta as dependências da classe [Aniche 2015]".
Problema: o parágrafo já definiu o CBO, e o *commit* está na Tabela 3. Se a intenção era dizer que só contam as dependências de saída, a frase não diz isso.
Correção: retire, ou diga o que muda em relação ao original.

**11. SUGESTÃO. Conferência humana repetida na Seção 4.**
Onde: Seção 4, "O único leitor humano foi o autor, que conferiu a classificação do Semgrep"; "a conferência humana do estudo principal foi feita pelo autor"; "e conferiu a classificação do Semgrep na leitura humana".
Correção: a terceira menção pode sair, porque está dois períodos depois da segunda e a Tabela 1 também a registra.

**12. SUGESTÃO. Repetição na Conclusão.**
Onde: Seção 9, "o comportamento caiu de 61 para 54" e, dois parágrafos depois, "o comportamento caiu de 59 para 54"; "a estrutura desnecessária, também fora da calibração, subiu de 4 para 9" e "a *skill* veio acompanhada de mais estrutura desnecessária, medida por uma nota que a calibração não cobriu".
Problema: os dois fatos aparecem duas vezes, o primeiro com bases diferentes.
Correção: resolva junto com o apontamento 18, deixando cada fato em um lugar.

### Critério 3. Terminologia e siglas

As siglas pouco conhecidas estão expandidas, e os estrangeirismos estão em itálico no texto corrido.

**13. SUGESTÃO. "A linha da *skill*".**
Onde: Seção 7, trajetória, "a linha da *skill* no N2 da manutenção".
Correção: antes: "a linha da *skill*". Depois: "a posição da instrução de usar a *skill*". Motivo: é o nome usado na Tabela 2 e na Seção 6.2.

### Critério 4. Resumo e Abstract

O Resumo traz problema, objetivo, método e resultado com números. O Abstract é tradução fiel, frase a frase.

**14. RECOMENDADA. Ganho maior na manutenção sem a ressalva.**
Onde: Resumo, "e mais na manutenção (de 19 para 58 execuções em 75) que na construção (de 41 para 60)"; Abstract, "and more in maintenance". Comparar com a Seção 7, P5, "a comparação entre elas não isola o efeito da tarefa", e a Seção 9, "parte da diferença decorre do espaço para melhorar".
Problema: a frase foi acrescentada nesta versão e é a afirmação mais frágil do Resumo. Contando o malfeito como estrutura presente, a distância quase some (manutenção de 45 para 72, construção de 48 para 69), e a redação do `CLAUDE.md` difere entre as tarefas.
Motivo: quem lê só o Resumo sai com uma comparação entre tarefas que o próprio texto diz não poder isolar. O guia pede a mesma cautela no Resumo e na Conclusão.
Correção: acrescente uma oração curta sobre o ponto de partida menor, ou retire a comparação e deixe só os números. O Resumo está em 249 palavras, então cada acréscimo pede um corte.

### Critério 5. Introdução

Sem apontamentos. A Introdução apresenta contexto, problema, relevância com fontes, objetivo geral, perguntas numeradas, contexto institucional e organização do texto. O texto não menciona o AYTY, e o trabalho aparece como TCC da UFPB.

### Critério 6. Coerência metodológica

A metodologia está completa quanto a etapas, ferramentas, versões, envolvidos e critérios.

**15. RECOMENDADA. Origem da descrição dos malfeitos.**
Onde: Seção 6.3, "Nessas 12 execuções com malfeito, cada plano tem classe própria, e, em 11 delas, resta um único ponto fora da estrutura"; Seção 7, P1, "produziu estruturas quase completas"; Seção 6.3, controles, "o agente criou uma interface `Season`"; Seção 6.2, que descreve notas e categorias e não menciona o relatório escrito.
Problema: li os 12 `review.md`, e o número confere. Mas a informação é o texto que o verificador escreveu, e não uma medida. O leitor entende que alguém contou no código.
Motivo: o guia pede a origem de cada resultado. Aqui ela importa mais, porque a Seção 8 registra que a calibração não testou o malfeito, e a descrição sustenta a nova leitura da exceção do Strategy.
Correção: diga na Seção 6.2 que o verificador grava uma justificativa por dimensão e, na Seção 6.3, que a descrição vem dela. Se você conferiu alguma no código, informe quantas.

**16. SUGESTÃO. *Tokens* com o subagente.**
Onde: Seção 8, "os vereditos das hipóteses de *tokens* permanecem os mesmos quando esse consumo, registrado na transcrição, é incluído".
Problema: reproduzi com `usage_by_model` em `meta.json`, e a afirmação confere. O cálculo, porém, não está em nenhum *script* do repositório.
Correção: diga qual campo foi usado, ou acrescente o *script* ao repositório. Motivo: o leitor não tem como repetir a conta.

**17. SUGESTÃO. Piloto fora do mapa das fases.**
Onde: Seção 6.2, "em um piloto sem banco de dados, anterior às execuções relatadas aqui"; Seção 4 e Figura 3, que não têm essa etapa.
Problema: o piloto é citado uma vez e justifica uma diferença que só existe no N2 da manutenção, onde está o maior ganho. Os dados estão em `TCC_V1/maintenance/runs-2026-10-21-sem-banco`.
Correção: diga em uma oração que esse piloto é o início da extensão, ou como ele se relaciona com a fase intermediária, e indique a pasta.

### Critério 7. Apresentação, resultados e conclusão

A solução está descrita com artefatos e métricas de dimensão, e o processo de avaliação está descrito. As limitações são específicas, e os trabalhos futuros decorrem do trabalho.

**18. OBRIGATÓRIA. Os efeitos da *skill* nas demais dimensões são lidos com dois pesos.**
Onde: Seção 7, P6, "o que não permite ler o valor como efeito da orientação" e "mas não na arquitetura"; Seção 7, P6, "subiram só no N1 (25, 37 e 25)"; Seção 9, "mas o comportamento caiu de 59 para 54 execuções boas em 75 do N1 para o N2".
Problema: o N2 difere do N1 pela *skill* e por uma linha do `CLAUDE.md` (conferi o `diff`). A passagem do N1 para o N2 é, portanto, a leitura do componente, que é o que a P4 e a recomendação da Conclusão tratam. Fonte: `review.json`, contando nota bom, regra que reproduz todas as células das Tabelas 7 e 14. Os pares são do mesmo cenário, modelo e rodada.

| manutenção, N1 para N2 | N1 | N2 | pares melhores | pares piores | saldo por modelo (Haiku 4.5, Sonnet 4.5, Sonnet 5.5) |
|---|---|---|---|---|---|
| arquitetura | 57 | 68 | 13 | 2 | +4, +5, +2 |
| testes | 37 | 25 | 7 | 19 | −5, −5, −2 |
| comportamento | 59 | 54 | 6 | 11 | −4, −1, 0 |
| banco | 62 | 66 | 8 | 4 | +4, 0, 0 |
| *design* do restante | 25 | 25 | 5 | 5 | +1, +1, −2 |

O comportamento é o único desses movimentos que chega à recomendação. A queda dos testes é maior, mais consistente e aparece nos três modelos, e a alta da arquitetura tem o sinal mais limpo da tabela. O argumento "o N1 não acompanha" descarta a arquitetura, mas o mesmo padrão vale para o ganho do *pattern* com a *skill* e para a queda do comportamento, que o texto aceita. Na construção, o N1 para o N2 também vai de 9 para 16 na arquitetura (12 pares melhores, 5 piores) e de 9 para 6 nos testes (3 e 6). Não investiguei a causa e não há teste de significância.
Motivo: o guia diz que uma recomendação prática precisa refletir tudo o que os dados mostraram, e não só parte deles. Quem lê a Conclusão entende que o custo da *skill* na manutenção é o comportamento, quando o maior custo medido é nos testes, e que não houve ganho fora do *pattern*.
Correção: aplique o mesmo critério às três dimensões na P6, retire ou reformule "mas não na arquitetura" e inclua testes e arquitetura na recomendação da Seção 9, com a cautela de que é descrição exploratória.

**19. RECOMENDADA. H5 e o outro modelo empatado.**
Onde: Seção 5.3, "O Opus 4.7 teve o maior saldo (+5), e o Haiku 4.5, +1, de modo que a hipótese foi apoiada"; Seção 7, P3, "O maior ganho foi do Opus 4.7, que, empatado com o Haiku 4.5, era o que menos acertava"; Tabela 13.
Problema: a regra de empate está registrada em `DECISOES.md` e em `hipoteses.mjs`, e o veredito está certo. Mas o Haiku 4.5, igualmente sem acertos no N0, teve o menor saldo fora do teto (+1, junto com o Sonnet 5). A resposta da P3 mostra só o lado favorável.
Motivo: H5 é uma das cinco hipóteses principais, e o leitor da Discussão entende que quem menos acerta ganha mais.
Correção: acrescente na P3 uma oração com o saldo do Haiku 4.5 e diga que o apoio depende da regra de empate.

**20. RECOMENDADA. Exceções do controle e estrutura desnecessária.**
Onde: Seção 7, parágrafo após a P2, "As duas exceções, ambas com a *skill* e no mesmo modelo e cenário, são coerentes com o risco indicado pela construção, em que a estrutura desnecessária também aumenta com a *skill*"; Tabela 14.
Problema: as duas exceções são do Sonnet 5.5. Na construção, esse modelo tem estrutura desnecessária 0, 0 e 0. A alta vem do Haiku 4.5 (1, 0, 3) e do Sonnet 4.5 (3, 5, 6). Na manutenção, o Sonnet 5.5 também fica em zero.
Motivo: a frase liga dois achados como se fossem o mesmo risco, e os dados mostram modelos diferentes. Isso muda o que o leitor conclui sobre quem exagera com a *skill*.
Correção: diga que os modelos são diferentes, ou retire a ligação. A Seção 8 já diz que as duas exceções não bastam para estimar o risco.

**21. SUGESTÃO. "Oscilaram" no banco.**
Onde: Seção 7, P6, "Arquitetura e banco oscilaram sem direção clara"; Tabela 7, banco na construção, 0, 0 e 0.
Problema: na construção, o banco não oscila: fica em zero pela exigência de migrações.
Correção: restrinja a frase à manutenção. O ajuste pode ser feito junto com o apontamento 18.

### Critério 8. Figuras, tabelas e gráficos

Sem apontamentos. Inspecionei as dez figuras. Todas têm número, título e fonte, e todas são apresentadas antes de aparecer, com descrição. Os gráficos têm eixos e legenda, e os painéis da Figura 10 usam a mesma escala. As 16 tabelas têm número, título, fonte e chamada no texto, com a ressalva de ordem do apontamento 3.

### Critério 9. Consistência de conteúdo

Os números são consistentes entre as seções, e o estilo é uniforme.

**22. RECOMENDADA. O que é secundário e o que não teve conferência humana.**
Onde: Seção 5.1, "No pagamento, porém, o enunciado não diz que novas formas virão [...]; por isso, e por não ter conferência humana, esse ponto é secundário"; Seção 5.3, "Os pontos de frete, cupom e pagamento são medidas secundárias, cuja classificação não teve conferência humana".
Problema: a frase nova da Seção 5.1 atribui ao pagamento uma condição que também vale para frete e cupom. Quem lê o método entende que esses dois são principais e foram conferidos, e descobre o contrário só nos resultados.
Motivo: a mesma informação precisa ter o mesmo alcance nas duas seções, e a conferência é o que dá validade à classificação.
Correção: diga na Seção 5.1 que os três pontos são secundários e sem conferência humana, e deixe o motivo próprio do pagamento como acréscimo.

### Critério 10. Normas e referências

**Citações e lista.** As 43 referências da lista estão citadas, e todas as citações do texto estão na lista. Não há "[CITAR]", `utm_source=` nem citação direta de livro sem página. A numeração das seções é consistente.

**Conferidas nesta rodada pelo Crossref (26 de 26 com DOI).** Todos os DOIs levam ao trabalho citado, com autor e veículo iguais: Ahmed et al. 2025; Ampatzoglou et al. 2026; Arabat e Sayagh 2026; Arcuri e Briand 2014 (publicação *online* em 2012, fascículo de 2014); Basili et al. 1996; Boehm 1976; Campbell 2018; Chatlatanagulchai et al. 2026; Chidamber e Kemerer 1994; Ernst e Baldassarre 2023; Galster et al. 2026; He et al. 2026; Hou et al. 2024; Jiang e Nam 2026; Kim 2025; Kim 2026; Kjellberg et al. 2026; Liu et al. 2026; Muñoz Barón et al. 2020; Prechelt et al. 2001; Sallou et al. 2024; Vokáč et al. 2004; Wang et al. 2025; Xi et al. 2025; Zhang e Budgen 2012; Zisa et al. 2026.

**Links.** Os 23 endereços do texto responderam com sucesso. Pela API do GitHub, confirmei que o repositório público traz os resultados da manutenção com os controles, o segundo verificador e o `OBJETIVO.md`.

**Não reconferidas nesta rodada.** Brown et al. 2020, Vaswani et al. 2017, Fowler 2018, Gamma et al., Martin, Meyer e Yourdon e Constantine foram conferidos em rodadas anteriores; a consulta ao DBLP não retornou dados.

**Limite desta conferência.** Conferi a existência e os metadados, e não o que cada artigo afirma.

**23. SUGESTÃO. Yao et al. 2023 continua sem conferência.**
Onde: Referências, "Yao, S. et al. (2023). ReAct".
Problema: não tem DOI, o DBLP não respondeu, e o OpenReview devolveu uma página de verificação de navegador.
Correção: abra essa referência manualmente antes da entrega. Motivo: o guia pede que cada referência seja conferida.

### Critério 11. Tecnologias e repositório

Sem apontamentos. A Tabela 3 traz tecnologias com versão e papel. O link do repositório está no texto e abre para qualquer pessoa.

## Matriz de rastreabilidade

| objetivo ou pergunta | onde é desenvolvido | etapa do método que gera os dados | resultado | resposta explícita | situação | ação pendente |
|---|---|---|---|---|---|---|
| Objetivo geral: avaliar o efeito de orientações persistentes sobre a decisão de aplicar um *pattern*, na construção e na manutenção | Seções 5 e 6 | Seções 5.1 e 6.2 | Tabelas 5, 7 e 8 | Seção 9, primeiro e segundo parágrafos | atendido | nenhuma |
| P1. Uso do *pattern* esperado | Seções 5.3 e 6.3 | Semgrep (5.1); verificador (6.2) | Tabelas 4, 5, 7, 8 e 13; Figuras 9 e 10 | Seção 7, P1; Seção 9 | atendido | 15 |
| P2. Exagero, corretude e qualidade do código | Seções 5.3 e 6.3 | Semgrep, suíte, SonarQube (5.1); verificador e controles (6.2) | Tabelas 4, 5, 12 e 14 | Seção 7, P2; Seção 9 | atendido | 2, 20, 22 |
| P3. Variação entre modelos | Seções 5.3 e 6.3 | quartetos e regras (5.1); três modelos (6.2) | Tabelas 13 e 14 | Seção 7, P3; Seção 9 | atendido | 19 |
| P4. Componentes e custo | Seções 5.3 e 6.3 | níveis, custo, abertura da *skill* (5.1); custo (6.2) | Tabelas 4, 5, 11 e 15 | Seção 7, P4; Seção 9 | atendido, com recomendação a ajustar | 16, 18 |
| P5. Tipo de tarefa | Seção 6.3 | tarefas e categorias (6.2) | Tabelas 7, 8 e 10; Figuras 9 e 10 | Seção 7, P5; Seção 9 | atendido | 14 |
| P6. Demais dimensões | Seção 6.3 | dimensões e roteiros (6.2) | Tabelas 7 e 14 | Seção 7, P6; Seção 9 | atendido, com leitura a ajustar | 18, 21 |
| P7. Estabilidade | Seções 5.3 e 6.3 | perfis (5.1); repetições (6.2) | Seção 5.3; Tabela 16 | Seção 7, P7; Seção 9 | atendido | nenhuma |
| P8. Estrutura orientada a objetos | Seções 5.3 e 6.3 | CK (2, 5.1 e 6.2) | Tabelas 6, 9 e 10 | Seção 7, P8; Seção 9 | atendido | nenhuma |
| Controle negativo na manutenção (tratado com a P2) | Seção 6.3 | Seção 6.2 | Tabela 12 | Seção 7, parágrafo após a P2; Seção 9 | atendido | 20 |

Nenhuma célula ficou como "não encontrado", e não encontrei resultado sem pergunta correspondente.

## Checklist (seção 10 do guia)

**Elementos obrigatórios da resolução**

| item | situação |
|---|---|
| A Introdução descreve o problema e o contexto | atendido |
| A metodologia descreve o processo e identifica os envolvidos | atendido |
| Há descrição das tecnologias, com versão | atendido |
| A solução é apresentada com artefatos e métricas | atendido |
| O texto traz o link do repositório público | atendido |
| Se houve avaliação, o processo e os resultados estão descritos | atendido |

**Critério 1**

| item | situação |
|---|---|
| Sem erros de ortografia, acentuação, concordância ou regência | atendido |
| Tempos verbais consistentes | atendido |
| Sem frases longas que encadeiam resultados | parcial (1) |
| Sem declarações de importância sem fato | atendido |
| Sem gerúndio de comentário | atendido |
| "É" e "tem" onde cabem | atendido |
| Afirmações de terceiros com fonte | atendido |
| Sem contrastes montados e fórmulas | atendido |
| Títulos com maiúscula só onde cabe | atendido |
| Sem vírgula antes do "e" final e sem decalques | atendido |

**Critério 2**

| item | situação |
|---|---|
| Cada seção prepara a seguinte | parcial (2, 4) |
| Todo termo técnico definido antes do uso | atendido |
| Cada pergunta retomada e chega à Conclusão com o mesmo nome | atendido |
| Tamanho de cada seção corresponde ao papel | parcial (5; ponto já decidido 15) |

**Critério 3**

| item | situação |
|---|---|
| Cada conceito tem um nome só | parcial (13; ponto já decidido 15) |
| Nenhum nome designa duas coisas | atendido |
| Siglas pouco conhecidas expandidas | atendido |
| Estrangeirismos em itálico | parcial (ponto já decidido 16) |

**Critério 4**

| item | situação |
|---|---|
| Resumo com problema, objetivo, o que foi feito e resultado com número | atendido |
| Resumo se entende sozinho | atendido |
| Abstract é tradução fiel | atendido |

**Critério 5**

| item | situação |
|---|---|
| Contexto, problema e relevância com fatos ou fontes | atendido |
| Objetivos ou perguntas claros, numerados e verificáveis | atendido |
| Objetivos correspondem ao que foi feito | atendido |
| Contexto institucional explicado | atendido |
| Parágrafo de organização corresponde às seções | atendido |

**Critério 6**

| item | situação |
|---|---|
| Cada etapa diz o que, como, com quais ferramentas e quem | atendido |
| Sequência e dependências claras | parcial (17) |
| Cada escolha tem motivo declarado | parcial (ponto já decidido 17) |
| Todo resultado tem etapa de origem | parcial (15, 16) |
| Toda etapa aparece nos resultados | atendido; os dois itens sem cálculo estão declarados na Seção 8 |
| Versões informadas | atendido |

**Critério 7**

| item | situação |
|---|---|
| Solução com métricas e artefatos | atendido |
| Resultados concretos e organizados pelas perguntas | atendido |
| Cada pergunta com resposta explícita | atendido |
| Sem resultado desligado dos objetivos | atendido |
| Conclusão com resultados concretos e as mesmas ressalvas | parcial (18) |
| Limitações e trabalhos futuros específicos | atendido |

**Critério 8**

| item | situação |
|---|---|
| Toda figura e tabela com número, título e fonte | atendido |
| Toda figura e tabela apresentada com descrição | parcial (3) |
| Todo gráfico com eixos e legenda | atendido |
| Código em texto, nenhum em imagem | pendente pelo guia, mantido por decisão (ponto já decidido 1) |
| Nenhuma imagem com fundo preto ou escuro com código | atendido |
| Mesma informação não repetida em tabela e figura | parcial, mantido por decisão (ponto já decidido 3) |

**Critério 9**

| item | situação |
|---|---|
| Mesmo número com o mesmo valor | atendido |
| Resumo, Introdução e Conclusão com a mesma cautela | parcial (14) |
| Sem contradição entre seções | parcial (20, 22) |
| Sem trechos genéricos | atendido |
| Sem etapas citadas uma vez e nunca mais | parcial (17) |
| Estilo uniforme | atendido |

**Critério 10**

| item | situação |
|---|---|
| Toda citação na lista e vice-versa | atendido |
| Cada DOI leva ao trabalho citado | atendido (26 de 26 pelo Crossref) |
| Sem marcador pendente | atendido |
| Citações diretas de livro com página e links com data de acesso | atendido |
| Numeração consistente e elementos do modelo do curso | parcial: a numeração confere; o modelo só pode ser conferido no arquivo final, porque revisei o Markdown |

**Critério 11**

| item | situação |
|---|---|
| Tecnologias em tabela, com versão e papel | atendido |
| Link do repositório abre para qualquer pessoa | atendido |

**Revisão do texto inteiro e matriz**

| item | situação |
|---|---|
| Leitura do texto inteiro | atendido nesta autoavaliação; cabe a você repetir antes da entrega |
| Matriz completa, sem "não encontrado" | atendido |

**Uso de ferramentas de IA**

| item | situação |
|---|---|
| Declaração de uso depois da Conclusão | pendente (ponto já decidido 4) |
| IA usada como instrumento descrita na metodologia | atendido |
| Conferi todo conteúdo, código e referência sugeridos | pendente: só você pode confirmar, e falta abrir Yao et al. 2023 (23) |
