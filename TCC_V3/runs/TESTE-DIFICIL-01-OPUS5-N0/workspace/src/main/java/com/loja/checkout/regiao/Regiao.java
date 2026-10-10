package com.loja.checkout.regiao;

import com.loja.checkout.dominio.Identificavel;
import java.math.BigDecimal;

/**
 * Regiao onde o cliente mora. Muda apenas o percentual que a seguradora cobra.
 */
public interface Regiao extends Identificavel {

    BigDecimal percentualSeguro();
}
