// Propõe se uma execução é válida, a partir do que o meta.json já registra.
//
// Mora em módulo próprio para poder ser testado:
//   node infra/scripts/validade.teste.mjs
//
// A decisão final continua sendo humana, pela tabela de exceções da §13.3. Isto
// só evita que o campo dependa da memória: as 24 execuções de medição ficaram
// com `valida: null` porque ninguém tinha definido quem marcava.

// O alias de um modelo e o snapshot datado dele são o MESMO modelo.
//
// Nono defeito de ferramenta, achado na FUMACA-03 em 21/09/2026, e este era meu:
// pedimos `claude-haiku-4-5` e as mensagens vêm com `claude-haiku-4-5-20251001`,
// enquanto Opus 5 e Sonnet 5 reportam o id simples. A comparação estrita marcava
// as duas execuções de Haiku como troca de modelo, e elas teriam sido
// descartadas por engano — exatamente o erro mais caro que este campo pode
// cometer.
//
// `startsWith` sozinho não serve: `claude-opus-5-1` começa com `claude-opus-5` e
// é outro modelo. Só o sufixo de data de 8 dígitos é removido.
const SNAPSHOT = /-\d{8}$/;
export const normalizar = (id) => (id ?? "").replace(SNAPSHOT, "");
export const mesmoModelo = (observado, pedido) => normalizar(observado) === normalizar(pedido);

// Encerramentos que indicam falha de infraestrutura, não resultado do modelo.
// Build quebrado NÃO entra: a §13.3 diz que modelo que não entregou nada conta
// como resultado.
export const ENCERRAMENTO_INVALIDO = ["interrompido", "erro_sem_resultado", "erro", "limite_turnos"];

export function proporValida({ encerramento, modeloSolicitado, modelosObservados = [] }) {
  if (ENCERRAMENTO_INVALIDO.includes(encerramento))
    return { valida: false, motivo: `encerramento: ${encerramento}` };
  const outros = modelosObservados.filter((m) => m && !mesmoModelo(m, modeloSolicitado));
  if (outros.length)
    return { valida: false, motivo: `outro modelo observado: ${outros.join(", ")}` };
  return { valida: true, motivo: "concluido, sem troca de modelo" };
}
