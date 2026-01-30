package org.orcid.mp.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;

@Configuration
public class InternalClientConfig {

    @Value("${application.memberService.apiUrl}")
    private String memberServiceApiUrl;

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService clientService) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials() // <--- This enables the M2M flow
                        .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, clientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }


    /**
     * RestClient for making internal requests to the member service with scope 'internal', where no user token is present for authorization.
     * @param authorizedClientManager
     * @return
     */
    @Bean("internalMemberServiceRestClient")
    public RestClient internalRestClient(OAuth2AuthorizedClientManager authorizedClientManager) {

        ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
            // "internal-user-client" MUST match the registration ID in your application.yml
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId("internal-user-client")
                    .principal("user-service") // Arbitrary string identifying who is running this
                    .build();

            // This performs the magic: checks cache, or calls URL to get fresh token
            OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);

            if (authorizedClient != null) {
                String token = authorizedClient.getAccessToken().getTokenValue();
                request.getHeaders().setBearerAuth(token);
            }

            return execution.execute(request, body);
        };

        return RestClient.builder()
                // Point this to your MEMBER SERVICE (Resource Server)
                .baseUrl(memberServiceApiUrl)
                .requestInterceptor(interceptor)
                .build();
    }

}
