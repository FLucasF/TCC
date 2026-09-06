# O que falta para fechar o harness e iniciar o Spec-Driven

## Estado atual

O harness base já está montado:

- [x] `AGENTS.md` define o contrato operacional, as boundaries do repositório e o bloqueio de implementação sem spec aplicável.
- [x] Existem skills separadas para backend, frontend e QA, todas subordinadas às specs em `specs/`.
- [x] Existem agentes customizados para revisão (`reviewer`) e validação (`verifier`).
- [x] A responsabilidade de implementação continua com o agente principal; não é necessário criar outro agente para isso.
- [x] Existe uma suíte de evals do harness com sete casos, rubric e templates de registro.
- [x] A configuração atual usa modelos e níveis de reasoning explícitos para agente principal, subagentes, reviewer e verifier.
- [x] O runtime está definido como `workspace-write`, aprovação `on-request` e rede desabilitada para comandos no workspace.

O que falta agora é fechar a organização e preparar a entrada da camada Spec-Driven. Não é necessário executar os evals para concluir esta etapa estrutural.

## 1. Fechar a configuração do harness

- [ ] Revisar e versionar juntas as alterações locais atuais em:
  - `AGENTS.md`;
  - `.codex/config.toml`;
  - `.codex/agents/reviewer.toml`;
  - `.codex/agents/verifier.toml`.
- [ ] Adicionar `enabled = true` em `[agents]` para deixar explícito que o harness usa subagentes, mesmo sendo o default atual do Codex.
- [ ] Não fixar `max_concurrent_threads_per_session` sem necessidade. A concorrência pode continuar dependente do ambiente.
- [ ] Remover `.codex/hooks.json` enquanto ele estiver vazio. Hooks só devem voltar quando houver uma regra mecânica e determinística que realmente precise ser aplicada.
- [ ] Fazer uma última revisão de portabilidade: nenhum caminho pessoal, segredo, token, credencial ou configuração específica da máquina pode permanecer em `.codex/`.
- [ ] Encerrar esta etapa com um commit exclusivo de harness e working tree limpa.

## 2. Tornar as boundaries vazias reais no Git

Atualmente `specs/` e `contracts/` existem localmente, mas não têm arquivos rastreados. Em um clone novo, essas pastas não existirão.

- [ ] Criar `specs/README.md` para documentar a organização e o ciclo de vida das specs.
- [ ] Criar `contracts/README.md` para explicar quando um contrato compartilhado deve ser criado ou atualizado.
- [ ] Não usar apenas `.gitkeep`: os arquivos README devem explicar a função e a autoridade de cada boundary.

## 3. Definir a convenção Spec-Driven

Usar uma pasta por capability/feature:

```text
specs/
└── <id>-<slug>/
    ├── SPEC.md
    ├── PLAN.md
    └── TASKS.md
```

- [ ] Definir no `specs/README.md` o formato do identificador e do slug.
- [ ] Definir os estados mínimos da spec: `draft`, `approved`, `implemented` e `superseded`.
- [ ] Considerar válida para implementação somente uma `SPEC.md` versionada e com estado `approved`.
- [ ] Definir como uma spec aprovada é alterada: mudança de comportamento exige nova versão ou alteração explícita antes do código.
- [ ] Definir como specs substituídas apontam para a versão sucessora.
- [ ] Definir que decisões ainda abertas impedem aprovação quando puderem mudar comportamento, contrato, persistência, segurança ou critério de aceite.

### Autoridade dos documentos

- `SPEC.md`: fonte de verdade do comportamento e do escopo do produto.
- `PLAN.md`: desenho de implementação derivado da spec; não pode mudar requisitos.
- `TASKS.md`: decomposição executável e rastreável do plano; não pode ampliar escopo.
- `contracts/`: representação versionada de contratos externamente observáveis quando a spec exigir.
- `AGENTS.md`: constituição e contrato operacional do repositório.

Não criar um `constitution.md` separado: ele duplicaria a autoridade já atribuída ao `AGENTS.md` e poderia gerar conflito.

## 4. Criar os templates mínimos

- [ ] Criar um template de `SPEC.md` contendo, no mínimo:
  - identificação, versão e estado;
  - objetivo e problema;
  - escopo incluído e excluído;
  - atores e permissões;
  - requisitos e regras de negócio;
  - estados, falhas e casos de borda;
  - impactos em API, persistência, segurança, frontend e E2E;
  - critérios de aceite observáveis;
  - decisões abertas e dependências.
- [ ] Criar um template de `PLAN.md` com boundaries afetadas, abordagem, contratos, dados, compatibilidade, validação e riscos conhecidos.
- [ ] Criar um template de `TASKS.md` com tarefas pequenas, dependências, critério de conclusão e vínculo com os critérios de aceite.
- [ ] Evitar campos obrigatórios que não se apliquem a todas as features; nesses casos, exigir `N/A` com justificativa curta.

## 5. Criar o workflow de Spec-Driven

- [ ] Criar uma skill `spec-driven-development` responsável por:
  - elaborar e revisar `SPEC.md`;
  - detectar decisões ausentes sem inventá-las;
  - produzir `PLAN.md` somente depois da aprovação da spec;
  - decompor o plano em `TASKS.md` rastreáveis;
  - atualizar a spec antes de qualquer mudança de comportamento;
  - impedir que plano ou tarefas substituam requisitos da spec.
- [ ] Atualizar o `AGENTS.md` para distinguir claramente quatro atividades:
  1. descoberta e esclarecimento;
  2. criação/aprovação da spec;
  3. planejamento e decomposição;
  4. implementação e validação.
- [ ] Manter as skills de backend, frontend e QA focadas em execução. Elas devem receber uma spec válida, não definir requisitos de produto.
- [ ] Definir que o reviewer compara a mudança com `SPEC.md`, `PLAN.md`, `TASKS.md` e contratos aplicáveis.
- [ ] Definir que o verifier valida os critérios de aceite pelas ferramentas existentes e informa `PASS`, `FAIL` ou `BLOCKED`.

## 6. Fechar a estratégia de contratos

Antes da primeira feature com API, decidir e documentar:

- [ ] Se o OpenAPI versionado em `contracts/` será escrito primeiro ou gerado pelo backend.
- [ ] Qual artefato é autoritativo quando `SPEC.md`, OpenAPI e implementação divergirem.
- [ ] Como mudanças incompatíveis serão versionadas.
- [ ] Como o frontend consumirá o contrato sem duplicar tipos manualmente.

Até essa decisão existir, não adicionar Springdoc, Orval ou outra dependência apenas por estar listada na stack desejada. A dependência deve entrar quando uma spec aprovada exigir o contrato e definir o fluxo.

## 7. Ordem recomendada

1. Consolidar e commitar a configuração atual do harness.
2. Remover o `hooks.json` vazio.
3. Versionar `specs/` e `contracts/` com seus READMEs.
4. Definir ciclo de vida, validade e precedência dos documentos.
5. Criar os templates de `SPEC.md`, `PLAN.md` e `TASKS.md`.
6. Criar a skill `spec-driven-development`.
7. Atualizar `AGENTS.md` e os papéis de reviewer/verifier para o novo fluxo.
8. Decidir a autoridade e o versionamento dos contratos.
9. Criar a primeira `SPEC.md` real do produto.
10. Somente depois da aprovação dessa spec, produzir `PLAN.md`, `TASKS.md` e iniciar código.

## Não é bloqueador agora

- Executar ou automatizar os sete evals do harness.
- Criar hooks sem uma regra determinística concreta.
- Adicionar novos agentes customizados.
- Adicionar MCP, plugins ou integrações externas.
- Montar CI/CD do produto.
- Instalar Playwright, Springdoc, Orval, bibliotecas de teste ou outras dependências futuras.
- Definir antecipadamente arquitetura, endpoints, entidades, telas ou banco de uma feature que ainda não possui spec.

## Definição de pronto

O harness estará organizacionalmente fechado e pronto para receber Spec-Driven quando:

- [ ] a configuração estiver explícita, portátil, versionada e sem alterações locais pendentes;
- [ ] não houver arquivos placeholder vazios ou sem função definida;
- [ ] `specs/` e `contracts/` existirem em qualquer clone e tiverem autoridade documentada;
- [ ] o ciclo `SPEC → PLAN → TASKS → implementação → validação` estiver definido;
- [ ] houver templates prontos e uma skill responsável pela criação e revisão dos artefatos Spec-Driven;
- [ ] estiver claro que apenas uma `SPEC.md` aprovada autoriza alteração de comportamento;
- [ ] a primeira feature puder começar pela spec sem exigir novas decisões sobre o funcionamento do próprio harness.
