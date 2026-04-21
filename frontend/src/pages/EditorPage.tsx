import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { useTranslation } from 'react-i18next';
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
  const { t } = useTranslation();

  const [title, setTitle] = useState('');
  const [originalText, setOriginalText] = useState('');
  const [translatedText, setTranslatedText] = useState('');
  const [sourceLang, setSourceLang] = useState('EN');
  const [targetLang, setTargetLang] = useState('DE');
  const [status, setStatus] = useState<'OFFEN' | 'IN_PRUEFUNG' | 'KORREKTUR' | 'ERLEDIGT'>('OFFEN');
  
  const [reviewers, setReviewers] = useState<Reviewer[]>([]);
  const [loading, setLoading] = useState(false);
  const [translating, setTranslating] = useState(false);
  const [error, setError] = useState('');

  const fileInputRef = useRef<HTMLInputElement>(null);
  const hasUserEdited = useRef(false);

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

    if (id && !hasUserEdited.current) return;

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
      console.error(t('error.loading_reviewers'));
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
    } catch (err) {
      setError(t('error.loading_document'));
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
        addToast(t('editor.saved'), 'success');
      } else {
        const response = await api.post('/documents', docData);
        addToast(t('editor.created'), 'success');
        navigate(`/editor/${response.data.id}`);
      }
    } catch (err) {
      setError(t('error.saving'));
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
      console.error(t('error.translation'), err);
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
        hasUserEdited.current = true;
        addToast(t('editor.import_success'), 'success');
      } else if (file.name.endsWith('.txt')) {
        const text = await file.text();
        setOriginalText(text);
        hasUserEdited.current = true;
        addToast(t('editor.import_success'), 'success');
      } else {
        addToast(t('error.format_not_supported'), 'error');
      }
    } catch (err) {
      console.error(err);
      addToast(t('error.file_reading'), 'error');
    }
    
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleExport = async () => {
    if (!translatedText) {
      addToast(t('error.no_text_to_export'), 'error');
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
      addToast(t('editor.export_success'), 'success');
    } catch (err) {
      console.error(err);
      addToast(t('error.exporting'), 'error');
    }
  };

  const handleAssign = async (reviewerId: string, deadline: string) => {
    try {
      setLoading(true);
      await api.post(`/documents/${id}/assign?reviewerId=${reviewerId}&deadline=${deadline}`);
      addToast(t('editor.assigned'), 'success');
      navigate('/');
    } catch (err) {
      setError(t('error.assignment_failed'));
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateStatus = async (newStatus: string) => {
    try {
      setLoading(true);
      await api.post(`/documents/${id}/status?status=${newStatus}`);
      addToast(newStatus === 'ERLEDIGT' ? t('editor.completed') : t('editor.correction_requested'), 'success');
      navigate('/');
    } catch (err) {
      setError(t('error.status_update_failed'));
    } finally {
      setLoading(false);
    }
  };
  
  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'OFFEN': return <span className="badge badge-draft">{t('status.draft')}</span>;
      case 'IN_PRUEFUNG': return <span className="badge badge-review">{t('status.in_review')}</span>;
      case 'KORREKTUR': return <span className="badge badge-correction">{t('status.correction')}</span>;
      case 'ERLEDIGT': return <span className="badge badge-completed">{t('status.completed')}</span>;
      default: return <span className="badge">{status}</span>;
    }
  };

  const isReadOnly = (user?.role === 'USER' && status !== 'OFFEN' && status !== 'KORREKTUR') || 
                     (user?.role === 'REVIEWER' && status === 'ERLEDIGT');

  if (loading && !originalText) return <div className="loading-screen">{t('common.loading')}...</div>;

  return (
    <div className="editor-container">
      <div className="editor-header">
        <button onClick={() => navigate('/')} className="btn-back">
          <ChevronLeft size={20} /> {t('common.dashboard')}
        </button>
        {isReadOnly && (
          <div className="readonly-banner">
            <Lock size={16} /> {t('editor.readonly_mode')}
          </div>
        )}
        <div className="title-input-container">
          <input 
            type="text" 
            className="editor-title-input" 
            placeholder={t('editor.title_placeholder')} 
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
              <Save size={18} /> {t('common.save')}
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
                {t('editor.request_correction')}
              </button>
              <button 
                onClick={() => handleUpdateStatus('ERLEDIGT')} 
                className="btn btn-success" 
                disabled={loading}
              >
                {t('editor.confirm_and_complete')}
              </button>
            </div>
          )}
        </div>
      </div>

      <div className="language-bar card">
        <div className="lang-selectors">
          <select value={sourceLang} onChange={(e) => {
            setSourceLang(e.target.value);
            hasUserEdited.current = true;
          }} disabled={isReadOnly}>
            <option value="EN">{t('lang.en')}</option>
            <option value="DE">{t('lang.de')}</option>
            <option value="FR">{t('lang.fr')}</option>
            <option value="AR">{t('lang.ar')}</option>
          </select>
          <Languages size={20} className="lang-icon" />
          <select value={targetLang} onChange={(e) => {
            setTargetLang(e.target.value);
            hasUserEdited.current = true;
          }} disabled={isReadOnly}>
            <option value="DE">{t('lang.de')}</option>
            <option value="EN">{t('lang.en')}</option>
            <option value="FR">{t('lang.fr')}</option>
            <option value="AR">{t('lang.ar')}</option>
          </select>
        </div>
        
        {translating && (
          <div className="translation-status">
            <Wand2 size={16} className="spinning" /> {t('editor.live_translation')}...
          </div>
        )}
      </div>

      <div className="split-editor">
        <div className="editor-pane card">
          <div className="pane-header">
            <label>{t('editor.original_text')} ({sourceLang})</label>
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
                  title={t('editor.import_file')}
                >
                  <Upload size={14} /> {t('common.import')}
                </button>
              </div>
            )}
          </div>
          <textarea
            className="text-area"
            value={originalText}
            onChange={(e) => {
              setOriginalText(e.target.value);
              hasUserEdited.current = true;
            }}
            placeholder={t('editor.original_placeholder')}
            readOnly={isReadOnly}
          />
        </div>
        <div className="editor-pane card">
          <div className="pane-header">
            <label>{t('editor.translated_text')} ({targetLang})</label>
            <div className="pane-actions">
              <button 
                onClick={handleExport} 
                className="btn-small" 
                style={{ display: 'flex', alignItems: 'center', gap: '4px' }}
                disabled={!translatedText}
                title={t('editor.export_file')}
              >
                <Download size={14} /> {t('common.export')}
              </button>
            </div>
          </div>
          <textarea
            className="text-area"
            value={translatedText}
            onChange={(e) => setTranslatedText(e.target.value)}
            placeholder={t('editor.translated_placeholder')}
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

  const { t } = useTranslation();
  const { addToast } = useToast();

  const handleSubmit = () => {
    if (!reviewerId) {
      addToast(t('assignment.error_reviewer'), 'error');
      return;
    }

    let finalDeadline = '';
    if (deadlineType === 'days') {
      const d = new Date();
      d.setDate(d.getDate() + parseInt(days));
      finalDeadline = d.toISOString();
    } else {
      if (!date) {
        addToast(t('assignment.error_date'), 'error');
        return;
      }
      finalDeadline = new Date(date).toISOString();
    }

    onAssign(reviewerId, finalDeadline);
  };

  if (!isOpen) {
    return (
      <button onClick={() => setIsOpen(true)} className="btn btn-primary" disabled={loading}>
        <Send size={18} /> {t('assignment.submit_btn')}
      </button>
    );
  }

  return (
    <div className="modal-overlay">
      <div className="modal-content card">
        <h3>{t('assignment.title')}</h3>
        
        <div className="form-group">
          <label>{t('assignment.select_reviewer')}</label>
          <select value={reviewerId} onChange={(e) => setReviewerId(e.target.value)} className="reviewer-select">
            <option value="">{t('assignment.placeholder_reviewer')}</option>
            {reviewers.map(r => (
              <option key={r.id} value={r.id}>{r.username}</option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label>{t('assignment.set_deadline')}</label>
          <div className="deadline-toggle">
            <button 
              className={`btn-toggle ${deadlineType === 'days' ? 'active' : ''}`}
              onClick={() => setDeadlineType('days')}
            >
              {t('assignment.in_days')}
            </button>
            <button 
              className={`btn-toggle ${deadlineType === 'date' ? 'active' : ''}`}
              onClick={() => setDeadlineType('date')}
            >
              {t('assignment.choose_date')}
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
          <button onClick={() => setIsOpen(false)} className="btn btn-secondary">{t('common.cancel')}</button>
          <button onClick={handleSubmit} className="btn btn-primary" disabled={!reviewerId || loading}>
            {t('assignment.confirm_btn')}
          </button>
        </div>
      </div>
    </div>
  );
};

export default EditorPage;
