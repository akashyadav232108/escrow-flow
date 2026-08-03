import api from './client';
import type { CreateReviewInput, RatingSummary, Review, ReviewPage } from '../types';

export const reviewApi = {
  create: (projectId: number, payload: CreateReviewInput) =>
    api.post<Review>(`/projects/${projectId}/reviews`, payload).then((res) => res.data),

  listForUser: (userId: number, page = 0, size = 20) =>
    api
      .get<ReviewPage>(`/users/${userId}/reviews`, { params: { page, size } })
      .then((res) => res.data),

  ratingSummary: (userId: number) =>
    api.get<RatingSummary>(`/users/${userId}/rating-summary`).then((res) => res.data),
};
