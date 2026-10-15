package com.loja.checkout.service.entrega;

import java.math.BigDecimal;

/**
 * Uma forma de entrega. Novas transportadoras entram implementando esta
 * interface e registrando um componente Spring, sem precisar tocar no
 * restante do calculo.
 */
public interface EntregaCalculadora {

    String getCodigo();

    boolean disponivel(BigDecimal pesoTotalKg, BigDecimal subtotalProdutos);

    BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    int getPrazoDias();
}
