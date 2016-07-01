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

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.RandomDocumentPicks;
import org.elasticsearch.test.ESTestCase;
import org.junit.Before;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;

public class OpenNlpProcessorTests extends ESTestCase {

    private OpenNlpService service;

    @Before
    public void createOpenNlpService() throws IOException {
        service = new OpenNlpService(getDataPath("/models/en-ner-person.bin").getParent(), Settings.EMPTY).start();
    }

    public void testThatExtractionsWork() throws Exception {
        OpenNlpProcessor processor = new OpenNlpProcessor(service, randomAsciiOfLength(10), "source_field", "target_field",
                EnumSet.allOf(OpenNlpProcessor.Property.class));

        Map<String, Object> entityData = getIngestDocumentData(processor);

        assertThatHasElements(entityData, "names", "Kobe Bryant", "Michael Jordan");
        assertThatHasElements(entityData, "dates", "Yesterday");
        assertThatHasElements(entityData, "locations", "Munich", "New York");
    }

    public void testThatFieldsCanBeExcluded() throws Exception {
        OpenNlpProcessor processor = new OpenNlpProcessor(service, randomAsciiOfLength(10), "source_field", "target_field",
                EnumSet.of(OpenNlpProcessor.Property.DATES));

        Map<String, Object> entityData = getIngestDocumentData(processor);

        assertThat(entityData, not(hasKey("locations")));
        assertThat(entityData, not(hasKey("names")));
        assertThatHasElements(entityData, "dates", "Yesterday");
    }

    public void testThatExistingValuesAreMergedWithoutDuplicates() throws Exception {
        OpenNlpProcessor processor = new OpenNlpProcessor(service, randomAsciiOfLength(10), "source_field", "target_field",
                EnumSet.allOf(OpenNlpProcessor.Property.class));

        IngestDocument ingestDocument = getIngestDocument();

        Map<String, Object> entityData = new HashMap<>();
        entityData.put("names", Arrays.asList("Magic Johnson", "Kobe Bryant"));
        entityData.put("locations", Arrays.asList("Paris", "Munich"));
        entityData.put("dates", Arrays.asList("Today", "Yesterday"));

        ingestDocument.setFieldValue("target_field", entityData);

        processor.execute(ingestDocument);

        entityData = getIngestDocumentData(ingestDocument);

        assertThatHasElements(entityData, "names", "Magic Johnson", "Kobe Bryant", "Michael Jordan");
        assertThatHasElements(entityData, "dates", "Today", "Yesterday");
        assertThatHasElements(entityData, "locations", "Paris", "Munich", "New York");
    }

    private Map<String, Object> getIngestDocumentData(OpenNlpProcessor processor) throws Exception {
        IngestDocument ingestDocument = getIngestDocument();
        processor.execute(ingestDocument);
        return getIngestDocumentData(ingestDocument);
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
        Set<String> values = getValues(entityData, field);
        assertThat(values, containsInAnyOrder(items));
    }

    private Set<String> getValues(Map<String, Object> entityData, String field) {
        assertThat(entityData, hasKey(field));
        assertThat(entityData.get(field), instanceOf(Set.class));
        @SuppressWarnings("unchecked")
        Set<String> values = (Set<String>) entityData.get(field);
        return values;
    }
}
