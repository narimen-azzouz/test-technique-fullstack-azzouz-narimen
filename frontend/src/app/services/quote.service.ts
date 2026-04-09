import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { QuoteRequest, QuoteResponse } from '../models/quote.model';

/**
 * Service for managing quotes
 * This service handles all API communication with the backend pricing engine
 */
@Injectable({
  providedIn: 'root'
})
export class QuoteService {
  private readonly apiUrl = environment.apiUrl;
  private readonly endpoint = '/quotes';

  constructor(private http: HttpClient) {}

  /**
   * Create a new quote
   * POST /api/quotes
   *
   * @param request Quote request data
   * @returns Observable of the created quote response with calculated pricing
   *
   */
  createQuote(request: QuoteRequest): Observable<QuoteResponse> {
    return this.http
      .post<QuoteResponse>(`${this.apiUrl}${this.endpoint}`, request)
      .pipe(catchError((error) => this.handleError(error)));
  }

  /**
   * Get a single quote by ID
   * GET /api/quotes/:id
   *
   * @param id Quote ID
   * @returns Observable of the quote details
   *
   */
  getQuote(id: number): Observable<QuoteResponse> {
    return this.http
      .get<QuoteResponse>(`${this.apiUrl}${this.endpoint}/${id}`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  /**
   * Get all quotes with optional filtering
   * GET /api/quotes?productId=X&minPrice=Y
   *
   * @param filters Optional filter criteria
   * @param filters.productId Filter by product ID
   * @param filters.minPrice Filter by minimum price
   * @returns Observable of array of quotes
   *
   */
  getQuotes(filters?: { productId?: number; minPrice?: number }): Observable<QuoteResponse[]> {
    let params = new HttpParams();

    if (filters?.productId !== undefined && filters.productId !== null) {
      params = params.set('productId', String(filters.productId));
    }
    if (filters?.minPrice !== undefined && filters.minPrice !== null) {
      params = params.set('minPrice', String(filters.minPrice));
    }

    return this.http
      .get<QuoteResponse[]>(`${this.apiUrl}${this.endpoint}`, { params })
      .pipe(catchError((error) => this.handleError(error)));
  }

  /**
   * Handle HTTP errors
   *
   * @param error The error object from HttpClient
   * @returns Observable that throws a user-friendly error message
   *
   */
  private handleError(error: any): Observable<never> {
    console.error('Quote service error:', error);

    const backendMessage =
      error?.error?.message ||
      error?.error?.error ||
      error?.message ||
      'Failed to process quote';

    return throwError(() => new Error(String(backendMessage)));
  }
}
