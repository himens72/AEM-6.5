package com.kpmg.core.models.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kpmg.core.models.UniversalFilteringESApiResponse;
import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * JUnit 5 tests for UniversalFilteringImpl#getESApiResponse().
 * No changes to the production Sling Model are necessary.
 *
 * Uses Mockito's static mocking (Mockito 5+ / Java 21) to replace
 * HttpClient.newHttpClient() for the duration of each API call.
 */
class UniversalFilteringImplTest {

    private static final String PRIMARY_TAG =
            "kpmg-tags:ie/en/insights/sponsorship";

    private static final String SUCCESS_JSON = """
            {
              "hits": {
                "total": {"value": 2},
                "hits": [
                  {
                    "_index": "kpmg",
                    "_id": "1",
                    "_source": {
                      "title": "First Article",
                      "description": "First description",
                      "qualified_url": "/ie/en/insights/first.html"
                    }
                  },
                  {
                    "_index": "kpmg",
                    "_id": "2",
                    "_source": {
                      "title": "Second Article"
                    }
                  }
                ]
              },
              "aggregations": {
                "tags_aggregation": {
                  "doc_count_error_upper_bound": 0,
                  "sum_other_doc_count": 0,
                  "buckets": [
                    {"key": "Sponsorship", "doc_count": 38},
                    {"key": "Inclusion", "doc_count": 2}
                  ]
                }
              }
            }
            """;

    private static final String NO_RESULTS_JSON = """
            {
              "hits": {"total": {"value": 0}, "hits": []},
              "aggregations": {
                "tags_aggregation": {
                  "doc_count_error_upper_bound": 0,
                  "sum_other_doc_count": 0,
                  "buckets": []
                }
              }
            }
            """;

    private final ObjectMapper mapper = new ObjectMapper();

    private UniversalFilteringImpl model;
    private HttpClient httpClient;
    private HttpResponse<String> httpResponse;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        model = new UniversalFilteringImpl();
        httpClient = mock(HttpClient.class);
        httpResponse = mock(HttpResponse.class);

        // Your actual production configuration field, found even if private.
        // The endpoint is a path; getEndpointUrl(...) supplies the origin.
        Field configField = findField(model.getClass(),
                "universalFilterApiConfiguration");
        Object configMock = mock(configField.getType(), invocation -> {
            if ("getElasticSearchEndpoint".equals(
                    invocation.getMethod().getName())) {
                return "/ecsearch/ie_en";
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        configField.setAccessible(true);
        configField.set(model, configMock);

        // Supply the Sling request used by getEndpointUrl(...).
        SlingHttpServletRequest slingRequest =
                mock(SlingHttpServletRequest.class);
        when(slingRequest.getScheme()).thenReturn("https");
        when(slingRequest.getServerName()).thenReturn("www.kpmg.com");
        when(slingRequest.getServerPort()).thenReturn(443);
        when(slingRequest.getHeader("Host")).thenReturn("www.kpmg.com");
        when(slingRequest.getHeader("X-Forwarded-Proto"))
                .thenReturn("https");
        when(slingRequest.getHeader("X-Forwarded-Host"))
                .thenReturn("www.kpmg.com");
        when(slingRequest.getRequestURL()).thenReturn(
                new StringBuffer("https://www.kpmg.com/ie/en/home.html"));
        when(slingRequest.getRequestURI())
                .thenReturn("/ie/en/home.html");
        injectRequest(slingRequest);

        setField("sortBy", "filterDate");
        setField("numberOfCards", "4");
        setField("primaryTag", PRIMARY_TAG);
        setField("primaryTagCheckbox", "true");
        setField("templateTypes", Arrays.asList("insight", "news"));
        // 'items' remains null, exercising the model's null-safe path.
    }

    @Test
    void successfulResponseDeserializesHitsAndAggregations() throws Exception {
        ApiCall call = execute(200, SUCCESS_JSON);
        UniversalFilteringESApiResponse result = call.result();

        assertNotNull(result);
        assertEquals(2L, result.getHits().getTotal().getValue());
        assertEquals(2, result.getHits().getHits().size());
        assertEquals("First Article",
                result.getHits().getHits().get(0)
                        .getUniversalFilterCard().getTitle());
        assertEquals("Sponsorship",
                result.getAggregations().getTagsAggregation()
                        .getBuckets().get(0).getKey());
        assertEquals(38L,
                result.getAggregations().getTagsAggregation()
                        .getBuckets().get(0).getDocCount());
    }

    @Test
    void responseWithZeroResults() throws Exception {
        UniversalFilteringESApiResponse result =
                execute(200, NO_RESULTS_JSON).result();
        assertNotNull(result);
        assertEquals(0L, result.getHits().getTotal().getValue());
        assertTrue(result.getHits().getHits().isEmpty());
        assertTrue(result.getAggregations().getTagsAggregation()
                .getBuckets().isEmpty());
    }

    @Test
    void verifiesPostHeadersAndBasePayload() throws Exception {
        HttpRequest request = execute(200, SUCCESS_JSON).request();

        assertEquals("POST", request.method());
        assertEquals("https", request.uri().getScheme());
        assertTrue(request.uri().getPath().contains("ecsearch"));
        assertEquals("application/json", request.headers()
                .firstValue("Content-Type").orElse(null));
        assertEquals("application/json", request.headers()
                .firstValue("Accept").orElse(null));

        JsonNode payload = readBody(request);
        assertEquals("universal_filtering_template_onload",
                payload.path("id").asText());
        assertEquals(0, payload.path("params").path("from").asInt());
    }

    @ParameterizedTest
    @CsvSource({
            "orgPublishDate, desc, sortByOriginalPubDate",
            "alphabetical, asc, sortByAlphabetical",
            "filterDate, desc, sortByFilterDate"
    })
    void verifiesSortOptions(String sortBy, String direction, String flag)
            throws Exception {
        setField("sortBy", sortBy);

        JsonNode params = readBody(execute(200, SUCCESS_JSON).request())
                .path("params");
        assertEquals(direction, params.path("sort").asText());
        assertTrue(params.path(flag).asBoolean());
    }

    @ParameterizedTest
    @CsvSource({"4, 8", "6, 6", "other, 6"})
    void verifiesCardCount(String configuredCount, int expectedSize)
            throws Exception {
        setField("numberOfCards", configuredCount);

        JsonNode params = readBody(execute(200, SUCCESS_JSON).request())
                .path("params");
        assertEquals(expectedSize, params.path("size").asInt());
    }

    @Test
    void primaryTagEnabledIncludesPrimaryTagAndFilterTags() throws Exception {
        JsonNode params = readBody(execute(200, SUCCESS_JSON).request())
                .path("params");
        assertEquals(PRIMARY_TAG, params.path("primary_tag").asText());
        assertEquals(PRIMARY_TAG,
                params.path("filter_tags_1").get(0).asText());
    }

    @Test
    void primaryTagDisabledOmitsPrimaryTagParameter() throws Exception {
        setField("primaryTagCheckbox", "false");

        JsonNode params = readBody(execute(200, SUCCESS_JSON).request())
                .path("params");
        assertFalse(params.has("primary_tag"));
        // In your current implementation, primaryTag is still added
        // unconditionally to filter_tags_1 even if checkbox is false.
        assertEquals(PRIMARY_TAG,
                params.path("filter_tags_1").get(0).asText());
    }

    @Test
    void blankPrimaryTagOmitsPrimaryTagParameter() throws Exception {
        setField("primaryTag", "");

        JsonNode params = readBody(execute(200, SUCCESS_JSON).request())
                .path("params");
        assertFalse(params.has("primary_tag"));
    }

    @Test
    void documentTypesAreUppercaseAndBlankOnesSkipped() throws Exception {
        setField("templateTypes", Arrays.asList(
                "insight", null, "  ", "news"));

        JsonNode types = readBody(execute(200, SUCCESS_JSON).request())
                .path("params").path("document_type");
        assertEquals(2, types.size());
        assertEquals("INSIGHT", types.get(0).asText());
        assertEquals("NEWS", types.get(1).asText());
    }

    @Test
    void nullDocumentTypesProducesEmptyArray() throws Exception {
        setField("templateTypes", null);
        JsonNode types = readBody(execute(200, SUCCESS_JSON).request())
                .path("params").path("document_type");
        assertTrue(types.isArray());
        assertEquals(0, types.size());
    }

    @ParameterizedTest
    @CsvSource({"199", "300", "400", "404", "500"})
    void httpErrorReturnsEmptyPojo(int status) throws Exception {
        UniversalFilteringESApiResponse result =
                execute(status, "{\"error\":\"failed\"}").result();
        assertNotNull(result);
        assertNotNull(result.getHits());
        assertTrue(result.getHits().getHits().isEmpty());
    }

    @Test
    void blankApiBodyReturnsEmptyPojo() throws Exception {
        UniversalFilteringESApiResponse result = execute(200, "").result();
        assertNotNull(result);
        assertTrue(result.getHits().getHits().isEmpty());
    }

    @Test
    void invalidJsonReturnsEmptyPojo() throws Exception {
        UniversalFilteringESApiResponse result =
                execute(200, "{invalid-json").result();
        assertNotNull(result);
        assertTrue(result.getHits().getHits().isEmpty());
    }

    @Test
    void ioExceptionReturnsEmptyPojo() throws Exception {
        when(httpClient.send(any(HttpRequest.class),
                org.mockito.ArgumentMatchers
                        .<HttpResponse.BodyHandler<String>>any()))
                .thenThrow(new IOException("connection refused"));
        UniversalFilteringESApiResponse result = executeMocked();
        assertNotNull(result);
        assertTrue(result.getHits().getHits().isEmpty());
    }

    @Test
    void interruptedExceptionReturnsEmptyPojo() throws Exception {
        when(httpClient.send(any(HttpRequest.class),
                org.mockito.ArgumentMatchers
                        .<HttpResponse.BodyHandler<String>>any()))
                .thenThrow(new InterruptedException("cancelled"));
        UniversalFilteringESApiResponse result = executeMocked();
        assertNotNull(result);
        assertTrue(result.getHits().getHits().isEmpty());
    }

    // -------------------------- Test helpers --------------------------

    /** Intercepts HttpClient.newHttpClient() without modifying the Sling Model. */
    private UniversalFilteringESApiResponse executeMocked() {
        try (MockedStatic<HttpClient> factory = mockStatic(HttpClient.class)) {
            factory.when(HttpClient::newHttpClient).thenReturn(httpClient);
            return model.getESApiResponse();
        }
    }

    private ApiCall execute(int status, String body) throws Exception {
        when(httpResponse.statusCode()).thenReturn(status);
        when(httpResponse.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class),
                org.mockito.ArgumentMatchers
                        .<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(httpResponse);

        UniversalFilteringESApiResponse result = executeMocked();
        ArgumentCaptor<HttpRequest> requestCaptor =
                ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(),
                org.mockito.ArgumentMatchers
                        .<HttpResponse.BodyHandler<String>>any());
        return new ApiCall(result, requestCaptor.getValue());
    }

    private record ApiCall(UniversalFilteringESApiResponse result,
                           HttpRequest request) { }

    /** Reads the real BodyPublishers.ofString payload for JSON assertions. */
    private JsonNode readBody(HttpRequest request) throws Exception {
        HttpRequest.BodyPublisher publisher = request.bodyPublisher()
                .orElseThrow(() -> new AssertionError("No request body"));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CountDownLatch complete = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        publisher.subscribe(new Flow.Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer buffer) {
                byte[] chunk = new byte[buffer.remaining()];
                buffer.get(chunk);
                bytes.write(chunk, 0, chunk.length);
            }

            @Override
            public void onError(Throwable throwable) {
                failure.set(throwable);
                complete.countDown();
            }

            @Override
            public void onComplete() {
                complete.countDown();
            }
        });

        assertTrue(complete.await(5, TimeUnit.SECONDS),
                "Timed out reading POST body");
        assertNull(failure.get(), "Error reading POST body");
        return mapper.readTree(bytes.toString(StandardCharsets.UTF_8));
    }

    private void setField(String name, Object value) throws Exception {
        Field field = findField(model.getClass(), name);
        field.setAccessible(true);
        field.set(model, value);
    }

    private static Field findField(Class<?> type, String name)
            throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // Also search AbstractKPMGComponentImpl (the superclass).
            }
        }
        throw new NoSuchFieldException(name);
    }

    private void injectRequest(SlingHttpServletRequest request)
            throws Exception {
        boolean injected = false;
        for (Class<?> c = model.getClass(); c != null;
             c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (field.getType().isInstance(request)
                        && field.getType().getSimpleName()
                                .contains("Request")) {
                    field.setAccessible(true);
                    field.set(model, request);
                    injected = true;
                }
            }
        }
        assertTrue(injected, "No SlingHttpServletRequest field found. "
                + "Adjust injectRequest() to match the request field "
                + "used by your getEndpointUrl(...) method.");
    }
}
