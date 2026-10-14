
@ExtendWith(MockitoExtension.class)
class UniversalFilteringImplTest {
    @InjectMocks
    private UniversalFilteringImpl model;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @Test
    void testGetESApiResponse() throws Exception {

        // Read JSON from resources
        String mockJson;

        try (InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(
                    "UniversalFilterESApiResponse.json")) {

            assertNotNull(inputStream);

            mockJson = new String(
                inputStream.readAllBytes(),
                StandardCharsets.UTF_8
            );
        }

        // Your existing endpoint and payload setup
        // must be completed before calling the method.

        // Mock response
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(mockJson);

        // Mock HTTP send
        when(httpClient.send(
                any(HttpRequest.class),
                ArgumentMatchers
                    .<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(httpResponse);

        // IMPORTANT: Mock static HttpClient creation
        try (MockedStatic<HttpClient> mockedClient =
                Mockito.mockStatic(HttpClient.class)) {

            mockedClient
                .when(HttpClient::newHttpClient)
                .thenReturn(httpClient);

            // Actual Sling Model method
            UniversalFilteringESApiResponse result =
                    model.getESApiResponse();

            assertNotNull(result);

            // Verify HTTP call was mocked
            verify(httpClient, times(1)).send(
                any(HttpRequest.class),
                ArgumentMatchers
                    .<HttpResponse.BodyHandler<String>>any()
            );

            verify(httpResponse).body();
        }
    }
}
