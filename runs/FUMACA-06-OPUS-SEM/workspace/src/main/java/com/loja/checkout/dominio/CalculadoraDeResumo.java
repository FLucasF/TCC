package com.loja.checkout.dominio;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoPedidoRequest;
import com.loja.checkout.api.ResumoPedidoResponse;
import com.loja.checkout.cupom.CatalogoDeCupons;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.CatalogoDeEntregas;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.CatalogoDePagamentos;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Monta o resumo da compra: produtos, cupom, frete, total e ajuste do pagamento. */
@Service
public class CalculadoraDeResumo {

    private final CatalogoDeEntregas entregas;
    private final CatalogoDeCupons cupons;
    private final CatalogoDePagamentos pagamentos;

    public CalculadoraDeResumo(CatalogoDeEntregas entregas, CatalogoDeCupons cupons,
            CatalogoDePagamentos pagamentos) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.pagamentos = pagamentos;
    }

    public ResumoPedidoResponse calcular(ResumoPedidoRequest request) {
        if (request == null) {
            throw new ErroDeNegocio(CodigoErro.PEDIDO_INVALIDO);
        }

        Pedido pedido = montarPedido(request.itens());

        ModalidadeEntrega entrega = entregas.buscar(request.modalidadeEntrega())
                .orElseThrow(() -> new ErroDeNegocio(CodigoErro.MODALIDADE_INVALIDA));
        if (!entrega.atende(pedido)) {
            throw new ErroDeNegocio(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotal = pedido.subtotalProdutos();
        BigDecimal frete = Dinheiro.centavos(entrega.frete(pedido));
        int prazo = entrega.prazoDias(pedido);

        BigDecimal descontoCupom = Dinheiro.ZERO;
        if (request.cupom() != null && !request.cupom().isBlank()) {
            Cupom cupom = cupons.buscar(request.cupom())
                    .orElseThrow(() -> new ErroDeNegocio(CodigoErro.CUPOM_INVALIDO));
            if (!cupom.aplicavel(pedido, frete)) {
                throw new ErroDeNegocio(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = Dinheiro.centavos(cupom.desconto(pedido, frete));
        }

        BigDecimal totalPedido = Dinheiro.centavos(subtotal.subtract(descontoCupom).add(frete));

        FormaPagamento pagamento = pagamentos.buscar(request.formaPagamento())
                .orElseThrow(() -> new ErroDeNegocio(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!pagamento.permiteParcelas(parcelas)) {
            throw new ErroDeNegocio(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!pagamento.atende(totalPedido)) {
            throw new ErroDeNegocio(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);
        BigDecimal totalFinal = Dinheiro.centavos(resultado.totalFinal());
        BigDecimal ajuste = Dinheiro.centavos(totalFinal.subtract(totalPedido));

        return new ResumoPedidoResponse(subtotal, descontoCupom, frete, prazo, ajuste, totalFinal,
                parcelas, Dinheiro.centavos(resultado.valorParcela()));
    }

    private Pedido montarPedido(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ErroDeNegocio(CodigoErro.PEDIDO_INVALIDO);
        }
        List<Item> convertidos = new ArrayList<>(itens.size());
        for (ItemRequest item : itens) {
            if (item == null
                    || !positivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || !positivo(item.pesoKg())) {
                throw new ErroDeNegocio(CodigoErro.PEDIDO_INVALIDO);
            }
            convertidos.add(new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return new Pedido(convertidos);
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }
}
