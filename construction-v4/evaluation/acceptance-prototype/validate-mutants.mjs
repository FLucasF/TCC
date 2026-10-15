// Valida a suite de aceitacao nos dois sentidos, sem modelo nenhum:
//   1. a calculadora de referencia, servida por HTTP, passa em tudo;
//   2. cada mutante (mutants.mjs), servido do mesmo jeito, e REPROVADO.
// A suite roda como roda nos pacotes: processo separado, pela API.
//
// Uso: node evaluation/acceptance-prototype/validate-mutants.mjs [suite.mjs]
// Sai com 0 se a referencia passa e todos os mutantes sao reprovados.
import fs from "node:fs";
import http from "node:http";
import path from "node:path";
import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";
import { MUTANTES } from "./mutants.mjs";

const aqui = path.dirname(fileURLToPath(import.meta.url));
const suite = path.resolve(process.argv[2] ?? path.join(aqui, "strategy.mjs"));
const refSrc = fs.readFileSync(path.join(aqui, "ref-strategy.mjs"), "utf8").replace(/\r\n/g, "\n");
const nomesDosCasos = [...fs.readFileSync(suite, "utf8").matchAll(/^\s*\["([^"]+)",\s*\{/gm)].map((m) => m[1]);

function aplicar(m) {
  let src = refSrc;
  for (const [de, para] of m.trocas) {
    const n = src.split(de).length - 1;
    if (n !== 1) throw new Error(`${m.id}: o trecho aparece ${n} vezes no ref-strategy.mjs, e precisa aparecer 1: ${de}`);
    src = src.replace(de, para);
  }
  return src;
}

// a resposta da API: dinheiro em reais, erro como 400 { erro }
const INTEIROS = new Set(["prazoEntregaDias", "parcelas"]);
function servir(calcular) {
  const srv = http.createServer((req, res) => {
    let corpo = "";
    req.on("data", (d) => (corpo += d));
    req.on("end", () => {
      let status = 200, saida;
      try {
        const r = calcular(JSON.parse(corpo || "{}"));
        if (r.erro) { status = 400; saida = { erro: r.erro }; }
        else saida = Object.fromEntries(Object.entries(r).map(([k, v]) => [k, typeof v === "number" && !INTEIROS.has(k) ? v / 100 : v]));
      } catch (e) { status = 500; saida = { erro: "EXCECAO", detalhe: e.message }; }
      res.writeHead(status, { "Content-Type": "application/json" }).end(JSON.stringify(saida));
    });
  });
  return new Promise((ok) => srv.listen(0, "127.0.0.1", () => ok(srv)));
}

function rodarSuite(porta) {
  return new Promise((ok) => {
    const p = spawn(process.execPath, [suite], { env: { ...process.env, BASE: `http://127.0.0.1:${porta}` } });
    let out = "";
    p.stdout.on("data", (d) => (out += d));
    p.stderr.on("data", (d) => (out += d));
    p.on("close", (code) => ok({ code, out }));
  });
}

// quais casos falharam, pelo nome, a partir das linhas "FALHA <caso> ..."
function casosQueFalharam(out) {
  const falhos = new Set();
  for (const linha of out.split("\n")) {
    const resto = linha.trim().startsWith("FALHA ") ? linha.trim().slice(6) : null;
    if (resto === null) continue;
    const nome = nomesDosCasos.filter((n) => resto.startsWith(n + " ") || resto.startsWith(n + ":")).sort((a, b) => b.length - a.length)[0];
    falhos.add(nome ?? "(nao identificado) " + resto.slice(0, 60));
  }
  return [...falhos];
}

async function avaliar(src) {
  const { calcular } = await import("data:text/javascript," + encodeURIComponent(src));
  const srv = await servir(calcular);
  const r = await rodarSuite(srv.address().port);
  srv.close();
  return { ...r, resultado: r.out.match(/RESULTADO: .*/)?.[0] ?? "(sem linha RESULTADO)", falhos: casosQueFalharam(r.out) };
}

console.log(`suite: ${path.relative(process.cwd(), suite)} (${nomesDosCasos.length} casos)\n`);
let problemas = 0;

const ref = await avaliar(refSrc);
const refOk = ref.code === 0;
if (!refOk) problemas++;
console.log(`${refOk ? "PASSA     " : "FALHA     "} referencia   ${ref.resultado}`);
if (!refOk) console.log(ref.out);

const pegos = {};
for (const m of MUTANTES) {
  const r = await avaliar(aplicar(m));
  const reprovado = r.code !== 0;
  if (!reprovado) problemas++;
  pegos[m.id] = r.falhos;
  console.log(`${reprovado ? "REPROVADO " : "PASSOU (!)"} ${m.id.padEnd(5)} ${m.regra}`);
  console.log(`           ${reprovado ? "pego por: " + r.falhos.join(" | ") : "nenhum caso pegou: falta caso para esta regra"}`);
}

const unicos = MUTANTES.filter((m) => pegos[m.id].length === 1).map((m) => `${m.id} (${pegos[m.id][0]})`);
console.log(`\nmutantes reprovados: ${MUTANTES.length - (problemas - (refOk ? 0 : 1))} de ${MUTANTES.length}`);
console.log(`pegos por um caso so (se esse caso mudar, a regra fica descoberta): ${unicos.join(", ") || "nenhum"}`);
process.exit(problemas ? 1 : 0);
