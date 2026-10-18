# Como funciona a construction-v7

Este documento descreve o experimento de construção antes da primeira execução: o que se quer medir, o que o agente
recebe, como uma execução roda, como ela é avaliada, como o avaliador foi conferido, quanto custa e os pontos que
pedem a sua conferência. As partes que o agente lê estão em português; os scripts, em inglês.

## 1. A pergunta

**Quando o enunciado precisa de um pattern, o agente aplica (e aplica certo)? E o harness muda essa resposta?** Para
isso, cada combinação de modelo e cenário roda em três braços, do sem harness ao com a skill.

As duas pesquisas têm a mesma forma: os mesmos 5 patterns, os mesmos cenários, o mesmo domínio, os mesmos braços e o
mesmo jeito de avaliar. A diferença é só o ponto de partida: na manutenção, o agente recebe a base pronta e um pedido
de mudança; na construção, uma pasta vazia e o enunciado do sistema inteiro. Os controles negativos (os cenários
pares) saíram das duas em 18/10/2026: ficam só os cenários que pedem o pattern.

## 2. O desenho

| | |
|---|---|
| patterns | Strategy, Observer, Factory Method, Decorator e Template Method |
| cenários | 5, um por pattern, todos pedindo o padrão (01, 03, 05, 07 e 09, os números da manutenção) |
| braços | N0, N1 e N2 |
| modelos | Claude Haiku 4.5, Claude Sonnet 4.5 e Claude Sonnet 5.5, effort `medium` |
| réplicas | 1 por modelo, cenário e braço: 5 × 3 × 3 = **45 execuções** |
| avaliação | Claude Opus 5.5, effort `high`, às cegas, uma execução por conversa |
| pilha pedida ao agente | Java 21 e Javalin 6.7.0, sem Spring, Maven, dados em memória, porta na variável `PORT`, um jar executável |
| imagem | `experimento-harness:v5`: Claude Code 2.1.288, Java 21, Maven 3.9.16 (a mesma da manutenção) |

## 3. O que o agente recebe

Cada execução recebe **um enunciado só**, montado assim: o texto comum (`experiment/scenarios/frame.md`) com a parte
do cenário no lugar de `{{PEDIDO}}`. Nenhum agente vê os outros cenários.

**O texto comum** descreve a pousada inteira, como a base da manutenção funciona, em prosa e na voz do dono, sem
tabela, lista nem título:
- os 5 quartos (101 e 102 STANDARD para 2, R$ 249,90; 201 e 202 DELUXE para 3, R$ 389,90; 301 SUITE para 4,
  R$ 579,90);
- o preço (a diária por noite, 20% a mais na alta temporada, de 15/12 ao fim de fevereiro e julho inteiro; o plano
  `NON_REFUNDABLE` com 15% de desconto; arredondamento "meio para o par" em cada passo; um exemplo conferido);
- o ciclo da reserva (`PENDING`, `CONFIRMED`, `CHECKED_IN`, `CHECKED_OUT`, `CANCELLED`), o que cada situação permite,
  a devolução no cancelamento, o quarto que não pode ter duas reservas na mesma noite, a capacidade;
- o comprovante (com o texto exato de exemplo), onde ele sai (a confirmação, o check-out, a segunda via) e o e-mail de
  confirmação (assunto e texto exatos);
- a lista de mensagens enviadas (os e-mails não saem de verdade);
- os dois relatórios em CSV (ocupação e financeiro), com as colunas, a ordem e a linha de total;
- a API inteira (as rotas, os campos do JSON, os formatos) e os erros (o código e o status de cada um);
- a parte técnica (Java 21, Javalin 6.7.0 sem Spring, Maven, memória, `PORT` com 7070 como padrão, o jar
  executável, o `mvn verify` passando) e o "não precisa me perguntar nada".

**A parte do cenário** vem no fim, depois de "E agora a parte que precisa estar no sistema desde o primeiro dia". São
os 5 pedidos da manutenção que pedem o pattern, reescritos de "mudança" para "desde o começo", com os mesmos casos, os
mesmos exemplos conferidos e as mesmas frases de crescimento:

| cenário | o que pede | o sinal de que vai crescer |
|---|---|---|
| 01 planos de tarifa (Strategy) | mais 3 planos (convênio, pacote de fim de semana, estadia longa), cada um com preço, regra de uso e devolução, com 3 exemplos conferidos | "o comercial vive inventando plano" |
| 03 avisar a equipe (Observer) | a governança e o financeiro avisados na confirmação e no cancelamento de reserva confirmada | "vai ter cada vez mais gente querendo ser avisada" (restaurante, transfer) |
| 05 o documento por tipo de hóspede (Factory Method) | CPF, empresa (fatura com ISS) ou estrangeiro (booking receipt em inglês), nos três lugares onde o documento sai | "agências de turismo vão precisar de um documento próprio" |
| 07 as regras de envio (Decorator) | bloquear endereço inválido, cópia para o arquivo, modo de teste desviando tudo | "não mandar de madrugada, limite por dia, cada regra valendo sempre ou só num modo" |
| 09 os relatórios (Template Method) | mais 2 relatórios (taxa de turismo, por plano), os 4 com linha de título | "o contador vai pedir mais" (cancelamentos, hóspedes por estado) |

O cenário 05 troca um trecho do texto comum, porque muda o que ele diz: tira de lá o "o hóspede informa o CPF" (os
três jeitos de se identificar vão no fim). Os trechos padrão estão em `experiment/scenarios/defaults/`, e os do 05,
em `05-*.<TRECHO>.md`. O enunciado tem de 1.512 a 1.750 palavras.

## 4. Os braços

Os arquivos do braço vão para a raiz da pasta vazia, antes do agente começar:

- **N0**: nada. Só o enunciado.
- **N1**: um `CLAUDE.md` com dois princípios, sem nomear pattern nem citar a régua: "antes de escrever cada parte,
  pergunte quantos lugares mudariam para o próximo caso parecido; se forem vários, organize para que o próximo caso
  entre num lugar só" e "faça essa organização só onde o enunciado mostra que a variação existe e vai continuar; um
  valor, um texto, um campo pedem uma solução simples".
- **N2**: o mesmo `CLAUDE.md` com uma linha a mais, "antes de decidir a estrutura, consulte a skill `gof-patterns`",
  e a skill `gof-patterns` (a referência dos 23 patterns do GoF, com "quando não usar").

São os mesmos arquivos da `construction-v6`. O script só roda o N1 e o N2 com a pasta do braço commitada, e guarda o
hash dela no `meta.json`.

## 5. Como uma execução roda

`python infra/scripts/run_one_scenario.py <cenário> <braço> <modelo> medium`:

1. **confere** o cenário, o braço, o modelo, que a pasta da execução ainda não existe, que o `.env` tem o token da
   assinatura e nenhuma variável que mude a cobrança ou o modelo (sem ler nem imprimir o valor), e que a imagem existe;
2. **monta** o enunciado (o texto comum com os trechos e o cenário) e confere que não sobrou nenhum `{{…}}`;
3. **prepara** a pasta vazia com os arquivos do braço;
4. **roda o agente** num container da imagem, que só enxerga a pasta de trabalho e o enunciado: `claude -p --model
   <modelo> --effort medium --output-format stream-json --verbose --dangerously-skip-permissions
   --no-session-persistence`, com o enunciado pela entrada. Antes, o container registra a versão do Claude Code, que o
   `~/.claude` está vazio e que não há outro `CLAUDE.md` fora da pasta;
5. **compila** num segundo container, sem o token: o `pom.xml` mais raso, `mvn -B verify`;
6. **guarda** o histórico do git do agente num `agent-git.bundle`, se ele tiver criado um, e escreve o `meta.json`:
   a identidade (o hash dos cenários, do harness e do enunciado, o modelo, o effort), o tempo, os turnos, o custo, os
   tokens, as ferramentas usadas e **se o agente abriu a skill**.

Cada execução fica em `runs/<cenário>-<pattern>/<braço>-<modelo>/` (por exemplo `runs/01-strategy/N2-haiku-4-5/`),
com a pasta do projeto, o enunciado, a transcrição, o build e o `meta.json`. Uma execução nunca é refeita por cima de
outra.

## 6. Como uma execução é avaliada

`python evaluation/evaluator/evaluate_with_agent.py evaluate-runs <cenário> <braço> claude-opus-5-5 high`

**Às cegas.** As execuções de um cenário e braço ganham códigos sorteados (`run-01`, `run-02`, `run-03`); a chave
fica fora do container. O avaliador recebe só o `enunciado.md` (o texto exato que o agente recebeu) e o `projeto/`
(sem `target/`, `.git` e os arquivos do harness, para não saber o braço). Uma execução por conversa.

**O que o avaliador lê** (`evaluation/evaluator/common.md` e depois o `prompt.md` do pattern):
- a definição das quatro classes e do que conta em cada uma;
- para o cenário daquele pattern, o que é acerto, alternativa equivalente, omissão e malfeito, e o comportamento a
  conferir;
- a regra de julgar pelo código, nunca pelos nomes, comentários ou README;
- a regra de que toda afirmação precisa de `arquivo:linha`.

**O que ele faz:**
1. lê o enunciado inteiro e o código da parte do cenário;
2. decide a classe do desenho;
3. procura **estrutura sem necessidade fora da parte do cenário** (o exagero colateral);
4. **testa o comportamento**: copia o projeto, compila, sobe o serviço com `PORT=7070`, chama a API com `curl` como
   o enunciado descreve, confere as regras do cenário e uma ou duas da parte comum, roda o `mvn verify` e conta os
   testes do agente.

**A resposta** é uma análise escrita em markdown. Começa com uma visão geral e com a seção **"Como foi implementado,
e por que o julgamento"**: como o projeto foi construído (os pacotes e as classes principais, o caminho de uma
requisição, onde ficam os dados, os testes do agente e, com mais detalhe, como a parte do cenário foi feita: as
classes, como cada caso é representado e escolhido, onde mora cada regra) e o raciocínio que leva às conclusões (o
que aproxima ou afasta o desenho do pattern, o que pesou no colateral e no comportamento), com `arquivo:linha`.
Depois vêm três itens, cada um fechado por uma linha de conclusão:
- **o enunciado**: `acerto`, `alternativa equivalente`, `malfeito` ou `omissão`;
- **o exagero colateral**: `contenção` (não há) ou `exagero` (há);
- **o comportamento**: `segue`, `em parte`, `não segue` ou `não se aplica`.

A análise é a correção; a linha de conclusão é o resumo que se conta nas tabelas.

## 7. Como o avaliador foi conferido (a calibração)

Antes de avaliar execuções de verdade, o avaliador corrige às cegas **10 soluções de referência** cuja classe se sabe,
duas por cenário. Cada uma é um projeto completo: a base da pousada da manutenção com um patch de referência aplicado,
o que é exatamente um sistema construído do zero com o cenário feito **com o pattern** (`pattern`, esperado
`acerto`) ou **sem ele** (`naive`, esperado `omissão`). As 10 passam no `mvn verify`.

**O avaliador acertou 10 de 10** com o texto atual, que tem a seção "Como foi implementado, e por que o julgamento"
(US$ 5,50, 18/10/2026), em `evaluation/evaluator/<pattern>/calibration/opus-5-5-high/`, e as 10 respostas trazem a
seção. As calibrações anteriores ficaram guardadas: a sem essa seção (10 de 10, US$ 4,87) em
`opus-5-5-high-sem-explicacao/`, e a com os controles (20 de 20, US$ 9,07) em `opus-5-5-high-com-controles/`.

## 8. O que se espera ver

| | N0 | N1 | N2 |
|---|---|---|---|
| a hipótese | o padrão do próprio modelo | mais acertos | o mesmo do N1, ou mais, com a skill |

Na manutenção, a primeira rodada mostrou o harness levando o Haiku e o Sonnet 4.5 a acertos que eles não tinham no N0
(sobretudo no N2), o Sonnet 5.5 melhorando no N1, e o Template Method sem nenhum acerto. A construction-v7 permite ver
se isso se repete construindo do zero, cenário a cenário.

## 9. O custo

Pelos números da manutenção e da construção em Javalin: uns US$ 0,80 a 2,50 por execução (construir o sistema
inteiro custa mais que uma mudança) e uns US$ 0,45 por avaliação. As 45 execuções e as 45 avaliações devem ficar entre
US$ 50 e 70, uns 8% a 10% da cota semanal. Em 3 a 6 frentes paralelas, o lote leva umas 2 a 3 horas.

## 10. Os pontos para você conferir antes de rodar

1. **Sem controles negativos, o exagero só aparece no colateral.** A pesquisa deixa de medir se o agente se segura
   quando a variação não pede estrutura; o exagero colateral (estrutura sem necessidade fora da parte do cenário)
   continua registrado e é o único sinal de excesso.
2. **O exagero colateral deve aparecer mais que na manutenção.** O agente escreve o sistema inteiro, então qualquer
   interface com uma implementação só (um repositório atrás de uma interface, por exemplo, comum em projetos do zero)
   conta como exagero colateral.
3. **O agente pode aplicar padrões em outras partes.** Num cenário de Observer, ele pode, por exemplo, fazer um Strategy
   para os dois planos de tarifa da parte comum. Isso entra no colateral, e não na classe do cenário.
4. **A parte comum já traz dois casos em vários pontos** (dois planos, dois relatórios, um documento). Os critérios
   mandam o pattern cobrir também esses casos (os cinco planos, os quatro relatórios), como na manutenção.
5. **As referências da calibração têm o estilo da base** (escritas pelo Claude). A calibração mostra que o avaliador
   separa as classes nesses projetos; projetos de outros agentes terão outras formas, e a leitura das análises
   escritas é o que compensa.
6. **Uma réplica por combinação.** Cada célula é um caso; os indícios pedem repetição antes de virar conclusão.
7. **O avaliador é um Claude avaliando Claude.** As provas que faltam (a IA contra ela mesma e você corrigindo três
   execuções às cegas) valem para esta pesquisa também.
8. **O texto comum é grande** (cerca de 1.250 palavras antes da parte do cenário). Vale ver no piloto se a parte do fim
   recebe a mesma atenção.

## 11. Os comandos

```bash
python infra/scripts/run_one_scenario.py 01 N0 claude-sonnet-5-5 medium --dry-run
```

```bash
python evaluation/evaluator/evaluate_with_agent.py calibrate claude-opus-5-5 high --workers 4
```

```bash
python evaluation/evaluator/evaluate_with_agent.py evaluate-runs 01 N0 claude-opus-5-5 high --workers 3
```
