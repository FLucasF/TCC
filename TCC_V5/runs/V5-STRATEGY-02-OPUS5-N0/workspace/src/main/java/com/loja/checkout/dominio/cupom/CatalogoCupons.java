package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Catalogo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogoCupons extends Catalogo<PoliticaCupom> {

    public CatalogoCupons(List<PoliticaCupom> politicas) {
        super(politicas);
    }
}
