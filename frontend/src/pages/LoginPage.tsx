import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';
import './LoginPage.css';

const LoginPage: React.FC = () => {
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<'ADMIN' | 'USER' | 'REVIEWER'>('USER');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  
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
        navigate('/');
      } else {
        await api.post('/auth/signup', { username, password, role });
        setIsLogin(true);
        setError('Registrierung erfolgreich! Bitte loggen Sie sich ein.');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Authentifizierung fehlgeschlagen');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card card">
        <h1>{isLogin ? 'Willkommen zurück' : 'Account erstellen'}</h1>
        <p>{isLogin ? 'Bitte loggen Sie sich in Ihren Account ein' : 'Registrieren Sie einen neuen Account'}</p>
        
        {error && <div className={`alert ${error.includes('erfolgreich') ? 'alert-success' : 'alert-error'}`}>{error}</div>}

        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label htmlFor="username">Benutzername</label>
            <input
              type="text"
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Benutzername eingeben"
              required
            />
          </div>
          
          <div className="form-group">
            <label htmlFor="password">Passwort</label>
            <input
              type="password"
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Passwort eingeben"
              required
            />
          </div>

          {!isLogin && (
            <div className="form-group">
              <label htmlFor="role">Rolle</label>
              <select
                id="role"
                value={role}
                onChange={(e) => setRole(e.target.value as any)}
              >
                <option value="USER">Benutzer</option>
                <option value="REVIEWER">Reviewer</option>
                <option value="ADMIN">Administrator</option>
              </select>
            </div>
          )}

          <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
            {loading ? 'Wird verarbeitet...' : (isLogin ? 'Anmelden' : 'Registrieren')}
          </button>
        </form>

        <div className="login-footer">
          <button onClick={() => setIsLogin(!isLogin)} className="btn-link">
            {isLogin ? "Noch keinen Account? Registrieren" : "Bereits einen Account? Anmelden"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;

