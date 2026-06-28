import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { TextProcessingService } from '../TextProcessingService';
import { finalize } from 'rxjs/operators';
import { Subscription, interval } from 'rxjs';

@Component({
  selector: 'app-text-processor-component',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './text-processor-component.html',
  styleUrl: './text-processor-component.css'
})
export class TextProcessorComponent implements OnInit, OnDestroy {
  processorForm!: FormGroup;
  selectedFile: File | null = null;

  isLoading: boolean = false;
  progressValue: number = 0;
  showDownloadButton: boolean = false;

  errorMessage: string = '';
  successMessage: string = '';

  private processedBlob: Blob | null = null;
  private progressSubscription?: Subscription;

  constructor(
    private processingService: TextProcessingService,
    private cdr: ChangeDetectorRef // Forces templates to repaint on async interval changes
  ) {}

  ngOnInit(): void {
    this.processorForm = new FormGroup({
      format: new FormControl('csv', Validators.required),
      outputPath: new FormControl('')
    });
  }

  onFileSelected(event: any): void {
    const file: File = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      this.resetUIStates();
    }
  }

  private resetUIStates(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.showDownloadButton = false;
    this.progressValue = 0;
    this.processedBlob = null;
  }

onSubmit(): void {
  if (!this.selectedFile || this.processorForm.invalid) return;

  this.isLoading = true;
  this.errorMessage = '';
  this.successMessage = '';
  this.showDownloadButton = false;
  this.progressValue = 0;

  const formatValue = this.processorForm.get('format')?.value;
  const outputPathValue = this.processorForm.get('outputPath')?.value;

  this.startSimulatedProgress();

  this.processingService.processFile(this.selectedFile, formatValue, outputPathValue)
    .pipe(
      finalize(() => {
        this.stopProgressTimer();
        this.isLoading = false;
        this.cdr.detectChanges();
      })
    )
    .subscribe({
      next: (blob: Blob) => {
        this.progressValue = 100;

        // If the user provided a local output path, parse the text out of the blob response
        if (outputPathValue && outputPathValue.trim() !== '') {
          const reader = new FileReader();
          reader.onload = () => {
            try {
              const textResult = reader.result as string;
              // Check if it's our success message or a backend exception message
              if (textResult.includes("SUCCESS")) {
                this.successMessage = `Success! The file has been fully processed and saved on the server to: ${outputPathValue}`;
              } else {
                this.successMessage = `Processing finished. Check target directory output destination path.`;
              }
            } catch (e) {
              this.successMessage = `Success! The file has been successfully written locally to disk.`;
            }
            this.showDownloadButton = false;
            this.cdr.detectChanges();
          };
          reader.readAsText(blob);
        } else {
          // Standard web browser client downpour download path
          this.successMessage = `File conversion complete! Click the button below to download your document.`;
          this.showDownloadButton = true;
          this.processedBlob = blob;
          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        console.error('Network catch details:', err);
        this.progressValue = 0;
        this.errorMessage = `Connection disrupted or invalid path permissions. Check your backend console logs for validation traces.`;
        this.cdr.detectChanges();
      }
    });
}

 private startSimulatedProgress(): void {
  this.stopProgressTimer();
  this.progressValue = 0;

  // Change interval to 300ms to give the browser room to process network stream frames
  this.progressSubscription = interval(300).subscribe(() => {
    if (this.progressValue < 80) {
      this.progressValue += 4; // Steady steady climb up to 80%
    } else if (this.progressValue < 95) {
      this.progressValue += 1; // Creep slower while disk IO writes are finalizing
    }
    
    // Mark for check instead of forcefully blocking the event thread
    this.cdr.markForCheck(); 
  });
}

  private stopProgressTimer(): void {
    if (this.progressSubscription) {
      this.progressSubscription.unsubscribe();
      this.progressSubscription = undefined;
    }
  }

  triggerFileDownload(): void {
    if (!this.processedBlob) return;

    const formatValue = this.processorForm.get('format')?.value;
    const blobUrl = window.URL.createObjectURL(this.processedBlob);
    const link = document.createElement('a');
    link.href = blobUrl;

    const outputName = this.selectedFile!.name.substring(0, this.selectedFile!.name.lastIndexOf('.')) || this.selectedFile!.name;
    link.download = `${outputName}_processed.${formatValue}`;

    link.click();
    window.URL.revokeObjectURL(blobUrl);
    this.showDownloadButton = false;
  }

  ngOnDestroy(): void {
    this.stopProgressTimer();
  }
}