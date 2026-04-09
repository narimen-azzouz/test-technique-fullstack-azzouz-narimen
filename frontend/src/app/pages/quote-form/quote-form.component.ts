import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { QuoteService } from '../../services/quote.service';
import { ProductService } from '../../services/product.service';
import { Product } from '../../models/product.model';

/**
 * Available zones with their codes (must match backend data.sql)
 */
const ZONES = [
  { code: 'TUN', name: 'Grand Tunis' },
  { code: 'SFX', name: 'Sfax' },
  { code: 'SOU', name: 'Sousse' }
];

/**
 * Component for creating a new quote
 */
@Component({
  selector: 'app-quote-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './quote-form.component.html',
  styleUrl: './quote-form.component.css'
})
export class QuoteFormComponent implements OnInit {
  form: FormGroup;
  products: Product[] = [];
  zones = ZONES;
  loading = false;
  submitted = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private quoteService: QuoteService,
    private productService: ProductService,
    private router: Router
  ) {
    this.form = this.fb.group({
      clientName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      productId: ['', [Validators.required]],
      zoneCode: ['', [Validators.required]],
      clientAge: ['', [Validators.required, Validators.min(18), Validators.max(99)]]
    });
  }

  ngOnInit(): void {
    this.loading = true;
    this.errorMessage = null;
    this.productService.getProducts().subscribe({
      next: (products) => {
        this.products = products;
        this.loading = false;
      },
      error: (err: Error) => {
        this.errorMessage = err.message || 'Failed to load products';
        this.loading = false;
      }
    });
  }

  /**
   * Submit the form
   */
  onSubmit(): void {
    this.submitted = true;

    this.errorMessage = null;
    this.successMessage = null;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const request = {
      clientName: String(this.form.value.clientName).trim(),
      productId: Number(this.form.value.productId),
      zoneCode: String(this.form.value.zoneCode),
      clientAge: Number(this.form.value.clientAge)
    };

    this.loading = true;
    this.quoteService.createQuote(request).subscribe({
      next: (createdQuote) => {
        this.loading = false;
        this.router.navigate(['/quotes', createdQuote.quoteId]);
      },
      error: (err: Error) => {
        this.loading = false;
        this.errorMessage = err.message || 'Failed to create quote';
      }
    });
  }

  /**
   * Check if a form field has an error (provided helper)
   */
  hasError(fieldName: string, errorType: string): boolean {
    const field = this.form.get(fieldName);
    return !!(field && field.hasError(errorType) && (field.dirty || field.touched || this.submitted));
  }

  /**
   * Check if a form field is invalid (provided helper)
   */
  isFieldInvalid(fieldName: string): boolean {
    const field = this.form.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched || this.submitted));
  }

  /**
   * Get error message for a field (provided helper)
   */
  getErrorMessage(fieldName: string): string {
    const field = this.form.get(fieldName);
    if (!field || !field.errors) return '';

    if (field.hasError('required')) return `This field is required`;
    if (field.hasError('minlength')) return `Minimum ${field.errors['minlength'].requiredLength} characters`;
    if (field.hasError('min')) return `Minimum value is ${field.errors['min'].min}`;
    if (field.hasError('max')) return `Maximum value is ${field.errors['max'].max}`;

    return 'Invalid input';
  }
}
