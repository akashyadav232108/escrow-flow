import api from './client';
import type { NotificationItem, NotificationPage, UnreadCountResponse } from '../types';

export const notificationApi = {
  list: (page = 0, size = 20) =>
    api
      .get<NotificationPage>('/notifications', { params: { page, size } })
      .then((res) => res.data),

  unreadCount: () =>
    api.get<UnreadCountResponse>('/notifications/unread-count').then((res) => res.data),

  markRead: (id: number) =>
    api.post<NotificationItem>(`/notifications/${id}/read`).then((res) => res.data),

  markAllRead: () =>
    api.post<UnreadCountResponse>('/notifications/read-all').then((res) => res.data),
};
