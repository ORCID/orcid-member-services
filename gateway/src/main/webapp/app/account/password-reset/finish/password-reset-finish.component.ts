import { Component, OnInit, AfterViewInit, Renderer, ElementRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { NgbModalRef } from '@ng-bootstrap/ng-bootstrap';

import { LoginModalService } from 'app/core';
import { PasswordResetFinishService } from './password-reset-finish.service';

@Component({
  selector: 'jhi-password-reset-finish',
  templateUrl: './password-reset-finish.component.html'
})
export class PasswordResetFinishComponent implements OnInit, AfterViewInit {
  doNotMatch: string;
  error: string;
  keyMissing: boolean;
  success: string;
  modalRef: NgbModalRef;
  key: string;
  invalidKey: boolean;
  expiredKey: boolean;
  activationEmailResent: boolean;
  showPasswordForm: boolean;

  passwordForm = this.fb.group({
    newPassword: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(50)]],
    confirmPassword: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(50)]]
  });

  constructor(
    private passwordResetFinishService: PasswordResetFinishService,
    private loginModalService: LoginModalService,
    private route: ActivatedRoute,
    private elementRef: ElementRef,
    private renderer: Renderer,
    private fb: FormBuilder
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.key = params['key'];
    });
    this.keyMissing = !this.key;
    this.passwordResetFinishService.validateKey({ key: this.key }).subscribe(res => {
      this.expiredKey = res.body.expiredKey;
      this.invalidKey = res.body.invalidKey;
      this.showPasswordForm = !this.invalidKey && !this.expiredKey;
      if (this.expiredKey) {
        this.passwordResetFinishService.resendActivationEmail({ key: this.key }).subscribe(result => {
          this.activationEmailResent = result.body.resent;
        });
      }
    });
  }

  ngAfterViewInit() {
    if (this.elementRef.nativeElement.querySelector('#password') != null) {
      this.renderer.invokeElementMethod(this.elementRef.nativeElement.querySelector('#password'), 'focus', []);
    }
  }

  finishReset() {
    this.doNotMatch = null;
    this.error = null;
    const password = this.passwordForm.get(['newPassword']).value;
    const confirmPassword = this.passwordForm.get(['confirmPassword']).value;
    if (password !== confirmPassword) {
      this.doNotMatch = 'ERROR';
    } else {
      this.passwordResetFinishService.save({ key: this.key, newPassword: password }).subscribe(
        () => {
          this.success = 'OK';
        },
        () => {
          this.success = null;
          this.error = 'ERROR';
        }
      );
    }
  }

  login() {
    this.modalRef = this.loginModalService.open();
  }
}
