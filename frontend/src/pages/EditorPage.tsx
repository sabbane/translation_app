import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import api from '../api/axios';
import { Languages, Send, Save, Wand2, ChevronLeft, Lock, Upload, Download } from 'lucide-react';
import * as mammoth from 'mammoth';
import { Document as DocxDocument, Packer, Paragraph, TextRun } from 'docx';
import './EditorPage.css';

interface Reviewer {
  id: string;
  username: string;
}

const EditorPage: React.FC = () => {
  const { id } = useParams<{ id?: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { addToast } = useToast();

  const [title, setTitle] = useState('');
  const [originalText, setOriginalText] = useState('');
  const [translatedText, setTranslatedText] = useState('');
  const [sourceLang, setSourceLang] = useState('EN');
  const [targetLang, setTargetLang] = useState('DE');
  const [status, setStatus] = useState<'OFFEN' | 'IN_PRUEFUNG' | 'KORREKTUR' | 'ERLEDIGT'>('OFFEN');
  
  const [reviewers, setReviewers] = useState<Reviewer[]>([]);
  const [selectedReviewerId, setSelectedReviewerId] = useState('');
  const [loading, setLoading] = useState(false);
  const [translating, setTranslating] = useState(false);
  const [error, setError] = useState('');

  const fileInputRef = useRef<HTMLInputElement>(null);

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
        addToast('Dokument aktualisiert!', 'success');
      } else {
        const response = await api.post('/documents', docData);
        addToast('Dokument erstellt!', 'success');
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

  const handleImport = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    if (!title) {
      setTitle(file.name.replace(/\.[^/.]+$/, ""));
    }

    try {
      if (file.name.endsWith('.docx')) {
        const arrayBuffer = await file.arrayBuffer();
        const result = await mammoth.extractRawText({ arrayBuffer });
        setOriginalText(result.value);
        addToast('DOCX erfolgreich importiert!', 'success');
      } else if (file.name.endsWith('.txt')) {
        const text = await file.text();
        setOriginalText(text);
        addToast('TXT erfolgreich importiert!', 'success');
      } else {
        addToast('Nicht unterstütztes Format', 'error');
      }
    } catch (err) {
      console.error(err);
      addToast('Fehler beim Lesen der Datei', 'error');
    }
    
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleExport = async () => {
    if (!translatedText) {
      addToast('Kein Text zum Exportieren vorhanden', 'error');
      return;
    }
    
    try {
      const doc = new DocxDocument({
        sections: [{
          properties: {},
          children: translatedText.split('\n').map(line => 
            new Paragraph({
              children: [new TextRun(line)]
            })
          ),
        }],
      });

      const blob = await Packer.toBlob(doc);
      const element = document.createElement("a");
      element.href = URL.createObjectURL(blob);
      element.download = `${title || 'translation'}-exported.docx`;
      document.body.appendChild(element);
      element.click();
      document.body.removeChild(element);
      addToast('DOCX erfolgreich exportiert!', 'success');
    } catch (err) {
      console.error(err);
      addToast('Fehler beim Exportieren', 'error');
    }
  };

  const handleAssign = async (reviewerId: string, deadline: string) => {
    try {
      setLoading(true);
      await api.post(`/documents/${id}/assign?reviewerId=${reviewerId}&deadline=${deadline}`);
      addToast('Zur Überprüfung eingereicht!', 'success');
      navigate('/');
    } catch (err) {
      setError('Zuweisung fehlgeschlagen');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateStatus = async (newStatus: string) => {
    try {
      setLoading(true);
      await api.post(`/documents/${id}/status?status=${newStatus}`);
      addToast(newStatus === 'ERLEDIGT' ? 'Dokument abgeschlossen!' : 'Korrektur angefordert!', 'success');
      navigate('/');
    } catch (err) {
      setError('Status-Update fehlgeschlagen');
    } finally {
      setLoading(false);
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

  const isReadOnly = user?.role === 'ADMIN' || 
                     (user?.role === 'USER' && status !== 'OFFEN' && status !== 'KORREKTUR') || 
                     (user?.role === 'REVIEWER' && status === 'ERLEDIGT');

  if (loading && !originalText) return <div className="loading-screen">Lade...</div>;

  return (
    <div className="editor-container">
      <div className="editor-header">
        <button onClick={() => navigate('/')} className="btn-back">
          <ChevronLeft size={20} /> Dashboard
        </button>
        {isReadOnly && (
          <div className="readonly-banner">
            <Lock size={16} /> Nur Lese-Modus
          </div>
        )}
        <div className="title-input-container">
          <input 
            type="text" 
            className="editor-title-input" 
            placeholder="Dokumenttitel..." 
            value={title} 
            onChange={(e) => setTitle(e.target.value)}
            disabled={isReadOnly}
          />
          {id && <div className="editor-status-badge">{getStatusBadge(status)}</div>}
        </div>
        {error && <div className="alert alert-error" style={{margin: '0 1rem', padding: '0.5rem', flex: 1}}>{error}</div>}
        
        <div className="editor-actions">
          {!isReadOnly && (
            <button onClick={handleSave} className="btn btn-secondary" disabled={loading}>
              <Save size={18} /> Speichern
            </button>
          )}

          {user?.role === 'USER' && (status === 'OFFEN' || status === 'KORREKTUR') && id && (
            <AssignmentModal 
              reviewers={reviewers} 
              onAssign={handleAssign} 
              loading={loading} 
            />
          )}

          {user?.role === 'REVIEWER' && status === 'IN_PRUEFUNG' && (
            <div className="reviewer-actions">
              <button 
                onClick={() => handleUpdateStatus('KORREKTUR')} 
                className="btn btn-danger" 
                disabled={loading}
              >
                Nachbesserung anfordern
              </button>
              <button 
                onClick={() => handleUpdateStatus('ERLEDIGT')} 
                className="btn btn-success" 
                disabled={loading}
              >
                Bestätigen & Abschließen
              </button>
            </div>
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
          <div className="pane-header">
            <label>Originaltext ({sourceLang})</label>
            {!isReadOnly && (
              <div className="pane-actions">
                <input 
                  type="file" 
                  accept=".txt,.docx" 
                  ref={fileInputRef} 
                  onChange={handleImport} 
                  style={{ display: 'none' }} 
                />
                <button 
                  onClick={() => fileInputRef.current?.click()} 
                  className="btn-small" 
                  style={{ display: 'flex', alignItems: 'center', gap: '4px' }}
                  title="Textdatei importieren (.txt, .docx)"
                >
                  <Upload size={14} /> Import
                </button>
              </div>
            )}
          </div>
          <textarea
            className="text-area"
            value={originalText}
            onChange={(e) => setOriginalText(e.target.value)}
            placeholder="Text zum Übersetzen eingeben oder Datei importieren..."
            readOnly={isReadOnly}
          />
        </div>
        <div className="editor-pane card">
          <div className="pane-header">
            <label>Übersetzung ({targetLang})</label>
            <div className="pane-actions">
              <button 
                onClick={handleExport} 
                className="btn-small" 
                style={{ display: 'flex', alignItems: 'center', gap: '4px' }}
                disabled={!translatedText}
                title="Als DOCX exportieren"
              >
                <Download size={14} /> Export
              </button>
            </div>
          </div>
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

  const { addToast } = useToast();

  const handleSubmit = () => {
    if (!reviewerId) {
      addToast('Bitte Reviewer wählen', 'error');
      return;
    }

    let finalDeadline = '';
    if (deadlineType === 'days') {
      const d = new Date();
      d.setDate(d.getDate() + parseInt(days));
      finalDeadline = d.toISOString();
    } else {
      if (!date) {
        addToast('Bitte Datum wählen', 'error');
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
