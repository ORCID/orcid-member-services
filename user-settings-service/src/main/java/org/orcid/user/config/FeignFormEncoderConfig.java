package org.orcid.user.config;

import org.springframework.context.annotation.Bean;

import feign.codec.Encoder;
import feign.form.FormEncoder;

public class FeignFormEncoderConfig {
	@Bean
    public Encoder encoder() {
        return new FormEncoder();
    }
}
