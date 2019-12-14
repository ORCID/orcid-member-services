package org.orcid.client;

import javax.ws.rs.core.MediaType;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

@AuthorizedFeignClient(name = "USERSETTINGSSERVICE")
public interface UserSettingsClient {

    @RequestMapping(method = RequestMethod.GET, value = "/settings/api/user/{login}", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @HystrixProperty(name = "hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", value = "3000")
    ResponseEntity<String> getUserSettings(@PathVariable("login") String login);
    
}
