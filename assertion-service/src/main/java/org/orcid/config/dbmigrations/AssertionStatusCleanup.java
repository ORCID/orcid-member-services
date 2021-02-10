package org.orcid.config.dbmigrations;

import org.orcid.service.AssertionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssertionStatusCleanup {
	private static final Logger LOG = LoggerFactory.getLogger(AssertionStatusCleanup.class);
	
	@Autowired
    private AssertionService assertionsService;

    @Bean
    CommandLineRunner runner() {
        return args -> {
        	LOG.info("Running the assertion status cleanup process.");
        	assertionsService.assertionStatusCleanup();
        };
    }
}
