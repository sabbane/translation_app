import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { X, BookOpen, Globe, FileText, CheckCircle, ArrowRight } from 'lucide-react';
import './UserManualModal.css';

interface UserManualModalProps {
  onClose: () => void;
}

const UserManualModal: React.FC<UserManualModalProps> = ({ onClose }) => {
  const { t, i18n } = useTranslation();
  const [selectedLang, setSelectedLang] = useState(i18n.language || 'de');

  const languages = [
    { code: 'de', name: 'Deutsch' },
    { code: 'en', name: 'English' },
    { code: 'fr', name: 'Français' },
    { code: 'ar', name: 'العربية' }
  ];

  // Update i18n language when selectedLang changes for the content in this modal
  useEffect(() => {
    const originalLang = i18n.language;
    i18n.changeLanguage(selectedLang);
    return () => {
      // We don't necessarily want to revert the system language when closing, 
      // but the user might want the UI to stay in the chosen manual language.
      // However, if they just wanted to read the manual in English but keep the UI in German, 
      // it's tricky. For now, let's keep the language change persistent as it's a common pattern.
    };
  }, [selectedLang, i18n]);

  return (
    <div className="manual-overlay">
      <div className={`manual-modal display-mode card ${selectedLang === 'ar' ? 'rtl' : ''}`}>
        <button className="close-btn" onClick={onClose} aria-label="Close">
          <X size={24} />
        </button>

        <div className="manual-modal-header">
          <div className="icon-wrapper">
            <BookOpen size={32} />
          </div>
          <h2>{t('manual.title')}</h2>
          <div className="lang-switcher-inline">
            {languages.map(lang => (
              <button
                key={lang.code}
                className={`lang-dot ${selectedLang === lang.code ? 'active' : ''}`}
                onClick={() => setSelectedLang(lang.code)}
                title={lang.name}
              >
                {lang.code.toUpperCase()}
              </button>
            ))}
          </div>
        </div>

        <div className="manual-content-scroll">
          <section className="manual-section intro">
            <h3>{t('manual.intro_title')}</h3>
            <p className="intro-text">{t('manual.intro_text')}</p>
          </section>

          <div className="features-grid">
            <section className="manual-section feature">
              <div className="section-title">
                <div className="feature-icon user-icon">
                  <FileText size={20} />
                </div>
                <h4>{t('manual.user_functions_title')}</h4>
              </div>
              <p>{t('manual.user_functions_text')}</p>
            </section>

            <section className="manual-section feature">
              <div className="section-title">
                <div className="feature-icon reviewer-icon">
                  <CheckCircle size={20} />
                </div>
                <h4>{t('manual.reviewer_functions_title')}</h4>
              </div>
              <p>{t('manual.reviewer_functions_text')}</p>
            </section>
          </div>
        </div>

        <div className="manual-modal-footer">
          <button className="btn btn-primary start-btn" onClick={onClose}>
            <span>{t('common.dashboard')}</span>
            <ArrowRight size={18} />
          </button>
        </div>
      </div>
    </div>
  );
};

export default UserManualModal;
