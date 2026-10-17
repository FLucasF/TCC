# O protocolo da correção por IA (V6)

Como a correção da construção é feita, e o que a sustenta. Decisões de 15/10 e 17/10/2026, com o orientador: a
correção é da **IA junto com o Lucas**, sem scripts deterministas no lugar da leitura. As ferramentas (o PIT, o
relatório de arquitetura, a foto do banco) entram como material para a IA decidir, e a própria correção é uma IA
lendo as réguas em markdown e interpretando o código. Desde 17/10, a correção é **igual à da manutenção**: a IA
testa o comportamento ela mesma (a 5ª dimensão), a resposta é texto com uma linha de conclusão por item, e um script
que não julga nada prepara a cópia às cegas e sobe a IA. O que está marcado como **proposta** ainda é decisão
do Lucas.

## 1. O que se julga

Cinco dimensões, cada uma com a sua régua em `rubric/`:

| dimensão | régua | o material além do código |
|---|---|---|
| a implementação dos padrões | `patterns/`, um prompt por pattern (Strategy: P1 a P5; Adapter: P6; State: P7; Factory: F; e, em todos, o E) | nenhum |
| a arquitetura | `architecture.md` (A1 a A6) | o relatório de arquitetura (ArchUnit) |
| a cobertura e a qualidade dos testes | `tests.md` (T1 a T4) | os números do PIT |
| a qualidade do banco | `database.md` (B1 a B10) | a foto do esquema (`evaluation/measures/photograph_database.py`) |
| o comportamento | `behavior.md` (C1 a C10) | nenhum: a IA sobe o serviço com um PostgreSQL ao lado e testa ela mesma |

Cada item é uma pergunta, com as definições e o que conta como certo e como problema; a resposta é um **feedback
escrito**, com evidência, e **uma linha de conclusão por item** (`acerto`, `omissão`, `malfeito`, `contenção`,
`exagero` nos padrões; `segue`, `em parte`, `não segue` nas outras dimensões; `não se aplica` em todas). Decisão do
Lucas, 17/10: o texto é o que se lê, a conclusão é o que se conta, nas duas pesquisas. A régua não procura a solução perfeita:
pergunta se o código segue uma prática, e cada prática do banco cita a fonte publicada. A base dos padrões é a régua do Semgrep do V6 (as
perguntas P1 a P7 e as decisões de calibração de 14/10), que por sua vez é a régua do V5 reescrita.

O **comportamento** é a 5ª dimensão (decisão de 17/10): a suíte deixou de ser material da correção, e a IA testa
o serviço ela mesma, pela API, com um PostgreSQL 18 ao lado e uma TransLog falsa que ela própria escreve.

## 2. Quem corrige

Uma IA sozinha, às cegas, num container da imagem do experimento, lendo `rubric/common.md` e a régua da dimensão:
`evaluation/judge/judge_with_agent.py` monta a pasta de trabalho com um código neutro (`exec-01`…), sobe a IA e guarda
a correção em `analysis/<lote>/judge/`. O script não julga nada. O Lucas lê as correções depois. **Proposta:** o Claude Opus 5.5,
effort `high`, que foi o testado (seção 4). O modelo, o effort e as réguas ficam registrados em cada lote de
correções; trocar qualquer um deles no meio de um lote é uma emenda datada, e o que já foi corrigido é corrigido de
novo.

## 3. As regras da correção

Estão no `rubric/common.md`, que a IA lê antes de cada correção. Em resumo:

- **Uma dimensão por conversa**, e nos padrões **um pattern por conversa** (como na manutenção, que tem um prompt
  por pattern), para a impressão de uma não contaminar a outra (o efeito halo). Cada pattern traz, além do ponto que
  pede o padrão, o remendo e o controle negativo dele, do mapa de cenários das notas.
- **Às cegas**: a pasta de trabalho só tem o enunciado, o projeto (sem os arquivos do harness) e o material, com um
  código neutro no lugar do nome da execução (inclusive na linha do PIT); a chave fica fora do container
  (`key.json`). A IA não vê o `meta.json`, a transcrição nem os logs.
- **Julgar pelo que o código faz**: os comentários, os READMEs e os nomes das classes não contam. Na validação, a IA
  leu o projeto sem comentários; na correção sem script, ela lê o projeto como está e é instruída a ignorá-los.
- **Evidência obrigatória**: toda afirmação cita `arquivo:linha` ou o elemento do material.
- **Na dúvida, dizer**: o que a evidência mostra e o que fica incerto, sem arredondar para o lado mais provável.

**A leitura das correções.** Uma IA lê as correções escritas para juntar os resultados (por modelo, por braço do
harness) e para as comparações da seção 4. Como as respostas não são palavras de uma lista, a contagem e a
concordância são uma interpretação dessa IA, que precisa citar as correções em que se apoia; o Lucas confere uma
amostra dessa leitura, como confere a correção.

## 4. A validação das réguas

Quatro provas. As duas primeiras foram feitas em 17/10/2026, com a versão anterior das réguas, que pedia um valor de
lista em cada item (as perguntas, as definições e os critérios são os mesmos de agora; mudou só a forma da
resposta). Os números estão no README; as respostas, em `batches/`. Numa correção escrita, a conferência é se o
texto diz o mesmo que o esperado (por exemplo, que o brinde do GOLD é decidido fora do nível).

1. **Erros plantados.** O `planted/runs/PLANTED-01` é uma cópia do OPUS55-N0-03 com defeitos conhecidos, um ou dois
   por dimensão (o `planted/README.md` diz quais). A correção tem de achar cada um e não se confundir com o resto
   (`planted/esperado.md`). Resultado: 21 de 21.
2. **Contra as ferramentas.** Nos pilotos, as respostas que uma ferramenta já deu: P1 a P7 e F pela régua do
   Semgrep (validada em 2.200 de 2.200 respostas contra o V5), e B1 a B7 pela foto do esquema
   (`expected/pilotos.md`). Resultado: 115 de 116; a diferença era a régua (os gramas no P6), e, decidido que os gramas entram, as 116 conferem.
3. **Contra ela mesma.** Corrigir os mesmos pilotos de novo, em conversas novas, e ver, item a item, se as duas
   correções chegam à mesma conclusão. Mede quanto da resposta é do código e quanto é do acaso. **Falta fazer.**
4. **Contra o Lucas.** O Lucas corrige às cegas uma amostra (três execuções, as quatro dimensões), escrevendo do
   mesmo jeito, e compara, item a item, se ele e a IA chegaram à mesma conclusão. **Falta fazer.**

**Proposta de critério:** as réguas só valem para as execuções do TCC se a correção achar todos os defeitos
plantados e se, em cada dimensão, chegar à mesma conclusão que ela mesma e que o Lucas na grande maioria dos itens.
Um item em que as conclusões divergem muito é reescrito (e as provas rodam de novo) ou passa a ser só descritivo.

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
