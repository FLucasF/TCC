package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * O que a forma de pagamento olha para decidir se atende e quanto ajusta.
 *
 * @param totalPedido      produtos - cupom + frete + imposto (base do ajuste de pagamento)
 * @param totalSemImposto  produtos - cupom + frete (base do limite do boleto)
 * @param parcelas         numero de parcelas escolhido
 */
public record ContextoPagamento(BigDecimal totalPedido, BigDecimal totalSemImposto, int parcelas) {
}
