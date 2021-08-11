package org.orcid.member.client;

import javax.ws.rs.core.MediaType;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

@AuthorizedFeignClient(name = "assertionservice")
public interface AssertionServiceClient {

    @RequestMapping(method = RequestMethod.GET, value = "/api/assertion/owner/{encryptedEmail}", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @HystrixProperty(name = "hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", value = "50000")
    ResponseEntity<String> getOwnerOfUser(@PathVariable("encryptedEmail") String encryptedEmail);

    @RequestMapping(method = RequestMethod.DELETE, value = "/api/assertion/delete/{salesforceId}", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @HystrixProperty(name = "hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", value = "50000")
    ResponseEntity<String> deleteAssertionsForSalesforceId(@PathVariable("salesforceId") String salesforceId);

    @RequestMapping(method = RequestMethod.PUT, value = "/api/assertion/update/{salesforceId}/{newSalesforceId}", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @HystrixProperty(name = "hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", value = "50000")
    ResponseEntity<String> updateAssertionsSalesforceId(@PathVariable("salesforceId") String salesforceId, @PathVariable("newSalesforceId") String newSalesforceId);

}
