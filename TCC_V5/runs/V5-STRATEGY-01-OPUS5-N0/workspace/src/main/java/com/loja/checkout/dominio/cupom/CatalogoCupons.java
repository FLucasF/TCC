package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Catalogo;
import java.util.List;
import org.springframework.stereotype.Component;

/** Catalogo dos cupons que valem hoje. */
@Component
public class CatalogoCupons extends Catalogo<Cupom> {

    public CatalogoCupons(List<Cupom> cupons) {
        super(cupons, Cupom::codigo);
    }
}
