package br.com.loja.checkout.clube;

import br.com.loja.checkout.Codificado;
import java.math.BigDecimal;

public interface NivelClube extends Codificado {

    BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    BigDecimal freteCobrado(BigDecimal frete);

    boolean brinde(BigDecimal subtotalProdutos);
}
