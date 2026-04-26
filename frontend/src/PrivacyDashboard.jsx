import { useState, useEffect, useRef } from 'react'
import { BarChart, Bar, RadarChart, Radar, PolarGrid, PolarAngleAxis, PolarRadiusAxis, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import { motion, AnimatePresence } from 'framer-motion'
import { Shield, Activity, Lock, Globe, Database, Fingerprint, EyeOff, ShieldAlert, ChevronRight, Zap, MousePointer2, Radar as RadarIcon } from 'lucide-react'

// ── Fingerprint collectors ────────────────────────────────────────────────────
function getGPUInfo() {
  try {
    const canvas = document.createElement('canvas')
    const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl')
    if (gl) {
      const dbg = gl.getExtension('WEBGL_debug_renderer_info')
      if (dbg) return { vendor: gl.getParameter(dbg.UNMASKED_VENDOR_WEBGL), renderer: gl.getParameter(dbg.UNMASKED_RENDERER_WEBGL) }
    }
  } catch (_) {}
  return { vendor: 'Unknown', renderer: 'Unknown' }
}

function getCanvasFingerprint() {
  try {
    const c = document.createElement('canvas')
    const ctx = c.getContext('2d')
    ctx.textBaseline = 'top'
    ctx.font = '14px Arial'
    ctx.fillStyle = '#f60'
    ctx.fillRect(125, 1, 62, 20)
    ctx.fillStyle = '#069'
    ctx.fillText('MAArK', 2, 15)
    return c.toDataURL().slice(-40)
  } catch (_) { return 'Error' }
}

async function getAudioFP() {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)()
    return 'AudioCtx ' + ctx.sampleRate + 'Hz'
  } catch (_) { return 'Not available' }
}

async function getStorageInfo() {
  if (navigator.storage?.estimate) {
    const e = await navigator.storage.estimate()
    return { quota: (e.quota / 1e9).toFixed(2) + ' GB', usage: (e.usage / 1e6).toFixed(2) + ' MB' }
  }
  return { quota: 'Unknown', usage: 'Unknown' }
}

async function detectAdblock() {
  try {
    await fetch('https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js', { method: 'HEAD', mode: 'no-cors' })
    return false
  } catch (_) { return true }
}

async function detectIncognito() {
  if (navigator.storage?.estimate) {
    const { quota } = await navigator.storage.estimate()
    return quota < 120000000
  }
  return false
}

// ── Mock data injector (privacy protection) ───────────────────────────────────
function injectMockFingerprint() {
  try {
    const isMobile = Math.random() > 0.5;
    const res = isMobile ? [412, 915] : [1920, 1080];
    const cores = isMobile ? 8 : 12;
    const mem = isMobile ? 8 : 32;
    const platform = isMobile ? 'Linux armv81' : 'Win32';
    
    Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => cores, configurable: true })
    Object.defineProperty(navigator, 'deviceMemory',        { get: () => mem, configurable: true })
    Object.defineProperty(navigator, 'language',            { get: () => 'en-US', configurable: true })
    Object.defineProperty(navigator, 'platform',            { get: () => platform, configurable: true })
    Object.defineProperty(screen, 'width',                  { get: () => res[0], configurable: true })
    Object.defineProperty(screen, 'height',                 { get: () => res[1], configurable: true })
    Object.defineProperty(screen, 'colorDepth',             { get: () => 24, configurable: true })
    Object.defineProperty(window, 'devicePixelRatio',       { get: () => (isMobile ? 2.6 : 1), configurable: true })
    
    console.log(`[MAArK] Local Demo: Spoofing as ${platform} (${res[0]}x${res[1]})`);
    return true
  } catch (_) { return false }
}

// ── Stat Card ─────────────────────────────────────────────────────────────────
function StatCard({ label, value, color, icon: Icon }) {
  const gradients = {
    purple: 'linear-gradient(135deg, rgba(124,58,237,0.2), rgba(76,29,149,0.1))',
    blue:   'linear-gradient(135deg, rgba(59,130,246,0.2), rgba(30,58,138,0.1))',
    pink:   'linear-gradient(135deg, rgba(236,72,153,0.2), rgba(131,24,67,0.1))',
    yellow: 'linear-gradient(135deg, rgba(234,179,8,0.2), rgba(113,63,18,0.1))',
    red:    'linear-gradient(135deg, rgba(239,68,68,0.2), rgba(127,29,29,0.1))',
    green:  'linear-gradient(135deg, rgba(34,197,94,0.2), rgba(20,83,45,0.1))'
  }
  const iconColors = {
    purple: '#a855f7', blue: '#3b82f6', pink: '#ec4899', yellow: '#eab308', red: '#ef4444', green: '#22c55e'
  }
  
  return (
    <motion.div 
      whileHover={{ y: -4, scale: 1.02 }}
      transition={{ type: "spring", stiffness: 300 }}
      style={{
        background: gradients[color],
        borderRadius: 24, padding: '24px', border: `1px solid ${iconColors[color]}40`,
        display: 'flex', flexDirection: 'column', gap: 12, flex: 1, minWidth: 160,
        boxShadow: '0 8px 32px rgba(0,0,0,0.2)', backdropFilter: 'blur(10px)'
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontSize: 13, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '1px', fontWeight: 600 }}>{label}</span>
        {Icon && <Icon size={18} color={iconColors[color]} />}
      </div>
      <span style={{ fontSize: 42, fontWeight: 800, color: '#fff', letterSpacing: '-1px' }}>{value}</span>
    </motion.div>
  )
}

// ── Info Card ─────────────────────────────────────────────────────────────────
function InfoCard({ title, data, icon: Icon }) {
  return (
    <motion.div 
      whileHover={{ y: -2 }}
      style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.05)', borderRadius: 20, padding: 24, marginBottom: 16 }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 20 }}>
        {Icon && <div style={{ padding: 8, background: 'rgba(124,58,237,0.1)', borderRadius: 10 }}><Icon size={18} color="var(--accent-violet)" /></div>}
        <div style={{ fontSize: 14, fontWeight: 700, color: '#f0f0f8', letterSpacing: '0.5px' }}>{title}</div>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 16 }}>
        {data && Object.entries(data).map(([k, v]) => (
          <div key={k} style={{ display: 'flex', flexDirection: 'column', gap: 6, background: 'rgba(0,0,0,0.2)', padding: 12, borderRadius: 12 }}>
            <span style={{ color: 'var(--text-muted)', fontSize: 11, textTransform: 'uppercase', fontWeight: 600 }}>{k}</span>
            <span style={{ color: 'var(--accent-cyan)', fontFamily: 'monospace', fontSize: 13, wordBreak: 'break-all' }}>{String(v)}</span>
          </div>
        ))}
      </div>
    </motion.div>
  )
}

// ── Education Card ────────────────────────────────────────────────────────────
function EduCard({ title, desc, risk }) {
  const rc = risk === 'High' ? '#ef4444' : risk === 'Medium' ? '#eab308' : '#3b82f6'
  const bg = risk === 'High' ? 'rgba(239,68,68,0.05)' : risk === 'Medium' ? 'rgba(234,179,8,0.05)' : 'rgba(59,130,246,0.05)'
  const br = risk === 'High' ? 'rgba(239,68,68,0.2)' : risk === 'Medium' ? 'rgba(234,179,8,0.2)' : 'rgba(59,130,246,0.2)'
  return (
    <motion.div 
      whileHover={{ scale: 1.01 }}
      style={{ background: bg, border: `1px solid ${br}`, borderRadius: 20, padding: 28, marginBottom: 20 }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <span style={{ fontWeight: 700, fontSize: 18, color: '#f0f0f8' }}>{title}</span>
        <span style={{ fontSize: 12, fontWeight: 700, color: rc, background: rc + '22', padding: '6px 14px', borderRadius: 999, border: `1px solid ${rc}40` }}>{risk} Risk</span>
      </div>
      <p style={{ fontSize: 15, color: 'var(--text-secondary)', lineHeight: 1.7, margin: 0 }}>{desc}</p>
    </motion.div>
  )
}

// ── Main Export ───────────────────────────────────────────────────────────────
export default function PrivacyDashboard() {
  const [tab, setTab] = useState('overview')
  const [fp, setFp] = useState({})
  const [net, setNet] = useState({})
  const [collecting, setCollecting] = useState(true)
  const [threatScore, setThreatScore] = useState(0)
  const [shieldOn, setShieldOn] = useState(false)
  const [shieldMsg, setShieldMsg] = useState('')
  const [torStatus, setTorStatus] = useState('Disconnected')
  const [torEnabled, setTorEnabled] = useState(false)
  const [behavior, setBehavior] = useState({ clicks: 0, scrolls: 0, keystrokes: 0, tabHides: 0, idleTime: 0, mouseMovements: [] })
  
  const lastActivity = useRef(Date.now())
  const idleTimer = useRef(null)

  // Collect fingerprint
  useEffect(() => {
    async function collect() {
      const gpu = getGPUInfo()
      const canvas = getCanvasFingerprint()
      const audio = await getAudioFP()
      const storage = await getStorageInfo()
      const adblock = await detectAdblock()
      const incognito = await detectIncognito()
      const data = {
        hardware: { cores: navigator.hardwareConcurrency || '?', memory: (navigator.deviceMemory || '?') + ' GB', platform: navigator.platform },
        gpu,
        screen: { width: screen.width, height: screen.height, colorDepth: screen.colorDepth, pixelRatio: devicePixelRatio },
        browser: { language: navigator.language, cookieEnabled: navigator.cookieEnabled, doNotTrack: navigator.doNotTrack },
        privacy: { adblock, incognito, automation: !!navigator.webdriver },
        storage,
        canvas,
        audio,
      }
      setFp(data)
      let score = 0
      if (gpu.vendor !== 'Unknown') score += 15
      if (canvas !== 'Error') score += 15
      if (audio !== 'Not available') score += 10
      if (!adblock) score += 20
      if (!incognito) score += 15
      if (data.browser.cookieEnabled) score += 15
      if (navigator.hardwareConcurrency) score += 10
      setThreatScore(Math.min(score, 100))
    }
    collect()
  }, [shieldOn])

  // Collect network via IPC
  useEffect(() => {
    async function fetchNet() {
      if (window.maark?.getNetworkInfo) {
        const data = await window.maark.getNetworkInfo()
        setNet(data)
      } else {
        setNet({ error: 'Network lookup unavailable' })
      }
    }
    fetchNet()
  }, [])

  // Behavior tracking (window-wide)
  useEffect(() => {
    const reset = () => { lastActivity.current = Date.now(); setBehavior(p => ({ ...p, idleTime: 0 })) }
    const onGlobalMove = () => reset()
    const onClick = () => reset()
    const onScroll = () => reset()
    const onKey = () => reset()
    const onVis = () => {}
    
    // Fetch initial totals
    if (window.maark?.getBiometricTotals) {
      window.maark.getBiometricTotals().then(totals => {
        setBehavior(p => ({ ...p, ...totals }))
      })
    }

    if (collecting) {
      window.addEventListener('mousemove', onGlobalMove)
      window.addEventListener('click', onClick)
      window.addEventListener('scroll', onScroll)
      window.addEventListener('keydown', onKey)
      document.addEventListener('visibilitychange', onVis)

      // Tab Biometrics listener
      const onBiometric = (data) => {
        if (data.totals) {
          setBehavior(p => ({ ...p, ...data.totals }))
        }
        reset()
      }
      
      if (window.maark?.on) {
        window.maark.on('biometric:update', onBiometric)
      }
      
      idleTimer.current = setInterval(() => {
        setBehavior(p => ({ ...p, idleTime: Math.floor((Date.now() - lastActivity.current) / 1000) }))
      }, 1000)
      
      return () => {
        window.removeEventListener('mousemove', onGlobalMove)
        window.removeEventListener('click', onClick)
        window.removeEventListener('scroll', onScroll)
        window.removeEventListener('keydown', onKey)
        document.removeEventListener('visibilitychange', onVis)
        if (window.maark?.off) window.maark.off('biometric:update', onBiometric)
        clearInterval(idleTimer.current)
      }
    }
  }, [collecting])

  // Tor Status Listener
  useEffect(() => {
    if (window.maark?.getTorStatus) {
      window.maark.getTorStatus().then(setTorStatus)
    }
    if (window.maark?.getPrivacyConfig) {
      window.maark.getPrivacyConfig().then(c => setTorEnabled(!!c.torEnabled))
    }

    const onTorStatus = (status) => setTorStatus(status)
    const onPrivacyConfig = (c) => setTorEnabled(!!c.torEnabled)

    window.maark?.on('tor:status', onTorStatus)
    window.maark?.on('privacy:config', onPrivacyConfig)

    return () => {
      if (window.maark?.off) {
        window.maark.off('tor:status', onTorStatus)
        window.maark.off('privacy:config', onPrivacyConfig)
      }
    }
  }, [])

  const toggleTor = () => {
    const next = !torEnabled
    setTorEnabled(next)
    if (window.maark?.setPrivacyConfig) {
      window.maark.setPrivacyConfig({ torEnabled: next })
    }
  }

  const activateShield = () => {
    const ok = injectMockFingerprint()
    setShieldOn(true)
    setShieldMsg(ok ? '✅ Mock fingerprint injected. Navigator properties spoofed globally.' : '⚠️ Partial protection — some APIs could not be overridden.')
    if (window.maark?.setMockFingerprint) {
      window.maark.setMockFingerprint(true)
    }
    if (window.maark?.api?.toggleShield) {
      window.maark.api.toggleShield()
    }
  }

  const exportData = () => {
    const blob = new Blob([JSON.stringify({ fp, net, behavior, threatScore }, null, 2)], { type: 'application/json' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = `maark-privacy-report-${Date.now()}.json`
    a.click()
  }

  const scoreColor = threatScore < 30 ? '#22c55e' : threatScore < 60 ? '#eab308' : '#ef4444'
  const scoreLabel = threatScore < 30 ? 'Low Risk' : threatScore < 60 ? 'Medium Risk' : 'High Risk'

  const radarData = [
    { cat: 'Hardware', score: fp.hardware ? 90 : 0 },
    { cat: 'GPU', score: fp.gpu?.vendor !== 'Unknown' ? 85 : 20 },
    { cat: 'Canvas', score: fp.canvas !== 'Error' ? 95 : 0 },
    { cat: 'Audio', score: fp.audio !== 'Not available' ? 80 : 0 },
    { cat: 'Network', score: net.ip ? 100 : 0 },
    { cat: 'Behavior', score: behavior.clicks > 0 ? 75 : 0 },
  ]

  const barData = [
    { name: 'Clicks', value: behavior.clicks },
    { name: 'Scrolls', value: behavior.scrolls },
    { name: 'Keys', value: behavior.keystrokes },
    { name: 'TabHides', value: behavior.tabHides },
  ]

  const tabsList = [
    { id: 'overview', label: 'Overview', icon: Activity },
    { id: 'technical', label: 'Technical Data', icon: Database },
    { id: 'network', label: 'Network', icon: Globe },
    { id: 'behavior', label: 'Behavior', icon: MousePointer2 },
    { id: 'education', label: 'Learning Center', icon: ShieldAlert },
    { id: 'protect', label: 'Protect', icon: Shield }
  ]

  return (
    <motion.div 
      initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
      className="insights-overlay"
      style={{ position: 'fixed', top: 124, left: 0, right: 0, bottom: 0, background: 'var(--bg-base)', zIndex: 1000, display: 'flex', flexDirection: 'column' }}
    >
      {/* HEADER */}
      <div style={{ padding: '24px 40px', borderBottom: '1px solid rgba(255,255,255,0.08)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'rgba(20, 20, 35, 0.7)', backdropFilter: 'blur(20px)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <div style={{ width: 48, height: 48, borderRadius: 16, background: 'var(--grad-violet)', display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: '0 0 20px rgba(124,58,237,0.4)' }}>
            <Shield size={24} color="#fff" />
          </div>
          <div>
            <h1 style={{ margin: 0, fontSize: 24, fontWeight: 700, color: '#fff', display: 'flex', alignItems: 'center', gap: 12, letterSpacing: '-0.5px' }}>
              Privacy & Security Insights
              {shieldOn && <motion.span initial={{ scale: 0 }} animate={{ scale: 1 }} style={{ fontSize: 12, padding: '4px 12px', background: 'rgba(16,185,129,0.15)', color: '#10b981', borderRadius: 999, border: '1px solid rgba(16,185,129,0.3)', letterSpacing: '0.5px' }}>PROTECTED</motion.span>}
            </h1>
            <p style={{ margin: '4px 0 0 0', fontSize: 14, color: 'var(--text-secondary)' }}>Real-time telemetry and fingerprint analysis.</p>
          </div>
        </div>
      </div>

      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        {/* SIDEBAR NAVIGATION */}
        <div style={{ width: 260, borderRight: '1px solid rgba(255,255,255,0.05)', padding: 24, display: 'flex', flexDirection: 'column', gap: 8, background: 'rgba(10, 10, 15, 0.5)' }}>
          {tabsList.map(t => (
            <button key={t.id} onClick={() => setTab(t.id)} style={{
              display: 'flex', alignItems: 'center', gap: 12,
              padding: '14px 16px', borderRadius: 12, fontSize: 14, fontWeight: 600, cursor: 'pointer', border: 'none', fontFamily: 'inherit',
              background: tab === t.id ? 'rgba(124,58,237,0.15)' : 'transparent',
              color: tab === t.id ? '#c4b5fd' : 'var(--text-muted)',
              transition: 'all 0.2s ease', borderLeft: tab === t.id ? '3px solid var(--accent-violet)' : '3px solid transparent'
            }}>
              <t.icon size={18} />
              {t.label}
              {tab === t.id && <ChevronRight size={16} style={{ marginLeft: 'auto', opacity: 0.5 }} />}
            </button>
          ))}
          
          <div style={{ marginTop: 'auto' }}>
            <div style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.05)', borderRadius: 16, padding: 20 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                <span style={{ fontWeight: 600, fontSize: 13, color: 'var(--text-secondary)' }}>Threat Score</span>
                <span style={{ color: scoreColor, fontWeight: 800, fontSize: 14 }}>{scoreLabel}</span>
              </div>
              <div style={{ height: 8, background: 'rgba(0,0,0,0.5)', borderRadius: 999, overflow: 'hidden', boxShadow: 'inset 0 1px 3px rgba(0,0,0,0.5)' }}>
                <motion.div 
                  initial={{ width: 0 }}
                  animate={{ width: `${threatScore}%` }}
                  transition={{ duration: 1, ease: "easeOut" }}
                  style={{ height: '100%', background: `linear-gradient(90deg, #22c55e, #eab308, #ef4444)`, borderRadius: 999 }} 
                />
              </div>
              <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 12, fontWeight: 600 }}>{threatScore}% trackable</div>
            </div>
          </div>
        </div>

        {/* MAIN CONTENT AREA */}
        <div style={{ flex: 1, padding: 48, overflowY: 'auto', position: 'relative', background: 'radial-gradient(circle at top right, rgba(124,58,237,0.05), transparent 60%)' }}>
          <div style={{ maxWidth: 1100, margin: '0 auto' }}>
            
            <AnimatePresence mode="wait">
              <motion.div
                key={tab}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                transition={{ duration: 0.3 }}
              >
                {/* OVERVIEW */}
                {tab === 'overview' && (
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
                    <div style={{ background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.05)', borderRadius: 24, padding: 32, boxShadow: '0 10px 40px rgba(0,0,0,0.2)' }}>
                      <div style={{ fontSize: 16, fontWeight: 700, color: 'var(--text-primary)', marginBottom: 24, display: 'flex', alignItems: 'center', gap: 8 }}><RadarIcon size={20} color="var(--accent-violet)"/> Tracking Capabilities</div>
                      <ResponsiveContainer width="100%" height={300}>
                        <RadarChart data={radarData}>
                          <PolarGrid stroke="rgba(255,255,255,0.1)" />
                          <PolarAngleAxis dataKey="cat" stroke="var(--text-secondary)" tick={{ fontSize: 13, fontWeight: 500 }} />
                          <PolarRadiusAxis stroke="rgba(255,255,255,0.1)" tick={false} axisLine={false} />
                          <Radar dataKey="score" stroke="var(--accent-violet)" strokeWidth={2} fill="url(#colorUv)" fillOpacity={0.5} />
                          <defs>
                            <linearGradient id="colorUv" x1="0" y1="0" x2="0" y2="1">
                              <stop offset="5%" stopColor="var(--accent-violet)" stopOpacity={0.8}/>
                              <stop offset="95%" stopColor="var(--accent-cyan)" stopOpacity={0.2}/>
                            </linearGradient>
                          </defs>
                        </RadarChart>
                      </ResponsiveContainer>
                    </div>
                    <div style={{ background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.05)', borderRadius: 24, padding: 32, boxShadow: '0 10px 40px rgba(0,0,0,0.2)' }}>
                      <div style={{ fontSize: 16, fontWeight: 700, color: 'var(--text-primary)', marginBottom: 24, display: 'flex', alignItems: 'center', gap: 8 }}><Activity size={20} color="var(--accent-violet)"/> Session Behavior</div>
                      <ResponsiveContainer width="100%" height={300}>
                        <BarChart data={barData} margin={{ top: 20, right: 0, left: -20, bottom: 0 }}>
                          <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} />
                          <XAxis dataKey="name" stroke="var(--text-secondary)" tick={{ fontSize: 13, fontWeight: 500 }} axisLine={false} tickLine={false} />
                          <YAxis stroke="var(--text-muted)" tick={{ fontSize: 12 }} axisLine={false} tickLine={false} />
                          <Tooltip cursor={{fill: 'rgba(255,255,255,0.02)'}} contentStyle={{ background: 'rgba(15,15,24,0.9)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 12, boxShadow: '0 8px 32px rgba(0,0,0,0.5)', color: '#fff' }} itemStyle={{ color: '#fff', fontWeight: 700 }} />
                          <Bar dataKey="value" fill="var(--accent-violet)" radius={[6, 6, 0, 0]} barSize={40} />
                        </BarChart>
                      </ResponsiveContainer>
                    </div>
                    
                    <div 
                      style={{ gridColumn: '1 / -1', background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.05)', borderRadius: 24, padding: 32, position: 'relative', overflow: 'hidden', minHeight: 220 }}
                      onMouseMove={(e) => {
                        if (!collecting) return
                        const rect = e.currentTarget.getBoundingClientRect()
                        const x = e.clientX - rect.left
                        const y = e.clientY - rect.top
                        setBehavior(p => ({ ...p, mouseMovements: [...p.mouseMovements.slice(-200), { x, y }] }))
                        lastActivity.current = Date.now()
                        setBehavior(p => ({ ...p, idleTime: 0 }))
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', position: 'relative', zIndex: 2 }}>
                        <div style={{ fontSize: 16, fontWeight: 700, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: 8 }}><MousePointer2 size={20} color="var(--accent-violet)"/> Interactive Mouse Heatmap ({behavior.mouseMovements.length} pts)</div>
                        <div style={{ fontSize: 13, color: 'var(--text-muted)', background: 'rgba(0,0,0,0.3)', padding: '6px 12px', borderRadius: 8, border: '1px solid rgba(255,255,255,0.05)' }}>Idle: {behavior.idleTime}s</div>
                      </div>
                      <div style={{ marginTop: 12, color: 'var(--text-secondary)', fontSize: 14, position: 'relative', zIndex: 2, maxWidth: 600, lineHeight: 1.6 }}>
                        Move your mouse inside this box to generate tracking data. Websites can track every micro-movement to fingerprint your behavior.
                      </div>
                      
                      {/* The dots */}
                      {behavior.mouseMovements.map((p, i) => (
                        <div key={i} style={{ 
                          position: 'absolute', width: 14, height: 14, borderRadius: '50%', 
                          background: 'var(--accent-violet)', opacity: 0.4, 
                          left: p.x, top: p.y, 
                          transform: 'translate(-50%,-50%)',
                          boxShadow: '0 0 20px 6px rgba(124, 58, 237, 0.4)',
                          pointerEvents: 'none',
                          zIndex: 1
                        }} />
                      ))}
                    </div>
                  </div>
                )}

                {/* TECHNICAL */}
                {tab === 'technical' && (
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
                    <InfoCard title="Hardware Fingerprint" data={fp.hardware} icon={Database} />
                    <InfoCard title="GPU Fingerprint" data={fp.gpu} icon={Activity} />
                    <InfoCard title="Screen Metrics" data={fp.screen} icon={Globe} />
                    <InfoCard title="Browser Headers" data={fp.browser} icon={Fingerprint} />
                    <InfoCard title="Privacy State" data={fp.privacy} icon={Shield} />
                    <InfoCard title="Storage State" data={fp.storage} icon={Database} />
                    <InfoCard title="Canvas Fingerprint" data={{ hash: fp.canvas }} icon={EyeOff} />
                    <InfoCard title="Audio Context" data={{ value: fp.audio }} icon={Activity} />
                  </div>
                )}

                {/* NETWORK */}
                {tab === 'network' && (
                  <div>
                    <InfoCard title="Network Info" data={net} icon={Globe} />
                    
                    <div style={{ display: 'flex', gap: 24, marginTop: 24 }}>
                      <div style={{ flex: 1, background: net.vpn ? 'rgba(239,68,68,0.05)' : 'rgba(16,185,129,0.05)', border: `1px solid ${net.vpn ? 'rgba(239,68,68,0.2)' : 'rgba(16,185,129,0.2)'}`, borderRadius: 24, padding: 40, textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', boxShadow: '0 10px 40px rgba(0,0,0,0.1)' }}>
                        <div style={{ fontSize: 56, marginBottom: 16 }}>{net.vpn ? '🔴' : '🟢'}</div>
                        <div style={{ fontWeight: 800, fontSize: 24, color: net.vpn ? '#ef4444' : '#10b981', letterSpacing: '-0.5px' }}>{net.vpn ? 'VPN / Proxy Detected' : 'No VPN Detected'}</div>
                        <div style={{ fontSize: 15, color: 'var(--text-secondary)', marginTop: 12, lineHeight: 1.6, maxWidth: 300 }}>{net.vpn ? 'Your connection appears to be routed through a proxy or VPN service.' : 'Direct connection identified. Your real IP is exposed.'}</div>
                      </div>
                      
                      <div style={{ flex: 1.5, padding: 40, background: 'rgba(234,179,8,0.05)', border: '1px solid rgba(234,179,8,0.2)', borderRadius: 24, boxShadow: '0 10px 40px rgba(0,0,0,0.1)' }}>
                        <div style={{ fontWeight: 700, fontSize: 18, color: '#eab308', marginBottom: 16, display: 'flex', alignItems: 'center', gap: 10 }}>
                          <ShieldAlert size={24} /> What websites see
                        </div>
                        <p style={{ fontSize: 16, color: '#f0f0f8', lineHeight: 1.7, margin: 0 }}>
                          Your real IP <strong style={{ color: '#fff', background: 'rgba(255,255,255,0.1)', padding: '2px 6px', borderRadius: 6 }}>{net.ip || '...'}</strong> reveals your exact location in <strong style={{ color: '#fff' }}>{net.city}, {net.region}</strong>, your Internet Service Provider (<strong style={{ color: '#fff' }}>{net.isp}</strong>), and your timezone (<strong style={{ color: '#fff' }}>{net.timezone}</strong>).
                        </p>
                        <p style={{ fontSize: 15, color: 'var(--text-secondary)', lineHeight: 1.7, marginTop: 24, marginBottom: 0 }}>
                          This information is automatically sent to every website you visit and is frequently used to build ad targeting profiles or restrict content geographically.
                        </p>
                      </div>
                    </div>
                  </div>
                )}

                {/* BEHAVIOR */}
                {tab === 'behavior' && (
                  <div>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 20, marginBottom: 32 }}>
                      <StatCard label="Clicks" value={behavior.clicks} color="purple" icon={MousePointer2} />
                      <StatCard label="Scrolls" value={behavior.scrolls} color="blue" icon={Activity} />
                      <StatCard label="Keystrokes" value={behavior.keystrokes} color="pink" icon={Database} />
                      <StatCard label="Tab Hides" value={behavior.tabHides} color="red" icon={EyeOff} />
                      <StatCard label="Mouse Moves" value={behavior.mouseMovements.length} color="green" icon={MousePointer2} />
                      <StatCard label="Idle (s)" value={behavior.idleTime} color="yellow" icon={Activity} />
                    </div>
                    
                    <div style={{ padding: 32, background: 'rgba(239,68,68,0.05)', border: '1px solid rgba(239,68,68,0.2)', borderRadius: 24, marginBottom: 32, boxShadow: '0 10px 40px rgba(0,0,0,0.1)' }}>
                      <div style={{ fontWeight: 700, fontSize: 18, color: '#ef4444', marginBottom: 16, display: 'flex', alignItems: 'center', gap: 10 }}>
                        <ShieldAlert size={24} /> Behavioral Biometrics Risk
                      </div>
                      <p style={{ fontSize: 16, color: '#f0f0f8', lineHeight: 1.7, margin: 0 }}>
                        Modern tracking goes beyond cookies and IPs. Your unique click patterns, scroll speed, and typing rhythm create a <strong style={{ color: '#fff' }}>behavioral fingerprint</strong>.
                      </p>
                      <p style={{ fontSize: 15, color: 'var(--text-secondary)', lineHeight: 1.7, marginTop: 16, marginBottom: 0 }}>
                        This means even if you use a VPN and clear all your cookies, trackers can still re-identify you simply based on <em>how</em> you move your mouse and type on your keyboard.
                      </p>
                    </div>
                    
                    <button onClick={() => setCollecting(c => !c)} style={{
                      padding: '16px 32px', borderRadius: 16, cursor: 'pointer', fontFamily: 'inherit', fontWeight: 700, fontSize: 15, width: 'fit-content',
                      background: collecting ? 'rgba(239,68,68,0.1)' : 'rgba(16,185,129,0.1)',
                      color: collecting ? '#ef4444' : '#10b981', border: `1px solid ${collecting ? 'rgba(239,68,68,0.3)' : 'rgba(16,185,129,0.3)'}`,
                      transition: 'all 0.2s', boxShadow: '0 8px 20px rgba(0,0,0,0.1)'
                    }}>
                      {collecting ? '⏹ Stop Behavioral Collection Demo' : '▶ Start Behavioral Collection Demo'}
                    </button>
                  </div>
                )}

                {/* EDUCATION */}
                {tab === 'education' && (
                  <div>
                    <EduCard title="Canvas Fingerprinting" risk="High" desc="Websites render hidden graphics using the HTML5 Canvas API and capture the exact pixel output. Because every GPU, graphics driver, and OS renders fonts and anti-aliasing slightly differently, this produces a highly unique fingerprint used as a persistent tracking ID — no cookies needed." />
                    <EduCard title="WebGL & GPU Tracking" risk="High" desc="Sites query your GPU model and 3D rendering capabilities via WebGL to build a hardware-specific identifier. This exposes your exact graphics card vendor and renderer, which persists across browsers and completely bypasses incognito mode." />
                    <EduCard title="Behavioral Biometrics" risk="Medium" desc="Advanced trackers analyze the velocity of your mouse movements, click patterns, scroll rhythm and typing cadence. These subtle physical characteristics form a unique behavioral signature used to invisibly re-identify users, typically for fraud prevention but increasingly for ad targeting." />
                    <EduCard title="Network Fingerprinting" risk="High" desc="Your IP address reveals your city, ISP, and timezone. WebRTC leaks (used for real-time video/audio) can expose your real IP even when you are connected through a VPN. Every single website you visit logs this basic network data." />
                    <EduCard title="Storage Supercookies" risk="Medium" desc="When standard cookies are blocked or cleared, trackers use alternative storage mechanisms like localStorage, IndexedDB, ETags, and browser cache to store unique IDs. These 'supercookies' survive standard clearing methods, requiring strict browser configurations to defeat." />
                  </div>
                )}

                {/* PROTECT */}
                {tab === 'protect' && (
                  <div style={{ maxWidth: 800 }}>
                    {shieldOn && (
                      <motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} style={{ background: 'rgba(16,185,129,0.1)', border: '1px solid rgba(16,185,129,0.3)', borderRadius: 16, padding: 20, marginBottom: 24, fontSize: 15, color: '#10b981', display: 'flex', alignItems: 'center', gap: 16, fontWeight: 500 }}>
                        <Shield size={28} />
                        {shieldMsg}
                      </motion.div>
                    )}
                    
                    <div style={{ padding: 40, background: 'rgba(16,185,129,0.05)', border: '1px solid rgba(16,185,129,0.2)', borderRadius: 24, marginBottom: 24, boxShadow: '0 10px 40px rgba(0,0,0,0.1)' }}>
                      <div style={{ fontWeight: 800, fontSize: 22, color: '#10b981', marginBottom: 16, display: 'flex', alignItems: 'center', gap: 12 }}><Zap size={24} /> MAArK Privacy Shield</div>
                      <p style={{ fontSize: 16, color: '#f0f0f8', lineHeight: 1.7, marginBottom: 32 }}>
                        Activate to dynamically inject spoofed navigator properties into the page environment. Websites will receive randomized hardware concurrency, memory capacity, and language values instead of your real data, effectively poisoning their fingerprinting attempts.
                      </p>
                      <button onClick={activateShield} disabled={shieldOn} style={{
                        padding: '18px 36px', borderRadius: 16, border: 'none', cursor: shieldOn ? 'default' : 'pointer',
                        background: shieldOn ? 'rgba(16,185,129,0.2)' : '#10b981', color: shieldOn ? '#10b981' : '#fff', 
                        fontWeight: 700, fontSize: 16, fontFamily: 'inherit', transition: 'all 0.2s',
                        boxShadow: shieldOn ? 'none' : '0 8px 24px rgba(16,185,129,0.4)'
                      }}>
                        {shieldOn ? 'Shield is Active' : '⚡ Activate Mock Injection'}
                      </button>
                    </div>

                    <div style={{ padding: 40, background: 'rgba(124,58,237,0.05)', border: '1px solid rgba(124,58,237,0.2)', borderRadius: 24, marginBottom: 24, boxShadow: '0 10px 40px rgba(0,0,0,0.1)' }}>
                      <div style={{ fontWeight: 800, fontSize: 20, color: '#a78bfa', marginBottom: 24, display: 'flex', alignItems: 'center', gap: 12 }}><Shield size={24} /> MAArK Native Protections</div>
                      <div style={{ display: 'grid', gap: 16, fontSize: 15, color: 'var(--text-secondary)', lineHeight: 1.6 }}>
                        <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start' }}><div style={{ color: '#c4b5fd', background: 'rgba(124,58,237,0.1)', padding: 4, borderRadius: '50%' }}>✓</div> <span><strong style={{ color: '#fff' }}>User-Agent Rotation</strong> — MAArK actively rotates your UA string at the network level.</span></div>
                        <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start' }}><div style={{ color: '#c4b5fd', background: 'rgba(124,58,237,0.1)', padding: 4, borderRadius: '50%' }}>✓</div> <span><strong style={{ color: '#fff' }}>Proxy Routing Engine</strong> — Toggle Elite proxies in the Privacy Sidebar to mask your real IP.</span></div>
                        <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start' }}><div style={{ color: '#c4b5fd', background: 'rgba(124,58,237,0.1)', padding: 4, borderRadius: '50%' }}>✓</div> <span><strong style={{ color: '#fff' }}>Ad & Tracker Blocking</strong> — Intrusive ad networks and analytics platforms are blocked before they load.</span></div>
                        <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start' }}><div style={{ color: '#c4b5fd', background: 'rgba(124,58,237,0.1)', padding: 4, borderRadius: '50%' }}>✓</div> <span><strong style={{ color: '#fff' }}>Canvas Noise Injection</strong> — Each canvas read by a website returns slightly randomized pixels, breaking ID hashes.</span></div>
                      </div>
                    </div>

                    <div style={{ padding: 40, background: 'rgba(59,130,246,0.05)', border: '1px solid rgba(59,130,246,0.2)', borderRadius: 24, marginBottom: 32, boxShadow: '0 10px 40px rgba(0,0,0,0.1)' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                        <div style={{ fontWeight: 800, fontSize: 22, color: '#3b82f6', display: 'flex', alignItems: 'center', gap: 12 }}><Globe size={24} /> Tor Onion Routing</div>
                        <div style={{ 
                          fontSize: 13, fontWeight: 700, padding: '6px 16px', borderRadius: 999,
                          background: torStatus === 'Connected' ? 'rgba(16,185,129,0.15)' : 'rgba(59,130,246,0.15)',
                          color: torStatus === 'Connected' ? '#10b981' : '#3b82f6',
                          border: `1px solid ${torStatus === 'Connected' ? 'rgba(16,185,129,0.4)' : 'rgba(59,130,246,0.4)'}`
                        }}>
                          {torStatus}
                        </div>
                      </div>
                      <p style={{ fontSize: 16, color: '#f0f0f8', lineHeight: 1.7, marginBottom: 24 }}>
                        Tor masks your IP by bouncing your traffic through three volunteer relays. This provides the highest level of anonymity and makes your real identity nearly impossible to trace.
                      </p>
                      <button onClick={toggleTor} style={{
                        padding: '16px 32px', borderRadius: 16, border: 'none', cursor: 'pointer',
                        background: torEnabled ? 'rgba(239,68,68,0.2)' : '#3b82f6', 
                        color: torEnabled ? '#ef4444' : '#fff',
                        fontWeight: 700, fontSize: 15, fontFamily: 'inherit', transition: 'all 0.2s',
                        boxShadow: torEnabled ? 'none' : '0 8px 24px rgba(59,130,246,0.4)'
                      }}>
                        {torEnabled ? 'Disable Tor Routing' : 'Enable Tor Routing'}
                      </button>
                      {torEnabled && torStatus !== 'Connected' && (
                        <div style={{ marginTop: 16, fontSize: 14, color: 'var(--text-secondary)', fontWeight: 500, display: 'flex', alignItems: 'center', gap: 8 }}>
                          <span style={{ display: 'inline-block', width: 8, height: 8, borderRadius: '50%', background: '#3b82f6', animation: 'pulse 1.5s infinite' }} /> Bootstrapping the onion network... This may take up to 60 seconds.
                        </div>
                      )}
                    </div>

                    <button onClick={exportData} style={{
                      padding: '20px 32px', borderRadius: 16, border: '1px solid rgba(124,58,237,0.3)',
                      background: 'rgba(124,58,237,0.1)', color: '#c4b5fd', cursor: 'pointer', fontFamily: 'inherit', fontWeight: 700, fontSize: 16, transition: 'all 0.2s', width: '100%',
                      boxShadow: '0 8px 24px rgba(0,0,0,0.2)'
                    }}>
                      💾 Export Full Privacy Report JSON
                    </button>
                  </div>
                )}
              </motion.div>
            </AnimatePresence>
          </div>
        </div>
      </div>
    </motion.div>
  )
}

