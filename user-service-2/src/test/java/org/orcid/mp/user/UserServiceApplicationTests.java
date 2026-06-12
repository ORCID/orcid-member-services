package org.orcid.mp.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.togglz.core.repository.StateRepository;

@SpringBootTest
class UserServiceApplicationTests {

	@MockitoBean
	private StateRepository stateRepository;

	@Test
	void contextLoads() {
	}

}
