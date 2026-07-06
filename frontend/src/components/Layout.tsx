import { useState } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { logout } from '../store/slices/authSlice';

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `sidebar-link${isActive ? ' sidebar-link-active' : ''}`;

export default function Layout() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const user = useAppSelector((state) => state.auth.user);
  const [navOpen, setNavOpen] = useState(false);

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
      </header>

      {navOpen && <div className="sidebar-backdrop" onClick={closeNav} />}

      <aside className={`sidebar${navOpen ? ' sidebar-open' : ''}`}>
        <div className="sidebar-brand">
          <span className="auth-brand-mark">E</span>
          Escrow Flow
        </div>
        <nav className="sidebar-nav">
          <NavLink to="/" end className={navLinkClass} onClick={closeNav}>
            Dashboard
          </NavLink>
          <NavLink to="/wallet" className={navLinkClass} onClick={closeNav}>
            Wallet
          </NavLink>
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
          <Outlet />
        </main>
      </div>
    </div>
  );
}
