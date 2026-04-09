import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Product } from '../models/product.model';

/**
 * Service for managing products (insurance products)
 */
@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private readonly apiUrl = environment.apiUrl;
  private readonly endpoint = '/products';

  constructor(private http: HttpClient) {}

  /**
   * Get all available products
   * GET /api/products
   *
   * @returns Observable of array of products
   *
   */
  getProducts(): Observable<Product[]> {
    return this.http
      .get<Product[]>(`${this.apiUrl}${this.endpoint}`)
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
    console.error('Product service error:', error);
    return throwError(() => new Error('Failed to load products'));
  }
}
