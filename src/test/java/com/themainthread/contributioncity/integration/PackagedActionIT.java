package com.themainthread.contributioncity.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.xml.sax.InputSource;

import com.sun.net.httpserver.HttpServer;
import com.themainthread.contributioncity.Fixtures;

import jakarta.json.Json;
import jakarta.json.JsonObject;

class PackagedActionIT {
    private static final String TOKEN = "fixture-token";

    @TempDir
    Path directory;

    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private HttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        assertTrue(Files.isRegularFile(Path.of(System.getProperty("city.test.jar"))),
                "Run ./mvnw verify to build the application before integration tests");
        byte[] response = Fixtures.json("contribution-calendar.json").getBytes(StandardCharsets.UTF_8);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/graphql", exchange -> {
            try (exchange) {
                requests.add(new Request(exchange.getRequestMethod(), exchange.getRequestHeaders().getFirst("Authorization"),
                        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void javaAndJbangProduceIdenticalSvgOutputsAndSummaryWithOneRequestEach() throws Exception {
        String previous = null;
        for (Launcher launcher : Launcher.values()) {
            int before = requests.size();
            run(launcher, "fixture-user").requireSuccess();
            assertEquals(before + 1, requests.size());
            Request request = requests.getLast();
            assertEquals("POST", request.method());
            assertEquals("Bearer " + TOKEN, request.authorization());
            try (var reader = Json.createReader(new StringReader(request.body()))) {
                JsonObject body = reader.readObject();
                assertEquals(Json.createObjectBuilder().add("login", "fixture-user").build(),
                        body.getJsonObject("variables"));
                assertFalse(body.getString("query").contains("rateLimit"));
            }
            String svg = Files.readString(directory.resolve("contribution-city.svg"));
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            assertEquals("svg", factory.newDocumentBuilder().parse(new InputSource(new StringReader(svg)))
                    .getDocumentElement().getTagName());
            assertEquals("svg-path=contribution-city.svg\ntotal-contributions=44\n",
                    Files.readString(directory.resolve("output.txt")).replace("\r\n", "\n"));
            assertTrue(Files.readString(directory.resolve("summary.md")).contains("4 weeks rendered"));
            if (previous != null) {
                assertEquals(previous, svg);
            }
            previous = svg;
            Files.delete(directory.resolve("contribution-city.svg"));
        }
    }

    @ParameterizedTest
    @EnumSource(Launcher.class)
    void invalidInputFailsBeforeNetworkOrSuccessOutput(Launcher launcher) throws Exception {
        assertNotEquals(0, run(launcher, "bad/user").exitCode());
        assertTrue(requests.isEmpty());
        assertFalse(Files.exists(directory.resolve("contribution-city.svg")));
        assertEquals("", Files.readString(directory.resolve("output.txt")));
        assertEquals("", Files.readString(directory.resolve("summary.md")));
    }

    private TestProcesses.Result run(Launcher launcher, String username) throws Exception {
        Path output = Files.writeString(directory.resolve("output.txt"), "");
        Path summary = Files.writeString(directory.resolve("summary.md"), "");
        String inputs = Json.createObjectBuilder().add("username", username).add("github-token", TOKEN)
                .add("weeks", "4").add("height", "3").build().toString();
        Map<String, String> environment = Map.of("JSON_INPUTS", inputs, "GITHUB_TOKEN", TOKEN,
                "GITHUB_GRAPHQL_URL", "http://127.0.0.1:" + server.getAddress().getPort() + "/graphql",
                "GITHUB_EVENT_NAME", "workflow_dispatch", "GITHUB_OUTPUT", output.toString(),
                "GITHUB_STEP_SUMMARY", summary.toString());
        List<String> command = new ArrayList<>(launcher == Launcher.JAVA
                ? List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-jar")
                : List.of("jbang", "--java", "21"));
        command.add(Path.of(System.getProperty("city.test.jar")).toAbsolutePath().toString());
        TestProcesses.Result result = TestProcesses.run(directory, environment, command);
        assertFalse(result.output().contains(TOKEN), "The action must keep its token out of process output");
        return result;
    }

    enum Launcher {
        JAVA,
        JBANG
    }

    private record Request(String method, String authorization, String body) {
    }
}
