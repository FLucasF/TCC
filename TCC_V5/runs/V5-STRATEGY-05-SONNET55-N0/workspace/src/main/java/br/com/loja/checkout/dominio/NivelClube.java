package br.com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Nível do clube e suas vantagens.
 *
 * @param percentualCredito % dos produtos devolvido como crédito
 * @param freteGratis       se o nível nunca paga frete
 * @param brindeAcimaDe     produtos acima deste valor ganham brinde (null = nunca)
 */
public record NivelClube(String codigo, BigDecimal percentualCredito, boolean freteGratis, BigDecimal brindeAcimaDe) {

    public BigDecimal credito(BigDecimal subtotal) {
        return Dinheiro.percentual(subtotal, percentualCredito);
    }

    public boolean ganhaBrinde(BigDecimal subtotal) {
        return brindeAcimaDe != null && subtotal.compareTo(brindeAcimaDe) > 0;
    }
}
