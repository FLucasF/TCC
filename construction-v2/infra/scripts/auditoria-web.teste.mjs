// Testa o detector de acesso externo.
//
// Uso:  node infra/scripts/auditoria-web.teste.mjs
//
// Existe porque a família de defeitos das ferramentas de medição já chegou a
// sete, e todos apareceram por acaso. Dois deles foram neste detector.
//
// Os casos marcados "regressão" reproduzem defeitos reais, com o comando
// copiado da execução que os revelou.

import { externo } from "./auditoria-web.mjs";

// Copiado de runs/MED-07-VAZIO-OPUS-COM/meta.json, comandos_suspeitos[0].
const POM_POR_HEREDOC = `mkdir -p /workspace/src/main/java/com/loja/checkout/{dominio,entrega,cupom,pagamento,web} /workspace/src/test/java/com/loja/checkout
cat > /workspace/pom.xml <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
</project>
EOF
echo ok`;

const CASOS = [
  // --- regressões: comandos reais que já foram classificados errado ---
  [false, "regressão MED-07-OPUS: heredoc escrevendo pom.xml com namespaces XML", POM_POR_HEREDOC],
  [false, "regressão FUMACA-01: agente conferindo a própria app", "curl -s -X POST http://localhost:8080/checkout/resumo -H 'Content-Type: application/json' -d '{}'"],

  // --- não é rede ---
  [false, "build do Maven", "cd /workspace && mvn -B -q verify"],
  [false, "git local", 'git add -A && git commit -m "resumo de checkout"'],
  [false, "URL dentro de comentário escrito em arquivo", 'echo "// ver https://docs.spring.io/spring-boot" >> Leia.java'],
  [false, "URL em string de código, sem verbo de rede", `sed -i 's|http://old|http://new|' application.properties`],
  [false, "curl no endereço local sem esquema", "curl -s localhost:8080/actuator/health"],
  [false, "curl em host.docker.internal", "curl http://host.docker.internal:8080/checkout/resumo"],

  // --- é rede ---
  [true, "curl para fora", "curl -sL https://raw.githubusercontent.com/spring-projects/spring-boot/main/README.adoc"],
  [true, "wget para fora", "wget https://repo1.maven.org/maven2/org/foo/bar/1.0/bar-1.0.jar"],
  [true, "curl sem URL visível e sem endereço local", 'curl -s "$ENDPOINT" -o saida.json'],
  [true, "git clone remoto", "git clone https://github.com/exemplo/projeto.git /tmp/p"],
  [true, "heredoc fechado, e rede depois", "cat > f.txt <<'EOF'\nnada\nEOF\ncurl https://exemplo.com/x"],
];

let falharam = 0;
console.log("autoteste do detector de acesso externo\n");
for (const [esperado, nome, comando] of CASOS) {
  const obtido = externo(comando);
  const ok = obtido === esperado;
  if (!ok) falharam++;
  console.log(`  ${ok ? "ok  " : "RUIM"}  esperado=${String(esperado).padEnd(5)} obtido=${String(obtido).padEnd(5)} ${nome}`);
  if (!ok) console.log("        comando: " + JSON.stringify(comando.slice(0, 120)));
}
console.log(`\n${CASOS.length - falharam}/${CASOS.length} casos corretos`);
process.exitCode = falharam;
