package com.loja.checkout.servico;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.comum.CheckoutException;
import com.loja.checkout.comum.CodigoErro;
import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.clube.CatalogoNiveisClube;
import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.cupom.CatalogoCupons;
import com.loja.checkout.dominio.cupom.ContextoCupom;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.entrega.CatalogoEntregas;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.pagamento.CatalogoFormasPagamento;
import com.loja.checkout.dominio.pagamento.ContextoPagamento;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.ResultadoPagamento;
import com.loja.checkout.dominio.regiao.Regiao;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/** Monta o resumo da compra que o site mostra antes de confirmar o pedido. */
@Service
public class CalculadoraResumoService {

    private final CatalogoEntregas entregas;
    private final CatalogoCupons cupons;
    private final CatalogoNiveisClube niveisClube;
    private final CatalogoFormasPagamento formasPagamento;

    public CalculadoraResumoService(CatalogoEntregas entregas,
                                    CatalogoCupons cupons,
                                    CatalogoNiveisClube niveisClube,
                                    CatalogoFormasPagamento formasPagamento) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.niveisClube = niveisClube;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcular(ResumoRequest request) {
        // 1 a 9: as validacoes seguem a ordem combinada com o site.
        Pedido pedido = montarPedido(request);
        NivelClube nivel = niveisClube.buscar(texto(request == null ? null : request.nivelClube()))
                .orElseThrow(() -> new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = regiao(request.regiao());
        ModalidadeEntrega modalidade = entregas.buscar(texto(request.modalidadeEntrega()))
                .orElseThrow(() -> new CheckoutException(CodigoErro.MODALIDADE_INVALIDA));
        if (!modalidade.atende(pedido)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = nivel.freteGratis() ? Dinheiro.ZERO : modalidade.calcularFrete(pedido);

        ContextoCupom contextoCupom = new ContextoCupom(pedido, subtotalProdutos, frete);
        BigDecimal descontoCupom = calcularDescontoCupom(request.cupom(), contextoCupom);

        BigDecimal imposto = Dinheiro.percentual(subtotalProdutos.subtract(descontoCupom), regiao.aliquotaPercentual());
        BigDecimal totalSemImposto = Dinheiro.centavos(subtotalProdutos.subtract(descontoCupom).add(frete));
        BigDecimal totalPedido = Dinheiro.centavos(totalSemImposto.add(imposto));

        FormaPagamento formaPagamento = formasPagamento.buscar(texto(request.formaPagamento()))
                .orElseThrow(() -> new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelasPermitidas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        ContextoPagamento contextoPagamento = new ContextoPagamento(totalPedido, totalSemImposto);
        if (!formaPagamento.atende(contextoPagamento)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento pagamento = formaPagamento.calcular(contextoPagamento, parcelas);
        BigDecimal ajustePagamento = Dinheiro.centavos(pagamento.totalFinal().subtract(totalPedido));

        return new ResumoResponse(
                subtotalProdutos,
                Dinheiro.centavos(descontoCupom),
                Dinheiro.centavos(frete),
                modalidade.prazoDias(),
                imposto,
                ajustePagamento,
                Dinheiro.centavos(pagamento.totalFinal()),
                parcelas,
                Dinheiro.centavos(pagamento.valorParcela()),
                nivel.calcularCredito(subtotalProdutos),
                nivel.temBrinde(subtotalProdutos));
    }

    private Pedido montarPedido(ResumoRequest request) {
        List<ItemRequest> itens = request == null ? null : request.itens();
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Pedido(itens.stream().map(this::montarItem).toList());
    }

    private ItemPedido montarItem(ItemRequest item) {
        if (item == null
                || !Dinheiro.positivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || !Dinheiro.positivo(item.pesoKg())) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private Regiao regiao(String codigo) {
        try {
            return Regiao.valueOf(texto(codigo));
        } catch (IllegalArgumentException | NullPointerException excecao) {
            throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
        }
    }

    private BigDecimal calcularDescontoCupom(String codigo, ContextoCupom contexto) {
        String informado = texto(codigo);
        if (informado == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(informado)
                .orElseThrow(() -> new CheckoutException(CodigoErro.CUPOM_INVALIDO));
        if (!cupom.aplicavel(contexto)) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return cupom.calcularDesconto(contexto);
    }

    /** Campos de texto ausentes ou em branco viram nulo. */
    private String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }
}
