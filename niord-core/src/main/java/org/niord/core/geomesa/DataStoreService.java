/*
 * Copyright 2021 GLA UK Research and Development Directive
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.niord.core.geomesa;

import org.apache.commons.lang.StringUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.niord.core.aton.AtonNode;
import org.niord.core.settings.Setting;
import org.niord.core.settings.SettingsService;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * The GeoMesa Data Store Service.
 * <p>
 * This service initialises and controls the GeoMesa data store, which in Niord
 * is implemented using Kafka.
 * </p>
 * <p>
 * During initialisation, Niord will connect to kafka and any relevant messages
 * will be sent to the broker.
 * </p>
 */
@Singleton
@Startup
@SuppressWarnings("unused")
public class DataStoreService {

    private static final int EXECUTOR_POOL_SIZE = 2;
    private static final int EXECUTOR_POOL_TIMEOUT = 5;

    /**
     * The Zookeeper Host Address Setting.
     */
    private static final Setting ZOOKEEPER_ADDR = new Setting("zookeeperAddress")
            .description("The Zookeeper host address to connect to the Geomesa Kafka Data Store")
            .cached(true)
            .editable(true)
            .web(false);

    /**
     * The Kafka Host Address Setting.
     */
    private static final Setting KAFKA_ADDR = new Setting("kafkaAddress")
            .description("The Kafka host address to connect to the Geomesa Kafka Data Store")
            .cached(true)
            .editable(true)
            .web(false);

    @Inject
    Logger log;

    @Inject
    SettingsService settingsService;

    // The selected data store producer
    private ScheduledExecutorService processPool;
    private List<Future<?>> futures = new ArrayList<Future<?>>();
    private DataStore producer;

    /**
     * Initialize the data store.
     */
    @PostConstruct
    private void init() {
        // Create a process pool to run this so that we don't stall
        processPool = Executors.newScheduledThreadPool(EXECUTOR_POOL_SIZE);

        // And boostrap the datastore
        this.boostrapDataStore();
    }

    /**
     * Clean up Lucene index.
     */
    @PreDestroy
    private void closeIndex() {
        if (this.producer != null) {
            this.producer.dispose();
            this.producer = null;
        }
        futures.clear();
        processPool.shutdown();
    }

    /**
     * Initialise the service based on the provided settings. If the required
     * settings have not been set, then the datastore will NOT be initialised
     * but no error will be thrown.
     */
    private void boostrapDataStore() {
        // We got a producer so move on
        if(this.producer != null) {
            return;
        }

        // Check is the zookeeper and kafka broker have been defined
        String zookeeperAddr = this.getZookeeperAddress();
        String kafkaAddr = this.getKafkaAddress();
        if(Objects.nonNull(zookeeperAddr) && Objects.nonNull(kafkaAddr)) {
            Map<String, String> params = new HashMap<>();
            params.put("kafka.brokers", kafkaAddr);
            params.put("kafka.zookeepers", zookeeperAddr);
            params.put("kafka.consumer.count", "0");
            // Cancel any old futures still remaining
            if(!futures.isEmpty()) {
                futures.stream().filter(Future::isDone).forEach(f -> f.cancel(true));
                futures.clear();
            }
            // And set up a new future task to connect to the datastore
            Future<?> handler = processPool.submit(() -> {
                try {
                    // Create the producer
                    this.producer = this.createDataStore(params);

                    // Create the AtoN Schema
                    if(this.producer != null) {
                        GeomesaAton gmAton = new GeomesaAton();
                        this.createSchema(this.producer, gmAton.getSimpleFeatureType());
                    }
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            });
            futures.add(handler);
            // Allow a few seconds for the connection and then cancel
            processPool.schedule(() -> {
                if(!handler.isDone()) {
                    handler.cancel(true);
                }
                futures.clear();
            }, EXECUTOR_POOL_TIMEOUT, TimeUnit.SECONDS);
        }
    }

    /**
     * Creates the Geomesa Data Store from scratch. There is no problem if the
     * store already exists, this will do nothing.
     *
     * @param params the parameters for the generating the datastore
     * @return the generated data store
     * @throws IOException
     */
    private DataStore createDataStore(Map<String, String> params) throws IOException {
        log.info("Creating GeoMesa Data Store");
        DataStore producer = DataStoreFinder.getDataStore(params);
        if (producer == null) {
            throw new RuntimeException("Could not create data store with provided parameters");
        }
        log.info("GeoMesa Data Store created");
        return producer;
    }

    /**
     * Creates a new schema in the provided data store. The schema is pretty
     * much like the table in a database that will accept the row data.
     * @param datastore the datastore to create the schema into
     * @param sft the simple feature type i.e. the schema description
     * @throws IOException
     */
    private void createSchema(DataStore datastore, SimpleFeatureType sft) throws IOException {
        log.info("Creating schema: " + DataUtilities.encodeType(sft));
        // we only need to do the once - however, calling it repeatedly is a no-op
        datastore.createSchema(sft);
        log.info("Schema created");
    }

    /**
     * A generic function that writes the provided data into the datastore
     * schema, based on the provided simple feature type. The list of features
     * are generic points of interest that will be send to the data store.
     *
     * @param datastore the datastore to write the feature into
     * @param sft the simple feature type
     * @param features the list of features to be writter to the data store
     * @throws IOException
     */
    private void writeFeatures(DataStore datastore, SimpleFeatureType sft, List<SimpleFeature> features) throws IOException {
        SimpleFeatureStore producerFS = (SimpleFeatureStore) datastore.getFeatureSource(sft.getTypeName());
        producerFS.addFeatures(new ListFeatureCollection(sft, features));
    }

    /**
     * Pushes a new/updated AtoN node into the Geomesa Data Store. Currently
     * this only supports the Kafka Message Streams.
     *
     * @param aton the AtoN node to be pushed into the datastore
     */
    public void pushAton(AtonNode aton) {
        // We need a valid producer to push the AtoN to
        if(this.producer == null) {
            return;
        }

        // Translate the AtoNs to the Geomesa simple features
        GeomesaAton gmAton = new GeomesaAton();
        try {
            this.writeFeatures(this.producer, gmAton.getSimpleFeatureType(), gmAton.getFeatureData(Collections.singletonList(aton)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the Zookeeper host address to access the Geomesa Data Store.
     *
     * @return the Zookeeper host address to access the Geomesa Data Store
     */
    public String getZookeeperAddress() {
        return StringUtils.trimToNull(settingsService.getString(ZOOKEEPER_ADDR));
    }

    /**
     * Returns the Kafka host address to access the Geomesa Data Store.
     *
     * @return the Kafka host address to access the Geomesa Data Store
     */
    public String getKafkaAddress() {
        return StringUtils.trimToNull(settingsService.getString(KAFKA_ADDR));
    }

}
