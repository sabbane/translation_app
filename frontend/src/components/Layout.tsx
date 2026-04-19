import React from 'react';
import { Link, useNavigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Layout.css';

const Layout: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="layout">
      <nav className="navbar">
        <div className="container nav-content">
          <Link to="/" className="logo">
            <span className="logo-icon">🌐</span>
            <span className="logo-text">TranslateApp</span>
          </Link>
          <div className="nav-links">
            <Link to="/">Dashboard</Link>
            <Link to="/editor">Editor</Link>
            {user?.role === 'ADMIN' && (
              <Link to="/users">User Management</Link>
            )}
          </div>
          {user && (
            <div className="user-menu">
              <div className="user-info">
                <span className="username">{user.username}</span>
                <span className={`role-tag role-${user.role.toLowerCase()}`}>{user.role}</span>
              </div>
              <button onClick={handleLogout} className="btn-logout">
                Logout
              </button>
            </div>
          )}
        </div>
      </nav>
      <main className="container main-content">
        <Outlet />
      </main>
      <footer className="footer">
        <div className="container">
          <p>&copy; 2025 Translation App. Built with React & Spring Boot.</p>
        </div>
      </footer>
    </div>
  );
};

export default Layout;
