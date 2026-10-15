package br.tcc.checkout;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import br.tcc.checkout.dto.ItemRequest;
import br.tcc.checkout.dto.ResumoRequest;
import br.tcc.checkout.dto.ResumoResponse;

@Service
class CheckoutService {

    ResumoResponse calcularResumo(ResumoRequest request) {
        List<ItemPedido> itens = validarItens(request.itens());
        BigDecimal subtotalProdutos = Dinheiro.arredondar(somarSubtotal(itens));
        BigDecimal pesoTotalKg = somarPeso(itens);
        PedidoContexto pedido = new PedidoContexto(itens, subtotalProdutos, pesoTotalKg);

        ModalidadeEntrega modalidade = resolverModalidade(request.modalidadeEntrega());
        if (!modalidade.disponivelPara(pedido)) {
            throw new CheckoutException(ErroCodigo.MODALIDADE_INDISPONIVEL);
        }
        BigDecimal frete = Dinheiro.arredondar(modalidade.calcularFrete(pedido));

        Cupom cupom = resolverCupom(request.cupom());
        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (cupom != null) {
            if (!cupom.aplicavelPara(pedido)) {
                throw new CheckoutException(ErroCodigo.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = Dinheiro.arredondar(cupom.calcularDesconto(pedido, frete));
        }

        BigDecimal totalPedido = Dinheiro.arredondar(subtotalProdutos.subtract(descontoCupom).add(frete));

        FormaPagamento formaPagamento = resolverFormaPagamento(request.formaPagamento());
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException(ErroCodigo.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.disponivelPara(totalPedido)) {
            throw new CheckoutException(ErroCodigo.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                resultado.ajuste(),
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela());
    }

    private List<ItemPedido> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(ErroCodigo.PEDIDO_INVALIDO);
        }
        List<ItemPedido> resultado = new ArrayList<>();
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException(ErroCodigo.PEDIDO_INVALIDO);
            }
            resultado.add(new ItemPedido(item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return resultado;
    }

    private BigDecimal somarSubtotal(List<ItemPedido> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return subtotal;
    }

    private BigDecimal somarPeso(List<ItemPedido> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }

    private ModalidadeEntrega resolverModalidade(String valor) {
        if (valor == null) {
            throw new CheckoutException(ErroCodigo.MODALIDADE_INVALIDA);
        }
        try {
            return ModalidadeEntrega.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(ErroCodigo.MODALIDADE_INVALIDA);
        }
    }

    private Cupom resolverCupom(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        try {
            return Cupom.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(ErroCodigo.CUPOM_INVALIDO);
        }
    }

    private FormaPagamento resolverFormaPagamento(String valor) {
        if (valor == null) {
            throw new CheckoutException(ErroCodigo.FORMA_PAGAMENTO_INVALIDA);
        }
        try {
            return FormaPagamento.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(ErroCodigo.FORMA_PAGAMENTO_INVALIDA);
        }
    }
}
