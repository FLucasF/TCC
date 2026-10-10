# A imagem da bancada (`infra/docker/`)

Uma imagem Docker só, a mesma para todas as execuções, para o build e para a suíte de
aceitação. Tudo nela tem versão exata; mudar qualquer linha do `Dockerfile` é uma
imagem nova, com tag nova e registro no `DECISOES.md`.

```bash
docker build -f infra/docker/Dockerfile -t experimento-harness:v5 .     # da raiz do TCC
```

| imagem | Claude Code | usada em |
|---|---|---|
| `experimento-harness:v3` | 2.1.269 | V3 e V4 (`sha256:54de317c…`) |
| `experimento-harness:v5` | 2.1.288 (o primeiro a aceitar o Opus 5.5 é o 2.1.280) | V5 (`sha256:8e96815c…`) |

## O que tem no `Dockerfile`

| parte | o que é | por quê |
|---|---|---|
| `maven:3.9.16-eclipse-temurin-21` | Java 21 e Maven, a base | o enunciado pede Java 21 e Spring Boot 4.1.1 |
| o Node copiado de `node:24.19.0` | só para instalar o Claude Code pelo npm | o Claude Code é um pacote npm |
| `git`, `jq`, `ca-certificates` | ferramentas de linha de comando de um ambiente de desenvolvimento comum | o agente pode usá-las como usaria numa máquina de verdade (o motivo de cada uma não foi registrado; a bancada mesma não as usa dentro do container) |
| `@anthropic-ai/claude-code@<versão>` | o agente, numa versão travada | uma atualização no meio do lote mudaria o instrumento; `DISABLE_AUTOUPDATER=1` impede que ele se atualize sozinho |
| `CLAUDE_CODE_DISABLE_AUTO_MEMORY=1` | desliga a memória automática | uma execução não pode aprender com a anterior |
| o usuário `experimento`, sem root | quem roda o agente e o Maven | por isso o `MAVEN_CONFIG` e o entrypoint da imagem oficial do Maven, que supõem root (`/root/.m2`), são apagados |
| o **aquecimento** (abaixo) | baixa as dependências do Maven **na hora do build da imagem** | para o download não entrar na medição |

## Para que serve o `aquecimento/`

É um projeto Spring Boot **mínimo** (uma classe com `main` e um teste vazio), com as
versões que o enunciado pede (Spring Boot 4.1.1, Java 21) e os starters de um serviço
web: `webmvc`, `validation` e os de teste. Ele existe **só durante o `docker build`**:

1. o `Dockerfile` copia o projeto para `/tmp/aquecimento` dentro da imagem;
2. roda `mvn verify` nele uma vez. Isso obriga o Maven a **baixar** tudo o que um
   projeto desses precisa (o Spring Boot e as dependências dele, os plugins do build, e o
   provider do JUnit, que o Surefire só baixa quando existe um teste: por isso o teste
   vazio) e a guardar em `~/.m2`, o cache do Maven, **dentro da imagem**;
3. apaga o projeto (`rm -rf /tmp/aquecimento`). O cache fica.

**Por quê.** Sem isso, cada execução começaria com o Maven baixando centenas de
arquivos da internet na primeira compilação do agente. Isso tem três problemas:

- **o tempo**: o download (minutos, variando com a rede) entraria na duração de relógio
  de cada execução, e o agente esperaria por ele no meio do trabalho;
- **o ruído entre níveis**: num quarteto, 4 execuções baixando ao mesmo tempo disputam a
  rede, e uma pode sair mais lenta só por isso;
- **a falha**: uma queda da rede no meio do download vira "não compilou", que é nota 0,
  por um motivo que não é do código.

Com o cache quente, o primeiro `mvn` do agente já acha tudo em `~/.m2` e compila na hora.
O Maven continua **online**: se o agente quiser uma dependência a mais, ele baixa, e
isso fica registrado no `meta.json` em vez de ser impedido.

**O agente nunca vê o aquecimento.** O projeto é apagado antes de a imagem ficar pronta,
e a pasta de trabalho do agente (`/workspace`) nasce vazia. O `.dockerignore` da raiz é
uma lista branca: só o `Dockerfile` e o `aquecimento/` entram no build (o `target/` de
um `mvn` local no aquecimento fica fora). O enunciado, o harness e o `.env` nunca entram
na imagem, e as linhas `[pre]` de cada execução provam que não há `CLAUDE.md` nenhum fora
do workspace.
