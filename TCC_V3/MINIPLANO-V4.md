# Miniplano do V4: o que falta antes de rodar

> Escrito em 09/10/2026, depois do redesenho da avaliação (08 e 09/10). Substitui o
> [`MINIPLANO.md`](MINIPLANO.md) a partir daqui: aquele cobria a régua grande e o V4
> de 3 modelos. O porquê de cada escolha está no [`DECISOES.md`](DECISOES.md) (as
> entradas de 08 e 09/10). Vamos ajustando item por item; cada item fechado ganha a
> data e, se mudar alguma decisão, uma entrada nova no `DECISOES.md`.
>
> O professor só quer reunião com um resultado pronto e analisado; por isso as
> decisões são do Lucas, e a reunião é o último passo.
>
> **Legenda:** 🔴 sem isto não roda · 🟡 decisão pendente · 💰 gasta cota ·
> 👤 é do Lucas · 🤖 o Claude faz quando o Lucas pedir

## O V4 em uma página

| | |
|---|---|
| pergunta | o quanto de harness (N0 a N3) muda a forma como agentes aplicam o Strategy, e isso depende do modelo? |
| tarefa | o checkout em Java/Spring, enunciado do V4 (`experiment/prompt/prompt.md`, `8c70bb30…`) |
| níveis | N0 nada · N1 CLAUDE.md · N2 + skill de padrões · N3 + processo com revisor |
| modelos | 5, escolhidos pelo mapa (proposta: Haiku 4.5, Haiku 5.5, Sonnet 4.6, Sonnet 5, Opus 5) |
| execuções | 5 modelos × 4 níveis × 5 réplicas = **100**, em quartetos, um por vez, ordem sorteada |
| avaliação | **correção** (suíte, 100) · **qualidade** (SonarQube e CK, 100) · **padrão** (Semgrep, 100) · **conferência** do Semgrep pela leitura do Lucas (20 às cegas) · releitura (4 ou 5) |
| saída | se o Semgrep concordar com o Lucas em menos de 18 de 20 numa pergunta, ela vira descritiva |

---

## Passo 1. As decisões do Lucas 👤

O professor só quer reunião com um resultado pronto e analisado (09/10). Então as
decisões são do Lucas, registradas com o motivo no `DECISOES.md`; ele as vê no passo
7, junto com o resultado. O que vale como pré-registro é o commit do `OBJETIVO`
congelado antes de rodar.

v - decidido que sim · x - decidido que não

- [v] 🔴 **A reestruturação da avaliação**: automático na base, leitura humana em
      amostra para conferir, o Claude fora da avaliação.
- [v] 🔴 **Semgrep no lugar do PR-Agent** (a sugestão do professor): sem IA, mesmo
      resultado sempre, validado pela leitura. O ensaio deu P1–P4: 72 de 72.
- [v] 🔴 **5 modelos × 5 réplicas**, escolhidos por um mapa no N0, sem o Fable.
- [v] 🔴 **Avaliador único com releitura.** O documento do professor de 12/09 pede dois
      avaliadores e a concordância entre eles. Aqui, só o Lucas lê os 20 pacotes; no
      lugar da concordância entre dois, ele relê 4 ou 5 desses pacotes uma ou duas
      semanas depois, sem ver as respostas antigas, e mede-se se ele responde igual
      (a leitura é estável?). Mais fraco que dois leitores, e custa uns 40 minutos.
      Se o professor reclamar, entra o segundo.
- [x] **A manutenção** fica fora do V4 e vira trabalho futuro; a tabela de extensão sai.
- [x] **O PR-Agent** fica fora, nem como ilustração.
- [v] **O teto e o custo do N3 são resultados**, não defeitos.
- [v] **O peso da recusa com status de sucesso.** Cada caso de recusa vale 1 com o
      código e o status certos, **0,5** (proposta) com o código certo e status de
      sucesso, e 0 com o código errado ou sem recusa. A suíte mostra as contas (12) e
      as recusas (9) separadas. No quarteto do Haiku: o N1 vai de 11 para 13,5 de 21,
      e o N2 de 12 para 14,5; o N0 (17) e o N3 (20) não mudam. *Confirmado: 0,5.*
- [v] **A nota de 0 a 100**, como resumo, ao lado dos componentes. Pesos **A**: contas 30
      (12 casos), recusas 20 (9 casos, com o 0,5), padrão 40 (P1 a P4, 10 por ponto:
      10 com as duas respostas certas, 5 com uma, 0 com nenhuma) e P5 sem exagero 10.
      **Trava:** não compilou ou não subiu, nota 0. As variantes B (40+20+40) e C
      (35+25+40) saem ao lado, para mostrar que a ordem não depende do peso (no
      quarteto do Haiku, a ordem foi a mesma nas três). A qualidade do Sonar fica fora
      da nota, em tabela própria.

## Passo 2. Um quarteto de Sonnet ou Opus no enunciado do V4 💰

- [x] 🔴 **Verificar a calculadora do seguro.** Nenhuma implementação real do V4 fez 21
      de 21 ainda (o Haiku: 20, 17, 11, 12, 20). Uma implementação independente que
      chegue a 21 de 21 confirma a calculadora; se um caso falhar em todos os modelos
      fortes, a suspeita é da calculadora, e o caso é revisto **antes** do V4.
      *Feito em 09/10: o quarteto do Opus 5 (`TESTE-NIVEIS-01-OPUS`), 4 implementações,
      21 de 21 cada (`DECISOES.md`, §5).*
- [x] 🔴 **Medir a cota** de um quarteto de modelo forte (o f4 só mediu o Haiku: 10 a
      15% da janela de 5 h). Anotar o percentual antes e depois. 👤 *Feito em 09/10: o
      quarteto do Opus 5 gasta cerca de 80%; começou em 20% e o N3 bateu no limite.*
- [x] Com esse número: o **plano de cota** das 100 execuções (quantas janelas, quantos
      dias). 🤖 *Feito em 09/10 (`DECISOES.md`, §2): um quarteto do Opus por janela
      zerada (5 janelas), os do Haiku 4.5 cabem em 1; os outros 3 modelos se estimam
      pelo custo do N0 no mapa. Medido no plano anterior: com o Claude Max (09/10), o
      mapa mede de novo o gasto por janela, e o plano é refeito com esse número.
      Refeito com o mapa e com o N0 do exploratório: ~$0,78 por 1% da janela; uma
      rodada ~55%; o confirmatório ~2,8 janelas, o exploratório ~2,7, o total ~5,5; um
      quarteto só começa com pelo menos 35% livre.*

## Passo 3. O mapa e o fechamento do Semgrep 💰

- [x] 🔴 **Registrar a regra de escolha** no `DECISOES.md` **antes** de rodar: os 5 devem
      compilar; ter modelos no teto e pelo menos 2 fora dele; olhando a suíte e o P4
      pelo Semgrep no N0. 🤖 *Feito em 09/10* (`DECISOES.md`, §2): o Haiku 4.5 e o Opus 5
      entram sempre; dos outros, o de menor nota A, o do meio e o de maior, com pelo menos
      2 fora do teto no grupo final.
- [x] 🔴 **Rodar o mapa**: cada candidato 1 vez no N0. O Haiku 4.5 e o Opus 5 já rodaram;
      faltam Haiku 5.5, Sonnet 4.5, 4.6 e 5, e Opus 4.6 (5 execuções). 💰 *Feito em 09/10
      (`TESTE-MAPA-01`): as 5 juntas, todas compilaram e subiram; 8% da janela do Max.*
- [x] 🔴 **Escolher os 5** pela regra, e pôr os 5 no `run-levels.sh`, com a ordem sorteada
      das rodadas. 🤖 *Feito em 09/10: Haiku 4.5, Sonnet 4.5, Opus 4.6, Sonnet 5 e Opus 5
      (`DECISOES.md`, §2). Os 5 estão em `experiment/desenho-v4.json`, que o
      `run-levels.sh` passou a ler; a ordem, em `experiment/ordem-v4.csv` (semente
      20261009).*
- [x] 🔴 **Testar o P5 do Semgrep** em pacotes novos (os do mapa e um quarteto do Haiku
      5.5), lidos antes de rodar. 🤖 *Feito em 09/10 com os 4 Opus do passo 2, que as regras
      nunca tinham visto: 40 de 40, o P5 8 de 8.*
- [x] 🔴 **Testar um exagero de mentira**: um código com uma classe por região, para ver o
      Semgrep marcar `estrutura` (o EXT não tem nenhum). 🤖 *Feito em 09/10: 4 jeitos de
      exagerar (classe por região, enum com corpo, mapa de objetos, switch que devolve
      objeto), os 4 deram `estrutura`.*
- [x] 🔴 **O alarme de nomes desconhecidos**: pacote que não cita nenhum nome de um ponto
      vira `indeterminado`, e não `isolado`. 🤖 *Feito em 09/10 (testado com o clube em
      inglês: `GOLD`, `SILVER`).*
- [x] Congelar o Semgrep (hash das regras, do classificador e da imagem no README). 🤖
      *Feito em 09/10* (`evaluation/tools/semgrep/README.md`).

## Passo 4. Implementar o que foi decidido 🤖

- [x] 🔴 **Suíte:** relatar as contas (12) e as recusas (9) separadas; a recusa com o
      código certo e status de sucesso vale 0,5 (ou o valor que o Lucas escolher);
      congelar com hash. *Feito em 09/10* (`CONTAS`, `RECUSAS` e `PONTOS` depois do
      `RESULTADO`, que segue tudo ou nada; o `acceptance.sh` grava os três no resultado e no
      CSV; referência 21 de 21 e mutantes 17 de 17). Falta só congelar, depois do passo 2.
- [x] 🔴 **Cópia cega:** o `anonymize.mjs` passa a tirar **todos** os comentários e o README
      da cópia que o Lucas lê (o Semgrep, o Sonar e a suíte continuam no original).
      *Feito em 09/10:* opção `--sem-comentarios`; sem ela, o pacote sai idêntico ao de
      antes. Testado num pacote do Opus: 65 linhas de comentário saíram, o código ainda
      compila, as linhas ficam no lugar e o Semgrep dá o mesmo resultado nos dois.
- [x] 🔴 **Sorteio da amostra:** 1 pacote por modelo × nível (20) e os 4 ou 5 da releitura,
      com semente registrada. *Feito em 09/10* (`evaluation/tools/sample.mjs`; a releitura
      é sorteada num comando separado, depois, para o Lucas não saber de antemão).
- [x] 🔴 **Comparação:** script que cruza a planilha do Lucas com o CSV do Semgrep e aplica
      a regra de saída. *Feito em 09/10* (`evaluation/tools/compare.mjs`; serve também
      para a releitura; testado com 3 respostas trocadas de propósito).
- [x] 🔴 **Planilha do Lucas:** as perguntas, `arquivo:linha`, dúvida e a coluna "achei que
      sabia o nível?". *Feito em 09/10:* o `sample.mjs` gera a planilha em branco.
- [x] **Nota de 0 a 100:** script com os pesos que forem pré-registrados. *Feito em
      09/10* (`evaluation/tools/nota.mjs`; pesos A, com B e C ao lado, e a trava).
- [x] `verify.mjs` enxuto: confere que todas as execuções têm a mesma imagem, a mesma
      versão do Claude Code e os modelos certos. *Feito em 09/10*
      (`infra/scripts/verify.mjs`, com o desenho em `experiment/desenho-v4.json`): as 4
      checagens do plano, mais o modelo que respondeu, o corte pela cota, o quarteto
      que não começou junto, a suíte de cada medição e os valores da planilha. A prova:
      `verify-teste.mjs`, 37 de 37 casos (33 defeitos acusados na checagem certa).

## Passo 5. Pré-registro e calibração

- [v] **A conferência cobre o P4 e o P5** (≈ 4 a 6 h). O Semgrep mede do P1 ao P5, mas o
      P1 a P3 ficam sem conferência humana e entram como secundários; a hipótese
      principal do padrão fica no P4, e a do exagero no P5.
- [x] 🔴 **Régua enxuta** (versão 4 da `evaluation/regua.md`), com as definições que saíram
      da calibração do L3RG ("nomear", "valor", o `if` de validação na tabela, o ruído
      da busca), e o `GUIA-DA-REGUA.md` cortado para ela. 🤖, revisado pelo 👤
      *Escrita em 09/10, com o gabarito do V4 (só os casos de cada ponto, o seguro no
      P5). A régua, o gabarito e o guia foram revisados pelo Lucas em 09/10 (de
      acordo).*
- [x] 🔴 **Reescrever o `OBJETIVO.md`**: hipóteses principais (padrão pelo Semgrep, correção,
      qualidade, modelo), os limites do §4.1 recalculados para 25 pares, a regra de
      saída, os pesos da nota, de onde veio a complexidade cognitiva (o ensaio). 🤖,
      revisado pelo 👤 *Escrito e revisado pelo Lucas em 09/10 (de acordo; a qualidade
      fica como principal). Falta só a lista dos 5 modelos, que sai do mapa.*
- [x] 🔴 **Calibração do Lucas**: 1 ou 2 pacotes de treino com a régua enxuta, cronometrando
      o primeiro. 👤 *Feito em 09/10: 2 pacotes, 8 de 8 com o Semgrep; 6 ajustes de texto
      na régua e no guia. O Lucas treina sozinho com pacotes do V3 antes do V4.*
- [ ] 🔴 **Congelar o `OBJETIVO`** (commit e hash) **antes** da primeira execução do V4. 👤

## Passo 6. Montar e rodar o V4

- [ ] Criar a pasta `TCC_V4` (a árvore combinada em 09/10) e conferir que tudo marcado como
      igual ao V3 tem o mesmo hash. 🤖
- [ ] Desligar o SonarQube durante as execuções (`docker stop tcc-sonarqube`). 👤
- [ ] Rodar os 25 quartetos, na ordem sorteada; quarteto interrompido é refeito inteiro. 💰
- [ ] Rodar a suíte, as métricas e o Semgrep nos 100. 🤖
- [ ] Anonimizar, sortear os 20, gerar as cópias cegas. 🤖
- [ ] **A leitura dos 20, às cegas**, e o commit da planilha. 👤
- [ ] Commit do CSV do Semgrep; comparação; regra de saída. 🤖
- [ ] Abrir o mapa; tabelas por nível e modelo. 🤖
- [ ] Releitura de 4 ou 5 pacotes, uma ou duas semanas depois. 👤
- [ ] 🔴 **O exploratório**, obrigatório (decisão de 09/10): os 25 quartetos do `V4-EXPLOR` (Haiku 5.5,
      Sonnet 4.6, Opus 4.7, Opus 4.8, Sonnet 5.5), na ordem de
      `experiment/ordem-v4-exploratorio.csv`, **depois** do confirmatório; a suíte, as
      métricas e o Semgrep neles; tabelas marcadas como exploratórias. Sem leitura
      humana. 💰

## Passo 7. Reunião com o professor, com o resultado analisado 👤

- [ ] As tabelas por nível e modelo: correção (contas e recusas), qualidade, padrão
      (Semgrep) e a concordância da leitura com o Semgrep.
- [ ] As escolhas que fogem do documento dele de 12/09, com o motivo de cada uma (as
      entradas de 08 e 09/10 do `DECISOES.md`): um avaliador só, com releitura; o
      Semgrep no lugar do PR-Agent; a manutenção fora.
- [ ] As limitações (abaixo).

---

## Limitações que ficam (vão para o texto, não se corrigem)

- Uma regra do Semgrep pode falhar em silêncio diante de uma forma de escrever não
  prevista; a conferência e o alarme reduzem, não eliminam.
- O teto: o Opus e o Sonnet devem acertar quase tudo.
- O N3 gasta bem mais; se melhorar, pode ser o processo ou só mais computação.
- Os instrumentos foram desenhados com ajuda de IA; a conferência humana é o que os
  valida.
- O ensaio influenciou escolhas (a complexidade cognitiva), e isso é declarado.

## Já pronto

- O enunciado do V4 (73 números conferidos; as leituras revisadas pelo Lucas em 07/10).
- Os níveis N0 a N3, a imagem e o `run-levels.sh`, testados no f4 (quarteto do Haiku,
  09/10).
- A suíte de 21 casos, com 17 mutantes reprovados (falta o passo 2).
- As métricas (SonarQube e CK), testadas no EXT e no f4.
- O Semgrep do P1 ao P4, validado no ensaio (`evaluation/tools/semgrep/`).
- O fluxo de validação, emulado ponta a ponta no EXT.

## Onde parou

| data | o quê |
|---|---|
| 09/10 | miniplano escrito; o passo 1 virou decisões do Lucas (o professor só vê o resultado analisado) |
| 09/10 | passo 1 fechado (peso 0,5, nota A com trava e meio-ponto, conferência no P4 e P5); começando o passo 4, que não gasta cota; os passos 2 e 3 esperam a cota |
| 09/10 | passo 4 quase todo feito (suíte, cópia cega, sorteio, comparação, planilha, nota); falta o `verify.mjs` |
| 09/10 | passo 3 sem cota fechado: o alarme, o exagero de mentira, o teste nos 4 Opus (40 de 40), a regra do mapa registrada e o Semgrep congelado; o mapa fica para o fim, com o passo 2 |
| 09/10 | passo 5: a régua enxuta, o gabarito do V4, o guia e o `OBJETIVO` escritos (rascunhos para o Lucas revisar); o README do projeto atualizado |
| 09/10 | o Lucas revisou o `OBJETIVO` e a régua e ficou de acordo; a qualidade fica como hipótese principal |
| 09/10 | gabarito e guia revisados; calibração em 2 pacotes (8 de 8 com o Semgrep), 6 ajustes de texto aplicados; a régua fica pronta para congelar |
| 09/10 | passo 2 registrado (a calculadora confirmada pelo Opus, a cota e o plano de cota); o `verify.mjs` pronto e provado (37 de 37); falta só o que gasta cota: o mapa, a escolha dos 5, congelar, criar o `TCC_V4` e rodar |
| 09/10 | o Lucas revisou o enunciado e manteve como está (o contrato fixo, o P3 sem frase de crescimento); o mapa rodou e a regra escolheu Haiku 4.5, Sonnet 4.5, Opus 4.6, Sonnet 5 e Opus 5; ordem sorteada; plano de cota refeito no Max. Falta: congelar, criar o `TCC_V4` e rodar |
| 09/10 | o Lucas pediu o panorama de todos os modelos: virou o lote exploratório `V4-EXPLOR` (5 modelos, 100 execuções, sem hipótese), à parte do confirmatório; o Opus 5.5 ficou fora (exige um Claude Code mais novo); ordem sorteada |
| 09/10 | o N0 do Opus 4.7, do Opus 4.8 e do Sonnet 5.5: o Opus 4.7 com 60 (fora do teto), os outros dois com 100; o exploratório fica obrigatório, com dois modelos fora do teto; cota refeita (~5,5 janelas no total). Falta: congelar, criar o `TCC_V4` e rodar |
