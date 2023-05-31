import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EMAIL_REGEXP } from 'app/app.constants';
import { AccountService } from 'app/core';
import { MSMemberService } from 'app/entities/member';
import { AddConsortiumMemberConfirmationComponent, AlertService } from 'app/shared';
import { COUNTRIES } from 'app/shared/constants/orcid-api.constants';

import { ISFMemberData } from 'app/shared/model/salesforce-member-data.model';
import { ISFNewConsortiumMember, SFNewConsortiumMember } from 'app/shared/model/salesforce-new-consortium-member.model';
import { IMSUser } from 'app/shared/model/user.model';
import { DateUtilService } from 'app/shared/util/date-util.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-add-consortium-member',
  templateUrl: './add-consortium-member.component.html',
  styleUrls: ['./add-consortium-member.scss']
})
export class AddConsortiumMemberComponent implements OnInit, OnDestroy {
  COUNTRIES = COUNTRIES;
  memberDataSubscription: Subscription;
  account: IMSUser;
  memberData: ISFMemberData;
  isSaving: boolean;
  invalidForm: boolean;
  routeData: any;
  editForm: FormGroup;
  currentMonth: number;
  currentYear: number;
  monthList: [number, string][];
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
    private accountService: AccountService,
    protected activatedRoute: ActivatedRoute
  ) {}

  ngOnInit() {
    this.accountService.identity().then(account => {
      this.account = account;
    });

    this.currentMonth = this.dateUtilService.getCurrentMonthNumber();
    this.currentYear = this.dateUtilService.getCurrentYear();
    this.monthList = this.dateUtilService.getMonthsList();
    this.yearList = this.dateUtilService.getFutureYearsIncludingCurrent(10);
    this.editForm = this.fb.group(
      {
        orgName: [null, [Validators.required, Validators.maxLength(41)]],
        emailDomain: [null, [Validators.maxLength(80)]],
        street: [null, [Validators.maxLength(40)]],
        city: [null, [Validators.maxLength(40)]],
        state: [null, [Validators.maxLength(40)]],
        orgCountry: [null, [Validators.maxLength(40)]],
        postcode: [null, [Validators.maxLength(40)]],
        trademarkLicense: [null, [Validators.required]],
        startMonth: [this.monthList[this.currentMonth - 1][0], [Validators.required]],
        startYear: [this.yearList[0], [Validators.required]],
        contactName: [null, [Validators.maxLength(40)]],
        contactJobTitle: [null, [Validators.maxLength(128)]],
        contactEmail: [null, [Validators.pattern(EMAIL_REGEXP), Validators.maxLength(40)]],
        contactPhone: [null, [Validators.maxLength(40)]]
      },
      { validator: this.dateValidator.bind(this) }
    );

    this.memberDataSubscription = this.memberService.memberData.subscribe(data => {
      this.memberData = data;
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

  createNewConsortiumMemberFromForm(): ISFNewConsortiumMember {
    return new SFNewConsortiumMember(
      this.editForm.get('orgName').value,
      this.editForm.get('trademarkLicense').value,
      this.editForm.get('startMonth').value,
      this.editForm.get('startYear').value,
      this.editForm.get('emailDomain').value,
      this.editForm.get('street').value,
      this.editForm.get('city').value,
      this.editForm.get('state').value,
      this.editForm.get('orgCountry').value,
      this.editForm.get('postcode').value,
      this.editForm.get('contactName').value,
      this.editForm.get('contactJobTitle').value,
      this.editForm.get('contactEmail').value,
      this.editForm.get('contactPhone').value
    );
  }

  save() {
    if (this.editForm.status === 'INVALID') {
      this.editForm.markAllAsTouched();
      this.invalidForm = true;
    } else {
      this.invalidForm = false;
      this.isSaving = true;
      const newConsortiumMember = this.createNewConsortiumMemberFromForm();

      this.memberService.addConsortiumMember(newConsortiumMember).subscribe(
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
    this.alertService.activeAlert.next(AddConsortiumMemberConfirmationComponent);
    this.router.navigate(['']);
  }

  onSaveError() {
    this.isSaving = false;
  }
}
