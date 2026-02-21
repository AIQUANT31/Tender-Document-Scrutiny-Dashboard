import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ValidationResponse {
  valid: boolean;
  message: string;
  matchedDocuments: string[];
  missingDocuments: string[];
  warnings: string[];
  duplicateDocuments: string[];
}

@Injectable({
  providedIn: 'root'
})
export class DocumentValidationService {
  private apiUrl = 'http://localhost:8080/api/bids';

  // Document type keywords mapping for client-side validation
  private documentKeywords: { [key: string]: string[] } = {
    'AADHAR': ['aadhar', 'aadhaar', 'uidai', 'uid', 'aadhar card', 'aadhaar card', 'unique identification'],
    'PAN': ['pan', 'permanent account', 'income tax', 'pan card', 'pan number', 'income tax department'],
    'GST': ['gst', 'goods and services', 'gstin'],
    'TENDER': ['tender', 'bid document', 'proposal'],
    'COMPANY_REGISTRATION': ['company registration', 'moa', 'aoa', 'incorporation'],
    'POWER_OF_ATTORNEY': ['power of attorney', 'poa', 'authorization'],
    'BANK_STATEMENT': ['bank statement', 'bank account'],
    'AUDITED_FINANCIALS': ['audited', 'financial statements', 'balance sheet'],
    'WORK_EXPERIENCE': ['work experience', 'completed projects'],
    'TECHNICAL_CAPABILITY': ['technical', 'capability statement'],
    'QUALITY_CERTIFICATE': ['quality', 'iso', 'certification'],
    'EMD': ['emd', 'earnest money', 'security deposit', 'bid security'],
    'TENDER_FEE': ['tender fee', 'document fee'],
    'AFFIDAVIT': ['affidavit', 'declaration', 'undertaking'],
    'NATIONALITY': ['nationality', 'citizenship'],
    'REGISTRATION': ['registration', 'license', 'permit']
  };

  /**
   * Validate document CONTENT using OCR - checks actual PDF text!
   * This is the proper rule-based validation you requested
   */
  validateDocumentContent(requiredDocuments: string[], files: File[]): Observable<ValidationResponse> {
    const formData = new FormData();
    
    // Add required documents as comma-separated string
    formData.append('requiredDocuments', requiredDocuments.join(','));
    
    // Add each file
    for (const file of files) {
      formData.append('files', file);
    }
    
    return this.http.post<ValidationResponse>(
      `${this.apiUrl}/validate-content`, 
      formData
    );
  }

  /**
   * Validate with rules - combines filename and content validation
   * Returns confidence scores
   */
  validateWithRules(requiredDocuments: string[], files: File[]): Observable<ValidationResponse> {
    const formData = new FormData();
    formData.append('requiredDocuments', requiredDocuments.join(','));
    
    for (const file of files) {
      formData.append('files', file);
    }
    
    return this.http.post<ValidationResponse>(
      `${this.apiUrl}/validate-with-rules`, 
      formData
    );
  }

  constructor(private http: HttpClient) {}

  /**
   * Validate documents using backend API
   */
  validateDocuments(requiredDocuments: string[], uploadedFileNames: string[]): Observable<ValidationResponse> {
    const request = {
      requiredDocuments: requiredDocuments,
      uploadedFileNames: uploadedFileNames
    };
    return this.http.post<ValidationResponse>(`${this.apiUrl}/validate-documents`, request);
  }

  /**
   * Validate documents using fuzzy matching (more lenient)
   */
  validateDocumentsFuzzy(requiredDocuments: string[], uploadedFileNames: string[]): Observable<ValidationResponse> {
    const request = {
      requiredDocuments: requiredDocuments,
      uploadedFileNames: uploadedFileNames
    };
    return this.http.post<ValidationResponse>(`${this.apiUrl}/validate-documents-fuzzy`, request);
  }

  /**
   * Client-side validation using keyword matching
   * This provides immediate feedback without API call
   */
  validateDocumentsClientSide(requiredDocuments: string[], uploadedFileNames: string[]): ValidationResponse {
    const response: ValidationResponse = {
      valid: true,
      message: '',
      matchedDocuments: [],
      missingDocuments: [],
      warnings: [],
      duplicateDocuments: []
    };

    if (!requiredDocuments || requiredDocuments.length === 0) {
      response.message = 'No required documents specified. Validation passed.';
      return response;
    }

    if (!uploadedFileNames || uploadedFileNames.length === 0) {
      response.valid = false;
      response.missingDocuments = [...requiredDocuments];
      response.message = 'No documents uploaded. Required: ' + requiredDocuments.join(', ');
      return response;
    }

    // Check each required document against uploaded files
    for (const requiredDoc of requiredDocuments) {
      const requiredDocLower = requiredDoc.toLowerCase().trim();
      let found = false;

      for (const uploadedFile of uploadedFileNames) {
        const fileNameLower = uploadedFile.toLowerCase().trim();
        
        if (this.matchesRequiredDocument(requiredDocLower, fileNameLower)) {
          response.matchedDocuments.push(`${requiredDoc} -> ${uploadedFile}`);
          found = true;
          break;
        }
      }

      if (!found) {
        response.missingDocuments.push(requiredDoc);
      }
    }

    if (response.missingDocuments.length > 0) {
      response.valid = false;
      response.message = 'Missing required documents: ' + response.missingDocuments.join(', ');
    } else {
      response.message = 'All required documents validated successfully!';
    }

    // Add warnings for extra documents
    if (response.matchedDocuments.length > requiredDocuments.length) {
      response.warnings.push('Extra documents uploaded beyond requirements');
    }

    return response;
  }

  /**
   * Check if an uploaded file matches a required document type
   */
  private matchesRequiredDocument(requiredDoc: string, fileName: string): boolean {
    // Direct match
    if (fileName.includes(requiredDoc)) {
      return true;
    }

    // Check against predefined keywords
    for (const keywords of Object.values(this.documentKeywords)) {
      for (const keyword of keywords) {
        if (requiredDoc.includes(keyword) && fileName.includes(keyword)) {
          return true;
        }
      }
    }

    // Check individual words from required doc
    const words = requiredDoc.split(/[\s,_-]+/);
    for (const word of words) {
      if (word.length > 3 && fileName.includes(word)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Get supported document types
   */
  getSupportedDocumentTypes(): string[] {
    return Object.keys(this.documentKeywords);
  }

  /**
   * Get keywords for a specific document type
   */
  getKeywordsForDocumentType(documentType: string): string[] {
    return this.documentKeywords[documentType.toUpperCase()] || [];
  }
}
