package com.loja.checkout.clube;

import com.loja.checkout.dominio.Catalogo;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CatalogoNiveisClube extends Catalogo<NivelClube> {

    public CatalogoNiveisClube(List<NivelClube> niveis) {
        super(niveis);
    }
}
