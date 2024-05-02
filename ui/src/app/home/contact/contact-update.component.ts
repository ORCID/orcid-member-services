import { Component, OnDestroy, OnInit } from '@angular/core'
import { ISFMemberData } from '../../member/model/salesforce-member-data.model'
import {
  ISFMemberContact,
  ISFMemberContactUpdate,
  SFMemberContactRole,
  SFMemberContactUpdate,
} from '../../member/model/salesforce-member-contact.model'
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormGroup,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms'
import { MemberService } from '../../member/service/member.service'
import { AccountService } from '../../account/service/account.service'
import { AlertService } from '../../shared/service/alert.service'
import { ActivatedRoute, Router } from '@angular/router'
import { AlertType, EMAIL_REGEXP } from '../../app.constants'
import { EMPTY, Subject, combineLatest, switchMap, takeUntil } from 'rxjs'
import { IAccount } from '../../account/model/account.model'
import { faTrashAlt } from '@fortawesome/free-solid-svg-icons'

@Component({
  selector: 'app-contact-update',
  templateUrl: './contact-update.component.html',
  styleUrls: ['./contact-update.component.scss'],
})
export class ContactUpdateComponent implements OnInit, OnDestroy {
  account: IAccount | undefined | null
  memberData: ISFMemberData | undefined | null
  contact: ISFMemberContact | undefined
  isSaving = false
  invalidForm = false
  routeData: any
  contactId: string | undefined
  managedMember: string | undefined
  destroy$ = new Subject()
  faTrashAlt = faTrashAlt

  rolesData = [
    new SFMemberContactRole(1, false, 'Main relationship contact'),
    new SFMemberContactRole(2, false, 'Voting contact'),
    new SFMemberContactRole(3, false, 'Technical contact'),
    new SFMemberContactRole(4, false, 'Invoice contact'),
    new SFMemberContactRole(5, false, 'Comms contact'),
    new SFMemberContactRole(6, false, 'Product contact'),
  ]

  validateContactRoles: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
    const rolesArray = control as FormArray
    const selectedRoles = rolesArray.controls.filter((control) => control.value.selected)
    if (selectedRoles.length < 1) {
      return { oneRoleSelected: true }
    }
    return null
  }

  editForm: FormGroup = this.fb.group({
    name: [null, [Validators.required, Validators.maxLength(80)]],
    phone: [null, [Validators.maxLength(40)]],
    email: [null, [Validators.required, Validators.pattern(EMAIL_REGEXP), Validators.maxLength(80)]],
    title: [null, [Validators.maxLength(128)]],
    roles: this.fb.array(
      this.rolesData.map((val: SFMemberContactRole) =>
        this.fb.group({ id: val.id, selected: val.selected, name: val.name })
      ),
      [this.validateContactRoles]
    ),
  })

  constructor(
    private memberService: MemberService,
    private accountService: AccountService,
    private fb: FormBuilder,
    private alertService: AlertService,
    private router: Router,
    protected activatedRoute: ActivatedRoute
  ) {}

  ngOnInit() {
    this.activatedRoute.params.subscribe((params) => {
      if (params['contactId']) {
        this.contactId = params['contactId']
      }
      if (params['id']) {
        this.managedMember = params['id']
        this.memberService.setManagedMember(params['id'])
      }
    })

    combineLatest([this.activatedRoute.params, this.accountService.getAccountData()])
      .pipe(
        switchMap(([params, account]) => {
          if (params['contactId']) {
            this.contactId = params['contactId']
          }
          if (params['id']) {
            this.managedMember = params['id']
          }
          if (account) {
            this.account = account
            if (this.managedMember) {
              this.memberService.setManagedMember(params['id'])
              return this.memberService.getMemberData(this.managedMember)
            } else {
              return this.memberService.getMemberData(this.account?.salesforceId)
            }
          } else {
            return EMPTY
          }
        }),
        takeUntil(this.destroy$)
      )
      .subscribe((data) => {
        this.memberData = data
        if (data?.contacts && this.contactId) {
          this.contact = Object.values(data.contacts).find((contact) => contact.contactEmail == this.contactId)
          if (this.contact) {
            this.updateForm(this.contact)
          }
        }
      })

    this.editForm.valueChanges.subscribe(() => {
      if (this.editForm!.status === 'VALID') {
        this.invalidForm = false
      }
    })
  }

  updateForm(contact: ISFMemberContact) {
    this.editForm!.patchValue({
      name: contact.name,
      phone: contact.phone,
      title: contact.title,
      email: contact.contactEmail,
      roles: this.editForm!.controls['roles'].value.map((role: SFMemberContactRole) => {
        if (contact.memberOrgRole?.includes(role.name)) {
          return { ...role, selected: true }
        } else {
          return role
        }
      }),
    })
  }

  ngOnDestroy(): void {
    this.destroy$.next(true)
    this.destroy$.complete()
  }

  get roles(): FormArray {
    return this.editForm!.get('roles') as FormArray
  }

  createContactFromForm(): ISFMemberContactUpdate {
    return {
      ...new SFMemberContactUpdate(),
      contactNewName: this.editForm!.get('name')?.value,
      contactNewEmail: this.editForm!.get('email')?.value,
      contactNewPhone: this.editForm!.get('phone')?.value,
      contactNewJobTitle: this.editForm!.get('title')?.value,
      contactNewRoles: this.editForm!.get('roles')
        ?.value.filter((role: SFMemberContactRole) => role.selected)
        .map((role: SFMemberContactRole) => role.name),
    }
  }

  save() {
    if (this.editForm!.status === 'INVALID') {
      this.invalidForm = true
      Object.keys(this.editForm!.controls).forEach((key) => {
        this.editForm!.get(key)?.markAsDirty()
      })
      this.editForm!.markAllAsTouched()
    } else {
      console.log('form valid')

      this.invalidForm = false
      this.isSaving = true
      const contact = this.createContactFromForm()
      contact.contactMember = this.memberData!.name
      if (this.contactId) {
        contact.contactEmail = this.contact!.contactEmail
        contact.contactName = this.contact!.name
      }

      this.memberService.updateContact(contact, this.memberData!.id!).subscribe({
        next: (res) => {
          if (res) {
            this.onSaveSuccess()
          } else {
            console.error(res)
            this.onSaveError()
          }
        },
        error: (err) => {
          console.error(err)
          this.onSaveError()
        },
      })
    }
  }

  delete() {
    this.isSaving = true
    const contact = new SFMemberContactUpdate()
    if (this.contactId) {
      contact.contactEmail = this.contact!.contactEmail
      contact.contactName = this.contact!.name
      contact.contactMember = this.memberData!.name
    }
    this.memberService.updateContact(contact, this.memberData!.id!).subscribe({
      next: (res) => {
        if (res) {
          this.onSaveSuccess()
        } else {
          console.error(res)
          this.onSaveError()
        }
      },
      error: (err) => {
        console.error(err)
        this.onSaveError()
      },
    })
  }

  onSaveSuccess() {
    this.isSaving = false
    this.alertService.broadcast(AlertType.CONTACT_UPDATED)
    this.router.navigate([''])
  }

  onSaveError() {
    this.isSaving = false
  }
}
