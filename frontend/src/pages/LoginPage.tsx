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
        setError('Signup successful! Please login.');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card card">
        <h1>{isLogin ? 'Welcome Back' : 'Create Account'}</h1>
        <p>{isLogin ? 'Please login to your account' : 'Sign up for a new account'}</p>
        
        {error && <div className={`alert ${error.includes('successful') ? 'alert-success' : 'alert-error'}`}>{error}</div>}

        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label htmlFor="username">Username</label>
            <input
              type="text"
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Enter your username"
              required
            />
          </div>
          
          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              type="password"
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
              required
            />
          </div>

          {!isLogin && (
            <div className="form-group">
              <label htmlFor="role">Role</label>
              <select
                id="role"
                value={role}
                onChange={(e) => setRole(e.target.value as any)}
              >
                <option value="USER">User</option>
                <option value="REVIEWER">Reviewer</option>
                <option value="ADMIN">Admin</option>
              </select>
            </div>
          )}

          <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
            {loading ? 'Processing...' : (isLogin ? 'Login' : 'Sign Up')}
          </button>
        </form>

        <div className="login-footer">
          <button onClick={() => setIsLogin(!isLogin)} className="btn-link">
            {isLogin ? "Don't have an account? Sign Up" : "Already have an account? Login"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;

