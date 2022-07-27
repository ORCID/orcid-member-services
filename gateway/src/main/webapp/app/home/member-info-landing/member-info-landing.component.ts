import { Component, Input } from '@angular/core';
import { ISFMemberData } from 'app/shared/model/salesforce.member.data.model';

@Component({
  selector: 'app-member-info-landing',
  templateUrl: './member-info-landing.component.html',
  styleUrls: ['member-info-landing.component.scss']
})
export class MemberInfoLandingComponent {
  @Input() memberData: ISFMemberData;

  isActive() {
    if (this.memberData && new Date(this.memberData.membershipEndDateString) > new Date()) return true;
    else return false;
  }
}
