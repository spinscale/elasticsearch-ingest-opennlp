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
import org.elasticsearch.xcontent.ToXContent;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.core.PathUtils;
import org.elasticsearch.core.Tuple;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;
import org.elasticsearch.ingest.RandomDocumentPicks;
import org.elasticsearch.test.ESTestCase;
import org.junit.BeforeClass;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class OpenNlpProcessorTests extends ESTestCase {

    private static OpenNlpService service;

    @BeforeClass
    public static void createOpenNlpService() throws Exception {
        Settings settings = Settings.builder()
                .put("ingest.opennlp.model.file.names", "en-ner-persons.bin")
                .put("ingest.opennlp.model.file.locations", "en-ner-locations.bin")
                .put("ingest.opennlp.model.file.dates", "en-ner-dates.bin")
                .build();

        Path path = PathUtils.get(OpenNlpProcessorTests.class.getResource("/models/en-ner-persons.bin").toURI());
        service = new OpenNlpService(path.getParent(), settings).start();
    }

    public void testThatExtractionsWork() throws Exception {
        OpenNlpProcessor processor = new OpenNlpProcessor(service, randomAlphaOfLength(10), "source_field", "target_field",
                null, new HashSet<>(Arrays.asList("names", "dates", "locations")), "description");

        Map<String, Object> entityData = getIngestDocumentData(processor);

        assertThatHasElements(entityData, "names", "Kobe Bryant", "Michael Jordan");
        assertThatHasElements(entityData, "dates", "Yesterday");
        assertThatHasElements(entityData, "locations", "Munich", "New York");
    }

    public void testThatFieldsCanBeExcluded() throws Exception {
        OpenNlpProcessor processor = new OpenNlpProcessor(service, randomAlphaOfLength(10), "source_field", "target_field",
                null, new HashSet<>(Arrays.asList("dates")), "description");

        Map<String, Object> entityData = getIngestDocumentData(processor);

        assertThat(entityData, not(hasKey("locations")));
        assertThat(entityData, not(hasKey("names")));
        assertThatHasElements(entityData, "dates", "Yesterday");
    }

    public void testThatExistingValuesAreMergedWithoutDuplicates() throws Exception {
        OpenNlpProcessor processor = new OpenNlpProcessor(service, randomAlphaOfLength(10), "source_field", "target_field",
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

    public void testConstructorNoFieldsSpecified() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("field", "source_field");
        config.put("target_field", "target_field");

        OpenNlpProcessor.Factory factory = new OpenNlpProcessor.Factory(service);
        Map<String, Processor.Factory> registry = Collections.emptyMap();
        OpenNlpProcessor processor = factory.create(registry, randomAlphaOfLength(10), "description", config);

        Map<String, Object> entityData = getIngestDocumentData(processor);

        assertThatHasElements(entityData, "names", "Kobe Bryant", "Michael Jordan");
        assertThatHasElements(entityData, "dates", "Yesterday");
        assertThatHasElements(entityData, "locations", "Munich", "New York");
    }

    public void testToXContent() throws Exception {
        OpenNlpProcessor processor = new OpenNlpProcessor(service, randomAlphaOfLength(10), "source_field", "target_field",
                null, new HashSet<>(Arrays.asList("names", "dates", "locations")), "description");

        IngestDocument ingestDocument = getIngestDocument();
        processor.execute(ingestDocument);

        SimulateProcessorResult result = new SimulateProcessorResult("type", "tag", "description",
                ingestDocument, Tuple.tuple("data", true));

        try (XContentBuilder builder = XContentFactory.jsonBuilder()) {
            result.toXContent(builder, ToXContent.EMPTY_PARAMS);
        }
    }

    public void testAnnotatedText() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("field", "source_field");
        config.put("annotated_text_field", "my_annotated_text_field");

        OpenNlpProcessor.Factory factory = new OpenNlpProcessor.Factory(service);
        Map<String, Processor.Factory> registry = Collections.emptyMap();
        OpenNlpProcessor processor = factory.create(registry, randomAlphaOfLength(10), "description", config);

        IngestDocument ingestDocument = processor.execute(getIngestDocument());
        String content = ingestDocument.getFieldValue("my_annotated_text_field", String.class);
        assertThat(content, is("[Kobe Bryant](Person_Kobe Bryant) was one of the best basketball players of all times. Not even" +
                " [Michael Jordan](Person_Michael Jordan) has ever scored 81 points in one game. [Munich](Location_Munich) is really" +
                " an awesome city, but [New York](Location_New York) is as well. [Yesterday](Date_Yesterday) has been the hottest" +
                " day of the year."));
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
        return RandomDocumentPicks.randomIngestDocument(random(), document);
    }

    private Map<String, Object> getIngestDocumentData(IngestDocument ingestDocument) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) ingestDocument.getSourceAndMetadata().get("target_field");
        return data;
    }

    private void assertThatHasElements(Map<String, Object> entityData, String field, String ... items) {
        List<String> values = getValues(entityData, field);
        assertThat(values, hasSize(items.length));
        assertThat(values, containsInAnyOrder(items));
    }

    private List<String> getValues(Map<String, Object> entityData, String field) {
        assertThat(entityData, hasKey(field));
        assertThat(entityData.get(field), instanceOf(List.class));
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) entityData.get(field);
        return values;
    }
}
