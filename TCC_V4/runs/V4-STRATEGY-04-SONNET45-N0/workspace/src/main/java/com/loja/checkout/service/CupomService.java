package com.loja.checkout.service;

import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.exception.CheckoutException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class CupomService {

    private static final Map<String, String> CUPONS_VALIDOS = Map.of(
        "BEMVINDO10", "BEMVINDO10",
        "MENOS50", "MENOS50",
        "FRETEGRATIS", "FRETEGRATIS",
        "LEVE3PAGUE2", "LEVE3PAGUE2"
    );

    public void validarCupom(String cupom, BigDecimal subtotalProdutos) {
        if (cupom == null || cupom.isBlank()) {
            return;
        }

        if (!CUPONS_VALIDOS.containsKey(cupom)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }

        if ("MENOS50".equals(cupom) && subtotalProdutos.compareTo(new BigDecimal("300.00")) < 0) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
    }

    public BigDecimal calcularDesconto(String cupom, BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        if (cupom == null || cupom.isBlank()) {
            return new BigDecimal("0.00");
        }

        return switch (cupom) {
            case "BEMVINDO10" -> subtotalProdutos.multiply(new BigDecimal("0.10"))
                .setScale(2, RoundingMode.HALF_EVEN);
            case "MENOS50" -> new BigDecimal("50.00");
            case "FRETEGRATIS" -> frete;
            case "LEVE3PAGUE2" -> calcularDescontoLeve3Pague2(itens);
            default -> new BigDecimal("0.00");
        };
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<ItemCarrinho> itens) {
        BigDecimal desconto = new BigDecimal("0.00");

        for (ItemCarrinho item : itens) {
            int quantidade = item.getQuantidade();
            int unidadesGratis = quantidade / 3;

            if (unidadesGratis > 0) {
                BigDecimal descontoItem = item.getPrecoUnitario()
                    .multiply(new BigDecimal(unidadesGratis))
                    .setScale(2, RoundingMode.HALF_EVEN);
                desconto = desconto.add(descontoItem);
            }
        }

        return desconto;
    }
}
