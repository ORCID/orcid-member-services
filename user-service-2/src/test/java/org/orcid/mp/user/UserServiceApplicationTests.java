package org.orcid.mp.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.mongodb.client.MongoClient;

@SpringBootTest
class UserServiceApplicationTests {

	@MockitoBean
    private MongoTemplate mongoTemplate;

    @MockitoBean
    private MongoClient mongoClient;

	@Test
	void contextLoads() {
	}

}
