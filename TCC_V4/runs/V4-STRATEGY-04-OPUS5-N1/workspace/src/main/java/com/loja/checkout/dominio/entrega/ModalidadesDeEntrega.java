package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.CodigoErro;
import java.math.BigDecimal;
import java.util.List;

/** As opcoes de entrega que a loja oferece hoje. */
public final class ModalidadesDeEntrega {

    public static final Catalogo<ModalidadeEntrega> CATALOGO = Catalogo.de(
            CodigoErro.MODALIDADE_INVALIDA,
            ModalidadeEntrega::codigo,
            List.of(
                    new EntregaPorPeso("ECONOMICA", new BigDecimal("12.00"), new BigDecimal("2.00"), 7),
                    new EntregaPorPeso("EXPRESSA", new BigDecimal("25.00"), new BigDecimal("4.50"), 2),
                    new RetiradaNaLoja(),
                    new Motoboy()));

    private ModalidadesDeEntrega() {
    }
}
