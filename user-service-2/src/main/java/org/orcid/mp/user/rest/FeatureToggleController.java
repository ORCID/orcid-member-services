package org.orcid.mp.user.rest;

import org.orcid.mp.user.config.togglz.PortalFeatures;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.togglz.core.manager.FeatureManager;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/features")
public class FeatureToggleController {

    private final FeatureManager featureManager;

    public FeatureToggleController(FeatureManager featureManager) {
        this.featureManager = featureManager;
    }

    @GetMapping
    public Map<String, Boolean> getAllFeatureStates() {
        Map<String, Boolean> featureMap = new HashMap<>();
        for (PortalFeatures feature : PortalFeatures.values()) {
            featureMap.put(feature.name(), featureManager.isActive(feature));
        }
        return featureMap;
    }
}