# Decisões abertas

O que falta fechar. Este documento **encolhe até zero** — decisão fechada sai
daqui e entra no `plano.md` escrita no presente, e o motivo dela vai para o
`diario-de-bordo.md`.

Nada aqui é opinião solta: cada item trava alguma coisa concreta, e está dito o
quê. Cada um traz uma recomendação, o motivo dela, e as outras opções.

**Ordem de urgência:**

| grupo | trava | itens |
|---|---|---|
| **A** | a primeira execução do lote | 1 a 6 |
| **B** | a avaliação dos pacotes | 7 a 14 |
| **C** | a entrega do TCC | 15 e 16 |

Os do grupo A precisam fechar antes de qualquer execução, porque entram no
pré-registro. Os do grupo B **também**, quando definem o que é medido — um
critério de medida escolhido depois de ver o dado não é critério, é resultado.

---

# A · Trava a primeira execução

## 1. O recorte, com o orientador

**A pergunta de pesquisa fala em "reconhecimento e implementação" do padrão. O
instrumento mede a consequência.**

O teste de extensão conta quantos arquivos existentes precisam mudar para uma
variante nova entrar. Isso mede se o desenho ficou extensível — não mede se o
modelo *percebeu* que precisava de Strategy. Um modelo que chegue a um desenho
extensível por acaso pontua igual a um que reconheceu o problema.

Junto, na mesma conversa: **a P4.** A matriz do orientador tem "1" em cada
célula. Isso é uma *tarefa* por célula (o que foi assumido) ou uma *execução*
por célula? Se for a segunda, o desenho de 18 execuções muda.

| | opção | consequência |
|---|---|---|
| **(a)** | Aceitar a contagem e **reescrever a pergunta**: "um harness reduz o custo de acrescentar uma variante nova?" | Instrumento e pergunta passam a coincidir. Abandona a palavra "reconhecimento" e enfraquece a promessa de avaliar "em profundidade" |
| (b) | Aceitar a contagem e promover a coluna `forma` de descritiva a **desfecho secundário declarado**, como indicador de reconhecimento | Triangula o construto sem custo de execução. Exige critérios escritos para as seis formas — ver §7 |
| (c) | Reabrir a rubrica qualitativa | As três ambiguidades que a derrubaram voltam a ser bloqueio, e uma delas **invertia** a classificação entre "parcial" e "sem Strategy" |

**Recomendo (a) combinado com (b).** É a única leitura em que o que se pergunta
e o que se mede são a mesma coisa, e o (b) custa uma coluna de anotação, não uma
execução. Mas **o recorte é decisão do orientador, não de implementação** — ele
escreveu "Design de baixo nível" na matriz, e é ele quem diz se uma contagem
objetiva honra isso.

## 2. O enunciado: byte a byte, ou versão nova?

Foram identificados quatro pontos de redação melhoráveis no enunciado.

| | opção | consequência |
|---|---|---|
| **(a)** | **Byte a byte** (sha256 `53db3424…`), com os quatro tratados como nota no gabarito e declarados "identificados, avaliados e mantidos" | Preserva a calibração inteira: 25 execuções já rodaram neste texto, e os 71 casos de teste foram gerados a partir dele |
| (b) | Versão nova, com **todas** as quatro correções de uma vez, hash novo no pré-registro | A calibração da v1 vira referência aproximada. Custa reconferir os 71 casos |

**Recomendo (a).** Um dos quatro pontos já foi testado contra o dado: o segundo
exemplo conferido desambigua a fórmula de juros, e um modelo errou **mesmo com o
valor certo escrito na tela** — isso é falha de leitura, não de redação.

**Não existe meio-termo.** Corrigir um ponto e não os outros, ou corrigir no meio
do lote, é o pior dos mundos.

## 3. Ferramentas e rede

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

**Rede.**

| | opção | consequência |
|---|---|---|
| (a) | Aberta nos dois braços | `web_calls` e `suspect_commands` viram desfecho reportado por braço e por modelo. A ameaça do tutorial canônico de Strategy entra nas ameaças à validade |
| (b) | `--network none` no container do agente | Exige `mvn -o` e tira do agente a possibilidade de acrescentar dependência |
| (c) | Proxy liberando só a API da Anthropic e o repositório Maven | Mais controle, mais peça para manter |

**Recomendo (a)**, porque é o que já rodou e porque bloquear a web muda o que o
agente é. Mas seja qual for, tem que ser **idêntica nos dois braços e nos três
modelos**, e a escolha toca três lugares que não podem divergir: a linha do
`--tools`, a configuração de rede do `docker run`, e a seção de ameaças.

## 4. O pré-registro vira executável?

Hoje o executor **calcula** os hashes do enunciado e do harness e os grava no
`meta.json` — mas não compara com nada. Se alguém editar o enunciado por engano
depois do pré-registro fechar, nada falha e o lote sai contaminado em silêncio.

**Recomendo:** um `pre-registro.lock` versionado com os hashes do enunciado, do
harness, do digest da imagem e dos scripts de medição, **conferido no preflight,
que aborta na divergência**. Não avisa: aborta.

Duas notas:

- Isso trava o lote diante de qualquer edição — que é exatamente o comportamento
  desejado, e precisa estar combinado com o orientador antes da coleta.
- O `cp -r harness/. workspace/` é indiscriminado. Um `.bak` ou um README
  esquecido na pasta entra no workspace do agente e queima o lote. O lock
  precisa cobrir **o conteúdo exato da pasta**, não só o `CLAUDE.md`.

**Carimbo de data externo.** O pré-registro é um arquivo dentro do próprio
repositório, e a única prova de que ele precede o dado é o histórico do git, que
o autor controla. Vale considerar um carimbo fora do seu alcance — um e-mail
datado ao orientador com o hash do commit resolve, e é grátis.

## 5. Dois braços ou três?

O braço HARNESS difere do CONTROL em **três** coisas ao mesmo tempo:

1. o conteúdo das quatro regras
2. a existência de um arquivo de orientação de processo
3. o acréscimo de contexto ao prompt

Se o efeito aparecer, o desenho atual não distingue qual das três o causou.

| | opção | consequência |
|---|---|---|
| (a) | **Três braços**: CONTROL × PLACEBO × HARNESS. O placebo é um `CLAUDE.md` do mesmo tamanho e do mesmo registro de voz, com orientação genérica sem relação com variação ou estrutura | Separa conteúdo de dose. É a única forma de sustentar que o efeito vem *das regras*. O lote vai de 18 para **27** execuções e de 54 para **81** aplicações manuais |
| **(b)** | **Dois braços**, com a variável independente redefinida por escrito como "a presença de um `CLAUDE.md` com orientação de processo" | Honesto e barato. A afirmação fica mais fraca, e a limitação entra na §4 do plano |

**Recomendo (b)**, pelo trabalho manual: o placebo acrescenta 27 aplicações de
extensão à mão, e o gargalo do projeto é humano, não computacional. Mas a
escolha muda a redação da pergunta, então ela vem **antes** de fechar a §1.

## 6. Teto de parede e critério de interrupção

Não há limite automático de tempo nem de turnos. A única parada é humana — e o
plano reconhece que interromper à mão é julgamento do autor, que **entra no
experimento**.

Nenhuma das 49 execuções foi interrompida, e a mais longa levou 585 s.

**Recomendo:** timeout de parede generoso como rede de segurança operacional —
cerca de 30 minutos, ~3× a mais longa já observada —, com a interrupção
registrada como dado e um valor de validade definido **antes**. Não é corte
cognitivo: é seguro contra execução travada.

`--max-turns` fica descartado: aquilo corta o raciocínio e muda o que se mede.

---

# B · Trava a avaliação

## 7. A unidade do desfecho: arquivo ou tipo de topo?

Java permite vários tipos de topo por arquivo, e **isso já aconteceu no dado**:
uma execução tem 12 arquivos `.java` de produção e 23 tipos de topo declarados.
Contar arquivos, ali, subestima a dispersão.

| | opção | consequência |
|---|---|---|
| **(a)** | **Tipo de topo** declarado em nível de arquivo | Continua mecânico — um contador de declarações sobre o diff resolve — e deixa de depender de como o modelo distribuiu tipos em arquivos. `git diff --numstat` vira medida secundária de linhas |
| (b) | Arquivo, declarando a taxa medida de distorção | Mais simples, e a distorção fica registrada |

**Recomendo (a)**, com uma regra escrita: **tipo aninhado privado não conta.**
Um `private static class` dentro do serviço não é ponto de extensão.

**E, na mesma decisão: "registro declarativo" conta 0 ou 1?** Quando a variante
nova entra só acrescentando uma linha num mapa ou numa lista, isso é zero
arquivo alterado ou um?

Recomendo separar por **quem faz o registro**: se o framework descobre sozinho
(injeção de `List<T>` do Spring, por exemplo), conta **0** — ninguém precisou
editar nada. Se um humano precisa acrescentar a entrada à mão, conta **1**.

## 8. O executor de casos precisa aceitar um diretório

**Isto bloqueia literalmente o desfecho primário.** O executor hoje monta o
caminho a partir de um `run_id` — e os pacotes anonimizados moram em outro lugar,
com código cego. Não existe caminho suportado para apontá-lo para um pacote.

Pior: usar `run_id` para avaliar **quebra a cegueira**, porque o `run_id` diz a
condição.

**Recomendo:** o executor passa a receber caminho e rótulo como argumentos
independentes, e o modo `run_id` vira açúcar sintático.

**A cadeia inteira nunca rodou de ponta a ponta.** As 49 execuções provaram a
metade que *produz* dado; a metade que o *avalia* nunca foi exercitada — a pasta
de pacotes não existe, e nenhuma planilha foi preenchida. Isso tem que rodar num
pacote de calibração antes do lote.

## 9. Cronometrar uma aplicação de extensão

É **o último número que falta** para dimensionar o lote, e é o único limite real
que sobrou.

Fazer em 1 ou 2 pacotes de calibração, **nunca do lote**, escolhendo o par que
mais separa: um desenho com `enum` com corpo contra um com classes e injeção de
lista. O mesmo ensaio testa o executor novo do §8 — os dois problemas se
resolvem de uma vez.

## 10. O `n`, e a forma de reportar

A variância entre réplicas idênticas atinge o desfecho primário **na mesma ordem
de grandeza do efeito procurado**. Aumentar um pouco o n não resolve isso.

| | opção | consequência |
|---|---|---|
| (a) | Subir para 5 ou 6 por célula | ~US$ 36 e ~48% de uma janela de cota — barato. Mas vai de 54 para **90 ou 108** aplicações manuais |
| **(b)** | Manter 3 e **trocar a tabela principal**: publicar a distribuição inteira (x/3 em 0 / 1 / 2+) e os **três deltas pareados por modelo** | O pareamento é o que compensa parcialmente a variância, e hoje a análise o joga fora ao calcular a diferença sobre medianas de célula |

**Recomendo (b).** Com n=3 a mediana esconde justamente o que interessa, e o par
simultâneo SEM/COM é a única estrutura do desenho que cancela variação de
horário e carga. Usar a mediana desperdiça o pareamento que a rodada simultânea
paga para obter.

## 11. A regra de leitura de cada hipótese

**Nenhum critério numérico está escrito.** O Mann-Whitney foi descartado com
razão — com n=3 por braço o menor p bicaudal possível é 0,10 — e **nada foi posto
no lugar**. O pré-registro declara fora "qualquer teste não listado", então sem
isso não há como dizer se uma hipótese foi apoiada.

**Recomendo** regras determinísticas e descritivas, escritas antes do lote. Por
exemplo, e são exemplos a discutir, não proposta fechada:

- **H1 apoiada** se a mediana de arquivos alterados em HARNESS for ≤ à de
  CONTROL nos três modelos **e** pelo menos 7 dos 9 pares simultâneos tiverem
  delta ≤ 0
- **H4 apoiada** se P1 ≤ P2 ≤ P3 em pelo menos 5 das 6 células
- **H5 apoiada** se o delta em P2 e P3 for mais negativo que em P1 nos três modelos

E, junto: **o que significa um resultado nulo.** O desfecho tem três níveis na
prática (0, 1, 2+). Declarar antes qual diferença é grande o bastante para
interessar evita que o resultado seja interpretado depois conforme deu.

## 12. Regra única de dado faltante

Hoje há **duas regras que se contradizem**: uma diz que build quebrado é execução
válida e conta como resultado; outra diz que pacote que não compila não entra na
tabela principal.

**Recomendo:** uma tabela única, situação → valor de `valid` → entra ou não na
tabela principal, com a categoria **"não avaliável"** para pacote que não compila
ou não passa na suíte, e a célula publicando `n<3` explicitamente.

Descartado: exclusão silenciosa. Uma célula com 2 réplicas tem que aparecer como
tendo 2.

**E o caso do par que morre pela metade:** se um braço falha por infraestrutura,
refaz-se o par inteiro (preserva a simultaneidade, custa o dobro) ou só a metade
que morreu (quebra o pareamento)? Recomendo refazer o par inteiro — o pareamento
é o ativo mais valioso do desenho.

## 13. Testes e dependências

**Duas portas por onde variação que nada tem a ver com Strategy entra direto no
desfecho.**

**Arquivo de teste escrito pelo modelo.** O enunciado pede `mvn verify`, o que
convida a escrever teste. Se o modelo escreveu testes e a extensão obriga a
mexer neles, isso conta como "arquivo existente alterado"?

Recomendo **contar as duas colunas separadas** e reportar ambas. Custa uma
coluna e é a opção mais informativa.

**Dependência geradora de código.** Uma biblioteca que gera código muda o que
"arquivo alterado" significa. Recomendo tratá-la como **covariável descritiva
reportada por braço**, com regra de exclusão condicional escrita no
pré-registro e disparada só por biblioteca geradora de código.

## 14. Quem aplica as 54 extensões, e a auditoria

O mesmo autor escreveu o harness, escolheu o enunciado, roda o lote, aplica as 54
extensões e conta os arquivos. A anonimização embaralha e tira os rastros, mas
não muda quem é a pessoa.

**Recomendo** uma auditoria pré-especificada: **k pacotes sorteados com semente
registrada** — sugiro 6 dos 18 —, o orientador aplica as três extensões sem ver a
planilha do autor, e reporta-se a **concordância exata do inteiro** por (pacote,
ponto). O limiar de divergência aceitável é declarado antes.

Isso não é kappa nem segundo avaliador: é conferência de **procedimento**. E dá
ao trabalho a única prova externa que ele pode ter de que a contagem não é
conveniente.

---

# C · Trava a entrega

## 15. Backup fora da máquina

**O repositório não tem remoto.** `git remote -v` volta vazio. As execuções e os
pacotes do lote **são o dado primário e irrepetível do TCC**, e hoje não existe
segunda cópia de nada.

Isso não trava tecnicamente, e por isso é o mais fácil de pular.

**Recomendo:** remoto privado, com push depois de cada rodada. O que é rastreado
são ~25 MB e o `.git` tem 4,4 MB — os ~970 MB de `target/` já estão fora por
`.gitignore`. O `.env` com o token está ignorado e **precisa continuar**.

Alternativa mínima: cópia para um disco separado depois de cada rodada,
registrada no diário.

## 16. Onde a monografia mora

A v1 produz dado e para. O único lugar do repositório que trata da escrita são
cinco caixas de checklist.

Decidir antes do lote:

1. A monografia mora neste repositório ou em outro? Se em outro, o backup do §15
   vale para os dois.
2. O que vai no apêndice, em formato concreto: o enunciado de 146 linhas
   inteiro, o harness de 14, os 71 arquivos de caso ou só os 11 de extensão, e a
   tabela de hashes.
3. Como cada tabela de resultado carrega, ao lado, **o comando que gerou o CSV e
   a data** — já que as tabelas não saem mais de script, e sim da leitura do CSV
   à mão.

---

# Fora da lista, mas decidido

Registrado aqui só para não voltar à mesa:

- **Proibido acrescentar réplicas depois de olhar o dado.** Com cota sobrando e
  um lote barato, a tentação de "rodar mais três" depois de ver um resultado
  quase-significativo é real, e é a forma mais comum de contaminar um
  experimento pequeno. O `n` fecha antes e não se mexe.
- **Detecção de troca de snapshot do modelo no meio do lote.** O campo que grava
  os modelos observados nas mensagens já serve de sentinela. Se a Anthropic
  publicar um snapshot novo no meio do lote, isso aparece no dado — e a regra é
  registrar, declarar, e decidir com o orientador. Não é para ser descoberto
  depois.
