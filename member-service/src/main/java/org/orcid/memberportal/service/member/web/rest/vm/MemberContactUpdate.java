package org.orcid.memberportal.service.member.web.rest.vm;

import org.orcid.memberportal.service.member.client.model.MemberContact;

public class MemberContactUpdate {

    private MemberContact old;

    private MemberContact to;

    public MemberContact getOld() {
        return old;
    }

    public void setOld(MemberContact old) {
        this.old = old;
    }

    public MemberContact getTo() {
        return to;
    }

    public void setTo(MemberContact to) {
        this.to = to;
    }
}
