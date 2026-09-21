package com.loja.roupas.checkout;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutResumoService {

    private static final BigDecimal SCALE_PRICE = new BigDecimal("0.01");
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public CheckoutResponse calcular(CheckoutRequest request) {
        List<ItemCarrinho> itens = request.itens();
        String modalidadeEntregaCodigo = request.modalidadeEntrega();
        String cupomCodigo = request.cupom();
        String formaPagamentoCodigo = request.formaPagamento();
        int parcelas = request.parcelas() != null ? request.parcelas() : 1;

        // Validação 1: Pedido inválido
        String erroValidacaoPedido = validarPedido(itens);
        if (erroValidacaoPedido != null) {
            throw new ErroCheckout(erroValidacaoPedido);
        }

        // Validação 2: Modalidade não existe
        ModalidadeEntrega modalidade = ModalidadeEntrega.fromCodigo(modalidadeEntregaCodigo);
        if (modalidade == null) {
            throw new ErroCheckout("MODALIDADE_INVALIDA");
        }

        // Validação 3: Modalidade não atende
        BigDecimal pesoTotal = calcularPesoTotal(itens);
        if (modalidade == ModalidadeEntrega.MOTOBOY && pesoTotal.compareTo(new BigDecimal("5.00")) > 0) {
            throw new ErroCheckout("MODALIDADE_INDISPONIVEL");
        }

        // Validação 4: Cupom não existe
        Cupom cupom = null;
        if (cupomCodigo != null && !cupomCodigo.isEmpty()) {
            cupom = Cupom.fromCodigo(cupomCodigo);
            if (cupom == null) {
                throw new ErroCheckout("CUPOM_INVALIDO");
            }
        }

        // Cálculo de subtotal de produtos
        BigDecimal subtotalProdutos = calcularSubtotalProdutos(itens);

        // Validação 5: Cupom não aplicável
        if (cupom != null && !cupom.isAplicavel(subtotalProdutos, itens)) {
            throw new ErroCheckout("CUPOM_NAO_APLICAVEL");
        }

        // Validação 6: Forma de pagamento não existe
        FormaPagamento formaPagamento = FormaPagamento.fromCodigo(formaPagamentoCodigo);
        if (formaPagamento == null) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }

        // Validação 7: Parcelamento inválido
        String erroParcelamento = validarParcelamento(formaPagamento, parcelas);
        if (erroParcelamento != null) {
            throw new ErroCheckout(erroParcelamento);
        }

        // Cálculo de frete
        BigDecimal frete = calcularFrete(modalidade, pesoTotal);

        // Total do pedido = produtos - desconto cupom + frete
        BigDecimal descontoCupom = calcularDescontoCupom(cupom, subtotalProdutos, frete, itens);
        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete);

        // Validação 8: Forma de pagamento não atende
        if (formaPagamento == FormaPagamento.BOLETO && totalPedido.compareTo(new BigDecimal("1000.00")) > 0) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        // Cálculo de ajuste de pagamento
        BigDecimal ajustePagamento = calcularAjustePagamento(formaPagamento, totalPedido, parcelas);

        // Total final
        BigDecimal totalFinal = totalPedido.add(ajustePagamento);

        // Cálculo de parcela
        BigDecimal valorParcela = calcularValorParcela(formaPagamento, totalFinal, parcelas);

        return new CheckoutResponse(
            arredondar(subtotalProdutos),
            arredondar(descontoCupom),
            arredondar(frete),
            modalidade.getPrazo(),
            arredondar(ajustePagamento),
            arredondar(totalFinal),
            parcelas,
            arredondar(valorParcela)
        );
    }

    private String validarPedido(List<ItemCarrinho> itens) {
        if (itens == null || itens.isEmpty()) {
            return "PEDIDO_INVALIDO";
        }

        for (ItemCarrinho item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                return "PEDIDO_INVALIDO";
            }
            if (item.quantidade() == null || item.quantidade() <= 0) {
                return "PEDIDO_INVALIDO";
            }
            if (item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                return "PEDIDO_INVALIDO";
            }
        }

        return null;
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        return itens.stream()
            .map(item -> item.pesoKg().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        return itens.stream()
            .map(item -> item.precoUnitario().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        if (modalidade == ModalidadeEntrega.RETIRADA_LOJA) {
            return BigDecimal.ZERO;
        }

        if (modalidade == ModalidadeEntrega.MOTOBOY) {
            return new BigDecimal("18.00");
        }

        BigDecimal frete = modalidade.getTarifa()
            .add(modalidade.getPorKg().multiply(pesoTotal));

        return arredondar(frete);
    }

    private BigDecimal calcularDescontoCupom(Cupom cupom, BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        if (cupom == null) {
            return BigDecimal.ZERO;
        }

        return switch (cupom) {
            case BEMVINDO10 -> arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
            case MENOS50 -> new BigDecimal("50.00");
            case FRETEGRATIS -> frete;
            case LEVE3PAGUE2 -> calcularDescontoLeve3Pague2(subtotalProdutos, itens);
        };
    }

    private BigDecimal calcularDescontoLeve3Pague2(BigDecimal subtotalNormal, List<ItemCarrinho> itens) {
        BigDecimal subtotalComDesconto = BigDecimal.ZERO;

        for (ItemCarrinho item : itens) {
            int quantidade = item.quantidade();
            int quantidadeAGraca = quantidade / 3;
            int quantidadeAPagar = quantidade - quantidadeAGraca;

            BigDecimal valorItem = item.precoUnitario().multiply(new BigDecimal(quantidadeAPagar));
            subtotalComDesconto = subtotalComDesconto.add(valorItem);
        }

        return arredondar(subtotalNormal.subtract(subtotalComDesconto));
    }

    private String validarParcelamento(FormaPagamento formaPagamento, int parcelas) {
        if (formaPagamento == FormaPagamento.PIX || formaPagamento == FormaPagamento.BOLETO) {
            if (parcelas != 1) {
                return "PARCELAMENTO_INVALIDO";
            }
        } else if (formaPagamento == FormaPagamento.CARTAO) {
            if (parcelas < 1 || parcelas > 12) {
                return "PARCELAMENTO_INVALIDO";
            }
        }

        return null;
    }

    private BigDecimal calcularAjustePagamento(FormaPagamento formaPagamento, BigDecimal totalPedido, int parcelas) {
        return switch (formaPagamento) {
            case PIX -> arredondar(totalPedido.multiply(new BigDecimal("0.05")).negate());
            case BOLETO -> new BigDecimal("3.49");
            case CARTAO -> calcularAjusteCartao(totalPedido, parcelas);
        };
    }

    private BigDecimal calcularAjusteCartao(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            return BigDecimal.ZERO;
        }

        BigDecimal taxa = new BigDecimal("0.0199");
        BigDecimal periodosMedio = new BigDecimal(parcelas + 1).divide(new BigDecimal("2"), 20, RoundingMode.HALF_UP);
        BigDecimal juros = arredondar(totalPedido.multiply(taxa).multiply(periodosMedio));

        return juros;
    }

    private BigDecimal calcularValorParcela(FormaPagamento formaPagamento, BigDecimal totalFinal, int parcelas) {
        if (formaPagamento == FormaPagamento.PIX || formaPagamento == FormaPagamento.BOLETO) {
            return totalFinal;
        }

        return arredondar(totalFinal.divide(new BigDecimal(parcelas), 20, RoundingMode.HALF_UP));
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, ROUNDING);
    }
}
