
@Test
void testGetESApiResponse() throws Exception {

    // Read mocked JSON from file
    String mockJson;
    try (InputStream is = getClass()
            .getClassLoader()
            .getResourceAsStream(
                "UniversalFilterESApiResponse.json")) {

        assertNotNull(is);
        mockJson = new String(
            is.readAllBytes(),
            StandardCharsets.UTF_8
        );
    }

    HttpClient mockClient = mock(HttpClient.class);

    @SuppressWarnings("unchecked")
    HttpResponse<String> mockResponse =
        mock(HttpResponse.class);

    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.body()).thenReturn(mockJson);

    when(mockClient.send(
        any(HttpRequest.class),
        ArgumentMatchers
            .<HttpResponse.BodyHandler<String>>any()
    )).thenReturn(mockResponse);

    try (MockedStatic<HttpClient> mockedStatic =
            Mockito.mockStatic(HttpClient.class)) {

        mockedStatic
            .when(HttpClient::newHttpClient)
            .thenReturn(mockClient);

        // Verify static mocking works
        assertSame(
            mockClient,
            HttpClient.newHttpClient()
        );

        // Call actual method INSIDE static mock
        UniversalFilteringESApiResponse result =
            model.getESApiResponse();

        assertNotNull(result);

        mockedStatic.verify(
            HttpClient::newHttpClient,
            atLeastOnce()
        );

        verify(mockClient).send(
            any(HttpRequest.class),
            ArgumentMatchers
                .<HttpResponse.BodyHandler<String>>any()
        );
    }
}
