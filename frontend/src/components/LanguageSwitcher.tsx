import React, { useState, useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { ChevronDown, Globe } from 'lucide-react';
import './LanguageSwitcher.css';

const LanguageSwitcher: React.FC = () => {
  const { i18n } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const languages = [
    { 
      code: 'en', 
      label: 'English', 
      flag: (
        <svg viewBox="0 0 60 30" width="20">
          <clipPath id="s"><path d="M0,0 v30 h60 v-30 z"/></clipPath>
          <path d="M0,0 v30 h60 v-30 z" fill="#012169"/>
          <path d="M0,0 L60,30 M60,0 L0,30" stroke="#fff" strokeWidth="6"/>
          <path d="M0,0 L60,30 M60,0 L0,30" stroke="#C8102E" strokeWidth="4"/>
          <path d="M30,0 v30 M0,15 h60" stroke="#fff" strokeWidth="10"/>
          <path d="M30,0 v30 M0,15 h60" stroke="#C8102E" strokeWidth="6"/>
        </svg>
      )
    },
    { 
      code: 'de', 
      label: 'Deutsch', 
      flag: (
        <svg viewBox="0 0 5 3" width="20">
          <rect width="5" height="3" y="0" fill="#000"/>
          <rect width="5" height="2" y="1" fill="#D00"/>
          <rect width="5" height="1" y="2" fill="#FFCE00"/>
        </svg>
      )
    },
    { 
      code: 'fr', 
      label: 'Français', 
      flag: (
        <svg viewBox="0 0 3 2" width="20">
          <rect width="1" height="2" x="0" fill="#002395"/>
          <rect width="1" height="2" x="1" fill="#FFF"/>
          <rect width="1" height="2" x="2" fill="#ED2939"/>
        </svg>
      )
    },
    { 
      code: 'ar', 
      label: 'العربية', 
      flag: (
        <svg viewBox="0 0 3 2" width="20">
          <rect width="3" height="2" fill="#c1272d"/>
          <path 
            d="M1.5 0.75 L1.647 1.185 L1.262 0.915 L1.738 0.915 L1.353 1.185 Z" 
            fill="none" 
            stroke="#006233" 
            strokeWidth="0.06"
            transform="translate(0, 0.1) scale(1.2) translate(0, -0.1)"
          />
        </svg>
      )
    }
  ];

  const currentLanguage = languages.find(l => i18n.language?.startsWith(l.code)) || languages[1]; // Fallback to DE

  const changeLanguage = (lng: string) => {
    i18n.changeLanguage(lng);
    setIsOpen(false);
  };

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div className="language-dropdown-container" ref={dropdownRef}>
      <button 
        className={`language-toggle ${isOpen ? 'active' : ''}`}
        onClick={() => setIsOpen(!isOpen)}
      >
        <Globe size={18} className="globe-icon" />
        <span className="current-code">{currentLanguage.code.toUpperCase()}</span>
        <span className="current-flag">{currentLanguage.flag}</span>
        <ChevronDown size={16} className={`chevron ${isOpen ? 'rotate' : ''}`} />
      </button>

      {isOpen && (
        <div className="language-menu">
          {languages.map((lang) => (
            <button
              key={lang.code}
              onClick={() => changeLanguage(lang.code)}
              className={`menu-item ${currentLanguage.code === lang.code ? 'selected' : ''}`}
            >
              <span className="menu-flag">{lang.flag}</span>
              <span className="menu-label">{lang.label}</span>
              <span className="menu-code">{lang.code.toUpperCase()}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
};

export default LanguageSwitcher;
