import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  inject,
  signal,
  OnInit,
} from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { FormBuilder, FormControl, Validators, ReactiveFormsModule } from '@angular/forms'
import { ActivatedRoute, Router } from '@angular/router'
import { faBan, faCheckCircle, faSave } from '@fortawesome/free-solid-svg-icons'
import moment from 'moment'
import { Observable } from 'rxjs'

import { map } from 'rxjs/operators'
import { AccountService } from '../account'
import { IAccount } from '../account/model/account.model'
import { AlertMessage, AlertType, DATE_TIME_FORMAT, emailValidator } from '../app.constants'
import { ErrorService } from '../error/service/error.service'
import { IMember } from '../member/model/member.model'
import { MemberService } from '../member/service/member.service'
import { AlertService } from '../shared/service/alert.service'
import { IUser, User } from './model/user.model'
import { IUserValidation } from './model/user-validation.model'
import { UserService } from './service/user.service'
import { FeatureToggleService } from '../shared/service/feature-toggle.service'
import { ErrorAlertComponent } from '../error/error-alert.component'
import { NgbAlertModule } from '@ng-bootstrap/ng-bootstrap'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'

@Component({
  selector: 'app-user-update',
  templateUrl: './user-update.component.html',
  styleUrls: ['./user-update.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, ErrorAlertComponent, NgbAlertModule, FaIconComponent],
})
export class UserUpdateComponent implements OnInit {
  protected alertService = inject(AlertService)
  protected userService = inject(UserService)
  protected memberService = inject(MemberService)
  protected activatedRoute = inject(ActivatedRoute)
  protected router = inject(Router)
  protected accountService = inject(AccountService)
  protected errorService = inject(ErrorService)
  private fb = inject(FormBuilder)
  private cdref = inject(ChangeDetectorRef)
  protected featureService = inject(FeatureToggleService)
  private destroyRef = inject(DestroyRef)

  protected isSaving = signal(false)
  protected isExistentMember = signal(false)
  protected existentUser = signal<IUser | null>(null)
  protected faCheckCircle = faCheckCircle
  protected faBan = faBan
  protected faSave = faSave
  protected showIsAdminCheckbox = signal(false)
  protected currentAccount = signal<IAccount | null>(null)
  protected validation = signal<Partial<IUserValidation>>({})
  protected disableMfa = signal(false)

  editForm = this.fb.group({
    id: new FormControl<string | null>(null),
    email: new FormControl<string | null>(null, [
      Validators.required,
      Validators.email,
      Validators.maxLength(50),
      emailValidator,
    ]),
    firstName: new FormControl<string | null>(null, [Validators.required, Validators.maxLength(50)]),
    lastName: new FormControl<string | null>(null, [Validators.required, Validators.maxLength(50)]),
    mainContact: new FormControl<boolean | null>(null),
    manageApiCredentialsEnabled: new FormControl<boolean | null>(null),
    assertionServiceEnabled: new FormControl<boolean | null>(null),
    memberId: new FormControl<string | null>(null, Validators.required),
    activated: new FormControl<boolean | null>(null),
    isAdmin: new FormControl<boolean | null>(null),
    createdBy: new FormControl<string | null>(null),
    createdDate: new FormControl<string | null>(null),
    lastModifiedBy: new FormControl<string | null>(null),
    lastModifiedDate: new FormControl<string | null>(null),
  })

  mfaForm = this.fb.group({
    id: new FormControl<string | null>(null),
    twoFactorAuthentication: new FormControl<boolean | null>(null),
  })

  memberList = signal<IMember[]>([])
  protected hasOwner = signal(false)

  ngOnInit() {
    this.featureService.initFeatures().subscribe()
    this.isSaving.set(false)
    this.isExistentMember.set(false)
    this.existentUser.set(null)
    this.activatedRoute.data.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(({ user }) => {
      this.existentUser.set(user)
    })
    this.editForm.disable()
    this.accountService.getAccountData().subscribe((account) => {
      this.currentAccount.set(account ?? null)
      this.getMemberList().subscribe((list: IMember[]) => {
        this.memberList.set(list)
        this.editForm.enable()
        const existentUser = this.existentUser()
        if (existentUser) {
          this.updateForm(existentUser)
          this.updateMfaForm(existentUser)
        } else {
          if (this.hasRoleOrgOwner() || this.hasRoleConsortiumLead()) {
            this.editForm.patchValue({
              memberId: this.getMemberId(),
            })
          }
        }
      })
    })
    this.cdref.detectChanges()
    this.onChanges()
  }

  onChanges(): void {
    this.editForm.get('memberId')?.valueChanges.subscribe(() => {
      console.log('UserUpdateComponent: memberId value changed, checking if selected org is superadminEnabled')
      const selectedOrg = this.memberList().find((cm) => cm.id === this.editForm.get(['memberId'])?.value)
      if (this.hasRoleAdmin()) {
        if (selectedOrg) {
          this.showIsAdminCheckbox.set(selectedOrg.superadminEnabled || false)
        } else {
          this.showIsAdminCheckbox.set(false)
        }
      } else {
        this.showIsAdminCheckbox.set(false)
      }
    })

    this.editForm.get('firstName')?.valueChanges.subscribe(() => this.isSaving.set(false))
    this.editForm.get('lastName')?.valueChanges.subscribe(() => this.isSaving.set(false))
    this.editForm.get('salesforceId')?.valueChanges.subscribe(() => this.isSaving.set(false))
    this.editForm.get('mainContact')?.valueChanges.subscribe(() => this.isSaving.set(false))
    this.editForm.get('assertionServiceEnabled')?.valueChanges.subscribe(() => this.isSaving.set(false))

    // MFA
    this.mfaForm.get('twoFactorAuthentication')?.valueChanges.subscribe((val) => {
      this.isSaving.set(false)
      if (val != null) this.disableMfa.set(!val)
    })
  }

  updateForm(user: IUser) {
    this.editForm.patchValue({
      id: user.id,
      email: user.email,
      firstName: user.firstName,
      lastName: user.lastName,
      mainContact: user.mainContact,
      memberId: user.memberId,
      manageApiCredentialsEnabled: user.mainContact || user.manageApiCredsEnabled,
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

    if (user.memberId) {
      this.isExistentMember.set(true)
    }
    if (user.email) {
      this.editForm.get('email')?.disable()
    }
  }

  updateMfaForm(user: IUser) {
    this.mfaForm.patchValue({
      id: user.id,
      twoFactorAuthentication: user.mfaEnabled,
    })
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
      return this.memberService.find(this.currentAccount()?.memberId ?? '').pipe(
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

  getMemberId() {
    return this.accountService.getMemberId()
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

  validateOrgOwners(event?: Event) {
    this.isSaving.set(true)
    const memberId = this.editForm.get('memberId')?.value
    if (memberId) {
      this.userService.hasOwner(memberId).subscribe((value) => {
        this.isSaving.set(false)
        if (!this.editForm.get('mainContact')?.value) {
          this.hasOwner.set(false)
        } else {
          this.hasOwner.set(value)
        }
      })
    }
    if (event) {
      const checked = (event.target as HTMLInputElement).checked
      const manageApiCredentialsControl = this.editForm.get('manageApiCredentialsEnabled')
      if (checked) {
        manageApiCredentialsControl?.patchValue(checked)
      }
      checked ? manageApiCredentialsControl?.disable() : manageApiCredentialsControl?.enable()
    }
  }

  confirmOwnershipChange() {
    const question = $localize`:@@gatewayApp.msUserServiceMSUser.changeOwnership.question.string:Are you sure you want to transfer ownership? You are about to transfer ownership of this organization account. If you are the organization owner after transferring ownership, you will no longer have access to administrative functions, such as managing users.`
    const result = confirm(question)
    if (result) {
      this.save()
    }
  }

  saveMfa() {
    if (this.mfaForm.valid) {
      this.isSaving.set(true)
      const userFromForm = this.createFromForm()
      this.userService.validate(userFromForm).subscribe((response) => {
        const data = response
        if (data.valid) {
          if (userFromForm != null && this.hasRoleAdmin() && this.disableMfa() && userFromForm.id) {
            this.subscribeToUpdateResponse(this.accountService.disableMfa(userFromForm.id))
          }
        } else {
          this.isSaving.set(false)
          this.validation.set(data)
        }
      })
    }
  }

  save() {
    if (this.editForm.valid) {
      this.isSaving.set(true)
      const userFromForm = this.createFromForm()
      this.userService.validate(userFromForm).subscribe((response) => {
        const data = response
        if (data.valid) {
          if (userFromForm.id !== null) {
            if (this.currentAccount()?.id === userFromForm.id) {
              // ownership change functions redirect to homepage instead of redirecting to users list
              // as users who lose org owner status shouldn't have access to the users list
              if (this.currentAccount()?.mainContact !== userFromForm.mainContact) {
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
          this.isSaving.set(false)
          this.validation.set(data)
        }
      })
    }
  }

  sendActivate() {
    const existentUser = this.existentUser()
    if (existentUser?.id) {
      this.userService.sendActivate(existentUser).subscribe((res) => {
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
    const existentUser = this.existentUser()
    return !!(existentUser && existentUser.email && !existentUser.activated)
  }

  private createFromForm(): IUser {
    return {
      ...new User(),

      id: this.editForm.get(['id'])?.value || null,
      email: this.editForm.get(['email'])?.value || null,
      firstName: this.editForm.get(['firstName'])?.value || null,
      lastName: this.editForm.get(['lastName'])?.value || null,
      mainContact: this.editForm.get(['mainContact'])?.value || false,
      manageApiCredsEnabled: this.editForm.get(['manageApiCredentialsEnabled'])?.value || false,
      isAdmin: this.editForm.get(['isAdmin'])?.value || false,
      memberId: this.editForm.get(['memberId'])?.value || null,
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

  protected subscribeToUpdateResponse(result: Observable<IUser | boolean>) {
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
    this.isSaving.set(false)
    this.navigateToUsersList()
    this.alertService.broadcast(AlertType.TOAST, AlertMessage.USER_CREATED)
  }

  protected onUpdateSuccess() {
    this.isSaving.set(false)
    this.navigateToUsersList()
    this.alertService.broadcast(AlertType.TOAST, AlertMessage.USER_UPDATED)
  }

  protected onSaveSuccessOwnershipChange() {
    this.isSaving.set(false)
    this.navigateToHomePage()
    this.alertService.broadcast(AlertType.TOAST, AlertMessage.USER_CREATED)
  }

  protected onUpdateSuccessOwnershipChange() {
    this.isSaving.set(false)
    this.navigateToHomePage()
    this.alertService.broadcast(AlertType.TOAST, AlertMessage.USER_UPDATED)
  }

  protected onSaveError() {
    this.isSaving.set(false)
  }
}
