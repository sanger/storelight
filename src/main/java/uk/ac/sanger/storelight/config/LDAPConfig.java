package uk.ac.sanger.storelight.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author dr6
 */
@Configuration
@PropertySource("classpath:ldap.properties")
public class LDAPConfig {
    @Value("${contextFactory}")
    String contextFactory;
    @Value("${providerUrl}")
    String providerUrl;
    @Value("${securityAuthentication}")
    String securityAuthentication;
    @Value("${bypassPassword}")
    String bypassPassword;
    @Value("${userDnPatterns}")
    String userDnPatterns;
    @Value("${groupSearchBase}")
    String groupSearchBase;

    public LDAPConfig() {}

    public LDAPConfig(String contextFactory, String providerUrl, String securityAuthentication, String bypassPassword,
                      String userDnPatterns, String groupSearchBase) {
        this.contextFactory = contextFactory;
        this.providerUrl = providerUrl;
        this.securityAuthentication = securityAuthentication;
        this.bypassPassword = bypassPassword;
        this.userDnPatterns = userDnPatterns;
        this.groupSearchBase = groupSearchBase;
    }

    public String getContextFactory() {
        return this.contextFactory;
    }

    public String getProviderUrl() {
        return this.providerUrl;
    }

    public String getSecurityAuthentication() {
        return this.securityAuthentication;
    }

    public String getBypassPassword() {
        return this.bypassPassword;
    }

    public String getUserDnPatterns() {
        return this.userDnPatterns;
    }

    public String getGroupSearchBase() {
        return this.groupSearchBase;
    }
}
