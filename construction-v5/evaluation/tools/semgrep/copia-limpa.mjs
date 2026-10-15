// Copia os .java, .properties e .yml de src/main de cada pacote para <destino>, sem comentarios e com as
// linhas no mesmo lugar (o mesmo removedor da copia cega do Lucas). O Semgrep le esta
// copia: as regras de texto deixam de ver comentario, e o arquivo:linha bate com o
// original. Chamado pelo detect.sh (09/10).
//
// O projeto de cada pacote e o do pom.xml mais raso (fora de target/ e .git/), a mesma
// regra do build (run-one.sh), da suite (executor.sh) e das metricas (metrics.sh): o
// Semgrep le exatamente o codigo que foi compilado e testado, mesmo com o projeto numa
// subpasta (<pacote>/checkout/pom.xml) ou com um rascunho do agente em outra pasta. So
// aceitar <pacote>/src/main deixou 4 pacotes do V4 sem nenhum arquivo (ensaio de 10/10).
// Num projeto multimodulo (o pom da raiz so lista <module>), vale o src/main de cada
// modulo listado, e nao uma pasta que o pom nao cita (um rascunho com pom proprio).
// Sem pom.xml, vale o src/main mais raso. Os caminhos continuam relativos ao pacote
// (checkout/src/main/...), os mesmos da copia cega.
//
// Uso:  node copia-limpa.mjs <raiz-dos-pacotes> <destino-vazio>

import { readdirSync, readFileSync, mkdirSync, existsSync } from "node:fs";
import { join, relative, dirname } from "node:path";
import { copiaSemComentarios } from "../sem-comentarios.mjs";

const [raiz, destino] = process.argv.slice(2);
if (!raiz || !destino) { console.error("uso: node copia-limpa.mjs <raiz> <destino>"); process.exit(2); }
if (existsSync(destino) && readdirSync(destino).length) { console.error(`${destino} nao esta vazio`); process.exit(2); }

const IGNORADAS = new Set(["target", ".git"]);
const posix = (p) => p.replace(/\\/g, "/");

// Todas as pastas do pacote com pom.xml, e todas as src/main, com a profundidade.
function procura(dir, prof, achados) {
  for (const e of readdirSync(dir, { withFileTypes: true })) {
    if (!e.isDirectory()) { if (e.name === "pom.xml") achados.poms.push({ prof, dir }); continue; }
    if (IGNORADAS.has(e.name)) continue;
    const p = join(dir, e.name);
    if (e.name === "main" && posix(dir).endsWith("/src")) achados.mains.push({ prof, dir: p });
    procura(p, prof + 1, achados);
  }
  return achados;
}
// O mais raso; empate de profundidade desempata pelo caminho (como o sort do build).
const maisRaso = (lista) => lista.slice().sort((a, b) => a.prof - b.prof || (posix(a.dir) < posix(b.dir) ? -1 : 1));

// O src/main do projeto de um pom e o de cada <module> que ele lista (recursivo).
function mainsDoProjeto(dir, vistos = new Set()) {
  if (vistos.has(dir)) return [];
  vistos.add(dir);
  const pom = readFileSync(join(dir, "pom.xml"), "utf8").replace(/<!--[\s\S]*?-->/g, "");
  const out = existsSync(join(dir, "src", "main")) ? [join(dir, "src", "main")] : [];
  for (const [, m] of pom.matchAll(/<module>\s*([^<]+?)\s*<\/module>/g))
    if (existsSync(join(dir, m, "pom.xml"))) out.push(...mainsDoProjeto(join(dir, m), vistos));
  return out;
}

let n = 0;
const varre = (dir, pacote) => {
  for (const e of readdirSync(dir, { withFileTypes: true })) {
    const p = join(dir, e.name);
    if (e.isDirectory()) { if (!IGNORADAS.has(e.name)) varre(p, pacote); continue; }
    if (!/\.(java|properties|ya?ml)$/.test(e.name)) continue;
    const rel = posix(relative(join(raiz, pacote), p));
    const alvo = join(destino, pacote, rel);
    mkdirSync(dirname(alvo), { recursive: true });
    copiaSemComentarios(p, alvo, rel);
    n++;
  }
};

const pacotes = readdirSync(raiz, { withFileTypes: true }).filter((d) => d.isDirectory()).map((d) => d.name);
for (const pac of pacotes) {
  mkdirSync(join(destino, pac), { recursive: true });
  const { poms, mains } = procura(join(raiz, pac), 0, { poms: [], mains: [] });
  const rel = (d) => posix(relative(join(raiz, pac), d)) || ".";
  let lidos = [];
  if (poms.length) {
    const [pom, segundo] = maisRaso(poms);
    if (segundo && segundo.prof === pom.prof)
      console.error(`AVISO ${pac}: dois pom.xml na mesma profundidade; vale ${rel(pom.dir)}`);
    lidos = mainsDoProjeto(pom.dir);
    if (lidos.length > 1) console.error(`AVISO ${pac}: multimodulo; valem ${lidos.map(rel).join(", ")}`);
  } else if (mains.length) {
    lidos = [maisRaso(mains)[0].dir];
    console.error(`AVISO ${pac}: sem pom.xml; vale ${rel(lidos[0])}`);
  }
  for (const m of lidos) varre(m, pac);
  if (!lidos.length) console.error(`AVISO ${pac}: nenhum src/main (o classificador marca sem-codigo)`);
}
console.error(`${pacotes.length} pacotes, ${n} arquivos sem comentarios`);
