package com.loja.checkout.service;

import com.loja.checkout.dto.Item;
import com.loja.checkout.dto.RequisicaoCheckout;
import com.loja.checkout.dto.ResumoCheckout;
import com.loja.checkout.model.Cupom;
import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.model.ModalidadeEntrega;
import com.loja.checkout.util.Arredondador;

import java.util.List;

public class CheckoutService {

    public ResumoCheckout calcularResumo(RequisicaoCheckout requisicao) throws CheckoutException {
        validarRequisicao(requisicao);

        List<Item> itens = requisicao.getItens();
        ModalidadeEntrega modalidade = ModalidadeEntrega.fromString(requisicao.getModalidadeEntrega());
        String cupomCod = requisicao.getCupom();
        FormaPagamento formaPagamento = FormaPagamento.fromString(requisicao.getFormaPagamento());
        Integer parcelas = requisicao.getParcelas();

        // Validação 2: Modalidade deve existir
        if (modalidade == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        // Passo 1: Calcular subtotal e peso
        Double subtotalProdutos = Arredondador.arredondar(calcularSubtotal(itens));
        Double pesoTotal = calcularPesoTotal(itens);

        // Validação 3: Motoboy só até 5kg
        if (modalidade == ModalidadeEntrega.MOTOBOY && pesoTotal > 5.0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        // Validação 4 e 5: Cupom
        Cupom cupom = null;
        Double descontoCupom = 0.0;
        Double frete = 0.0;

        if (cupomCod != null && !cupomCod.isEmpty()) {
            cupom = Cupom.fromString(cupomCod);
            if (cupom == null) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }

            // Calcular frete temporariamente para validar cupom
            frete = Arredondador.arredondar(modalidade.calcularFrete(pesoTotal));

            if (!cupom.eAplicavel(subtotalProdutos, frete, pesoTotal)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }

            // Para LEVE3PAGUE2, precisamos calcular o desconto com base nos itens
            if (cupom == Cupom.LEVE3PAGUE2) {
                descontoCupom = Arredondador.arredondar(calcularDescontoLeve3Pague2(itens));
            } else if (cupom == Cupom.FRETEGRATIS) {
                descontoCupom = cupom.calcularFreteNoDesconto(frete);
            } else {
                descontoCupom = Arredondador.arredondar(cupom.calcularDesconto(subtotalProdutos, frete, pesoTotal));
            }
        }

        // Passo 2: Desconto do cupom
        Double totalAposCupom = Arredondador.arredondar(subtotalProdutos - descontoCupom);

        // Passo 3: Frete
        frete = Arredondador.arredondar(modalidade.calcularFrete(pesoTotal));

        // Para FRETEGRATIS, não cobrar frete no total
        Double freteACobrar = (cupom == Cupom.FRETEGRATIS) ? 0.0 : frete;

        // Passo 4: Total do pedido
        Double totalPedido = Arredondador.arredondar(totalAposCupom + freteACobrar);

        // Validação 6: Forma de pagamento deve existir
        if (formaPagamento == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        // Validação 7: Número de parcelas válido
        if (!eParcelamentoValido(formaPagamento, parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        // Validação 8: Boleto acima de 1000
        if (formaPagamento == FormaPagamento.BOLETO && totalPedido > 1000.00) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        // Passo 5: Ajuste da forma de pagamento
        Double ajustePagamento = calcularAjustePagamento(formaPagamento, totalPedido, parcelas);
        Double totalFinal = Arredondador.arredondar(totalPedido + ajustePagamento);

        // Calcular parcelas
        Double valorParcela = calcularValorParcela(formaPagamento, totalFinal, parcelas);

        return new ResumoCheckout(
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

    private void validarRequisicao(RequisicaoCheckout requisicao) throws CheckoutException {
        List<Item> itens = requisicao.getItens();

        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (Item item : itens) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() < 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }

        if (requisicao.getModalidadeEntrega() == null || requisicao.getModalidadeEntrega().isEmpty()) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        if (requisicao.getFormaPagamento() == null || requisicao.getFormaPagamento().isEmpty()) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private Double calcularSubtotal(List<Item> itens) {
        return itens.stream()
            .mapToDouble(item -> item.getPrecoUnitario() * item.getQuantidade())
            .sum();
    }

    private Double calcularPesoTotal(List<Item> itens) {
        return itens.stream()
            .mapToDouble(item -> item.getPesoKg() * item.getQuantidade())
            .sum();
    }

    private Double calcularDescontoLeve3Pague2(List<Item> itens) {
        Double desconto = 0.0;
        for (Item item : itens) {
            Integer gratisPorItem = item.getQuantidade() / 3;
            desconto += gratisPorItem * item.getPrecoUnitario();
        }
        return desconto;
    }

    private boolean eParcelamentoValido(FormaPagamento formaPagamento, Integer parcelas) {
        if (formaPagamento == FormaPagamento.PIX || formaPagamento == FormaPagamento.BOLETO) {
            return parcelas == 1;
        }
        if (formaPagamento == FormaPagamento.CARTAO) {
            return parcelas >= 1 && parcelas <= 12;
        }
        return false;
    }

    private Double calcularAjustePagamento(FormaPagamento formaPagamento, Double totalPedido, Integer parcelas) {
        return switch (formaPagamento) {
            case PIX -> Arredondador.arredondar(-totalPedido * 0.05);
            case BOLETO -> 3.49;
            case CARTAO -> calcularAjusteCartao(totalPedido, parcelas);
        };
    }

    private Double calcularAjusteCartao(Double totalPedido, Integer parcelas) {
        if (parcelas <= 3) {
            return 0.0;
        }
        Double taxaMensal = 0.0199;
        Double r = Math.pow(1 + taxaMensal, parcelas);
        Double parcela = totalPedido * (taxaMensal * r) / (r - 1);
        Double totalFinal = Arredondador.arredondar(parcela) * parcelas;
        return Arredondador.arredondar(totalFinal - totalPedido);
    }

    private Double calcularValorParcela(FormaPagamento formaPagamento, Double totalFinal, Integer parcelas) {
        return Arredondador.arredondar(totalFinal / parcelas);
    }
}
