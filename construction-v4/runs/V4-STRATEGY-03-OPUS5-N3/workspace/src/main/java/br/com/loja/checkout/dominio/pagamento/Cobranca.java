package br.com.loja.checkout.dominio.pagamento;

import br.com.loja.checkout.dominio.Dinheiro;

/** Como o pedido vai ser cobrado: o valor final e o valor de cada parcela. */
public record Cobranca(Dinheiro totalFinal, Dinheiro valorParcela) {
}
