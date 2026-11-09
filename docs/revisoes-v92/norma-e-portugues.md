# Revisão de norma e de português da versão 92

Texto de referência: `J:\TCC\docs\versoes\artigo-v92.md`. A coluna "linha" indica a linha desse arquivo. Nada foi alterado no artigo. Não foram propostas mudanças de números nem de sentido. Ficaram de fora os itens já conhecidos: *abstract*, palavras-chave, seção de uso de IA, nota de rodapé do modelo do professor, evento alvo, marcadores [CITAR], citações do kappa e do teste de Page, tamanho do Resumo e reescrita da Seção 8.

O texto do `.docx`, sem marcação e sem espaços, é idêntico ao de `J:\TCC\docs\artigo-v5-e-extensao.md` (55.820 caracteres). A `artigo-v92.md` difere desse arquivo apenas pela linha de versão no topo e pelos caminhos das imagens.

## 1. Medidas do .docx

| item | exigido | encontrado | situação |
|---|---|---|---|
| Papel | A4 | 11906 x 16838 twips (21,0 x 29,7 cm), retrato | conforme |
| Margem superior | 3,5 cm | 1984 twips (3,50 cm) | conforme |
| Margem inferior | 2,5 cm | 1417 twips (2,50 cm) | conforme |
| Margens laterais | 3 cm | 1701 twips (3,00 cm) | conforme |
| Fonte do corpo | Times 12 | Times New Roman 12 (`docDefaults`) | conforme |
| Espaço antes do parágrafo | 6 pt | `before=120` (6 pt) nos 111 parágrafos de corpo e referências; entrelinha simples | conforme |
| Título | 16, negrito | Times New Roman 16, negrito, centralizado, 12 pt antes e depois | conforme |
| Seções | 13, negrito | 10 títulos em Times New Roman 13, negrito, 12 pt antes | conforme |
| Subseções | (modelo: 12, negrito) | 6 títulos em Times New Roman 12, negrito | conforme |
| Recuo de parágrafo | (modelo: 1,27 cm, exceto o primeiro) | 720 twips (1,27 cm); primeiro parágrafo depois de título, tabela ou figura sem recuo | conforme |
| Número de página, cabeçalho e rodapé | (modelo: sem) | nenhum | conforme |
| Legenda de tabela antes | sim | 7 de 7 | conforme |
| Legenda de figura depois | sim | 6 de 6 | conforme |
| Tabelas e figuras citadas antes | sim | 13 de 13 | conforme |
| Largura de figuras e tabelas | dentro da mancha | figuras e tabelas com 15,0 cm, igual à mancha | conforme |
| Tabelas | não fixado | Times New Roman 10 | sem exigência |
| Legendas | não fixado no pedido | Arial 10, negrito, centralizadas, 6 pt antes e depois | ver D4 e D5 |
| Resumo | até 10 linhas | cerca de 25 linhas na largura total (item já conhecido) | conhecido |
| Mínimo de páginas | 12 | cerca de 23 (ver abaixo) | conforme |

Estimativa de páginas: 23, com margem de erro de cerca de uma página. Foi calculada com as larguras reais da fonte Times New Roman, quebra de linha simulada, alturas de figuras e tabelas e o espaço útil de 15,0 x 23,7 cm; a soma dá 22,6 alturas de página. O texto vai até a página 20 ou 21, e as referências ocupam as páginas 21 a 23. O arquivo não foi aberto em um editor, então a paginação real não foi vista.

### Problemas do .docx

O formato do bloco em D2 e as medidas D3, D5 e D6 vêm do texto do modelo da SBC, que não estava no pedido nem em cópia em `J:\TCC`; foram lembradas de memória e devem ser conferidas no modelo.

| id | onde | problema | correção exata |
|---|---|---|---|
| D1 | 2º parágrafo do `.docx` (linhas 5 a 7) | A nota "Rascunho de 23/10/2026, com as cinco rodadas..." continua no arquivo final. | Apagar o parágrafo inteiro. |
| D2 | Depois do título | Não há bloco de autoria (nome, instituição e e-mail). O item não consta da lista de pendências conhecidas. | Inserir, centralizados: `[nome do autor]`, em negrito; `Centro de Ciências Aplicadas e Educação, Universidade Federal da Paraíba`; `[e-mail]`, em Courier New 10. |
| D3 | 40 entradas das Referências | Primeira linha com recuo de 1,27 cm (`firstLine=720`). O modelo pede primeira linha sem recuo e as seguintes com 0,5 cm. | Nas referências, usar `<w:ind w:left="283" w:hanging="283"/>`, no lugar de `firstLine=720`. |
| D4 | Legendas das Figuras 1, 5 e 6 | A legenda inteira é um único trecho, e o itálico de *harness* e *pattern*, que existe no `.md`, se perde no `.docx`. | Fazer o gerador respeitar o itálico dentro da legenda. |
| D5 | Legendas | Oito das 13 legendas passam de uma linha (Figuras 2, 3, 5 e 6; Tabelas 3, 5, 6 e 7) e estão centralizadas. O modelo pede legenda justificada, com recuo de 0,8 cm nos dois lados, quando passa de uma linha. | Nessas oito: `jc=both` e `<w:ind w:left="454" w:right="454"/>`. |
| D6 | Resumo | "Resumo" está como título de seção em 13 pt e o texto vai de margem a margem. O modelo pede a palavra em negrito no início do parágrafo e recuo de 0,8 cm nos dois lados. | Na reescrita do Resumo, iniciar com **Resumo.** em negrito 12 e aplicar `left=454` e `right=454`; o mesmo no *Abstract*. |

## 2. Estrutura exigida pelo Regulamento (Art. 6º)

| elemento | onde está | situação |
|---|---|---|
| Problema | Seção 1, linhas 80 e 81 ("este trabalho parte do seguinte problema de pesquisa") | presente |
| Motivação | Seção 1, linhas 72 a 78 | presente |
| Objetivos | Seção 1: geral nas linhas 81 a 83; específicos nas linhas 90 a 94 | presente |
| Metodologia | Seções 4.1, 4.2 e 5.2 | presente, sem título próprio em nível de seção, o que o Art. 6º não exige |
| Fundamentação e trabalhos relacionados | Seções 2 e 3 | presente |
| Trabalho desenvolvido | Seções 4 e 5 | presente |
| Resultados | Seções 4.3, 5.3 e 6 | presente |
| Conclusões | Seção 8 | presente |
| Referências | 40 entradas e 1 marcador conhecido | presente |
| Mínimo de 12 páginas | cerca de 23 | atendido |

## 3. Citações e referências

Contagem: 61 citações no texto, sendo 49 entre colchetes (em 45 grupos) e 12 na forma Silva (2020). Há 40 entradas, todas citadas, e toda citação tem entrada. A ordem alfabética está correta. O formato dos colchetes e o uso de "e" e "et al." estão corretos. Os DOIs foram consultados no Crossref em 09/11/2026: todos os 24 existentes correspondem ao título e aos autores da entrada.

### Erros (corrigir)

| id | seção (linha) | trecho | problema | correção exata |
|---|---|---|---|---|
| C1 | 1 (61) | Fowler descreve como "generalidade especulativa" | Autor no texto e citação entre colchetes repetem o nome. | `Fowler (2018) descreve como "generalidade especulativa"`, retirando `[Fowler 2018]` do fim da frase |
| C2 | 2 (133) | O princípio vem de Meyer, que o resolve com herança [Meyer 1997] | Idem. | `O princípio vem de Meyer (1997), que o resolve com herança` |
| C3 | 2 (134) | com interfaces, é de Martin [Martin 2003] | Idem. | `com interfaces, é de Martin (2003)` |
| C4 | 2 (144) | métricas de Chidamber e Kemerer [Chidamber e Kemerer 1994] | Idem. | `métricas de Chidamber e Kemerer (1994)` |
| C5 | 7 (398) | Na LCOM de Chidamber e Kemerer, o valor zero não indica coesão máxima [Chidamber e Kemerer 1994] | Idem. | `Na LCOM de Chidamber e Kemerer (1994), o valor zero não indica coesão máxima` |
| C6 | 7 (398) | em Basili et al., cuja amostra é de 180 classes C++ de alunos [Basili et al. 1996] | "Basili et al." sem ano, com a citação repetida no fim. | `em Basili et al. (1996), cuja amostra é de 180 classes C++ de alunos` |
| R1 | Refs. (414) | Mittas, N.; Chatzigeorgiou, A. (2026) | Único caso sem "e" antes do último autor. | `Mittas, N. e Chatzigeorgiou, A. (2026)` |
| R2 | Refs. (440) | Galster, M. et al. (2026) | O artigo tem seis autores. As demais entradas com até seis autores listam todos (Wang tem seis), e "et al." só aparece com sete ou mais. | `Galster, M.; Mohsenimofidi, S.; Lulla, J. L.; Abubakar, M. A.; Treude, C. e Baltes, S. (2026)` |
| R3 | Refs. (424, 434, 468, 488) | *IEEE TSE* | Periódico abreviado, enquanto outros estão por extenso (*IEEE Transactions on Computers*). | `*IEEE Transactions on Software Engineering*` nas quatro entradas |
| R4 | Refs. (432, 448, 460) | *ACM TOSEM* | Idem. | `*ACM Transactions on Software Engineering and Methodology*` nas três entradas |
| R5 | Refs. (480) | *Proc. ACM Softw. Eng.* | Idem. | `*Proceedings of the ACM on Software Engineering*` |
| R6 | Refs. (412) | *MSR 2025*, 526-538. | O ano do evento aparece em dois formatos (*MSR 2025*, *SANER 2026*, *PROMISE 2026*, *NeurIPS 2020*, *NIPS 2017* e *ICLR 2023*, contra *MSR '26*, *TechDebt '18*, *ESEM '20*, *AIware '26* e *ICSE-NIER '24*). A entrada também não tem DOI. | `*MSR '25*, 526-538. doi:10.1109/MSR66628.2025.00086`; e, nas demais, `*SANER '26*`, `*PROMISE '26*`, `*NeurIPS '20*`, `*NIPS '17*` e `*ICLR '23*` |
| R7 | Refs. (418) | *How Claude remembers your project*; *Extend Claude with skills*; *Create custom subagents*. https://code.claude.com/docs/en. | Uma entrada reúne três documentos, e o endereço é genérico. | Três entradas: `Anthropic (2026a). *How Claude remembers your project*. https://code.claude.com/docs/en/memory. Acesso em 02/11/2026.`; `Anthropic (2026b). *Extend Claude with skills*. https://code.claude.com/docs/en/skills. Acesso em 02/11/2026.`; `Anthropic (2026c). *Create custom subagents*. https://code.claude.com/docs/en/sub-agents. Acesso em 02/11/2026.` No texto: `[Anthropic 2026a]` nas linhas 33 e 41, `[Anthropic 2026b]` na 42 e `[Anthropic 2026c]` na 44. |
| R8 | Refs. (426) | *IEEE Transactions on Computers*, C-25(12), 1226-1241. | Falta o DOI, presente nas demais entradas de periódico. | Acrescentar `doi:10.1109/TC.1976.1674590` |
| R9 | Refs. (428) | Language Models are Few-Shot Learners. *NeurIPS 2020*. | Faltam as páginas (de memória; o Crossref não indexa este evento; conferir no PDF). | `*NeurIPS '20*, 1877-1901.` |
| R10 | Refs. (432) | *ACM TOSEM*. doi:10.1145/3840295 | Faltam volume, número e páginas, já publicados segundo o Crossref. | `*ACM Transactions on Software Engineering and Methodology*, 35(11), 1-38. doi:10.1145/3840295` |
| R11 | Refs. (458) | *PROMISE 2026*. doi:10.1145/3803846.3807469 | Faltam as páginas. | `*PROMISE '26*, 91-100. doi:10.1145/3803846.3807469` |
| R12 | Refs. (444) | *software-patterns*, skill gof-patterns, commit 85e94a3. | Termos em inglês sem itálico. | `*software-patterns*, *skill* gof-patterns, *commit* 85e94a3.` |

Sem correção necessária: Liu et al. (2026), linha 460, não traz volume porque o Crossref ainda o registra sem volume, como artigo 3796507. Yao et al. (2023) não traz páginas porque o ICLR não as atribui.

### Preferências de estilo

| id | seção (linha) | trecho | observação | correção sugerida |
|---|---|---|---|---|
| RS1 | Refs. | doi:10.1016/j.jss.2026.112862 | O DOI como endereço é mais prático para o leitor, e as entradas com DOI terminam sem ponto, ao contrário das outras. | `https://doi.org/10.1016/j.jss.2026.112862`, em todas as entradas |
| RS2 | Refs. (426, 434) | A metrics suite for object oriented design | Títulos em caixa alta e baixa convivem com títulos em minúsculas. Seguem a fonte, então não é erro. | Manter como está na fonte, ou padronizar um dos dois formatos |

## 4. Português e uniformidade (Seções 1 a 7 e Resumo)

### Erros (corrigir)

| id | seção (linha) | trecho | problema | correção exata |
|---|---|---|---|---|
| E1 | Resumo (16) | mostrou, de N0 a N2, a mesma direção | N0 e N2 não são definidos no Resumo. | `mostrou, do nível sem orientação (N0) ao nível com arquivo e *skill* (N2), a mesma direção` |
| E2 | 1 (23-24) | capazes de gerar texto a partir de uma entrada chamada *prompt*, inclusive código-fonte [Hou et al. 2024], e baseados na arquitetura Transformer | Emenda mal encaixada: "e baseados" sobra na coordenação, e "inclusive código-fonte" parece referir-se ao *prompt*. | Toda a definição: `modelos baseados na arquitetura Transformer [Vaswani et al. 2017; Brown et al. 2020], treinados com grandes volumes de texto e capazes de gerar texto, inclusive código-fonte, a partir de uma entrada chamada *prompt* [Hou et al. 2024].` |
| E3 | 1 (25-26) | e é sobre esse ponto que este trabalho se concentra | Regência: concentrar-se **em**. | `e é nesse ponto que este trabalho se concentra` |
| E4 | 1 (41-42) | um conjunto de instruções sobre um assunto cuja descrição fica sempre | "cuja" liga a descrição a "assunto", e não à *skill*. | `um conjunto de instruções sobre um assunto; sua descrição fica sempre` |
| E5 | 1 (76) | apenas 0,92% tratam dessa atividade | Concordância com numeral menor que 2, sem especificador junto. | `apenas 0,92% trata dessa atividade` |
| E6 | 1 (90) | Os objetivos específicos são os seguintes. | A Seção 6 retoma as "perguntas de pesquisa apresentadas na Introdução", mas a Introdução não usa esse nome para P1 a P8. | `Os objetivos específicos, formulados como perguntas de pesquisa (P1 a P8), são os seguintes.` |
| E7 | 1 (91) | o uso do *pattern* pedido (P1) | O texto insiste que o pedido não menciona o *pattern*; a Seção 5.2 usa "esperado". | `o uso do *pattern* esperado (P1)` |
| E8 | 1 (93) | a saber, o design do resto do código, arquitetura, testes, banco de dados e comportamento (P6) | Artigo só no primeiro item; "resto"; *design* sem itálico. | `a saber, o *design* do restante do código, a arquitetura, os testes, o banco de dados e o comportamento (P6)` |
| E9 | 1 (94) | a extensão inclui ainda um controle negativo | "ainda" repetido em duas frases seguidas. | `a extensão inclui também um controle negativo` |
| E10 | 1 (94) | tratado junto com a P2 | Tom informal. | `tratado em conjunto com a P2` |
| E11 | Resumo (12); 1 (36) | em volta do modelo // em volta dele | Tom informal. A Figura 1 traz a mesma expressão. | `em torno do modelo`; `em torno dele`; na Figura 1, `o software em torno do modelo` |
| E12 | 2 (115) | **Encapsular o que varia.** A parte que muda de um caso para outro fica num objeto próprio | Rótulo em negrito; "num". | `O primeiro conceito é o encapsulamento do que varia: a parte que muda de um caso para outro fica em um objeto próprio` |
| E13 | 2 (120-121) | **Polimorfismo no lugar da condicional.** Em vez de perguntar qual é o caso, o código chama um método, e cada caso responde do seu jeito | Rótulo em negrito; tom informal. | `O segundo conceito é a substituição da condicional por polimorfismo: em vez de verificar qual é o caso, o código chama um método, e cada caso responde à sua maneira` |
| E14 | 2 (132) | **O princípio aberto/fechado.** O caso novo deve entrar numa classe nova, sem mudar o código que já funciona. | Rótulo em negrito; tom informal; "numa". | `O princípio aberto/fechado estabelece que um caso novo deve ser acrescentado em uma classe nova, sem alterar o código que já funciona.` |
| E15 | 2 (137) | **Acoplamento e coesão.** Uma boa estrutura tem módulos pouco dependentes uns dos outros (baixo acoplamento) e cujos elementos | Rótulo em negrito; coordenação sem paralelismo. | `Quanto ao acoplamento e à coesão, uma boa estrutura tem módulos que dependem pouco uns dos outros (baixo acoplamento) e cujos elementos` |
| E16 | 2 (143) | **As métricas.** Os dois estudos medem a estrutura com a ferramenta CK | Rótulo em negrito. | `Para medir essa estrutura, os dois estudos empregam a ferramenta CK` |
| E17 | 2 (123-124) | **acerto** é a variação inteira // **omissão** é a condicional // **malfeito** é a interface | Negrito dentro do parágrafo. Também nas linhas 144 a 146 (**CBO**, **LCOM**, **WMC**, **DIT**). | Retirar o negrito desses sete termos. |
| E18 | 2 (124) | é a interface que existe mas convive | Falta vírgula antes de "mas". | `é a interface que existe, mas convive` |
| E19 | 2 (110) | o interessado num aviso) e que vai ganhar casos novos | "num"; tom informal. | `o interessado em um aviso) e que receberá novos casos` |
| E20 | 2 (111) | cada caso novo obriga a mexer em todos esses lugares | Tom informal. | `cada caso novo obriga a alterar todos esses trechos` |
| E21 | 2 (112) | Os conceitos de orientação a objetos abaixo explicam | "abaixo" é de relatório. | `Os conceitos de orientação a objetos apresentados a seguir explicam` |
| E22 | 2 (117) | o Observer, quem é avisado; o Decorator, um comportamento a mais em volta de outro | Tom informal. | `o Observer, o conjunto dos interessados em um aviso; o Decorator, um comportamento adicional em torno de outro` |
| E23 | 2 (134-135) | É o critério que os textos dos cenários pressionam ao dizer que a variação vai crescer. | Uso impróprio de "pressionar"; "textos dos cenários" no lugar de "enunciados". | `É esse o critério que os enunciados dos cenários procuram acionar ao afirmar que a variação crescerá.` |
| E24 | 2 (139) | a LCOM mede só um aspecto da coesão | Sigla usada antes de ser apresentada. | `a LCOM (*Lack of Cohesion of Methods*), apresentada adiante, mede apenas um aspecto da coesão` |
| E25 | 2 (144-146) | **CBO** (com quantas outras classes uma classe se acopla) | As siglas não são expandidas. | `CBO (*Coupling Between Objects*, com quantas outras classes uma classe se acopla)`; `LCOM (no original, os pares...`, já expandida na linha 139; `WMC (*Weighted Methods per Class*, a complexidade somada...`; `DIT (*Depth of Inheritance Tree*, a profundidade de herança)` |
| E26 | 3 (152) | O resultado foi medido contra propriedades | Decalque de *measured against*. | `O resultado foi comparado com propriedades` |
| E27 | 3 (164) | desenvolvedores júniores | O plural de "júnior" é "juniores", sem acento. | `desenvolvedores juniores` |
| E28 | 4 (189) | as bancadas de teste | A linha 213 usa "bancada de testes". | `as bancadas de testes` |
| E29 | 4.1 (201) | o contrato da API | Sigla sem expansão na primeira ocorrência (no corpo). | `o contrato da interface de programação (*Application Programming Interface*, API)` |
| E30 | 4.1 (207) | por escolha deste desenho | Inserção mal encaixada. | `por decisão do desenho do estudo` |
| E31 | 4.1 (209); 6 (370) | não-inferioridade | Sem hífen, pelo Acordo Ortográfico e pelo VOLP. | `não inferioridade`, nas duas ocorrências |
| E32 | 4.2 (213) | O pré-registro do V5 foi congelado | "V5" não é associado ao estudo principal no texto. | `O pré-registro do estudo principal (V5) foi congelado` |
| E33 | 4.3 (233) | atingiram a regra de saída | Uma regra é atendida, e não atingida. | `atenderam à regra de saída` |
| E34 | 4.3 (233) | e a regra de análise fixada antes dos dados, no código e no registro de decisões de 09/10, antes do ensaio com os dados do V4, exige | Repetição deixada por emenda. | `e a regra de análise fixada no código e no registro de decisões de 09/10, antes do ensaio com os dados do V4, exige` |
| E35 | 4.3 (235) | segundo a regra direcional, cuja queda é pequena em números absolutos | "cuja" refere-se à regra, e não ao exagero. | `segundo a regra direcional, embora a queda seja pequena em números absolutos` |
| E36 | 4.3, Tabela 2 (248, 249) | LCOM médio do projeto | O texto usa "a LCOM" e "LCOM média". | `LCOM média do projeto`, nas duas linhas |
| E37 | 5.1 (260) | entrega prontos parte dos mecanismos medidos | Concordância com "parte". | `entrega pronta parte dos mecanismos medidos` |
| E38 | 5.1 (260) | quando o agente a abria passando uma pergunta como argumento, um modelo respondeu | Correlação de tempos, e dois sujeitos para o mesmo evento. | `quando a abriu passando uma pergunta como argumento, um modelo respondeu` |
| E39 | 5.2 (264) | em Java 21 com Javalin e os dados em um PostgreSQL 18 | Frase truncada. | `em Java 21 com Javalin, com os dados armazenados em PostgreSQL 18` |
| E40 | 5.2 (268) | As 52 notas esperadas conferiram | Uso coloquial de "conferir". | `As 52 notas coincidiram com as esperadas` |
| E41 | 5.2 (272) | e as 12 notas esperadas conferiram | Idem. | `e as 12 notas coincidiram com as esperadas` |
| E42 | 5.3, Tabela 3 (284) | design do resto do código | "resto"; sem itálico. | `*design* do restante do código` |
| E43 | 5.3 (311) | acerta ali as 5 execuções de cada nível | "ali" informal; numeral (o texto escreve até dez por extenso). | `acerta nesse caso as cinco execuções de cada nível` |
| E44 | 5.3 (311) | vai de 22 a 25 acertos em 25 na construção e de 11 a 25 na manutenção; o Sonnet 4.5, de 14 a 22 e de 5 a 20; e o Haiku 4.5, de 5 a 13 e de 3 a 13 | "de X a Y" parece intervalo; o texto usa "de X para Y" para mudanças. | `passa de 22 para 25 acertos em 25 na construção e de 11 para 25 na manutenção; o Sonnet 4.5, de 14 para 22 e de 5 para 20; e o Haiku 4.5, de 5 para 13 e de 3 para 13` |
| E45 | 5.3 (317) | quando dois pedidos chegam simultaneamente | Correlação de tempos com "permitiram" e "fosse". | `quando dois pedidos chegavam simultaneamente` |
| E46 | 5.3 (347) | em nenhuma das 10 do Haiku 4.5 | Numeral. | `em nenhuma das dez do Haiku 4.5` |
| E47 | 6 (370) | (saldo -5) | Hífen no lugar do sinal de menos. | `(saldo −5)` |
| E48 | 6 (376) | uma escreveu os arquivos, e o serviço não subiu | Jargão. | `uma escreveu os arquivos, e o serviço não chegou a iniciar` |
| E49 | 6 (382) | a *skill* que travava o agente | Tom informal. | `a *skill* que interrompia o trabalho do agente` |
| E50 | 7 (390) | Esse ganho, como registrado na Introdução, | "ganho" não tem referente na frase anterior; emenda mal encaixada. | `O efeito dos *patterns* sobre a facilidade de mudança, como registrado na Introdução,` |
| E51 | 7 (394) | e entrou a regra de que | Tom informal. | `e foi introduzida a regra de que` |
| E52 | 7 (394) | Isso mostra que a leitura se repete | Depois da emenda, "Isso" aponta para a severidade, e não para a concordância. | `Essa concordância mostra que a leitura se repete` |
| E53 | 7 (396) | com a mesma imagem. | "imagem" sem explicação. | `com a mesma imagem de contêiner.` |
| E54 | 7 (396) | o que derrubou os testes de integração | Tom informal. | `o que fez falhar os testes de integração` |

### Uniformidade de termos (corrigir)

| id | onde (linha) | situação | correção exata |
|---|---|---|---|
| U1 | Resumo (16); Seção 2 (109, 110, 112, 116, 140); Tabelas 3, 4 e 6 e suas legendas (278, 283, 296, 298, 332); títulos (3, 107, 254) | *pattern* e *patterns* têm 85 ocorrências em itálico e 16 sem itálico (1 delas na Seção 8, linha 403). A Seção 2 inteira está sem. | `*pattern*` e `*patterns*` em todas, inclusive em títulos e legendas |
| U2 | 3, 93, 107 e 266 (duas), 284, 376 (duas), 394 (duas) | *design* sozinho, como dimensão, está sempre sem itálico, e *design patterns* com itálico fica sem ele no título e no título da Seção 2. | `*design*` em todas |
| U3 | 15, 57, 96, 130, 154, 201 (duas) | "padrão" e *pattern* se alternam, embora a linha 55 declare "aqui chamados de *patterns*". A Figura 2 diz "pattern Strategy" e a legenda dela, "padrão Strategy". | `do *pattern* Strategy` (15); `O *pattern* Strategy` (57); `sobre o *pattern* Strategy` (96); `com o *pattern* Strategy` (130); `o *pattern* Singleton` (154); `pede o *pattern* Strategy` e `a aplicação do *pattern*` (201) |
| U4 | 93, 284 contra 102, 266, 272, 317, 376, 392, 394 | "resto" duas vezes e "restante" nas demais. | Corrigido em E8 e E42 |
| U5 | 110, 115, 132 | "num" e "numa" fora de citações, contra "em um" e "em uma" no restante. | Corrigido em E12, E14 e E19 |

Sem divergência: *skill* (45 de 45 em itálico), *harness* (8 de 8), *commit*, *prompt*, *framework*, *script*, *tokens*, *hash*, *checkout*, *enum*, *records*, *downloads* e *pull requests*. O termo usado é sempre "proprietário" (três ocorrências); "dono" não aparece.

### Preferências de estilo

| id | seção (linha) | trecho | observação | correção sugerida |
|---|---|---|---|---|
| S1 | Resumo (14-15) | levou o ponto mais complexo do padrão Strategy de 67% para 93% das execuções corretas | Construção truncada. | `elevou de 67% para 93% a proporção de execuções corretas no ponto mais complexo do *pattern* Strategy` |
| S2 | 1 (62); 6 (368) | onde ele não se justifica // onde ela não cabia | "onde" sem sentido de lugar. | `quando ele não se justifica`; `quando ela não era necessária` |
| S3 | 1 (81-82) | Para responder a essa pergunta, o objetivo geral é avaliar | O objetivo não responde; quem responde é o trabalho. | `Para responder a essa pergunta, definiu-se como objetivo geral avaliar` |
| S4 | 1 (100) | sobre o que esperar delas, e sobre o custo | Vírgula dispensável. | `sobre o que esperar delas e sobre o custo` |
| S5 | 2 (113) | e dão as medidas usadas nos dois estudos | Tom informal. | `e fornecem as medidas usadas nos dois estudos` |
| S6 | 2 (122); 3 (161) | as classes da correção // as classes aqui adotadas | "classes" confunde-se com as classes de orientação a objetos. | `as categorias da correção`; `as categorias aqui adotadas` |
| S7 | 3 (163-164) | experimento em cruzamento | Forma mais usada em português. | `experimento cruzado` |
| S8 | 3 (183) | Este trabalho faz essa medida | Verbo genérico. | `Este trabalho realiza essa medição` |
| S9 | 4.1 (201) | em que apenas a porcentagem varia e a aplicação do padrão constituiria exagero, denominado ponto do seguro | Aposto distante do nome. | `um controle negativo, o seguro por região, denominado ponto do seguro, em que apenas a porcentagem varia e a aplicação do *pattern* constituiria exagero` |
| S10 | 4.1 (205) | Participaram do estudo os 11 modelos Claude disponíveis | A frase seguinte exclui o Fable, que também estava disponível. | `Participaram do estudo 11 dos modelos Claude disponíveis` |
| S11 | 4.1 (209) | em que o que menos acerta | Cacofonia. | `em que aquele que menos acerta` |
| S12 | 4.2 (213) | O ensaio revelou um defeito real no Semgrep, em que projetos situados em uma subpasta não eram lidos, corrigido antes do congelamento. | "corrigido" distante de "defeito". | `O ensaio revelou, no Semgrep, um defeito real, corrigido antes do congelamento: projetos situados em uma subpasta não eram lidos.` |
| S13 | 4.3 (235); 6 (366) | hipótese de não repetir o comum | Rótulo pouco claro. | `hipótese de não repetição do que é comum` |
| S14 | 5.3 (278) | Execuções boas por dimensão | Tom informal. | `Execuções com nota boa por dimensão` |
| S15 | Tabelas 3 a 6; Figuras 5 e 6 | Construir / Manter | O texto usa "construção" e "manutenção". | `Construção` e `Manutenção` |
| S16 | 5.3 (290, 311) contra 16, 364, 390, 402 | Do N0 ao N2 | Duas formas. | Escolher uma; sugestão: `do N0 ao N2` |
| S17 | 5.3 (317) | nem o Haiku 4.5 nem o Sonnet 4.5 o tiveram em nenhuma das 75 execuções | Pronome distante. | `nem o Haiku 4.5 nem o Sonnet 4.5 receberam essa nota em nenhuma das 75 execuções` |
| S18 | 5.3 (347) | embora o acréscimo tenha permanecido único | Pouco claro. | `embora se tratasse de um único acréscimo` |
| S19 | 5.2 a 7 | *pattern* certo | "*pattern* certo" aparece oito vezes (290, 294, 296, 311, 315, 317, 368, 382); o registro pede "correto". Em 382, "aumento do *pattern* certo" também é impreciso. | `*pattern* correto`; na linha 382, `O aumento das execuções com o *pattern* correto` |
| S20 | 6 (364 a 380) | A primeira pergunta (P1) indaga como | Oito parágrafos com a mesma abertura lembram pergunta e resposta. | `Quanto ao uso do *pattern* (P1), ...`; `No que se refere ao exagero e à correção (P2), ...`, e assim por diante |
| S21 | 6 (372) | embora tenha modificado sua forma: a forma literal | Repetição. | `embora tenha modificado sua implementação: a forma literal` |
| S22 | 6 (376) | Nessas dimensões, pesam o modelo e o ponto de partida | Vem logo após "nessa dimensão", que se refere a outra coisa. | `Em arquitetura, testes, banco e comportamento, pesam o modelo e o ponto de partida` |
| S23 | 6 (380) | quase não têm nenhum desses mecanismos | Dupla negação. | `praticamente não apresentam esses mecanismos` |
| S24 | 1 a 7 | só | "só" aparece 15 vezes como advérbio, contra 21 de "apenas" e 3 de "somente". | Trocar por `apenas` nas ocorrências adverbiais; manter "um só" e "uma só" |
| S25 | 5.2, 5.3, 7 | a nota bom | Nome da nota sem destaque. | `a nota "bom"`, também em "com problemas" e "ruim" |
| S26 | Todo o texto | desenho | Três sentidos: *design* do código (207, 233), plano do estudo (189, 197) e etapa do processo (43, 203). Ao lado, "projeto do código" (173) e *design* (266). | Reservar `desenho` ao plano do estudo e ao critério pré-registrado e explicar o termo na primeira ocorrência |
| S27 | 5.2, 6, 7 | banco // base | Abreviações de "banco de dados" e "base de código". | Escrever por extenso na primeira ocorrência de cada seção |
| S28 | 4.1 (205), 6 (378) contra 5.3 (317) | se repetiu // repetiu-se | Próclise e ênclise alternadas. | Uniformizar em `repetiu-se` e `repetiram-se` |
| S29 | Tabelas | Fonte: elaboração própria | Figuras têm fonte, e tabelas não. | Acrescentar às tabelas, ou retirar das figuras |
| S30 | `.docx` | legendas e tabelas | Sem `keepNext`, a legenda pode separar-se da tabela ou da figura em quebra de página. | `keepNext` na legenda de tabela e no parágrafo da figura |
