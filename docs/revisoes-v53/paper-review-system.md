# Relatório de revisão (método paper-review-system)

**Artigo**: Orientações de projeto e design patterns em agentes de programação: um estudo com pré-registro e sua extensão à manutenção (versão 53)
**Evento (perfil adaptado)**: bom artigo de evento brasileiro de engenharia de software, como o SBES. Não há evento alvo definido.
**Data da revisão**: 09/11/2026
**Base do julgamento**: somente o texto de `J:\TCC\docs\versoes\artigo-v53.md` e as seis figuras PNG. Os dados e o código do repositório não foram consultados. As aspas « » trazem trechos literais do artigo, sem as marcas de itálico e de código; todos foram conferidos por comparação automática com o texto.

---

## Como o método foi aplicado

**Execução.** As fases 1, 1.5, 2 e 4 foram feitas em uma só passada, sem subagentes. Os papéis A a E do método foram cumpridos em sequência. A fase 3 (auditoria de código) não se aplica, porque a leitura se restringiu ao artigo.

**O que não foi executado, por instrução.** Os scripts e os servidores MCP do plugin (`quick-assess.py`, Paper Parser, Academic Search) e qualquer busca na rede. Ficaram sem verificar a existência e os metadados das 40 referências, a correspondência entre o que o artigo atribui a cada fonte e o que ela diz, as citações omitidas e o estado da arte dos últimos 12 meses. Os documentos `docs/reviewer-perspectives/` não fazem parte da lista recebida e não há perfil de SBES entre eles.

**Critérios adaptados ao perfil de evento brasileiro de engenharia de software.** O perfil não existe no plugin; os pesos abaixo são julgamento deste revisor e o conhecimento sobre o SBES é geral, sem consulta à chamada de trabalhos.

| Critério do plugin (grandes conferências) | Adaptação usada aqui |
|---|---|
| Pesos por evento | Solidez técnica 30%, experimentos 25%, novidade 15%, motivação e relevância 10%, reprodutibilidade e ciência aberta 10%, trabalhos relacionados 5%, escrita 5%. Material suplementar recebe nota, com peso zero, porque já pesa em reprodutibilidade. |
| Verificação de fórmulas e símbolos | O artigo não tem fórmulas. Em seu lugar, foram conferidas a aritmética das tabelas, a coerência entre números e as regras de decisão. |
| Baselines de SOTA, datasets, leaderboards | O N0 é a linha de base. O critério virou adequação do desenho experimental, das tarefas e das condições comparadas. |
| Ablação | A sequência cumulativa N0 a N3 foi julgada como a ablação do estudo. |
| Error bars, múltiplas execuções, testes | Mantido, voltado a contagens: intervalos de confiança, testes exatos ou modelos mistos, agrupamento por modelo. |
| Params, FLOPs, FPS | Substituído por custo, *tokens* e tempo. |
| Perfil ACL, LLM como avaliador e versão da API | Aproveitado: versão dos modelos, custo, contaminação por dados de treino, validação do avaliador de IA. |
| Acréscimos próprios da área | Ameaças à validade, aderência ao pré-registro, disponibilidade de artefatos, validade de construto das medidas. |

---

## Leitura rápida (fase 1.5)

**Tipo**: empírico, experimento controlado com agentes como sujeitos. **Estrutura**: completa (Introdução, Fundamentação, Relacionados, dois estudos, Discussão, Limitações, Conclusão), com seção própria de limitações. **Citações** (40 entradas): 19 de 2025 ou 2026 (47,5%), mediana 2024, sem autocitação. **Rigor estatístico no texto**: várias execuções por condição; nenhum intervalo de confiança; um único teste de hipótese (Page), declarado informativo. **Volume**: cerca de 10 mil palavras no corpo, mais sete tabelas e seis figuras, o que deve exceder com folga um limite típico de evento se houver um.

**Conferência aritmética (positiva).** As linhas de `pattern` da Tabela 3 batem com as somas da Tabela 4 e com as Figuras 5 e 6. A Tabela 6 reproduz, por média ponderada, os valores da Tabela 5. Fecham: 90 + 144 + 216 = 450; 89 + 96 + 109 = 96 + 103 + 95 = 294; 195 de 210 notas e a soma por dimensão; 52 notas de calibração; US$ 346; e os percentuais 22% a 46% e 3% a 27% da Seção 6. Os 14 pares do ponto do clube, somados aos 37 acertos do N0, dão os 51 do N1. A única discrepância numérica encontrada está em W1.

**Impressão inicial.** Trabalho sério e incomumente franco sobre seus próprios limites. O risco principal não é falta de dados: é o resumo e a conclusão afirmarem mais do que as seções internas sustentam.

---

## Resumo do artigo

O artigo testa se orientações persistentes (`CLAUDE.md`, *skills* e um processo com subagente) mudam a decisão de agentes de programação de aplicar um *design pattern* não mencionado no pedido. O estudo principal, pré-registrado, usa 11 modelos Claude, quatro níveis cumulativos e cinco réplicas em um serviço de *checkout* (220 execuções). O acerto do ponto mais difícil do Strategy sobe de 67% para 93% só com o arquivo. A extensão exploratória (450 execuções e 90 de controle negativo) cobre cinco *patterns*, construção e manutenção, três modelos e um verificador de IA, e relata a mesma direção em nove de dez combinações e ganho maior na manutenção. As métricas CK são usadas para sugerir o mecanismo.

## Pontos fortes

**S1. Pré-registro com regras de decisão concretas.** Tipos de hipótese definidos antes da coleta e registro do instrumento: «com o hash de cada arquivo do instrumento (122 arquivos)» (Seção 4.2).

**S2. Pareamento cuidadoso.** Os quatro níveis rodam juntos, «de modo que o horário e a carga do servidor fossem iguais dentro de cada par» (Seção 4.1).

**S3. Instrumento de correção testado.** A suíte foi posta à prova «com 17 versões defeituosas escritas manualmente, todas reprovadas por ela» (Seção 4.1).

**S4. Controles negativos, raros nesta literatura.** O ponto do seguro e os cinco cenários em «que o pattern não se justifica» (Seção 5.2) atacam a pergunta certa, a do excesso.

**S5. Honestidade ao relatar efeitos que enfraquecem a tese.** Exemplos: «A extensão, portanto, não reproduz o efeito do estudo principal nesse ponto» (Seção 6) e a complexidade cuja «queda com a orientação não é evidência independente da mudança de desenho» (Seção 7).

**S6. Leitura correta do que a concordância entre modelos significa.** «Isso mostra que a leitura se repete, e não que está certa» (Seção 7).

**S7. Consistência numérica.** Conforme a conferência acima, tabelas, figuras e texto concordam quase integralmente, o que é raro em um manuscrito com tantos números.

---

## Problemas, por ordem de gravidade

Classificação: **Maior** altera uma afirmação central. **Moderado** enfraquece a evidência ou impede a verificação. Os cálculos marcados como "deste revisor" usam apenas números do artigo e ignoram pareamento e agrupamento por modelo, servindo só para dar ordem de grandeza.

### W1. (Maior) «sem piorar a correção» é mais forte do que os dados — Resumo, Seções 4.1, 4.3, 5.3 e 6

Trechos: «sem piorar a correção» (Resumo); «não passa de 7 em 55» (4.1); «três a mais melhoraram do que pioraram» (4.3); «o Haiku 4.5 tenha piorado nos cinco pares (saldo -5)» (Seção 6); «oscilam sem direção clara» (Seção 6).

- Não inferioridade não é ausência de piora. A margem tolera saldo negativo de 7 em 55, cerca de 12,7 pontos percentuais, sem justificativa.
- **Discrepância numérica**: a Tabela 1 dá 71% e 84% da suíte inteira (39 e 46 em 55), portanto saldo líquido +7 em uma medida binária pareada, mas o texto diz +3. A medida do par deve ser outra (casos aprovados?), e ela não é definida.
- O modelo mais fraco piora nas cinco réplicas. Esse dado só aparece na Discussão, e o Resumo e a Seção 8 generalizam.
- Sinal convergente na extensão, não discutido: o comportamento na manutenção cai 61, 59, 54 (Tabela 3). Como o Sonnet 5.5 «teve o comportamento avaliado como bom nas 150 execuções» (5.3), por subtração os outros dois modelos vão de 36 para 34 e 29 em 50 (72% para 58%). A diferença não é significativa isoladamente (Fisher p ≈ 0,21, deste revisor), mas tem a direção do Haiku 4.5 no estudo principal e ocorre onde o ganho de `pattern` é maior. Chamar isso de oscilação esconde uma possível troca entre refatorar e preservar comportamento.

*Sugestão*: escrever "não inferior dentro da margem pré-registrada, exceto no Haiku 4.5"; justificar a margem; definir a unidade do par; cruzar `pattern` e comportamento na manutenção, por modelo.

### W2. (Maior) «efeito mais forte» na manutenção vem da *skill*, e o tratamento pode diferir entre tarefas — Resumo, Seções 5.2 e 6

Trechos: «um efeito mais forte quando o agente muda um código pronto» (Resumo); «de 19 para 33, contra 41 para 53» (Resumo); «os dados indicam efeito maior na manutenção» (Seção 6); «a linha da skill no N2 da manutenção» (Seção 6).

- Só com o arquivo, os ganhos são +14 e +12, praticamente iguais (razão de chances ≈ 2,3 e 2,0, deste revisor). A diferença aparece toda no degrau da *skill* (+25 contra +7).
- Essa *skill* foi escrita para o estudo, por quem conhece tarefas e critério.
- A expressão «a linha da skill no N2 da manutenção» surge pela primeira vez na Discussão. A Seção 5.2 descreve o N2 apenas como arquivo e *skill*. Se o N2 da manutenção recebeu instrução que o da construção não recebeu, a interação tarefa × nível, afirmação central da extensão, está confundida com a mudança de tratamento.
- Há teto na construção (o Sonnet 5.5 já acerta 22 de 25) e nenhum teste de interação.

*Sugestão*: descrever exatamente a instrução de cada nível e tarefa; reformular para "com o arquivo apenas, o ganho é semelhante; a diferença vem da *skill*"; estimar a interação por modelo logístico misto.

### W3. (Maior) Relato seletivo dos desfechos — Seções 4.1, 4.3 e 5.3

Trechos: «quatro pontos em que a variação pede o padrão Strategy» (4.1); «Este último é o mais difícil» (4.1); «as quatro perguntas de desenho» (4.3); «por CBO, LCOM, WMC, RFC e DIT» (4.1).

- O estudo mede quatro pontos, mas só o do clube tem resultado. Frete, cupom e pagamento não aparecem em tabela, texto ou figura.
- Não se diz se o clube foi o desfecho primário fixado no pré-registro, nem se «mais difícil» é uma classificação prévia ou observada nos dados. Se observada, a manchete 67% para 93% é seleção pelo resultado.
- Não fica claro o que são as «quatro perguntas», já que há quatro pontos e mais o controle.
- O RFC é definido duas vezes na Seção 2 e declarado como medido, mas nenhum valor é apresentado. LCOM e DIT somem na extensão, embora o CK tenha sido rodado da mesma forma.

*Sugestão*: tabela com os quatro pontos por nível; declarar o desfecho primário; relatar todas as métricas coletadas, ao menos em apêndice.

### W4. (Maior) Conclusões sobre contenção não têm desenho que as sustente — Resumo, Seções 4.3, 5.3 e 8

Trechos: «sem aumentar o exagero» (Resumo); «A hipótese do exagero foi contrariada» (4.3); «em nenhuma das 10 do Haiku 4.5» (5.3); «não leva o agente a aplicar pattern onde ele não cabe» (Seção 8).

- A hipótese era de alteração. Ser contrariada não demonstra que o exagero não sobe; isso exigiria margem de não inferioridade. Os números vão de 0 para 1 e 3 execuções em 55, a partir de um piso de 0%.
- Há um único ponto de controle por execução, de baixa tentação («apenas a porcentagem varia»).
- Nos controles da extensão, o Haiku 4.5 não abriu a *skill* em nenhuma execução do N2. Só 20 das 30 receberam o tratamento. Entre elas, são 2 exceções em 20, 10% (IC 95% exato de 1,2% a 31,7%, deste revisor); em 30, de 0,8% a 22,1%. O Resumo informa «28 de 30» sem essa ressalva.
- A frase da Seção 8 contradiz a cautela das Seções 6 e 7 («não bastam para estimar o risco da skill»). O apontamento é sobre a afirmação, não sobre a redação pendente da seção.
- O Resumo relata os controles favoráveis, mas omite a alta de 4 para 9 execuções com exagero na construção.
- «Exagero» tem três operacionalizações: o ponto do seguro, a «estrutura sem necessidade fora da parte que o pattern resolve» e a estrutura para variação não pedida. O número de cada uma não se compara com o das outras.

*Sugestão*: formular como limite superior com intervalo; relatar a exposição à *skill*; nomear as três medidas de forma distinta; mencionar o 4 para 9 no Resumo com a ressalva de que também é compatível com ruído (Fisher p ≈ 0,25, deste revisor).

### W5. (Maior) Ausência de incerteza quantificada e regras de decisão incompletas — Seções 4.1, 4.3, 5.3 e 7

Trechos: «de alteração, sem direção» (4.1); «publicada a título informativo» (4.3); «decidido por um único modelo fora do teto» (4.3); «sem teste de significância» (Seção 7).

- Nenhum percentual tem intervalo. O ponto do clube, por exemplo, é 67% (IC 53% a 79%) e 93% (IC 82% a 98%), deste revisor, antes de corrigir pelo agrupamento em modelos.
- A evidência pode ser forte onde não é mostrada: 14 pares melhores e 0 piores dariam p ≈ 0,0001 em um teste de sinais simples, ainda que os pares não sejam independentes.
- A regra de alteração não é definida. O leitor não consegue saber por que 1 em 55 é «contrariada» e 3 em 55 é «apoiada».
- A regra direcional degenera quando só um modelo está fora do teto: um modelo é «a maioria», sem tamanho mínimo de efeito.
- Na extensão, altas de +2 em 15 (Observer e Factory Method na construção) entram na conta de «nove das dez», em um cenário em que 18% das rodadas não repetem o resultado da primeira.
- O piso de dez repetições é um motivo para cautela, não para dispensar análise. A agregação em 75 por nível e os testes exatos ou de permutação ainda são possíveis.
- Não há tratamento de multiplicidade, e o número total de hipóteses não é informado.

*Sugestão*: intervalos em todas as tabelas e figuras; modelo logístico misto ou *bootstrap* por modelo; definir por extenso cada regra.

### W6. (Maior) O desenho cumulativo não permite atribuir a contribuição de cada componente — Seções 1, 4.1, 4.3 e 6

Trechos: «como cada componente da configuração contribui para ele (P4)» (Seção 1); «quatro níveis cumulativos» (4.1); «não foi aberta espontaneamente em nenhuma das 110 execuções» (4.3); «O CLAUDE.md responde pela maior parte do efeito» (Seção 6).

- Com níveis aninhados, o arquivo sempre vem primeiro e absorve o que o teto deixa. A *skill* nunca é testada sem ele.
- A frase sobre a maior parte do efeito não vale para a manutenção (+14 do arquivo, +25 da *skill*).
- No estudo principal, a *skill* nunca foi aberta, de modo que o N2 e o N3 não receberam o tratamento nominal. A inconclusão é atribuída ao teto, quando a não exposição já a explicaria.
- N1 contra N2 funciona quase como uma comparação A/A e mostra o ruído do experimento: suíte 84% e 80%, linhas 584 e 614, interfaces 2,9 e 3,4, *enums* 4,0 e 2,7, exagero 2% e 5%. A queda «de 3 execuções em 55 para nenhuma» parte dessa linha de base e deveria ser lida à luz disso.
- A taxa de abertura da *skill* nas 150 execuções do N2 com *pattern* não é informada.

*Sugestão*: reformular P4 como efeito incremental nessa ordem; relatar a exposição; usar N1 contra N2 como estimativa de ruído; registrar o desenho aninhado como ameaça.

### W7. (Maior, trata a limitação declarada de forma incoerente) «pattern certo» e «calibrado» dizem mais do que foi feito — Resumo, Seções 1, 5.3, 6 e 7

Trechos: «um verificador de qualidade calibrado» (Resumo); «no pattern, acerto ou alternativa» (legenda da Tabela 3); «nunca sem ela» (Seção 6); «testou apenas acerto e omissão» (Seção 7); «a calibração não cobriu o design do restante do código nem o exagero» (Seção 7).

- A limitação do verificador está declarada. O problema é o Resumo e as contribuições venderem «calibrado» sem qualificação quando a calibração não testou duas das quatro classes da medida principal, nem o exagero, nem o design.
- 39 dos 264 «certos» (28 + 11, Tabela 6) são alternativas, e há 89 malfeitos, exatamente as fronteiras não testadas.
- O rótulo «pattern certo» (Tabela 4, Figuras 5 e 6, «acertos» no Resumo) inclui a alternativa, mas a legenda da Tabela 4 não diz. No Decorator, o texto admite que a forma literal aparece com a *skill* e «nunca sem ela», logo os 10 de 15 do N1 na construção não são Decorator.
- O 52 de 52 foi obtido no mesmo conjunto usado para ajustar o verificador (50 de 52 antes), portanto é calibração, não validação.
- A única tendência positiva de P6 («acompanha a orientação») repousa no design, a nota sem calibração e com a menor concordância (kappa 0,53).

*Sugestão*: separar acerto e alternativa em tabelas e figuras; análise de sensibilidade sem as alternativas; qualificar «calibrado» no Resumo e na Introdução.

### W8. (Maior) A única réplica conceitual falhou e o Resumo não diz — Resumo, Seções 5.3 e 6

Trechos: «mostrou a mesma direção em nove das dez combinações» (Resumo); «A extensão, portanto, não reproduz o efeito do estudo principal nesse ponto» (Seção 6); «O aumento do pattern certo apareceu na mesma direção nos dois estudos» (Seção 6).

- A combinação excluída é a mais próxima do estudo principal: mesmo *pattern*, mesmo tipo de tarefa.
- O tratamento testado no estudo principal (N1) a piorou: de 9 para 7 em 15.
- A explicação pelo teto do Sonnet 5.5 cobre 5 execuções por nível. Nas outras 10, os acertos são 4, 2 e 4 (por subtração), sem efeito algum.
- As duas frases da Seção 6 ficam em tensão.

*Sugestão*: dizer no Resumo qual é a exceção e investigar o porquê (enunciado, *framework*, ponto de variação).

### W9. (Moderado) A proteção do pré-registro é menos sólida do que a Seção 4.2 sugere — Seções 4.2, 4.3 e 7

Trechos: «o que permite verificar a data do registro» (4.2); «o autor viu apenas informações operacionais, sem a direção de nenhum efeito» (4.2); «depois de um ensaio anterior, em que foi a métrica que mais mudou com a configuração» (Seção 7); «O agente de IA que operou o ensaio teve acesso aos resultados» (4.2); «no código e no registro de decisões de 09/10» (4.3).

- A data de um *commit* é fixada por quem o faz. Sem carimbo de terceiros (OSF, Zenodo, Software Heritage), a verificação é fraca.
- A frase sobre o autor vale para o V4. Pelas 119 execuções de bancada, em que o texto da Figura 3 indica "com e sem orientação", e pela Seção 7, efeitos foram vistos antes. Não se diz se o `CLAUDE.md` do estudo principal foi ajustado nessas execuções.
- Não se esclarece se o agente que viu os resultados do V4 participou da redação do pré-registro do V5.
- A hipótese do modelo só é apoiada pela regra de empate. O outro modelo mais fraco teve +1 em 5, o que contraria o espírito da hipótese.
- Falta tabela com todas as hipóteses, tipo de regra, resultado e desvios. O texto menciona pelo menos cinco principais, *skill*, processo, CBO, LCOM, classes e duplicação.
- Os resultados do V4, um lote executado, não são relatados.
- A referência citada trata de *registered reports*, em que há revisão do protocolo, situação distinta da descrita.

### W10. (Moderado) O «mecanismo» é em boa parte consequência da definição — Resumo, Seções 5.3 e 6

Trechos: «sugerem o mecanismo» (Resumo); «Esse aumento reflete as classes abstratas do Template Method e do Factory Method» (5.3); «os projetos com omissão quase não têm interfaces nem métodos abstratos» (5.3); «o acoplamento médio muda pouco» (Resumo); «multiplicando o CBO médio pelos tipos» (Seção 6).

- O número de condicionais nunca é relatado, embora a afirmação seja «troca condicionais por polimorfismo».
- Template Method implica método abstrato. Omissão é definida pela condicional. A concordância da Tabela 6 é em boa parte circular e não valida o verificador.
- O CBO médio tem efeito de denominador: muitos tipos pequenos reduzem a média. O próprio cálculo da Seção 6 indica alta de 22% a 46% no total estimado de dependências, mas o Resumo fica em «muda pouco».
- No estudo principal, o cálculo multiplica uma mediana (CBO) por uma média (tipos).
- A queda na cauda da LCOM pode ser mecânica, porque a métrica cresce com o número de pares de métodos, e dividir classes grandes a reduz.

*Sugestão*: contar condicionais sobre o caso; somar arestas de dependência; relatar por *pattern*; condicionar a afirmação de mecanismo.

### W11. (Moderado) O tratamento não aparece no artigo — Seções 4.1, 4.2, 5.1 e 5.2

Trechos: «com princípios gerais, sem nomear patterns» (4.1); «por outra escrita para este trabalho» (5.1); «https://github.com/FLucasF/TCC» (4.2).

O objeto do experimento (o texto do `CLAUDE.md`, a *skill* escrita para o estudo, a instrução do N2) não é mostrado nem resumido; o único fragmento é «o próximo caso num lugar só». Também faltam uma declaração de disponibilidade de dados, identificadores exatos dos modelos, versões de Semgrep, SonarQube e JaCoCo, as regras Semgrep e as *skills* do verificador. O *link* é dado apenas para o *hash* do pré-registro. Se o evento adotar revisão duplo-cega, a URL pessoal identifica o autor.

*Sugestão*: apêndice ou pacote citado com o texto integral dos tratamentos e seção de artefatos.

### W12. (Moderado) Validade externa e de construto sem tratamento — Seções 5.1, 5.2 e 7

Trechos: «o sistema pronto, sem nenhum pattern» (5.2); «o teto, já que cinco dos 11 modelos não tinham o que melhorar» (5.1); «o Claude Haiku 4.5, o Sonnet 4.5 e o Sonnet 5.5» (5.2).

- A Seção 7 não trata do fato de haver um sistema pequeno e sintético por estudo, em uma só linguagem, escrito por quem define o critério, nem de a base da manutenção ser sem *pattern* por construção.
- A motivação da extensão é o teto, mas ela inclui o Sonnet 5.5, um dos cinco modelos no teto, e deixa de fora o Opus 4.7, o de maior efeito. A escolha dos três modelos não é justificada.
- Na manutenção, só uma frase sobre o crescimento separa o cenário positivo, em que a omissão é erro, do controle, em que a mudança mínima é o acerto. O vocabulário de «acerto» e «ganho» trata como norma uma escolha do pesquisador, o que a Seção 7 reconhece apenas parcialmente.
- A Seção 7 não segue a divisão usual em validade interna, externa, de construto e de conclusão.

### W13. (Moderado) As demais dimensões são mal lidas — Seções 5.3 e 6

Trechos: «oscilam sem direção clara» e «A configuração versa sobre estrutura, e é nela que atua» (Seção 6); «supera o Haiku 4.5 com a skill em todas elas» (Seção 6).

- A arquitetura é estrutura, e na construção vai 16, 9, 16. A frase final não se sustenta nessa linha.
- Banco com 0, 0, 0 em 225 é efeito de piso da rubrica, sem explicar se migrações foram pedidas.
- Comportamento com 25, 25, 25 na construção corresponde exatamente às 75 execuções do Sonnet 5.5, o que indica um critério que só separa modelos. Testes na construção caem 11, 9, 6.
- As afirmações por modelo em P3 não têm tabela, e a principal não pode ser conferida.

### W14. (Moderado) A estabilidade é confundida com a taxa de base — Seções 5.3 e 6

Trecho: «O resultado da primeira rodada se repetiu em 82% das repetições» (Seção 6).

A medida ancora em uma rodada arbitrária e sobe automaticamente perto de 0 ou de 100%. O dado de que o Sonnet 5.5 é o mais estável (109 de 120) pode refletir apenas seu teto. *Sugestão*: variância por célula ou correlação intraclasse, comparada ao esperado sob a taxa observada.

### W15. (Moderado) A Figura 2 ilustra o Strategy com o caso que o artigo chama de exagero — Seções 2 e 4.1

Trechos: «A mesma regra com uma condicional e com o padrão Strategy» (legenda); «apenas a porcentagem varia e a aplicação do padrão constituiria exagero» (4.1).

Na figura, os planos diferem só por um multiplicador (1, 1,2 e 0,9). Além disso, a frase da figura de que o código existente não muda ignora o lugar de escolha que a própria Seção 2 exige («escolhida em um só lugar»). *Sugestão*: exemplo em que o comportamento, e não um valor, varie.

### W16. (Moderado) Resultados novos só na Discussão e custos incompletos — Seções 4.1, 1 e 6

Trechos: «de 1,2% para 2,5% das linhas» (Seção 6); «O custo foi medido pelos tokens e pelo tempo» (4.1); «e sobre o custo que podem trazer» (Seção 1).

Duplicação, hipótese de classes, saldo do Haiku 4.5 e a concentração do exagero no Sonnet 4.5 surgem apenas na Seção 6. *Tokens* e tempo não têm tabela, e o custo da extensão não é relatado, apesar da promessa prática da Introdução.

---

## Auditoria de referências

**Sem verificação externa.** O status "Presente" significa só que a entrada existe e é citada. Não atesta existência real, metadados ou fidelidade. Os dois marcadores `[CITAR]` e as duas citações ausentes já conhecidas ficaram fora.

**Resultado do confronto interno**: 40 entradas. Toda citação do corpo tem entrada, toda entrada é citada no corpo, e não há divergência de autor ou ano entre corpo e lista. Kim (2025) e Kim (2026) estão desambiguadas pelo ano.

| # | Referência | Status | Observação (só pelo texto) |
|---|---|---|---|
| 1 | Ahmed et al. 2025 | Presente | Sem DOI, ao contrário das vizinhas de congresso |
| 2 | Ampatzoglou et al. 2026 | Presente | Autores sem o "e" final, diferente do padrão |
| 3 | Anthropic 2026 | Presente | Três documentos em uma entrada; fora de ordem |
| 4 | Aniche 2015 | Presente | Deveria vir antes de Anthropic; ferramenta |
| 5 | Arabat e Sayagh 2026 | Presente | |
| 6 | Arcuri e Briand 2014 | Presente | |
| 7 | Basili et al. 1996 | Presente | |
| 8 | Boehm 1976 | Presente | Sem DOI |
| 9 | Brown et al. 2020 | Presente | Sem páginas |
| 10 | Campbell 2018 | Presente | |
| 11 | Chatlatanagulchai et al. 2026 | Presente | Sem volume |
| 12 | Chidamber e Kemerer 1994 | Presente | |
| 13 | Ernst e Baldassarre 2023 | Presente | Ver W9, sobre *registered reports* |
| 14 | Fowler 2018 | Presente | |
| 15 | Galster et al. 2026 | Presente | |
| 16 | Gamma et al. 1995 | Presente | |
| 17 | Grndlvl 2026 | Presente | Autor é nome de usuário |
| 18 | He et al. 2026 | Presente | |
| 19 | Hou et al. 2024 | Presente | |
| 20 | JaCoCo 2026 | Presente | Sem versão |
| 21 | Jiang e Nam 2026 | Presente | |
| 22 | Kim 2025 | Presente | |
| 23 | Kim 2026 | Presente | |
| 24 | Kjellberg et al. 2026 | Presente | Sem páginas |
| 25 | Liu et al. 2026 | Presente | Sem volume |
| 26 | Martin 2003 | Presente | Atribuição da fórmula do princípio aberto/fechado a conferir (Menores, item 15) |
| 27 | Meyer 1997 | Presente | Princípio é anterior a esta 2.ª edição |
| 28 | Muñoz Barón et al. 2020 | Presente | |
| 29 | Prechelt et al. 2001 | Presente | |
| 30 | Sallou et al. 2024 | Presente | |
| 31 | Semgrep 2026 | Presente | Sem versão |
| 32 | SonarSource 2026 | Presente | Sem versão |
| 33 | Vaswani et al. 2017 | Presente | |
| 34 | Vokáč et al. 2004 | Presente | |
| 35 | Wang et al. 2025 | Presente | |
| 36 | Xi et al. 2025 | Presente | |
| 37 | Yao et al. 2023 | Presente | Sem páginas |
| 38 | Yourdon e Constantine 1978 | Presente | |
| 39 | Zhang e Budgen 2012 | Presente | |
| 40 | Zisa et al. 2026 | Presente | |

**Qualidade das citações (pelo texto).** Boa recência; nenhuma autocitação; seis itens são ferramentas ou documentação, adequados ao uso que têm; o estilo do evento varia («MSR 2025», «MSR '26», «IEEE TSE» e «IEEE Transactions on Computers»).

**Citações possivelmente omitidas (lembrança do revisor, não verificada).** Antes de sustentar que «ainda faltam estudos controlados» e que «nenhum dos trabalhos lidos» mede o efeito, convém procurar estudos experimentais sobre arquivos de contexto do tipo AGENTS.md (lembro-me de Gloaguen et al. e Lulla et al., 2026, como preprints; autores e títulos devem ser conferidos). A decisão de excluir preprints é defensável, mas, em área tão rápida, o texto deveria declará-la ao apresentar a lacuna. Para as ameaças e a condução do experimento, cabem referências como Wohlin et al. (*Experimentation in Software Engineering*) e os *Empirical Standards* da ACM SIGSOFT.

## Comparação com o estado da arte

**Não realizada** (exigiria busca externa). Pelo próprio artigo, o N0 é a única linha de base, e não há comparação com modelo de outro fornecedor, limitação já declarada.

## Análise de trabalhos concorrentes (somente os que o próprio artigo discute)

Não é o levantamento dos últimos 12 meses que o método pede; lista apenas o que a Seção 3 apresenta, sem verificação.

| Trabalho | Tipo de estudo, segundo o artigo | Diferença afirmada | Citado |
|---|---|---|---|
| Kim 2025 | Refatoração em dois programas, cinco modelos | *Pattern* nomeado; critério por propriedades | Sim |
| Kjellberg et al. 2026 | Singleton, 13 modelos | Orientação no *prompt*, sem agente | Sim |
| Zisa et al. 2026 | Refatoração baseada em *patterns*, dois modelos | Sem orientação persistente | Sim |
| Ampatzoglou et al. 2026 | Experimento com dez júniores | Com pessoa conduzindo | Sim |
| Chatlatanagulchai et al. 2026 | Caracterização de arquivos de contexto | Sem experimento | Sim |
| Arabat e Sayagh 2026 | Antes e depois em 148 projetos | Sem medir projeto do código | Sim |
| Kim 2026 | Três organizações de agentes | Variável é a organização | Sim |

---

## Comentários por dimensão

### 1. Resumo e motivação (4/5)
A lacuna é clara e apoiada por dados externos («apenas 0,92% tratam dessa atividade», Seção 1). O Resumo, porém, escolhe os resultados favoráveis (W1, W4, W8).

### 2. Novidade (4/5)
Medir a decisão de usar um *pattern* não nomeado, manipulando a configuração persistente, com controle negativo, é uma pergunta nova entre os trabalhos discutidos. Nota condicionada à busca não realizada.

### 3. Solidez técnica (3/5)
As regras prévias são um mérito, mas a unidade do par, a regra de alteração e a atribuição por componente têm falhas (W1, W5, W6), e o mecanismo é parcialmente circular (W10). Sem fórmulas para verificar.

### 4. Experimentos (3/5)
Escala, pareamento, controles e teste do instrumento são bons. Faltam desfechos (W3), incerteza (W5) e exposição ao tratamento (W6). A extensão tem confusão potencial na comparação mais anunciada (W2).

### 5. Qualidade da escrita (4/5)
As Seções 4 a 7 são claras e encadeadas. Perdem pontos a densidade de números na Seção 5.3, os resultados novos na Discussão (W16) e os termos com duplo sentido (ver Menores).

### 6. Reprodutibilidade (3/5)
Boa identificação de parte do instrumento (versão do Claude Code, *commit* do CK, *hash*), mas os tratamentos não aparecem no artigo (W11).

### 7. Trabalhos relacionados (3/5)
Comparação honesta, item por item, inclusive do que outros fazem melhor («com uma resolução que as classes aqui adotadas não têm»). A cobertura é restrita e não foi possível avaliá-la de fora.

### 8. Material suplementar (3/5)
Há um repositório público citado, mas o artigo não descreve o que ele contém.

## Auditoria de código

Não se aplica: a instrução limitou a leitura ao artigo.

## Problemas menores

1. «o uso do pattern pedido (P1)» (Seção 1) contradiz «quando o pedido não os menciona» (Seção 1).
2. «correção» tem dois sentidos: qualidade do código («A correção foi aferida») e avaliação («A correção é realizada por uma IA»).
3. «os 11 modelos Claude disponíveis» convive com «O Fable foi excluído» (4.1).
4. «nas dez rodadas do Sonnet 5.5» (Seção 6): a frase trata da construção, onde são 5 rodadas por nível; se somar as duas tarefas, dizer.
5. «O pré-registro do V5» (4.2): V5 não é definido, e a Figura 3 expõe nomes internos (`construction-v7`, `TCC_V1`) e pula a v6.
6. A Figura 1 cita N0 antes de ele ser definido. A nota dentro da Figura 4 ("Em destaque, o que varia em cada estudo") destaca só a configuração, embora a tarefa e o *pattern* variem na extensão.
7. As Figuras 5 e 6 repetem a Tabela 3 (primeira linha) e a Tabela 4, sem acrescentar intervalo.
8. «réplicas» e «rodadas» designam a mesma coisa; `pattern` alterna entre itálico e redondo; a Seção 2 usa rótulos em negrito e registro informal («obriga a mexer»), diferente das Seções 4 a 7.
9. A Tabela 2 funde *enums* com corpo e classes anônimas, medida não apresentada na Seção 2, e o texto fala só de *enums*.
10. «com as mesmas regras de resumo» (5.2) não combina com medianas na Tabela 2 e médias na Tabela 5.
11. «(62 até 06/10)» (4.2) e «por escolha deste desenho» (4.1) ficam sem explicação.
12. «complexidade cognitiva (média)» não diz a unidade (por projeto? por método?).
13. O título fala em extensão «à manutenção», mas a extensão também cobre construção.
14. «cinco dimensões» (Seção 1), «sete notas» (5.2) e seis linhas na Tabela 3: explicitar a relação.
15. A fórmula «aberto para extensão, fechado para modificação» é atribuída a Martin; convém conferir em Meyer (1.ª edição, 1988), de onde a expressão costuma ser derivada. Não verificado.
16. Lista de referências: ordem Anthropic/Aniche e demais itens da tabela de auditoria.

## Perguntas aos autores

- Q1. Qual foi o desfecho primário no pré-registro? O clube foi escolhido antes de qualquer dado?
- Q2. Quais os resultados de frete, cupom e pagamento?
- Q3. Qual é a unidade do par na correção, e por que o saldo é +3 se a Tabela 1 implica +7?
- Q4. Como se justifica a margem de 7 em 55?
- Q5. Qual é a regra exata do tipo «alteração»?
- Q6. O que é «a linha da skill no N2 da manutenção»? Ela existia no N2 da construção?
- Q7. Com que frequência a *skill* foi aberta nas 150 execuções do N2 com *pattern*?
- Q8. Os acertos do Decorator sem a *skill* são todos alternativas?
- Q9. O `CLAUDE.md` do estudo principal mudou durante as 119 execuções de bancada?
- Q10. O agente que viu o V4 ajudou a escrever o pré-registro do V5? Quais foram os resultados do V4?
- Q11. Na manutenção, o comportamento piora entre as execuções classificadas com o *pattern* certo?
- Q12. Por que Sonnet 5.5 e não Opus 4.7 na extensão?
- Q13. Por que a nota de banco é zero em toda a construção? As migrações foram pedidas no enunciado?

---

## Avaliação geral

| Dimensão | Nota | Síntese |
|---|---|---|
| Resumo e motivação | 4/5 | Lacuna clara; resumo seletivo |
| Novidade | 4/5 | Pergunta nova; sem busca externa |
| Solidez técnica | 3/5 | Regras prévias boas, mas incompletas |
| Experimentos | 3/5 | Bom desenho; sem incerteza; desfechos faltando |
| Qualidade da escrita | 4/5 | Clara; resultados novos na Discussão |
| Reprodutibilidade | 3/5 | Tratamentos ausentes do texto |
| Trabalhos relacionados | 3/5 | Comparação honesta; cobertura não verificada |
| Material suplementar | 3/5 | Repositório citado, conteúdo não descrito |
| **Geral (ponderada 3,3)** | **3/5** | |

### Recomendação
- [ ] Strong Accept / Accept / Weak Accept
- [x] **Borderline**
- [ ] Weak Reject / Reject / Strong Reject

### Confiança do revisor
- [x] **3 (razoavelmente confiante)**: as afirmações foram conferidas apenas pelo texto; referências, literatura e dados ficaram sem verificação.

### Comentário final
O artigo tem um desenho acima da média e uma franqueza pouco comum. A principal fragilidade é a distância entre as seções internas, que qualificam quase tudo, e as frases de Resumo e Conclusão, que retiram as qualificações. A maior parte do que trava a aceitação pode ser resolvida com os dados já coletados: relatar os outros três pontos, acrescentar incerteza, reescrever as afirmações sobre correção, contenção e manutenção, separar acerto de alternativa e mostrar os tratamentos. A exceção é W2: se o N2 diferiu entre as tarefas, a comparação entre construção e manutenção precisa de nova execução ou deve deixar de ser afirmada. Feitas essas correções, o trabalho tem perfil de aceitação em um bom evento brasileiro de engenharia de software.
