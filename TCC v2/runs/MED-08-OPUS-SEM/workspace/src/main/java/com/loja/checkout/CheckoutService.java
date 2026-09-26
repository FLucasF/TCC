package com.loja.checkout;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.cupom.CatalogoCupons;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.entrega.CatalogoEntregas;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.CatalogoPagamentos;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Monta o resumo da compra que o cliente ve antes de confirmar o pedido. */
@Service
public class CheckoutService {

    private final CatalogoEntregas entregas;
    private final CatalogoCupons cupons;
    private final CatalogoPagamentos pagamentos;

    public CheckoutService(CatalogoEntregas entregas, CatalogoCupons cupons, CatalogoPagamentos pagamentos) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.pagamentos = pagamentos;
    }

    public ResumoResponse calcular(ResumoRequest requisicao) {
        Pedido pedido = validarPedido(requisicao);

        ModalidadeEntrega modalidade = entregas.buscar(requisicao.modalidadeEntrega())
                .orElseThrow(() -> new ErroCheckout(CodigoErro.MODALIDADE_INVALIDA));
        if (!modalidade.atende(pedido)) {
            throw new ErroCheckout(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = modalidade.frete(pedido);
        BigDecimal descontoCupom = calcularDescontoCupom(requisicao, pedido, frete);

        BigDecimal totalPedido = Dinheiro.arredondar(subtotalProdutos.subtract(descontoCupom).add(frete));

        FormaPagamento formaPagamento = pagamentos.buscar(requisicao.formaPagamento())
                .orElseThrow(() -> new ErroCheckout(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = requisicao.parcelas() == null ? 1 : requisicao.parcelas();
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new ErroCheckout(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.atende(totalPedido)) {
            throw new ErroCheckout(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal totalFinal = Dinheiro.arredondar(resultado.totalFinal());
        BigDecimal ajustePagamento = Dinheiro.arredondar(totalFinal.subtract(totalPedido));

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoEntregaDias(pedido),
                ajustePagamento,
                totalFinal,
                parcelas,
                Dinheiro.arredondar(resultado.valorParcela()));
    }

    private Pedido validarPedido(ResumoRequest requisicao) {
        if (requisicao == null || requisicao.itens() == null || requisicao.itens().isEmpty()) {
            throw new ErroCheckout(CodigoErro.PEDIDO_INVALIDO);
        }
        List<ItemPedido> itens = new ArrayList<>();
        for (ItemRequest item : requisicao.itens()) {
            if (item == null
                    || naoPositivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || naoPositivo(item.pesoKg())) {
                throw new ErroCheckout(CodigoErro.PEDIDO_INVALIDO);
            }
            itens.add(new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return new Pedido(itens);
    }

    private boolean naoPositivo(BigDecimal valor) {
        return valor == null || valor.compareTo(BigDecimal.ZERO) <= 0;
    }

    private BigDecimal calcularDescontoCupom(ResumoRequest requisicao, Pedido pedido, BigDecimal frete) {
        String codigo = requisicao.cupom();
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo).orElseThrow(() -> new ErroCheckout(CodigoErro.CUPOM_INVALIDO));
        ContextoCupom contexto = new ContextoCupom(pedido, frete);
        if (!cupom.aplicavel(contexto)) {
            throw new ErroCheckout(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.arredondar(cupom.desconto(contexto));
    }
}
