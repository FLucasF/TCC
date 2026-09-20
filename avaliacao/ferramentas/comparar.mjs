// Roda DENTRO do container. Espera a aplicação subir, chama cada caso e
// compara campo a campo com o esperado.
//
// Uso: node /comparar.mjs /casos.json
//
// Sai com código igual ao número de casos que falharam, e 66 se a aplicação
// nunca respondeu.

import { readFileSync } from "node:fs";

const URL_ALVO = process.env.URL_ALVO ?? "http://localhost:8080/checkout/resumo";
const casos = JSON.parse(readFileSync(process.argv[2], "utf8"));

const esperar = (ms) => new Promise((r) => setTimeout(r, ms));

// Dinheiro é comparado em centavos: a resposta pode vir 409.7 ou 409.70, e as
// duas estão certas. Comparar texto marcaria uma delas como erro.
const centavos = (v) => Math.round(Number(v) * 100);
const igual = (obtido, esperado) =>
  typeof esperado === "number" && !Number.isInteger(esperado)
    ? centavos(obtido) === centavos(esperado)
    : Number(obtido) === Number(esperado);

async function subiu() {
  for (let i = 0; i < 90; i++) {
    try {
      await fetch(URL_ALVO, { method: "POST", headers: { "Content-Type": "application/json" }, body: "{}" });
      return true;
    } catch { await esperar(2000); }
  }
  return false;
}

if (!await subiu()) {
  console.log("a aplicação não respondeu em 180s");
  process.exit(66);
}

let falharam = 0;
for (const caso of casos) {
  let corpo = null, status = null;
  try {
    const r = await fetch(URL_ALVO, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(caso.requisicao),
    });
    status = r.status;
    corpo = await r.json();
  } catch (e) {
    console.log(`${caso.id.padEnd(12)} FALHA  sem resposta: ${e.message}`);
    falharam++;
    continue;
  }

  const erros = [];
  for (const [campo, valor] of Object.entries(caso.esperado)) {
    if (!igual(corpo?.[campo], valor)) erros.push(`${campo}: esperado ${valor}, veio ${corpo?.[campo]}`);
  }
  if (status !== 200) erros.unshift(`HTTP ${status}`);

  if (erros.length === 0) {
    console.log(`${caso.id.padEnd(12)} ok     ${caso.descricao}`);
  } else {
    falharam++;
    console.log(`${caso.id.padEnd(12)} ERRO   ${caso.descricao}`);
    for (const e of erros) console.log(`${" ".repeat(19)}${e}`);
  }
}

console.log(`${casos.length - falharam}/${casos.length} casos corretos`);
process.exit(falharam);
