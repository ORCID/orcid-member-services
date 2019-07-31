package org.orcid.user.client;

import java.util.List;
import java.util.Map;

import org.orcid.user.config.FeignFormEncoderConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import feign.Body;
import feign.Headers;
import feign.Param;

@AuthorizedFeignClient (
		name = "OAUTH2SERVICE"   
	)
public interface Oauth2ServiceClient {

	@RequestMapping(method = RequestMethod.POST, value = "/api/register")
    @Headers("Content-Type: application/json")
    /*@Body("%7B\n" +
            "  \"login\": \"{login}\"," +
            "  \"password\": {password}," +
            "  \"email\": {email}," +
            "  \"authorities\": {authorities}" +
            "%7D")*/	
    void registerUser(Map<String, ?> queryMap);
}
