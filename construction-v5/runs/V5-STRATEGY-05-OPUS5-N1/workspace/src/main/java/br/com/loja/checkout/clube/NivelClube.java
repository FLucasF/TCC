package br.com.loja.checkout.clube;

import br.com.loja.checkout.comum.Codificado;
import java.math.BigDecimal;

/**
 * Um nível do clube e as vantagens dele. Nível novo = uma classe nova que
 * implementa isto.
 */
public interface NivelClube extends Codificado {

    BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    boolean isentaFrete();

    boolean ganhaBrinde(BigDecimal subtotalProdutos);
}
