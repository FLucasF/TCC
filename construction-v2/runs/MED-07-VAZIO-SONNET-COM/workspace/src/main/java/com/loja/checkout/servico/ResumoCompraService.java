package com.loja.checkout.servico;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.cupom.CupomRegistry;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.entrega.ModalidadeEntregaRegistry;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.FormaPagamentoRegistry;
import com.loja.checkout.dominio.pagamento.ResultadoPagamento;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class ResumoCompraService {

    private final ModalidadeEntregaRegistry modalidadeEntregaRegistry;
    private final CupomRegistry cupomRegistry;
    private final FormaPagamentoRegistry formaPagamentoRegistry;

    public ResumoCompraService() {
        this(new ModalidadeEntregaRegistry(), new CupomRegistry(), new FormaPagamentoRegistry());
    }

    public ResumoCompraService(
            ModalidadeEntregaRegistry modalidadeEntregaRegistry,
            CupomRegistry cupomRegistry,
            FormaPagamentoRegistry formaPagamentoRegistry) {
        this.modalidadeEntregaRegistry = modalidadeEntregaRegistry;
        this.cupomRegistry = cupomRegistry;
        this.formaPagamentoRegistry = formaPagamentoRegistry;
    }

    public ResumoCompra calcular(
            Pedido pedido, String codigoModalidade, String codigoCupom, String codigoFormaPagamento, int parcelas) {

        if (!pedido.valido()) {
            throw new ErroNegocioException("PEDIDO_INVALIDO");
        }

        ModalidadeEntrega modalidade = modalidadeEntregaRegistry.buscar(codigoModalidade)
                .orElseThrow(() -> new ErroNegocioException("MODALIDADE_INVALIDA"));
        if (!modalidade.disponivel(pedido)) {
            throw new ErroNegocioException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = modalidade.calcularFrete(pedido);

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (codigoCupom != null) {
            Cupom cupom = cupomRegistry.buscar(codigoCupom)
                    .orElseThrow(() -> new ErroNegocioException("CUPOM_INVALIDO"));
            if (!cupom.aplicavel(pedido, subtotalProdutos)) {
                throw new ErroNegocioException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.calcularDesconto(pedido, subtotalProdutos, frete);
        }

        FormaPagamento formaPagamento = formaPagamentoRegistry.buscar(codigoFormaPagamento)
                .orElseThrow(() -> new ErroNegocioException("FORMA_PAGAMENTO_INVALIDA"));
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new ErroNegocioException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal totalPedido = Dinheiro.arredondar(subtotalProdutos.subtract(descontoCupom).add(frete));

        if (!formaPagamento.disponivel(totalPedido)) {
            throw new ErroNegocioException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = resultadoPagamento.totalFinal().subtract(totalPedido);

        return new ResumoCompra(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoEntregaDias(),
                ajustePagamento,
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela());
    }
}
