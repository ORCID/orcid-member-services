package org.orcid.user.web.rest.errors;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import feign.Response;
import feign.codec.ErrorDecoder;

@Component
public class FeignErrorDecoder implements ErrorDecoder {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Exception decode(String methodKey, Response response) {
        try {            
            switch (response.status()) {
            case 400:
                logger.error("Feign client. Status code " + response.status() + ", methodKey = " + methodKey);
                return new ResponseStatusException(HttpStatus.valueOf(response.status()), getErrorMessage(response));
            case 404: {
                logger.error("Feign client. Status code " + response.status() + ", methodKey = " + methodKey);
                return new ResponseStatusException(HttpStatus.valueOf(response.status()), getErrorMessage(response));
            }
            default:
                return new Exception(response.reason());
            }
        } catch (Exception e) {
            logger.error("Unable to parse Feign exception");
            throw new RuntimeException(e);
        }
    }

    private String getErrorMessage(Response response) throws JSONException {        
        try {
            JSONObject j = new JSONObject(IOUtils.toString(response.body().asInputStream(), java.nio.charset.StandardCharsets.UTF_8));
            return j.getString("title");
        } catch (IOException e) {
            logger.error("read conflict response body exception. {}", e.toString());
            return "Unknown: " + e.toString();
        }
    }

}
