import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { QuoteService } from '../../services/quote.service';
import { ProductService } from '../../services/product.service';
import { QuoteResponse } from '../../models/quote.model';
import { Product } from '../../models/product.model';

/**
 * Component for displaying a list of all quotes
 */
@Component({
  selector: 'app-quote-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './quote-list.component.html',
  styleUrl: './quote-list.component.css'
})
export class QuoteListComponent implements OnInit {
  quotes: QuoteResponse[] = [];
  filteredQuotes: QuoteResponse[] = [];
  products: Product[] = [];
  loading = false;
  errorMessage: string | null = null;

  // Filter state
  selectedProductId: number | null = null;
  minPrice: number | null = null;

  // Sort state
  sortField: 'date' | 'price' = 'date';
  sortDirection: 'asc' | 'desc' = 'desc';

  constructor(
    private quoteService: QuoteService,
    private productService: ProductService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadProducts();
    this.loadQuotes();
  }

  private loadProducts(): void {
    this.productService.getProducts().subscribe({
      next: (products) => {
        this.products = products;
      },
      error: () => {
        this.products = [];
      }
    });
  }

  private loadQuotes(filters?: { productId?: number; minPrice?: number }): void {
    this.loading = true;
    this.errorMessage = null;

    this.quoteService.getQuotes(filters).subscribe({
      next: (quotes) => {
        this.quotes = quotes;
        this.filteredQuotes = quotes;
        this.loading = false;
        this.sortQuotes();
      },
      error: (err: Error) => {
        this.loading = false;
        this.errorMessage = err.message || 'Failed to load quotes';
        this.filteredQuotes = [];
      }
    });
  }

  /**
   * Apply filters to the quotes
   */
  applyFilters(): void {
    const filters: { productId?: number; minPrice?: number } = {};

    if (this.selectedProductId !== null) {
      filters.productId = this.selectedProductId;
    }
    if (this.minPrice !== null && !Number.isNaN(this.minPrice)) {
      filters.minPrice = this.minPrice;
    }

    this.loadQuotes(filters);
  }

  /**
   * Reset all filters and reload all quotes
   */
  resetFilters(): void {
    this.selectedProductId = null;
    this.minPrice = null;
    this.loadQuotes();
  }

  /**
   * Toggle sort direction or change sort field
   */
  changeSortField(field: 'date' | 'price'): void {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'asc';
    }
    this.sortQuotes();
  }

  /**
   * Sort filteredQuotes in memory
   */
  private sortQuotes(): void {
    const direction = this.sortDirection === 'asc' ? 1 : -1;

    this.filteredQuotes.sort((a, b) => {
      if (this.sortField === 'price') {
        return (a.finalPrice - b.finalPrice) * direction;
      }

      const aTime = new Date(a.createdAt).getTime();
      const bTime = new Date(b.createdAt).getTime();
      return (aTime - bTime) * direction;
    });
  }

  /**
   * Navigate to quote detail page
   */
  viewQuote(id: number): void {
    this.router.navigate(['/quotes', id]);
  }
}
