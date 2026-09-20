# Experimento: Claude Code com harness × sem harness

Bancada que roda o Claude Code em modo headless sobre a mesma tarefa, com e sem
uma camada de instrução de projeto, e compara o que sai.

Desenho completo em [docs/plano.md](docs/plano.md).

## Estrutura

A divisão é por **quem enxerga o quê**:

```
experimento/     copiado para dentro do workspace do agente
  prompt/        o enunciado, idêntico nas duas condições
  harness/       só na condição COM. Vira a raiz do projeto sob teste

infra/           roda de fora, o agente nunca vê
  docker/        imagem fixada por versão
  scripts/       executar / par / rodada / extração

avaliacao/       nunca chega ao agente. Gabarito, rotas de teste, notas
docs/            plano, notas do harness
runs/<id>/       workspace, transcrição, build, meta.json
runs/logs/       saída de terminal de cada execução
```

> [!warning] `experimento/harness/` é copiado inteiro
> `cp -r experimento/harness/. workspace/`. Qualquer arquivo largado ali chega
> ao agente. Anotação vai em `docs/harness-notas.md`, nunca ali dentro.

## Versões fixadas

| item | versão |
|---|---|
| imagem base | `maven:3.9.16-eclipse-temurin-21` |
| Node | `24.19.0` |
| Claude Code | `2.1.269` |
| Spring Boot | `4.1.1`, **pedido no enunciado** |

> [!note] O workspace nasce vazio
> Até 20/09/2026 havia um esqueleto Spring Boot como ponto de partida. Ele saiu,
> e Java 21 e Spring Boot 4.1.1 passaram a ser **pedidos no enunciado**, em
> "Observações do time técnico". É pedido, não garantia: o que o agente de fato
> escolheu fica em `fundacao.spring_boot` e `fundacao.java` no `meta.json`, e o
> `~/.m2` da imagem é aquecido com essas mesmas versões.

## Preparação

1. Abrir o **Docker Desktop** e esperar ficar "running".
2. Gerar o token da assinatura no seu terminal (abre o navegador):
   ```bash
   claude setup-token
   ```
3. Criar `.env` na raiz, já coberto pelo `.gitignore`:
   ```
   CLAUDE_CODE_OAUTH_TOKEN=<token>
   ```
4. Construir a imagem, a partir da raiz:
   ```bash
   docker build -f infra/docker/Dockerfile -t experimento-harness:v1 .
   ```

## Rodar

Uma execução:

```bash
infra/scripts/executar.sh FUMACA-01 claude-haiku-4-5 SEM
```

O par `SEM` e `COM` ao mesmo tempo — de propósito, para horário e carga de
servidor ficarem iguais nos dois braços:

```bash
infra/scripts/par.sh MED-06-HAIKU claude-haiku-4-5
```

Os três modelos nas duas condições, seis execuções em paralelo:

```bash
EFFORT=medium infra/scripts/rodada.sh MED-07
```

`EFFORT` é opcional e vale `high` por padrão, conforme D8 do plano. O valor
usado vai para o `meta.json` de cada execução.

Para interromper, o container tem nome fixo:

```bash
docker stop exp-MED-06-HAIKU-COM
```

## Conferir uma execução

Os quatro exemplos do enunciado:

```bash
avaliacao/ferramentas/conferir-exemplos.sh MED-05-HAIKU-COM
```

A rota do `FRETEGRATIS`, que nenhum exemplo cobre e que 4 de 10 execuções
erraram:

```bash
CASOS=avaliacao/casos/rotas-sem-exemplo.json avaliacao/ferramentas/conferir-exemplos.sh MED-05-HAIKU-COM
```

O que mais ficou descoberto está em
[avaliacao/rotas-descobertas.md](avaliacao/rotas-descobertas.md).

## Convenção de id

| prefixo | o que é |
|---|---|
| `FUMACA-` | teste de infraestrutura. **Fora da análise** |
| `MED-` | rodada de medição, para calibrar custo e tempo. **Fora da análise** |
| a definir | o lote de verdade |
