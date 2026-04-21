import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTranslation } from 'react-i18next';
import LanguageSwitcher from '../components/LanguageSwitcher';
import api from '../api/axios';
import './LoginPage.css';

const LoginPage: React.FC = () => {
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<'ADMIN' | 'USER' | 'REVIEWER'>('USER');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { t } = useTranslation();
  
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      if (isLogin) {
        const response = await api.post('/auth/signin', { username, password });
        const { accessToken, id, username: resUsername, role: resRole } = response.data;
        login(accessToken, { id, username: resUsername, role: resRole });
        localStorage.setItem('showManual', 'true');
        navigate('/');
      } else {
        await api.post('/auth/signup', { username, password, role });
        setIsLogin(true);
        setError(t('login.success_signup'));
      }
    } catch (err: any) {
      setError(err.response?.data?.message || t('login.error_auth'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-header">
        <LanguageSwitcher />
      </div>
      <div className="login-card card">
        <h1>{isLogin ? t('login.title_login') : t('login.title_signup')}</h1>
        <p>{isLogin ? t('login.subtitle_login') : t('login.subtitle_signup')}</p>
        
        {error && <div className={`alert ${error.includes('erfolgreich') ? 'alert-success' : 'alert-error'}`}>{error}</div>}

        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label htmlFor="username">{t('login.username')}</label>
            <input
              type="text"
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder={t('login.username_placeholder')}
              required
            />
          </div>
          
          <div className="form-group">
            <label htmlFor="password">{t('login.password')}</label>
            <input
              type="password"
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder={t('login.password_placeholder')}
              required
            />
          </div>

          {!isLogin && (
            <div className="form-group">
              <label htmlFor="role">{t('login.role')}</label>
              <select
                id="role"
                value={role}
                onChange={(e) => setRole(e.target.value as any)}
              >
                <option value="USER">{t('login.role_user')}</option>
                <option value="REVIEWER">{t('login.role_reviewer')}</option>
                <option value="ADMIN">{t('login.role_admin')}</option>
              </select>
            </div>
          )}

          <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
            {loading ? t('login.btn_processing') : (isLogin ? t('login.btn_login') : t('login.btn_signup'))}
          </button>
        </form>

        <div className="login-footer">
          <button onClick={() => setIsLogin(!isLogin)} className="btn-link">
            {isLogin ? t('login.link_no_account') : t('login.link_has_account')}
          </button>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;

