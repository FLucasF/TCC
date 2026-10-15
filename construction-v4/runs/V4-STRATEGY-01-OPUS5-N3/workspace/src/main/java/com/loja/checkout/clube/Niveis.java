package com.loja.checkout.clube;

import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.Erro;
import com.loja.checkout.dominio.PedidoRecusadoException;

import java.util.List;

/** Os niveis do clube. Nivel novo entra nesta lista. */
public final class Niveis {

    private static final Catalogo<NivelClube> CATALOGO = Catalogo.de(List.of(
            new Bronze(),
            new Prata(),
            new Ouro()
    ), NivelClube::codigo);

    private Niveis() {
    }

    public static NivelClube exigir(String codigo) {
        return CATALOGO.buscar(codigo)
                .orElseThrow(() -> new PedidoRecusadoException(Erro.NIVEL_CLUBE_INVALIDO));
    }
}
