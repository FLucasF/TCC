package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Catalogo;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CatalogoCupons extends Catalogo<Cupom> {

    public CatalogoCupons(List<Cupom> cupons) {
        super(cupons);
    }
}
