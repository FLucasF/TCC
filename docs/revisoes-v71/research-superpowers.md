---
title: "Revisão por pares (construtiva e adversarial) — Orientações de projeto e design patterns em agentes de programação"
reviewer: agente único, uma passada (método requesting-peer-review / peer-reviewer, do plugin research-superpowers)
role: constructive + adversarial (consolidadas)
date: 2026-11-09
manuscript: J:\TCC\docs\versoes\artigo-v71.md
reporting_standard: adaptação de CONSORT (experimento pareado) e de guias de experimentos em engenharia de software; ameaças específicas de estudos com LLMs
---

# Como o método foi aplicado

**Usado:** auditoria de evidência (afirmação explícita, tipo de evidência, confundidores, falácias, falsificação), os dois papéis de revisor, a classificação Major / Minor / Editorial / Fora de escopo e o modelo de relatório.

**Pulado ou adaptado, por instrução ou por dependência do projeto do plugin:**

- Os dois subagentes de contexto limpo: pedido expresso de não criar subagentes. Os dois papéis foram feitos em sequência pelo mesmo revisor, sem o isolamento que o método pressupõe.
- `make render`, wiki, `knowledge/synthesis`, `knowledge/sources`, `output/`, `knowledge/_meta/log.md` e `gate-overrides.log`: não existem aqui e não foram criados.
- Plano de pesquisa e pré-registro: não lidos, para manter o julgamento independente. O teste de falsificação considera só o que o artigo relata.
- Conversa com o autor sobre cada problema Major (aceitar, rejeitar, adiar): fica para depois.
- Conferência de citações na fonte (MCP ou texto integral): sem rede e sem acesso às fontes. Ver a seção "Auditoria de citações".
- Listas de disciplina do plugin (teologia, arqueologia): não se aplicam. Só a de reprodutibilidade foi aproveitada.

Trechos literais aparecem entre « ». Os problemas já conhecidos, listados no pedido, não foram apontados.

# Resumo

O artigo é honesto em vários pontos difíceis: relata hipóteses contrariadas, declara desvios e separa o que é pré-registrado do que é exploratório. Todas as contas das tabelas conferem entre si (Tabelas 3, 4, 5 e 6, Figuras 5 e 6, custo total, produto CBO × tipos).

O problema está na passagem dos resultados para as frases de síntese. A Conclusão contradiz a Tabela 7, o "ponto mais difícil" contradiz a Tabela 1, e o achado sobre manutenção ignora o espaço disponível para melhorar. O rótulo "pattern certo" inclui uma classe não calibrada, e o Resumo e a Conclusão deixam de fora os custos que a Discussão reconhece. Nada disso exige novas execuções.

# Problemas Major (por gravidade)

## M1. A orientação e o critério de correção partilham texto e autor, e a medida é apresentada como "decisão"

- §3: «de modo que mede a decisão de usá-lo».
- §4.1, critério do Semgrep: «cada caso isolado e nenhuma condição sobre o caso no cálculo».
- §5.1, princípio do `CLAUDE.md`: «o próximo caso num lugar só».
- §5.2: «ele orienta a reorganizar o código antes da mudança».
- §7: «foram escritas por quem conhece as tarefas e o critério».

A sobreposição é declarada só para a extensão, mas o princípio citado em §5.1 parece vir do arquivo de princípios do estudo principal e quase enuncia o critério do Semgrep. A seção de limitações do estudo principal não diz nada sobre isso. Se o arquivo pede que cada caso fique em um lugar e o critério verifica exatamente isso, o resultado mede em boa parte a obediência a uma instrução próxima do critério. Na manutenção, o agente é mandado reorganizar, enquanto o N0 é contado como omissão por fazer a mudança pedida.

Há também um erro de lógica em §7: «como vale para os três níveis, ela não favorece nenhum deles». A frase sobre o crescimento da variação pode valer para todos e ainda assim interagir com a orientação. O próprio §2 diz que esse é «o critério que os textos dos cenários pressionam ao dizer que a variação vai crescer». Um sinal que ativa a condição da regra ajuda mais quem recebeu a regra. Só Observer, Decorator e Template Method têm a frase, e essa explicação alternativa não é discutida, inclusive para o Strategy.

**Correção:** declarar a sobreposição também no estudo principal; trocar "decisão" por uma formulação que admita a adesão a instruções; transcrever o N1 em apêndice; corrigir a frase de §7; levar a ressalva ao Resumo e à Conclusão.

## M2. "Aumento maior na manutenção" ignora o espaço para melhorar e diferenças no instrumento

- Resumo: «um aumento maior na manutenção do que na construção quando se somam o arquivo e uma *skill*».
- §6 P5: «Os dados indicam aumento maior na manutenção somente quando a *skill* é acrescentada».

A comparação usa ganhos absolutos com pontos de partida muito diferentes: 41 de 75 na construção (espaço de 34) e 19 de 75 na manutenção (espaço de 56). Os dois chegam quase ao mesmo lugar, 60 e 58, e o Sonnet 5.5 chega a 25 de 25 nas duas tarefas. Como proporção do espaço disponível, o ganho até N2 é de 56% contra 70%, e até N1 é de 35% (construção) contra 25% (manutenção). Só com o arquivo, a ordem se inverte. O artigo trata o teto com cuidado no estudo principal, mas aqui não o menciona.

Além disso, só a manutenção teve o N2 alterado: «a instrução de usar a *skill* foi deslocada para o primeiro item do arquivo». Por isso, «embora a *skill* tenha sido aberta na mesma proporção nas duas» não é evidência independente, porque a posição da instrução foi ajustada em uma das tarefas para aumentar a abertura. §7 trata só da redação do `CLAUDE.md` e não dessa mudança.

O mesmo vale para §6 P4, «a *skill* acrescenta mais (+25 acertos) do que o arquivo (+14)». O efeito da ordem cumulativa é ressalvado para o estudo principal («o que decorre também da ordem cumulativa dos níveis»), mas não aqui.

**Correção:** relatar também o ganho sobre o espaço disponível; dizer que a convergência em 58 e 60 é compatível com teto; reduzir a afirmação no Resumo.

## M3. "Pattern certo" e "acerto" misturam uma classe calibrada e outra não calibrada

- §2: «**acerto** é a variação inteira atrás de uma interface».
- Tabela 3: «no pattern, acerto ou».
- Resumo: «de 19 para 58 acertos em 75».
- Tabela 6: «| manter | acerto | 99 |».

"Acerto" tem dois sentidos: a classe (99 na manutenção) e a soma com a alternativa equivalente (110). Das 264 execuções positivas, 39 (15%) são alternativas equivalentes, classe que a calibração não testou: «sem malfeito nem alternativa equivalente».

A consequência é maior no Decorator. §6 P4 diz que «a forma literal do Decorator apareceu com ela, nas dez rodadas do Sonnet 5.5 e em uma do Sonnet 4.5, e nunca sem ela». Se a forma literal corresponde à classe acerto, todos os positivos do Decorator no N0 e no N1 são alternativas (22 execuções), inclusive o +6 na construção e o +2 na manutenção que entram no +18 destacado em P1 e §7. O ganho de N1 no Decorator seria, portanto, de soluções que não são Decorator, avaliadas em uma classe sem calibração. A Figura 5 e a Figura 6 também dizem "pattern certo".

**Correção:** dar um único nome à soma, como "variação resolvida"; reservar "acerto" para a classe; dizer no texto que os ganhos do Decorator sem *skill* são alternativas; apresentar a principal contagem também só com acertos.

## M4. A Conclusão contradiz a Tabela 7

- §8: «não se observou aumento de estrutura desnecessária nos controles negativos».
- Tabela 7: «| **total (de 30)** | **30** | **30** | **28** |».
- §6: «são coerentes com o risco indicado pela construção».
- §8, na frase anterior: «Na construção com a *skill*, o exagero sobe de 4 para 9 execuções em 75».

De N0 para N2 o exagero vai de 0 para 2 em 30. O mesmo tipo de diferença é chamado de "sobe" na construção e de "não se observou aumento" na manutenção. §6 ainda diz que o controle «só poderia revelar piora», e foi o que apareceu, ainda que pouco.

**Correção:** escrever "o exagero ficou em 0, 0 e 2 de 30; o controle não sustenta a aplicação indiscriminada, mas as duas exceções vieram com a *skill*".

## M5. "Ponto mais difícil" é contrariado pela Tabela 1

- Resumo: «levou o ponto mais difícil do padrão Strategy de 67% para 93%».
- §4.1: «Este último é o mais difícil, porque um mesmo caso afeta três regras».
- Tabela 1: «| ponto do clube certo | 67% | 93% | 93% | 98% |» e «| ponto do pagamento certo (secundário) | 55% | 91% | 95% | 98% |».

No N0, o clube é o segundo ponto mais fácil (frete 78%, clube 67%, cupom 65%, pagamento 55%). A dificuldade é uma suposição do desenho e os dados não a confirmam. A frase aparece no Resumo, na Discussão e na Conclusão. Além disso, «execuções corretas», no Resumo, confunde desenho com correção funcional, que é outra medida do artigo.

**Correção:** chamar de "ponto pré-registrado como principal" ou "ponto com três regras afetadas" e observar que a dificuldade presumida não apareceu no N0.

## M6. As regras de decisão não podem ser conferidas, e o exagero N3 × N2 parece violá-las

- §4.1: «nas medidas binárias, há ao menos 5 pares não empatados».
- §4.3 (N1 × N0): «pois apenas 1 par em 55 apresentou diferença», daí "contrariada".
- §4.3 (N3 × N2): «de 3 execuções em 55 para nenhuma», daí "apoiado".

De 3 para 0 há no máximo 3 pares diferentes, abaixo do mínimo de 5. Talvez essa hipótese use a regra direcional, em que os modelos fora do teto são dois ou três, mas o texto nunca diz qual regra cada hipótese usa. Mesmo assim, a regra direcional se degenera: o efeito no desenho é «decidido por um único modelo». O artigo não enumera as hipóteses. «Das cinco hipóteses principais» não vem com lista, a complexidade não tem número de pares, e uma hipótese sobre classes surge só em §6 («embora a hipótese correspondente tenha ficado inconclusiva»).

Os 55 pares também são tratados como independentes, embora sejam cinco por modelo, e os limites (7 em 55, 36 em 55, 5 pares) não têm justificativa no texto.

**Correção:** incluir uma tabela com hipótese, comparação, tipo de regra, limite, resultado observado e veredito.

## M7. O Resumo e a Conclusão deixam de fora os custos reconhecidos na Discussão

- Resumo: «sem alteração detectada no exagero, sem piorar a correção e com menor complexidade cognitiva».
- §6 P3: «o Haiku 4.5 tenha piorado nos cinco pares (saldo -5)».
- §6 P2: «a duplicação também sobe».
- §6 P6: «o ganho em *pattern* veio acompanhado de alguma perda nessa dimensão».
- §1: «e sobre o custo que podem trazer».

Dos 7 pares que pioraram na correção, 5 são de um único modelo, justamente o mais fraco no conjunto dos níveis, que ganhou só +1 no ponto do clube. Assim, "sem piorar" vale para o conjunto, e não para quem mais precisaria da orientação. Não aparecem na síntese: duplicação dobrando com hipótese contrariada, linhas de 519 para 584, custo por execução de 1,32 para 1,52 e comportamento de 61 para 59 e 54 na manutenção. A introdução promete falar de custo, e a Conclusão mal fala.

**Correção:** uma oração no Resumo e na Conclusão com os custos mais importantes e a ressalva sobre o Haiku 4.5.

## M8. "O efeito veio dos modelos que não acertavam" é consequência aritmética, e a hipótese do modelo favorece a si mesma

- Resumo: «o efeito veio dos seis modelos que não acertavam sozinhos».
- §4.1: «situação em que a orientação só pode empatar ou piorar».
- §4.1: «o que menos acerta no N0 deve ter o maior saldo de pares».

Quem já está em 5 de 5 não pode melhorar, de modo que a frase é verdadeira por definição e não é achado. Na hipótese do modelo, o saldo máximo é 5 menos os acertos no N0: só um modelo com 0 de 5 pode chegar a +5. Essa hipótese favorece a confirmação por construção, e com cinco réplicas há regressão à média. O desempate mostra a fragilidade: entre os dois piores no N0, um teve +5 e o outro +1, e este último é o mais fraco no conjunto.

**Correção:** dizer que os cinco modelos no teto não tinham como melhorar e que os outros seis melhoraram 14 pares sem nenhum pior; apresentar a hipótese do modelo com essa limitação.

## M9. O resultado principal é generalizado, embora a única repetição direta tenha falhado

- §8: «mostra que um arquivo de princípios aumenta o uso correto do padrão Strategy».
- §6: «A extensão, portanto, não reproduz o efeito do estudo principal nesse ponto».
- §6: «o Strategy vai de 15 para 14 acertos em 30».
- §6: «O aumento do *pattern* certo apareceu na mesma direção nos dois estudos».

A comparação que o estudo principal testou (N1 × N0, Strategy, construção) foi na direção oposta na extensão: 9 para 7 em 15. A última frase citada de §6 convive mal com a anterior. "A mesma direção" no Resumo também compara janelas diferentes: N0 a N1 no estudo principal, N0 a N2 na extensão, e o N2 da extensão é outro tratamento (*skill* escrita para o estudo, com instrução de uso). A convergência entre estudos tem o mesmo autor, a mesma família de arquivos e modelos parcialmente iguais.

Há ainda um único arquivo por nível: "um arquivo de princípios" é, na prática, este arquivo, em um único enunciado de *checkout*.

**Correção:** escrever na Conclusão "neste serviço e com este arquivo" e mencionar ali que a extensão não repetiu o efeito no Strategy, com o número 15 para 14; dizer em §6 que a convergência vale para o conjunto dos *patterns*, e não para o Strategy.

## M10. O N2 do estudo principal é tratado de duas maneiras, e ele oferece uma medida de ruído que não foi usada

- §6 P2: «o `CLAUDE.md` e a *skill* não alteram o exagero».
- §6 P4: «de modo que o tratamento não chegou a ser aplicado».

Se a *skill* não foi aberta em nenhuma das 110 execuções, não se pode dizer que ela não altera o exagero. Também é preciso dizer que a descrição da *skill* continua no contexto, como afirma a Introdução. Na prática, N1 e N2 quase coincidem. Essa é uma oportunidade: as diferenças entre eles mostram o tamanho do ruído (exagero de 2% para 5%, suíte de 84% para 80%, tipos de 27,9 para 30,2, constantes de *enum* de 4,0 para 2,7, CBO de 2,48 para 2,59). O aumento de cerca de 0,2 no CBO da extensão e a queda de 3 para 0 no exagero de N3 estão nessa ordem de grandeza. A queda N3 × N2 sai de um N2 acima do N1 (1 execução) sem diferença real de tratamento.

**Correção:** retirar "e a *skill*" em P2; usar N1 × N2 como referência explícita de ruído nas leituras descritivas.

## M11. A extensão, sem medida de incerteza, é descrita com verbos de conclusão, e a estabilidade depende da taxa de base

- §8: «A extensão, com cinco rodadas, mostra».
- §7: «sem teste de significância».
- §6 P7: «o que mostra que uma execução isolada não basta» e «A variação acompanha o modelo».

Comparações como +18 × +8, 14 × 12, 96, 103 e 95 de 120 não têm intervalo nem dispersão. O limite das cinco rodadas é citado só na extensão («fica abaixo do piso de dez repetições»), embora o estudo principal também tenha cinco réplicas por modelo e faça afirmações por modelo (3 de 5, 0 de 5). A estabilidade como concordância com a primeira rodada depende da taxa de base: um modelo perto de 0% ou de 100% repete o resultado por isso. O Sonnet 5.5 no teto e a conclusão de que a variação acompanha o modelo são, em parte, esse efeito.

**Correção:** dar intervalos descritivos (Wilson ou *bootstrap* por combinação), sem transformar em testes; usar "observou-se" e "sugere"; relatar uma medida de concordância corrigida pela taxa de base.

## M12. O comportamento na construção esconde um resultado extremo e sustenta uma frase sem apoio

- §5.3: «o Haiku 4.5, na construção, não o teve em nenhuma das 75».
- §6 P6: «O comportamento não varia na construção».
- Tabela 3: «| comportamento | 25 | 25 | 25 | 61 | 59 | 54 |».
- §6 P6: «é nas dimensões ligadas a ela que os ganhos aparecem».
- Tabela 3: «| arquitetura | 16 | 9 | 16 | 58 | 57 | 68 |».

O Sonnet 5.5 foi bom nas 150 execuções, o que dá 25 por nível na construção. Logo, os 25 de cada nível na Tabela 3 são todos dele, e **o Sonnet 4.5 também teve zero em 75**. O texto só menciona o Haiku 4.5. Por isso, "não varia" é um padrão de tudo ou nada por modelo, e não ausência de efeito da orientação.

A frase final de P6 não se sustenta: arquitetura é uma dimensão de estrutura e não melhora de forma clara na construção. A única dimensão fora do *pattern* com ganho é o design, que tem o menor acordo («design, em 24 (kappa de Cohen de 0,53, o mais baixo)») e não foi calibrada («a calibração não cobriu o design do restante do código»). Mesmo assim, §6 afirma que «O design do restante do código acompanha a orientação».

**Correção:** dizer que dois dos três modelos têm zero na construção; refazer a frase final de P6 com a ressalva sobre o design.

## M13. Limite da proteção por pré-registro

- §4.2: «Antes da medição, a bancada de testes acumulou 119 execuções».
- §4.2: «o autor viu apenas informações operacionais».
- §4.2: «embora a data de um *commit* seja declarada por quem o realiza».

A Figura 3 descreve as bancadas como "Claude Code com e sem orientação; ajustes do enunciado e da medição". Portanto, antes do congelamento já se comparavam níveis e se ajustavam enunciado e medição. A frase de cegamento do autor se aplica ao V4 e não a essas 119 execuções. O texto também não informa se o agente que viu os resultados do V4 teve participação na redação final do V5. O pré-registro protege a análise a partir de 10/10, mas não a escolha do enunciado, do arquivo e do critério. §7 reconhece isso só para a complexidade.

**Correção:** dizer o que foi visto em cada fase e o que o pré-registro cobre; separar a afirmação sobre o V4 da afirmação sobre as bancadas.

## M14. "Piloto" tem significado ambíguo, e pode haver dois instrumentos dentro das 450 execuções

- §5.2: «depois de um piloto em que o Haiku 4.5 a abriu em apenas 1 de 5 execuções».
- §5.2: «o piloto (90)».

As 90 execuções do piloto contam nas 450 e correspondem a uma execução por combinação, o que dá exatamente 5 execuções do Haiku 4.5 no N2 da manutenção. Se esse piloto foi o que levou à mudança, a primeira rodada do N2 da manutenção foi feita com a instrução antiga ou foi refeita, e nenhum dos dois casos é relatado. Se foi outro piloto (fase intermediária), o texto precisa dizer. A resposta afeta os 58 de 75, os 19 de 25 de abertura e a estabilidade em P7, que compara com a primeira rodada.

**Correção:** esclarecer em uma frase. Se a primeira rodada usou outra instrução, relatar o resultado sem ela.

# Problemas Minor

1. **Resumo e §8 contra §4.1.** «descritivas e analisadas depois dos resultados» contraria «apenas o CBO e a LCOM integram as hipóteses do pré-registro». O CBO do estudo principal é a própria base de "o acoplamento médio muda pouco". Além disso, §6 afirma «O CBO médio, portanto, não basta», ressalva que não chega ao Resumo nem à Conclusão, embora o total estimado de dependências suba de 22% a 46%.
2. **§1 (efeito da inclusão recente dos controles).** O texto diz «em 450 execuções avaliadas por um verificador de IA calibrado» e define «o exagero e a correção do código (P2)» como pergunta só do estudo principal. As 90 execuções de controle não aparecem nos objetivos, nas contribuições, em §5.1 ou nas Figuras 3 e 4, mas são discutidas em P2.
3. **§5.3.** A *skill* foi aberta «em nenhuma das 10 do Haiku 4.5». No N2 do controle, um terço das execuções não recebeu o conteúdo da *skill*, e na extensão com *pattern* o mesmo modelo abriu em 19 de 25. O controle tem, na prática, 20 execuções com *skill*, e a diferença não é discutida.
4. **§6 P4.** «dez rodadas do Sonnet 5.5» está em frase sobre o Decorator na construção, onde só há cinco rodadas desse modelo no N2. As dez devem somar as duas tarefas, o que precisa ser dito.
5. **§4.3.** «as quatro perguntas de desenho» não são definidas, e a mesma seção diz que os outros pontos têm classificação «cuja classificação não teve conferência humana». Sem definir as perguntas, um leitor pode entender que os quatro pontos foram conferidos. Também falta dizer quem foi o leitor às cegas.
6. **§6 P8.** «os projetos com omissão quase não têm nenhum desses mecanismos» inclui constantes de *enum*, que a Tabela 6 não mede. A omissão só existe na extensão. O texto também diz «A orientação substitui a condicional pelo polimorfismo», mais forte que "compatíveis" do Resumo; a extensão não mede condicionais.
7. **§8.** «e distribui a complexidade por mais classes» convive com a queda da complexidade total no estudo principal, e não há soma de WMC por projeto. A evidência é só a maior classe e o número de tipos. Falta informar a unidade de «| complexidade cognitiva (média) | 33,7 |» (por método, classe ou projeto).
8. **§6 P3.** «o modelo pesa mais que a orientação nas outras dimensões» usa o contraste extremo entre o melhor modelo e o pior. «a orientação alcança o modelo» vale para 13 contra 11 só na manutenção; na construção é 13 contra 22.
9. **§5.1.** «o teto, já que cinco dos 11 modelos não tinham o que melhorar» motiva a extensão, mas um dos três modelos escolhidos é justamente um desses cinco, com 22 de 25 no N0 na construção. A escolha dos três modelos não é justificada.
10. **§7.** «o que preserva a comparação entre níveis» precisa de apoio: o estudo principal teve quartetos simultâneos e «em ordem sorteada», e a extensão não diz qual foi a ordem dos níveis nas até 10 horas.
11. **§5.2 contra §1.** O exagero da extensão («O verificador registra ainda o exagero, isto é, a estrutura sem necessidade fora da parte que o *pattern* resolve») mede outra coisa em relação ao exagero do estudo principal e ao de «o uso de um *pattern* onde ele não se justifica é chamado de exagero». O mesmo nome cobre três medidas, sem calibração na da extensão.
12. **§1.** «calibrado em casos de resposta conhecida» aparece como contribuição, sem as limitações que §7 lista. A contribuição fica maior do que a evidência.
13. **§6.** A Discussão não cita nenhum trabalho. Faltam comparações com Kim (2025), Kjellberg et al. (2026), Galster et al. (2026) e He et al. (2026, complexidade cognitiva).
14. **§4.1.** «O custo foi medido pelos *tokens* e pelo tempo», mas o artigo relata só dólares equivalentes. Não há custo da extensão.
15. **§5.2.** «com as mesmas regras de resumo» convive com «| CBO médio do projeto, mediana |» na Tabela 2 e «médias de 75 execuções por nível» na Tabela 5. Os valores não se comparam diretamente, e o produto CBO × tipos mistura mediana com média.
16. **§6 P6.** «arquitetura, testes e banco oscilam sem direção clara», mas «| banco | 0 | 0 | 0 | 65 | 62 | 66 |» mostra chão na construção. É uma dimensão sem variação, talvez porque o critério exige migrações não pedidas. Os testes na construção também não foram solicitados, então a dimensão mede algo fora do pedido.
17. **§7.** «O controle negativo do estudo principal reduz essa ameaça apenas em parte» não explica como um controle de exagero reduz a ameaça de repetição de memória.

# Editorial

- Linha de abertura e nota em itálico («Os números vêm dos» etc.) citam arquivos internos e devem sair da versão final.
- §4.1: «por escolha deste desenho» não se entende.
- §4.1: «os 11 modelos Claude disponíveis no Claude Code em 10/10/2026» seguido de «O Fable foi excluído»: há mais de 11 disponíveis.
- §1 chama P1 a P8 de objetivos específicos; §6 diz «retoma as perguntas de pesquisa».
- O termo "patterns" aparece às vezes em itálico, às vezes não (compare §2 e §3).
- Referências: Aniche deve vir antes de Anthropic. "Anthropic (2026)" reúne três documentos em uma entrada.
- Figura 4: a legenda "Em destaque, o que varia em cada estudo" destaca só a configuração, mas tarefa, *pattern* e modelo também variam.
- Figura 2: "o código existente não muda" ignora o ponto único de escolha que §2 menciona.
- Figura 3: a extensão não mostra as 90 execuções de controle.

# Avaliação metodológica

**Pontos fortes.** Pareamento em quartetos no estudo principal; regras de decisão fixadas antes; suíte verificada com versões defeituosas; registro de hipóteses contrariadas; desvios declarados; repositório público com *hash*; separação entre confirmatório e exploratório; teste de concordância de um segundo avaliador.

**Lacunas em relação a um relato de experimento:**

| Item | Situação |
|---|---|
| Unidade de análise e dependência entre pares | Não tratada: cinco pares por modelo (M6) |
| Hipóteses, regra por hipótese, resultado | Não enumeradas (M6) |
| Tamanho de efeito e incerteza | Ausentes na extensão; quase ausentes no principal (M11) |
| Multiplicidade | Vários testes por regra sem discussão |
| Aleatorização na extensão | Não descrita (Minor 10) |
| Cegamento | Declarado; o verificador do controle sabe que é controle (já em §7) |
| Validade de construto do tratamento | Parcial (M1) |
| Reprodutibilidade | Boa para o estudo principal (*commit*, versões, imagem). Falta o texto do N1 no próprio artigo |

# Teste de falsificação

O artigo enfrenta bem as hipóteses contrariadas (exagero N1 × N0, CBO, LCOM, duplicação) e não as reescreve. O ponto fraco é a passagem para a síntese: as contrariadas sobre custo desaparecem do Resumo e da Conclusão (M7), e a hipótese do modelo é confirmada com regra de desempate em um cenário favorecido por construção (M8). A extensão não tem hipótese congelada, portanto não pode ser falsificada no mesmo sentido. Mesmo assim, o caso que poderia contrariar a tese (Strategy na construção, 9 para 7) é reconhecido em §6, mas não limita a afirmação principal de §8 (M9).

Os critérios originais não foram lidos, por instrução; este teste julga apenas a coerência do que o artigo relata.

# Verificação disciplinar (reprodutibilidade)

- Origem dos dados e ferramentas com versão: sim, em boa parte (Claude Code, CK, commit).
- Disponibilidade de código e dados: repositório citado em §4.2; não há frase equivalente sobre os dados da extensão.
- Limitações do método: explícitas e extensas, embora parte delas não chegue às conclusões.

# Auditoria de citações

Sem rede e sem as fontes, nenhuma citação foi conferida no texto original. O método pede cinco verificações; as linhas abaixo apontam o que o autor deve conferir, e **não são verificações concluídas**. A escolha foi dirigida por relevância, e não sorteada.

| Citação | Afirmação | Fonte confirma? | Observação |
|---|---|---|---|
| Ernst e Baldassarre 2023 | Definição de pré-registro | Não verificado | O título trata de *registered reports*, que têm revisão do protocolo por pares. O procedimento aqui é autoarquivado; convém distinguir |
| Arcuri e Briand 2014 | Piso de dez repetições | Não verificado | De memória, o texto defende bem mais repetições quando possível e medidas de efeito; conferir a página |
| Hou et al. 2024 | 0,92% de 395 artigos | Não verificado | Conferir a porcentagem e o denominador |
| Galster et al. 2026 | *Skills* 5,5%, subagentes 4,6% | Não verificado | Conferir a base das porcentagens |
| Basili et al. 1996 | LCOM sem relação significativa com falhas | Não verificado | Conferir se a variante de LCOM é a mesma medida pelo CK |

Todas as citações do texto aparecem nas referências, e todas as referências são citadas.

# Avaliação da evidência (GRADE adaptado)

| Afirmação | Tipo | Nota | Razão |
|---|---|---|---|
| O arquivo aumentou o ponto do clube (67% para 93%) | Experimento pareado, pré-registrado | Moderada, neste serviço | Direção forte (14 × 0); um enunciado, um arquivo, sobreposição com o critério |
| O arquivo não piora a correção | Não-inferioridade | Moderada no conjunto; fraca por modelo | Haiku 4.5 piora em 5 de 5 |
| N1 não altera o exagero | Rara e em piso | Fraca | Um ponto de controle; base 0% |
| Mais ganho na manutenção | Exploratório | Fraca | Teto, instrumento diferente, sem incerteza |
| Orientações não causam aplicação indiscriminada | Controle exploratório | Fraca | A própria orientação pede contenção; teto; duas rodadas |
| Polimorfismo como mecanismo | Descritivo pós-resultados | Muito fraca | Em parte tautológico, como o artigo diz |

# Passe construtivo: o que mais fortaleceria o artigo

1. **O argumento mais importante e mais frágil é o da "decisão".** Uma frase que diga claramente que o estudo mede a resposta do agente a uma orientação escrita por quem conhece o critério protege o artigo melhor do que qualquer defesa.
2. **Um quadro de "o que cada estudo permite afirmar"** no início da Discussão, separando confirmado, sugerido e não sustentado, resolveria M7, M9 e M11 de uma vez.
3. **Uma tabela de hipóteses** (M6) é o acréscimo de maior retorno: dá valor real ao pré-registro.
4. **O N1 × N2 como medida de ruído** (M10) transforma uma falha do desenho em recurso para ler as tabelas descritivas.
5. **Os resultados negativos são a parte mais interessante:** Strategy sem reprodução, Haiku 4.5 piorando na correção e comportamento em queda. Dar destaque a eles aumenta a credibilidade.
6. **Uma frase sobre o sentido de "certo":** o estudo trata o *pattern* como resposta esperada, mas não mostra que ele é melhor aqui (§7 reconhece). A linguagem («acerto», «erros», «não acertam sozinhos») poderia refletir isso.

# Fora de escopo

Novas execuções, outras famílias de modelos e a medição do custo do próximo caso já constam como trabalhos futuros. Nenhuma das correções acima depende delas.

# Recomendação

**Revisão maior (major revision).** O estudo principal tem mérito para um bom evento brasileiro de engenharia de software e a extensão é útil como exploração, mas a versão atual não deve ser submetida. As frases de síntese afirmam mais do que os resultados sustentam em pontos verificáveis (M2 a M5, M7 a M9), e o escopo do construto precisa ser declarado (M1). M4, M5 e M12 são correções de texto rápidas. M6 e M14 exigem esclarecimento com base nos registros. Nenhum item exige novas execuções.
