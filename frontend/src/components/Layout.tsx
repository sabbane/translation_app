import React from 'react';
import { Link, useNavigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTranslation } from 'react-i18next';
import LanguageSwitcher from './LanguageSwitcher';
import UserManualModal from './UserManualModal';
import './Layout.css';

const Layout: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [showManual, setShowManual] = React.useState(false);
  const manualProcessed = React.useRef(false);

  React.useEffect(() => {
    if (manualProcessed.current) return;
    
    if (localStorage.getItem('showManual') === 'true' && localStorage.getItem('disableManual') !== 'true') {
      setShowManual(true);
      manualProcessed.current = true;
    }
  }, []);

  const handleCloseManual = () => {
    setShowManual(false);
    localStorage.removeItem('showManual');
  };

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
            <Link to="/">{t('nav.dashboard')}</Link>
            <Link to="/editor">{t('nav.editor')}</Link>
            {user?.role === 'ADMIN' && (
              <Link to="/users">{t('nav.users')}</Link>
            )}
          </div>
          <div className="nav-actions">
            <LanguageSwitcher />
            {user && (
              <div className="user-menu">
                <div className="user-info">
                  <span className="username">{user.username}</span>
                  <span className={`role-tag role-${user.role.toLowerCase()}`}>
                    {user.role === 'ADMIN' ? t('login.role_admin') : 
                     user.role === 'REVIEWER' ? t('login.role_reviewer') : t('login.role_user')}
                  </span>
                </div>
                <button onClick={handleLogout} className="btn-logout">
                  {t('common.logout')}
                </button>
              </div>
            )}
          </div>
        </div>
      </nav>
      <main className="container main-content">
        <Outlet />
      </main>
      <footer className="footer">
        <div className="container">
          <p>{t('footer.copyright')}</p>
        </div>
      </footer>
      {showManual && <UserManualModal onClose={handleCloseManual} />}
    </div>
  );
};

export default Layout;
