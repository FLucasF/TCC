# O protocolo da correção por IA (V6)

Como a correção da construção é feita, e o que a sustenta. Decisões de 15/10 e 17/10/2026, com o orientador: a
correção é da **IA junto com o Lucas**, sem scripts deterministas no lugar da leitura. As ferramentas (a suíte, o
PIT, o relatório de arquitetura, a foto do banco) entram como material para a IA decidir, e a própria correção é
uma IA lendo as réguas em markdown e interpretando o código. O que está marcado como **proposta** ainda é decisão
do Lucas.

## 1. O que se julga

Quatro dimensões, cada uma com a sua régua em `rubric/`:

| dimensão | régua | o material além do código |
|---|---|---|
| a implementação dos padrões | `patterns.md` (P1 a P7, F e E) | nenhum |
| a arquitetura | `architecture.md` (A1 a A6) | o relatório de arquitetura (ArchUnit) |
| a cobertura e a qualidade dos testes | `tests.md` (T1 a T4) | os números do PIT |
| a qualidade do banco | `database.md` (B1 a B10) | a foto do esquema |

Cada item tem uma **lista fechada** de valores. A régua não procura a solução perfeita: pergunta se o código segue
uma prática, e cada prática do banco cita a fonte publicada. A base dos padrões é a régua do Semgrep do V6 (as
perguntas P1 a P7 e as decisões de calibração de 14/10), que por sua vez é a régua do V5 reescrita.

A **correção do comportamento** (a suíte) não é uma das quatro dimensões, e a IA não lê o resultado dela. Se ela
entra como dimensão ou só como condição, está em aberto (ver as notas).

## 2. Quem corrige

Uma IA numa sessão com o Lucas, lendo `rubric/common.md` e a régua da dimensão. **Proposta:** o Claude Opus 5.5,
effort `high`, que foi o testado (seção 4). O modelo, o effort e as réguas ficam registrados em cada lote de
correções; trocar qualquer um deles no meio de um lote é uma emenda datada, e o que já foi corrigido é corrigido de
novo.

## 3. As regras da correção

Estão no `rubric/common.md`, que a IA lê antes de cada correção. Em resumo:

- **Uma dimensão por conversa**, para a impressão de uma não contaminar a outra (o efeito halo).
- **Às cegas**: a IA não abre o que diz quem fez o trabalho ou se ele acertou (o `meta.json`, a transcrição, os
  logs, o resultado da suíte), não usa o nome da pasta e não tenta adivinhar o modelo. O nome da pasta das
  execuções diz o modelo (`OPUS55-N0-03`): **proposta**, nas execuções que valem, o Lucas entrega cada uma numa
  cópia com um nome neutro e guarda a chave fora dela.
- **Julgar pelo que o código faz**: os comentários, os READMEs e os nomes das classes não contam. Na validação, a IA
  leu o projeto sem comentários; na correção sem script, ela lê o projeto como está e é instruída a ignorá-los.
- **Evidência obrigatória**: toda resposta cita `arquivo:linha` ou o elemento do material.
- **Na dúvida, `indeterminado`**, que conta como discordância na conferência (a regra do V5).

## 4. A validação das réguas

Quatro provas. As duas primeiras foram feitas em 17/10/2026 (os números estão no README; as respostas, em
`batches/`).

1. **Erros plantados.** O `planted/runs/PLANTED-01` é uma cópia do OPUS55-N0-03 com defeitos conhecidos, um ou dois
   por dimensão (o `planted/README.md` diz quais). A correção tem de achar cada um e não se confundir com o resto
   (`planted/esperado.md`). Resultado: 21 de 21.
2. **Contra as ferramentas.** Nos pilotos, as respostas que uma ferramenta já deu: P1 a P7 e F pela régua do
   Semgrep (validada em 2.200 de 2.200 respostas contra o V5), e B1 a B7 pela foto do esquema
   (`expected/pilotos.md`). Resultado: 115 de 116.
3. **Contra ela mesma.** Corrigir os mesmos pilotos de novo, em conversas novas, e comparar item a item. Mede quanto
   da resposta é do código e quanto é do acaso. **Falta fazer.**
4. **Contra o Lucas.** O Lucas corrige às cegas uma amostra (três execuções, as quatro dimensões), no mesmo formato
   da IA, e a concordância é contada item a item. **Falta fazer.**

**Proposta de critério:** as réguas só valem para as execuções do TCC se a correção acertar todos os defeitos
plantados e se, em cada dimensão, concordar com ela mesma e com o Lucas em pelo menos 80% dos itens. Um item com
concordância baixa é reescrito (e as provas rodam de novo) ou passa a ser só descritivo.

## 5. Nas execuções que valem

- **Proposta:** cada execução é corrigida **duas vezes**, em conversas separadas; quando as duas discordam num item,
  o Lucas lê aquele item, com a evidência das duas, e a leitura dele vale.
- Sobre o risco de **um Claude corrigir código de Claude**: está declarado. Se a banca pedir, uma amostra pode ser
  corrigida também por uma IA de outra família, com as mesmas réguas, e a concordância entra no texto.
- **As réguas não podem estar nos braços do harness.** Se um braço receber uma skill ou uma regra com as mesmas
  práticas da régua, a correção premia quem recebeu a própria régua. Os harnesses N1 a N3 ainda vão ser
  redesenhados; quando forem, isso se confere ou se declara.

## 6. O congelamento

`python infra/scripts/freeze.py` escreve o `CONGELADO-V6.sha256` (o sha256 de cada arquivo da V6 que decide um
resultado: o enunciado, as réguas, as medidas, a suíte e o gabarito), como o do V5. Ele só roda com tudo
commitado, e só uma vez; `--check` confere. **Ainda não foi rodado**: antes faltam os harnesses, as provas 3 e 4 e as
decisões em aberto.
