// Testa a proposta de validade.
//
// Uso:  node infra/scripts/validade.teste.mjs
//
// Os casos marcados "regressão" reproduzem defeitos reais, com os ids copiados
// da execução que os revelou.

import { proporValida, mesmoModelo } from "./validade.mjs";

const CASOS = [
  // --- regressão: o nono defeito, achado na FUMACA-03 ---
  [true, "regressão FUMACA-03: Haiku reporta o snapshot datado nas mensagens",
   { encerramento: "concluido", modeloSolicitado: "claude-haiku-4-5", modelosObservados: ["claude-haiku-4-5-20251001"] }],

  // --- válidas ---
  [true, "conclusão limpa, id identico",
   { encerramento: "concluido", modeloSolicitado: "claude-opus-5", modelosObservados: ["claude-opus-5"] }],
  [true, "sem mensagens de assistente registradas",
   { encerramento: "concluido", modeloSolicitado: "claude-opus-5", modelosObservados: [] }],
  [true, "build quebrado NAO invalida (§13.3: conta como resultado)",
   { encerramento: "concluido", modeloSolicitado: "claude-sonnet-5", modelosObservados: ["claude-sonnet-5"] }],

  // --- invalidas por encerramento ---
  [false, "interrompida a mao", { encerramento: "interrompido", modeloSolicitado: "claude-opus-5", modelosObservados: [] }],
  [false, "sem evento result", { encerramento: "erro_sem_resultado", modeloSolicitado: "claude-opus-5", modelosObservados: [] }],
  [false, "erro da API", { encerramento: "erro", modeloSolicitado: "claude-opus-5", modelosObservados: [] }],
  [false, "teto de turnos", { encerramento: "limite_turnos", modeloSolicitado: "claude-opus-5", modelosObservados: [] }],

  // --- invalida por troca de modelo, que e o que o campo existe para pegar ---
  [false, "troca de modelo de verdade",
   { encerramento: "concluido", modeloSolicitado: "claude-opus-5", modelosObservados: ["claude-opus-5", "claude-haiku-4-5"] }],
  [false, "familia parecida NAO e o mesmo modelo",
   { encerramento: "concluido", modeloSolicitado: "claude-opus-5", modelosObservados: ["claude-opus-5-1"] }],
];

let falharam = 0;
console.log("autoteste da proposta de validade\n");
for (const [esperado, nome, entrada] of CASOS) {
  const r = proporValida(entrada);
  const ok = r.valida === esperado;
  if (!ok) falharam++;
  console.log(`  ${ok ? "ok  " : "RUIM"}  esperado=${String(esperado).padEnd(5)} obtido=${String(r.valida).padEnd(5)} ${nome}`);
  if (!ok) console.log(`        motivo: ${r.motivo}`);
}

// A normalizacao sozinha, para o limite ficar explicito.
const NORM = [
  [true, "claude-haiku-4-5-20251001", "claude-haiku-4-5"],
  [true, "claude-opus-5", "claude-opus-5"],
  [false, "claude-opus-5-1", "claude-opus-5"],
  [false, "claude-haiku-4-5", "claude-sonnet-5"],
];
console.log("\n  normalizacao do snapshot datado:");
for (const [esperado, a, b] of NORM) {
  const ok = mesmoModelo(a, b) === esperado;
  if (!ok) falharam++;
  console.log(`  ${ok ? "ok  " : "RUIM"}  ${a} ${esperado ? "==" : "!="} ${b}`);
}

console.log(`\n${CASOS.length + NORM.length - falharam}/${CASOS.length + NORM.length} casos corretos`);
process.exitCode = falharam;
