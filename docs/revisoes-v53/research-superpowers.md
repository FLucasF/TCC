---
title: "Revisão por pares (construtiva e adversarial) — artigo-v53"
reviewer: Claude Opus 5.5, passada única, sem subagentes
role: constructive + adversarial (ver "O que foi pulado")
date: 2026-11-09
manuscript: J:\TCC\docs\versoes\artigo-v53.md
reporting_standard: itens de CONSORT adaptados a experimento com agentes (alocação, cegamento, desfechos, desvios) + auditoria de reprodutibilidade
---

# Revisão por pares do artigo-v53

Método: `requesting-peer-review` e `peer-reviewer` do plugin research-superpowers. Régua: bom artigo de evento brasileiro de engenharia de software. Li só o artigo e as seis figuras. Não consultei os dados do repositório. Os itens que o pedido marcou como já conhecidos não aparecem aqui, salvo quando o texto os trata de modo incoerente.

## O que foi pulado do método

- **Dois revisores em contextos separados.** O método pede duas passadas com contexto novo. Aqui o mesmo revisor fez as duas numa passada só, como foi pedido. Cada problema traz a crítica (papel adversarial) e o conserto (papel construtivo), mas as duas leituras não são independentes.
- **Tudo que depende da wiki:** `knowledge/synthesis`, `knowledge/sources`, `knowledge/_meta/log.md`, `gate-overrides.log`, `lint-wiki.py` e o arquivo de plano em `input/ideas`.
- **Hipóteses e critérios do pré-registro original.** Só usei o que o artigo diz deles.
- **`make render` e os MCPs de busca.** Não há projeto QMD, e a rede estava vedada.
- **Conversa com o autor sobre cada problema maior** (aceitar, rejeitar, adiar). Fica para o autor.
- **Auditoria de citações contra as fontes.** Não abri nenhuma fonte. A seção própria diz o que deu para fazer.
- **Checklists de teologia e arqueologia:** não se aplicam. Da lista do método, usei só a auditoria de reprodutibilidade.

## Resumo da revisão

**Passada construtiva.** O estudo principal é bem desenhado para um TCC: quartetos simultâneos, regras de decisão fixadas antes, suíte testada com versões defeituosas, conferência humana e um relato franco da trajetória (Seções 4.2, 6 e 7). A aritmética das tabelas fecha quase toda; as exceções estão nos problemas 1 e 9. O que mais precisa de trabalho não são os dados, e sim a distância entre o que a Seção 6 admite e o que o Resumo e a Conclusão afirmam.

**Passada adversarial.** Resumo e Conclusão escolhem o lado favorável de resultados que o próprio corpo do texto matiza. "Sem piorar a correção" esconde um modelo que piora em 5 de 5 pares. "Sem aumentar o exagero" vem de uma hipótese contrariada, sem teste de equivalência. "Mesma direção" esconde que a única célula comparável aos dois estudos não reproduziu. "Efeito mais forte na manutenção" não vale para o arquivo sozinho. A queda do comportamento na manutenção é descrita como oscilação.

## Problemas maiores (por ordem de gravidade)

### 1. "Sem piorar a correção" não se sustenta sem ressalva, e a medida não está definida

- Resumo: "sem aumentar o exagero, sem piorar a correção". A Conclusão repete.
- Seção 6, P3: "o Haiku 4.5 tenha piorado nos cinco pares (saldo -5)". Esse número aparece só na Discussão, e não no Resumo, nos Resultados ou na Conclusão. É justamente um dos dois modelos mais fracos, os que a tese diz serem os beneficiados.
- A medida usada nos pares não é definida. A Tabela 1 traz 71% e 84% na suíte inteira, ou seja, cerca de 39 e 46 de 55 (+7 execuções). A Seção 4.3 diz: "três a mais melhoraram do que pioraram". Com medida binária, o saldo teria de ser +7. Deve ser outra medida (casos passados?), mas o texto não diz.
- A margem "não passa de 7 em 55" (Seção 4.1) equivale a quase 13 pontos percentuais e não tem justificativa. Ela também ignora que os pares se agrupam por modelo, e é assim que 5 de 5 pares ruins de um modelo somem no conjunto.

**Conserto:** definir a medida; justificar a margem; mostrar a tabela de correção por modelo; escrever no Resumo que a não-inferioridade vale para o conjunto e que o Haiku 4.5 piorou. Relatar também o desfecho conjunto: pattern certo e suíte inteira na mesma execução.

### 2. "Sem aumentar o exagero" vem de uma hipótese contrariada, sem evidência de equivalência

- Seção 4.3: "A hipótese do exagero foi contrariada". A regra era "de alteração, sem direção" (Seção 4.1). Não achar alteração não é o mesmo que mostrar que não aumentou.
- Com 1 em 55, o intervalo de Wilson de 95% (calculado por este revisor) vai até cerca de 9,6%. E tudo vem de um único ponto de um único sistema.
- O artigo usou não-inferioridade para a correção, mas não para o exagero.
- A regra "de alteração" não tem critério numérico no texto. Um par em 55 conta como "contrariada", e a queda de 3 para 0 conta como apoiada (Seção 4.3: "teve efeito apoiado no desenho, decidido por um único modelo fora do teto"). O leitor não consegue verificar a fronteira.
- "Exagero" tem três definições: o ponto do seguro (estudo principal), a "estrutura sem necessidade fora da parte que o *pattern* resolve" (extensão) e o tamanho da mudança (controles). A Discussão e a Conclusão as leem como uma só.

**Conserto:** trocar por "não se detectou aumento, com resultado no piso", dar o intervalo e dizer no Resumo que a hipótese não se confirmou; dar o critério numérico da regra de alteração; nomear os três construtos de forma diferente.

### 3. "A mesma direção" esconde que a única célula comparável não reproduziu

- Resumo: "mostrou a mesma direção em nove das dez combinações". A exceção não é citada no Resumo.
- Seção 6: "A extensão, portanto, não reproduz o efeito do estudo principal nesse ponto". Essa é a única célula que repete pattern e tarefa do estudo principal, e foi de 9 para 7 no N1.
- A explicação pelo teto não basta: "de modo que restam 10 execuções por nível". Sem as 5 do Sonnet 5.5 sobram 4, 2 e 4 acertos em 10, ou seja, há espaço de sobra. O teto não explica a ausência de efeito.
- Seção 6: "O aumento do *pattern* certo apareceu na mesma direção nos dois estudos". Entre os estudos mudaram o `CLAUDE.md`, a *skill*, três enunciados, o domínio e o *framework*. Não é replicação da intervenção, e o texto deveria dizer isso nessa frase.

**Conserto:** citar a exceção no Resumo e tratá-la como a principal falha de replicação; discutir causas reais (domínio, Javalin, enunciado sem a frase de reforço); rodar N0 e N1 com o enunciado do *checkout* nos três modelos, se houver tempo.

### 4. "Efeito mais forte" na manutenção só vale para o N2, e o N2 pode não ser igual nas duas tarefas

- Resumo: "um efeito mais forte quando o agente muda um código pronto". Seção 8: "o efeito é mais forte quando o agente muda um código pronto".
- Só com o arquivo, o ganho é +14 na manutenção e +12 na construção: dois acertos de diferença. A diferença entre os ganhos é 2,7 pontos, com intervalo aproximado de -18,7 a +24,0 (cálculo deste revisor, sem considerar o agrupamento por modelo). No N2 é 26,7 pontos (6,8 a 46,6).
- Em relação ao espaço disponível no N0, o N1 ganha 25% na manutenção e 35% na construção. A direção se inverte conforme a escala.
- A Seção 6 cita "a linha da *skill* no N2 da manutenção". A Seção 5.2 define o N2 só como "o `CLAUDE.md` e a *skill*". Se existe uma linha própria da manutenção, a comparação entre tarefas no N2, que dá o maior número do artigo (19 para 58), está confundida com a instrução.
- A Seção 7 reconhece a falta de teste, mas a Conclusão afirma sem ressalva.

**Conserto:** restringir a frase à combinação arquivo e *skill*; descrever no método a configuração exata por tarefa; dar a diferença dos ganhos com intervalo.

### 5. A queda do comportamento na manutenção é apresentada como oscilação

- Seção 6, P6: "arquitetura, testes, banco e comportamento oscilam sem direção clara".
- Tabela 3, comportamento na manutenção: 61, 59, 54, uma queda monotônica justamente onde o ganho de pattern é maior.
- A Seção 5.3 diz que o Sonnet 5.5 teve bom "nas 150 execuções", então 25 por nível são dele. Nos dois modelos fora do teto, a conta fica 36, 34 e 29 em 50. Pela mesma lógica de teto que o artigo aplica ao pattern, o comportamento cai de 72% para 58%.
- Os testes na construção também caem de forma monotônica (11, 9, 6).
- O Resumo cita só o lado bom da extensão, e, para a extensão, o P2 fala em custo apenas para o exagero.

**Conserto:** reportar essa queda no Resumo e na Discussão; analisar por modelo; dar o cruzamento entre classe do pattern e comportamento (o malfeito quebra o comportamento?).

### 6. "Calibrado" no Resumo é incoerente com o que a Seção 7 admite

- Resumo: "um verificador de qualidade calibrado". Seção 1: "calibrado em casos de resposta conhecida".
- Seção 7: "a calibração não cobriu o design do restante do código nem o exagero" e "testou apenas acerto e omissão, sem malfeito nem alternativa equivalente".
- Consequência: 128 das 449 execuções (29%; 89 malfeitas e 39 alternativas, Tabela 6) caíram em classes nunca calibradas, e a fronteira entre malfeito e alternativa define o "pattern certo", que inclui alternativa.
- O P6 afirma "O design do restante do código acompanha a orientação", na dimensão não calibrada e com a menor concordância. O exagero de 4 para 9, levado à Conclusão, também não foi calibrado.
- A limitação é declarada. O problema é o uso sem ressalva nos lugares que o leitor lê primeiro.

**Conserto:** escrever "calibrado em casos extremos de acerto e omissão"; marcar design e exagero como indicativos; calibrar malfeito e alternativa com casos construídos.

### 7. A proteção contra decisões posteriores é mais fraca do que a Seção 4.2 faz parecer

- Seção 4.2: "sem a direção de nenhum efeito". Seção 7: "em que foi a métrica que mais mudou com a configuração". O autor conheceu ao menos o efeito na complexidade antes de registrar.
- 119 execuções serviram para ajustar enunciado, suíte e análise. O artigo não diz se o `CLAUDE.md` do N1 ou a escolha do ponto do clube como desfecho principal (o ponto com mais espaço de ganho) foram definidos à luz delas.
- A Seção 7 diz que as orientações "foram escritas por quem conhece as tarefas e o critério" apenas ao falar da extensão. O mesmo vale, pelo que o texto mostra, para o `CLAUDE.md` e o Semgrep do estudo principal, e é o resultado mais importante do artigo.
- "o que permite verificar a data do registro": a data de um *commit* é declarada por quem o faz. Um *hash* prova integridade, não data, salvo com carimbo de terceiros (OSF, Zenodo, Software Heritage).
- O V4 foi executado e descartado, e seus resultados não aparecem. Ainda que não sejam confirmatórios, são uma replicação natural.

**Conserto:** dizer o que foi fixado depois de ver quais dados; estender a limitação ao estudo principal; publicar os resultados do V4 em apêndice; depositar o pré-registro em lugar com carimbo externo ou trocar a frase.

### 8. A intervenção não é mostrada, não tem placebo, e a Conclusão generaliza demais

- Seção 4.1: "um arquivo `CLAUDE.md` com princípios gerais, sem nomear *patterns*". O texto do arquivo não aparece, nem em trecho ou apêndice. O leitor não consegue julgar "sem nomear".
- Não há controle ativo, como um `CLAUDE.md` de mesmo tamanho sem conteúdo de projeto. Não dá para separar o efeito do conteúdo do efeito de haver uma instrução de cuidado.
- Seção 4.1: "O texto contém quatro pontos em que a variação pede o padrão Strategy". Só o ponto do clube é reportado, e o frete, o cupom e o pagamento somem.
- Seção 8: "um arquivo de princípios aumenta o uso correto do padrão Strategy (de 67% para 93%)". O número é de um ponto, de um sistema, em Java, com um texto específico. O Resumo mantém "o ponto mais difícil", a Conclusão perde a ressalva.
- Domínio único, linguagem única e esforço único não aparecem como ameaças na Seção 7.

**Conserto:** incluir o `CLAUDE.md` em apêndice; reportar os quatro pontos; reescrever a Conclusão no escopo real; declarar a ausência de placebo.

### 9. "O efeito veio dos que não acertavam" é quase verdadeiro por construção

- Resumo: "o efeito veio dos seis modelos que não acertavam sozinhos". Modelos com 5 de 5 no N0 não podem melhorar, então a frase é em boa parte aritmética. Selecionar pelo N0 e medir o ganho contra o mesmo N0 também traz regressão à média.
- Os seis tinham 12 de 30 no N0 (37 menos 25), e o Haiku 5.5 tinha 3 de 5. "Não acertavam" é forte demais.
- A hipótese "o que menos acerta no N0 deve ter o maior saldo de pares" favorece quem parte de zero. Mesmo assim, um dos dois empatados "teve saldo +1", o menor possível sem ser zero, e é o modelo mais fraco do conjunto. A substância da hipótese não se confirma, embora a regra sim.
- P3 é pré-registrada e pergunta justamente pela variação entre modelos, mas não há tabela com os 11 modelos.

**Conserto:** trocar por "o ganho se concentra onde havia espaço"; dizer que a regra foi cumprida por um dos dois empatados e não pelo outro; incluir a tabela por modelo e nível; separar a réplica de referência da de ganho, se possível.

### 10. O "mecanismo" é uma redescrição do desfecho, e o acoplamento mostrado é o lado favorável

- Resumo: "sugerem o mecanismo: com a orientação, o código troca condicionais por polimorfismo". O desfecho já é definido como a variação atrás de uma interface (Seção 2). Contar interfaces mede o mesmo fenômeno, não explica por que ele acontece. O CK não conta condicionais, e o WMC da maior classe na extensão quase não muda (40,8 para 38,3; 32,3 para 30,9).
- Resumo: "o acoplamento médio muda pouco". Seção 6, P8: "o aumento em relação ao N0 fica entre cerca de 22% e 46%". O segundo número fica fora do Resumo e da Conclusão. A média cai por efeito do denominador, porque as classes novas são pequenas. A conta também multiplica mediana (CBO, Tabela 2) por média (tipos).
- Na Tabela 6, na manutenção, o malfeito tem mais interfaces (0,77) do que o acerto (0,61), o que enfraquece "as métricas acompanham a leitura do verificador".

**Conserto:** trocar "mecanismo" por "assinatura estrutural"; dar o acoplamento total por projeto, medido de verdade, com distribuição; levar os dois números à Conclusão.

### 11. A *skill* do estudo principal nunca foi usada, o que muda a leitura de N2 e N3

- Seção 4.3: "a *skill* não foi aberta espontaneamente em nenhuma das 110 execuções que a tinham". Seção 6, P4: "A *skill* pública não acrescentou efeito mensurável". Sem abertura, ela não foi testada, e não cabe dizer que não teve efeito.
- N1 e N2 são quase o mesmo tratamento, então as diferenças entre eles (84% e 80% na suíte, exagero de 1 para 3, 584 e 614 linhas) dão uma ideia do ruído. A queda do exagero de N2 para N3, tida como apoiada, tem tamanho parecido com esse ruído.
- Na extensão, a taxa de abertura nas 150 execuções N2 com pattern não é dada. Nos controles, a Seção 5.3 diz que não houve abertura "em nenhuma das 10 do Haiku 4.5". Isso pesa sobre a frase "a orientação alcança o modelo".

**Conserto:** reescrever como falha de entrega do tratamento; usar a diferença N1-N2 como referência de ruído; dar a taxa de abertura por modelo e tarefa e separar o resultado por quem abriu.

### 12. A Conclusão sobre os controles negativos é mais forte do que a Discussão permite

- Seção 8: "os controles negativos indicam que a orientação não leva o agente a aplicar *pattern* onde ele não cabe". Seção 6: "não permite afirmar que a orientação melhora a contenção". A Seção 6 também limita a "ao menos nesses pedidos", e a Conclusão não.
- Os pedidos são pontuais (Seção 5.2: "um percentual e um período, um texto, um campo, uma assinatura ou uma coluna"). Eles diferem dos positivos também no tamanho. A contenção pode vir da pequenez do pedido, e não de um julgamento sobre a necessidade do pattern.
- Com 0 em 30, o limite superior aproximado é 11%. Com 2 em 30, 21%. As duas exceções são 2 de 2 numa célula (Sonnet 5.5, N2, temporada).
- As duas rodadas e os ajustes foram decididos depois de conhecidos os resultados positivos (Seção 7), o que o Resumo não diz.

**Conserto:** limitar a frase ("em pedidos pontuais"); dar os intervalos; criar pares mínimos, com pedido do mesmo tamanho do positivo e sem variação futura real.

### 13. A frase acrescentada aos enunciados pode favorecer os níveis orientados

- Seção 7: "como vale para os três níveis, ela não favorece nenhum deles". Isso é falso como argumento. Ser constante entre níveis não impede interação.
- A Seção 2 diz que esse é o critério "que os textos dos cenários pressionam ao dizer que a variação vai crescer". Uma frase assim pode ativar justamente os princípios do `CLAUDE.md`.
- Ela foi posta em só três dos cinco enunciados, o que também confunde a comparação entre patterns.

**Conserto:** trocar a frase por "pode ter interagido com a orientação"; se possível, rodar uma amostra sem ela.

### 14. Resultados pré-registrados negativos ficam fora do Resumo, e há resultados novos só na Discussão

- Seção 6, P2: "a duplicação também sobe (de 1,2% para 2,5% das linhas, com a hipótese de não repetir o comum contrariada)". Não aparece nos Resultados, no Resumo nem na Conclusão. As hipóteses contrariadas de CBO e LCOM também não vão ao Resumo.
- "Das cinco hipóteses principais" (Seção 4.3): não há lista completa de hipóteses, regra e resultado, nem o total delas.
- Primeira aparição só na Discussão: Haiku 4.5 com -5, duplicação, classes, exagero por modelo, forma literal do Decorator e dados de repetição por modelo.

**Conserto:** incluir tabela com todas as hipóteses, regra, resultado e desvio; mover os números novos para os Resultados; dizer no Resumo quantas hipóteses foram apoiadas e quais não.

## Problemas menores

1. **LCOM (Seção 4.3).** "os projetos com as classes menos coesas tornam-se mais raros": leitura posterior de uma hipótese contrariada, com corte tirado do próprio N0 e apoio em seis projetos. LCOM cresce com pares de métodos, então quebrar classes grandes a reduz mecanicamente, e isso não mostra mais coesão.
2. **Parcela do `CLAUDE.md` (P4).** "responde pela maior parte do efeito": no desenho cumulativo o primeiro componente leva o crédito. Na manutenção da extensão é o contrário (+14 do arquivo, +25 da *skill*).
3. **Comparação escolhida (P3).** "o Haiku 4.5 com a *skill* (13 em 25) supera o Sonnet 5.5 sem orientação (11 em 25)": diferença de 2 execuções, e na construção ocorre o contrário (13 contra 22).
4. **"O modelo pesa mais" (P3 e Seção 8).** Compara só os extremos, sem tabela por modelo e dimensão.
5. **Estabilidade (P7).** "A variação acompanha o modelo": modelos perto do teto repetem por base de frequência. Comparar com a primeira rodada também trata uma rodada como referência. Usar kappa ou correlação intraclasse.
6. **Sem incerteza.** O estudo principal só traz p para o teste de Page, e o texto não diz qual é a unidade de bloco. O argumento das cinco rodadas (Seção 7) impede teste por combinação, mas não intervalos para 75 execuções por nível.
7. **Dimensões com piso (Tabela 3).** O banco na construção é 0, 0, 0, e o texto não diz se o enunciado pedia migrações. A falta de Docker "derrubou os testes de integração", e não se sabe quanto disso entra na nota de testes.
8. **Comportamento na construção.** São 25, 25 e 25. O Sonnet 5.5 leva tudo, logo o Sonnet 4.5 também tem 0 em 75, o que o texto não diz. As falhas de concorrência somam 75, e outras 75 execuções não ficaram boas por motivo não explicado.
9. **Classe por nível.** A Tabela 6 não separa nível. A omissão virou acerto ou virou malfeito?
10. **Modelos da extensão.** A escolha dos três não é justificada, e um deles estava no teto. Seção 4.1: "os 11 modelos Claude disponíveis", mas o Fable estava disponível e foi excluído por expectativa de resultado.
11. **Reprodutibilidade.** A versão do Claude Code na extensão não é dada, e o repositório citado é só o do pré-registro do V5.
12. **Custo.** A Seção 1 promete informar sobre "o custo que podem trazer", mas tokens e tempo (Seção 4.1) não são mostrados, a extensão não tem custo, e o N3 (+50% em relação ao N0) não é discutido.
13. **Unidade da complexidade (Tabela 1).** "complexidade cognitiva (média)": média de quê, por projeto ou por método? E ela conta entre as hipóteses apoiadas, sendo declarada não independente.
14. **Termo ambíguo (Resumo).** "execuções corretas" se confunde com "correção", que é outra medida.
15. **Calibração.** A Seção 5.2 diz "As 52 notas esperadas conferiram", e o ajuste após 50 de 52 só aparece na Seção 7.
16. **Hipótese não testada (Seção 2).** "que isso apareça nas métricas de acoplamento é uma hipótese deste trabalho": falta cruzar as métricas com a nota de exagero.
17. **Ameaça de memorização (Seção 7).** "O controle negativo do estudo principal reduz essa ameaça apenas em parte": o texto não explica como.
18. **Contagem (P4).** "nas dez rodadas do Sonnet 5.5" está numa frase sobre a construção, onde só há cinco. Devem ser as duas tarefas.
19. **Perguntas.** A Seção 1 chama P1 a P8 de objetivos e define P1 a P4 só para o estudo principal. A Seção 6 as trata como perguntas e as responde também com a extensão.

## Editorial

- Linhas 5 a 7: a nota de rascunho cita arquivos internos (`DECISOES.md`, `rodadas-cinco-patterns.md`). Tirar antes de submeter.
- V4, V5 e `TCC_V1` são rótulos internos. A Figura 3 traz `construction-v5` e `TCC_V1`, que o leitor não conhece.
- As Figuras 3 e 4 não mostram as 90 execuções de controle. As Figuras 5 e 6 não dizem que "certo" inclui alternativa e não têm barra de incerteza.
- Figura 2: "o código existente não muda". Alguém precisa escolher a classe nova, e o texto define acerto como "escolhida em um só lugar".
- Referências fora de ordem: Aniche vem antes de Anthropic. Três documentos da Anthropic numa só entrada dificultam localizar a fonte.
- Seção 7: "as demais notas do verificador oficial" não é claro: demais em relação a quê?
- A Seção 2 mantém rótulos em negrito e `patterns` sem itálico, diferente das Seções 4 a 7.

## Avaliação metodológica

| item | situação |
|---|---|
| Alocação | Quartetos simultâneos com ordem sorteada: bom. Na extensão, até 10 horas entre níveis, declarado. |
| Cegamento | Semgrep e leitor humano às cegas. O verificador recebe o pattern esperado, o que é aceitável, mas o desenho do código pode revelar o nível. |
| Desfechos | Primário reportado para um de quatro pontos (problema 8). Falta a lista completa de hipóteses (problema 14). |
| Medida | Regra de "alteração" sem critério numérico (problema 2). Correção em pares sem definição (problema 1). |
| Fidelidade do tratamento | Texto da intervenção ausente (problema 8). *Skill* não aberta (problema 11). |
| Desvios | Releitura humana não feita, declarada. Os demais estão na Seção 7. |
| Estatística | Regras de contagem em vez de testes, aceitável no pré-registro; faltam intervalos e tamanhos de efeito. |
| Reprodutibilidade | *Hash* e repositório para o estudo principal. Extensão sem pacote e sem versão do agente. |

## Teste de falsificação

O artigo engaja bem com o que deu errado no corpo do texto: exagero, CBO, LCOM e duplicação são ditas contrariadas, e a falha do Strategy na construção é admitida. O problema está na passagem para o Resumo e a Conclusão, onde esses resultados viram frases favoráveis ou somem. A hipótese do modelo foi cumprida por uma regra de empate, com o outro empatado quase sem ganho. A extensão não tem critério que a refutasse, o que combina com o caráter exploratório, mas então o "mostra" da Conclusão não combina com ela.

## Graduação da evidência (GRADE adaptado)

| afirmação | grau | razão principal |
|---|---|---|
| Arquivo eleva o ponto do clube de 67% para 93% | moderado | Pré-registrado e consistente nos seis; um ponto, um sistema, sem placebo |
| Sem piorar a correção | fraco | Margem larga, medida indefinida, um modelo piora em 5 de 5 |
| Sem aumentar o exagero | fraco | Hipótese contrariada, sem equivalência, piso |
| Efeito vem dos que não acertavam | fraco | Quase por construção |
| Nove das dez combinações | moderado para N2, fraco para N1 | Exploratório, verificador parcialmente calibrado, célula comparável falha |
| Mais forte na manutenção | moderado para N2, muito fraco para N1 | Diferença de 2 no N1; possível confusão no N2 |
| Controles negativos | fraco | Teto, pedidos pontuais, decidido depois |
| Mecanismo | muito fraco | Descrição do próprio desfecho, análise posterior |

## Auditoria de citações

As fontes não foram abertas. Fiz duas coisas: confirmei que todas as citações do texto têm entrada nas referências, e todas as entradas são citadas, salvo o marcador já conhecido; e olhei cinco citações que sustentam argumento, de memória. Trate a tabela como lista do que conferir.

| citação | afirmação no artigo | confere? | nota |
|---|---|---|---|
| Arcuri e Briand 2014 | "piso de dez repetições sugerido" | não verificado | De memória, a recomendação geral do texto é bem maior quando possível. Conferir a página e citar também a recomendação geral. |
| Ernst e Baldassarre 2023 | Define pré-registro | parcial, de memória | O texto trata de *registered reports*, com revisão antes da coleta. Pré-registro é mais amplo e o estudo não teve essa revisão. |
| Basili et al. 1996 | LCOM sem relação significativa; 180 classes C++ de alunos | compatível, de memória | Conferir a tabela. |
| Boehm 1976 | Manutenção preserva funções principais | compatível, de memória | Conferir a página. |
| Campbell 2018 | Complexidade cognitiva conta condicionais | compatível, de memória | Sem ressalva. |

## Recomendação

**Passada construtiva:** aceitar com revisões. O estudo principal tem valor e a transparência é rara em trabalhos deste porte.

**Passada adversarial:** revisão maior. O Resumo e a Conclusão não podem ficar como estão.

**Consolidada: revisão maior (major revision).** Quase tudo se resolve com reescrita e análises sobre dados já coletados. Novas execuções seriam necessárias apenas para o placebo, para repetir o *checkout* e para pares mínimos no controle, e são desejáveis, não obrigatórias, se as afirmações forem reduzidas ao que os dados sustentam.
