package br.tcc.checkout.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import br.tcc.checkout.domain.Cupom;
import br.tcc.checkout.domain.FormaPagamento;
import br.tcc.checkout.domain.ModalidadeEntrega;
import br.tcc.checkout.dto.CheckoutRequest;
import br.tcc.checkout.dto.CheckoutResponse;
import br.tcc.checkout.dto.Item;
import br.tcc.checkout.util.MoedaUtil;

@Service
public class CheckoutService {

    public CheckoutResponse calcularResumo(CheckoutRequest request) throws CheckoutException {
        List<Item> itens = request.itens();
        String modalidadeStr = request.modalidadeEntrega();
        String cupomStr = request.cupom();
        String formaPagamentoStr = request.formaPagamento();
        int parcelas = request.parcelas() != null ? request.parcelas() : 1;

        validarPedido(itens);
        validarModalidade(modalidadeStr);
        ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(modalidadeStr);
        validarModalidadeDisponivel(modalidade, itens);

        Cupom cupom = null;
        if (cupomStr != null && !cupomStr.isEmpty()) {
            cupom = Cupom.fromString(cupomStr);
            validarCupom(cupom, cupomStr);
            validarCupomAplicavel(cupom, itens);
        }

        validarFormaPagamento(formaPagamentoStr);
        FormaPagamento formaPagamento = FormaPagamento.fromString(formaPagamentoStr);

        BigDecimal subtotalProdutos = calcularSubtotal(itens);
        BigDecimal descontoCupom = calcularDescontoCupom(cupom, subtotalProdutos, itens);
        BigDecimal totalAposCupom = MoedaUtil.arredondar(subtotalProdutos.subtract(descontoCupom));

        BigDecimal pesoTotal = calcularPesoTotal(itens);
        BigDecimal frete = calcularFrete(modalidade, pesoTotal, cupom);

        BigDecimal totalPedido = MoedaUtil.arredondar(totalAposCupom.add(frete));

        validarParcelamento(formaPagamento, totalPedido, parcelas);
        FormaPagamento.PagamentoResult pagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal totalFinal = MoedaUtil.arredondar(pagamento.totalComAjuste);
        BigDecimal valorParcela = MoedaUtil.arredondar(pagamento.valorParcela);
        BigDecimal ajustePagamento = MoedaUtil.arredondar(pagamento.ajuste);

        return new CheckoutResponse(
            MoedaUtil.arredondar(subtotalProdutos),
            MoedaUtil.arredondar(descontoCupom),
            MoedaUtil.arredondar(frete),
            modalidade.prazoEntregaDias,
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela
        );
    }

    private void validarPedido(List<Item> itens) throws CheckoutException {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (Item item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().signum() <= 0 ||
                item.quantidade() == null || item.quantidade() <= 0 ||
                item.pesoKg() == null || item.pesoKg().signum() < 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private void validarModalidade(String modalidade) throws CheckoutException {
        if (modalidade == null || modalidade.isEmpty()) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        try {
            ModalidadeEntrega.valueOf(modalidade);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
    }

    private void validarModalidadeDisponivel(ModalidadeEntrega modalidade, List<Item> itens) throws CheckoutException {
        BigDecimal pesoTotal = calcularPesoTotal(itens);
        if (!modalidade.estaDisponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private void validarCupom(Cupom cupom, String cupomStr) throws CheckoutException {
        if (cupom == null) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
    }

    private void validarCupomAplicavel(Cupom cupom, List<Item> itens) throws CheckoutException {
        BigDecimal subtotal = calcularSubtotal(itens);
        if (!cupom.estaAplicavel(subtotal, itens)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
    }

    private void validarFormaPagamento(String formaPagamento) throws CheckoutException {
        if (formaPagamento == null || formaPagamento.isEmpty()) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        if (FormaPagamento.fromString(formaPagamento) == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private void validarParcelamento(FormaPagamento formaPagamento, BigDecimal totalPedido, int parcelas) throws CheckoutException {
        if (!isParcelasValida(formaPagamento, parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
        if (!formaPagamento.estaDisponivel(totalPedido, parcelas)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private boolean isParcelasValida(FormaPagamento formaPagamento, int parcelas) {
        return switch (formaPagamento) {
            case PIX, BOLETO -> parcelas == 1;
            case CARTAO -> parcelas >= 1 && parcelas <= 12;
        };
    }

    private BigDecimal calcularSubtotal(List<Item> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Item item : itens) {
            BigDecimal itemTotal = item.precoUnitario()
                .multiply(BigDecimal.valueOf(item.quantidade()));
            subtotal = subtotal.add(itemTotal);
        }
        return MoedaUtil.arredondar(subtotal);
    }

    private BigDecimal calcularDescontoCupom(Cupom cupom, BigDecimal subtotalProdutos, List<Item> itens) {
        if (cupom == null) {
            return BigDecimal.ZERO;
        }
        return cupom.calcularDesconto(subtotalProdutos, itens);
    }

    private BigDecimal calcularPesoTotal(List<Item> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (Item item : itens) {
            BigDecimal itemPeso = item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade()));
            pesoTotal = pesoTotal.add(itemPeso);
        }
        return pesoTotal;
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, BigDecimal pesoTotal, Cupom cupom) {
        if (cupom != null && cupom.isFretegratis()) {
            return BigDecimal.ZERO;
        }
        return MoedaUtil.arredondar(modalidade.calcularFrete(pesoTotal));
    }
}
