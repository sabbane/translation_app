import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';
import { Languages, Send, Save, Wand2, ChevronLeft } from 'lucide-react';
import './EditorPage.css';

interface Reviewer {
  id: string;
  username: string;
}

const EditorPage: React.FC = () => {
  const { id } = useParams<{ id?: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [title, setTitle] = useState('');
  const [originalText, setOriginalText] = useState('');
  const [translatedText, setTranslatedText] = useState('');
  const [sourceLang, setSourceLang] = useState('EN');
  const [targetLang, setTargetLang] = useState('DE');
  const [status, setStatus] = useState('OFFEN');
  
  const [reviewers, setReviewers] = useState<Reviewer[]>([]);
  const [selectedReviewerId, setSelectedReviewerId] = useState('');
  const [loading, setLoading] = useState(false);
  const [translating, setTranslating] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (user?.role === 'USER') {
      fetchReviewers();
    }
    if (id) {
      fetchDocument();
    }
  }, [id, user]);

  useEffect(() => {
    if (isReadOnly || !originalText || originalText.length < 3) return;

    const delayDebounceFn = setTimeout(() => {
      handleAutoTranslate();
    }, 800); // 800ms debounce

    return () => clearTimeout(delayDebounceFn);
  }, [originalText, sourceLang, targetLang]);

  const fetchReviewers = async () => {
    try {
      const response = await api.get('/documents/reviewers');
      setReviewers(response.data);
    } catch (err) {
      console.error('Laden der Reviewer fehlgeschlagen');
    }
  };

  const fetchDocument = async () => {
    try {
      setLoading(true);
      const response = await api.get(`/documents/${id}`);
      const doc = response.data;
      setTitle(doc.title);
      setOriginalText(doc.originalText);
      setTranslatedText(doc.translatedText || '');
      setSourceLang(doc.sourceLanguage);
      setTargetLang(doc.targetLanguage);
      setStatus(doc.status);
      if (doc.reviewer) setSelectedReviewerId(doc.reviewer.id);
    } catch (err) {
      setError('Dokument konnte nicht geladen werden');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    try {
      setLoading(true);
      const docData = {
        title,
        originalText,
        translatedText,
        sourceLanguage: sourceLang,
        targetLanguage: targetLang
      };

      if (id) {
        await api.put(`/documents/${id}`, docData);
        alert('Dokument aktualisiert!');
      } else {
        const response = await api.post('/documents', docData);
        alert('Dokument erstellt!');
        navigate(`/editor/${response.data.id}`);
      }
    } catch (err) {
      setError('Speichern fehlgeschlagen');
    } finally {
      setLoading(false);
    }
  };

  const handleAutoTranslate = async () => {
    if (!originalText || originalText.length < 2) return;
    try {
      setTranslating(true);
      const response = await api.post('/translation/auto', {
        text: originalText,
        sourceLang,
        targetLang
      });
      setTranslatedText(response.data.translatedText);
    } catch (err) {
      console.error('Übersetzungsfehler:', err);
    } finally {
      setTranslating(false);
    }
  };

  const handleAssign = async (reviewerId: string, deadline: string) => {
    try {
      setLoading(true);
      await api.post(`/documents/${id}/assign?reviewerId=${reviewerId}&deadline=${deadline}`);
      alert('Zur Überprüfung eingereicht!');
      navigate('/');
    } catch (err) {
      setError('Zuweisung fehlgeschlagen');
    } finally {
      setLoading(false);
    }
  };

  const isReadOnly = (user?.role === 'USER' && status !== 'OFFEN') || 
                     (user?.role === 'REVIEWER' && status === 'BESTAETIGT');

  if (loading && !originalText) return <div className="loading-screen">Lade...</div>;

  return (
    <div className="editor-container">
      <div className="editor-header">
        <button onClick={() => navigate('/')} className="btn-back">
          <ChevronLeft size={20} /> Dashboard
        </button>
        <div className="title-input-container">
          <input 
            type="text" 
            className="editor-title-input" 
            placeholder="Dokumenttitel..." 
            value={title} 
            onChange={(e) => setTitle(e.target.value)}
            disabled={isReadOnly}
          />
        </div>
        {error && <div className="alert alert-error" style={{margin: '0 1rem', padding: '0.5rem', flex: 1}}>{error}</div>}
        
        <div className="editor-actions">
          {!isReadOnly && (
            <button onClick={handleSave} className="btn btn-secondary" disabled={loading}>
              <Save size={18} /> Speichern
            </button>
          )}

          {user?.role === 'USER' && status === 'OFFEN' && id && (
            <AssignmentModal 
              reviewers={reviewers} 
              onAssign={handleAssign} 
              loading={loading} 
            />
          )}
        </div>
      </div>

      <div className="language-bar card">
        <div className="lang-selectors">
          <select value={sourceLang} onChange={(e) => setSourceLang(e.target.value)} disabled={isReadOnly}>
            <option value="EN">Englisch</option>
            <option value="DE">Deutsch</option>
            <option value="FR">Französisch</option>
          </select>
          <Languages size={20} className="lang-icon" />
          <select value={targetLang} onChange={(e) => setTargetLang(e.target.value)} disabled={isReadOnly}>
            <option value="DE">Deutsch</option>
            <option value="EN">Englisch</option>
            <option value="FR">Französisch</option>
          </select>
        </div>
        
        {translating && (
          <div className="translation-status">
            <Wand2 size={16} className="spinning" /> Live-Übersetzung...
          </div>
        )}
      </div>

      <div className="split-editor">
        <div className="editor-pane card">
          <label>Originaltext ({sourceLang})</label>
          <textarea
            className="text-area"
            value={originalText}
            onChange={(e) => setOriginalText(e.target.value)}
            placeholder="Text zum Übersetzen eingeben..."
            readOnly={isReadOnly}
          />
        </div>
        <div className="editor-pane card">
          <label>Übersetzung ({targetLang})</label>
          <textarea
            className="text-area"
            value={translatedText}
            onChange={(e) => setTranslatedText(e.target.value)}
            placeholder="Die Übersetzung erscheint hier..."
            readOnly={isReadOnly}
          />
        </div>
      </div>
    </div>
  );
};

interface AssignmentModalProps {
  reviewers: Reviewer[];
  onAssign: (reviewerId: string, deadline: string) => void;
  loading: boolean;
}

const AssignmentModal: React.FC<AssignmentModalProps> = ({ reviewers, onAssign, loading }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [reviewerId, setReviewerId] = useState('');
  const [deadlineType, setDeadlineType] = useState<'days' | 'date'>('days');
  const [days, setDays] = useState('14');
  const [date, setDate] = useState('');

  const handleSubmit = () => {
    if (!reviewerId) {
      alert('Bitte Reviewer wählen');
      return;
    }

    let finalDeadline = '';
    if (deadlineType === 'days') {
      const d = new Date();
      d.setDate(d.getDate() + parseInt(days));
      finalDeadline = d.toISOString();
    } else {
      if (!date) {
        alert('Bitte Datum wählen');
        return;
      }
      finalDeadline = new Date(date).toISOString();
    }

    onAssign(reviewerId, finalDeadline);
  };

  if (!isOpen) {
    return (
      <button onClick={() => setIsOpen(true)} className="btn btn-primary" disabled={loading}>
        <Send size={18} /> Zur Review einreichen
      </button>
    );
  }

  return (
    <div className="modal-overlay">
      <div className="modal-content card">
        <h3>Dokument zur Review einreichen</h3>
        
        <div className="form-group">
          <label>Reviewer auswählen</label>
          <select value={reviewerId} onChange={(e) => setReviewerId(e.target.value)} className="reviewer-select">
            <option value="">Wählen...</option>
            {reviewers.map(r => (
              <option key={r.id} value={r.id}>{r.username}</option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label>Überprüfungsfrist festlegen</label>
          <div className="deadline-toggle">
            <button 
              className={`btn-toggle ${deadlineType === 'days' ? 'active' : ''}`}
              onClick={() => setDeadlineType('days')}
            >
              In Tagen
            </button>
            <button 
              className={`btn-toggle ${deadlineType === 'date' ? 'active' : ''}`}
              onClick={() => setDeadlineType('date')}
            >
              Datum wählen
            </button>
          </div>

          {deadlineType === 'days' ? (
            <input 
              type="number" 
              value={days} 
              onChange={(e) => setDays(e.target.value)} 
              min="1"
              className="deadline-input"
            />
          ) : (
            <input 
              type="date" 
              value={date} 
              onChange={(e) => setDate(e.target.value)}
              className="deadline-input"
            />
          )}
        </div>

        <div className="modal-footer">
          <button onClick={() => setIsOpen(false)} className="btn btn-secondary">Abbrechen</button>
          <button onClick={handleSubmit} className="btn btn-primary" disabled={!reviewerId || loading}>
            Zuweisen & Einreichen
          </button>
        </div>
      </div>
    </div>
  );
};

export default EditorPage;
