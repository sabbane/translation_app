import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import { useTranslation } from 'react-i18next';
import { Pencil, Trash2, UserPlus } from 'lucide-react';
import './UserManagement.css';
import Modal from '../components/Modal';

interface User {
  id: string;
  username: string;
  role: 'ADMIN' | 'USER' | 'REVIEWER';
}

const UserManagement: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { t } = useTranslation();
  
  // Modal state
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [userToDelete, setUserToDelete] = useState<User | null>(null);

  // Form state
  const [showForm, setShowForm] = useState(false);
  const [editingUserId, setEditingUserId] = useState<string | null>(null);
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    role: 'USER'
  });

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const response = await api.get('/users');
      setUsers(response.data);
      setError('');
    } catch (err: any) {
      setError(t('users.error_load'));
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingUserId) {
        // Update user
        const payload: any = { 
          username: formData.username,
          role: formData.role
        };
        if (formData.password) {
          payload.password = formData.password;
        }
        await api.put(`/users/${editingUserId}`, payload);
      } else {
        // Create user
        await api.post('/users', formData);
      }
      setShowForm(false);
      fetchUsers();
      resetForm();
    } catch (err: any) {
      setError(err.response?.data?.message || t('users.error_save'));
    }
  };

  const handleEdit = (user: User) => {
    setEditingUserId(user.id);
    setFormData({
      username: user.username,
      password: '', // don't load password
      role: user.role
    });
    setShowForm(true);
  };

  const handleDeleteClick = (user: User) => {
    setUserToDelete(user);
    setDeleteModalOpen(true);
  };

  const confirmDelete = async () => {
    if (!userToDelete) return;
    try {
      await api.delete(`/users/${userToDelete.id}`);
      setUsers(users.filter(u => u.id !== userToDelete.id));
      setDeleteModalOpen(false);
      setUserToDelete(null);
    } catch (err: any) {
      console.error('Löschfehler:', err.response?.data || err.message);
      const serverMsg = err.response?.data?.message;
      setError(serverMsg || t('users.error_delete'));
      setDeleteModalOpen(false);
    }
  };

  const resetForm = () => {
    setFormData({ username: '', password: '', role: 'USER' });
    setEditingUserId(null);
  };

  const toggleForm = () => {
    if (showForm) {
      setShowForm(false);
      resetForm();
    } else {
      setShowForm(true);
    }
  };

  return (
    <div className="dashboard-container">
      <div className="dashboard-header">
        <h1>{t('users.title')}</h1>
        <button className="btn btn-primary" onClick={toggleForm}>
          {showForm ? t('common.cancel') : <><UserPlus size={18} /> {t('users.new_user')}</>}
        </button>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {showForm && (
        <div className="card form-card">
          <h2>{editingUserId ? t('users.edit_user') : t('users.create_user')}</h2>
          <form onSubmit={handleSubmit} className="user-form">
            <div className="form-group">
              <label>{t('login.username')}</label>
              <input
                type="text"
                name="username"
                value={formData.username}
                onChange={handleInputChange}
                required
              />
            </div>
            <div className="form-group">
              <label>{t('login.password')} {editingUserId && `(${t('users.password_hint_optional')})`}</label>
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleInputChange}
                required={!editingUserId}
              />
            </div>
            <div className="form-group">
              <label>{t('login.role')}</label>
              <select
                name="role"
                value={formData.role}
                onChange={handleInputChange}
              >
                <option value="USER">{t('login.role_user')}</option>
                <option value="REVIEWER">{t('login.role_reviewer')}</option>
                <option value="ADMIN">{t('login.role_admin')}</option>
              </select>
            </div>
            <div className="form-actions">
              <button type="submit" className="btn btn-primary">
                {editingUserId ? t('users.update_user') : t('users.create_user')}
              </button>
            </div>
          </form>
        </div>
      )}

      <Modal 
        isOpen={deleteModalOpen}
        onClose={() => setDeleteModalOpen(false)}
        onConfirm={confirmDelete}
        title={t('users.delete_title')}
        message={t('users.delete_message', { username: userToDelete?.username })}
        confirmText={t('common.delete')}
        type="danger"
      />

      <div className="data-table-container">
        {loading ? (
          <div className="loading">{t('users.loading')}</div>
        ) : users.length === 0 ? (
          <div className="empty-state">{t('users.empty')}</div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>{t('users.table_username')}</th>
                <th>{t('users.table_role')}</th>
                <th>{t('users.table_actions')}</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td style={{ fontWeight: 600 }}>{u.username}</td>
                  <td>
                    <span className={`role-badge role-${u.role.toLowerCase()}`}>
                      {u.role === 'ADMIN' ? t('login.role_admin') : 
                       u.role === 'REVIEWER' ? t('login.role_reviewer') : t('login.role_user')}
                    </span>
                  </td>
                  <td>
                    <div className="action-buttons">
                      <button 
                        onClick={() => handleEdit(u)} 
                        className="btn-icon" 
                        title={t('dashboard.edit_tooltip')}
                      >
                        <Pencil size={18} />
                      </button>
                      <button 
                        onClick={() => handleDeleteClick(u)} 
                        className="btn-icon btn-delete" 
                        title={t('common.delete')}
                      >
                        <Trash2 size={18} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default UserManagement;
