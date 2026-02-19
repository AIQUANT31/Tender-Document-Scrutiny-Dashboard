import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-bid-document-upload',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './bid-document-upload.component.html',
  styleUrl: './bid-document-upload.component.css'
})
export class BidDocumentUploadComponent {
  @Input() bidId: number | null = null;
  @Input() tenderId: number | null = null;
  @Input() requiredDocuments: string[] = [];
  @Output() documentUploaded = new EventEmitter<string[]>();
  @Output() filesSelected = new EventEmitter<File[]>();

  selectedFiles: File[] = [];
  uploading = false;
  uploadError: string | null = null;
  uploadSuccess = false;
  uploadedFileNames: string[] = [];

  constructor(private http: HttpClient) {}

  get hasRequiredDocuments(): boolean {
    return this.requiredDocuments.length > 0;
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const newFiles = Array.from(input.files);
      
      for (const file of newFiles) {
        // Validate file type
        if (file.type !== 'application/pdf') {
          this.uploadError = 'Only PDF files are allowed';
          return;
        }

        // Validate file size (max 50MB)
        if (file.size > 50 * 1024 * 1024) {
          this.uploadError = 'File size must be less than 50MB';
          return;
        }

        // Add to selected files if not already added
        const exists = this.selectedFiles.some(f => f.name === file.name && f.size === file.size);
        if (!exists) {
          this.selectedFiles.push(file);
        }
      }

      this.uploadError = null;
      this.uploadSuccess = false;
      
      // Emit files to parent
      this.filesSelected.emit(this.selectedFiles);
    }
    
    // Reset input
    input.value = '';
  }

  removeFile(index: number): void {
    this.selectedFiles.splice(index, 1);
    this.filesSelected.emit(this.selectedFiles);
  }

  clearFiles(): void {
    this.selectedFiles = [];
    this.filesSelected.emit(this.selectedFiles);
  }

  uploadFiles(): void {
    if (this.selectedFiles.length === 0) {
      this.uploadError = 'Please select at least one file';
      return;
    }

    this.uploading = true;
    this.uploadError = null;

    const formData = new FormData();
    for (const file of this.selectedFiles) {
      formData.append('files', file);
    }
    if (this.bidId) {
      formData.append('bidId', this.bidId.toString());
    }

    this.http.post<any>('http://localhost:8080/api/bids/upload-documents', formData).subscribe({
      next: (response) => {
        if (response.success) {
          this.uploadSuccess = true;
          this.uploadedFileNames = response.fileNames || this.selectedFiles.map(f => f.name);
          this.documentUploaded.emit(this.uploadedFileNames);
        } else {
          this.uploadError = response.message || 'Error uploading documents';
        }
        this.uploading = false;
      },
      error: (error) => {
        console.error('Error uploading documents:', error);
        this.uploadError = 'Error uploading documents. Please try again.';
        this.uploading = false;
      }
    });
  }

  downloadDocument(): void {
    if (this.bidId) {
      window.open(`http://localhost:8080/api/bids/download-document/${this.bidId}`, '_blank');
    }
  }
}
