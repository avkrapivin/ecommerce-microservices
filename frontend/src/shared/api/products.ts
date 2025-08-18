import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// Create axios instance
const api = axios.create({
  baseURL: API_BASE_URL,
});

export interface Product {
  id: number;
  name: string;
  description: string;
  price: number; // Price in cents (integer)
  images: ProductImage[];
  categoryId: number;
  categoryName: string;
  stockQuantity: number;
  sku: string;
  active: boolean;
  reviews: ProductReview[];
  specifications: ProductSpecification[];
  createdAt: string;
  updatedAt: string;
}

export interface ProductImage {
  id: number;
  productId: number;
  imageUrl: string;
  fileName: string;
  fileType: string;
  fileSize: number;
  isMain: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProductReview {
  id: number;
  productId: number;
  userId: number;
  rating: number;
  comment: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProductSpecification {
  id: number;
  productId: number;
  name: string;
  value: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProductListResponse {
  content: Product[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface Category {
  id: number;
  name: string;
  description?: string;
}

// Products API functions
export const productsApi = {
  async getProducts(
    page: number = 0, 
    size: number = 20,
    search?: string,
    categoryId?: number,
    minPrice?: number,
    maxPrice?: number,
    sortBy?: string,
    sortDirection?: string
  ): Promise<ProductListResponse> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    
    if (search) params.append('search', search);
    if (categoryId) params.append('categoryId', categoryId.toString());
    if (minPrice) params.append('minPrice', minPrice.toString());
    if (maxPrice) params.append('maxPrice', maxPrice.toString());
    if (sortBy) params.append('sortBy', sortBy);
    if (sortDirection) params.append('sortDirection', sortDirection);
    
    const response = await api.get(`/products?${params.toString()}`);
    return response.data;
  },

  async getProduct(id: number): Promise<Product> {
    const response = await api.get(`/products/${id}`);
    return response.data;
  },

  async getProductsByCategory(categoryId: number, page: number = 0, size: number = 20): Promise<ProductListResponse> {
    const response = await api.get(`/products/category/${categoryId}?page=${page}&size=${size}`);
    return response.data;
  },

  async searchProducts(query: string, page: number = 0, size: number = 20): Promise<ProductListResponse> {
    const response = await api.get(`/products/search?q=${encodeURIComponent(query)}&page=${page}&size=${size}`);
    return response.data;
  },

  async getCategories(): Promise<Category[]> {
    const response = await api.get('/categories');
    return response.data;
  },

  async getCategory(id: number): Promise<Category> {
    const response = await api.get(`/categories/${id}`);
    return response.data;
  },
};

// Export individual functions for backward compatibility
export const {
  getProducts,
  getProduct,
  getProductsByCategory,
  searchProducts,
  getCategories,
  getCategory,
} = productsApi;
