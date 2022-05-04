/*
 * Copyright [2016] [Alexander Reelsen]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package de.spinscale.elasticsearch.ingest.opennlp;

import org.elasticsearch.action.ingest.SimulateProcessorResult;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.PathUtils;
import org.elasticsearch.core.Tuple;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;
import org.elasticsearch.xcontent.ToXContent;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenNlpProcessorTests {

    private static OpenNlpService service;

    @BeforeAll
    public static void createOpenNlpService() throws Exception {
        Settings settings = Settings.builder()
                .put("ingest.opennlp.model.file.names", "en-ner-persons.bin")
                .put("ingest.opennlp.model.file.locations", "en-ner-locations.bin")
                .put("ingest.opennlp.model.file.dates", "en-ner-dates.bin")
                .build();

        Path path = PathUtils.get(OpenNlpProcessorTests.class.getResource("/models/en-ner-persons.bin").toURI());
        service = new OpenNlpService(path.getParent(), settings).start();
    }

    @Test
    public void testThatExtractionsWork() throws Exception {
        OpenNlpProcessor processor = new OpenNlpProcessor(service, null, "source_field", "target_field",
                null, new HashSet<>(Arrays.asList("names", "dates", "locations")), "description");

        Map<String, Object> entityData = getIngestDocumentData(processor);

        assertThatHasElements(entityData, "names", "Kobe Bryant", "Michael Jordan");
        assertThatHasElements(entityData, "dates", "Yesterday");
        assertThatHasElements(entityData, "locations", "Munich", "New York");
    }

    @Test
    public void testThatFieldsCanBeExcluded() throws Exception {
        OpenNlpProcessor processor = new OpenNlpProcessor(service, null, "source_field", "target_field",
                null, new HashSet<>(Arrays.asList("dates")), "description");

        Map<String, Object> entityData = getIngestDocumentData(processor);

        assertThat(entityData).doesNotContainKey("locations");
        assertThat(entityData).doesNotContainKey("names");
        assertThatHasElements(entityData, "dates", "Yesterday");
    }

    @Test
    public void testThatExistingValuesAreMergedWithoutDuplicates() throws Exception {
        OpenNlpProcessor processor = new OpenNlpProcessor(service, null, "source_field", "target_field",
                null, new HashSet<>(Arrays.asList("names", "dates", "locations")), "description");

        IngestDocument ingestDocument = getIngestDocument();

        Map<String, Object> entityData = new HashMap<>();
        entityData.put("names", Arrays.asList("Magic Johnson", "Kobe Bryant"));
        entityData.put("locations", Arrays.asList("Paris", "Munich"));
        entityData.put("dates", Arrays.asList("Today", "Yesterday"));

        ingestDocument.setFieldValue("target_field", entityData);

        ingestDocument = processor.execute(ingestDocument);

        entityData = getIngestDocumentData(ingestDocument);

        assertThatHasElements(entityData, "names", "Magic Johnson", "Kobe Bryant", "Michael Jordan");
        assertThatHasElements(entityData, "dates", "Today", "Yesterday");
        assertThatHasElements(entityData, "locations", "Paris", "Munich", "New York");
    }

    @Test
    public void testConstructorNoFieldsSpecified() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("field", "source_field");
        config.put("target_field", "target_field");

        OpenNlpProcessor.Factory factory = new OpenNlpProcessor.Factory(service);
        Map<String, Processor.Factory> registry = Collections.emptyMap();
        OpenNlpProcessor processor = factory.create(registry, null, "description", config);

        Map<String, Object> entityData = getIngestDocumentData(processor);

        assertThatHasElements(entityData, "names", "Kobe Bryant", "Michael Jordan");
        assertThatHasElements(entityData, "dates", "Yesterday");
        assertThatHasElements(entityData, "locations", "Munich", "New York");
    }

    @Test
    public void testToXContent() throws Exception {
        OpenNlpProcessor processor = new OpenNlpProcessor(service, null, "source_field", "target_field",
                null, new HashSet<>(Arrays.asList("names", "dates", "locations")), "description");

        IngestDocument ingestDocument = getIngestDocument();
        processor.execute(ingestDocument);

        SimulateProcessorResult result = new SimulateProcessorResult("type", "tag", "description",
                ingestDocument, Tuple.tuple("data", true));

        try (XContentBuilder builder = XContentFactory.jsonBuilder()) {
            result.toXContent(builder, ToXContent.EMPTY_PARAMS);
        }
    }

    @Test
    public void testAnnotatedText() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("field", "source_field");
        config.put("annotated_text_field", "my_annotated_text_field");

        OpenNlpProcessor.Factory factory = new OpenNlpProcessor.Factory(service);
        Map<String, Processor.Factory> registry = Collections.emptyMap();
        OpenNlpProcessor processor = factory.create(registry, null, "description", config);

        IngestDocument ingestDocument = processor.execute(getIngestDocument());
        String content = ingestDocument.getFieldValue("my_annotated_text_field", String.class);
        assertThat(content).endsWith("[Kobe Bryant](Person_Kobe Bryant) was one of the best basketball players of all times. Not even" +
                " [Michael Jordan](Person_Michael Jordan) has ever scored 81 points in one game. [Munich](Location_Munich) is really" +
                " an awesome city, but [New York](Location_New York) is as well. [Yesterday](Date_Yesterday) has been the hottest" +
                " day of the year.");
    }

    private Map<String, Object> getIngestDocumentData(OpenNlpProcessor processor) throws Exception {
        IngestDocument ingestDocument = getIngestDocument();
        return getIngestDocumentData(processor.execute(ingestDocument));
    }

    private IngestDocument getIngestDocument() throws Exception {
        return getIngestDocument("Kobe Bryant was one of the best basketball players of all times. Not even Michael Jordan has ever " +
                "scored 81 points in one game. Munich is really an awesome city, but New York is as well. Yesterday has been the " +
                "hottest day of the year.");
    }

    private IngestDocument getIngestDocument(String content) throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("source_field", content);
        //     public IngestDocument(String index, String id, String routing, Long version, VersionType versionType, Map<String, Object> source) {
        return new IngestDocument("my-index", "my-id", null, 1L, VersionType.INTERNAL, document);
    }

    private Map<String, Object> getIngestDocumentData(IngestDocument ingestDocument) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) ingestDocument.getSourceAndMetadata().get("target_field");
        return data;
    }

    private void assertThatHasElements(Map<String, Object> entityData, String field, String ... items) {
        List<String> values = getValues(entityData, field);
        assertThat(values).hasSize(items.length);
        assertThat(values).contains(items);
    }

    private List<String> getValues(Map<String, Object> entityData, String field) {
        assertThat(entityData).containsKey(field);
        assertThat(entityData.get(field)).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) entityData.get(field);
        return values;
    }
}
