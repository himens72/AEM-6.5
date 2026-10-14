
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

        // Read mock JSON from file
        String mockJson;
        try (InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream("mock/es-response.json")) {

            assertNotNull(inputStream);
            mockJson = new String(
                inputStream.readAllBytes(),
                StandardCharsets.UTF_8
            );
        }

        // Initialize your model's endpoint and payload
        // using your existing test setup.

        // Mock API response
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(mockJson);

        when(httpClient.send(
            any(HttpRequest.class),
            ArgumentMatchers
                .<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(httpResponse);

        // Mock HttpClient.newHttpClient()
        try (MockedStatic<HttpClient> mockedHttpClient =
                Mockito.mockStatic(HttpClient.class)) {

            mockedHttpClient
                .when(HttpClient::newHttpClient)
                .thenReturn(httpClient);

            // Execute actual method
            UniversalFilteringESApiResponse result =
                model.getESApiResponse();

            assertNotNull(result);

            verify(httpClient, times(1)).send(
                any(HttpRequest.class),
                ArgumentMatchers
                    .<HttpResponse.BodyHandler<String>>any()
            );
        }
    }
}
