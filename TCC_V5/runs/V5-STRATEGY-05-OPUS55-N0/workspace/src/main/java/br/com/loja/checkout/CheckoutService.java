package br.com.loja.checkout;

import static br.com.loja.checkout.dominio.CodigoErro.*;

import br.com.loja.checkout.api.ItemRequest;
import br.com.loja.checkout.api.ResumoRequest;
import br.com.loja.checkout.api.ResumoResponse;
import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.ContextoCupom;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.dominio.Catalogo;
import br.com.loja.checkout.dominio.CheckoutException;
import br.com.loja.checkout.dominio.CodigoErro;
import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.ItemPedido;
import br.com.loja.checkout.dominio.Pedido;
import br.com.loja.checkout.dominio.Regiao;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.FormaPagamento;
import br.com.loja.checkout.pagamento.ResultadoPagamento;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Calcula o resumo da compra. As validações seguem a ordem combinada com o site; o primeiro problema encontrado é devolvido. */
@Service
public class CheckoutService {

    private final Catalogo<NivelClube> niveis;
    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<FormaPagamento> formasPagamento;

    public CheckoutService(List<NivelClube> niveis, List<ModalidadeEntrega> modalidades, List<Cupom> cupons,
            List<FormaPagamento> formasPagamento) {
        this.niveis = new Catalogo<>(niveis);
        this.modalidades = new Catalogo<>(modalidades);
        this.cupons = new Catalogo<>(cupons);
        this.formasPagamento = new Catalogo<>(formasPagamento);
    }

    public ResumoResponse calcularResumo(ResumoRequest request) {
        if (request == null) {
            throw new CheckoutException(PEDIDO_INVALIDO);
        }
        Pedido pedido = montarPedido(request.itens());
        NivelClube nivel = niveis.buscar(request.nivelClube()).orElseThrow(() -> erro(NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.buscar(request.regiao()).orElseThrow(() -> erro(REGIAO_INVALIDA));
        ModalidadeEntrega modalidade = modalidades.buscar(request.modalidadeEntrega())
                .orElseThrow(() -> erro(MODALIDADE_INVALIDA));
        if (!modalidade.atende(pedido)) {
            throw erro(MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotal = pedido.subtotalProdutos();
        BigDecimal frete = nivel.isentaFrete() ? Dinheiro.ZERO : Dinheiro.arredondar(modalidade.calcularFrete(pedido));
        BigDecimal desconto = calcularDescontoCupom(request.cupom(), new ContextoCupom(pedido, frete));

        FormaPagamento formaPagamento = formasPagamento.buscar(request.formaPagamento())
                .orElseThrow(() -> erro(FORMA_PAGAMENTO_INVALIDA));
        int parcelas = lerParcelas(request.parcelas());
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw erro(PARCELAMENTO_INVALIDO);
        }

        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = Dinheiro.arredondar(subtotal.subtract(desconto).add(frete).add(seguro));
        if (!formaPagamento.atende(totalPedido)) {
            throw erro(FORMA_PAGAMENTO_INDISPONIVEL);
        }
        ResultadoPagamento pagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal totalFinal = Dinheiro.arredondar(pagamento.totalFinal());

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                totalFinal.subtract(totalPedido),
                totalFinal,
                pagamento.parcelas(),
                Dinheiro.arredondar(pagamento.valorParcela()),
                Dinheiro.percentual(subtotal, nivel.percentualCredito()),
                nivel.ganhaBrinde(pedido));
    }

    private Pedido montarPedido(List<ItemRequest> itensRequest) {
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw erro(PEDIDO_INVALIDO);
        }
        List<ItemPedido> itens = new ArrayList<>();
        for (ItemRequest item : itensRequest) {
            if (item == null || !positivo(item.precoUnitario()) || !positivo(item.pesoKg())
                    || !positivo(item.quantidade()) || !inteiro(item.quantidade())) {
                throw erro(PEDIDO_INVALIDO);
            }
            itens.add(new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade().intValueExact(), item.pesoKg()));
        }
        return new Pedido(itens);
    }

    private BigDecimal calcularDescontoCupom(String codigo, ContextoCupom contexto) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo).orElseThrow(() -> erro(CUPOM_INVALIDO));
        if (!cupom.aplicavel(contexto)) {
            throw erro(CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.arredondar(cupom.desconto(contexto));
    }

    private static int lerParcelas(BigDecimal parcelas) {
        if (parcelas == null) {
            return 1;
        }
        if (!inteiro(parcelas)) {
            throw erro(PARCELAMENTO_INVALIDO);
        }
        return parcelas.intValueExact();
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    /** Número inteiro que cabe num int. */
    private static boolean inteiro(BigDecimal valor) {
        try {
            valor.intValueExact();
            return true;
        } catch (ArithmeticException e) {
            return false;
        }
    }

    private static CheckoutException erro(CodigoErro codigo) {
        return new CheckoutException(codigo);
    }
}
