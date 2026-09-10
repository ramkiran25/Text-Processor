import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TextProcessingService {
  private apiUrl = 'http://localhost:8080/api/text/process';

  constructor(private http: HttpClient) {}

  processFile(file: File, format: string, outputPath: string): Observable<Blob> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('format', format);
    
    if (outputPath && outputPath.trim() !== '') {
      formData.append('outputPath', outputPath.trim());
    }

    // ALWAYS expect a raw blob from the streaming endpoint to keep CORS happy
    return this.http.post(this.apiUrl, formData, { responseType: 'blob' });
  }
}