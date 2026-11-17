package loja;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class Main {
    private static final LojaService loja = new LojaService();

    public static void main(String[] args) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 8765), 0);
        servidor.createContext("/", Main::atender);
        servidor.setExecutor(null);
        servidor.start();
        System.out.println("LOJA VÉRTICE iniciada: http://localhost:8765");
        System.out.println("Pressione Ctrl+C para parar o servidor.");
    }

    private static void atender(HttpExchange exchange) throws IOException {
        try {
            String metodo = exchange.getRequestMethod();
            String caminho = exchange.getRequestURI().getPath();
            if (!caminho.startsWith("/api/")) {
                arquivosEstaticos(exchange, caminho, metodo);
                return;
            }
            responderApi(exchange, caminho, metodo);
        } catch (ErroNegocio e) {
            responderJson(exchange, e.getCodigoHttp(), "{\"erro\":" + Json.entreAspas(e.getMessage()) + "}");
        } catch (NumberFormatException e) {
            responderJson(exchange, 400, "{\"erro\":\"Identificador ou quantidade inválido.\"}");
        } catch (Exception e) {
            System.err.println("Erro na requisição: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            responderJson(exchange, 500, "{\"erro\":\"Ocorreu um erro interno.\"}");
        } finally {
            exchange.close();
        }
    }

    private static void responderApi(HttpExchange exchange, String caminho, String metodo) throws IOException {
        String[] partes = caminho.substring(1).split("/");
        Map<String, String> parametros = parametros(exchange);

        if (metodo.equals("GET") && caminho.equals("/api/products")) {
            ArrayList<String> produtos = new ArrayList<>();
            for (Produto p : loja.listarProdutos()) produtos.add(p.toJson());
            responderJson(exchange, 200, Json.lista(produtos));
            return;
        }
        if (metodo.equals("GET") && partes.length == 3 && partes[1].equals("products")) {
            Produto produto = loja.buscarProduto(Integer.parseInt(partes[2]));
            responderJson(exchange, 200, produto.toJson());
            return;
        }
        if (metodo.equals("GET") && caminho.equals("/api/cart")) {
            responderJson(exchange, 200, loja.carrinhoJson());
            return;
        }
        if (metodo.equals("POST") && caminho.equals("/api/cart/items")) {
            int id = Integer.parseInt(obrigatorio(parametros, "produtoId"));
            int quantidade = Integer.parseInt(parametros.getOrDefault("quantidade", "1"));
            loja.adicionarAoCarrinho(id, quantidade);
            responderJson(exchange, 200, loja.carrinhoJson());
            return;
        }
        if (metodo.equals("DELETE") && partes.length == 4 && partes[1].equals("cart") && partes[2].equals("items")) {
            loja.removerDoCarrinho(Integer.parseInt(partes[3]));
            responderJson(exchange, 200, loja.carrinhoJson());
            return;
        }
        if (metodo.equals("POST") && caminho.equals("/api/checkout")) {
            Pedido pedido = loja.finalizarCompra(parametros.getOrDefault("cupom", ""));
            responderJson(exchange, 201, pedido.toJson());
            return;
        }
        if (metodo.equals("GET") && caminho.equals("/api/orders")) {
            boolean apenasAtivos = "active".equals(parametros.getOrDefault("view", "all"));
            ArrayList<String> json = new ArrayList<>();
            for (Pedido pedido : loja.listarPedidos(apenasAtivos)) json.add(pedido.toJson());
            responderJson(exchange, 200, Json.lista(json));
            return;
        }
        if (metodo.equals("POST") && partes.length == 4 && partes[1].equals("orders") && partes[3].equals("cancel")) {
            Pedido pedido = loja.cancelarPedido(Integer.parseInt(partes[2]));
            responderJson(exchange, 200, pedido.toJson());
            return;
        }
        throw new ErroNegocio(404, "Rota não encontrada.");
    }

    private static String obrigatorio(Map<String, String> parametros, String chave) {
        String valor = parametros.get(chave);
        if (valor == null || valor.isBlank()) throw new ErroNegocio(400, "Campo obrigatório: " + chave);
        return valor;
    }

    private static Map<String, String> parametros(HttpExchange exchange) throws IOException {
        Map<String, String> resultado = new LinkedHashMap<>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query != null) adicionarParametros(resultado, query);
        if (exchange.getRequestMethod().equals("POST")) {
            String corpo = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            adicionarParametros(resultado, corpo);
        }
        return resultado;
    }

    private static void adicionarParametros(Map<String, String> destino, String texto) {
        if (texto.isBlank()) return;
        for (String parte : texto.split("&")) {
            String[] dupla = parte.split("=", 2);
            String chave = URLDecoder.decode(dupla[0], StandardCharsets.UTF_8);
            String valor = URLDecoder.decode(dupla.length > 1 ? dupla[1] : "", StandardCharsets.UTF_8);
            destino.put(chave, valor);
        }
    }

    private static void arquivosEstaticos(HttpExchange exchange, String caminho, String metodo) throws IOException {
        if (!metodo.equals("GET")) throw new ErroNegocio(405, "Método não permitido.");
        String arquivo = switch (caminho) {
            case "/", "/index.html" -> "index.html";
            case "/styles.css" -> "styles.css";
            case "/app.js" -> "app.js";
            default -> null;
        };
        if (arquivo == null) throw new ErroNegocio(404, "Página não encontrada.");
        Path local = Path.of("web", arquivo);
        byte[] conteudo = Files.readAllBytes(local);
        String mime = arquivo.endsWith(".css") ? "text/css" : arquivo.endsWith(".js") ? "text/javascript" : "text/html";
        exchange.getResponseHeaders().set("Content-Type", mime + "; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, conteudo.length);
        exchange.getResponseBody().write(conteudo);
    }

    private static void responderJson(HttpExchange exchange, int codigo, String json) throws IOException {
        byte[] conteudo = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(codigo, conteudo.length);
        exchange.getResponseBody().write(conteudo);
    }
}
