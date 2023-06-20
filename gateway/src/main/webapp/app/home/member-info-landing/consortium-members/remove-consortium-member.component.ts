import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MSMemberService } from 'app/entities/member';
import { AlertService, RemoveConsortiumMemberConfirmationComponent } from 'app/shared';
import { COUNTRIES } from 'app/shared/constants/orcid-api.constants';

import { ISFConsortiumMemberData, ISFMemberData, SFConsortiumMemberData } from 'app/shared/model/salesforce-member-data.model';
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
  memberData: ISFMemberData;
  consortiumMember: SFConsortiumMemberData;
  isSaving: boolean;
  invalidForm: boolean;
  routeData: any;
  editForm: FormGroup;
  consortiumMemberId: string;
  currentMonth: number;
  currentYear: number;
  monthList: [number, string][];
  yearList: string[];

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
    this.yearList = this.dateUtilService.getFutureYearsIncludingCurrent(1);
    this.editForm = this.fb.group({
      terminationMonth: [null, [Validators.required]],
      terminationYear: [null, [Validators.required]]
    });

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

  ngOnDestroy(): void {
    if (this.memberDataSubscription) {
      this.memberDataSubscription.unsubscribe();
    }
  }

  createConsortiumMemberFromForm(): ISFConsortiumMemberData {
    return {
      ...new SFConsortiumMemberData(),
      terminationMonth: this.editForm.get('terminationMonth').value,
      terminationYear: this.editForm.get('terminationYear').value,
      orgName: this.consortiumMember.orgName
    };
  }

  save() {
    if (this.editForm.status === 'INVALID') {
      this.editForm.markAllAsTouched();
      Object.keys(this.editForm.controls).forEach(key => {
        this.editForm.get(key).markAsDirty();
      });
      this.invalidForm = true;
    } else {
      this.invalidForm = false;
      this.isSaving = true;
      const consortiumMember = this.createConsortiumMemberFromForm();

      this.memberService.removeConsortiumMember(consortiumMember).subscribe(
        res => {
          if (res) {
            this.onSaveSuccess(consortiumMember.orgName);
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

  onSaveSuccess(orgName: string) {
    this.isSaving = false;
    this.alertService.showHomepageLightboxModal({ alertComponent: RemoveConsortiumMemberConfirmationComponent, data: orgName });
    this.router.navigate(['']);
  }

  onSaveError() {
    this.isSaving = false;
  }
}
