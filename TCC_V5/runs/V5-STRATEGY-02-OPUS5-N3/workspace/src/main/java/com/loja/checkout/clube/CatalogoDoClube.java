package com.loja.checkout.clube;

import com.loja.checkout.dominio.Catalogo;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CatalogoDoClube extends Catalogo<NivelClube> {

    public CatalogoDoClube(List<NivelClube> niveis) {
        super(niveis);
    }
}
