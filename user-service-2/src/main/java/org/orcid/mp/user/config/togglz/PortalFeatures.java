package org.orcid.mp.user.config.togglz;

import org.togglz.core.Feature;
import org.togglz.core.annotation.EnabledByDefault;
import org.togglz.core.annotation.Label;
import org.togglz.core.context.FeatureContext;

/**
 * Feature toggles for the ORCID Member Portal
 */
public enum PortalFeatures implements Feature {

    @Label("Manage API credentials")
    MANAGE_API_CREDENTIALS;

    public boolean isActive() {
        return FeatureContext.getFeatureManager().isActive(this);
    }
}