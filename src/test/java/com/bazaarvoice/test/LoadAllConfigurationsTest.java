package com.bazaarvoice.test;

import com.bazaarvoice.cca.config.UniversalWidgetConfiguration;
import com.bazaarvoice.cca.config.WidgetConfiguration;
import com.bazaarvoice.cca.submit.config.BooleanControlConfiguration;
import com.bazaarvoice.cca.submit.config.DefaultBooleanControlConfiguration;
import com.bazaarvoice.cca.util.VersionNumber;
import com.bazaarvoice.config.RosettaDisplayConfiguration;
import com.bazaarvoice.config.deserializer.ClientDeserializer;
import com.bazaarvoice.config.deserializer.ColorDeserializer;
import com.bazaarvoice.config.deserializer.PatternDeserializer;
import com.bazaarvoice.config.deserializer.VersionNumberDeserializer;
import com.bazaarvoice.profiles.config.ProfilesSiteConfiguration;
import com.bazaarvoice.prr.config.DisplayConfiguration;
import com.bazaarvoice.prr.model.Client;
import com.bazaarvoice.qa.config.QASiteConfiguration;
import com.bazaarvoice.rosetta.api.CloseableIterable;
import com.bazaarvoice.rosetta.api.Rosetta;
import com.bazaarvoice.rosetta.api.RosettaClientBuilder;
import com.bazaarvoice.rosetta.api.RosettaConfiguration;
import com.bazaarvoice.rosetta.api.model.BaseSiteModel;
import com.bazaarvoice.rosetta.api.model.BundleModel;
import com.bazaarvoice.rosetta.api.search.BundleRequest;
import com.bazaarvoice.rosetta.api.search.BundleResponse;
import com.bazaarvoice.stories.config.StoriesSiteConfiguration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class LoadAllConfigurationsTest {

    private static final Logger LOG = LoggerFactory.getLogger(LoadAllConfigurationsTest.class);
    private static final String host = "https://rosetta.prod.us-east-1.nexus.bazaarvoice.com";

    private Rosetta rosetta;

    @Test
    public void loadAllConfigFiles()
        throws InterruptedException {
        CloseableIterable<BundleResponse> bundles = rosetta.findBundles(new BundleRequest());
        int countBundles = 0;
        List<String> bundleNamesToLoad = new ArrayList<>();
        for (BundleResponse bundle : bundles) {
            countBundles++;
            bundleNamesToLoad.add(bundle.getName());
        }
        ExecutorService bundleLoader = Executors.newFixedThreadPool(128);
        CountDownLatch bundlesLoaded = new CountDownLatch(countBundles);
        long startedLoadingBundles = System.currentTimeMillis();
        List<BundleModel.Site> sitesToLoad = new CopyOnWriteArrayList<>();
        for (String bundleName : bundleNamesToLoad) {
            bundleLoader.execute(() -> {
                long startedToLoadSites = System.currentTimeMillis();
                try {
                    BundleModel bundleModel = rosetta.getBundle(bundleName, BundleModel.class);
                    sitesToLoad.addAll(bundleModel.getSites());
                } finally {
                    bundlesLoaded.countDown();
                    LOG.info("Loaded bundle metadata: {}, time: {}", bundleName, System.currentTimeMillis() - startedToLoadSites);
                }
            });
        }
        bundlesLoaded.await();

        long startedLoadSites = System.currentTimeMillis();
        CountDownLatch sitesLoaded = new CountDownLatch(sitesToLoad.size());
        for (BundleModel.Site s : sitesToLoad) {
            bundleLoader.execute(() -> {
                long startLoadSite = System.currentTimeMillis();
                String displayCode = s.getDisplayCode();
                String applicationCode = s.getApplicationCode();
                try {
                    switch (applicationCode) {
                        case DisplayConfiguration.APPLICATION_CODE:
                            rosetta.getSite(displayCode, applicationCode, DisplayConfiguration.class);
                            break;
                        case QASiteConfiguration.APPLICATION_CODE:
                            rosetta.getSite(displayCode, applicationCode, QASiteConfiguration.class);
                            break;
                        case ProfilesSiteConfiguration.APPLICATION_CODE:
                            rosetta.getSite(displayCode, applicationCode, ProfilesSiteConfiguration.class);
                            break;
                        case StoriesSiteConfiguration.APPLICATION_CODE:
                            rosetta.getSite(displayCode, applicationCode, StoriesSiteConfiguration.class);
                            break;
                        default:
                            throw new RuntimeException("Unknown Application Code = " + applicationCode);
                    }
                } finally {
                    sitesLoaded.countDown();
                    LOG.info("Loaded site {}/{} with time: {}", displayCode, applicationCode, System.currentTimeMillis() - startLoadSite);
                }
            });
        }
        sitesLoaded.await();
        LOG.info("Loaded sites {} time: {}", sitesToLoad.size(), System.currentTimeMillis() - startedLoadSites);
        bundleLoader.shutdown();
        bundles.close();
        LOG.info("Loaded bundles: {}, time: {}", countBundles, System.currentTimeMillis() - startedLoadingBundles);
    }

    @Test
    public void loadDefaultUiConfigurations()
        throws InterruptedException {
        String[] defaultUiVersions = {"4.7", "4.8", "4.9", "5.0", "5.1", "5.3", "5.6"};
        long startLoadDefaultUI = System.currentTimeMillis();
        long totalSites = 0;
        ExecutorService defaultUiLoader = Executors.newFixedThreadPool(64);
        for (String version : defaultUiVersions) {
            LOG.info("Started loading defaultUI " + version);
            long startTime = System.nanoTime();
            BundleModel defaultUiBundle = rosetta.getDefaultUiBundle(version, BundleModel.class);
            for (BundleModel.Client client : defaultUiBundle.getClients()) {
                long startLoadClient = System.currentTimeMillis();
                String name = client.getName();
                rosetta.getDefaultUiClient(version, name, BaseSiteModel.class);
                LOG.info("Loaded client {} time: {} ms", name, System.currentTimeMillis() - startLoadClient);
            }
            AtomicInteger c = new AtomicInteger();
            CountDownLatch loadDefaultUiSites = new CountDownLatch(defaultUiBundle.getSites().size());
            for (BundleModel.Site site : defaultUiBundle.getSites()) {
                defaultUiLoader.execute(() -> {
                    try {
                        long startLoadSite = System.currentTimeMillis();
                        if (site.isActive()) {
                            BaseSiteModel defaultUiSite = rosetta.getDefaultUiSite(version, site.getDisplayCode(), site.getApplicationCode(), BaseSiteModel.class);
                            LOG.info("Loaded site {}/{} time: {} ms", defaultUiSite.getDisplayCode(), defaultUiSite.getApplicationCode(), System.currentTimeMillis() - startLoadSite);
                            c.addAndGet(1);
                        }
                    } finally {
                        loadDefaultUiSites.countDown();
                    }
                });
            }
            loadDefaultUiSites.await();
            long millis = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
            totalSites += c.get();
            LOG.info(String.format("Loaded defaultUI %s with %d sites and time %d ms", version, c.get(), millis));
        }

        defaultUiLoader.shutdown();
        LOG.info("Loaded DefaultUI with {} sites and time: {} sec", totalSites, (System.currentTimeMillis() - startLoadDefaultUI) / 1000);
    }

    @Before
    public void setUp() {
        RosettaConfiguration rosettaConfiguration = new RosettaConfiguration();
        rosettaConfiguration.setFixedUrl(URI.create(host));
        rosettaConfiguration.setGzipEnabled(true);
        rosettaConfiguration.setUseZooKeeper(false);
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addAbstractTypeMapping(BooleanControlConfiguration.class, DefaultBooleanControlConfiguration.class);
        simpleModule.addAbstractTypeMapping(WidgetConfiguration.class, WidgetConfiguration.TopRatedProducts.class);
        simpleModule.addAbstractTypeMapping(UniversalWidgetConfiguration.class, WidgetConfiguration.BayesianTopRatedProducts.class);
        simpleModule.addDeserializer(Color.class, new ColorDeserializer());
        simpleModule.addDeserializer(Pattern.class, new PatternDeserializer());
        simpleModule.addDeserializer(Client.class, new ClientDeserializer());
        simpleModule.addDeserializer(VersionNumber.class, new VersionNumberDeserializer());
        objectMapper.registerModule(simpleModule);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        rosetta = new RosettaClientBuilder()
            .using(rosettaConfiguration)
            .using(objectMapper)
            .build();
    }

    @Test
    public void loadTestcustomer() {
        DisplayConfiguration testcustomerConfig = rosetta.getSite("56test", "PRR", RosettaDisplayConfiguration.class);
        System.out.println(testcustomerConfig.getAvatarConfiguration().toString());
    }

}
