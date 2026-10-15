package com.loja.checkout.aplicacao;

import com.loja.checkout.api.ResumoRequest;
import org.springframework.stereotype.Component;

@Component
public class MontadorDeRascunho {

    private final Catalogos catalogos;

    public MontadorDeRascunho(Catalogos catalogos) {
        this.catalogos = catalogos;
    }

    public Rascunho montar(ResumoRequest pedidoBruto) {
        return new Rascunho(pedidoBruto, catalogos);
    }
}
