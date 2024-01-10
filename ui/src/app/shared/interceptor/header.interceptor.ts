import { Injectable } from '@angular/core'
import { HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http'

@Injectable()
export class HeaderInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler) {
    const langCode = localStorage.getItem('langCode')

    if (langCode) {
      const request = req.clone({
        headers: req.headers.set('Accept-Language', langCode),
      })

      // send cloned request with header to the next handler.
      return next.handle(request)
    }

    // pass through unaltered request
    return next.handle(req)
  }
}
