package br.com.loja.checkout.clube;

import br.com.loja.checkout.resumo.Codificado;
import java.math.BigDecimal;

/** Cada nível tem seu conjunto de vantagens, calculado sobre o valor dos produtos. */
public interface NivelClube extends Codificado {

    Vantagens vantagens(BigDecimal subtotalProdutos);
}
