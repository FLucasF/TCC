package com.loja.service;

import com.loja.dto.ItemPedido;
import com.loja.dto.RequisicaoCheckout;
import com.loja.dto.RespostaCheckout;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CheckoutService {
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    private static final Map<String, Integer> PRAZO_ENTREGA = Map.ofEntries(
        Map.entry("ECONOMICA", 7),
        Map.entry("EXPRESSA", 2),
        Map.entry("RETIRADA_LOJA", 1),
        Map.entry("MOTOBOY", 0)
    );

    private static final Map<String, Integer> ALIQUOTA_IMPOSTO = Map.ofEntries(
        Map.entry("SUDESTE", 12),
        Map.entry("SUL", 11),
        Map.entry("CENTRO_OESTE", 9),
        Map.entry("NORTE", 7),
        Map.entry("NORDESTE", 7)
    );

    public Object processarCheckout(RequisicaoCheckout requisicao) {
        String validacao = validarRequisicao(requisicao);
        if (validacao != null) {
            return new com.loja.dto.RespostaErro(validacao);
        }

        try {
            Integer parcelas = requisicao.getParcelas() != null ? requisicao.getParcelas() : 1;

            BigDecimal subtotalProdutos = calcularSubtotal(requisicao.getItens());
            BigDecimal freteBase = calcularFrete(requisicao.getModalidadeEntrega(),
                                                requisicao.getItens(), requisicao.getNivelClube());
            BigDecimal descontoCupom = calcularDescontoCupom(requisicao.getCupom(), subtotalProdutos, freteBase,
                                                            requisicao.getItens());
            Integer prazoEntrega = PRAZO_ENTREGA.get(requisicao.getModalidadeEntrega());

            BigDecimal baseImposto = subtotalProdutos.subtract(descontoCupom);
            BigDecimal imposto = requisicao.getRegiao() != null ?
                calcularImposto(baseImposto, requisicao.getRegiao()) : BigDecimal.ZERO;

            BigDecimal totalAntesAjuste = subtotalProdutos
                .subtract(descontoCupom)
                .add(freteBase)
                .add(imposto);

            Map<String, BigDecimal> ajuste = calcularAjustePagamento(requisicao.getFormaPagamento(),
                                                                     totalAntesAjuste, parcelas);
            BigDecimal ajustePagamento = ajuste.get("ajuste");
            BigDecimal totalFinal = ajuste.get("total");
            BigDecimal valorParcela = ajuste.get("parcela");

            BigDecimal creditoProximaCompra = calcularCredito(requisicao.getNivelClube(), subtotalProdutos);
            Boolean brinde = calcularBrinde(requisicao.getNivelClube(), subtotalProdutos);

            return new RespostaCheckout(
                subtotalProdutos, descontoCupom, freteBase, prazoEntrega, imposto,
                ajustePagamento, totalFinal, parcelas, valorParcela,
                creditoProximaCompra, brinde
            );
        } catch (Exception e) {
            return new com.loja.dto.RespostaErro("ERRO_INTERNO");
        }
    }

    private String validarRequisicao(RequisicaoCheckout requisicao) {
        if (requisicao.getItens() == null || requisicao.getItens().isEmpty()) {
            return "PEDIDO_INVALIDO";
        }

        for (ItemPedido item : requisicao.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() < 0) {
                return "PEDIDO_INVALIDO";
            }
        }

        if (requisicao.getNivelClube() == null ||
            (!requisicao.getNivelClube().equals("BRONZE") &&
             !requisicao.getNivelClube().equals("PRATA") &&
             !requisicao.getNivelClube().equals("OURO"))) {
            return "NIVEL_CLUBE_INVALIDO";
        }

        if (requisicao.getRegiao() != null && !ALIQUOTA_IMPOSTO.containsKey(requisicao.getRegiao())) {
            return "REGIAO_INVALIDA";
        }

        if (requisicao.getModalidadeEntrega() == null || !PRAZO_ENTREGA.containsKey(requisicao.getModalidadeEntrega())) {
            return "MODALIDADE_INVALIDA";
        }

        if ("MOTOBOY".equals(requisicao.getModalidadeEntrega()) && calcularPesoTotal(requisicao.getItens()) > 5) {
            return "MODALIDADE_INDISPONIVEL";
        }

        if (requisicao.getCupom() != null && !validarCupom(requisicao.getCupom())) {
            return "CUPOM_INVALIDO";
        }

        if (requisicao.getCupom() != null && !cupomAplicavel(requisicao.getCupom(), calcularSubtotal(requisicao.getItens()))) {
            return "CUPOM_NAO_APLICAVEL";
        }

        if (requisicao.getFormaPagamento() == null ||
            (!requisicao.getFormaPagamento().equals("PIX") &&
             !requisicao.getFormaPagamento().equals("CARTAO") &&
             !requisicao.getFormaPagamento().equals("BOLETO"))) {
            return "FORMA_PAGAMENTO_INVALIDA";
        }

        Integer parcelas = requisicao.getParcelas() != null ? requisicao.getParcelas() : 1;
        if (!parcelasValidas(requisicao.getFormaPagamento(), parcelas)) {
            return "PARCELAMENTO_INVALIDO";
        }

        BigDecimal totalAntesAjuste = calcularSubtotal(requisicao.getItens())
            .subtract(calcularDescontoCupom(requisicao.getCupom(), calcularSubtotal(requisicao.getItens()),
                                           calcularFrete(requisicao.getModalidadeEntrega(), requisicao.getItens(),
                                                        requisicao.getNivelClube()),
                                           requisicao.getItens()))
            .add(calcularFrete(requisicao.getModalidadeEntrega(), requisicao.getItens(), requisicao.getNivelClube()));

        if ("BOLETO".equals(requisicao.getFormaPagamento()) && totalAntesAjuste.compareTo(new BigDecimal("1000.00")) > 0) {
            return "FORMA_PAGAMENTO_INDISPONIVEL";
        }

        return null;
    }

    private boolean validarCupom(String cupom) {
        return cupom.equals("BEMVINDO10") || cupom.equals("MENOS50") ||
               cupom.equals("FRETEGRATIS") || cupom.equals("LEVE3PAGUE2");
    }

    private boolean cupomAplicavel(String cupom, BigDecimal subtotal) {
        if ("MENOS50".equals(cupom)) {
            return subtotal.compareTo(new BigDecimal("300.00")) >= 0;
        }
        return true;
    }

    private boolean parcelasValidas(String formaPagamento, Integer parcelas) {
        if ("PIX".equals(formaPagamento) || "BOLETO".equals(formaPagamento)) {
            return parcelas == 1;
        }
        if ("CARTAO".equals(formaPagamento)) {
            return parcelas >= 1 && parcelas <= 12;
        }
        return false;
    }

    private BigDecimal calcularSubtotal(List<ItemPedido> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            BigDecimal preco = new BigDecimal(item.getPrecoUnitario().toString());
            BigDecimal quantidade = new BigDecimal(item.getQuantidade());
            total = total.add(preco.multiply(quantidade));
        }
        return arredondar(total);
    }

    private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotal, BigDecimal frete, List<ItemPedido> itens) {
        if (cupom == null) {
            return BigDecimal.ZERO;
        }

        if ("BEMVINDO10".equals(cupom)) {
            return arredondar(subtotal.multiply(new BigDecimal("0.10")));
        } else if ("MENOS50".equals(cupom)) {
            return arredondar(new BigDecimal("50.00"));
        } else if ("FRETEGRATIS".equals(cupom)) {
            return arredondar(frete);
        } else if ("LEVE3PAGUE2".equals(cupom)) {
            return calcularDescontoLeve3Pague2(itens);
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<ItemPedido> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            Integer unidadesGratis = item.getQuantidade() / 3;
            BigDecimal preco = new BigDecimal(item.getPrecoUnitario().toString());
            desconto = desconto.add(preco.multiply(new BigDecimal(unidadesGratis)));
        }
        return arredondar(desconto);
    }

    private BigDecimal calcularFrete(String modalidade, List<ItemPedido> itens, String nivelClube) {
        if ("OURO".equals(nivelClube)) {
            return BigDecimal.ZERO;
        }

        Double pesoTotal = calcularPesoTotal(itens);

        if ("ECONOMICA".equals(modalidade)) {
            BigDecimal base = new BigDecimal("12.00");
            BigDecimal porKg = new BigDecimal(pesoTotal.toString()).multiply(new BigDecimal("2.00"));
            return arredondar(base.add(porKg));
        } else if ("EXPRESSA".equals(modalidade)) {
            BigDecimal base = new BigDecimal("25.00");
            BigDecimal porKg = new BigDecimal(pesoTotal.toString()).multiply(new BigDecimal("4.50"));
            return arredondar(base.add(porKg));
        } else if ("RETIRADA_LOJA".equals(modalidade)) {
            return BigDecimal.ZERO;
        } else if ("MOTOBOY".equals(modalidade)) {
            return new BigDecimal("18.00");
        }

        return BigDecimal.ZERO;
    }

    private Double calcularPesoTotal(List<ItemPedido> itens) {
        Double pesoTotal = 0.0;
        for (ItemPedido item : itens) {
            pesoTotal += item.getPesoKg() * item.getQuantidade();
        }
        return pesoTotal;
    }

    private BigDecimal calcularImposto(BigDecimal baseImposto, String regiao) {
        Integer aliquota = ALIQUOTA_IMPOSTO.get(regiao);
        BigDecimal percentual = new BigDecimal(aliquota).divide(new BigDecimal("100"), 10, RoundingMode.HALF_EVEN);
        return arredondar(baseImposto.multiply(percentual));
    }

    private Map<String, BigDecimal> calcularAjustePagamento(String formaPagamento, BigDecimal total, Integer parcelas) {
        Map<String, BigDecimal> resultado = new HashMap<>();

        if ("PIX".equals(formaPagamento)) {
            BigDecimal desconto = arredondar(total.multiply(new BigDecimal("0.05")));
            BigDecimal totalComDesconto = total.subtract(desconto);
            resultado.put("ajuste", desconto.negate());
            resultado.put("total", totalComDesconto);
            resultado.put("parcela", totalComDesconto);
        } else if ("BOLETO".equals(formaPagamento)) {
            BigDecimal taxa = new BigDecimal("3.49");
            BigDecimal totalComTaxa = total.add(taxa);
            resultado.put("ajuste", taxa);
            resultado.put("total", totalComTaxa);
            resultado.put("parcela", totalComTaxa);
        } else if ("CARTAO".equals(formaPagamento)) {
            if (parcelas <= 3) {
                BigDecimal valorParcela = arredondar(total.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN));
                resultado.put("ajuste", BigDecimal.ZERO);
                resultado.put("total", total);
                resultado.put("parcela", valorParcela);
            } else {
                BigDecimal taxa = new BigDecimal("0.0199");
                BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
                BigDecimal potencia = umMaisTaxa.pow(parcelas);
                BigDecimal inverso = BigDecimal.ONE.divide(potencia, 10, RoundingMode.HALF_EVEN);
                BigDecimal denominador = BigDecimal.ONE.subtract(inverso);
                BigDecimal numerador = total.multiply(taxa);
                BigDecimal valorParcela = arredondar(numerador.divide(denominador, 10, RoundingMode.HALF_EVEN));
                BigDecimal totalComJuros = valorParcela.multiply(new BigDecimal(parcelas));
                BigDecimal ajuste = totalComJuros.subtract(total);
                resultado.put("ajuste", ajuste);
                resultado.put("total", totalComJuros);
                resultado.put("parcela", valorParcela);
            }
        } else {
            resultado.put("ajuste", BigDecimal.ZERO);
            resultado.put("total", total);
            resultado.put("parcela", total);
        }

        return resultado;
    }

    private BigDecimal calcularCredito(String nivelClube, BigDecimal subtotal) {
        if ("BRONZE".equals(nivelClube)) {
            return BigDecimal.ZERO;
        } else if ("PRATA".equals(nivelClube)) {
            return arredondar(subtotal.multiply(new BigDecimal("0.02")));
        } else if ("OURO".equals(nivelClube)) {
            return arredondar(subtotal.multiply(new BigDecimal("0.05")));
        }
        return BigDecimal.ZERO;
    }

    private Boolean calcularBrinde(String nivelClube, BigDecimal subtotal) {
        if ("OURO".equals(nivelClube) && subtotal.compareTo(new BigDecimal("500.00")) > 0) {
            return true;
        }
        return false;
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, ROUNDING_MODE);
    }
}
