import { ChangeDetectorRef, Component, OnInit } from '@angular/core'
import { HttpResponse } from '@angular/common/http'
import { FormBuilder, FormControl, Validators } from '@angular/forms'
import { ActivatedRoute, Router } from '@angular/router'
import { EMPTY, Observable } from 'rxjs'
import * as moment from 'moment'
import { faBan, faCheckCircle, faSave } from '@fortawesome/free-solid-svg-icons'

import { map } from 'rxjs/operators'
import { AlertService } from '../shared/service/alert.service'
import { UserService } from './service/user.service'
import { MemberService } from '../member/service/member.service'
import { AccountService } from '../account'
import { IUser, User } from './model/user.model'
import { IMember } from '../member/model/member.model'
import { ErrorService } from '../error/service/error.service'
import { AlertMessage, AlertType, DATE_TIME_FORMAT, emailValidator } from '../app.constants'

@Component({
  selector: 'app-user-update',
  templateUrl: './user-update.component.html',
})
export class UserUpdateComponent {
  isSaving = false
  isExistentMember = false
  existentUser: IUser | null = null
  faCheckCircle = faCheckCircle
  faBan = faBan
  faSave = faSave
  showIsAdminCheckbox = false
  currentAccount: any
  validation: any

  editForm = this.fb.group({
    id: new FormControl<string | null>(null),
    email: new FormControl<string | null>(null, [
      Validators.required,
      Validators.email,
      Validators.maxLength(50),
      emailValidator,
    ]),
    firstName: new FormControl<string | null>(null, Validators.required),
    lastName: new FormControl<string | null>(null, Validators.required),
    mainContact: new FormControl<boolean | null>(null),
    assertionServiceEnabled: new FormControl<boolean | null>(null),
    salesforceId: new FormControl<string | null>(null, Validators.required),
    activated: new FormControl<boolean | null>(null),
    isAdmin: new FormControl<boolean | null>(null),
    createdBy: new FormControl<string | null>(null),
    createdDate: new FormControl<string | null>(null),
    lastModifiedBy: new FormControl<string | null>(null),
    lastModifiedDate: new FormControl<string | null>(null),
  })

  memberList = [] as IMember[]
  hasOwner = false

  constructor(
    protected alertService: AlertService,
    protected userService: UserService,
    protected memberService: MemberService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected accountService: AccountService,
    protected errorService: ErrorService,
    private fb: FormBuilder,
    private cdref: ChangeDetectorRef
  ) {
    this.validation = {}
  }

  ngOnInit() {
    this.isSaving = false
    this.isExistentMember = false
    this.existentUser = null
    this.activatedRoute.data.subscribe(({ user }) => {
      this.existentUser = user
    })
    this.editForm.disable()
    this.accountService.getAccountData().subscribe((account) => {
      this.currentAccount = account
      this.getMemberList().subscribe((list: IMember[]) => {
        list.forEach((msMember: IMember) => {
          this.memberList.push(msMember)
        })
        this.editForm.enable()
        if (this.existentUser) {
          this.updateForm(this.existentUser)
        }
      })
    })
    this.cdref.detectChanges()
    this.onChanges()
  }

  onChanges(): void {
    this.editForm.get('salesforceId')?.valueChanges.subscribe((val) => {
      const selectedOrg = this.memberList.find((cm) => cm.salesforceId === this.editForm.get(['salesforceId'])?.value)
      if (this.hasRoleAdmin()) {
        if (selectedOrg) {
          this.showIsAdminCheckbox = selectedOrg.superadminEnabled || false
        } else {
          this.showIsAdminCheckbox = false
        }
      } else {
        this.showIsAdminCheckbox = false
      }
    })

    this.editForm.get('firstName')?.valueChanges.subscribe((val) => (this.isSaving = false))
    this.editForm.get('lastName')?.valueChanges.subscribe((val) => (this.isSaving = false))
    this.editForm.get('salesforceId')?.valueChanges.subscribe((val) => (this.isSaving = false))
    this.editForm.get('mainContact')?.valueChanges.subscribe((val) => (this.isSaving = false))
    this.editForm.get('assertionServiceEnabled')?.valueChanges.subscribe((val) => (this.isSaving = false))
  }

  updateForm(user: IUser) {
    this.editForm.patchValue({
      id: user.id,
      email: user.email,
      firstName: user.firstName,
      lastName: user.lastName,
      mainContact: user.mainContact,
      salesforceId: user.salesforceId,
      activated: user.activated,
      isAdmin: user.isAdmin,
      createdBy: user.createdBy,
      createdDate: user.createdDate != null ? user.createdDate.format(DATE_TIME_FORMAT) : null,
      lastModifiedBy: user.lastModifiedBy,
      lastModifiedDate: user.lastModifiedDate != null ? user.lastModifiedDate.format(DATE_TIME_FORMAT) : null,
    })

    if (user.mainContact) {
      this.editForm.get('mainContact')?.disable()
      this.editForm.get('salesforceId')?.disable()
    }

    if (user.salesforceId) {
      this.isExistentMember = true
    }
    if (user.email) {
      this.editForm.get('email')?.disable()
    }
  }

  getMemberList(): Observable<IMember[]> {
    if (this.hasRoleAdmin()) {
      return this.memberService.getAllMembers().pipe(
        map((members) => {
          if (members) {
            return members
          }
          return []
        })
      )
    } else {
      return this.memberService.find(this.currentAccount.salesforceId).pipe(
        map((res) => {
          if (res) {
            return [res]
          }
          return []
        })
      )
    }
  }

  navigateToUsersList() {
    this.router.navigate(['/users'])
  }

  navigateToHomePage() {
    this.router.navigate(['/'])
  }

  disableSalesForceIdDD() {
    if (this.hasRoleAdmin()) {
      return false
    } else if (this.hasRoleOrgOwner() || this.hasRoleConsortiumLead()) {
      this.editForm.patchValue({
        salesforceId: this.getSalesForceId(),
      })
      return true
    }
    return this.isExistentMember
  }

  getSalesForceId() {
    return this.accountService.getSalesforceId()
  }

  hasRoleAdmin() {
    return this.accountService.hasAnyAuthority(['ROLE_ADMIN'])
  }

  hasRoleOrgOwner() {
    return this.accountService.hasAnyAuthority(['ROLE_ORG_OWNER'])
  }

  hasRoleConsortiumLead() {
    return this.accountService.hasAnyAuthority(['ROLE_CONSORTIUM_LEAD'])
  }

  validateOrgOwners() {
    this.isSaving = true
    const sfId = this.editForm.get('salesforceId')?.value
    if (sfId) {
      this.userService.hasOwner(sfId).subscribe((value) => {
        this.isSaving = false
        if (!this.editForm.get('mainContact')?.value) {
          this.hasOwner = false
        } else {
          this.hasOwner = value
        }
      })

      if (this.editForm.get('mainContact')?.value) {
        this.editForm.get('salesforceId')?.disable()
      } else {
        this.editForm.get('salesforceId')?.enable()
      }
    }
  }

  confirmOwnershipChange() {
    const question = $localize`:@@gatewayApp.msUserServiceMSUser.changeOwnership.question.string:Are you sure you want to transfer ownership? You are about to transfer ownership of this organization account. If you are the organization owner after transferring ownership, you will no longer have access to administrative functions, such as managing users.`
    const result = confirm(question)
    if (result) {
      this.save()
    }
  }

  save() {
    if (this.editForm.valid) {
      this.isSaving = true
      const userFromForm = this.createFromForm()

      this.userService.validate(userFromForm).subscribe((response) => {
        const data = response
        if (data.valid) {
          if (userFromForm.id !== null) {
            if (this.currentAccount.id === userFromForm.id) {
              // ownership change functions redirect to homepage instead of redirecting to users list
              // as users who lose org owner status shouldn't have access to the users list
              if (this.currentAccount.mainContact !== userFromForm.mainContact) {
                this.subscribeToUpdateResponseWithOwnershipChange(this.userService.update(userFromForm))
              } else {
                this.subscribeToUpdateResponse(this.userService.update(userFromForm))
              }
            } else if (userFromForm.mainContact && !this.hasRoleAdmin()) {
              this.subscribeToUpdateResponseWithOwnershipChange(this.userService.update(userFromForm))
            } else {
              this.subscribeToUpdateResponse(this.userService.update(userFromForm))
            }
          } else {
            if (userFromForm.mainContact && !this.hasRoleAdmin()) {
              this.subscribeToSaveResponseWithOwnershipChange(this.userService.create(userFromForm))
            } else {
              this.subscribeToSaveResponse(this.userService.create(userFromForm))
            }
          }
        } else {
          this.isSaving = false
          this.validation = data
        }
      })
    }
  }

  sendActivate() {
    if (this.existentUser?.id) {
      this.userService.sendActivate(this.existentUser).subscribe((res) => {
        if (res) {
          this.alertService.broadcast(AlertType.TOAST, AlertMessage.SEND_ACTIVATION_SUCCESS)
        } else {
          this.alertService.broadcast(AlertType.TOAST, AlertMessage.SEND_ACTIVATION_FAILURE)
        }
        this.navigateToUsersList()
      })
    }
  }

  displaySendActivate() {
    if (this.existentUser && this.existentUser.email && !this.existentUser.activated) {
      return true
    }
    return false
  }

  private createFromForm(): IUser {
    return {
      ...new User(),

      id: this.editForm.get(['id'])?.value || null,
      email: this.editForm.get(['email'])?.value || null,
      firstName: this.editForm.get(['firstName'])?.value || null,
      lastName: this.editForm.get(['lastName'])?.value || null,
      mainContact: this.editForm.get(['mainContact'])?.value || false,
      isAdmin: this.editForm.get(['isAdmin'])?.value || false,
      salesforceId: this.editForm.get(['salesforceId'])?.value || null,
      createdBy: this.editForm.get(['createdBy'])?.value || null,
      createdDate:
        this.editForm.get(['createdDate'])?.value != null
          ? moment(this.editForm.get(['createdDate'])?.value, DATE_TIME_FORMAT)
          : undefined,
      lastModifiedBy: this.editForm.get(['lastModifiedBy'])?.value || null,
      lastModifiedDate:
        this.editForm.get(['lastModifiedDate'])?.value != null
          ? moment(this.editForm.get(['lastModifiedDate'])?.value, DATE_TIME_FORMAT)
          : undefined,
    }
  }

  protected subscribeToSaveResponse(result: Observable<IUser>) {
    result.subscribe({
      next: () => this.onSaveSuccess(),
    })
  }

  protected subscribeToUpdateResponse(result: Observable<IUser>) {
    result.subscribe({
      next: () => this.onUpdateSuccess(),
    })
  }

  protected subscribeToSaveResponseWithOwnershipChange(result: Observable<IUser>) {
    result.subscribe({
      next: () => this.onSaveSuccessOwnershipChange(),
    })
  }

  protected subscribeToUpdateResponseWithOwnershipChange(result: Observable<IUser>) {
    result.subscribe({
      next: () => this.onUpdateSuccessOwnershipChange(),
    })
  }

  protected onSaveSuccess() {
    this.isSaving = false
    this.navigateToUsersList()
    this.alertService.broadcast(AlertType.TOAST, AlertMessage.USER_CREATED)
  }

  protected onUpdateSuccess() {
    this.isSaving = false
    this.navigateToUsersList()
    this.alertService.broadcast(AlertType.TOAST, AlertMessage.USER_UPDATED)
  }

  protected onSaveSuccessOwnershipChange() {
    this.isSaving = false
    this.navigateToHomePage()
    this.alertService.broadcast(AlertType.TOAST, AlertMessage.USER_CREATED)
  }

  protected onUpdateSuccessOwnershipChange() {
    this.isSaving = false
    this.navigateToHomePage()
    this.alertService.broadcast(AlertType.TOAST, AlertMessage.USER_UPDATED)
  }

  protected onSaveError() {
    this.isSaving = false
  }
}
