# Referências para o artigo: trabalhos relacionados e fundamentação

*Levantamento de 02/11/2026, no Google Scholar, com o local de publicação conferido no Crossref, no DBLP ou na página do
próprio artigo. O ranking dos eventos é o CORE2023, visto no portal ICORE. O das revistas não foi conferido em uma tabela
oficial (Qualis ou JCR): elas estão aqui pela reputação na área, e vale você conferir na lista que o seu curso usa.*

**Como cada artigo foi lido:**

| artigo | o que foi lido |
|---|---|
| Ampatzoglou et al. 2026 | publicado, corpo inteiro |
| Chatlatanagulchai et al. 2026 | preprint v2 do arXiv, quase inteiro; confira os números na versão da TOSEM |
| Kim 2025 | publicado, corpo inteiro |
| Kim 2026 | publicado, corpo inteiro |

Preprints ficam de fora (decisão de 03/11/2026). A leitura da reserva está na seção 3.

## 1. Os quatro trabalhos relacionados

### Kim (2025), *Journal of Systems and Software*

**O que faz.** Avalia cinco LLMs de empresas diferentes (GPT-4o, Claude 3.5 Sonnet, Microsoft 365 Copilot, Gemini 1.5 e
Llama 3.1 405B) na aplicação de design patterns por refatoração, em dois programas Java sem pattern (257 e 154 linhas). São
cinco patterns no total (Factory Method, Strategy, Composite, Observer e Singleton), quatro por programa. Um pedido aberto
identifica patterns, mas essa etapa não dá nota; na que dá, o pedido nomeia o pattern e o local. O resultado é comparado
com propriedades de cada pattern escritas em lógica de predicados, em três métricas (PSR, CPC e PIQS). O Claude teve a maior
média de PIQS, 89,51, e o Gemini a menor, 71,04, com uma execução por célula e sem teste estatístico.

**O que o nosso faz de diferente.**
- Kim compara os modelos; o nosso compara, para o mesmo modelo, níveis de orientação persistente.
- Lá, a etapa que dá nota manda aplicar um pattern nomeado num local nomeado. Aqui, o pedido nunca fala em pattern, e o que
  se mede é se o agente decide usá-lo sozinho.
- Lá, a atividade é a refatoração pedida. Aqui, o estudo principal constrói do zero; a extensão constrói e mantém, com um
  pedido de mudança que não nomeia o pattern, num sistema também sem pattern, com um agente autônomo.
- O nosso tem um controle negativo para o exagero, e a extensão mede também arquitetura, testes, banco e comportamento.

**Onde Kim é mais forte, e vale reconhecer.** Usa modelos de cinco empresas, e o nosso só usa Claude. O critério é por
propriedade, publicado e com peso, e mostra qual parte do pattern falhou; a nossa classificação em acerto, alternativa,
malfeito e omissão não tem essa resolução. A especificação é mais formal, mas isso não a torna medida melhor: só 3 dos 10
programas rodaram, a execução não entra na nota, e algumas tabelas se contradizem, então não cite os números por pattern.

**Um uso útil.** Com o pattern e o local nomeados, o Strategy deu 100 nos cinco modelos; no nosso estudo principal, sem nomear,
o ponto difícil do Strategy parte de 67%. Isso sugere que o difícil é decidir, e não implementar, com a ressalva de que modelos,
programas e medidas são outros.

### Ampatzoglou et al. (2026), *Journal of Systems and Software*

**O que faz.** É um experimento controlado, em cruzamento (*crossover*), com 10 desenvolvedores júniores e 100 pontos de
dados. Eles refatoram tarefas de livro para aplicar três patterns GoF (Abstract Factory, Bridge e Template Method) e dois
princípios (SRP e OCP), com e sem a versão gratuita do ChatGPT. Dois autores dão a nota, de 0 a 10, em cinco etapas:
- **Identificar o problema:** quem trabalhou sem o LLM foi melhor (cerca de 1,1 ponto; p = 0,037).
- **Atributo de qualidade afetado:** quem trabalhou sem o LLM foi melhor só nas tarefas de pattern (cerca de 1,8 ponto; p = 0,048).
- **Escolher a prática:** sem diferença entre os grupos (p = 0,811).
- **Implementar:** quem usou o LLM foi melhor (cerca de 1,9 ponto; p = 0,003).
- **Identificar o mecanismo de extensão:** sem diferença entre os grupos.

**O que o nosso faz de diferente.**
- Lá, há uma pessoa conduzindo o LLM, e a variável é ter ou não ter o LLM. Aqui, o agente trabalha sozinho e a variável é
  a orientação dada a ele.
- Em vez de 10 pessoas, são centenas de execuções repetidas, com vários modelos.
- O nosso cobre também a construção do zero, e não só a refatoração.
- Lá, toda tarefa tem uma prática que cabe, e o formulário pergunta pelo problema. Aqui, há um cenário em que o pattern seria
  exagero, e ninguém pergunta nada.
- Lá, a qualidade é a nota dos avaliadores. Aqui, há também métricas de código. O artigo não traz pré-registro nem controle
  negativo.

**Como dialogam.** O grupo com o LLM foi pior em identificar o problema. Os autores dizem que isso confirma Liu et al. (2025):
sem pedido específico, LLMs não identificam sozinhos as oportunidades de refatoração em cenários complexos (seção 5.1). O nosso
mede esse ponto com um agente: sem ninguém dizer que há um pattern a aplicar, a orientação faz o agente percebê-lo? *(A ligação
com o nosso estudo é leitura minha. Eles medem pessoas respondendo a uma pergunta direta, e não um agente decidindo sozinho.)*

**Onde o artigo é fraco, e a banca sabe.** A amostra é pequena e não houve análise de poder; o humano guia o modelo.

### Chatlatanagulchai et al. (2026), *ACM Transactions on Software Engineering and Methodology*

**O que faz.** Analisa 2.303 arquivos de contexto de 1.925 repositórios, de Claude Code, Codex e Copilot. Mede tamanho,
legibilidade, manutenção e o tipo de instrução. Em uma amostra de 332 arquivos, os temas mais comuns são testes (75,9%), detalhes
de implementação (70,8%) e arquitetura (68,1%). Os menos comuns incluem segurança (14,8%) e desempenho (14,5%).

**O que o nosso faz de diferente.**
- Eles descrevem o que os desenvolvedores escrevem nesses arquivos, observando repositórios. O nosso manipula o arquivo em
  um experimento, com a mesma tarefa, e mede o efeito no código que o agente produz.
- O estudo deles não é um experimento controlado sobre a qualidade do código; o nosso trata justamente dessa lacuna.
- O nosso separa o arquivo de princípios, a *skill* e o processo, para ver quanto cada um contribui.

**Cuidado.** Eles trazem dois casos antes e depois sobre indentação (violações de 60,8% para 11,0% no repositório com Claude Code)
e dizem que isso não prova causa. Também citam estudos de presença e ausência do arquivo, sobre eficiência. Então não escreva que
ninguém testou o arquivo: a lacuna é sobre a decisão de projeto.

### Kim (2026), *Information and Software Technology*

**O que faz.** Compara três formas de organizar agentes (especializados por tarefa, por fase e generalistas de processo),
com um único LLM, em nove tarefas de engenharia de software e três domínios. Mede a validade dos artefatos. Nenhuma
configuração venceu em todas as tarefas (validade média de 0,56, com 9 execuções e um avaliador; só uma das tarefas é código), e
o autor sugere, sem variar o modelo, que a capacidade do LLM pesa mais que a estratégia de orquestração.

**O que o nosso faz de diferente.**
- Em Kim, "configuração" é orquestração: 9, 4 ou 1 agentes, definidos por papel, objetivo e história, sem conteúdo de projeto. O
  nosso varia o conteúdo de projeto: princípios, *skill* e processo (o N3 inclui um subagente revisor).
- Lá, há um LLM só. Aqui, são 11 modelos no estudo principal e 3 na extensão, todos Claude, o que permite ver o efeito por modelo.
- Lá, mede-se a validade de artefatos, a maioria documentos e UML, sem patterns. Aqui, mede-se a decisão de projeto, com o exagero
  como risco oposto.

**Como dialogam.** Pouco. Na extensão, o modelo pesa mais que a orientação nas outras dimensões, o que é compatível com a hipótese que
Kim levanta sem testar. *(Este artigo é o mais distante dos quatro; rebaixá-lo é uma decisão em aberto.)*

## 2. Rascunho da seção

Os valores entre colchetes dependem das revisões que ainda estão rodando.

> **Trabalhos Relacionados**
>
> A capacidade de LLMs aplicarem design patterns foi avaliada por Kim (2025), que comparou cinco LLMs na refatoração de dois
> sistemas Java, medindo a implementação contra propriedades dos patterns especificadas em lógica de predicados. Neste
> trabalho, o foco não é comparar modelos, e sim o efeito de orientações persistentes em um mesmo modelo. O pedido feito ao
> agente também não menciona patterns, de modo que se mede a decisão de usá-los, e não apenas a qualidade da implementação
> quando o pattern é solicitado.
>
> Ampatzoglou et al. (2026) investigaram a refatoração para patterns e princípios de projeto em um experimento em cruzamento
> com dez desenvolvedores júniores. O uso do LLM aumentou a correção da implementação, enquanto os participantes sem o LLM foram
> superiores na identificação do problema; na escolha da prática, não houve diferença. O presente trabalho retira o humano do
> ciclo: um agente autônomo executa a tarefa sozinho, e avalia-se se orientações de projeto o levam a reconhecer a necessidade do
> pattern, tanto na construção quanto na manutenção.
>
> Os arquivos de contexto, como o `CLAUDE.md`, foram caracterizados por Chatlatanagulchai et al. (2026), em 2.303 arquivos
> de 1.925 repositórios. O estudo mostra o que os desenvolvedores escrevem nesses arquivos, mas não mede, em experimento
> controlado, o efeito deles sobre a qualidade do código. Arabat e Sayagh (2026) compararam 148 projetos antes e depois da criação
> do arquivo, com resultado misto na aceitação de *pull requests* e sem medir o projeto do código. Jiang e Nam (2026) apontam o
> efeito de regras persistentes como questão aberta. Este trabalho aborda essa questão ao manipular o arquivo e aplicar a mesma
> tarefa em cada nível de orientação.
>
> Kim (2026) comparou três estratégias de organização de agentes e não encontrou uma configuração superior em todas as
> tarefas, sugerindo, sem variar o modelo, que a capacidade do LLM influencia mais a qualidade dos artefatos. Aqui, a variável
> não é a organização dos agentes, mas o conteúdo de projeto das orientações, avaliado em vários modelos de uma mesma família.
>
> Em conjunto, este trabalho se diferencia por medir o efeito de orientações persistentes sobre a decisão de aplicar um
> pattern não nomeado no pedido, com um controle negativo para o exagero, com pré-registro no estudo principal (220
> execuções de 11 modelos) e com uma extensão a cinco patterns, na construção e na manutenção ([450] execuções).

**Antes de usar:**
- A frase final compara com o conjunto dos quatro. Pré-registro e controle negativo não aparecem em nenhum deles (nos dois de Kim,
  conferido na leitura).
- Redija a lacuna assim: nenhum texto publicado lido mede se um arquivo de contexto muda o pattern que o agente escolhe sozinho, nem o
  caso do exagero. Isso é o que foi lido, e não prova de que ninguém mediu.
- Galster et al. (2026, publicado no AIware) definem *agent harness* como as camadas de software em volta do modelo (p. 11), e
  Galster, Kim 2025 e Zisa não usam orientação persistente sobre patterns. Isso ajuda a redigir a lacuna.

## 3. Reserva para trabalhos relacionados

**Kjellberg e Zisa podem substituir um dos quatro principais, se você decidir rebaixar Kim 2026.** Arabat e Jiang entram bem como
apoio à lacuna, e já estão no rascunho.

| trabalho | local | ranking | situação | o que serve, e o limite |
|---|---|---|---|---|
| Arabat e Sayagh 2026 | MSR | trilha curta | lido inteiro | efeito do arquivo, antes e depois, em 148 projetos: 27,7% sobem e 26,35% caem na aceitação; observacional, sem medir projeto do código |
| Jiang e Nam 2026 | MSR | CORE A | lido quase inteiro | 401 repositórios; o efeito das regras é "questão aberta"; ferramenta Cursor, maioria TypeScript |
| Horikawa et al. 2026 | TOSEM | revista | lido em parte | agentes refatoram sobretudo no nível baixo e quase não reduzem cheiros de projeto; Claude Code é só 0,6% dos commits |
| Cao et al. 2024, JavaBench | ASE | CORE A* | lido; local não aparece no texto | OO em Java; zero projetos completos só na métrica mais dura; modelos de 2023; o artigo traz três valores para o mesmo Pass@5 |
| Cordeiro et al. 2026 | TOSEM | revista | preprint v1 lido | um único modelo pequeno; pior em cheiros de projeto; a conta só considera código que passou nos testes |
| Pehlivan et al. 2025 | ASE | CORE A* | lido; local não aparece no texto | detecção de violações em trechos sintéticos; modelos mal em DIP; relação distante |
| Kjellberg, Fotrousi e Staron 2026 | PROMISE (aceito; doi 10.1145/3803846.3807469) | ranking não conferido | lido inteiro | 13 modelos de 6 famílias, 164 tarefas, sem réplicas; Singleton sempre nomeado, orientação no *prompt*, sem agente, controle negativo nem manutenção; serve de contraste, e declara em aberto quando aplicar o pattern |
| Zisa, Passaro e Soldani 2026 | SANER | CORE A | lido inteiro | pattern nomeado, 2 modelos, 450 execuções, sem orientação persistente nem controle negativo; o *prompt* difere entre modelos (p. 224) |
| Pandey et al. 2025 | Empirical Software Engineering | revista | lido | reconhecimento de patterns criacionais com vetores e k-NN, sem geração nem agente; só uma frase de contraste, sem citar números, porque o artigo se contradiz |
| Della Porta et al. 2026 | IEEE TSE | revista | lido; versão aceita, ainda sem edição final | qualidade do *prompt*; só contraste, nada sobre orientação persistente |

### Ficaram de fora

Preprints e *workshops* não entram (decisão de 03/11/2026): Gloaguen et al., Khatri 2026, Lulla et al. 2026, Machado et al. 2025,
SkillsBench, SWE-Skills-Bench e os três lidos em 03/11 (*Precedent or Intent?*, *Agent Skills Can Be Harmful* e *Can LLMs Produce Better
Object-Oriented Designs?*). Nenhuma afirmação do artigo se apoia neles. Cordeiro et al., acima, foi lido como preprint, mas saiu na TOSEM:
confira os números na versão publicada antes de usar. O mesmo vale para Chatlatanagulchai et al.

## 4. Fundamentação teórica

A fundamentação, com o veredito de cada referência, está em `fundamentacao-teorica.md`.

## 5. Referências conferidas

Os dados abaixo vieram do Crossref, salvo indicação. O formato final fica para quando você aplicar o modelo da SBC.

**Principais**
- Ampatzoglou, A.; Arvanitou, E.-M.; Almpanopoulos, S.; Mittas, N.; Chatzigeorgiou, A. (2026). AI-assisted code refactoring: Where can it be helpful and where do humans outperform it? *Journal of Systems and Software*, 238, 112862. doi:10.1016/j.jss.2026.112862
- Chatlatanagulchai, W. et al. (2026). Agent READMEs: An Empirical Study of Context Files for Agentic Coding. *ACM TOSEM*. doi:10.1145/3840295 *(volume e páginas: conferir)*
- Kim, D.-K. (2025). Comparative analysis of design pattern implementation validity in LLM-based code refactoring. *Journal of Systems and Software*, 230, 112519. doi:10.1016/j.jss.2025.112519
- Kim, D.-K. (2026). Artifact validity under varying agent configurations in LLM-assisted software development: A comparative analysis. *Information and Software Technology*, 192, 108022. doi:10.1016/j.infsof.2026.108022

**Citado por Ampatzoglou et al. (2026); não lido nem conferido**
- Liu, B.; Jiang, Y.; Zhang, Y. et al. (2025). Exploring the potential of general purpose LLMs in automated software refactoring: an empirical study. *Automated Software Engineering*, 32, 26.

**Reserva**
- Arabat, A.; Sayagh, M. (2026). Toward Instructions-as-Code: Understanding the Impact of Instruction Files on Agentic Pull Requests. *MSR '26*, 969-973. doi:10.1145/3793302.3793601
- Cao, J. et al. (2024). JavaBench: A Benchmark of Object-Oriented Code Generation for Evaluating Large Language Models. *ASE '24*, 870-882. doi:10.1145/3691620.3695470
- Cordeiro, J.; Noei, S.; Zou, Y. (2026). An Empirical Study on the Code Refactoring Capability of Large Language Models. *ACM TOSEM*. doi:10.1145/3801158
- Della Porta, A. et al. (2026). Understanding Prompt Quality and Its Relation to LLM-based Code Generation. *IEEE TSE*, 1-20. doi:10.1109/tse.2026.3719766
- Horikawa, K. et al. (2026). Agentic Refactoring: An Empirical Study of AI Coding Agents. *ACM TOSEM*. doi:10.1145/3859242
- Jiang, S.; Nam, D. (2026). Beyond the Prompt: An Empirical Study of Cursor Rules. *MSR '26*, 397-409. doi:10.1145/3793302.3793367
- Pandey, S. K. et al. (2025). Design pattern recognition: a study of large language models. *Empirical Software Engineering*, 30(3), 69. doi:10.1007/s10664-025-10625-1
- Pehlivan, F. et al. (2025). Are We SOLID Yet? An Empirical Study on Prompting LLMs to Detect Design Principle Violations. *ASE 2025*, 3958-3962. doi:10.1109/ase63991.2025.00350
- Zisa, B.; Passaro, L.; Soldani, J. (2026). Design Pattern-Based Code Refactoring with LLMs. *SANER 2026*, 222-227. doi:10.1109/saner67736.2026.00033

**Fundamentação**
- Basili, V. R.; Briand, L. C.; Melo, W. L. (1996). A validation of object-oriented design metrics as quality indicators. *IEEE TSE*, 22(10), 751-761. doi:10.1109/32.544352
- Brown, T. B. et al. (2020). Language Models are Few-Shot Learners. *NeurIPS 2020* (DBLP; páginas: conferir).
- Chidamber, S. R.; Kemerer, C. F. (1994). A metrics suite for object oriented design. *IEEE TSE*, 20(6), 476-493. doi:10.1109/32.295895
- Ernst, N. A.; Baldassarre, M. T. (2023). Registered reports in software engineering. *Empirical Software Engineering*, 28(2), 55. doi:10.1007/s10664-022-10277-5
- Fan, A. et al. (2023). Large Language Models for Software Engineering: Survey and Open Problems. *ICSE-FoSE*, 31-53. doi:10.1109/icse-fose59343.2023.00008
- Galster, M. et al. (2026). Configuring Agentic AI Coding Tools: An Exploratory Study. *AIware '26*, 11-20. doi:10.1145/3805760.3814887 *(conferência da ACM SIGSOFT, ainda nova; não conferi ranking)*
- Hou, X. et al. (2024). Large Language Models for Software Engineering: A Systematic Literature Review. *ACM TOSEM*, 33(8), art. 220, 79 p. doi:10.1145/3695988
- Jimenez, C. E. et al. (2024). SWE-bench: Can Language Models Resolve Real-World GitHub Issues? *ICLR 2024* (página da Universidade de Princeton e buscadores; conferir no DBLP).
- Liu, J. et al. (2026). Large Language Model-Based Agents for Software Engineering: A Survey. *ACM TOSEM*. doi:10.1145/3796507
- Ouyang, S. et al. (2025). An Empirical Study of the Non-Determinism of ChatGPT in Code Generation. *ACM TOSEM*, 34(2), 1-28. doi:10.1145/3697010
- Prechelt, L. et al. (2001). A controlled experiment in maintenance: comparing design patterns to simpler solutions. *IEEE TSE*, 27(12), 1134-1144. doi:10.1109/32.988711
- Sallou, J.; Durieux, T.; Panichella, A. (2024). Breaking the Silence: the Threats of Using LLMs in Software Engineering. *ICSE-NIER '24*, 102-106. doi:10.1145/3639476.3639764
- Stevens, W. P.; Myers, G. J.; Constantine, L. L. (1974). Structured design. *IBM Systems Journal*, 13(2), 115-139. doi:10.1147/sj.132.0115
- Vaswani, A. et al. (2017). Attention is All you Need. *NIPS 2017*, 5998-6008 (DBLP).
- Wang, L. et al. (2024). A survey on large language model based autonomous agents. *Frontiers of Computer Science*, 18(6), 186345. doi:10.1007/s11704-024-40231-1
- Wohlin, C. et al. (2024). *Experimentation in Software Engineering*. Springer. doi:10.1007/978-3-662-69306-3
- Xi, Z. et al. (2025). The rise and potential of large language model based agents: a survey. *Science China Information Sciences*, 68(2), 121101. doi:10.1007/s11432-024-4222-0
- Yang, J. et al. (2024). SWE-agent: Agent-Computer Interfaces Enable Automated Software Engineering. *NeurIPS 37*, 50528-50652. doi:10.52202/079017-1601
- Yao, S. et al. (2023). ReAct: Synergizing Reasoning and Acting in Language Models. *ICLR 2023* (DBLP).
- Zhang, C.; Budgen, D. (2012). What Do We Know about the Effectiveness of Software Design Patterns? *IEEE TSE*, 38(5), 1213-1231. doi:10.1109/tse.2011.79

**Livros: conferir ano, edição e editora no exemplar que você citar**
- Gamma, E.; Helm, R.; Johnson, R.; Vlissides, J. *Design Patterns: Elements of Reusable Object-Oriented Software*. Addison-Wesley.
- Fowler, M. *Refactoring: Improving the Design of Existing Code*. 2. ed. Addison-Wesley (registro de 2018, direitos de 2019: use o que constar no seu exemplar).
- Kerievsky, J. (2004). *Refactoring to Patterns*. Addison-Wesley.
- Martin, R. C. (2003). *Agile Software Development: Principles, Patterns, and Practices*. Prentice Hall.
- Meyer, B. (1997). *Object-Oriented Software Construction*. 2. ed. Prentice Hall.

**Sem artigo revisado por pares:** a ferramenta CK (Aniche), a documentação do Claude Code e a *skill* pública devem ser
citadas como software ou página da web.
