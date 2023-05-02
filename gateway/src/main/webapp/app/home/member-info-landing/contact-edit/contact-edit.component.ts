import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EMAIL_REGEXP } from 'app/app.constants';
import { AccountService } from 'app/core';
import { MSMemberService } from 'app/entities/member';
import { ISFMemberContact, SFMemberContact } from 'app/shared/model/salesforce-member-contact.model';
import { ISFMemberData } from 'app/shared/model/salesforce-member-data.model';
import { IMSUser } from 'app/shared/model/user.model';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-contact-edit',
  templateUrl: './contact-edit.component.html',
  styleUrls: ['./contact-edit.component.scss']
})
export class ContactEditComponent implements OnInit, OnDestroy {
  memberDataSubscription: Subscription;
  account: IMSUser;
  memberData: ISFMemberData;
  contact: ISFMemberContact;
  objectKeys = Object.keys;
  // TODO move to constants
  MEMBER_LIST_URL: string = 'https://orcid.org/members';
  isSaving: boolean;
  invalidForm: boolean;

  editForm: FormGroup;
  routeData: any;

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
        this.rolesData.map(val => this.fb.group({ id: val.id, selected: val.selected, name: val.name }, [[this.validateContactRoles]]))
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
    this.editForm.valueChanges.subscribe(() => {
      if (this.editForm.status === 'VALID') {
        this.editForm.controls.roles.setErrors(this.validateContactRoles(this.roles));
        this.invalidForm = false;
      }
    });
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

  validateContactRoles(rolesArray: FormArray): ValidationErrors | null {
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

  get roles(): FormArray {
    return this.editForm.get('roles') as FormArray;
  }

  createContactFromForm(): SFMemberContact {
    return {
      ...new SFMemberContact(),
      name: this.editForm.get('firstName').value + ' ' + this.editForm.get('lastName').value,
      contactEmail: this.editForm.get('email').value,
      phone: this.editForm.get('phone').value,
      title: this.editForm.get('title').value,
      memberOrgRole: this.editForm
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
  }

  delete() {
    this.isSaving = true;
    this.memberService.removeContact(this.contact.contactEmail).subscribe(
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
