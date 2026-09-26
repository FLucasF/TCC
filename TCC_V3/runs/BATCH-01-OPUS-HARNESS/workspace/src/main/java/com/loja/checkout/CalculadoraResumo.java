package com.loja.checkout;

import com.loja.checkout.cupom.CatalogoCupons;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.SemCupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.entrega.CatalogoModalidades;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.CatalogoFormasPagamento;
import com.loja.checkout.pagamento.Cobranca;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * A sequencia do resumo, igual para todo pedido: validar, somar produtos,
 * cobrar o frete, descontar o cupom e aplicar o ajuste do pagamento.
 */
@Service
public class CalculadoraResumo {

    private static final int PARCELAS_PADRAO = 1;

    private final CatalogoModalidades modalidades;
    private final CatalogoCupons cupons;
    private final CatalogoFormasPagamento formasPagamento;

    public CalculadoraResumo(CatalogoModalidades modalidades,
                             CatalogoCupons cupons,
                             CatalogoFormasPagamento formasPagamento) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcular(ResumoRequest requisicao) {
        Pedido pedido = lerPedido(requisicao);

        ModalidadeEntrega modalidade = modalidades.buscar(requisicao.modalidadeEntrega())
                .orElseThrow(() -> new ErroCheckout("MODALIDADE_INVALIDA"));
        if (!modalidade.atende(pedido)) {
            throw new ErroCheckout("MODALIDADE_INDISPONIVEL");
        }
        BigDecimal frete = modalidade.frete(pedido);

        ContextoCupom contexto = new ContextoCupom(pedido, frete);
        Cupom cupom = lerCupom(requisicao.cupom());
        if (!cupom.aplicavel(contexto)) {
            throw new ErroCheckout("CUPOM_NAO_APLICAVEL");
        }
        BigDecimal descontoCupom = cupom.desconto(contexto);

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal totalPedido = Dinheiro.centavos(subtotalProdutos.subtract(descontoCupom).add(frete));

        FormaPagamento formaPagamento = formasPagamento.buscar(requisicao.formaPagamento())
                .orElseThrow(() -> new ErroCheckout("FORMA_PAGAMENTO_INVALIDA"));
        int parcelas = requisicao.parcelas() == null ? PARCELAS_PADRAO : requisicao.parcelas();
        if (!formaPagamento.aceitaParcelamento(parcelas)) {
            throw new ErroCheckout("PARCELAMENTO_INVALIDO");
        }
        if (!formaPagamento.atende(totalPedido)) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        Cobranca cobranca = formaPagamento.cobrar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoEntregaDias(),
                Dinheiro.centavos(cobranca.totalFinal().subtract(totalPedido)),
                cobranca.totalFinal(),
                cobranca.parcelas(),
                cobranca.valorParcela());
    }

    private Pedido lerPedido(ResumoRequest requisicao) {
        List<ItemRequest> itens = requisicao.itens();
        if (itens == null || itens.isEmpty()) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }
        return new Pedido(itens.stream().map(this::lerItem).toList());
    }

    private ItemPedido lerItem(ItemRequest item) {
        if (item == null
                || !positivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || !positivo(item.pesoKg())) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private Cupom lerCupom(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return SemCupom.INSTANCIA;
        }
        return cupons.buscar(codigo).orElseThrow(() -> new ErroCheckout("CUPOM_INVALIDO"));
    }
}
