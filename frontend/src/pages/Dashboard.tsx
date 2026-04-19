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
  title: string;
  originalText: string;
  translatedText: string;
  sourceLanguage: string;
  targetLanguage: string;
  status: 'OFFEN' | 'UEBERSETZT' | 'BESTAETIGT';
  creator: UserInfo;
  reviewer?: UserInfo;
  createdAt: string;
  reviewDeadline?: string;
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
      setError('Dokumente konnten nicht geladen werden');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDocuments();
  }, []);

  const handleDelete = async (id: string) => {
    if (!window.confirm('Möchten Sie dieses Dokument wirklich löschen?')) return;
    
    try {
      await api.delete(`/documents/${id}`);
      setDocuments(documents.filter(doc => doc.id !== id));
    } catch (err) {
      alert('Löschen fehlgeschlagen');
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
        <h1>{user?.role === 'ADMIN' ? 'Administrator Dashboard' : 
             user?.role === 'REVIEWER' ? 'Reviewer Dashboard' : 'Meine Dokumente'}</h1>
        {user?.role === 'USER' && (
          <Link to="/editor" className="btn btn-primary">
            Neues Dokument
          </Link>
        )}
      </div>

      {user?.role === 'ADMIN' && (
        <div className="dashboard-controls">
          <input 
            type="text" 
            placeholder="Dokumente suchen nach Text, Sprache oder Benutzername..." 
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            className="search-input"
          />
        </div>
      )}

      {error && <div className="alert alert-error">{error}</div>}

      <div className="data-table-container">
        {loading ? (
          <div className="loading">Lade Dokumente...</div>
        ) : filteredDocuments.length === 0 ? (
          <div className="empty-state">Keine Dokumente gefunden.</div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Titel</th>
                <th>Sprachen</th>
                <th>Status</th>
                {user?.role === 'ADMIN' && <th>Ersteller</th>}
                <th>Reviewer</th>
                <th>Frist</th>
                <th>Datum</th>
                <th>Aktionen</th>
              </tr>
            </thead>
            <tbody>
              {filteredDocuments.map((doc) => {
                const deadlineDate = doc.reviewDeadline ? new Date(doc.reviewDeadline) : null;
                const now = new Date();
                const diffTime = deadlineDate ? deadlineDate.getTime() - now.getTime() : null;
                const diffDays = diffTime ? Math.ceil(diffTime / (1000 * 60 * 60 * 24)) : null;

                let rowClass = '';
                if (deadlineDate && doc.status !== 'BESTAETIGT') {
                  if (diffTime! < 0) rowClass = 'row-deadline-expired';
                  else if (diffDays! < 7) rowClass = 'row-deadline-warning';
                }

                return (
                  <tr key={doc.id} className={rowClass}>
                    <td className="text-truncate" title={doc.title}>
                    {doc.title}
                  </td>
                  <td>
                    <span style={{ fontWeight: 600 }}>{doc.sourceLanguage}</span>
                    <span style={{ margin: '0 8px', color: 'var(--text-muted)' }}>→</span>
                    <span style={{ fontWeight: 600 }}>{doc.targetLanguage}</span>
                  </td>
                  <td>{getStatusBadge(doc.status)}</td>
                  {user?.role === 'ADMIN' && <td>{doc.creator.username}</td>}
                  <td>{doc.reviewer?.username || '-'}</td>
                  <td>{doc.reviewDeadline ? new Date(doc.reviewDeadline).toLocaleDateString() : '-'}</td>
                  <td>{new Date(doc.createdAt).toLocaleDateString()}</td>
                  <td>
                    <div className="action-buttons">
                      <Link to={`/editor/${doc.id}`} className="btn-icon" title="Ansehen/Bearbeiten">
                        Bearbeiten
                      </Link>
                      {(user?.role === 'ADMIN' || (user?.role === 'USER' && doc.creator.id === user.id)) && (
                        <button 
                          onClick={() => handleDelete(doc.id)} 
                          className="btn-icon btn-delete" 
                          title="Löschen"
                        >
                          Löschen
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default Dashboard;

