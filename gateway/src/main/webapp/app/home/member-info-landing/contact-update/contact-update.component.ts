import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EMAIL_REGEXP } from 'app/app.constants';
import { MSMemberService } from 'app/entities/member';
import { AlertService, ContactUpdateConfirmationAlert } from 'app/shared';
import {
  ISFMemberContact,
  ISFMemberContactUpdate,
  SFMemberContact,
  SFMemberContactUpdate
} from 'app/shared/model/salesforce-member-contact.model';
import { ISFMemberData } from 'app/shared/model/salesforce-member-data.model';
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
  isSaving: boolean;
  invalidForm: boolean;
  routeData: any;
  editForm: FormGroup;
  contactId: string;

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
    protected activatedRoute: ActivatedRoute
  ) {}

  ngOnInit() {
    this.activatedRoute.params.subscribe(params => {
      if (params['id']) {
        this.contactId = params['id'];
      }
    });
    this.editForm = this.fb.group({
      name: [null, [Validators.required, Validators.maxLength(80)]],
      phone: [null, [Validators.maxLength(40)]],
      email: [null, [Validators.required, Validators.pattern(EMAIL_REGEXP), Validators.maxLength(80)]],
      title: [null, [Validators.maxLength(128)]],
      roles: this.fb.array(
        // add interface
        this.rolesData.map(val => this.fb.group({ id: val.id, selected: val.selected, name: val.name })),
        [this.validateContactRoles]
      )
    });

    this.memberDataSubscription = this.memberService.memberData.subscribe(data => {
      this.memberData = data;
      if (data.contacts && this.contactId) {
        this.contact = Object.values(data.contacts).find(contact => contact.contactEmail == this.contactId);
        this.updateForm(this.contact);
      }
    });
    this.editForm.valueChanges.subscribe(() => {
      if (this.editForm.status === 'VALID') {
        this.invalidForm = false;
      }
    });
  }

  validateContactRoles(rolesArray: FormArray): ValidationErrors | null {
    const selectedRoles = rolesArray.controls.filter(control => control.value.selected);
    if (selectedRoles.length < 1) {
      return { oneRoleSelected: true };
    }
    return null;
  }

  updateForm(contact: ISFMemberContact) {
    this.editForm.patchValue({
      name: contact.name,
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
      contactNewName: this.editForm.get('name').value,
      contactNewEmail: this.editForm.get('email').value,
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
      Object.keys(this.editForm.controls).forEach(key => {
        this.editForm.get(key).markAsDirty();
      });
      this.editForm.markAllAsTouched();
    } else {
      this.invalidForm = false;
      this.isSaving = true;
      const contact = this.createContactFromForm();
      contact.contactMember = this.memberData.name;
      if (this.contactId) {
        contact.contactEmail = this.contact.contactEmail;
        contact.contactName = this.contact.name;
      }

      this.memberService.updateContact(contact, this.memberData.id).subscribe(
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

  delete() {
    this.isSaving = true;
    const contact = new SFMemberContactUpdate();
    if (this.contactId) {
      contact.contactEmail = this.contact.contactEmail;
      contact.contactName = this.contact.name;
      contact.contactMember = this.memberData.name;
    }
    this.memberService.updateContact(contact, this.memberData.id).subscribe(
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

  onSaveSuccess() {
    this.isSaving = false;
    this.alertService.showHomepageLightboxModal({ alertComponent: ContactUpdateConfirmationAlert });
    this.router.navigate(['']);
  }

  onSaveError() {
    this.isSaving = false;
  }
}
