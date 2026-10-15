package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.enums.Cupom;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class CheckoutService {

    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        validarPedido(request);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request);
        BigDecimal pesoTotal = calcularPesoTotal(request);

        validarModalidadeDisponivel(request.getModalidadeEntrega(), pesoTotal);

        Cupom cupom = validarCupom(request.getCupom(), subtotalProdutos);
        BigDecimal descontoCupom = calcularDescontoCupom(cupom, request, subtotalProdutos);

        validarFormaPagamento(request);

        BigDecimal frete = calcularFrete(request.getModalidadeEntrega(), request.getNivelClube(), pesoTotal);

        if (cupom != null && cupom.getTipo() == Cupom.TipoCupom.FRETE_GRATIS) {
            BigDecimal freteOriginal = calcularFreteOriginal(request.getModalidadeEntrega(), pesoTotal);
            descontoCupom = freteOriginal;
            frete = arredondar(BigDecimal.ZERO);
        }

        BigDecimal seguro = calcularSeguro(request.getRegiao(), subtotalProdutos);

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);

        validarFormaPagamentoDisponivel(request.getFormaPagamento(), totalPedido);

        BigDecimal ajustePagamento = calcularAjustePagamento(request.getFormaPagamento(), totalPedido);
        BigDecimal totalFinal = totalPedido.add(ajustePagamento);

        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        BigDecimal valorParcela = calcularValorParcela(request.getFormaPagamento(), totalFinal, parcelas);

        if (request.getFormaPagamento() == FormaPagamento.CARTAO && parcelas > PARCELAS_SEM_JUROS) {
            totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
            ajustePagamento = totalFinal.subtract(totalPedido);
        }

        BigDecimal creditoProximaCompra = calcularCredito(request.getNivelClube(), subtotalProdutos);
        boolean brinde = verificarBrinde(request.getNivelClube(), subtotalProdutos);

        CheckoutResponse response = new CheckoutResponse();
        response.setSubtotalProdutos(subtotalProdutos);
        response.setDescontoCupom(descontoCupom);
        response.setFrete(frete);
        response.setPrazoEntregaDias(request.getModalidadeEntrega().getPrazoEntregaDias());
        response.setSeguro(seguro);
        response.setAjustePagamento(ajustePagamento);
        response.setTotalFinal(totalFinal);
        response.setParcelas(parcelas);
        response.setValorParcela(valorParcela);
        response.setCreditoProximaCompra(creditoProximaCompra);
        response.setBrinde(brinde);

        return response;
    }

    private void validarPedido(CheckoutRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }

        if (request.getNivelClube() == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        if (request.getRegiao() == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        if (request.getModalidadeEntrega() == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
    }

    private BigDecimal calcularSubtotalProdutos(CheckoutRequest request) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrinho item : request.getItens()) {
            BigDecimal valorItem = item.getPrecoUnitario()
                .multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(valorItem);
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(CheckoutRequest request) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemCarrinho item : request.getItens()) {
            BigDecimal pesoItem = item.getPesoKg()
                .multiply(new BigDecimal(item.getQuantidade()));
            pesoTotal = pesoTotal.add(pesoItem);
        }
        return pesoTotal;
    }

    private void validarModalidadeDisponivel(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        if (modalidade.getLimitePesoKg() != null &&
            pesoTotal.compareTo(modalidade.getLimitePesoKg()) > 0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private Cupom validarCupom(String codigoCupom, BigDecimal subtotalProdutos) {
        if (codigoCupom == null || codigoCupom.isBlank()) {
            return null;
        }

        try {
            Cupom cupom = Cupom.valueOf(codigoCupom);

            if (cupom.getValorMinimoCompra().compareTo(BigDecimal.ZERO) > 0 &&
                subtotalProdutos.compareTo(cupom.getValorMinimoCompra()) < 0) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }

            return cupom;
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
    }

    private BigDecimal calcularDescontoCupom(Cupom cupom, CheckoutRequest request, BigDecimal subtotalProdutos) {
        if (cupom == null) {
            return arredondar(BigDecimal.ZERO);
        }

        return switch (cupom.getTipo()) {
            case PERCENTUAL_PRODUTOS -> arredondar(subtotalProdutos.multiply(cupom.getValor()));
            case VALOR_FIXO_PRODUTOS -> cupom.getValor();
            case FRETE_GRATIS -> arredondar(BigDecimal.ZERO);
            case LEVE3PAGUE2 -> calcularDescontoLeve3Pague2(request);
        };
    }

    private BigDecimal calcularDescontoLeve3Pague2(CheckoutRequest request) {
        Map<String, ItemCarrinho> itensAgrupados = new HashMap<>();
        for (ItemCarrinho item : request.getItens()) {
            itensAgrupados.put(item.getNome(), item);
        }

        BigDecimal descontoTotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itensAgrupados.values()) {
            int quantidadeGratis = item.getQuantidade() / 3;
            BigDecimal descontoItem = item.getPrecoUnitario()
                .multiply(new BigDecimal(quantidadeGratis));
            descontoTotal = descontoTotal.add(descontoItem);
        }

        return arredondar(descontoTotal);
    }

    private void validarFormaPagamento(CheckoutRequest request) {
        if (request.getFormaPagamento() == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        FormaPagamento forma = request.getFormaPagamento();

        if (parcelas < forma.getParcelasMin() || parcelas > forma.getParcelasMax()) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, NivelClube nivel, BigDecimal pesoTotal) {
        if (nivel.isFreteGratis()) {
            return arredondar(BigDecimal.ZERO);
        }

        return calcularFreteOriginal(modalidade, pesoTotal);
    }

    private BigDecimal calcularFreteOriginal(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        BigDecimal frete = modalidade.getTaxaBase()
            .add(modalidade.getTaxaPorKg().multiply(pesoTotal));
        return arredondar(frete);
    }

    private BigDecimal calcularSeguro(com.loja.checkout.enums.Regiao regiao, BigDecimal subtotalProdutos) {
        BigDecimal seguro = subtotalProdutos.multiply(regiao.getPercentualSeguro());
        return arredondar(seguro);
    }

    private void validarFormaPagamentoDisponivel(FormaPagamento forma, BigDecimal total) {
        if (forma.getLimiteMaximo() != null && total.compareTo(forma.getLimiteMaximo()) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularAjustePagamento(FormaPagamento forma, BigDecimal totalPedido) {
        if (forma.isAplicaDescontoNoTotal()) {
            BigDecimal desconto = arredondar(totalPedido.multiply(forma.getPercentualDesconto()));
            return desconto.negate();
        }

        if (forma.getTarifa().compareTo(BigDecimal.ZERO) > 0) {
            return forma.getTarifa();
        }

        return arredondar(BigDecimal.ZERO);
    }

    private BigDecimal calcularValorParcela(FormaPagamento forma, BigDecimal totalFinal, int parcelas) {
        if (forma != FormaPagamento.CARTAO || parcelas <= PARCELAS_SEM_JUROS) {
            return arredondar(totalFinal.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN));
        }

        BigDecimal taxa = TAXA_JUROS_MENSAL;
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
        BigDecimal umMaisTaxaElevado = umMaisTaxa.pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(
            BigDecimal.ONE.divide(umMaisTaxaElevado, 10, RoundingMode.HALF_EVEN)
        );

        BigDecimal parcela = totalFinal.multiply(taxa).divide(denominador, 10, RoundingMode.HALF_EVEN);
        return arredondar(parcela);
    }

    private BigDecimal calcularCredito(NivelClube nivel, BigDecimal subtotalProdutos) {
        if (nivel.getPercentualCredito().compareTo(BigDecimal.ZERO) == 0) {
            return arredondar(BigDecimal.ZERO);
        }

        BigDecimal credito = subtotalProdutos.multiply(nivel.getPercentualCredito());
        return arredondar(credito);
    }

    private boolean verificarBrinde(NivelClube nivel, BigDecimal subtotalProdutos) {
        return nivel.getValorMinimoBrinde().compareTo(BigDecimal.ZERO) > 0 &&
               subtotalProdutos.compareTo(nivel.getValorMinimoBrinde()) > 0;
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
