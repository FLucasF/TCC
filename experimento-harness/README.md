# experimento-harness

Execução do experimento descrito em `../Plano-Experimento-Harness.md`.

> [!warning] Nunca coloque aqui dentro, em pastas copiadas para o container:
> `../gabarito-avaliador.md`, testes escondidos, soluções de referência, o `.env` versionado.

## Estrutura

| Pasta | Conteúdo |
|---|---|
| `skeleton/` | Esqueleto Spring Boot 4.1.1 / Java 21 (Web + Validation), sem testes, sem wrapper |
| `prompt/prompt.md` | Cópia de `../prompt-experimento.md` |
| `harness/` | **Ainda não existe.** Só entra nas runs `COM` |
| `docker/` | Dockerfile, `settings.xml` (Maven offline), aquecimento do `~/.m2` |
| `scripts/` | `executar.sh`, `sonda-auth.sh`, `extrair-meta.mjs` |
| `runs/<id>/` | `workspace/`, `claude-output.jsonl`, `stderr.txt`, `build.txt`, `meta.json` |

## Versões fixadas

| Item | Versão |
|---|---|
| Imagem base | `maven:3.9.16-eclipse-temurin-21` |
| Node | `24.19.0` |
| Claude Code | `2.1.269` |
| Spring Boot | `4.1.1` |

## Passo a passo (Git Bash, dentro desta pasta)

1. Abrir o **Docker Desktop** e esperar ficar "running".
2. Gerar o token da assinatura **no seu terminal** (abre o navegador):
   ```bash
   claude setup-token
   ```
3. Criar `.env` nesta pasta (já está no `.gitignore`):
   ```
   CLAUDE_CODE_OAUTH_TOKEN=<cole o token aqui>
   ```
4. Construir a imagem:
   ```bash
   docker build -f docker/Dockerfile -t experimento-harness:v1 .
   ```
5. Conferir autenticação:
   ```bash
   scripts/sonda-auth.sh
   ```
6. Rodar uma execução:
   ```bash
   scripts/executar.sh FUMACA-01 claude-haiku-4-5 SEM
   ```

Runs com prefixo `FUMACA-` são testes de infraestrutura e **não entram na análise**.
