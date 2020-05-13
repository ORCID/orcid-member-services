package org.orcid.member.client;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.orcid.member.service.user.MemberServiceUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

@AuthorizedFeignClient(name = "user-service")
public interface UserServiceClient {

    @RequestMapping(method = RequestMethod.GET, value = "/api/users/{loginOrId}", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @HystrixProperty(name = "hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", value = "5000")
    ResponseEntity<MemberServiceUser> getUser(@PathVariable("loginOrId") String loginOrId);
    
    @RequestMapping(method = RequestMethod.GET, value = "/api/users/salesforce/{salesforceId}", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @HystrixProperty(name = "hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", value = "5000")
    ResponseEntity<List<MemberServiceUser>> getUsersBySalesforceId(@PathVariable("salesforceId") String salesforceId);
    
    @RequestMapping(method = RequestMethod.POST, value = "/api/users", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @HystrixProperty(name = "hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", value = "5000")
    ResponseEntity<Void> registerUser(Map<String, ?> queryMap);

    @RequestMapping(method = RequestMethod.PUT, value = "/api/users", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @HystrixProperty(name = "hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", value = "5000")
    ResponseEntity<String> updateUser(MemberServiceUser memberServiceUser);
}
