import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MSMemberService } from 'app/entities/member';
import { AlertService, RemoveConsortiumMemberConfirmationComponent } from 'app/shared';
import { COUNTRIES } from 'app/shared/constants/orcid-api.constants';

import { ISFConsortiumMemberData, ISFMemberData, SFConsortiumMemberData } from 'app/shared/model/salesforce-member-data.model';
import { IMSUser } from 'app/shared/model/user.model';
import { DateUtilService } from 'app/shared/util/date-util.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-remove-consortium-member',
  templateUrl: './remove-consortium-member.component.html',
  styleUrls: ['./remove-consortium-member.scss']
})
export class RemoveConsortiumMemberComponent implements OnInit, OnDestroy {
  COUNTRIES = COUNTRIES;
  memberDataSubscription: Subscription;
  account: IMSUser;
  memberData: ISFMemberData;
  consortiumMember: SFConsortiumMemberData;
  isSaving: boolean;
  invalidForm: boolean;
  routeData: any;
  editForm: FormGroup;
  consortiumMemberId: string;
  currentMonth: number;
  currentYear: number;
  monthList: string[];
  yearList: string[];

  rolesData = [
    { id: 1, selected: false, name: 'Main relationship contact' },
    { id: 2, selected: false, name: 'Voting contact' },
    { id: 3, selected: false, name: 'Technical contact' },
    { id: 4, selected: false, name: 'Invoice contact' },
    { id: 5, selected: false, name: 'Comms contact' },
    { id: 6, selected: false, name: 'Product contact' }
  ];

  constructor(
    private memberService: MSMemberService,
    private fb: FormBuilder,
    private alertService: AlertService,
    private router: Router,
    private dateUtilService: DateUtilService,
    protected activatedRoute: ActivatedRoute
  ) {}

  ngOnInit() {
    this.activatedRoute.params.subscribe(params => {
      if (params['id']) {
        this.consortiumMemberId = params['id'];
      }
    });

    this.currentMonth = this.dateUtilService.getCurrentMonthNumber();
    this.currentYear = this.dateUtilService.getCurrentYear();
    this.monthList = this.dateUtilService.getMonthsList();
    this.yearList = this.dateUtilService.getFutureYearsIncludingCurrent(10);
    this.editForm = this.fb.group(
      {
        orgName: [null, [Validators.required, Validators.maxLength(41)]],
        endMonth: [this.monthList[this.currentMonth - 1][0], [Validators.required]],
        endYear: [this.yearList[0], [Validators.required]]
      },
      { validator: this.dateValidator.bind(this) }
    );

    this.memberDataSubscription = this.memberService.memberData.subscribe(data => {
      this.memberData = data;
      this.consortiumMember = Object.values(data.consortiumMembers).find(
        (member: ISFConsortiumMemberData) => member.salesforceId === this.consortiumMemberId
      );
    });
    this.editForm.valueChanges.subscribe(() => {
      if (this.editForm.status === 'VALID') {
        this.invalidForm = false;
      }
    });
  }

  dateValidator(form: FormGroup) {
    const startMonth = form.controls['startMonth'].value;
    const startYear = form.controls['startYear'].value;

    if (startYear == this.currentYear && startMonth < this.currentMonth) {
      return { invalidDate: true };
    }
    return null;
  }

  ngOnDestroy(): void {
    this.memberDataSubscription.unsubscribe();
  }

  createConsortiumMemberFromForm(): ISFConsortiumMemberData {
    return {
      ...new SFConsortiumMemberData(),
      name: this.editForm.get('orgName').value,
      endMonth: this.editForm.get('endMonth').value,
      endYear: this.editForm.get('endYear').value
    };
  }

  save() {
    if (this.editForm.status === 'INVALID') {
      this.editForm.markAllAsTouched();
      this.invalidForm = true;
    } else {
      this.invalidForm = false;
      this.isSaving = true;
      const consortiumMember = this.createConsortiumMemberFromForm();

      this.memberService.removeConsortiumMember(consortiumMember).subscribe(
        res => {
          if (res) {
            this.onSaveSuccess();
          } else {
            console.error(res);
            this.onSaveError();
          }
        },
        err => {
          console.error(err);
          this.onSaveError();
        }
      );
    }
  }

  onSaveSuccess() {
    this.isSaving = false;
    this.alertService.activeAlert.next(RemoveConsortiumMemberConfirmationComponent);
    this.router.navigate(['']);
  }

  onSaveError() {
    this.isSaving = false;
  }
}
