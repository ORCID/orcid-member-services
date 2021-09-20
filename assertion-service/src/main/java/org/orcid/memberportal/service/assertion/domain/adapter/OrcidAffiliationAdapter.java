package org.orcid.memberportal.service.assertion.domain.adapter;

import org.apache.commons.lang3.StringUtils;
import org.orcid.jaxb.model.common.Iso3166Country;
import org.orcid.jaxb.model.v3.release.common.Day;
import org.orcid.jaxb.model.v3.release.common.DisambiguatedOrganization;
import org.orcid.jaxb.model.v3.release.common.FuzzyDate;
import org.orcid.jaxb.model.v3.release.common.Month;
import org.orcid.jaxb.model.v3.release.common.Organization;
import org.orcid.jaxb.model.v3.release.common.OrganizationAddress;
import org.orcid.jaxb.model.v3.release.common.Url;
import org.orcid.jaxb.model.v3.release.common.Year;
import org.orcid.jaxb.model.v3.release.record.Affiliation;
import org.orcid.jaxb.model.v3.release.record.Distinction;
import org.orcid.jaxb.model.v3.release.record.Education;
import org.orcid.jaxb.model.v3.release.record.Employment;
import org.orcid.jaxb.model.v3.release.record.ExternalID;
import org.orcid.jaxb.model.v3.release.record.ExternalIDs;
import org.orcid.jaxb.model.v3.release.record.InvitedPosition;
import org.orcid.jaxb.model.v3.release.record.Membership;
import org.orcid.jaxb.model.v3.release.record.Qualification;
import org.orcid.jaxb.model.v3.release.record.Service;
import org.orcid.memberportal.service.assertion.domain.Assertion;

public class OrcidAffiliationAdapter {

    public static Affiliation toOrcidAffiliation(Assertion assertion) {
        Affiliation orcidAffiliation;
        switch (assertion.getAffiliationSection()) {
        case DISTINCTION:
            orcidAffiliation = new Distinction();
            break;
        case EDUCATION:
            orcidAffiliation = new Education();
            break;
        case EMPLOYMENT:
            orcidAffiliation = new Employment();
            break;
        case INVITED_POSITION:
            orcidAffiliation = new InvitedPosition();
            break;
        case MEMBERSHIP:
            orcidAffiliation = new Membership();
            break;
        case QUALIFICATION:
            orcidAffiliation = new Qualification();
            break;
        case SERVICE:
            orcidAffiliation = new Service();
            break;
        default:
            throw new IllegalArgumentException("Invalid affiliation section found");
        }

        if (!StringUtils.isBlank(assertion.getPutCode())) {
            orcidAffiliation.setPutCode(Long.valueOf(assertion.getPutCode()));
        }
        orcidAffiliation.setDepartmentName(assertion.getDepartmentName());

        if (!StringUtils.isBlank(assertion.getEndYear())) {
            FuzzyDate endDate = new FuzzyDate();
            endDate.setYear(new Year(Integer.valueOf(assertion.getEndYear())));
            endDate.setMonth(StringUtils.isBlank(assertion.getEndMonth()) ? null : new Month(Integer.valueOf(assertion.getEndMonth())));
            endDate.setDay(StringUtils.isBlank(assertion.getEndDay()) ? null : new Day(Integer.valueOf(assertion.getEndDay())));
            orcidAffiliation.setEndDate(endDate);
        }

        if (!StringUtils.isBlank(assertion.getExternalId()) && !StringUtils.isBlank(assertion.getExternalIdType())) {
            ExternalIDs extIds = new ExternalIDs();
            ExternalID extId = new ExternalID();
            extId.setValue(assertion.getExternalId());
            extId.setType(assertion.getExternalIdType());
            extId.setUrl(StringUtils.isBlank(assertion.getExternalIdUrl()) ? null : new Url(assertion.getExternalIdUrl()));
            extIds.getExternalIdentifier().add(extId);
            orcidAffiliation.setExternalIdentifiers(extIds);
        }

        Organization org = new Organization();
        org.setName(assertion.getOrgName());
        OrganizationAddress address = new OrganizationAddress();
        address.setCity(assertion.getOrgCity());
        address.setCountry(Iso3166Country.valueOf(assertion.getOrgCountry()));
        address.setRegion(StringUtils.isBlank(assertion.getOrgRegion()) ? null : assertion.getOrgRegion());
        org.setAddress(address);

        DisambiguatedOrganization disambiguatedOrg = new DisambiguatedOrganization();
        disambiguatedOrg.setDisambiguatedOrganizationIdentifier(assertion.getDisambiguatedOrgId());
        disambiguatedOrg.setDisambiguationSource(assertion.getDisambiguationSource());
        org.setDisambiguatedOrganization(disambiguatedOrg);

        orcidAffiliation.setOrganization(org);

        orcidAffiliation.setRoleTitle(assertion.getRoleTitle());

        if (!StringUtils.isBlank(assertion.getStartYear())) {
            FuzzyDate startDate = new FuzzyDate();
            startDate.setYear(new Year(Integer.valueOf(assertion.getStartYear())));
            startDate.setMonth(StringUtils.isBlank(assertion.getStartMonth()) ? null : new Month(Integer.valueOf(assertion.getStartMonth())));
            startDate.setDay(StringUtils.isBlank(assertion.getStartDay()) ? null : new Day(Integer.valueOf(assertion.getStartDay())));
            orcidAffiliation.setStartDate(startDate);
        }

        orcidAffiliation.setUrl(StringUtils.isBlank(assertion.getUrl()) ? null : new Url(assertion.getUrl()));

        return orcidAffiliation;
    }
}
