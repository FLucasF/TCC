package com.loja.service;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ItemRequest;
import com.loja.enums.FormaPagamento;
import com.loja.enums.ModalidadeEntrega;
import com.loja.enums.NivelClube;
import com.loja.enums.Regiao;
import com.loja.exception.CheckoutException;
import com.loja.model.Cupom;
import com.loja.util.ArredondamentoUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        validarRequisicao(request);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.getItens());

        NivelClube nivel = NivelClube.valueOf(request.getNivelClube());
        Regiao regiao = Regiao.valueOf(request.getRegiao());
        ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(request.getModalidadeEntrega());
        FormaPagamento forma = FormaPagamento.valueOf(request.getFormaPagamento());

        Double pesoTotal = calcularPesoTotal(request.getItens());

        String codigoCupom = request.getCupom();
        Cupom cupom = null;
        if (codigoCupom != null) {
            cupom = Cupom.fromCodigo(codigoCupom);
            if (cupom == null) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }
            validarCupom(cupom, subtotalProdutos);
        }

        BigDecimal descontoCupom = calcularDescontoCupom(cupom, subtotalProdutos, request.getItens());

        BigDecimal produtosComDesconto = ArredondamentoUtil.arredondarMeioParaPar(
            subtotalProdutos.subtract(descontoCupom)
        );

        BigDecimal frete = calcularFrete(modalidade, pesoTotal, cupom, nivel);

        BigDecimal imposto = calcularImposto(produtosComDesconto, regiao);

        Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        validarParcelamento(forma, parcelas);

        BigDecimal totalAntesAjuste = ArredondamentoUtil.arredondarMeioParaPar(
            subtotalProdutos.subtract(descontoCupom).add(frete).add(imposto)
        );

        validarFormaPagementoPraValor(forma, totalAntesAjuste);

        BigDecimal ajustePagamento = calcularAjustePagamento(forma, totalAntesAjuste, parcelas);

        BigDecimal totalFinal = ArredondamentoUtil.arredondarMeioParaPar(
            totalAntesAjuste.add(ajustePagamento)
        );

        BigDecimal valorParcela = calcularValorParcela(forma, totalFinal, parcelas);

        BigDecimal creditoProximaCompra = calcularCredito(nivel, subtotalProdutos);

        Boolean brinde = ehElegiavelBrinde(nivel, subtotalProdutos);

        CheckoutResponse response = new CheckoutResponse();
        response.setSubtotalProdutos(ArredondamentoUtil.arredondarMeioParaPar(subtotalProdutos));
        response.setDescontoCupom(descontoCupom);
        response.setFrete(frete);
        response.setPrazoEntregaDias(modalidade.getPrazoDias());
        response.setImposto(imposto);
        response.setAjustePagamento(ajustePagamento);
        response.setTotalFinal(totalFinal);
        response.setParcelas(parcelas);
        response.setValorParcela(valorParcela);
        response.setCreditoProximaCompra(creditoProximaCompra);
        response.setBrinde(brinde);

        return response;
    }

    private void validarRequisicao(CheckoutRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemRequest item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() < 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }

        if (request.getNivelClube() == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        try {
            NivelClube.valueOf(request.getNivelClube());
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        if (request.getRegiao() == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        try {
            Regiao.valueOf(request.getRegiao());
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        if (request.getModalidadeEntrega() == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        try {
            ModalidadeEntrega.valueOf(request.getModalidadeEntrega());
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        Double pesoTotal = calcularPesoTotal(request.getItens());
        ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(request.getModalidadeEntrega());
        if (pesoTotal > modalidade.getPesoMaximoKg()) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        if (request.getFormaPagamento() == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        try {
            FormaPagamento.valueOf(request.getFormaPagamento());
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private void validarCupom(Cupom cupom, BigDecimal subtotalProdutos) {
        if (cupom == null) {
            return;
        }

        if (cupom == Cupom.MENOS50) {
            if (subtotalProdutos.compareTo(new BigDecimal("300.00")) < 0) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }
    }

    private void validarParcelamento(FormaPagamento forma, Integer parcelas) {
        if (forma == FormaPagamento.PIX || forma == FormaPagamento.BOLETO) {
            if (parcelas != 1) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }

        if (forma == FormaPagamento.CARTAO) {
            if (parcelas < 1 || parcelas > 12) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }
    }

    private void validarFormaPagementoPraValor(FormaPagamento forma, BigDecimal valor) {
        if (forma == FormaPagamento.BOLETO) {
            if (valor.compareTo(new BigDecimal("1000.00")) > 0) {
                throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
            }
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemRequest> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            BigDecimal precoItem = BigDecimal.valueOf(item.getPrecoUnitario())
                .multiply(BigDecimal.valueOf(item.getQuantidade()));
            total = total.add(precoItem);
        }
        return total;
    }

    private Double calcularPesoTotal(List<ItemRequest> itens) {
        Double pesoTotal = 0.0;
        for (ItemRequest item : itens) {
            pesoTotal += item.getPesoKg() * item.getQuantidade();
        }
        return pesoTotal;
    }

    private BigDecimal calcularDescontoCupom(Cupom cupom, BigDecimal subtotal, List<ItemRequest> itens) {
        if (cupom == null) {
            return BigDecimal.ZERO;
        }

        switch (cupom) {
            case BEMVINDO10:
                return ArredondamentoUtil.arredondarMeioParaPar(
                    subtotal.multiply(new BigDecimal("0.10"))
                );

            case MENOS50:
                return ArredondamentoUtil.arredondarMeioParaPar(
                    new BigDecimal("50.00")
                );

            case FRETEGRATIS:
                return BigDecimal.ZERO;

            case LEVE3PAGUE2:
                return calcularDescontoLeve3Pague2(itens);

            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<ItemRequest> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            int unidadesGratis = item.getQuantidade() / 3;
            BigDecimal descontoItem = BigDecimal.valueOf(unidadesGratis)
                .multiply(BigDecimal.valueOf(item.getPrecoUnitario()));
            desconto = desconto.add(descontoItem);
        }
        return ArredondamentoUtil.arredondarMeioParaPar(desconto);
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, Double pesoTotal, Cupom cupom, NivelClube nivel) {
        if (nivel == NivelClube.OURO) {
            return BigDecimal.ZERO;
        }

        if (cupom == Cupom.FRETEGRATIS) {
            return BigDecimal.ZERO;
        }

        BigDecimal freteCalculado = modalidade.getValorBase()
            .add(modalidade.getValorPorKg().multiply(BigDecimal.valueOf(pesoTotal)));

        return ArredondamentoUtil.arredondarMeioParaPar(freteCalculado);
    }

    private BigDecimal calcularImposto(BigDecimal produtosComDesconto, Regiao regiao) {
        BigDecimal impostoCalculado = produtosComDesconto.multiply(regiao.getAliquota());
        return ArredondamentoUtil.arredondarMeioParaPar(impostoCalculado);
    }

    private BigDecimal calcularAjustePagamento(FormaPagamento forma, BigDecimal total, Integer parcelas) {
        switch (forma) {
            case PIX:
                BigDecimal desconto = total.multiply(new BigDecimal("0.05"));
                return ArredondamentoUtil.arredondarMeioParaPar(desconto).negate();

            case BOLETO:
                return new BigDecimal("3.49");

            case CARTAO:
                if (parcelas <= 3) {
                    return BigDecimal.ZERO;
                } else {
                    BigDecimal parcela = calcularParcelaComJuros(total, parcelas);
                    BigDecimal totalComJuros = parcela.multiply(BigDecimal.valueOf(parcelas));
                    BigDecimal ajuste = totalComJuros.subtract(total);
                    return ArredondamentoUtil.arredondarMeioParaPar(ajuste);
                }

            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calcularParcelaComJuros(BigDecimal total, Integer parcelas) {
        BigDecimal taxa = new BigDecimal("0.0199");
        BigDecimal um = BigDecimal.ONE;
        BigDecimal numerador = total.multiply(taxa);
        BigDecimal umMaisTaxa = um.add(taxa);
        BigDecimal potenciaPositiva = umMaisTaxa.pow(parcelas);
        BigDecimal potenciaInversa = um.divide(potenciaPositiva, 10, java.math.RoundingMode.HALF_EVEN);
        BigDecimal denominador = um.subtract(potenciaInversa);
        BigDecimal parcela = numerador.divide(denominador, 10, java.math.RoundingMode.HALF_EVEN);
        return parcela;
    }

    private BigDecimal calcularValorParcela(FormaPagamento forma, BigDecimal totalFinal, Integer parcelas) {
        if (parcelas == 1) {
            return totalFinal;
        }

        if (forma == FormaPagamento.CARTAO && parcelas > 3) {
            BigDecimal parcela = calcularParcelaComJuros(totalFinal, parcelas);
            return ArredondamentoUtil.arredondarMeioParaPar(parcela);
        } else {
            BigDecimal parcela = totalFinal.divide(BigDecimal.valueOf(parcelas), 10, java.math.RoundingMode.HALF_EVEN);
            return ArredondamentoUtil.arredondarMeioParaPar(parcela);
        }
    }

    private BigDecimal calcularCredito(NivelClube nivel, BigDecimal subtotal) {
        switch (nivel) {
            case BRONZE:
                return BigDecimal.ZERO;

            case PRATA:
                return ArredondamentoUtil.arredondarMeioParaPar(
                    subtotal.multiply(new BigDecimal("0.02"))
                );

            case OURO:
                return ArredondamentoUtil.arredondarMeioParaPar(
                    subtotal.multiply(new BigDecimal("0.05"))
                );

            default:
                return BigDecimal.ZERO;
        }
    }

    private Boolean ehElegiavelBrinde(NivelClube nivel, BigDecimal subtotal) {
        if (nivel != NivelClube.OURO) {
            return false;
        }
        return subtotal.compareTo(new BigDecimal("500.00")) > 0;
    }
}
