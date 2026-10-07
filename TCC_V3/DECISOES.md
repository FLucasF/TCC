# Decisões de projeto

O `OBJETIVO.md` e os READMEs dizem **o que** o estudo é; este arquivo diz **por
quê**, e o que foi descartado no caminho. Cobre o TCC_V3 (a bancada, desde 24/09)
e o V4 que ele prepara. As versões anteriores (o harness antigo e a v2) estão em
`docs/briefings/decisoes-do-harness.md`, em `TCC v2/` e no histórico do git.

Cada entrada tem a data (de 2026), a decisão, o motivo, o que se descartou e onde
está o detalhe. Uma decisão que mudou **não é apagada**: fica marcada como
substituída, e a nova ganha entrada própria. A fonte de cada uma é o commit do dia
(`git log` traz o motivo no corpo da mensagem) ou o documento citado.

Para acrescentar: uma entrada na seção do tema, no mesmo formato.

**Sumário:** [1. Pergunta e recorte](#1-pergunta-e-recorte) ·
[2. Desenho do experimento](#2-desenho-do-experimento) ·
[3. Níveis de harness](#3-níveis-de-harness) · [4. Enunciado](#4-enunciado) ·
[5. Correção](#5-correção-a-suíte-de-aceitação) ·
[6. Leitura de desenho](#6-leitura-de-desenho-a-régua) ·
[7. Métricas automáticas](#7-métricas-automáticas) ·
[8. Regras de leitura](#8-regras-de-leitura-das-hipóteses) ·
[9. Integridade da bancada](#9-integridade-e-reprodutibilidade-da-bancada) ·
[10. Documentação](#10-documentação)

---

## 1. Pergunta e recorte

**12/09: o recorte é construir software × desenho de baixo nível.** Padrões de
projeto, com e sem harness, em mais de um modelo; manutenção, arquitetura, testes
e banco de dados ficam fora. *Por quê:* a célula combinada na orientação de 12/09.
*Onde:* OBJETIVO §1.

**12/09: começar pelo harness mais simples (só um `CLAUDE.md`) e por um padrão
(Strategy).** *Por quê:* o primeiro par organiza o corpo do projeto (bancada,
régua, leitura, análise); componentes e padrões novos entram depois, um de cada
vez, sobre o mesmo corpo. *Onde:* OBJETIVO §1.

**26/09: a influência é lida em três eixos (desenho, correção, custo), e a
negativa conta tanto quanto a positiva.** *Por quê:* um harness pode melhorar um
eixo e piorar outro; medir só o desenho esconderia estrutura comprada com erro de
cálculo ou com gasto. Por isso todo enunciado tem ponto positivo e controle
negativo. *Onde:* OBJETIVO §3.

**05/10: quatro hipóteses principais (desenho, exagero, correção, modelo); as
outras são secundárias.** *Por quê:* com muitas hipóteses e poucos pares, alguma
"dá certo" por acaso; só as principais respondem à pergunta. *Onde:* OBJETIVO §4.

**06/10: o custo é todo secundário.** Antes, "Custo: tokens de entrada" era
principal. *Por quê:* orientação de 12/09; o custo depende de cache e de variáveis
fora do experimento, e no piloto o sinal trocou entre modelos. É medido e
publicado, mas não sustenta conclusão. *Onde:* OBJETIVO §4 e §4.4.

## 2. Desenho do experimento

**24/09: pares simultâneos.** A execução com harness roda no mesmo instante que a
sem harness, do mesmo modelo. *Por quê:* horário, fila e carga do servidor ficam
iguais, e a comparação é dentro do par. *Onde:* `infra/scripts/rodada.sh`.

**24/09: o Claude Code como vem.** Sem restrição de ferramenta (subagente
inclusive), rede aberta nos dois braços, sem limite de tempo nem de turnos,
`effort=medium`. *Por quê:* o efeito medido é o do harness somado ao Claude Code
real, não a um ambiente que nenhum usuário tem. O que difere e não se controla
fica medido e declarado: o Haiku recebe 30 ferramentas e o Opus e o Sonnet, 26.
*Onde:* README, OBJETIVO §6.

**24/09: quem decide se uma execução vale é humano.** O campo `valid` do
`meta.json` sai sempre vazio. *Por quê:* um script que julga validade acabaria
decidindo resultado.

**24/09: o pré-registro congela também o instrumento, não só o estímulo.** Os
cinco scripts da bancada têm hash, com o que se perde se cada um tiver defeito.
*Por quê:* na v2, o pré-registro congelava só o enunciado, e dois defeitos
apareceram depois no extrator, que não estava sob registro. *Onde:* README,
tabela de scripts.

**24/09: execuções de teste e de fumaça ficam fora da análise pelo prefixo**
(`SMOKE-`, `TESTE-`, e depois `BATCH-` como piloto). *Por quê:* elas existem para
achar defeito antes de custar dado; o `agregar.mjs --prefix` separa sem apagar dado
bruto.

**26/09: um experimento só (o `EXT`), e o `BATCH` vira piloto.** *Por quê:* o
estímulo do piloto é outro (três pontos), e as execuções não são comparáveis. Ele
fica como o motivo de P4 e P5. *Onde:* `experiment/prompt/README.md`.

**26/09: a avaliação fica numa pasta por padrão, e o `anonimizar.mjs --padrao`
recusa sobrescrever um mapa.** *Por quê:* pacote cego não traz `meta.json`; só a
pasta liga um pacote ao gabarito dele, e o mapa é a única ligação entre código cego
e execução. Com um padrão não pesava; com o segundo, pesaria.

**29/09: o State entra como segundo padrão.** *Por quê:* testa o que o Strategy
não testa, o caso que muda durante a vida do objeto. *Substituída em 07/10* (ver
abaixo).

**30/09: não rodar lote com a assinatura em uso em outro lugar.** *Por quê:* a
`TESTE-STATE-01` inteira caiu no limite de uso (HTTP 429) aos 95 segundos, porque
as execuções usam o mesmo token da sessão interativa. *Onde:* README.

**03/10: o plano em duas fases: preparar (construir, conferir e congelar, sem
olhar dado) e medir.** O OBJETIVO é congelado com o orientador entre as duas;
depois, mudança só como emenda datada. *Por quê:* a linha entre preparar e medir
não estava escrita. Rodar a suíte sobre o EXT não gasta tokens, mas já é olhar
resultado, e por isso fica na segunda fase. *Onde:* `PLANO-IMPLEMENTACAO.md`.

**03/10: o `verificar.mjs` vai para a fase 2, roda só sobre os lotes da análise, e
só conta como pronto quando acusa dados corrompidos de propósito.** *Por quê:* os
dados que ele confere só existem na fase 2; SMOKE, TESTE e BATCH não têm o desenho
que as checagens exigem. A prova de que acusa vem da mesma ideia dos mutantes (e
do teste negativo do `verify_scores.py` do Akita). *Onde:* plano, Parte 3.

**03/10: o desenho da manutenção (Parte 5), por consenso.** Só Strategy, sobre
cópias dos workspaces, sessão nova a cada sprint, `CLAUDE.md` mantido no braço com
harness, quatro sprints na ordem P2, P1, P4, P5 (uma falha não interrompe a
sequência), medida principal em arquivos tocados de `src/main`. *Por quê:* testa
manter, não só construir, com o mesmo corpo. Fica para depois do V4 e precisa ser
revista para os níveis. *Onde:* plano, Parte 5.

**06/10: o TCC_V3 é a bancada; o experimento que vale roda no V4.** *Por quê:* os
lotes daqui (inclusive o `EXT`) serviram para achar defeitos de bancada, de
enunciado e de instrumento; misturá-los com o lote que vale contaminaria o
pré-registro. *Onde:* OBJETIVO §2.

**06/10: 5 réplicas no V4 (3 na bancada).** *Por quê:* 3 bastam para testar a
bancada; para o lote que vale, 5 réplicas dão 15 pares por comparação em vez de 9,
e as regras de leitura ganham margem. *Onde:* OBJETIVO §2 e §4.1.

**06/10: no V4, os quatro níveis rodam juntos, num quarteto simultâneo por
modelo.** *Por quê:* todos os níveis se comparam em pares sem repetir o N0 em cada
lote, e a tendência de N0 a N3 fica possível. *Descartado:* um lote por nível
(mais execuções para a mesma informação). *Onde:* plano, Parte 6.

**06/10: o `rodada-niveis.sh` confere tudo antes de lançar qualquer execução, e
chama o `executar.sh` sem mudá-lo.** Um modelo por vez é o mais seguro para a cota.
*Por quê:* um quarteto com um nível a menos não serve para a comparação, e é melhor
falhar sem gastar cota; o `executar.sh` está congelado.

**06/10: o cache de prompt não é desligado.** Os tokens ficam separados no
`meta.json`. *Por quê:* um usuário real usa cache, e o isolamento das execuções não
teria como impedi-lo (ele fica nos servidores). Ele muda tempo e tokens, não o
código gerado. *Onde:* OBJETIVO §4.4.

**07/10: o V4 fica só com o Strategy; o State vai para `history/state/`.**
*Por quê:* o State provou que a bancada aceita um segundo padrão (enunciado, suíte e
agregação funcionaram); a régua dele nunca foi calibrada. Com um padrão, o V4 cai
de 120 para 60 execuções, metade da cota. As hipóteses "entre padrões" ficam
escritas para o futuro. *Onde:* `history/state/README.md`.

## 3. Níveis de harness

**26/09: uma pasta por versão de harness, escolhida por `HARNESS`.** *Por quê:*
testar `CLAUDE.md`, skills e combinações sem trocar o conteúdo de uma pasta que já
rodou; sem a variável, o `executar.sh` faz o mesmo de antes, com o mesmo hash.

**06/10: a escada N0 a N3, que acumula.** N0 sem harness; N1 o `CLAUDE.md`; N2 o
N1 mais uma skill; N3 o N2 mais processo. *Por quê:* a proposta do orientador
(12/09); acumulando, cada degrau é comparado com o de baixo e o efeito de cada
componente se isola. *Descartado:* `only-skills` (skills sem o `CLAUDE.md`), que
nunca rodou e não é degrau. *Onde:* `experiment/harnesses/README.md`.

**06/10: a verificação automática é descartada, e N3 e N4 trocam de número.** Na
proposta do orientador, o N3 era a verificação (build e testes por hook) e o N4 o
processo. *Por quê:* as 62 execuções da bancada já rodavam o Maven por conta
própria (o Haiku, 20 de 20, de 4 a 14 vezes por execução); um sensor que visse mais
teria de usar a suíte ou as métricas, e o nível seria corrigido pelo gabarito. A
troca de número deixa os níveis existentes contíguos e o descartado no fim.

**06/10: a skill do N2 é pública e intacta (`gof-patterns`).** *Por quê:* foi
escrita sem conhecer as tarefas; o rascunho de skill escrito com o Claude ecoou uma
frase do enunciado ("entra um caso novo toda semana"), o que entregaria a resposta.
*Descartado:* skills próprias. A limitação (os exemplos canônicos dela caem perto
do domínio) está no OBJETIVO §6. *Onde:* `experiment/third-party/gof-patterns/`.

**06/10: o processo do N3: desenho curto em `.claude/desenho.md` antes do código e
um subagente revisor só de leitura, com o mesmo modelo (`model: inherit`) e sem o
vocabulário da régua nem dos enunciados.** *Por quê:* o desenho fica em `.claude/`
porque o anonimizador remove essa pasta, e um arquivo solto entregaria o nível na
leitura cega; o revisor não pode trazer o gabarito para dentro do harness nem usar
um modelo mais forte que o do nível.

**06/10: o teste de bancada de cada nível usa um enunciado que só pergunta o que o
agente recebeu.** *Por quê:* confere que cada nível chega inteiro (hash, skill,
subagente) sem gastar uma execução de verdade. O N2 e o N3 mostraram que a skill
chega mas não é notada espontaneamente; se ela é usada numa tarefa real é o que a
hipótese "Skills: só valem se carregadas" mede.

## 4. Enunciado

**24/09: o enunciado ganha P4 (clube) e P5 (controle negativo).** *Por quê:* o
piloto bateu no teto: com três pontos simétricos, Opus e Sonnet acertaram tudo nos
dois braços. O P4 tem um caso assimétrico (o OURO afeta três coisas) e testa a
regra de decidir a assinatura pelo caso mais exigente; o P5 é um ponto onde usar o
padrão é errado e testa a regra de não criar estrutura especulativa. *Onde:*
`experiment/prompt/README.md`.

**24/09: o exemplo 5 tem o crédito do OURO num empate de arredondamento (20,485 →
20,48).** *Por quê:* exercita o meio-para-o-par que o enunciado exige.

**24/09 e 29/09: nenhum enunciado tem palavra de arquitetura** ("padrão",
"interface", "estratégia", "extensível"…), e todos falam na voz do dono da loja.
*Por quê:* a pergunta é se o **harness** muda o desenho; se o enunciado pedisse o
padrão, o efeito viria dele. Conferido por busca.

**06/10: o contrato técnico (API, erros e exemplos) fica no enunciado.** *Por quê:*
é o que permite a mesma suíte de caixa-preta em todas as execuções. Sem ele, cada
agente inventa a sua API: um teste com um "usuário comum", sem contrato, mostrou os
modelos divergindo no formato da API, o que é outra pergunta e deixaria a correção
para ser lida à mão. É igual em todos os níveis, então não distorce a comparação; o
custo é de realismo, declarado. *Descartado:* tirar o contrato; validar a correção
com uma IA lendo o código (é opinião, varia entre leituras e seria o Claude
corrigindo o Claude).

**06/10: o cliente "entende o básico" e montou a parte técnica pesquisando.**
*Por quê:* antes a história tinha três autores (o cliente, o "desenvolvedor do
site", o "time técnico"), e um cliente leigo dificilmente entregaria um contrato.

**06/10: os exemplos não viram BDD (Gherkin).** *Por quê:* BDD é uma instrução de
processo ("comece pelos cenários") e chegaria a todos os níveis, esbarrando no N3,
que é o nível de processo; tende a levar parte dos agentes ao Cucumber, variação
que não vem do harness; e deixa o gabarito ainda mais explícito. Os exemplos já são
cenários, em português simples.

**30/09 e 06/10: as três inconsistências do Strategy.** Achadas na bancada (os
exemplos 1 a 4 vindos do piloto, sem clube nem região; a resposta do anexo com
números misturados; o limite do boleto com e sem imposto), primeiro registradas
como limitação e tratadas como observação na suíte; no V4, corrigidas no texto.
*Por quê:* o enunciado que já rodou não muda, mas o do V4 ainda não rodou: era a
chance de entrar no experimento sem defeito conhecido. *Onde:*
`experiment/prompt/README.md`, OBJETIVO §6.

**06/10: o exemplo 2 fica em Centro-Oeste.** *Por quê:* com Sul, a parcela exata
caía a 0,015 centavo do empate do arredondamento, e o exemplo testaria precisão
numérica em vez da regra.

**06/10: uma versão de enunciado que rodou nunca é editada; a nova é outro arquivo,
e a que rodou vai para `history/` com os mesmos bytes.** *Por quê:* o hash gravado
no `meta.json` de cada execução tem de continuar batendo. *Onde:*
`history/prompt-v3/`.

**07/10: o imposto por região (P5) vira seguro por região.** *Por quê:* imposto
somado no checkout não existe no Brasil. O P5 precisa só da forma (cinco regiões,
só a porcentagem muda), e o seguro a mantém: é a forma que, no imposto, já pegou um
exagero na bancada (o Haiku com harness escreveu uma interface, cinco classes e uma
fábrica). A base passou a ser "os produtos sem desconto e sem frete", o que tirou
uma leitura ambígua com o FRETEGRATIS, e o boleto passou a usar o total do pedido.
*Descartado:* embalagem para presente com preço fixo (parece tabela de preço, e o
controle negativo arriscava ficar no piso, sem ninguém exagerando); trocar o
cenário inteiro (recomeçar a preparação).

## 5. Correção (a suíte de aceitação)

**30/09: a correção é medida por uma suíte de caixa-preta pela API.** *Por quê:*
cada execução tem classes e pacotes diferentes, mas o contrato do enunciado é o
mesmo. O `mvn verify` só roda os testes que o próprio modelo escreveu. *Onde:*
`evaluation/acceptance-prototype/README.md`.

**03/10: a unidade é o caso, não o campo.** *Por quê:* contar campos dava
denominadores diferentes (um caso que devolve erro vira uma verificação em vez de
onze) e pesava cada erro pelo número de campos que ele contamina; a hipótese da
correção já está escrita em casos.

**03/10: mutantes provam que a suíte reprova o errado, e toda fronteira tem caso no
valor exato.** *Por quê:* passar a referência só mostra que a suíte não reprova o
certo. Quatro mutantes passavam pela suíte de 30/09, todos em fronteiras ("até
5 kg", "passa de R$ 1.000"…). Os mutantes se chamam MUT1, MUT2…: os nomes M1, M2
colidiam com os códigos antigos das hipóteses de Modelo.

**03/10: um ponto em que o enunciado se contradiz vira observação, não caso.**
*Por quê:* contar mediria a contradição, não o código. Decidido antes de a suíte
rodar sobre o EXT, para a escolha não ser guiada por qual braço ela favorece.
*Substituída em 06/10:* no V4 as contradições foram corrigidas e a suíte não tem
mais observações.

**06/10: o `conferir-enunciado.mjs` confere os números escritos no enunciado contra
a calculadora.** *Por quê:* os exemplos novos do V4 saíram da calculadora, e um
erro de cópia entre os dois deixaria o enunciado e a suíte se contradizendo. No
enunciado antigo, ele acusa as inconsistências conhecidas, o que prova que pega
erro.

**03/10 → 07/10: como se verifica a calculadora de referência.** O plano original
era escrever um serviço de referência do zero. Em 03/10 virou uma conferência à mão
(dois casos de colisão calculados antes de ver a calculadora, e as regras uma a
uma). Em 07/10 virou duas frentes: a **aritmética**, por implementações
independentes (as dos agentes, que chegam aos mesmos números), e as **leituras do
enunciado**, revisadas pelo Lucas. *Por quê:* refazer a conta à mão repete o que as
implementações independentes já verificam; o risco que sobra é de leitura, e só o
autor decide o que o texto quer dizer. A troca vai ao orientador.

**07/10: as cinco leituras da calculadora ficam.** Revisadas pelo Lucas, sem
divergência: o cliente OURO com FRETEGRATIS é aceito sem erro (recusar seria regra
inventada); o brinde conta os produtos antes do cupom; o LEVE3PAGUE2 conta por item
(o exemplo 4 já mostra isso); sem juros, o valor final é o total, e o centavo que
sobra cai numa parcela, como faz a operadora; na Price, só a parcela é arredondada,
o padrão do mercado. *Onde:* README da suíte, seção 4.

**07/10: o `acceptance.sh` roda a suíte num lote inteiro.** Nunca mede duas vezes
(uma execução com `acceptance.txt` é pulada); só mede execução do enunciado atual
(as da bancada, com imposto, são recusadas, salvo num ensaio declarado); confere o
ID da imagem; grava em cada resultado os hashes da suíte, da calculadora, do
enunciado e da imagem; separa "não seguiu o contrato" (todo caso com 404), "não
compilou" e "não subiu" de "errou casos"; e refaz o CSV do lote a partir dos
arquivos de cada execução, que são a fonte. *Por quê:* no V4 são 60 execuções, e
o resultado de correção tem de sair igual para todas, sem nada feito à mão no meio
e sem chance de medir de novo até dar o resultado esperado. Nome e variáveis em
inglês, a pedido do Lucas. *Onde:* `infra/scripts/acceptance.sh`.

**07/10: no V4, a aritmética precisa ser verificada de novo.** As implementações da
bancada seguem o enunciado com imposto e não servem para a calculadora com seguro.
*Por isso:* o teste de bancada do V4 inclui um quarteto de Sonnet ou de Opus, e a
suíte roda sobre ele.

## 6. Leitura de desenho (a régua)

**24/09: antes da régua, uma leitura estrutural só para ver se o sinal aparece**
(classes, `enum` abstrato, `enum` de dados ou `switch` em cada ponto), congelada no
git antes de abrir o mapa. *Por quê:* saber se valia construir a régua; ela mostrou
o teto do piloto.

**24/09: toda leitura cega é commitada antes de abrir o mapa de anonimização, e os
mapas só entram no git depois.** *Por quê:* a data do commit é a prova de que a
leitura não viu o braço. É o que faz a leitura valer.

**27/09: a régua em três níveis** (propriedades observáveis, ficha do padrão,
gabarito do enunciado), **com evidência `arquivo:linha` obrigatória, e quem lê não
julga "é Strategy?".** Registra o que vê (onde mora cada caso, como é escolhido, se
a assinatura comporta o caso exigente…), e na dúvida marca indeterminado; a ficha
converte depois. *Por quê:* duas pessoas aplicam e chegam ao mesmo resultado, e um
padrão novo só escreve o terceiro nível. *Onde:* `evaluation/regua.md`.

**26 e 27/09: as onze regras de leitura da régua**, tomadas pelo Claude por
delegação do Lucas depois das rodadas de calibração, cada uma com o motivo no §5
da régua. Em resumo: fábrica com `switch` é acerto (escolhe num lugar só); no
controle negativo, `switch` que devolve o valor é acerto (só estrutura por caso é
exagero); a assinatura conta em todos os casos exigentes; o custo de um caso novo
não conta o arquivo da estrutura e olha a edição, não o arquivo; chamar uma função
comum não é repetição; pergunta no contrato, lida sem nomear o caso, comporta o
caso exigente; parâmetro sem uso não é estrutura especulativa; a parte comum do P3
sai do gabarito (media o formato do contrato); um `if` que faz a conta de um caso é
condicional no cálculo; um `switch` que calcula conta no custo.

**27/09: a calibração com dois leitores do Claude, um deles no lugar do Lucas.**
Três rodadas, de 50 células indeterminadas a 0. *Por quê:* testar se a régua é
clara para quem a segue à letra antes de gastar a leitura humana. Não mostra que
uma pessoa leria igual: a medida que vale segue sendo a leitura do Lucas.
*Onde:* `evaluation/calibracao-relatorio.md`.

**Plano, Parte 4: leitura dupla (Lucas e Claude) com kappa, e a do Claude isolada
num container que só vê os pacotes cegos.** *Por quê:* hoje a leitura do Claude
confia que o agente não abriu o mapa; isolada, fica impossível. *07/10:* o
`ler-cego.sh` fica para depois, porque só é preciso antes da leitura do V4.

**07/10: o Jev (TypeSafe AI) não entra no estudo.** *Por quê:* a documentação
oficial diz que ele não é calculadora (fora da correção) e que erra mais com
raciocínio de vários passos, estado grande e texto que "argumenta pela própria
classificação", que é o que a régua pede e o que os comentários dos agentes fazem.
Pior: um leitor levado por nomes de classe daria nota maior aos níveis com a skill
de padrões, e o viés andaria junto com o tratamento. *Ficou como ideia futura:* um
nível de sensor de desenho no harness, com perguntas pequenas sobre cada trecho
editado.

## 7. Métricas automáticas

**06/10: SonarQube e CK como instrumento secundário de desenho.** Pedido na
orientação de 12/09. Complementam a régua, não a substituem, e não decidem
hipótese. *Por quê:* medem tamanho, complexidade, duplicação, acoplamento e
coesão, não se o padrão foi aplicado onde devia. *Descartado:* ArchUnit, que
exigiria escrever testes de arquitetura para cada padrão, outro instrumento a
construir e validar. *Onde:* `evaluation/tools/README.md`.

**06/10: versões travadas, e uma análise nunca é refeita.** SonarQube e scanner
pelos digests das imagens, o perfil de regras salvo, o CK por hash. *Por quê:* uma
versão nova mudaria os números entre execuções do mesmo lote.

**06/10: o CK é compilado de um commit, não a versão publicada.** *Por quê:* a 0.7.0
ignorava todo `record` (31 de 40 classes num pacote de teste).

**06/10: o corpo de cada constante de `enum` fica numa coluna própria
(`ck_anonymous`), fora das médias.** *Por quê:* o CK conta cada um como classe
anônima, e isso distorceria a comparação entre desenhos com `enum` e com classes.

**06/10: quatro métricas pré-registradas como conferência de hipóteses** (OBJETIVO
§4.9): complexidade cognitiva, duplicação, número de classes, e CBO e LCOM.
*Por quê:* uma conferência independente da régua, decidida antes dos dados. Uma
divergência manda reler o par e é registrada; nunca se resolve trocando um número
pelo outro.

## 8. Regras de leitura das hipóteses

**24/09: a leitura é por pares, não por um teste de dois grupos.** *Por quê:* com 3
réplicas por braço, o Mann-Whitney tem só 20 arranjos e o menor p possível é 0,10:
nunca chegaria a 5%. Os pares simultâneos usam o desenho que existe.

**26/09: as regras de leitura são decididas antes dos dados, e são descritivas.**
*Por quê:* com poucos pares, um teste por modelo não chega a 5% (com 5 pares, o
menor p é 0,0625); decidir depois permitiria escolher a regra pelo resultado. O
teste de sinal sobre todos os pares é publicado ao lado, como informação.

**05/10: as hipóteses têm nomes ("Eixo: afirmação") no lugar de códigos, e ★ marca
as principais.** *Por quê:* o §4 só se lia com os códigos decorados. Os códigos
antigos ficam no §7, para ler os commits anteriores.

**05/10: três correções feitas antes de ver qualquer resultado do EXT:** a regra
do teto (um modelo que já acerta tudo não conta para "melhor" e é lido por "Modelo:
no teto, não piora"); na medida sim/não, contam só os pares não empatados; a
hipótese do modelo ganha regra própria, o saldo de pares. *Por quê:* sem a primeira,
com Opus e Sonnet no teto, a hipótese do desenho nunca poderia ser apoiada; sem a
segunda, com quase tudo empatado, a regra nunca seria alcançada.

**06/10: as principais comparam N1 com N0.** *Por quê:* é o contraste mais limpo,
em que só o `CLAUDE.md` muda. Os degraus de cima (N2 × N1, N3 × N2) têm hipóteses
próprias (§4.8).

**06/10: a tendência de N0 a N3 é lida pelo teste de Page, só como informação.**
*Por quê:* o orientador sugeriu o Jonckheere-Terpstra; o Page é a versão pareada
dele, que respeita os quartetos. Não decide hipótese, porque as conclusões vêm das
hipóteses.

**06/10: o tamanho do efeito é o saldo de pares dividido pelo número de pares** (a
versão pareada do Cliff's delta), publicado ao lado, sem mudar o veredito.

**07/10: o "não piora" olha o saldo (piores − melhores), apoiado até 2.** *Por
quê:* sem efeito nenhum, alguns pares saem diferentes por acaso para os dois lados;
com 3 melhores e 3 piores, contar só os piores declararia que o harness piora.
*Descartado:* "no máximo 2 piores", a regra de 06/10. *Onde:* OBJETIVO §4.1.

**07/10: o "altera", em medida contínua, exige 12 de 15, e 10 ou 11 é
inconclusivo.** *Por quê:* 12 é o menor número cuja chance de sair por sorte fica
abaixo de 5% (3,5%); 11 já tem 11,8%. Não chegar à prova não prova o contrário: só
os pares divididos (9 a 6 ou mais equilibrado) contrariam. *Onde:* OBJETIVO §4.1,
com a tabela das chances.

**07/10: o "altera", em medida sim/não, exige 5 pares não empatados, todos do mesmo
lado; 3 a 0 ou 4 a 0 é inconclusivo.** *Por quê:* com 4, a chance de sorte era
12,5%; com 5, 6,3% (declarado, um pouco acima da convenção); com 6, 3,1%, mas o
exagero é raro e a hipótese principal ficaria inconclusiva quase sempre.
*Descartado:* 4 (fraco demais) e 6 (quase impossível de alcançar).

## 9. Integridade e reprodutibilidade da bancada

**24/09 e 26/09: tudo sai do git com LF, e os arquivos com hash citado são
protegidos por regra própria no `.gitattributes`.** *Por quê:* a máquina tem
`core.autocrlf=true`; sem as regras, o git converteria o fim de linha e os hashes
do README deixariam de bater sem que uma palavra mudasse. Conferido num clone: 4
dos 8 hashes divergiam antes.

**26/09: as sessões abertas no TCC_V3 ignoram o `CLAUDE.md` da raiz do repositório
(`claudeMdExcludes`).** *Por quê:* o Claude Code lê os `CLAUDE.md` das pastas acima,
e aquele descreve o repositório geral e mandava para uma pasta que não existe mais.
Não toca o experimento: o container só monta o workspace e o enunciado.

**29/09: o `PROMPT_FILE` tem de ser caminho absoluto.** *Por quê:* ele vira a
origem de uma montagem do Docker, que recusa caminho relativo, e a execução
falharia depois de já ter criado a pasta da run.

**29/09: nunca usar o "reset de fábrica" do Docker Desktop.** *Por quê:* apagaria a
imagem `experimento-harness:v3`, cujo digest está no pré-registro.

**05/10: as pastas em inglês, com três exceções.** Ficaram em português o caminho
dentro do container (`/experimento/prompt.md`), para os lotes novos verem o mesmo
ambiente do EXT; `infra/docker/aquecimento`, que o `Dockerfile` copia para a imagem
(mudar exigiria reconstruí-la); e as pastas internas dos harnesses de
`history/bench-test`, cujos caminhos entram no hash gravado naqueles `meta.json`.
*Por quê:* preferência do autor, sem mudar nenhum hash: conferido que os scripts
renomeados fazem exatamente o mesmo.

**06/10: depois de cada mudança na bancada, um teste de bancada barato** (Haiku,
esforço baixo, um enunciado que só pergunta o que o agente recebeu). *Por quê:*
conferir que nada quebrou sem gastar uma execução de verdade.

## 10. Documentação

**24/09: cada documento faz um trabalho só, e o git é o diário.** 1.753 linhas de
documentação viraram um README de 130. *Por quê:* na v2, os documentos descreviam
o desenho e narravam como se chegou nele ao mesmo tempo, e 27 dos 78 avisos em
destaque existiam só para contar o que mudou. As mensagens de commit carregam o
motivo de cada passo.

**26/09: o OBJETIVO diz para quê; o README diz como roda.** *Por quê:* faltava um
lugar para a pergunta, as hipóteses e o que conta como resposta.

**03/10: o plano de implementação entra no git.** *Por quê:* estava fora dos
commits, e as trocas de método (como a da referência escrita do zero) precisavam
de registro datado.

**07/10: este `DECISOES.md`.** *Por quê:* o porquê das decisões estava espalhado
entre mensagens de commit, cinco documentos e conversas; algumas (não tirar o
contrato, não usar BDD, não usar o Jev) só existiam na conversa. O git continua
sendo o diário; este arquivo é o índice dele, legível sem `git log`.

**07/10: o `MINIPLANO.md` resume o que falta antes do V4.** *Por quê:* o plano é
longo; o miniplano é a lista do dia, com quem faz cada item.
