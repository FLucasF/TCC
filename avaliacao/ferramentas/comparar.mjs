// Roda DENTRO do container. Espera a aplicação subir, chama cada caso e
// compara campo a campo com o esperado.
//
// Uso: node /comparar.mjs /casos.json
//
// Sai com código igual ao número de casos que falharam, e 66 se a aplicação
// nunca respondeu.
//
// Formato do caso:
//   { id, descricao, requisicao, status_esperado?, esperado: { campo: valor } }
//
// `status_esperado` é opcional e vale 200. Caso de erro declara 400 e põe o
// código em `esperado.erro`. Antes de 20/09/2026 a ferramenta não tinha esse
// campo e tratava todo status diferente de 200 como falha, então um 400
// devolvido corretamente era contado como erro — a precedência dos oito códigos
// era impossível de testar.

import { readFileSync } from "node:fs";

const URL_ALVO = process.env.URL_ALVO ?? "http://localhost:8080/checkout/resumo";
const casos = JSON.parse(readFileSync(process.argv[2], "utf8"));

const esperar = (ms) => new Promise((r) => setTimeout(r, ms));

// Dinheiro é comparado em centavos: a resposta pode vir 409.7 ou 409.70, e as
// duas estão certas. Comparar texto marcaria uma delas como erro.
const centavos = (v) => Math.round(Number(v) * 100);

// Código de erro é string e precisa de comparação exata: o enunciado fixa os
// oito códigos em maiúsculas. Sem este ramo a comparação caía no numérico e
// `Number("PEDIDO_INVALIDO")` vira NaN, que nunca é igual a nada — todo caso de
// erro falhava, inclusive o correto.
const igual = (obtido, esperado) => {
  if (typeof esperado === "string") return String(obtido) === esperado;
  return typeof esperado === "number" && !Number.isInteger(esperado)
    ? centavos(obtido) === centavos(esperado)
    : Number(obtido) === Number(esperado);
};

async function subiu() {
  for (let i = 0; i < 90; i++) {
    try {
      await fetch(URL_ALVO, { method: "POST", headers: { "Content-Type": "application/json" }, body: "{}" });
      return true;
    } catch { await esperar(2000); }
  }
  return false;
}

async function comparar() {
let falharam = 0;
for (const caso of casos) {
  let corpo = null, status = null, bruto = null;
  try {
    const r = await fetch(URL_ALVO, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(caso.requisicao),
    });
    status = r.status;
    // Lê como texto antes de decodificar: resposta de erro é onde mais aparece
    // corpo vazio ou não-JSON, e um `r.json()` direto estouraria a exceção,
    // apagando o status — que é justamente o que se quer conferir.
    bruto = await r.text();
    if (bruto) { try { corpo = JSON.parse(bruto); } catch { corpo = null; } }
  } catch (e) {
    console.log(`${caso.id.padEnd(16)} FALHA  sem resposta: ${e.message}`);
    falharam++;
    continue;
  }

  const statusEsperado = caso.status_esperado ?? 200;
  const erros = [];
  for (const [campo, valor] of Object.entries(caso.esperado)) {
    if (!igual(corpo?.[campo], valor)) erros.push(`${campo}: esperado ${valor}, veio ${corpo?.[campo]}`);
  }
  if (status !== statusEsperado) erros.unshift(`HTTP ${status}, esperado ${statusEsperado}`);
  if (corpo === null && bruto !== null) erros.push(`corpo não é JSON: ${JSON.stringify(bruto.slice(0, 80))}`);

  if (erros.length === 0) {
    console.log(`${caso.id.padEnd(16)} ok     ${caso.descricao}`);
  } else {
    falharam++;
    console.log(`${caso.id.padEnd(16)} ERRO   ${caso.descricao}`);
    for (const e of erros) console.log(`${" ".repeat(23)}${e}`);
  }
}

console.log(`${casos.length - falharam}/${casos.length} casos corretos`);
return falharam;
}

// `exitCode` em vez de `process.exit()`: no Windows, sair na marra enquanto o
// undici ainda fecha sockets dispara um assert do libuv, no async.c da porta
// Windows, e o processo morre com 0xC0000409 em vez do número de falhas.
// Dentro do container Linux o `process.exit()` funcionava; o autoteste roda
// direto no host, e foi lá que apareceu. O código de saída continua sendo a
// contagem de falhas, ou 66 quando a app nunca subiu, que é o que o
// conferir-exemplos.sh lê e distingue.
if (!await subiu()) {
  console.log("a aplicação não respondeu em 180s");
  process.exitCode = 66;
} else {
  process.exitCode = await comparar();
}
