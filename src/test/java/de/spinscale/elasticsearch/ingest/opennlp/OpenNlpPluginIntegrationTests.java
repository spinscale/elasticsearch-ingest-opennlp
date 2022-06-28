package de.spinscale.elasticsearch.ingest.opennlp;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.ingest.SimulateResponse;
import co.elastic.clients.elasticsearch.ingest.simulate.Document;
import co.elastic.clients.elasticsearch.ingest.simulate.PipelineSimulation;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import jakarta.json.JsonString;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
public class OpenNlpPluginIntegrationTests {

    private static String text = "Kobe Bryant was one of the best basketball players of all times. Not even Michael Jordan has ever scored 81 points in one game. Munich is really an awesome city, but New York is as well. Yesterday has been the hottest day of the year.";
    private static GenericContainer container;
    private static RestClient restClient;
    private static ElasticsearchClient client;

    @BeforeAll
    public static void startContainer() {
        ImageFromDockerfile image = new ImageFromDockerfile().withDockerfile(Paths.get("./Dockerfile"));
        container = new GenericContainer(image);
        container.addEnv("discovery.type", "single-node");
        container.withEnv("xpack.security.enabled", "false");
        container.addExposedPorts(9200);
        container.setWaitStrategy(new LogMessageWaitStrategy().withRegEx(".*(\"message\":\\s?\"started[\\s?|\"].*|] started\n$)"));

        container.start();
        container.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger(OpenNlpPluginIntegrationTests.class)));

        // Create the low-level client
        restClient = RestClient.builder(new HttpHost("localhost", container.getMappedPort(9200))).build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        client = new ElasticsearchClient(transport);
    }

    @AfterAll
    public static void stopContainer() throws IOException {
        if (restClient != null) {
            restClient.close();
        }
        if (container != null) {
            container.close();
        }
    }

    @Test
    public void testNodesInfo() throws Exception {
        String endpoint = String.format("http://localhost:%s/_nodes", container.getMappedPort(9200));
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(endpoint))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.body()).contains("\"name\":\"ingest-opennlp\"");
        assertThat(response.body()).contains("\"type\":\"opennlp\"");
    }

    @Test
    public void testProcessor() throws Exception {
        String endpoint = String.format("http://localhost:%s", container.getMappedPort(9200));
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + "/_ingest/pipeline/my_pipeline"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("""
          { "processors": [ { "opennlp" : { "field" : "field1" } } ] }
                        """))
                .build();
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        client.index(b -> b.index("test")
                .id("1")
                .pipeline("my_pipeline")
                .document(Map.of("field1", text))
        );

        GetResponse<Map> response = client.get(b -> b.index("test").id("1"), Map.class);

        assertThat(response.found()).isTrue();
        Map<String, Object> source = response.source();
        assertThat(source).containsKey("entities");
        Map<String, Object> entities = (Map<String, Object>) source.get("entities");
        assertThat(entities).containsOnlyKeys("dates", "locations", "names");
        List<String> dates = (List<String>) entities.get("dates");
        assertThat(dates).containsOnly("Yesterday");
        List<String> names = (List<String>) entities.get("names");
        assertThat(names).containsOnly("Kobe Bryant", "Michael Jordan");
        List<String> locations = (List<String>) entities.get("locations");
        assertThat(locations).containsOnly("Munich", "New York");
    }

    @Test
    public void testProcessorSelectFields() throws Exception {
        String endpoint = String.format("http://localhost:%s", container.getMappedPort(9200));
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + "/_ingest/pipeline/my_pipeline"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("""
          { "processors": [ { "opennlp" : { "field" : "field1", "fields": ["locations"] } } ] }
                        """))
                .build();
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        client.index(b -> b.index("test")
                .id("1")
                .pipeline("my_pipeline")
                .document(Map.of("field1", text))
        );

        GetResponse<Map> response = client.get(b -> b.index("test").id("1"), Map.class);

        assertThat(response.found()).isTrue();
        Map<String, Object> source = response.source();
        assertThat(source).containsKey("entities");
        Map<String, Object> entities = (Map<String, Object>) source.get("entities");
        assertThat(entities).containsOnlyKeys("locations");
        List<String> locations = (List<String>) entities.get("locations");
        assertThat(locations).containsOnly("Munich", "New York");
    }

    @Test
    public void testProcessorAnnotatedTextOutput() throws Exception {
        String endpoint = String.format("http://localhost:%s", container.getMappedPort(9200));
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + "/_ingest/pipeline/my_pipeline"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("""
          { "processors": [ { "opennlp" : { "field" : "field1", "annotated_text_field" : "annotated_text" } } ] }
                        """))
                .build();
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        client.index(b -> b.index("test")
                .id("1")
                .pipeline("my_pipeline")
                .document(Map.of("field1", text))
        );

        GetResponse<Map> response = client.get(b -> b.index("test").id("1"), Map.class);

        assertThat(response.found()).isTrue();
        Map<String, Object> source = response.source();
        assertThat(source).containsKey("annotated_text");
        String expectedAnnotatedText = "[Kobe Bryant](Person_Kobe Bryant) was one of the best basketball players of all times. Not even [Michael Jordan](Person_Michael Jordan) has ever scored 81 points in one game. [Munich](Location_Munich) is really an awesome city, but [New York](Location_New York) is as well. [Yesterday](Date_Yesterday) has been the hottest day of the year.";
        assertThat(source.get("annotated_text").toString()).isEqualTo(expectedAnnotatedText);
    }

    @Test
    public void testSimulatePipeline() throws Exception {
        String endpoint = String.format("http://localhost:%s", container.getMappedPort(9200));
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + "/_ingest/pipeline/my_pipeline"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("""
          { "processors": [ { "opennlp" : { "field" : "field1" } } ] }
                        """))
                .build();
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        SimulateResponse response = client.ingest().simulate(b -> b
                .id("my_pipeline")
                .docs(Document.of(d -> d.source(JsonData.of(Map.of("field1", text)))))
        );

        assertThat(response.docs()).hasSize(1);
        PipelineSimulation simulation = response.docs().get(0);
        assertThat(simulation.doc().source()).hasEntrySatisfying("field1", jsonData -> {
            assertThat(jsonData.to(String.class)).isEqualTo(text);
        });

        assertThat(simulation.doc().source()).hasEntrySatisfying("entities", data -> {
            Map<String, Object> entities = data.to(Map.class);
            assertThat(entities).hasSize(3);
            assertThat(entities).containsOnlyKeys("dates", "names", "locations");
            assertThat(entities.get("dates")).asList().map(o -> ((JsonString) o).getString()).contains("Yesterday");
            assertThat(entities.get("names")).asList().map(o -> ((JsonString) o).getString()).contains("Kobe Bryant", "Michael Jordan");
            assertThat(entities.get("locations")).asList().map(o -> ((JsonString) o).getString()).contains("Munich", "New York");
        });
    }
}
