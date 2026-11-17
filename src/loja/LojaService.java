package loja;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LojaService {
    private final Map<Integer, Produto> catalogo = new LinkedHashMap<>();
    private final Map<Integer, Integer> carrinho = new LinkedHashMap<>();
    private final List<Pedido> pedidos = new ArrayList<>();
    private final PrecoService precoService = new PrecoService();
    private int proximoPedido = 1001;

    public LojaService() {
        adicionar(new Produto(1, "Headset Pixel", "Áudio", "Som imersivo para suas partidas.", "🎧", "violet", 17990, 5));
        adicionar(new Produto(2, "Mouse Orbit", "Setup", "Precisão e conforto o dia inteiro.", "🖱️", "blue", 8990, 3));
        adicionar(new Produto(3, "Teclado Vega", "Setup", "Teclas mecânicas e iluminação RGB.", "⌨️", "pink", 24990, 4));
        adicionar(new Produto(4, "Webcam Flow", "Streaming", "Imagem nítida em chamadas e lives.", "📷", "orange", 13990, 2));
        adicionar(new Produto(5, "Hub Nexus", "Acessórios", "Mais conexões para o seu notebook.", "🔌", "mint", 6990, 8));
        adicionar(new Produto(6, "Mousepad Arc", "Acessórios", "Superfície ampla e deslizamento suave.", "🌀", "yellow", 4990, 7));
    }

    private void adicionar(Produto produto) { catalogo.put(produto.getId(), produto); }

    public synchronized List<Produto> listarProdutos() { return new ArrayList<>(catalogo.values()); }
    public synchronized Produto buscarProduto(int id) { return produtoOuErro(id); }

    public synchronized void adicionarAoCarrinho(int produtoId, int quantidade) {
        Produto produto = produtoOuErro(produtoId);
        if (quantidade <= 0) throw new ErroNegocio(400, "A quantidade deve ser positiva.");
        int quantidadeAtual = carrinho.getOrDefault(produtoId, 0);
        int quantidadeNova = quantidadeAtual + quantidade;
        if ( quantidadeNova > produto.getEstoque() ) {
            throw new ErroNegocio(409, "Quantidade indisponível no estoque.");
        }
        carrinho.put(produtoId, quantidadeNova);
    }

    public synchronized void removerDoCarrinho(int produtoId) {
        carrinho.remove(produtoId);
    }

    public synchronized String carrinhoJson() {
        List<String> itens = new ArrayList<>();
        int subtotal = 0;
        int quantidadeTotal = 0;
        for (Map.Entry<Integer, Integer> item : carrinho.entrySet()) {
            Produto produto = produtoOuErro(item.getKey());
            int quantidade = item.getValue();
            int total = produto.getPrecoCentavos() * quantidade;
            subtotal += total;
            quantidadeTotal += quantidade;
            itens.add("{\"produtoId\":" + produto.getId() + ",\"nome\":" +
                    Json.entreAspas(produto.getNome()) + ",\"emoji\":" +
                    Json.entreAspas(produto.getEmoji()) + ",\"quantidade\":" + quantidade +
                    ",\"precoCentavos\":" + produto.getPrecoCentavos() +
                    ",\"totalCentavos\":" + total + "}");
        }
        return "{\"items\":" + Json.lista(itens) + ",\"subtotalCentavos\":" + subtotal +
               ",\"quantidadeTotal\":" + quantidadeTotal + "}";
    }

    public synchronized Pedido finalizarCompra(String cupom) {
        if (carrinho.isEmpty()) throw new ErroNegocio(400, "Adicione produtos antes de finalizar.");
        int subtotal = 0;
        List<String> itens = new ArrayList<>();
        for (Map.Entry<Integer, Integer> item : carrinho.entrySet()) {
            Produto produto = produtoOuErro(item.getKey());
            int quantidade = item.getValue();
            subtotal += produto.getPrecoCentavos() * quantidade;
            itens.add("{\"nome\":" + Json.entreAspas(produto.getNome()) +
                    ",\"quantidade\":" + quantidade + "}");
        }
        int desconto = precoService.desconto(subtotal, cupom);
        int frete = precoService.frete(subtotal);
        Pedido pedido = new Pedido(proximoPedido++, itens, subtotal - desconto + frete, new LinkedHashMap<>(carrinho));
        for (Map.Entry<Integer, Integer> item : carrinho.entrySet()) {
            produtoOuErro(item.getKey()).diminuirEstoque(item.getValue());
        }
        pedidos.add(pedido);
        carrinho.clear();
        return pedido;
    }

    public synchronized List<Pedido> listarPedidos(boolean apenasAtivos) {
        if (!apenasAtivos) return new ArrayList<>(pedidos);
        return pedidos.stream()
                .filter(pedido -> pedido.getStatus() != Pedido.Status.CANCELADO)
                .collect(Collectors.toList());
    }

    public synchronized Pedido cancelarPedido(int id) {
        for (Pedido pedido : pedidos) {
            if (pedido.getId() == id) {
                pedido.cancelar();
                for (Map.Entry<Integer, Integer> item : pedido.getQuantidades().entrySet()) {
                    produtoOuErro(item.getKey()).devolverAoEstoque(item.getValue());
                }
                return pedido;
            }
        }
        throw new ErroNegocio(404, "Pedido não encontrado.");
    }

    private Produto produtoOuErro(int id) {
        Produto produto = catalogo.get(id);
        if (produto == null) throw new ErroNegocio(404, "Produto não encontrado.");
        return produto;
    }
}
