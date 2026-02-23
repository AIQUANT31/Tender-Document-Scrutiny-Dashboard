import { Component, Input, Output, EventEmitter, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-tender-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tender-form.component.html',
  styleUrl: './tender-form.component.css'
})
export class TenderFormComponent implements OnChanges {
  @Input() isOpen = false;
  @Input() title = 'Create New Tender';
  @Output() onSubmitForm = new EventEmitter<any>();
  @Output() onCancel = new EventEmitter<void>();

  constructor() {
      console.log('TenderFormComponent initialized, isOpen:', this.isOpen);

  }

  ngOnChanges() {
      console.log('TenderFormComponent ngOnChanges, isOpen:', this.isOpen);
  }

  formData = {
    title: '',
    description: '',
    budget: null as number | null,
    deadline: '',
    openingDate: '',
    location: '',
    category: '',
    userType: '',
    contactNumber: '',
    isWhatsapp: false,
    comments: '',
    requiredDocuments: [] as string[]
  };

  customDocument = '';
  formError = false;
  selectedCategory = '';

  // Category-based document requirements
  categoryDocuments: { [key: string]: string[] } = {
    construction: [
      'Technical Specifications',
      'Financial Proposal',
      'Company Registration',
      'Tax Clearance',
      'Experience Certificates',
      'Equipment List',
      'Safety Certificates'
    ],
    infrastructure: [
      'Technical Specifications',
      'Financial Proposal',
      'Company Registration',
      'Tax Clearance',
      'Experience Certificates',
      'Environmental Impact Assessment',
      'Permits and Licenses'
    ],
    engineering: [
      'Technical Specifications',
      'Financial Proposal',
      'Company Registration',
      'Tax Clearance',
      'Professional Certifications',
      'Experience Certificates',
      'Engineering Licenses'
    ],
    renovation: [
      'Technical Specifications',
      'Financial Proposal',
      'Company Registration',
      'Tax Clearance',
      'Experience Certificates',
      'Portfolio of Previous Work',
      'Insurance Certificate'
    ],
    maintenance: [
      'Technical Specifications',
      'Financial Proposal',
      'Company Registration',
      'Tax Clearance',
      'Service Level Agreements',
      'Experience Certificates',
      'Equipment List'
    ]
  };

  get currentCategoryDocuments(): string[] {
    if (this.selectedCategory && this.categoryDocuments[this.selectedCategory]) {
      return this.categoryDocuments[this.selectedCategory];
    }
    return [];
  }

  get allPredefinedDocuments(): string[] {
    return this.predefinedDocuments;
  }

  onCategoryChange(): void {
    
    this.formData.requiredDocuments = [];
  }

  get isCategorySelected(): boolean {
    return !!this.selectedCategory;
  }

  predefinedDocuments = [
    'Technical Specifications',
    'Financial Proposal',
    'Company Registration',
    'Tax Clearance',
    'Experience Certificates',
    'Bank Statement',
    'Power of Attorney',
    'Letter of Intent'
  ];

  isValidForm(): boolean {
    this.formError = false;
    
    // Check required fields
    if (!this.formData.title || !this.formData.title.trim()) {
      return false;
    }
    if (!this.formData.description || !this.formData.description.trim()) {
      return false;
    }
    if (!this.formData.budget || this.formData.budget <= 0) {
      return false;
    }
    if (!this.formData.deadline) {
      return false;
    }
    if (!this.formData.openingDate) {
      return false;
    }
    if (!this.formData.location || !this.formData.location.trim()) {
      return false;
    }
    if (!this.selectedCategory) {
      return false;
    }
    if (!this.formData.userType) {
      return false;
    }
    
    // Documents are optional if not category selected
    return true;
  }

  onSubmit(): void {

      console.log('onSubmit called');
    console.log('Form validity:', this.isValidForm());
    console.log('Form data:', this.formData);
    console.log('Selected category:', this.selectedCategory);
    if (this.isValidForm()) {
      // Sync selectedCategory to formData.category before submitting
      this.formData.category = this.selectedCategory;
      
      // Convert requiredDocuments array to comma-separated string
      const submitData = {
        ...this.formData,
        requiredDocuments: this.formData.requiredDocuments.join(',')
      };
         
      console.log('Submitting data:', submitData);
      this.onSubmitForm.emit(submitData);
      this.resetForm();
    } else {
      this.formError = true;
      console.log('Form is invalid');
    }
  }

  private resetForm(): void {
    this.formData = {
      title: '',
      description: '',
      budget: null,
      deadline: '',
      openingDate: '',
      location: '',
      category: '',
      userType: '',
      contactNumber: '',
      isWhatsapp: false,
      comments: '',
      requiredDocuments: []
    };
    this.selectedCategory = '';
    this.customDocument = '';
    this.formError = false;
  }

  toggleDocument(doc: string): void {
    const index = this.formData.requiredDocuments.indexOf(doc);
    if (index > -1) {
      this.formData.requiredDocuments.splice(index, 1);
    } else {
      this.formData.requiredDocuments.push(doc);
    }
  }

  addCustomDocument(): void {
    if (this.customDocument.trim() && !this.formData.requiredDocuments.includes(this.customDocument.trim())) {
      this.formData.requiredDocuments.push(this.customDocument.trim());
      this.customDocument = '';
    }
  }

  removeDocument(index: number): void {
    this.formData.requiredDocuments.splice(index, 1);
  }

  // Get status preview based on deadline date
  getDeadlineStatus(): string {
    if (!this.formData.deadline) {
      return 'OPEN';
    }
    
    console.log('getDeadlineStatus called with deadline:', this.formData.deadline);
    
    // Parse the deadline date
    const deadlineStr = this.formData.deadline;
    const deadlineDate = new Date(deadlineStr);
    
    // Get current date at midnight for fair comparison
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    
    // Set deadline to midnight of that day for comparison
    const deadlineDay = new Date(deadlineDate.getFullYear(), deadlineDate.getMonth(), deadlineDate.getDate());
    
    console.log('Comparing deadlineDay:', deadlineDay, 'with today:', today);
    console.log('deadlineDay < today:', deadlineDay < today);
    
    if (deadlineDay < today) {
      return 'CLOSED';
    }
    return 'OPEN';
  }

  onDeadlineChange(): void {
 
  }
}
