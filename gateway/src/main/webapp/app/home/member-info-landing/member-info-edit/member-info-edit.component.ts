import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EMAIL_REGEXP, URL_REGEXP } from 'app/app.constants';
import { AccountService } from 'app/core';
import { MSMemberService } from 'app/entities/member';
import { ISFMemberData, SFMemberData } from 'app/shared/model/salesforce-member-data.model';
import { SFPublicDetails } from 'app/shared/model/salesforce-public-details.model';
import { IMSUser } from 'app/shared/model/user.model';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-member-info-edit',
  templateUrl: './member-info-edit.component.html',
  styleUrls: ['./member-info-edit.component.scss']
})
export class MemberInfoEditComponent implements OnInit, OnDestroy {
  memberDataSubscription: Subscription;
  account: IMSUser;
  memberData: ISFMemberData;
  objectKeys = Object.keys;
  // TODO move to constants
  MEMBER_LIST_URL: string = 'https://orcid.org/members';
  isSaving: boolean;
  invalidForm: boolean;
  quillConfig = {
    toolbar: [['bold', 'italic'], [{ list: 'ordered' }, { list: 'bullet' }], ['link']]
  };
  quillStyles = {
    fontFamily: 'inherit',
    fontSize: '14px',
    letterSpacing: '0.25px',
    marginRight: '0'
  };

  editForm = this.fb.group({
    name: [null, [Validators.required, Validators.maxLength(255)]],
    description: [null, [Validators.maxLength(5000)]],
    website: [null, [Validators.pattern(URL_REGEXP), Validators.maxLength(255)]],
    email: [null, [Validators.pattern(EMAIL_REGEXP), Validators.maxLength(80)]]
  });

  constructor(
    private accountService: AccountService,
    private memberService: MSMemberService,
    private fb: FormBuilder,
    protected activatedRoute: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    this.memberDataSubscription = this.memberService.memberData.subscribe(data => {
      this.memberData = data;
      this.validateUrl();
      this.updateForm(data);
    });
    this.editForm.valueChanges.subscribe(() => {
      if (this.editForm.status === 'VALID') {
        this.invalidForm = false;
      }
    });
  }

  ngOnDestroy(): void {
    this.memberDataSubscription.unsubscribe();
  }

  validateUrl() {
    if (!/(http(s?)):\/\//i.test(this.memberData.website)) {
      this.memberData.website = 'http://' + this.memberData.website;
    }
  }

  updateForm(data: SFMemberData) {
    if (data && data.id) {
      this.editForm.patchValue({
        name: data.publicDisplayName,
        description: data.publicDisplayDescriptionHtml,
        website: data.website,
        email: data.publicDisplayEmail
      });
    }
  }

  filterCRFID(id) {
    return id.replace(/^.*dx.doi.org\//g, '');
  }

  createDetailsFromForm(): SFPublicDetails {
    return {
      ...new SFPublicDetails(),
      name: this.editForm.get(['name']).value,
      description: this.editForm.get(['description']).value,
      website: this.editForm.get(['website']).value,
      email: this.editForm.get(['email']).value
    };
  }

  save() {
    if (this.editForm.status === 'INVALID') {
      this.invalidForm = true;
    } else {
      this.invalidForm = false;
      this.isSaving = true;
      const details = this.createDetailsFromForm();
      this.memberService.updatePublicDetails(details).subscribe(
        res => {
          this.memberService.memberData.next({
            ...this.memberService.memberData.value,
            publicDisplayDescriptionHtml: details.description,
            publicDisplayName: details.name,
            publicDisplayEmail: details.email,
            website: details.website
          });
          this.onSaveSuccess();
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
    this.router.navigate(['']);
  }

  onSaveError() {
    this.isSaving = false;
  }
}
