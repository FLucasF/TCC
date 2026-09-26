package br.tcc.checkout.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

import br.tcc.checkout.dto.ItemCarrinho;
import br.tcc.checkout.dto.ResumoCheckoutRequest;
import br.tcc.checkout.dto.ResumoCheckoutResponse;
import br.tcc.checkout.enums.Cupom;
import br.tcc.checkout.enums.FormaPagamento;
import br.tcc.checkout.enums.ModalidadeEntrega;
import br.tcc.checkout.exception.CheckoutException;

@Service
public class CheckoutService {
    private static final BigDecimal TAXA_JUROS_CARTAO = BigDecimal.valueOf(0.0199);
    private static final BigDecimal TAXA_PIX = BigDecimal.valueOf(0.05);
    private static final BigDecimal TARIFA_BOLETO = BigDecimal.valueOf(3.49);
    private static final BigDecimal LIMITE_BOLETO = BigDecimal.valueOf(1000.00);
    private static final BigDecimal PESO_MAXIMO_MOTOBOY = BigDecimal.valueOf(5);

    public ResumoCheckoutResponse calcularResumo(ResumoCheckoutRequest request) throws CheckoutException {
        validarPedido(request);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.getItens());

        BigDecimal descontoCupom = BigDecimal.ZERO;
        Cupom cupomAplicado = null;

        if (request.getCupom() != null && !request.getCupom().isEmpty()) {
            cupomAplicado = Cupom.fromCodigo(request.getCupom());
            if (cupomAplicado == null) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }
            descontoCupom = calcularDescontoCupom(cupomAplicado, request.getItens(), subtotalProdutos);
        }

        ModalidadeEntrega modalidadeEntrega = ModalidadeEntrega.fromCodigo(request.getModalidadeEntrega());
        if (modalidadeEntrega == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        BigDecimal pesTotal = calcularPesoTotal(request.getItens());
        validarModalidadeDisponivel(modalidadeEntrega, pesTotal);

        BigDecimal frete = calcularFrete(modalidadeEntrega, pesTotal);

        if (Cupom.FRETEGRATIS == cupomAplicado) {
            descontoCupom = frete;
        }

        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete);
        arredondarParaCentavos(totalPedido);

        FormaPagamento formaPagamento = FormaPagamento.fromCodigo(request.getFormaPagamento());
        if (formaPagamento == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        validarParcelamento(formaPagamento, parcelas);
        validarFormaPagamentoDisponivel(formaPagamento, totalPedido);

        BigDecimal ajustePagamento = calcularAjustePagamento(formaPagamento, totalPedido, parcelas);
        BigDecimal totalFinal = totalPedido.add(ajustePagamento);
        totalFinal = arredondarParaCentavos(totalFinal);

        BigDecimal valorParcela = calcularValorParcela(totalFinal, parcelas);

        return new ResumoCheckoutResponse(
            arredondarParaCentavos(subtotalProdutos),
            arredondarParaCentavos(descontoCupom),
            arredondarParaCentavos(frete),
            modalidadeEntrega.getPrazoEntregaDias(),
            arredondarParaCentavos(ajustePagamento),
            totalFinal,
            parcelas,
            valorParcela
        );
    }

    private void validarPedido(ResumoCheckoutRequest request) throws CheckoutException {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            if (item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) < 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            BigDecimal precoItem = item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(precoItem);
        }
        return subtotal;
    }

    private BigDecimal calcularDescontoCupom(Cupom cupom, List<ItemCarrinho> itens, BigDecimal subtotalProdutos) throws CheckoutException {
        switch (cupom) {
            case BEMVINDO10:
                return arredondarParaCentavos(subtotalProdutos.multiply(BigDecimal.valueOf(0.10)));

            case MENOS50:
                if (subtotalProdutos.compareTo(BigDecimal.valueOf(300.00)) < 0) {
                    throw new CheckoutException("CUPOM_NAO_APLICAVEL");
                }
                return arredondarParaCentavos(BigDecimal.valueOf(50.00));

            case FRETEGRATIS:
                return BigDecimal.ZERO;

            case LEVE3PAGUE2:
                return calcularDescontoLeve3Pague2(itens);

            default:
                throw new CheckoutException("CUPOM_INVALIDO");
        }
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<ItemCarrinho> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            int quantidade = item.getQuantidade();
            int itensGratis = quantidade / 3;
            BigDecimal descotoItem = item.getPrecoUnitario().multiply(new BigDecimal(itensGratis));
            desconto = desconto.add(descotoItem);
        }
        return arredondarParaCentavos(desconto);
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            BigDecimal pesoItem = item.getPesoKg().multiply(new BigDecimal(item.getQuantidade()));
            peso = peso.add(pesoItem);
        }
        return peso;
    }

    private void validarModalidadeDisponivel(ModalidadeEntrega modalidade, BigDecimal pesoTotal) throws CheckoutException {
        if (ModalidadeEntrega.MOTOBOY == modalidade) {
            if (pesoTotal.compareTo(PESO_MAXIMO_MOTOBOY) > 0) {
                throw new CheckoutException("MODALIDADE_INDISPONIVEL");
            }
        }
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        BigDecimal frete = modalidade.getValorBase();
        frete = frete.add(modalidade.getValorPorKg().multiply(pesoTotal));
        return arredondarParaCentavos(frete);
    }

    private void validarParcelamento(FormaPagamento formaPagamento, int parcelas) throws CheckoutException {
        if (FormaPagamento.PIX == formaPagamento || FormaPagamento.BOLETO == formaPagamento) {
            if (parcelas != 1) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        } else if (FormaPagamento.CARTAO == formaPagamento) {
            if (parcelas < 1 || parcelas > 12) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }
    }

    private void validarFormaPagamentoDisponivel(FormaPagamento formaPagamento, BigDecimal total) throws CheckoutException {
        if (FormaPagamento.BOLETO == formaPagamento) {
            if (total.compareTo(LIMITE_BOLETO) > 0) {
                throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
            }
        }
    }

    private BigDecimal calcularAjustePagamento(FormaPagamento formaPagamento, BigDecimal total, int parcelas) throws CheckoutException {
        switch (formaPagamento) {
            case PIX:
                return arredondarParaCentavos(total.multiply(TAXA_PIX).negate());

            case BOLETO:
                return arredondarParaCentavos(TARIFA_BOLETO);

            case CARTAO:
                if (parcelas <= 3) {
                    return BigDecimal.ZERO;
                } else {
                    return calcularJurosCartao(total, parcelas);
                }

            default:
                throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private BigDecimal calcularJurosCartao(BigDecimal total, int parcelas) {
        BigDecimal taxa = TAXA_JUROS_CARTAO;
        double tasoDouble = taxa.doubleValue();
        double fator = Math.pow(1 + tasoDouble, -parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(new BigDecimal(fator));

        BigDecimal numerador = taxa;
        BigDecimal parcela = total.multiply(numerador).divide(denominador, 10, RoundingMode.HALF_EVEN);
        parcela = arredondarParaCentavos(parcela);

        BigDecimal valorTotal = parcela.multiply(new BigDecimal(parcelas));
        BigDecimal juros = valorTotal.subtract(total);

        return arredondarParaCentavos(juros);
    }

    private BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas) {
        if (parcelas <= 0) {
            parcelas = 1;
        }
        return arredondarParaCentavos(totalFinal.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN));
    }

    private BigDecimal arredondarParaCentavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
