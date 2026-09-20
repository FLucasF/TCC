package br.tcc.checkout.service;

import br.tcc.checkout.dto.ItemCarrinho;
import br.tcc.checkout.dto.RequisicaoResumo;
import br.tcc.checkout.dto.RespostaResumo;
import br.tcc.checkout.exception.ErroCheckout;
import br.tcc.checkout.model.Cupom;
import br.tcc.checkout.model.FormaPagamento;
import br.tcc.checkout.model.ModalidadeEntrega;
import br.tcc.checkout.util.ArredondadorMoeda;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CheckoutService {

    public RespostaResumo calcularResumo(RequisicaoResumo requisicao) {
        validarRequisicao(requisicao);

        List<ItemCarrinho> itens = requisicao.getItens();
        ModalidadeEntrega modalidade = ModalidadeEntrega.fromString(requisicao.getModalidadeEntrega());
        String codigoCupom = requisicao.getCupom();
        FormaPagamento formaPagamento = FormaPagamento.fromString(requisicao.getFormaPagamento());
        int parcelas = requisicao.getParcelas() != null ? requisicao.getParcelas() : 1;

        // Passo 1: Calcular subtotal de produtos
        BigDecimal subtotalProdutos = calcularSubtotalProdutos(itens, codigoCupom);

        // Passo 2: Calcular desconto do cupom
        BigDecimal descontoCupom = calcularDescontoCupom(subtotalProdutos, codigoCupom, itens);

        // Passo 3: Calcular frete
        BigDecimal pesoTotal = calcularPesoTotal(itens);
        BigDecimal frete = calcularFrete(modalidade, pesoTotal);

        // Passo 4: Total do pedido (produtos - cupom + frete)
        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete);

        // Passo 5: Calcular ajuste de pagamento e total final
        BigDecimal ajustePagamento = calcularAjustePagamento(totalPedido, formaPagamento, parcelas);
        BigDecimal totalFinal = totalPedido.add(ajustePagamento);

        // Passo 6: Calcular valor da parcela
        BigDecimal valorParcela = calcularValorParcela(totalFinal, formaPagamento, parcelas);

        return new RespostaResumo(
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

    private void validarRequisicao(RequisicaoResumo requisicao) {
        // 1. Validar carrinho
        if (requisicao.getItens() == null || requisicao.getItens().isEmpty()) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : requisicao.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                    item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                    item.getPesoKg() == null || item.getPesoKg() < 0) {
                throw new ErroCheckout("PEDIDO_INVALIDO");
            }
        }

        // 2. Validar modalidade de entrega
        if (requisicao.getModalidadeEntrega() == null || requisicao.getModalidadeEntrega().isEmpty()) {
            throw new ErroCheckout("MODALIDADE_INVALIDA");
        }

        ModalidadeEntrega modalidade = ModalidadeEntrega.fromString(requisicao.getModalidadeEntrega());
        if (modalidade == null) {
            throw new ErroCheckout("MODALIDADE_INVALIDA");
        }

        // 3. Validar se modalidade atende o pedido
        if (modalidade == ModalidadeEntrega.MOTOBOY) {
            BigDecimal pesoTotal = calcularPesoTotal(requisicao.getItens());
            if (pesoTotal.compareTo(new BigDecimal("5.00")) > 0) {
                throw new ErroCheckout("MODALIDADE_INDISPONIVEL");
            }
        }

        // 4. Validar cupom
        String codigoCupom = requisicao.getCupom();
        if (codigoCupom != null && !codigoCupom.isEmpty()) {
            Cupom cupom = Cupom.obter(codigoCupom);
            if (cupom == null) {
                throw new ErroCheckout("CUPOM_INVALIDO");
            }

            // 5. Validar se cupom é aplicável
            BigDecimal subtotal = calcularSubtotalProdutos(requisicao.getItens(), null);
            if (!validarAplicabilidadeCupom(subtotal, cupom)) {
                throw new ErroCheckout("CUPOM_NAO_APLICAVEL");
            }
        }

        // 6. Validar forma de pagamento
        if (requisicao.getFormaPagamento() == null || requisicao.getFormaPagamento().isEmpty()) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }

        FormaPagamento formaPagamento = FormaPagamento.fromString(requisicao.getFormaPagamento());
        if (formaPagamento == null) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }

        // 7. Validar parcelamento
        int parcelas = requisicao.getParcelas() != null ? requisicao.getParcelas() : 1;
        if (!validarParcelamento(formaPagamento, parcelas)) {
            throw new ErroCheckout("PARCELAMENTO_INVALIDO");
        }

        // 8. Validar se forma de pagamento atende o pedido
        BigDecimal totalPedido = calcularTotalPedido(requisicao.getItens(), codigoCupom, modalidade);
        if (!validarFormaPagamento(formaPagamento, totalPedido)) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens, String codigoCupom) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (ItemCarrinho item : itens) {
            BigDecimal precoItem = new BigDecimal(item.getPrecoUnitario());
            int quantidade = item.getQuantidade();
            BigDecimal totalItem = precoItem.multiply(new BigDecimal(quantidade));
            subtotal = subtotal.add(totalItem);
        }

        return ArredondadorMoeda.arredondar(subtotal);
    }

    private BigDecimal calcularDescontoCupom(BigDecimal subtotalProdutos, String codigoCupom, List<ItemCarrinho> itens) {
        if (codigoCupom == null || codigoCupom.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Cupom cupom = Cupom.obter(codigoCupom);
        if (cupom == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal desconto = BigDecimal.ZERO;

        switch (cupom.getTipo()) {
            case PERCENTUAL:
                desconto = subtotalProdutos.multiply(cupom.getValor());
                break;
            case FIXO:
                desconto = cupom.getValor();
                break;
            case FRETE_GRATIS:
                desconto = BigDecimal.ZERO;
                break;
            case LEVE3PAGUE2:
                desconto = calcularDescontoLeve3Pague2(itens);
                break;
        }

        return ArredondadorMoeda.arredondar(desconto);
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<ItemCarrinho> itens) {
        BigDecimal desconto = BigDecimal.ZERO;

        for (ItemCarrinho item : itens) {
            BigDecimal precoItem = new BigDecimal(item.getPrecoUnitario());
            int quantidade = item.getQuantidade();
            // A cada 3 unidades, 1 sai de graça
            int unidadesGratis = quantidade / 3;
            BigDecimal descontoItem = precoItem.multiply(new BigDecimal(unidadesGratis));
            desconto = desconto.add(descontoItem);
        }

        return ArredondadorMoeda.arredondar(desconto);
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            BigDecimal peso = new BigDecimal(item.getPesoKg());
            BigDecimal quantidade = new BigDecimal(item.getQuantidade());
            pesoTotal = pesoTotal.add(peso.multiply(quantidade));
        }
        return pesoTotal;
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        BigDecimal frete = modalidade.getValorFixo()
                .add(modalidade.getValorPorKg().multiply(pesoTotal));
        return ArredondadorMoeda.arredondar(frete);
    }

    private BigDecimal calcularAjustePagamento(BigDecimal totalPedido, FormaPagamento formaPagamento, int parcelas) {
        BigDecimal ajuste = BigDecimal.ZERO;

        switch (formaPagamento) {
            case PIX:
                ajuste = totalPedido.multiply(new BigDecimal("-0.05"));
                ajuste = ArredondadorMoeda.arredondar(ajuste);
                break;
            case CARTAO:
                if (parcelas > 3) {
                    // Aplicar juros (1,99% a.m.)
                    BigDecimal taxa = new BigDecimal("0.0199");
                    BigDecimal juros = calcularJurosCartao(totalPedido, taxa, parcelas);
                    ajuste = juros.subtract(totalPedido);
                }
                break;
            case BOLETO:
                ajuste = new BigDecimal("3.49");
                break;
        }

        return ArredondadorMoeda.arredondar(ajuste);
    }

    private BigDecimal calcularJurosCartao(BigDecimal totalPedido, BigDecimal taxa, int parcelas) {
        // Fórmula: parcela = total × taxa ÷ (1 − (1 + taxa)^−parcelas)
        // Reescrevendo: parcela = total × taxa ÷ (1 − 1 / (1 + taxa)^parcelas)
        BigDecimal um = BigDecimal.ONE;
        BigDecimal umMaisTaxa = um.add(taxa);

        // Calcular (1 + taxa)^parcelas
        BigDecimal potencia = umMaisTaxa.pow(parcelas);
        // Calcular 1 / (1 + taxa)^parcelas
        BigDecimal umDividoPotencia = um.divide(potencia, 10, RoundingMode.HALF_EVEN);
        // Calcular 1 − 1 / (1 + taxa)^parcelas
        BigDecimal denominador = um.subtract(umDividoPotencia);

        BigDecimal parcela = totalPedido.multiply(taxa).divide(denominador, 10, RoundingMode.HALF_EVEN);
        parcela = ArredondadorMoeda.arredondar(parcela);

        BigDecimal totalJuros = parcela.multiply(new BigDecimal(parcelas));
        return ArredondadorMoeda.arredondar(totalJuros);
    }

    private BigDecimal calcularValorParcela(BigDecimal totalFinal, FormaPagamento formaPagamento, int parcelas) {
        return ArredondadorMoeda.arredondar(
                totalFinal.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN)
        );
    }

    private boolean validarAplicabilidadeCupom(BigDecimal subtotal, Cupom cupom) {
        if (cupom.getTipo() == Cupom.TipoCupom.FIXO) {
            return subtotal.compareTo(cupom.getMinimo()) >= 0;
        }
        return true;
    }

    private boolean validarParcelamento(FormaPagamento formaPagamento, int parcelas) {
        if (parcelas < 1) {
            return false;
        }

        switch (formaPagamento) {
            case PIX:
            case BOLETO:
                return parcelas == 1;
            case CARTAO:
                return parcelas >= 1 && parcelas <= 12;
            default:
                return false;
        }
    }

    private boolean validarFormaPagamento(FormaPagamento formaPagamento, BigDecimal totalPedido) {
        if (formaPagamento == FormaPagamento.BOLETO) {
            return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
        }
        return true;
    }

    private BigDecimal calcularTotalPedido(List<ItemCarrinho> itens, String codigoCupom, ModalidadeEntrega modalidade) {
        BigDecimal subtotal = calcularSubtotalProdutos(itens, codigoCupom);
        BigDecimal desconto = calcularDescontoCupom(subtotal, codigoCupom, itens);
        BigDecimal pesoTotal = calcularPesoTotal(itens);
        BigDecimal frete = calcularFrete(modalidade, pesoTotal);
        return subtotal.subtract(desconto).add(frete);
    }
}
