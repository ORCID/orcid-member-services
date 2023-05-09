import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EMAIL_REGEXP, URL_REGEXP } from 'app/app.constants';
import { AccountService } from 'app/core';
import { MSMemberService } from 'app/entities/member';
import {
  ISFMemberContact,
  ISFMemberContactUpdate,
  SFMemberContact,
  SFMemberContactUpdate
} from 'app/shared/model/salesforce-member-contact.model';
import { ISFMemberData, SFMemberData } from 'app/shared/model/salesforce-member-data.model';
import { SFPublicDetails } from 'app/shared/model/salesforce-public-details.model';
import { IMSUser } from 'app/shared/model/user.model';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-contact-update',
  templateUrl: './contact-update.component.html',
  styleUrls: ['./contact-update.component.scss']
})
export class ContactUpdateComponent implements OnInit, OnDestroy {
  memberDataSubscription: Subscription;
  account: IMSUser;
  memberData: ISFMemberData;
  contact: ISFMemberContact;
  objectKeys = Object.keys;
  // TODO move to constants
  MEMBER_LIST_URL: string = 'https://orcid.org/members';
  isSaving: boolean;
  invalidForm: boolean;
  routeData: any;
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
      firstName: [null, [Validators.required, Validators.maxLength(40)]],
      lastName: [null, [Validators.required, Validators.maxLength(40)]],
      phone: [null, [Validators.maxLength(255)]],
      email: [null, [Validators.required, Validators.pattern(EMAIL_REGEXP), Validators.maxLength(80)]],
      title: [null, [Validators.maxLength(80)]],
      roles: this.fb.array(
        // add interface
        this.rolesData.map(val => this.fb.group({ id: val.id, selected: val.selected, name: val.name })),
        [this.validateContactRoles]
      )
    });
    this.routeData = this.activatedRoute.data.subscribe(data => {
      console.log(data);

      this.contact = data.contact;
      console.log(this.contact);
      this.updateForm(data.contact);
    });
  }

  ngOnInit() {
    this.memberDataSubscription = this.memberService.memberData.subscribe(data => {
      this.memberData = data;
    });
    console.log(this.editForm);
    this.editForm.valueChanges.subscribe(() => {
      console.log(this.editForm);
      console.log(this.roles);

      if (this.editForm.status === 'VALID') {
        this.invalidForm = false;
      }
    });
  }

  validateContactRoles(rolesArray: FormArray): ValidationErrors | null {
    console.log(rolesArray);

    const lastFiveRoles = rolesArray.controls.slice(-5);
    const selectedRoles = lastFiveRoles.filter(control => control.value.selected);
    console.log(selectedRoles);

    if (selectedRoles.length < 1) {
      console.log('haha');
      // rolesArray.updateValueAndValidity();
      return { oneRoleSelected: true };
    }
    //rolesArray.updateValueAndValidity();
    return null;
  }

  updateForm(contact: ISFMemberContact) {
    this.editForm.patchValue({
      firstName: contact.name,
      lastName: contact.name,
      phone: contact.phone,
      title: contact.title,
      email: contact.contactEmail,
      roles: contact.memberOrgRole.map(role => {
        return { selected: true, name: role };
      })
    });
  }

  ngOnDestroy(): void {
    this.memberDataSubscription.unsubscribe();
  }

  get roles(): FormArray {
    return this.editForm.get('roles') as FormArray;
  }

  createContactFromForm(): ISFMemberContactUpdate {
    return {
      ...new SFMemberContact(),
      contactNewFirstName: this.editForm.get('firstName').value,
      contactNewLastName: this.editForm.get('lastName').value,
      contactEmail: this.editForm.get('email').value,
      contactNewPhone: this.editForm.get('phone').value,
      contactNewJobTitle: this.editForm.get('title').value,
      contactNewRoles: this.editForm
        .get('roles')
        // add interface
        .value.filter(role => role.selected)
        .map(role => role.name)
    };
  }

  save() {
    if (this.editForm.status === 'INVALID') {
      this.invalidForm = true;
    } else {
      this.invalidForm = false;
      this.isSaving = true;
      const contact = this.createContactFromForm();
      this.memberService.updateContact(contact).subscribe(
        res => {
          console.log(res);

          // TODO: update this.memberData object with the relevant changes
          //
          this.onSaveSuccess();
        },
        err => {
          console.error(err);
          this.onSaveError();
        }
      );
    }
  }

  delete() {
    this.isSaving = true;
    this.memberService.updateContact(this.contact).subscribe(
      res => {
        console.log(res);

        // update this.memberData object with the relevant changes
        //
        this.onSaveSuccess();
      },
      err => {
        console.error(err);
        this.onSaveError();
      }
    );
  }

  onSaveSuccess() {
    this.isSaving = false;
    this.router.navigate(['']);
  }

  onSaveError() {
    this.isSaving = false;
  }
}
