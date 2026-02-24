import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { NavComponent } from '../../components/nav/nav.component';
import { TenderFormComponent } from '../../components/button/create-tender/tender-form.component';
import { TenderDetailComponent } from '../../components/tender-detail/tender-detail.component';
import { PaginationComponent } from '../../components/pagination/pagination.component';

interface TenderData {
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
  location?: string | null;
  contactNumber?: string | null;
  isWhatsapp?: boolean;
  comments?: string | null;
  userType?: string;
}

@Component({
  selector: 'app-tender-page',
  standalone: true,
  imports: [CommonModule, RouterModule, NavComponent, TenderFormComponent, TenderDetailComponent, FormsModule, PaginationComponent],
  templateUrl: './tender.html',
  styleUrl: './tender.css'
})
export class TenderPage implements OnInit {
  username = 'User';
  showTenderForm = false;
  showTenderDetail = false;
  selectedTender: TenderData | null = null;
  tenders: TenderData[] = [];
  userId: number | null = null;
  loading = false;
  searchTerm = '';
  statusFilter = '';
  
  
  currentPage: number = 1;
  itemsPerPage: number = 10;

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {
    if (typeof localStorage !== 'undefined') {
      this.username = localStorage.getItem('username') || 'User';
      const userIdStr = localStorage.getItem('userId');
      if (userIdStr) {
        this.userId = parseInt(userIdStr, 10);
      }
    }
    
    this.loadTenders();
  }

  ngOnInit() {
      console.log('TenderPage ngOnInit');
  }

  loadTenders() {
    console.log('Loading tenders from API...');
    this.loading = true;
    this.http.get<TenderData[]>('http://localhost:8080/api/tenders').subscribe({
      next: (data) => {
         console.log('Tenders loaded:', data);
        this.tenders = data.map(tender => {
          const normalizedTender = {
            ...tender,
            createdAt: this.normalizeDate(tender.createdAt),
            updatedAt: this.normalizeDate(tender.updatedAt),
            deadline: this.normalizeDate(tender.deadline)
          };
        
          return this.checkAndUpdateTenderStatus(normalizedTender);
        });
        this.loading = false;
        this.cdr.detectChanges(); 
      },
      error: (error) => {
        console.error('Error loading tenders:', error);
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  
  checkAndUpdateTenderStatus(tender: TenderData): TenderData {
    if (tender.deadline) {
      
      const deadlineStr = tender.deadline;
      const deadlineDate = new Date(deadlineStr);
      const now = new Date();
      
      
      if (deadlineDate <= now) {
        return { ...tender, status: 'CLOSED' };
      }
    }
    
    return tender;
  }

  
  getTenderStatus(tender: TenderData): string {
  
    if (tender.deadline) {
      // Parse deadline date - handle ISO format
      const deadlineStr = tender.deadline;
      const deadlineDate = new Date(deadlineStr);
      const now = new Date();
      
     
      if (deadlineDate <= now) {
        return 'CLOSED';
      }
    }
    return tender.status || 'OPEN';
  }

  
  normalizeDate(dateValue: any): string | null {
    if (!dateValue) return null;
    
   
    if (Array.isArray(dateValue) && dateValue.length >= 6) {
      const [year, month, day, hours, minutes, seconds] = dateValue;
      const date = new Date(year, month - 1, day, hours, minutes, seconds);
      return date.toISOString();
    }
    
    const dateStr = String(dateValue);
    

    if (dateStr.includes('T') || dateStr.includes('-')) {
      return dateStr;
    }
    
    const parts = dateStr.split(',');
    if (parts.length >= 6) {
      const year = parseInt(parts[0], 10);
      const month = parseInt(parts[1], 10) - 1; 
      const day = parseInt(parts[2], 10);
      const hours = parseInt(parts[3], 10);
      const minutes = parseInt(parts[4], 10);
      const seconds = parseInt(parts[5], 10);
      
      const date = new Date(year, month, day, hours, minutes, seconds);
      return date.toISOString();
    }
    
    return dateStr;
  }

  createTender() {
     console.log('Create tender button clicked');
    console.log('Current showTenderForm value:', this.showTenderForm);
    this.showTenderForm = true;
        console.log('New showTenderForm value:', this.showTenderForm);

    this.cdr.detectChanges();
        console.log('Change detection completed');

    
  }

  onTenderSubmit(formData: any) {
        console.log('Form submitted with data:', formData);

    if (this.userId) {
      const request = {
        name: formData.title,
        description: formData.description,
        createdBy: this.userId,
        budget: formData.budget ? parseFloat(formData.budget) : 0,
        openingDate: formData.openingDate ? formData.openingDate + 'T00:00:00' : null,
        deadline: formData.deadline ? formData.deadline + 'T00:00:00' : null,
        requiredDocuments: formData.requiredDocuments || '',
        category: formData.category || formData.selectedCategory || '',
        location: formData.location || '',
        contactNumber: formData.contactNumber || '',
        isWhatsapp: formData.isWhatsapp || false,
        comments: formData.comments || '',
        userType: formData.userType || ''
      };
            console.log('Sending request to API:', request);

      this.http.post<any>('http://localhost:8080/api/tenders/create', request).subscribe({
        next: (response) => {
                    console.log('Tender created successfully:', response);

          if (response.success) {
            alert('Tender created successfully!');
            this.showTenderForm = false;
          
            this.tenders = [];
            this.loadTenders();
          } else {
            alert(response.message || 'Error creating tender');
          }
        },
        error: (error) => {
          console.error('Error creating tender:', error);
          console.error('Error status:', error.status);
          console.error('Error message:', error.message);
          console.error('Error error:', error.error);
          console.error('Error error.message:', error.error?.message);
          console.error('Full error object:', JSON.stringify(error, null, 2));
          
          let errorMessage = 'Error creating tender. Please try again.';
      
          if (error.error && error.error.message) {
            errorMessage = 'Error: ' + error.error.message;
          } else if (error.message) {
            errorMessage = 'Error: ' + error.message;
          }
          
        
          if (error.status === 400 && error.error) {
            if (error.error.message && error.error.message.includes('foreign key')) {
              errorMessage = 'Error: Invalid user. Please logout and login again.';
            }
          }
          
          alert(errorMessage);
          this.cdr.detectChanges();
        }
      });
    } else {
      alert('Please login first to create a tender.');
      this.showTenderForm = false;
    }
  }

  onTenderCancel() {
    this.showTenderForm = false;
  }

  viewDetails(tenderId: number) {
    this.loading = true;
    this.http.get<TenderData>(`http://localhost:8080/api/tenders/${tenderId}`).subscribe({
      next: (data) => {
        this.selectedTender = data;
        this.showTenderDetail = true;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error fetching tender details:', error);
        alert('Error loading tender details');
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  confirmDelete(tenderId: number) {
    if (confirm('Are you sure you want to delete this tender?')) {
      this.onDeleteTender(tenderId);
    }
  }

  closeTenderDetail() {
    this.showTenderDetail = false;
    this.selectedTender = null;
  }

  onDeleteTender(tenderId: number) {
    if (this.userId) {
      this.http.delete<any>(`http://localhost:8080/api/tenders/${tenderId}?userId=${this.userId}`).subscribe({
        next: (response) => {
          if (response.success) {
            alert('Tender deleted successfully!');
            this.loadTenders();
          } else {
            alert(response.message);
          }
          this.cdr.detectChanges();
        },
        error: (error) => {
          console.error('Error deleting tender:', error);
          alert('Error deleting tender');
          this.cdr.detectChanges();
        }
      });
    }
  }

  getTotalTenders(): number {
    return this.filteredTenders.length;
  }

  getOpenTenders(): number {
    return this.filteredTenders.filter(t => this.getTenderStatus(t) === 'OPEN').length;
  }

  getClosedTenders(): number {
    return this.filteredTenders.filter(t => this.getTenderStatus(t) === 'CLOSED').length;
  }

  getTotalBudget(): number {
    return this.filteredTenders.reduce((sum, t) => sum + (t.budget || 0), 0);
  }

  get filteredTenders(): TenderData[] {
    return this.tenders.filter(tender => {
      const matchesSearch = tender.name.toLowerCase().includes(this.searchTerm.toLowerCase()) || 
                           tender.description.toLowerCase().includes(this.searchTerm.toLowerCase());
    
      const computedStatus = this.getTenderStatus(tender);
      const matchesStatus = this.statusFilter === '' || computedStatus === this.statusFilter;
      return matchesSearch && matchesStatus;
    });
  }
  
  
  get paginatedTenders(): TenderData[] {
    const filtered = this.filteredTenders;
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return filtered.slice(startIndex, endIndex);
  }
  
  onPageChange(newPage: number): void {
    this.currentPage = newPage;
    const tenderList = document.querySelector('.tender-list');
    if (tenderList) {
      tenderList.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  onSearchOrFilterChange(): void {
    this.currentPage = 1;
  }
}
