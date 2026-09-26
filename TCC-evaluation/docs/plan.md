# Plano — Experimento harness × modelo, fase 1: construir do zero

## Contexto

Feedback do orientador sobre o piloto de 12/09/2026. A matriz de pesquisa cruza
duas fases (construir / manter software), quatro dimensões (arquitetura, design
de baixo nível = padrões de projeto, testes, banco de dados), duas condições
(com / sem harness) e o modelo (Opus / Sonnet). Pedidos: projeto mais completo,
manutenção desse projeto medindo gasto, e harness mais completo e realista.

Pergunta central: **para que usar harness?** O que ele influencia e o que não;
o que o modelo já resolve sozinho; se Opus e Sonnet respondem diferente.

**Escopo deste plano:** fase 1 — construir do zero uma aplicação full-stack,
avaliando **design de baixo nível** primeiro. As apps ficam arquivadas para as
avaliações seguintes (testes, banco, arquitetura) e para a fase de manutenção,
sem rodar de novo.

## Lições do piloto que viram requisito

1. **Isolamento.** O piloto não vazou (zero memória, nenhuma run viu outra,
   contexto inicial A/B idêntico exceto a regra nos 10 pares), mas havia estado
   mutável compartilhado (`~/.m2`) e a app nova tem banco e Docker.
2. **Cota.** Rodar modelo por modelo deixou o Sonnet com n=3 quando a janela
   acabou.
3. **Headless.** Regras "pare e pergunte" e permissões `ask` não funcionam com
   `claude -p`.
4. **Métricas.** Grep de `switch` erra acoplamento; tempo de API ≈ 96% da
   duração; cache de prefixo não é aprendizado; `tool_bash` ≠ builds; pom pode
   estar aninhado; transcrição tem de ser achada por `session_id`.
5. **Avaliação.** A rubrica cega do piloto nunca foi pontuada — tudo que avalia
   precisa existir e ser testado antes de rodar.
6. **Teto.** Tarefa simples: sem harness já tirou 5/6.

## Decisões

| # | Decisão |
|---|---|
| D1 | Harness **novo**. O de `J:\TCC\harness` foi material de estudo e não entra. |
| D2 | Conteúdo do harness de **fontes externas citadas**, referência ao lado de cada regra. |
| D3 | Camadas: **CLAUDE.md + skills + hooks determinísticos** (formatador, lint, compilação). Sem subagente. |
| D3a | Permissões fora do harness: iguais nos dois braços; `--dangerously-skip-permissions` dentro de container descartável. |
| D4 | Stack: **Java 21 / Spring Boot 3 + React TypeScript + PostgreSQL (Flyway)**, Docker Compose. |
| D5 | Execução em **blocos por repetição** (1 bloco = par Opus + par Sonnet), retomada quando a cota reabre. |
| D6 | Domínio: **reservas de espaços de coworking**. |
| D7 | Tamanho médio (~6–8 entidades, 15–20 endpoints, 4–5 telas), **6 pontos de pressão pré-registrados**. |
| D8 | Design avaliado por **triangulação calibrada**: juiz LLM às cegas + amostra cega do usuário (kappa) + métricas estáticas. |
| D9 | **Pausa "começar a próxima?" a cada par A‖B**, mostrando só dados operacionais, nunca A vs B. |
| D10 | Alvo fixado: **5 por célula, Opus + Sonnet = 20 runs**. Extensão só do Sonnet, declarada. |
| D11 | Prompt em **nível de produto**, sem rotas, campos, classes, arquitetura ou padrões; idêntico nos braços. Harness orienta design em tom geral, **nunca por cenário**. |
| D12 | Juiz **Opus**, modelo num parâmetro único (`JUIZ_MODELO`). |
| D13 | Cumprimento funcional por **agente verificador Playwright com roteiro fixo**; usuário confere as 6 apps da calibração. |
| D14 | Harness cobre **as 4 dimensões**; avaliação de **design primeiro**, demais depois sobre as mesmas apps. |
| D15 | **Imagem base congelada** por digest; container novo por run, destruído no fim. |
| D16 | Concluída = agente encerra **e** runner verifica build, `compose up`, healthcheck, front. **Limite 3h**; estouro é resultado. |
| D17 | **Docker próprio por run (DinD)**, rede própria, imagem do Postgres pré-carregada. |
| D18 | Desfecho primário: **nota da rubrica 0–12** (0/1/2 × 6 pontos). Excesso de engenharia separado; resto secundário com Holm. |
| D19 | **Rodada de medição** (1 par Opus + 1 par Sonnet) antes do lote, **fora do conjunto**; pipeline congela depois. |
| D20 | Fontes: **docs Claude Code + 2–3 harnesses públicos adotados + guias** (Spring Boot, Google Java Style, React TS, Flyway/PostgreSQL, OWASP). Congelado por hash. |

Padrão técnico sem consulta: ordem dos modelos **alterna por bloco** (ímpar:
Opus primeiro; par: Sonnet primeiro) para contrabalançar horário.

## Perguntas de pesquisa

- **RQ1 (primária).** O harness muda a nota de design de baixo nível (0–12)?
  Testada separadamente em Opus e em Sonnet.
- **RQ2.** O harness muda o custo — turnos, tokens de saída, tempo de API?
- **RQ3.** O efeito difere entre modelos (interação harness × modelo)?
- **RQ4.** O que o modelo já resolve sozinho: pontos de pressão com nota 2 no
  braço sem harness.
- Posteriores, mesmas apps: testes, banco, arquitetura; depois manutenção.

Testes bilaterais. O sinal do piloto (Opus: harness reduziu custo e cortou
testes pela metade) fica como hipótese exploratória, não direcional.

## Desenho experimental

- Fatorial 2 × 2 (harness × modelo), n = 5 por célula → **20 runs**, mais 4 de
  medição descartadas.
- Unidade de execução: **par A‖B** simultâneo (mesmo modelo e repetição).
- Bloco = par Opus + par Sonnet. 5 blocos. Pausa após cada par.
- Retomada: o registro diz o que já terminou; par interrompido por 429 é
  marcado incompleto e refeito inteiro.

## Aplicação-alvo: reservas de coworking

Arquivos entregues ao agente, **idênticos nos dois braços**:

- `requisitos.md` — histórias e critérios de aceitação em linguagem de negócio:
  espaços (salas, mesas), planos de cliente, reserva por hora, cancelamento,
  notificações, importação de calendário, fila de espera, painel administrativo.
  Completo o bastante para não precisar perguntar.
- `prompt.txt` — curto: implementar o produto de `requisitos.md`, subir com um
  comando, entregar testes; "se algo não estiver especificado, decida e siga".

**Pontos de pressão** (`avaliacao/pontos-de-pressao.md`, nunca dado ao agente):

| | Requisito | Pressão estrutural esperada |
|---|---|---|
| P1 | Ciclo de vida da reserva: pendente, confirmada, em uso, concluída, cancelada, no-show | estado |
| P2 | Preço por plano e faixa horária | strategy |
| P3 | Multa de cancelamento por plano e antecedência | política intercambiável |
| P4 | Notificação por evento no canal preferido (e-mail, SMS, push), credencial e formato próprios | observer + factory |
| P5 | Importação de calendário externo em dois formatos | adapter |
| P6 | Fila de espera acionada por cancelamento | reação a evento |

**Checagem anti-vazamento** por script: `requisitos.md` e `prompt.txt` sem
termos de solução (padrão, strategy, observer, extensível, desacoplado, SOLID,
interface, camada, arquitetura…); harness sem termos do domínio (reserva, plano,
preço, notificação, fila…).

## Harness (braço B)

- `CLAUDE.md` — regras sempre ativas nas 4 dimensões. Design em tom geral
  ("aplique padrões de projeto quando julgar necessário").
- `.claude/skills/` — design de baixo nível, testes, banco e migrações,
  arquitetura do back, front React TS.
- `.claude/settings.json` — hooks `PostToolUse` em Edit/Write: formatador
  (Spotless / Prettier), lint (Checkstyle / ESLint), compilação.
- `FONTES.md` — de onde veio cada regra; critério de seleção dos harnesses
  públicos declarado (adoção e manutenção recente).
- Congelado: hash sha256 da árvore gravado em `HASH`, conferido no preflight e
  registrado em cada run.

Braço A: nenhum CLAUDE.md, skill ou hook. **As ferramentas dos hooks ficam na
imagem base para os dois braços** — A pode usá-las se decidir; o que muda é
só o harness.

## Isolamento e execução

**Imagem base** (`infra/Dockerfile.base`, fixada por digest): JDK 21, Maven,
Node, Claude Code CLI em versão fixa, Checkstyle, ESLint, Prettier, cache de
dependências comuns (Spring Boot starters, React/Vite), imagem `postgres` salva
para `docker load` no DinD.

**Por run:** rede Docker própria → container DinD privilegiado → container da
run com workspace vazio + `prompt.txt` + `requisitos.md` (+ harness só em B);
credencial copiada só leitura; `CLAUDE_CONFIG_DIR` novo;
`CLAUDE_CODE_DISABLE_AUTO_MEMORY=1`; nenhum volume compartilhado. No fim:
workspace com `git commit` do estado final, transcrição e `_result.json`
arquivados; containers, rede e daemon destruídos.

**Runner** (`runner/run-lote.sh`, evolução do `run-pilot.sh`): preflight de
CLI, provedor, auth, digest da imagem e hash do harness; modo par; ordem
alternada por bloco; heartbeat; limite 3h; detecção de 429 com o horário de
reset; pausa por par com custo, tokens, duração, build, pares restantes;
verificação pós-run fora do agente (build, `compose up`, healthcheck, front)
em `verificacao.csv`.

## Avaliação

1. **Funcional (covariável).** Verificador Playwright percorre
   `avaliacao/roteiro-aceitacao.md` no app rodando → % de critérios cumpridos.
   Usuário confere à mão as 6 apps da calibração.
2. **Design (primário).** Código anonimizado (sem `CLAUDE.md`, `.claude/`,
   transcrição, identificadores de run). Juiz com `avaliacao/juiz-prompt.md`
   versionado aplica `avaliacao/rubrica.md` (0/1/2 por ponto: condicional
   central ou acoplamento direto / abstração com decisão acoplada / caso novo
   não toca código existente) → 0–12; excesso de engenharia em separado.
3. **Calibração.** Sorteio estratificado de 6 apps (3 A, 3 B, dois modelos),
   pontuadas às cegas pelo usuário **antes** de ver o juiz. Kappa ponderado por
   ponto; aceitar o juiz com κ ≥ 0,6. Abaixo disso, revisar a rubrica e
   repontuar tudo — sem rodar nada de novo.
4. **Métricas estáticas (secundárias).** CK (CBO, LCOM, WMC), PMD, abstração
   especulativa, arquivos e linhas; ESLint no front.
5. **Custo e comportamento (secundários).** Turnos, tokens, tempo de API e
   local, ferramentas, builds, reescritas — `metricas.py` reaproveitado.
6. **Validação de manipulação run a run.** Hash do contexto inicial por par
   idêntico exceto o harness; skills e hooks presentes só em B; evidência de
   hook disparado em B.

## Análise pré-registrada

`analise/preregistro.md`, com hash, fechado **antes da rodada de medição**;
depois dela só mudam mecânicas de pipeline, declaradas.

- **Primário:** Mann-Whitney exato A vs B na nota 0–12, por modelo; Holm entre
  os dois testes; tamanho de efeito por Cliff's delta.
- **Interação:** permutação sobre a diferença das diferenças (exploratório).
- **Secundários:** Opus vs Sonnet dentro de cada braço; métricas estáticas;
  custo; Holm.
- **Sensibilidade:** repetir o primário só com apps ≥ 70% dos critérios; relatar
  correlação nota × cumprimento.
- Relatório sempre com **tempo por run** (total, API, local).

## Estrutura

Tudo vive em **`J:\TCC\TCC-evaluation\`**: plano, harness, runner, avaliação
e os dados de todas as runs. A pasta está dentro do repositório git `J:\TCC`.

```
J:\TCC\TCC-evaluation\
  docs/         plano-fase1-construcao.md (este plano), preregistro.md,
                registro de decisões, relatórios
  produto/      requisitos.md, prompt.txt, checar-vazamento.py
  harness/      CLAUDE.md, .claude/skills/, .claude/settings.json, FONTES.md, HASH
  infra/        Dockerfile.base, run.compose.yml
  runner/       run-lote.sh
  avaliacao/    pontos-de-pressao.md, rubrica.md, roteiro-aceitacao.md,
                juiz-prompt.md, verificador/, anonimizar.sh
  analise/      metricas.py, analise.py
  dados/        medicao/, runs/, runs.csv, verificacao.csv, metricas.csv,
                notas-juiz.csv, calibracao/, relatorios/
  .gitignore
```

**Persistência e git.** Versionados: `docs/`, `produto/`, `harness/`,
`infra/`, `runner/`, `avaliacao/`, `analise/` e os CSVs de `dados/`. Fora do
git (`.gitignore`): `dados/runs/**` e `dados/medicao/**` (apps completas,
`node_modules`, `target`), tarballs de imagem e **qualquer `.credentials.json`**.
O arquivador **remove a credencial** do config dir antes de guardar a
transcrição: cada run recebe uma cópia dela, e ela nunca pode chegar ao
repositório. As apps são guardadas como `tar.gz` por run, fora do git.

Reaproveitados de `J:\TCC\teste-tcc`: `run-pilot.sh` (preflight, pausa,
aborto, heartbeat), `metricas.py`, `analise.py` (Mann-Whitney exato validado
contra scipy), `anonimizar.sh`, script de validação por hash do contexto
inicial.

## Etapas

0. Salvar este plano em `J:\TCC\TCC-evaluation\docs\plano-fase1-construcao.md`;
   criar a estrutura de pastas e o `.gitignore`.
1. `requisitos.md`, `prompt.txt`, checagem anti-vazamento.
2. Pontos de pressão, rubrica, roteiro de aceitação, prompt do juiz.
3. Curadoria do harness com `FONTES.md`; congelar hash.
4. Imagem base, DinD, runner.
5. Verificador funcional, coletores, juiz, anonimizador.
6. Pré-registro fechado.
7. Rodada de medição (fora do conjunto); ajustes; congelar pipeline.
8. Lote: 5 blocos, pausa por par.
9. Avaliação: funcional → anonimização → juiz → calibração → métricas →
   análise → relatório.

## Verificação

- **Runner com CLI stub** cobrindo sucesso, 429, timeout e JSON inválido:
  pausa, retomada, par incompleto, ordem alternada.
- **Isolamento com duas runs simultâneas:** `docker ps` dentro de A não vê B;
  `docker inspect` sem volume compartilhado; workspace inicial com hash igual
  nos dois braços, exceto o harness.
- **Harness:** hash conferido no preflight; hooks disparando em `claude -p`
  visíveis na transcrição; braço A sem bloco `instructions`.
- **Anti-vazamento:** script passa em `requisitos.md`, `prompt.txt` e harness.
- **Verificador funcional** aplicado às apps da medição e conferido à mão.
- **Juiz:** prompt versionado; κ ≥ 0,6 na amostra cega.
- **Medição** entrega custo e tempo reais por run → estimativa de janelas de
  cota antes de iniciar o lote.

## Limites conhecidos

- Cegagem imperfeita: formatação aplicada por hooks pode denunciar o braço B.
- Juiz da mesma família dos modelos avaliados (trocável por parâmetro).
- Uso headless ≠ uso interativo com um humano respondendo.
- Estimativa de 1–2h por run e 5–7 janelas de cota ainda não medida — a rodada
  de medição confirma.