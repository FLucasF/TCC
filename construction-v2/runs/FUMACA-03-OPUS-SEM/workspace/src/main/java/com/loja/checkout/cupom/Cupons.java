package com.loja.checkout.cupom;

import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Catalogo dos cupons que valem hoje. */
@Component
public class Cupons {

    private final Map<String, Cupom> porCodigo = new HashMap<>();

    public Cupons(List<Cupom> cupons) {
        for (Cupom cupom : cupons) {
            porCodigo.put(cupom.codigo(), cupom);
        }
    }

    /** Resolve o codigo digitado pelo cliente e confere a condicao da promocao. */
    public Cupom resolver(String codigo, Pedido pedido, BigDecimal frete) {
        Cupom cupom = porCodigo.get(codigo);
        if (cupom == null) {
            throw new ErroCheckout(CodigoErro.CUPOM_INVALIDO);
        }
        if (!cupom.aplicavel(pedido, frete)) {
            throw new ErroCheckout(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return cupom;
    }
}
