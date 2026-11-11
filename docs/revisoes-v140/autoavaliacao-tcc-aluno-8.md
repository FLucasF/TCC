Autoavaliação feita com ferramenta de IA. A avaliação oficial é do orientador.

# Autoavaliação antes da entrega: artigo, versão 140 (rodada 8)

Este relatório aplica os critérios de revisão do professor Rodrigo Rebouças de Almeida, que são orientações dele para os seus orientandos e não critérios oficiais da disciplina, do curso ou da UFPB. O uso desta ferramenta é uso de IA e entra na declaração de uso do TCC (seção 8 do guia). O relatório não diz se o trabalho está aprovado e não reescreve o seu texto. Quando um ponto aparece como OBRIGATÓRIA, isso significa que o roteiro de revisão o trata como obrigatório.

Os apontamentos têm número único, na ordem da seção "Apontamentos por critério". No total, são 2 obrigatórios, 5 recomendados e 2 sugestões.

## Pontos já decididos

Estes pontos foram decididos por você, já foram verificados ou dependem de você. Nenhum conta como pendência neste relatório, e nenhum apontamento abaixo pede lista no corpo do texto.

**Protegidos por decisão sua**

1. As figuras de código (Figuras 2, 5, 6, 7 e 8) ficam como imagem com fundo branco. Abri as cinco, e todas têm fundo branco.
2. A Seção 6.2 mantém uma figura por *pattern*.
3. As Figuras 9 e 10 ficam junto das Tabelas 7 e 8.
4. A declaração de uso de ferramentas de IA será escrita por último. O apoio à redação do artigo não entra na Tabela 1.
5. A coluna "como foi conferido" da Tabela 1 foi confirmada por você.
6. O título continua o mesmo, e o nome da H8 segue o pré-registro.
7. Os motivos que você deu estão mantidos: o Strategy por ser difundido e adequado ao domínio; os cinco *patterns* por cobrirem as três categorias do catálogo; o N3 fora da extensão; a segunda rodada do controle decidida antes do resultado da primeira; as 10 horas entre níveis por causa da janela de 5 horas da cota.
8. As referências ficam só com revistas e eventos respeitados, sem *preprints*, com correções só de textos publicados, e a definição de IA vem de livro.
9. O registro é prosa impessoal em parágrafos encadeados, sem listas no corpo e sem rótulos em negrito no começo do parágrafo. As perguntas P1 a P8 ficam em parágrafo.
10. O Resumo fica entre 150 e 250 palavras, e o Abstract é tradução fiel. Contei 248 e 249 palavras.
11. A Introdução define cada termo para o leitor.
12. *Harness* segue o sentido de Galster et al., com o aviso sobre o pré-registro.
13. O uso só de modelos da Anthropic está declarado como limitação.

**Já verificado: não procede**

14. Tabela 11, *tokens* de saída do N2 da construção (38,9 mil): todas as células usam a soma dos segmentos. Não reabri o ponto.

**Pendências que dependem de você**

15. Tabela comparativa dos trabalhos relacionados (Apêndice C do guia), um nome só para os níveis e "et al." nas referências.
16. Citar Page (1963) e Cohen (1960) depende de você ler ou aprovar. Itálico nas figuras.
17. O motivo de a hipótese "quem melhora não é quem exagera" e a proporção entre ler, escrever e executar não terem sido calculadas (OBJETIVO §4.5 e §4.6). O texto só registra o fato.
18. O aviso da API do OpenAlex pedindo o e-mail do usuário, de uma rodada anterior, não foi atendido. Nesta rodada não usei o OpenAlex.
19. Yao et al. 2023 continua sem conferência por automação e precisa ser aberta por você. Nesta rodada o DBLP devolveu uma página de verificação contra robôs, o OpenReview recusou o acesso (403) e o Semantic Scholar limitou os pedidos (429). Não tentei contornar essas barreiras. Nenhuma página ou API trouxe instrução dirigida a mim.

## Pontos a tratar

### Obrigatórios

O roteiro de revisão trata estes pontos como obrigatórios, porque mudam a leitura de um resultado.

| nº | ponto | onde |
|---|---|---|
| 3 | O `CLAUDE.md` da manutenção manda testar o que é novo, o que coincide com o critério da nota de testes e não chega ao N0. O método não diz isso, e a Discussão afirma que a configuração "versa sobre estrutura". | Seções 6.2, 7 (P6) e 9; Tabela 7 |
| 6 | A frase de fechamento da P6 diz que os movimentos "não se repetem nas duas tarefas", mas, do N1 para o N2, a arquitetura sobe e os testes caem nas duas. | Seção 7 (P6); Seção 9; Tabela 7 |

### Recomendados, por peso

| nº | ponto | onde |
|---|---|---|
| 9 | A Introdução chama de confirmatória a evidência da P4, que só tem hipóteses secundárias. | Seções 1, 5.1 e 7; Tabela 4 |
| 4 | H3 é descrita como "casos da suíte aprovados", mas os números vêm dos pontos da suíte, que dão meio ponto em recusas. | Seções 5.1 e 5.3; Tabelas 4 e 13 |
| 7 | A ressalva "não demonstram efeito da *skill*" vale só para arquitetura e testes, enquanto o *pattern* e a estrutura desnecessária, com o mesmo estatuto, são lidos como efeito ou custo. | Seção 7 (P2, P4 e P6); Seção 9 |
| 2 | No Resumo e no Abstract, os dois-pontos ligam os totais à exceção do Strategy. | Resumo e Abstract |
| 1 | "Esse roteiro" ficou depois de uma frase sobre a justificativa do verificador. | Seção 6.2 |

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

A sequência é coerente: o problema leva às perguntas, o estudo principal mede a decisão de usar um *pattern*, as suas lacunas motivam a extensão, e a Discussão responde na ordem das perguntas. As duas falhas que afetam o argumento estão na descrição do tratamento da manutenção (3) e no fechamento da P6 (6).

### Resumo, Introdução e Conclusão

Os três prometem o mesmo objeto, e os números que coincidem têm o mesmo valor. A ressalva do ponto de partida menor da manutenção agora aparece no Resumo. O defeito restante do Resumo é de encadeamento (2). A Introdução dá à P4 um estatuto que o método não dá (9).

### Números repetidos

Somei as tabelas e recalculei a partir dos dados o que indico abaixo. Todos os valores fecham. Nenhum apontamento desta rodada é de número errado; o apontamento 4 é de número certo com o nome errado da medida.

| conferência | fonte e regra | resultado |
|---|---|---|
| Tabelas 7, 8, 12, 14 e 15 | `review.json` das 540 revisões; *pattern* certo = acerto + alternativa equivalente; dimensão = nota bom | conferem em todas as células |
| Tabela 8 contra Tabelas 7 e 14 e Figuras 9 e 10 | soma das linhas e leitura das imagens | fecham |
| Tabela 16 e 294 de 360, 54 de 90 | `review.json`, repetição comparada com a rodada 1 | conferem |
| Categorias: 27 e 30; 7 e 26; 32 e 15; 55 e 56; 9 e 14; 45, 53, 72; 48, 66, 69; 12, 8, 4 e 2 | `design_class` | conferem |
| Strategy na construção: omissão 6, 1, 1; malfeito 0, 7, 5 | `design_class` | conferem |
| Observer da manutenção: malfeito é a categoria mais frequente | `design_class` (27 de 45) | confere |
| Decorator como o que mais errou e Factory Method como o que mais acertou na primeira rodada | `review.json`, rodada 1 (7 e 16 em 18) | confere |
| +18 e +8; 15 para 14 no Strategy; 14 e 12; 39 e 19; +25 e +14 | soma da Tabela 8 | conferem |
| Factory Method na manutenção, Sonnet 5.5: 0 de 5 no N0 e 10 de 10 com orientação | `review.json` | confere |
| Tabela 9 | `oo-metrics.csv` | confere em todas as células |
| Tabela 10 contra Tabela 9 | média ponderada das categorias | fecha em todas as colunas |
| Abstratos no N2: 4,87 e 3,60; 4,20 e 2,13; 0,33 e 0 | `oo-metrics.csv` por cenário | conferem |
| Tabela 6; seis projetos com LCOM de 37,2 ou mais, um no N2 | `metricas.csv` | conferem |
| "Constantes de *enum*" na leitura da Tabela 6 | `ck/class.csv`: tipos anônimos dentro de *enum* | confere: 685 das 693 |
| Conta do CBO pelos tipos (22% a 46%; 3% a 27%) | Tabelas 6 e 9, pela regra declarada | confere |
| Tabelas 4, 5 e 13; os 28 vereditos; 14 pares; 10 e 7; H23; H26; Page 0,007; perfis 7, 3 e 1; *tokens* 6 e 5 modelos | `resultados-V5-STRATEGY.md`, `hipoteses-V5-STRATEGY.md` | conferem |
| *Skill* não aberta em 110 execuções | `tool_calls_by_name` em `meta.json` do N2 e do N3 | confere |
| Concordância 174 de 176 | `conferencia-V5-STRATEGY.md` | confere |
| 36 de 55 (3,0%), 5 de 5, 8 de 9, 13%, 3 em 25 | §4.1 do `OBJETIVO.md` | conferem |
| Custo por nível da Tabela 11; US$ 380 e US$ 346 | `cost_usd` em `meta.json`; soma das médias | conferem |
| `mvn verify`: 20 falhas; 11 por Docker, 9 ruim e 2 com problemas | `build_exit_code`, `build.txt`, `review.json` | conferem |
| Versões do Semgrep e do SonarQube; *commit* 54e13fc9 de 10/10 | READMEs das ferramentas; `git log` | conferem |
| *Scripts* citados nas Seções 4 e 5 | arquivos no repositório | existem |
| Nome do *pattern* nos enunciados | `prompt.md` dos dez cenários com *pattern* | nenhum menciona |
| Regra de empate da H5, de 09/10 | `DECISOES.md` e `hipoteses.mjs` | confere |

Não recalculei nesta rodada os *tokens* e o tempo da Tabela 11, o segundo verificador, a calibração e as reservas simultâneas, que a rodada anterior conferiu e o texto não mudou.

### Termos centrais

| termo | situação |
|---|---|
| configuração, orientação, orientações persistentes, *harness* | nomes declarados na Introdução; ponto já decidido 15 |
| acerto, certo, *pattern* certo | sentidos diferentes nos dois estudos, com aviso nas Seções 6.2 e 7 |
| exagero, estrutura desnecessária, contenção | distinguidos na Seção 6.2 |
| corretude, comportamento | diferença declarada na P2 |
| casos aprovados, pontos da suíte | o texto usa o primeiro para o que é o segundo; ver 4 |

### Proporção

Contei as palavras do corpo, com as tabelas (18.761 palavras). Introdução: 8,5%. Fundamentação: 3,8%. Trabalhos relacionados: 3,0%. Método e processo (Seções 4, 5.1, 5.2, 6.1 e 6.2): 39,5%. Resultados (5.3 e 6.3): 19,1%. Discussão: 14,8%. Limitações: 7,7%. Conclusão: 3,4%. A Discussão fica acima da faixa do guia porque responde a oito perguntas. Fundamentação e Trabalhos relacionados ficam abaixo, e não vi falta de conceito usado depois. Sem apontamento.

### Percurso de cada objetivo

O percurso está na matriz de rastreabilidade, no fim deste relatório. As oito perguntas têm resposta explícita na Seção 7.

### Proveniência dos resultados

| resultado | etapa de origem | situação |
|---|---|---|
| acerto nos pontos e exagero no ponto do seguro | Seção 5.1 | descrito |
| suíte e *build* | Seção 5.1 | descrito; regra de pontuação ausente, ver 4 |
| complexidade, duplicação, linhas, *code smells* | Seção 5.1 | descrito |
| CK nos dois estudos | Seções 2, 5.1 e 6.2 | descrito |
| *tokens*, tempo, turnos, custo | Seções 4, 5.1 e 6.2 | descrito |
| perfis distintos, abertura da *skill*, revisor | Seção 5.1 | descrito |
| categoria do *pattern*, dimensões, estrutura desnecessária, justificativas | Seção 6.2 | descrito |
| reservas simultâneas, `mvn verify` | Seções 4 e 6.2 | descrito |
| estabilidade, segundo verificador, controles negativos | Seção 6.2 | descrito |

### Justificativa das escolhas

Têm motivo declarado: o Strategy, os cinco *patterns*, o Javalin, o banco, o esforço *medium*, o Fable fora, cinco rodadas, o quarteto, os limites das regras, o N1 contra o N0 nas principais, o N3 fora da extensão, a posição da instrução da *skill*, os três modelos, o Opus 5.5, as 30 revisões, as duas rodadas de controle, a exigência de migrações, o Semgrep, o SonarQube e o CK. Não encontrei escolha sem motivo que mude a leitura de um resultado. O problema do apontamento 3 é de escolha não descrita, e não de motivo faltante.

## Apontamentos por critério

### Critério 1. Língua portuguesa e redação

Sem apontamentos. Não encontrei erros de ortografia, acentuação ou concordância, nem marcadores pendentes, fórmulas de ênfase, gerúndio de comentário ou resíduos de conversa.

### Critério 2. Encadeamento e coerência

**1. RECOMENDADA. "Esse roteiro" perdeu o antecedente.**
Onde: Seção 6.2, parágrafo "Cada projeto foi revisado às cegas": "e o comportamento, por um roteiro de chamadas à API por cenário. O verificador gravou também uma justificativa escrita para cada dimensão. Esse roteiro inclui dez tentativas".
Problema: a frase sobre a justificativa, acrescentada na rodada anterior, entrou entre "roteiro" e "Esse roteiro".
Motivo: o leitor procura o roteiro na frase imediatamente anterior e encontra a justificativa. Esse roteiro é a origem do resultado das reservas simultâneas (Seção 6.3).
Correção: leve a frase da justificativa para depois de "Esse roteiro inclui dez tentativas [...] as mesmas noites".

### Critério 3. Terminologia e siglas

Sem apontamentos. As siglas pouco conhecidas estão expandidas, e os estrangeirismos estão em itálico no texto corrido.

### Critério 4. Resumo e Abstract

O Resumo traz problema, objetivo, método e resultado com números. O Abstract é tradução fiel, frase a frase.

**2. RECOMENDADA. Os dois-pontos ligam os totais à exceção.**
Onde: Resumo, "em nove de dez combinações de cinco *patterns* e duas tarefas, com exceção do Strategy na construção: de 19 para 58 execuções em 75 na manutenção, que partiu de um nível menor, e de 41 para 60 na construção". Abstract: "except for Strategy in construction: from 19 to 58 runs out of 75 in maintenance".
Problema: depois da edição da rodada anterior, os números vêm logo em seguida a "exceção do Strategy na construção". Eles são os totais dos cinco *patterns* (Tabela 7), e não dados da exceção. O Strategy na construção ficou em 9, 7 e 9 em 15.
Motivo: quem lê só o Resumo não tem a Tabela 7 para perceber o engano, e o número principal da extensão fica preso à única combinação em que ele não vale.
Correção: separe a exceção dos totais, com ponto ou com a exceção depois dos números. Antes: "com exceção do Strategy na construção: de 19 para 58". Depois: "No total, foi de 19 para 58". O Resumo está em 248 palavras, então a troca precisa ser neutra em tamanho.

### Critério 5. Introdução

Sem apontamentos. A Introdução apresenta contexto, problema, relevância com fontes, objetivo geral, perguntas numeradas, contexto institucional e organização do texto. O texto não menciona o AYTY, e o trabalho aparece como TCC da UFPB. O estatuto da P4 está no apontamento 9.

### Critério 6. Coerência metodológica

A metodologia está completa quanto a etapas, ferramentas, versões e envolvidos.

**3. OBRIGATÓRIA. O tratamento da manutenção inclui uma instrução sobre testes que o método não descreve.**
Onde: Seção 6.2, "A redação do `CLAUDE.md` difere entre as tarefas: na manutenção, ele orienta a reorganizar o código antes da mudança e afirma que a orientação vale também para os casos já existentes"; Tabela 2, "`CLAUDE.md` próprio da manutenção, que orienta a reorganizar antes da mudança"; Seção 7, P6, "A configuração versa sobre estrutura"; Seção 7, P6, e Seção 9, "os testes caíram de 37 para 25".
Problema: o arquivo `TCC_V1/maintenance/experiment/harnesses/N1/CLAUDE.md` (e o N2) também diz: "Ajuste os testes do que mudou de propósito, teste o que é novo e termine com o `mvn verify` passando". O enunciado que o N0 recebe pede que o `mvn verify` passe, mas não pede testes novos. A *skill* de testes do verificador dá a nota bom só quando os testes conferem "inclusive a parte nova do cenário" (a Seção 6.2 repete esse critério). Portanto, a nota de testes na manutenção mede algo que o N1 e o N2 receberam como instrução e o N0 não recebeu. O dado acompanha: testes 25, 37 e 25 (Tabela 7); do N0 para o N1, 21 pares melhores e 9 piores, com alta nos três modelos (5 para 6, 8 para 11 e 12 para 20, Tabela 14). A construção não tem essa linha.
Motivo: a configuração é a variável do estudo, e a frase "versa sobre estrutura" não vale para o arquivo da manutenção. Sem essa informação, o leitor não sabe que o 37 tem provável origem em uma instrução direta. A P6 e a Conclusão citam só a queda de 37 para 25, de modo que parece um custo da *skill* sobre um valor de partida, quando o N2 volta ao valor do N0 (25) mesmo mantendo a instrução. O texto já faz esse tipo de ressalva para o critério de acerto na P5 ("coincide com uma instrução do `CLAUDE.md` da manutenção que o N0 não recebe"), e o mesmo raciocínio cabe aqui.
Correção: descreva essa instrução na Seção 6.2, registre na P6 e na Seção 8 que ela coincide com o critério da nota de testes, restrinja "versa sobre estrutura" e informe o valor do N0 quando citar a queda de 37 para 25.

**4. RECOMENDADA. H3 não mede "casos aprovados".**
Onde: Seção 5.3, "nos casos da suíte aprovados em cada par, 10 pares melhoraram e 7 pioraram, saldo de +3 em 55"; Tabela 4, H3 e H20, "casos da suíte aprovados"; Seção 5.1, que não descreve a pontuação; Tabela 13, coluna "saldo H3".
Problema: a medida pré-registrada são os pontos da suíte (`OBJETIVO.md`, §4.4): um caso de recusa vale 1, ou 0,5 com o código certo e *status* de sucesso, e quem não compila ou não sobe tem 0. Testei as duas regras em `acceptance-V5-STRATEGY.csv`. Com a coluna `pontos` saem 10, 7 e +3, e toda a coluna da Tabela 13. Com a coluna `passed`, isto é, casos aprovados, saem 9, 8 e +1, e o Sonnet 4.5 cai de +2 para 0. Os pontos diferem dos casos aprovados em 18 das 220 execuções (por exemplo, 12 casos e 16 pontos).
Motivo: H3 é uma das cinco hipóteses principais, e o leitor que refizer a conta pelo que o texto diz obtém outro saldo. O veredito não muda (+1 e +3 ficam acima de −7), por isso não é obrigatória.
Correção: diga na Seção 5.1 como os pontos são dados e use "pontos da suíte" na Seção 5.3 e nas Tabelas 4 e 13. O mesmo vale para H11, H12 e H14, que usam a mesma pontuação.

**5. SUGESTÃO. Falta a condição de rejeição da regra de comparação entre modelos.**
Onde: Seção 5.1, "As condições de rejeição são as seguintes", que trata de três das quatro regras.
Problema: o pré-registro (§4.1) diz que H5 é contrariada quando outro modelo tem saldo maior e inconclusiva com empate no maior saldo.
Correção: acrescente uma oração. Motivo: é a regra de uma hipótese principal, e o apoio a ela já depende de uma regra de empate.

### Critério 7. Apresentação, resultados e conclusão

A solução está descrita com artefatos e métricas de dimensão, e o processo de avaliação está descrito. As limitações são específicas, e os trabalhos futuros decorrem do trabalho.

**6. OBRIGATÓRIA. "Não se repetem nas duas tarefas" contradiz a Tabela 7 na comparação da *skill*.**
Onde: Seção 7, P6, "nas demais dimensões, os movimentos têm sinais diferentes e não se repetem nas duas tarefas"; Seção 7, P6, "Na manutenção, do N1 para o N2, isto é, com a *skill*, a arquitetura subiu de 57 para 68 [...] e os testes caíram de 37 para 25"; Seção 9, que cita essas duas dimensões só na manutenção; Tabela 7.
Problema: a frase, reescrita na rodada anterior, vem logo depois da leitura do N1 para o N2, e nessa comparação os movimentos se repetem. Fonte: `review.json`, contando nota bom, regra que reproduz todas as células das Tabelas 7 e 14; pares de mesmo cenário, modelo e rodada.

| N1 para N2 | construção | pares melhores e piores | manutenção | pares melhores e piores |
|---|---|---|---|---|
| arquitetura | 9 para 16 | 12 e 5 | 57 para 68 | 13 e 2 |
| testes | 9 para 6 | 3 e 6 | 37 para 25 | 7 e 19 |
| *pattern* certo, para comparação | 53 para 60 | 12 e 5 | 33 para 58 | 26 e 1 |

Na construção, a arquitetura tem o mesmo saldo de pares do *pattern*. A frase só é correta para outras comparações: do N0 para o N2, a arquitetura fica em 16 e 16 na construção e sobe na manutenção, e os testes caem na construção e empatam na manutenção. Na construção, a queda dos testes vem só do Sonnet 5.5 (9 para 6), e a alta da arquitetura vem do Sonnet 5.5 (6 para 12) e, pouco, do Sonnet 4.5 (3 para 4).
Motivo: é a afirmação que resume a P6, e o leitor sai dela entendendo que o que acompanha a *skill* nas demais dimensões não se repete, quando a arquitetura sobe e os testes caem nas duas tarefas. O guia pede que a recomendação reflita tudo o que os dados mostraram. Não há teste de significância, e o texto pode manter essa cautela.
Correção: diga a qual comparação a frase se aplica, ou registre que, do N1 para o N2, arquitetura e testes têm o mesmo sinal nas duas tarefas. Ajuste a recomendação da Seção 9, que hoje menciona na construção só a estrutura desnecessária.

**7. RECOMENDADA. A ressalva da P6 vale para leituras que o texto faz sem ela.**
Onde: Seção 7, P6, "Como a extensão é descritiva, sem teste de significância, esses movimentos não demonstram efeito da *skill*"; Seção 7, P4, "a *skill* acrescenta mais (+25 execuções com o *pattern* certo)"; Seção 7, P2, "mas apontam um custo: a estrutura desnecessária aumentou com a *skill*".
Problema: as três leituras têm o mesmo estatuto: descritivas, na mesma extensão, do N1 para o N2 ou do N0 para o N2. A estrutura desnecessária (4, 5 e 9 em 75) é o movimento menor e vem de uma nota fora da calibração, mas é lida como custo. A arquitetura (13 pares melhores e 2 piores) não pode ser lida como efeito.
Motivo: quando a mesma evidência recebe duas leituras, o leitor conclui que a cautela depende do resultado. A Seção 9 já usa uma fórmula uniforme ("são descrições exploratórias, sem teste de significância").
Correção: use a mesma ressalva nas três leituras, por exemplo na abertura da resposta sobre a extensão, e troque "apontam um custo" por uma descrição do que foi observado.

**8. SUGESTÃO. "O tratamento não chegou a ser aplicado".**
Onde: Seção 7, P4, "a *skill* não foi aberta em nenhuma das 110 execuções, de modo que o tratamento não chegou a ser aplicado"; Introdução, "sua descrição fica sempre disponível ao agente".
Problema: o conteúdo não foi carregado, mas a descrição esteve na entrada de todas as execuções do N2 e do N3, e a própria Introdução diz isso.
Correção: diga que o conteúdo da *skill* não chegou ao agente. Motivo: o N2 difere do N1 por essa descrição, e o texto usa a diferença como ausência total de tratamento.

### Critério 8. Figuras, tabelas e gráficos

Sem apontamentos. Inspecionei as dez figuras. Todas têm número, título e fonte, e todas são apresentadas antes de aparecer, com descrição. Os gráficos têm eixos e legenda, os painéis da Figura 10 usam a mesma escala, e os valores das Figuras 9 e 10 batem com as Tabelas 7 e 8. As 16 tabelas têm número, título, fonte e chamada no texto.

### Critério 9. Consistência de conteúdo

Os números são consistentes entre as seções, e o estilo é uniforme. Os apontamentos 3, 6 e 7 também são de consistência, mas estão nos critérios 6 e 7.

**9. RECOMENDADA. A P4 é anunciada como confirmatória e só tem hipóteses secundárias.**
Onde: Introdução, "As quatro primeiras têm evidência confirmatória no estudo principal"; Seção 5.1, "as demais são secundárias e não sustentam conclusão sozinhas"; Tabela 4, em que as hipóteses da P4 são H16 a H18 e H21 a H28; Seção 7, P4, "com pré-registro".
Problema: as cinco principais servem à P1, à P2 e à P3 (H1 a H5). Nenhuma serve à P4. Pelo próprio texto e pelo `OBJETIVO.md` (§4), o que responde à P4 não sustenta conclusão sozinho. Na Seção 7, só as hipóteses de custo aparecem como secundárias, e as da *skill* e do processo (H21, H25 e H26) aparecem como resultado apoiado, sem esse aviso.
Motivo: o leitor da Introdução espera, para a P4, a mesma força da P1, e é nela que está a afirmação sobre o processo.
Correção: diga na Introdução que a P4 tem só hipóteses secundárias no estudo principal e repita o aviso na abertura da resposta da P4.

### Critério 10. Normas e referências

Sem apontamentos.

**Citações e lista.** As 43 referências da lista estão citadas, e todas as citações do texto estão na lista. Não há "[CITAR]", `utm_source=` nem citação direta de livro sem página. A numeração das seções é consistente.

**Conferidas nesta rodada pelo Crossref (26 de 26 com DOI).** Todos os DOIs levam ao trabalho citado, com autor e veículo iguais: Ahmed et al. 2025; Ampatzoglou et al. 2026; Arabat e Sayagh 2026; Arcuri e Briand 2014 (publicação *online* em 2012, fascículo de 2014); Basili et al. 1996; Boehm 1976; Campbell 2018; Chatlatanagulchai et al. 2026; Chidamber e Kemerer 1994; Ernst e Baldassarre 2023; Galster et al. 2026; He et al. 2026; Hou et al. 2024; Jiang e Nam 2026; Kim 2025; Kim 2026; Kjellberg et al. 2026; Liu et al. 2026; Muñoz Barón et al. 2020; Prechelt et al. 2001; Sallou et al. 2024; Vokáč et al. 2004; Wang et al. 2025; Xi et al. 2025; Zhang e Budgen 2012; Zisa et al. 2026.

**Números atribuídos a terceiros.** Conferi no resumo publicado: Chatlatanagulchai et al. (2.303 arquivos, 1.925 repositórios), Arabat e Sayagh (148 projetos) e Kjellberg et al. (13 modelos). Não consegui conferir nesta rodada os números de Galster et al., He et al. e Hou et al.

**Links.** Os 23 endereços do texto responderam com sucesso, inclusive o repositório público.

**Não reconferidas nesta rodada.** Brown et al. 2020, Vaswani et al. 2017, Fowler 2018, Gamma et al., Martin, Meyer e Yourdon e Constantine foram conferidos em rodadas anteriores. Yao et al. 2023 está no ponto já decidido 19.

**Limite desta conferência.** Conferi a existência e os metadados. Só nos três casos acima conferi o que o artigo afirma.

### Critério 11. Tecnologias e repositório

Sem apontamentos. A Tabela 3 traz tecnologias com versão e papel. O link do repositório está no texto e abre para qualquer pessoa.

## Matriz de rastreabilidade

| objetivo ou pergunta | onde é desenvolvido | etapa do método que gera os dados | resultado | resposta explícita | situação | ação pendente |
|---|---|---|---|---|---|---|
| Objetivo geral: avaliar o efeito de orientações persistentes sobre a decisão de aplicar um *pattern*, na construção e na manutenção | Seções 5 e 6 | Seções 5.1 e 6.2 | Tabelas 5, 7 e 8 | Seção 9, primeiro e segundo parágrafos | atendido | nenhuma |
| P1. Uso do *pattern* esperado | Seções 5.3 e 6.3 | Semgrep (5.1); verificador (6.2) | Tabelas 4, 5, 7, 8 e 13; Figuras 9 e 10 | Seção 7, P1; Seção 9 | atendido | 2 |
| P2. Exagero, corretude e qualidade do código | Seções 5.3 e 6.3 | Semgrep, suíte, SonarQube (5.1); verificador e controles (6.2) | Tabelas 4, 5, 12 e 14 | Seção 7, P2; Seção 9 | atendido | 4, 7 |
| P3. Variação entre modelos | Seções 5.3 e 6.3 | quartetos e regras (5.1); três modelos (6.2) | Tabelas 13 e 14 | Seção 7, P3; Seção 9 | atendido | 5 |
| P4. Componentes e custo | Seções 5.3 e 6.3 | níveis, custo, abertura da *skill* (5.1); custo (6.2) | Tabelas 4, 5, 11 e 15 | Seção 7, P4; Seção 9 | atendido, com estatuto a ajustar | 7, 8, 9 |
| P5. Tipo de tarefa | Seção 6.3 | tarefas e categorias (6.2) | Tabelas 7, 8 e 10; Figuras 9 e 10 | Seção 7, P5; Seção 9 | atendido | nenhuma |
| P6. Demais dimensões | Seção 6.3 | dimensões e roteiros (6.2); instrução sobre testes não descrita | Tabelas 7 e 14 | Seção 7, P6; Seção 9 | atendido, com leitura a ajustar | 1, 3, 6, 7 |
| P7. Estabilidade | Seções 5.3 e 6.3 | perfis (5.1); repetições (6.2) | Seção 5.3; Tabela 16 | Seção 7, P7; Seção 9 | atendido | nenhuma |
| P8. Estrutura orientada a objetos | Seções 5.3 e 6.3 | CK (2, 5.1 e 6.2) | Tabelas 6, 9 e 10 | Seção 7, P8; Seção 9 | atendido | nenhuma |
| Controle negativo na manutenção (tratado com a P2) | Seção 6.3 | Seção 6.2 | Tabela 12 | Seção 7, parágrafo após a P2; Seção 9 | atendido | nenhuma |

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
| Cada seção prepara a seguinte | parcial (1) |
| Todo termo técnico definido antes do uso | atendido |
| Cada pergunta retomada e chega à Conclusão com o mesmo nome | atendido |
| Tamanho de cada seção corresponde ao papel | parcial (ponto já decidido 15) |

**Critério 3**

| item | situação |
|---|---|
| Cada conceito tem um nome só | parcial (4; ponto já decidido 15) |
| Nenhum nome designa duas coisas | atendido |
| Siglas pouco conhecidas expandidas | atendido |
| Estrangeirismos em itálico | parcial (ponto já decidido 16) |

**Critério 4**

| item | situação |
|---|---|
| Resumo com problema, objetivo, o que foi feito e resultado com número | atendido |
| Resumo se entende sozinho | parcial (2) |
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
| Cada etapa diz o que, como, com quais ferramentas e quem | parcial (3, 4) |
| Sequência e dependências claras | atendido |
| Cada escolha tem motivo declarado | parcial (ponto já decidido 17) |
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
| Conclusão com resultados concretos e as mesmas ressalvas | parcial (3, 6) |
| Limitações e trabalhos futuros específicos | atendido |

**Critério 8**

| item | situação |
|---|---|
| Toda figura e tabela com número, título e fonte | atendido |
| Toda figura e tabela apresentada com descrição | atendido |
| Todo gráfico com eixos e legenda | atendido |
| Código em texto, nenhum em imagem | pendente pelo guia, mantido por decisão (ponto já decidido 1) |
| Nenhuma imagem com fundo preto ou escuro com código | atendido |
| Mesma informação não repetida em tabela e figura | parcial, mantido por decisão (ponto já decidido 3) |

**Critério 9**

| item | situação |
|---|---|
| Mesmo número com o mesmo valor | atendido |
| Resumo, Introdução e Conclusão com a mesma cautela | parcial (9) |
| Sem contradição entre seções | parcial (3, 6, 7, 9) |
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
| Declaração de uso depois da Conclusão | pendente (ponto já decidido 4) |
| IA usada como instrumento descrita na metodologia | atendido |
| Conferi todo conteúdo, código e referência sugeridos | pendente: só você pode confirmar, e falta abrir Yao et al. 2023 (ponto já decidido 19) |
