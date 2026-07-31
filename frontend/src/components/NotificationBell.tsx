import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { notificationApi } from '../api/notificationApi';
import type { NotificationItem } from '../types';
import { isAdminRole } from '../utils/roles';
import { useAppSelector } from '../store/hooks';

const POLL_MS = 20_000;

function formatRelativeTime(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime();
  const minutes = Math.floor(diffMs / 60_000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

function pathForNotification(
  item: NotificationItem,
  admin: boolean,
): string | null {
  if (!item.referenceType || item.referenceId == null) return null;
  if (item.referenceType === 'PROJECT') return `/projects/${item.referenceId}`;
  if (item.referenceType === 'DISPUTE' && admin) return `/admin/disputes/${item.referenceId}`;
  return null;
}

export default function NotificationBell() {
  const navigate = useNavigate();
  const user = useAppSelector((state) => state.auth.user);
  const admin = isAdminRole(user?.role);
  const [open, setOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [loadingList, setLoadingList] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);

  const refreshUnread = useCallback(async () => {
    try {
      const res = await notificationApi.unreadCount();
      setUnreadCount(res.unreadCount);
    } catch {
      // ignore transient poll errors
    }
  }, []);

  const loadList = useCallback(async () => {
    setLoadingList(true);
    try {
      const page = await notificationApi.list(0, 15);
      setItems(page.content);
    } catch {
      setItems([]);
    } finally {
      setLoadingList(false);
    }
  }, []);

  useEffect(() => {
    void refreshUnread();
    const id = window.setInterval(() => {
      void refreshUnread();
    }, POLL_MS);
    return () => window.clearInterval(id);
  }, [refreshUnread]);

  useEffect(() => {
    if (!open) return;
    void loadList();
  }, [open, loadList]);

  useEffect(() => {
    if (!open) return;
    const onPointerDown = (event: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', onPointerDown);
    return () => document.removeEventListener('mousedown', onPointerDown);
  }, [open]);

  const handleToggle = () => {
    setOpen((prev) => !prev);
  };

  const handleMarkAll = async () => {
    try {
      await notificationApi.markAllRead();
      setUnreadCount(0);
      setItems((prev) => prev.map((item) => ({ ...item, read: true })));
    } catch {
      // keep prior state
    }
  };

  const handleItemClick = async (item: NotificationItem) => {
    if (!item.read) {
      try {
        await notificationApi.markRead(item.id);
        setItems((prev) =>
          prev.map((row) => (row.id === item.id ? { ...row, read: true } : row)),
        );
        setUnreadCount((count) => Math.max(0, count - 1));
      } catch {
        // still allow navigation
      }
    }

    const path = pathForNotification(item, admin);
    setOpen(false);
    if (path) navigate(path);
  };

  const badgeLabel = unreadCount > 99 ? '99+' : String(unreadCount);

  return (
    <div className="notification-bell" ref={rootRef}>
      <button
        type="button"
        className="notification-bell-btn"
        aria-label={unreadCount > 0 ? `Notifications, ${unreadCount} unread` : 'Notifications'}
        aria-expanded={open}
        onClick={handleToggle}
      >
        <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
          <path
            fill="currentColor"
            d="M12 22a2.5 2.5 0 0 0 2.45-2h-4.9A2.5 2.5 0 0 0 12 22Zm7-5V11a7 7 0 1 0-14 0v6l-2 2v1h18v-1l-2-2Z"
          />
        </svg>
        {unreadCount > 0 && <span className="notification-badge">{badgeLabel}</span>}
      </button>

      {open && (
        <div className="notification-panel" role="dialog" aria-label="Notifications">
          <div className="notification-panel-header">
            <span>Notifications</span>
            {unreadCount > 0 && (
              <button type="button" className="notification-mark-all" onClick={handleMarkAll}>
                Mark all read
              </button>
            )}
          </div>
          <div className="notification-panel-body">
            {loadingList && <p className="notification-empty">Loading…</p>}
            {!loadingList && items.length === 0 && (
              <p className="notification-empty">No notifications yet.</p>
            )}
            {!loadingList &&
              items.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  className={`notification-item${item.read ? '' : ' notification-item-unread'}`}
                  onClick={() => void handleItemClick(item)}
                >
                  <div className="notification-item-title">{item.title}</div>
                  <div className="notification-item-message">{item.message}</div>
                  <div className="notification-item-meta">{formatRelativeTime(item.createdAt)}</div>
                </button>
              ))}
          </div>
        </div>
      )}
    </div>
  );
}
