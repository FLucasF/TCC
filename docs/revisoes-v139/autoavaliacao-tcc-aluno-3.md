Autoavaliação feita com ferramenta de IA. A avaliação oficial é do orientador.

# Autoavaliação antes da entrega: artigo, versão 139 (r3)

Os critérios usados aqui são as orientações do professor Rodrigo Rebouças de Almeida para os seus orientandos, e não critérios oficiais da disciplina de TCC, do curso ou da UFPB. Quando houver diferença, valem a resolução de TCC e as normas oficiais. O uso desta revisão é uso de ferramenta de IA e entra na declaração de uso do TCC (seção 8 do guia).

Este relatório segue o modo 3 da skill (conferir antes de entregar). Li o texto inteiro, as 16 tabelas e as 10 figuras (abri as imagens), fiz a leitura macro e apliquei os 11 critérios. Como o texto em Markdown não tem páginas, cada apontamento indica a seção, o parágrafo e um trecho curto.

## Pontos já decididos (registrados, não contados como pendência)

1. **Figuras de código (Figuras 2, 5, 6, 7 e 8) como imagem com fundo branco.** O roteiro diz que trechos de código não devem aparecer como imagem e que imagens de código com fundo preto não são aceitas (critério 8). As cinco figuras têm fundo branco, e a manutenção como imagem foi combinada com o orientador. Registrado.
2. **Declaração de uso de ferramentas de IA, a ser escrita por último.** O roteiro pede um parágrafo próprio depois da Conclusão e antes das Referências, dizendo quais ferramentas, em quais partes, para quê e como o resultado foi conferido (seção 8 do guia). A versão 139 ainda não tem esse parágrafo; ele fica pendente por decisão sua, e o checklist o marca assim.
3. **Figuras 9 e 10 ao lado das Tabelas 7 e 8.** O roteiro recomenda não repetir a mesma informação em tabela e figura (seção 5 do guia). A Figura 9 repete a primeira linha da Tabela 7, e a Figura 10 repete a Tabela 8 (conferi os 36 valores das barras contra as tabelas: todos iguais). Mantidas por decisão sua.
4. **Nome da hipótese H8 ("gera mais arquivos", medida pelo número de classes).** O roteiro pede o mesmo nome para o mesmo conceito (critério 3), e o nome difere da medida. O nome segue o pré-registro congelado e não muda. Registrado.

## Pontos a tratar

O roteiro de revisão trata como obrigatório o ponto a seguir:

1. **OBRIGATÓRIA.** Os critérios que levam uma execução a receber a nota bom, com problemas ou ruim em cada dimensão do verificador não estão no texto, e dois resultados dependem deles para serem lidos (banco 0 de 225 na construção; testes com falhas de ambiente). Seção 6.2, parágrafo "Cada projeto foi revisado por um agente"; Tabelas 7 e 14. Ver critério 6, apontamento 6.1.

Recomendados, em ordem de peso:

2. Ligar cada hipótese (H1 a H28) a uma pergunta (P1 a P8) e trazer para a resposta da P4 os vereditos de custo pré-registrados (critério 7, apontamento 7.1).
3. Ajustar a cautela das afirmações apoiadas em notas que a calibração não cobriu (design do restante do código e estrutura desnecessária) no Resumo, na Introdução e na Conclusão (critério 9, apontamento 9.1).
4. Corrigir a frase da P5 que diz "somente quando a skill é acrescentada", contrariada pelos números 14 e 12 (critério 9, apontamento 9.2).
5. Harmonizar a descrição da calibração entre a Seção 6.2 e a Seção 8 (critério 6, apontamento 6.2).
6. Descrever como o verificador tratou a falha dos testes de integração sem Docker (critério 6, apontamento 6.3).
7. Ligar a semelhança entre rodadas do estudo principal a uma pergunta (critério 7, apontamento 7.2).
8. Levar para os resultados os dados que aparecem pela primeira vez na Discussão (critério 7, apontamento 7.3).
9. Declarar o motivo de quatro escolhas do método (critério 6, apontamento 6.4).
10. Completar a tabela de IA com a conferência de cada etapa e com quem rodou a análise final (critério 6, apontamento 6.5).
11. Completar a Tabela 3 com as tecnologias dos scripts de análise (critério 11, apontamento 11.1).
12. Dividir as frases mais longas da metodologia e dos resultados (critério 1, apontamento 1.1).

As sugestões estão nos apontamentos por critério.

## Leitura macro

### Mapa das seções

| seção | o que entrega | o que deixa para a seguinte |
|---|---|---|
| Resumo / Abstract | problema, objetivo, os dois estudos e os resultados principais com números (200 e 203 palavras) | |
| 1. Introdução | agente, harness, CLAUDE.md, skill, subagente, pattern, exagero, construção e manutenção; problema, objetivo geral, P1 a P8, contribuições e organização | os conceitos de OO usados nas medidas |
| 2. Fundamentação | encapsulamento, polimorfismo, aberto/fechado, acoplamento e coesão; CK, complexidade cognitiva e duplicação | as medidas usadas nos dois estudos |
| 3. Trabalhos relacionados | Kim (2025), Kjellberg et al., Zisa et al., Ampatzoglou et al., arquivos de contexto, Kim (2026); lacuna no último parágrafo | a lacuna que o desenho ocupa |
| 4. Visão geral do processo | fases (Figura 3), desenho (Figura 4), repositório, envolvidos, IA (Tabela 1), níveis (Tabela 2), tecnologias (Tabela 3), isolamento das execuções | o protocolo de cada estudo |
| 5. Estudo principal | método, regras de decisão, 28 hipóteses (Tabela 4), proteção contra decisões posteriores, resultados (Tabelas 5 e 6) | os três pontos em aberto da extensão |
| 6. Extensão | motivação, mudanças da fase intermediária, método, verificador, calibração, controles, resultados (Tabelas 7 a 12) | |
| 7. Discussão | resposta a P1 a P8, uma por parágrafo, e a trajetória do estudo | |
| 8. Limitações | ameaças gerais, por estudo, do verificador, da execução e das métricas | |
| 9. Conclusão | retomada do objetivo, resultados dos dois estudos, custo, recomendação prática, trabalhos futuros | |
| Apêndice A | Tabelas 13 a 16, por modelo | |

O parágrafo de organização (último da Introdução) anuncia as Seções 2 a 9 e o Apêndice A, na ordem e com o conteúdo que existem. Está correto. A declaração de uso de IA, quando for escrita, entra nesse parágrafo também.

### Linha do argumento

Problema (efeito da configuração sobre a decisão de aplicar um pattern não pedido), objetivo geral, P1 a P8, método, resultados e discussão formam uma sequência em que cada parte usa a anterior. A Discussão responde às oito perguntas na ordem em que foram feitas, e a Conclusão retoma o objetivo geral. Os conceitos centrais são definidos antes do uso, com duas exceções pequenas (apontamento 2.1). Não encontrei conclusão que não decorra dos resultados; os desvios de cautela estão no apontamento 9.1.

### Resumo, Introdução e Conclusão

Os três prometem e afirmam a mesma coisa: 67% para 93% no ponto do clube, sem alteração detectada no exagero, sem piora da corretude no conjunto com piora no Haiku 4.5, nove de dez combinações na extensão, exceção do Strategy na construção, 88 de 90 controles contidos, estrutura desnecessária de 4 para 9 em 75 e ganho nos modelos fora do teto. A cautela é a mesma nos três, salvo nas medidas não calibradas (apontamento 9.1). A Conclusão acrescenta a queda do comportamento na manutenção e o custo, que não estão no Resumo; isso não é contradição, mas o Resumo não menciona custo, que faz parte da P4.

### Números repetidos

Conferi cada número que aparece em mais de um lugar e somei todas as tabelas que se desdobram em outras. **Não encontrei nenhuma divergência.** As conferências feitas:

- Tabela 13 contra Tabela 5: acertos no ponto do clube somam 37, 51, 51 e 54 em 55 (67%, 93%, 93%, 98%); os saldos de H1 somam +14 ("14 pares melhores e nenhum pior"); os saldos de H3 somam +3 ("saldo de +3 em 55"); Haiku 4.5 soma 6 em 20; Opus 4.7 +5 e Haiku 4.5 +1 (H5); Sonnet 4.5 2 de 5 e Haiku 5.5 3 de 5 (Seções 6.2 e 7).
- Tabela 5: exagero 0%, 2%, 5%, 0% corresponde a 0, 1, 3 e 0 execuções em 55, o que bate com "apenas 1 dos 55 pares" (H2) e "de 3 execuções em 55 para nenhuma" (H26); custo médio vezes 55 dá US$ 345,95, o "US$ 346" da Seção 5.3.
- Tabela 4: 13 apoiadas, 6 não apoiadas, 2 contrariadas e 7 inconclusivas, total 28; "não apoiada" aparece só nas hipóteses de alteração e "contrariada" só nas direcionais, como a Seção 5.1 define; quatro das cinco principais apoiadas.
- Tabela 8 contra Tabela 7 e Figura 9: 41, 53, 60 e 19, 33, 58.
- Tabela 14 contra Tabela 7: todas as 36 células de dimensão fecham (pattern, design do restante, arquitetura, testes, banco, comportamento), e a estrutura desnecessária soma 4, 5, 9 e 0, 1, 2; "6 delas no Sonnet 4.5" confere.
- Tabela 10 contra Tabela 7: acerto mais alternativa dá 154 na construção e 110 na manutenção, iguais às somas da Tabela 7; categorias somam 224 e 225, total 449. As médias ponderadas da Tabela 10 reproduzem as médias da Tabela 9 em tipos, interfaces, métodos abstratos, WMC e CBO.
- Tabela 15 contra Tabela 8 e texto: Decorator 4, 10, 10 e 3, 5, 8; categoria acerto 11 (dez do Sonnet 5.5, uma do Sonnet 4.5); 0 para 5 na construção.
- Tabela 16 contra texto: 89, 96, 109 por modelo; 96, 103, 95 por nível; total 294 de 360 (82%); 54 de 90 combinações estáveis; Sonnet 5.5 com 19 de 20 com orientação.
- Tabela 11: custo médio vezes 75 dá US$ 380,25 ("US$ 380"); +0,01 e +0,06 do N1 para o N2.
- Tabela 12: 30, 30, 28; "20 das 30 execuções, 18 delas contidas".
- Distribuição das 450: 90 + 144 + 216; 540 revisões; 58 de calibração (26 + 26 + 6); 210 notas (30 vezes 7); 195 coincidências somando 30, 30, 29, 29, 27, 26 e 24; 174 de 176 respostas (44 vezes 4).
- Discussão: +18 contra +8 (N0 para N1), 4, 2 e 4 em 10, 15 para 14 em 30, +14 e +25, 14 e 12, 39 e 19; a faixa de 22% a 46% e de 3% a 27% do produto CBO vezes tipos (recalculei: 21,8% a 46,3% e 3,4% a 27,3%).
- Regras de decisão: 36 de 55 tem chance bilateral de 3,0% e 35 de 55, 5,8%, como o texto diz; 5 de 5 dá 6,25% e 8 de 9, 3,9%; −7 em 55 e 3 em 25 correspondem a 13%.

### Termos centrais

| termo | situação |
|---|---|
| configuração / orientações persistentes / orientação / configuração do harness | sinônimos declarados na Introdução, mas o título usa um quarto nome, "orientações de projeto" (apontamento 3.1) |
| harness | sentido técnico na Introdução; o texto avisa que o pré-registro usa o termo para a configuração. Correto |
| exagero, estrutura desnecessária, contenção | separados de forma explícita na Seção 6.2. Correto |
| pattern certo / categoria acerto / uso correto | definidos na Seção 6.2; no estudo principal "acerto" é a classificação do Semgrep. A Discussão avisa que as avaliações diferem. Correto |
| teto, saldo, quarteto, ponto do clube, ponto do seguro | definidos antes do uso e usados com o mesmo sentido |
| N1 e N2 | o texto avisa, na Tabela 2, que não são o mesmo tratamento nos dois estudos. Correto |
| construção / manutenção | nas tabelas aparecem como "Construir" e "Manter" (Tabelas 7 a 11) e como "construção" e "manutenção" (Tabelas 14 e 15) (apontamento 3.1) |

### Proporção

Contei as palavras do texto principal (15.567): Introdução 10,0%; Fundamentação 4,4%; Trabalhos relacionados 3,7%; Visão geral 8,3%; Estudo principal 22,3%; Extensão 25,3%; Discussão 13,9%; Limitações 8,6% (cerca de 1.350 palavras); Conclusão 3,5%. Método e resultados ocupam o espaço que o trabalho pede. A Fundamentação e os Trabalhos relacionados estão abaixo da faixa do guia, o que se explica em parte porque a Introdução define os conceitos de agente, harness e skill; as Limitações estão bem acima da "meia página" (apontamento 2.2).

### Percurso de cada objetivo

Cada pergunta é desenvolvida na Seção 5.3 ou 6.3 e respondida em um parágrafo próprio da Seção 7, que começa com o código da pergunta. O detalhamento está na matriz. As lacunas são de ligação, não de ausência: as hipóteses não dizem a qual pergunta servem (apontamento 7.1) e um resultado descritivo do estudo principal não tem pergunta (apontamento 7.2).

### Proveniência dos resultados

Todos os números das Tabelas 4 a 16 têm etapa de origem: suíte de aceitação, Semgrep, SonarQube, CK e transcrição do Claude Code (Seção 5.1); verificador, roteiro de API, CK, transcrição e segundo verificador (Seção 6.2); controles (Seção 6.2, últimos parágrafos). A conferência humana (Seção 4 e 5.1) e o teste de Page (Seção 5.1) também. As lacunas: o critério de cada nota não está descrito (apontamento 6.1), o tratamento das falhas sem Docker não está descrito (6.3), e três resultados aparecem pela primeira vez na Discussão (7.3).

### Justificativa das escolhas

Têm motivo declarado: o Strategy no estudo principal, os 11 modelos e a exclusão do Fable, os quartetos simultâneos, as cinco rodadas, o esforço medium, o limite de −7, o limite de 36 e o de 6,3%, a regra de saída de 90%, os 44 casos da conferência humana, os cinco patterns, o Javalin, o PostgreSQL, a exclusão do N3, os três modelos da extensão, a skill no primeiro item da manutenção, o Opus 5.5 como verificador, o Fable 5.1 e o número de 30, a segunda rodada dos controles e as cinco rodadas da extensão (pelo custo). Sem motivo: apontamento 6.4.

## Apontamentos por critério

### Critério 1. Língua portuguesa e gramática

**1.1 Frases longas que encadeiam vários resultados ou condições. RECOMENDADA.**
- Onde: Seção 6.2, parágrafo "Cada projeto foi revisado por um agente (Claude Opus 5.5), escolhido por ser o modelo mais capaz disponível com custo que cabia na cota de uso, às cegas quanto ao modelo e ao nível"; Seção 5.1, parágrafo das condições de rejeição ("há no máximo 2 pares não empatados, ou eles se dividem, com o lado menor tendo ao menos um terço deles, como em H2, com um só par não empatado"); Seção 5.3, frase "O processo (N3 contra N2) teve efeito apoiado no *design* (H25), decidido por [...] de 3 execuções em 55 para nenhuma"; Introdução, parágrafo das perguntas P1 a P8.
- Motivo: na primeira frase, "às cegas" fica longe do verbo e parece qualificar a escolha do modelo; na segunda, "como em H2" vem depois da condição de divisão, mas H2 é exemplo da primeira condição (no máximo 2 pares). O leitor precisa reler para saber a que cada trecho se refere. O guia pede uma informação principal por frase.
- Correção: separar cada informação em uma frase. Exemplo de forma, com a sua frase:
  - Antes: "Cada projeto foi revisado por um agente (Claude Opus 5.5), escolhido por ser o modelo mais capaz disponível com custo que cabia na cota de uso, às cegas quanto ao modelo e ao nível: [...]"
  - Depois: "Cada projeto foi revisado às cegas quanto ao modelo e ao nível por um agente, o Claude Opus 5.5. Esse modelo foi escolhido por ser o mais capaz disponível com custo que cabia na cota de uso."
  Faça o mesmo nas outras três passagens; na das condições de rejeição, ponha "como em H2" logo depois de "no máximo 2 pares não empatados".

**1.2 Concordância. SUGESTÃO.**
- Onde: Seção 4, quinto parágrafo: "em 44 execuções sorteadas com semente registrada, um por modelo e nível".
- Motivo: o referente é "execuções", feminino.
- Correção: "uma por modelo e nível".

**1.3 Tempo verbal do método. SUGESTÃO.**
- Onde: Seção 5.1 usa o passado ("o agente recebeu uma pasta vazia"); a Seção 6.2 usa o presente para o mesmo tipo de afirmação ("Na tarefa de construção, o agente recebe uma pasta vazia", "O verificador registra ainda", "Cada dimensão recebe"); a Seção 4 também mistura ("O enunciado é entregue", "disparam").
- Motivo: o guia pede passado para o que foi feito e presente para o que se conclui. A mistura entre as duas seções de método do mesmo artigo chama atenção.
- Correção: passar para o passado as frases que descrevem o que foi feito nas execuções da extensão, mantendo no presente o que descreve o instrumento que continua no repositório, se preferir, desde que de forma uniforme.

Sem outros problemas: não encontrei fórmulas de ênfase, gerúndio de comentário, verbos rebuscados, atribuição vaga, vírgula antes do "e" final de enumeração nem títulos com maiúscula em todas as palavras.

### Critério 2. Encadeamento lógico e coerência textual

**2.1 Termos usados antes de definidos. SUGESTÃO.**
- Onde: Tabela 1 (Seção 4) usa a coluna "esforço" com *medium*, *high* e *xhigh*, definidos só na Seção 5.1 ("O esforço foi *medium*, parâmetro do Claude Code que regula o grau de raciocínio"); "Fable" aparece na Tabela 1 e na Seção 5.1 ("Ficou de fora apenas o Fable") e só é dito modelo Claude na Seção 6.2 ("o Claude Fable 5.1").
- Motivo: o leitor encontra o termo antes de saber o que é.
- Correção: definir esforço na frase que apresenta a Tabela 1 e escrever "o Claude Fable" na primeira ocorrência.

**2.2 Proporção das seções. SUGESTÃO.**
- Onde: Fundamentação (4,4%), Trabalhos relacionados (3,7%) e Limitações (8,6%, cerca de 1.350 palavras).
- Motivo: o guia sugere 10% a 15%, 5% a 10% e meia página. A Fundamentação curta se compensa pelos conceitos da Introdução; as Limitações longas repetem pontos que já estão na Discussão (as precisões do CLAUDE.md, +18 contra +8; a redação diferente entre tarefas, também na P5 e na Conclusão).
- Correção: nas Limitações, manter cada ameaça uma vez e remeter à Discussão quando ela já estiver discutida lá.

Sem outros problemas: cada seção prepara a seguinte, e cada pergunta é retomada na Seção 7 com o mesmo código.

### Critério 3. Terminologia e siglas

**3.1 Nome da variável estudada e rótulos das tarefas. SUGESTÃO.**
- Onde: o título usa "Orientações de projeto"; a Introdução usa "configuração" e "orientações persistentes" e declara a sinonímia; a P1 da Discussão usa "configuração do *harness*"; o Resumo usa "níveis de orientação" e a Tabela 2, "níveis de configuração". Nas tabelas, as tarefas aparecem como "Construir"/"Manter" (Tabelas 7 a 11) e "construção"/"manutenção" (Tabelas 14 e 15). O título fala em "extensão à manutenção", mas a extensão cobre também a construção com cinco patterns.
- Motivo: o texto avisa que "configuração" e "orientações persistentes" são a mesma coisa, o que resolve a maior parte; o título, porém, introduz um terceiro nome que não reaparece, e o leitor pode achar que "orientações de projeto" é outra coisa.
- Correção: usar no título um dos dois nomes declarados e escolher um par de rótulos para as tarefas nas tabelas.

**3.2 Itálico de estrangeirismo. SUGESTÃO.**
- Onde: "software" está em itálico só na Introdução, parágrafo da Figura 1 ("o *software* em torno do modelo"), e sem itálico nas demais ocorrências ("desenvolvimento de software", "um software existente").
- Correção: uniformizar.

Siglas: LLM, CBO, LCOM, WMC, DIT, API e IA estão expandidas; as demais são de uso corrente. Correto.

### Critério 4. Resumo e Abstract

**4.1 Termo não definido no Resumo. SUGESTÃO.**
- Onde: Resumo, "Em 90 controles negativos na manutenção, 88 execuções mantiveram a mudança pequena".
- Motivo: o guia pede que o Resumo defina em poucas palavras o termo que o leitor de fora não conhece. "Controle negativo" só se entende com a definição da Introdução.
- Correção: acrescentar, junto ao termo, uma oração curta dizendo que são pedidos em que o pattern não se justifica.

O Resumo tem problema, objetivo, o que foi feito e resultado com número, em 200 palavras. O Abstract é tradução fiel (conferi frase a frase).

### Critério 5. Introdução

Sem apontamentos. Contexto, problema e relevância vêm com fatos e fontes (He et al., Hou et al., Galster et al.); o objetivo geral é único; as perguntas estão numeradas e todas são respondidas; o contexto institucional (TCC da Licenciatura, UFPB) está dito, e o texto não menciona laboratório nem cliente, então o ponto do AYTY não se aplica; o parágrafo de organização corresponde às seções.

### Critério 6. Coerência metodológica

**6.1 Critério das notas por dimensão não descrito. OBRIGATÓRIA.**
- Onde: Seção 6.2, parágrafo "Cada projeto foi revisado por um agente": o texto diz o que cada dimensão examina ("os testes, pela contagem e pela cobertura", "o banco, pelo esquema extraído com o `pg_dump` e por conferências simples") e que "Cada dimensão recebe a nota bom, com problemas ou ruim", mas não diz o que leva à nota bom. Tabela 7, linha banco: 0, 0 e 0 na construção; Seção 7, P6: "na construção, nenhuma das 225 execuções aplicou migrações". O mesmo parágrafo diz que cada dimensão tem três notas, mas a categoria do pattern tem quatro valores e a estrutura desnecessária é registrada como presença.
- Motivo: o roteiro trata a falta de motivo e de descrição como obrigatória quando a escolha muda a leitura do resultado, e cita o critério de avaliação como exemplo. Sem o critério, o leitor não sabe se o banco zerado na construção mede um defeito dos agentes ou uma exigência do critério (por exemplo, exigir migrações que o enunciado da construção não pede). O mesmo vale para os testes, que dependem também do apontamento 6.3.
- Correção: descrever, para cada uma das sete notas, a escala usada e o que define bom, com problemas e ruim, ao menos em uma frase por dimensão, e dizer por que o banco exige o que exige. Se o detalhe estiver nas skills do verificador no repositório, resuma aqui e indique a pasta.

**6.2 Descrição da calibração diferente em dois lugares. RECOMENDADA.**
- Onde: Seção 6.2, último parágrafo: "O verificador foi calibrado em seis soluções de referência (quatro contidas e duas com exagero)". Seção 8, quarto parágrafo: "Sua calibração usou seis das dez soluções de referência dos cenários com *pattern*, as que ainda se aplicavam à base com banco, e nenhuma do cenário das crianças". E, na calibração dos cenários com pattern (Seção 6.2), "26 revisões" geram "52 notas", embora cada revisão produza sete notas; a divisão "20 da categoria do *pattern*, 20 do comportamento e 4 de cada uma destas dimensões" não diz de quais revisões vem cada grupo.
- Motivo: a Seção 6.2 descreve soluções de controle (contidas e com exagero); a Seção 8 diz que são soluções dos cenários com pattern. O leitor não sabe se as seis soluções são as mesmas, nem como uma solução de cenário positivo serve de referência para um pedido de controle.
- Correção: dizer, na Seção 6.2, de onde vêm as seis soluções da calibração dos controles e como foram adaptadas; e dizer quais notas de cada revisão entraram na comparação (por exemplo, que as soluções de referência deram a categoria e o comportamento e os exemplos degradados deram arquitetura, testes e banco, se for esse o caso).

**6.3 Testes de integração que falharam por falta de Docker. RECOMENDADA.**
- Onde: Seção 8, quinto parágrafo: "O ambiente não dispunha de Docker dentro do contêiner do agente, o que fez falhar os testes de integração que dependiam dele." Tabela 7, linha testes (11, 9 e 6 na construção).
- Motivo: é uma condição da coleta que pode mudar a nota de testes, e aparece só nas Limitações, sem dizer quantas execuções foram afetadas nem se o verificador descontou essas falhas.
- Correção: na Seção 6.2, dizer como o verificador tratou os testes que falharam por ambiente e, se possível, quantas execuções tinham esses testes.

**6.4 Escolhas sem motivo declarado. RECOMENDADA.**
- Onde e o quê:
  - Seção 5.1: "o N2 acrescenta ao N1 uma *skill* pública de *patterns* [Grndlvl 2026]". Não diz por que essa skill.
  - Seção 6.2: a ordem das repetições ("as repetições do *pattern* que mais errou, o Decorator, e do que mais acertou, o Factory Method (144), e, por fim, as dos outros três (216)"). Não diz por que repetir primeiro os extremos.
  - Seção 8: "os níveis foram executados em um intervalo de cerca de 10 horas, no máximo". No estudo principal, os níveis rodaram juntos para igualar horário e carga (Seção 5.1); a extensão abandona esse cuidado sem dizer por quê, e a informação aparece só nas Limitações.
  - Seção 5.1: "as cinco primeiras são as principais". Não diz por que essas cinco.
- Motivo: uma escolha sem motivo parece arbitrária. Nenhuma delas muda a leitura de um resultado a ponto de ser obrigatória, mas a da simultaneidade afeta a comparação entre níveis da extensão.
- Correção: uma oração com "porque" ou "para" em cada ponto (o motivo só você sabe) e, no caso da simultaneidade, levar a frase para a Seção 6.2.

**6.5 Tabela de IA sem a conferência e sem a análise final. RECOMENDADA.**
- Onde: Tabela 1 e o parágrafo anterior (Seção 4). A tabela tem etapa, modelo e esforço; a conferência aparece em prosa e só para parte das etapas ("O autor supervisionou essas etapas [...]"). A Tabela 1 tem linha para "disparo, coleta e ensaio da análise", mas não diz quem rodou a análise confirmatória do estudo principal, nem quem gerou as tabelas e figuras da extensão.
- Motivo: o guia pede, quando agentes operam etapas, uma tabela com "o que fez" e "como foi conferido". O texto já tem quase tudo; falta fechar quem produziu os números finais e como você os conferiu.
- Correção: acrescentar à Tabela 1 a coluna "como foi conferido" e uma linha para a análise final dos dois estudos.

**6.6 Atividades que aparecem só fora do método. SUGESTÃO.**
- Onde: a releitura humana prevista no pré-registro e não realizada (só na Seção 8, segundo parágrafo); os dois roteiros de comportamento corrigidos depois de testados (só na Seção 8, quarto parágrafo); o `mvn verify` dos controles (Seção 6.3, "O comando `mvn verify` passou nas 90 execuções"), que o método da extensão não menciona.
- Correção: citar cada uma, em uma frase, no método do estudo correspondente.

Elementos atendidos neste critério: o quê, como e com quê estão descritos nos dois estudos; os envolvidos estão identificados (autor, orientador e o que coube aos agentes de IA, Seção 4); a sequência das fases está na Figura 3 e na Seção 4; as versões estão na Tabela 3.

### Critério 7. Apresentação da solução, resultados e conclusão

**7.1 Hipóteses sem a pergunta a que servem, e custo da P4 sem os vereditos pré-registrados. RECOMENDADA.**
- Onde: Tabela 4 (sem coluna de pergunta); Seção 5.3, primeiro parágrafo ("que respondem às perguntas P1 a P4 e P8"); Seção 7, P4 ("O processo (N3) [...] é também o nível mais caro, com custo médio de US$ 1,98").
- Motivo: o leitor precisa deduzir que H6 serve à P1, H11 a H15 à P2, H9 e H10 à P8, e H16 a H18, H22, H27 e H28 ao custo da P4. Várias hipóteses têm resultado só na Tabela 4 e não aparecem no texto (H6, H11 a H18, H22, H24, H27, H28). A resposta à P4 sobre custo usa a média descritiva de US$ e não menciona que H27 e H28 (o processo aumenta tokens e tempo) ficaram inconclusivas e que H22 foi contrariada. A Seção 8 explica parte disso (tokens do subagente), mas a P4 não liga as duas coisas.
- Correção: acrescentar à Tabela 4 uma coluna com a pergunta de cada hipótese e, na P4, uma frase com os vereditos de custo pré-registrados ao lado da média descritiva.

**7.2 Resultado descritivo sem pergunta. RECOMENDADA.**
- Onde: Seção 5.3, segundo parágrafo: "as cinco rodadas de cada modelo produziram menos perfis distintos de classificação do Semgrep no N1 do que no N0 em 7 dos 11 modelos". A Introdução restringe a P7 à extensão ("Na extensão, busca-se verificar [...] quão estáveis são os resultados (P7)"), e a resposta da P7 na Seção 7 não usa esse dado.
- Motivo: o roteiro trata resultado sem objetivo como recomendado: ligar a uma pergunta ou retirar.
- Correção: estender a P7 aos dois estudos e citar esse dado na resposta, ou retirá-lo.

**7.3 Resultados que aparecem pela primeira vez na Discussão. RECOMENDADA.**
- Onde: Seção 7, P6: "na construção, nenhuma das 225 execuções aplicou migrações (uma escreveu os arquivos, e o serviço não chegou a iniciar)"; P7: "o Sonnet 5.5, na manutenção e sem orientação, não teve o *pattern* certo no Factory Method em nenhuma das cinco rodadas"; P4: "na construção do Strategy, em uma rodada, o Haiku 4.5 abriu a *skill* [...] e encerrou sem código" (que explica a execução sem código da Tabela 9).
- Motivo: o guia pede que a Discussão não traga resultado novo; o leitor procura os dados na Seção 6.3 e não os encontra.
- Correção: levar os três dados para a Seção 6.3 e, na Discussão, apenas retomá-los.

Atendido: a solução (os dois experimentos) é apresentada com métricas de dimensão (220, 450 e 90 execuções, 28 hipóteses, 21 casos de aceitação, custo) e artefatos (Figuras 1 a 8, repositório). A avaliação tem processo e resultado descritos, incluindo a concordância do segundo verificador. As limitações e os trabalhos futuros são específicos do trabalho. Cada pergunta tem resposta explícita na Seção 7.

**7.4 Conclusão sem os códigos das perguntas. SUGESTÃO.**
- Onde: Seção 9.
- Motivo: a Conclusão cobre as oito perguntas, mas sem os códigos P1 a P8, e o checklist pede que cada pergunta chegue à Conclusão com o mesmo nome.
- Correção: citar o código entre parênteses na frase que retoma cada resposta.

### Critério 8. Figuras, tabelas e gráficos

Todas as figuras e tabelas têm número, título e fonte, e são apresentadas no texto antes de aparecer. As Figuras 2 e 5 a 8 (código como imagem, fundo branco) e as Figuras 9 e 10 (repetem tabelas) estão nos pontos já decididos.

**8.1 Rótulo do eixo vertical e descrição da Figura 4. SUGESTÃO.**
- Onde: Figuras 9 e 10 não têm rótulo no eixo vertical; a medida está no título interno ("Execuções com o pattern certo (de 75)"). A frase que apresenta a Figura 4 diz só que "os blocos escuros indicam o tratamento manipulado", e a nota da figura diz "Em destaque, o que varia em cada estudo", embora na extensão variem também a tarefa e o modelo.
- Correção: nomear o eixo vertical ("execuções") e, na frase da Figura 4, dizer o que o leitor deve comparar entre os dois desenhos e ajustar a nota para "o tratamento manipulado".

### Critério 9. Inconsistências de conteúdo

**9.1 Cautela diferente para as medidas que a calibração não cobriu. RECOMENDADA.**
- Onde: Introdução, contribuições: "um verificador de qualidade em cinco dimensões, calibrado em casos de resposta conhecida"; Resumo: "a estrutura desnecessária subiu de 4 para 9 execuções em 75"; Conclusão: "com exceção do *design* do restante do código, que acompanhou a orientação". Seção 8, quinto parágrafo: "a calibração não cobriu o *design* do restante do código nem a estrutura desnecessária", e o design teve a menor concordância com o segundo verificador ("kappa de Cohen de 0,53, o mais baixo").
- Motivo: as duas afirmações que mais dependem dessas notas aparecem no Resumo e na Conclusão sem a ressalva, e a Introdução diz que o verificador foi calibrado nas cinco dimensões, o que a Seção 8 desmente para uma delas. O roteiro pede a mesma cautela em todas as partes.
- Correção: na Introdução, dizer quais dimensões a calibração cobriu; na Conclusão, uma oração com a ressalva junto ao design do restante e à estrutura desnecessária. No Resumo, basta não apresentar a medida como calibrada.

**9.2 Afirmação da P5 contrariada pelos próprios números. RECOMENDADA.**
- Onde: Seção 7, P5: "Os dados indicam aumento maior na manutenção somente quando a *skill* é acrescentada: só com o arquivo, o ganho é de 14 execuções na manutenção e de 12 na construção".
- Motivo: 14 é maior que 12, então o aumento também é maior na manutenção só com o arquivo. A Conclusão diz, com mais cuidado, "Apenas com o arquivo, os aumentos foram próximos".
- Correção: usar na P5 a formulação da Conclusão (aumentos próximos só com o arquivo, diferença grande com o arquivo e a skill).

**9.3 Pares não empatados das hipóteses binárias não informados. SUGESTÃO.**
- Onde: Seção 5.3 e Tabela 4, H23 ("a *skill* altera o exagero", não apoiada) e H26 (apoiada com 3 execuções).
- Motivo: pela regra da Seção 5.1, H23 só é "não apoiada" se houver no máximo 2 pares não empatados ou divisão com o lado menor de ao menos um terço; com 1 exagero no N1 e 3 no N2 (Tabela 5), o veredito depende de como esses casos se distribuem nos pares, e o texto não informa. O leitor não consegue conferir a regra.
- Correção: informar, para H2, H23 e H26, o número de pares não empatados e para que lado foram.

Sem outros problemas: não encontrei contradição numérica entre seções, trecho genérico desligado do trabalho nem mudança de estilo.

### Critério 10. Normas e referências

Conferi as 41 referências contra as citações: toda citação está na lista, e toda referência da lista é citada. Abri pela API do Crossref os 26 DOIs da lista: todos resolvem e levam ao trabalho citado, com primeiro autor, ano e título iguais (o Arcuri e Briand aparece no Crossref com data de publicação on-line de 2012; o volume 24(3) é de 2014, como está na lista). Abri também os nove links de documentação e de ferramentas (Anthropic, CK, JaCoCo, Semgrep, SonarQube, Grndlvl, Poole e Mackworth) e o repositório do trabalho, com o commit 54e13fc9 e as cinco pastas citadas: todos abrem. Não conferi o conteúdo das afirmações atribuídas a cada fonte, só a existência e a identidade. Brown et al., Vaswani et al. e Yao et al. não têm DOI, o que é comum para esses anais. Não há marcador pendente, link com parâmetro de rastreio nem citação direta de livro sem página. Os links têm data de acesso. A numeração das seções é consistente.

Sem apontamentos.

### Critério 11. Elementos obrigatórios da resolução

**11.1 Tecnologias dos scripts de análise ausentes da Tabela 3. RECOMENDADA.**
- Onde: Tabela 3; Seção 4, último parágrafo, cita scripts `.mjs` (`extract-meta.mjs`, `aggregate.mjs`, `verify.mjs`) e `.py` (`run_rounds.py`, `run_one_scenario.py`, `run_database.py`), e a Seção 5.1 cita o teste de Page e a Seção 6.2, o kappa.
- Motivo: Node.js e Python, e a biblioteca que calculou o teste de Page e o kappa, fazem parte do processo e importam para repetir a análise, e não estão na tabela.
- Correção: acrescentar essas linhas, com versão e papel. Na mesma tabela, a nota do Docker ("versão instalada em 09/11/2026") deixa em dúvida se foi a versão usada nas execuções de outubro; diga qual foi (SUGESTÃO).

Repositório público: presente e acessível. Descrição das tecnologias: presente, com versão e papel.

## Matriz de rastreabilidade

| objetivo / pergunta | onde é desenvolvido | etapa do método que gera os dados | resultado (seção, tabela) | resposta explícita (onde) | situação | ação pendente |
|---|---|---|---|---|---|---|
| Objetivo geral: avaliar o efeito de orientações persistentes sobre a decisão de aplicar um pattern, na construção e na manutenção | Seções 5 e 6 | Seções 5.1 e 6.2 | Tabelas 5 a 12 | Seção 9, primeiro e segundo parágrafos | atendido | nenhuma |
| P1. Como a configuração muda o uso do pattern esperado | Seções 5.3 e 6.3 | 5.1 (Semgrep, conferência humana); 6.2 (verificador, categorias) | Tabelas 4 (H1, H6), 5, 7, 8, 13, 14; Figuras 9 e 10 | Seção 7, P1: "A evidência [...] aponta para o aumento do uso correto" | atendido | indicar H1 e H6 como hipóteses da P1 (7.1) |
| P2. Exagero e corretude (com o controle negativo da manutenção) | Seções 5.3, 6.3 | 5.1 (suíte de aceitação, Semgrep no ponto do seguro, SonarQube); 6.2 (estrutura desnecessária, controles) | Tabelas 4 (H2, H3, H7, H8, H11 a H15, H23, H26), 5, 7, 12, 14 | Seção 7, P2 e parágrafo do controle negativo | atendido | cautela da estrutura desnecessária (9.1); pares não empatados (9.3) |
| P3. Variação do efeito entre os modelos | Seções 5.3 e 6.3 | 5.1 (quartetos, pares, teto); 6.2 (três modelos) | Tabelas 4 (H5, H19, H20), 13, 14 | Seção 7, P3: "O efeito concentra-se nos modelos que não acertam sem orientação" | atendido | nenhuma |
| P4. Contribuição de cada componente e custo | Seções 5.3 e 6.3 | 5.1 (níveis cumulativos, transcrição, custo); 6.2 (custo, abertura da skill) | Tabelas 4 (H16 a H18, H21 a H28), 5, 11, 15 | Seção 7, P4 e dois parágrafos seguintes | parcial | citar na resposta os vereditos de custo H16 a H18, H22, H27 e H28 (7.1) |
| P5. Efeito do tipo de tarefa em cinco patterns | Seção 6.3 | 6.2 (duas tarefas, cinco cenários) | Tabelas 7, 8; Figura 10 | Seção 7, P5 | atendido | corrigir "somente quando" (9.2) |
| P6. Demais dimensões de qualidade | Seção 6.3 | 6.2 (verificador: arquitetura, testes, banco, comportamento, design do restante) | Tabelas 7, 14 | Seção 7, P6 | parcial | descrever o critério das notas (6.1) e o tratamento dos testes sem Docker (6.3); levar o dado das migrações para a 6.3 (7.3) |
| P7. Estabilidade quando a tarefa se repete | Seção 6.3 | 6.2 (cinco rodadas, comparação com a primeira) | Tabela 16 | Seção 7, P7: "se repetiu em 82% das repetições, o que mostra que uma execução isolada não basta" | atendido | ligar a semelhança entre rodadas do estudo principal (7.2) |
| P8. Estrutura orientada a objetos | Seções 5.3 e 6.3 | Seção 2 (CK, métricas); 5.1 e 6.2 (CK na mesma versão) | Tabelas 4 (H9, H10), 6, 9, 10 | Seção 7, P8 | atendido | nenhuma |
| nenhum: semelhança entre rodadas no estudo principal | Seção 5.3 | 5.1 (questões descritivas) | Seção 5.3, segundo parágrafo | não encontrado | resultado sem pergunta | ligar à P7 ou retirar (7.2) |

## Checklist do guia (seção 10)

**Elementos obrigatórios da resolução**
- A Introdução descreve o problema e o contexto que motivaram o trabalho: atendido.
- A metodologia descreve o processo e identifica os envolvidos: atendido.
- Há uma descrição das tecnologias usadas, com versão: parcial (faltam Node.js, Python e a biblioteca estatística, 11.1).
- A solução é apresentada com artefatos e métricas de dimensão: atendido.
- O texto traz o link do repositório público: atendido.
- Se houve avaliação da solução, o processo e os resultados estão descritos: parcial (critério das notas, 6.1).

**Critério 1. Língua portuguesa e redação**
- Sem erros de ortografia, acentuação, concordância ou regência: parcial (1.2).
- Tempos verbais consistentes: parcial (1.3).
- Sem frases longas que encadeiam vários resultados: parcial (1.1).
- Sem declarações de importância sem fato: atendido.
- Sem gerúndio de comentário: atendido.
- "é" e "tem" em vez de "atua como", "serve como": atendido.
- Afirmações atribuídas a terceiros citam a fonte: atendido.
- Sem contrastes montados nem fórmulas de enchimento: atendido.
- Títulos com maiúscula só na primeira palavra e nomes próprios: atendido.
- Sem vírgula antes do "e" final nem decalques do inglês: atendido.

**Critério 2. Encadeamento**
- Cada seção prepara a seguinte: atendido.
- Todo termo técnico é definido antes de ser usado: parcial (2.1).
- Cada pergunta é retomada e chega à Conclusão com o mesmo nome: parcial (7.4).
- O tamanho de cada seção corresponde ao seu papel: parcial (2.2).

**Critério 3. Terminologia e siglas**
- Cada conceito tem um nome só: parcial (3.1).
- Nenhum nome designa duas coisas diferentes: atendido (o próprio texto separa N1/N2 entre estudos e os sentidos de harness).
- Siglas pouco conhecidas expandidas: atendido.
- Estrangeirismos em itálico: parcial (3.2).

**Critério 4. Resumo e Abstract**
- Problema, objetivo, o que foi feito e resultado com número: atendido.
- Resumo se entende sozinho: parcial (4.1).
- Abstract é tradução fiel: atendido.

**Critério 5. Introdução**
- Contexto, problema e relevância com fatos ou fontes: atendido.
- Objetivos ou perguntas claros, numerados e verificáveis: atendido.
- Objetivos correspondem ao que o trabalho fez: atendido.
- Contexto institucional explicado: atendido (não há laboratório nem cliente).
- Parágrafo de organização corresponde às seções: atendido.

**Critério 6. Metodologia**
- Cada etapa diz o que, como, com quê e quem: atendido.
- Sequência das etapas clara: atendido.
- Cada escolha tem motivo declarado: parcial (6.4).
- Todo resultado tem etapa de origem: parcial (6.1, 6.3).
- Toda etapa aparece nos resultados e toda atividade está na metodologia: parcial (6.6).
- Versões informadas quando importam: parcial (11.1).

**Critério 7. Solução, resultados e conclusão**
- Solução descrita com métricas e artefatos: atendido.
- Resultados concretos e organizados pelas perguntas: parcial (7.1).
- Cada pergunta tem resposta explícita: atendido.
- Não há resultado desligado dos objetivos: parcial (7.2).
- Conclusão traz os resultados concretos e mantém as ressalvas da discussão: parcial (9.1).
- Limitações e trabalhos futuros específicos: atendido.

**Critério 8. Figuras, tabelas e código**
- Toda figura e tabela tem número, título e fonte: atendido.
- Toda figura e tabela é apresentada no texto com descrição: parcial (Figura 4, 8.1).
- Todo gráfico tem eixos com rótulo e unidade, e legenda: parcial (8.1).
- Todo trecho de código em texto monoespaçado: pendente por decisão combinada com o orientador (Figuras 2, 5 a 8).
- Nenhuma imagem com fundo preto ou escuro com código: atendido.
- A mesma informação não aparece em tabela e figura: pendente por decisão sua (Figuras 9 e 10).

**Critério 9. Consistência de conteúdo**
- O mesmo número tem o mesmo valor em todas as partes: atendido.
- Resumo, Introdução e Conclusão com a mesma cautela: parcial (9.1).
- Sem contradição entre seções: parcial (6.2, 9.2).
- Sem trechos genéricos: atendido.
- Sem funcionalidades, tecnologias ou etapas citadas uma vez: atendido.
- Estilo uniforme: atendido.

**Critério 10. Normas e referências**
- Toda citação está na lista e toda referência é citada: atendido.
- Cada DOI leva ao trabalho citado: atendido (26 de 26 conferidos).
- Sem marcador pendente: atendido.
- Citações diretas de livro com página e links com data de acesso: atendido.
- Numeração das seções consistente e elementos do modelo do curso: atendido.

**Critério 11. Tecnologias e repositório**
- Tecnologias em tabela, com versão e papel: parcial (11.1).
- Link do repositório abre para qualquer pessoa: atendido.

**Revisão do texto inteiro e matriz**
- Leitura do texto inteiro: atendido (este relatório).
- Matriz completa, sem célula "não encontrado": parcial (uma linha, 7.2).

**Uso de ferramentas de IA**
- Declaração de uso depois da Conclusão: pendente (decidido: será escrita por último).
- IA usada como instrumento descrita na metodologia: parcial (6.5).
- Conferi todo conteúdo, código e referência que uma ferramenta sugeriu: não verificável por esta revisão; cabe a você confirmar.
