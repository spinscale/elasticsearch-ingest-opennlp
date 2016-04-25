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
import org.elasticsearch.ingest.RandomDocumentPicks;
import org.elasticsearch.ingest.core.IngestDocument;
import org.elasticsearch.test.ESTestCase;
import org.junit.Before;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;

public class OpenNlpProcessorTests extends ESTestCase {

    private OpenNlpProcessor processor;
    private OpenNlpService service;

    @Before
    public void createStandardProcessor() throws IOException {
        service = new OpenNlpService(getDataPath("/models/en-ner-person.bin").getParent(),
                Settings.EMPTY).start();
        processor = new OpenNlpProcessor(service, randomAsciiOfLength(10), "source_field",
                "target_field", EnumSet.allOf(OpenNlpProcessor.Property.class));
    }

    public void testThatExtractionsWork() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("source_field", "Kobe Bryant was one of the best basketball players of all times. Not even Michael Jordan has ever " +
                "scored 81 points in one game. Munich is really an awesome city, but New York is as well. Yesterday has been the " +
                "hottest day of the year.");

        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        processor.execute(ingestDocument);

        @SuppressWarnings("unchecked")
        Map<String, Object> entityData = (Map<String, Object>) ingestDocument.getSourceAndMetadata().get("target_field");

        assertThatHasElements(entityData, "names", "Kobe Bryant", "Michael Jordan");
        assertThatHasElements(entityData, "dates", "Yesterday");
        assertThatHasElements(entityData, "locations", "Munich", "New York");
    }

    public void testThatFieldsCanBeExcluded() throws Exception {
        processor = new OpenNlpProcessor(service, randomAsciiOfLength(10), "source_field",
                "target_field", EnumSet.of(OpenNlpProcessor.Property.DATES));

        Map<String, Object> document = new HashMap<>();
        document.put("source_field", "Kobe Bryant was one of the best basketball players of all times. Not even Michael Jordan has ever " +
                "scored 81 points in one game. Munich is really an awesome city, but New York is as well. Yesterday has been the " +
                "hottest day of the year.");

        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        processor.execute(ingestDocument);

        @SuppressWarnings("unchecked")
        Map<String, Object> entityData = (Map<String, Object>) ingestDocument.getSourceAndMetadata().get("target_field");
        assertThat(entityData, not(hasKey("locations")));
        assertThat(entityData, not(hasKey("names")));
        assertThatHasElements(entityData, "dates", "Yesterday");
    }

    private void assertThatHasElements(Map<String, Object> entityData, String field, String ... items) {
        assertThat(entityData, hasKey(field));
        assertThat(entityData.get(field), instanceOf(List.class));

        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) entityData.get(field);
        assertThat(names, containsInAnyOrder(items));
    }
}
