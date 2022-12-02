import { Component, Input, OnInit } from '@angular/core';
import { AccountService } from 'app/core';
import { ISFMemberData } from 'app/shared/model/salesforce-member-data.model';

@Component({
  selector: 'app-member-info-edit',
  templateUrl: './member-info-edit.component.html',
  styleUrls: ['./member-info-edit.component.scss']
})
export class MemberInfoEditComponent implements OnInit {
  constructor(private accountService: AccountService) {}
  @Input() memberData: ISFMemberData;

  ngOnInit() {
    this.accountService.getCurrentMemberData().then(res => {
      this.memberData = res.value;
    });
  }
}
