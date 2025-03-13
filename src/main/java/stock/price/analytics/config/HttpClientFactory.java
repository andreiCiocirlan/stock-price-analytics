package stock.price.analytics.config;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class HttpClientFactory {
    private static CloseableHttpClient httpClient;

    private HttpClientFactory() {}

    public static synchronized CloseableHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = createHttpClient();
        }
        return httpClient;
    }

    private static CloseableHttpClient createHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD)
                .build();
        return HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
}
