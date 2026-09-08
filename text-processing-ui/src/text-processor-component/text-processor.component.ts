import { Component, signal, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpResponse } from '@angular/common/http';
import { TextProcessingService } from '../TextProcessingService';
import { Subscription, interval } from 'rxjs';

@Component({
  selector: 'app-text-processor',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './text-processor.component.html',
  styleUrls: ['./text-processor.component.css'],
})
export class TextProcessorComponent implements OnDestroy {
  private textProcessingService = inject(TextProcessingService);

  selectedFile = signal<File | null>(null);
  selectedFormat = signal<string>('xml');
  outputPath = signal<string>('');
  isProcessing = signal<boolean>(false);
  progress = signal<number>(0);
  statusMessage = signal<string>('');
  errorMessage = signal<string>('');

  private progressSubscription?: Subscription;

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile.set(input.files[0]);
      this.statusMessage.set('');
      this.errorMessage.set('');
      this.progress.set(0);
    }
  }

  executeProcessing(): void {
    const file = this.selectedFile();
    if (!file) return;

    this.isProcessing.set(true);
    this.progress.set(5);
    this.statusMessage.set('');
    this.errorMessage.set('');

    const serverPath = this.outputPath().trim();

    this.startSimulatedProgress();

    this.textProcessingService.processTextFile(file, this.selectedFormat()).subscribe({
      next: (event) => {
        // Narrow the type to HttpResponse<Blob>
        if (event instanceof HttpResponse) {
          this.stopSimulatedProgress();
          this.progress.set(100);
          this.isProcessing.set(false);

          if (serverPath) {
            this.statusMessage.set(`File processed and saved to server path: ${serverPath}`);
          } else {
            const defaultFilename = `processed_document.${this.selectedFormat()}`;
            this.statusMessage.set(`Transformation complete! Saved as "${defaultFilename}".`);

            if (event.body) {
              const blob = new Blob([event.body], {
                type: event.headers.get('content-type') || 'application/octet-stream',
              });
              const url = window.URL.createObjectURL(blob);
              const a = document.createElement('a');
              a.href = url;
              a.download = defaultFilename;
              a.click();
              window.URL.revokeObjectURL(url);
            }
          }
        }
      },
      error: (err) => {
        this.stopSimulatedProgress();
        this.isProcessing.set(false);
        this.progress.set(0);
        this.errorMessage.set(
          err.error?.message || 'Processing failed due to a server stream error.',
        );
      },
    });
  }

  private startSimulatedProgress(): void {
    this.stopSimulatedProgress();

    // Ticks every 400ms with smaller, realistic increments
    this.progressSubscription = interval(400).subscribe(() => {
      const current = this.progress();
      if (current < 40) {
        this.progress.set(current + 2);
      } else if (current < 75) {
        this.progress.set(current + 1);
      } else if (current < 90) {
        // Very slow crawling near completion while waiting for server response
        this.progress.set(current + 0.5);
      }
    });
  }
  private stopSimulatedProgress(): void {
    if (this.progressSubscription) {
      this.progressSubscription.unsubscribe();
      this.progressSubscription = undefined;
    }
  }

  ngOnDestroy(): void {
    this.stopSimulatedProgress();
  }
}
