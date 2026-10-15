package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        validarPedido(request);

        NivelClube nivelClube = validarNivelClube(request.getNivelClube());
        Regiao regiao = validarRegiao(request.getRegiao());
        ModalidadeEntrega modalidade = validarModalidadeEntrega(request.getModalidadeEntrega());

        BigDecimal pesoTotal = calcularPesoTotal(request.getItens());

        validarModalidadeDisponivel(modalidade, pesoTotal);

        Cupom cupom = validarCupom(request.getCupom());

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.getItens());

        validarCupomAplicavel(cupom, subtotalProdutos, request.getItens());

        FormaPagamento formaPagamento = validarFormaPagamento(request.getFormaPagamento());
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

        validarParcelamento(formaPagamento, parcelas);

        BigDecimal frete = calcularFrete(modalidade, pesoTotal, nivelClube);

        BigDecimal descontoCupom = cupom != null
                ? cupom.calcularDesconto(subtotalProdutos, frete, request.getItens())
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

        if (cupom == Cupom.FRETEGRATIS) {
            frete = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }

        BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);

        BigDecimal totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);

        validarFormaPagamentoDisponivel(formaPagamento, totalPedido);

        ResultadoPagamento resultadoPagamento = calcularPagamento(formaPagamento, totalPedido, parcelas);

        BigDecimal creditoProximaCompra = calcularCredito(subtotalProdutos, nivelClube);

        boolean ganhaBrinde = nivelClube.ganhaBrinde(subtotalProdutos);

        return new CheckoutResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.getPrazoEntregaDias(),
                seguro,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                creditoProximaCompra,
                ganhaBrinde
        );
    }

    private void validarPedido(CheckoutRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemRequest item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private NivelClube validarNivelClube(String nivel) {
        if (nivel == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        try {
            return NivelClube.valueOf(nivel);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private Regiao validarRegiao(String regiao) {
        if (regiao == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega validarModalidadeEntrega(String modalidade) {
        if (modalidade == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        try {
            return ModalidadeEntrega.valueOf(modalidade);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
    }

    private void validarModalidadeDisponivel(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        if (!modalidade.aceitaPeso(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private Cupom validarCupom(String cupom) {
        if (cupom == null || cupom.isEmpty()) {
            return null;
        }
        try {
            return Cupom.valueOf(cupom);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
    }

    private void validarCupomAplicavel(Cupom cupom, BigDecimal subtotalProdutos, List<ItemRequest> itens) {
        if (cupom != null && !cupom.aplicavel(subtotalProdutos, itens)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
    }

    private FormaPagamento validarFormaPagamento(String formaPagamento) {
        if (formaPagamento == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        try {
            return FormaPagamento.valueOf(formaPagamento);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private void validarParcelamento(FormaPagamento formaPagamento, int parcelas) {
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    private void validarFormaPagamentoDisponivel(FormaPagamento formaPagamento, BigDecimal totalPedido) {
        if (formaPagamento == FormaPagamento.BOLETO &&
            totalPedido.compareTo(new BigDecimal("1000.00")) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        return itens.stream()
                .map(item -> item.getPesoKg().multiply(BigDecimal.valueOf(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemRequest> itens) {
        BigDecimal subtotal = itens.stream()
                .map(item -> item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, BigDecimal pesoTotal, NivelClube nivelClube) {
        if (nivelClube.isFreteGratis()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }
        return modalidade.calcularFrete(pesoTotal).setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, Regiao regiao) {
        return subtotalProdutos.multiply(regiao.getPercentualSeguro())
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    private ResultadoPagamento calcularPagamento(FormaPagamento formaPagamento, BigDecimal totalPedido, int parcelas) {
        return switch (formaPagamento) {
            case PIX -> {
                BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05"))
                        .setScale(2, RoundingMode.HALF_EVEN);
                BigDecimal totalFinal = totalPedido.subtract(desconto);
                BigDecimal ajuste = totalFinal.subtract(totalPedido);
                yield new ResultadoPagamento(totalFinal, totalFinal, ajuste);
            }
            case BOLETO -> {
                BigDecimal tarifa = new BigDecimal("3.49");
                BigDecimal totalFinal = totalPedido.add(tarifa);
                yield new ResultadoPagamento(totalFinal, totalFinal, tarifa);
            }
            case CARTAO -> {
                if (parcelas <= 3) {
                    BigDecimal valorParcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
                    BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
                    BigDecimal ajuste = totalFinal.subtract(totalPedido);
                    yield new ResultadoPagamento(totalFinal, valorParcela, ajuste);
                } else {
                    BigDecimal taxa = new BigDecimal("0.0199");
                    BigDecimal fator = BigDecimal.ONE.add(taxa).pow(parcelas);
                    BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, 10, RoundingMode.HALF_EVEN));
                    BigDecimal valorParcela = totalPedido.multiply(taxa)
                            .divide(denominador, 2, RoundingMode.HALF_EVEN);
                    BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
                    BigDecimal ajuste = totalFinal.subtract(totalPedido);
                    yield new ResultadoPagamento(totalFinal, valorParcela, ajuste);
                }
            }
        };
    }

    private BigDecimal calcularCredito(BigDecimal subtotalProdutos, NivelClube nivelClube) {
        return subtotalProdutos.multiply(nivelClube.getPercentualCredito())
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    private record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela, BigDecimal ajuste) {}

    public static class CheckoutException extends RuntimeException {
        private final String codigo;

        public CheckoutException(String codigo) {
            super(codigo);
            this.codigo = codigo;
        }

        public String getCodigo() {
            return codigo;
        }
    }
}
