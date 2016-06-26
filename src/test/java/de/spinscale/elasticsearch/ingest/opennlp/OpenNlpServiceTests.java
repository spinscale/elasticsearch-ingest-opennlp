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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

/*
 * Important: You need to run gradle from the command line first
 * to download the models referenced here
 */
public class OpenNlpServiceTests extends ESTestCase {

    public void testThatModelsCanBeLoaded() throws IOException, URISyntaxException {
        OpenNlpService service = new OpenNlpService(getDataPath("/models/en-ner-person.bin").getParent(), Settings.EMPTY).start();

        Set<String> names = service.findNames("Kobe Bryant was one of the best basketball players of all time.");
        assertThat(names, hasSize(1));
        assertThat(names, hasItem("Kobe Bryant"));

        Set<String> locations = service.findLocations("Munich is really an awesome city, but New York is as well.");
        assertThat(locations, hasSize(2));
        assertThat(locations, contains("Munich", "New York"));

        Set<String> dates = service.findDates("Yesterday has been the hottest day of the year.");
        assertThat(dates, hasSize(1));
        assertThat(dates, contains("Yesterday"));
    }
}
