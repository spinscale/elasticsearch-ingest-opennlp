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

import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.Supplier;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESTestCase;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;

public class OpenNlpThreadSafeTests extends ESTestCase {

    private OpenNlpService service;
    private ExecutorService executorService;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Settings settings = Settings.builder()
                .put("ingest.opennlp.model.file.ner.names", "en-ner-persons.bin")
                .put("ingest.opennlp.model.file.ner.locations", "en-ner-locations.bin")
                .put("ingest.opennlp.model.file.ner.dates", "en-ner-dates.bin")
                .put("ingest.opennlp.model.file.pos", "en-pos-maxent.bin")
                .build();
        service = new OpenNlpService(getDataPath("/models/en-ner-persons.bin").getParent(), settings).start();
        executorService = Executors.newFixedThreadPool(10);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        terminate(executorService);
    }

    public void testThatOpenNlpServiceIsThreadSafe() throws Exception {
        int runs = 1000;
        CountDownLatch latch = new CountDownLatch(2*runs);
        List<OpennlpRunnable> runnables = new ArrayList<>();

        for (int i = 0; i < runs; i++) {
            String city = randomFrom("Munich", "Stockholm", "Madrid", "San Francisco", "Cologne", "Paris", "London", "Amsterdam");

            OpennlpNerRunnable runnable = new OpennlpNerRunnable(i, city, latch);
            runnables.add(runnable);
            executorService.submit(runnable);
        }

        for (int i = 0; i < runs; i++) {
            String[] cities = randomFrom(
                    new String[]{"Munich"},
                    new String[]{"Munich", "Stockholm"},
                    new String[]{"Munich", "Stockholm", "Madrid"},
                    new String[]{"Munich", "Stockholm", "Madrid", "Miami"},
                    new String[]{"Munich", "Stockholm", "Madrid", "Miami", "Cologne"},
                    new String[]{"Munich", "Stockholm", "Madrid", "Miami", "Cologne", "Paris"},
                    new String[]{"Munich", "Stockholm", "Madrid", "Miami", "Cologne", "Paris", "London"},
                    new String[]{"Munich", "Stockholm", "Madrid", "Miami", "Cologne", "Paris", "London", "Amsterdam"});

            OpennlpPosRunnable runnable = new OpennlpPosRunnable(i, cities, latch);
            runnables.add(runnable);
            executorService.submit(runnable);
        }

        latch.await(60, TimeUnit.SECONDS);
        for (OpennlpRunnable runnable : runnables) {
            runnable.assertResultIsCorrect();
        }
    }

    interface OpennlpRunnable extends Runnable {
        void assertResultIsCorrect();
    }

    private class OpennlpNerRunnable implements OpennlpRunnable {

        private int idx;
        final String city;
        private CountDownLatch latch;
        String result;

        OpennlpNerRunnable(int idx, String city, CountDownLatch latch) {
            this.idx = idx;
            this.city = city;
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                Set<String> locations = service.find(city + " is really an awesome city, but others are as well.", "locations");
                // logger.info("Got {}, expected {}, index {}", locations, city, idx);
                if (locations.size() > 0) {
                    result = locations.stream().findFirst().get();
                }
            } catch (Exception e) {
                logger.error((Supplier<?>) () -> new ParameterizedMessage("Unexpected exception"), e);
            } finally {
                latch.countDown();
            }
        }

        @Override
        public void assertResultIsCorrect() {
            assertThat(String.format(Locale.ROOT, "Expected task %s to have result %s", idx, city), result, is(city));
        }
    }

    private class OpennlpPosRunnable implements OpennlpRunnable {

        private int idx;
        final String[] cities;
        private CountDownLatch latch;
        int result;

        OpennlpPosRunnable(int idx, String[] cities, CountDownLatch latch) {
            this.idx = idx;
            this.cities = cities;
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                Map<String, Number> tags = service.countTags(
                        String.join(", ", cities) + (cities.length > 1 ? " are " : " is ") + "really awesome",
                        new HashSet<>(Arrays.asList("NNP")), false);

                if (tags.containsKey("NNP")) {
                    result = tags.get("NNP").intValue();
                }
            } catch (Exception e) {
                logger.error((Supplier<?>) () -> new ParameterizedMessage("Unexpected exception"), e);
            } finally {
                latch.countDown();
            }
        }

        @Override
        public void assertResultIsCorrect() {
            assertThat(String.format(Locale.ROOT, "Expected task %s to have result %s", idx, cities.length), result, is(cities.length));
        }
    }
}
