import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpEvent, HttpEventType, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class TextProcessingService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/text/process';

  processTextFile(file: File, format: string): Observable<HttpEvent<Blob>> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    formData.append('format', format.toLowerCase());

    const request = new HttpRequest('POST', this.apiUrl, formData, {
      reportProgress: true,
      responseType: 'blob',
    });

    return this.http.request(request);
  }
}
