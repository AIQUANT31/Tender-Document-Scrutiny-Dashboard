import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pagination.component.html',
  styleUrl: './pagination.component.css'
})
export class PaginationComponent {
  
  @Input() totalItems: number = 0;
  
 
  @Input() itemsPerPage: number = 10;
  
 
  @Input() currentPage: number = 1;
 
  @Output() pageChange = new EventEmitter<number>();


  get totalPages(): number {
    return Math.ceil(this.totalItems / this.itemsPerPage);
  }

 
  get pageNumbers(): number[] {
    const pages: number[] = [];
    for (let i = 1; i <= this.totalPages; i++) {
      pages.push(i);
    }
    return pages;
  }

  get isPreviousDisabled(): boolean {
    return this.currentPage <= 1;
  }


  get isNextDisabled(): boolean {
    return this.currentPage >= this.totalPages;
  }

  get startIndex(): number {
    return (this.currentPage - 1) * this.itemsPerPage + 1;
  }

  
  get endIndex(): number {
    const end = this.currentPage * this.itemsPerPage;
    return end > this.totalItems ? this.totalItems : end;
  }

  
  onPageClick(page: number): void {
   
    if (page === this.currentPage) {
      return;
    }
    
  
    if (page < 1) {
      page = 1;
    }
    
    
    if (page > this.totalPages) {
      page = this.totalPages;
    }
    
    
    this.pageChange.emit(page);
  }

  
  goToPreviousPage(): void {
    if (!this.isPreviousDisabled) {
      this.onPageClick(this.currentPage - 1);
    }
  }

  
  goToNextPage(): void {
    if (!this.isNextDisabled) {
      this.onPageClick(this.currentPage + 1);
    }
  }

  
  goToFirstPage(): void {
    this.onPageClick(1);
  }

  goToLastPage(): void {
    this.onPageClick(this.totalPages);
  }
}
