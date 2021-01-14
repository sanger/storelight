package uk.ac.sanger.storelight.graphql;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.*;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URL;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

/**
 * @author dr6
 */
@Component
public class GraphQLProvider {

    private GraphQL graphQL;

    private final PlatformTransactionManager transactionManager;
    private final DataFetchers dataFetchers;
    private final LocationMutations locationMutations;
    private final StoreMutations storeMutations;
    private final UnstoreMutations unstoreMutations;

    @Autowired
    public GraphQLProvider(PlatformTransactionManager transactionManager,
                           DataFetchers dataFetchers,
                           LocationMutations locationMutations,
                           StoreMutations storeMutations,
                           UnstoreMutations unstoreMutations) {
        this.transactionManager = transactionManager;
        this.dataFetchers = dataFetchers;
        this.locationMutations = locationMutations;
        this.storeMutations = storeMutations;
        this.unstoreMutations = unstoreMutations;
    }

    @Bean
    public GraphQL graphQL() {
        return graphQL;
    }

    @PostConstruct
    public void init() throws IOException {
        //noinspection UnstableApiUsage
        URL url = Resources.getResource("schema.graphqls");
        //noinspection UnstableApiUsage
        String sdl = Resources.toString(url, Charsets.UTF_8);
        GraphQLSchema graphQLSchema = buildSchema(sdl);
        this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
    }

    private GraphQLSchema buildSchema(String sdl) {
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
        RuntimeWiring runtimeWiring = buildWiring();
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
    }

    private RuntimeWiring buildWiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query")
                        .dataFetcher("location", dataFetchers.getLocation())
                        .dataFetcher("stored", dataFetchers.getStored())
                )
                .type(newTypeWiring("Mutation")
                        .dataFetcher("addLocation", transact(locationMutations.addLocation()))
                        .dataFetcher("editLocation", transact(locationMutations.editLocation()))

                        .dataFetcher("storeBarcode", transact(storeMutations.storeBarcode()))
                        .dataFetcher("storeBarcodes", transact(storeMutations.storeBarcodes()))
                        .dataFetcher("store", transact(storeMutations.store()))

                        .dataFetcher("unstoreBarcode", transact(unstoreMutations.unstoreBarcode()))
                        .dataFetcher("unstoreBarcodes", transact(unstoreMutations.unstoreBarcodes()))
                        .dataFetcher("empty", transact(unstoreMutations.empty()))
                )
                .scalar(CustomTypes.ADDRESS)
                .build();
    }

    private <T> DataFetcher<T> transact(DataFetcher<T> dataFetcher) {
        return dfe -> {
            DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
            transactionDefinition.setName("Mutation transaction");
            transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
            TransactionStatus status = transactionManager.getTransaction(transactionDefinition);
            boolean success = false;
            try {
                T value = dataFetcher.get(dfe);
                success = true;
                return value;
            } finally {
                if (success) {
                    transactionManager.commit(status);
                } else {
                    transactionManager.rollback(status);
                }
            }
        };
    }
}