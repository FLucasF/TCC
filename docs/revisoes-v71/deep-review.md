# Deep Review do artigo v71

**Artigo revisado:** `J:\TCC\docs\versoes\artigo-v71.md` e as seis figuras PNG de `J:\TCC\docs\figuras\`.
**Régua:** bom artigo de evento brasileiro de engenharia de software.
**Fonte da evidência:** só o texto e as figuras. Nada do repositório nem das revisões anteriores foi consultado.

## Como a revisão foi feita

O dono do artigo limitou o método do plugin, e estes desvios são intencionais:

- O pedido fez o papel da entrevista de calibração: público, régua, foco nas incoerências criadas pelas correções e lista do que já é conhecido.
- Não houve subagentes. As passadas foram feitas em sequência por um só revisor: aritmética das tabelas, regras de decisão, coerência entre seções, construto e medição, ameaças à validade, redação e figuras.
- A rodada de crítica foi autocrítica. Vários achados candidatos caíram nela, por exemplo a soma das tabelas e o aumento de 22% a 46%, que estão corretos.
- Não foram usados rede nem scripts do plugin. As citações foram julgadas só pela coerência interna, sem conferência das fontes.

Verificação computacional, com Python e só com números do próprio artigo. **Conferem:**

- as somas da Tabela 4 contra a Tabela 3 e a Figura 5;
- a Tabela 6 contra a Tabela 3 (154 e 110 execuções certas; 224 e 225 execuções);
- os +18 e +8 da Discussão;
- 294 de 360 repetições, nas duas decomposições;
- 195 de 210 notas na segunda revisão;
- 37 e 51 de 55 no ponto do clube;
- US$ 346 de custo;
- os intervalos de CBO vezes tipos.

Os totais estão consistentes. Os problemas abaixo são de interpretação, de regra e de coerência entre seções.

Os trechos entre « » são literais do artigo, sem os asteriscos de itálico e as crases.

## Avaliação geral

O artigo pretende mostrar que orientações persistentes aumentam o uso adequado de *patterns* não pedidos, com um experimento pré-registrado e uma extensão exploratória. O efeito principal (37 para 51 de 55) e o da manutenção (19 para 58 de 75) são sustentados pelos números, e o controle experimental e a franqueza sobre desvios estão acima do comum nessa régua. O maior obstáculo é que as frases de síntese, sobretudo em "Por fim" da Seção 6, no Resumo e na Conclusão, dizem mais do que as seções que elas resumem, e às vezes o contrário. Confiança alta nos achados de coerência e média nos que dependem de detalhes que o texto não informa.

## Problemas críticos

Nenhum problema encontrado invalida o resultado central do estudo principal. Limites residuais: as citações não foram conferidas, os dados brutos não foram vistos e as afirmações por modelo da extensão não podem ser checadas só pelo texto, porque não há tabela por modelo. Os achados S1 a S4 e a pergunta Q1 são os que mais se aproximam de crítico e devem ser resolvidos antes da submissão.

## Preocupações significativas

### S1. A síntese afirma convergência entre os estudos no ponto em que a Discussão diz que não houve

- **Local:** Seção 6, parágrafo "Por fim": «O aumento do pattern certo apareceu na mesma direção nos dois estudos». Seção 6, P1: «A extensão, portanto, não reproduz o efeito do estudo principal nesse ponto». Seção 8: «mostra que um arquivo de princípios aumenta o uso correto do padrão Strategy».
- **Problema:** o único *pattern* comum aos dois estudos é o Strategy. Na célula comparável ao resultado de destaque (Strategy, construção, N1 contra N0), a extensão vai na direção **oposta**: 9 para 7 em 15, ou 4 para 2 em 10 sem o Sonnet 5.5. A frase de "Por fim" apresenta a concordância como robustez, justamente "com enunciados e correções diferentes". O Resumo e a Conclusão registram só «a exceção do Strategy na construção», sem dizer que só com o arquivo houve queda. A comparação N0 a N2 do Resumo também usa um N2 que não é o mesmo tratamento nos dois estudos: no principal, *skill* pública nunca aberta; na extensão, *skill* própria, com instrução de uso.
- **Por que importa:** a replicação parcial é o argumento de força da extensão, e o enunciado mais direto dela falha.
- **Correção sugerida:** trocar por algo como "o aumento agregado apareceu nos dois estudos, mas o Strategy, único *pattern* comum, não se repetiu na construção". Pôr o 9, 7, 9 no Resumo ou na Conclusão. Limitar a Conclusão a "neste serviço". Em P1, a explicação «O enunciado e o domínio são outros» deve incluir também o *framework*, o banco e a forma de correção, que a Seção 4 reconhece terem mudado.
- **Confiança:** alta.

### S2. A Conclusão nega um aumento que os dados mostram e trata números iguais de modos diferentes

- **Local:** Seção 8: «não se observou aumento de estrutura desnecessária nos controles negativos». Mesma seção: «o exagero sobe de 4 para 9 execuções em 75». Seção 6: «coerentes com o risco indicado pela construção». Seção 5.3: «de 0, 1 e 2 na manutenção».
- **Problema:** no controle, as exceções vão de 0, 0 para 2, todas com a *skill*, e a própria Discussão as lê como coerentes com um risco. Nos cenários positivos da manutenção, o exagero também vai de 0 para 2. "Não se observou aumento" é falso na letra. A diferença de tratamento também não tem base: 4 para 9 em 75 "sobe" (Fisher exato, p = 0,25), e 0 para 2 em 30 "não aumenta" (p = 0,49). Nenhum dos dois é distinguível de ruído, e a ressalva cabe aos dois.
- **Por que importa:** é a frase final sobre o custo da orientação.
- **Correção sugerida:** "nos controles, 88 de 90 mantiveram a mudança pequena; as duas exceções ocorreram com a *skill*, número que não permite estimar o risco". Usar a mesma cautela em 4 para 9.
- **Confiança:** alta.

### S3. "O ponto mais difícil" é contradito pela Tabela 1

- **Local:** Seção 4.1: «Este último é o mais difícil». Resumo: «levou o ponto mais difícil do padrão Strategy». Seção 8: «no ponto mais difícil». Tabela 1: «ponto do pagamento certo (secundário)».
- **Problema:** no N0, que define a dificuldade, o pagamento acerta 55% e o cupom 65%, abaixo dos 67% do clube. No N1, o pagamento (91%) também fica abaixo do clube (93%). A justificativa do texto é estrutural («um mesmo caso afeta três regras»), mas o Resumo e a Conclusão a usam como fato empírico.
- **Por que importa:** é o adjetivo que qualifica o resultado principal, e o leitor o desmente na primeira tabela.
- **Correção sugerida:** "o ponto definido no pré-registro como medida principal" ou "o ponto estruturalmente mais complexo", e comentar que os secundários têm N0 mais baixo.
- **Confiança:** alta.

### S4. O exagero é "apoiado" no N3 com menos pares do que a regra descrita exige

- **Local:** Seção 4.1: «há ao menos 5 pares não empatados». Seção 4.3: «teve efeito apoiado no desenho» e, na mesma frase, o exagero «de 3 execuções em 55 para nenhuma».
- **Problema:** com 3 execuções no N2 e 0 no N3, há no máximo 3 pares não empatados, abaixo dos 5 da regra de alteração. Na comparação N1 contra N0, a mesma medida é julgada por essa regra («apenas 1 par em 55 apresentou diferença»). A única leitura que salva o "apoiado" é a regra direcional, aplicada a no máximo 3 modelos, mas o texto não diz isso. Além disso, "teto" está definido em termos de acertar, sem dizer o que significa para o exagero. O N0 do controle principal é 0%, o mesmo piso que a Discussão reconhece no controle da extensão, mas lá sem a ressalva.
- **Por que importa:** parece mudança de régua dentro do estudo pré-registrado, o que é justamente o que o pré-registro deveria afastar.
- **Correção sugerida:** declarar a regra de cada comparação do exagero e dizer quantos modelos decidiram. Se foi a direcional, dizer que a evidência é de poucos eventos.
- **Confiança:** média, porque o pré-registro não foi lido.

### S5. A Discussão afirma o efeito de um tratamento que ela mesma diz não ter sido aplicado

- **Local:** Seção 6, P2: «o CLAUDE.md e a skill não alteram o exagero». Seção 6, P4: «de modo que o tratamento não chegou a ser aplicado».
- **Problema:** a *skill* pública não foi aberta em nenhuma das 110 execuções. Não se pode dizer que ela não altera o exagero. Além disso, "não alteram" é mais forte que o «sem alteração detectada» do Resumo, com taxa entre 0% e 5% e um único ponto de medida.
- **Correção sugerida:** "o exagero não mudou de forma detectável do N0 ao N2, nível em que a *skill* não chegou a ser aberta".
- **Confiança:** alta.

### S6. "O efeito veio dos modelos que não acertavam" é aritmética, e uma das hipóteses apoiadas é quase mecânica

- **Local:** Resumo: «o efeito veio dos seis modelos que não acertavam sozinhos». Seção 4.1: «situação em que a orientação só pode empatar ou piorar» e «o que menos acerta no N0 deve ter o maior saldo de pares». Seção 6, P3: «o Haiku 4.5 tenha piorado nos cinco pares (saldo -5)». Resumo: «sem piorar a correção».
- **Problema:** há três pontos ligados.
  1. Quem acerta 5 de 5 não pode melhorar, então o efeito só pode vir dos outros. Isso não é achado.
  2. Quem começa em 0 pode ganhar +5, e quem começa em 3 só pode ganhar +2. A hipótese do modelo é favorecida pelo próprio teto, e um dos dois mais fracos teve só +1.
  3. O Haiku 4.5, o mais fraco, ganha 1 no desenho e piora em todos os 5 pares de correção. Ele responde por 5 dos 7 pares piores, e isso some do Resumo e da Conclusão. A contagem «quatro foram apoiadas» também reúne medidas pouco independentes: desenho, complexidade (que conta condicionais) e a hipótese do modelo, derivada do desenho.
- **Correção sugerida:** escrever "seis modelos tinham margem; todos os que tinham margem melhoraram ou empataram" e citar a piora do Haiku ao lado de «sem piorar a correção». Comentar o viés de teto da hipótese do modelo.
- **Confiança:** alta.

### S7. "Aumento maior na manutenção" reflete sobretudo um ponto de partida mais baixo

- **Local:** Resumo: «um aumento maior na manutenção do que na construção». Seção 6, P5: «58 em 75 no N2, contra 60».
- **Problema:** as duas tarefas chegam ao mesmo nível (58 e 60). A construção tinha 34 execuções de margem, e a manutenção, 56. A manutenção capturou 70% da margem no N2, e a construção 56%, diferença bem menor que 39 contra 19. Só com o arquivo a ordem se inverte: 25% contra 35%. O texto cita a diferença de redação do arquivo como confusão, mas não o efeito de piso.
- **Por que importa:** o título do artigo destaca a manutenção, e esta é a leitura que o leitor levará.
- **Correção sugerida:** dizer que "com a *skill*, as duas tarefas convergem (77% e 80% de soluções certas), a partir de pontos de partida muito diferentes".
- **Confiança:** alta.

### S8. O "pattern certo" inclui uma classe sem definição e sem calibração, e isso pesa no Decorator

- **Local:** Seção 2: «acerto é a variação inteira atrás de uma interface». Legenda da Tabela 3: «no pattern, acerto ou alternativa». Seção 7: «testou apenas acerto e omissão, sem malfeito nem alternativa equivalente». Seção 6, P4: «a forma literal do Decorator apareceu com ela». Seção 5.2: «o verificador recebe o enunciado, o pattern esperado e o projeto».
- **Problema:** "alternativa equivalente" soma 39 das 264 execuções certas e nunca é definida; aparece só por exemplos na Seção 5.3. Se a forma literal do Decorator nunca surgiu sem a *skill*, as 22 execuções "certas" de Decorator no N0 e no N1 não têm a forma literal. O maior ganho isolado só com o arquivo (Decorator na construção, de 4 para 10) mede então "variação isolada por lista de regras ou forma próxima", e não uso do Decorator. A fronteira entre acerto e malfeito, que decide 89 execuções, não foi calibrada, e o verificador sabe qual *pattern* procurar. "Acerto" também tem dois sentidos: a classe (126 e 99 na Tabela 6) e a soma com a alternativa (154 e 110).
- **Por que importa:** a pergunta do artigo é sobre aplicar *patterns*. Parte do efeito pode ser "resolver a variação" de outro modo, o que é defensável, mas precisa ser dito.
- **Correção sugerida:** definir o critério de equivalência na Seção 5.2. Separar acerto e alternativa ao menos no Decorator e no Template Method, com dados já existentes. Usar "solução adequada" para a soma.
- **Confiança:** alta na falta de definição; média na decomposição do Decorator.

### S9. A ameaça das orientações ajustadas é declarada só na extensão

- **Local:** Seção 7: «foram escritas por quem conhece as tarefas e o critério». Seção 4.2: «a bancada de testes acumulou 119 execuções». Figura 3, etapa 1: "Claude Code com e sem orientação".
- **Problema:** no estudo principal, o `CLAUDE.md` também foi escrito por quem conhecia o enunciado e os dois critérios do Semgrep, e a Figura 3 indica bancadas com e sem orientação antes do congelamento. O pré-registro protege a análise, e não a afinação do tratamento ao instrumento. A Seção 7 traz essa ameaça apenas para a extensão, e o estudo principal parece isento.
- **Por que importa:** atinge o resultado de destaque, 67% para 93%.
- **Correção sugerida:** dizer se o texto do N1 mudou ao longo das bancadas e estender a ameaça ao estudo principal.
- **Confiança:** média, pois o texto não diz se houve ajuste; o silêncio é o problema.

### S10. "Não favorece nenhum deles" não se sustenta

- **Local:** Seção 7: «como vale para os três níveis, ela não favorece nenhum deles». Seção 2: «É o critério que os textos dos cenários pressionam».
- **Problema:** se o `CLAUDE.md` orienta a isolar o que vai variar, a frase que anuncia crescimento é o gatilho dessa regra. Estar igual nos três níveis não impede que ela renda mais nos níveis com orientação. A frase foi acrescentada ao Observer, ao Decorator e ao Template Method, não ao Strategy, único *pattern* sem ganho na construção. Isso também confunde a atribuição do +18 às precisões, que a Seção 7 faz sozinha.
- **Correção sugerida:** trocar por "pode interagir com as orientações" e citar a frase ao lado das precisões como explicação alternativa.
- **Confiança:** alta.

### S11. "Exagero" designa três medidas diferentes, que o texto soma como se fossem uma

- **Local:** Introdução: «o uso de um pattern onde ele não se justifica é chamado de exagero». Seção 5.2: «a estrutura sem necessidade fora da parte que o pattern resolve» e «uma estrutura criada para a variação não solicitada». Seção 7: «a calibração não cobriu o design do restante do código nem o exagero».
- **Problema:** o estudo principal mede um *pattern* no ponto do seguro. O controle da extensão mede estrutura em pedidos pontuais. A nota da extensão exclui, por definição, a parte do *pattern* e não corresponde à definição da Introdução. A Discussão liga as três, e a Conclusão apoia o "sobe de 4 para 9" numa nota não calibrada, sem dizer isso.
- **Correção sugerida:** nomes diferentes, como "exagero no ponto de controle", "estrutura desnecessária no restante do código" e "falta de contenção", e a ressalva da calibração junto de 4 para 9.
- **Confiança:** alta.

### S12. A Seção 6 (P6) conclui mais do que a Tabela 3 permite

- **Local:** Seção 6, P6: «é nas dimensões ligadas a ela que os ganhos aparecem», «arquitetura, testes e banco oscilam sem direção clara», «O comportamento não varia na construção» e «de modo que o ganho em pattern veio acompanhado de alguma perda nessa dimensão». Seção 5.3: «O Sonnet 5.5 teve o comportamento avaliado como bom nas 150 execuções».
- **Problema:**
  - A arquitetura é uma dimensão estrutural e não melhora de modo claro (16, 9, 16 na construção), o que contradiz a frase final. O único outro ganho é o design, a nota não calibrada e de menor concordância (24 de 30, kappa 0,53).
  - Os testes na construção caem de forma monotônica (11, 9, 6), e não oscilam.
  - O "não varia" do comportamento é saturação: 25 por nível são exatamente as 25 do Sonnet 5.5. Daí se deduz que o Sonnet 4.5 também teve zero nas 75, mas a Seção 5.3 cita só o Haiku.
  - A queda de 61 para 54 (p = 0,25) é chamada de perda no corpo do texto e não aparece no Resumo nem na Conclusão, embora a Introdução prometa informar «sobre o custo que podem trazer».
- **Correção sugerida:** limitar a frase ao design, com a ressalva da calibração. Dizer que o comportamento na construção é determinado pelo modelo. Tratar a queda na manutenção com a mesma cautela e levá-la à Conclusão, ou dizer que não é distinguível de ruído.
- **Confiança:** alta.

### S13. Resumo e Conclusão não acompanharam as ressalvas do corpo

- **Local e problema:**
  - Resumo: «As métricas de orientação a objetos, descritivas e analisadas depois dos resultados» e a mesma frase na Seção 8. A Seção 4.1 diz: «apenas o CBO e a LCOM integram as hipóteses do pré-registro». O acoplamento citado na frase é justamente o CBO pré-registrado.
  - Resumo: «o acoplamento médio muda pouco», sem o aumento do total na Seção 6, P8: «entre cerca de 22% e 46% no estudo principal». Essa conta mostra que o CBO médio é diluído por classes novas e pequenas.
  - Resumo: «28 de 30 com o arquivo e a skill», embora a *skill* não tenha sido aberta «em nenhuma das 10 do Haiku 4.5». Das 20 em que foi aberta, 18 ficaram contidas, ou 90%.
  - O Resumo e a Conclusão não dizem o que a Seção 6 admite: «o controle avalia, portanto, a adesão a essa instrução». Essa cláusula do `CLAUDE.md` também não aparece na Seção 5.2.
  - O Resumo omite o exagero de 4 para 9, presente na Conclusão, e não diz que «A redação do CLAUDE.md difere entre as tarefas» ao comparar as tarefas, ressalva que a Conclusão faz.
- **Correção sugerida:** rever as duas seções como um par, com uma só lista de ressalvas.
- **Confiança:** alta.

### S14. As hipóteses pré-registradas não são legíveis no artigo

- **Local:** Seção 4.3: «Das cinco hipóteses principais», «o que contraria a hipótese de não repetir o comum». Seção 6, P2: «embora a hipótese correspondente tenha ficado inconclusiva».
- **Problema:** o texto define quatro tipos de regra, mas nunca lista as hipóteses, a regra de cada uma e o resultado. A hipótese da duplicação e a das classes surgem sem definição, e o resultado da segunda aparece só na Discussão. Para a complexidade, hipótese contínua apoiada, não se informa quantos dos 55 pares foram na mesma direção. Os limiares de 36, 7 e 5 também não têm justificativa. Para 36 de 55, a cauda binomial unilateral é 0,015, o que sugere uma origem que poderia ser dita.
- **Por que importa:** é a principal vantagem declarada do estudo.
- **Correção sugerida:** uma tabela compacta com hipótese, regra, contagem e decisão.
- **Confiança:** alta.

### S15. Uma só tarefa e uma só linguagem por estudo não aparecem como limitação

- **Local:** Seção 7: «impede separar o efeito da configuração do efeito do par formado pelo modelo e pelo agente». Seção 8: «mostra que um arquivo de princípios aumenta».
- **Problema:** o estudo principal tem um único enunciado, um único domínio e Java com Spring; a extensão, um único sistema e Java com Javalin. A Seção 7 discute fornecedor e agente, mas não a generalização entre tarefas, domínios e linguagens, justamente o fator que a Discussão usa para explicar a divergência do Strategy.
- **Correção sugerida:** acrescentar a ameaça e escrever a Conclusão no passado e no recorte estudado.
- **Confiança:** alta.

### S16. A comparação entre modelo e orientação usa os extremos e só um dos lados

- **Local:** Seção 6, P3: «o modelo pesa mais que a orientação nas outras dimensões» e «a orientação alcança o modelo». Seção 5.3: «e o Haiku 4.5, de 5 a 13 e de 3 a 13».
- **Problema:** comparar o melhor modelo sem orientação com o pior modelo com *skill* não mede o peso relativo. Serve para qualquer fator com extremos afastados. "A orientação alcança o modelo" se apoia em 13 contra 11 de 25 na manutenção. Na construção, o mesmo cruzamento dá 13 contra 22, o contrário, e não é citado.
- **Correção sugerida:** citar as duas tarefas e reduzir a afirmação, ou decompor o efeito com dados já existentes.
- **Confiança:** alta.

### S17. O ganho atribuído à *skill* reúne mais de uma mudança

- **Local:** Seção 6, P4: «a skill acrescenta mais (+25 acertos) do que o arquivo (+14)» e «o que decorre também da ordem cumulativa dos níveis». Seção 5.2: «foi deslocada para o primeiro item do arquivo».
- **Problema:** de N1 para N2 na extensão muda a presença da *skill*, a instrução imperativa de usá-la e, na manutenção, a posição dessa instrução. A ressalva da ordem cumulativa é feita só para o estudo principal. A frase também escolhe a manutenção: na construção, o arquivo acrescenta mais (+12) do que a *skill* (+7), como no estudo principal.
- **Correção sugerida:** chamar o N2 de "arquivo, *skill* e instrução de uso", citar as duas tarefas e estender a ressalva da ordem.
- **Confiança:** alta.

## Problemas menores

- **Resumo:** «de 67% para 93% das execuções corretas» dá a entender 93% entre as corretas. Basta "das execuções".
- **Resumo:** «de N0 a N2» aparece sem que os níveis tenham sido definidos.
- **Introdução e Seção 6:** a Introdução atribui P1 a P4 ao estudo principal («No estudo principal, busca-se verificar»), mas a Discussão responde a elas com a extensão. O controle negativo da extensão não tem pergunta própria.
- **Seção 6, P1:** o «portanto» de «A extensão, portanto, não reproduz» não decorre da frase anterior, que fala de diferenças de enunciado, e sim dos números 9, 7, 9.
- **Seção 6, P5:** «(58 em 75 no N2, contra 60)» fica solto; falta "na construção".
- **Seção 6, P2:** «O código torna-se, assim, menos complexo e maior» dispensa a ressalva que a Seção 7 faz à complexidade cognitiva. A linha «complexidade cognitiva (média)» da Tabela 1 não diz se é por projeto ou por método.
- **Seção 4.1:** «os 11 modelos Claude disponíveis» convive com «O Fable foi excluído». Melhor "11 dos modelos disponíveis".
- **Seção 4.1:** «por escolha deste desenho» está mal encaixado na frase da suíte.
- **Seção 4.3:** «o que muda é a cauda da distribuição» é uma leitura posterior a uma hipótese contrariada, e o corte de 37,2 é o próprio percentil 90 do N0. Marcar como exploratório.
- **Seção 4.3:** a frase «não piora a correção, medida pelos casos da suíte» carrega um parêntese longo no meio de uma enumeração.
- **Seção 5.1:** as precisões decorrem do Decorator («como "num método só" no Decorator»), mas a Seção 7 as aproxima também do Template Method. Falta dizer por quê. O texto da orientação principal não aparece nem em trecho curto; três linhas ajudariam a julgar a principal variável.
- **Seção 5.3:** «as métricas acompanham a leitura do verificador» é em parte circular, pois o verificador procura esses mecanismos. Na manutenção, o malfeito tem mais interfaces (0,77) que o acerto (0,61). A afirmação «quase não têm nenhum desses mecanismos» (P8) inclui *enums*, que a extensão não mede.
- **Seção 6, P7:** a estabilidade acompanha a taxa de acerto, pois um modelo em 25 de 25 repete por construção. «A variação acompanha o modelo» precisa dessa ressalva.
- **Seção 6, "Por fim":** o parágrafo termina em uma informação solta sobre onde a contenção foi testada, sem concluir.
- **Seção 7:** «Esse ganho, como registrado na Introdução» não tem antecedente; a frase anterior fala em facilitar mudanças.
- **Seção 1:** o pré-registro é definido citando um texto sobre *registered reports*, mecanismo mais forte, com revisão antes da coleta. O daqui é autorregistro em repositório próprio. Confiança média, pois a fonte não foi lida.
- **Seção 5.3:** não há medida de incerteza, embora a extensão interprete diferenças pequenas. Intervalos descritivos deixariam claro que 19 para 58 é sólido (p < 0,001) e que 41 para 53 (p = 0,06), 61 para 54 e 4 para 9 não o são.
- **Figuras 3 e 4:** não mostram as 90 execuções de controle, e a Figura 3 usa nomes internos (construction-v2, TCC_V1) não explicados.
- **Figura 2:** "o código existente não muda" conflita com «escolhida em um só lugar» da Seção 2, pois esse lugar muda a cada plano novo.
- **Cabeçalho:** o parágrafo em itálico «Rascunho de 23/10/2026» ainda é nota de trabalho e deve sair.

## Perguntas para o autor

**Q1. O piloto do N2 da manutenção faz parte das 450 execuções?** Seção 5.2: «depois de um piloto em que o Haiku 4.5 a abriu em apenas 1 de 5 execuções» e «o piloto (90)». As 5 execuções combinam com um Haiku por cenário no piloto de 90, que entra na conta das 450. Se foi o mesmo piloto e não foi refeito, a primeira rodada do N2 da manutenção usou outra configuração, e o 58 de 75 e o 69 de 75 misturam duas. Se foi outro piloto, basta dizer. Esta é a pergunta mais importante.

**Q2. Quais são «as quatro perguntas de desenho»?** Seção 4.3. As 176 respostas equivalem a 44 pacotes vezes 4 perguntas. Se forem os quatro pontos, contradizem «cuja classificação não teve conferência humana». Se forem dois critérios em dois pontos, basta dizer.

**Q3. Quem escreveu a regra de empate?** Seção 4.2: «O agente de IA que operou o ensaio teve acesso aos resultados». Seção 4.3: «fixada antes dos dados». Os dados do V4 já existiam em 09/10. Se esse agente participou da regra, "antes dos dados" vale para o V5, mas não para dados semelhantes.

**Q4. Por que esses três modelos?** Seção 5.2: «Os modelos são o Claude Haiku 4.5, o Sonnet 4.5 e o Sonnet 5.5». A Seção 5.1 cita o teto como motivação, mas o Sonnet 5.5 já estava no teto no estudo principal.

**Q5. A ordem dos níveis foi sorteada na extensão?** Seção 7: «o que preserva a comparação entre níveis». O estudo principal tratou a simultaneidade como controle; aqui são até 10 horas. Sem sorteio, "preserva" é forte.

**Q6. O Docker afetou mais alguns níveis?** Seção 7: «o que derrubou os testes de integração que dependiam dele». Quais notas de testes e cobertura mudaram, e em que níveis?

**Q7. O enunciado de construção pedia migrações?** Seção 6, P6: «nenhuma das 225 execuções aplicou migrações». Se não pedia, o zero na linha de banco mede algo que não foi solicitado, e a dimensão perde utilidade na comparação.

**Q8. O cegamento resiste ao código?** Seção 5.2: «às cegas quanto ao modelo e ao nível». Se a forma literal do Decorator só surge com a *skill*, o próprio código pode revelar o nível ao verificador.

## Veredito

**Aceitar com revisões maiores, sem necessidade de novas execuções.** O desenho e a transparência bastam para um bom artigo de evento brasileiro de engenharia de software, e o resultado principal sobrevive. Como TCC, o trabalho está em condição de aprovação depois dos ajustes. O texto ainda não deve ser submetido, porque as frases mais lidas (Resumo, "Por fim" e Conclusão) contêm afirmações que o corpo do artigo desmente.

**O que levaria a aceitar sem ressalvas:** corrigir S1 a S5 e S13, que são edições de texto; responder Q1 e Q2; incluir a tabela de hipóteses (S14); definir "alternativa equivalente" e separar acerto de alternativa no Decorator e no Template Method (S8), com dados já existentes.

**O que rebaixaria o veredito para revisão com nova análise ou rejeição:**

- Q1 confirmar que execuções com configuração diferente entraram nas 450 sem separação. Seria preciso refazer essa rodada ou excluí-la e recalcular.
- O `CLAUDE.md` do N1 ter sido ajustado nas bancadas olhando os critérios do Semgrep (S9), sem que isso seja declarado. O 67% para 93% precisaria ser reescrito como efeito de uma orientação afinada a esta tarefa.
- O "apoiado" do exagero no N3 não ter base em regra pré-registrada (S4). A contagem de hipóteses precisaria mudar.
- A decomposição do Decorator (S8) mostrar que quase todo o ganho sem *skill* está em alternativas. O argumento do "*pattern* certo" no N1 teria de virar "variação isolada".
