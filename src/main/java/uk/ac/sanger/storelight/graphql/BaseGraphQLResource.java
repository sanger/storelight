package uk.ac.sanger.storelight.graphql;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import uk.ac.sanger.storelight.requests.LocationIdentifier;

/**
 * Base class of graphql resources with some helper methods
 * @author dr6
 */
public abstract class BaseGraphQLResource {
    protected final ObjectMapper objectMapper;

    protected BaseGraphQLResource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    protected LocationIdentifier getLocationIdentifier(DataFetchingEnvironment dfe) {
        return getLocationIdentifier(dfe, "location");
    }

    protected LocationIdentifier getLocationIdentifier(DataFetchingEnvironment dfe, String name) {
        LocationIdentifier li = arg(dfe, name, LocationIdentifier.class);
        if (li==null || !li.isSpecified()) {
            throw new IllegalArgumentException("No identifier given for location.");
        }
        return li;
    }

    protected <E> E arg(DataFetchingEnvironment dfe, String name, Class<E> cls) {
        return objectMapper.convertValue(dfe.getArgument(name), cls);
    }

    protected <E> E arg(DataFetchingEnvironment dfe, String name, TypeReference<E> typeRef) {
        return objectMapper.convertValue(dfe.getArgument(name), typeRef);
    }

    protected StoreRequestContext context(DataFetchingEnvironment dfe) {
        return dfe.getContext();
    }

    protected StoreRequestContext auth(DataFetchingEnvironment dfe) {
        StoreRequestContext ctxt = context(dfe);
        if (ctxt==null || ctxt.getApiKey()==null || ctxt.getApiKey().isEmpty()) {
            throw new AuthenticationCredentialsNotFoundException("No API key.");
        }
        if (ctxt.getApp()==null) {
            throw new AuthenticationCredentialsNotFoundException("Invalid API key.");
        }
        return ctxt;
    }
}
