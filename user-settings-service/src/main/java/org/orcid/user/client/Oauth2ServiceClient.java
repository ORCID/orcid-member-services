package org.orcid.user.client;

import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.orcid.user.config.Constants;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

@AuthorizedFeignClient(name = "OAUTH2SERVICE")
public interface Oauth2ServiceClient {

    @RequestMapping(method = RequestMethod.GET, value = "/api/users/{login}", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @HystrixProperty(name = "hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", value = Constants.HYSTRIX_TIMEOUT)
    ResponseEntity<String> getUser(@PathVariable("login") String login);
    
    @RequestMapping(method = RequestMethod.GET, value = "/api/users/id/{id}", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @HystrixProperty(name = "hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", value = Constants.HYSTRIX_TIMEOUT)
    ResponseEntity<String> getUserById(@PathVariable("id") String id);
    
    @RequestMapping(method = RequestMethod.POST, value = "/api/users", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @HystrixProperty(name = "hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", value = Constants.HYSTRIX_TIMEOUT)
    ResponseEntity<Void> registerUser(Map<String, ?> queryMap);

    @RequestMapping(method = RequestMethod.PUT, value = "/api/users", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @HystrixProperty(name = "hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", value = Constants.HYSTRIX_TIMEOUT)
    ResponseEntity<String> updateUser(Map<String, ?> queryMap);
    
    @RequestMapping(method = RequestMethod.DELETE, value = "/api/users/clear/{id}", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @HystrixProperty(name = "hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", value = Constants.HYSTRIX_TIMEOUT)
    ResponseEntity<String> clearUser(@PathVariable("id") String id);
}
