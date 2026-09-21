// Recalcula os campos DERIVADOS dos meta.json já gravados.
//
// Uso:
//   node infra/scripts/reauditar.mjs                  # só mostra o que mudaria
//   node infra/scripts/reauditar.mjs --gravar
//   node infra/scripts/reauditar.mjs --prefixo FUMACA-03 --gravar
//
// Por que existe: quando uma regra de medida é corrigida, as execuções já
// coletadas ficam com o número antigo. Elas são REAVALIADAS, não refeitas — é a
// exceção única à regra de "nada muda" do pré-registro §10. Isto é a ferramenta
// que faz essa reavaliação.
//
// Só toca no que é derivado e reconstruível:
//
//   auditoria.acesso_web_suspeito, comandos_suspeitos   <- do claude-output.jsonl
//   auditoria.chamadas_web                              <- de chamadas_por_ferramenta
//   valida_proposta, motivo_proposta                    <- do encerramento e dos modelos
//
// NÃO toca em tempos, hashes, tokens nem imagem: vieram da linha de comando na
// hora da execução e não existem em outro lugar. E não toca em `valida`, que é
// decisão humana pela §13.3.

import { readdirSync, existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { externo } from "./auditoria-web.mjs";
import { proporValida } from "./validade.mjs";

const args = process.argv.slice(2);
const gravar = args.includes("--gravar");
const i = args.indexOf("--prefixo");
const prefixo = i >= 0 ? args[i + 1] : null;

let mudaram = 0, iguais = 0;
for (const r of readdirSync("runs").sort()) {
  if (prefixo && !r.startsWith(prefixo)) continue;
  const metaPath = join("runs", r, "meta.json");
  if (!existsSync(metaPath)) continue;
  const meta = JSON.parse(readFileSync(metaPath, "utf8"));
  const mudancas = [];

  // --- auditoria de rede, do jsonl ---
  const jsonlPath = join("runs", r, "claude-output.jsonl");
  if (existsSync(jsonlPath)) {
    const comandos = [];
    for (const linha of readFileSync(jsonlPath, "utf8").split(/\r?\n/)) {
      if (!linha.trim()) continue;
      let e;
      try { e = JSON.parse(linha); } catch { continue; }
      if (e.type !== "assistant" || !e.message) continue;
      for (const b of e.message.content ?? [])
        if (b.type === "tool_use" && b.name === "Bash" && b.input?.command) comandos.push(b.input.command);
    }
    const suspeitos = comandos.filter(externo);
    const antes = meta.auditoria.comandos_suspeitos ?? [];
    if (antes.length !== suspeitos.length || meta.auditoria.acesso_web_suspeito !== suspeitos.length > 0) {
      mudancas.push(`acesso_web_suspeito ${meta.auditoria.acesso_web_suspeito} -> ${suspeitos.length > 0} (${antes.length} -> ${suspeitos.length} comandos)`);
      meta.auditoria.acesso_web_suspeito = suspeitos.length > 0;
      meta.auditoria.comandos_suspeitos = suspeitos;
    }
  }

  // --- chamadas de web, do proprio meta ---
  const f = meta.resultado_execucao?.chamadas_por_ferramenta ?? {};
  const web = (f.WebSearch ?? 0) + (f.WebFetch ?? 0);
  if (meta.auditoria.chamadas_web !== web) {
    mudancas.push(`chamadas_web ${meta.auditoria.chamadas_web ?? "(ausente)"} -> ${web}`);
    meta.auditoria.chamadas_web = web;
  }

  // --- proposta de validade ---
  const p = proporValida({
    encerramento: meta.resultado_execucao?.encerramento,
    modeloSolicitado: meta.modelo_solicitado,
    modelosObservados: meta.modelos_observados?.mensagens ?? [],
  });
  if (meta.valida_proposta !== p.valida || meta.motivo_proposta !== p.motivo) {
    mudancas.push(`valida_proposta ${meta.valida_proposta ?? "(ausente)"} -> ${p.valida}  [${p.motivo}]`);
    meta.valida_proposta = p.valida;
    meta.motivo_proposta = p.motivo;
  }

  if (!mudancas.length) { iguais++; continue; }
  mudaram++;
  console.log(r);
  for (const m of mudancas) console.log(`    ${m}`);

  if (gravar) {
    // Lista, nao campo unico: as duas execucoes de Opus do MED-07 ja tinham
    // sido reauditadas em 20/09, e sobrescrever apagaria esse registro. O
    // historico completo esta no git, mas o arquivo tambem deve contar a
    // propria historia. Absorve tambem o `auditoria.reauditado_em`, que a
    // versao anterior do script gravava um nivel abaixo.
    // Data de hoje, nao chumbada: a versao anterior gravava uma constante, que
    // viraria mentira na proxima vez que o script rodasse.
    const hoje = new Date().toISOString().slice(0, 10);
    const antigos = [meta.reauditado_em, meta.auditoria?.reauditado_em]
      .flat()
      .filter(Boolean);
    delete meta.auditoria?.reauditado_em;
    meta.reauditado_em = [...new Set([...antigos, hoje])].sort();
    writeFileSync(metaPath, JSON.stringify(meta, null, 2) + "\n");
  }
}

console.log(`\n${mudaram} execucao(oes) mudariam, ${iguais} ficam iguais.`);
console.log(gravar ? "GRAVADO." : "Nada foi gravado. Use --gravar para aplicar.");
