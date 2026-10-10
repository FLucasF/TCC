package com.loja.checkout.clube;

import com.loja.checkout.dominio.Identificavel;
import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/** Um nivel do clube da loja, com as vantagens dele. Uma implementacao por nivel. */
public interface NivelClube extends Identificavel {

    /** O frete que o cliente deste nivel paga, a partir do frete da modalidade. */
    BigDecimal frete(Pedido pedido, BigDecimal freteDaModalidade);

    /** O credito que fica guardado para a proxima compra. */
    BigDecimal creditoProximaCompra(Pedido pedido);

    boolean brinde(Pedido pedido);
}
