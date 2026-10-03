# Plano de implementação — avaliação do TCC

26/09/2026 · Lucas

## Visão geral

São seis partes, e nenhuma mexe na bancada de execução congelada: `executar.sh`, `rodada.sh`, prompt, harness e imagem continuam com os mesmos hashes. Tudo o que entra é instrumento de medida, aplicado depois das execuções.

Regra que vale para todas as partes: um instrumento novo é congelado (commit + hash no README) antes de ser usado em qualquer pacote. É a mesma disciplina que você já usa no pré-registro.

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

**Como:** testes de caixa-preta via HTTP contra `POST /checkout/resumo`, o contrato que o enunciado já define. Caixa-preta porque cada execução tem pacotes e classes diferentes.

- [ ] Casos a partir dos exemplos conferidos do enunciado e da ordem de precedência dos erros.
- [ ] Um script novo, separado (por exemplo `infra/scripts/aceitacao.sh`), que sobe o app de cada `runs/*/workspace` num container sem token, roda os casos e grava `aceitacao.txt` ao lado do `build.txt`.
- [ ] Conferir o gabarito (`ref-strategy.mjs`, a calculadora de onde a suíte tira o esperado) contra o enunciado, à mão e sem IA: **regra por regra** (cada regra do enunciado ↔ a linha que a implementa) e **dois casos de colisão calculados à mão antes de ver a saída** da calculadora. Roteiro e tabela em `avaliacao/aceitacao-prototipo/README.md`, seção "Conferência humana do gabarito". Divergência se resolve pelo texto do enunciado; se o texto não decide, vira inconsistência no §6 do `OBJETIVO.md`.
  - *Trocado em 03/10, a confirmar com o orientador.* Antes: "validar a suíte numa implementação sua, de referência" (escrever o serviço do zero). Motivo: as cinco implementações de Opus e Sonnet já concordam com a calculadora em todos os casos, então um erro do gabarito teria de ser compartilhado por todas; a conferência humana fecha esse risco que sobra a um custo proporcional (cerca de uma hora em vez de um projeto).
- [x] Validar também o outro lado: a suíte **reprova** código errado. Cada mutante de `avaliacao/aceitacao-prototipo/mutantes.mjs` (a calculadora com um erro plantado, um por regra do enunciado que tem armadilha) precisa ser reprovado; o `validar-mutantes.mjs` sai com 0. Feito em 03/10: 15 de 15.
- [x] A unidade é o **caso** (passa ou falha), como a C1 está escrita, e não a verificação de campo. Pontos em que o enunciado se contradiz entram como observação, sem contar (03/10).

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

**Limite a declarar na metodologia:** o script garante coerência entre as fontes, não a correção da classificação. Isso fica com a Parte 4.

**Pronto quando:** roda limpo sobre BATCH e EXT.

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

- [ ] Escrever o pedido de mudança no tom do enunciado, sem palavra de arquitetura. Por exemplo: uma transportadora nova e um nível DIAMANTE.
- [ ] Rodar sobre uma cópia do workspace de cada execução, mesmo modelo e mesma condição, 6 simultâneas como hoje.
- [ ] Medir: arquivos tocados, condicionais adicionados ou editados, testes quebrados, tokens e tempo. Rodar a suíte oculta de novo, ampliada com os casos novos.
- [ ] Pré-registro próprio antes de rodar, porque é outro estímulo.

**Pronto quando:** o pré-registro está congelado. É a parte maior; pode ficar para depois da análise do primeiro experimento.

## Ordem e dependências

| Parte | Tipo | Depende de | Gasta tokens? |
| --- | --- | --- | --- |
| 0 — Arrumar a casa | corrigir | — | não |
| 1 — Régua | implementar | 0 | não |
| 2 — Suíte oculta | implementar | 0 | não |
| 3 — `verificar.mjs` | testar | 0 (no experimento, as checagens 1 e 5 esperam a 4) | não |
| 4 — Leitura dupla | rodar | 1, 3 | sim |
| 5 — Manutenção | rodar | 1, 2, 3 | sim, e mais |

A ordem segue corrigir → implementar → testar → rodar: nada que gaste tokens começa antes de todos os instrumentos estarem prontos e testados. O `verificar.mjs` já confere a Parte 0, por exemplo se o mapa do piloto recriado bate com a leitura dele. Com as Partes 0 a 4 prontas, o experimento fica completo; a 5 é extensão.
