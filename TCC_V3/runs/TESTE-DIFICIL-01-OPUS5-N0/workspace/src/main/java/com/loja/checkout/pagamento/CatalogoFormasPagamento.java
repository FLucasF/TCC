package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Catalogo;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CatalogoFormasPagamento extends Catalogo<FormaPagamento> {

    public CatalogoFormasPagamento(List<FormaPagamento> formas) {
        super(formas);
    }
}
