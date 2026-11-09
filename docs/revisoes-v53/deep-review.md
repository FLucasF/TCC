# Revisão do artigo-v53 pelo método Deep Review

Régua usada: bom artigo de evento brasileiro de engenharia de software. Leitura só do artigo e das figuras, como revisor externo. Os itens já conhecidos do autor não foram apontados, exceto quando o texto os trata de modo incoerente.

## Avaliação geral

O artigo quer mostrar que orientações persistentes aumentam o uso de *design patterns* por agentes, sem custo relevante, e que o efeito se estende a cinco *patterns* e é mais forte na manutenção. O estudo principal sustenta uma afirmação mais estreita do que a escrita, e é transparente acima da média; a extensão sustenta bem a descrição, mas não três das frases comparativas que o Resumo e a Conclusão tiram dela. O maior obstáculo é que a própria Tabela 4 mostra que o efeito do arquivo sozinho não se repete no Strategy e se concentra nos *patterns* cujo arquivo foi ajustado depois de ver dados. Confiança alta nas contas e média nas causas, porque não consultei os dados nem os textos dos instrumentos.

**Veredito: revisão maior.** Detalhes no fim do relatório.

## Problemas críticos

### 1. A extensão não reproduz o achado pré-registrado no único ponto comparável, e "a mesma direção" encobre isso

- **Local:** Resumo, “mostrou a mesma direção em nove das dez combinações”; Seção 6 (P1), “de modo que restam 10 execuções por nível para evidenciar efeito”; Seção 6 (último parágrafo), “O aumento do pattern certo apareceu na mesma direção nos dois estudos”; Seção 8, “mostra a mesma direção em nove das dez combinações”; Tabela 4.
- **Problema:** o resultado principal é que o arquivo sozinho (N1) melhora o Strategy. Na Tabela 4, o Strategy com o arquivo sozinho vai de 15 para 14 acertos em 30 (construir 9→7; manter 6→7). A explicação pelo teto não funciona: sem as 5 execuções do Sonnet 5.5, restam 4, 2 e 4 acertos em 10 no N0, N1 e N2. Havia espaço para subir, e o N1 caiu. A contagem de "nove das dez" compara o N0 com o N2 e conta como direção diferenças de +2 em 15 (Observer e Factory Method na construção). Com 15 execuções, o intervalo de Wilson de 95% de 9/15 vai de 36% a 80%, e o de 7/15, de 25% a 70%. Além disso, os dois N1 e os dois N2 não são o mesmo tratamento: o arquivo recebeu duas precisões, a *skill* foi trocada e passou a haver instrução de uso.
- **Por que importa:** a frase de "mesma direção" é usada como evidência convergente logo depois de o texto admitir as mudanças feitas após ver dados. Ela é o elo entre o estudo com pré-registro e o estudo exploratório, e o único ponto em que os dois medem a mesma coisa não confirma.
- **Correção sugerida:** dizer no Resumo e na Conclusão que o efeito do arquivo sobre o Strategy não se repetiu na extensão. Dar a contagem N0→N1 separada da N0→N2. Retirar a explicação do teto ou mostrar por que 4→2→4 em 10 é compatível com ela. Trocar "mesma direção" por uma descrição que distinga ganhos grandes de oscilações de uma ou duas execuções.
- **Confiança:** alta.

### 2. O ganho "só com o arquivo" concentra-se nos dois *patterns* em que o arquivo foi ajustado depois de ver dados

- **Local:** Resumo, “só com o arquivo, de 19 para 33, contra 41 para 53”; Seção 5.1, “o que levou o CLAUDE.md a receber duas precisões”; Seção 7, “aproximam-se mais de dois cenários, o Decorator e o Template Method”; Seção 7, “como vale para os três níveis, ela não favorece nenhum deles”.
- **Problema:** somando a Tabela 4, do N0 ao N1 Decorator e Template Method vão de 14 para 32 em 60 (+18), e os outros três, de 46 para 54 em 90 (+8). Na construção, +11 dos +12 do N1 vêm desses dois; Strategy, Observer e Factory Method juntos vão de 32 para 33 em 45. A Seção 7 reconhece o viés em palavras, mas o texto nunca mostra que a maior parte do efeito está justamente nesses cenários, e o Resumo apresenta o número como efeito do arquivo. Há também um erro de lógica: uma frase igual nos três níveis não fica neutra quando pode interagir com o tratamento. Uma frase dizendo que a variação vai crescer pode ser o gatilho do princípio do arquivo, e favorecer o N1 e o N2 mais do que o N0.
- **Por que importa:** nos três cenários de construção sem esse ajuste no arquivo, o arquivo sozinho praticamente não teve efeito. A leitura honesta do dado é outra, e é mais fraca do que a do Resumo.
- **Correção sugerida:** apresentar a contagem separada entre cenários ajustados e não ajustados, no Resumo inclusive. Se houver orçamento, rodar o Decorator e o Template Method no N1 com o arquivo anterior às precisões. Retirar a afirmação de neutralidade da frase.
- **Confiança:** alta nas contas, média na causa.

### 3. "Mais forte na manutenção" só aparece no N2, que parece ser diferente entre as tarefas, e a comparação de tarefas não isola a tarefa

- **Local:** Resumo, “um efeito mais forte quando o agente muda um código pronto”; Seção 6 (P5), “os dados indicam efeito maior na manutenção”; Seção 8, “o efeito é mais forte quando o agente muda um código pronto”; Seção 6 (último parágrafo), “a linha da skill no N2 da manutenção”.
- **Problema:** com o arquivo sozinho, o ganho é +14 na manutenção e +12 na construção. A razão de chances é 2,3 contra 2,0, e a fração do espaço disponível que foi ganha é 25% contra 35%, ou seja, menor na manutenção. A diferença vem só do N2 (+39 contra +19; razão de chances 10,1 contra 3,3), e o N2 termina quase igual nas duas tarefas (58 e 60): o que difere é o ponto de partida. E o N2 da manutenção teve uma "linha da *skill*", citada só no último parágrafo da Seção 6, acrescentada depois de ver execuções e nunca descrita na Seção 5.2. As duas tarefas também diferem em tudo mais: sistema inteiro contra mudança focada, e uma base sem nenhum *pattern* escrita para o estudo.
- **Por que importa:** é um dos dois resultados de destaque da extensão e aparece sem ressalva na Conclusão. O que os dados mostram é que a manutenção parte de mais baixo e que a *skill* com instrução alcança o mesmo patamar da construção.
- **Correção sugerida:** reescrever como "a manutenção parte de um patamar mais baixo; com a *skill* e a instrução de usá-la, atinge o da construção". Descrever a linha na Seção 5.2 e dizer se ela existe também na construção. Não usar "efeito mais forte" sem medida relativa e sem separar N1 de N2.
- **Confiança:** alta nas contas, média no alcance da "linha", cujo texto o artigo não dá.

## Preocupações significativas

### 1. O tratamento principal não é mostrado, e pode coincidir com o critério de correção

- **Local:** Seção 4.1, “com princípios gerais, sem nomear patterns”; Seção 5.1, “o próximo caso num lugar só”; Seção 4.1, “cada caso isolado e nenhuma condição sobre o caso no cálculo”; Seção 3, “de modo que mede a decisão de usá-lo”; Seção 7, “Na extensão, as orientações”.
- **Problema:** o artigo não traz o `CLAUDE.md`, nem um trecho, nem o tamanho, nem o link direto. O único princípio citado é quase a paráfrase do critério do Semgrep. O autor conhecia o critério e já tinha 119 execuções de bancada ao escrever o arquivo do estudo principal, mas a Seção 7 declara essa ameaça só para a extensão.
- **Por que importa:** o resultado pode ser obediência a uma instrução estrutural explícita, e não a "decisão" que a Seção 3 diz medir. Sem o texto, o leitor não pode julgar nem replicar.
- **Correção sugerida:** incluir o arquivo no artigo ou em apêndice, comparar suas frases com os dois critérios, declarar a ameaça também no estudo principal e suavizar "mede a decisão".
- **Confiança:** alta na omissão, média na sobreposição.

### 2. Só o ponto do clube é relatado, e os outros números indicam que o efeito não se limita a ele

- **Local:** Seção 4.1, “quatro pontos em que a variação pede o padrão Strategy”; Seção 4.3, “as quatro perguntas de desenho”; Resumo, “o ponto mais difícil”; Seção 4.3, “O efeito decorreu dos modelos fora do teto”.
- **Problema:** frete, cupom e pagamento não têm resultado. O texto não diz se o clube era o desfecho primário no pré-registro, nem mostra que ele é o mais difícil. Além disso, a complexidade média cai 10,8 por execução: se a queda viesse só das 14 execuções que passaram a acertar o clube, cada uma teria de cair cerca de 42, acima da média do N0 (33,7). E a mediana do WMC da maior classe cai de 35 para 24, o que exige mudança em boa parte dos projetos, não em 14 de 55.
- **Por que importa:** a leitura de que o efeito é só dos modelos fora do teto vale para o clube, não para a estrutura. A omissão dos outros pontos também abre a suspeita de relato seletivo, mesmo que não seja o caso.
- **Correção sugerida:** acrescentar uma linha por ponto na Tabela 1, dizer qual era o desfecho primário e separar por modelos no teto e fora dele as métricas de complexidade e de maior classe.
- **Confiança:** alta na omissão, média na inferência, que depende da unidade da complexidade.

### 3. "Sem piorar a correção" esconde quedas, e o critério de "direção" é assimétrico

- **Local:** Resumo, “sem aumentar o exagero, sem piorar a correção”; Seção 6 (P3), “o Haiku 4.5 tenha piorado nos cinco pares (saldo -5)”; Seção 6 (P6), “oscilam sem direção clara”; Tabela 3.
- **Problema:** o Haiku 4.5, um dos seis modelos de onde o Resumo diz vir o efeito, ganha +1 no clube e piora nos cinco pares de correção; o Resumo não diz isso. Na extensão, o comportamento na manutenção desce de forma monotônica (61, 59, 54) justamente onde o ganho de *pattern* é maior, e os testes na construção também (11, 9, 6). Isso é chamado de oscilação, enquanto +2 em 15 conta como direção positiva.
- **Por que importa:** a mensagem de ausência de custo depende dessa assimetria. A correção é o custo que mais interessa a quem escreve orientações.
- **Correção sugerida:** ressalvar o Haiku 4.5 no Resumo e aplicar um mesmo critério de direção a ganhos e perdas, discutindo a queda de comportamento na manutenção.
- **Confiança:** alta.

### 4. O denominador 88 de 90 inclui execuções sem orientação

- **Local:** Seção 6, “as mesmas orientações que elevaram o pattern certo de 19 para 58 execuções em 75 quase não produziram estrutura onde ela não cabia, pois 88 das 90 execuções”; Seção 8, “os controles negativos indicam que a orientação não leva o agente a aplicar pattern onde ele não cabe: 88 de 90”; Seção 5.3, “A skill foi aberta nas 20 execuções”.
- **Problema:** 30 das 90 execuções são do N0, sem orientação nenhuma. As "mesmas orientações" que levaram a 58 em 75 são as do N2, que tem 28 de 30. Com a *skill* de fato aberta, são 18 de 20 (Wilson de 95%: 70% a 97%); no Sonnet 5.5 com a *skill*, 8 de 10; e, no cenário da temporada desse modelo, o exagero ocorreu nas 2 de 2 rodadas, o que sugere algo sistemático.
- **Por que importa:** o número conforta mais do que o dado permite, e está na Conclusão.
- **Correção sugerida:** usar 28 de 30 e 18 de 20 no texto, mantendo o total só como descrição.
- **Confiança:** alta.

### 5. Os controles negativos estão no piso e nada mostra que seriam capazes de detectar exagero

- **Local:** Seção 5.2, “um percentual e um período, um texto, um campo, uma assinatura ou uma coluna”; Seção 5.2, “mas por enquanto é só o e-mail”; Seção 6, “Esse resultado afasta a explicação”; Seção 1, “o uso de um pattern onde ele não se justifica”; Seção 5.2, “a estrutura sem necessidade fora da parte que o pattern resolve”; Seção 4.1, “de alteração, sem direção”.
- **Problema:** 0% no N0 do estudo principal e 30 de 30 no N0 e no N1 da extensão: as tarefas são tão pequenas que ninguém faria estrutura ali. Não há controle positivo do instrumento, como uma condição que mande aplicar *patterns* sempre. Os cenários positivos e os de controle diferem também no texto ("vai crescer" contra "por enquanto é só"), de modo que a discriminação pode ser leitura da pista do enunciado. "Exagero" tem três definições sob o mesmo nome. E a regra "de alteração" não é explicada: 1 par em 55 é "contrariada", e 3 em 55 é "apoiada".
- **Por que importa:** "sem aumentar o exagero" vem de um único ponto no piso, e é a metade do argumento de que a orientação não tem custo.
- **Correção sugerida:** declarar a sensibilidade não demonstrada, dar nomes diferentes às três medidas e explicar o limiar da regra. Um controle menos trivial, com duas ou três variantes estáveis, ajudaria.
- **Confiança:** alta.

### 6. "Pattern certo" conta soluções que não são o *pattern*, e "calibrado" é mais amplo do que a calibração

- **Local:** legenda da Tabela 3, “no pattern, acerto ou alternativa”; Seção 6 (P4), “a forma literal do Decorator apareceu com ela”; Resumo, “um verificador de qualidade calibrado”; Seção 5.2, “As 52 notas esperadas conferiram”; Seção 7, “depois de uma primeira rodada com 50 de 52”; Seção 7, “sem malfeito nem alternativa equivalente”.
- **Problema:** se a forma literal do Decorator nunca apareceu sem a *skill*, os ganhos de 4 para 10 na construção são todos "alternativa" (uma lista de regras), classe que nunca foi calibrada. Pela Tabela 6, 128 das 449 execuções (39 de alternativa e 89 de malfeito, 28,5%) estão em classes sem calibração, e é nessas fronteiras que o desfecho binário se decide. O 52 de 52 foi obtido depois de ajustes no mesmo conjunto de casos, o que a Seção 5.2 não diz.
- **Por que importa:** a limitação do verificador foi declarada, mas o Resumo, a contribuição da Introdução e a Seção 5.2 a tratam de modo incoerente com a Seção 7.
- **Correção sugerida:** escrever "calibrado para acerto e omissão, em casos conhecidos"; dar em separado os números de acerto e de alternativa; mencionar o 50 de 52 na Seção 5.2.
- **Confiança:** alta.

### 7. Falta a classe do *pattern* por nível, e "certo" é uma escolha normativa

- **Local:** Tabela 6; Seção 6 (P5), “o agente sem orientação tende a acrescentar mais uma condição”; Seção 6 (P7), “Alguns erros são constantes”; Seção 7, “e não se ele facilita as mudanças seguintes”; Resumo, “das execuções corretas”.
- **Problema:** a Tabela 6 mistura os níveis, e o malfeito é 60 de 225 na manutenção. Se a orientação transforma omissão em malfeito (interface mais condicionais), isso é custo, e nem o "exagero" nem o "pattern certo" o captam. Não se sabe também quantos não acertos do N0 são de fato "mais uma condição". Quanto à norma, o Sonnet 5.5, o modelo mais forte, zera o Factory Method na manutenção sem orientação, enquanto os outros dois fazem 6 de 10. O texto chama isso de erro, mas uma extensão mínima pode ser defensável, e a própria Introdução lembra que o ganho do *pattern* é incerto.
- **Por que importa:** o desfecho do estudo é "fez o que o autor esperava", e o texto o lê como "fez o certo".
- **Correção sugerida:** cruzar classe e nível numa tabela e usar "*pattern* esperado" no lugar de "certo", "corretas" e "erros".
- **Confiança:** alta na falta da tabela, média na interpretação do Sonnet 5.5.

### 8. "O `CLAUDE.md` responde pela maior parte do efeito" é efeito da ordem dos níveis

- **Local:** Seção 6 (P4), “responde pela maior parte do efeito”; Seção 6 (P4), “matizes trazidos pela extensão”; Seção 1, “como cada componente da configuração contribui para ele”.
- **Problema:** os níveis são cumulativos e em ordem fixa, e o primeiro componente consome o espaço disponível (dez dos 11 modelos no teto já no N1). Na manutenção da extensão ocorre o inverso: a *skill* soma +25 e o arquivo, +14. Isso é mais que um matiz. Não há condição só com *skill*.
- **Por que importa:** a P4 pergunta sobre a contribuição de cada componente, e o desenho só informa o acréscimo de cada um nessa ordem.
- **Correção sugerida:** responder em termos de "acréscimo sobre o nível anterior" e registrar a inversão na manutenção.
- **Confiança:** alta.

### 9. A *skill* não foi aplicada no estudo principal, e a adesão na extensão não é relatada

- **Local:** Seção 4.3, “pois dez dos 11 modelos já estavam no teto no N1”; Seção 4.3, “não foi aberta espontaneamente em nenhuma das 110 execuções”; Seção 6 (P3), “o Haiku 4.5 com a skill (13 em 25)”; Seção 5.2, “o N2 (o CLAUDE.md e a skill)”.
- **Problema:** com 0 de 110 aberturas, o N2 do estudo principal não testou a *skill*, e essa é a razão principal da inconclusão, mais do que o teto. O N1 contra o N2 funciona quase como um teste A/A e dá a medida do ruído: suíte 84%→80%, exagero 2%→5%, tipos 27,9→30,2, *enums* com corpo 4,0→2,7. A queda de 3 para 0 no N3, "apoiada", é desse mesmo tamanho. Na extensão, a abertura só é dada para os controles, em que o Haiku 4.5 abriu 0 de 10; "com a *skill*" pode não descrever o que ele fez nos 450. E o rótulo N2 designa tratamentos diferentes nos dois estudos, sem que a instrução de uso seja descrita na Seção 5.2.
- **Por que importa:** atribui-se à *skill* o que pode vir da instrução, ou de nada.
- **Correção sugerida:** tabela de abertura por modelo, tarefa e nível, resultados também por abertura, e rótulos distintos para os dois N2.
- **Confiança:** alta.

### 10. O "mecanismo" redescreve o desfecho, e o Resumo omite o aumento total de dependências

- **Local:** Resumo, “sugerem o mecanismo”; Seção 6 (P8), “A orientação substitui a condicional pelo polimorfismo”; Seção 5.3, “as métricas acompanham a leitura do verificador”; Seção 6 (P8), “entre cerca de 22% e 46% no estudo principal”; Seção 2, “a variação inteira atrás de uma interface”.
- **Problema:** acerto é definido por interface e omissão por condicional, de modo que a Tabela 6 é em parte circular. No N2, o Template Method e o Factory Method respondem por 91% (construção) e 98% (manutenção) dos métodos abstratos, e esses *patterns* são definidos por eles. A extensão não mede condicionais. E isso diz o que mudou no código, não por que o agente mudou. Quanto ao acoplamento, o Resumo e a Conclusão dão só "muda pouco"; o aumento de 22% a 46% fica na Seção 6, embora a média por classe seja diluída pelas classes novas. A cauda da LCOM é análise posterior e decorre em parte mecanicamente de repartir classes grandes, pois essa LCOM cresce com o número de pares de métodos.
- **Por que importa:** "mecanismo" é uma palavra forte para uma análise descritiva, posterior e parcialmente circular.
- **Correção sugerida:** trocar por "assinatura estrutural" e levar ao Resumo o aumento total de dependências.
- **Confiança:** alta.

### 11. A proteção do pré-registro é apresentada como maior do que foi, e a hipótese do modelo é frágil

- **Local:** Seção 4.2, “o autor viu apenas informações operacionais, sem a direção de nenhum efeito”; Seção 7, “em que foi a métrica que mais mudou com a configuração”; Seção 4.3, “no código e no registro de decisões de 09/10”; Seção 4.1, “situação em que a orientação só pode empatar ou piorar”; Seção 4.3, “O efeito decorreu dos modelos fora do teto”.
- **Problema:** a frase da Seção 4.2 vale para o V4, mas a Seção 7 mostra que o autor conhecia efeitos das fases anteriores, e a Figura 3 diz que elas tinham orientação e ausência de orientação. O agente que viu o V4 teve contato com os resultados, e o texto não diz se ele participou da análise congelada. A regra de empate decide a hipótese do modelo e é de um dia antes do congelamento. Além disso, a hipótese favorece a si mesma: quem está no teto não pode ter saldo positivo, e o outro modelo empatado só teve +1. "O efeito decorreu dos modelos fora do teto" é, assim, consequência da definição, como a própria Seção 4.1 mostra.
- **Por que importa:** o pré-registro é o principal diferencial do artigo e precisa ser descrito sem exagero.
- **Correção sugerida:** dizer o que o autor sabia de cada fase, onde está a regra de empate e quem a escreveu; apresentar o resultado do Haiku 4.5 como contrário à ideia geral; trocar a frase do teto por "nenhum modelo no teto piorou".
- **Confiança:** média.

### 12. O tratamento estatístico é inconsistente entre os estudos

- **Local:** Seção 7, “por isso, a extensão descreve as diferenças, sem teste de significância”; Seção 4.3, “teste de Page, p = 0,007”; Seção 4.1, “não passa de 7 em 55”; Seção 8, “o efeito é mais forte”.
- **Problema:** o motivo dado (cinco rodadas) vale igualmente para o estudo principal, que também tem cinco réplicas e publica teste. A extensão se diz descritiva, mas a Conclusão fala em efeito e em "mostra". Não há intervalo de confiança em nenhuma tabela, e a margem de 7 em 55 não é justificada. Os 55 pares são 11 modelos repetidos 5 vezes, não 55 observações independentes.
- **Por que importa:** o leitor não consegue saber o tamanho da incerteza, e a escolha parece conveniente.
- **Correção sugerida:** dar intervalos nas proporções principais, justificar a margem, tratar a limitação das cinco réplicas nos dois estudos e usar "observou-se" na extensão.
- **Confiança:** alta.

### 13. As generalizações e a escolha dos modelos vão além do desenho

- **Local:** Seção 8, “aumenta o uso correto do padrão Strategy”; Seção 6 (P3), “o modelo pesa mais que a orientação nas outras dimensões”; Seção 5.1, “cinco dos 11 modelos não tinham o que melhorar”; Seção 5.2, “o Claude Haiku 4.5, o Sonnet 4.5 e o Sonnet 5.5”.
- **Problema:** o estudo principal tem um serviço e um ponto medido. A P3 trata de modelos, mas não há tabela por modelo em nenhum dos estudos, e o "pesa mais" é sustentado por uma comparação de extremos (o melhor sem orientação contra o pior com a *skill*). O critério de escolha dos três modelos não é dado: o teto é citado como motivo, mas o Sonnet 5.5, que estava no teto, foi mantido, e o Opus 4.7, de maior ganho, ficou de fora.
- **Por que importa:** limita o alcance da resposta à P3 e a comparação entre os estudos.
- **Correção sugerida:** tabela por modelo, justificativa da escolha e Conclusão restrita ao ponto medido.
- **Confiança:** alta.

### 14. A resposta à P6 usa as dimensões menos informativas

- **Local:** Seção 6 (P6), “O design do restante do código acompanha a orientação”; Seção 6 (P6), “A configuração versa sobre estrutura, e é nela que atua”; Seção 7, “a calibração não cobriu o design do restante do código nem o exagero”; Seção 7, “kappa de Cohen de 0,53, o mais baixo”; Seção 5.2, “sem que os testes sejam solicitados”.
- **Problema:** a única outra dimensão dita responsiva é a menos calibrada e a de menor concordância; o verificador conhece o *pattern* esperado, o que pode contaminar essa nota. As dimensões ditas insensíveis, por sua vez, quase não podiam responder na construção: banco é 0 nas 225; testes não foram pedidos; e o comportamento bom são exatamente as 75 do Sonnet 5.5 (por subtração, o Sonnet 4.5 faz 0 de 75, o que o texto não diz). O 4→9 de exagero na Conclusão também vem de uma medida sem calibração.
- **Por que importa:** "é nela que atua" é uma conclusão sobre especificidade que o instrumento não permite tirar.
- **Correção sugerida:** retirar a frase, marcar a nota de design como de baixa confiabilidade e dizer que banco e comportamento na construção separam modelos, não níveis.
- **Confiança:** alta.

## Problemas menores

- Seção 5.2, “com dois ajustes de redação feitos antes das execuções”, e Seção 7, “depois de conhecidos os resultados dos cenários com pattern”: as duas frases são verdadeiras, mas a primeira esconde a segunda; juntar.
- Seção 4.1, “os 11 modelos Claude disponíveis”, e “O Fable foi excluído”: não eram todos os disponíveis.
- Seção 5.2, “com as mesmas regras de resumo”: a Tabela 2 usa medianas e a Tabela 5, médias, para WMC e CBO; a Seção 6 lê as duas lado a lado.
- Seção 2 e Seção 4.1: o RFC é definido e medido, mas não aparece em nenhuma tabela.
- Resultados que só aparecem na Discussão: “de 1,2% para 2,5% das linhas”; “89 no Haiku 4.5, 96 no Sonnet 4.5 e 109 no Sonnet 5.5”. Mover para as Seções 4.3 e 5.3.
- Falta um quadro de todas as hipóteses com a regra e a decisão; parte delas só surge na Seção 6, como “a hipótese de não repetir o comum contrariada”.
- "Acerto" tem dois sentidos: classe na Tabela 6 e acerto mais alternativa em “de 19 para 58 acertos em 75”.
- Seção 1, “um verificador de qualidade em cinco dimensões”; a Tabela 3 tem seis linhas e a Seção 5.2 fala em “sete notas por execução”. E “Cada dimensão recebe a nota bom, com problemas ou ruim” não vale para a classe do *pattern*, que tem quatro valores.
- Termos sem definição: “saldo de pares”, “220 pacotes”, “O pré-registro do V5”, “(62 até 06/10)”, “com esforço medium”, “em equivalente de API”, “por escolha deste desenho”.
- Seção 4.3, “foi contrariada, no sentido de que a configuração não o altera”: difícil de ler, pois a hipótese contrariada é a favorável ao autor; reformular.
- Seção 5.1, “como "num método só" no Decorator”: o leitor não entende o problema; as “duas precisões” nunca são enunciadas.
- Tabela 2, “classes anônimas”: a linha soma classes anônimas, e o texto a lê só como *enum*. Seção 6 (P8), “quase não têm nenhum desses mecanismos”: a Tabela 6 não tem *enums*.
- Seção 7, “o que preserva a comparação entre níveis”: até 10 horas de intervalo não equivalem à simultaneidade que a Seção 4.1 trata como virtude; dizer também se a ordem dos níveis foi sorteada.
- Seção 1, “sobre o custo que podem trazer”: o custo em dólares da Tabela 1 (+15% no N1, +50% no N3) não é discutido, e o custo da extensão não é dado. A Seção 4.1 diz “pelos tokens e pelo tempo”, mas a tabela traz dólares.
- Seção 1, “Os objetivos específicos são os seguintes”: P1 a P8 são objetivos ali e "perguntas" na Seção 6.
- Figura 2, “o código existente não muda”: o ponto de escolha muda, como a própria Seção 2 admite em “escolhida em um só lugar”.
- Figura 3: nomes internos como `construction-v7` e `TCC_V1` não significam nada para o leitor. Figura 4: “Em destaque, o que varia em cada estudo” marca só a configuração, embora tarefa e *pattern* também variem, e omite as 90 execuções de controle.
- Nota inicial, “Rascunho de 23/10/2026”, com menção a arquivos internos: retirar. Seção 2 ainda usa rótulos em negrito e *pattern* sem itálico, o que destoa das Seções 4 a 7.
- Redação: “A extensão recomenda cautela” (quem recomenda é o autor); “a direção não é a de ausência de custo” (preferir "há indício de custo").

## Perguntas aos autores

1. Seção 4.3, “três a mais melhoraram do que pioraram”: a Tabela 1 vai de 71% a 84%, ou seja, +7 execuções passando na suíte inteira. Qual é a medida do par? Se for o número de casos, é preciso dizê-lo; a resposta resolve a aparente diferença entre 3 e 7.
2. A regra de empate de 09/10 estava no texto congelado? Foi escrita depois do V4? O V4 tinha empate entre os modelos mais fracos?
3. O que diz “a linha da skill no N2 da manutenção”, onde fica, e existe na construção? A resposta decide o alcance do problema crítico 3.
4. Qual foi a taxa de abertura da *skill* por modelo e tarefa nas 450 execuções?
5. O ponto do clube era o desfecho primário no pré-registro? Quais os resultados dos outros três pontos?
6. Seção 4.2, “o que permite verificar a data do registro”: a data de um *commit* pode ser definida por quem o faz. Há evidência externa, como o evento de *push* ou um arquivamento de terceiro?
7. A complexidade cognitiva da Tabela 1 é a soma por projeto? A inferência da preocupação 2 depende disso.
8. Na nota inicial, “preprints ficam de fora”; o corpo do texto não diz isso nem dá o protocolo de busca, e a novidade em “nenhum dos trabalhos lidos” depende dele. Até onde me lembro, há preprints de 2026 que medem o efeito de arquivos de contexto sobre agentes; não verifiquei, por estar sem rede. O filtro é defensável, mas deve ser declarado e o trabalho mais próximo, ao menos mencionado.

## Veredito

**Revisão maior.** O estudo principal está acima da média dos artigos de evento brasileiro em transparência e é aproveitável com uma redação mais estreita. A extensão é útil como descrição, mas o Resumo e a Conclusão tiram dela três comparações que a Tabela 4 não sustenta. Quase toda a correção é de redação e de tabelas já possíveis com os dados existentes, sem novas execuções.

**O que levaria a aceitar:** reescrever o Resumo, a Seção 6 e a Conclusão quanto aos problemas críticos 1 a 3 e às preocupações 3 e 4; incluir o texto do arquivo; dar os outros três pontos do estudo principal, a tabela por modelo, a tabela de classe por nível e a taxa de abertura da *skill*; descrever a "linha da *skill*". Se houver orçamento, rodar o N1 do Decorator e do Template Method com o arquivo anterior às precisões.

**O que levaria a recusar:** o arquivo repetir em essência o critério do Semgrep, o que tornaria o resultado principal uma medida de obediência; os outros três pontos não mostrarem efeito, sem isso ter sido dito; a regra de empate ter sido escrita conhecendo o V4; ou a "linha da *skill*" existir só na manutenção sem que se possa separar seu efeito.

## Limites desta revisão e verificações feitas

- O método foi adaptado a pedido: sem entrevista de calibração, substituída pelo pedido recebido; leitura por especialidade e crítica de cada achado feitas numa só passada, sem subagentes; sem rede, portanto sem conferir as referências; sem consulta aos dados.
- Refiz com um cálculo próprio, sobre os números do artigo: as somas da Tabela 4 contra as Tabelas 3 e 6; as totalizações por modelo; as contagens de execuções da extensão e dos controles (450, 90, 449, 360 e 294); as médias ponderadas da Tabela 6 contra a Tabela 5 (interfaces e métodos abstratos); os percentuais da Tabela 1 como frações de 55; o custo total de US$ 346; as concordâncias da segunda revisão (195 de 210); e os intervalos de 22% a 46% e de 3% a 27% da Seção 6. **Todos conferem.** As Figuras 5 e 6 coincidem com as Tabelas 3 e 4.
- Razões de chances e intervalos de Wilson são meus, apenas para dimensionar a incerteza; não são resultados do artigo.
