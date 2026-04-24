import { useState, useEffect, useRef } from 'react'
import { BarChart, Bar, RadarChart, Radar, PolarGrid, PolarAngleAxis, PolarRadiusAxis, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'

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
function StatCard({ label, value, color }) {
  const gradients = {
    purple: 'linear-gradient(135deg, #7c3aed, #4c1d95)',
    blue:   'linear-gradient(135deg, #3b82f6, #1e3a8a)',
    pink:   'linear-gradient(135deg, #ec4899, #831843)',
    yellow: 'linear-gradient(135deg, #eab308, #713f12)',
    red:    'linear-gradient(135deg, #ef4444, #7f1d1d)',
    green:  'linear-gradient(135deg, #22c55e, #14532d)'
  }
  
  return (
    <div style={{
      background: gradients[color],
      borderRadius: 16, padding: '24px', border: '1px solid rgba(255,255,255,0.1)',
      display: 'flex', flexDirection: 'column', gap: 8, flex: 1, minWidth: 150
    }}>
      <span style={{ fontSize: 36, fontWeight: 700, color: '#fff' }}>{value}</span>
      <span style={{ fontSize: 13, color: 'rgba(255,255,255,0.8)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>{label}</span>
    </div>
  )
}

// ── Info Card ─────────────────────────────────────────────────────────────────
function InfoCard({ title, data }) {
  return (
    <div style={{ background: 'rgba(124,58,237,0.05)', border: '1px solid rgba(124,58,237,0.2)', borderRadius: 16, padding: 20, marginBottom: 16 }}>
      <div style={{ fontSize: 13, fontWeight: 700, color: '#a78bfa', textTransform: 'uppercase', letterSpacing: '0.08em', marginBottom: 16 }}>{title}</div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))', gap: 12 }}>
        {data && Object.entries(data).map(([k, v]) => (
          <div key={k} style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            <span style={{ color: '#9898B8', fontSize: 12, textTransform: 'uppercase' }}>{k}</span>
            <span style={{ color: '#c4b5fd', fontFamily: 'monospace', fontSize: 14, wordBreak: 'break-all' }}>{String(v)}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

// ── Education Card ────────────────────────────────────────────────────────────
function EduCard({ title, desc, risk }) {
  const rc = risk === 'High' ? '#ef4444' : risk === 'Medium' ? '#eab308' : '#3b82f6'
  return (
    <div style={{ background: 'rgba(124,58,237,0.05)', border: '1px solid rgba(124,58,237,0.2)', borderRadius: 16, padding: 24, marginBottom: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <span style={{ fontWeight: 600, fontSize: 18, color: '#f0f0f8' }}>{title}</span>
        <span style={{ fontSize: 12, fontWeight: 600, color: rc, background: rc + '22', padding: '4px 12px', borderRadius: 999 }}>{risk} Risk</span>
      </div>
      <p style={{ fontSize: 14, color: '#9898B8', lineHeight: 1.6, margin: 0 }}>{desc}</p>
    </div>
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
    const onClick = () => { setBehavior(p => ({ ...p, clicks: p.clicks + 1 })); reset() }
    const onScroll = () => { setBehavior(p => ({ ...p, scrolls: p.scrolls + 1 })); reset() }
    const onKey = () => { setBehavior(p => ({ ...p, keystrokes: p.keystrokes + 1 })); reset() }
    const onVis = () => { if (document.hidden) setBehavior(p => ({ ...p, tabHides: p.tabHides + 1 })) }
    
    if (collecting) {
      window.addEventListener('mousemove', onGlobalMove)
      window.addEventListener('click', onClick)
      window.addEventListener('scroll', onScroll)
      window.addEventListener('keydown', onKey)
      document.addEventListener('visibilitychange', onVis)

      // Tab Biometrics listener
      const onBiometric = (data) => {
        if (data.type === 'click') setBehavior(p => ({ ...p, clicks: p.clicks + 1 }))
        if (data.type === 'keystroke') setBehavior(p => ({ ...p, keystrokes: p.keystrokes + 1 }))
        if (data.type === 'scroll') setBehavior(p => ({ ...p, scrolls: p.scrolls + 1 }))
        if (data.type === 'tabHide') setBehavior(p => ({ ...p, tabHides: p.tabHides + 1 }))
        if (data.type === 'move') setBehavior(p => ({ ...p, mouseMovements: [...p.mouseMovements, Date.now()] })) // Count as movement
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

  const tabsList = ['overview', 'technical', 'network', 'behavior', 'education', 'protect']

  return (
    <div style={{
      position: 'absolute', top: 132, left: 0, right: 0, bottom: 0, 
      backgroundColor: '#0D0D14', zIndex: 1000, display: 'flex', flexDirection: 'column',
      color: '#f0f0f8', overflow: 'hidden'
    }}>


      {/* HEADER */}
      <div style={{ padding: '24px 40px', borderBottom: '1px solid rgba(255,255,255,0.05)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: '#13131F' }}>
        <div>
          <h1 style={{ margin: 0, fontSize: 24, fontWeight: 600, color: '#fff', display: 'flex', alignItems: 'center', gap: 12 }}>
            🔍 Privacy & Security Insights
            {shieldOn && <span style={{ fontSize: 12, padding: '4px 10px', background: 'rgba(16,185,129,0.2)', color: '#10b981', borderRadius: 999, border: '1px solid rgba(16,185,129,0.4)' }}>Protected</span>}
          </h1>
          <p style={{ margin: '8px 0 0 0', fontSize: 14, color: '#9898B8' }}>Monitor and control the fingerprint and behavior data you expose to the web.</p>
        </div>
      </div>

      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        {/* SIDEBAR NAVIGATION */}
        <div style={{ width: 240, borderRight: '1px solid rgba(255,255,255,0.05)', padding: 24, display: 'flex', flexDirection: 'column', gap: 8, background: '#101018' }}>
          {tabsList.map(t => (
            <button key={t} onClick={() => setTab(t)} style={{
              padding: '12px 16px', borderRadius: 8, fontSize: 14, fontWeight: 600, cursor: 'pointer', border: 'none', fontFamily: 'inherit', textTransform: 'capitalize', textAlign: 'left',
              background: tab === t ? 'rgba(124,58,237,0.15)' : 'transparent',
              color: tab === t ? '#c4b5fd' : '#9898B8',
              transition: 'all 0.15s'
            }}>
              {t === 'overview' ? '📊 Overview' : t === 'technical' ? '🖥️ Technical Data' : t === 'network' ? '📡 Network' : t === 'behavior' ? '🖱️ Behavior' : t === 'education' ? '📚 Learning Center' : '🛡️ Protect'}
            </button>
          ))}
          
          <div style={{ marginTop: 'auto' }}>
            <div style={{ background: 'rgba(124,58,237,0.1)', border: '1px solid rgba(124,58,237,0.25)', borderRadius: 12, padding: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                <span style={{ fontWeight: 600, fontSize: 13 }}>Threat Score</span>
                <span style={{ color: scoreColor, fontWeight: 700, fontSize: 13 }}>{scoreLabel}</span>
              </div>
              <div style={{ height: 6, background: 'rgba(255,255,255,0.1)', borderRadius: 999, overflow: 'hidden' }}>
                <div style={{ height: '100%', width: threatScore + '%', background: `linear-gradient(90deg, #22c55e, #eab308, #ef4444)`, borderRadius: 999, transition: 'width 1s ease' }} />
              </div>
              <div style={{ fontSize: 11, color: '#9898B8', marginTop: 8 }}>{threatScore}% trackable</div>
            </div>
          </div>
        </div>

        {/* MAIN CONTENT AREA */}
        <div style={{ flex: 1, padding: 40, overflowY: 'auto', position: 'relative' }}>
          <div style={{ maxWidth: 1000, margin: '0 auto' }}>
            
            {/* OVERVIEW */}
            {tab === 'overview' && (
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
                <div style={{ background: 'rgba(124,58,237,0.05)', border: '1px solid rgba(124,58,237,0.2)', borderRadius: 16, padding: 24 }}>
                  <div style={{ fontSize: 14, fontWeight: 600, color: '#a78bfa', marginBottom: 16 }}>📡 Tracking Capabilities</div>
                  <ResponsiveContainer width="100%" height={300}>
                    <RadarChart data={radarData}>
                      <PolarGrid stroke="rgba(124,58,237,0.3)" />
                      <PolarAngleAxis dataKey="cat" stroke="#c4b5fd" tick={{ fontSize: 12 }} />
                      <PolarRadiusAxis stroke="rgba(124,58,237,0.5)" tick={{ fontSize: 10 }} />
                      <Radar dataKey="score" stroke="#a855f7" fill="#a855f7" fillOpacity={0.4} />
                    </RadarChart>
                  </ResponsiveContainer>
                </div>
                <div style={{ background: 'rgba(124,58,237,0.05)', border: '1px solid rgba(124,58,237,0.2)', borderRadius: 16, padding: 24 }}>
                  <div style={{ fontSize: 14, fontWeight: 600, color: '#a78bfa', marginBottom: 16 }}>📊 Session Behavior</div>
                  <ResponsiveContainer width="100%" height={300}>
                    <BarChart data={barData}>
                      <CartesianGrid strokeDasharray="3 3" stroke="rgba(124,58,237,0.15)" vertical={false} />
                      <XAxis dataKey="name" stroke="#c4b5fd" tick={{ fontSize: 12 }} axisLine={false} tickLine={false} />
                      <YAxis stroke="#c4b5fd" tick={{ fontSize: 12 }} axisLine={false} tickLine={false} />
                      <Tooltip contentStyle={{ background: '#13131F', border: '1px solid rgba(124,58,237,0.3)', borderRadius: 8, fontSize: 13 }} cursor={{fill: 'rgba(124,58,237,0.1)'}} />
                      <Bar dataKey="value" fill="#a855f7" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
                
                <div 
                  style={{ gridColumn: '1 / -1', background: 'rgba(124,58,237,0.05)', border: '1px solid rgba(124,58,237,0.2)', borderRadius: 16, padding: 24, position: 'relative', overflow: 'hidden', minHeight: 180 }}
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
                    <div style={{ fontSize: 14, fontWeight: 600, color: '#a78bfa' }}>🖱️ Interactive Mouse Heatmap ({behavior.mouseMovements.length} pts)</div>
                    <div style={{ fontSize: 12, color: '#9898B8' }}>Idle: {behavior.idleTime}s</div>
                  </div>
                  <div style={{ marginTop: 12, color: '#9898B8', fontSize: 14, position: 'relative', zIndex: 2 }}>
                    Move your mouse inside this box to generate tracking data. Websites can track every micro-movement to fingerprint your behavior.
                  </div>
                  
                  {/* The dots */}
                  {behavior.mouseMovements.map((p, i) => (
                    <div key={i} style={{ 
                      position: 'absolute', width: 12, height: 12, borderRadius: '50%', 
                      background: '#a855f7', opacity: 0.3, 
                      left: p.x, top: p.y, 
                      transform: 'translate(-50%,-50%)',
                      boxShadow: '0 0 15px 4px rgba(168, 85, 247, 0.3)',
                      pointerEvents: 'none',
                      zIndex: 1
                    }} />
                  ))}
                </div>
              </div>
            )}

            {/* TECHNICAL */}
            {tab === 'technical' && (
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
                <InfoCard title="🖥 Hardware Fingerprint" data={fp.hardware} />
                <InfoCard title="🎮 GPU Fingerprint" data={fp.gpu} />
                <InfoCard title="📺 Screen Metrics" data={fp.screen} />
                <InfoCard title="🌐 Browser Headers" data={fp.browser} />
                <InfoCard title="🔒 Privacy State" data={fp.privacy} />
                <InfoCard title="💾 Storage State" data={fp.storage} />
                <InfoCard title="🎨 Canvas Fingerprint" data={{ hash: fp.canvas }} />
                <InfoCard title="🔊 Audio Context" data={{ value: fp.audio }} />
              </div>
            )}

            {/* NETWORK */}
            {tab === 'network' && (
              <div>
                <InfoCard title="📡 Network Info" data={net} />
                
                <div style={{ display: 'flex', gap: 24, marginTop: 24 }}>
                  <div style={{ flex: 1, background: net.vpn ? 'rgba(239,68,68,0.05)' : 'rgba(16,185,129,0.05)', border: `1px solid ${net.vpn ? 'rgba(239,68,68,0.3)' : 'rgba(16,185,129,0.3)'}`, borderRadius: 16, padding: 32, textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
                    <div style={{ fontSize: 48, marginBottom: 12 }}>{net.vpn ? '🔴' : '🟢'}</div>
                    <div style={{ fontWeight: 700, fontSize: 20, color: net.vpn ? '#ef4444' : '#10b981' }}>{net.vpn ? 'VPN / Proxy Detected' : 'No VPN Detected'}</div>
                    <div style={{ fontSize: 14, color: '#9898B8', marginTop: 8 }}>{net.vpn ? 'Your connection appears to be routed through a proxy or VPN service.' : 'Direct connection identified. Your real IP is exposed.'}</div>
                  </div>
                  
                  <div style={{ flex: 1, padding: 32, background: 'rgba(234,179,8,0.05)', border: '1px solid rgba(234,179,8,0.2)', borderRadius: 16 }}>
                    <div style={{ fontWeight: 600, fontSize: 16, color: '#eab308', marginBottom: 12, display: 'flex', alignItems: 'center', gap: 8 }}>
                      ⚠️ What websites see
                    </div>
                    <p style={{ fontSize: 15, color: '#f0f0f8', lineHeight: 1.6, margin: 0 }}>
                      Your real IP <strong>{net.ip || '...'}</strong> reveals your exact location in <strong>{net.city}, {net.region}</strong>, your Internet Service Provider (<strong>{net.isp}</strong>), and your timezone (<strong>{net.timezone}</strong>).
                    </p>
                    <p style={{ fontSize: 14, color: '#9898B8', lineHeight: 1.6, marginTop: 16, marginBottom: 0 }}>
                      This information is automatically sent to every website you visit and is frequently used to build ad targeting profiles or restrict content geographically.
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* BEHAVIOR */}
            {tab === 'behavior' && (
              <div>
                <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginBottom: 24 }}>
                  <StatCard label="Clicks" value={behavior.clicks} color="purple" />
                  <StatCard label="Scrolls" value={behavior.scrolls} color="blue" />
                  <StatCard label="Keystrokes" value={behavior.keystrokes} color="pink" />
                  <StatCard label="Tab Hides" value={behavior.tabHides} color="red" />
                  <StatCard label="Mouse Moves" value={behavior.mouseMovements.length} color="green" />
                  <StatCard label="Idle (s)" value={behavior.idleTime} color="yellow" />
                </div>
                
                <div style={{ padding: 24, background: 'rgba(239,68,68,0.05)', border: '1px solid rgba(239,68,68,0.2)', borderRadius: 16, marginBottom: 24 }}>
                  <div style={{ fontWeight: 600, fontSize: 16, color: '#ef4444', marginBottom: 12 }}>🚨 Behavioral Biometrics Risk</div>
                  <p style={{ fontSize: 15, color: '#f0f0f8', lineHeight: 1.6, margin: 0 }}>
                    Modern tracking goes beyond cookies and IPs. Your unique click patterns, scroll speed, and typing rhythm create a <strong>behavioral fingerprint</strong>.
                  </p>
                  <p style={{ fontSize: 14, color: '#9898B8', lineHeight: 1.6, marginTop: 12, marginBottom: 0 }}>
                    This means even if you use a VPN and clear all your cookies, trackers can still re-identify you simply based on <em>how</em> you move your mouse and type on your keyboard.
                  </p>
                </div>
                
                <button onClick={() => setCollecting(c => !c)} style={{
                  padding: '16px 24px', borderRadius: 12, border: 'none', cursor: 'pointer', fontFamily: 'inherit', fontWeight: 600, fontSize: 14, width: 'fit-content',
                  background: collecting ? 'rgba(239,68,68,0.1)' : 'rgba(16,185,129,0.1)',
                  color: collecting ? '#ef4444' : '#10b981', border: `1px solid ${collecting ? 'rgba(239,68,68,0.3)' : 'rgba(16,185,129,0.3)'}`
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
                  <div style={{ background: 'rgba(16,185,129,0.1)', border: '1px solid rgba(16,185,129,0.3)', borderRadius: 12, padding: 16, marginBottom: 24, fontSize: 14, color: '#10b981', display: 'flex', alignItems: 'center', gap: 12 }}>
                    <span style={{ fontSize: 24 }}>🛡️</span>
                    {shieldMsg}
                  </div>
                )}
                
                <div style={{ padding: 32, background: 'rgba(16,185,129,0.05)', border: '1px solid rgba(16,185,129,0.2)', borderRadius: 16, marginBottom: 24 }}>
                  <div style={{ fontWeight: 700, fontSize: 20, color: '#10b981', marginBottom: 12 }}>MAArK Privacy Shield</div>
                  <p style={{ fontSize: 15, color: '#f0f0f8', lineHeight: 1.6, marginBottom: 24 }}>
                    Activate to dynamically inject spoofed navigator properties into the page environment. Websites will receive randomized hardware concurrency, memory capacity, and language values instead of your real data, effectively poisoning their fingerprinting attempts.
                  </p>
                  <button onClick={activateShield} disabled={shieldOn} style={{
                    padding: '16px 32px', borderRadius: 12, border: 'none', cursor: shieldOn ? 'default' : 'pointer',
                    background: shieldOn ? 'rgba(16,185,129,0.2)' : '#10b981', color: shieldOn ? '#10b981' : '#fff', 
                    fontWeight: 700, fontSize: 15, fontFamily: 'inherit', transition: 'all 0.2s'
                  }}>
                    {shieldOn ? 'Shield is Active' : '⚡ Activate Mock Injection'}
                  </button>
                </div>

                <div style={{ padding: 24, background: 'rgba(124,58,237,0.05)', border: '1px solid rgba(124,58,237,0.2)', borderRadius: 16, marginBottom: 24 }}>
                  <div style={{ fontWeight: 600, fontSize: 16, color: '#a78bfa', marginBottom: 16 }}>📤 MAArK Browser Native Protections</div>
                  <div style={{ display: 'grid', gap: 12, fontSize: 14, color: '#9898B8' }}>
                    <div style={{ display: 'flex', gap: 12 }}><span style={{ color: '#c4b5fd' }}>✓</span> <span><strong style={{ color: '#f0f0f8' }}>User-Agent Rotation</strong> — MAArK actively rotates your UA string at the network level.</span></div>
                    <div style={{ display: 'flex', gap: 12 }}><span style={{ color: '#c4b5fd' }}>✓</span> <span><strong style={{ color: '#f0f0f8' }}>Proxy Routing Engine</strong> — Toggle Elite proxies in the Privacy Sidebar to mask your real IP.</span></div>
                    <div style={{ display: 'flex', gap: 12 }}><span style={{ color: '#c4b5fd' }}>✓</span> <span><strong style={{ color: '#f0f0f8' }}>Ad & Tracker Blocking</strong> — Intrusive ad networks and analytics platforms are blocked before they load.</span></div>
                    <div style={{ display: 'flex', gap: 12 }}><span style={{ color: '#c4b5fd' }}>✓</span> <span><strong style={{ color: '#f0f0f8' }}>Canvas Noise Injection</strong> — Each canvas read by a website returns slightly randomized pixels, breaking ID hashes.</span></div>
                  </div>
                </div>

                <button onClick={exportData} style={{
                  padding: '16px 24px', borderRadius: 12, border: '1px solid rgba(124,58,237,0.3)',
                  background: 'rgba(124,58,237,0.1)', color: '#c4b5fd', cursor: 'pointer', fontFamily: 'inherit', fontWeight: 600, fontSize: 14, transition: 'all 0.2s', width: '100%'
                }}>
                  💾 Export Full Privacy Report JSON
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

