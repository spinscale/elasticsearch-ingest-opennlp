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
import org.elasticsearch.common.util.iterable.Iterables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenNlpThreadSafeTests {

    private static final Settings settings = Settings.builder()
                .put("ingest.opennlp.model.file.locations", "en-ner-locations.bin")
                .build();
    private OpenNlpService service;
    private ExecutorService executorService;

    @BeforeEach
    public void setup() throws Exception {
        service = new OpenNlpService(Path.of("src/test/resources/models/"), settings).start();
        executorService = Executors.newFixedThreadPool(10);
    }

    @AfterEach
    public void tearDown() throws Exception {
        executorService.shutdownNow();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    public void testThatOpenNlpServiceIsThreadSafe() throws Exception {
        int runs = 1000;
        CountDownLatch latch = new CountDownLatch(runs);
        List<OpennlpRunnable> runnables = new ArrayList<>();

        String[] cities = new String[] {"Munich", "Stockholm", "Madrid", "San Francisco", "Cologne", "Paris", "London", "Amsterdam"};

        for (int i = 0; i < runs; i++) {
            String city = cities[runs % cities.length];

            OpennlpRunnable runnable = new OpennlpRunnable(i, city, latch);
            runnables.add(runnable);
            executorService.submit(runnable);
        }

        latch.await(30, TimeUnit.SECONDS);
        for (OpennlpRunnable runnable : runnables) {
            runnable.assertResultIsCorrect();
        }
    }

    private class OpennlpRunnable implements Runnable {

        private int idx;
        final String city;
        private CountDownLatch latch;
        String result;

        OpennlpRunnable(int idx, String city, CountDownLatch latch) {
            this.idx = idx;
            this.city = city;
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                ExtractedEntities locations = service.find(city + " is really an awesome city, but others are as well.", "locations");
                // logger.info("Got {}, expected {}, index {}", locations, city, idx);
                if (locations.getEntityValues().size() > 0) {
                    result = Iterables.get(locations.getEntityValues(), 0);
                }
            } catch (Exception e) {
            } finally {
                latch.countDown();
            }
        }

        private void assertResultIsCorrect() {
            assertThat(result).isEqualTo(city);
        }
    }
}
