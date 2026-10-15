# Decisões abertas

O que falta fechar. Este documento **encolhe até zero** — decisão fechada sai
daqui e entra no `plano.md` escrita no presente, e o motivo dela vai para o
`diario-de-bordo.md`.

Cada item trava alguma coisa concreta, e está dito o quê.

---

# A · Trava a primeira execução do lote

## 1. O enunciado: byte a byte, ou versão nova?

Foram identificados quatro pontos de redação melhoráveis no enunciado.

| | opção | consequência |
|---|---|---|
| **(a)** | **Byte a byte** (sha256 `53db3424…`), com os quatro tratados como nota e declarados "identificados, avaliados e mantidos" | Preserva a calibração: 25 execuções já rodaram neste texto |
| (b) | Versão nova, com **todas** as quatro correções de uma vez, hash novo no pré-registro | A calibração vira referência aproximada |

**Recomendo (a).** Um dos quatro já foi testado contra o dado: o segundo exemplo
conferido desambigua a fórmula de juros, e um modelo errou **mesmo com o valor
certo escrito na tela** — isso é falha de leitura, não de redação.

**Não existe meio-termo.** Corrigir um ponto e não os outros, ou corrigir no meio
do lote, é o pior dos mundos.

## 2. Ferramentas e rede

**Este é o confundidor mais concreto do experimento, e está medido:** o Haiku
recebe `TaskCreate`, `TaskGet`, `TaskList` e `TaskUpdate` em 23 de 25 execuções;
o Opus e o Sonnet, em 0 de 12 cada. Como **o modelo é o fator de bloco**, uma
diferença de ferramental entre modelos entra direto na H3.

**Ferramentas.** Lista negra não resolve — não dá para bloquear o que não se
sabe que existe. A lista branca resolve por construção.

- Lista de partida: `--tools "Bash,Read,Write,Edit"`. Já foi provada suficiente:
  uma execução com esse conjunto construiu a API com `mvn verify` passando.
- **A flag `--tools` nunca rodou nesta bancada.** Antes do lote, uma fumaça de 6
  execuções conferindo **dois** campos: `tools_available` idêntico nos três
  modelos, e `permission_denials` vazio nas seis.

**Rede.** Aberta nos dois braços é o que já rodou, e é o que recomendo. Seja qual
for, tem que ser **idêntica nos dois braços e nos três modelos**.

## 3. O pré-registro vira executável?

Hoje o executor **calcula** os hashes do enunciado e do harness e os grava no
`meta.json` — mas não compara com nada. Se alguém editar o enunciado por engano
depois do pré-registro fechar, nada falha e o lote sai contaminado em silêncio.

**Recomendo:** um `pre-registro.lock` versionado com os hashes do enunciado, do
harness e do digest da imagem, **conferido no preflight, que aborta na
divergência**. Não avisa: aborta.

Nota: o `cp -r harness/. workspace/` é indiscriminado. Um `.bak` ou um README
esquecido na pasta entra no workspace do agente e queima o lote. O lock precisa
cobrir **o conteúdo exato da pasta**, não só o `CLAUDE.md`.

## 4. Teto de parede

Não há limite automático de tempo nem de turnos. Nenhuma das 49 execuções de
calibração foi interrompida, e a mais longa levou 585 s.

**Recomendo:** timeout de parede de cerca de 30 minutos — ~3× a mais longa já
observada — como rede de segurança contra execução travada, com a interrupção
registrada como dado. Não é corte cognitivo.

`--max-turns` fica descartado: aquilo corta o raciocínio e muda o que se mede.

## 5. O formato do `run_id` do lote

O prefixo `BATCH-` está decidido; a numeração não. São 18 execuções, e o
identificador precisa carregar modelo, condição e réplica sem ambiguidade.

---

# B · Trava a análise

## 6. A regra de leitura de cada hipótese

**Nenhum critério numérico está escrito.** O Mann-Whitney foi descartado com
razão — com n=3 por braço o menor p bicaudal possível é 0,10 — e **nada foi posto
no lugar**.

Sem isso, você chega no fim com números e sem regra para dizer se a H1 foi
apoiada. E o risco não é teórico: é olhar o número e escolher a regra depois.

**Recomendo** regras determinísticas e descritivas, escritas antes do lote, no
formato "H_n apoiada se ___".

## 7. A tabela principal é pareada, não mediana de célula

Isto não é preferência de apresentação. Está medido, e a mediana **produz
conclusão errada**.

Nas 12 execuções pareadas do enunciado atual, lendo o consumo de entrada:

| leitura | opus | sonnet | haiku |
|---|---|---|---|
| **mediana de célula** | +8% | −28% | +66% |
| **pares simultâneos** | 3 de 4 positivos, de −14% a +99% | **4 de 4 negativos**, de −15% a −28% | 3 de 4 positivos, de −46% a +79% |

A mediana erra nos dois sentidos: o `+66%` do Haiku parece efeito forte e é ruído
numa faixa de 125 pontos; o `−28%` do Sonnet **subestima o achado mais sólido do
dado inteiro** — 4 de 4 na mesma direção, numa faixa de 13 pontos.

O par simultâneo é a única estrutura do desenho que cancela horário e carga de
servidor, e você **já paga** por ele ao rodar seis containers ao mesmo tempo.
Reportar mediana de célula joga fora o que foi comprado.

**Vale para a H2 e para qualquer desfecho que venha da avaliação.**

---

# C · Da avaliação, e por isso adiadas

Estas ficam para o `avaliacao.md`, escrito depois que os pacotes existirem — ver
`plano.md` §14. Estão listadas aqui só para não se perderem.

- **A unidade da contagem, se a avaliação for por contagem:** arquivo ou tipo de
  topo declarado? Java permite vários tipos por arquivo, e no dado de calibração
  há execução com 12 arquivos e 23 tipos.
- **"Registro declarativo" conta como alteração?** Quando a variante nova entra
  só acrescentando uma linha a uma lista, isso é mexer num arquivo ou não? Há
  desenho em que o framework descobre sozinho e ninguém escreve nada.
- **Arquivo de teste escrito pelo modelo conta?** O enunciado pede `mvn verify`,
  o que convida a escrever teste.
- **Regra de dado faltante:** o que fazer com pacote que não compila, e com par
  em que um braço morre por falha de infraestrutura. Recomendo, quando chegar a
  hora: refazer o **par inteiro**, porque o pareamento é o ativo mais valioso do
  desenho.
- **O elo `runs/` → avaliação nunca rodou de ponta a ponta.** As execuções de
  calibração provaram a metade que produz dado; a metade que o avalia nunca foi
  exercitada. Vale exercitar num pacote de calibração antes do lote.

---

# Fechadas, para não voltarem à mesa

- **A H2 fica.** Consumo de tokens e tempo continuam sendo medidos, e por isso o
  `extrair-meta.mjs` e o `agregar.mjs` continuam existindo.
- **A pergunta de pesquisa fica como está** — "reconhecimento e implementação".
  A avaliação será desenhada para responder a ela, e não o contrário.
- **Dois braços, não três.** Sem braço placebo. A limitação está declarada no
  `plano.md` §4.
- **`n` = 3 por célula**, fechado antes do lote e sem acréscimo depois de olhar
  o dado.
- **A bancada são cinco scripts**, e nenhum deles olha o código para dizer se
  está certo. A suíte escondida de 60 casos e as ferramentas que a rodavam saíram
  do desenho: o que fica como controle de funcionamento é o `mvn verify` que o
  `executar.sh` já roda.
- **Sem segundo avaliador.** Quem avalia é o autor, e a conferência externa é do
  orientador, por amostra.
