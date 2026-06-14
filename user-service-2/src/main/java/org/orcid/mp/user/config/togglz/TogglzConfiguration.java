package org.orcid.mp.user.config.togglz;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.togglz.core.manager.EnumBasedFeatureProvider;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.manager.FeatureManagerBuilder;
import org.togglz.core.repository.StateRepository;
import org.togglz.core.spi.FeatureProvider;
import org.togglz.core.user.FeatureUser;
import org.togglz.core.user.SimpleFeatureUser;
import org.togglz.core.user.UserProvider;
import org.togglz.mongodb.MongoStateRepository;

import com.mongodb.client.MongoClient;

/**
 * Togglz configuration for feature toggle management
 */
@Configuration
public class TogglzConfiguration {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoClient mongoClient; 

    @Bean
    public FeatureProvider featureProvider() {
        return new EnumBasedFeatureProvider(PortalFeatures.class);
    }

    @Bean
    public StateRepository stateRepository() {
        String databaseName = mongoTemplate.getDb().getName();
        
        return MongoStateRepository.newBuilder(mongoClient, databaseName)
                .collection("togglz_features")
                .build();
    }

    @Bean
    public UserProvider userProvider() {
        return new UserProvider() {
            @Override
            public FeatureUser getCurrentUser() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    String username = authentication.getName();
                    boolean isAdmin = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(auth -> auth.equals("ROLE_ADMIN"));
                    return new SimpleFeatureUser(username, isAdmin);
                }
                return null;
            }
        };
    }

    @Bean
    public FeatureManager featureManager(StateRepository stateRepository,
                                         FeatureProvider featureProvider,
                                         UserProvider userProvider) {
        return new FeatureManagerBuilder()
            .featureProvider(featureProvider)
            .stateRepository(stateRepository)
            .userProvider(userProvider)
            .build();
    }
}