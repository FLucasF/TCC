package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Catalogo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogoCupons extends Catalogo<Cupom> {

    public CatalogoCupons(List<Cupom> cupons) {
        super(cupons);
    }
}
