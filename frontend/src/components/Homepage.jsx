import { useState, useEffect } from 'react';
import { Search } from 'lucide-react';
import { motion } from 'framer-motion';

export default function Homepage({ recentSites, onNavigate }) {
  const [query, setQuery] = useState('');
  const [isFocused, setIsFocused] = useState(false);
  const [time, setTime] = useState(new Date());

  useEffect(() => {
    const timer = setInterval(() => setTime(new Date()), 60000);
    return () => clearInterval(timer);
  }, []);

  const handleSearch = (e) => {
    e.preventDefault();
    if (!query.trim()) return;
    const url = query.includes('.') 
      ? (query.startsWith('http') ? query : `https://${query}`) 
      : `https://www.startpage.com/do/search?q=${encodeURIComponent(query)}`;
    onNavigate(url);
  };

  return (
    <div style={{
      position: 'absolute', top: 124, left: 0, right: 0, bottom: 0,
      display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
      background: 'var(--bg-base)', zIndex: 1, padding: 40,
      backgroundImage: 'radial-gradient(circle at 50% -20%, rgba(124, 58, 237, 0.15), transparent 50%)'
    }}>
      
      <motion.div 
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.8, ease: "easeOut" }}
        style={{ position: 'absolute', top: 40, right: 60, textAlign: 'right' }}
      >
        <div style={{ fontSize: 48, fontWeight: 300, color: 'var(--text-primary)', letterSpacing: '-1px' }}>
          {time.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
        </div>
        <div style={{ fontSize: 14, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '1px' }}>
          {time.toLocaleDateString([], { weekday: 'long', month: 'long', day: 'numeric' })}
        </div>
      </motion.div>

      <motion.div 
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, ease: "easeOut" }}
        style={{ display: 'flex', fontSize: 110, fontWeight: 800, letterSpacing: '-6px', marginBottom: 8, filter: 'drop-shadow(0 0 40px rgba(124,58,237,0.2))' }}
      >
        <span style={{ color: 'var(--accent-violet)' }}>M</span>
        <span style={{ color: 'var(--text-primary)' }}>A</span>
        <span style={{ color: 'var(--accent-cyan)' }}>A</span>
        <span style={{ color: 'var(--text-primary)' }}>r</span>
        <span style={{ color: 'var(--text-primary)' }}>K</span>
      </motion.div>
      
      <motion.p 
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.3, duration: 0.6 }}
        style={{ color: 'var(--text-muted)', fontSize: 18, letterSpacing: '4px', marginBottom: 64, textTransform: 'uppercase', fontWeight: 500 }}
      >
        Privacy, Redefined
      </motion.p>

      <motion.form 
        onSubmit={handleSearch}
        initial={{ scale: 0.95, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ delay: 0.2, duration: 0.5, type: 'spring', bounce: 0.4 }}
        style={{
          width: '100%', maxWidth: 720, display: 'flex', alignItems: 'center', gap: 16,
          background: isFocused ? 'rgba(25, 25, 45, 0.9)' : 'rgba(20, 20, 35, 0.7)',
          border: `1px solid ${isFocused ? 'rgba(124,58,237,0.5)' : 'rgba(255,255,255,0.08)'}`,
          padding: '16px 28px', borderRadius: '32px',
          boxShadow: isFocused ? '0 0 0 4px rgba(124,58,237,0.1), 0 10px 40px rgba(0,0,0,0.5)' : '0 8px 32px rgba(0,0,0,0.4)',
          transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)', backdropFilter: 'blur(20px)'
        }}
      >
        <Search size={24} color={isFocused ? 'var(--accent-violet)' : 'var(--text-muted)'} style={{ transition: 'color 0.3s' }} />
        <input 
          type="text" 
          placeholder="Search securely or type a URL..." 
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onFocus={() => setIsFocused(true)}
          onBlur={() => setIsFocused(false)}
          autoFocus
          style={{
            flex: 1, background: 'transparent', border: 'none', outline: 'none',
            color: 'var(--text-primary)', fontSize: 20, padding: '4px 0', fontFamily: 'inherit', fontWeight: 400
          }}
        />
        <div style={{ padding: '4px 10px', background: 'rgba(255,255,255,0.05)', borderRadius: '6px', color: 'var(--text-muted)', fontSize: 12, fontWeight: 600 }}>
          ⌘K
        </div>
      </motion.form>

      {recentSites && recentSites.length > 0 && (
        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5, duration: 0.5 }}
          style={{ display: 'flex', gap: 20, marginTop: 64, flexWrap: 'wrap', justifyContent: 'center', maxWidth: 800 }}
        >
          {recentSites.slice(0, 6).map((site, i) => (
            <div 
              key={i} 
              onClick={() => onNavigate(site.url)}
              style={{
                display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12,
                padding: '20px', background: 'rgba(255,255,255,0.02)',
                border: '1px solid rgba(255,255,255,0.04)', borderRadius: '24px', cursor: 'pointer',
                transition: 'all 0.2s ease', minWidth: 110
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.background = 'rgba(255,255,255,0.05)';
                e.currentTarget.style.borderColor = 'rgba(124,58,237,0.3)';
                e.currentTarget.style.transform = 'translateY(-4px)';
                e.currentTarget.style.boxShadow = '0 10px 30px rgba(0,0,0,0.3)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = 'rgba(255,255,255,0.02)';
                e.currentTarget.style.borderColor = 'rgba(255,255,255,0.04)';
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = 'none';
              }}
            >
              <div style={{
                width: 56, height: 56, borderRadius: '50%', background: 'linear-gradient(135deg, rgba(255,255,255,0.05), rgba(255,255,255,0.01))',
                display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 24,
                color: 'var(--text-primary)', boxShadow: 'inset 0 1px 1px rgba(255,255,255,0.1), 0 4px 12px rgba(0,0,0,0.2)',
                border: '1px solid rgba(255,255,255,0.05)'
              }}>
                {site.title?.[0] || site.url.replace(/^https?:\/\/(www\.)?/, '')[0].toUpperCase() || 'W'}
              </div>
              <span style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-secondary)', maxWidth: 90, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {site.title || site.url.replace(/^https?:\/\/(www\.)?/, '').split('/')[0]}
              </span>
            </div>
          ))}
        </motion.div>
      )}
    </div>
  );
}
