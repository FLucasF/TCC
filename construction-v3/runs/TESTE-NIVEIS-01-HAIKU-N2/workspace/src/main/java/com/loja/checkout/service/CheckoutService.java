package com.loja.checkout.service;

import com.loja.checkout.enums.Cupom;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.Regiao;
import com.loja.checkout.model.CheckoutRequest;
import com.loja.checkout.model.CheckoutResponse;
import com.loja.checkout.model.ItemCarrinho;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CheckoutService {

    private static final BigDecimal SCALE = BigDecimal.TEN.pow(2);
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public Object calcularResumo(CheckoutRequest request) {
        String erro = validarPedido(request);
        if (erro != null) {
            return new com.loja.checkout.model.ErrorResponse(erro);
        }

        BigDecimal subtotalProdutos = calcularSubtotal(request);

        boolean temCupom = request.getCupom() != null && !request.getCupom().isEmpty();
        BigDecimal descontoCupom = BigDecimal.ZERO;
        if (temCupom) {
            descontoCupom = calcularDescontoCupom(request, subtotalProdutos);
        }

        BigDecimal totalSemFrete = subtrair(subtotalProdutos, descontoCupom);

        BigDecimal frete = BigDecimal.ZERO;
        Integer prazoEntrega = 0;
        boolean ouroNaoPagaFrete = request.getNivelClube() == NivelClube.OURO;

        if (!ouroNaoPagaFrete) {
            frete = calcularFrete(request, subtotalProdutos);
        }
        prazoEntrega = getPrazoEntrega(request.getModalidadeEntrega());

        BigDecimal seguro = calcularSeguro(subtotalProdutos, request.getRegiao());

        BigDecimal totalPedido = somar(totalSemFrete, frete, seguro);

        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        boolean temJuros = request.getFormaPagamento() == FormaPagamento.CARTAO && parcelas > 3;

        BigDecimal valorParcela;
        BigDecimal ajustePagamento;
        BigDecimal totalFinal;

        if (temJuros) {
            valorParcela = calcularParcelaComJuros(totalPedido, parcelas);
            BigDecimal totalComJuros = valorParcela.multiply(new BigDecimal(parcelas));
            ajustePagamento = subtrair(totalComJuros, totalPedido);
            totalFinal = totalComJuros;
        } else {
            ajustePagamento = calcularAjustePagamento(request, totalPedido, parcelas);
            totalFinal = somar(totalPedido, ajustePagamento);
            valorParcela = arredondar(totalFinal.divide(new BigDecimal(parcelas), 20, RoundingMode.HALF_EVEN));
        }

        BigDecimal creditoProximaCompra = calcularCreditoClube(request, subtotalProdutos);

        boolean brinde = request.getNivelClube() == NivelClube.OURO &&
                        subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;

        return new CheckoutResponse(
            arredondar(subtotalProdutos),
            arredondar(descontoCupom),
            arredondar(frete),
            prazoEntrega,
            arredondar(seguro),
            arredondar(ajustePagamento),
            arredondar(totalFinal),
            parcelas,
            arredondar(valorParcela),
            arredondar(creditoProximaCompra),
            brinde
        );
    }

    private String validarPedido(CheckoutRequest request) {
        // 1. Carrinho vazio ou item inválido
        if (request.getItens() == null || request.getItens().isEmpty()) {
            return "PEDIDO_INVALIDO";
        }

        for (ItemCarrinho item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getQuantidade() == null ||
                item.getPesoKg() == null ||
                item.getPrecoUnitario().signum() <= 0 ||
                item.getQuantidade() <= 0 ||
                item.getPesoKg().signum() < 0) {
                return "PEDIDO_INVALIDO";
            }
        }

        // 2. Nível do clube
        if (request.getNivelClube() == null) {
            return "NIVEL_CLUBE_INVALIDO";
        }

        // 3. Região
        if (request.getRegiao() == null) {
            return "REGIAO_INVALIDA";
        }

        // 4 e 5. Modalidade de entrega
        if (request.getModalidadeEntrega() == null) {
            return "MODALIDADE_INVALIDA";
        }

        BigDecimal pesoTotal = calcularPesoTotal(request);
        if (request.getModalidadeEntrega() == ModalidadeEntrega.MOTOBOY &&
            pesoTotal.compareTo(new BigDecimal("5")) > 0) {
            return "MODALIDADE_INDISPONIVEL";
        }

        // 6 e 7. Cupom
        if (request.getCupom() != null && !request.getCupom().isEmpty()) {
            Cupom cupomEnum = null;
            try {
                cupomEnum = Cupom.valueOf(request.getCupom());
            } catch (IllegalArgumentException e) {
                return "CUPOM_INVALIDO";
            }

            BigDecimal subtotal = calcularSubtotal(request);
            if (cupomEnum == Cupom.MENOS50 && subtotal.compareTo(new BigDecimal("300.00")) < 0) {
                return "CUPOM_NAO_APLICAVEL";
            }
        }

        // 8. Forma de pagamento
        if (request.getFormaPagamento() == null) {
            return "FORMA_PAGAMENTO_INVALIDA";
        }

        // 9. Número de parcelas
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        if (request.getFormaPagamento() == FormaPagamento.PIX ||
            request.getFormaPagamento() == FormaPagamento.BOLETO) {
            if (parcelas != 1) {
                return "PARCELAMENTO_INVALIDO";
            }
        } else if (request.getFormaPagamento() == FormaPagamento.CARTAO) {
            if (parcelas < 1 || parcelas > 12) {
                return "PARCELAMENTO_INVALIDO";
            }
        }

        // 10. Boleto acima de R$ 1.000
        BigDecimal subtotal = calcularSubtotal(request);
        BigDecimal descontoCupom = BigDecimal.ZERO;
        if (request.getCupom() != null && !request.getCupom().isEmpty()) {
            descontoCupom = calcularDescontoCupom(request, subtotal);
        }
        BigDecimal totalSemFrete = subtrair(subtotal, descontoCupom);
        BigDecimal frete = calcularFrete(request, subtotal);
        BigDecimal seguro = calcularSeguro(subtotal, request.getRegiao());
        BigDecimal totalPedido = somar(totalSemFrete, frete, seguro);

        if (request.getFormaPagamento() == FormaPagamento.BOLETO &&
            totalPedido.compareTo(new BigDecimal("1000.00")) > 0) {
            return "FORMA_PAGAMENTO_INDISPONIVEL";
        }

        return null;
    }

    private BigDecimal calcularSubtotal(CheckoutRequest request) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrinho item : request.getItens()) {
            BigDecimal itemTotal = item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(itemTotal);
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularDescontoCupom(CheckoutRequest request, BigDecimal subtotalProdutos) {
        Cupom cupom = Cupom.valueOf(request.getCupom());

        switch (cupom) {
            case BEMVINDO10:
                return arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));

            case MENOS50:
                return new BigDecimal("50.00");

            case FRETEGRATIS:
                BigDecimal frete = calcularFrete(request, subtotalProdutos);
                return arredondar(frete);

            case LEVE3PAGUE2:
                return calcularDescontoLeve3Pague2(request, subtotalProdutos);

            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calcularDescontoLeve3Pague2(CheckoutRequest request, BigDecimal subtotalProdutos) {
        BigDecimal desconto = BigDecimal.ZERO;

        for (ItemCarrinho item : request.getItens()) {
            int gratuitas = item.getQuantidade() / 3;
            if (gratuitas > 0) {
                BigDecimal valorGratuitas = item.getPrecoUnitario().multiply(new BigDecimal(gratuitas));
                desconto = desconto.add(valorGratuitas);
            }
        }

        return arredondar(desconto);
    }

    private BigDecimal calcularFrete(CheckoutRequest request, BigDecimal subtotalProdutos) {
        if (request.getNivelClube() == NivelClube.OURO) {
            return BigDecimal.ZERO;
        }

        ModalidadeEntrega modalidade = request.getModalidadeEntrega();
        BigDecimal pesoTotal = calcularPesoTotal(request);

        switch (modalidade) {
            case ECONOMICA:
                return arredondar(new BigDecimal("12.00").add(pesoTotal.multiply(new BigDecimal("2.00"))));

            case EXPRESSA:
                return arredondar(new BigDecimal("25.00").add(pesoTotal.multiply(new BigDecimal("4.50"))));

            case RETIRADA_LOJA:
                return BigDecimal.ZERO;

            case MOTOBOY:
                return new BigDecimal("18.00");

            default:
                return BigDecimal.ZERO;
        }
    }

    private Integer getPrazoEntrega(ModalidadeEntrega modalidade) {
        switch (modalidade) {
            case ECONOMICA:
                return 7;
            case EXPRESSA:
                return 2;
            case RETIRADA_LOJA:
                return 1;
            case MOTOBOY:
                return 0;
            default:
                return 0;
        }
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, Regiao regiao) {
        BigDecimal percentual = getPercentualSeguro(regiao);
        return arredondar(subtotalProdutos.multiply(percentual));
    }

    private BigDecimal getPercentualSeguro(Regiao regiao) {
        switch (regiao) {
            case SUDESTE:
                return new BigDecimal("0.01");
            case SUL:
                return new BigDecimal("0.01");
            case CENTRO_OESTE:
                return new BigDecimal("0.015");
            case NORTE:
                return new BigDecimal("0.025");
            case NORDESTE:
                return new BigDecimal("0.02");
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calcularParcelaComJuros(BigDecimal total, int parcelas) {
        BigDecimal taxa = new BigDecimal("0.0199");
        BigDecimal um = BigDecimal.ONE;
        BigDecimal taxaMais1 = um.add(taxa);

        BigDecimal potencia = taxaMais1.pow(parcelas, new java.math.MathContext(20));
        BigDecimal inversa = um.divide(potencia, 20, RoundingMode.HALF_EVEN);
        BigDecimal divisor = um.subtract(inversa);

        BigDecimal valor = total.multiply(taxa).divide(divisor, 20, RoundingMode.HALF_EVEN);
        return arredondar(valor);
    }

    private BigDecimal calcularAjustePagamento(CheckoutRequest request, BigDecimal totalPedido, int parcelas) {
        switch (request.getFormaPagamento()) {
            case PIX:
                BigDecimal desconto = arredondar(totalPedido.multiply(new BigDecimal("0.05")));
                return desconto.negate();

            case BOLETO:
                return new BigDecimal("3.49");

            case CARTAO:
                return BigDecimal.ZERO;

            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calcularCreditoClube(CheckoutRequest request, BigDecimal subtotalProdutos) {
        switch (request.getNivelClube()) {
            case BRONZE:
                return BigDecimal.ZERO;
            case PRATA:
                return arredondar(subtotalProdutos.multiply(new BigDecimal("0.02")));
            case OURO:
                return arredondar(subtotalProdutos.multiply(new BigDecimal("0.05")));
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calcularPesoTotal(CheckoutRequest request) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemCarrinho item : request.getItens()) {
            BigDecimal peso = item.getPesoKg().multiply(new BigDecimal(item.getQuantidade()));
            pesoTotal = pesoTotal.add(peso);
        }
        return pesoTotal;
    }

    private BigDecimal arredondar(BigDecimal valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return valor.setScale(2, ROUNDING);
    }

    private BigDecimal somar(BigDecimal... valores) {
        BigDecimal resultado = BigDecimal.ZERO;
        for (BigDecimal valor : valores) {
            resultado = resultado.add(valor);
        }
        return resultado;
    }

    private BigDecimal subtrair(BigDecimal minuendo, BigDecimal subtraendo) {
        return minuendo.subtract(subtraendo);
    }
}
