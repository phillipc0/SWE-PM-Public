package de.telekom.swepm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Component
public class TestUtils {

    @Autowired
    private MockMvc mockMvc;

    public ResultActions postRequest(String path, Object body) throws Exception {
        return mockMvc.perform(post(path)
            .contentType(APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(body)));
    }

    public ResultActions getRequest(String path) throws Exception {
        return mockMvc.perform(get(path));
    }

    public ResultActions patchRequest(String path, Object body) throws Exception {
        return mockMvc.perform(patch(path)
            .contentType(APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(body)));
    }

    public ResultActions deleteRequest(String path) throws Exception {
        return mockMvc.perform(delete(path));
    }
}
