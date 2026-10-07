# Plano de implementação — avaliação do TCC

26/09/2026 · Lucas

## Visão geral

São sete partes (0 a 6), e nenhuma mexe na bancada de execução congelada: `executar.sh`, `rodada.sh`, prompt, harness e imagem continuam com os mesmos hashes. O TCC_V3 é a bancada de testes: o `EXT` serve para validar os instrumentos, e o lote que vale roda numa versão futura (V4), com 5 réplicas. Tudo o que entra é instrumento de medida, aplicado depois das execuções.

Regra que vale para todas as partes: um instrumento novo é congelado (commit + hash no README) antes de ser usado em qualquer pacote. É a mesma disciplina que você já usa no pré-registro.

O plano cuida da **avaliação** e da **preparação do V4** (a escada de níveis e como ela roda, Parte 6). A bancada (o ambiente de execução) já está pronta e congelada, e rodar lotes é execução, acompanhada no §5 do `OBJETIVO.md`. A ordem entre preparar e medir está em "Ordem e dependências", no fim.

## Instruções para o agente

Este plano é executado pelo Claude Code, uma parte por vez. Regras para o agente:

1. **Só implementar o que o Lucas autorizar.** A Parte 0 foi revisada em 26/09. Entre 03 e 06/10, as Partes 2, 3, 5 e 6 foram trabalhadas com autorização dele, passo a passo. A Parte 1 (régua) e a 4 (leitura dupla) seguem em revisão: não mexer sem pedir.
2. **Não alterar nada congelado:** `executar.sh`, `rodada.sh`, `extrair-meta.mjs`, `agregar.mjs`, `anonimizar.mjs`, `experiment/harnesses/` (os níveis N1 a N3, com hash no README), as ferramentas de métricas (`evaluation/tools/`), o conteúdo dos prompts e o `Dockerfile`. Mover um arquivo congelado é permitido; mudar os bytes dele não. Conferir com `sha256sum` antes e depois de cada movimentação.
3. **Não abrir nem imprimir o `.env`.** Não abrir mapa de anonimização, exceto no passo que pede isso.
4. **Parar e perguntar antes de:** apagar qualquer arquivo ou pasta, `git subtree`, `git push`, ou qualquer coisa que reescreva histórico.
5. **Um commit por passo**, em português, no estilo do repositório: título curto e corpo explicando o porquê.
6. **Ao fim de cada passo**, rodar a checagem indicada e mostrar a saída real do comando, não um resumo.
7. **Se uma checagem falhar, parar.** Não tentar contornar.
8. **Toda decisão de projeto entra no `DECISOES.md`**: data, a decisão, o motivo, o que se descartou e onde está o detalhe. Uma decisão que muda ganha entrada nova; a antiga fica.

Este plano está em `PLANO-IMPLEMENTACAO.md`, na raiz do `TCC_V3`. Ficou fora dos commits até 03/10, quando o Lucas decidiu versioná-lo.

## Parte 0 — Arrumar a casa

**Objetivo:** não perder o que já foi feito antes de construir em cima.

**Decisão:** um experimento só, com o prompt de 5 pontos (P1 a P5). O lote `BATCH`, de 3 pontos, vira piloto: foi ele que mostrou o efeito de teto e motivou o prompt estendido.

### Reorganizar as pastas

Rodar dentro de `J:\TCC\TCC_V3`, nesta ordem:

- [x] **1. Commitar as execuções `EXT-01` a `EXT-03`.** Antes, conferir com `git status` que nenhum `target/` nem `.env` entra.
- [x] **2. Proteger os arquivos antigos.** Acrescentar `history/** -text` ao `.gitattributes` (sem isso o git pode mudar o fim de linha do prompt antigo, e o hash junto) e `history/pilot/packages/` ao `.gitignore`.
- [x] **3. Prompt único.** `git mv experiment/prompt/prompt.md history/pilot/prompt.md` e depois `git mv experiment/prompt/prompt-estendido.md experiment/prompt/prompt.md`. Checagem: o `sha256sum` de `experiment/prompt/prompt.md` começa com `b7cdb594cb49efee` e o de `history/pilot/prompt.md` com `53db3424b3972795`.
- [x] **4. Recriar o mapa do piloto.**
    1. Guardar o mapa atual: `mv evaluation/mapa-anonimizacao.csv evaluation/mapa-anonimizacao.guardado.csv`.
    2. `node evaluation/tools/anonimizar.mjs $(ls runs | grep '^BATCH-' | LC_ALL=C sort) --seed 24`.
    3. Checagem: os 18 códigos do mapa gerado são exatamente os 18 da `evaluation/leitura-claude-cego.csv`. Se não forem, parar.
    4. Mover o mapa gerado para `history/pilot/mapa-anonimizacao.csv`, as 18 pastas geradas em `evaluation/packages/` para `history/pilot/packages/`, e `git mv evaluation/leitura-claude-cego.csv history/pilot/`.
    5. Restaurar: `mv evaluation/mapa-anonimizacao.guardado.csv evaluation/mapa-anonimizacao.csv`.
- [x] **5. Commitar os mapas.** `git add -f evaluation/mapa-anonimizacao.csv` (está no `.gitignore`) e `git add history/`.
- [x] **6. Atualizar a documentação.** No `README.md` e no `experiment/prompt/README.md`: caminhos novos, tabela de hashes, e a decisão (um experimento; `BATCH` = piloto).

As execuções ficam em `runs/`; o prefixo `BATCH` ou `EXT` no nome já diz de qual são.

### Subir no repositório geral sem perder o histórico

`J:\TCC` é o repositório `FLucasF/TCC` (público), e o `TCC_V3` tem `.git` próprio dentro dele. Um `git add TCC_V3` simples só sobe uma referência vazia. O `git subtree add` traz os arquivos e os commits, com os mesmos identificadores, então referências como `5c9c37d` continuam valendo.

Rodar com o Claude Code aberto em `J:\TCC`, não dentro do `TCC_V3`, porque um passo move essa pasta:

1. Conferir que o `TCC_V3` está com `git status` limpo depois dos passos 1 a 6 acima. O subtree só leva o que está commitado.
2. Commitar as alterações pendentes do repositório geral. O subtree exige a árvore limpa.
3. **Perguntar antes.** Mover `J:\TCC\TCC_V3` para `J:\TCC_V3_temp`.
4. **Perguntar antes.** Em `J:\TCC`: `git subtree add --prefix=TCC_V3 J:/TCC_V3_temp master`.
5. Copiar de volta o que o git ignora e o experimento usa: `.env`, `evaluation/packages/` e `history/pilot/packages/`. Sem abrir o `.env`.
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

**Objetivo:** transformar "o padrão foi aplicado?" numa classificação que duas pessoas aplicam e chegam no mesmo resultado. Medir as hipóteses de desenho do `OBJETIVO.md` (as de Desenho e as três primeiras de Exagero) e servir a qualquer padrão, não só ao Strategy: o Strategy é o primeiro.

**Formato: três níveis.** Só o terceiro é escrito de novo a cada enunciado.

| nível | vale para | o que diz |
|---|---|---|
| **1. Propriedades** | todo padrão, todo enunciado | o que se observa em cada ponto de variação, em termos de código: onde mora cada caso, como o caso é escolhido, se a assinatura comporta o caso mais exigente, se a parte comum se repete, quantos lugares se editam para um caso novo, e se a estrutura é proporcional à variação |
| **2. Ficha do padrão** | todo enunciado daquele padrão | quais propriedades contam como **acerto** e quais como **exagero**. Primeira ficha: Strategy |
| **3. Gabarito do enunciado** | só aquele enunciado | a lista dos pontos, os identificadores dos casos de cada um, qual é positivo, qual é controle negativo, qual tem caso mais exigente |

**Entrega:** `evaluation/regua.md` (níveis 1 e 2) e `evaluation/strategy/gabarito.md` (nível 3 do enunciado atual, na mesma pasta dos pacotes que lê, com cabeçalho de padrão, enunciado, hash e lotes), com:

1. Cada propriedade com seus valores possíveis e a regra observável de cada valor.
2. A forma encontrada (`classes`, `enum-abstrato`, `enum-dados`, `switch`…) registrada como **descrição**, não como veredito.
3. Evidência obrigatória em cada célula (`arquivo:linha`). Sem evidência, o valor é `indeterminado`.
4. A planilha em formato longo, uma linha por pacote × ponto, para o kappa sair por propriedade e para um padrão novo não mudar as colunas.
5. Exemplos resolvidos de pacotes fora da análise: os **SMOKE** (enunciado de três pontos, cobrem P1 a P3) e os **TESTE-P4** (enunciado atual, cobrem P4 e P5).

**Pronto quando:** aplicada aos pacotes SMOKE e TESTE-P4 sem nenhum caso em que você hesite entre dois valores. Cada hesitação vira uma regra escrita antes de fechar. Depois, commit e hash no README.

## Parte 2 — Suíte oculta de aceitação

**Objetivo:** saber se o código calcula certo. Hoje o `mvn verify` só roda os testes que o próprio modelo escreveu.

**Como:** testes de caixa-preta via HTTP contra o contrato que cada enunciado já define: `POST /checkout/resumo` no Strategy (`strategy.mjs`). Caixa-preta porque cada execução tem pacotes e classes diferentes. O State tinha a sua (`state.mjs`), que foi para `history/state/` quando ele saiu do V4 (07/10).

- [ ] Casos a partir dos exemplos conferidos do enunciado e da ordem de precedência dos erros.
- [x] **Feito em 07/10: `infra/scripts/acceptance.sh`** (nome e variáveis em inglês, a pedido do Lucas). Um script novo, separado, que sobe o app de cada `runs/*/workspace` num container sem token, roda os casos e grava `aceitacao.txt` ao lado do `build.txt`.
- [ ] Verificar o gabarito (`ref-strategy.mjs`, a calculadora de onde a suíte tira o esperado) em duas frentes. Roteiro em `evaluation/acceptance-prototype/README.md`, seção "Como se sabe que a suíte mede certo".
  - **A aritmética, por implementações independentes:** a suíte roda sobre o que o teste de bancada do V4 (f4) produzir, e as implementações de Opus e Sonnet passam em tudo. Por isso o f4 inclui ao menos um quarteto de Sonnet ou de Opus.
  - **As leituras do enunciado, pelo Lucas, sem IA:** os 5 pontos em que o texto admite mais de uma leitura e a calculadora escolheu uma. Divergência se resolve pelo texto; se o texto não decide, vira inconsistência no §6 do `OBJETIVO.md`. **Feita em 07/10, sem divergência.**
  - Os números dos exemplos 1 a 4 e da resposta do anexo, no enunciado do V4, saíram da calculadora; se uma das frentes acusar erro, eles são recalculados antes de o V4 rodar.
  - *Trocado duas vezes, a confirmar com o orientador.* Em 03/10: a implementação de referência escrita do zero virou uma conferência humana (dois casos calculados à mão e as regras uma a uma). Em 07/10: a conferência virou as duas frentes acima. Motivo: refazer a conta à mão repete o que as implementações independentes já verificam; o risco que sobra é de leitura, e é ele que a revisão do Lucas cobre, em cerca de 15 minutos.
- [x] Validar também o outro lado: a suíte **reprova** código errado. Cada mutante de `evaluation/acceptance-prototype/mutantes.mjs` (a calculadora com um erro plantado, um por regra do enunciado que tem armadilha) precisa ser reprovado; o `validar-mutantes.mjs` sai com 0. Feito em 03/10: 15 de 15. Refeito em 06/10 para o enunciado do V4: 21 casos, 17 de 17 (o mutante 7 voltou e entrou o 17, a fronteira do boleto). Refeito em 07/10, com o seguro no lugar do imposto: 17 de 17.
- [x] A unidade é o **caso** (passa ou falha), como a hipótese da correção está escrita, e não a verificação de campo. Pontos em que o enunciado se contradiz entram como observação, sem contar (03/10). No enunciado do V4 as contradições foram corrigidas, e a suíte não tem mais observações (06/10).
- ~~**State:** conferir a cópia dos valores do `state.mjs` e as fronteiras sem caso~~. Saiu com o State (07/10); o passo ficou anotado em `history/state/README.md`, para quando ele voltar.

Como roda sobre os workspaces já salvos, vale para as 36 execuções existentes sem rodar modelo de novo e sem tocar no `executar.sh`.

**Pronto quando:** as duas frentes da verificação do gabarito estão registradas sem divergência aberta, todos os mutantes são reprovados e a suíte está congelada com hash.

## Parte 3 — `verificar.mjs`

**Objetivo:** todo número do texto do TCC recalculável a partir dos dados brutos. Fica em `infra/scripts/`, no estilo do `agregar.mjs`.

**Versão enxuta (decidida em 07/10):** ficam as checagens 1, 2, 4 e 6, as que acusam um defeito que nenhuma outra peça acusa. A 3 e a 5 saem: a contagem de acertos e os totais para o texto saem direto dos CSVs (o do `agregar.mjs`, o do `acceptance.sh` e o da leitura), sem um segundo cálculo para manter.

Construir uma checagem por vez, nesta ordem:

| # | Checagem | Pode rodar antes da revelação? |
| --- | --- | --- |
| 1 | códigos da leitura ↔ mapa do lote, um para um | não |
| 2 | `resultados.csv` bate com os `meta.json` | sim |
| ~~3~~ | ~~dentro da leitura: contagem de acertos bate com as categorias~~ (saiu, 07/10) | — |
| 4 | desenho completo: todas as réplicas de cada modelo × nível (3 × 2 no EXT; 5 × 4 no V4), e os pares ou quartetos simultâneos existentes | sim |
| ~~5~~ | ~~imprime os totais por braço e por par, que vão para o texto~~ (saiu, 07/10: os totais saem dos CSVs) | — |
| 6 | o `enunciado_hash` do cabeçalho de `evaluation/<padrao>/gabarito.md` é o `prompt_hash` do `meta.json` de cada execução dos `lotes` do cabeçalho: nenhum pacote lido com gabarito de outro enunciado | sim |

Sai com código 1 e lista os problemas, ou com 0 se estiver tudo coerente.

**Prova de que acusa.** Um verificador que nunca acusa nada parece igual a um que funciona. Para cada checagem, uma cópia dos dados corrompida de propósito (um código trocado no mapa, uma linha a mais no CSV, uma réplica faltando, um hash errado no cabeçalho do gabarito...) tem de fazer o script sair com 1, apontando a checagem certa; os dados verdadeiros têm de sair com 0. É a mesma ideia dos mutantes da Parte 2, e do *proven negative test* do `verify_scores.py` do Akita. As cópias corrompidas ficam numa pasta de teste, geradas por script, nunca à mão sobre os dados reais.

**Limite a declarar na metodologia:** o script garante coerência entre as fontes, não a correção da classificação. Isso fica com a Parte 4.

**Só os lotes da análise.** Roda sobre os lotes que valem, os do V4 (o EXT serve de ensaio), por padrão, recebendo o lote como argumento, como o `--prefix` do `agregar.mjs`. SMOKE, TESTE e BATCH ficam fora: não sustentam nenhum número do texto, e não têm o desenho que as checagens exigem (uma réplica só, outro enunciado, execuções invalidadas de propósito), então falhariam por construção, e um verificador cheio de exceções esconde problema de verdade. A conferência do mapa do piloto foi única, feita na Parte 0.

Em 03/10 conferiu-se à mão, sem ler desfecho, o que as checagens 4 e 6 vão automatizar: as 18 execuções do EXT são 18 combinações distintas (3 réplicas × 3 modelos × 2 braços), todas com o enunciado `b7cdb594…` (o mesmo do cabeçalho do gabarito), a mesma imagem e a mesma versão do Claude Code, e o harness só no braço `HARNESS`.

**Pronto quando:** roda limpo sobre o EXT (o ensaio), e cada cópia corrompida (ou dado sintético) faz sair com 1 na checagem certa; depois, sobre os lotes do V4.

## Parte 4 — Leitura dupla e kappa

**Objetivo:** mostrar que a classificação não depende de quem leu. É o que responde à orientação de não deixar a IA solta.

- [ ] Refazer a leitura do Claude com a régua congelada, em sessão nova, sem o mapa. As leituras atuais foram feitas antes da régua.
- [ ] Fazer a sua leitura, à mão, dos mesmos pacotes, sem olhar a do Claude.
- [ ] Commitar as duas antes de abrir o mapa.
- [ ] Calcular o kappa de Cohen por ponto de variação.
- [ ] Nas divergências, decidir juntos e registrar o motivo numa coluna `resolucao`, sem apagar a leitura original.

**Pronto quando:** as duas leituras e a tabela de resolução estão commitadas, com o kappa anotado. Um kappa acima de 0,8 costuma ser lido como concordância forte.

### Isolamento da leitura do Claude

A leitura atual confia que o agente não abriu o mapa. O mapa continua em `evaluation/` e os pacotes são cópias exatas de `runs/*/workspace`, então um agente com acesso ao repositório consegue descobrir o braço. A leitura passa a rodar no mesmo tipo de container da bancada, onde isso fica impossível, e não só evitado.

- [ ] Criar `infra/scripts/ler-cego.sh`, no molde do `executar.sh`: `docker run --rm`, mesma imagem, `claude -p` com `--no-session-persistence`.
- [ ] O script recebe **só o nome do padrão** e monta tudo a partir de `evaluation/<padrao>/`, para não haver escolha separada de gabarito que possa errar.
- [ ] Montar no container só isto: uma cópia de `evaluation/<padrao>/packages/` (somente leitura), a régua e o `gabarito.md` da mesma pasta, e uma pasta de saída para o CSV. Nada do repositório, nem `runs/`, nem o mapa.
- [ ] A régua entra como prompt, igual ao `prompt.md` na execução.
- [ ] Reaproveitar o preflight do `executar.sh`: registrar que `~/.claude` está vazio e que não existe `CLAUDE.md` fora da pasta montada. Guardar esse log junto do CSV como prova.
- [ ] Não fazer a leitura no app do Claude nem numa sessão do Claude Code no seu computador: os dois podem ter memória sobre o TCC.

Por que isso elimina a memória: o container nasce com `~/.claude` vazio (sem memória, sem `CLAUDE.md` de usuário) e é destruído no fim (`--rm`), então nada de uma leitura sobra para a próxima.

## Parte 5 — Sprint de manutenção (segundo experimento)

**Objetivo:** medir se o design com padrões torna a mudança mais barata, e não só se ele existe. É a ideia central do v4 do Akita e a dimensão "manter software" da orientação.

### Desenho, decidido em 03/10 (consenso Lucas + Claude, a levar ao orientador)

| decisão | escolha | por quê |
|---|---|---|
| padrões | **só Strategy** | o State saiu do V4 em 07/10 (`history/state/`); se voltar, entra com este mesmo desenho |
| ponto de partida | uma **cópia** do workspace de cada uma das 18 execuções `EXT`, mesmo modelo e mesma condição | reaproveita o build já analisado, sem rodar de novo; o par continua o mesmo |
| sessão | **nova** a cada sprint (`claude -p`, como hoje) | as execuções do EXT rodaram com `--no-session-persistence`, então não há sessão a retomar; e é o realista para "manter software": um dev que pega o código e precisa lê-lo |
| `CLAUDE.md` no braço `HARNESS` | **continua** no workspace | mede o harness como seria usado: presente no build e na manutenção. Conferido em 03/10: os 9 `CLAUDE.md` do EXT estão byte a byte iguais ao original, e nenhum `CONTROL` tem `CLAUDE.md` escrito pelo modelo. Limite a declarar: não separa o efeito do desenho do efeito da orientação |
| mudanças | **quatro sprints em sequência, no mesmo workspace**, uma mudança por sprint, na ordem **P2 → P1 → P4 → P5**, a mesma para todos | um ponto por sprint deixa cada diff limpo; a ordem vai do mais simples ao controle negativo, que mede o exagero sobre o código já mexido pelas outras três, como num projeto real |
| P2 | um cupom novo | ponto positivo simples; provavelmente teto, serve de referência |
| P1 | uma transportadora nova **com limite de peso** (tarifa e prazo próprios, só até X kg) | caso exigente do mesmo tipo do MOTOBOY, dentro de um ponto só: o contrato comporta sem remendo? (*Desenho: comporta o caso exigente*) |
| P4 | um nível **DIAMANTE**, que mexe em mais de uma regra (crédito, frete, brinde) | o caso mais exigente; é onde o Haiku CONTROL já fez remendo no build |
| P5 | uma **região nova**, só uma porcentagem | controle negativo: uma linha num desenho proporcional, classe + fábrica no que exagerou. Dá preço ao exagero (a hipótese do exagero) |
| falha numa sprint | **segue**: a próxima parte do código como ficou, e `build_ok` de cada sprint é dado (*Correção: não quebra o build*) | como num projeto real; parar daria vantagem a quem quebra cedo. Só falha de infraestrutura (429, Docker) se refaz, com id novo |
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

**A revisar com o V4.** Este desenho foi decidido sobre o EXT (3 réplicas, dois braços). Com o V4 (5 réplicas, quatro níveis, Parte 6), o ponto de partida passa a ser o lote que vale, e o tamanho muda; revisar antes de pré-registrar.

## Parte 6 — Escada de níveis e o desenho do V4

**Objetivo:** responder ao título do TCC ("quanto harness"), comparando níveis que acumulam, como na proposta do orientador (12/09).

### Os níveis (montados e testados em 06/10)

| nível | o que acrescenta | estado |
|---|---|---|
| N0 | nada (o braço `CONTROL`) | pronto |
| N1 | o `CLAUDE.md` com 4 regras | pronto, hash `560577922737dbb9` |
| N2 | o N1 + a skill pública `gof-patterns` | pronto, hash `27987df0bd1febe2` |
| N3 | o N2 + processo (desenho antes do código, revisor) | pronto, hash `5f4c492bf3d12f68` |
| N4 | verificação automática | **descartado**: o agente já se verifica sozinho (62 de 62) |

O conteúdo de cada nível, os motivos do descarte e da troca de número entre N3 e N4, e o teste de bancada estão em `experiment/harnesses/README.md`.

### Como roda no V4: os quatro níveis juntos (decidido em 06/10)

Para cada modelo, N0, N1, N2 e N3 rodam **ao mesmo tempo**, um quarteto simultâneo no lugar do par simultâneo de hoje. Todos os níveis se comparam em pares, sem repetir a `CONTROL` por lote, e a pergunta de tendência (a qualidade sobe de N0 a N3?) fica possível. Tamanho: 4 níveis × 3 modelos × 5 réplicas = **60 execuções**, só o Strategy (contra 90 com um lote por nível). Eram 120 com o State, que saiu em 07/10 (ver `history/state/README.md`).

**Consumo esperado**, pelos `meta.json` de 50 execuções de teste (custo-equivalente de API, que a assinatura não cobra, mas mede o peso): Haiku ~US$ 0,55 e ~7 min por execução; Sonnet ~US$ 1,03 e ~7 min; Opus ~US$ 1,74 e ~8 min, com caudas de até 39 min. Um quarteto dos três modelos (12 execuções) ~US$ 13; o V4 inteiro (60 execuções) ~US$ 66. Quantas execuções cabem numa janela de 5 h depende do plano da assinatura; a concorrência (os 3 modelos juntos ou um modelo por vez) é decidida na hora de rodar.

### A fazer

- [ ] `infra/scripts/run-levels.sh`: roda os quatro níveis de um modelo em paralelo, chamando o `executar.sh` congelado (sem mudá-lo), com a mesma checagem prévia do `rodada.sh`. **Montado em 06/10**, com as recusas testadas sem gastar cota (argumentos inválidos, nível ausente, execução já existente: nada é lançado). Falta o teste real e o hash no README.
- [x] Regras de leitura do §4.1 refeitas para **5 réplicas** e para **pares entre níveis** (N1 × N0, N2 × N1, N3 × N2), e a tendência de N0 a N3 (06/10). As principais seguem em N1 × N0; a tendência é informação (teste de Page), não critério. Os limites mantêm a proporção dos de 3 réplicas e ficam para o Lucas revisar.
- [ ] Plano de cota: em quantos dias, e em que ordem, as 60 execuções rodam, sempre com a assinatura livre.
- [x] Enunciados do V4 (06/10): nos dois, o cliente que **entende o básico** e montou a parte técnica pesquisando, no lugar do "desenvolvedor do site" e do "time técnico"; no do Strategy, as três inconsistências do §6 corrigidas (exemplos 1 a 4 com clube e região; resposta do anexo; boleto sobre o total do pedido) e, em 07/10, o imposto por região trocado por **seguro por região**, com a mesma forma (o P5 continua sendo o controle negativo). Hashes: `prompt.md` `d798c11402a52f8c` e, antes de o State sair, `state.md` `4591f7425e1551fd` (hoje em `history/state/`); os que rodaram na bancada estão em `history/prompt-v3/`.
- [ ] Ao montar o V4: o cabeçalho do gabarito do Strategy passa para o enunciado novo e os lotes do V4 (hoje aponta o do `EXT`, em `history/prompt-v3/`), e o P5 dele passa a falar do seguro.
- [ ] No teste real do `run-levels.sh` (f4), incluir ao menos um quarteto de Sonnet ou de Opus, e rodar a suíte sobre ele: é a verificação da aritmética da calculadora do V4 por implementações independentes.

**Pronto quando:** o script de quatro níveis e as regras novas estão congelados com hash, e o OBJETIVO pode ser congelado com eles.

## Ordem e dependências

Duas fases, separadas por uma linha: **nenhum dado do experimento é olhado antes de tudo da fase 1 estar congelado.** Rodar a suíte sobre o EXT não gasta tokens, mas já é olhar resultado, por isso fica na fase 2.

### Fase 1: preparar (construir, conferir e congelar, sem olhar dado)

| # | o quê | parte | quem | depende de |
| --- | --- | --- | --- | --- |
| 0 | arrumar a casa | 0 | Claude | — (**feito**, 26/09) |
| a | (depois de **f5**) **congelar o `OBJETIVO.md`**: aprovar as hipóteses (§4) e as regras de leitura (§4.1), trocar o aviso de RASCUNHO por "congelado em DD/MM, com o orientador", commit e hash no README. Daí em diante, mudança no §1 a §4 só como emenda datada e com motivo, sem apagar o original; §5 e §6 continuam sendo atualizados | — | Lucas + orientador | — |
| b | confirmar com o orientador as trocas: (1) a verificação do gabarito em duas frentes no lugar da referência escrita do zero; (2) a escada N0 a N3, com a troca do N3 pelo N4 e o descarte da verificação automática; (3) o enunciado do V4 (cliente que entende o básico, inconsistências corrigidas, imposto virou seguro); (4) o V4 só com o Strategy, sem o State; (5) o desenho da Parte 5 | 2, 5, 6 | Lucas + orientador | — |
| c | verificar o gabarito do Strategy: as leituras, pelo Lucas (**feito**, 07/10, sem divergência), e a aritmética, por implementações independentes no f4 | 2 | Lucas; f4 | — |
| d | calibração humana da régua do Strategy (SMOKE e TESTE-P4) e **congelar a régua** | 1 | Lucas | — |
| e | ~~régua do State~~: saiu com o State (07/10) | — | — | — |
| f | `acceptance.sh` (**feito**, 07/10) e **congelar a suíte** | 2 | Claude | c (feito); o congelamento espera a verificação da aritmética no f4 |
| f2 | **métricas automáticas** (CK + SonarQube, secundárias): montadas, testadas e congeladas com hash em 06/10 — ver `evaluation/tools/README.md` | — | Claude | — (**feito**, 06/10) |
| f3 | **níveis N0 a N3** montados, com hash, e testados na bancada | 6 | Claude + Lucas | — (**feito**, 06/10) |
| f4 | `run-levels.sh` (os quatro níveis de um modelo em paralelo) e **congelar** | 6 | Claude | f3 (montado e com as recusas testadas em 06/10; falta o teste real, que gasta cota, e o hash no README) |
| f5 | regras do §4.1 para 5 réplicas e pares entre níveis, no OBJETIVO | 6 | Claude, revisão do Lucas | — (**feito**: escrito em 06/10, com as principais em N1 × N0 e a tendência pelo teste de Page como informação; limites revisados pelo Lucas em 07/10: o "não piora" pelo saldo, o "altera" contínuo em 12 de 15 com faixa inconclusiva, o sim/não em 5 pares) |

### Fase 2: medir (só com a fase 1 inteira congelada)

| # | o quê | parte | quem | gasta tokens? |
| --- | --- | --- | --- | --- |
| g | gerar o CSV do lote (`agregar.mjs --prefix <lote> --out analysis/resultados-<lote>.csv`: são as hipóteses de custo) e construir o `verificar.mjs` com as checagens 2, 4 e 6 e a prova de que acusa | 3 | Claude | não, mas lê dado |
| h | suíte sobre o lote (as hipóteses de correção) | 2 | Claude | não, mas lê dado |
| h2 | métricas automáticas sobre o lote (`evaluation/tools/metricas.sh <prefixo>`) | — | Claude | não, mas lê dado |
| i | leitura dupla do lote, kappa e resolução | 4 | Lucas + Claude | sim |
| j | completar o `verificar.mjs` (a checagem 1, que depende da leitura) e rodar sobre tudo; os totais para o texto saem dos CSVs | 3 | Claude | não |
| k | manutenção: extensão, depois da análise do V4; precisa de pré-registro próprio (os itens dela voltam à fase 1) | 5 | — | sim, e mais |

**O lote do V4** (o Strategy, com os quatro níveis) é execução e roda só com a fase 1 inteira congelada. Ele é medido pelas etapas g a j, com a régua e a suíte do Strategy. O EXT continua sendo o ensaio: tudo da fase 2 pode ser testado nele antes.

A ordem segue corrigir → implementar → testar → rodar. O `verificar.mjs` fica na fase 2 porque ele confere dados, e os que importam (custos, suíte, leituras) só existem ali; construí-lo antes seria fazer uma ferramenta sem material. Com a fase 1 congelada, o V4 rodado e g a j feitos sobre os dois lotes, o experimento fica completo; a Parte 5 é extensão.
