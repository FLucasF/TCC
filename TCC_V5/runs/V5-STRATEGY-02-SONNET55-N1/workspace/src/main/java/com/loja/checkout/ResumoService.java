package com.loja.checkout;

import static com.loja.checkout.dominio.CodigoErro.*;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemCarrinho;
import com.loja.checkout.dominio.PedidoRecusadoException;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.entrega.Entrega;
import com.loja.checkout.pagamento.Cobranca;
import com.loja.checkout.pagamento.FormaPagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {

    private final Catalogo<NivelClube> niveis;
    private final Catalogo<Regiao> regioes = new Catalogo<>(List.of(Regiao.values()));
    private final Catalogo<Entrega> entregas;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<FormaPagamento> pagamentos;

    public ResumoService(List<NivelClube> niveis, List<Entrega> entregas,
                         List<Cupom> cupons, List<FormaPagamento> pagamentos) {
        this.niveis = new Catalogo<>(niveis);
        this.entregas = new Catalogo<>(entregas);
        this.cupons = new Catalogo<>(cupons);
        this.pagamentos = new Catalogo<>(pagamentos);
    }

    public ResumoPedido calcular(PedidoSolicitado pedido) {
        Carrinho carrinho = montarCarrinho(pedido.itens());
        NivelClube nivel = exigir(niveis.buscar(pedido.nivelClube()), NIVEL_CLUBE_INVALIDO);
        Regiao regiao = exigir(regioes.buscar(pedido.regiao()), REGIAO_INVALIDA);
        Entrega entrega = exigir(entregas.buscar(pedido.modalidadeEntrega()), MODALIDADE_INVALIDA);
        if (!entrega.atende(carrinho)) {
            throw new PedidoRecusadoException(MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.isentaFrete() ? Dinheiro.ZERO : entrega.frete(carrinho);
        BigDecimal desconto = descontoDoCupom(pedido.cupom(), new ContextoCupom(carrinho, frete));
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        FormaPagamento pagamento = exigir(pagamentos.buscar(pedido.formaPagamento()), FORMA_PAGAMENTO_INVALIDA);
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!pagamento.aceitaParcelas(parcelas)) {
            throw new PedidoRecusadoException(PARCELAMENTO_INVALIDO);
        }
        if (!pagamento.disponivel(totalPedido)) {
            throw new PedidoRecusadoException(FORMA_PAGAMENTO_INDISPONIVEL);
        }
        Cobranca cobranca = pagamento.cobrar(totalPedido, parcelas);

        return new ResumoPedido(subtotal, desconto, frete, entrega.prazoDias(), seguro,
                cobranca.totalFinal().subtract(totalPedido), cobranca.totalFinal(), parcelas,
                cobranca.valorParcela(), nivel.credito(subtotal), nivel.brinde(subtotal));
    }

    private BigDecimal descontoDoCupom(String codigo, ContextoCupom contexto) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = exigir(cupons.buscar(codigo), CUPOM_INVALIDO);
        if (!cupom.aplicavel(contexto)) {
            throw new PedidoRecusadoException(CUPOM_NAO_APLICAVEL);
        }
        return cupom.desconto(contexto);
    }

    private Carrinho montarCarrinho(List<PedidoSolicitado.Item> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoRecusadoException(PEDIDO_INVALIDO);
        }
        return new Carrinho(itens.stream().map(this::montarItem).toList());
    }

    private ItemCarrinho montarItem(PedidoSolicitado.Item item) {
        if (item == null || !positivo(item.precoUnitario()) || !positivo(item.pesoKg())
                || item.quantidade() == null || item.quantidade() <= 0) {
            throw new PedidoRecusadoException(PEDIDO_INVALIDO);
        }
        return new ItemCarrinho(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private <T> T exigir(Optional<T> valor, CodigoErro erro) {
        return valor.orElseThrow(() -> new PedidoRecusadoException(erro));
    }
}
