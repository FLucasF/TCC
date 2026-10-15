package br.com.loja.checkout.entrega;

import br.com.loja.checkout.comum.Catalogo;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CatalogoModalidades extends Catalogo<ModalidadeEntrega> {

    public CatalogoModalidades(List<ModalidadeEntrega> modalidades) {
        super(modalidades);
    }
}
