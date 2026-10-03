# Plano de implementação — avaliação do TCC

26/09/2026 · Lucas

## Visão geral

São seis partes, e nenhuma mexe na bancada de execução congelada: `executar.sh`, `rodada.sh`, prompt, harness e imagem continuam com os mesmos hashes. Tudo o que entra é instrumento de medida, aplicado depois das execuções.

Regra que vale para todas as partes: um instrumento novo é congelado (commit + hash no README) antes de ser usado em qualquer pacote. É a mesma disciplina que você já usa no pré-registro.

O plano cuida só da **avaliação**. A bancada (o ambiente de execução) já está pronta e congelada, e rodar lotes é execução, acompanhada no §5 do `OBJETIVO.md`. A ordem entre preparar e medir está em "Ordem e dependências", no fim.

## Instruções para o agente

Este plano é executado pelo Claude Code, uma parte por vez. Regras para o agente:

1. **Só implementar partes revisadas.** Revisada hoje: **Parte 0**. As Partes 1 a 5 ainda estão em revisão e não devem ser tocadas.
2. **Não alterar nada congelado:** `executar.sh`, `rodada.sh`, `extrair-meta.mjs`, `agregar.mjs`, `anonimizar.mjs`, `experimento/harness/`, o conteúdo dos prompts e o `Dockerfile`. Mover um arquivo congelado é permitido; mudar os bytes dele não. Conferir com `sha256sum` antes e depois de cada movimentação.
3. **Não abrir nem imprimir o `.env`.** Não abrir mapa de anonimização, exceto no passo que pede isso.
4. **Parar e perguntar antes de:** apagar qualquer arquivo ou pasta, `git subtree`, `git push`, ou qualquer coisa que reescreva histórico.
5. **Um commit por passo**, em português, no estilo do repositório: título curto e corpo explicando o porquê.
6. **Ao fim de cada passo**, rodar a checagem indicada e mostrar a saída real do comando, não um resumo.
7. **Se uma checagem falhar, parar.** Não tentar contornar.

Este plano está em `PLANO-IMPLEMENTACAO.md`, na raiz do `TCC_V3`. Ficou fora dos commits até 03/10, quando o Lucas decidiu versioná-lo.

## Parte 0 — Arrumar a casa

**Objetivo:** não perder o que já foi feito antes de construir em cima.

**Decisão:** um experimento só, com o prompt de 5 pontos (P1 a P5). O lote `BATCH`, de 3 pontos, vira piloto: foi ele que mostrou o efeito de teto e motivou o prompt estendido.

### Reorganizar as pastas

Rodar dentro de `J:\TCC\TCC_V3`, nesta ordem:

- [x] **1. Commitar as execuções `EXT-01` a `EXT-03`.** Antes, conferir com `git status` que nenhum `target/` nem `.env` entra.
- [x] **2. Proteger os arquivos antigos.** Acrescentar `historico/** -text` ao `.gitattributes` (sem isso o git pode mudar o fim de linha do prompt antigo, e o hash junto) e `historico/piloto/pacotes/` ao `.gitignore`.
- [x] **3. Prompt único.** `git mv experimento/prompt/prompt.md historico/piloto/prompt.md` e depois `git mv experimento/prompt/prompt-estendido.md experimento/prompt/prompt.md`. Checagem: o `sha256sum` de `experimento/prompt/prompt.md` começa com `b7cdb594cb49efee` e o de `historico/piloto/prompt.md` com `53db3424b3972795`.
- [x] **4. Recriar o mapa do piloto.**
    1. Guardar o mapa atual: `mv avaliacao/mapa-anonimizacao.csv avaliacao/mapa-anonimizacao.guardado.csv`.
    2. `node avaliacao/ferramentas/anonimizar.mjs $(ls runs | grep '^BATCH-' | LC_ALL=C sort) --seed 24`.
    3. Checagem: os 18 códigos do mapa gerado são exatamente os 18 da `avaliacao/leitura-claude-cego.csv`. Se não forem, parar.
    4. Mover o mapa gerado para `historico/piloto/mapa-anonimizacao.csv`, as 18 pastas geradas em `avaliacao/pacotes/` para `historico/piloto/pacotes/`, e `git mv avaliacao/leitura-claude-cego.csv historico/piloto/`.
    5. Restaurar: `mv avaliacao/mapa-anonimizacao.guardado.csv avaliacao/mapa-anonimizacao.csv`.
- [x] **5. Commitar os mapas.** `git add -f avaliacao/mapa-anonimizacao.csv` (está no `.gitignore`) e `git add historico/`.
- [x] **6. Atualizar a documentação.** No `README.md` e no `experimento/prompt/README.md`: caminhos novos, tabela de hashes, e a decisão (um experimento; `BATCH` = piloto).

As execuções ficam em `runs/`; o prefixo `BATCH` ou `EXT` no nome já diz de qual são.

### Subir no repositório geral sem perder o histórico

`J:\TCC` é o repositório `FLucasF/TCC` (público), e o `TCC_V3` tem `.git` próprio dentro dele. Um `git add TCC_V3` simples só sobe uma referência vazia. O `git subtree add` traz os arquivos e os commits, com os mesmos identificadores, então referências como `5c9c37d` continuam valendo.

Rodar com o Claude Code aberto em `J:\TCC`, não dentro do `TCC_V3`, porque um passo move essa pasta:

1. Conferir que o `TCC_V3` está com `git status` limpo depois dos passos 1 a 6 acima. O subtree só leva o que está commitado.
2. Commitar as alterações pendentes do repositório geral. O subtree exige a árvore limpa.
3. **Perguntar antes.** Mover `J:\TCC\TCC_V3` para `J:\TCC_V3_temp`.
4. **Perguntar antes.** Em `J:\TCC`: `git subtree add --prefix=TCC_V3 J:/TCC_V3_temp master`.
5. Copiar de volta o que o git ignora e o experimento usa: `.env`, `avaliacao/pacotes/` e `historico/piloto/pacotes/`. Sem abrir o `.env`.
6. Checagem: `git log --oneline <commit do subtree>^2` mostra os commits antigos (por exemplo `5c9c37d`) e os hashes do README batem com `sha256sum`. (`git log -- TCC_V3` não serve: numa subtree ele só mostra o commit de incorporação, porque os commits antigos gravaram os caminhos sem o prefixo.)
7. **Perguntar antes.** Apagar `J:\TCC_V3_temp` e dar `git push`.

O `.gitignore` e o `.gitattributes` do `TCC_V3` continuam valendo dentro da subpasta, e os scripts acham a raiz pelo próprio caminho, então nada quebra. O `TCC v2` pode entrar do mesmo jeito, como histórico.

**Pronto quando:** as checagens dos passos 3, 4 e 6 passaram, `git status` está limpo nos dois repositórios e o push foi feito.

**✅ Feito em 26/09/2026.** Push em `FLucasF/TCC`, `main` em `02d4c10`. O que diferiu do texto acima:

- As duas execuções `TESTE-P4-OPUS-*`, que o plano não citava, foram commitadas como teste, fora do desenho, como as `SMOKE`.
- O `TCC v2` entrou junto, também por subtree (36 commits).
- Antes do subtree, o `.gitattributes` do `TCC_V3` ganhou `*.mjs` e `Dockerfile` com `eol=lf` (commit `993a381`): num clone com `core.autocrlf=true`, 4 dos 8 hashes do README deixavam de bater. Depois, `* text=auto eol=lf` como regra geral (`02d4c10`).
- A cópia de volta levou tudo o que `git ls-files --others` listava (2874 arquivos no `TCC_V3`, incluindo este plano) e recriou as pastas vazias, não só `.env` e pacotes.
- A pasta não pôde ser movida da sessão que tinha começado dentro dela ("Device or resource busy"); o subtree rodou numa sessão aberta em `J:\TCC`.
- `TCC_V3/.claude/settings.json` com `claudeMdExcludes`: sessões no `TCC_V3` não carregam o `J:\TCC\CLAUDE.md` (conferido com `claude -p`, com controle numa pasta vizinha). Não afeta as execuções, que rodam no container.

## Parte 1 — Régua do desfecho

**Objetivo:** transformar "o padrão foi aplicado?" numa classificação que duas pessoas aplicam e chegam no mesmo resultado. Medir as hipóteses de desenho do `OBJETIVO.md` (D1 a D5, N1 a N3) e servir a qualquer padrão, não só ao Strategy: o Strategy é o primeiro.

**Formato: três níveis.** Só o terceiro é escrito de novo a cada enunciado.

| nível | vale para | o que diz |
|---|---|---|
| **1. Propriedades** | todo padrão, todo enunciado | o que se observa em cada ponto de variação, em termos de código: onde mora cada caso, como o caso é escolhido, se a assinatura comporta o caso mais exigente, se a parte comum se repete, quantos lugares se editam para um caso novo, e se a estrutura é proporcional à variação |
| **2. Ficha do padrão** | todo enunciado daquele padrão | quais propriedades contam como **acerto** e quais como **exagero**. Primeira ficha: Strategy |
| **3. Gabarito do enunciado** | só aquele enunciado | a lista dos pontos, os identificadores dos casos de cada um, qual é positivo, qual é controle negativo, qual tem caso mais exigente |

**Entrega:** `avaliacao/regua.md` (níveis 1 e 2) e `avaliacao/strategy/gabarito.md` (nível 3 do enunciado atual, na mesma pasta dos pacotes que lê, com cabeçalho de padrão, enunciado, hash e lotes), com:

1. Cada propriedade com seus valores possíveis e a regra observável de cada valor.
2. A forma encontrada (`classes`, `enum-abstrato`, `enum-dados`, `switch`…) registrada como **descrição**, não como veredito.
3. Evidência obrigatória em cada célula (`arquivo:linha`). Sem evidência, o valor é `indeterminado`.
4. A planilha em formato longo, uma linha por pacote × ponto, para o kappa sair por propriedade e para um padrão novo não mudar as colunas.
5. Exemplos resolvidos de pacotes fora da análise: os **SMOKE** (enunciado de três pontos, cobrem P1 a P3) e os **TESTE-P4** (enunciado atual, cobrem P4 e P5).

**Pronto quando:** aplicada aos pacotes SMOKE e TESTE-P4 sem nenhum caso em que você hesite entre dois valores. Cada hesitação vira uma regra escrita antes de fechar. Depois, commit e hash no README.

## Parte 2 — Suíte oculta de aceitação

**Objetivo:** saber se o código calcula certo. Hoje o `mvn verify` só roda os testes que o próprio modelo escreveu.

**Como:** testes de caixa-preta via HTTP contra o contrato que cada enunciado já define: `POST /checkout/resumo` no Strategy (`strategy.mjs`), `/pedidos` no State (`state.mjs`). Caixa-preta porque cada execução tem pacotes e classes diferentes. **Vale para os dois padrões**, cada um com a sua conferência do gabarito; os itens abaixo que citam a calculadora são do Strategy, e o State tem o item próprio no fim da lista.

- [ ] Casos a partir dos exemplos conferidos do enunciado e da ordem de precedência dos erros.
- [ ] Um script novo, separado (por exemplo `infra/scripts/aceitacao.sh`), que sobe o app de cada `runs/*/workspace` num container sem token, roda os casos e grava `aceitacao.txt` ao lado do `build.txt`.
- [ ] Conferir o gabarito (`ref-strategy.mjs`, a calculadora de onde a suíte tira o esperado) contra o enunciado, à mão e sem IA: **regra por regra** (cada regra do enunciado ↔ a linha que a implementa) e **dois casos de colisão calculados à mão antes de ver a saída** da calculadora. Roteiro e tabela em `avaliacao/aceitacao-prototipo/README.md`, seção "Conferência humana do gabarito". Divergência se resolve pelo texto do enunciado; se o texto não decide, vira inconsistência no §6 do `OBJETIVO.md`.
  - *Trocado em 03/10, a confirmar com o orientador.* Antes: "validar a suíte numa implementação sua, de referência" (escrever o serviço do zero). Motivo: as cinco implementações de Opus e Sonnet já concordam com a calculadora em todos os casos, então um erro do gabarito teria de ser compartilhado por todas; a conferência humana fecha esse risco que sobra a um custo proporcional (cerca de uma hora em vez de um projeto).
- [x] Validar também o outro lado: a suíte **reprova** código errado. Cada mutante de `avaliacao/aceitacao-prototipo/mutantes.mjs` (a calculadora com um erro plantado, um por regra do enunciado que tem armadilha) precisa ser reprovado; o `validar-mutantes.mjs` sai com 0. Feito em 03/10: 15 de 15.
- [x] A unidade é o **caso** (passa ou falha), como a C1 está escrita, e não a verificação de campo. Pontos em que o enunciado se contradiz entram como observação, sem contar (03/10).
- [ ] **State:** conferir, à mão, que cada valor esperado no `state.mjs` é o do enunciado (`experimento/prompt/state.md`): os 8 exemplos e os erros. Aqui o gabarito não é uma calculadora escrita com IA, são os números do próprio enunciado copiados para o teste, então a conferência é só de cópia (cerca de 15 minutos). Antes do lote `STATE`, rever também se o enunciado tem fronteiras ("até", "acima de", "a partir de") sem caso no valor exato, a lição dos mutantes do Strategy.

Como roda sobre os workspaces já salvos, vale para as 36 execuções existentes sem rodar modelo de novo e sem tocar no `executar.sh`.

**Pronto quando:** a conferência humana do gabarito está registrada sem divergência aberta, todos os mutantes são reprovados e a suíte está congelada com hash.

## Parte 3 — `verificar.mjs`

**Objetivo:** todo número do texto do TCC recalculável a partir dos dados brutos. Fica em `infra/scripts/`, no estilo do `agregar.mjs`.

Construir uma checagem por vez, nesta ordem:

| # | Checagem | Pode rodar antes da revelação? |
| --- | --- | --- |
| 1 | códigos da leitura ↔ mapa do lote, um para um | não |
| 2 | `resultados.csv` bate com os `meta.json` | sim |
| 3 | dentro da leitura: contagem de acertos bate com as categorias | sim |
| 4 | desenho completo: 3 réplicas por modelo × condição, pares existentes | sim |
| 5 | imprime os totais por braço e por par, que vão para o texto | não |
| 6 | o `enunciado_hash` do cabeçalho de `avaliacao/<padrao>/gabarito.md` é o `prompt_hash` do `meta.json` de cada execução dos `lotes` do cabeçalho: nenhum pacote lido com gabarito de outro enunciado | sim |

Sai com código 1 e lista os problemas, ou com 0 se estiver tudo coerente.

**Prova de que acusa.** Um verificador que nunca acusa nada parece igual a um que funciona. Para cada checagem, uma cópia dos dados corrompida de propósito (um código trocado no mapa, uma linha a mais no CSV, uma réplica faltando, um hash errado no cabeçalho do gabarito...) tem de fazer o script sair com 1, apontando a checagem certa; os dados verdadeiros têm de sair com 0. É a mesma ideia dos mutantes da Parte 2, e do *proven negative test* do `verify_scores.py` do Akita. As cópias corrompidas ficam numa pasta de teste, geradas por script, nunca à mão sobre os dados reais.

**Limite a declarar na metodologia:** o script garante coerência entre as fontes, não a correção da classificação. Isso fica com a Parte 4.

**Só os lotes da análise.** Roda sobre o EXT (e o STATE, quando rodar), por padrão, recebendo o lote como argumento, como o `--prefix` do `agregar.mjs`. SMOKE, TESTE e BATCH ficam fora: não sustentam nenhum número do texto, e não têm o desenho que as checagens exigem (uma réplica só, outro enunciado, execuções invalidadas de propósito), então falhariam por construção, e um verificador cheio de exceções esconde problema de verdade. A conferência do mapa do piloto foi única, feita na Parte 0.

Em 03/10 conferiu-se à mão, sem ler desfecho, o que as checagens 4 e 6 vão automatizar: as 18 execuções do EXT são 18 combinações distintas (3 réplicas × 3 modelos × 2 braços), todas com o enunciado `b7cdb594…` (o mesmo do cabeçalho do gabarito), a mesma imagem e a mesma versão do Claude Code, e o harness só no braço `HARNESS`.

**Pronto quando:** roda limpo sobre o EXT, e cada cópia corrompida (ou dado sintético) faz sair com 1 na checagem certa.

## Parte 4 — Leitura dupla e kappa

**Objetivo:** mostrar que a classificação não depende de quem leu. É o que responde à orientação de não deixar a IA solta.

- [ ] Refazer a leitura do Claude com a régua congelada, em sessão nova, sem o mapa. As leituras atuais foram feitas antes da régua.
- [ ] Fazer a sua leitura, à mão, dos mesmos pacotes, sem olhar a do Claude.
- [ ] Commitar as duas antes de abrir o mapa.
- [ ] Calcular o kappa de Cohen por ponto de variação.
- [ ] Nas divergências, decidir juntos e registrar o motivo numa coluna `resolucao`, sem apagar a leitura original.

**Pronto quando:** as duas leituras e a tabela de resolução estão commitadas, com o kappa anotado. Um kappa acima de 0,8 costuma ser lido como concordância forte.

### Isolamento da leitura do Claude

A leitura atual confia que o agente não abriu o mapa. O mapa continua em `avaliacao/` e os pacotes são cópias exatas de `runs/*/workspace`, então um agente com acesso ao repositório consegue descobrir o braço. A leitura passa a rodar no mesmo tipo de container da bancada, onde isso fica impossível, e não só evitado.

- [ ] Criar `infra/scripts/ler-cego.sh`, no molde do `executar.sh`: `docker run --rm`, mesma imagem, `claude -p` com `--no-session-persistence`.
- [ ] O script recebe **só o nome do padrão** e monta tudo a partir de `avaliacao/<padrao>/`, para não haver escolha separada de gabarito que possa errar.
- [ ] Montar no container só isto: uma cópia de `avaliacao/<padrao>/pacotes/` (somente leitura), a régua e o `gabarito.md` da mesma pasta, e uma pasta de saída para o CSV. Nada do repositório, nem `runs/`, nem o mapa.
- [ ] A régua entra como prompt, igual ao `prompt.md` na execução.
- [ ] Reaproveitar o preflight do `executar.sh`: registrar que `~/.claude` está vazio e que não existe `CLAUDE.md` fora da pasta montada. Guardar esse log junto do CSV como prova.
- [ ] Não fazer a leitura no app do Claude nem numa sessão do Claude Code no seu computador: os dois podem ter memória sobre o TCC.

Por que isso elimina a memória: o container nasce com `~/.claude` vazio (sem memória, sem `CLAUDE.md` de usuário) e é destruído no fim (`--rm`), então nada de uma leitura sobra para a próxima.

## Parte 5 — Sprint de manutenção (segundo experimento)

**Objetivo:** medir se o design com padrões torna a mudança mais barata, e não só se ele existe. É a ideia central do v4 do Akita e a dimensão "manter software" da orientação.

### Desenho, decidido em 03/10 (consenso Lucas + Claude, a levar ao orientador)

| decisão | escolha | por quê |
|---|---|---|
| padrões | **só Strategy**, por agora | o State ainda não tem lote nem régua calibrada; entra depois, com este mesmo desenho |
| ponto de partida | uma **cópia** do workspace de cada uma das 18 execuções `EXT`, mesmo modelo e mesma condição | reaproveita o build já analisado, sem rodar de novo; o par continua o mesmo |
| sessão | **nova** a cada sprint (`claude -p`, como hoje) | as execuções do EXT rodaram com `--no-session-persistence`, então não há sessão a retomar; e é o realista para "manter software": um dev que pega o código e precisa lê-lo |
| `CLAUDE.md` no braço `HARNESS` | **continua** no workspace | mede o harness como seria usado: presente no build e na manutenção. Conferido em 03/10: os 9 `CLAUDE.md` do EXT estão byte a byte iguais ao original, e nenhum `CONTROL` tem `CLAUDE.md` escrito pelo modelo. Limite a declarar: não separa o efeito do desenho do efeito da orientação |
| mudanças | **quatro sprints em sequência, no mesmo workspace**, uma mudança por sprint, na ordem **P2 → P1 → P4 → P5**, a mesma para todos | um ponto por sprint deixa cada diff limpo; a ordem vai do mais simples ao controle negativo, que mede o exagero sobre o código já mexido pelas outras três, como num projeto real |
| P2 | um cupom novo | ponto positivo simples; provavelmente teto, serve de referência |
| P1 | uma transportadora nova **com limite de peso** (tarifa e prazo próprios, só até X kg) | caso exigente do mesmo tipo do MOTOBOY, dentro de um ponto só: o contrato comporta sem remendo? (D2) |
| P4 | um nível **DIAMANTE**, que mexe em mais de uma regra (crédito, frete, brinde) | o caso mais exigente; é onde o Haiku CONTROL já fez remendo no build |
| P5 | uma **região nova**, só uma porcentagem | controle negativo: uma linha num desenho proporcional, classe + fábrica no que exagerou. Dá preço ao exagero (N1) |
| falha numa sprint | **segue**: a próxima parte do código como ficou, e `build_ok` de cada sprint é dado (C2) | como num projeto real; parar daria vantagem a quem quebra cedo. Só falha de infraestrutura (429, Docker) se refaz, com id novo |
| simultaneidade | as 6 de cada rodada juntas, como hoje | mantém o pareamento |
| tamanho | 4 sprints × 18 = **72 execuções**, em 12 rodadas de 6 | execuções pequenas, mas é cota de assinatura: rodar com a assinatura livre |

### Medidas

- **★ Arquivos tocados além do registro do caso, só em `src/main`**, contados no diff entre a cópia de antes e a de depois de cada sprint (a bancada tira as cópias; o agente não precisa usar git). É a versão medida da propriedade `custo_caso_novo` (§2.6 da régua), e permite conferir se a previsão da régua bate com o custo real. O que é "registro do caso" segue a régua.
- Secundárias: testes tocados ou criados (`src/test`), condicionais adicionados ou editados que nomeiam casos, `build_ok`, tokens e tempo (`meta.json`).
- Correção: a suíte oculta de novo após cada sprint, ampliada com os casos da mudança; a calculadora de referência e os mutantes ganham a regra nova, e a conferência humana do gabarito vale também para as regras novas.

### A fazer

- [ ] Escrever os quatro pedidos de mudança no tom do enunciado, sem palavra de arquitetura, com os valores (tarifas, porcentagens, limites) e exemplos conferidos; cada fronteira nova com caso no valor exato.
- [ ] Estender a bancada para rodar uma sprint sobre uma cópia e tirar as cópias de antes e depois, sem mudar o `executar.sh` congelado (script novo, ou versão nova com hash novo).
- [ ] Hipóteses próprias no `OBJETIVO.md`, com a medida ★ e a regra de leitura por pares, **antes** de rodar.

**Pronto quando:** os quatro pedidos, a extensão da bancada e as hipóteses estão congelados com hash. É a parte maior; pode ficar para depois da análise do primeiro experimento.

## Ordem e dependências

Duas fases, separadas por uma linha: **nenhum dado do experimento é olhado antes de tudo da fase 1 estar congelado.** Rodar a suíte sobre o EXT não gasta tokens, mas já é olhar resultado, por isso fica na fase 2.

### Fase 1: preparar (construir, conferir e congelar, sem olhar dado)

| # | o quê | parte | quem | depende de |
| --- | --- | --- | --- | --- |
| 0 | arrumar a casa | 0 | Claude | — (**feito**, 26/09) |
| a | **congelar o `OBJETIVO.md`**: aprovar as hipóteses (§4) e as regras de leitura (§4.1), trocar o aviso de RASCUNHO por "congelado em DD/MM, com o orientador", commit e hash no README. Daí em diante, mudança no §1 a §4 só como emenda datada e com motivo, sem apagar o original; §5 e §6 continuam sendo atualizados | — | Lucas + orientador | — |
| b | confirmar com o orientador as trocas marcadas "a confirmar" (conferência humana no lugar da referência escrita do zero; desenho da Parte 5) | 2, 5 | Lucas + orientador | — |
| c | conferir o gabarito do Strategy (~1 h) e do State (~15 min) | 2 | Lucas | — |
| d | calibração humana da régua do Strategy (SMOKE e TESTE-P4) e **congelar a régua** | 1 | Lucas | — |
| e | régua do State: ajuste da tabela de transições, calibração sobre a `TESTE-STATE-02` | 1 | Lucas (Claude no ajuste) | d |
| f | `aceitacao.sh` e **congelar a suíte** | 2 | Claude | c |

### Fase 2: medir (só com a fase 1 inteira congelada)

| # | o quê | parte | quem | gasta tokens? |
| --- | --- | --- | --- | --- |
| g | gerar o CSV do EXT (`agregar.mjs --prefix EXT --out analise/resultados-ext.csv`: são os custos, K1 a K4) e construir o `verificar.mjs` com as checagens 2, 4 e 6 e a prova de que acusa | 3 | Claude | não, mas lê dado |
| h | suíte sobre o EXT (C1 a C4) | 2 | Claude | não, mas lê dado |
| i | leitura dupla do EXT, kappa e resolução | 4 | Lucas + Claude | sim |
| j | completar o `verificar.mjs` (checagens 1, 3 e 5, que dependem da leitura), rodar sobre tudo e tirar dele os totais para o texto | 3 | Claude | não |
| k | manutenção: extensão, depois da análise do EXT; precisa de pré-registro próprio (os itens dela voltam à fase 1) | 5 | — | sim, e mais |

O **lote STATE** é execução, não está neste plano (§5 do `OBJETIVO.md`). Roda depois de **e**; depois de rodar, é medido pelas mesmas partes (h, i, j), com a régua e a suíte do State.

A ordem segue corrigir → implementar → testar → rodar. O `verificar.mjs` fica na fase 2 porque ele confere dados, e os que importam (custos, suíte, leituras) só existem ali; construí-lo antes seria fazer uma ferramenta sem material. Com a fase 1 congelada e g a j feitos, o experimento do Strategy fica completo; o State repete g a j sobre o lote dele, e a Parte 5 é extensão.
