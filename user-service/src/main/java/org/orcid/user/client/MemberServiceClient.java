package org.orcid.user.client;

import javax.ws.rs.core.MediaType;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

@AuthorizedFeignClient(name = "memberservice")
public interface MemberServiceClient {

    @RequestMapping(method = RequestMethod.GET, value = "/api/members/{id}", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @HystrixProperty(name = "hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", value = "5000")
    ResponseEntity<String> getMember(@PathVariable("id") String salesforceId);
    
}
