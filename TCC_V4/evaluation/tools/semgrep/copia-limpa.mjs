// Copia os .java, .properties e .yml de src/main de cada pacote para <destino>, sem comentarios e com as
// linhas no mesmo lugar (o mesmo removedor da copia cega do Lucas). O Semgrep le esta
// copia: as regras de texto deixam de ver comentario, e o arquivo:linha bate com o
// original. Chamado pelo detect.sh (09/10).
//
// Uso:  node copia-limpa.mjs <raiz-dos-pacotes> <destino-vazio>

import { readdirSync, statSync, mkdirSync, existsSync } from "node:fs";
import { join, relative, dirname } from "node:path";
import { copiaSemComentarios } from "../sem-comentarios.mjs";

const [raiz, destino] = process.argv.slice(2);
if (!raiz || !destino) { console.error("uso: node copia-limpa.mjs <raiz> <destino>"); process.exit(2); }
if (existsSync(destino) && readdirSync(destino).length) { console.error(`${destino} nao esta vazio`); process.exit(2); }

let n = 0;
const varre = (dir, pacote) => {
  for (const e of readdirSync(dir)) {
    const p = join(dir, e);
    if (statSync(p).isDirectory()) { if (e !== "target" && e !== ".git") varre(p, pacote); continue; }
    if (!/\.(java|properties|ya?ml)$/.test(e)) continue;
    const rel = relative(join(raiz, pacote), p).replace(/\\/g, "/");
    if (!rel.startsWith("src/main/")) continue;
    const alvo = join(destino, pacote, rel);
    mkdirSync(dirname(alvo), { recursive: true });
    copiaSemComentarios(p, alvo, rel);
    n++;
  }
};
const pacotes = readdirSync(raiz, { withFileTypes: true }).filter((d) => d.isDirectory()).map((d) => d.name);
for (const pac of pacotes) { mkdirSync(join(destino, pac), { recursive: true }); varre(join(raiz, pac), pac); }
console.error(`${pacotes.length} pacotes, ${n} arquivos sem comentarios`);
