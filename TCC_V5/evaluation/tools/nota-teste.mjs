// Prova o nota.mjs em casos com a conta feita a mao: os pesos, o meio ponto, o exagero
// do P5, a trava (nao compilou = 0) e o "indeterminado" (sem nota, decisao de 10/10).
//
// Uso:  node evaluation/tools/nota-teste.mjs
//
// COMO LER. Cada caso e uma execucao: o que a suite mediu (status, contas de 12,
// recusas de 9) e as respostas do Semgrep (P1 a P5). O teste monta os dois CSVs numa
// pasta temporaria, roda o nota.mjs de verdade e compara a nota A e as marcas com o
// esperado. Termina com "N de N casos como a regra manda".

import { mkdtempSync, writeFileSync, rmSync } from "node:fs";
import { join, dirname } from "node:path";
import { tmpdir } from "node:os";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const AQUI = dirname(fileURLToPath(import.meta.url));
const CERTO = { loc: "isolado", sel: "consulta" };
const pac = (p4 = CERTO, p5 = "dados", outros = CERTO) => ({ P1: outros, P2: outros, P3: outros, P4: p4, P5: p5 });

// [descricao, status, contas, recusas, respostas, nota A esperada ("" = sem nota), marca esperada]
const CASOS = [
  ["tudo certo", "all_passed", 12, 9, pac(), "100.0", ""],
  ["P4 errado nos dois: 30 + 20 + 40", "some_failed", 12, 9, pac({ loc: "espalhado", sel: "condicional-no-calculo" }), "90.0", ""],
  ["P4 com uma certa (meio ponto): 30 + 20 + 45", "some_failed", 12, 9, pac({ loc: "isolado", sel: "condicional-no-calculo" }), "95.0", ""],
  ["fabrica (condicional-unica) conta como certa", "all_passed", 12, 9, pac({ loc: "isolado", sel: "condicional-unica" }), "100.0", ""],
  ["exagero no P5: 30 + 20 + 40", "all_passed", 12, 9, pac(CERTO, "estrutura"), "90.0", ""],
  ["P5 condicional nao e exagero", "all_passed", 12, 9, pac(CERTO, "condicional"), "100.0", ""],
  ["metade das contas: 15 + 20 + 50", "some_failed", 6, 9, pac(), "85.0", ""],
  ["P4 indeterminado: sem nota", "all_passed", 12, 9, pac({ loc: "indeterminado", sel: "consulta" }), "", "sem nota; P4 indeterminado"],
  ["P5 indeterminado: sem nota", "all_passed", 12, 9, pac(CERTO, "indeterminado"), "", "sem nota; P5 indeterminado"],
  ["P1 indeterminado tambem tira a nota", "all_passed", 12, 9, pac(CERTO, "dados", { loc: "indeterminado", sel: "indeterminado" }), "", "sem nota"],
  ["nao compilou: trava 0, mesmo com indeterminado", "build_failed", "", "", pac({ loc: "indeterminado", sel: "indeterminado" }), "0.0", "trava: build_failed"],
  ["nao subiu: trava 0", "app_did_not_start", "", "", pac(), "0.0", "trava: app_did_not_start"],
];

const base = mkdtempSync(join(tmpdir(), "nota-teste-"));
const acc = ["run_id,status,passed,total,contas,recusas",
  ...CASOS.map(([, st, c, r], i) => `R${i},${st},0,21,${c},${r}`)].join("\n") + "\n";
const cab = "pacote,P1_localizacao,P1_selecao,P2_localizacao,P2_selecao,P3_localizacao,P3_selecao,P4_localizacao,P4_selecao,P5_forma,P5_proporcao,evidencia,avisos";
const sem = [cab, ...CASOS.map(([, , , , p], i) =>
  [`R${i}`, ...["P1", "P2", "P3", "P4"].flatMap((k) => [p[k].loc, p[k].sel]), "enum-dados", p.P5, '""', '""'].join(","))].join("\n") + "\n";
writeFileSync(join(base, "acc.csv"), acc);
writeFileSync(join(base, "sem.csv"), sem);
const r = spawnSync(process.execPath, [join(AQUI, "nota.mjs"), join(base, "acc.csv"), join(base, "sem.csv")], { encoding: "utf8" });
rmSync(base, { recursive: true, force: true });
if (r.status !== 0) { console.error(r.stderr); process.exit(1); }

const linhas = new Map(r.stdout.trim().split(/\r?\n/).slice(1).map((l) => [l.split(",")[0], l.split(",")]));
let ok = 0;
CASOS.forEach(([desc, , , , , notaA, marca], i) => {
  const l = linhas.get(`R${i}`);
  const [obtida, marcas] = [l?.[9], l?.[12] ?? ""];
  const certo = l && obtida === notaA && (marca ? marcas.startsWith(marca) || marcas.includes(marca) : marcas === "");
  if (certo) ok++;
  else console.log(`FALHOU ${desc}: nota A "${obtida}" (esperada "${notaA}"), marcas "${marcas}" (esperada "${marca}")`);
});
console.log(`${ok} de ${CASOS.length} casos como a regra manda.`);
process.exit(ok === CASOS.length ? 0 : 1);
