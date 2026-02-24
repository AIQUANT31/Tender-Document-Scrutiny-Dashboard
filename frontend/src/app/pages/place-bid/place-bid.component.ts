import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { NavComponent } from '../../components/nav/nav.component';
import { BidDocumentUploadComponent } from '../../components/bid-document-upload/bid-document-upload.component';
import { DocumentValidationService, ValidationResponse } from '../../services/document-validation.service';

interface TenderDetail {
  id: number;
  name: string;
  description: string;
  createdBy: number;
  createdAt: string | null;
  updatedAt: string | null;
  status: string;
  budget: number;
  deadline?: string | null;
  openingDate?: string | null;
  requiredDocuments?: string;
  category?: string;
  location?: string | null;
  contactNumber?: string | null;
  isWhatsapp?: boolean;
  comments?: string | null;
  userType?: string;
}

interface RequiredDocument {
  name: string;
  uploaded: boolean;
}

interface BidderProfile {
  id: number;
  companyName: string;
  email: string;
  phone: string;
  type: string;
  status: string;
}

@Component({
  selector: 'app-place-bid',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, NavComponent, BidDocumentUploadComponent],
  templateUrl: './place-bid.component.html',
  styleUrl: './place-bid.component.css'
})
export class PlaceBidComponent implements OnInit {
  tender: TenderDetail | null = null;
  loading = true;
  error: string | null = null;
  

  bidAmount = '';
  proposalText = '';
  contactNumber = '';
  bidError = '';
  
 
  bidderProfiles: BidderProfile[] = [];
  loadingProfiles = false;
  bidderId: number | null = null;
  

  currentUserId: number | null = null;
  
  createdBidId: number | null = null;
  
  selectedBidFile: File | null = null;
  selectedBidFiles: File[] = [];
  
  requiredDocumentsList: string[] = [];
  

  documentValidationResult: ValidationResponse | null = null;
  isValidatingDocuments = false;

  constructor(
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private documentValidation: DocumentValidationService
  ) {
  
    if (typeof localStorage !== 'undefined') {
      const userIdStr = localStorage.getItem('userId');
      if (userIdStr) {
        this.currentUserId = parseInt(userIdStr, 10);
      }
      
      
      const bidderIdStr = localStorage.getItem('bidderId');
      if (bidderIdStr) {
        this.bidderId = parseInt(bidderIdStr, 10);
      }
    }
  }

  ngOnInit() {
    const tenderId = this.route.snapshot.paramMap.get('id');
    if (tenderId) {
      this.loadTender(parseInt(tenderId, 10));
    } else {
      this.error = 'Invalid tender ID';
      this.loading = false;
    }
  }

  loadTender(tenderId: number) {
    this.loading = true;
    this.http.get<TenderDetail>(`http://localhost:8080/api/tenders/${tenderId}`).subscribe({
      next: (data) => {
        this.tender = data;
        this.loading = false;
        
        
        if (this.tender.status === 'CLOSED' || this.isDeadlinePassed()) {
          this.error = 'This tender is closed. Bids are no longer accepted.';
          this.loading = false;
          return;
        }
        
      
        if (this.currentUserId && this.tender.createdBy === this.currentUserId) {
          this.error = 'You cannot bid on your own tender.';
          this.loading = false;
          return;
        }
        
        
        this.parseRequiredDocuments();
        
      
        this.loadBidderProfiles();
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error loading tender:', error);
        this.error = 'Error loading tender details';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  isDeadlinePassed(): boolean {
    if (!this.tender?.deadline) return false;
    const deadlineDate = new Date(this.tender.deadline);
    const now = new Date();
  
    return deadlineDate <= now;
  }
  
  parseRequiredDocuments() {
    if (!this.tender?.requiredDocuments) {
      this.requiredDocumentsList = [];
      return;
    }
    
    try {
      
      const parsed = JSON.parse(this.tender.requiredDocuments);
      if (Array.isArray(parsed)) {
        this.requiredDocumentsList = parsed;
      } else {
        
        this.requiredDocumentsList = this.tender.requiredDocuments.split(',').map(d => d.trim()).filter(d => d);
      }
    } catch {
      // If JSON parsing fails, try comma-separated
      this.requiredDocumentsList = this.tender.requiredDocuments.split(',').map(d => d.trim()).filter(d => d);
    }
  }
  
  get hasRequiredDocuments(): boolean {
    return this.requiredDocumentsList.length > 0;
  }

  loadBidderProfiles() {
    if (!this.currentUserId) return;
    
    this.loadingProfiles = true;
    this.http.get<BidderProfile[]>(`http://localhost:8080/api/bidders/user/${this.currentUserId}`).subscribe({
      next: (profiles) => {
        this.bidderProfiles = profiles;
        if (profiles.length > 0) {
          this.bidderId = profiles[0].id;
          if (typeof localStorage !== 'undefined') {
            localStorage.setItem('bidderId', this.bidderId.toString());
          }
        }
        this.loadingProfiles = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error loading bidder profiles:', error);
        this.loadingProfiles = false;
        this.cdr.detectChanges();
      }
    });
  }

 
  async validateDocumentsAsync(): Promise<boolean> {
  
    if (!this.hasRequiredDocuments) {
      return true;
    }
    
    
    if (!this.selectedBidFiles || this.selectedBidFiles.length === 0) {
      this.bidError = 'This tender requires documents: ' + this.requiredDocumentsList.join(', ');
      return false;
    }
    
    this.isValidatingDocuments = true;
    this.bidError = 'Validating document content... please wait';
    this.cdr.detectChanges();
    
    try {
      
      const result = await this.documentValidation.validateWithRules(
        this.requiredDocumentsList,
        this.selectedBidFiles
      ).toPromise();
      
      this.documentValidationResult = result || null;
      this.isValidatingDocuments = false;
      
      if (!result?.valid) {
       
        this.cdr.detectChanges();
        return false;
      }
      
      if (result?.duplicateDocuments && result.duplicateDocuments.length > 0) {
        this.bidError = 'Duplicate documents detected. Please remove duplicates before submitting.';
        this.cdr.detectChanges();
        return false;
      }
     
      if (result?.matchedDocuments && result.matchedDocuments.length > 0) {
      }
      
      this.bidError = '';
      this.cdr.detectChanges();
      return true;
      
    } catch (error) {
      console.error('Validation error:', error);
      this.isValidatingDocuments = false;
      this.bidError = 'Validation failed. Please try again.';
      this.cdr.detectChanges();
      return false;
    }
  }

 
  validateDocumentsClientSide(): boolean {
    if (!this.hasRequiredDocuments || !this.selectedBidFiles || this.selectedBidFiles.length === 0) {
      return true;
    }
    
    const uploadedFileNames = this.selectedBidFiles.map(f => f.name);
    const result = this.documentValidation.validateDocumentsClientSide(
      this.requiredDocumentsList,
      uploadedFileNames
    );
    
    this.documentValidationResult = result;
    
    if (!result.valid) {
     
      return false;
    }
    
    return true;
  }

  async submitBid() {
    if (!this.tender) return;

    if (!this.bidAmount || parseFloat(this.bidAmount) <= 0) {
      this.bidError = 'Please enter a valid bid amount';
      return;
    }

    if (!this.bidderId) {
      this.bidError = 'Please select a bidder profile first';
      return;
    }

    
    const isValid = await this.validateDocumentsAsync();
    if (!isValid) {
      return; 
    }
    if (this.selectedBidFiles && this.selectedBidFiles.length > 0) {
      const formData = new FormData();
      formData.append('tenderId', this.tender.id.toString());
      formData.append('bidderId', this.bidderId.toString());
      formData.append('bidAmount', this.bidAmount);
      formData.append('proposalText', this.proposalText);
      formData.append('contactNumber', this.contactNumber);
      formData.append('status', 'PENDING');
      
      // Send all selected PDFs in one request
      for (const file of this.selectedBidFiles) {
        formData.append('files', file);
      }

      this.http.post<any>('http://localhost:8080/api/bids/create-with-document', formData).subscribe({
        next: (response) => {
            console.log('Bid response:', response);
          if (response.success) {
            alert('Bid placed successfully with ' + this.selectedBidFiles.length + ' document(s)!');
            this.router.navigate(['/tender']);
          } else {
            this.bidError = response.message || 'Error placing bid';
          }
          this.cdr.detectChanges();
        },
        error: (error) => {
          console.error('Error placing bid:', error);
          this.bidError = 'Error placing bid: ' + (error.error?.message || error.message || 'Please try again');
          this.cdr.detectChanges();
        }
      });
    } else {
      // No file, submit as JSON
      const bidRequest = {
        tenderId: this.tender.id,
        bidderId: this.bidderId,
        bidAmount: parseFloat(this.bidAmount),
        proposalText: this.proposalText,
        contactNumber: this.contactNumber,
        status: 'PENDING'
      };

      this.http.post<any>('http://localhost:8080/api/bids/create', bidRequest).subscribe({
        next: (response) => {
           console.log('Bid response:', response);
          if (response.success) {
            alert('Bid placed successfully!');
            this.router.navigate(['/tender']);
          } else {
            this.bidError = response.message || 'Error placing bid';
          }
          this.cdr.detectChanges();
        },
        error: (error) => {
          console.error('Error placing bid:', error);
          this.bidError = 'Error placing bid: ' + (error.error?.message || error.message || 'Please try again');
          this.cdr.detectChanges();
        }
      });
    }
  }

  goBack() {
    this.router.navigate(['/tender']);
  }

  onFileSelected(file: File) {
    this.selectedBidFile = file;
  }

  onFilesSelected(files: File[]) {
    this.selectedBidFiles = files;
  }
}
