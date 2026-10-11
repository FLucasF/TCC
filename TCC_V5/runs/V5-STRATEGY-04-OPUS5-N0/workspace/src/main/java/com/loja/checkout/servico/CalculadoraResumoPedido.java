package com.loja.checkout.servico;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoPedidoRequest;
import com.loja.checkout.api.ResumoPedidoResponse;
import com.loja.checkout.clube.CatalogoNiveisClube;
import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.CatalogoCupons;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.entrega.CatalogoEntregas;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.CatalogoFormasPagamento;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.seguro.CalculadoraSeguro;
import com.loja.checkout.seguro.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Monta o resumo da compra na ordem combinada: produtos, desconto do cupom, frete,
 * seguro, total do pedido e, por último, o ajuste da forma de pagamento.
 */
@Service
public class CalculadoraResumoPedido {

    private final CatalogoEntregas entregas;
    private final CatalogoCupons cupons;
    private final CatalogoNiveisClube niveisClube;
    private final CatalogoFormasPagamento formasPagamento;
    private final CalculadoraSeguro calculadoraSeguro;

    public CalculadoraResumoPedido(CatalogoEntregas entregas,
                                   CatalogoCupons cupons,
                                   CatalogoNiveisClube niveisClube,
                                   CatalogoFormasPagamento formasPagamento,
                                   CalculadoraSeguro calculadoraSeguro) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.niveisClube = niveisClube;
        this.formasPagamento = formasPagamento;
        this.calculadoraSeguro = calculadoraSeguro;
    }

    public ResumoPedidoResponse calcular(ResumoPedidoRequest request) {
        if (request == null) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }

        // 1. carrinho
        Pedido pedido = lerPedido(request.itens());
        BigDecimal subtotalProdutos = pedido.subtotalProdutos();

        // 2. clube e 3. região
        NivelClube nivelClube = niveisClube.buscar(request.nivelClube())
                .orElseThrow(() -> new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.buscar(request.regiao())
                .orElseThrow(() -> new CheckoutException(CodigoErro.REGIAO_INVALIDA));

        // 4. e 5. entrega
        ModalidadeEntrega modalidade = entregas.buscar(request.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException(CodigoErro.MODALIDADE_INVALIDA));
        if (!modalidade.atende(pedido)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        // O frete do nível com isenção sai zerado no resumo.
        BigDecimal frete = nivelClube.freteGratis() ? Dinheiro.ZERO : modalidade.frete(pedido);

        // 6. e 7. cupom
        ContextoCupom contextoCupom = new ContextoCupom(pedido, subtotalProdutos, frete);
        BigDecimal descontoCupom = Dinheiro.ZERO;
        if (temCupom(request.cupom())) {
            Cupom cupom = cupons.buscar(request.cupom())
                    .orElseThrow(() -> new CheckoutException(CodigoErro.CUPOM_INVALIDO));
            if (!cupom.aplicavel(contextoCupom)) {
                throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = cupom.desconto(contextoCupom);
        }

        BigDecimal seguro = calculadoraSeguro.calcular(regiao, subtotalProdutos);

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        // 8., 9. e 10. pagamento
        FormaPagamento formaPagamento = formasPagamento.buscar(request.formaPagamento())
                .orElseThrow(() -> new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.permiteParcelas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.atende(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento pagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = Dinheiro.arredondar(pagamento.totalFinal().subtract(totalPedido));

        BigDecimal credito = Dinheiro.percentual(subtotalProdutos, nivelClube.percentualCredito());

        return new ResumoPedidoResponse(
                subtotalProdutos,
                Dinheiro.arredondar(descontoCupom),
                Dinheiro.arredondar(frete),
                modalidade.prazoEntregaDias(),
                seguro,
                ajustePagamento,
                Dinheiro.arredondar(pagamento.totalFinal()),
                pagamento.parcelas(),
                Dinheiro.arredondar(pagamento.valorParcela()),
                credito,
                nivelClube.temBrinde(subtotalProdutos));
    }

    private Pedido lerPedido(List<ItemRequest> itensRecebidos) {
        if (itensRecebidos == null || itensRecebidos.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<Item> itens = new ArrayList<>(itensRecebidos.size());
        for (ItemRequest item : itensRecebidos) {
            if (item == null
                    || !positivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || !positivo(item.pesoKg())) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
            itens.add(new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return new Pedido(itens);
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    /** Cupom em branco ou ausente significa "cliente não usou cupom". */
    private boolean temCupom(String cupom) {
        return cupom != null && !cupom.isBlank();
    }
}
