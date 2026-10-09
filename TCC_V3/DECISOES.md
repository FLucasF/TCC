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
achar defeito antes de custar dado; o `aggregate.mjs --prefix` separa sem apagar dado
bruto.

**26/09: um experimento só (o `EXT`), e o `BATCH` vira piloto.** *Por quê:* o
estímulo do piloto é outro (três pontos), e as execuções não são comparáveis. Ele
fica como o motivo de P4 e P5. *Onde:* `experiment/prompt/README.md`.

**26/09: a avaliação fica numa pasta por padrão, e o `anonymize.mjs --padrao`
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

**03/10: o `verify.mjs` vai para a fase 2, roda só sobre os lotes da análise, e
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

**06/10: o script do quarteto (`run-levels.sh`, que se chamou `rodada-niveis.sh`
até 07/10) confere tudo antes de lançar qualquer execução, e chama o
`run-one.sh` sem mudá-lo.** Um modelo por vez é o mais seguro para a cota.
*Por quê:* um quarteto com um nível a menos não serve para a comparação, e é melhor
falhar sem gastar cota; o `run-one.sh` está congelado.

**07/10: o `verify.mjs` será enxuto (4 checagens), e o `analisar-rodada.mjs` sai
do V4.** Ficam as checagens que acusam o que nenhuma outra peça acusa: a leitura
bate com o mapa, o CSV bate com os `meta.json`, o desenho está completo, e o
gabarito é do enunciado das execuções. *Por quê:* a contagem de acertos e os
totais para o texto já saem dos CSVs, e repeti-los seria um segundo cálculo para
manter; o `analisar-rodada.mjs` resumia rodadas antes de o `aggregate.mjs` e o
`verify.mjs` existirem. *Descartado:* manter as 6 checagens do plano de 03/10.
*Onde:* plano, Parte 3; a lista dos scripts do V4 está no README.

**06/10: o cache de prompt não é desligado.** Os tokens ficam separados no
`meta.json`. *Por quê:* um usuário real usa cache, e o isolamento das execuções não
teria como impedi-lo (ele fica nos servidores). Ele muda tempo e tokens, não o
código gerado. *Onde:* OBJETIVO §4.4.

**07/10: o V4 fica só com o Strategy; o State vai para `history/state/`.**
*Por quê:* o State provou que a bancada aceita um segundo padrão (enunciado, suíte e
agregação funcionaram); a régua dele nunca foi calibrada. Com um padrão, o V4 cai
de 120 para 60 execuções, metade da cota. As hipóteses "entre padrões" ficam
escritas para o futuro. *Onde:* `history/state/README.md`.

**09/10: o V4 passa a ter 5 modelos × 5 réplicas (100 execuções), escolhidos por um
mapa.** *Proposta, a confirmar com o professor.* Antes do V4, cada candidato (Haiku
4.5 e 5.5, Sonnet 4.5, 4.6 e 5, Opus 4.6 e 5) roda uma vez no N0, e ficam 5 que
formem uma escada: todos compilam, há modelos no teto e pelo menos 2 fora dele,
olhando a suíte e o P4 pelo Semgrep. O Haiku 4.5 fica fixo como o mais fraco; o
Fable fica fora (escolha do Lucas: caro, e cairia no teto). *Por quê:* no ensaio do
EXT, o Sonnet e o Opus acertaram tudo nos dois braços; com 3 modelos, só um ficava
fora do teto, e a regra direcional ("na maioria dos modelos fora do teto") virava a
opinião de um modelo só. *Descartado:* os 14 modelos Claude no experimento (280
execuções, quase todas no teto); o Haiku 5.5 como o fraco (pioraria o teto); 5 × 3
réplicas (os mesmos 60, mas poucas réplicas por modelo), que fica de plano B se a
cota apertar. *Teste:* o Haiku 4.5 no N0, com o enunciado do V4, compilou, subiu e
fez 20 de 21 (`TESTE-MAPA-01`). *Pendente:* as regras do §4.1 foram calibradas para
15 pares; com 25, os limites precisam ser recalculados antes de congelar.

**09/10: os quartetos rodam um por vez, em ordem sorteada a cada rodada, e um quarteto
interrompido é refeito inteiro.** *Proposta.* Cada rodada tem uma réplica de cada
modelo, numa ordem sorteada. *Por quê:* o que protege a comparação é o quarteto do
mesmo instante; a ordem sorteada espalha um dia ruim por todos os modelos, em vez de
cair num só. *Descartado:* rodar tudo junto. O Docker tem 15,5 GB, e 20 containers
compilando ao mesmo tempo estouram a memória; a cota acabaria no meio; e muitas
sessões na mesma conta podem receber erro de limite, o que muda o comportamento do
agente. *Medido no f4 (09/10):* um quarteto de Haiku 4.5 gastou uns 10 a 15% da
janela de 5 h, e o N3 processou cerca de 3 vezes mais tokens que o N0.

**09/10: a regra de escolha dos 5 modelos, escrita antes de rodar o mapa.** Candidatos:
Haiku 4.5, Haiku 5.5, Sonnet 4.5, Sonnet 4.6, Sonnet 5, Opus 4.6 e Opus 5. Cada um roda
**uma vez no N0**, com o enunciado do V4, e recebe a suíte, o Semgrep e a nota A. O
Haiku 4.5 (`TESTE-MAPA-01`: 20 de 21, P4 errado) e o Opus 5 (`TESTE-NIVEIS-01-OPUS-N0`:
21 de 21, P4 certo) já rodaram assim; faltam 5 execuções. **No teto** quer dizer 21 de
21 e o P4 certo (isolado, e escolhido por consulta ou fábrica). A regra: (1) o Haiku 4.5
(o mais fraco) e o Opus 5 (o topo) entram sempre; (2) sai quem não compila ou não sobe;
(3) dos que sobram, ordenados pela nota A, entram **o de menor nota, o do meio e o de
maior nota** (empate: o mais barato); (4) se o grupo final tiver menos de 2 modelos fora
do teto, o de maior nota escolhido em (3) é trocado pelo próximo abaixo dele que esteja
fora do teto. *Por quê:* a regra fica fixa antes de ver qualquer resultado e só olha o
N0, que não diz nada sobre o efeito do harness. *Limite:* uma execução por modelo é
ruidosa (o Haiku 4.5 fez 20 e depois 17 no N0); o mapa só põe os modelos em degraus.

**09/10: o mapa rodou, e a regra escolheu Haiku 4.5, Sonnet 4.5, Opus 4.6, Sonnet 5 e
Opus 5.** As 5 execuções que faltavam (`TESTE-MAPA-01-*-N0`) rodaram juntas, às 18:04,
com o enunciado do V4; todas compilaram, subiram e responderam com o modelo pedido.
Notas A no N0 (suíte, Semgrep): Haiku 4.5 57,8 (20 de 21, nenhum ponto certo); Sonnet
4.5 66,1 (15 de 21, P1 e P4); Haiku 5.5 70,0 (21, só o P1); Opus 4.6 80,0 (21, P1 e P2,
**P4 errado**); Sonnet 4.6, Sonnet 5 e Opus 5 100. Pela regra: (1) Haiku 4.5 e Opus 5;
(2) ninguém sai; (3) o menor é o Sonnet 4.5, o do meio o Opus 4.6, e o maior um empate
entre o Sonnet 4.6 e o Sonnet 5, decidido pelo mais barato: o **Sonnet 5** (menor preço
por token, e gastou menos no mapa, $1,41 contra $1,66); (4) três fora do teto (Haiku
4.5, Sonnet 4.5, Opus 4.6), sem troca. O Haiku 5.5 fica fora. *Conferido à mão, sem
mudar nada:* as linhas que decidem a ordem são reais (no Opus 4.6, um `if` que zera o
frete do `"OURO"` e um `switch` sobre o nível que calcula o crédito no serviço; no
Sonnet 4.5, um `switch` sobre o tipo do cupom e um `if` do cartão no meio da conta). A
ordem não depende do P4 do Opus 4.6: com ele certo, a nota seria 90, ainda no meio. *O
Haiku 4.5 não foi medido de novo:* foi medido com a suíte anterior (`f77d1488…`), que
tem os mesmos 21 casos e só não separava contas e recusas; a única falha dele é uma
recusa com o código errado, que vale 0 nas duas, então os números (12 contas, 8
recusas) saem do resultado gravado. *Detalhe do Sonnet 4.5:* 4 das recusas falharam
com o 400 genérico do Spring, porque os campos do pedido viraram `enum` Java e um valor
inexistente para na conversão do JSON, antes da validação dele. *Corrigido no
`OBJETIVO`:* o rascunho listava só quatro candidatos ao lado dos dois fixos (sem o Opus
4.6); a regra que vale é a deste arquivo, commitada às 16:51, antes do mapa, com os
cinco. *A ordem das rodadas:* sorteada uma vez (`infra/scripts/draw-order.mjs`, semente
**20261009**), em `experiment/ordem-v4.csv`: cada rodada tem os 5 modelos em ordem
própria, e as rodadas vão em sequência. *Onde:* o `acceptance.txt` de cada execução
(os CSVs de `analysis/` não vão para o git e se refazem com o `acceptance.sh` e o
`detect.sh`), `OBJETIVO` §2, `experiment/desenho-v4.json`.

**09/10: um lote exploratório com os outros modelos, separado do confirmatório.**
*Decisão do Lucas, depois do mapa.* Ele quis mostrar todos os modelos do Claude, menos
o Fable. Trocar os 5 do confirmatório agora seria um desvio do pré-registro (a regra
foi fixada antes do mapa), e os modelos a mais quase todos ficariam no teto, onde não
há efeito para medir. Por isso, um segundo lote, `V4-EXPLOR`, com **Haiku 5.5, Sonnet
4.6, Opus 4.7, Opus 4.8 e Sonnet 5.5**, no mesmo desenho (4 níveis × 5 réplicas, 100
execuções), rodado **depois** do confirmatório, com as mesmas medidas automáticas e
**sem hipótese nem leitura humana**: entra no texto como tabelas exploratórias. É
**obrigatório** (o Lucas descartou deixá-lo opcional, "se sobrar tempo"): o trabalho
só fecha com os dois lotes. Os IDs
novos foram testados antes com o enunciado barato da bancada (`TESTE-IDS-01-*`,
centavos cada): Opus 4.7, Opus 4.8 e Sonnet 5.5 responderam com o modelo pedido; o
**Opus 5.5 ficou fora**, porque a API o recusa no Claude Code 2.1.269 da imagem ("does
not support this model; version 2.1.280 or newer"). *Descartados:* uma imagem à parte
com o Claude Code novo só para ele (a comparação com os outros misturaria o modelo e a
versão do Claude Code) e atualizar a bancada inteira (refazer o mapa e os testes). *O
gabarito* passa a valer para os dois lotes (`lotes: V4-STRATEGY, V4-EXPLOR`). *A
ordem:* sorteada uma vez, semente **20261010**, em `experiment/ordem-v4-exploratorio.csv`;
o comando de cada quarteto leva `DESENHO=` para o arquivo do exploratório. *A cota:*
Haiku 5.5 ~5% por quarteto, Sonnet 4.6 ~12%, Sonnet 5.5 ~10%, Opus 4.7 e 4.8 ~16% cada
(os três últimos estimados por modelos vizinhos, sem N0 medido); uma rodada ~60% de
uma janela, o lote ~3 janelas; com o confirmatório, ~5,5 janelas no total. *Achado no
teste:* o `run-levels.sh` não achava um `DESENHO` em caminho relativo (o que o arquivo
de ordem usa); corrigido antes de qualquer execução. *Onde:* `OBJETIVO` §2,
`experiment/desenho-v4-exploratorio.json`.
*O N0 dos três modelos novos, para saber se o lote vale (09/10, depois do sorteio da
ordem e sem mudar nada do desenho):* o Opus 4.7, o Opus 4.8 e o Sonnet 5.5 rodaram uma
vez no N0, como o mapa (`TESTE-MAPA-01-*-N0`, 19:18). Os três fizeram 21 de 21. Notas
A: **Opus 4.7 60,0** (os quatro pontos errados no Semgrep: tudo num serviço só, com
`switch`), Opus 4.8 100, Sonnet 5.5 100. Com o Haiku 5.5 (70,0) e o Sonnet 4.6 (100)
do mapa, o exploratório tem **dois modelos fora do teto** (Haiku 5.5 e Opus 4.7), e o
estudo todo, cinco. A linha do Opus no N0 não é uma escada (4.6: 80, 4.7: 60, 4.8 e 5:
100); a do Sonnet é (4.5: 66,1; 4.6, 5 e 5.5: 100). *Limite:* uma execução por modelo.
*O Lucas manteve o lote como obrigatório* depois de ver esses números; o N0 não diz
nada sobre o efeito do harness, que é o que o lote mede.

**09/10: as propostas do redesenho são decididas pelo Lucas, e o professor vê o
resultado analisado.** O professor pediu reunião só com um resultado pronto e
analisado. Por isso, as entradas de 09/10 marcadas como "proposta, a confirmar com o
professor" passam a ser decisões do Lucas: a avaliação automática na base com a
leitura em amostra, o Semgrep no lugar do PR-Agent, 5 modelos × 5 réplicas pelo mapa
e o avaliador único com releitura. Ficam em aberto só o peso da recusa com status de
sucesso e os pesos da nota. Duas decisões novas: **a manutenção fica fora do V4**
(vira trabalho futuro, e a hipótese *Desenho: caso novo com pouca edição* deixa de
ser medida), e **o PR-Agent fica fora**, nem como ilustração. *Por quê:* o que dá
valor ao pré-registro é o commit do `OBJETIVO` congelado antes de rodar, e não uma
aprovação prévia. *O risco aceito:* se o professor discordar de uma escolha depois,
ela vira limitação no texto, porque mudá-la exigiria rodar de novo. *Onde:*
`MINIPLANO-V4.md`, passos 1 e 7.

**09/10: a cota medida, e o plano de cota do V4.** Na assinatura, numa janela de 5 h:
um quarteto do **Haiku 4.5** gasta **10 a 15%**; um quarteto do **Opus 5**, cerca de
**80%**. O quarteto do Opus começou com a janela em 20% e o N3 bateu no limite
("You've hit your session limit") perto do fim. *A regra:* um quarteto do Opus só
começa com a janela **zerada**, e nada mais roda nela; os quartetos dos modelos
menores podem dividir uma janela, desde que a soma estimada fique abaixo de 80%.
Quarteto cortado pela cota é refeito inteiro (o `verify.mjs` acusa, ver §9). *A
conta:* os 5 quartetos do Opus 5 pedem **5 janelas**; os 5 do Haiku 4.5, **1**; os
dos outros 3 modelos saem do mapa (uma execução no N0 dá o custo do N0, e o N3 gasta
cerca de 3 vezes o N0). *Limite:* o limite semanal da assinatura não foi medido; se
ele apertar, as rodadas se espalham por mais dias, sem mudar o desenho (a ordem das
rodadas é sorteada, e cada quarteto é inteiro). *Onde:* `MINIPLANO-V4.md`, passo 2.
*Atualizada no mesmo dia:* o Lucas assinou o **Claude Max**, e os percentuais acima
são do plano anterior. A janela fica maior, mas a regra fica como margem de
segurança, porque um corte custa o quarteto inteiro. As 5 execuções do mapa medem de
novo o gasto por janela no plano novo, e o plano de cota é refeito com esse número.
Trocar de plano não muda o experimento: o modelo, o effort e a bancada são os
mesmos, e a assinatura só muda quanto cabe numa janela.
*Refeito com o mapa, no Max (09/10):* as 5 execuções do mapa custaram $6,94 em preço
de lista e levaram a janela de 23% para 31%, cerca de **$0,87 por 1%** (a leitura é
inteira, então ±12%). Estimativa por quarteto: Haiku 4.5 ~4% ($3,11, medido no f4);
Sonnet 4.5 ~7%; Sonnet 5 ~10%; Opus 5 ~12% ($10,30, medido); Opus 4.6 ~16%. Os três
sem quarteto medido usam o N0 do mapa × 6,4 (a maior razão quarteto/N0 medida, a do
Haiku; a do Opus foi 4,9). Uma rodada (os 5 quartetos) dá **cerca de 50% de uma
janela**, e o lote todo, cerca de 2,5 janelas. *A regra nova, que substitui a do Opus
em janela zerada:* um quarteto só começa com **pelo menos 30% da janela livre** (perto
do dobro do maior quarteto). *Limite:* o limite semanal do Max continua não medido.
*Refeito de novo, com o exploratório (09/10):* as 3 execuções novas custaram $3,97 e
levaram a janela de 33% para 39% ($0,66 por 1%). Juntando as duas medidas, 14% por
$10,91: **~$0,78 por 1%**. Por quarteto (quarteto medido, ou o N0 × 6,4):
confirmatório, Haiku 4.5 ~4%, Sonnet 4.5 ~8%, Sonnet 5 ~12%, Opus 5 ~13%, Opus 4.6
~18%, uma rodada ~55%, o lote ~2,8 janelas; exploratório, Sonnet 5.5 ~3%, Haiku 5.5
~6%, Opus 4.7 ~12%, Sonnet 4.6 ~14%, Opus 4.8 ~18%, uma rodada ~53%, o lote ~2,7
janelas. **Total: ~5,5 janelas.** Com o maior quarteto em ~18%, a regra sobe para
**pelo menos 35% da janela livre** antes de começar um quarteto. As estimativas
substituem as do lote exploratório escritas acima (as do Opus 4.7 e do Sonnet 5.5
eram altas demais).

## 3. Níveis de harness

**26/09: uma pasta por versão de harness, escolhida por `HARNESS`.** *Por quê:*
testar `CLAUDE.md`, skills e combinações sem trocar o conteúdo de uma pasta que já
rodou; sem a variável, o `run-one.sh` faz o mesmo de antes, com o mesmo hash.

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

**07/10: o conhecimento técnico do cliente é o de um curioso que estuda o básico de
programação, e o anexo sai do jargão.** Ficam o endereço e os nomes dos campos;
saem o verbo (`POST`), os números de status e o `null`, trocados por frases
simples, e os dois exemplos em JSON viram tabelas (campo, o que é, exemplo), com
uma frase sobre os tipos e o formato dito uma vez. *Risco aceito:* sem o exemplo
pronto, o agente monta a estrutura a partir da tabela, e quem montar diferente falha
na correção por formato, não por conta. *Por quê:* um dono de loja não saberia HTTP; um curioso que estuda o
básico saberia o que é um endereço e um JSON de exemplo, e é o mínimo para a suíte
testar. *Consequência:* a suíte aceita qualquer sucesso (2xx) e qualquer recusa
(4xx) com o código certo, e o `acceptance.sh` conta 405 (outro método) como "não
seguiu o contrato". *Descartado:* um enunciado em duas fases (o dono leigo, depois
a equipe do site com o contrato), que mediria o desenho sem nenhuma pista técnica,
mas mudaria a bancada inteira por um ganho pequeno: o contrato só fixa a fronteira
do serviço (o que entra e o que sai), não o desenho de dentro, que é o que o
estudo mede. Uma voz de estudante de computação também foi considerada e
descartada: o pedido era só o nível técnico, não mudar quem pede.

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

**09/10: o enunciado do V4 fica como está, depois da revisão do Lucas.** Duas
perguntas, as duas decididas por manter: (1) **o contrato fica fixo** (o endereço,
os nomes dos campos, o campo `erro`, os códigos e a ordem das recusas). *Por quê:* a
suíte é caixa-preta e precisa chamar todos os pacotes do mesmo jeito. Achar o
endereço por script daria certo na maioria das vezes, mas criaria uma falha que não
tem a ver com o harness, e mapear campos ou códigos escolhidos pelo modelo exigiria
interpretação, que saiu da avaliação. O "como o modelo trata o erro" continua livre
e medido: o status HTTP não é dito (daí o 0,5) e a organização da validação aparece
no SonarQube e no CK. *Descartado:* soltar só o nome do campo do erro (a suíte
procuraria o código em qualquer campo); seguro, mas sem ganho para a pergunta. (2)
**o P3 (pagamento) fica sem frase de crescimento**, ao contrário do P1, do P2 e do
P4. *Por quê:* o P3 é secundário, e no piloto os modelos já separavam o pagamento
sem a frase. *Limite:* um `switch` sobre as 3 formas conta como erro no Semgrep sem
que o enunciado dê motivo para separá-las; vai para as limitações. Os 73 números
foram conferidos de novo (`check-prompt.mjs`: 73 de 73).

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

**06/10: o `check-prompt.mjs` confere os números escritos no enunciado contra
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

**09/10: recusar com status de sucesso conta como falha, e a suíte passa a relatar as
contas e as recusas separadas.** *Decisão do Lucas.* No quarteto do Haiku
(`TESTE-NIVEIS-01`), o N1 e o N2 devolveram o código de erro certo com status de
sucesso. Isso é prática ruim, e um programador experiente usaria um status de erro;
então o caso falha. *A separação (proposta):* os 12 casos de conta e os 9 de recusa
em dois números, para o texto mostrar por que uma execução caiu. Só o N1 e o N2
fizeram isso, o que pode ser efeito do harness. *Descartado:* voltar os números de
status ao enunciado (o cliente curioso não os conhece) e aceitar qualquer status
(esconderia uma diferença que o harness pode causar).
*Ajustada no mesmo dia, pelo Lucas:* o erro não zera o caso; tem peso próprio. Cada
caso de recusa vale 1 com o código e o status certos, **0,5** com o código certo e
status de sucesso (valor proposto, a confirmar antes de congelar a suíte), e 0 com o
código errado ou sem recusa. *Por quê:* a regra de negócio estava certa e só a
convenção do HTTP falhou; zerar o caso trataria isso como não saber o erro. No
quarteto do Haiku, o N1 vai de 11 para 13,5 de 21 e o N2 de 12 para 14,5; o N0 e o
N3 não mudam.

**09/10: a calculadora do seguro está confirmada pelo quarteto do Opus 5.** As
quatro execuções do `TESTE-NIVEIS-01-OPUS` (N0 a N3, escritas sem ver a calculadora
nem a suíte) fizeram **21 de 21** cada uma, medidas com a suíte atual
(`7742de00…`): 12 de 12 contas e 9 de 9 recusas. *Por que vale:* quatro
implementações independentes, escritas só a partir do enunciado, chegando aos mesmos
valores da calculadora nos 21 casos tornam improvável um erro dela, e nenhum caso
falhou em todos os modelos fortes.
Fecha o passo 2 do `MINIPLANO-V4` e a pendência da entrada de 07/10 sobre a
calculadora com seguro. *Limite:* o N3 foi cortado pela cota no fim (ver §2), mas o
código já estava completo; como teste, serve para a calculadora, e num lote que vale
ele seria refeito.

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

**07/10: a calibração humana sobre 8 pacotes fora da análise** (os dois do
`TESTE-P4`, de 5 pontos, e os seis do piloto que mais travaram os agentes), com o
Lucas decidindo cada valor e o Claude só explicando a régua. *Por quê:* os SMOKE
estão misturados com os BATCH, e separá-los exigiria abrir o mapa. *Onde:*
`PLANO-TESTE-APRENDENDO.md`.

**08/10: a calibração humana para por uma regra, não pelo número de pacotes.**
Mínimo: o `L3RG` e o `L7MG` inteiros (5 pontos, o formato do V4). Depois, pacotes
do piloto até **dois seguidos passarem sem hesitação**, com pelo menos um código
que não seja `classes`. Deve ficar entre 12 e 16 leituras de ponto, em vez de 28.
*Por quê:* a calibração só serve para a régua ganhar regras; ela não entra em
estatística nenhuma (o kappa é da Parte 4, sobre os pacotes do V4), e o critério
escrito sempre foi "pronto quando não houver hesitação". O 8 foi uma escolha de
07/10, não uma exigência. *Descartado:* parar no `L3RG`, que só tem um tipo de
desenho (classes com catálogo) e nunca testaria `enum`, `switch` nem mapa.
*O que pesa contra cortar mais:* na Parte 4 o Lucas lê à mão os pacotes do V4, e
cada hesitação resolvida agora poupa a mesma dúvida em dezenas deles.
*Substituída no mesmo dia:* a calibração do L3RG ficou de lado (entrada "a
calibração humana do L3RG fica de lado", abaixo).

**Plano, Parte 4: leitura dupla (Lucas e Claude) com kappa, e a do Claude isolada
num container que só vê os pacotes cegos.** *Por quê:* hoje a leitura do Claude
confia que o agente não abriu o mapa; isolada, fica impossível. *07/10:* o
`blind-read.sh` fica para depois, porque só é preciso antes da leitura do V4.

**07/10: o Jev (TypeSafe AI) não entra no estudo.** *Por quê:* a documentação
oficial diz que ele não é calculadora (fora da correção) e que erra mais com
raciocínio de vários passos, estado grande e texto que "argumenta pela própria
classificação", que é o que a régua pede e o que os comentários dos agentes fazem.
Pior: um leitor levado por nomes de classe daria nota maior aos níveis com a skill
de padrões, e o viés andaria junto com o tratamento. *Ficou como ideia futura:* um
nível de sensor de desenho no harness, com perguntas pequenas sobre cada trecho
editado.

**08/10: a calibração humana do L3RG fica de lado.** Depois do P4 do L3RG, o Lucas
achou o tamanho da avaliação inviável e decidiu refazer a validação depois, sobre a
régua nova. As planilhas e as notas (`calibration-strategy/leitura-lucas.csv` e
`notas-lucas.md`) ficam como registro, e as propostas de texto que saíram delas
(definir "nomear" e "valor", o `if` de validação dentro da tabela, o aviso sobre o
ruído da busca) valem para a régua nova. *Substitui* a regra de parada de 08/10.
*Onde:* `evaluation/GUIA-DA-REGUA.md`.

**09/10: a avaliação passa a ser automática na base, com leitura humana em amostra
para conferir, e o Claude deixa de ser leitor.** *Proposta, a confirmar com o
professor.* As camadas: correção (a suíte), qualidade (SonarQube e CK), padrão (o
Semgrep, abaixo) e a leitura do Lucas numa amostra. *Por quê:* é o que o professor
propôs em 12/09 ("base em métricas automáticas; rubrica humana cega como
complemento, numa amostra"); a leitura de tudo à mão (1.560 respostas) era inviável;
e a leitura do Claude sobre código do Claude é o risco "avaliador da mesma família"
da tabela dele. *Descartado:* a régua de 8 propriedades lida nos pacotes inteiros
pelo Claude e pelo Lucas. A assinatura, a parte comum e o custo do caso novo saem da
leitura: a assinatura é quase o "espalhado" do P4, o SonarQube mede a duplicação, e
a manutenção mede o custo de verdade. *Ensaio:* aplicada ao EXT em 08 e 09/10 (a
suíte do V3, as métricas e a leitura de 6 pacotes sorteados).

**09/10: o padrão é medido pelo Semgrep, com regras próprias, do P1 ao P5.** *Decisão
do Lucas.* Regras determinísticas, sem IA, sobre os nomes dos casos do gabarito: em
cada ponto positivo, se um caso aparece numa condição fora da sua unidade
(localização) e se a escolha do caso faz a conta (seleção); no controle negativo, a
forma e a proporção. *Por quê:* mede os 100 pacotes, sem IA e com o mesmo resultado
sempre, e é o que permite a hipótese do desenho continuar principal. *Descartado:* o
PR-Agent, sugerido pelo professor (é um modelo de linguagem: IA avaliando IA, com
resultado que não se repete, em texto livre, e feito para revisar mudanças, não
projetos inteiros); e linters prontos (PMD, Checkstyle, SpotBugs), que repetem o
SonarQube e não sabem o que é um caso do negócio. *Validação:* regras escritas
olhando 5 pacotes de treino (o Haiku do V4), congeladas e testadas em pacotes do EXT
lidos antes: P4 36 de 36 (18 pacotes) e P1 a P3 36 de 36 (6 pacotes). O P5 errou 10
de 18 na primeira versão (`SUDESTE(new BigDecimal(...))`); foi corrigido, e ainda
precisa de pacotes novos, inclusive um com exagero, que o EXT não tem. *Limite:* uma
forma de escrever que ninguém previu derruba a regra em silêncio; por isso a
conferência humana. *Onde:* `evaluation/tools/semgrep/`.

**09/10: a calibração da régua versão 4, em 2 pacotes, encerrada.** O Lucas leu, com
a régua enxuta e o guia, um pacote do `TESTE-P4` e um do EXT (cópias cegas sem
comentários, com códigos novos, em `evaluation/calibration-v4/`). O resultado do Semgrep
foi calculado antes e só aberto depois: **8 de 8** (as 4 perguntas nos 2 pacotes, um
`espalhado` com `if` sobre texto e um `isolado` com classes e catálogo). Os 6 achados
foram de clareza do texto, e entraram na régua e no guia sem mudar regra nenhuma (o
caso sem unidade própria, a evidência de cada valor, o roteiro com os três testes por
linha). *Encerrada por decisão do Lucas:* o Claude ajudou a entender o que responder, e
o Lucas treina sozinho com pacotes do V3 antes da leitura do V4. *Onde:*
`evaluation/calibration-v4/notas-lucas.md`.

**09/10: o Lucas confere o Semgrep lendo 20 pacotes às cegas, um por modelo × nível,
com uma regra de saída pré-registrada.** *Proposta.* Ele responde às mesmas perguntas
sem ver o Semgrep; as duas leituras são commitadas; um script compara. Se a
concordância numa pergunta ficar abaixo de 18 de 20, aquela pergunta vira descritiva
(só a amostra). Nas discordâncias, a regra não é corrigida para refazer a conta
oficial. *Por quê:* o ensaio mostrou que uma regra pode errar em silêncio (o P5 da
primeira versão), e a regra de saída barrou isso no ensaio (2 de 6). *Pendente:* se a
conferência cobre só o P4 e o P5 (cerca de 4 a 6 h) ou o P1 a P5 (7 a 10 h).

**09/10: um avaliador só, com releitura.** *Decisão do Lucas.* Sem segundo leitor
humano; no lugar da concordância entre leitores, ele relê 4 ou 5 dos 20 pacotes uma
ou duas semanas depois, sem ver as respostas antigas. *Contraria* o documento do
professor de 12/09 ("dois avaliadores"), e por isso vai à reunião.

**09/10: a cópia que o Lucas lê vai sem nenhum comentário e sem README.** *Decisão do
Lucas.* *Substitui* a regra do `anonymize.mjs` ("comentário citando o harness é
resultado do modelo e não se remove"), que valia quando a régua lia tudo. Agora a
leitura humana só olha a estrutura, e as ferramentas rodam no código original.
Remover todos os comentários é mais seguro que caçar os que citam o harness. A coluna
`condition_leaks` continua (quantos pacotes tinham pista), e a planilha ganha a
coluna "achei que sabia o nível?", para o que sobra, como nomes de classe.

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

**09/10: o SonarQube fica, e nenhum linter a mais.** Um linter pronto daria os mesmos
números com outro nome; o SonarQube entrega a complexidade cognitiva pronta, a medida
que mais discriminou no ensaio (caiu com harness em 8 de 9 pares do EXT).
*Descartado:* trocar pelo PMD, que é mais leve, mas não dá o número por projeto e
nunca rodou aqui.

**09/10: a complexidade cognitiva é candidata a hipótese principal de qualidade,
escolhida a partir do ensaio.** *Proposta.* O texto declara que a escolha veio do EXT
(V3); o V4 são dados novos, e, se foi sorte no ensaio, o V4 mostra. *Atenção:* no
quarteto do Haiku do V4 (uma réplica), a complexidade subiu com o harness.

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

**09/10: o teto e o custo do N3 são resultados, não defeitos.** *Decisão do Lucas.*
Mostrar que o Opus e o Sonnet não precisam de harness para certas coisas responde a
*Modelo: no teto, não piora*; e um N3 que gasta muito mais sem melhorar é o exagero
que o TCC quer mostrar. O custo sai ao lado de cada resultado, porque um N3 que
melhora também pode estar só gastando mais.

**09/10: uma nota de 0 a 100 por execução, só como resumo.** *Proposta.* O Lucas pediu
uma nota como a de uma prova. Ela entra com os pesos escritos antes de rodar e sempre
ao lado dos componentes, e as hipóteses continuam sobre as medidas separadas. *Por
quê:* os pesos são uma escolha, e uma nota única esconderia o caso em que o harness
melhora uma camada e piora outra (o Haiku do EXT: código mais simples, contas mais
erradas). O benchmark do Akita usa nota de 0 a 100 e precisou de um catálogo de
descontos e de um verificador, porque as notas escorregavam de um modelo para outro.

**09/10: os pesos da nota, o valor da recusa parcial e o alcance da conferência.**
*Decisões do Lucas.* (1) **Nota, pesos A:** contas 30 (12 casos, proporcional),
recusas 20 (9 casos; o caso com o código certo e status de sucesso vale **0,5**),
padrão 40 (P1 a P4, 10 por ponto: 10 com a localização e a seleção certas, 5 com
uma, 0 com nenhuma) e P5 sem exagero 10. **Trava:** não compilou ou não subiu, nota
0, como no documento do professor ("sem corretude, as demais notas não valem"). As
variantes B (40+20+40) e C (35+25+40) são publicadas ao lado. *Por quê:* metade
correção, metade padrão, porque o padrão é o tema e um desenho que calcula errado não
serve; errar o valor cobrado pesa mais que errar o erro mostrado. No quarteto do Haiku
(`TESTE-NIVEIS-01`), as três variantes deram a mesma ordem (N3 87,5; N0 51,1; N2
45,6; N1 43,1 na A): os pesos mudam o número, não a conclusão. A qualidade do Sonar
fica fora da nota, porque não tem um "100" natural. (2) **A conferência do Lucas
cobre o P4 e o P5.** O Semgrep mede do P1 ao P5; o P1 a P3 ficam sem conferência
humana e entram como **secundários**, e a hipótese principal do padrão fica no P4 (a
do exagero, no P5). *Descartado:* conferir do P1 ao P5 (7 a 10 h em vez de 4 a 6 h).
*Correção:* a suíte tem 12 casos de conta e 9 de recusa (as entradas anteriores
diziam 11 e 10).

**09/10: as regras de leitura recalculadas para 25 pares** (5 modelos × 5 réplicas).
Com a mesma lógica da moeda das regras para 15 pares: o "altera" contínuo passa a
**18 de 25** (4,3% de chance por sorte; 16 ou 17, inconclusiva; 15 ou menos do lado
maior, contrariada); o "altera" sim/não continua sobre os pares não empatados, com o
mesmo teto de 6,3%, agora numa tabela (5 a 0, 8 a 1, 10 a 2, 11 a 3...); o "não piora"
passa a saldo **até 3** (a mesma proporção, cerca de 13% dos pares, que o 2 tinha em
15). *Por quê:* os limites de 07/10 foram escritos para 15 pares; mantê-los com 25
deixaria as regras mais frouxas do que o pretendido. *Onde:* `OBJETIVO.md` §4.1.

**09/10: o `OBJETIVO` reescrito para o V4 redesenhado.** As hipóteses principais
passam a ser: o desenho no **P4** (Semgrep, conferido), o exagero no P5 (Semgrep,
conferido), a correção (os pontos da suíte, com o 0,5), a qualidade (complexidade
cognitiva, **ainda proposta**) e o modelo. Saem do V4, por falta de instrumento:
*Desenho: comporta o caso exigente* e *Desenho: caso novo com pouca edição*. Viram
medidas por métrica: *não repete o comum* (duplicação), *mais arquivos* (classes) e
*estrutura especulativa* (CBO e LCOM). Entram a conferência, a releitura e a nota (§4.10)
e as limitações novas (§6). *Revisado pelo Lucas no mesmo dia:* de acordo com o texto, e
**a qualidade (a complexidade cognitiva) fica como hipótese principal**. Pendente
antes de congelar: só a lista dos 5 modelos, que sai do mapa.

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

**07/10: os scripts novos têm nome e variáveis em inglês** (`acceptance.sh`,
`run-levels.sh`, este renomeado de `rodada-niveis.sh` antes de ser congelado).
*Por quê:* preferência do autor. Os scripts já congelados (`run-one.sh`,
`rodada.sh`, `extract-meta.mjs`, `aggregate.mjs`, `anonymize.mjs` e os das
métricas) ficaram com o nome que têm, porque o README e os registros dos lotes
os citam assim.

**07/10: todos os scripts do V4 vão para o inglês, inclusive os congelados**
(`run-one.sh`, `extract-meta.mjs`, `aggregate.mjs`, `metrics.sh`,
`aggregate-metrics.mjs`, `anonymize.mjs`, `validate-mutants.mjs`, `mutants.mjs`,
`check-prompt.mjs`; e, a construir, `blind-read.sh` e `verify.mjs`). *Por quê:*
preferência do autor, e o V4 é o momento, antes de qualquer lote que vale. Mover
um arquivo congelado é permitido; os bytes mudaram só nos nomes que os scripts
citam, e cada um, desfeita a troca, é byte a byte igual ao anterior. Fica em
português o `rodada.sh`, que não vai para o V4. *Substitui* a parte da entrada
anterior que mantinha os congelados com o nome antigo. *Onde:* README, tabela de
scripts e a nota de 07/10.

**06/10: depois de cada mudança na bancada, um teste de bancada barato** (Haiku,
esforço baixo, um enunciado que só pergunta o que o agente recebeu). *Por quê:*
conferir que nada quebrou sem gastar uma execução de verdade.

**09/10: o `verify.mjs` construído, com o desenho do lote num arquivo.** O desenho do
V4 (prefixo, réplicas, níveis com o hash de cada harness, imagem, versão do Claude
Code, effort, suíte, gabarito) fica em `experiment/desenho-v4.json`, que congela com
o `OBJETIVO`; os modelos entram depois do mapa, e até lá o script recusa rodar. As 4
checagens de 07/10 (o desenho completo, o gabarito do enunciado das execuções, o CSV
igual aos `meta.json`, a leitura com o mapa) ganharam o que a bancada mostrou que
falta conferir: o modelo que **respondeu** (e não só o pedido), a execução cortada
pela cota, o quarteto que não começou junto (um nível refeito sozinho), a suíte com
que cada execução foi medida e os valores da planilha do Lucas fora da lista da
régua. *A prova de que acusa:* `infra/scripts/verify-teste.mjs` monta um lote
sintético e corrompe uma cópia por caso: o lote limpo e 3 variações legítimas saem
com 0, e os 33 defeitos saem com 1 na checagem certa (37 de 37). Rodado sobre o
`TESTE-NIVEIS-01` real, acusou o que já se sabia (o Haiku medido com a suíte antiga,
o N3 do Opus cortado pela cota, o lote fora do gabarito, o CSV sem as linhas) e
nada mais. *Limite:* garante coerência entre as fontes, não que a classificação
esteja certa. *Onde:* `PLANO-IMPLEMENTACAO.md`, Parte 3.

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
