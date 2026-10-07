#!/usr/bin/env bash
# Roda DENTRO do container: sobe o servico de um workspace e aplica um teste.
# Uso: executor.sh <arquivo-de-teste.mjs>
# /ws = workspace (somente leitura), /aceitacao = esta pasta.
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
