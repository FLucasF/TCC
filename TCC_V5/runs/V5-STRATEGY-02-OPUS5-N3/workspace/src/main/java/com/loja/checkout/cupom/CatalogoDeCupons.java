package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Catalogo;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CatalogoDeCupons extends Catalogo<Cupom> {

    public CatalogoDeCupons(List<Cupom> cupons) {
        super(cupons);
    }
}
