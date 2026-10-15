// Compares the v2 reference with V5's reference, purchase by purchase, where the rules are the same.
// Reads the lines of V5Differential.java ({ v5: the purchase in V5's format, v2: v2's answer }), runs
// V5's reference (the frozen TCC_V5 file, read only) and counts the differences.
//
// Usage: java -cp target/classes reference.V5Differential 20000 20261014 | node differential.mjs
import { createInterface } from "node:readline";
import { pathToFileURL } from "node:url";

const V5_REFERENCE = "J:/TCC/TCC_V5/evaluation/acceptance-prototype/ref-strategy.mjs";
const { calcular: v5Calculate } = await import(pathToFileURL(V5_REFERENCE).href);

let total = 0, equal = 0, refusals = 0;
const examples = [];
for await (const line of createInterface({ input: process.stdin })) {
  if (!line.trim()) continue;
  const { v5: purchase, v2 } = JSON.parse(line);
  const v5 = v5Calculate(purchase);
  total++;
  if (v2.erro) refusals++;
  const fields = v2.erro ? ["erro"] : Object.keys(v2);
  const different = fields.filter((field) => JSON.stringify(v5[field]) !== JSON.stringify(v2[field]));
  if (!different.length) { equal++; continue; }
  if (examples.length < 5) examples.push({ purchase, different: different.map((f) => `${f}: V5 ${JSON.stringify(v5[f])} × v2 ${JSON.stringify(v2[f])}`) });
}
console.log(`${total} purchases: ${equal} equal, ${total - equal} different (${refusals} were refusals in v2)`);
for (const e of examples) console.log("DIFFERENT:", JSON.stringify(e.purchase), "\n   ", e.different.join("\n    "));
process.exit(total > 0 && equal === total ? 0 : 1);
