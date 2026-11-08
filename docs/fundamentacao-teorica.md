# Referências para a Fundamentação Teórica

*Levantamento de 02/11/2026, revisado depois da leitura dos textos, e de novo em 03/11/2026, com o acesso pela CAPES.
Preprints ficam de fora; quando um artigo publicado foi lido na versão do arXiv, isso está indicado. Os trabalhos relacionados estão em
`referencias-trabalhos-relacionados.md`. As fichas de leitura de cada texto, com seção e página, estão fora do repositório,
na pasta de notas desta sessão (`leitura/vereditos/`). Peça para eu trazê-las para `docs/` se quiser guardá-las.*

**Como ler cada entrada.** O veredito diz se a referência sustenta o uso proposto: **serve**, **serve em parte** ou **não
lida**. "Lido inteiro" é o corpo do texto, sem apêndices e referências. Vários textos são a versão do arXiv: confira a página
e a redação na versão publicada antes de citar. "Como usar" é sugestão minha, não afirmação dos autores.

**Ranking.** Revistas: pela reputação na área, sem consulta ao Qualis ou JCR. Eventos com ranking conferido no CORE2023:
ASE (A*), MSR (A), SANER (A), EASE (A). Os demais eventos não foram conferidos no CORE; NeurIPS, ICLR e ICSE estão aqui pela
reputação conhecida.

## O que a leitura mudou

**Afirmações que estavam erradas ou fortes demais, já corrigidas abaixo:**
- **Ouyang et al. não justifica as cinco rodadas.** Eles usam cinco chamadas, mas não testam se bastam nem recomendam esse
  número. Veja "Por que cinco rodadas" na seção 8.
- **Ahmed et al. não dá aval a um verificador único.** Pede vários modelos e substitui só um avaliador humano entre vários. Veja
  a seção 8.
- **Martin não usa a sigla SOLID.** O livro traz os cinco princípios com siglas separadas.
- **"Aberto para extensão, fechado para modificação" é de Martin, não de Meyer.**
- **Jia e Harman não sustenta mutantes escritos à mão nem suíte de aceitação.**
- **Brown et al. não explica por que o `CLAUDE.md` funciona.** Era um salto meu.
- **Mohsenimofidi et al.:** 466 é o número de repositórios com algum arquivo de contexto; foram analisados 155 `AGENTS.md`.
- **Chatlatanagulchai et al.:** os percentuais vêm de uma amostra de 332 arquivos, não dos 2.303.

**O que a leitura trouxe de novo:**
- O controle negativo agora tem base escrita nos próprios livros (seção 5).
- Três levantamentos mostram que design de software é pouco estudado (seção 3).
- A frase de Fan et al. liga LLMs a design patterns, como agenda de pesquisa (seção 3).

**Três pontos que a banca pode cobrar:**
1. **Modelo só da Anthropic.** Todos os modelos e o verificador são da Anthropic, por restrição de custo. Sallou et
   al. pedem comparar com um modelo aberto. Já está declarado como limitação no rascunho para o professor; repita no artigo final.
2. **Vazamento.** Patterns clássicos estão no treino de qualquer modelo, então "decidir sozinho" pode ser memória, e não
   raciocínio. Sallou et al. trata dessa ameaça, e o controle negativo ajuda a responder.
3. **Gamma não está só do lado da economia.** O livro também recomenda antecipar mudanças (p. 23) e diz para trocar muitos
   condicionais por Strategy (pp. 316 e 318). O cenário de controle negativo precisa de poucas variantes e estáveis, senão o
   próprio Gamma manda usar o pattern.

## Dois avisos antes de escrever

**1. "Harness" pode ser cobrado pela banca.** Galster et al. (2026, versão publicada, p. 11) definem *agent harness* como as camadas de
software em volta do modelo, que conduzem o ciclo do agente; a definição deles se apoia em blogs. O artigo segue essa definição, e a
variável estudada é a **configuração do *harness***: o `CLAUDE.md`, a *skill* e o processo (decisão de 03/11/2026). O N0 é o Claude Code sem
configuração. O pré-registro do V5, congelado, chama essa configuração de *harness*, e o artigo avisa isso.

**2. Pré-registro não é *registered report*.** Ernst e Baldassarre (2023, p. 4 do preprint) separam os dois. O pré-registro é o
protocolo anunciado antes do estudo, até em um blog ou no OSF. O *registered report* acrescenta a revisão por pares em duas etapas
e o aceite em princípio da revista. O congelamento do V5 com *hash* cabe como pré-registro, desde que seja verificável
publicamente; um *hash* só local é mais fraco.

## 1. Modelos de IA

**Vaswani et al. (2017), *Attention is All you Need*, NIPS.** Serve. Lido inteiro.
- **O que traz:** o *Transformer*, baseado só em atenção, testado em tradução e análise sintática.
- **Como usar:** uma frase sobre a arquitetura.
- **Cuidado:** o artigo não fala de LLMs. A ligação com eles está em Brown et al. (seção 1) e Fan et al. (seção II-A).

**Ouyang et al. (2022), InstructGPT.** Serve em parte. Corpo lido, na versão do arXiv; o local (NeurIPS 2022) é de memória, então confira.
Autor diferente do Ouyang de 2025.
- **O que traz:** o ajuste por instruções melhora o seguimento de instruções, no GPT-3 de 2022.
- **Cuidado:** não diz nada sobre arquivo persistente, agente, princípios gerais, Claude ou modelos atuais.

**Brown et al. (2020), *Language Models are Few-Shot Learners*, NeurIPS.** Serve em parte. Lido em parte: sem os resultados
detalhados, os impactos e os apêndices.
- **O que traz:** o GPT-3 faz tarefas novas especificadas só por texto no *prompt*, sem atualizar pesos. O ganho cresce com a
  escala. Instrução sozinha é o caso mais fraco (TriviaQA: 64,3% sem exemplo, 71,2% com vários).
- **Como usar:** aprendizado em contexto, isto é, texto no *prompt* condiciona o modelo.
- **Cuidado:** é modelo de 2020, só pré-treinado, fora de programação. O próprio artigo não sabe se o modelo aprende ou só
  reconhece a tarefa (seção 5). Para o ajuste por instruções, veja Ouyang et al. (2022), abaixo.

**Hou et al. (2024), *TOSEM*.** Serve. Lido em parte: sem metodologia detalhada, dados e tarefas específicas.
- **O que traz:** revisão de 395 artigos, de 2017 a janeiro de 2024. Design de software é 0,92% dos estudos.
- **Como usar:** panorama da área e **lacuna**: design é pouco estudado.
- **Cuidado:** para antes dos agentes de linha de comando.

**Fan et al. (2023), *ICSE-FoSE*.** Serve. Lido em parte: sem a parte de testes e várias subseções.
- **O que traz:** alucinação como desafio central; é preciso eliminar soluções incorretas de forma confiável; técnicas híbridas.
  Pede inferência estatística por causa do não determinismo. Sugere, como pesquisa futura, usar LLMs para refatorar para design
  patterns (seção VI-E.2).
- **Como usar:** justificar a avaliação por testes e por análise estatística, e motivar o tema.
- **Cuidado:** a frase sobre patterns é agenda, não resultado. Avisa que testes gerados pelo próprio LLM podem embutir o erro.

## 2. Agentes de IA

**Xi et al. (2025), *Science China Information Sciences*.** Serve. Lido em parte; lido o preprint de 2023.
- **O que traz:** a definição mais citável: entidades que percebem o ambiente, decidem e agem. Quatro propriedades: autonomia,
  reatividade, pró-atividade e habilidade social (seção 2.3).
- **Como usar:** definição de agente.

**Wang et al. (2024), *Frontiers of Computer Science*.** Serve em parte. Lido em parte: sem as aplicações em ciências.
- **O que traz:** quatro módulos: perfil, memória, planejamento e ação. O perfil é texto escrito à mão e colocado no *prompt*.
- **Como usar:** como o agente é construído, e um paralelo com o `CLAUDE.md`.
- **Cuidado:** não serve para definir agente: a definição é uma epígrafe de Franklin e Graesser. O texto lido mostra 2025; confira ano
  e volume.

**Yao et al. (2023), *ReAct*, ICLR.** Serve em parte. Corpo lido inteiro.
- **O que traz:** o ciclo pensar, agir e observar.
- **Como usar:** a origem do ciclo.
- **Cuidado:** o artigo não trata de programação. Agente de programação é extensão: apoie em Fan et al. (seção VI, reparo) e Liu et
  al. (seção 5.1.1).

## 3. Agentes de programação

**Liu et al. (2026), *TOSEM*.** Serve. Lido em parte: sem a análise por tarefa.
- **O que traz:** 124 artigos. O critério do agente é o laço iterativo com retorno do ambiente (seção 3.1). A seção 7.1 separa LLM
  isolado e agente. Faltam agentes para design (seção 6).
- **Como usar:** a diferença entre LLM e agente; é a melhor fonte para isso.
- **Cuidado:** admite que não há fronteira nítida. A coleta termina em 2024: não cobre Claude Code nem arquivos de contexto.

**Yang et al. (2024), *SWE-agent*, NeurIPS.** Serve em parte. Corpo lido inteiro, sem as trajetórias do apêndice.
- **O que traz:** com o modelo fixo, trocar a interface entre o agente e o computador muda o resultado (GPT-4 Turbo: 18,0% contra
  11,0% só com o terminal). Seis execuções variaram pouco na média (17,3% a 18,7%), mas muito por tarefa (apêndice B.5).
- **Como usar:** a camada em volta do modelo muda o resultado, e a repetição de execuções.
- **Cuidado:** não testa orientação persistente. O que muda são ferramentas e retorno. O mais parecido, a demonstração no *prompt*,
  pesa pouco (16,3 contra 18,0).

**Jimenez et al. (2024), *SWE-bench*, ICLR.** Serve em parte. Corpo lido inteiro.
- **O que traz:** 2.294 tarefas de 12 repositórios Python. Vale "resolvido" se os testes passam. Os autores dizem que só testes não
  bastam para medir qualidade (seção 7).
- **Como usar:** motivar a medição de arquitetura e métricas além dos testes.
- **Cuidado:** avalia modelos com recuperação de contexto, não agentes. Agentes aparecem só como trabalho futuro.

**Documentação do Claude Code: *How Claude remembers your project*, *Extend Claude with skills* e *Create custom
subagents*.** Páginas lidas em parte. É fonte técnica, não artigo.
- **Como usar:** descrever a ferramenta usada no experimento.

## 4. Harness, arquivos de contexto e skills

**Nenhum dos oito textos sobre modelos e agentes trata de arquivo de instrução persistente.** Essa base vem só desta seção.

**Galster et al. (2026), *AIware*.** Serve. Versão publicada lida. A melhor fonte desta seção.
- **O que traz:** define *agent harness* (p. 11). Oito mecanismos de configuração em 2.853 repositórios; arquivos de contexto dominam, e
  poucos usam *skills* (5,5%) e subagentes (4,6%). Dizem que faltam estudos controlados sobre se uma configuração mais rica melhora o
  resultado.
- **Como usar:** definir *harness* e configuração, e apoiar a comparação N1 contra N2.
- **Cuidado:** a AIware é nova, e não conferi o ranking dela. A expressão *harness engineering* está só na versão estendida do arXiv, então
  não a atribua ao artigo publicado. Na primeira versão do arXiv, os números são outros.

**Chatlatanagulchai et al. (2026), *TOSEM*.** Serve. Preprint v2 lido quase inteiro.
- **O que traz:** 2.303 arquivos de contexto de 1.925 repositórios. Em uma amostra de 332, arquitetura aparece em 68,1%; segurança e
  desempenho aparecem pouco.
- **Como usar:** explicar o que são esses arquivos. Também está nos trabalhos relacionados, com outro papel.
- **Cuidado:** não use o 68,1% como prova de que se pede pattern: a categoria não separa patterns.

**Mohsenimofidi et al. (2026), *MSR*.** Serve. Lido inteiro. Artigo de 5 páginas.
- **O que traz:** 466 de 10.000 repositórios têm algum arquivo de contexto; analisam 155 `AGENTS.md`, sem estrutura estabelecida.
  Define engenharia de contexto (p. 1) e separa-a de engenharia de *prompt*.
- **Como usar:** definir engenharia de contexto e mostrar que a prática é recente. Eles pedem estudos sobre como o arquivo muda o
  comportamento do agente.
- **Cuidado:** a definição é emprestada de um preprint (Mei et al.) e de um blog.

**Jiang e Nam (2026), *MSR*.** Serve em parte. Lido quase inteiro.
- **O que traz:** 401 repositórios com regras do Cursor. Cerca de 65% têm diretrizes gerais de projeto. O efeito das regras é "uma
  questão aberta" (seção 6).
- **Como usar:** apoio direto à lacuna e ao conteúdo do N1.
- **Cuidado:** outra ferramenta; maioria TypeScript, só 9 repositórios Java.

**Documentação oficial do Claude Code.** Fonte técnica.
- **`CLAUDE.md`:** é carregado em toda sessão e tratado como contexto, não como regra imposta.
- **Skill:** a descrição fica sempre no contexto; o corpo só é carregado quando ela é usada.
- **Como usar:** esse funcionamento ajuda a explicar por que a *skill* pública não foi aberta espontaneamente no V5, enquanto
  o `CLAUDE.md` sempre chega ao agente. Essa ligação é hipótese de vocês, não afirmação da documentação.

## 5. Design patterns e o exagero

**Gamma et al., *Design Patterns*, Addison-Wesley.** Serve. Lidos os trechos pertinentes.
- **O que traz:** o catálogo, com intenção, aplicabilidade e consequências dos cinco patterns: Strategy (p. 315), Observer (p. 293),
  Factory Method (p. 107), Decorator (p. 175) e Template Method (p. 325). Programar para uma interface (p. 18). Preferir composição a
  herança (p. 20).
- **Como usar:** os patterns, e **a base direta do controle negativo**: a p. 31 diz que patterns não devem ser aplicados
  indiscriminadamente, pois a flexibilidade custa indireção. Use a seção "Consequences" para dizer o que é exagero em cada um.
- **Cuidado:** veja o terceiro ponto no início deste documento. A impressão lida traz direitos de 1995 e registro de 1994.

**Fowler, *Refactoring*, 2ª edição, Addison-Wesley.** Serve. Lidos os trechos pertinentes. **As páginas precisam ser conferidas no
exemplar**, pois esta cópia não as traz.
- **O que traz:** "Generalidade Especulativa" (cap. 3) e a seção sobre Yagni (cap. 2): flexibilidade antecipada tem custo. Trocar
  condicional por polimorfismo (p. 272), com o aviso de que o autor não troca todo condicional. Regra de três (cap. 2).
- **Como usar:** o exagero, com Generalidade Especulativa e Yagni.
- **Cuidado:** a regra de três é sobre duplicação, não sobre quando usar pattern. Usá-la para isso é extensão de vocês.

**Martin, *Agile Software Development: Principles, Patterns, and Practices*, Prentice Hall, 2003.** Serve em parte. Lidos os
trechos pertinentes.
- **O que traz:** SRP, OCP, LSP, DIP e ISP (caps. 8 a 12). OCP com abstração, e a solução rotulada como Strategy (pp. 99 e 101).
  **Complexidade desnecessária** (p. 89). Princípios se aplicam para remover cheiros, não por si (p. 86). Resistir à abstração prematura
  é tão importante quanto abstrair (p. 109).
- **Como usar:** o OCP e, sobretudo, seu limite: é a melhor base escrita do controle negativo, junto com Gamma p. 31.
- **Cuidado:** **a sigla SOLID não aparece no livro.** Se o artigo a usar, precisa de outra fonte: `[CITAR: origem da sigla SOLID]`.

**Kerievsky, *Refactoring to Patterns* (2004), Addison-Wesley.** **Não lido.** O PDF enviado é um rascunho de 2002, não citável.
- **O que o rascunho indica:** define sobre-engenharia e dá dois exemplos dele mesmo: Strategy onde bastava um condicional, Decorator
  onde bastava uma classe de dez linhas. Chega-se ao pattern refatorando.
- **Como usar:** base da pesquisa de manutenção e do exagero, depois de conferir no livro de 2004.
- **Cuidado:** ele afirma que a subengenharia é mais comum que a sobre-engenharia. A banca pode usar isso.

**Zhang e Budgen (2012), *IEEE TSE*.** Serve. Corpo lido inteiro. É um estudo de mapeamento.
- **O que traz:** de 611 artigos sobre patterns, ficam 10 artigos com 11 estudos experimentais, mais 7 relatos de experiência. A evidência é
  insuficiente. O apoio à manutenção é qualitativo e condicionado à documentação (p. 1229). Quase todos os experimentos mandam modificar
  sistemas prontos; o menos conhecido é o desenvolvimento novo (p. 1226).
- **Como usar:** base empírica do controle negativo: risco de solução complicada quando o pattern é usado sem necessidade (p. 1227). Pede
  estudos por pattern e do desenvolvimento à manutenção (p. 1229).
- **Cuidado:** diz que não há conhecimento suficiente para julgar quando usar cada pattern (p. 1229), o que pesa contra "os patterns têm um
  gabarito". O Strategy tem um só estudo, com alunos (Tabela 5). São humanos, com busca até 2009.

**Prechelt et al. (2001), *IEEE TSE*.** Serve, para justificar o controle negativo. Versão publicada lida.
- **O que traz:** profissionais em tarefas de manutenção; compara o pattern com uma solução mais simples. O resultado depende da tarefa, nos
  dois sentidos: Decorator ganha numa tarefa e perde em outra, e Observer perde numa e ganha em outra. Na dúvida, os autores recomendam o pattern.
- **Como usar:** é o experimento clássico sobre pattern em excesso na manutenção.
- **Cuidado:** não escreva "Observer negativo" nem "Decorator positivo". Mede esforço humano em papel, e só esses dois patterns coincidem com os do estudo.

**Vokáč et al. (2004), *Empirical Software Engineering*.** Serve. Lido inteiro.
- **O que traz:** replicação em ambiente real, com profissionais. O Observer inverte em relação a Prechelt, o Decorator melhora, e o Visitor fica
  fortemente negativo.
- **Como usar:** sempre ao lado de Prechelt, para mostrar que o efeito varia por pattern e por ambiente.
- **Cuidado:** não é evidência direta para o controle negativo: não tem Strategy, e o desfecho é outro. Não mede acoplamento.

**Ampatzoglou et al. (2013), *Journal of Systems and Software*.** Serve. Lido.
- **O que traz:** mapeamento de 118 artigos e 105 estudos, com 33 sobre qualidade. Em manutenibilidade, 18 de 23 resultados são positivos (p. 1955).
- **Como usar:** panorama. A decisão de aplicar um pattern depende do contexto.
- **Cuidado:** "efeitos controversos" esconde que a manutenibilidade pende para o positivo.

## 6. Orientação a objetos

**Meyer, *Object-Oriented Software Construction*, 2ª edição, Prentice Hall, 1997.** Serve. Lidos os trechos pertinentes.
- **O que traz:** o princípio aberto/fechado (p. 57). Para Meyer, "fechado" é estar disponível para uso por outros módulos, e a solução é
  a herança (pp. 59-60).
- **Como usar:** a origem do princípio. Para a versão com interfaces, cite Martin.
- **Cuidado:** não atribua a Meyer a fórmula "aberto para extensão, fechado para modificação". Se algum `CLAUDE.md` ou texto do estudo
  faz isso, está impreciso.

**Fowler e Martin** estão na seção 5.

**Yourdon e Constantine, *Structured Design*, 2ª edição, Yourdon Press, copyright 1978, 1975.** Serve em parte. Lidos o prefácio e os capítulos 6 e 7.
- **O que traz:** acoplamento (p. 76-77) e coesão (p. 95-96). O topo é a coesão funcional; trabalhar sobre os mesmos dados é a comunicacional, que
  não é máxima (p. 111). Decidir o nível exige juízo humano (p. 121).
- **Como usar:** definições. O livro atribui a primeira publicação a Stevens, Myers e Constantine (1974, *IBM Systems Journal*), que não conseguimos ler.
- **Cuidado:** é anterior à orientação a objetos. Ligar a CBO e LCOM é extensão de vocês. O ano "1979" não aparece nessa cópia.

**Parnas (1972), *Communications of the ACM*.** Serve. Lido inteiro.
- **O que traz:** cada módulo deve esconder uma decisão difícil ou provável de mudar; dividir por etapas do fluxo é pior.
- **Como usar:** base anterior aos patterns e, também, do controle negativo: onde nada deve mudar, isolar a variação não traz retorno.
  As duas ligações são de vocês; o artigo não fala de patterns nem de objetos.
- **Cuidado:** um exemplo pequeno, argumentado, sem experimento. Use como origem do conceito, não como evidência.

## 7. Como a qualidade foi medida

**Chidamber e Kemerer (1994), *IEEE TSE*.** Serve. Lido inteiro.
- **O que traz:** WMC (p. 482), DIT (p. 483), NOC (p. 485, só subclasses imediatas), CBO (p. 486), RFC (p. 487: métodos da classe mais os que eles chamam
  diretamente) e LCOM (p. 488; valor zero não indica coesão máxima, nota 28).
- **Como usar:** definir as métricas pelo original e dizer como a ferramenta as calcula. As distribuições assimétricas justificam medianas e percentis.
- **Cuidado:** não valida relação com qualidade. Não afirma que a LCOM é sensível ao número de métodos e campos; essa dedução é nossa. Os dados são de C++ e
  Smalltalk, de 1994.

**Basili, Briand e Melo (1996), *IEEE TSE*.** Serve em parte. Lido.
- **O que traz:** 180 classes C++ de alunos, tendo falhas como desfecho. CBO, RFC e DIT têm relação; a LCOM não foi significativa em nenhum caso; o WMC, que ali
  conta métodos, ficou em p = 0,06.
- **Como usar:** dizer que parte das métricas tem relação com falhas, e não com a "qualidade" em geral.
- **Cuidado:** o V5 tem hipótese sobre a LCOM, então a banca pode perguntar sobre isso.

**El-Emam (2001), *Object-Oriented Metrics: A Review of Theory and Practice*, relatório ERB-1085, National Research Council Canada.** Serve em
parte. Lido inteiro. Relatório técnico, sem revisão por pares; a decisão de usar é sua.
- **O que traz:** o tamanho da classe confunde a validação das métricas; não há limiares validados; a LCOM não previu falhas.
- **Como usar:** nas limitações: relate o tamanho ao lado das métricas e não use limiar.

**Aniche, *Java code metrics calculator (CK)*.** Página do projeto lida por resumo de uma ferramenta. É software.
- **Como usar:** a ferramenta usada. A própria página diz que a LCOM original "não é confiável". Use a citação que o projeto pede.

**Henderson-Sellers, *Object-Oriented Metrics*.** **Não lido nem conferido.** Veio do rascunho anterior.
- **Como usar:** a crítica à LCOM, se você encontrar o livro. Sem ele, a limitação se apoia na nota 28 de Chidamber e Kemerer e em Basili et al.

**Muñoz Barón et al. (2020), *ESEM*.** Serve. Lido inteiro.
- **O que traz:** meta-análise com cerca de 24 mil avaliações de 427 trechos. A complexidade cognitiva se relaciona com tempo (r = 0,54)
  e percepção (-0,29), mas tem pouco ou nenhum apoio para acertar o que o código faz (-0,13).
- **Como usar:** justificar a métrica como indicador de esforço de leitura, não de correção nem de qualidade geral.
- **Cuidado:** quase todos os trechos têm complexidade baixa, e os autores não recomendam limite. São trechos humanos.

**Inozemtseva e Holmes (2014), *ICSE*.** Serve em parte. Lido inteiro.
- **O que traz:** controlando o tamanho da suíte, a relação entre cobertura e eficácia cai para baixa a moderada. Cobertura mostra o que não
  foi testado, mas cobertura alta não prova eficácia, e não deve ser meta de qualidade.
- **Como usar:** o cuidado ao ler o JaCoCo.
- **Cuidado:** o título é mais forte que o resultado. Eficácia ali é medida por mutantes, e as suítes são humanas.

## 8. Método experimental

**Wohlin et al. (2024), *Experimentation in Software Engineering*, Springer.** **Não lido.** O PDF enviado era outro material.
- **Como usar:** desenho de experimentos e ameaças à validade. Ampatzoglou et al. (2026) também o usa para isso.

**Ernst e Baldassarre (2023), *Empirical Software Engineering*.** Serve. Preprint lido inteiro.
- **O que traz:** a diferença do segundo aviso, e a separação entre estudo confirmatório e exploratório.
- **Como usar:** motivar o pré-registro e marcar o que é confirmatório (as regras de decisão, como a não inferioridade) e o que é exploratório. O
  teste de Page é só informativo: o `OBJETIVO.md` do V5 diz que ele não decide nenhuma hipótese.

**Sallou et al. (2024), *ICSE-NIER*.** Serve. Lido inteiro. Artigo de 5 páginas.
- **O que traz:** três ameaças: modelos fechados, vazamento e reprodutibilidade. Orientações: rodar várias vezes, registrar versão,
  *prompts* e data, arquivar, comparar modelos abertos e fechados.
- **Como usar:** organizar a seção de ameaças à validade nesses três blocos, dizendo o que o estudo faz e o que não faz.
- **Cuidado:** as orientações são iniciais, não validadas. Cite como lista de ameaças, não como padrão exigido.

**Ouyang et al. (2025), *TOSEM*.** Serve em parte. Preprint lido inteiro.
- **O que traz:** 829 problemas. Na configuração padrão, a mesma entrada gera saídas diferentes, e temperatura 0 não elimina isso.
  Recomenda relatar média e variância de várias requisições.
- **Como usar:** "mesma entrada, saídas diferentes; por isso o estudo repete e relata a variação".
- **Cuidado:** é geração de código pelo ChatGPT, sem agente.

**Por que cinco rodadas.** **Nenhuma fonte lida dá esse número.** Ouyang et al. usa cinco chamadas por escolha própria; a justificativa que
aparece ("número muito usado") é para outro parâmetro, só em uma pergunta de pesquisa, e cita um preprint de aprendizado por reforço. Sallou et
al. pede "várias" e usa dez no exemplo. Yang et al. usa seis com agente. Fan et al. pede inferência estatística. Arcuri e Briand recomendam 1000 repetições,
com piso de 10, em algoritmos randomizados: cinco fica abaixo disso, e a banca pode cobrar. O caminho honesto: citá-los para
a necessidade de repetir, e justificar o número cinco pelo custo e pela variação observada nas próprias rodadas. Nas rodadas deste estudo, a primeira rodada (*pattern* certo ou não) se repetiu em 294 de 360 rodadas novas (82%), e 54 de 90 combinações tiveram as cinco rodadas iguais.

**Ahmed et al. (2025), *MSR*.** Serve em parte. Corpo lido inteiro.
- **O que traz:** seis LLMs em dez tarefas de rotulagem. Trocar **um** avaliador humano por um modelo mantém a concordância em sete delas.
  Propõe medir a concordância entre vários modelos fortes e usar se ficar acima de 0,5. Diz ser arriscado trocar todos os humanos.
- **Como usar:** a concordância entre LLM e humano depende da tarefa e deve ser medida antes.
- **Cuidado:** **o verificador do estudo conflita com a recomendação:** é um modelo só, avalia todas as execuções e julga um projeto inteiro,
  enquanto o artigo trata de rótulos curtos. Declare como limitação. O Fable 5.1 revisou 30 execuções, mas um segundo modelo da mesma empresa não
  resolve o conflito: mostra repetição, e não acerto.

**Wang et al. (2025), *Can LLMs Replace Human Evaluators?*, Proc. ACM Softw. Eng. 2(ISSTA).** Serve em parte. Lido na versão do arXiv.
- **O que traz:** avalia LLMs como juízes em engenharia de software, com trechos curtos de código. O alinhamento com humanos vai de quase humano a inutilizável.
  Modelos podem concordar entre si sem concordar com humanos.
- **Como usar:** a limitação do verificador, ao lado de Ahmed et al.
- **Cuidado:** trata de trechos, e não de um projeto inteiro. É mais favorável que Ahmed, então cite os dois.

## 9. Lacunas: partes do experimento que estavam sem fundamento

### Essenciais

**Manutenção: Swanson (1976), *ICSE*.** Serve. Lido inteiro. A manutenção são 225 das 670 execuções (33,6%).
- **O que traz:** manutenção corretiva, adaptativa e perfectiva (p. 493). Mudança que revisa a especificação funcional é *redevelopment*, fora da manutenção (p. 494).
- **Cuidado:** os cenários do estudo trazem regra nova, então não cabem nos três tipos em sentido estrito. Como chamar isso no artigo é uma decisão em aberto. É uma
  taxonomia proposta, sem dados.

**Boehm (1976), *IEEE Transactions on Computers*.** Serve em parte. Lidas a definição e a seção VII.
- **O que traz:** define manutenção (p. 1235), separando *update* (muda a especificação) de *repair* (mantém), e a divide em entender, modificar e revalidar.
- **Como usar:** os cenários do estudo são *update*. "Entender, modificar, revalidar" explica a base pronta com 26 testes.
- **Cuidado:** os números são de 1976, muito aproximados segundo o próprio autor.

**Evolução: Lehman (1980), *Proceedings of the IEEE*.** Serve só como motivação. Lido, menos a correção (*Proc. IEEE* 69(4), 485, 1981, doi:10.1109/proc.1981.12005).
- **Como usar:** as leis I e II apoiam estudar a mudança contínua; as leis III a V, não.
- **Cuidado:** ele limita as leis a sistema, projeto e organização estabelecidos (p. 1068).

**Aleatoriedade e testes estatísticos: Arcuri e Briand (2014), *Software Testing, Verification and Reliability*.** Serve em parte. Lido inteiro, no manuscrito da versão de 2014: confira volume e páginas.
- **O que traz:** recomenda muitas repetições (n = 1000, com piso de 10) e testes como U de Mann-Whitney, Fisher e A12.
- **Como usar:** a necessidade de repetir e de relatar a variação. A revisão deles excluiu aprendizado de máquina (p. 5), então a extensão para agentes é analogia de vocês.
- **Cuidado:** não justifica o número cinco. Não trata do teste de Page.

**O teste usado no V5: Page (1963), *Journal of the American Statistical Association*.** **Não lida.**
- **Como usar:** fonte do teste de Page.

**Mutantes: Jia e Harman (2011), *IEEE TSE*.** Serve em parte. Lido em parte: sem as seções III a V.
- **O que traz:** a definição de mutação e de escore de mutação; mutantes são alterações automáticas e sintáticas. Que eles representem falhas
  reais é hipótese com apoio limitado. Andrews et al. acham defeitos manuais e mutantes diferentes (seção VI-B).
- **Como usar:** só a lógica geral: versões defeituosas medem a capacidade da suíte de achar falhas.
- **Cuidado:** não sustenta a suíte de aceitação nem os mutantes escritos à mão. Apresente isso como escolha de desenho de vocês. Just et al. (2014, FSE),
  lido inteiro, também não sustenta: trata de mutantes gerados por operadores (73% das falhas reais acopladas; 17% sem operador possível).

**Banco e concorrência: Berenson et al. (1995), *SIGMOD*.** Serve. Lido inteiro.
- **O que traz:** atualização perdida, fantasma e escrita enviesada, e os níveis de isolamento que os permitem.
- **Como usar:** nomeie o fenômeno. Consultar a disponibilidade e depois inserir a reserva é fantasma ou escrita enviesada, possível em READ
  COMMITTED e em *snapshot*. Ler a linha do quarto e depois atualizá-la é atualização perdida. O certo depende do código dos casos.
- **Cuidado:** é de 1995 e não trata de PostgreSQL nem de `FOR UPDATE`. Para isso: `[CITAR: documentação do PostgreSQL]`.

**Esquema e evolução: Qiu, Li e Su (2013), *ESEC/FSE*.** Serve em parte. Lido.
- **Como usar:** justificar por que o esquema faz parte da avaliação.
- **Cuidado:** não serve para migrações, que o artigo deixa fora. Chaves estrangeiras aparecem em só 3 de 10 projetos.

### Úteis, se o texto pedir

**He et al. (2026), *MSR*.** Serve. Corpo lido inteiro.
- **O que traz:** 806 repositórios que adotaram o Cursor: velocidade sobe por dois meses; alertas (+30,3%) e complexidade cognitiva (+41,6%) persistem.
  Boa parte dos alertas vem do crescimento do código. Sugere regras do Cursor para impor qualidade, sem testar isso.
- **Como usar:** na Introdução, para motivar por que medir a qualidade. A sugestão não testada reforça a sua hipótese.
- **Cuidado:** observacional. Cite o 41,6% da tabela; a discussão do artigo fala em 25%.

**Campbell (2018), *TechDebt*.** Serve só para definir. Lido. Artigo de 2 páginas.
- **Como usar:** definição da complexidade cognitiva, que é de 2016-17.
- **Cuidado:** mede a reação de desenvolvedores em 22 projetos, e não o entendimento. Ela conta condicionais por construção, então "reduz a complexidade" não é evidência
  independente do desenho.

**Cohen (1960), *Educational and Psychological Measurement*.** **Não lida**: só comprando; vi a primeira página.
- **Como usar:** fonte do kappa, que o artigo usa na comparação com o Fable 5.1. A biblioteca da UFPB pode ajudar.

**Piaggio et al. (2006), *JAMA*.** **Não lida.** Vem da medicina.
- **Como usar:** a ideia de não inferioridade. Veja se a banca aceita uma fonte de fora da computação.

**Fora:** Wagner et al. (2025), texto de posição preliminar, cujo evento não aparece no texto lido.

## Ordem sugerida para a seção

1. **Dos modelos aos agentes:** Vaswani, Brown, Xi, Liu e Yao.
2. **Como se orienta um agente:** Galster, Mohsenimofidi, Chatlatanagulchai e a documentação, com a sua definição de *harness*.
3. **Design patterns e orientação a objetos:** Parnas, Gamma, Meyer e Martin; depois o custo (Gamma p. 31, Fowler, Martin p. 89) e a evidência mista
   (Zhang e Budgen, Prechelt, Ampatzoglou).
4. **Como medir:** Chidamber e Kemerer, Basili, Muñoz Barón, e a ressalva de El-Emam.

O método experimental e os LLMs como avaliadores podem ir para a Metodologia em vez da Fundamentação, se o modelo do seu curso preferir.

## O que falta ler

**Artigos sem acesso:** Page 1963 e Cohen 1960 (só comprando), Stevens 1974 (o IEEE não entregou o PDF). Piaggio 2006 não foi tentado.

**Livros:** Wohlin 2024 (só comprando), Kerievsky 2004 e Russell e Norvig (para definir IA); Henderson-Sellers é opcional. A biblioteca da UFPB é o melhor caminho.

**Falta ler:** a correção de Lehman (1981), que é curta.

## Referências

Dados do Crossref, salvo indicação. O formato final fica para quando você aplicar o modelo da SBC.

- Ahmed, T.; Devanbu, P.; Treude, C.; Pradel, M. (2025). Can LLMs Replace Manual Annotation of Software Engineering Artifacts? *MSR 2025*, 526-538.
- Aniche, M. (2015). *Java code metrics calculator (CK)*. https://github.com/mauricioaniche/ck/ (forma pedida pelo projeto).
- Ampatzoglou, A.; Charalampidou, S.; Stamelos, I. (2013). Research state of the art on GoF design patterns: A mapping study. *Journal of Systems and Software*, 86(7), 1945-1964. doi:10.1016/j.jss.2013.03.063
- Anthropic. *How Claude remembers your project*. https://code.claude.com/docs/en/memory. Acesso em 02/11/2026.
- Anthropic. *Extend Claude with skills*. https://code.claude.com/docs/en/skills. Acesso em 02/11/2026.
- Anthropic. *Create custom subagents*. https://code.claude.com/docs/en/sub-agents. Acesso em 02/11/2026.
- Basili, V. R.; Briand, L. C.; Melo, W. L. (1996). A validation of object-oriented design metrics as quality indicators. *IEEE TSE*, 22(10), 751-761. doi:10.1109/32.544352
- Brown, T. B. et al. (2020). Language Models are Few-Shot Learners. *NeurIPS 2020* (DBLP; páginas: conferir).
- Chatlatanagulchai, W. et al. (2026). Agent READMEs: An Empirical Study of Context Files for Agentic Coding. *ACM TOSEM*. doi:10.1145/3840295 (volume e páginas: conferir)
- Chidamber, S. R.; Kemerer, C. F. (1994). A metrics suite for object oriented design. *IEEE TSE*, 20(6), 476-493. doi:10.1109/32.295895
- Ernst, N. A.; Baldassarre, M. T. (2023). Registered reports in software engineering. *Empirical Software Engineering*, 28(2), 55. doi:10.1007/s10664-022-10277-5
- Fan, A. et al. (2023). Large Language Models for Software Engineering: Survey and Open Problems. *ICSE-FoSE*, 31-53. doi:10.1109/icse-fose59343.2023.00008
- Galster, M. et al. (2026). Configuring Agentic AI Coding Tools: An Exploratory Study. *AIware '26*, 11-20. doi:10.1145/3805760.3814887
- Hou, X. et al. (2024). Large Language Models for Software Engineering: A Systematic Literature Review. *ACM TOSEM*, 33(8), art. 220, 79 p. doi:10.1145/3695988
- Inozemtseva, L.; Holmes, R. (2014). Coverage is not strongly correlated with test suite effectiveness. *ICSE '14*, 435-445. doi:10.1145/2568225.2568271
- Jiang, S.; Nam, D. (2026). Beyond the Prompt: An Empirical Study of Cursor Rules. *MSR '26*, 397-409. doi:10.1145/3793302.3793367
- Jimenez, C. E. et al. (2024). SWE-bench: Can Language Models Resolve Real-World GitHub Issues? *ICLR 2024* (página da Universidade de Princeton e buscadores; conferir no DBLP).
- Liu, J. et al. (2026). Large Language Model-Based Agents for Software Engineering: A Survey. *ACM TOSEM*. doi:10.1145/3796507
- Mohsenimofidi, S.; Galster, M.; Treude, C.; Baltes, S. (2026). Context Engineering for AI Agents in Open-Source Software. *MSR '26*, 194-198.
- Muñoz Barón, M.; Wyrich, M.; Wagner, S. (2020). An Empirical Validation of Cognitive Complexity as a Measure of Source Code Understandability. *ESEM '20*, 1-12. doi:10.1145/3382494.3410636
- Ouyang, S.; Zhang, J. M.; Harman, M.; Wang, M. (2025). An Empirical Study of the Non-Determinism of ChatGPT in Code Generation. *ACM TOSEM*, 34(2), 1-28. doi:10.1145/3697010
- Prechelt, L.; Unger, B.; Tichy, W. F.; Brössler, P.; Votta, L. G. (2001). A controlled experiment in maintenance: comparing design patterns to simpler solutions. *IEEE TSE*, 27(12), 1134-1144. doi:10.1109/32.988711
- Sallou, J.; Durieux, T.; Panichella, A. (2024). Breaking the Silence: the Threats of Using LLMs in Software Engineering. *ICSE-NIER '24*, 102-106. doi:10.1145/3639476.3639764
- Stevens, W. P.; Myers, G. J.; Constantine, L. L. (1974). Structured design. *IBM Systems Journal*, 13(2), 115-139. doi:10.1147/sj.132.0115
- Vaswani, A. et al. (2017). Attention is All you Need. *NIPS 2017*, 5998-6008 (DBLP).
- Vokáč, M. et al. (2004). A Controlled Experiment Comparing the Maintainability of Programs Designed with and without Design Patterns: A Replication in a Real Programming Environment. *Empirical Software Engineering*, 9(3), 149-195. doi:10.1023/B:EMSE.0000027778.69251.1f
- Wang, L. et al. (2024). A survey on large language model based autonomous agents. *Frontiers of Computer Science*, 18(6), 186345. doi:10.1007/s11704-024-40231-1 (o texto lido mostra 2025: conferir ano e volume)
- Wohlin, C. et al. (2024). *Experimentation in Software Engineering*. Springer. doi:10.1007/978-3-662-69306-3
- Xi, Z. et al. (2025). The rise and potential of large language model based agents: a survey. *Science China Information Sciences*, 68(2), 121101. doi:10.1007/s11432-024-4222-0
- Yang, J. et al. (2024). SWE-agent: Agent-Computer Interfaces Enable Automated Software Engineering. *NeurIPS 37*, 50528-50652. doi:10.52202/079017-1601
- Yao, S. et al. (2023). ReAct: Synergizing Reasoning and Acting in Language Models. *ICLR 2023* (DBLP).
- Zhang, C.; Budgen, D. (2012). What Do We Know about the Effectiveness of Software Design Patterns? *IEEE TSE*, 38(5), 1213-1231. doi:10.1109/tse.2011.79
- Boehm, B. W. (1976). Software Engineering. *IEEE Transactions on Computers*, C-25(12), 1226-1241 (última página: conferir).
- Wang, R.; Guo, J.; Gao, C.; Fan, G.; Chong, C. Y.; Xia, X. (2025). Can LLMs Replace Human Evaluators? An Empirical Study of LLM-as-a-Judge in Software Engineering. *Proc. ACM Softw. Eng.*, 2(ISSTA), art. ISSTA086. doi:10.1145/3728963
- Just, R. et al. (2014). Are mutants a valid substitute for real faults in software testing? *FSE 2014* (páginas e DOI: conferir).
- Ouyang, L. et al. (2022). Training language models to follow instructions with human feedback. *NeurIPS 2022* (local de memória: conferir).

**Acrescentadas pelas lacunas**
- Arcuri, A.; Briand, L. (2014). A Hitchhiker's guide to statistical tests for assessing randomized algorithms in software engineering. *Software Testing, Verification and Reliability*, 24(3), 219-250. doi:10.1002/stvr.1486
- Berenson, H. et al. (1995). A critique of ANSI SQL isolation levels. *Proceedings of ACM SIGMOD '95*, 1-10 (anais em *SIGMOD Record*, 24(2): conferir). doi:10.1145/568271.223785
- Campbell, G. A. (2018). Cognitive complexity: an overview and evaluation. *TechDebt '18*, 57-58. doi:10.1145/3194164.3194186
- Cohen, J. (1960). A Coefficient of Agreement for Nominal Scales. *Educational and Psychological Measurement*, 20(1), 37-46. doi:10.1177/001316446002000104
- He, H.; Miller, C.; Agarwal, S.; Kästner, C.; Vasilescu, B. (2026). Speed at the Cost of Quality: How Cursor AI Increases Short-Term Velocity and Long-Term Complexity in Open-Source Projects. *MSR '26*, 181-193. doi:10.1145/3793302.3793349
- Jia, Y.; Harman, M. (2011). An Analysis and Survey of the Development of Mutation Testing. *IEEE TSE*, 37(5), 649-678. doi:10.1109/tse.2010.62
- Lehman, M. M. (1980). Programs, life cycles, and laws of software evolution. *Proceedings of the IEEE*, 68(9), 1060-1076. doi:10.1109/proc.1980.11805; correção em *Proceedings of the IEEE*, 69(4), 485, 1981. doi:10.1109/proc.1981.12005
- Page, E. B. (1963). Ordered Hypotheses for Multiple Treatments: A Significance Test for Linear Ranks. *Journal of the American Statistical Association*, 58(301), 216-230. doi:10.1080/01621459.1963.10500843
- Parnas, D. L. (1972). On the criteria to be used in decomposing systems into modules. *Communications of the ACM*, 15(12), 1053-1058. doi:10.1145/361598.361623
- Piaggio, G. et al. (2006). Reporting of Noninferiority and Equivalence Randomized Trials. *JAMA*, 295(10), 1152. doi:10.1001/jama.295.10.1152 (páginas: conferir)
- Qiu, D.; Li, B.; Su, Z. (2013). An empirical analysis of the co-evolution of schema and code in database applications. *ESEC/FSE 2013*, 125-135. doi:10.1145/2491411.2491431
- Swanson, E. B. (1976). The dimensions of maintenance. *Proceedings of the 2nd International Conference on Software Engineering* (Google Scholar; páginas: conferir).

**Livros: conferir ano, edição e editora no exemplar que você citar**
- Fowler, M. *Refactoring: Improving the Design of Existing Code*. 2. ed. Addison-Wesley (registro de 2018, direitos de 2019: use o que constar no seu exemplar).
- Gamma, E.; Helm, R.; Johnson, R.; Vlissides, J. *Design Patterns: Elements of Reusable Object-Oriented Software*. Addison-Wesley (registro de 1994, direitos de 1995).
- Henderson-Sellers, B. *Object-Oriented Metrics*. (Não conferido.)
- El-Emam, K. (2001). *Object-Oriented Metrics: A Review of Theory and Practice*. Relatório técnico ERB-1085, National Research Council Canada.
- Kerievsky, J. (2004). *Refactoring to Patterns*. Addison-Wesley. (Não lido.)
- Yourdon, E.; Constantine, L. L. *Structured Design: Fundamentals of a Discipline of Computer Program and Systems Design*. 2. ed. Yourdon Press, copyright 1978, 1975.
- Martin, R. C. (2003). *Agile Software Development: Principles, Patterns, and Practices*. Prentice Hall.
- Meyer, B. (1997). *Object-Oriented Software Construction*. 2. ed. Prentice Hall.
