import { Injectable } from '@angular/core'
import { HttpClient, HttpRequest, HttpEvent, HttpResponse } from '@angular/common/http'
import { Observable } from 'rxjs/internal/Observable'
import { map, of } from 'rxjs'

@Injectable()
export class FileUploadService {
  constructor(private http: HttpClient) {}

  uploadFile(
    resourceUrl: string,
    file: File,
    expectedResponseType: 'arraybuffer' | 'blob' | 'json' | 'text' | undefined
  ): Observable<string> {
    const formdata: FormData = new FormData()
    formdata.append('file', file)
    const req = new HttpRequest('POST', resourceUrl, formdata, {
      reportProgress: true,
      responseType: expectedResponseType,
    })
    this.http.request<string>(req).pipe(
      map((res: HttpEvent<string>) => {
        if (res instanceof HttpResponse) {
          return res.body
        }
        return of('')
      })
    )
    return of('')
  }
}
