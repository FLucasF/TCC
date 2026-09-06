Resumindo, seu TCC está ficando assim:

- **Projeto prático:** Job Manager com React + Spring.
- **Agente:** Codex CLI.
- **Abordagens:** Spec-Driven + RPI + Harness Engineering.
- **Objetivo:** estudar como um harness estruturado melhora/organiza o desenvolvimento feito por um agente de IA.

Papel de cada parte:

- `specs/` → o que deve ser construído.
- `AGENTS.md` → regras gerais do agente.
- `.agents/skills/` → como executar tarefas, por exemplo Research e Plan.
- `.codex/` → configurações do Codex, agentes, rules, hooks etc.
- `scripts/` → verificações automáticas.
- Memories → contexto auxiliar entre sessões, mas não fonte da verdade.