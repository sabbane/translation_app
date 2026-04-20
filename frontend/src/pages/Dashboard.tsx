import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';
import { Pencil, Trash2, Plus, LayoutGrid, List, FileText, CheckCircle2, Clock, AlertCircle, Calendar } from 'lucide-react';
import api from '../api/axios';
import './Dashboard.css';
import Modal from '../components/Modal';

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
  status: 'OFFEN' | 'IN_PRUEFUNG' | 'KORREKTUR' | 'ERLEDIGT';
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
  const [viewMode, setViewMode] = useState<'table' | 'grid'>('table');
  const [statusFilter, setStatusFilter] = useState('');
  const [langFilter, setLangFilter] = useState('');

  // Modal state
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [docToDelete, setDocToDelete] = useState<Document | null>(null);

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

  const handleDeleteClick = (doc: Document) => {
    setDocToDelete(doc);
    setDeleteModalOpen(true);
  };

  const confirmDelete = async () => {
    if (!docToDelete) return;
    try {
      await api.delete(`/documents/${docToDelete.id}`);
      setDocuments(documents.filter(doc => doc.id !== docToDelete.id));
      setDeleteModalOpen(false);
      setDocToDelete(null);
    } catch (err) {
      alert('Löschen fehlgeschlagen');
      setDeleteModalOpen(false);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'OFFEN': return <span className="badge badge-draft">Entwurf</span>;
      case 'IN_PRUEFUNG': return <span className="badge badge-review">In Prüfung</span>;
      case 'KORREKTUR': return <span className="badge badge-correction">Korrektur</span>;
      case 'ERLEDIGT': return <span className="badge badge-completed">Fertig</span>;
      default: return <span className="badge">{status}</span>;
    }
  };

  const filteredDocuments = documents.filter(doc => {
    // Search Query Filter
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      const matchesSearch = (
        (doc.originalText && doc.originalText.toLowerCase().includes(query)) ||
        (doc.sourceLanguage && doc.sourceLanguage.toLowerCase().includes(query)) ||
        (doc.targetLanguage && doc.targetLanguage.toLowerCase().includes(query)) ||
        (doc.status && doc.status.toLowerCase().includes(query)) ||
        (doc.creator && doc.creator.username.toLowerCase().includes(query)) ||
        (doc.reviewer && doc.reviewer.username.toLowerCase().includes(query))
      );
      if (!matchesSearch) return false;
    }

    // Status Filter
    if (statusFilter && doc.status !== statusFilter) {
      return false;
    }

    // Language Filter
    if (langFilter) {
      const docLangCombo = `${doc.sourceLanguage}-${doc.targetLanguage}`;
      if (docLangCombo !== langFilter) {
        return false;
      }
    }

    return true;
  });

  return (
    <div className="dashboard-container">
      <div className="dashboard-header">
        <h1>{user?.role === 'ADMIN' ? 'Administrator Dashboard' : 
             user?.role === 'REVIEWER' ? 'Reviewer Dashboard' : 'Meine Dokumente'}</h1>
        {user?.role === 'USER' && (
          <Link to="/editor" className="btn btn-primary">
            <Plus size={20} /> Neues Dokument
          </Link>
        )}
      </div>

      <Modal 
        isOpen={deleteModalOpen}
        onClose={() => setDeleteModalOpen(false)}
        onConfirm={confirmDelete}
        title="Dokument löschen"
        message={`Möchten Sie das Dokument "${docToDelete?.title}" wirklich unwiderruflich löschen?`}
        confirmText="Löschen"
        type="danger"
      />

      <div className="dashboard-controls">
        <div className="filters-group">
          <input 
            type="text" 
            placeholder="Dokumente suchen..." 
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            className="search-input"
          />
          <select 
            value={statusFilter} 
            onChange={e => setStatusFilter(e.target.value)}
            className="filter-select"
          >
            <option value="">Alle Status</option>
            <option value="OFFEN">Entwurf</option>
            <option value="IN_PRUEFUNG">In Prüfung</option>
            <option value="KORREKTUR">Korrektur</option>
            <option value="ERLEDIGT">Fertig</option>
          </select>
          <select 
            value={langFilter} 
            onChange={e => setLangFilter(e.target.value)}
            className="filter-select"
          >
            <option value="">Alle Sprachen</option>
            <option value="DE-EN">DE → EN</option>
            <option value="EN-DE">EN → DE</option>
            <option value="DE-FR">DE → FR</option>
            <option value="FR-DE">FR → DE</option>
            <option value="EN-FR">EN → FR</option>
            <option value="FR-EN">FR → EN</option>
          </select>
        </div>
        <div className="view-toggle">
          <button 
            className={viewMode === 'table' ? 'active' : ''} 
            onClick={() => setViewMode('table')}
            title="Listenansicht"
          >
            <List size={20} />
          </button>
          <button 
            className={viewMode === 'grid' ? 'active' : ''} 
            onClick={() => setViewMode('grid')}
            title="Kachelansicht"
          >
            <LayoutGrid size={20} />
          </button>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className={viewMode === 'table' ? 'data-table-container' : ''}>
        {loading ? (
          <div className="loading">Lade Dokumente...</div>
        ) : filteredDocuments.length === 0 ? (
          <div className="empty-state">Keine Dokumente gefunden.</div>
        ) : viewMode === 'table' ? (
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
                if (doc.status === 'ERLEDIGT') {
                  rowClass = 'row-completed';
                } else if (deadlineDate) {
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
                        <Pencil size={18} />
                      </Link>
                      {(user?.role === 'ADMIN' || (user?.role === 'USER' && doc.creator.id === user.id)) && (
                        <button 
                          onClick={() => handleDeleteClick(doc)} 
                          className="btn-icon btn-delete" 
                          title="Löschen"
                        >
                          <Trash2 size={18} />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
                );
              })}
            </tbody>
          </table>
        ) : (
          <div className="document-grid">
            {filteredDocuments.map((doc) => {
              const deadlineDate = doc.reviewDeadline ? new Date(doc.reviewDeadline) : null;
              const now = new Date();
              const diffTime = deadlineDate ? deadlineDate.getTime() - now.getTime() : null;
              const diffDays = diffTime ? Math.ceil(diffTime / (1000 * 60 * 60 * 24)) : null;

              let cardClass = 'doc-card';
              if (doc.status === 'ERLEDIGT') {
                // Done documents don't need warning classes
              } else if (deadlineDate) {
                if (diffTime! < 0) cardClass += ' expired';
                else if (diffDays! < 7) cardClass += ' warning';
              }

              const words = doc.title.split(' ');
              const truncatedTitle = words.length > 2 
                ? words.slice(0, 2).join(' ') + '...' 
                : doc.title;

              return (
                <div key={doc.id} className={cardClass} onClick={() => window.location.href = `/editor/${doc.id}`}>
                  <div className="doc-icon-wrapper">
                    <FileText size={32} />
                    {doc.status === 'ERLEDIGT' && (
                      <div className="status-seal" title="Abgeschlossen">
                        <CheckCircle2 size={20} fill="white" />
                      </div>
                    )}
                    {doc.status === 'IN_PRUEFUNG' && (
                      <div className="status-seal" style={{ color: '#3b82f6' }} title="In Prüfung">
                        <Clock size={20} fill="white" />
                      </div>
                    )}
                  </div>
                  
                  <div className="doc-card-title" title={doc.title}>{truncatedTitle}</div>
                  
                  <div className="doc-card-info">
                    <div className="doc-card-langs">
                      {doc.sourceLanguage} → {doc.targetLanguage}
                    </div>
                    <div className="doc-card-status">
                      {getStatusBadge(doc.status)}
                    </div>
                  </div>

                  <div className="doc-card-reviewer">
                    <strong>Reviewer:</strong> {doc.reviewer?.username || '-'}
                  </div>

                  {doc.reviewDeadline && (
                    <div className="card-deadline">
                      {diffTime! < 0 ? <AlertCircle size={14} /> : <Calendar size={14} />}
                      {new Date(doc.reviewDeadline).toLocaleDateString()}
                    </div>
                  )}

                  <div className="doc-card-actions">
                    {(user?.role === 'ADMIN' || (user?.role === 'USER' && doc.creator.id === user.id)) && (
                      <button 
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDeleteClick(doc);
                        }} 
                        className="btn-icon btn-delete"
                      >
                        <Trash2 size={16} />
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default Dashboard;

