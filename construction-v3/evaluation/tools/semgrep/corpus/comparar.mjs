// Compara a saida do classificar.mjs com o esperado.csv do corpus.
// Colunas especiais: avisos_contem (o aviso tem de ter o texto) e avisos_sem (nao pode ter).
import { readFileSync } from "node:fs";
const parse = (l) => { const o = []; let c = "", q = false; for (let i = 0; i < l.length; i++) { const ch = l[i]; if (q) { if (ch === '"' && l[i + 1] === '"') { c += '"'; i++; } else if (ch === '"') q = false; else c += ch; } else if (ch === '"') q = true; else if (ch === ",") { o.push(c); c = ""; } else c += ch; } o.push(c); return o; };
const ler = (f) => { const L = readFileSync(f, "utf8").split(/\r?\n/).filter((l) => l && !l.startsWith("#")); const h = parse(L[0]); return Object.fromEntries(L.slice(1).map((l) => { const v = parse(l); return [v[0], Object.fromEntries(h.map((k, i) => [k, v[i]]))]; })); };
const [arqEsp, arqSai] = process.argv.slice(2);
const E = ler(arqEsp), S = ler(arqSai);
let ok = 0, total = 0;
for (const [p, e] of Object.entries(E)) {
  const s = S[p]; const erros = [];
  for (const [k, v] of Object.entries(e)) {
    if (k === "pacote" || k === "porque" || v === "*" || v === undefined) continue;
    total++;
    const av = s?.avisos ?? "";
    const certo = k === "avisos_contem" ? av.includes(v) : k === "avisos_sem" ? !av.includes(v) : s?.[k] === v;
    if (certo) ok++; else erros.push(`${k}: esperado ${v}, veio ${k.startsWith("avisos") ? JSON.stringify(av) : s?.[k]}`);
  }
  console.log(`${erros.length ? "ERRO" : "ok  "} ${p}${erros.length ? "\n       " + erros.join("\n       ") + "\n       evid: " + (s?.evidencia ?? "") : ""}`);
}
console.log(`\n${ok} de ${total} respostas certas`);
