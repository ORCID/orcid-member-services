package org.orcid.domain;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.orcid.domain.enumeration.AffiliationSection;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "assertion")
public class Affiliation implements Serializable {
    private static final long serialVersionUID = 1845971448687999429L;

    @Id
    private String id;

    @Pattern(regexp = ".*@.*\\..*")
    @Field("email")
    private String email;

    @Field("affiliation_section")
    private AffiliationSection affiliationSection;

    @Size(max = 4000)
    @Field("department_name")
    private String departmentName;

    @Size(max = 4000)
    @Field("role_title")
    private String roleTitle;

    @Field("start_year")
    private String startYear;

    @Field("start_month")
    private String startMonth;

    @Field("start_day")
    private String startDay;

    @Field("end_year")
    private String endYear;

    @Field("end_month")
    private String endMonth;

    @Field("end_day")
    private String endDay;

    @NotNull
    @Field("org_name")
    private String orgName;

    @NotNull
    @Field("org_country")
    private String orgCountry;

    @NotNull
    @Field("org_city")
    private String orgCity;

    @Field("org_region")
    private String orgRegion;

    @NotNull
    @Field("disambiguated_org_id")
    private String disambiguatedOrgId;

    @Field("disambiguation_source")
    private String disambiguationSource;

    @Field("external_id")
    private String externalId;

    @Field("external_id_type")
    private String externalIdType;

    @Field("external_id_url")
    private String externalIdUrl;

    @Field("put_code")
    private String putCode;

    @Field("created")
    private Instant created;

    @Field("modified")
    private Instant modified;

    @NotNull
    @Field("admin_id")
    private String adminId;

    @Field("orcid_error")
    private String orcidError;

    @Field("updated")
    private boolean updated;

    @Field("deleted_from_orcid")
    private Instant deletedFromORCID;

    private String status;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not
    // remove
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public Affiliation email(String email) {
        this.email = email;
        return this;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public AffiliationSection getAffiliationSection() {
        return affiliationSection;
    }

    public Affiliation affiliationSection(AffiliationSection affiliationSection) {
        this.affiliationSection = affiliationSection;
        return this;
    }

    public void setAffiliationSection(AffiliationSection affiliationSection) {
        this.affiliationSection = affiliationSection;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public Affiliation departmentName(String departmentName) {
        this.departmentName = departmentName;
        return this;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getRoleTitle() {
        return roleTitle;
    }

    public Affiliation roleTitle(String roleTitle) {
        this.roleTitle = roleTitle;
        return this;
    }

    public void setRoleTitle(String roleTitle) {
        this.roleTitle = roleTitle;
    }

    public String getStartYear() {
        return startYear;
    }

    public Affiliation startYear(String startYear) {
        this.startYear = startYear;
        return this;
    }

    public void setStartYear(String startYear) {
        this.startYear = startYear;
    }

    public String getStartMonth() {
        return startMonth;
    }

    public Affiliation startMonth(String startMonth) {
        this.startMonth = startMonth;
        return this;
    }

    public void setStartMonth(String startMonth) {
        this.startMonth = startMonth;
    }

    public String getStartDay() {
        return startDay;
    }

    public Affiliation startDay(String startDay) {
        this.startDay = startDay;
        return this;
    }

    public void setStartDay(String startDay) {
        this.startDay = startDay;
    }

    public String getEndYear() {
        return endYear;
    }

    public Affiliation endYear(String endYear) {
        this.endYear = endYear;
        return this;
    }

    public void setEndYear(String endYear) {
        this.endYear = endYear;
    }

    public String getEndMonth() {
        return endMonth;
    }

    public Affiliation endMonth(String endMonth) {
        this.endMonth = endMonth;
        return this;
    }

    public void setEndMonth(String endMonth) {
        this.endMonth = endMonth;
    }

    public String getEndDay() {
        return endDay;
    }

    public Affiliation endDay(String endDay) {
        this.endDay = endDay;
        return this;
    }

    public void setEndDay(String endDay) {
        this.endDay = endDay;
    }

    public String getOrgName() {
        return orgName;
    }

    public Affiliation orgName(String orgName) {
        this.orgName = orgName;
        return this;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getOrgCountry() {
        return orgCountry;
    }

    public Affiliation orgCountry(String orgCountry) {
        this.orgCountry = orgCountry;
        return this;
    }

    public void setOrgCountry(String orgCountry) {
        this.orgCountry = orgCountry;
    }

    public String getOrgCity() {
        return orgCity;
    }

    public Affiliation orgCity(String orgCity) {
        this.orgCity = orgCity;
        return this;
    }

    public void setOrgCity(String orgCity) {
        this.orgCity = orgCity;
    }

    public String getOrgRegion() {
        return orgRegion;
    }

    public Affiliation orgRegion(String orgRegion) {
        this.orgRegion = orgRegion;
        return this;
    }

    public void setOrgRegion(String orgRegion) {
        this.orgRegion = orgRegion;
    }

    public String getDisambiguatedOrgId() {
        return disambiguatedOrgId;
    }

    public Affiliation disambiguatedOrgId(String disambiguatedOrgId) {
        this.disambiguatedOrgId = disambiguatedOrgId;
        return this;
    }

    public void setDisambiguatedOrgId(String disambiguatedOrgId) {
        this.disambiguatedOrgId = disambiguatedOrgId;
    }

    public String getDisambiguationSource() {
        return disambiguationSource;
    }

    public Affiliation disambiguationSource(String disambiguationSource) {
        this.disambiguationSource = disambiguationSource;
        return this;
    }

    public void setDisambiguationSource(String disambiguationSource) {
        this.disambiguationSource = disambiguationSource;
    }

    public String getExternalId() {
        return externalId;
    }

    public Affiliation externalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getExternalIdType() {
        return externalIdType;
    }

    public Affiliation externalIdType(String externalIdType) {
        this.externalIdType = externalIdType;
        return this;
    }

    public void setExternalIdType(String externalIdType) {
        this.externalIdType = externalIdType;
    }

    public String getExternalIdUrl() {
        return externalIdUrl;
    }

    public Affiliation externalIdUrl(String externalIdUrl) {
        this.externalIdUrl = externalIdUrl;
        return this;
    }

    public void setExternalIdUrl(String externalIdUrl) {
        this.externalIdUrl = externalIdUrl;
    }

    public String getPutCode() {
        return putCode;
    }

    public Affiliation putCode(String putCode) {
        this.putCode = putCode;
        return this;
    }

    public void setPutCode(String putCode) {
        this.putCode = putCode;
    }

    public Instant getCreated() {
        return created;
    }

    public Affiliation created(Instant created) {
        this.created = created;
        return this;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public Instant getModified() {
        return modified;
    }

    public Affiliation modified(Instant modified) {
        this.modified = modified;
        return this;
    }

    public void setModified(Instant modified) {
        this.modified = modified;
    }

    public String getAdminId() {
        return adminId;
    }

    public Affiliation adminId(String adminId) {
        this.adminId = adminId;
        return this;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters
    // and setters here, do not remove

    public String getOrcidError() {
        return orcidError;
    }

    public void setOrcidError(String orcidError) {
        this.orcidError = orcidError;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public Instant getDeletedFromORCID() {
        return deletedFromORCID;
    }

    public void setDeletedFromORCID(Instant deletedFromORCID) {
        this.deletedFromORCID = deletedFromORCID;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Affiliation affiliation = (Affiliation) o;
        if (affiliation.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), affiliation.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Affiliation{" + "id=" + getId() + ", email='" + getEmail() + "'" + ", affiliationSection='" + getAffiliationSection() + "'" + ", departmentName='"
                + getDepartmentName() + "'" + ", roleTitle='" + getRoleTitle() + "'" + ", startYear='" + getStartYear() + "'" + ", startMonth='" + getStartMonth() + "'"
                + ", startDay='" + getStartDay() + "'" + ", endYear='" + getEndYear() + "'" + ", endMonth='" + getEndMonth() + "'" + ", endDay='" + getEndDay() + "'"
                + ", orgName='" + getOrgName() + "'" + ", orgCountry='" + getOrgCountry() + "'" + ", orgCity='" + getOrgCity() + "'" + ", orgRegion='" + getOrgRegion()
                + "'" + ", disambiguatedOrgId='" + getDisambiguatedOrgId() + "'" + ", disambiguationSource='" + getDisambiguationSource() + "'" + ", externalId='"
                + getExternalId() + "'" + ", externalIdType='" + getExternalIdType() + "'" + ", externalIdUrl='" + getExternalIdUrl() + "'" + ", putCode='" + getPutCode()
                + "'" + ", created='" + getCreated() + "'" + ", modified='" + getModified() + "'" + ", adminId='" + getAdminId() + "'" + ", orcidError='"
                + getOrcidError() + "'" + ", updated='" + isUpdated() + "'" + ", deletedFromORCID='" + getDeletedFromORCID() + "'" + "}";
    }
}
