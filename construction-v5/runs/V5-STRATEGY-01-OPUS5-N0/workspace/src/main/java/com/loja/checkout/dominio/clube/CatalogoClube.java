package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Catalogo;
import java.util.List;
import org.springframework.stereotype.Component;

/** Catalogo dos niveis do clube da loja. */
@Component
public class CatalogoClube extends Catalogo<NivelClube> {

    public CatalogoClube(List<NivelClube> niveis) {
        super(niveis, NivelClube::codigo);
    }
}
