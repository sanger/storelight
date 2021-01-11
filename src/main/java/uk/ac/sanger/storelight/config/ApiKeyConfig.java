package uk.ac.sanger.storelight.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author dr6
 */
@Configuration
public class ApiKeyConfig {
    @Value("#{${uk.ac.sanger.storelight.apikeys}}")
    Map<String, String> apiKeys;

    public Map<String, String> getApiKeys() {
        return this.apiKeys;
    }

    public boolean isValid(String apiKey) {
        return getApiKeys().containsKey(apiKey);
    }

    public String getApp(String apiKey) {
        return getApiKeys().get(apiKey);
    }
}
