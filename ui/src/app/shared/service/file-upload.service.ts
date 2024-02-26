import { Injectable } from '@angular/core'
import { HttpClient, HttpRequest, HttpEvent, HttpResponse } from '@angular/common/http'
import { Observable } from 'rxjs/internal/Observable'
import { filter, map, of } from 'rxjs'

@Injectable({ providedIn: 'root' })
export class FileUploadService {
  constructor(private http: HttpClient) {}

  uploadFile(
    resourceUrl: string,
    file: File,
    expectedResponseType: 'arraybuffer' | 'blob' | 'json' | 'text' | undefined
  ): Observable<string> {
    console.log('uploading file')
    const formdata: FormData = new FormData()
    formdata.append('file', file)
    const req = new HttpRequest('POST', resourceUrl, formdata, {
      reportProgress: true,
      responseType: expectedResponseType,
    })

    return this.http.request<string>(req).pipe(
      filter((event: HttpEvent<string>): event is HttpResponse<string> => event instanceof HttpResponse),
      map((res: HttpResponse<string>) => {
        return res.body != null ? res.body : ''
      })
    )
  }
}
