package org.orcid.user.client;

import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

import feign.Headers;

@AuthorizedFeignClient (
		name = "OAUTH2SERVICE"   
	)
public interface Oauth2ServiceClient {

	@RequestMapping(method = RequestMethod.POST, value = "/api/users", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
	@HystrixProperty(name = "hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", value = "3000")
	ResponseEntity<Void> registerUser(Map<String, ?> queryMap);
	
	@RequestMapping(method = RequestMethod.GET, value = "/api/users/{login}", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
	@HystrixProperty(name = "hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", value = "3000")
	ResponseEntity<String> getUser(@PathVariable("login") String login);
}
