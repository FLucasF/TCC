# Plano do experimento

Este documento descreve **o desenho**: o que o experimento é, como roda e o que
fica registrado. Está escrito no presente. Não há aqui nenhum registro de como o
desenho chegou a ser este — isso mora em `diario-de-bordo.md`, que é
cronológico, e é o único lugar onde a história é bem-vinda.

A regra vale para todo documento deste repositório: **um documento, um trabalho.**

| documento | trabalho |
|---|---|
| `plano.md` | o desenho, no presente |
| `pre-registro.md` | o congelado: hashes, e o que foi declarado antes de olhar o dado |
| `diario-de-bordo.md` | a história, em ordem cronológica |
| `decisoes-abertas.md` | o que falta fechar. Encolhe até zero |
| `avaliacao.md` | **ainda não existe** — ver §14 |

---

# Parte I — A ideia

## 1. A pergunta

> Um harness — um arquivo `CLAUDE.md` com orientação de processo — aumenta o
> **reconhecimento e a implementação** do padrão Strategy, quando um agente de
> código constrói uma API a partir de um enunciado que tem pontos pedindo esse
> padrão?

O que se compara é **a mesma tarefa, o mesmo modelo, o mesmo ambiente**, com e
sem esse arquivo. A diferença entre os dois braços é o arquivo, e nada mais.

**Por que isso importa.** A prática de escrever um `CLAUDE.md` para orientar
agentes de código é difundida e não é medida. Recomenda-se por experiência
pessoal e por analogia com revisão de código humano. Este trabalho mede um
efeito específico, numa tarefa específica, e diz com que confiança.

## 2. O que "harness" quer dizer aqui

Harness é **o arquivo `experimento/harness/CLAUDE.md`, e só ele**. Quatro regras
sobre como raciocinar diante de variação de comportamento, escritas sem nomear
padrão, domínio, classe ou teste.

Definição operacional: no braço HARNESS esse arquivo é copiado para a raiz do
workspace antes de o agente começar; no braço CONTROL o workspace nasce vazio.
Nenhuma outra diferença existe entre os braços.

O que **não** é harness neste trabalho, ainda que a palavra seja usada assim por
aí: hooks, skills, subagentes, MCP, comandos de barra, ou configuração de
ferramentas. Todos esses estão desligados, **igualmente nos dois braços**, e são
variáveis controladas — ver §7.

## 3. As hipóteses

| | hipótese | direção |
|---|---|---|
| **H1** | Com harness, o reconhecimento e a implementação de Strategy são maiores | declarada |
| **H2** | O harness **altera** o consumo de tokens e o tempo | **sem direção** |
| **H3** | O efeito do harness é maior no Haiku 4.5 do que no Opus 5 | declarada |
| **H4** | O acerto cai de P1 para P3, nas duas condições | declarada |
| **H5** | O efeito do harness é maior em P2 e P3 do que em P1 | declarada |

A H2 é deliberadamente não-direcional, e o dado de calibração justifica isso: o
efeito do harness sobre o consumo **muda de sinal conforme o modelo**. Declarar
direção seria escolher o resultado antes de medir.

A H1, a H3, a H4 e a H5 dependem de uma medida de "reconhecimento e
implementação" que é definida em `avaliacao.md` — ver §14.

## 4. O que este experimento não responde

Declarado aqui, no começo, e não escondido perto do fim. Estas limitações valem
**qualquer que seja** a avaliação escolhida depois, porque vêm do desenho e não
do instrumento.

**Não responde se o harness melhora código em geral.** A unidade de
generalização é a **tarefa**, e há **uma** tarefa. As 18 execuções são réplicas
de um único enunciado, não uma amostra de tarefas. Qualquer afirmação da forma
"o harness ajuda a escrever código melhor" está fora do alcance deste desenho.

**Não produz significância estatística.** Com n=3 por braço, nenhum teste
disponível pode produzir p<0,05 — são C(6,3)=20 arranjos possíveis, e o menor p
bicaudal alcançável é 0,10. Os resultados são descritivos.

**Não isola "as quatro regras" de "haver um CLAUDE.md".** O braço HARNESS difere
do CONTROL em três coisas ao mesmo tempo: o conteúdo das regras, a existência de
um arquivo de orientação, e o acréscimo de contexto ao prompt. Separar exigiria
um terceiro braço com um harness neutro.

**A variância entre execuções idênticas é grande, e está medida.** Execuções com
o mesmo modelo, mesma condição, mesmo enunciado e mesmo harness variaram de
**1,5× a 2,4×** no consumo de entrada, e produziram formas de código diferentes
entre si. Isso limita o que qualquer desfecho pode afirmar com n=3.

---

# Parte II — O desenho

## 5. As variáveis

**Independente**

| variável | níveis |
|---|---|
| condição | `CONTROL` (workspace vazio) · `HARNESS` (com o `CLAUDE.md`) |

**De bloco**

| variável | níveis |
|---|---|
| modelo | Opus 5 · Sonnet 5 · Haiku 4.5, sempre por ID completo |

**Dependentes**

| variável | papel | de onde sai |
|---|---|---|
| reconhecimento e implementação de Strategy, em P1, P2 e P3 | **principal** | `avaliacao.md`, §14 — **a definir** |
| `input_total`, `output`, `duration_api_ms`, `turns`, `tool_calls` | secundária (H2) | `meta.json` |
| build passou depois da execução | controle | `meta.json` |
| obediência às versões pedidas | controle | `meta.json` |

**Controladas** — cada uma com o mecanismo que a garante, e não só a intenção:

| o que | valor | mecanismo |
|---|---|---|
| enunciado | idêntico nos dois braços | mesmo arquivo montado somente leitura; sha256 conferido no preflight |
| modelo | ID completo, nunca alias | preflight recusa alias; `model_init` e `models_observed` gravados |
| raciocínio | `medium` | `--effort medium`, gravado no `meta.json` |
| ferramentas | **lista branca idêntica** | `--tools`, e o conjunto recebido é gravado e conferido |
| skills, comandos de barra | desligados | `--disable-slash-commands`, nos dois braços |
| subagente | bloqueado | fora da lista branca |
| memória entre execuções | inexistente | `HOME` vazio, `--no-session-persistence`, container `--rm` |
| Java, Maven, SO | idênticos | imagem fixada por **digest**, conferido no preflight |
| horário e carga de servidor | iguais dentro da rodada | as seis execuções rodam simultâneas |

## 6. As células, e o n

3 modelos × 2 condições × 3 réplicas = **18 execuções**.

**Por que 3 réplicas.** Porque a variância entre execuções idênticas é real e
foi medida: três execuções do mesmo modelo, na mesma condição, com o mesmo
enunciado e o mesmo hash de harness, produziram **três formas de código
diferentes**. Uma réplica não distingue efeito de sorteio.

**Por que não mais que 3.** O limite não é cota nem dinheiro — o lote inteiro
custa cerca de US$ 15 em preço de tabela, e uma rodada de seis consome ~8% da
janela de cinco horas. O limite é o trabalho manual de avaliar 18 pacotes.

**O `n` fecha aqui e não se mexe.** Acrescentar réplicas depois de olhar o dado
transforma o resultado em escolha, e é a forma mais comum de contaminar um
experimento pequeno.

## 7. O que é mantido constante, e como

O princípio: **tudo que não é a condição é igual nos dois braços, e a igualdade é
verificada, não presumida.**

Três mecanismos, em ordem de força:

1. **Impossibilidade.** O container nasce sem `HOME`, sem sessão anterior e sem
   memória. Não há o que igualar porque não há o que persistir.
2. **Verificação no preflight.** Hash do enunciado, hash do harness, digest da
   imagem, ID completo do modelo. Divergiu, a execução não começa.
3. **Registro para conferência depois.** O conjunto de ferramentas recebido, os
   modelos observados nas mensagens, a versão do CLI. Vai para o `meta.json` e é
   conferido antes da análise.

> [!warning] O conjunto de ferramentas é confundidor do fator de bloco
> Medido: o Haiku recebe quatro ferramentas que o Opus e o Sonnet não recebem.
> Como o modelo é o fator de bloco, uma diferença de ferramental entre modelos
> entra direto na H3. Lista negra não resolve: não dá para bloquear o que não se
> sabe que existe. **A lista é branca**, e o preflight confere que os três
> modelos receberam exatamente o mesmo conjunto.

## 8. A tarefa

Um pedido de cliente não-programador, dono de loja de roupas, para uma API que
devolve o resumo de um checkout. O workspace nasce **vazio**: o agente monta o
projeto inteiro.

O enunciado é escrito em voz de leigo e descreve **negócio**, nunca software.
Nenhuma palavra de arquitetura aparece: não há "padrão", "interface",
"polimorfismo", "estratégia", "extensível". O contrato técnico — nomes de campo,
oito códigos de erro com precedência, quatro exemplos numéricos conferidos —
fica isolado num anexo atribuído a um terceiro, "o desenvolvedor do site". Isso
permite ser rigoroso com o contrato sem que o cliente leigo passe a falar como
programador.

**Os três pontos de variação**, plantados em dificuldade crescente:

| | ponto | variantes | por que a dificuldade é essa |
|---|---|---|---|
| **P1** | Entrega | ECONOMICA, EXPRESSA, RETIRADA_LOJA, MOTOBOY | **fácil**: as variantes estão numa tabela, alinhadas na página; a pista de mudança é explícita e futura; cada variante tem três comportamentos visíveis de uma vez — custo, prazo, disponibilidade |
| **P2** | Cupons | BEMVINDO10, MENOS50, FRETEGRATIS, LEVE3PAGUE2 | **médio**: apresentados como exemplos concretos, não como tipos de regra. Cada um lê uma entrada diferente — um olha o subtotal, outro a lista de itens, outro o frete já calculado. Quem fixa a assinatura no primeiro caso não implementa os outros sem remendo |
| **P3** | Pagamento | PIX, CARTAO, BOLETO | **difícil**: as regras estão espalhadas entre uma seção de FAQ de atendimento e outra de observações do financeiro, e há juros com fórmula. A pista de mudança é a mais fraca das três |

A dificuldade não é uma opinião sobre os pontos: é uma propriedade construída do
enunciado — **apresentação**, **força da pista de mudança** e **dispersão da
informação**. A H4 testa se ela se confirma.

> [!danger] O enunciado e o harness são congelados por hash
> Os dois vão para este repositório **byte a byte**, e o preflight aborta se o
> sha256 não bater com o do pré-registro. Mudar qualquer um dos dois cria outro
> experimento.
>
> O hash é de conteúdo byte a byte: um editor que normalize fim de linha muda o
> hash sem mudar uma palavra. O `.gitattributes` deste repositório trata
> `experimento/**` como binário por isso.

---

# Parte III — Como é feito

*(a escrever: o ambiente, uma execução passo a passo, a rodada, e o que fica
registrado campo a campo)*

A bancada são **cinco scripts**. Nenhum deles olha o código para dizer se está
certo.

| script | o que faz |
|---|---|
| `infra/scripts/executar.sh` | roda **uma** execução: preflight, container, `claude -p`, e o build num segundo container sem token |
| `infra/scripts/rodada.sh` | dispara as **seis** de uma rodada ao mesmo tempo |
| `infra/scripts/extrair-meta.mjs` | lê a transcrição e escreve o `meta.json`: os números da H2, mais os campos de controle |
| `infra/scripts/agregar.mjs` | junta os `meta.json` num CSV, uma linha por execução |
| `avaliacao/ferramentas/anonimizar.mjs` | monta os pacotes cegos: tira o `CLAUDE.md`, o `meta.json` e a transcrição, normaliza as datas de modificação, embaralha e atribui código aleatório |

---

# Parte IV — Como é avaliado

## 14. A avaliação ainda não está desenhada

**Isto é deliberado, e a ordem é essa de propósito.**

Este plano descreve o experimento que **produz** os pacotes de código. Como esses
pacotes são avaliados é assunto de um documento próprio, `avaliacao.md`, escrito
depois — quando os pacotes existirem e for possível olhar para eles.

**O que isso não significa.** Não significa que a régua será escolhida depois de
ver o resultado. Significa que ela será escrita depois de ver **que forma o
código tem**, e antes de qualquer pacote do lote ser avaliado.

**Três restrições que a avaliação, seja qual for, tem de respeitar:**

1. **Cega.** Quem avalia recebe `avaliacao/pacotes/<CODIGO>/` — código-fonte,
   nome aleatório, datas normalizadas — e não sabe de que braço veio. O mapa
   código → execução fica fora do repositório até a avaliação fechar.
2. **Congelada antes de comparar.** A planilha é commitada antes de o mapa ser
   reaberto. Sem isso, a comparação vira ajuste.
3. **Com regra de leitura escrita antes.** Para cada hipótese, o critério que
   decide se ela foi apoiada tem de estar no papel **antes** de os números
   existirem. Sem isso, a régua é escolhida depois do resultado.

> [!note] O que já está pronto para quando esse documento for escrito
> Os pacotes cegos (`anonimizar.mjs`), três extensões por ponto com casos
> prontos (`DRONE` em P1, `DEZOFF` em P2, `CARTEIRA_DIGITAL` em P3), e um
> catálogo de **seis formas de código** observadas nas execuções de calibração.

---

# Parte V — Tocar o projeto

*(a escrever)*
