# Registro de construção do harness

Por que o harness em `harness/` ficou como está: o que foi construído e cortado, o
que a definição subtrativa produziu, e onde ele fica em relação à literatura.

O `harness/README.md` é o manual — como usar e como estender. Este arquivo é o
registro, e é onde a história cresce. Separados em 2026-09-07, porque um arquivo que
faz os dois trabalhos cresce sem parar: o README foi de 8.810 para 14.730 bytes em
três dias, enquanto o `CLAUDE.md` ficou parado.

Todo texto abaixo veio do `harness/README.md`, sem reescrita — exceto duas correções
de fato, marcadas onde aparecem.

---

## What was removed, and the signal that reverses it

On 2026-09-06 this harness had a `verify/` directory: a manifest mapping changed
paths to commands, an executor called from two hooks, per-edit checks, a completion
gate that blocked with exit code 2, and a JSONL trace. It was removed the same day.

**Why.** The verification layer was never observed changing an outcome. During the
session that built it, the hooks never fired — the session had been opened one
directory up — and every check ran anyway, by choice. That is one session and not
evidence of much, but the harness's own filter is subtractive: what the model does
on its own does not belong here. Nothing had been measured that put verification
outside that filter.

**What it cost to remove.** Three things it did are not recoverable by prose:

1. **The gate made "done" contingent rather than declared.** A claim that survived
   an `exit 2` is a different object from a claim. Prose asks; it cannot refuse.
2. **`BLOCKED` never became `FAIL`.** With Maven absent the model reads "failed" and
   goes on to *fix healthy code*. That is a specific destructive behavior, and the
   distinction only existed because it was written down in code.
3. **Output filtered to the touched file.** It is what made a duplication check cost
   ~30 tokens instead of a full report. Without an executor there is no filter.

**The signal that brings it back.** Any one of:

- you find a commit where the suite did not run and should have
- the agent "fixes" working code because a tool was missing from the environment
- you stop trusting "done" and start re-running things yourself before reading

The code is in this repository's history: commit `8b3cbdb` is the last one that has
the complete `verify/`, and `52395cd` is the removal.

`git checkout 8b3cbdb -- harness/verify` brings back the executor and its 41 tests.
**They will not run yet** — verified: the suite loads the real `.claude/validation.json`
and fails with `ENOENT` without it. The rest of the layer is in that same commit and
has to be copied back deliberately: the manifest, the two hooks in `settings.json`,
and the `CLAUDE.md` section that told the model it did not need to run the checks. A
plain `git revert 52395cd` does not do it either, because the prose files were
rewritten afterwards and conflict.

The code is a checkout. The wiring is a decision, and it should be made again rather
than restored by reflex.

---

## Where this sits, and what is missing

The survey *Externalization in LLM Agents* (arXiv:2604.08224) decomposes a harness
into six dimensions. It is worth stating plainly which of them this harness has,
because most of the rows are now empty, and a row that just said *present* would
hide which part is missing.

The paper is explicit that this is **"an analytical framework for comparing harness
architectures rather than an implementation checklist"** — so the goal is not to
fill every row. It is to know which rows are empty and why.

| Dimension | State here |
|---|---|
| **Skills** | Six, with progressive disclosure. Missing the paper's third attribute: revision driven by observed failure |
| **Verification / Control** | **Absent.** Built and removed on 2026-09-06 — see above |
| **Permission** | **Partial.** Declarative deny/ask rules, not isolation — see below |
| **Protocols** | Inherited via MCP. The hook contract went with the executor. Not designed |
| **Memory** | Semantic: `CLAUDE.md`, the skills and the README, all reviewed. Personalised: the platform's auto-memory, left on and not governed by the harness — see *Memory the harness does not write*, in the README. Episodic and working context: none |
| **Observability** | **Absent.** The trace was written by the executor and went with it |

Four of six empty is a real result and not a gap to apologise for. It is what the
subtractive definition produces when it is applied honestly, including to the
harness's own work.

*Correção de 2026-09-07: a linha Skills dizia "Five". São seis desde que a
`impact-analysis` entrou.*

Working context, the fourth dimension, is absent on purpose. InfiAgent
(arXiv:2601.03204) keeps files as the state and rebuilds context every step from the
files plus a fixed window of recent actions. Neither half is available here: the
platform's compaction is closed, and with a person in the loop, the person is the
window.

### Permission is policy, not isolation

The paper describes this dimension as sandboxing, filesystem isolation and network
restriction. What is here are permission *rules*, which is a weaker thing.

Concretely: denying `curl` and `wget` by name does not close the network. A wrapper
the matcher does not strip — `docker exec c curl ...` — is matched as a `docker`
command and passes. Closing the network needs an allowlist-shaped policy or a real
sandbox, neither of which is configured.

The rules are worth having. They are not isolation, and the difference should not be
blurred.

---

## Deferred, each with the signal that brings it back

| Deferred | Signal |
|---|---|
| The verification layer | See *What was removed*, above — three signals, any one of them |
| Turning auto-memory off | A second model-written instruction steers a session. Then the store is an unreviewed guide layer: off in `settings.json`, with `settings.local.json` as the per-machine opt-in |
| Episodic memory | You catch yourself correcting the same thing a third time |
| Clean-context reviewer | Reviewing diffs yourself becomes the bottleneck. Note that the platform already ships one; this row is a reminder to check before building |
| Evaluation cases | You are changing rules and cannot tell whether they help |

**Adotado em 2026-09-07 — índice de código.** Esta tabela trazia a linha *"Code
index — you watch it open eight files to answer one structural question"*. O índice
foi adotado: `graphify` instalado com versão fixa por `scripts/install-graphify.sh`,
e a skill `impact-analysis` roteia entre grafo e busca conforme a coisa mudada seja
ou não um nó do grafo.

O que a adoção mediu, e que vale mais que a decisão: o grafo respondeu `toIso` em
1.213 bytes contra 4.701 da busca e excluiu dois arquivos que a busca incluiu errado;
devolveu `No unique node match` para um campo de resposta, que a busca resolveu em
285 bytes; e um grafo de 2.278 nós contém **zero** nós com rota HTTP, então a
fronteira entre front e back continua invisível para ele.

---

## Design notes

**The definition is subtractive on purpose.** "What the model does not do on its own"
generates a small harness by construction. An earlier version defined it as an
action interface, context selection, a control loop, and guards — three of which are
software, which is why that version generated software.

**The subtraction was eventually applied to the harness's own code.** A verification
layer was built, measured, and cut in one day. That is the definition working rather
than failing, but it is worth naming the asymmetry it exposes: the filter asks
whether the model does something on its own, and *"the model usually does it"* and
*"the model always does it"* are different answers that the filter cannot tell apart.
Everything the removed layer did lived in that gap.

**Rules are curated against defaults, not copied from a style guide.** Most of a
best-practices document confirms what a capable model already does. The parts that
change behavior are the ones that restrain it: prefer duplication over the wrong
abstraction, do not refactor beyond what you touched, do not infer a requirement
that was not stated.

**Asking has a cost curve.** The agent stops for what is expensive to reverse or
whose effect does not show up where the change is — a breaking change that fails
nothing qualifies; a comment you can delete after reading the diff does not. Ask too
often and the questions stop protecting anything, because you stop reading them.
