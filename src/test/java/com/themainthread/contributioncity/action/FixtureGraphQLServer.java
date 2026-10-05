package com.themainthread.contributioncity.action;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import com.sun.net.httpserver.HttpServer;
import com.themainthread.contributioncity.Fixtures;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class FixtureGraphQLServer implements QuarkusTestResourceLifecycleManager {
    private HttpServer server;

    @Override
    public Map<String, String> start() {
        try {
            Path requests = Path.of("target/wiring-requests.jsonl");
            if (!Files.exists(requests)) {
                Files.writeString(requests, "");
            }
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/graphql", exchange -> {
                String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Files.writeString(requests, request + "\n", StandardOpenOption.APPEND);
                byte[] response = Fixtures
                        .json(request.contains("missing-user") ? "graphql-error.json" : "contribution-calendar.json")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(response);
                }
            });
            server.start();
            return Map.of("city.test.graphql.url", "http://127.0.0.1:" + server.getAddress().getPort() + "/graphql");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}
