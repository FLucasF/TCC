# Desenho

## O que muda de caso para caso
- Entrega (entra opção nova toda semana): disponibilidade, frete, prazo.
- Cupom (marketing cria sempre): condição de aplicação e valor do desconto.
- Nível do clube (virão novos): crédito, frete cobrado, brinde.
- Forma de pagamento: parcelas permitidas, disponibilidade, valor final e parcela.

## O que é igual
- Ordem do cálculo, arredondamento meio-para-o-par a cada etapa, ordem dos erros.
- Seguro por região: só o percentual muda -> enum `Regiao` com percentual, sem estratégia.

## Estrutura
- Uma interface por eixo variável (`Entrega`, `Cupom`, `NivelClube`, `FormaPagamento`), um `@Component` por caso.
  Cada serviço indexa os beans por código; achar o caso é busca em mapa, sem cadeia de if.
  Opção nova = classe nova, nada mais muda.
- Assinaturas olhando o caso mais exigente: `Cupom` recebe `Carrinho` (itens, subtotal, peso) e o frete
  (FRETEGRATIS); `Entrega` recebe `Carrinho`; `FormaPagamento` recebe total + parcelas.
- `ResumoService` orquestra o fluxo fixo; `ErroNegocio(codigo)` vira `{"erro": codigo}` com HTTP 400.
- Request usa String para enums para que valor desconhecido vire código de erro, não falha de JSON.
