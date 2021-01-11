package uk.ac.sanger.storelight.graphql;

import graphql.*;
import graphql.spring.web.servlet.GraphQLInvocation;
import graphql.spring.web.servlet.GraphQLInvocationData;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;
import uk.ac.sanger.storelight.config.ApiKeyConfig;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author dr6
 */
@Component
@Primary
public class CustomGraphQLInvocation implements GraphQLInvocation {
    private final GraphQL graphQL;
    private final ApiKeyConfig apiKeyConfig;

    @Autowired
    public CustomGraphQLInvocation(GraphQL graphQL, ApiKeyConfig apiKeyConfig) {
        this.graphQL = graphQL;
        this.apiKeyConfig = apiKeyConfig;
    }

    private String getHeaderOrVariable(String name, WebRequest request, Map<String, ?> variables) {
        String value = request.getHeader(name);
        if (value!=null) {
            return value;
        }
        Object obj = variables.get(name);
        return (obj!=null ? obj.toString() : null);
    }

    @Override
    public CompletableFuture<ExecutionResult> invoke(GraphQLInvocationData invocationData, WebRequest request) {
        Map<String, Object> variables = invocationData.getVariables();
        StoreRequestContext context = getStoreRequestContext(request, variables);

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(invocationData.getQuery())
                .operationName(invocationData.getOperationName())
                .variables(variables)
                .context(context)
                .build();
        return graphQL.executeAsync(executionInput);
    }

    @NotNull
    private StoreRequestContext getStoreRequestContext(WebRequest request, Map<String, Object> variables) {
        String apiKey = getHeaderOrVariable(StorelightApi.API_KEY, request, variables);
        String username = getHeaderOrVariable(StorelightApi.API_KEY, request, variables);
        String app = (apiKey==null ? null : apiKeyConfig.getApp(apiKey));
        return new StoreRequestContext(apiKey, app, username);
    }
}
