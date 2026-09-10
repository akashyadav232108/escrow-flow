import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import BackButton from './BackButton';
import NotificationBell from './NotificationBell';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { logout } from '../store/slices/authSlice';
import { isAdminRole, isSuperAdminRole } from '../utils/roles';

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `sidebar-link${isActive ? ' sidebar-link-active' : ''}`;

export default function Layout() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const user = useAppSelector((state) => state.auth.user);
  const [navOpen, setNavOpen] = useState(false);
  const [isMobile, setIsMobile] = useState(false);
  const admin = isAdminRole(user?.role);
  const superAdmin = isSuperAdminRole(user?.role);

  useEffect(() => {
    const mq = window.matchMedia('(max-width: 768px)');
    const update = () => setIsMobile(mq.matches);
    update();
    mq.addEventListener('change', update);
    return () => mq.removeEventListener('change', update);
  }, []);

  const closeNav = () => setNavOpen(false);

  const handleLogout = () => {
    closeNav();
    dispatch(logout());
    navigate('/login');
  };

  const initial = user?.name?.trim().charAt(0).toUpperCase() ?? '?';

  return (
    <div className="app-shell">
      <header className="mobile-topbar">
        <div className="sidebar-brand">
          <span className="auth-brand-mark">E</span>
          Escrow Flow
        </div>
        <div className="mobile-topbar-actions">
          {isMobile && <NotificationBell />}
          <button
            type="button"
            className="hamburger-btn"
            aria-label={navOpen ? 'Close navigation' : 'Open navigation'}
            aria-expanded={navOpen}
            onClick={() => setNavOpen((prev) => !prev)}
          >
            <span />
            <span />
            <span />
          </button>
        </div>
      </header>

      {navOpen && <div className="sidebar-backdrop" onClick={closeNav} />}

      <aside className={`sidebar${navOpen ? ' sidebar-open' : ''}`}>
        <div className="sidebar-brand-row">
          <div className="sidebar-brand">
            <span className="auth-brand-mark">E</span>
            Escrow Flow
          </div>
          {!isMobile && (
            <div className="sidebar-bell-desktop">
              <NotificationBell />
            </div>
          )}
        </div>
        <nav className="sidebar-nav">
          {admin ? (
            <>
              <NavLink to="/admin" end className={navLinkClass} onClick={closeNav}>
                Admin
              </NavLink>
              <NavLink to="/admin/disputes" className={navLinkClass} onClick={closeNav}>
                Disputes
              </NavLink>
              <NavLink to="/admin/users" className={navLinkClass} onClick={closeNav}>
                Users
              </NavLink>
              {superAdmin && (
                <NavLink to="/admin/admins" className={navLinkClass} onClick={closeNav}>
                  Admins
                </NavLink>
              )}
            </>
          ) : (
            <>
              <NavLink to="/" end className={navLinkClass} onClick={closeNav}>
                Dashboard
              </NavLink>
              <NavLink to="/wallet" className={navLinkClass} onClick={closeNav}>
                Wallet
              </NavLink>
            </>
          )}
        </nav>
        <div className="sidebar-footer">
          {user && (
            <Link to="/profile" className="sidebar-user" onClick={closeNav}>
              <span className="sidebar-user-avatar">{initial}</span>
              <div>
                <div className="sidebar-user-name">{user.name}</div>
                <div className="sidebar-user-role">{user.role.toLowerCase()}</div>
              </div>
            </Link>
          )}
          <button type="button" className="sidebar-logout" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </aside>
      <div className="app-main">
        <main className="app-content">
          <BackButton />
          <Outlet />
        </main>
      </div>
    </div>
  );
}
