import { Component, Input, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { BidderFormData, DEFAULT_BIDDER_FORM_DATA, BidderData } from '../../models/bidder.model';

@Component({
  selector: 'app-bidder-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './bidder-form.component.html',
  styleUrl: './bidder-form.component.css'
})
export class BidderFormComponent {
  @Input() showBidderForm: boolean = false;
  @Input() isEditing: boolean = false;
  @Input() formData: BidderFormData = DEFAULT_BIDDER_FORM_DATA;
  @Input() userId: number | null = null;
  @Input() selectedBidderId: number | null = null;
  
  @Output() cancel = new EventEmitter<void>();
  @Output() submit = new EventEmitter<BidderFormData>();
  @Output() formClose = new EventEmitter<void>();

  isSubmitting = false;

  constructor(private cdr: ChangeDetectorRef) {}

  onCancel() {
    this.cancel.emit();
    this.formClose.emit();
  }

  onSubmit() {
    // Validate form
    if (!this.formData.companyName || !this.formData.email) {
      alert('Please fill in all required fields');
      return;
    }
    
    // Emit the form data to parent
    this.submit.emit(this.formData);
  }

  closeForm() {
    this.formClose.emit();
  }
}
