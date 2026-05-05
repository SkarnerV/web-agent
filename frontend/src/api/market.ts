import { get, post, put, del } from './client'
import type {
  MarketItemVO,
  MarketItemDetailVO,
  PublishRequest,
  VisibilityUpdateRequest,
  ReviewCreateRequest,
  ReviewVO,
  MarketListParams,
  PageResult,
} from './types'

export function publishAsset(data: PublishRequest) {
  return post<MarketItemVO>('/market/publish', data)
}

export function updateVisibility(itemId: string, data: VisibilityUpdateRequest) {
  return put<MarketItemVO>(`/market/items/${itemId}/visibility`, data)
}

export function listMarketItems(params?: MarketListParams) {
  return get<PageResult<MarketItemVO>>('/market/items', params as Record<string, string | number | boolean | undefined>)
}

export function getMarketItem(itemId: string) {
  return get<MarketItemDetailVO>(`/market/items/${itemId}`)
}

export function getFeatured(type?: string) {
  return get<PageResult<MarketItemVO>>('/market/featured', { type })
}

export function addFavorite(itemId: string) {
  return post<void>(`/market/items/${itemId}/favorite`)
}

export function removeFavorite(itemId: string) {
  return del(`/market/items/${itemId}/favorite`)
}

export function createReview(itemId: string, data: ReviewCreateRequest) {
  return post<ReviewVO>(`/market/items/${itemId}/reviews`, data)
}

export function listReviews(itemId: string, page?: number, pageSize?: number) {
  return get<ReviewVO[]>(`/market/items/${itemId}/reviews`, {
    page: page ?? 1,
    page_size: pageSize ?? 20,
  })
}

export function importMarketItem(itemId: string) {
  return post<Record<string, unknown>>(`/market/items/${itemId}/import`)
}
