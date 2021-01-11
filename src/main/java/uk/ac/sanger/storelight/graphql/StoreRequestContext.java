package uk.ac.sanger.storelight.graphql;

import com.google.common.base.MoreObjects;

import java.util.Objects;

import static uk.ac.sanger.storelight.utils.BasicUtils.repr;

/**
 * @author dr6
 */
public class StoreRequestContext {
    private final String apiKey;
    private final String app;
    private final String username;

    public StoreRequestContext(String apiKey, String app, String username) {
        this.apiKey = apiKey;
        this.app = app;
        this.username = username;
    }

    public String getApiKey() {
        return this.apiKey;
    }

    public String getApp() {
        return this.app;
    }

    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoreRequestContext that = (StoreRequestContext) o;
        return (Objects.equals(this.apiKey, that.apiKey)
                && Objects.equals(this.app, that.app)
                && Objects.equals(this.username, that.username));
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiKey, username);
    }

    @Override
    public String toString() {
        if (app!=null) {
            return String.format("(app=%s, user=%s)", app, repr(username));
        }
        return MoreObjects.toStringHelper(this)
                .add("apiKey", apiKey==null ? "null" : "********")
                .add("app", app)
                .add("username", repr(username))
                .toString();
    }
}
