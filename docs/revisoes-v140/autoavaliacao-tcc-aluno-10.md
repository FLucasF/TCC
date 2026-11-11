Autoavaliação feita com ferramenta de IA. A avaliação oficial é do orientador.

# Autoavaliação antes da entrega: artigo, versão 140 (rodada 10)

Este relatório aplica os critérios de revisão do professor Rodrigo Rebouças de Almeida, que são orientações dele para os seus orientandos e não critérios oficiais da disciplina, do curso ou da UFPB. O uso desta ferramenta é uso de IA e entra na declaração de uso do TCC (seção 8 do guia). O relatório não diz se o trabalho está aprovado e não reescreve o seu texto.

Os apontamentos têm número único, na ordem da seção "Apontamentos por critério". No total, são 0 obrigatórios, 2 recomendados e 3 sugestões. Os pontos já decididos usam a numeração D1 a D19, para não se confundirem com os apontamentos.

## Pontos já decididos

Estes pontos foram decididos por você, já foram verificados ou dependem de você. Nenhum conta como pendência neste relatório, e nenhum apontamento abaixo pede lista no corpo do texto.

**Protegidos por decisão sua**

- D1. As figuras de código (Figuras 2, 5, 6, 7 e 8) ficam como imagem com fundo branco. Abri as cinco, e todas têm fundo branco.
- D2. A Seção 6.2 mantém uma figura por *pattern*.
- D3. As Figuras 9 e 10 ficam junto das Tabelas 7 e 8.
- D4. A declaração de uso de ferramentas de IA será escrita por último. O apoio à redação do artigo não entra na Tabela 1.
- D5. A coluna "como foi conferido" da Tabela 1 foi confirmada por você.
- D6. O título continua o mesmo, e o nome da H8 segue o pré-registro.
- D7. Os motivos que você deu estão mantidos: o Strategy por ser difundido e adequado ao domínio; os cinco *patterns* por cobrirem as três categorias do catálogo; o N3 fora da extensão; a segunda rodada do controle decidida antes do resultado da primeira; as 10 horas entre níveis por causa da janela de 5 horas da cota.
- D8. As referências ficam só com revistas e eventos respeitados, sem *preprints*, com correções só de textos publicados, e a definição de IA vem de livro.
- D9. O registro é prosa impessoal em parágrafos encadeados, sem listas no corpo e sem rótulos em negrito no começo do parágrafo. As perguntas P1 a P8 ficam em parágrafo.
- D10. O Resumo fica entre 150 e 250 palavras, e o Abstract é tradução fiel. Contei 250 palavras em cada um.
- D11. A Introdução define cada termo para o leitor.
- D12. *Harness* segue o sentido de Galster et al., com o aviso sobre o pré-registro.
- D13. O uso só de modelos da Anthropic está declarado como limitação.

**Já verificado: não procede**

- D14. Tabela 11, *tokens* de saída do N2 da construção (38,9 mil): todas as células usam a soma dos segmentos. Não reabri o ponto.

**Pendências que dependem de você**

- D15. Tabela comparativa dos trabalhos relacionados (Apêndice C do guia), um nome só para os níveis e "et al." nas referências.
- D16. Citar Page (1963) e Cohen (1960) depende de você ler ou aprovar. Itálico nas figuras.
- D17. O motivo de a hipótese "quem melhora não é quem exagera" e a proporção entre ler, escrever e executar não terem sido calculadas (OBJETIVO §4.5 e §4.6). O texto só registra o fato.
- D18. O aviso da API do OpenAlex pedindo o e-mail do usuário, de uma rodada anterior, não foi atendido. Nesta rodada não usei o OpenAlex. A API do Semantic Scholar devolveu, num campo de aviso, endereços do Unpaywall com o marcador `<INSERT_YOUR_EMAIL>`; não preenchi nem abri esses endereços.
- D19. Yao et al. 2023 continua sem conferência por automação e precisa ser aberta por você. Nesta rodada não tentei de novo. Nenhuma página ou API trouxe instrução dirigida a mim.

## Pontos a tratar

### Obrigatórios

Nenhum. Não encontrei número errado, resultado sem etapa de origem, pergunta sem resposta ou elemento obrigatório ausente, e nenhum dos pontos abaixo muda o valor de um resultado.

### Recomendados, por peso

| nº | ponto | onde |
|---|---|---|
| 2 | A nota de testes da construção não diz o que mede numa tarefa em que os testes não foram pedidos, e a Conclusão usa essa nota numa recomendação. | Seções 6.2, 7 e 9 |
| 4 | A abertura da Discussão diz que a extensão não demonstra efeito, e dois trechos ainda falam em "efeito" da orientação com base nela. | Seção 7 |

### Sugestões

| nº | ponto | onde |
|---|---|---|
| 1 | A frase reescrita na rodada anterior põe o 19, do N0, entre "os níveis com as mesmas orientações". | Seção 7, controle negativo |
| 3 | A execução sem código conta como omissão, e o texto não diz. | Seções 6.2 e 6.3 |
| 5 | As 119 execuções são só de `construction-v3`, e a Figura 3 põe duas pastas nas bancadas. | Seção 5.2; Figura 3 |

## Leitura macro

### Mapa das seções

| seção | o que entrega | o que prepara |
|---|---|---|
| Resumo e Abstract | problema, objetivo, os dois estudos e os resultados principais | a leitura do trabalho |
| 1. Introdução | contexto, *harness*, *patterns*, relevância, objetivo geral, P1 a P8, contribuições e organização | as definições e o compromisso das perguntas |
| 2. Fundamentação | encapsulamento, polimorfismo, aberto/fechado, acoplamento, coesão e métricas | a leitura das Tabelas 6, 9 e 10 |
| 3. Trabalhos relacionados | os trabalhos próximos e a lacuna | a diferença do desenho experimental |
| 4. Visão geral do processo | fases, desenhos, repositório, envolvidos, IA (Tabela 1), níveis (Tabela 2), tecnologias (Tabela 3) | os dois estudos |
| 5. Estudo principal | método, regras, 28 hipóteses, proteção contra decisões posteriores e resultados | os pontos em aberto da Seção 6.1 |
| 6. Extensão | evolução do desenho, método, calibração, controles e resultados | a Discussão |
| 7. Discussão | resposta a P1 a P8, comparação com os trabalhos e trajetória | Limitações e Conclusão |
| 8. Limitações | ameaças por estudo, avaliação e métricas | a cautela da Conclusão |
| 9. Conclusão | resultados, recomendação prática e trabalhos futuros | fim |
| Apêndice A | Tabelas 13 a 16 | apoio às Seções 5.3, 6.3 e 7 |

O parágrafo de organização corresponde às seções e ao apêndice que existem, na mesma ordem.

### Linha do argumento

A sequência é coerente: o problema leva às perguntas, o estudo principal mede a decisão de usar um *pattern*, as suas lacunas motivam a extensão, e a Discussão responde na ordem das perguntas. As correções da rodada anterior ficaram bem encadeadas, com uma exceção pequena (1).

### Resumo, Introdução e Conclusão

Os três prometem o mesmo objeto, e os números que coincidem têm o mesmo valor. A Conclusão mantém as ressalvas da Discussão. A frase dos totais no Resumo e no Abstract agora tem verbo.

### Números repetidos

Somei as tabelas e recalculei a partir dos dados o que indico abaixo. Todos os valores fecham, e nenhum apontamento desta rodada é de número errado.

| conferência | fonte e regra | resultado |
|---|---|---|
| Tabelas 7, 8, 12 e 14, célula a célula | `review.json` das 540 revisões; *pattern* certo = acerto + alternativa equivalente; dimensão = nota bom | conferem |
| Tabela 15 e as somas 4, 10, 3, 5 e 11 | `design_class` por cenário e modelo | conferem |
| Tabela 7 contra Tabelas 8 e 14; Figuras 9 e 10 | soma das linhas e leitura das imagens | fecham |
| Tabela 10 contra Tabelas 7 e 8 | 126 + 28 = 154 = 41 + 53 + 60; 99 + 11 = 110 = 19 + 33 + 58; omissão 27 + 9 + 5 = 41, sem a execução sem código | fecham |
| Categorias: 27 e 30; 7 e 26; 32 e 15; 55 e 56; 9 e 14; 45, 53, 72; 48, 66, 69; 12, 8, 4 e 2 | `design_class` | conferem |
| Strategy na construção: omissão 6, 1, 1; malfeito 0, 7, 5; 4, 2 e 4 em 10 | `design_class` | conferem (ver 3) |
| Observer da manutenção, malfeito como categoria mais frequente | `design_class`: 27 de 45 | confere |
| Factory Method da manutenção no Sonnet 5.5: nenhuma em cinco no N0, dez em dez com orientação | `design_class` | confere |
| Decorator como o que mais errou e Factory Method como o que mais acertou na primeira rodada | `design_class` da rodada 1: 7 e 16 certos em 18 | confere |
| +18 e +8; 15 para 14 no Strategy; 14 e 12; 39 e 19; +25 e +14 | soma da Tabela 8 | conferem |
| Tabela 16, 294 de 360, 54 de 90 | recalculado por combinação; soma das linhas e colunas | conferem |
| Tabela 9 e LCOM | `oo-metrics.csv`, 449 execuções com código | conferem |
| Métodos abstratos no N2: 4,87 e 3,60; 4,20 e 2,13; 0,33 e 0 | `oo-metrics.csv` por cenário | conferem |
| Conta do CBO pelos tipos (22% a 46%; 3% a 27%) | Tabelas 6 e 9, pela regra declarada | confere |
| Tabela 5 | `notas-V5-STRATEGY.csv` (ponto com 10), `acceptance-V5-STRATEGY.csv` (`passed` = 21), `metricas.csv`, `resultados.csv` | confere |
| Tabela 6, inclusive percentil na posição 50 e 6, 1, 0 e 0 projetos com LCOM de 37,2 ou mais | `metricas.csv` | confere |
| Duplicação 1,2% e 2,5% | `metricas.csv` | confere |
| Tabela 4, 28 vereditos | `hipoteses-V5-STRATEGY.md`; "contrariada" em alteração lida como "não apoiada" | conferem |
| H3 com 10, 7 e +3; Haiku 4.5 com 0 e 5 | `acceptance-V5-STRATEGY.csv`, pares N1 e N0 | confere |
| Tabela 13 | soma das colunas: 37, 51, 51 e 54; saldo H1 +14; saldo H3 +3 | fecham com a Tabela 5 |
| Page 0,007 e abaixo de 0,001; perfis 7, 3 e 1; *tokens* 6 e 5 | `hipoteses-V5-STRATEGY.md` | conferem |
| 174 de 176 | `conferencia-V5-STRATEGY.md`: 44, 44, 42 e 44 | confere |
| Regra de empate da H5 e desvio da releitura | `DECISOES.md`, entradas de 09/10 e 12/10 | conferem |
| 36 de 55 (3,0%), 5 de 5, 8 de 9, 13% | `OBJETIVO.md` §4.1 | conferem |
| *Skill*: 69 de 75 em cada tarefa, 19 de 25 no Haiku 4.5; 20 de 30 nos controles | `skill_opened` em `meta.json` | conferem |
| Controles: 88 de 90; três notas fora de bom do Haiku 4.5; `mvn verify` nas 90 | `review.json` e `meta.json` | conferem |
| Reservas simultâneas: 59 e 16 | seção do roteiro em `measures/behavior.md` | conferem |
| Banco da construção: 39 e 36, 36 e 39, 35 e 40; uma execução com arquivos de migração; comportamento ruim 71 e 32 | `review.json`; arquivos da `workspace` | conferem |
| `mvn verify`: 8, 7, 4 e 1 falhas; 11 por Docker, 9 ruim e 2 com problemas | `build_exit_code`, `build.txt`, `review.json` | conferem |
| Tabela 11, custo; US$ 380 e US$ 346 | `cost_usd`, `cost_estimated_usd` | conferem |
| Segundo verificador: 30, 30, 29, 29, 27, 26, 24; 15 diferenças um degrau mais brandas; kappa 0,53 | `review.json` de `second-verifier` | conferem |
| Calibração: 26 revisões, 52 notas, 40 + 12; 58 revisões na Tabela 1 | conta do desenho | fecham |
| Piloto: Haiku 4.5 abriu a *skill* em 1 de 5 | `DECISOES.md`, entrada de 21/10 | confere |

Não recalculei nesta rodada os *tokens* e o tempo da Tabela 11, as datas e os intervalos das rodadas, o *commit* de congelamento e a cegueira do verificador, que rodadas anteriores conferiram e o texto não mudou.

### Termos centrais

| termo | situação |
|---|---|
| configuração, orientação, orientações persistentes, *harness* | nomes declarados na Introdução; D15 |
| acerto, certo, *pattern* certo | sentidos diferentes nos dois estudos, com aviso nas Seções 6.2 e 7 |
| exagero, estrutura desnecessária, contenção | distinguidos na Seção 6.2 |
| corretude, comportamento | diferença declarada na P2 |
| pontos da suíte, suíte inteira | diferença declarada na Seção 5.3 |
| bancada, bancadas | ver 5 |

### Proporção

Contei as palavras do corpo, com as tabelas (19.953 palavras). Introdução: 8,1%. Fundamentação: 3,6%. Trabalhos relacionados: 2,9%. Seção 4: 7,7%. Seção 5: 20,8%. Seção 6: 27,2%. Discussão: 14,2%. Limitações: 7,3%. Conclusão: 3,3%. Apêndice: 4,9%. A Discussão fica acima da faixa do guia porque responde a oito perguntas. Fundamentação e Trabalhos relacionados ficam abaixo, e não vi falta de conceito usado depois. Sem apontamento.

### Percurso de cada objetivo

O percurso está na matriz de rastreabilidade, no fim deste relatório. As oito perguntas têm resposta explícita na Seção 7.

### Proveniência dos resultados

| resultado | etapa de origem | situação |
|---|---|---|
| acerto nos pontos e exagero no ponto do seguro | Seção 5.1 | descrito |
| suíte, pontuação e *build* | Seção 5.1 | descrito |
| complexidade, duplicação, linhas, *code smells* | Seção 5.1 | descrito |
| CK nos dois estudos | Seções 2, 5.1 e 6.2 | descrito |
| *tokens*, tempo, turnos, custo | Seções 4, 5.1 e 6.2 | descrito |
| perfis distintos, abertura da *skill*, revisor | Seção 5.1 | descrito |
| categoria do *pattern*, dimensões, estrutura desnecessária, justificativas | Seção 6.2 | descrito; ver 2 e 3 |
| reservas simultâneas, `mvn verify` | Seções 4 e 6.2 | descrito |
| estabilidade, segundo verificador, controles negativos | Seção 6.2 | descrito |

### Justificativa das escolhas

Têm motivo declarado: o Strategy, os cinco *patterns*, o Javalin, o banco, o esforço *medium*, o Fable fora, cinco rodadas, o quarteto, os limites das regras, o N1 contra o N0 nas principais, o N3 fora da extensão, a posição da instrução da *skill*, os três modelos, o Opus 5.5, as 30 revisões, as duas rodadas de controle, a exigência de migrações, o Semgrep, o SonarQube e o CK. Falta o motivo de dar nota aos testes na construção, em que eles não foram pedidos (2).

## Apontamentos por critério

### Critério 1. Língua portuguesa e redação

Sem apontamentos.

### Critério 2. Encadeamento e coerência

**1. SUGESTÃO. O 19 não pertence a um nível com orientação.**
Onde: Seção 7, parágrafo do controle negativo, "nos níveis com as mesmas orientações em que o *pattern* certo foi de 19 para 58 execuções em 75".
Problema: o 19 é o N0 (Tabela 7), que não tem orientação. A frase, reescrita na rodada anterior para tirar o verbo de causa, passou a dizer que o 19 foi medido num nível orientado. A mesma frase depois separa "as 60 execuções com orientação" das "30 do N0".
Motivo: os números estão certos, mas a frase pede releitura.
Correção: antes, "nos níveis com as mesmas orientações em que o *pattern* certo foi de 19 para 58 execuções em 75"; depois, "com as mesmas orientações que acompanharam a subida do *pattern* certo de 19 para 58 execuções em 75".

### Critério 3. Terminologia e siglas

Sem apontamentos.

### Critério 4. Resumo e Abstract

Sem apontamentos. O Resumo traz problema, objetivo, método e resultado com números, e o Abstract é tradução fiel, frase a frase.

### Critério 5. Introdução

Sem apontamentos. O texto não menciona o AYTY, e o trabalho aparece como TCC da UFPB.

### Critério 6. Coerência metodológica

**2. RECOMENDADA. A nota de testes na construção não tem motivo declarado.**
Onde: Seção 6.2, primeiro parágrafo, "sem que os testes fossem solicitados"; Seção 6.2, parágrafo das dimensões, "a ausência de testes, muitas falhas ou a falta de compilação dão ruim"; Seção 7, P6, "os testes caíram (11, 9 e 6, todos do Sonnet 5.5)"; Seção 9, "Do N1 para o N2, a arquitetura subiu e os testes caíram nas duas tarefas".
Problema: o texto diz que o enunciado da construção não pede testes e, depois, aplica a mesma régua das duas tarefas, em que a ausência de testes dá ruim. O parágrafo justifica a escolha equivalente no banco ("A exigência de migrações vale também para a construção [...] porque"), mas não a dos testes. O motivo existe no projeto: `TCC_V1/evaluation/quality/skills/review-tests/SKILL.md` diz que, na construção, o texto não pede testes e "o que se mede é a iniciativa e a qualidade do que o agente escreveu".
Motivo: critério de avaliação sem motivo faz o leitor perguntar por que um item não pedido é cobrado, e a Conclusão leva essa nota a quem escreve orientações. Não é obrigatória porque o dado não muda e porque a ausência de testes pesa pouco no resultado: em `build.txt`, só 11 das 225 execuções da construção não rodaram nenhum teste, todas do Haiku 4.5, modelo sem nota bom em nenhum nível. A queda de 11 para 6 ocorre no Sonnet 5.5, que escreveu testes nas 75 execuções.
Correção: confirme se o motivo da *skill* é o seu e declare-o numa oração, no parágrafo das dimensões, como foi feito para as migrações.

**3. SUGESTÃO. A execução sem código está contada como omissão.**
Onde: Seção 6.2, "Na omissão, a variação ficou em uma condicional ou em uma cópia por caso"; Seção 6.3, "a omissão foi de 6 para 1 e 1 execução"; Seção 6.3, "encerrou sem código".
Problema: a única omissão do N2 do Strategy na construção é a execução do Haiku 4.5 (rodada 2) que não escreveu código (`review.json`, `design_class` = omissão). Ela não tem condicional nem cópia, de modo que não cabe na definição. A Tabela 10 a exclui (41 omissões em 224), e as Tabelas 7 e 8 a mantêm no total de 75 e de 15, como *pattern* não certo.
Motivo: quem lê "1 execução" imagina um código com condicional. Com código, a omissão no N2 desse caso foi zero, o que não muda a direção do que o texto afirma.
Correção: diga, numa oração da Seção 6.3, como a execução sem código foi classificada.

### Critério 7. Apresentação, resultados e conclusão

Sem apontamentos. A solução está descrita com artefatos e métricas de dimensão, o processo de avaliação está descrito, as limitações são específicas, e os trabalhos futuros decorrem do trabalho.

### Critério 8. Figuras, tabelas e gráficos

Sem apontamentos. Inspecionei as dez figuras. Todas têm número, título e fonte, e todas são chamadas antes de aparecer, com descrição. Os gráficos têm eixo e legenda, os painéis da Figura 10 usam a mesma escala, e os valores das Figuras 9 e 10 batem com as Tabelas 7 e 8. As 16 tabelas têm número, título, fonte e chamada antes de aparecer.

### Critério 9. Consistência de conteúdo

**4. RECOMENDADA. Ainda há "efeito" da orientação na extensão.**
Onde: Seção 7, abertura, "suas comparações entre níveis descrevem o que foi observado e não demonstram efeito"; Seção 7, parágrafo de comparação, "nos agentes, o efeito das orientações também variou por *pattern* e tarefa (P1 e P5)"; Seção 7, P7, "o efeito da orientação sobre a estabilidade depende do modelo".
Problema: a rodada anterior trocou os verbos de causa em quatro trechos, e estes dois ficaram. O primeiro se apoia em P5, que é só da extensão, e na variação por *pattern*, que só a extensão mede. O segundo junta medidas que o próprio parágrafo chama de diferentes, e a P7 é descritiva nos dois estudos.
Motivo: o roteiro pede a mesma cautela em todo o texto, e aqui a palavra contradiz a abertura da mesma seção. Não é obrigatória porque os números estão certos e a ressalva vem antes.
Correção: antes, "o efeito das orientações também variou por *pattern* e tarefa"; depois, "o resultado observado com as orientações também variou por *pattern* e tarefa". Faça a mesma troca na P7.

**5. SUGESTÃO. As 119 execuções não cobrem as duas pastas das bancadas.**
Onde: Seção 5.2, "a bancada de testes acumulou 119 execuções"; Figura 3, fase 1, "Bancadas, construction-v2 e v3".
Problema: as 119 são os `meta.json` de `construction-v3/runs`. A pasta `construction-v2/runs` tem outras 49. Lido com a Figura 3, o número parece o total da fase.
Motivo: o número não entra em nenhum resultado, e a Seção 4 já avisa que as fases preparatórias não são somadas.
Correção: indique, na frase, que as 119 são da pasta `construction-v3`, ou informe o total das duas.

### Critério 10. Normas e referências

Sem apontamentos.

**Citações e lista.** As 43 referências da lista estão citadas, e todas as citações do texto estão na lista. Não há "[CITAR]", `utm_source=` nem citação direta de livro sem página. A numeração das seções é consistente.

**Conferidas nesta rodada pelo Crossref (26 de 26 com DOI).** Todos os DOIs levam ao trabalho citado, com autor, ano e veículo iguais: Ahmed et al. 2025; Ampatzoglou et al. 2026; Arabat e Sayagh 2026; Arcuri e Briand 2014 (publicação *online* em 2012, fascículo de 2014); Basili et al. 1996; Boehm 1976; Campbell 2018; Chatlatanagulchai et al. 2026; Chidamber e Kemerer 1994; Ernst e Baldassarre 2023; Galster et al. 2026; He et al. 2026; Hou et al. 2024; Jiang e Nam 2026; Kim 2025; Kim 2026; Kjellberg et al. 2026; Liu et al. 2026; Muñoz Barón et al. 2020; Prechelt et al. 2001; Sallou et al. 2024; Vokáč et al. 2004; Wang et al. 2025; Xi et al. 2025; Zhang e Budgen 2012; Zisa et al. 2026.

**Conteúdo atribuído a terceiros, conferido nesta rodada.** Nas versões abertas, usadas só para a conferência e apagadas depois: Ahmed et al. (substituir todos os humanos é arriscado), Wang et al. (na sumarização, correlação de 31,50 com humanos contra 79,43 entre os métodos com LLMs grandes; nas outras tarefas, 83,04 e 68,63 com humanos) e Ampatzoglou et al. (10 desenvolvedores juniores, experimento em cruzamento, resultados por etapa). No resumo publicado: Zisa et al. (dois modelos, GPT-4o e Gemini 2.5 Flash Preview, de empresas diferentes), Kjellberg et al. (13 modelos, Singleton) e Zhang e Budgen (literatura até 2009).

**Links.** Os 23 endereços do texto responderam com sucesso, inclusive o repositório público.

**Repositório.** Conferi na árvore do `origin/main` os caminhos dos arquivos de orientação, das *skills* do verificador, dos *scripts* e do pré-registro. Todos estão publicados.

**Não conferidas nesta rodada.** O conteúdo de Kim (2025) e Kim (2026) não pôde ser confirmado: as bases não trazem o resumo, e a busca não encontrou texto aberto. Abra esses dois antes da entrega. Brown et al. 2020, Vaswani et al. 2017, Fowler 2018, Gamma et al., Martin, Meyer e Yourdon e Constantine foram conferidos em rodadas anteriores. Yao et al. 2023 está em D19.

### Critério 11. Tecnologias e repositório

Sem apontamentos. A Tabela 3 traz tecnologias com versão e papel, e o link do repositório está no texto e abre para qualquer pessoa.

## Matriz de rastreabilidade

| objetivo ou pergunta | onde é desenvolvido | etapa do método que gera os dados | resultado | resposta explícita | situação | ação pendente |
|---|---|---|---|---|---|---|
| Objetivo geral: avaliar o efeito de orientações persistentes sobre a decisão de aplicar um *pattern*, na construção e na manutenção | Seções 5 e 6 | Seções 5.1 e 6.2 | Tabelas 5, 7 e 8 | Seção 9, primeiro e segundo parágrafos | atendido | nenhuma |
| P1. Uso do *pattern* esperado | Seções 5.3 e 6.3 | Semgrep (5.1); verificador (6.2) | Tabelas 4, 5, 7, 8 e 13; Figuras 9 e 10 | Seção 7, P1; Seção 9 | atendido | 3 |
| P2. Exagero, corretude e qualidade do código | Seções 5.3 e 6.3 | Semgrep, suíte, SonarQube (5.1); verificador e controles (6.2) | Tabelas 4, 5, 12 e 14 | Seção 7, P2; Seção 9 | atendido | nenhuma |
| P3. Variação entre modelos | Seções 5.3 e 6.3 | quartetos e regras (5.1); três modelos (6.2) | Tabelas 13 e 14 | Seção 7, P3; Seção 9 | atendido | nenhuma |
| P4. Componentes e custo | Seções 5.3 e 6.3 | níveis, custo, abertura da *skill* (5.1); custo (6.2) | Tabelas 4, 5, 11 e 15 | Seção 7, P4; Seção 9 | atendido | nenhuma |
| P5. Tipo de tarefa | Seção 6.3 | tarefas e categorias (6.2) | Tabelas 7, 8 e 10; Figuras 9 e 10 | Seção 7, P5; Seção 9 | atendido | 4 |
| P6. Demais dimensões | Seção 6.3 | dimensões e roteiros (6.2) | Tabelas 7 e 14 | Seção 7, P6; Seção 9 | atendido | 2 |
| P7. Estabilidade | Seções 5.3 e 6.3 | perfis (5.1); repetições (6.2) | Seção 5.3; Tabela 16 | Seção 7, P7; Seção 9 | atendido | 4 |
| P8. Estrutura orientada a objetos | Seções 5.3 e 6.3 | CK (2, 5.1 e 6.2) | Tabelas 6, 9 e 10 | Seção 7, P8; Seção 9 | atendido | nenhuma |
| Controle negativo na manutenção (tratado com a P2) | Seção 6.3 | Seção 6.2 | Tabela 12 | Seção 7, parágrafo após a P2; Seção 9 | atendido | 1 |

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
| Sem frases longas que encadeiam resultados | parcial: restam frases longas, já tratadas em parte nas rodadas anteriores |
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
| Cada seção prepara a seguinte | atendido |
| Todo termo técnico definido antes do uso | atendido |
| Cada pergunta retomada e chega à Conclusão com o mesmo nome | atendido |
| Tamanho de cada seção corresponde ao papel | parcial: método acima da faixa e Fundamentação abaixo, sem falta de conteúdo |

**Critério 3**

| item | situação |
|---|---|
| Cada conceito tem um nome só | parcial (D15) |
| Nenhum nome designa duas coisas | atendido |
| Siglas pouco conhecidas expandidas | atendido |
| Estrangeirismos em itálico | parcial (D16) |

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
| Sequência e dependências claras | atendido |
| Cada escolha tem motivo declarado | parcial (2, D17) |
| Todo resultado tem etapa de origem | atendido |
| Toda etapa aparece nos resultados | atendido; os dois itens sem cálculo estão declarados na Seção 8 |
| Versões informadas | atendido |

**Critério 7**

| item | situação |
|---|---|
| Solução com métricas e artefatos | atendido |
| Resultados concretos e organizados pelas perguntas | atendido |
| Cada pergunta com resposta explícita | atendido |
| Sem resultado desligado dos objetivos | atendido |
| Conclusão com resultados concretos e as mesmas ressalvas | atendido |
| Limitações e trabalhos futuros específicos | atendido |

**Critério 8**

| item | situação |
|---|---|
| Toda figura e tabela com número, título e fonte | atendido |
| Toda figura e tabela apresentada com descrição | atendido |
| Todo gráfico com eixos e legenda | atendido |
| Código em texto, nenhum em imagem | pendente pelo guia, mantido por decisão (D1) |
| Nenhuma imagem com fundo preto ou escuro com código | atendido |
| Mesma informação não repetida em tabela e figura | parcial, mantido por decisão (D3) |

**Critério 9**

| item | situação |
|---|---|
| Mesmo número com o mesmo valor | atendido |
| Resumo, Introdução e Conclusão com a mesma cautela | atendido |
| Sem contradição entre seções | parcial (4) |
| Sem trechos genéricos | atendido |
| Sem etapas citadas uma vez e nunca mais | atendido |
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
| Declaração de uso depois da Conclusão | pendente (D4) |
| IA usada como instrumento descrita na metodologia | atendido |
| Conferi todo conteúdo, código e referência sugeridos | pendente: só você pode confirmar, e falta abrir Yao et al. 2023 (D19), Kim 2025 e Kim 2026 |
