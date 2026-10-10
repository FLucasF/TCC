package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.Erro;
import com.loja.checkout.dominio.PedidoRecusadoException;

import java.math.BigDecimal;
import java.util.List;

/** As opcoes de entrega oferecidas hoje. Parceria nova entra nesta lista. */
public final class Modalidades {

    private static final Catalogo<ModalidadeEntrega> CATALOGO = Catalogo.de(List.of(
            new EntregaPorPeso("ECONOMICA", new BigDecimal("12.00"), new BigDecimal("2.00"), 7),
            new EntregaPorPeso("EXPRESSA", new BigDecimal("25.00"), new BigDecimal("4.50"), 2),
            new RetiradaLoja(),
            new Motoboy()
    ), ModalidadeEntrega::codigo);

    private Modalidades() {
    }

    public static ModalidadeEntrega exigir(String codigo) {
        return CATALOGO.buscar(codigo)
                .orElseThrow(() -> new PedidoRecusadoException(Erro.MODALIDADE_INVALIDA));
    }
}
