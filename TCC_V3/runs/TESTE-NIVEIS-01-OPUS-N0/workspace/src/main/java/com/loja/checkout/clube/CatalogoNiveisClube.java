package com.loja.checkout.clube;

import com.loja.checkout.comum.Catalogo;
import com.loja.checkout.comum.CodigoErro;
import java.util.List;
import org.springframework.stereotype.Component;

/** Todos os niveis do clube da loja. */
@Component
public class CatalogoNiveisClube extends Catalogo<NivelClube> {

    public CatalogoNiveisClube(List<NivelClube> niveis) {
        super(niveis, CodigoErro.NIVEL_CLUBE_INVALIDO);
    }
}
