package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Catalogo;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CatalogoDePagamentos extends Catalogo<FormaPagamento> {

    public CatalogoDePagamentos(List<FormaPagamento> formas) {
        super(formas);
    }
}
