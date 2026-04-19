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
      console.error('Failed to load reviewers');
    }
  };

  const fetchDocument = async () => {
    try {
      setLoading(true);
      const response = await api.get(`/documents/${id}`);
      const doc = response.data;
      setOriginalText(doc.originalText);
      setTranslatedText(doc.translatedText || '');
      setSourceLang(doc.sourceLanguage);
      setTargetLang(doc.targetLanguage);
      setStatus(doc.status);
      if (doc.reviewer) setSelectedReviewerId(doc.reviewer.id);
    } catch (err) {
      setError('Failed to load document');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    try {
      setLoading(true);
      const docData = {
        originalText,
        translatedText,
        sourceLanguage: sourceLang,
        targetLanguage: targetLang
      };

      if (id) {
        await api.put(`/documents/${id}`, docData);
        alert('Document updated!');
      } else {
        const response = await api.post('/documents', docData);
        alert('Document created!');
        navigate(`/editor/${response.data.id}`);
      }
    } catch (err) {
      setError('Failed to save document');
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
      console.error('Auto-translation error:', err);
      // We don't alert during live typing to avoid annoying the user
    } finally {
      setTranslating(false);
    }
  };

  const handleAssign = async () => {
    if (!selectedReviewerId) {
      alert('Please select a reviewer');
      return;
    }
    try {
      setLoading(true);
      await api.post(`/documents/${id}/assign?reviewerId=${selectedReviewerId}`);
      alert('Submitted for review!');
      navigate('/');
    } catch (err) {
      setError('Failed to assign reviewer');
    } finally {
      setLoading(false);
    }
  };

  const isReadOnly = (user?.role === 'USER' && status !== 'OFFEN') || 
                     (user?.role === 'REVIEWER' && status === 'BESTAETIGT');

  if (loading && !originalText) return <div className="loading-screen">Loading...</div>;

  return (
    <div className="editor-container">
      <div className="editor-header">
        <button onClick={() => navigate('/')} className="btn-back">
          <ChevronLeft size={20} /> Dashboard
        </button>
        {error && <div className="alert alert-error" style={{margin: '0 1rem', padding: '0.5rem', flex: 1}}>{error}</div>}
        
        <div className="editor-actions">
          {!isReadOnly && (
            <button onClick={handleSave} className="btn btn-secondary" disabled={loading}>
              <Save size={18} /> Save
            </button>
          )}

          {user?.role === 'USER' && status === 'OFFEN' && id && (
            <div className="assign-box">
              <select 
                value={selectedReviewerId} 
                onChange={(e) => setSelectedReviewerId(e.target.value)}
                className="reviewer-select"
              >
                <option value="">Select Reviewer...</option>
                {reviewers.map(r => (
                  <option key={r.id} value={r.id}>{r.username}</option>
                ))}
              </select>
              <button onClick={handleAssign} className="btn btn-primary" disabled={loading || !selectedReviewerId}>
                <Send size={18} /> Submit for Review
              </button>
            </div>
          )}
        </div>
      </div>

      <div className="language-bar card">
        <div className="lang-selectors">
          <select value={sourceLang} onChange={(e) => setSourceLang(e.target.value)} disabled={isReadOnly}>
            <option value="EN">English</option>
            <option value="DE">German</option>
            <option value="FR">French</option>
          </select>
          <Languages size={20} className="lang-icon" />
          <select value={targetLang} onChange={(e) => setTargetLang(e.target.value)} disabled={isReadOnly}>
            <option value="DE">German</option>
            <option value="EN">English</option>
            <option value="FR">French</option>
          </select>
        </div>
        
        {translating && (
          <div className="translation-status">
            <Wand2 size={16} className="spinning" /> Live-Translating...
          </div>
        )}
      </div>

      <div className="split-editor">
        <div className="editor-pane card">
          <label>Original Text ({sourceLang})</label>
          <textarea
            className="text-area"
            value={originalText}
            onChange={(e) => setOriginalText(e.target.value)}
            placeholder="Enter text to translate..."
            readOnly={isReadOnly}
          />
        </div>
        <div className="editor-pane card">
          <label>Translation ({targetLang})</label>
          <textarea
            className="text-area"
            value={translatedText}
            onChange={(e) => setTranslatedText(e.target.value)}
            placeholder="Translation will appear here..."
            readOnly={isReadOnly}
          />
        </div>
      </div>
    </div>
  );
};

export default EditorPage;
