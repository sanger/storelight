package uk.ac.sanger.storelight;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.ac.sanger.storelight.graphql.StorelightApi;

import java.io.IOException;
import java.net.URL;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tool for helping test graphql api
 * @author dr6
 */
@TestComponent
public class GraphQLTester {
    @Autowired
    private MockMvc mockMvc;

    public <T> T post(String query, String apiKey) throws Exception {
        JSONObject jo = new JSONObject();
        jo.put("query", query);
        var builder = MockMvcRequestBuilders.post("/graphql")
                .content(jo.toString());
        if (apiKey!=null) {
            builder = builder.header(StorelightApi.API_KEY, apiKey);
        }
        MvcResult result = mockMvc.perform(builder
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        //noinspection unchecked
        return (T) result.getAsyncResult();
    }

    public <T> T post(String query) throws Exception {
        return post(query, "testkey");
    }

    @SuppressWarnings("UnstableApiUsage")
    public String readResource(String path) throws IOException {
        URL url = Resources.getResource(path);
        return Resources.toString(url, Charsets.UTF_8);
    }
}
