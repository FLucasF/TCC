package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.comum.Catalogo;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CatalogoFormasPagamento extends Catalogo<FormaPagamento> {

    public CatalogoFormasPagamento(List<FormaPagamento> formas) {
        super(formas);
    }
}
