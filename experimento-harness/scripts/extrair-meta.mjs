// Lê a saída stream-json de uma run e escreve runs/<id>/meta.json.
// Uso: node extrair-meta.mjs <run_dir> --chave valor ...
import { readFileSync, writeFileSync, existsSync } from "node:fs";
import { join } from "node:path";

const [runDir, ...resto] = process.argv.slice(2);
const arg = {};
for (let i = 0; i < resto.length; i += 2) arg[resto[i].replace(/^--/, "")] = resto[i + 1];

const eventos = [];
const linhasInvalidas = [];
const saida = join(runDir, "claude-output.jsonl");
if (existsSync(saida)) {
  readFileSync(saida, "utf8").split(/\r?\n/).forEach((linha, n) => {
    if (!linha.trim()) return;
    try { eventos.push(JSON.parse(linha)); } catch { linhasInvalidas.push(n + 1); }
  });
}

const init = eventos.find((e) => e.type === "system" && e.subtype === "init") ?? null;
const result = [...eventos].reverse().find((e) => e.type === "result") ?? null;

// Chamadas de ferramenta e comandos de terminal, a partir das mensagens do assistente.
const ferramentas = {};
const comandosBash = [];
const modelosNasMensagens = new Set();
for (const e of eventos) {
  if (e.type !== "assistant" || !e.message) continue;
  if (e.message.model) modelosNasMensagens.add(e.message.model);
  for (const bloco of e.message.content ?? []) {
    if (bloco.type !== "tool_use") continue;
    ferramentas[bloco.name] = (ferramentas[bloco.name] ?? 0) + 1;
    if (bloco.name === "Bash" && bloco.input?.command) comandosBash.push(bloco.input.command);
  }
}

const suspeitos = comandosBash.filter((c) => /\b(curl|wget)\b|https?:\/\//i.test(c));
const usage = result?.usage ?? {};
const num = (v) => (typeof v === "number" ? v : 0);
const entrada = num(usage.input_tokens), cacheEscrita = num(usage.cache_creation_input_tokens),
  cacheLeitura = num(usage.cache_read_input_tokens);

const stderr = existsSync(join(runDir, "stderr.txt")) ? readFileSync(join(runDir, "stderr.txt"), "utf8") : "";
const pre = stderr.split(/\r?\n/).filter((l) => l.startsWith("[pre]"));

const codigoSaida = Number(arg.codigo_saida);
let encerramento = "concluido";
if (codigoSaida === 124) encerramento = "timeout";
else if (!result) encerramento = "erro_sem_resultado";
else if (result.is_error) encerramento = result.subtype === "error_max_turns" ? "limite_turnos" : "erro";

const meta = {
  run_id: arg.run_id,
  valida: null, // decidido por humano, conforme as regras de exceção do plano
  motivo_invalidade: null,
  modelo_solicitado: arg.modelo,
  modelo_init: init?.model ?? null,
  modelos_observados: {
    mensagens: [...modelosNasMensagens],
    uso_por_modelo: result?.modelUsage ? Object.keys(result.modelUsage) : [],
  },
  condicao: arg.condicao,
  ambiente: {
    imagem: arg.imagem,
    imagem_id: arg.imagem_id,
    claude_code_versao: init?.claude_code_version ?? null,
    hash_prompt: arg.hash_prompt,
    hash_skeleton: arg.hash_skeleton,
    hash_harness: arg.hash_harness || null,
    verificacoes_pre_execucao: pre,
  },
  parametros: {
    effort: "high",
    max_turnos: Number(arg.max_turnos),
    tempo_maximo_s: Number(arg.tempo_maximo_s),
    ferramentas_bloqueadas: arg.ferramentas_bloqueadas.split(","),
    permission_mode_init: init?.permissionMode ?? null,
  },
  tempo: {
    inicio: arg.inicio,
    fim: arg.fim,
    duracao_s: Number(arg.duracao_s),
    duracao_cli_ms: result?.duration_ms ?? null,
    duracao_api_ms: result?.duration_api_ms ?? null,
  },
  resultado_execucao: {
    codigo_saida: codigoSaida,
    encerramento,
    subtype_resultado: result?.subtype ?? null,
    turnos: result?.num_turns ?? null,
    chamadas_ferramenta: Object.values(ferramentas).reduce((a, b) => a + b, 0),
    chamadas_por_ferramenta: ferramentas,
    resposta_final: typeof result?.result === "string" ? result.result.slice(0, 500) : null,
    build_pos_execucao_ok: Number(arg.build_codigo) === 0,
  },
  tokens: {
    entrada,
    saida: num(usage.output_tokens),
    cache_leitura: cacheLeitura,
    cache_escrita: cacheEscrita,
    entrada_total: entrada + cacheEscrita + cacheLeitura,
    custo_estimado_usd: result?.total_cost_usd ?? null,
    uso_por_modelo: result?.modelUsage ?? null,
  },
  isolamento_init: init
    ? {
        cwd: init.cwd ?? null,
        ferramentas_disponiveis: init.tools ?? null,
        mcp_servers: init.mcp_servers ?? null,
        slash_commands: init.slash_commands ?? null,
        agents: init.agents ?? null,
        skills: init.skills ?? null,
        plugins: init.plugins ?? null,
      }
    : null,
  auditoria: {
    comandos_bash: comandosBash.length,
    acesso_web_suspeito: suspeitos.length > 0,
    comandos_suspeitos: suspeitos,
    ferramentas_bloqueadas_disponiveis: (init?.tools ?? []).filter((t) =>
      arg.ferramentas_bloqueadas.split(",").includes(t)),
    linhas_jsonl_invalidas: linhasInvalidas,
  },
};

writeFileSync(join(runDir, "meta.json"), JSON.stringify(meta, null, 2) + "\n");
console.log(`meta.json: ${encerramento}, turnos=${meta.resultado_execucao.turnos}, ` +
  `ferramentas=${meta.resultado_execucao.chamadas_ferramenta}, entrada_total=${meta.tokens.entrada_total}, ` +
  `saida=${meta.tokens.saida}, build_ok=${meta.resultado_execucao.build_pos_execucao_ok}`);
