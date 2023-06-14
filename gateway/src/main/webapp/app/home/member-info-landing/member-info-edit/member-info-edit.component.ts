import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, Renderer2 } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EMAIL_REGEXP, URL_REGEXP } from 'app/app.constants';
import { AccountService } from 'app/core';
import { MSMemberService } from 'app/entities/member';
import { COUNTRIES } from 'app/shared/constants/orcid-api.constants';
import { ISFAddress } from 'app/shared/model/salesforce-address.model';
import { ISFMemberData, SFMemberData } from 'app/shared/model/salesforce-member-data.model';
import { ISFMemberUpdate, SFMemberUpdate } from 'app/shared/model/salesforce-member-update.model';
import { IMSUser } from 'app/shared/model/user.model';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-member-info-edit',
  templateUrl: './member-info-edit.component.html',
  styleUrls: ['./member-info-edit.component.scss']
})
export class MemberInfoEditComponent implements OnInit, OnDestroy {
  COUNTRIES = COUNTRIES;
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
    orgName: [null, [Validators.required, Validators.maxLength(41)]],
    street: [null, [Validators.maxLength(255)]],
    city: [null, [Validators.maxLength(40)]],
    state: [null, [Validators.maxLength(80)]],
    country: [null, [Validators.required]],
    postcode: [null, [Validators.maxLength(20)]],
    trademarkLicense: [null, [Validators.required]],
    publicName: [null, [Validators.required, Validators.maxLength(255)]],
    description: [null, [Validators.maxLength(5000)]],
    website: [null, [Validators.pattern(URL_REGEXP), Validators.maxLength(255)]],
    email: [null, [Validators.pattern(EMAIL_REGEXP), Validators.maxLength(80)]]
  });

  constructor(
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
        orgName: data.name,
        street: data.billingAddress.street,
        city: data.billingAddress.city,
        state: data.billingAddress.state,
        country: data.billingAddress.country,
        postcode: data.billingAddress.postalCode,
        trademarkLicense: data.trademarkLicense,
        publicName: data.publicDisplayName,
        description: data.publicDisplayDescriptionHtml,
        website: data.website,
        email: data.publicDisplayEmail
      });
    }
  }

  filterCRFID(id) {
    return id.replace(/^.*dx.doi.org\//g, '');
  }

  createDetailsFromForm(): ISFMemberUpdate {
    const address: ISFAddress = {
      street: this.editForm.get(['street']).value,
      city: this.editForm.get(['city']).value,
      state: this.editForm.get(['state']).value,
      country: this.editForm.get(['country']).value,
      postalCode: this.editForm.get(['postcode']).value
    };
    return {
      ...new SFMemberUpdate(),
      orgName: this.editForm.get(['orgName']).value,
      billingAddress: address,
      trademarkLicense: this.editForm.get(['trademarkLicense']).value,
      publicName: this.editForm.get(['publicName']).value,
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
      this.memberService.updateMemberDetails(details).subscribe(
        res => {
          this.memberService.memberData.next({
            ...this.memberService.memberData.value,
            publicDisplayDescriptionHtml: details.description,
            publicDisplayName: details.publicName,
            name: details.orgName,
            billingAddress: details.billingAddress,
            trademarkLicense: details.trademarkLicense,
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
