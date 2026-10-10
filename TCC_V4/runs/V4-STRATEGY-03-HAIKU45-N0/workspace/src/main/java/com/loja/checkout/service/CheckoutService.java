package com.loja.checkout.service;

import com.loja.checkout.dto.ItemRequisicao;
import com.loja.checkout.dto.RequisicaoCheckout;
import com.loja.checkout.dto.RespostaCheckout;
import com.loja.checkout.dto.RespostaErro;
import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.model.ModalidadeEntrega;
import com.loja.checkout.model.NivelClube;
import com.loja.checkout.model.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CheckoutService {

    private static final BigDecimal SCALE = new BigDecimal("0.01");

    public Object calcularResumo(RequisicaoCheckout requisicao) {
        var validacao = validar(requisicao);
        if (validacao != null) {
            return validacao;
        }

        var nivel = NivelClube.valueOf(requisicao.getNivelClube());
        var regiao = Regiao.valueOf(requisicao.getRegiao());
        var modalidade = ModalidadeEntrega.valueOf(requisicao.getModalidadeEntrega());
        var formaPagamento = FormaPagamento.valueOf(requisicao.getFormaPagamento());
        var parcelas = requisicao.getParcelas() != null ? requisicao.getParcelas() : 1;

        var subtotalProdutos = calcularSubtotal(requisicao.getItens());

        var descontoCupom = calcularDescontoCupom(requisicao.getCupom(), subtotalProdutos, requisicao.getItens());

        var frete = calcularFrete(modalidade, requisicao.getItens(), nivel);

        var seguro = calcularSeguro(regiao, subtotalProdutos);

        var totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);

        var ajustePagamento = calcularAjustePagamento(formaPagamento, totalPedido, parcelas);

        var totalFinal = totalPedido.add(ajustePagamento);

        var valorParcela = calcularValorParcela(formaPagamento, totalFinal, parcelas);

        var creditoProximaCompra = calcularCredito(nivel, subtotalProdutos);

        var brinde = nivel.temBrinde(subtotalProdutos);

        var resposta = new RespostaCheckout();
        resposta.setSubtotalProdutos(subtotalProdutos);
        resposta.setDescontoCupom(descontoCupom);
        resposta.setFrete(frete);
        resposta.setPrazoEntregaDias(modalidade.getPrazo());
        resposta.setSeguro(seguro);
        resposta.setAjustePagamento(ajustePagamento);
        resposta.setTotalFinal(totalFinal);
        resposta.setParcelas(parcelas);
        resposta.setValorParcela(valorParcela);
        resposta.setCreditoProximaCompra(creditoProximaCompra);
        resposta.setBrinde(brinde);

        return resposta;
    }

    private RespostaErro validar(RequisicaoCheckout requisicao) {
        if (requisicao.getItens() == null || requisicao.getItens().isEmpty()) {
            return new RespostaErro("PEDIDO_INVALIDO");
        }

        for (var item : requisicao.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                return new RespostaErro("PEDIDO_INVALIDO");
            }
            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                return new RespostaErro("PEDIDO_INVALIDO");
            }
            if (item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) < 0) {
                return new RespostaErro("PEDIDO_INVALIDO");
            }
        }

        if (requisicao.getNivelClube() == null) {
            return new RespostaErro("NIVEL_CLUBE_INVALIDO");
        }
        try {
            NivelClube.valueOf(requisicao.getNivelClube());
        } catch (IllegalArgumentException e) {
            return new RespostaErro("NIVEL_CLUBE_INVALIDO");
        }

        if (requisicao.getRegiao() == null) {
            return new RespostaErro("REGIAO_INVALIDA");
        }
        try {
            Regiao.valueOf(requisicao.getRegiao());
        } catch (IllegalArgumentException e) {
            return new RespostaErro("REGIAO_INVALIDA");
        }

        if (requisicao.getModalidadeEntrega() == null) {
            return new RespostaErro("MODALIDADE_INVALIDA");
        }
        try {
            var modalidade = ModalidadeEntrega.valueOf(requisicao.getModalidadeEntrega());
            var pesoTotal = calcularPesoTotal(requisicao.getItens());
            if (modalidade == ModalidadeEntrega.MOTOBOY && pesoTotal.compareTo(new BigDecimal("5")) > 0) {
                return new RespostaErro("MODALIDADE_INDISPONIVEL");
            }
        } catch (IllegalArgumentException e) {
            return new RespostaErro("MODALIDADE_INVALIDA");
        }

        var cupom = requisicao.getCupom();
        if (cupom != null && !cupom.isEmpty()) {
            if (!isCupomValido(cupom)) {
                return new RespostaErro("CUPOM_INVALIDO");
            }
            var subtotal = calcularSubtotal(requisicao.getItens());
            if (!isCupomAplicavel(cupom, subtotal)) {
                return new RespostaErro("CUPOM_NAO_APLICAVEL");
            }
        }

        if (requisicao.getFormaPagamento() == null) {
            return new RespostaErro("FORMA_PAGAMENTO_INVALIDA");
        }
        try {
            FormaPagamento.valueOf(requisicao.getFormaPagamento());
        } catch (IllegalArgumentException e) {
            return new RespostaErro("FORMA_PAGAMENTO_INVALIDA");
        }

        var parcelas = requisicao.getParcelas() != null ? requisicao.getParcelas() : 1;
        var formaPagamento = FormaPagamento.valueOf(requisicao.getFormaPagamento());
        if (!isParcelamentoValido(formaPagamento, parcelas)) {
            return new RespostaErro("PARCELAMENTO_INVALIDO");
        }

        var subtotal = calcularSubtotal(requisicao.getItens());
        var descontoCupom = calcularDescontoCupom(requisicao.getCupom(), subtotal, requisicao.getItens());
        var frete = calcularFrete(
            ModalidadeEntrega.valueOf(requisicao.getModalidadeEntrega()),
            requisicao.getItens(),
            NivelClube.valueOf(requisicao.getNivelClube())
        );
        var seguro = calcularSeguro(Regiao.valueOf(requisicao.getRegiao()), subtotal);
        var totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (formaPagamento == FormaPagamento.BOLETO && totalPedido.compareTo(new BigDecimal("1000.00")) > 0) {
            return new RespostaErro("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        return null;
    }

    private BigDecimal calcularSubtotal(java.util.List<ItemRequisicao> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (var item : itens) {
            var precoItem = item.getPrecoUnitario()
                .multiply(new BigDecimal(item.getQuantidade()))
                .setScale(2, RoundingMode.HALF_EVEN);
            subtotal = subtotal.add(precoItem);
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotal, java.util.List<ItemRequisicao> itens) {
        if (cupom == null || cupom.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return switch (cupom) {
            case "BEMVINDO10" -> subtotal.multiply(new BigDecimal("0.10"))
                .setScale(2, RoundingMode.HALF_EVEN);
            case "MENOS50" -> new BigDecimal("50.00");
            case "FRETEGRATIS" -> BigDecimal.ZERO;
            case "LEVE3PAGUE2" -> calcularDescontoLeve3Pague2(itens);
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal calcularDescontoLeve3Pague2(java.util.List<ItemRequisicao> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (var item : itens) {
            var itensGratis = item.getQuantidade() / 3;
            var descontoItem = item.getPrecoUnitario()
                .multiply(new BigDecimal(itensGratis))
                .setScale(2, RoundingMode.HALF_EVEN);
            desconto = desconto.add(descontoItem);
        }
        return desconto.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, java.util.List<ItemRequisicao> itens, NivelClube nivel) {
        if (nivel.temFreteGratis()) {
            return BigDecimal.ZERO;
        }

        if (modalidade == ModalidadeEntrega.RETIRADA_LOJA) {
            return BigDecimal.ZERO;
        }

        var pesoTotal = calcularPesoTotal(itens);
        var frete = modalidade.getBaseValue()
            .add(modalidade.getPerKgValue().multiply(pesoTotal))
            .setScale(2, RoundingMode.HALF_EVEN);
        return frete;
    }

    private BigDecimal calcularPesoTotal(java.util.List<ItemRequisicao> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (var item : itens) {
            peso = peso.add(item.getPesoKg().multiply(new BigDecimal(item.getQuantidade())));
        }
        return peso;
    }

    private BigDecimal calcularSeguro(Regiao regiao, BigDecimal subtotal) {
        return subtotal.multiply(regiao.getPercentualSeguro())
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularAjustePagamento(FormaPagamento forma, BigDecimal totalPedido, Integer parcelas) {
        return switch (forma) {
            case PIX -> totalPedido.multiply(new BigDecimal("-0.05"))
                .setScale(2, RoundingMode.HALF_EVEN);
            case BOLETO -> new BigDecimal("3.49");
            case CARTAO -> {
                if (parcelas <= 3) {
                    yield BigDecimal.ZERO;
                } else {
                    var taxaMensal = new BigDecimal("0.0199");
                    var numerador = totalPedido.multiply(taxaMensal);
                    var denominador = BigDecimal.ONE
                        .subtract(BigDecimal.ONE.add(taxaMensal).pow(-parcelas, new java.math.MathContext(10)));
                    var valorParcela = numerador.divide(denominador, 10, RoundingMode.HALF_EVEN)
                        .setScale(2, RoundingMode.HALF_EVEN);
                    var totalFinal = valorParcela.multiply(new BigDecimal(parcelas))
                        .setScale(2, RoundingMode.HALF_EVEN);
                    yield totalFinal.subtract(totalPedido);
                }
            }
        };
    }

    private BigDecimal calcularValorParcela(FormaPagamento forma, BigDecimal totalFinal, Integer parcelas) {
        if (forma == FormaPagamento.CARTAO && parcelas > 3) {
            return totalFinal.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
        }
        return totalFinal.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularCredito(NivelClube nivel, BigDecimal subtotal) {
        return subtotal.multiply(nivel.getPercentualCredito())
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    private boolean isCupomValido(String cupom) {
        return cupom.equals("BEMVINDO10") || cupom.equals("MENOS50") ||
               cupom.equals("FRETEGRATIS") || cupom.equals("LEVE3PAGUE2");
    }

    private boolean isCupomAplicavel(String cupom, BigDecimal subtotal) {
        return switch (cupom) {
            case "BEMVINDO10" -> true;
            case "MENOS50" -> subtotal.compareTo(new BigDecimal("300.00")) >= 0;
            case "FRETEGRATIS" -> true;
            case "LEVE3PAGUE2" -> true;
            default -> false;
        };
    }

    private boolean isParcelamentoValido(FormaPagamento forma, Integer parcelas) {
        return switch (forma) {
            case PIX -> parcelas == 1;
            case BOLETO -> parcelas == 1;
            case CARTAO -> parcelas >= 1 && parcelas <= 12;
        };
    }
}
