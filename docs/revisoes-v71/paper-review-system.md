# Relatório de revisão (método paper-review-system)

**Artigo**: Orientações de projeto e design patterns em agentes de programação: um estudo com pré-registro e sua extensão à manutenção (versão 71)
**Evento considerado**: perfil adaptado de um bom artigo de evento brasileiro de engenharia de software, como o SBES (não há evento alvo definido)
**Data da revisão**: 09/11/2026
**Material lido**: somente `J:\TCC\docs\versoes\artigo-v71.md` e as seis figuras em `J:\TCC\docs\figuras\`

---

## Como o método foi aplicado

**O que foi seguido.** As fases do `review-paper`: leitura e extração, leitura rápida (fase 1.5), avaliação nas 8 dimensões (papéis A a E do método), relatório no modelo `review-template.md` e notas pelo `scoring-guide.md`.

**O que mudou na execução, conforme o pedido.**

- Uma única passada, sem subagentes.
- O Markdown foi lido no lugar do PDF, e as figuras foram lidas em PNG.
- Nenhum script ou servidor MCP do plugin foi usado. A leitura rápida foi feita à mão.
- Não houve busca externa, nem leitura dos dados do repositório ou de `docs/revisoes-v53`.
- Fase 3 (auditoria de código): não realizada.

**Critérios adaptados ao perfil de evento brasileiro de engenharia de software.**

| Critério do plugin (grandes conferências) | Adaptação |
|---|---|
| Pesos por conferência (`venue-profiles.md`) | Pesos próprios: Experimentos 25%, Correção técnica 20%, Novidade 15%, Resumo e motivação 10%, Escrita 10%, Reprodutibilidade 10%, Trabalhos relacionados 5%, Material suplementar 5% |
| Comparação com o estado da arte em rankings | Trocada pela comparação com a linha de base experimental (N0) e pelo posicionamento em relação aos trabalhos citados |
| Barras de erro, Params, FLOPs, FPS | Trocados por incerteza das proporções, regras de decisão e custo (tokens, tempo, US$) |
| Checklists NeurIPS, ACL, MICCAI | Trocados por: ameaças à validade, disponibilidade de artefatos, pré-registro e desvios. Do perfil ACL, mantive a verificação sobre uso de LLM: versão, custo, contaminação e LLM como avaliador |
| Bases de dados e métricas obrigatórias | Não se aplicam |
| Mapeamento de nota do evento | Não usei escala oficial de nenhum evento. Mantive a recomendação do modelo do plugin |
| Revisão anônima | Verificada só como alerta, porque o evento não está definido |

**Pontos conhecidos, não apontados**, conforme o pedido: abstract em inglês, palavras-chave, seção de uso de IA, marcadores [CITAR], citação do kappa e do teste de Page, evento alvo, tamanho do resumo, prosa da Seção 8, tabela por modelo e cruzamento de classe por nível. As limitações já declaradas (um fornecedor, verificador de IA, ausência de leitura humana) só aparecem aqui onde o texto as trata de modo incoerente.

---

## Leitura rápida (fase 1.5)

| Sinal | Resultado |
|---|---|
| Tipo de artigo | Empírico, experimento controlado com extensão exploratória |
| Estrutura | Introdução, fundamentação, relacionados, método, resultados, discussão, limitações e conclusão, todos presentes |
| Atualidade das referências | 19 de 40 entradas são de 2025 ou 2026 (47,5%). Sem ferramentas e documentação, 14 de 34 (41%). Mediana: 2024. Acima do limiar de 25% do método |
| Sinais estatísticos | Sem intervalo de confiança nem desvio em nenhuma proporção. Só há valores de p no teste de Page. As regras de decisão são por contagem de pares |
| Eficiência | O método promete tokens e tempo; a Tabela 1 traz só US$ |
| Anonimato | O texto traz link do GitHub com o nome do usuário. Precisa de atenção se o evento for duplo-cego |

**Primeira impressão.** Trabalho incomum para o nível de TCC: desenho cuidadoso, pré-registro, controles negativos e franqueza nas limitações. Conferi todas as somas entre tabelas e texto, e as tabelas estão quase sempre certas (ver "Conferência numérica"). Os problemas estão nas frases que interpretam as tabelas, em especial no resumo, na conclusão e no fechamento da discussão.

---

## Summary (síntese)

O artigo mede como orientações persistentes (`CLAUDE.md`, *skills*, processo) mudam o uso de *design patterns* por agentes de programação, quando o pedido não nomeia o *pattern*. No estudo principal, pré-registrado, 11 modelos Claude constroem um *checkout* em Spring Boot, em quatro níveis e cinco réplicas (220 execuções), com suíte de aceitação, Semgrep, SonarQube e CK. Um arquivo de princípios leva o ponto do clube de 67% para 93%. A extensão exploratória usa cinco *patterns*, construção e manutenção, três modelos, Javalin e PostgreSQL (450 execuções), com verificador de IA calibrado, e mais 90 execuções de controle negativo na manutenção. O *pattern* certo sobe de N0 a N2 em nove de dez combinações, e o ganho com a *skill* é maior na manutenção (19 para 58 em 75, contra 41 para 60).

## Strengths (pontos fortes)

- **S1. Pergunta bem delimitada.** Mede-se a decisão de aplicar o *pattern*, e não a qualidade quando ele é pedido. A Seção 3 separa isso de Kim (2025), Kjellberg et al. e Zisa et al.
- **S2. Pré-registro com verificação.** *Hash* de 122 arquivos, regras de decisão fixadas antes e registro do lote V4 descartado, com a ressalva de que a data de um *commit* é declarada por quem o faz (Seção 4.2).
- **S3. Controle de condições.** Quartetos simultâneos, versão única do agente, suíte testada com 17 versões defeituosas (Seção 4.1).
- **S4. Controles negativos nos dois estudos**, com a leitura honesta de que o teto no N0 e no N1 impede afirmar melhora (Seção 6).
- **S5. Tabelas consistentes.** Tabela 4 soma à Tabela 3; Tabela 6 reproduz as médias da Tabela 5 por ponderação; contagens por modelo e por nível somam 294 (Seção 5.3 e Seção 6).
- **S6. Medida de estabilidade (P7)**, rara nesse tipo de estudo.
- **S7. Autocrítica rara.** O texto admite que a complexidade cognitiva e as métricas de polimorfismo em parte redescrevem a mudança de desenho, e reconhece que a extensão não reproduz o efeito do Strategy na construção.

## Weaknesses (problemas por ordem de gravidade)

Nenhum dos problemas abaixo exige novas execuções. Todos se resolvem com análise já possível ou com ajuste de texto, exceto W22, que fica como trabalho futuro.

### Graves

**W1. O "ponto mais difícil" não é o mais difícil nos dados (Resumo, 4.1, 8; Tabela 1).**
Trechos: "levou o ponto mais difícil do padrão Strategy de 67% para 93%" (Resumo); "Este último é o mais difícil" (4.1); "(de 67% para 93% no ponto mais difícil)" (8).
Na Tabela 1, no N0, o pagamento tem 55%, o cupom 65% e o clube 67%. O clube é apenas o terceiro mais difícil dos quatro. No N1, o pagamento (91%) continua abaixo do clube (93%). O ganho do clube (+26) também é menor que o do pagamento (+36) e o do cupom (+28).
*Sugestão:* dizer "o ponto de desenho mais complexo, escolhido a priori como medida principal", ou "o ponto que afeta três regras", e retirar "mais difícil" do resumo e da conclusão. Justificar a escolha pelo motivo estrutural, não pelo resultado.

**W2. O fechamento da discussão contradiz a resposta da P1 (Seção 6).**
Trechos: "O aumento do *pattern* certo apareceu na mesma direção nos dois estudos" (parágrafo final) contra "A extensão, portanto, não reproduz o efeito do estudo principal nesse ponto" (P1).
A única célula da extensão comparável ao estudo principal (Strategy, construção, N0 para N1) vai de 9 para 7, e de 4 para 2 sem o Sonnet 5.5. Ali a direção é oposta. Resumo e conclusão tratam o caso só como "exceção", sem dizer que é justamente a réplica mais próxima do resultado principal.
*Sugestão:* reescrever a frase final para dizer que o aumento se repete nos outros *patterns*, mas não no Strategy em construção, e levar isso à conclusão.

**W3. "Pattern certo" e "acerto" incluem soluções que não são o *pattern* (Tabela 3, Tabela 6, P1, P4).**
Trechos: "no pattern, acerto ou alternativa" (legenda da Tabela 3); "de 19 para 33 e 58 acertos em 75" (P1); "a forma literal do Decorator apareceu com ela, nas dez rodadas do Sonnet 5.5 e em uma do Sonnet 4.5, e nunca sem ela" (P4).
"Acerto" tem dois sentidos: é uma classe (225 execuções na Tabela 6) e é a soma de acerto com alternativa (264 execuções nas Tabelas 3 e 4). Se "forma literal" corresponde à classe acerto, todas as 22 execuções de Decorator contadas como "certas" no N0 e no N1 são outra coisa, como a lista de regras citada em 5.3. Os +8 do Decorator de N0 a N1 estão dentro dos "+18 acertos" destacados na P1. A classe "alternativa equivalente" é listada em 5.2 sem definição, só recebe exemplos em 5.3, e não está na Seção 2.
*Sugestão:* definir o critério de equivalência no método. Reservar "acerto" para a classe e usar "variação resolvida" (ou similar) para a soma. Informar o quanto do ganho vem de cada classe nos *patterns* em que isso muda a leitura, ao menos no Decorator.

**W4. Erro de raciocínio sobre a frase acrescentada a três enunciados (Seção 7).**
Trecho: "como vale para os três níveis, ela não favorece nenhum deles".
A presença em todos os níveis exclui um efeito principal, mas não uma interação. A Seção 2 diz que a variação crescer é o critério "que os textos dos cenários pressionam", e o `CLAUDE.md` é o que age sobre esse critério. A frase pode, portanto, ajudar mais os níveis com orientação. Os "+18 contra +8" são atribuídos apenas às duas precisões, mas Decorator e Template Method receberam as duas intervenções. O Observer recebeu só a frase (+3 de N0 a N1), e Strategy e Factory Method, nenhuma (+5 somados).
*Sugestão:* retirar a afirmação, declarar a possível interação e apresentar essa divisão em três grupos, que o texto já permite calcular.

**W5. O argumento para não testar a extensão também atinge o estudo principal, e faltam medidas de incerteza (Seções 4.1, 4.3, 7).**
Trechos: "fica abaixo do piso de dez repetições" e "por isso, a extensão descreve as diferenças, sem teste de significância" (7); "Os quartetos se repetiram cinco vezes" (4.1); "teste de Page, p = 0,007" (4.3).
Os dois estudos têm cinco repetições, e só um deles é tratado como insuficiente. Além disso, a conclusão não decorre da premissa: a comparação agregada da extensão tem 75 execuções por nível. Nenhuma proporção tem intervalo de confiança. A própria P7 indica que 18% das repetições mudam de resultado, o que dá cerca de 2,7 mudanças em 15 execuções. De N0 a N1, seis das dez combinações variam 2 ou menos (Strategy construir -2, Strategy manter +1, Observer construir +2, Observer manter +1, Factory Method construir +1, Decorator manter +2).
*Sugestão:* dar a razão real (análise exploratória, sem hipóteses fixadas), incluir intervalos para as proporções principais e dizer que a direção de N0 a N1 é incerta na maior parte das células. Tratar a limitação de repetições também no estudo principal. Informar a unidade do teste de Page (11 modelos ou 55 quartetos).

**W6. As hipóteses não são enumeradas, e a regra aplicada a uma delas não pode ser deduzida (Seções 4.1, 4.3, 6).**
Trechos: "Das cinco hipóteses principais" (4.3); "há ao menos 5 pares não empatados" (4.1); "de 3 execuções em 55 para nenhuma" (4.3); "não alteram o exagero" (P2).
O artigo nunca lista as hipóteses nem o tipo de regra de cada uma. Com 3 execuções contra 0, há no máximo 3 pares não empatados, e a regra de alteração para medidas binárias não seria atingida. A queda no N3 só é "apoiada" se a regra for outra, o que o texto não diz. O mesmo limiar de 5 pares faz com que qualquer alteração em até 4 pares em 55 passe como "sem alteração". A P2 afirma "não alteram", mais forte que "sem alteração detectada" do resumo.
*Sugestão:* uma tabela curta com hipótese, comparação, regra, valor observado e decisão. Trocar "não alteram" por "não se detectou alteração, e a regra só detectaria a partir de 5 pares".

**W7. A regra de desempate sustenta um resultado apoiado e foi fixada depois do ensaio com dados (Seções 4.2, 4.3).**
Trechos: "a regra de análise fixada antes dos dados, no código e no registro de decisões de 09/10" (4.3); "O agente de IA que operou o ensaio teve acesso aos resultados" (4.2).
"Antes dos dados" vale para o V5, mas o V4, do mesmo desenho, já tinha sido usado no ensaio. A regra escrita em 4.1 é "o que menos acerta no N0 deve ter o maior saldo", e o desempate está no código e no registro, sem que se diga que consta do pré-registro. Em substância, dos dois modelos mais fracos, um ganhou +5 e o outro +1, e a hipótese entra nas "quatro apoiadas".
*Sugestão:* dizer se o desempate está nos arquivos com *hash* e se a data é anterior ao ensaio do V4. Se não estiver, marcar a hipótese como apoiada apenas com a regra complementar, ou contá-la à parte.

**W8. Resumo e conclusão selecionam os resultados favoráveis (Resumo, 8, P3, P6).**
Trechos: "sem piorar a correção" (Resumo e 8); "o Haiku 4.5 tenha piorado nos cinco pares (saldo -5)" (P3); "cai na manutenção (61, 59 e 54 em 75)" (P6).
Dos 7 pares piores na correção, 5 são do Haiku 4.5, que ganhou só +1 no ponto do clube. A queda de comportamento na manutenção não aparece nem no resumo nem na conclusão, e o Sonnet 5.5 tem 25 de 25 em todos os níveis (5.3 e Tabela 3). Logo, a queda inteira (de 36 para 29 em 50) vem dos dois modelos menores. O resumo também omite a alta do exagero na construção (de 4 para 9 em 75), que a conclusão traz.
*Sugestão:* acrescentar ao resumo e à conclusão a piora do Haiku 4.5, a queda de comportamento e a alta do exagero, ainda que em poucas palavras.

### Moderados

**W9. Resumo e conclusão dizem que as métricas são todas pós-resultado (Resumo, 8, 4.3).**
Trechos: "descritivas e analisadas depois dos resultados" (Resumo e 8) contra "As hipóteses pré-registradas do CBO e da LCOM foram contrariadas" (4.3). A P8 e a Seção 7 acertam a ressalva ("salvo quanto ao CBO e à LCOM do estudo principal"). *Sugestão:* levar a ressalva ao resumo e à conclusão.

**W10. Conferência humana: as "quatro perguntas" não são definidas e parecem contradizer a frase anterior (4.3).**
Trechos: "cuja classificação não teve conferência humana" contra "as quatro perguntas de desenho atingiram a regra de saída". Como o enunciado tem quatro pontos de Strategy, o leitor entende que os quatro foram conferidos. 4.1 descreve dois critérios, não quatro. *Sugestão:* listar as quatro perguntas.

**W11. A contribuição sobre o verificador é maior que o que a limitação admite (Introdução, 7).**
Trechos: "um verificador de qualidade em cinco dimensões, calibrado em casos de resposta conhecida" (1) contra "a calibração não cobriu o design do restante do código nem o exagero" (7). O texto também registra que o conjunto foi ajustado depois de 50 de 52, de modo que 52 de 52 é o resultado sobre os mesmos casos usados para o ajuste. As classes não calibradas (malfeito e alternativa) somam 128 das 449 execuções (28,5%), dado que o texto não apresenta. *Sugestão:* escrever "parcialmente calibrado", com a cobertura exata, nas duas ocorrências.

**W12. A ameaça do autor que conhece o critério aparece só na extensão (Seções 4.2, 7).**
Trecho: "Na extensão, as orientações, isto é, o `CLAUDE.md` e a *skill*, foram escritas por quem conhece as tarefas e o critério". No estudo principal, o `CLAUDE.md` também foi escrito por quem conhece o enunciado, e depois que "a bancada de testes acumulou 119 execuções". O texto não diz se o arquivo mudou nessas execuções ou no V4. *Sugestão:* declarar a ameaça nos dois estudos e informar se o texto do N1 mudou antes do congelamento.

**W13. A conclusão nega um aumento que a discussão admite (Seções 5.3, 6, 8).**
Trechos: "não se observou aumento de estrutura desnecessária nos controles negativos" (8); "são coerentes com o risco indicado pela construção" (6). A Tabela 7 vai de 30 a 28, e o exagero geral na manutenção é "de 0, 1 e 2" (5.3). *Sugestão:* "o aumento foi pequeno (2 em 30, no mesmo modelo e cenário)".

**W14. A síntese da P6 não acompanha a Tabela 3 (Seção 6, 8).**
Trechos: "é nas dimensões ligadas a ela que os ganhos aparecem"; "arquitetura, testes e banco oscilam sem direção clara" (P6); "o modelo e o ponto de partida pesam mais que a orientação" (8). A arquitetura é ligada à estrutura e vai de 16 a 9 a 16 na construção. O banco na construção não oscila, pois é zero nos três níveis. Os testes caem de forma contínua na construção (11, 9, 6). O design do restante cresce com a orientação, em desacordo com a generalização da conclusão. *Sugestão:* descrever cada dimensão pelo que a tabela mostra e excetuar o design na conclusão.

**W15. O controle negativo da extensão não tem lugar nos objetivos (Seção 1).**
Trechos: "No estudo principal, busca-se verificar como a configuração muda o uso do *pattern* pedido (P1), o exagero e a correção do código (P2)"; "em 450 execuções avaliadas por um verificador de IA calibrado". A Introdução e as contribuições não citam as 90 execuções, que aparecem na Seção 3, em 5.2 e em 6, e a P2 é respondida com elas. As Figuras 3 e 4 também não as mostram. *Sugestão:* incluir o controle no objetivo da P2 e nas contribuições.

**W16. Regras de resumo diferentes entre os dois estudos (5.2, Tabelas 2, 5, 6, P8).**
Trechos: "com as mesmas regras de resumo" (5.2); "CBO médio do projeto, mediana" (Tabela 2); "médias de 75 execuções por nível" (Tabela 5). O WMC e o CBO têm mediana em um estudo e média no outro, o que também impede comparar os níveis absolutos. Na P8, o cálculo do estudo principal multiplica uma média (tipos) por uma mediana (CBO). A Seção 2 diz que as medidas valem "nos dois estudos", mas LCOM e DIT não são relatados na extensão, sem explicação. *Sugestão:* padronizar a medida e explicar por que LCOM e DIT ficaram de fora.

**W17. O ganho maior na manutenção desconsidera o espaço disponível (P5, Resumo).**
Trecho: "Os dados indicam aumento maior na manutenção somente quando a *skill* é acrescentada". O N0 da construção já tem 41 em 75 (restam 34), e o da manutenção 19 (restam 56). Como fração do espaço disponível, com arquivo e *skill*: 70% contra 56%. Só com o arquivo: 25% na manutenção contra 35% na construção, ou seja, o sinal se inverte. *Sugestão:* apresentar também o ganho relativo.

**W18. Não fica claro se o piloto que motivou a mudança faz parte das 450 execuções (5.2).**
Trechos: "depois de um piloto em que o Haiku 4.5 a abriu em apenas 1 de 5 execuções"; "o piloto (90)". Se for o mesmo, as rodadas do N2 de manutenção não têm tratamento homogêneo. *Sugestão:* esclarecer, e dizer se a rodada 1 foi refeita.

**W19. A Tabela 6 mistura *patterns* diferentes em cada classe (5.3).**
Trecho: "as métricas acompanham a leitura do verificador". O próprio texto mostra que os métodos abstratos se concentram em Template Method e Factory Method. A diferença entre classes pode vir da composição de *patterns*. Na manutenção, o malfeito tem mais interfaces (0,77) que o acerto (0,61). *Sugestão:* registrar esse efeito de composição, ou mostrar a comparação dentro de cada *pattern*.

**W20. Comparar os extremos não mostra que o modelo pesa mais (P3).**
Trecho: "o modelo pesa mais que a orientação nas outras dimensões". O melhor modelo sem orientação contra o pior com *skill* é a comparação mais favorável à afirmação. *Sugestão:* citar a diferença entre modelos em um mesmo nível e a diferença entre níveis em um mesmo modelo.

**W21. O texto nomeia só um modelo no comportamento da construção (5.3, P6).**
Trechos: "o Haiku 4.5, na construção, não o teve em nenhuma das 75"; "O comportamento não varia na construção (25 em 75 nos três níveis)". O Sonnet 5.5 tem 25 bons por nível, e o total é 25. Logo, o Sonnet 4.5 também tem zero em 75. "Não varia" esconde que a linha é só o Sonnet 5.5. *Sugestão:* dizer isso, em especial por ter relação com a P3.

**W22. Não há braço placebo (4.1).**
Trecho: "o N1 acrescenta um arquivo `CLAUDE.md` com princípios gerais". Sem um arquivo de tamanho parecido e conteúdo neutro, o efeito não se separa do efeito de haver algum arquivo no contexto. A limitação não é declarada. *Sugestão:* declarar em 7 e incluir nos trabalhos futuros.

**W23. Fidelidade do tratamento (1, 4.1, P4).**
Trechos: "cuja descrição fica sempre disponível ao agente" (1); "o tratamento não chegou a ser aplicado" (P4). A descrição da *skill* estava no contexto, logo parte do tratamento foi aplicada. Como o conteúdo não foi aberto, N2 contra N1 funciona quase como repetição, e as diferenças ali (suíte de 84% para 80%, exagero de 2% para 5%) dão uma ideia do ruído. No N3, o texto não informa se o subagente revisor foi de fato acionado, embora o faça para a *skill*. *Sugestão:* ajustar a frase e informar quantas execuções do N3 acionaram o subagente.

**W24. "Exagero" designa três medidas diferentes (Tabela 1, 5.2, 5.3, P2).**
Trechos: "exagero no controle negativo (ponto do seguro)" (Semgrep); "a estrutura sem necessidade fora da parte que o *pattern* resolve" (verificador, não calibrado); "contenção" contra "exagero" no controle. A P2 passa de uma para outra em "A extensão recomenda cautela". *Sugestão:* nomear cada medida.

**W25. A escolha dos três modelos da extensão não é justificada (5.1, 5.2).**
Trechos: "o teto, já que cinco dos 11 modelos não tinham o que melhorar" (5.1); "Os modelos são o Claude Haiku 4.5, o Sonnet 4.5 e o Sonnet 5.5" (5.2). O Sonnet 5.5 é um dos modelos no teto do estudo principal, e na construção vai de 22 a 25 em 25. *Sugestão:* dizer como e por que os três foram escolhidos e como isso trata o teto.

**W26. "O efeito decorreu dos modelos fora do teto" é em parte consequência aritmética (Resumo, 4.3, 8).**
Modelo com 5 de 5 não tem como subir. O dado informativo é que nenhum dos seis piorou e quanto cada um ganhou. *Sugestão:* reformular nesses termos.

---

## Reference Audit (auditoria das referências)

**Não verificado por falta de busca externa:** existência de cada obra, autores e anos reais, DOIs, números atribuídos às fontes (5,5% e 4,6% em Galster; 806 repositórios em He; 395 artigos e 0,92% em Hou; 2.303 e 1.925 em Chatlatanagulchai; 148 em Arabat; "piso de dez" em Arcuri e Briand), adequação de cada citação ao conteúdo da fonte e publicações recentes que faltem.

**Resultado da conferência interna:** as 40 entradas têm citação no corpo, e todas as 40 obras citadas no corpo têm entrada. Não encontrei divergência de autor ou ano entre o corpo e a lista. Kim (2025) e Kim (2026) estão bem distinguidos.

### Verification Results

Status externo de todas as linhas: **não verificado**.

| # | Entrada | Citada em | Observação interna |
|---|---|---|---|
| 1 | Ahmed et al. 2025 | 7 | Sem DOI |
| 2 | Ampatzoglou et al. 2026 | 3 | Último autor sem "e", diferente das demais |
| 3 | Anthropic 2026 | 1 | Três documentos em uma entrada, citada para coisas distintas; usar 2026a, b, c. Fora da ordem alfabética (Aniche vem antes) |
| 4 | Aniche 2015 | 2 | Repositório; o *commit* está só no corpo |
| 5 | Arabat e Sayagh 2026 | 3 | Sem problema interno |
| 6 | Arcuri e Briand 2014 | 7 | Sem problema interno |
| 7 | Basili et al. 1996 | 7 | Sem problema interno |
| 8 | Boehm 1976 | 1 | Sem DOI |
| 9 | Brown et al. 2020 | 1 | Sem páginas nem DOI |
| 10 | Campbell 2018 | 7 | Sem problema interno |
| 11 | Chatlatanagulchai et al. 2026 | 3 | Sem volume ou páginas (conferir se ainda é publicação antecipada) |
| 12 | Chidamber e Kemerer 1994 | 2, 7 | Sem problema interno |
| 13 | Ernst e Baldassarre 2023 | 1 | O título trata de *registered reports*, modalidade com revisão antes da coleta; o texto a usa para pré-registro em geral |
| 14 | Fowler 2018 | 1, 2 | Sem problema interno |
| 15 | Galster et al. 2026 | 1 | Sem problema interno |
| 16 | Gamma et al. 1995 | 1, 2 | Sem problema interno |
| 17 | Grndlvl 2026 | 4.1 | Fonte não acadêmica, adequada como artefato |
| 18 | He et al. 2026 | 1 | Sem problema interno |
| 19 | Hou et al. 2024 | 1 | Sem problema interno |
| 20 | JaCoCo 2026 | 5.2 | Ferramenta |
| 21 | Jiang e Nam 2026 | 1, 3 | Sem problema interno |
| 22 | Kim 2025 | 3 | Sem problema interno |
| 23 | Kim 2026 | 3 | Sem problema interno |
| 24 | Kjellberg et al. 2026 | 3 | Sem páginas |
| 25 | Liu et al. 2026 | 1 | Sem volume ou páginas |
| 26 | Martin 2003 | 2 | Sem problema interno |
| 27 | Meyer 1997 | 2 | Sem problema interno |
| 28 | Muñoz Barón et al. 2020 | 7 | Sem problema interno |
| 29 | Prechelt et al. 2001 | 1, 7 | Sem problema interno |
| 30 | Sallou et al. 2024 | 7 | Sem problema interno |
| 31 | Semgrep 2026 | 4.1 | Sem versão, importante para a reprodução |
| 32 | SonarSource 2026 | 4.1 | Sem versão, importante para a reprodução |
| 33 | Vaswani et al. 2017 | 1 | Sem DOI, comum nesse evento |
| 34 | Vokáč et al. 2004 | 1, 7 | Sem problema interno |
| 35 | Wang et al. 2025 | 7 | Sem problema interno |
| 36 | Xi et al. 2025 | 1 | Sem problema interno |
| 37 | Yao et al. 2023 | 1 | Sem páginas, comum nesse evento |
| 38 | Yourdon e Constantine 1978 | 2 | Sem problema interno |
| 39 | Zhang e Budgen 2012 | 1, 7 | DOI em minúsculas, diferente do padrão das outras |
| 40 | Zisa et al. 2026 | 3 | Sem problema interno |

Formato: os nomes de evento e revista variam entre forma longa e sigla ("IEEE Transactions on Computers", "IEEE TSE", "*MSR 2025*", "*MSR '26*"). Vale padronizar segundo o modelo SBC.

### Missing Important Citations

Sem busca externa, não posso listar artigos recentes ausentes. Pelo próprio texto:

- O cabeçalho informa "preprints ficam de fora". Em área com publicação tão rápida, isso limita a afirmação de novidade da Seção 3, que já está bem ressalvada em "nenhum dos trabalhos lidos". Recomendo buscar também preprints e citar os mais próximos como trabalhos em andamento, ou declarar esse recorte no corpo.
- A Seção 7 não usa a organização usual da área (validade interna, externa, de construto e de conclusão). Uma referência clássica de experimentação em engenharia de software, como Wohlin et al., ajudaria.

## SOTA Comparison (adaptado)

Não houve busca externa. A linha de base é o N0, adequada ao desenho, e o estudo só compara uma família e um agente, o que já é limitação declarada. Falta apenas comparar o efeito obtido com as magnitudes relatadas pelos trabalhos citados, onde a medida permitir.

## Competitive Paper Analysis (só com o que o artigo cita)

A busca dos últimos 12 meses não foi feita. A tabela abaixo usa a descrição do próprio artigo, e o mês e o ano exatos não foram verificados.

| # | Trabalho | Ano | Linha | Citado | Diferença alegada |
|---|---|---|---|---|---|
| 1 | Kim | 2025 | Refatoração para *pattern* com propriedades lógicas | Sim | *Pattern* nomeado no processo; maior resolução da falha |
| 2 | Kjellberg et al. | 2026 | Orientar LLMs a usar Singleton | Sim | Sem agente nem persistência |
| 3 | Zisa et al. | 2026 | Refatoração baseada em *patterns* | Sim | *Pattern* nomeado, sem persistência |
| 4 | Ampatzoglou et al. | 2026 | Experimento com desenvolvedores | Sim | Com pessoa conduzindo |
| 5 | Chatlatanagulchai et al. | 2026 | Caracterização de arquivos de contexto | Sim | Sem experimento |
| 6 | Arabat e Sayagh | 2026 | Antes e depois do arquivo | Sim | Sem medida de projeto |
| 7 | Jiang e Nam | 2026 | Regras do Cursor | Sim | Questão em aberto |
| 8 | Kim | 2026 | Organização de agentes | Sim | Outra variável |

Avaliação: o posicionamento é claro. Os pontos em que os outros estudos são superiores, como cobrir mais de uma empresa, são reconhecidos.

## Detailed Comments

### 1. Resumo e motivação (4/5)
A motivação é clara e tem evidência citada (He et al., Hou et al., Galster et al., Jiang e Nam). A perda vem do resumo, que tem afirmações que não correspondem às tabelas ou ao corpo (W1, W8, W9).

### 2. Novidade (4/5, provisória)
Medir a decisão não solicitada de aplicar um *pattern* sob variação de configuração persistente é contribuição real, e o controle negativo a reforça. A nota é provisória porque não houve busca e os preprints foram excluídos.

### 3. Correção técnica (3/5)
A espinha dorsal é sólida e as contas fecham. As fragilidades estão nas inferências: W1, W4, W6, W7, W14, W17, W24. Não há equações a verificar; verifiquei a coerência entre definições, regras e decisões.

### 4. Experimentos (3/5)
Desenho e volume fortes para o nível e com controles adequados. Perde pela ausência de incerteza, pelo critério de "certo" (W3), pela falta de réplica na célula mais comparável (W2), pelo argumento desigual sobre repetições (W5) e pela ausência de braço placebo (W22). O uso de LLM atende ao que o perfil ACL pede: versão e esforço declarados, custo parcial (só no estudo principal), contaminação tratada em 7.

### 5. Escrita (4/5)
O texto é legível e bem estruturado. Faltam ajustes de vocabulário, como "acerto", "correção" e "exagero", e de registro na Seção 2. Há sentenças muito longas na P4 e na P8.

### 6. Reprodutibilidade (4/5)
Muito bom: versões, *commits*, data e repositório público, com caminhos dos textos de orientação. Lacunas: versão do Claude Code na extensão, versões do Semgrep e do SonarQube, identificadores exatos de cada modelo e nenhum trecho do `CLAUDE.md` no artigo, embora ele seja a variável estudada. Não abri o repositório, por instrução.

### 7. Trabalhos relacionados (4/5, provisória)
Seção focada, que declara o que cada estudo faz melhor. Só a busca externa pode confirmar a cobertura.

### 8. Material suplementar (3/5, provisória)
O artigo aponta o repositório com os instrumentos, mas não pude avaliar o conteúdo. No texto, não há apêndice com as hipóteses ou com o texto das orientações (W6).

## Conferência numérica (feita à mão)

| Item conferido | Resultado |
|---|---|
| Tabela 4 contra Tabela 3 e Figuras 5 e 6 | Bate em todas as células |
| Totais: 220, 450, 90, piloto 90 + 144 + 216 | Bate |
| Por modelo: 22+14+5 = 41; 25+22+13 = 60; 11+5+3 = 19; 25+20+13 = 58 | Bate |
| +18 e +8; 15 para 14; 4, 2 e 4 em 10 | Bate |
| Tabela 6 contra Tabela 3 (154 e 110) e 224 + 225 = 449 | Bate |
| Médias ponderadas da Tabela 6 contra Tabela 5, em todas as colunas | Bate |
| Clube: 37, 51, 51 e 54 em 55; 14 pares; saldos compatíveis (+5, +1, Haiku 5.5 +2, Sonnets +6); Haiku 4.5 com 6 em 20 | Bate |
| Custo: 6,29 × 55 = US$ 346 | Bate |
| 174 de 176; 52 notas; 12 notas; 195 de 210 | Bate |
| Estabilidade: 294 de 360 = 82%; 89+96+109 = 294; 96+103+95 = 294 | Bate |
| CBO × tipos: 22 a 46% e 3 a 27% | Bate (ver W16 sobre média × mediana) |
| Figuras 3 e 4 | Coerentes com o texto, mas sem as 90 execuções de controle |

Divergências entre texto e tabela estão em W1, W13, W14 e W21.

## Code Audit

Não aplicável, por instrução.

## Minor Issues

1. Seções 6 e 7: "correção" tem dois sentidos, o do software e o da avaliação ("A correção é realizada por uma IA", Seção 7; "enunciados e correções diferentes", Seção 6). Sugiro "avaliação" para o segundo.
2. Seção 1: "uso do *pattern* pedido" conflita com "quando o pedido não os menciona".
3. Seção 4.1: "os 11 modelos Claude disponíveis" conflita com "O Fable foi excluído". Dizer "11 dos modelos".
4. Seção 4.3: "a hipótese de não repetir o comum" não foi explicada antes.
5. Seção 4.3 e Tabela 2: o texto fala em "constantes de *enum* com corpo próprio", e a linha da tabela inclui "classes anônimas".
6. Seção 4.1: o custo é medido "pelos *tokens* e pelo tempo", mas só aparece US$. A extensão não traz custo.
7. Seção 4.2: "(62 até 06/10)" não diz para que serve o número. "V5" aparece sem apresentação.
8. Seção 2: registro menos formal que o restante ("obriga a mexer", "responde do seu jeito").
9. Seção 2: "que isso apareça nas métricas de acoplamento é uma hipótese deste trabalho" não é testada em nenhum estudo, pois não há cruzamento entre exagero e acoplamento.
10. Seção 2, definição de acerto ("atrás de uma interface"): não cobre o Template Method, que usa classe abstrata.
11. P8: "os projetos ganham interfaces (no estudo principal)": a Tabela 5 também mostra aumento na extensão (0,37 para 0,80 na manutenção).
12. P8: "quase não têm nenhum desses mecanismos" inclui constantes de *enum*, não medidas na Tabela 6.
13. Seção 6, controle: a frase "uma mudança pontual pede uma mudança simples" é essencial, mas não aparece em 5.2.
14. Seções 5.2 e 7: "dez soluções de referência" designa dois conjuntos.
15. N2 tem conteúdo diferente nos dois estudos; vale um aviso junto da Figura 4.
16. Itálico irregular de *pattern* e *skill* (Seções 2, 5 e 8, títulos e legendas).
17. Figura 2: "o código existente não muda" ignora o ponto onde a classe é escolhida, que o texto menciona ("escolhida em um só lugar").
18. Figura 3: nomes internos (construction-v2, TCC_V1) precisam de explicação ou podem sair.
19. Linhas 5 a 7 do arquivo: a nota de rascunho deve sair antes da entrega.

## Questions for Authors

1. Quais são as cinco hipóteses principais e todas as demais, com a regra de cada uma?
2. O desempate da hipótese do modelo consta dos arquivos com *hash*?
3. O agente que operou o ensaio do V4 participou da redação ou da revisão do pré-registro?
4. O `CLAUDE.md` do N1 mudou durante as 119 execuções ou entre o V4 e o V5?
5. Quantas execuções do V4 existiram?
6. A unidade do teste de Page é o modelo ou o quarteto?
7. Quais são as quatro perguntas conferidas pelo leitor humano?
8. Qual é o critério para aceitar uma solução como "alternativa equivalente"? A "forma literal" do Decorator corresponde à classe acerto?
9. Quantas das execuções certas de Decorator no N0 e no N1 são alternativas?
10. O piloto do Haiku 4.5 (1 de 5) está nas 450 execuções?
11. O subagente revisor do N3 foi acionado em quantas execuções?
12. Por que esses três modelos na extensão?
13. Por que LCOM e DIT não são relatados na extensão?

## Overall Assessment

| Dimensão | Nota | Comentário breve | Peso adaptado |
|---|---|---|---|
| Resumo e motivação | 4/5 | Motivação clara; resumo com incoerências | 10% |
| Novidade | 4/5 | Pergunta nova (provisória) | 15% |
| Correção técnica | 3/5 | Núcleo sólido, inferências a ajustar | 20% |
| Experimentos | 3/5 | Desenho forte, faltam incerteza e critério claro | 25% |
| Escrita | 4/5 | Boa; ajustes de vocabulário | 10% |
| Reprodutibilidade | 4/5 | Ótimos registros, poucas lacunas | 10% |
| Trabalhos relacionados | 4/5 | Focado (provisória) | 5% |
| Material suplementar | 3/5 | Repositório não avaliado (provisória) | 5% |
| **Geral (média ponderada)** | **3,5/5** | | |

### Acceptance Recommendation

- [x] **Weak Accept**, condicionado às correções de W1 a W8.

Como TCC, o trabalho está acima do esperado. Para um evento como o SBES, o estudo é publicável depois de corrigir as frases de resumo, conclusão e discussão que vão além das tabelas.

### Confidence Level

- [x] **3 (razoavelmente confiante).** Verifiquei toda a aritmética e a coerência interna, mas não a literatura externa, os dados nem o repositório.

### Summary Comment

Os dados que o artigo apresenta se sustentam entre si; o que precisa de correção é o que se diz sobre eles. Há três ajustes centrais: retirar "o ponto mais difícil", que a Tabela 1 desmente; admitir na conclusão que a célula mais próxima do estudo principal não repete o efeito; e deixar claro o que conta como "pattern certo", porque no Decorator o ganho no N1 não vem da forma literal do *pattern*. Depois disso, listar as hipóteses com suas regras e apresentar a incerteza das proporções tornaria a contribuição bem mais difícil de contestar. Nenhuma dessas mudanças exige novas execuções.
