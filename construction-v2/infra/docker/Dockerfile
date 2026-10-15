# Imagem única do experimento. Build a partir da raiz de experimento-harness/:
#   docker build -f infra/docker/Dockerfile -t experimento-harness:v3 .
#
# Tudo com versão exata. Mudou qualquer linha = nova tag da imagem + registro no diário.

FROM node:24.19.0-bookworm-slim AS node

FROM maven:3.9.16-eclipse-temurin-21

# Node (necessário para instalar o Claude Code via npm)
COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY --from=node /usr/local/lib/node_modules /usr/local/lib/node_modules
RUN ln -s ../lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm \
 && ln -s ../lib/node_modules/npm/bin/npx-cli.js /usr/local/bin/npx

RUN apt-get update \
 && apt-get install -y --no-install-recommends git jq ca-certificates \
 && rm -rf /var/lib/apt/lists/*

RUN npm install -g @anthropic-ai/claude-code@2.1.269

# A imagem oficial do Maven define MAVEN_CONFIG=/root/.m2 e um entrypoint que
# copia configurações para lá. Nenhum dos dois serve para um usuário não-root.
ENV MAVEN_CONFIG=""
ENTRYPOINT []

ENV DISABLE_AUTOUPDATER=1 \
    CLAUDE_CODE_DISABLE_AUTO_MEMORY=1

RUN useradd -m -s /bin/bash experimento
USER experimento
WORKDIR /home/experimento

# Aquece o ~/.m2 para o tempo de download não entrar na medição: dependências,
# plugins do build e o provider JUnit do Surefire, que só é baixado quando
# existe teste. O projeto de aquecimento é apagado em seguida.
#
# O Maven continua ONLINE nas execuções: o agente pode acrescentar dependência
# se julgar necessário, e isso é registrado no meta.json em vez de impedido.
COPY --chown=experimento infra/docker/aquecimento/ /tmp/aquecimento/
RUN cd /tmp/aquecimento && mvn -B -q verify && rm -rf /tmp/aquecimento


WORKDIR /workspace
