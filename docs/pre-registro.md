# Pré-registro

Escrito em 24/09/2026, **antes de qualquer execução do lote**.

Este documento existe para que nada aqui possa ser decidido depois de olhar o
dado. Ele não descreve como a bancada funciona — isso é o `plano.md` — e não
conta a história das decisões — isso é o `diario-de-bordo.md`.

**O que muda depois desta data entra como emenda numerada no fim, com data e
motivo.** Emenda silenciosa é o mesmo que não ter pré-registro.

> [!danger] Estado em 24/09/2026: falta uma coisa, e ela bloqueia o lote
> A **§7, regra de leitura das hipóteses**, está em aberto. Sem ela não existe
> critério para dizer se uma hipótese foi apoiada, e qualquer critério escrito
> depois dos números terá sido escolhido por eles.
>
> **O lote não roda antes de a §7 estar fechada e commitada.**

---

## 1. A pergunta

> Um harness — um arquivo `CLAUDE.md` com orientação de processo — aumenta o
> reconhecimento e a implementação do padrão Strategy, quando um agente de código
> constrói uma API a partir de um enunciado que tem pontos pedindo esse padrão?

## 2. As hipóteses

| | hipótese | direção |
|---|---|---|
| **H1** | Com harness, o reconhecimento e a implementação de Strategy são maiores | declarada |
| **H2** | O harness **altera** o consumo de tokens e o tempo | **sem direção** |
| **H3** | O efeito do harness é maior no Haiku 4.5 do que no Opus 5 | declarada |
| **H4** | O acerto cai de P1 para P3, nas duas condições | declarada |
| **H5** | O efeito do harness é maior em P2 e P3 do que em P1 | declarada |

A H2 é não-direcional, e o dado de calibração justifica: o efeito do harness
sobre o consumo **muda de sinal conforme o modelo**.

## 3. O desenho

3 modelos × 2 condições × 3 réplicas = **18 execuções**, em três rodadas.

| | |
|---|---|
| variável independente | condição: `CONTROL` (workspace vazio) · `HARNESS` (com o `CLAUDE.md`) |
| fator de bloco | modelo |
| unidade de análise | o **par simultâneo**: cada `CONTROL` contra a `HARNESS` que rodou no mesmo instante, no mesmo modelo. São 9 pares |

Os três modelos, por ID completo:

```
claude-opus-5
claude-sonnet-5
claude-haiku-4-5
```

`run_id`: `BATCH-<rodada>-<MODELO>-<CONDICAO>`. Duas execuções formam par quando
tudo é igual menos a última parte.

**`n` = 3 por célula, fechado aqui.** Acrescentar réplica depois de olhar o dado
está proibido por este documento.

## 4. Os parâmetros, escolhidos antes

| | valor | por quê |
|---|---|---|
| raciocínio | `--effort medium` nos três | a calibração de custo vale para `medium`, e `medium` compra mais repetições |
| ferramentas | **livres**, sem lista branca nem negra, inclusive subagente | a pergunta é sobre o Claude Code como ele vem |
| rede | **aberta** nos dois braços | é o que rodou na calibração |
| limite de tempo | **nenhum** | nenhuma das 56 execuções de calibração travou |
| limite de turnos | **nenhum** | `--max-turns` corta o raciocínio e mudaria o que se mede |
| workspace inicial | **vazio** | sem esqueleto de projeto |
| versões | **pedidas no enunciado**, não impostas | desobedecer não invalida; vira taxa reportada |

## 5. O que se mede

**Primário**

- Reconhecimento e implementação de Strategy, em P1, P2 e P3, medido **às cegas**
  sobre os pacotes anonimizados.

> [!important] O instrumento do desfecho primário é especificado em segunda etapa
> A régua exata está em `avaliacao.md`, que **ainda não existe**. Ela é escrita
> depois de os pacotes do lote existirem, e **antes de qualquer pacote do lote
> ser avaliado**, e é commitada nessa ordem — a data do commit é a prova.
>
> Isto é pré-registro em duas etapas, e está declarado como tal. O que a segunda
> etapa **não** pode fazer é ser escrita depois de os números existirem: aí a
> régua teria sido escolhida pelo resultado.
>
> Três restrições que ela tem de respeitar, e que ficam fixadas aqui:
>
> 1. **Cega.** Quem avalia recebe `avaliacao/pacotes/<CODIGO>/` e não sabe de que
>    braço veio.
> 2. **Congelada antes de comparar.** O resultado da avaliação é commitado antes
>    de o mapa de anonimização ser reaberto.
> 3. **Com regra de leitura escrita antes** dos números existirem — ver §7.

**Secundário (H2), medido pela bancada automaticamente**

`tokens.input_total`, `tokens.output`, `tokens.thinking`, `timing.duration_api_ms`,
`outcome.turns`, `outcome.tool_calls`.

> `input_total` e **não** `input`. Quase tudo entra por cache: o campo `input`
> sozinho fica na casa das centenas em execuções que consumiram milhões.
>
> `duration_api_ms` e **não** `duration_s`. As seis execuções de uma rodada são
> simultâneas e disputam CPU, o que contamina o relógio de parede.

**Controle**

`outcome.build_ok` · `foundation.versions_obeyed` · `models_observed` ·
`isolation_init.tools_available` · `outcome.permission_denials` ·
`outcome.subagents_spawned` · `environment.api_key_source`.

## 6. O que invalida uma execução

| situação | vale? |
|---|---|
| falha de infraestrutura: cota, erro de Docker, 5xx da API, interrupção à mão | **não vale.** Refeita do zero, com id novo e registro no diário |
| build quebrado | **vale, e conta como resultado** |
| desobediência às versões pedidas | **vale.** Vira taxa reportada por modelo e condição |
| outro modelo nas mensagens | **a decidir caso a caso**, com o motivo escrito |

O campo `valid` do `meta.json` nasce `null` e é preenchido **por humano**. Nenhum
script opina sobre isso.

> [!warning] Alias e snapshot datado são o MESMO modelo
> Pede-se `claude-haiku-4-5` e as mensagens voltam com
> `claude-haiku-4-5-20251001`. A comparação remove **só o sufixo de data de 8
> dígitos**. `startsWith` não serve: `claude-opus-5-1` começa com
> `claude-opus-5` e é outro modelo.

## 7. A regra de leitura de cada hipótese

> [!danger] EM ABERTO — bloqueia o lote
> Para cada hipótese, o critério que decide se ela foi apoiada tem de estar
> escrito **aqui**, antes de os números existirem.
>
> O Mann-Whitney foi descartado com razão: com n=3 por braço existem C(6,3)=20
> arranjos possíveis, e o menor p bicaudal alcançável é **0,10**. Nada foi posto
> no lugar ainda.
>
> A §9 deste documento declara fora "qualquer corte, agrupamento ou teste não
> listado na §5" — então, sem a §7 fechada, não existe resposta legítima depois.

## 8. Como os resultados são lidos

**A tabela principal compara pares simultâneos, não medianas de célula.** Isto
não é preferência de apresentação: foi medido, e a mediana **produz conclusão
errada**.

Nas 12 execuções pareadas de calibração, lendo o consumo de entrada:

| leitura | opus | sonnet | haiku |
|---|---|---|---|
| mediana de célula | +8% | −28% | +66% |
| **pares simultâneos** | 3 de 4 positivos, faixa −14% a +99% | **4 de 4 negativos, faixa −15% a −28%** | 3 de 4 positivos, faixa −46% a +79% |

A mediana erra nos dois sentidos: faz o `+66%` do Haiku parecer efeito forte
quando é ruído numa faixa de 125 pontos, e esconde que o Sonnet deu **4 de 4 na
mesma direção** numa faixa de 13 pontos.

O par simultâneo é a única estrutura do desenho que cancela horário, fila e carga
de servidor, e a bancada já paga por ele.

## 9. O que NÃO está pré-especificado

Declarado para não virar descoberta disfarçada depois:

- A análise qualitativa do que os modelos fizeram no lugar do Strategy
- O catálogo das formas de código pode ganhar entradas novas se o lote produzir
  uma que as 56 execuções de calibração não produziram
- Qualquer corte, agrupamento ou teste estatístico não listado na §5

## 10. As limitações, declaradas antes de ver o dado

**A unidade de generalização é a tarefa, e há uma tarefa.** As 18 execuções são
réplicas de um único enunciado, não uma amostra de tarefas. Nenhuma afirmação da
forma "o harness ajuda a escrever código melhor" está no alcance deste desenho.

**Não há significância estatística possível.** Ver §7. Os resultados são
descritivos.

**Não se isola "as quatro regras" de "haver um `CLAUDE.md`".** O braço `HARNESS`
difere do `CONTROL` em três coisas ao mesmo tempo: o conteúdo das regras, a
existência de um arquivo de orientação, e o acréscimo de contexto ao prompt.
Separar exigiria um terceiro braço com harness neutro, que custaria 9 execuções
e um terço a mais de avaliação manual. Decidido não fazer.

**A variância entre execuções idênticas é grande, e está medida.** Mesmo modelo,
mesma condição, mesmo enunciado, mesmo harness: o consumo de entrada variou de
**1,5× a 2,4×** na calibração, e as formas de código produzidas foram diferentes
entre si.

**O conjunto de ferramentas difere entre os modelos.** Medido na fumaça de
23/09/2026: Haiku 30, Opus 26, Sonnet 26. Como o modelo é o fator de bloco, isso
entra na H3. Decidido **não corrigir** — restringir ferramenta mediria uma versão
de laboratório do Claude Code. Fica como ameaça declarada.

**Não se sabe se `--effort` tem efeito no Haiku.** A flag é aceita pelos três, mas
a transcrição não reporta o valor aplicado de volta — nem o evento `init` nem o
`result` trazem esse campo. Duas execuções de calibração com `high` gastaram
4.054 e 8.922 tokens de raciocínio, dentro da faixa de 3.233 a 22.733 observada
com `medium`.

---

## 11. Os instrumentos, e seus hashes

**Estímulo e ambiente**

| instrumento | hash |
|---|---|
| `experimento/prompt/prompt.md` | `53db3424b397279573658bfc048a369a33e0a2c8b71530252105e4f841bfd124` |
| `experimento/harness/` (hash de árvore) | `560577922737dbb9252fe3dbd0e26d06e45a7abe6b455c002eae61f8ea24b882` |
| `infra/docker/Dockerfile` | `f9dd2d29f2038775d3a522e716e98d6044bf29eeead33f5812fea41bb578abdf` |
| imagem `experimento-harness:v3` | `sha256:54de317c40864b3ea2932396e6d492c63347e9ebf35a816616d572f699e2abd6` |
| Claude Code, dentro da imagem | `2.1.269` |

> [!danger] O hash é de conteúdo byte a byte
> Um editor que normalize fim de linha muda o hash sem mudar uma palavra. Esta
> máquina tem `core.autocrlf=true`, e por isso o `.gitattributes` trata
> `experimento/**` como binário. Conferido no commit `f625da5`: o hash do
> enunciado no git, no disco e neste documento são o mesmo.

**A bancada — congelada em 24/09/2026**

O pré-registro da versão anterior congelava o estímulo e **não** congelava o
instrumento de medida. Isso ficou frouxo: dois defeitos apareceram no extrator em
23/09/2026, e o instrumento que produz os números não estava sob registro.

| script | hash | se tiver defeito, o que se perde |
|---|---|---|
| `infra/scripts/executar.sh` | `be71fb1c98bdc14b` | **a execução.** Define o tratamento: flags, montagens, o que o agente recebe. Só se conserta re-executando |
| `infra/scripts/rodada.sh` | `e6c65d2e4d84ede4` | **o pareamento.** Se as seis não rodarem simultâneas, a §8 perde a base. Irrecuperável |
| `avaliacao/ferramentas/anonimizar.mjs` | `6f4e96ec116312e4` | **a cegueira.** Se vazar a condição e o pacote já tiver sido avaliado, não há como desavaliar |
| `infra/scripts/extrair-meta.mjs` | `8c1d1dd228ae5ddf` | **nada.** A transcrição sobrevive e o `meta.json` é reconstruível |
| `infra/scripts/agregar.mjs` | `4e8001f7a3012b8d` | **nada.** O CSV é derivado do `meta.json`, que sobrevive |

**A distinção é operacional, não decorativa.** Defeito nos três primeiros
compromete as execuções afetadas. Defeito nos dois últimos é emenda datada e
reprocessamento — foi o que aconteceu em 23/09, e nenhum dado foi perdido.

Qualquer alteração em qualquer um dos cinco exige emenda na §13, com data e
motivo. Isto é chato de propósito: é o que impede "ajustei o script e os números
melhoraram".

---

## 12. O que já foi validado, e como

**Fumaça de 23/09/2026**, prefixo `SMOKE-`, **fora da análise**. Seis execuções
simultâneas, 410 segundos, US$ 5,34.

| execução | turnos | `input_total` | `duration_api_ms` | ferramentas |
|---|---|---|---|---|
| HAIKU-CONTROL | 28 | 1.085.090 | 261.432 | 30 |
| HAIKU-HARNESS | 53 | 2.218.077 | 266.121 | 30 |
| OPUS-CONTROL | 22 | 842.403 | 285.552 | 26 |
| OPUS-HARNESS | 20 | 755.682 | 212.891 | 26 |
| SONNET-CONTROL | 33 | 1.483.317 | 282.652 | 26 |
| SONNET-HARNESS | 50 | 2.211.269 | 271.427 | 26 |

As seis terminaram sozinhas e as seis compilaram. Em todas: um modelo só nas
mensagens, `subagents_spawned` = 0, `permission_denials` vazio,
`api_key_source` = `none`, `~/.claude` vazio, nenhum `CLAUDE.md` fora do
workspace, e as versões pedidas obedecidas.

**O extrator foi validado contra 49 transcrições independentes** — todas as
execuções de calibração da versão anterior. Zero quebraram, e `input_total` e
`turns` bateram em **49 de 49** contra a implementação independente daquele
repositório.

**O caminho de execução interrompida foi exercitado** cortando o evento `result`
de uma transcrição real: `input_total` bate exato; `output` e `turns` ficam nulos
de propósito, porque as mensagens parciais trazem contagem do instante em que
foram emitidas e o número sairia errado.

---

## 13. Emendas

Nenhuma até aqui.
