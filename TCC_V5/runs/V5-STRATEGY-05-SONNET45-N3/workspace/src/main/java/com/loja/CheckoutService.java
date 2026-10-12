package com.loja;

import com.loja.clube.NivelClube;
import com.loja.clube.NivelClubeFactory;
import com.loja.cupom.Cupom;
import com.loja.cupom.CupomFactory;
import com.loja.dto.ItemCarrinho;
import com.loja.dto.Pedido;
import com.loja.dto.ResumoPedido;
import com.loja.entrega.ModalidadeEntrega;
import com.loja.entrega.ModalidadeEntregaFactory;
import com.loja.pagamento.FormaPagamento;
import com.loja.pagamento.FormaPagamentoFactory;
import com.loja.seguro.CalculadoraSeguro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CheckoutService {

    public ResumoPedido calcularResumo(Pedido pedido) {
        validarPedido(pedido);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(pedido.itens());

        NivelClube nivelClube = NivelClubeFactory.criar(pedido.nivelClube());
        if (nivelClube == null) {
            throw new ValidationException("NIVEL_CLUBE_INVALIDO");
        }

        BigDecimal seguro = CalculadoraSeguro.calcular(subtotalProdutos, pedido.regiao());
        if (seguro == null) {
            throw new ValidationException("REGIAO_INVALIDA");
        }

        ModalidadeEntrega modalidade = ModalidadeEntregaFactory.criar(pedido.modalidadeEntrega());
        if (modalidade == null) {
            throw new ValidationException("MODALIDADE_INVALIDA");
        }
        if (!modalidade.estaDisponivel(pedido.itens())) {
            throw new ValidationException("MODALIDADE_INDISPONIVEL");
        }

        if (pedido.cupom() != null && !pedido.cupom().isEmpty()) {
            Cupom cupom = CupomFactory.criar(pedido.cupom());
            if (cupom == null) {
                throw new ValidationException("CUPOM_INVALIDO");
            }
            if (!cupom.podeAplicar(subtotalProdutos)) {
                throw new ValidationException("CUPOM_NAO_APLICAVEL");
            }
        }

        FormaPagamento formaPagamento = FormaPagamentoFactory.criar(pedido.formaPagamento());
        if (formaPagamento == null) {
            throw new ValidationException("FORMA_PAGAMENTO_INVALIDA");
        }

        int numeroParcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;
        if (!formaPagamento.validarParcelamento(numeroParcelas)) {
            throw new ValidationException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal frete = nivelClube.temFreteGratis()
            ? BigDecimal.ZERO.setScale(2)
            : modalidade.calcularFrete(pedido.itens());

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (pedido.cupom() != null && !pedido.cupom().isEmpty()) {
            Cupom cupom = CupomFactory.criar(pedido.cupom());
            descontoCupom = cupom.calcularDesconto(subtotalProdutos, frete, pedido.itens());
        }

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro)
            .setScale(2, RoundingMode.HALF_EVEN);

        if (!formaPagamento.estaDisponivel(totalPedido, numeroParcelas)) {
            throw new ValidationException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        BigDecimal ajustePagamento = formaPagamento.calcularAjuste(totalPedido, numeroParcelas);
        BigDecimal totalFinal = totalPedido.add(ajustePagamento)
            .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal valorParcela = totalFinal
            .divide(BigDecimal.valueOf(numeroParcelas), 2, RoundingMode.HALF_EVEN);

        BigDecimal creditoProximaCompra = nivelClube.calcularCredito(subtotalProdutos);
        boolean brinde = nivelClube.ganhouBrinde(subtotalProdutos);

        return new ResumoPedido(
            subtotalProdutos,
            descontoCupom,
            frete,
            modalidade.obterPrazo(),
            seguro,
            ajustePagamento,
            totalFinal,
            numeroParcelas,
            valorParcela,
            creditoProximaCompra,
            brinde
        );
    }

    private BigDecimal calcularSubtotalProdutos(java.util.List<ItemCarrinho> itens) {
        return itens.stream()
            .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    private void validarPedido(Pedido pedido) {
        if (pedido.itens() == null || pedido.itens().isEmpty()) {
            throw new ValidationException("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : pedido.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.quantidade() <= 0 ||
                item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("PEDIDO_INVALIDO");
            }
        }
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }
}
