package org.orcid.memberportal.service.assertion.services.locale;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LocaleUtils {
    
    private static final Map<String, Locale> CUSTOM_LOCALES;
    
    static {
        CUSTOM_LOCALES = new HashMap<>();
        CUSTOM_LOCALES.put("zh_TW", Locale.TRADITIONAL_CHINESE);
    }
    
    /**
     * Returns Locale object for member portal custom locale if specified, otherwise creates a new Locale.
     * 
     * @param langKey
     * @return
     */
    public static Locale getLocale(String langKey) {
        if (CUSTOM_LOCALES.containsKey(langKey)) {
            return CUSTOM_LOCALES.get(langKey);
        }
        return new Locale(langKey);
    }

}
