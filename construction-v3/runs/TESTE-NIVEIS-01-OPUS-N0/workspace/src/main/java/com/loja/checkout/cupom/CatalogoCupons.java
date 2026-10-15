package com.loja.checkout.cupom;

import com.loja.checkout.comum.Catalogo;
import com.loja.checkout.comum.CodigoErro;
import java.util.List;
import org.springframework.stereotype.Component;

/** Todos os cupons que valem hoje. */
@Component
public class CatalogoCupons extends Catalogo<Cupom> {

    public CatalogoCupons(List<Cupom> cupons) {
        super(cupons, CodigoErro.CUPOM_INVALIDO);
    }
}
