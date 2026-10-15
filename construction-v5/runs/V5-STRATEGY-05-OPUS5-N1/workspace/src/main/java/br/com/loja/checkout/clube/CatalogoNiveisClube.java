package br.com.loja.checkout.clube;

import br.com.loja.checkout.comum.Catalogo;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CatalogoNiveisClube extends Catalogo<NivelClube> {

    public CatalogoNiveisClube(List<NivelClube> niveis) {
        super(niveis);
    }
}
