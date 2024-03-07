import { Pipe, PipeTransform } from '@angular/core'
import { AlertType } from 'src/app/app.constants'

@Pipe({
  name: 'localize',
})
export class LocalizePipe implements PipeTransform {
  transform(value: string, ...args: any[]): any {
    switch (value) {
      case AlertType.SEND_ACTIVATION_SUCCESS:
        return $localize`:@@gatewayApp.msUserServiceMSUser.sendActivate.success.string:Invite sent.`
      case AlertType.SEND_ACTIVATION_FAILURE:
        return $localize`:@@gatewayApp.msUserServiceMSUser.sendActivate.error.string:Invite email couldn't be sent.`
      case AlertType.USER_CREATED:
        return $localize`:@@userServiceApp.user.created.string:User created. Invite sent.`
      case AlertType.USER_UPDATED:
        return $localize`:@@userServiceApp.user.updated.string:User updated successfully`
      case AlertType.USER_DELETED:
        return $localize`:@@userServiceApp.user.deleted.string:User deleted successfully`

      // Affiliation pretty statuses

      case 'User denied access':
        return $localize`:@@gatewayApp.assertionStatus.userDeniedAccess.string:User denied access`
      case 'Pending':
        return $localize`:@@gatewayApp.assertionStatus.pending.string:Pending`
      case 'In ORCID':
        return $localize`:@@gatewayApp.assertionStatus.inOrcid.string:In ORCID`
      case 'User granted access':
        return $localize`:@@gatewayApp.assertionStatus.userGrantedAccess.string:User granted access`
      case 'User deleted from ORCID':
        return $localize`:@@gatewayApp.assertionStatus.userDeletedFromOrcid.string:User deleted from ORCID`
      case 'User revoked access':
        return $localize`:@@gatewayApp.assertionStatus.userRevokedAccess.string:User revoked access`
      case 'Error adding to ORCID':
        return $localize`:@@gatewayApp.assertionStatus.errorAddingToOrcid.string:Error adding to ORCID`
      case 'Error updating in ORCID':
        return $localize`:@@gatewayApp.assertionStatus.errorUpdatingInOrcid.string:Error updating in ORCID`
      case 'Pending retry in ORCID':
        return $localize`:@@gatewayApp.assertionStatus.pendingRetryInOrcid.string:Pending retry in ORCID`
      case 'Error deleting in ORCID':
        return $localize`:@@gatewayApp.assertionStatus.errorDeletingInOrcid.string:Error deleting in ORCID`
      case 'Notification requested':
        return $localize`:@@gatewayApp.assertionStatus.notificationRequested.string:Notification requested`
      case 'Notification sent':
        return $localize`:@@gatewayApp.assertionStatus.notificationSent.string:Notification sent`
      case 'Notification failed':
        return $localize`:@@gatewayApp.assertionStatus.notificationFailed.string:Notification failed`
      case 'Pending update in ORCID':
        return $localize`:@@gatewayApp.assertionStatus.pendingUpdateInOrcid.string:Pending update in ORCID`
    }
  }
}
