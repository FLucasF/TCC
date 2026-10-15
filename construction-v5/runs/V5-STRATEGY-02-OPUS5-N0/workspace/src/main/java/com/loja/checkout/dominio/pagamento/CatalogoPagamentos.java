package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Catalogo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogoPagamentos extends Catalogo<PoliticaPagamento> {

    public CatalogoPagamentos(List<PoliticaPagamento> politicas) {
        super(politicas);
    }
}
