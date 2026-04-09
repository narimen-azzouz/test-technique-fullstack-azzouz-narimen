import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { QuoteService } from '../../services/quote.service';
import { QuoteResponse } from '../../models/quote.model';

/**
 * Component for displaying the details of a single quote
 */
@Component({
  selector: 'app-quote-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './quote-detail.component.html',
  styleUrl: './quote-detail.component.css'
})
export class QuoteDetailComponent implements OnInit {
  quote: QuoteResponse | null = null;
  loading = false;
  errorMessage: string | null = null;

  constructor(
    private quoteService: QuoteService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const rawId = this.route.snapshot.paramMap.get('id');
    const id = rawId ? Number(rawId) : NaN;

    if (!rawId || Number.isNaN(id)) {
      this.errorMessage = 'Invalid quote id';
      this.quote = null;
      return;
    }

    this.loading = true;
    this.errorMessage = null;
    this.quoteService.getQuote(id).subscribe({
      next: (quote) => {
        this.quote = quote;
        this.loading = false;
      },
      error: (err: Error) => {
        this.loading = false;
        this.errorMessage = err.message || 'Failed to load quote';
        this.quote = null;
      }
    });
  }
}
