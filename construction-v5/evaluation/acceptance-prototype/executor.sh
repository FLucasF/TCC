#!/usr/bin/env bash
# Roda DENTRO do container: sobe o servico de um workspace e aplica um teste.
# Uso: executor.sh <arquivo-de-teste.mjs>
# /ws = workspace (somente leitura), /aceitacao = esta pasta.
#
# COMO LER. Quem chama e o infra/scripts/acceptance.sh, um container por execucao.
#   1. copia o workspace para /tmp (o original fica so para leitura) e acha o pom mais raso;
#   2. usa o .jar que o build do run-one.sh ja gerou; se nao houver, compila (sem testes);
#   3. liga o servico na porta 18080 e espera ele responder (ate 90 s);
#   4. roda a suite (strategy.mjs), que manda os 21 pedidos e imprime o RESULTADO.
# O codigo de saida diz o que houve: 0 passou tudo, 1 errou casos, 3 sem pom,
# 4 build falhou, 5 o servico morreu ao subir. Se ele passar dos 90 s vivo e sem
# responder, a suite roda assim mesmo e os casos falham (nos testes, subiu em segundos).
set -uo pipefail
cp -r /ws /tmp/ws
POM="$(find /tmp/ws -name pom.xml -not -path '*/target/*' -printf '%d %p\n' | sort -n | head -1 | cut -d' ' -f2-)"
[ -n "$POM" ] || { echo "RESULTADO: sem pom"; exit 3; }
cd "$(dirname "$POM")"
JAR="$(ls target/*.jar 2>/dev/null | grep -v -E 'plain|original|sources' | head -1)"
if [ -z "$JAR" ]; then
  mvn -q -B -DskipTests package >/tmp/build.log 2>&1 || { echo "RESULTADO: build falhou"; tail -5 /tmp/build.log; exit 4; }
  JAR="$(ls target/*.jar | grep -v -E 'plain|original|sources' | head -1)"
fi
java -jar "$JAR" --server.port=18080 >/tmp/app.log 2>&1 &
PID=$!
for i in $(seq 1 90); do
  curl -s -o /dev/null http://localhost:18080/ 2>/dev/null && break
  kill -0 $PID 2>/dev/null || { echo "RESULTADO: app nao subiu"; tail -15 /tmp/app.log; exit 5; }
  sleep 1
done
BASE=http://localhost:18080 node "/aceitacao/$1"
RC=$?
kill $PID 2>/dev/null
exit $RC
