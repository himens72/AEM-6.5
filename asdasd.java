
package com.kpmg.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UniversalFilterApiConfigurationTest {

    private UniversalFilterApiConfiguration configuration;

    private UniversalFilterApiConfiguration.Config config;

    @BeforeEach
    void setUp() {
        configuration = new UniversalFilterApiConfiguration();

        config = mock(UniversalFilterApiConfiguration.Config.class);
    }

    @Test
    void testActivate() {

        String endpoint = "https://kpmg.com/ecsearch";

        when(config.getElasticSearchEndpoint())
                .thenReturn(endpoint);

        configuration.activate(config);

        assertEquals(
                endpoint,
                configuration.getElasticSearchEndpoint()
        );
    }

    @Test
    void testActivateWithEmptyEndpoint() {

        when(config.getElasticSearchEndpoint())
                .thenReturn("");

        configuration.activate(config);

        assertEquals(
                "",
                configuration.getElasticSearchEndpoint()
        );
    }

    @Test
    void testModified() {

        when(config.getElasticSearchEndpoint())
                .thenReturn("https://kpmg.com/ecsearch");

        configuration.activate(config);

        when(config.getElasticSearchEndpoint())
                .thenReturn("https://kpmg.com/ecsearch/ie_en");

        configuration.activate(config);

        assertEquals(
                "https://kpmg.com/ecsearch/ie_en",
                configuration.getElasticSearchEndpoint()
        );
    }
}
