package io.github.jhipster.registry.service.dto;

public class VersionDTO {
    
    private String version;
    
    public VersionDTO(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
    
}
