import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';
import api from '../api/axios';
import './Dashboard.css';

interface UserInfo {
  id: string;
  username: string;
}

interface Document {
  id: string;
  originalText: string;
  translatedText: string;
  sourceLanguage: string;
  targetLanguage: string;
  status: 'OFFEN' | 'UEBERSETZT' | 'BESTAETIGT';
  creator: UserInfo;
  reviewer?: UserInfo;
  createdAt: string;
}

const Dashboard: React.FC = () => {
  const { user } = useAuth();
  const [documents, setDocuments] = useState<Document[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchDocuments = async () => {
    try {
      setLoading(true);
      const response = await api.get('/documents');
      // Spring Data Page object returns content in the 'content' field
      setDocuments(response.data.content || []);
    } catch (err: any) {
      setError('Failed to load documents');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDocuments();
  }, []);

  const handleDelete = async (id: string) => {
    if (!window.confirm('Are you sure you want to delete this document?')) return;
    
    try {
      await api.delete(`/documents/${id}`);
      setDocuments(documents.filter(doc => doc.id !== id));
    } catch (err) {
      alert('Failed to delete document');
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'OFFEN': return <span className="badge badge-open">Offen</span>;
      case 'UEBERSETZT': return <span className="badge badge-translated">Übersetzt</span>;
      case 'BESTAETIGT': return <span className="badge badge-confirmed">Bestätigt</span>;
      default: return <span className="badge">{status}</span>;
    }
  };

  const filteredDocuments = documents.filter(doc => {
    if (!searchQuery) return true;
    const query = searchQuery.toLowerCase();
    return (
      (doc.originalText && doc.originalText.toLowerCase().includes(query)) ||
      (doc.sourceLanguage && doc.sourceLanguage.toLowerCase().includes(query)) ||
      (doc.targetLanguage && doc.targetLanguage.toLowerCase().includes(query)) ||
      (doc.status && doc.status.toLowerCase().includes(query)) ||
      (doc.creator && doc.creator.username.toLowerCase().includes(query)) ||
      (doc.reviewer && doc.reviewer.username.toLowerCase().includes(query))
    );
  });

  return (
    <div className="dashboard-container">
      <div className="dashboard-header">
        <h1>{user?.role === 'ADMIN' ? 'Admin Dashboard' : 
             user?.role === 'REVIEWER' ? 'Reviewer Dashboard' : 'My Documents'}</h1>
        {user?.role === 'USER' && (
          <Link to="/editor" className="btn btn-primary">
            New Document
          </Link>
        )}
      </div>

      {user?.role === 'ADMIN' && (
        <div className="dashboard-controls" style={{ marginBottom: '1rem' }}>
          <input 
            type="text" 
            placeholder="Search documents by text, language, or username..." 
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            className="search-input"
            style={{ width: '100%', padding: '0.75rem', borderRadius: '4px', border: '1px solid var(--border-color)', backgroundColor: 'var(--bg-main)', color: 'var(--text-primary)' }}
          />
        </div>
      )}

      {error && <div className="alert alert-error">{error}</div>}

      <div className="card">
        {loading ? (
          <div className="loading">Loading documents...</div>
        ) : filteredDocuments.length === 0 ? (
          <div className="empty-state">No documents found.</div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Text Snippet</th>
                <th>Langs</th>
                <th>Status</th>
                {user?.role === 'ADMIN' && <th>Creator</th>}
                <th>Reviewer</th>
                <th>Date</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredDocuments.map((doc) => (
                <tr key={doc.id}>
                  <td className="text-truncate" title={doc.originalText}>
                    {doc.originalText.substring(0, 50)}...
                  </td>
                  <td>{doc.sourceLanguage} → {doc.targetLanguage}</td>
                  <td>{getStatusBadge(doc.status)}</td>
                  {user?.role === 'ADMIN' && <td>{doc.creator.username}</td>}
                  <td>{doc.reviewer?.username || '-'}</td>
                  <td>{new Date(doc.createdAt).toLocaleDateString()}</td>
                  <td>
                    <div className="action-buttons">
                      <Link to={`/editor/${doc.id}`} className="btn-icon" title="Edit">
                        View/Edit
                      </Link>
                      {(user?.role === 'ADMIN' || (user?.role === 'USER' && doc.creator.id === user.id)) && (
                        <button 
                          onClick={() => handleDelete(doc.id)} 
                          className="btn-icon btn-delete" 
                          title="Delete"
                        >
                          Delete
                        </button>
                      )}
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

export default Dashboard;

