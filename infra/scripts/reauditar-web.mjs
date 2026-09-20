// Recalcula SÓ os dois campos de auditoria de rede nos meta.json já gravados,
// a partir do claude-output.jsonl, que continua no disco.
//
// Uso:
//   node infra/scripts/reauditar-web.mjs           # só mostra o que mudaria
//   node infra/scripts/reauditar-web.mjs --gravar  # grava
//
// Por que existe: o detector teve dois defeitos, e o segundo marcou as duas
// execuções de Opus do MED-07 por causa dos namespaces XML no heredoc que
// escreve o pom.xml. meta.json é dado DERIVADO — sai do jsonl — então recalcular
// não é editar artefato. Os campos vindos da linha de comando na hora da
// execução (tempos, hashes, imagem) não existem em outro lugar e não são
// tocados.

import { readdirSync, existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { externo } from "./auditoria-web.mjs";

const gravar = process.argv.includes("--gravar");
const RAIZ = "runs";

let mudaram = 0, iguais = 0;
for (const r of readdirSync(RAIZ).sort()) {
  const metaPath = join(RAIZ, r, "meta.json");
  const jsonlPath = join(RAIZ, r, "claude-output.jsonl");
  if (!existsSync(metaPath) || !existsSync(jsonlPath)) continue;

  const comandos = [];
  for (const linha of readFileSync(jsonlPath, "utf8").split(/\r?\n/)) {
    if (!linha.trim()) continue;
    let e;
    try { e = JSON.parse(linha); } catch { continue; }
    if (e.type !== "assistant" || !e.message) continue;
    for (const b of e.message.content ?? []) {
      if (b.type === "tool_use" && b.name === "Bash" && b.input?.command) comandos.push(b.input.command);
    }
  }

  const meta = JSON.parse(readFileSync(metaPath, "utf8"));
  const antes = { s: meta.auditoria.acesso_web_suspeito, n: meta.auditoria.comandos_suspeitos.length };
  const suspeitos = comandos.filter(externo);
  const depois = { s: suspeitos.length > 0, n: suspeitos.length };

  if (antes.s === depois.s && antes.n === depois.n) { iguais++; continue; }
  mudaram++;
  console.log(`${r.padEnd(26)} suspeito ${antes.s} -> ${depois.s}   comandos ${antes.n} -> ${depois.n}`);
  for (const c of meta.auditoria.comandos_suspeitos.filter((c) => !suspeitos.includes(c))) {
    console.log(`    deixa de acusar: ${JSON.stringify(c.slice(0, 90))}`);
  }
  for (const c of suspeitos.filter((c) => !meta.auditoria.comandos_suspeitos.includes(c))) {
    console.log(`    passa a acusar:  ${JSON.stringify(c.slice(0, 90))}`);
  }

  if (gravar) {
    meta.auditoria.acesso_web_suspeito = depois.s;
    meta.auditoria.comandos_suspeitos = suspeitos;
    meta.auditoria.reauditado_em = "2026-09-20";
    writeFileSync(metaPath, JSON.stringify(meta, null, 2) + "\n");
  }
}

console.log(`\n${mudaram} execuções mudariam, ${iguais} ficam iguais.`);
console.log(gravar ? "GRAVADO." : "Nada foi gravado. Use --gravar para aplicar.");
