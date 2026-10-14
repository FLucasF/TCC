package com.loja.pedidos.service;

import com.loja.pedidos.dto.CheckoutRequestDTO;
import com.loja.pedidos.dto.ItemDTO;
import com.loja.pedidos.dto.ResumoDTO;
import com.loja.pedidos.domain.Produto;
import com.loja.pedidos.repository.ProdutoRepository;
import com.loja.pedidos.repository.ClienteRepository;
import com.loja.pedidos.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class ResumoPedidoService {
    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private TransLogService transLogService;

    public ResumoDTO calcularResumo(CheckoutRequestDTO req) {
        ResumoDTO resumo = new ResumoDTO();

        // 1. Soma dos produtos
        BigDecimal subtotal = calcularSubtotal(req.getItens());
        resumo.setSubtotalProdutos(subtotal);

        // 3. Frete (calcular antes do desconto para FRETEGRATIS)
        BigDecimal pesoTotal = calcularPesoTotal(req.getItens());
        BigDecimal frete = calcularFrete(req.getModalidadeEntrega(), pesoTotal, req.getCep(), req.getRegiao());

        // 2. Desconto do cupom (agora com conhecimento do frete)
        BigDecimal descontoCupom = calcularDescontoCupom(req.getCupom(), req.getItens(), subtotal, frete, req.getNivelClube(), req.getCliente().getCpf());
        resumo.setDescontoCupom(descontoCupom);

        resumo.setFrete(frete);

        // Prazo de entrega
        int prazo = calcularPrazoEntrega(req.getModalidadeEntrega(), pesoTotal, req.getCep(), req.getRegiao());
        resumo.setPrazoEntregaDias(prazo);

        // 4. Seguro
        BigDecimal seguro = calcularSeguro(subtotal, req.getRegiao());
        resumo.setSeguro(seguro);

        // Total antes do ajuste de pagamento
        BigDecimal totalAntesAjuste = MoneyUtil.add(subtotal, frete, seguro).subtract(descontoCupom);

        // 5. Ajuste da forma de pagamento
        BigDecimal ajustePagamento = calcularAjustePagamento(req.getFormaPagamento(), totalAntesAjuste, req.getParcelas());
        resumo.setAjustePagamento(ajustePagamento);

        // Total final
        BigDecimal totalFinal = MoneyUtil.add(totalAntesAjuste, ajustePagamento);
        resumo.setTotalFinal(totalFinal);

        // Parcelamento
        int parcelas = req.getParcelas() != null ? req.getParcelas() : 1;
        resumo.setParcelas(parcelas);

        BigDecimal valorParcela = MoneyUtil.divideRounded(totalFinal, new BigDecimal(parcelas));
        resumo.setValorParcela(valorParcela);

        // Crédito para próxima compra
        BigDecimal creditoProxima = calcularCreditoClube(req.getNivelClube(), subtotal);
        resumo.setCreditoProximaCompra(creditoProxima);

        // Brinde
        boolean brinde = "OURO".equals(req.getNivelClube()) && subtotal.compareTo(new BigDecimal("500.00")) >= 0;
        resumo.setBrinde(brinde);

        return resumo;
    }

    private BigDecimal calcularSubtotal(java.util.List<ItemDTO> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemDTO item : itens) {
            Optional<Produto> produto = produtoRepository.findById(item.getSku());
            if (produto.isPresent()) {
                BigDecimal preco = produto.get().getPreco();
                BigDecimal quantidade = new BigDecimal(item.getQuantidade());
                total = total.add(preco.multiply(quantidade));
            }
        }
        return MoneyUtil.roundToNearestEven(total);
    }

    private BigDecimal calcularPesoTotal(java.util.List<ItemDTO> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemDTO item : itens) {
            Optional<Produto> produto = produtoRepository.findById(item.getSku());
            if (produto.isPresent()) {
                BigDecimal pesoUnitario = produto.get().getPesoKg();
                BigDecimal quantidade = new BigDecimal(item.getQuantidade());
                peso = peso.add(pesoUnitario.multiply(quantidade));
            }
        }
        return peso;
    }

    private BigDecimal calcularDescontoCupom(String cupom, java.util.List<ItemDTO> itens,
                                             BigDecimal subtotal, BigDecimal frete, String nivelClube, String cpf) {
        if (cupom == null || cupom.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal desconto = BigDecimal.ZERO;

        if ("BEMVINDO10".equals(cupom)) {
            if (ehPrimeiraCompra(cpf)) {
                desconto = MoneyUtil.percentageOf(subtotal, new BigDecimal("10"));
            }
        } else if ("MENOS50".equals(cupom)) {
            if (subtotal.compareTo(new BigDecimal("300.00")) >= 0) {
                desconto = new BigDecimal("50.00");
            }
        } else if ("FRETEGRATIS".equals(cupom)) {
            desconto = frete;
        } else if ("LEVE3PAGUE2".equals(cupom)) {
            desconto = calcularDescontoLeve3Pague2(itens);
        }

        BigDecimal maximoDesconto = MoneyUtil.percentageOf(subtotal, new BigDecimal("30"));
        desconto = MoneyUtil.min(desconto, maximoDesconto);

        return desconto;
    }

    private boolean ehPrimeiraCompra(String cpf) {
        return pedidoRepository.findPedidosCompletados(cpf).isEmpty();
    }

    private BigDecimal calcularDescontoLeve3Pague2(java.util.List<ItemDTO> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemDTO item : itens) {
            Optional<Produto> produto = produtoRepository.findById(item.getSku());
            if (produto.isPresent()) {
                int gratis = item.getQuantidade() / 3;
                BigDecimal descontoItem = produto.get().getPreco().multiply(new BigDecimal(gratis));
                desconto = desconto.add(descontoItem);
            }
        }
        return MoneyUtil.roundToNearestEven(desconto);
    }

    private BigDecimal calcularFrete(String modalidade, BigDecimal pesoKg, String cep, String regiao) {
        if ("RETIRADA_LOJA".equals(modalidade)) {
            return BigDecimal.ZERO;
        }
        if ("MOTOBOY".equals(modalidade)) {
            return new BigDecimal("18.00");
        }

        String service = "ECONOMICA".equals(modalidade) ? "STD" : "EXP";
        TransLogService.FreteResult resultado = transLogService.consultarFrete(cep, pesoKg, service);

        if (resultado.available) {
            return resultado.price;
        }
        return BigDecimal.ZERO;
    }

    private int calcularPrazoEntrega(String modalidade, BigDecimal pesoKg, String cep, String regiao) {
        int diasAdicional = 0;

        if ("NORTE".equals(regiao)) {
            diasAdicional = 3;
        } else if ("NORDESTE".equals(regiao)) {
            diasAdicional = 2;
        }

        if ("RETIRADA_LOJA".equals(modalidade)) {
            return 1 + diasAdicional;
        }
        if ("MOTOBOY".equals(modalidade)) {
            return 0 + diasAdicional;
        }

        String service = "ECONOMICA".equals(modalidade) ? "STD" : "EXP";
        TransLogService.FreteResult resultado = transLogService.consultarFrete(cep, pesoKg, service);

        if (resultado.available) {
            return resultado.dias + diasAdicional;
        }
        return 0;
    }

    private BigDecimal calcularSeguro(BigDecimal subtotal, String regiao) {
        BigDecimal percentual;
        BigDecimal limite = null;

        switch (regiao) {
            case "SUDESTE":
            case "SUL":
                percentual = new BigDecimal("1");
                break;
            case "CENTRO_OESTE":
                percentual = new BigDecimal("1.5");
                break;
            case "NORTE":
                percentual = new BigDecimal("2.5");
                limite = new BigDecimal("30.00");
                break;
            case "NORDESTE":
                percentual = new BigDecimal("2");
                limite = new BigDecimal("30.00");
                break;
            default:
                percentual = new BigDecimal("1");
        }

        BigDecimal seguro = MoneyUtil.percentageOf(subtotal, percentual);
        if (limite != null) {
            seguro = MoneyUtil.min(seguro, limite);
        }

        return seguro;
    }

    private BigDecimal calcularAjustePagamento(String forma, BigDecimal total, Integer parcelas) {
        if ("PIX".equals(forma)) {
            return MoneyUtil.percentageOf(total, new BigDecimal("-5"));
        }
        if ("BOLETO".equals(forma)) {
            return new BigDecimal("3.49");
        }
        if ("CARTAO".equals(forma)) {
            int parcel = parcelas != null ? parcelas : 1;
            if (parcel <= 3) {
                return BigDecimal.ZERO;
            }
            return calcularJuroCartao(total, parcel);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calcularJuroCartao(BigDecimal total, int parcelas) {
        BigDecimal taxaMensal = new BigDecimal("0.0199");
        BigDecimal um = new BigDecimal("1");
        BigDecimal umMaisTaxa = um.add(taxaMensal);

        BigDecimal denom = um.subtract(
            umMaisTaxa.pow(parcelas * -1, new java.math.MathContext(10, java.math.RoundingMode.HALF_EVEN))
        );

        BigDecimal parcela = total.multiply(taxaMensal).divide(denom, 10, java.math.RoundingMode.HALF_EVEN);
        BigDecimal totalComJuros = parcela.multiply(new BigDecimal(parcelas));
        BigDecimal juros = MoneyUtil.subtract(totalComJuros, total);

        return MoneyUtil.roundToNearestEven(juros);
    }

    private BigDecimal calcularCreditoClube(String nivelClube, BigDecimal subtotal) {
        if ("PRATA".equals(nivelClube)) {
            return MoneyUtil.percentageOf(subtotal, new BigDecimal("2"));
        }
        if ("OURO".equals(nivelClube)) {
            return MoneyUtil.percentageOf(subtotal, new BigDecimal("5"));
        }
        return BigDecimal.ZERO;
    }
}
