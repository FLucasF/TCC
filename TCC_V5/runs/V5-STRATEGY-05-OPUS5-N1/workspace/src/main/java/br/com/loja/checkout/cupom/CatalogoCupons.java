package br.com.loja.checkout.cupom;

import br.com.loja.checkout.comum.Catalogo;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CatalogoCupons extends Catalogo<Cupom> {

    public CatalogoCupons(List<Cupom> cupons) {
        super(cupons);
    }
}
