import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import RequireAdmin from './components/RequireAdmin';
import RequireAuth from './components/RequireAuth';
import AdminAdminsPage from './pages/AdminAdminsPage';
import AdminDashboardPage from './pages/AdminDashboardPage';
import AdminDisputeDetailPage from './pages/AdminDisputeDetailPage';
import AdminDisputesPage from './pages/AdminDisputesPage';
import AdminUserDetailPage from './pages/AdminUserDetailPage';
import AdminUsersPage from './pages/AdminUsersPage';
import DashboardPage from './pages/DashboardPage';
import LoginPage from './pages/LoginPage';
import ProfilePage from './pages/ProfilePage';
import ProjectDetailPage from './pages/ProjectDetailPage';
import SignupPage from './pages/SignupPage';
import WalletPage from './pages/WalletPage';
import './App.css';

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route
        element={
          <RequireAuth>
            <Layout />
          </RequireAuth>
        }
      >
        <Route path="/" element={<DashboardPage />} />
        <Route path="/projects/:id" element={<ProjectDetailPage />} />
        <Route path="/wallet" element={<WalletPage />} />
        <Route path="/profile" element={<ProfilePage />} />

        <Route
          path="/admin"
          element={
            <RequireAdmin>
              <AdminDashboardPage />
            </RequireAdmin>
          }
        />
        <Route
          path="/admin/disputes"
          element={
            <RequireAdmin>
              <AdminDisputesPage />
            </RequireAdmin>
          }
        />
        <Route
          path="/admin/disputes/:id"
          element={
            <RequireAdmin>
              <AdminDisputeDetailPage />
            </RequireAdmin>
          }
        />
        <Route
          path="/admin/users"
          element={
            <RequireAdmin>
              <AdminUsersPage />
            </RequireAdmin>
          }
        />
        <Route
          path="/admin/users/:id"
          element={
            <RequireAdmin>
              <AdminUserDetailPage />
            </RequireAdmin>
          }
        />
        <Route
          path="/admin/admins"
          element={
            <RequireAdmin>
              <AdminAdminsPage />
            </RequireAdmin>
          }
        />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
