import { http } from './http';

export interface CategoryDto {
  id: number;
  name: string;
  parentId?: number | null;
}

export interface ProductImageDto {
  id: number;
  productId?: number;
  imageUrl: string;
  isMain?: boolean;
}

export interface ProductDto {
  id: number;
  name: string;
  price: number;
  description?: string;
  images?: ProductImageDto[];
  // Extend with more fields as needed
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // zero-based page index
}

export interface ProductFilter {
  search?: string;
  categoryId?: number;
  minPrice?: number;
  maxPrice?: number;
  specifications?: string[];
  sortBy?: string;
  sortDirection?: 'asc' | 'desc';
  page?: number; // zero-based
  size?: number;
}

export async function getProducts(filter: ProductFilter, signal?: AbortSignal) {
  const { data } = await http.get<PageResponse<ProductDto>>('/products', {
    params: {
      search: filter.search || undefined,
      categoryId: filter.categoryId || undefined,
      minPrice: filter.minPrice || undefined,
      maxPrice: filter.maxPrice || undefined,
      specifications:
        filter.specifications && filter.specifications.length > 0
          ? filter.specifications
          : undefined,
      sortBy: filter.sortBy || undefined,
      sortDirection: filter.sortDirection || undefined,
      page: filter.page ?? 0,
      size: filter.size ?? 24,
    },
    signal,
  });
  return data;
}

export async function getCategories() {
  const { data } = await http.get<CategoryDto[]>('/categories');
  return data;
}

export async function getProductById(id: number) {
  const { data } = await http.get<ProductDto>(`/products/${id}`);
  return data;
}

export async function getProductImages(productId: number) {
  const { data } = await http.get<ProductImageDto[]>(`/products/${productId}/images`);
  return data;
}

export interface ProductReviewDto {
  id: number;
  rating: number; // 1-5
  comment?: string;
  userName?: string;
  createdAt?: string;
}

export async function getProductReviews(productId: number) {
  const { data } = await http.get<ProductReviewDto[]>(`/products/${productId}/reviews`);
  return data;
}
