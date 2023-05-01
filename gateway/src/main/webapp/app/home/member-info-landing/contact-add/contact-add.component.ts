import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EMAIL_REGEXP, URL_REGEXP } from 'app/app.constants';
import { AccountService } from 'app/core';
import { MSMemberService } from 'app/entities/member';
import { SFMemberContact } from 'app/shared/model/salesforce-member-contact.model';
import { ISFMemberData, SFMemberData } from 'app/shared/model/salesforce-member-data.model';
import { SFPublicDetails } from 'app/shared/model/salesforce-public-details.model';
import { IMSUser } from 'app/shared/model/user.model';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-contact-add',
  templateUrl: './contact-add.component.html',
  styleUrls: ['./contact-add.component.scss']
})
export class ContactAddComponent implements OnInit, OnDestroy {
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

  editForm: FormGroup;

  rolesData = [
    { id: 1, selected: false, name: 'Main relationship contact' },
    { id: 2, selected: false, name: 'Voting contact' },
    { id: 3, selected: false, name: 'Technical contact' },
    { id: 4, selected: false, name: 'Invoice contact' },
    { id: 5, selected: false, name: 'Comms contact' },
    { id: 6, selected: false, name: 'Product contact' }
  ];

  constructor(
    private accountService: AccountService,
    private memberService: MSMemberService,
    private fb: FormBuilder,
    protected activatedRoute: ActivatedRoute,
    private router: Router
  ) {
    this.editForm = this.fb.group({
      firstName: [null, [Validators.required, Validators.maxLength(255)]],
      lastName: [null, [Validators.maxLength(5000)]],
      phone: [null, [Validators.pattern(URL_REGEXP), Validators.maxLength(255)]],
      email: [null, [Validators.pattern(EMAIL_REGEXP), Validators.maxLength(80)]],
      title: [null, [Validators.pattern(EMAIL_REGEXP), Validators.maxLength(80)]],
      roles: this.fb.array(
        this.rolesData.map(val => this.fb.group({ id: val.id, selected: val.selected, name: val.name }, [[this.validateRoles]]))
      )
    });
  }

  ngOnInit() {
    this.memberDataSubscription = this.accountService.memberData.subscribe(data => {
      this.memberData = data;
      this.validateUrl();
      this.updateForm(data);
    });
    console.log(this.roles.controls);

    this.editForm.valueChanges.subscribe(() => {
      if (this.editForm.status === 'VALID') {
        this.editForm.controls.roles.setErrors(this.validateRoles(this.roles));
        this.invalidForm = false;
      }
    });
  }

  validateRoles(rolesArray: FormArray): ValidationErrors | null {
    console.log(rolesArray);
    const lastFiveRoles = rolesArray.controls.slice(-5);
    const selectedRoles = lastFiveRoles.filter(control => control.value.selected);
    if (selectedRoles.length < 1) {
      return { atLeastOneRoleSelected: true };
    }
    return null;
  }

  ngOnDestroy(): void {
    this.memberDataSubscription.unsubscribe();
  }

  validateUrl() {
    if (!/(http(s?)):\/\//i.test(this.memberData.website)) {
      this.memberData.website = 'http://' + this.memberData.website;
    }
  }

  get roles(): FormArray {
    return this.editForm.get('roles') as FormArray;
  }

  updateForm(data: SFMemberData) {
    if (data && data.id) {
      /* this.editForm.patchValue({
        name: data.publicDisplayName,
        description: data.publicDisplayDescriptionHtml,
        website: data.website,
        email: data.publicDisplayEmail
      }); */
    }
  }

  filterCRFID(id) {
    return id.replace(/^.*dx.doi.org\//g, '');
  }

  createDetailsFromForm(): SFMemberContact {
    return {
      ...new SFMemberContact(),
      name: this.editForm.get(['firstName']).value,
      lastName: this.editForm.get(['lastName']).value,
      contactEmail: this.editForm.get(['email']).value,
      phone: this.editForm.get(['phone']).value,
      memberOrgRole: this.editForm.get(['roles']).value
    };
  }

  save() {
    if (this.editForm.status === 'INVALID') {
      this.invalidForm = true;
    } else {
      this.invalidForm = false;
      this.isSaving = true;
      /* const details = this.createDetailsFromForm();
      this.memberService.updatePublicDetails(details).subscribe(
        res => {
          this.accountService.updatePublicDetails(details);
          this.onSaveSuccess();
        },
        err => {
          console.error(err);
          this.onSaveError();
        }
      ); */
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
