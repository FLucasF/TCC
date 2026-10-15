// Decide se um comando de Bash executado pelo agente saiu da máquina.
//
// Mora em módulo próprio para poder ser testado: `node infra/scripts/auditoria-web.teste.mjs`.
// O `extrair-meta.mjs` importa daqui.
//
// Histórico, porque explica a forma:
//
// 1ª versão marcava toda execução, porque o agente sobe a própria aplicação e
//    chama `localhost:8080` para conferir os resultados contra os exemplos do
//    enunciado — na FUMACA-01 foram seis `curl`. Isso é o agente se verificando.
//    Corrigido com a lista de endereços locais.
//
// 2ª versão procurava URL em qualquer lugar do comando, e marcava quem só estava
//    escrevendo um arquivo. As duas execuções de Opus do MED-07 foram acusadas
//    por causa dos namespaces XML dentro do heredoc que escreve o `pom.xml`.
//    Como só o Opus escreve o pom assim, o defeito marcava um modelo inteiro.
//
// Desde 20/09/2026 a web está LIBERADA nas duas condições, então isto deixou de
// ser marca de violação e passou a ser registro descritivo. Para as 24 execuções
// de medição, que rodaram com web bloqueada, continua sendo prova de isolamento
// — e é por isso que o número precisa estar certo nelas também.

export const ENDERECO_LOCAL =
  /^(localhost|127(\.\d+){3}|\[::1\]|0\.0\.0\.0|host\.docker\.internal)(:\d+)?$/i;

// Verbo de rede. Sem um destes, URL no comando é texto, não tráfego.
// `git` só conta nos subcomandos que falam com o remoto; `git add` e `git
// commit` não são rede.
export const COMANDO_REDE =
  /(^|[\s;|&(])(curl|wget|aria2c|nc|ncat|telnet|ssh|scp|sftp|lftp|ftp)\b|(^|[\s;|&(])git\s+(clone|fetch|pull|push|ls-remote)\b|\bnpm\s+(install|i|add)\s+https?:|\bpip3?\s+install\s+https?:/i;

// `cat <<'EOF' ... EOF` vira `<<HEREDOC`: o que é escrito em arquivo não é
// tráfego, e é justamente onde moram URLs de namespace, schema e documentação.
export const semHeredoc = (c) =>
  c.replace(/<<-?\s*(['"]?)([A-Za-z_]\w*)\1[\s\S]*?^[ \t]*\2[ \t]*$/gm, "<<HEREDOC");

export function externo(comando) {
  const limpo = semHeredoc(comando);
  if (!COMANDO_REDE.test(limpo)) return false;
  const urls = limpo.match(/\bhttps?:\/\/[^\s"'`)]+/gi) ?? [];
  const alvos = urls.filter((u) => {
    try { return !ENDERECO_LOCAL.test(new URL(u).host); } catch { return true; }
  });
  if (alvos.length) return true;
  // Verbo de rede sem URL explícita: só é suspeito se não citar endereço local.
  return !urls.length && !/\b(localhost|127\.0\.0\.1|::1|host\.docker\.internal)\b/i.test(limpo);
}
