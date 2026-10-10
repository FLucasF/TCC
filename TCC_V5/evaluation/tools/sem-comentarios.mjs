// Tira os comentarios de um arquivo sem mudar o numero de linhas. Usado pela copia cega
// que o Lucas le (anonymize.mjs --sem-comentarios) e pela copia que o Semgrep le
// (semgrep/detect.sh): as duas leem o mesmo codigo, e o arquivo:linha bate com o original.
// Saiu do anonymize.mjs em 09/10, sem mudar uma linha da logica.

import { readFileSync, writeFileSync, copyFileSync } from "node:fs";

// Tira os comentarios de um .java sem mexer em texto entre aspas, e sem mudar o numero
// de linhas: cada quebra de linha dentro de um comentario fica onde estava.
export function semComentariosJava(fonte) {
  let saida = "", i = 0;
  const n = fonte.length;
  while (i < n) {
    const c = fonte[i], d = fonte[i + 1];
    if (c === '"' && fonte.startsWith('"""', i)) {            // bloco de texto
      const fim = fonte.indexOf('"""', i + 3);
      const ate = fim < 0 ? n : fim + 3;
      saida += fonte.slice(i, ate); i = ate;
    } else if (c === '"' || c === "'") {                      // texto ou caractere
      let j = i + 1;
      while (j < n && fonte[j] !== c && fonte[j] !== "\n") j += fonte[j] === "\\" ? 2 : 1;
      saida += fonte.slice(i, j + 1); i = j + 1;
    } else if (c === "/" && d === "/") {                      // comentario de linha
      while (i < n && fonte[i] !== "\n") i++;
    } else if (c === "/" && d === "*") {                      // comentario de bloco
      const fim = fonte.indexOf("*/", i + 2);
      const ate = fim < 0 ? n : fim + 2;
      saida += fonte.slice(i, ate).replace(/[^\n]/g, ""); i = ate;
    } else { saida += c; i++; }
  }
  return saida.replace(/[ \t]+$/gm, "");
}
export const semComentariosXml = (t) => t.replace(/<!--[\s\S]*?-->/g, (m) => m.replace(/[^\n]/g, "")).replace(/[ \t]+$/gm, "");
export const semComentariosLinha = (t) => t.replace(/^[ \t]*[#!].*$/gm, "");

export function copiaSemComentarios(origem, alvo, relativo) {
  const ext = relativo.toLowerCase().split(".").pop();
  const limpar = ext === "java" ? semComentariosJava
    : ext === "xml" ? semComentariosXml
    : ["properties", "yml", "yaml"].includes(ext) ? semComentariosLinha
    : null;
  if (!limpar) return copyFileSync(origem, alvo);
  writeFileSync(alvo, limpar(readFileSync(origem, "utf8")));
}
