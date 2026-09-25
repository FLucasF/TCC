package com.loja.checkout.dominio;

import com.loja.checkout.api.RequisicaoResumo;
import com.loja.checkout.api.RespostaResumo;
import com.loja.checkout.cupom.CatalogoCupons;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.CatalogoEntregas;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.CatalogoPagamentos;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Monta o resumo da compra a partir do que o cliente escolheu. */
@Service
public class CalculadoraResumo {

    private final CatalogoEntregas entregas;
    private final CatalogoCupons cupons;
    private final CatalogoPagamentos pagamentos;

    public CalculadoraResumo(CatalogoEntregas entregas, CatalogoCupons cupons,
                             CatalogoPagamentos pagamentos) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.pagamentos = pagamentos;
    }

    public RespostaResumo calcular(RequisicaoResumo requisicao) {
        if (requisicao == null) {
            throw new ErroNegocio(CodigoErro.PEDIDO_INVALIDO);
        }

        // 1. Produtos
        Carrinho carrinho = montarCarrinho(requisicao.itens());
        BigDecimal subtotalProdutos = carrinho.subtotalProdutos();

        // 2. Entrega
        ModalidadeEntrega modalidade = entregas.buscar(requisicao.modalidadeEntrega())
                .orElseThrow(() -> new ErroNegocio(CodigoErro.MODALIDADE_INVALIDA));
        if (!modalidade.atende(carrinho)) {
            throw new ErroNegocio(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        BigDecimal frete = Dinheiro.centavos(modalidade.frete(carrinho));

        // 3. Cupom
        BigDecimal descontoCupom = calcularDesconto(requisicao.cupom(), carrinho, subtotalProdutos, frete);

        // 4. Total do pedido
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete));

        // 5. Pagamento
        FormaPagamento formaPagamento = pagamentos.buscar(requisicao.formaPagamento())
                .orElseThrow(() -> new ErroNegocio(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = requisicao.parcelas() == null ? 1 : requisicao.parcelas();
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new ErroNegocio(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.atende(totalPedido)) {
            throw new ErroNegocio(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        ResultadoPagamento pagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = Dinheiro.centavos(
                pagamento.totalFinal().subtract(totalPedido));

        return new RespostaResumo(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoEntregaDias(),
                ajustePagamento,
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela());
    }

    private Carrinho montarCarrinho(List<RequisicaoResumo.ItemRequisicao> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ErroNegocio(CodigoErro.PEDIDO_INVALIDO);
        }
        List<ItemPedido> validados = new ArrayList<>();
        for (RequisicaoResumo.ItemRequisicao item : itens) {
            if (item == null || !positivo(item.precoUnitario()) || !positivo(item.pesoKg())
                    || item.quantidade() == null || item.quantidade() <= 0) {
                throw new ErroNegocio(CodigoErro.PEDIDO_INVALIDO);
            }
            validados.add(new ItemPedido(item.nome(), item.precoUnitario(),
                    item.quantidade(), item.pesoKg()));
        }
        return new Carrinho(validados);
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal calcularDesconto(String codigoCupom, Carrinho carrinho,
                                        BigDecimal subtotalProdutos, BigDecimal frete) {
        if (codigoCupom == null || codigoCupom.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigoCupom)
                .orElseThrow(() -> new ErroNegocio(CodigoErro.CUPOM_INVALIDO));
        ContextoCupom contexto = new ContextoCupom(carrinho, subtotalProdutos, frete);
        if (!cupom.aplicavel(contexto)) {
            throw new ErroNegocio(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.centavos(cupom.desconto(contexto));
    }
}
