import { Pipe, PipeTransform } from '@angular/core'
import { AlertType } from 'src/app/app.constants'

@Pipe({
  name: 'localize',
})
export class LocalizePipe implements PipeTransform {
  transform(value: string, ...args: any[]): any {
    switch (value) {
      case AlertType.SEND_ACTIVATION_SUCCESS:
        return $localize`:@@gatewayApp.msUserServiceMSUser.sendActivate.success.string:${AlertType.SEND_ACTIVATION_SUCCESS}`
      case AlertType.SEND_ACTIVATION_FAILURE:
        return $localize`:@@gatewayApp.msUserServiceMSUser.sendActivate.error.string:${AlertType.SEND_ACTIVATION_FAILURE}`
      case AlertType.USER_CREATED:
        return $localize`:@@userServiceApp.user.created.string:${AlertType.USER_CREATED}`
      case AlertType.USER_UPDATED:
        return $localize`:@@userServiceApp.user.updated.string:${AlertType.USER_UPDATED}`
    }
  }
}
