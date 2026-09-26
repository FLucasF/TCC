package com.loja.service;

import com.loja.dto.CheckoutRequestDTO;
import com.loja.dto.CheckoutResponseDTO;
import com.loja.dto.ItemPedidoDTO;
import com.loja.model.Cupom;
import com.loja.model.FormaPagamento;
import com.loja.model.ModalidadeEntrega;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public CheckoutResponseDTO calcularResumo(CheckoutRequestDTO request) {
        validarPedido(request);

        ModalidadeEntrega modalidade = validarModalidadeEntrega(request.getModalidadeEntrega());
        validarModalidadeDisponivel(request, modalidade);

        Cupom cupom = validarCupom(request.getCupom());
        validarCupomAplicavel(request, cupom);

        FormaPagamento formaPagamento = validarFormaPagamento(request.getFormaPagamento());
        Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        validarParcelamento(formaPagamento, parcelas);

        BigDecimal subtotalProdutos = calcularSubtotal(request.getItens());
        BigDecimal frete = calcularFrete(modalidade, request.getItens());
        BigDecimal descontoCupom = calcularDescontoCupom(cupom, request.getItens(), subtotalProdutos, frete);

        BigDecimal totalSemAjuste = subtotalProdutos.subtract(descontoCupom).add(frete);
        validarFormaPagamentoDisponivel(formaPagamento, totalSemAjuste);

        BigDecimal ajustePagamento = calcularAjustePagamento(formaPagamento, totalSemAjuste, parcelas);
        BigDecimal totalFinal = totalSemAjuste.add(ajustePagamento);

        BigDecimal valorParcela = calcularValorParcela(formaPagamento, totalFinal, parcelas);

        return new CheckoutResponseDTO(
            subtotalProdutos,
            descontoCupom,
            frete,
            modalidade.getPrazo(),
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela
        );
    }

    private void validarPedido(CheckoutRequestDTO request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }

        for (ItemPedidoDTO item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() < 0) {
                throw new ErroCheckout("PEDIDO_INVALIDO");
            }
        }
    }

    private ModalidadeEntrega validarModalidadeEntrega(String codigo) {
        if (codigo == null) {
            throw new ErroCheckout("MODALIDADE_INVALIDA");
        }
        ModalidadeEntrega modalidade = ModalidadeEntrega.fromCodigo(codigo);
        if (modalidade == null) {
            throw new ErroCheckout("MODALIDADE_INVALIDA");
        }
        return modalidade;
    }

    private void validarModalidadeDisponivel(CheckoutRequestDTO request, ModalidadeEntrega modalidade) {
        if (modalidade == ModalidadeEntrega.MOTOBOY) {
            double pesoTotal = request.getItens().stream()
                .mapToDouble(item -> item.getPesoKg() * item.getQuantidade())
                .sum();
            if (pesoTotal > 5.0) {
                throw new ErroCheckout("MODALIDADE_INDISPONIVEL");
            }
        }
    }

    private Cupom validarCupom(String codigo) {
        if (codigo == null || codigo.isEmpty()) {
            return null;
        }
        Cupom cupom = Cupom.fromCodigo(codigo);
        if (cupom == null) {
            throw new ErroCheckout("CUPOM_INVALIDO");
        }
        return cupom;
    }

    private void validarCupomAplicavel(CheckoutRequestDTO request, Cupom cupom) {
        if (cupom == null) {
            return;
        }

        BigDecimal subtotalProdutos = calcularSubtotal(request.getItens());

        if (cupom == Cupom.MENOS50) {
            if (subtotalProdutos.compareTo(new BigDecimal("300.00")) < 0) {
                throw new ErroCheckout("CUPOM_NAO_APLICAVEL");
            }
        }
    }

    private FormaPagamento validarFormaPagamento(String codigo) {
        if (codigo == null) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }
        FormaPagamento forma = FormaPagamento.fromCodigo(codigo);
        if (forma == null) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }
        return forma;
    }

    private void validarParcelamento(FormaPagamento formaPagamento, Integer parcelas) {
        if (parcelas < 1 || parcelas > 12) {
            throw new ErroCheckout("PARCELAMENTO_INVALIDO");
        }

        if (formaPagamento == FormaPagamento.PIX || formaPagamento == FormaPagamento.BOLETO) {
            if (parcelas != 1) {
                throw new ErroCheckout("PARCELAMENTO_INVALIDO");
            }
        }
    }

    private void validarFormaPagamentoDisponivel(FormaPagamento formaPagamento, BigDecimal total) {
        if (formaPagamento == FormaPagamento.BOLETO) {
            if (total.compareTo(new BigDecimal("1000.00")) > 0) {
                throw new ErroCheckout("FORMA_PAGAMENTO_INDISPONIVEL");
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemPedidoDTO> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedidoDTO item : itens) {
            BigDecimal precoItem = new BigDecimal(item.getPrecoUnitario()).multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(precoItem);
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularDescontoCupom(Cupom cupom, List<ItemPedidoDTO> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        if (cupom == null) {
            return arredondar(BigDecimal.ZERO);
        }

        switch (cupom.getTipo()) {
            case PERCENTUAL_PRODUTOS:
                // BEMVINDO10: 10%
                return arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));

            case VALOR_FIXO_PRODUTOS:
                // MENOS50: R$ 50,00
                return arredondar(new BigDecimal("50.00"));

            case FRETE_GRATIS:
                // Desconto igual ao valor do frete
                return frete;

            case LEVE3_PAGUE2:
                // A cada 3 unidades de um mesmo item, uma sai de graça
                return calcularDescontoLeve3Pague2(itens);

            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<ItemPedidoDTO> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemPedidoDTO item : itens) {
            int quantidade = item.getQuantidade();
            int itensGratis = quantidade / 3;
            BigDecimal precoItem = new BigDecimal(item.getPrecoUnitario());
            BigDecimal descontoItem = precoItem.multiply(new BigDecimal(itensGratis));
            desconto = desconto.add(descontoItem);
        }
        return arredondar(desconto);
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, List<ItemPedidoDTO> itens) {
        double pesoTotal = itens.stream()
            .mapToDouble(item -> item.getPesoKg() * item.getQuantidade())
            .sum();

        BigDecimal frete = modalidade.getTaxaBase()
            .add(modalidade.getTaxaPorKg().multiply(new BigDecimal(pesoTotal)));

        return arredondar(frete);
    }

    private BigDecimal calcularAjustePagamento(FormaPagamento formaPagamento, BigDecimal total, Integer parcelas) {
        switch (formaPagamento) {
            case PIX:
                // 5% de desconto
                return arredondar(total.multiply(new BigDecimal("0.05"))).negate();

            case CARTAO:
                if (parcelas <= 3) {
                    return arredondar(BigDecimal.ZERO);
                }
                // De 4x a 12x: juros de 1,99% a.m. (tabela Price)
                // parcela = total × taxa ÷ (1 − (1 + taxa)^−n)
                BigDecimal taxaMensal = new BigDecimal("0.0199");
                BigDecimal um = BigDecimal.ONE;
                BigDecimal umMaisTaxa = um.add(taxaMensal);
                BigDecimal potencia = umMaisTaxa.pow(parcelas);
                BigDecimal denominador = um.subtract(um.divide(potencia, 10, RoundingMode.HALF_EVEN));
                BigDecimal numerador = total.multiply(taxaMensal);
                BigDecimal parcela = numerador.divide(denominador, 10, RoundingMode.HALF_EVEN);
                parcela = arredondar(parcela);
                BigDecimal totalComJuros = parcela.multiply(new BigDecimal(parcelas));
                return arredondar(totalComJuros.subtract(total));

            case BOLETO:
                // Tarifa de R$ 3,49
                return arredondar(new BigDecimal("3.49"));

            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calcularValorParcela(FormaPagamento formaPagamento, BigDecimal totalFinal, Integer parcelas) {
        if (parcelas == 1) {
            return arredondar(totalFinal);
        }

        BigDecimal valorParcela = totalFinal.divide(new BigDecimal(parcelas), 10, ROUNDING_MODE);
        return arredondar(valorParcela);
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, ROUNDING_MODE);
    }

}
