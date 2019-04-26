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
import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

/*
 * Important: You need to run gradle from the command line first
 * to download the models referenced here
 */
public class OpenNlpServiceTests extends ESTestCase {

    public void testThatModelsCanBeLoaded() {
        Settings settings = Settings.builder()
                .put("ingest.opennlp.model.file.names", "en-ner-persons.bin")
                .put("ingest.opennlp.model.file.locations", "en-ner-locations.bin")
                .put("ingest.opennlp.model.file.dates", "en-ner-dates.bin")
                .build();
        OpenNlpService service = new OpenNlpService(getDataPath("/models/en-ner-persons.bin").getParent(), settings);
        service.start();

        ExtractedEntities nameEntites = service.find("Kobe Bryant was one of the best basketball players of all time.", "names");
        assertThat(nameEntites.getEntityValues(), hasSize(1));
        assertThat(nameEntites.getEntityValues(), hasItem("Kobe Bryant"));

        ExtractedEntities locationEntities = service.find("Munich is really an awesome city, but New York is as well.", "locations");
        assertThat(locationEntities.getEntityValues(), hasSize(2));
        assertThat(locationEntities.getEntityValues(), contains("Munich", "New York"));

        ExtractedEntities dateEntities = service.find("Yesterday has been the hottest day of the year.", "dates");
        assertThat(dateEntities.getEntityValues(), hasSize(1));
        assertThat(dateEntities.getEntityValues(), contains("Yesterday"));
    }
}
