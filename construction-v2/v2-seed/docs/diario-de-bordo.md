# Diário de bordo

Registro cronológico do projeto. **Toda** a história mora aqui — o `plano.md`
descreve o desenho no presente e não conta como chegou nele.

## Formato, durante o lote

Uma entrada por exceção:

```markdown
### 2026-10-05 14:25, BATCH-03-HAIKU-HARNESS
- Situação: cota atingida no turno 14
- Ação: execução descartada; refeita às 19:10, mesma posição
- Impacto: nenhum na configuração
```

> [!warning] Interromper à mão é julgamento seu, e entra no experimento
> Não existe limite automático de tempo nem de turnos. Se um dos braços for
> sistematicamente mais lento e você matar mais execuções dele, estará
> descartando justamente os casos difíceis daquele braço, e a comparação fica
> enviesada a favor dele.
>
> Registre **toda** interrupção com o tempo decorrido e o motivo, e use o mesmo
> critério de paciência nos dois braços do par — que rodam ao mesmo tempo
> justamente para isso ser comparável.

---

# Antes do lote

## 23/09/2026 — a v2 nasce, e o que ela herda

A versão anterior fica congelada como registro e não é mais editada. Esta é um
repositório novo, e a separação existe por um motivo específico.

**O problema da v1.** Os documentos dela faziam **dois trabalhos ao mesmo
tempo**: descreviam o desenho *e* narravam como o desenho chegou ali. O
`plano.md` tinha 1.289 linhas onde uma seção explicava que houve uma rubrica,
outra explicava quais scripts saíram, outra explicava quais variáveis
dependentes morreram. Medido no fim: **27 dos 78 avisos em destaque existiam só
para contar o que mudou** — 35%.

**A regra da v2, e vale para todo documento daqui:** um documento, um trabalho.

| documento | trabalho |
|---|---|
| `plano.md` | o desenho, no presente. Sem data, sem riscado, sem "revisto em" |
| `pre-registro.md` | o congelado: hashes, e o que foi declarado antes de olhar o dado |
| `diario-de-bordo.md` | a história, em ordem cronológica. É aqui que a arqueologia é bem-vinda |

### O que veio da v1 sem mudar nada

| artefato | por quê |
|---|---|
| `experimento/prompt/prompt.md` | 25 execuções já rodaram neste texto, e os casos de teste foram gerados a partir dele. sha256 `53db3424b3972795…` |
| `experimento/harness/CLAUDE.md` | é o tratamento. Hash de árvore `560577922737dbb9…` |
| `infra/docker/Dockerfile` e o projeto de aquecimento | tudo com versão exata |

Os dois primeiros são **congelados por hash**, e o hash é de conteúdo byte a
byte: um editor que normalize fim de linha muda o hash sem mudar uma palavra.
Por isso o `.gitattributes` trata `experimento/**` como binário.

### O que a v1 ensinou, e que virou restrição de desenho

Estes são fatos medidos nas 49 execuções de calibração, não opiniões.

**A variância entre execuções idênticas é grande.** Mesmo modelo, mesma
condição, mesmo enunciado, mesmo harness: o consumo de entrada variou de **1,5×
a 2,4×**, e as formas de código produzidas foram diferentes entre si. Uma
réplica não distingue efeito de sorteio. Daí `n`=3, fechado antes do lote.

**A leitura por mediana de célula produz conclusão errada.** Nas 12 execuções
pareadas, lendo o consumo de entrada:

| leitura | opus | sonnet | haiku |
|---|---|---|---|
| mediana de célula | +8% | −28% | +66% |
| pares simultâneos | 3 de 4 positivos, faixa −14% a +99% | **4 de 4 negativos, faixa −15% a −28%** | 3 de 4 positivos, faixa −46% a +79% |

A mediana erra nos dois sentidos: faz o `+66%` do Haiku parecer efeito forte
quando é ruído numa faixa de 125 pontos, e esconde que o Sonnet deu **4 de 4 na
mesma direção** numa faixa de 13 pontos — o achado mais sólido da calibração.
Por isso a tabela principal da v2 compara **pares simultâneos**.

**O ferramental difere entre modelos.** O Haiku recebe 30 ferramentas; o Opus e
o Sonnet, 26. As quatro a mais são `TaskCreate`, `TaskGet`, `TaskList` e
`TaskUpdate`. Como o modelo é o fator de bloco, a diferença entra na H3 — mas as
quatro **nunca foram chamadas** em 25 execuções, então o confundidor é teórico.
Fica declarado, não corrigido.

**O alias e o snapshot datado são o mesmo modelo.** Pede-se `claude-haiku-4-5` e
as mensagens voltam com `claude-haiku-4-5-20251001`; Opus e Sonnet reportam o id
simples. Comparação estrita marca as execuções de Haiku como troca de modelo, e
na v1 isso **teria descartado duas execuções boas**.

**Dez defeitos apareceram nas ferramentas de medição** entre 19 e 22/09/2026,
três deles por acaso. A regra que ficou: ferramenta de medida sem teste próprio
reporta número errado em silêncio, e número errado vira resultado do TCC.

### As decisões fechadas hoje

| decisão | por quê |
|---|---|
| **Ferramentas livres**, sem lista branca nem negra, inclusive subagente | A pergunta é sobre o Claude Code como ele vem. Restringir ferramenta mede uma versão de laboratório dele |
| **Rede aberta** nos dois braços | É o que já rodou. A ameaça — topar com um tutorial canônico de Strategy — fica declarada |
| **Sem conferência de hash no preflight** | O risco real é de uma vez só, na cópia para o repositório novo. Confere-se à mão uma vez |
| **Sem limite de tempo e sem limite de turnos** | Nenhuma das 49 travou; a mais longa levou 585 s |
| **`n` = 3 por célula**, fechado | Acrescentar réplica depois de olhar o dado transforma resultado em escolha |
| **Dois braços, sem placebo** | Um terceiro braço separaria "as quatro regras" de "haver um CLAUDE.md". Custa 9 execuções e 27 avaliações manuais. A limitação fica declarada |
| **A H2 fica** | Consumo de tokens e tempo continuam medidos |
| **A pergunta de pesquisa fica como está** | "reconhecimento e implementação". A avaliação será desenhada para responder a ela, e não o contrário |
| **Cinco scripts**, e nenhum olha o código para julgar | `executar.sh`, `rodada.sh`, `extrair-meta.mjs`, `agregar.mjs`, `anonimizar.mjs` |

### O que saiu do desenho, e não volta

A v1 chegou a ter uma **rubrica** de seis critérios de 0 a 2, com segundo
avaliador e kappa de Cohen. Ela foi aplicada a dois pacotes e, em três
critérios, não decidiu sozinha — numa delas, **invertendo** a classificação. Saiu
em 21/09, e com ela o segundo avaliador, o kappa e três planilhas.

Saiu também a **suíte escondida de 60 casos** e as ferramentas que a rodavam. O
controle de funcionamento passa a ser o `mvn verify` que o `executar.sh` já roda.

E saíram seis scripts que davam palpite sobre resultado ou sobre descarte. O
critério: **fica o que liga o container, o que transcreve o que aconteceu, e o
que prepara os pacotes cegos. Sai o que interpreta.**

### O que ainda não existe, de propósito

**A avaliação não está desenhada.** Este repositório constrói a bancada que
*produz* os pacotes de código; como eles são avaliados é assunto de um documento
próprio, escrito depois, quando os pacotes existirem.

Isso não significa escolher a régua depois de ver o resultado. Significa
escrevê-la depois de ver **que forma o código tem**, e antes de qualquer pacote
do lote ser avaliado.

Consequência a não esquecer: das cinco hipóteses, **só a H2 tem instrumento
hoje**. As outras quatro dependem dessa avaliação.
