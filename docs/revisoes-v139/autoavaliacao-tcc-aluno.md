# Autoavaliação do TCC, versão 139 (modo 3: conferir antes de entregar)

Autoavaliação feita com ferramenta de IA. A avaliação oficial é do orientador.

Os critérios usados aqui são as orientações do professor Rodrigo Rebouças de Almeida para os orientandos dele, e não critérios oficiais da disciplina de TCC, do curso ou da UFPB; quando houver diferença, valem a resolução e as normas oficiais. O uso desta skill é uso de ferramenta de IA e entra na declaração de uso do TCC (seção 8 do guia).

Texto avaliado: `artigo-v139.md` (Markdown, sem páginas). Cada apontamento é localizado por seção, parágrafo e um trecho curto. As figuras foram abertas e inspecionadas uma a uma.

---

## 1. Pontos a tratar

### 1.1 O que o roteiro trata como obrigatório

1. **[OBRIGATÓRIA] Quatro hipóteses da Tabela 4 têm veredito sem a medida descrita no método** (H13 *build*, H14 casos de borda, H15 *code smells*, H18 turnos). Ver apontamento 6.1.

Os cinco elementos da resolução estão presentes: problema e contexto (Seção 1), processo com envolvidos (Seção 4), tecnologias com versão (Tabela 3), apresentação com artefatos e métricas (Seções 5 e 6, Figuras 1 a 10, Tabelas 5 a 16) e repositório público (nota de rodapé da Seção 4). A avaliação também está descrita, com processo e resultados.

### 1.2 Recomendadas, na ordem de prioridade do guia (seção 9)

1. Regras de decisão da Seção 5.1 incompletas para derivar parte dos vereditos da Tabela 4 (apontamento 6.2).
2. Três das quatro questões descritivas anunciadas na Seção 5.1 sem resultado, entre elas o uso do subagente revisor (6.3).
3. P1 a P4 anunciadas como perguntas do estudo principal e respondidas também com a extensão; resultados que não dizem a qual pergunta servem (2.1).
4. Resultados que aparecem pela primeira vez na Discussão, um deles citado no Resumo (7.1).
5. Escolhas do método sem motivo declarado (6.4).
6. Custo definido de um jeito na Seção 5.1 e relatado de outro nos resultados (6.5).
7. As duas "precisões" do `CLAUDE.md` não são descritas, embora expliquem parte do resultado da P1 (6.6).
8. Número de revisões de calibração diferente entre a Tabela 1 e a Seção 6.2 (9.1).
9. Resumo menos cauteloso que a Conclusão sobre a extensão (4.1).
10. Complexidade cognitiva, medida da hipótese principal H4, definida só nas Limitações (2.2).
11. "Projeto" e o nome da variável estudada com mais de um sentido (3.1).
12. "Acerto" com dois sentidos na extensão (3.2).
13. Figuras 9 e 10 repetem dados das Tabelas 7 e 8 (8.1).
14. Frases longas que encadeiam vários resultados (1.1).

### 1.3 Dois pontos registrados sem nível, por decisão já combinada

- **Código em imagem (Figuras 2, 5, 6, 7 e 8).** O critério 8 do roteiro diz que trechos de código não devem aparecer como imagem e que imagens de código com fundo preto não são aceitas; o guia (seção 5) repete as duas regras e acrescenta que, para comparar duas versões, a forma esperada é uma tabela de duas colunas com bloco de texto em cada uma. As cinco figuras têm fundo branco, então a regra do fundo escuro está atendida; a regra do código como texto não está. Você combinou com o orientador manter as cinco como imagem com fundo branco. Fica registrado aqui, sem contar como pendência.
- **Declaração de uso de ferramentas de IA.** O guia (seção 8) pede um parágrafo próprio, depois da Conclusão e antes das Referências, com o título "Declaração de uso de ferramentas de IA", sempre que houve uso de IA em qualquer etapa. A versão 139 ainda não tem esse parágrafo. Você decidiu escrevê-lo por último. Lembre que ele precisa cobrir também o apoio da IA ao desenho dos instrumentos e às bancadas (Tabela 1), e o uso desta skill. A IA usada como instrumento (agentes estudados, operador, verificador) já está descrita no método, como o guia pede.

---

## 2. Leitura macro

### 2.1 Mapa das seções

| seção | o que afirma ou entrega | o que prepara |
|---|---|---|
| Resumo / Abstract | objetivo, os dois estudos e os resultados principais | (autossuficiente) |
| 1. Introdução | IA, agentes, *harness*, *patterns*, problema, objetivo geral, P1 a P8, contribuições, organização | os conceitos de OO e o recorte |
| 2. Fundamentação | encapsulamento, polimorfismo, aberto/fechado, acoplamento e coesão, métricas do CK | as medidas de P8 |
| 3. Trabalhos relacionados | Kim, Kjellberg, Zisa, Ampatzoglou, arquivos de contexto, lacuna | o desenho dos estudos |
| 4. Visão geral do processo | fases, desenho, repositório, envolvidos, IA por etapa, níveis, tecnologias, ambiente | os métodos das Seções 5 e 6 |
| 5. Estudo principal | método, pré-registro, regras de decisão, 28 hipóteses, resultados por nível e por métrica de OO | P1 a P4 e P8 |
| 6. Extensão | evolução do desenho, método, verificador, controles negativos, resultados | P5 a P8 e parte de P1, P2 e P4 |
| 7. Discussão | resposta explícita a P1 a P8 e trajetória do estudo | a Conclusão |
| 8. Limitações | ameaças gerais, por estudo, do verificador e das métricas | os trabalhos futuros |
| 9. Conclusão | retoma o objetivo, resultados, custo, recomendação e trabalhos futuros | |
| Apêndice A | Tabelas 13 a 16, dados por modelo | |

O parágrafo de organização da Introdução ("a Seção 2 apresenta... e o Apêndice A reúne os dados por modelo") corresponde às seções que existem, na mesma ordem. Nada a corrigir aqui.

### 2.2 Linha do argumento

A sequência problema, objetivo, método, resultados e conclusão se fecha. Há três pontos em que uma parte usa algo que a anterior não preparou: a complexidade cognitiva é usada como hipótese principal (H4) antes de ser definida (2.2); algumas hipóteses da Tabela 4 usam medidas não descritas no método (6.1); e a Discussão traz resultados que as seções de resultados não apresentaram (7.1).

### 2.3 Resumo, Introdução e Conclusão

Os três afirmam o mesmo objetivo e os mesmos números centrais (220 e 450 execuções, 90 controles, 67% para 93%, nove de dez combinações, 88 de 90). A diferença está na cautela: a Conclusão registra os custos da extensão (comportamento de 61 para 54 e estrutura desnecessária de 4 para 9 em 75) e o Resumo não (apontamento 4.1). Além disso, a piora do Haiku 4.5 na corretude, citada no Resumo, não está nas seções de resultados (7.1).

### 2.4 Números repetidos

Conferi cada número que aparece em mais de um lugar e somei as tabelas que se desdobram em outras. Fecham:

- Tabela 14 somada por nível e tarefa reproduz todas as células da Tabela 7, e a coluna de estrutura desnecessária dá 4, 5, 9 e 0, 1, 2, como na Seção 6.3 e na Conclusão.
- Tabela 8 somada por nível dá 41, 53, 60 e 19, 33, 58 (Tabela 7, Figura 9, Seção 7). Os ganhos de N0 a N1 dão +18 no Decorator e no Template Method e +8 nos outros três, como na P1 e nas Limitações; os ganhos da P5 (14, 12, 39 e 19) e da P4 (+25 e +14) também conferem.
- Tabela 15 bate com a linha do Decorator na Tabela 8 (4, 10, 10 e 3, 5, 8) e com o texto da P4 (0 para 5 na categoria acerto; 11 execuções com a forma literal).
- Tabela 10 soma 224 execuções na construção e 225 na manutenção (449); acerto mais alternativa equivalente dá 154 e 110, igual às somas da Tabela 7. As médias ponderadas da Tabela 10 reproduzem as da Tabela 9 (interfaces, métodos abstratos, tipos, WMC, CBO).
- Tabela 13: no ponto do clube, 37, 51, 51 e 54 acertos em 55 dão 67%, 93%, 93% e 98% (Tabela 5); os saldos de H1 dos seis modelos fora do teto somam +14 (Seção 5.3); os saldos de H3 somam +3 (Seção 5.3); o Haiku 4.5 tem 6 acertos em 20.
- Tabela 16 soma 294 repetições (82% de 360), 96, 103 e 95 por nível, 89, 96 e 109 por modelo, e 54 combinações estáveis, como na Seção 6.3 e na P7.
- Tabela 12 soma 30, 30 e 28 (88 de 90).
- Custos: as médias da Tabela 5 vezes 55 dão US$ 346, e as da Tabela 11 vezes 75 dão US$ 380, como no texto.
- Regras de decisão: com 55 pares, 36 do mesmo lado dão 3,0% bilateral e 35 dão 5,8%, o que confirma "36 é o menor número"; 5 de 5 dá 6,25% e 8 de 9 dá 3,9% no teste do sinal; 7 em 55 é 12,7% e 2 em 15 é 13,3%.
- Conta aproximada da P8 (CBO vezes tipos): 22% a 46% no estudo principal e 3% a 27% na extensão, como no texto.
- Segunda revisão: 30 + 30 + 29 + 29 + 27 + 26 + 24 = 195 de 210, e 15 diferenças.

Não fecha: o número de revisões de calibração (apontamento 9.1). Tabela 1: "verificador de qualidade (540 revisões e 32 de calibração)". Seção 6.2: "O verificador foi calibrado antes de sua utilização, em 26 revisões [...] A calibração teve duas rodadas. Na primeira, 50 das 52 notas coincidiram [...] Na segunda, as 52 notas coincidiram". Com duas rodadas de 26 revisões, mais as seis do controle, o total seria 58; 32 só fecha se cada caso for contado uma vez.

### 2.5 Termos centrais

Mantêm nome e sentido: *harness* (com a ressalva explícita sobre o pré-registro), *skill*, exagero, contenção, estrutura desnecessária, ponto do clube, ponto do seguro, teto, quarteto, verificador. N1 e N2 com conteúdos diferentes nos dois estudos estão avisados na Tabela 2. Com problema: "projeto" e o nome da variável (3.1), "acerto" (3.2) e, em menor grau, "dimensão", "piloto" e "soluções de referência" (3.3).

### 2.6 Proporção

Contagem aproximada de palavras do texto principal com o apêndice (cerca de 15.600): Introdução 10%, Fundamentação 4%, Trabalhos relacionados 4%, método (Seções 4, 5.1, 5.2, 6.1 e 6.2) 33%, resultados (5.3 e 6.3) 18%, Discussão 14%, Limitações 9%, Conclusão 3%, Apêndice 6%. Ver apontamento 2.4.

### 2.7 Percurso de cada objetivo

Está na matriz da seção 5 deste relatório. Todas as perguntas, P1 a P8, têm desenvolvimento, etapa de método, resultado e resposta explícita na Seção 7. O que falta é o caminho de volta: as seções de resultados não dizem a qual pergunta servem, e a Tabela 4 não diz qual hipótese responde a qual pergunta (2.1).

### 2.8 Proveniência dos resultados

Têm etapa de origem descrita: acertos por ponto (Semgrep e leitura humana, 5.1), suíte de aceitação (5.1), complexidade, linhas, classes e duplicação (SonarQube, 5.1), métricas do CK (Seção 2, 5.1 e 6.2), custo e *tokens* (transcrição, Seção 4), abertura da *skill* (5.1), notas do verificador, concorrência de reservas, migrações, estabilidade, segunda revisão e controles negativos (6.2). Sem etapa descrita: *build*, casos de borda, *code smells* e turnos (H13, H14, H15 e H18; apontamento 6.1). Com origem descrita de forma inconsistente: o custo em US$ (6.5).

### 2.9 Justificativa das escolhas

Têm motivo declarado: Javalin no lugar do Spring, banco de dados, troca da *skill*, posição da instrução no N2 da manutenção, frase acrescentada a três enunciados, os três modelos da extensão, esforço *medium*, exclusão do Fable, Opus 5.5 como verificador, 30 revisões do segundo verificador, segunda rodada do controle, quatro execuções refeitas, limite de −7, limite de 36 pares, limite de 6,3%, teste de Page, quartetos simultâneos, 44 projetos da leitura humana. Sem motivo: ver apontamento 6.4.

---

## 3. Apontamentos por critério

### Critério 1. Língua portuguesa e redação

Não encontrei erros de ortografia, acentuação, concordância ou regência, nem fórmulas de ênfase, atribuições vagas, conclusões genéricas, títulos com maiúsculas em todas as palavras ou marcadores pendentes.

**1.1 [RECOMENDADA] Frases longas que encadeiam vários resultados.**
- Onde: Resumo, segunda frase ("Em estudo pré-registrado, 11 modelos Claude construíram [...] embora o Haiku 4.5 tenha piorado"), com cerca de 60 palavras e quatro resultados; Seção 5.1, terceiro parágrafo (as quatro regras de decisão em sequência, com exceções e exemplos na mesma frase); Seção 7, parágrafo da P4, que reúne em um só parágrafo o efeito do arquivo, da *skill* pública, do processo, da abertura da *skill*, da forma do Decorator, do custo e da *skill* que interrompe o trabalho.
- Por quê: o leitor precisa reler para separar cada resultado, e no Resumo é a frase mais lida do trabalho.
- Correção: uma informação principal por frase; no parágrafo da P4, um parágrafo por componente (arquivo, *skill*, processo). Exemplo com a sua frase da Seção 5.1:
  - Antes: "Cada quarteto foi repetido em cinco rodadas, para que cada comparação tivesse 55 pares, e a ordem dos 55 quartetos foi sorteada com semente registrada, o que totaliza 220 execuções e 55 pares em cada comparação entre níveis vizinhos."
  - Depois: "Cada quarteto foi repetido em cinco rodadas, o que totaliza 220 execuções e 55 pares em cada comparação entre níveis vizinhos. A ordem dos 55 quartetos foi sorteada com semente registrada."

**1.2 [SUGESTÃO] Verbo rebuscado no lugar de "tem".**
- Onde: Seção 1, sétimo parágrafo: "O estudo principal conta com pré-registro".
- Por quê: "conta com" está entre os padrões do critério 1; "tem" diz o mesmo.
- Correção: "O estudo principal tem pré-registro, isto é, [...]".

**1.3 [SUGESTÃO] Tempo verbal diferente entre os dois métodos.**
- Onde: a Seção 5.1 usa o passado ("o agente recebeu uma pasta vazia"), e a Seção 6.2 usa o presente para o mesmo tipo de fato ("Na tarefa de construção, o agente recebe uma pasta vazia"; "o verificador recebe o enunciado"). Na Seção 6.3 e na Discussão, resultados observados também aparecem no presente ("o *pattern* certo aumenta", "O efeito concentra-se").
- Por quê: o guia pede passado para o que foi feito e observado e presente para o que se conclui.
- Correção: passe para o passado os procedimentos da Seção 6.2 e os resultados observados; deixe no presente só as conclusões.

### Critério 2. Encadeamento lógico e coerência textual

**2.1 [RECOMENDADA] P1 a P4 mudam de escopo entre a Introdução e a Discussão, e os resultados não dizem a qual pergunta servem.**
- Onde: Introdução, oitavo parágrafo: "No estudo principal, busca-se verificar como a configuração muda o uso do *pattern* esperado (P1), o exagero e a corretude do código (P2), como o efeito varia entre os modelos (P3) e como cada componente [...] (P4). Na extensão, [...] (P5) [...] (P6) [...] (P7)". Seção 7, P1: "A evidência, pré-registrada no estudo principal e exploratória na extensão"; P3: "Na extensão, o modelo pesa mais que a orientação"; P4: "com pré-registro e matizes trazidos pela extensão". As Seções 5.3 e 6.3 não citam nenhuma P, e a Tabela 4 não diz qual hipótese responde a qual pergunta.
- Por quê: o roteiro pede que cada pergunta chegue à Conclusão com o mesmo nome e o mesmo escopo e seja retomada onde é desenvolvida. Hoje o leitor da Introdução espera P1 a P4 respondidas só com o estudo principal e encontra a extensão nas respostas; e não consegue ligar H1 a H28 às perguntas sem montar essa ligação sozinho.
- Correção: (a) ajuste a Introdução para dizer que P1 a P4 têm evidência confirmatória no estudo principal e complementar na extensão, como a Discussão já faz; (b) acrescente à Tabela 4 uma coluna com a pergunta de cada hipótese (por exemplo, H1 e H6 na P1; H2, H3, H7, H8, H11 a H14 e H23 na P2; H5 na P3; H21 a H28 na P4; H9 e H10 na P8), ou uma frase que faça essa ligação; (c) abra os parágrafos das Seções 5.3 e 6.3 com a pergunta que eles atendem.

**2.2 [RECOMENDADA] Complexidade cognitiva usada antes de ser definida.**
- Onde: aparece na Seção 5.1 ("A qualidade foi medida pela complexidade cognitiva"), na Tabela 4 (H4, uma das cinco hipóteses principais) e na Tabela 5; a definição ("conta as condicionais por construção [Campbell 2018]") está só na Seção 8, quarto parágrafo antes do fim. O mesmo vale para a duplicação, que sustenta H7 e não é definida.
- Por quê: o roteiro pede que termos técnicos sejam definidos antes do uso, e o guia (4.5) pede que a fundamentação explique o que cada métrica usada nos resultados mede e como ler o valor. Sem a definição, o leitor não percebe, na Seção 5.3, que a queda da complexidade decorre em parte da própria troca de condicionais, ressalva que você só faz no fim.
- Correção: leve para a Seção 2, junto das métricas do CK, uma ou duas frases sobre o que a complexidade cognitiva e a duplicação medem, com a fonte, e diga ali que a queda da complexidade não é independente da mudança de *design*.

**2.3 [SUGESTÃO] Frase repetida literalmente.**
- Onde: Seção 6.3, parágrafo depois da Tabela 10, e Seção 7, P8: "Na construção, a mediana da LCOM é menor nos níveis com orientação (8,45 no N1 e 10,29 no N2) do que no N0 (12,57); na manutenção, quase não muda (5,07, 4,96 e 4,79)."
- Por quê: o critério 2 pede que se removam repetições desnecessárias.
- Correção: na P8, remeta à Seção 6.3 e diga só a leitura que responde à pergunta.

**2.4 [SUGESTÃO] Proporção das seções.**
- Onde: ver 2.6 da leitura macro. Fundamentação (4%) e Trabalhos relacionados (4%) estão abaixo das faixas do guia (10% a 15% e 5% a 10%); o método (33%), a Discussão (14%) e as Limitações (cerca de 9%, contra meia página no guia) estão acima.
- Por quê: o guia diz que uma seção muito fora da proporção deve ser conferida quanto ao papel. Aqui, a Discussão cresce porque apresenta resultados novos (7.1), e a Fundamentação fica curta porque não define métricas usadas nos resultados (2.2).
- Correção: tratar 2.2 e 7.1 já corrige boa parte da proporção. O método longo é compatível com um trabalho experimental; não precisa ser cortado por causa da tabela do guia.

### Critério 3. Terminologia e siglas

As siglas pouco conhecidas estão expandidas (LLM, CBO, LCOM, WMC, DIT, CCAE, DCX). Siglas de uso corrente (API, IA, JDK) não precisam de expansão.

**3.1 [RECOMENDADA] "Projeto" com três sentidos, e a variável estudada com três nomes.**
- Onde: título, "Orientações de projeto e *design patterns*"; Resumo, "orientações persistentes, em arquivos de projeto e *skills*"; Introdução, "padrões de projeto (*design patterns*)" e "o projeto de software recebe pouca atenção"; Seção 3, "o conteúdo de projeto das orientações"; e, em todo o texto, "projeto" como a pasta gerada pelo agente ("44 projetos sorteados", "os projetos ganham interfaces"). A variável é "configuração do *harness*" no problema de pesquisa, "orientações persistentes" no objetivo geral e "orientações de projeto" no título.
- Por quê: em texto técnico, o mesmo nome não pode designar duas coisas. No título, "orientações de projeto" pode ser lido como orientações sobre o *design* ou como orientações guardadas no projeto, e o leitor não sabe qual. Com três nomes para a variável, ele pode entender que são três coisas.
- Correção: escolha um nome para a variável (por exemplo, "configuração" ou "orientações persistentes") e use-o no título, no problema e no objetivo; reserve "projeto" para um sentido só e use "*design*" ou "projeto de software" para o outro, de forma constante.

**3.2 [RECOMENDADA] "Acerto" com dois sentidos na extensão.**
- Onde: Seção 6.2, fim do terceiro parágrafo depois das figuras: "Nos resultados da extensão, *pattern* certo e acertos somam as categorias acerto e alternativa equivalente; quando se trata só da primeira, o texto diz categoria acerto." Na Tabela 10 e na Tabela 15, "acerto" é só a categoria; na Seção 6.3 e na Discussão ("de 19 para 33 e 58 acertos em 75"), é a soma das duas.
- Por quê: você avisou, mas o aviso não impede a confusão: na P4, "a categoria acerto [...] passou de 0 no N1 para 5 no N2" aparece duas frases depois de "Os acertos aumentaram [...] de 33 para 58". O guia pede que nenhum nome designe duas coisas.
- Correção: dê nome próprio à categoria (por exemplo, "forma literal", expressão que você já usa na P4) e deixe "acerto" só para a soma, ou o contrário.

**3.3 [SUGESTÃO] Outros termos com variação.**
- "Dimensão": a Introdução fala em "verificador de qualidade em cinco dimensões"; a Tabela 7 tem seis linhas na coluna "dimensão", incluindo o *pattern*; a Seção 6.2 diz "Cada dimensão recebe a nota bom, com problemas ou ruim", o que não vale para a categoria do *pattern*, e fecha com "sete notas por execução". Diga uma vez quais são as cinco dimensões e quais são as sete notas.
- "Piloto": na Seção 6.2, o piloto (90) é a primeira das cinco rodadas e entra nos resultados; em geral, "piloto" sugere dados descartados. "Primeira rodada" evita a dúvida. Na mesma linha, "rodadas novas" (Seção 6.3) e "repetições" (P7, Tabela 16) são a mesma coisa.
- "Soluções de referência": na Seção 6.2 são as dez dos cenários com *pattern*; na Seção 8, "seis das dez soluções de referência" são as do controle negativo. Diga de quais dez se trata no segundo caso.

**3.4 [SUGESTÃO] Estrangeirismos sem itálico em alguns lugares.**
- Onde: "*software*" está em itálico na Seção 1, quarto parágrafo, e sem itálico em outras oito ocorrências; "scripts" está sem itálico na nota do repositório (Seção 4, "os scripts de análise") e em itálico no resto. Nas Figuras 1, 3, 4, 9 e 10, "harness", "skill", "pattern" e "checkout" estão sem itálico, enquanto a Figura 2 usa "*pattern*" em itálico.
- Correção: uniformize. "Software" pode ficar sem itálico se você o tratar como palavra já incorporada, desde que seja sempre assim.

### Critério 4. Resumo e Abstract

O Resumo tem objetivo, o que foi feito e resultados com números, e o Abstract é tradução fiel (conferi frase a frase). Os dois têm cerca de 160 palavras, dentro da faixa do guia.

**4.1 [RECOMENDADA] O Resumo é menos cauteloso que a Conclusão sobre a extensão.**
- Onde: o Resumo traz da extensão só os ganhos ("o uso correto aumentou [...] em nove de dez combinações"; "88 execuções mantiveram a mudança pequena"). A Conclusão, segundo parágrafo, registra os custos: "o comportamento caiu na manutenção (de 61 para 54 execuções boas em 75), e, na construção com a *skill*, a estrutura desnecessária subiu de 4 para 9 execuções em 75". A última frase do Resumo, "No estudo principal, o ganho concentrou-se nos modelos que não acertavam sem orientação", não traz a ressalva que a Seção 5.3 e a Conclusão fazem ("os únicos com espaço para melhorar").
- Por quê: o roteiro pede que Resumo e Conclusão afirmem a mesma coisa com a mesma cautela. Sem a ressalva, a última frase parece um achado, quando decorre em boa parte da definição de teto.
- Correção: acrescente ao Resumo, em uma oração, o custo observado na extensão, e leve para a última frase a ressalva do teto.

**4.2 [SUGESTÃO] Problema e termos no Resumo.**
- Onde: o Resumo começa pelo objetivo ("Este trabalho investiga como [...]") e não tem a frase de problema ou contexto que o guia (4.2) pede em primeiro lugar. Usa "LLMs" sem expandir e "*skills*" sem dizer o que é.
- Correção: uma frase inicial com o problema (por exemplo, o dado da Introdução sobre o pouco estudo do efeito dessas orientações) e a expansão de LLM; para "*skills*", poucas palavras entre vírgulas bastam.

### Critério 5. Introdução

A Introdução tem contexto, problema com dados e fontes (He et al., Hou et al., Galster et al.), objetivo geral, perguntas numeradas, contribuições e organização. O contexto institucional está claro: o trabalho é um TCC da Licenciatura da UFPB, sem cliente nem laboratório envolvido, então a orientação sobre o AYTY não se aplica. O descompasso de escopo de P1 a P4 está no apontamento 2.1.

### Critério 6. Coerência metodológica

O método diz o que foi feito, como, com quais ferramentas e quem participou (Seção 4, terceiro e quarto parágrafos, e Tabela 1), e a sequência das etapas está clara (Figura 3 e Seção 5.2).

**6.1 [OBRIGATÓRIA] Vereditos de hipóteses sem a medida descrita no método.**
- Onde: Tabela 4, H13 ("não quebra o *build*", medida "*build*"), H14 ("não piora os casos de borda", medida "casos de borda"), H15 ("reduz os *code smells*") e H18 ("altera o número de turnos"). A Seção 5.1 descreve a suíte como "21 casos (12 de cálculo e 9 de recusa)", sem casos de borda; atribui ao SonarQube "a complexidade cognitiva, [...] as linhas, [...] as classes e [...] a duplicação", sem *code smells* (a Tabela 3 repete essa lista); e diz que "O custo foi medido pelos *tokens* e pelo tempo", sem turnos. O *build* não tem procedimento descrito.
- Por quê: o roteiro trata como obrigatório que todo resultado tenha uma etapa do método que o produziu. Sem isso, o leitor não sabe o que é um caso de borda nessa suíte, que ferramenta contou os *code smells* nem como o *build* foi julgado, e não consegue conferir os vereditos.
- Correção: na Seção 5.1, uma oração por medida: quais casos da suíte são de borda (e se são parte dos 21), que ferramenta e regra contaram os *code smells*, como o *build* foi verificado e de onde vem o número de turnos. A alternativa é retirar essas hipóteses da tabela e dizer que estão no pré-registro.

**6.2 [RECOMENDADA] As regras de decisão da Seção 5.1 não bastam para derivar parte dos vereditos da Tabela 4.**
- Onde e por quê, ponto a ponto:
  - Condição de rejeição. A Seção 5.1 diz que a hipótese é inconclusiva "quando o resultado não atinge nem a condição de apoio nem a de rejeição", mas só dá a condição de rejeição das medidas contínuas de alteração ("33 ou menos levam à rejeição"). Faltam as das regras direcional, de não inferioridade e de alteração binária. Mesmo assim, a Tabela 4 tem H7 e H22 (direcionais) como "contrariada", H21, H24, H27 e H28 (direcionais) como "inconclusiva", e H2 e H23 (alteração binária) como "não apoiada". Em H2, a Seção 5.3 diz "apenas 1 dos 55 pares teve resultado diferente", abaixo do mínimo de 5 pares não empatados; pela definição dada, isso não atinge o apoio, mas o texto não diz por que é rejeição e não inconclusão.
  - Regra direcional em medidas contínuas. A regra é "a maioria dos modelos fora do teto melhora e nenhum piora", e "teto" é "o modelo que já acerta nas cinco rodadas". Isso funciona para o ponto do clube (H1, H21, H25), mas não diz o que é um modelo "melhorar" nem estar "no teto" em complexidade (H4, uma das hipóteses principais), duplicação (H7), classes (H8), *code smells* (H15), *tokens* (H22, H27), tempo (H28) e casos da suíte (H24).
  - Teto no exagero. H26 ("o processo reduz o exagero", direcional, apoiada) usa a mesma regra, mas "acertar nas cinco rodadas" não tem sentido direto para o exagero. Também vale dizer, ao lado do veredito, que a regra direcional não exige o mínimo de pares que a regra de alteração exige: H26 foi apoiada com 3 pares, número que não bastaria para H23.
  - Não inferioridade em subconjunto. O limite é "−7 em 55 pares", mas H19 e H20 ("no teto") usam só os cinco modelos no teto, isto é, 25 pares. O texto não diz qual limite valeu (os vereditos não mudam, porque os saldos foram 0 e +1 na Tabela 13, mas a regra precisa estar escrita).
- Por quê: a regra de decisão é o critério de avaliação do estudo principal. Quando o leitor não consegue reproduzir um veredito a partir dela, passa a duvidar dos vereditos que consegue reproduzir. Você já fez o trabalho mais difícil, que é fixar as regras antes; falta trazê-las inteiras para o texto.
- Correção: complete o terceiro parágrafo da Seção 5.1 (ou uma nota, ou um apêndice) com a condição de rejeição de cada regra, a forma de aplicar a regra direcional a medidas contínuas, o sentido de teto para exagero e *tokens* e o limite de não inferioridade usado em H19 e H20.

**6.3 [RECOMENDADA] Três das quatro questões descritivas anunciadas não têm resultado.**
- Onde: Seção 5.1, quarto parágrafo: "Outras quatro questões registradas são apenas descritivas: a semelhança entre rodadas, a mudança do sinal do custo por modelo e o uso efetivo da *skill* e do revisor." Só o uso da *skill* tem resultado ("não foi aberta espontaneamente em nenhuma das 110 execuções"). A semelhança entre rodadas do estudo principal, o sinal do custo por modelo e o uso efetivo do subagente revisor no N3 não aparecem.
- Por quê: o roteiro pede que toda etapa anunciada no método apareça nos resultados. O caso do revisor pesa mais, porque H25 e H26 atribuem efeito ao processo, e a P4 discute o que cada componente acrescenta; se o revisor não foi chamado em parte das 55 execuções do N3, a leitura muda, como você mesmo mostrou com a *skill* que não foi aberta. A Seção 8 confirma que o uso foi registrado ("registrado na transcrição").
- Correção: uma frase com o número de execuções do N3 em que o revisor foi chamado e uma frase para cada uma das outras duas questões, ou a indicação de que estão só no repositório.

**6.4 [RECOMENDADA] Escolhas sem motivo declarado.**
- Onde:
  - Cinco rodadas no estudo principal. A Seção 5.1 diz "Cada quarteto foi repetido em cinco rodadas, para que cada comparação tivesse 55 pares", o que descreve a consequência, não o motivo (custo, cota, poder?). Na extensão o motivo está dado ("decorreu do custo").
  - O Strategy como *pattern* do estudo principal e o serviço de *checkout* como domínio.
  - Os cinco *patterns* da extensão (Seção 6.1 apenas os lista).
  - O N3 "não avaliado" na extensão (Tabela 2); a Seção 9 o põe como trabalho futuro, sem dizer por que ficou de fora.
  - O limite de "ao menos 40 de 44 respostas iguais" para adotar o Semgrep (Seção 5.3).
- Por quê: o roteiro pede um motivo para cada etapa, amostra, número de repetições e critério. Nenhuma dessas escolhas muda a leitura dos resultados de forma direta (no caso dos 40 de 44, a concordância de 174 em 176 fica longe do limite), por isso o nível é recomendado.
- Correção: uma oração com "para" ou "porque" em cada uma; o motivo, só você sabe. Forma: "O estudo principal usou o Strategy porque [...]".

**6.5 [RECOMENDADA] O custo é definido de um jeito e relatado de outro.**
- Onde: Seção 5.1, segundo parágrafo: "O custo foi medido pelos *tokens* e pelo tempo." Tabela 5, Seção 5.3, P4 e Conclusão relatam o custo em dólares ("O lote custou US$ 346 em equivalente de API"; "US$ 1,98 por execução, contra US$ 1,47 no N2"). A expressão "equivalente de API" não é explicada, e "tempo de API" (H17, H28, Tabela 11) também não.
- Por quê: o custo responde a parte da P4 ("e a que custo"). Como as execuções couberam numa cota de uso, o leitor não sabe se os dólares foram pagos ou calculados, nem por quem; e não sabe se "tempo de API" é o tempo total da execução ou só o das chamadas ao modelo.
- Correção: na Seção 4 ou na 5.1, diga que o custo em dólares é o valor que o próprio Claude Code calcula pelos preços da API, sem cobrança real (se for isso), e o que é "tempo de API". Ajuste a frase da Seção 5.1 para incluir o custo em dólares.

**6.6 [RECOMENDADA] As duas precisões do `CLAUDE.md` não são descritas.**
- Onde: Seção 6.1: "o que levou o `CLAUDE.md` a receber duas precisões". A P1 e a Seção 8 atribuem a elas parte do resultado: "justamente os cenários dos quais as precisões do `CLAUDE.md` mais se aproximam"; "De N0 a N1, esses dois *patterns* somam +18 acertos, e os outros três, +8, diferença que não pode ser separada dessas precisões".
- Por quê: o leitor é chamado a ponderar um resultado por um texto que não leu. A Seção 6.2 descreve o `CLAUDE.md` da extensão, mas não aponta quais trechos são as precisões.
- Correção: uma frase na Seção 6.1 ou 6.2 com o conteúdo das duas precisões, ou a indicação de quais frases do resumo do `CLAUDE.md` na Seção 6.2 são elas.

**6.7 [SUGESTÃO] Momento da decisão da segunda rodada do controle.**
- Onde: Seção 6.2: "O controle foi previsto com uma rodada [...] e recebeu a segunda porque o consumo da primeira foi cerca de metade do estimado." Seção 8: "a decisão de realizar duas rodadas [foi tomada] no último dia de execução, depois de conhecidos os resultados dos cenários com *pattern*."
- Por quê: os dois trechos não se contradizem, mas nenhum diz se o resultado da primeira rodada do controle já era conhecido quando a segunda foi decidida. É o tipo de informação que você trata com cuidado no resto do texto (Seção 5.2).
- Correção: uma oração dizendo se a primeira rodada já tinha sido revisada ou não.

**6.8 [SUGESTÃO] Fundamentação anuncia uma medida que a Tabela 6 não traz.**
- Onde: Seção 2, último parágrafo: "Somam-se as contagens de tipos [...] e de métodos abstratos" para "os dois estudos"; a Tabela 6 (estudo principal) não tem métodos abstratos e traz "constantes de *enum* com corpo próprio e classes anônimas", que a Seção 2 não anuncia.
- Correção: alinhe a lista da Seção 2 com as linhas das Tabelas 6 e 9.

### Critério 7. Solução, resultados e conclusão

A solução (o desenho experimental e os instrumentos) está apresentada com artefatos e métricas de dimensão (760 execuções, 21 casos na suíte, 17 versões defeituosas, 26 revisões de calibração). Cada pergunta tem resposta explícita na Seção 7, e não há resultado desligado das perguntas. As limitações são específicas e os trabalhos futuros decorrem delas (por exemplo, medir o ganho do *pattern* pedindo um novo caso).

**7.1 [RECOMENDADA] Resultados que aparecem pela primeira vez na Discussão.**
- Onde: Seção 7, P3: "o Haiku 4.5 tenha piorado nos cinco pares (saldo −5)", resultado que está no Resumo e na Conclusão, mas não na Seção 5.3, que só dá o saldo do conjunto (+3); P4: "ela foi aberta em 69 das 75 execuções do N2 de cada tarefa (19 de 25 no Haiku 4.5 e 25 de 25 nos dois modelos Sonnet)", a mudança de forma no Decorator (Tabela 15) e o caso do Haiku 4.5 que "abriu a *skill* [...] e encerrou sem código"; P7: a estabilidade por modelo e por nível (Tabela 16); P1: "restam 4, 2 e 4 acertos em 10". As Tabelas 13 a 16 ficam no Apêndice e são citadas só na Seção 7.
- Por quê: o guia (4.11) pede que a discussão não apresente resultados novos, e o roteiro manda apontar o resultado que está no Resumo e não nos resultados. Separar o dado da interpretação também deixa a Discussão mais curta.
- Correção: leve esses números para as Seções 5.3 e 6.3, citando as Tabelas 13 a 16 ali, e deixe na Seção 7 só a leitura deles.

**7.2 [SUGESTÃO] A Conclusão resume a P6 sem o resultado positivo.**
- Onde: Conclusão, segundo parágrafo: "Nas demais dimensões de qualidade, o modelo e o ponto de partida pesaram mais que a orientação." Na P6, "O *design* do restante do código acompanha a orientação (de 14 para 25 em 75 na manutenção e de 15 para 20 ou 23 na construção)", e o *design* do restante é uma das "demais dimensões" da Introdução.
- Correção: uma oração dizendo que o *design* do restante do código foi a exceção.

### Critério 8. Figuras, tabelas e gráficos

Todas as figuras (1 a 10) e tabelas (1 a 16) têm número, título e fonte, e todas são apresentadas no texto com o que mostram. As figuras de código têm fundo branco. Sobre o código em imagem, ver 1.3 deste relatório.

**8.1 [RECOMENDADA] A mesma informação em tabela e em figura.**
- Onde: Seção 6.3: "A Figura 9 destaca a primeira linha da Tabela 7" e "A Figura 10 apresenta os dados da Tabela 8 por *pattern* e tarefa". As duas figuras trazem os mesmos números das tabelas, com rótulo em cada barra.
- Por quê: o guia (seção 5) pede que a mesma informação não apareça em tabela e figura; o leitor lê os mesmos números duas vezes.
- Correção: escolha a forma que o leitor entende mais rápido para cada caso. Uma saída é manter as figuras e tirar a linha do *pattern* da Tabela 7 e a Tabela 8 do corpo (a Tabela 8 pode ir para o Apêndice A), ou manter as tabelas e retirar as figuras.

**8.2 [SUGESTÃO] Eixo vertical sem rótulo nos gráficos.**
- Onde: Figuras 9 e 10. O eixo vertical não tem rótulo; a grandeza está no título interno ("Execuções com o pattern certo (de 75)"). A legenda (N0, N1, N2) e os rótulos do eixo horizontal estão presentes.
- Correção: escreva "execuções" no eixo vertical, ou deixe claro na legenda da figura que o título interno é o rótulo do eixo.

### Critério 9. Consistência de conteúdo

O estilo é uniforme, não há trechos genéricos sem ligação com o trabalho e não encontrei contradição de resultado entre seções; as ressalvas da Discussão (teto, ordem cumulativa dos níveis, precisões do `CLAUDE.md`, mesma redação nos níveis orientados do controle) chegam à Conclusão.

**9.1 [RECOMENDADA] Número de revisões de calibração diferente entre a Tabela 1 e a Seção 6.2.**
- Onde: Tabela 1: "verificador de qualidade (540 revisões e 32 de calibração)". Seção 6.2: "calibrado [...] em 26 revisões" e "A calibração teve duas rodadas. Na primeira, 50 das 52 notas coincidiram [...] Na segunda, as 52 notas coincidiram"; e, para o controle, "calibrado em seis soluções de referência".
- Por quê: 26 mais 6 dá 32, mas com duas rodadas de 26 revisões o total de revisões seria 58. O leitor não sabe qual dos dois números está certo.
- Correção: diga se a segunda rodada revisou de novo os 26 casos e ajuste a Tabela 1 (por exemplo, "58 revisões de calibração, em 32 casos").

**9.2 [SUGESTÃO] Um modelo ou dois no apoio ao desenho.**
- Onde: Seção 4, quinto parágrafo: "Um modelo da mesma família dos agentes estudados ajudou a desenhar os instrumentos"; Tabela 1: "apoio ao desenho dos instrumentos e às bancadas | Opus 5 e Opus 5.5".
- Correção: "Modelos da mesma família [...]" ou cite os dois.

### Critério 10. Normas e referências

Todas as 44 referências da lista são citadas no texto, e todas as citações do texto estão na lista. Não há marcador pendente nem citação direta de livro. A numeração das seções é consistente.

Conferi os 26 DOIs da lista no Crossref e no doi.org: todos resolvem para o trabalho citado, com o mesmo primeiro autor, título e ano (no Arcuri e Briand, o Crossref registra 2012 como data de publicação on-line e 2014 como data do volume 24(3), que é o que você cita; está certo). Não puderam ser conferidas por DOI as referências sem DOI (Brown et al. 2020, Vaswani et al. 2017, Yao et al. 2023, os livros e as páginas de ferramentas); confira essas pelo endereço ou pelo catálogo da editora.

**10.1 [SUGESTÃO] Links em nota de rodapé sem data de acesso.**
- Onde: notas de rodapé do Claude Code (Seção 1, segundo parágrafo), do Cursor (Seção 1, sexto parágrafo), do repositório (Seções 4 e 5.1) e das tecnologias (antes da Tabela 3). As referências da lista têm data de acesso; as notas não.
- Correção: acrescente "Acesso em dd/mm/aaaa" em cada nota, como na lista.

### Critério 11. Elementos obrigatórios da resolução

As tecnologias estão em tabela com versão e papel (Tabela 3), e o link do repositório público está no texto. Não abri o repositório nesta revisão; abra o link numa janela anônima para confirmar que ele abre para qualquer pessoa e que as pastas citadas (`construction-v5/infra/scripts`, `TCC_V1`, `construction-v5/experiment/harnesses`) existem com esses nomes.

**11.1 [SUGESTÃO] Tabela 3 incompleta e uma versão com data posterior ao estudo principal.**
- Onde: os *scripts* citados na Seção 4 são em Python (`run_rounds.py`), JavaScript (`verify.mjs`, `aggregate.mjs`) e *shell* (`run-levels.sh`), mas Python e Node.js não estão na Tabela 3. O Docker aparece como "29.7.2 (versão instalada em 09/11/2026)" com uso em "ambos", mas o estudo principal começou em 10/10.
- Correção: acrescente Python e Node.js com versão; diga se a versão do Docker foi a mesma no estudo principal ou informe as duas.

---

## 4. Matriz de rastreabilidade

| objetivo / pergunta | onde é desenvolvido | etapa do método que gera os dados | resultado (seção, tabela, figura) | resposta explícita (onde) | situação | ação pendente |
|---|---|---|---|---|---|---|
| Objetivo geral: avaliar o efeito de orientações persistentes sobre a decisão de aplicar um *pattern*, na construção e na manutenção | Seções 5 e 6 | os dois métodos (5.1 e 6.2) | 5.3, 6.3 | Seção 9, primeiro parágrafo ("Este trabalho avaliou experimentalmente...") | atendido | unificar o nome da variável (3.1) |
| P1. Como a configuração muda o uso do *pattern* esperado | Seções 5 e 6 | 5.1: Semgrep com leitura humana de 44 projetos; 6.2: categoria do *pattern* pelo verificador | Tabela 5 (ponto do clube), Tabela 4 (H1, H6), Tabelas 7 e 8, Figuras 9 e 10 | Seção 7, P1: "aponta para o aumento do uso correto"; Seção 9, primeiros dois parágrafos | atendido | dizer na Introdução que a extensão também responde à P1 e retomar a P1 nas Seções 5.3 e 6.3 (2.1); trazer para 6.3 os números novos da P1 (7.1) |
| P2. Exagero e corretude | Seções 5 e 6 | 5.1: Semgrep no ponto do seguro, suíte de aceitação, SonarQube; 6.2: estrutura desnecessária e controles negativos | Tabelas 4 (H2, H3, H7, H8, H11 a H14, H23, H26), 5, 12 e 14; Seção 6.3 | Seção 7, P2: "A corretude não piora, e a complexidade diminui [...] O código torna-se, assim, menos complexo e maior" | atendido com ressalva | descrever as medidas de H13 e H14 (6.1); completar as regras de rejeição usadas em H2 e H23 (6.2) |
| Controle negativo da manutenção (tratado com a P2) | Seção 6.2, dois últimos parágrafos do método | 6.2: cinco cenários, duas rodadas, verificador informado do controle | Tabela 12, Seção 6.3 | Seção 7, P2, segundo parágrafo: "quase não produziram estrutura onde ela não cabia" | atendido | dizer quando a segunda rodada foi decidida (6.7) |
| P3. Como o efeito varia entre os modelos | Seções 5.3 e 6.3 | 5.1: regra de comparação entre modelos (H5); 6.2: três modelos em faixas diferentes | Tabela 4 (H5), Tabelas 13 e 14 | Seção 7, P3: "O efeito concentra-se nos modelos que não acertam sem orientação" | atendido | levar o saldo −5 do Haiku 4.5 na corretude para a Seção 5.3 (7.1) |
| P4. Contribuição de cada componente e custo | Seções 5.3 e 6.3 | 5.1: níveis cumulativos, transcrição (abertura da *skill*, uso do revisor, *tokens*, custo) | Tabela 4 (H21 a H28), Tabelas 5, 11 e 15 | Seção 7, P4: "o `CLAUDE.md` concentra a maior parte do ganho [...] na manutenção da extensão, a *skill* acrescenta mais"; Seção 9, terceiro e quarto parágrafos | parcial | relatar o uso efetivo do revisor no N3 (6.3); definir custo em US$ e tempo de API (6.5); descrever as precisões do `CLAUDE.md` (6.6) |
| P5. Como o tipo de tarefa muda o efeito em cinco *patterns* | Seção 6 | 6.2: tarefas de construção e manutenção | Tabelas 7 e 8, Figuras 9 e 10 | Seção 7, P5: "Os dados indicam aumento maior na manutenção somente quando a *skill* é acrescentada" | atendido | nenhuma |
| P6. Demais dimensões de qualidade | Seção 6 | 6.2: verificador (jdeps, JaCoCo, `pg_dump`, roteiro de chamadas, concorrência) | Tabelas 7 e 14, Seção 6.3 | Seção 7, P6: "arquitetura, testes e banco oscilam sem direção clara"; Seção 9, segundo parágrafo | atendido | registrar na Conclusão a exceção do *design* do restante (7.2) |
| P7. Estabilidade dos resultados | Seção 6 | 6.2: comparação de cada rodada nova com a primeira | Seção 6.3 (294 de 360), Tabela 16 | Seção 7, P7: "se repetiu em 82% das repetições, o que mostra que uma execução isolada não basta"; Seção 9, terceiro parágrafo | atendido | levar a Tabela 16 para a Seção 6.3 (7.1) |
| P8. Estrutura orientada a objetos | Seções 2, 5.3 e 6.3 | Seção 2 e 5.1: CK (*commit* 8a1ef916); 6.2: CK nas 449 execuções com código | Tabelas 6, 9 e 10 | Seção 7, P8: "A orientação substitui a condicional pelo polimorfismo [...]"; Seção 9, segundo parágrafo | atendido | alinhar a Seção 2 com as linhas das Tabelas 6 e 9 (6.8) |

Não há resultado sem objetivo: estrutura desnecessária, controles negativos e custo da extensão se ligam a P2 e P4; segunda revisão e calibração servem à validade do verificador.

---

## 5. Checklist do guia (seção 10)

### Elementos obrigatórios da resolução
- A Introdução descreve o problema e o contexto que motivaram o trabalho. **atendido**
- A metodologia descreve o processo e identifica os envolvidos. **atendido** (autor, orientador e agentes de IA na Seção 4 e na Tabela 1; não há cliente nem usuários)
- Há uma descrição das tecnologias usadas, com versão. **atendido** (Tabela 3; ver 11.1)
- A solução é apresentada com artefatos e métricas de dimensão. **atendido**
- O texto traz o link do repositório público, ou declara o sigilo. **atendido** (acesso não conferido nesta revisão)
- Se houve avaliação da solução, o processo e os resultados estão descritos. **atendido**

### Critério 1. Língua portuguesa e redação
- Não há erros de ortografia, acentuação, concordância ou regência. **atendido**
- Os tempos verbais são consistentes. **parcial** (1.3)
- Não há frases longas que encadeiam vários resultados. **parcial** (1.1)
- Não há declarações de importância sem o fato que as sustenta. **atendido**
- Não há gerúndio de comentário no fim da frase. **atendido**
- Uso "é" e "tem" onde cabem. **parcial** (1.2)
- Toda afirmação atribuída a terceiros cita a fonte. **atendido**
- Não há contrastes montados nem fórmulas de enchimento. **atendido**
- Os títulos têm maiúscula só na primeira palavra e nos nomes próprios. **atendido**
- Não há vírgula antes do "e" final das enumerações nem decalques do inglês. **atendido**

### Critério 2. Encadeamento
- Cada seção prepara a seguinte, sem saltos de raciocínio. **parcial** (7.1)
- Todo termo técnico é definido antes de ser usado. **parcial** (2.2)
- Cada objetivo ou pergunta é retomado na seção em que é desenvolvido e chega à Conclusão com o mesmo nome. **parcial** (2.1)
- O tamanho de cada seção corresponde ao seu papel. **parcial** (2.4)

### Critério 3. Terminologia e siglas
- Cada conceito tem um nome só, do começo ao fim. **parcial** (3.1, 3.3)
- Nenhum nome designa duas coisas diferentes. **parcial** (3.1, 3.2)
- As siglas pouco conhecidas são expandidas na primeira ocorrência. **atendido**
- Os estrangeirismos estão em itálico. **parcial** (3.4)

### Critério 4. Resumo e Abstract
- O resumo contém o problema, o objetivo, o que foi feito e o resultado, com número. **parcial** (4.2: falta a frase de problema)
- O resumo se entende sozinho, sem termos não definidos. **parcial** (4.2)
- O Abstract é a tradução fiel do resumo. **atendido**

### Critério 5. Introdução
- Contexto, problema e relevância com fatos ou fontes. **atendido**
- Objetivo geral e perguntas claros, numerados e verificáveis. **atendido**
- Os objetivos correspondem ao que o trabalho de fato fez. **parcial** (2.1: escopo de P1 a P4)
- O contexto institucional está explicado. **atendido**
- O parágrafo de organização corresponde às seções e apêndices que existem. **atendido**

### Critério 6. Metodologia
- Cada etapa diz o que foi feito, como, com quais ferramentas e quem participou. **atendido**
- A sequência das etapas e as dependências estão claras. **atendido**
- Cada escolha do método tem motivo declarado. **parcial** (6.4)
- Todo resultado apresentado tem uma etapa da metodologia que o produziu. **parcial** (6.1, 6.5)
- Toda etapa aparece no desenvolvimento ou nos resultados, e toda atividade relatada está na metodologia. **parcial** (6.3, 6.6)
- As versões das ferramentas estão informadas quando importam. **atendido** (ver 11.1)

### Critério 7. Solução, resultados e conclusão
- A solução é descrita com métricas e artefatos. **atendido**
- Os resultados são concretos e organizados pelos objetivos ou perguntas. **parcial** (2.1)
- Cada objetivo e cada pergunta tem uma resposta explícita. **atendido**
- Não há resultado desligado dos objetivos. **atendido**
- A conclusão traz os resultados concretos e mantém as mesmas ressalvas da discussão. **atendido** (ver 7.2)
- As limitações e os trabalhos futuros são específicos do trabalho. **atendido**

### Critério 8. Figuras, tabelas e código
- Toda figura e tabela tem número, título e fonte. **atendido**
- Toda figura e tabela é apresentada no texto, com descrição do que mostra. **atendido**
- Todo gráfico tem eixos com rótulo e unidade, e legenda. **parcial** (8.2)
- Todo trecho de código está em texto com fonte monoespaçada, nenhum em imagem. **pendente** (Figuras 2, 5, 6, 7 e 8; mantidas como imagem por acordo com o orientador, ver 1.3)
- Nenhuma imagem tem fundo preto ou escuro com código. **atendido**
- A mesma informação não aparece repetida em tabela e figura. **parcial** (8.1)

### Critério 9. Consistência de conteúdo
- O mesmo número tem o mesmo valor em todas as partes do texto. **parcial** (9.1)
- Resumo, Introdução e Conclusão afirmam a mesma coisa com a mesma cautela. **parcial** (4.1)
- Não há contradição entre seções. **parcial** (6.5, 9.2)
- Não há trechos genéricos sobre o tema sem ligação com o trabalho. **atendido**
- Não há funcionalidades, tecnologias ou etapas citadas uma vez e nunca mais. **parcial** (6.1: casos de borda, *code smells*; 6.6: precisões)
- O estilo de escrita é uniforme ao longo do texto. **atendido**

### Critério 10. Normas e referências
- Toda citação está na lista, e toda referência é citada. **atendido**
- Abri cada DOI e ele leva ao trabalho citado. **atendido** (26 de 26 conferidos aqui)
- Não há marcador pendente como "[CITAR]". **atendido**
- As citações diretas de livro têm página, e os links têm data de acesso. **parcial** (10.1)
- A numeração das seções é consistente, e o documento tem os elementos do modelo do curso. **atendido**

### Critério 11. Tecnologias e repositório
- As tecnologias estão descritas em tabela, com versão e papel. **atendido** (ver 11.1)
- O link do repositório está no texto e abre para qualquer pessoa. **parcial** (o link está no texto; a abertura não foi conferida nesta revisão)

### Revisão do texto inteiro e matriz
- Fiz a leitura do texto inteiro descrita na seção 7. **atendido** (feita nesta autoavaliação; refaça depois das correções)
- A matriz de rastreabilidade está completa, sem célula "não encontrado". **atendido**

### Uso de ferramentas de IA
- Se usei ferramentas de IA, a declaração de uso está depois da Conclusão. **pendente** (você vai escrevê-la por último, ver 1.3)
- IA usada como instrumento do trabalho está descrita na metodologia. **atendido** (Tabela 1, Seções 4 e 6.2)
- Conferi todo conteúdo, código e referência que uma ferramenta sugeriu. **parcial** (os DOIs foram conferidos aqui; o restante só você pode confirmar)

---

## 6. Contagem

- OBRIGATÓRIA: 1 (6.1)
- RECOMENDADA: 14 (1.1, 2.1, 2.2, 3.1, 3.2, 4.1, 6.2, 6.3, 6.4, 6.5, 6.6, 7.1, 8.1, 9.1)
- SUGESTÃO: 14 (1.2, 1.3, 2.3, 2.4, 3.3, 3.4, 4.2, 6.7, 6.8, 7.2, 8.2, 9.2, 10.1, 11.1)
- Registros sem nível, por decisão combinada: código em imagem (Figuras 2, 5, 6, 7 e 8) e declaração de uso de IA.
