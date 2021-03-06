package com.greenfox.notification.controller;

import com.greenfox.notification.NotificationApplication;
import com.greenfox.notification.model.classes.heartbeat.Heartbeat;
import com.greenfox.notification.repository.HeartbeatRepository;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.charset.Charset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = NotificationApplication.class)
@WebAppConfiguration
@EnableWebMvc
public class ControllerTests {
  private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
          MediaType.APPLICATION_JSON.getSubtype(),
          Charset.forName("utf8"));
  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext webApplicationContext;
  @Autowired
  private HeartbeatRepository heartbeatRepository;

  @Before
  public void setup() throws Exception {
    mockMvc = webAppContextSetup(webApplicationContext).build();
    ConnectionFactory factory = new ConnectionFactory();
    factory.setUri(System.getenv("RABBITMQ_BIGWIG_TX_URL"));
    Connection connection = factory.newConnection();
    connection.close(-1);
  }

  @Test
  public void testGetWithEmptyTable() throws Exception {
    heartbeatRepository.deleteAll();
    mockMvc.perform(get("/heartbeat"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.database").value("error"))
            .andExpect(jsonPath("$.queue").value("ok"));
  }

  @Test
  public void testGetWithNotEmptyTable() throws Exception {
    heartbeatRepository.save(new Heartbeat());
    mockMvc.perform(get("/heartbeat"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.database").value("ok"))
            .andExpect(jsonPath("$.queue").value("ok"));
    heartbeatRepository.deleteAll();
  }

  @Test
  public void testUnsubscribeWithValidFields() throws Exception {
    mockMvc.perform(post("/unsubscriptions")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\n" +
                    "     \"data\": {\n" +
                    "       \"type\": \"unsubscribtions\",\n" +
                    "       \"attributes\": {\n" +
                    "         \"email\": \"john.doe@example.org\",\n" +
                    "         \"created_at\": \"2017-06-26T14:05:10+0000\" \n" +
                    "       }\n" +
                    "     }\n" +
                    "   }"))
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.links.self").value(System.getenv("HOSTNAME") + "/unsubscriptions/1"))
            .andExpect(jsonPath("$.data.id").value("1"))
            .andExpect(status().isOk());
  }

  @Test
  public void testUnsubscribeWithInvalidFields() throws Exception {
    mockMvc.perform(post("/unsubscriptions")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\n" +
                    "     \"data\": {\n" +
                    "       \"type\": \"unsubscribtions\",\n" +
                    "       \"attributes\": {\n" +
                    "         \"created_at\": \"2017-06-26T14:05:10+0000\" \n" +
                    "       }\n" +
                    "     }\n" +
                    "   }"))
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.errors.[0].status").value("400"))
            .andExpect(jsonPath("$.errors.[0].title").value("Bad Request"))
            .andExpect(jsonPath("$.errors.[0].detail").value("The attribute field: \"email\" is missing"));
  }
}
