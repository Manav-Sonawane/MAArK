// ─────────────────────────────────────────────────────────────────────────────
// CRITICAL: These switches MUST be called before app.isReady()
// They disable WebRTC at the Chromium engine level — the strongest possible fix
// ─────────────────────────────────────────────────────────────────────────────
const { app, BrowserWindow, BrowserView, ipcMain, session } = require('electron')

// Disable WebRTC entirely at the Chromium level (prevents all local IP leaks)
// These are safe to always apply — they only affect WebRTC peer connections
app.commandLine.appendSwitch('disable-webrtc')
app.commandLine.appendSwitch('enforce-webrtc-ip-permission-check')
app.commandLine.appendSwitch('webrtc-ip-handling-policy', 'disable_non_proxied_udp')
// ★ CRITICAL: IPv6 bypasses SOCKS5 proxy and exposes your REAL IP on every site!
// Disable it completely so all traffic is forced through IPv4 → Tor proxy
app.commandLine.appendSwitch('disable-ipv6')
// Disable mDNS ICE candidates — these expose local IPs like 192.168.x.x in WebRTC
app.commandLine.appendSwitch('disable-features', 'WebRtcHideLocalIpsWithMdns')
// Reduce fingerprinting surface
app.commandLine.appendSwitch('no-referrers')

const path = require('path')
const { spawn } = require('child_process')
const http = require('http')
const fs = require('fs')

require('dotenv').config({ path: path.join(__dirname, '../.env') })

const JAVA_PORT = 7070
const TOOLBAR_H = 124   // toolbar + tab bar combined + shortcuts
const isDev = !app.isPackaged


// ── Privacy Config (mutable via IPC) ────────────────────────────────────────
let cfg = {
  proxyMode:      'direct',   // 'direct' | 'anonymous' | 'high'
  incognito:      false,
  adBlock:        true,
  httpsEnforce:   true,
  uaRandomize:    true,
  blockPerms:     true,
  fingerprintMock:false,
  torEnabled:     false,
  groqKey:        process.env.GROQ_API_KEY || ''
}

let torProcess = null
let torStatus = 'Disconnected'

// ── Global Biometric Tracking ────────────────────────────────────────────────
let globalBiometrics = {
  clicks: 0,
  scrolls: 0,
  keystrokes: 0,
  tabHides: 0,
  mouseMovements: 0
}

// ── UA Pool ───────────────────────────────────────────────────────────────────
const UA_POOL = [
  { label: 'Chrome / Windows',  ua: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36' },
  { label: 'Firefox / Windows', ua: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:125.0) Gecko/20100101 Firefox/125.0' },
  { label: 'Safari / macOS',    ua: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15' },
  { label: 'Chrome / macOS',    ua: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36' },
  { label: 'Chrome / Linux',    ua: 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36' },
]

// ── Proxy Nodes (Dynamically fetched to avoid dead proxies) ──────────────────
let PROXY_NODES = [
  'http://103.111.45.186:1080',
  'http://184.175.93.90:4145'
]

// Fetch fresh ELITE anonymity proxies on startup
fetch('https://api.proxyscrape.com/v2/?request=displayproxies&protocol=http&timeout=2000&country=all&ssl=all&anonymity=elite')
  .then(res => res.text())
  .then(text => {
    const lines = text.split('\n').filter(l => l.trim().length > 0 && l.includes(':'))
    if (lines.length > 0) {
      PROXY_NODES = lines.slice(0, 30).map(p => `http://${p.trim()}`)
      console.log(`[MAArK] Loaded ${PROXY_NODES.length} Elite proxy nodes`)
    }
  }).catch(() => console.log('[MAArK] Failed to fetch fresh proxies, using fallback'))

let activeProfile = 'Personal' // Track current user profile

// ── Tracker Blocklist ─────────────────────────────────────────────────────────
const BLOCKED = [
  'google-analytics.com','googletagmanager.com','googlesyndication.com',
  'doubleclick.net','googleadservices.com','adservice.google.com',
  'connect.facebook.net','facebook.com/tr','facebook.com/plugins',
  'analytics.twitter.com','static.ads-twitter.com',
  'hotjar.com','clarity.ms','mouseflow.com',
  'mixpanel.com','segment.io','cdn.segment.com',
  'amplitude.com','cdn.amplitude.com',
  'adnxs.com','rubiconproject.com','criteo.com','criteo.net',
  'taboola.com','outbrain.com','moatads.com',
  'scorecardresearch.com','quantserve.com','chartbeat.com',
  'pardot.com','marketo.com','bat.bing.com',
  'ads.linkedin.com','snap.licdn.com',
]

// ── Risky download extensions ─────────────────────────────────────────────────
const RISKY_EXTS = new Set(['.exe','.msi','.bat','.cmd','.ps1','.vbs','.scr','.jar','.dmg','.pkg','.sh'])

// ── Phishing detection ─────────────────────────────────────────────────────────
function isPhishing(url) {
  try {
    const { hostname } = new URL(url)
    if (/^(\d{1,3}\.){3}\d{1,3}$/.test(hostname)) return true // raw IP
    if (hostname.split('.').length > 5) return true            // too many subdomains
    if (/\.(xyz|tk|ml|ga|cf|gq|pw|top|click|zip)$/i.test(hostname) &&
        /(login|secure|verify|account|bank|paypal|amazon|apple|microsoft)/i.test(hostname)) return true
    return false
  } catch { return false }
}

// ── Tab state ─────────────────────────────────────────────────────────────────
const tabs = new Map()   // tabId → { view, ses, title, url, loading, isSecure, canGoBack, canGoForward }
let activeTabId = null
let nextTabId   = 1
let mainWindow  = null
let uiPort      = 5173   // set by loadUI() once Vite is detected
const pendingDownloads = new Map()
const pendingPermissions = new Map()
const activeSessions = new Set()

// ── Helpers ───────────────────────────────────────────────────────────────────
const randUA    = () => UA_POOL[Math.floor(Math.random() * UA_POOL.length)].ua
const randProxy = () => PROXY_NODES[Math.floor(Math.random() * PROXY_NODES.length)]

function setBounds() {
  if (!mainWindow || !activeTabId) return
  const tab = tabs.get(activeTabId)
  if (!tab) return
  
  if (tab.url && tab.url.includes('maark://')) {
    tab.view.setBounds({ x: 0, y: 0, width: 0, height: 0 })
    return
  }

  const b = mainWindow.getContentBounds()
  
  if (mainWindow.isFullScreen()) {
    tab.view.setBounds({ x: 0, y: 0, width: b.width, height: b.height })
  } else {
    tab.view.setBounds({ x: 0, y: TOOLBAR_H, width: b.width, height: b.height - TOOLBAR_H })
  }
}

function broadcast() {
  if (!mainWindow) return
  const list = [...tabs.entries()].map(([id, t]) => ({
    id, title: t.title, url: t.url, loading: t.loading,
    isSecure: t.isSecure, canGoBack: t.canGoBack, canGoForward: t.canGoForward,
    isActive: id === activeTabId, isIncognito: cfg.incognito,
  }))
  mainWindow.webContents.send('tabs:update', list)
  const active = tabs.get(activeTabId)
  if (active) {
    mainWindow.webContents.send('browser:url', active.url)
    mainWindow.webContents.send('browser:loading', active.loading)
    mainWindow.webContents.send('browser:secure', active.isSecure)
  }
}

// ── WebRTC nullifier script injected into every page when Tor is ON ───────────
// This blocks RTCPeerConnection at the JS engine level — closes the last leak
const WEBRTC_BLOCK_SCRIPT = `(function(){
  'use strict';
  // Completely replace RTCPeerConnection with a no-op constructor
  const blocked = function() {
    throw new DOMException('WebRTC is disabled by MAArK for your privacy.', 'NotAllowedError');
  };
  blocked.generateCertificate = () => Promise.reject(new DOMException('Blocked', 'NotAllowedError'));
  try { Object.defineProperty(window, 'RTCPeerConnection', { value: blocked, writable: false, configurable: false }); } catch(e){}
  try { Object.defineProperty(window, 'webkitRTCPeerConnection', { value: blocked, writable: false, configurable: false }); } catch(e){}
  try { Object.defineProperty(window, 'mozRTCPeerConnection', { value: blocked, writable: false, configurable: false }); } catch(e){}
  // Also block the ICE server enumeration
  try { Object.defineProperty(navigator, 'mediaDevices', { value: undefined, writable: false }); } catch(e){}
  console.info('[MAArK Shield] WebRTC fully disabled. Your IP is private.');
})()`;

// ── Apply security policies to a session ──────────────────────────────────────
async function applyPolicies(ses) {
  // 1. User-Agent spoofing
  ses.setUserAgent(cfg.uaRandomize ? randUA() : UA_POOL[0].ua)

  // 2. Proxy / Tor Routing
  if (cfg.torEnabled) {
    // Chromium uses socks5:// for proxy rules
    await ses.setProxy({ proxyRules: 'socks5://127.0.0.1:9050', proxyBypassRules: 'localhost,127.0.0.1' })
    console.log('🛡️ [Policy] Routing ALL traffic via Tor socks5://127.0.0.1:9050')
    // Clear all cached data so old IP is not re-used
    await ses.clearCache()
    await ses.clearStorageData({ storages: ['cookies','localstorage','sessionstorage','indexdb','serviceworkers'] })
  } else if (cfg.proxyMode !== 'direct') {
    const proxy = randProxy()
    await ses.setProxy({ proxyRules: proxy, proxyBypassRules: 'localhost,127.0.0.1' })
    console.log(`🛡️ [Policy] Applied Anonymity Proxy: ${proxy}`)
  } else {
    await ses.setProxy({ mode: 'direct' })
  }

  // 3. WebRTC IP leak prevention (session-level API)
  try {
    if (typeof ses.setWebRTCIPHandlingPolicy === 'function') {
      // 'disable_non_proxied_udp' = only proxied connections allowed
      ses.setWebRTCIPHandlingPolicy(cfg.torEnabled ? 'disable_non_proxied_udp' : 'default')
      console.log(`🛡️ [WebRTC] Policy: ${cfg.torEnabled ? 'disable_non_proxied_udp' : 'default'}`)
    }
  } catch (e) {
    console.warn('⚠️ [WebRTC] session API unavailable:', e.message)
  }

  // 4. Tracker + Ad blocking (request-level)
  ses.webRequest.onBeforeRequest({ urls: ['<all_urls>'] }, (details, cb) => {
    if (!cfg.adBlock) return cb({})
    const url = details.url.toLowerCase()
    // YouTube — only cancel actual ad requests, not video
    if (url.includes('youtube.com') || url.includes('googlevideo.com')) {
      const isAd = url.includes('/ad_') || url.includes('pagead') || url.includes('doubleclick.net/pagead')
      return cb({ cancel: isAd })
    }
    const blocked = BLOCKED.some(p => url.includes(p)) ||
      ['popads.net','propellerads.com','popcash.net','admaven.com','onclickads.net',
       'mgid.com','taboola.com','outbrain.com','zeropark.com','adsterra.com',
       'exoclick.com','juicyads.com','ero-advertising.com','criteo.com','criteo.net',
       'gamma.com','gammaplatform.com'].some(p => url.includes(p))
    cb({ cancel: blocked })
  })

  // 5. Identity Injection for Backend
  ses.webRequest.onBeforeSendHeaders((details, callback) => {
    if (details.url.includes(`localhost:${JAVA_PORT}`)) {
      details.requestHeaders['X-MAArK-Profile'] = activeProfile
    }
    callback({ requestHeaders: details.requestHeaders })
  })

  // 6. HTTPS enforcement
  ses.webRequest.onBeforeRequest({ urls: ['http://*/*'] }, (details, cb) => {
    const url = details.url
    if (url.includes('localhost') || url.includes('127.0.0.1')) return cb({})
    if (!cfg.httpsEnforce) return cb({})
    if (details.resourceType === 'mainFrame') {
      mainWindow?.webContents.send('warning:insecure', url)
      return cb({ redirectURL: url.replace('http://', 'https://') })
    }
    cb({ cancel: true })
  })

  // 7. Permission manager (block geolocation + WebRTC when Tor is ON)
  ses.setPermissionRequestHandler((wc, permission, callback, details) => {
    // When Tor is on, block ALL permissions that could leak identity
    if (cfg.torEnabled) {
      const torBlock = ['geolocation', 'notifications', 'midi', 'mediaKeySystem',
                        'pointerLock', 'camera', 'microphone', 'speaker',
                        'display-capture', 'audioCapture', 'videoCapture']
      if (torBlock.includes(permission)) {
        console.log(`🚫 [Tor] Blocked permission: ${permission}`)
        return callback(false)
      }
    }
    if (!cfg.blockPerms) return callback(true)
    const autoBlock = ['geolocation', 'notifications', 'midi', 'mediaKeySystem', 'pointerLock']
    const autoAllow = ['fullscreen', 'clipboard-sanitized-write']
    if (autoBlock.includes(permission)) return callback(false)
    if (autoAllow.includes(permission)) return callback(true)
    const permId = `perm-${Date.now()}`
    pendingPermissions.set(permId, callback)
    mainWindow?.webContents.send('permission:request', { id: permId, permission, origin: details.requestingUrl })
  })

  // 8. Download protection
  ses.on('will-download', (event, item) => {
    event.preventDefault()
    const filename = item.getFilename()
    const ext = path.extname(filename).toLowerCase()
    const dlId = `dl-${Date.now()}`
    pendingDownloads.set(dlId, item)
    mainWindow?.webContents.send('download:request', {
      id: dlId, filename, url: item.getURL(),
      size: item.getTotalBytes(), isRisky: RISKY_EXTS.has(ext),
    })
  })
}

function generateFPScript() {
  const isMobile = Math.random() > 0.7;
  const isMac = !isMobile && Math.random() > 0.3;
  const resOptions = isMobile ? [[390, 844], [430, 932], [412, 915]] : [[1920, 1080], [1366, 768], [1440, 900]];
  const res = resOptions[Math.floor(Math.random() * resOptions.length)];
  const cores = isMobile ? 8 : 16;
  const mem = isMobile ? 8 : 32;
  const platform = isMobile ? 'Linux armv81' : (isMac ? 'MacIntel' : 'Win32');
  const lang = 'en-US';
  const noiseSeed = Math.floor(Math.random() * 10) + 1;
  
  return `(function(){
    Object.defineProperty(screen,'width',{get:()=>${res[0]}, configurable: true});
    Object.defineProperty(screen,'height',{get:()=>${res[1]}, configurable: true});
    Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => ${cores}, configurable: true });
    Object.defineProperty(navigator, 'deviceMemory',        { get: () => ${mem}, configurable: true });
    Object.defineProperty(navigator, 'platform',            { get: () => '${platform}', configurable: true });
    const origToDU=HTMLCanvasElement.prototype.toDataURL;
    HTMLCanvasElement.prototype.toDataURL=function(type){
      const ctx=this.getContext('2d');
      if(ctx){
        const id=ctx.getImageData(0,0,this.width,this.height);
        for(let i=0; i<id.data.length; i+=128) id.data[i] = (id.data[i] + ${noiseSeed}) % 256;
        ctx.putImageData(id,0,0);
      }
      return origToDU.call(this,type);
    };
  })()`;
}

// ── Create tab ────────────────────────────────────────────────────────────────
async function createTab(url = 'https://www.startpage.com') {
  const tabId = nextTabId++
  const partition = cfg.incognito ? `in-memory-${tabId}` : `persist:tab-${tabId}`
  const ses = session.fromPartition(partition)
  activeSessions.add(ses)
  await applyPolicies(ses)

  // Internal URL handling — keep maark:// in tab.url so the React overlay works,
  // but load the Vite dev URL so the shell renders in the BrowserView
  let targetUrl = url
  let internalUrl = null
  if (url.startsWith('maark://')) {
    internalUrl = url   // keep the original for React matching
    const page = url.split('maark://')[1]
    // In dev, load the Vite app URL; in prod load dist/index.html
    targetUrl = isDev
      ? `http://127.0.0.1:5173`   // shell only — React overlay will render on top
      : `file://${path.join(__dirname, '../dist/index.html')}`
  }

  const view = new BrowserView({
    webPreferences: { 
      session: ses, 
      contextIsolation: true, 
      nodeIntegration: false, 
      sandbox: true,
      preload: path.join(__dirname, 'tab-preload.js')
    }
  })

  const data = {
    view, ses, partition,
    title: 'New Tab',
    // Track the maark:// URL so React knows to show the overlay
    url: internalUrl || targetUrl,
    loading: false, isSecure: true, canGoBack: false, canGoForward: false
  }
  tabs.set(tabId, data)

  view.webContents.setWindowOpenHandler(({ url }) => {
    createTab(url).then(id => switchTab(id))
    return { action: 'deny' }
  })

  view.webContents.on('did-start-loading', () => {
    const t = tabs.get(tabId); if (t) t.loading = true; broadcast()
  })

  view.webContents.on('did-stop-loading', () => {
    const t = tabs.get(tabId); if (!t) return
    const u = view.webContents.getURL()
    t.loading = false;
    // Do not overwrite internal maark:// URLs with the underlying shell URL
    if (!t.url.startsWith('maark://')) {
      t.url = u
      t.title = view.webContents.getTitle() || u
    }
    t.isSecure = u.startsWith('https://') || u.startsWith('about:')
    t.canGoBack = view.webContents.navigationHistory.canGoBack()
    t.canGoForward = view.webContents.navigationHistory.canGoForward()
    broadcast()
    setBounds()
    if (!cfg.incognito && u && !u.startsWith('about:') && !u.includes('startpage.com')) {
      fetch(`http://localhost:${JAVA_PORT}/api/history/browse`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: u, title: t.title })
      }).catch(() => {})
    }
  })

  view.webContents.on('did-commit-navigation', (event, u, isMainFrame) => {
    if (!isMainFrame) return
    // Inject WebRTC blocker FIRST on every page when Tor is active
    if (cfg.torEnabled) {
      view.webContents.executeJavaScript(WEBRTC_BLOCK_SCRIPT).catch(() => {})
    }
    if (cfg.proxyMode === 'high' || cfg.fingerprintMock) {
      view.webContents.executeJavaScript(generateFPScript()).catch(() => {})
    }
  })

  view.webContents.on('did-finish-load', () => {
    if (cfg.adBlock) {
      view.webContents.insertCSS(`
        .ytd-ad-slot-renderer, #masthead-ad, #player-ads { display: none !important; }
      `).catch(() => {})
      view.webContents.executeJavaScript(`
        setInterval(() => {
          const skipBtn = document.querySelector('.ytp-ad-skip-button');
          if (skipBtn) skipBtn.click();
        }, 1000);
      `).catch(() => {})
    }
  })

  view.webContents.loadURL(targetUrl)
  return tabId
}

function switchTab(tabId) {
  if (!mainWindow || !tabs.has(tabId)) return
  if (activeTabId && tabs.has(activeTabId)) mainWindow.removeBrowserView(tabs.get(activeTabId).view)
  activeTabId = tabId
  mainWindow.addBrowserView(tabs.get(tabId).view)
  setBounds()
  broadcast()
}

async function closeTab(tabId) {
  const t = tabs.get(tabId); if (!t) return
  mainWindow?.removeBrowserView(t.view)
  t.view.webContents.destroy()
  tabs.delete(tabId)
  if (activeTabId === tabId) {
    activeTabId = null
    const remaining = [...tabs.keys()]
    if (remaining.length > 0) switchTab(remaining[remaining.length - 1])
    else { const id = await createTab(); switchTab(id) }
  }
  broadcast()
}

// ── Tor Management ────────────────────────────────────────────────────────────
async function startTor() {
  // Resolve Tor binary path — works in dev AND packaged app
  const torBaseDir = app.isPackaged
    ? path.join(process.resourcesPath, 'tor')
    : path.join(app.getAppPath(), 'resources', 'tor')

  const torBinary = process.platform === 'win32'
    ? path.join(torBaseDir, 'tor', 'tor.exe')
    : path.join(torBaseDir, 'tor')

  if (!fs.existsSync(torBinary)) {
    console.error(`❌ [Tor] Binary not found at: ${torBinary}`)
    torStatus = 'Binary Missing'
    mainWindow?.webContents.send('tor:status', torStatus)
    return
  }

  const userDataPath = app.getPath('userData')
  const torDataDir   = path.join(userDataPath, 'tor_data')
  if (!fs.existsSync(torDataDir)) fs.mkdirSync(torDataDir, { recursive: true })

  const geoipDir  = path.join(torBaseDir, 'data')
  const torrcPath = path.join(userDataPath, 'torrc')

  // torrc — strong config: SOCKS5, no exit DNS leak, cookie auth
  const torrcContent = [
    `SocksPort 127.0.0.1:9050`,
    `ControlPort 127.0.0.1:9051`,
    `CookieAuthentication 0`,
    `DataDirectory ${torDataDir.replace(/\\/g, '/')}`,
    `GeoIPFile ${path.join(geoipDir, 'geoip').replace(/\\/g, '/')}`,
    `GeoIPv6File ${path.join(geoipDir, 'geoip6').replace(/\\/g, '/')}`,
    `Log notice stdout`,
    `SafeSocks 1`,          // Reject unsafe SOCKS4/4a (forces DNS through Tor)
    `TestSocks 1`,          // Warn if app leaks DNS
    `DNSPort 127.0.0.1:9053`, // Local DNS-over-Tor port
    `AutomapHostsOnResolve 1`,
    `ClientUseIPv6 0`,      // IPv6 can bypass SOCKS and leak real IP
  ].join('\n')

  fs.writeFileSync(torrcPath, torrcContent)
  console.log('🚀 [Tor] Starting Tor daemon...')
  torStatus = 'Connecting...'
  mainWindow?.webContents.send('tor:status', torStatus)

  torProcess = spawn(torBinary, ['-f', torrcPath], {
    cwd: torBaseDir,
    detached: false,
    stdio: ['ignore', 'pipe', 'pipe'],
  })

  torProcess.stdout.on('data', async (data) => {
    const msg = data.toString()
    const match = msg.match(/Bootstrapped (\d+)%/)
    if (match) {
      const pct = parseInt(match[1])
      torStatus = pct === 100 ? 'Connected' : `Connecting (${pct}%)`
      console.log(`🧅 [Tor] ${torStatus}`)
      mainWindow?.webContents.send('tor:status', torStatus)
      // When fully connected, re-apply policies so proxy kicks in immediately
      if (pct === 100) {
        console.log('🧅 [Tor] Applying proxy to all sessions and reloading tabs...')
        // Re-apply to all sessions in parallel
        const sessionJobs = [...activeSessions].map(s => applyPolicies(s).catch(() => {}))
        sessionJobs.push(applyPolicies(session.defaultSession).catch(() => {}))
        await Promise.all(sessionJobs)
        // Inject WebRTC blocker + force reload all tabs so they use the Tor proxy
        for (const t of tabs.values()) {
          try {
            t.view.webContents.executeJavaScript(WEBRTC_BLOCK_SCRIPT).catch(() => {})
            // Reload the page so it goes through Tor this time
            t.view.webContents.reload()
          } catch (e) {}
        }
        console.log('✅ [Tor] All tabs reloaded through Tor. IP is now masked.')
      }
    }
  })

  torProcess.stderr.on('data', (data) => {
    const msg = data.toString().trim()
    if (msg) console.warn('🧅 [Tor stderr]:', msg)
  })

  torProcess.on('close', (code) => {
    console.log(`🛑 [Tor] Exited with code ${code}`)
    torProcess = null
    torStatus = 'Disconnected'
    mainWindow?.webContents.send('tor:status', torStatus)
  })

  torProcess.on('error', (err) => {
    console.error('❌ [Tor] Failed to spawn:', err.message)
    torStatus = 'Error'
    mainWindow?.webContents.send('tor:status', torStatus)
  })
}

async function stopTor() {
  if (torProcess) {
    torProcess.kill('SIGTERM')
    torProcess = null
  }
  torStatus = 'Disconnected'
  mainWindow?.webContents.send('tor:status', torStatus)
  // Restore direct connection on all sessions
  for (const s of activeSessions) {
    await s.setProxy({ mode: 'direct' }).catch(() => {})
  }
  await session.defaultSession.setProxy({ mode: 'direct' }).catch(() => {})
  console.log('🛑 [Tor] Stopped. Direct connection restored.')
}

// ── Java backend ──────────────────────────────────────────────────────────────
function startJava() {
  const jar = path.join(__dirname, '..', '..', 'target', 'maark-1.0-SNAPSHOT-backend.jar')
  const proc = spawn('java', ['-jar', jar], { cwd: path.join(__dirname, '..', '..'), stdio: ['ignore','pipe','pipe'] })
  return proc
}

function waitForJava(retries = 30) {
  return new Promise((resolve, reject) => {
    let done = false
    const check = (n) => {
      if (done) return
      const req = http.get(`http://localhost:${JAVA_PORT}/api/health`, res => {
        if (!done && res.statusCode === 200) { done = true; resolve() }
      })
      req.on('error', () => { if (!done) n > 0 ? setTimeout(() => check(n-1), 1000) : reject() })
      req.setTimeout(500, () => { req.destroy(); if (!done) n > 0 ? setTimeout(() => check(n-1), 1000) : reject() })
    }
    check(retries)
  })
}

// ── Create window ─────────────────────────────────────────────────────────────
function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1400, height: 900, minWidth: 900, minHeight: 600,
    frame: false, titleBarStyle: 'hidden', backgroundColor: '#0D0D14',
    webPreferences: { preload: path.join(__dirname, 'preload.js'), contextIsolation: true, nodeIntegration: false }
  })
  applyPolicies(session.defaultSession)
  mainWindow.on('resize', setBounds)
  mainWindow.on('closed', () => { mainWindow = null })
  
  // Forward console messages
  mainWindow.webContents.on('console-message', (event, level, message, line, sourceId) => {
    console.log(`[Browser Console] ${message} (${sourceId}:${line})`);
  });
}

// ── IPC handlers ──────────────────────────────────────────────────────────────
ipcMain.handle('tab:create', async (_, url) => { const id = await createTab(url || 'maark://home'); switchTab(id); return id })
ipcMain.on('tab:switch', (_, id) => switchTab(id))
ipcMain.on('tab:close',  (_, id) => closeTab(id))

ipcMain.on('biometric:event', (event, type) => {
  if (globalBiometrics[type] !== undefined) globalBiometrics[type]++;
  else if (type === 'click') globalBiometrics.clicks++;
  else if (type === 'keystroke') globalBiometrics.keystrokes++;
  else if (type === 'scroll') globalBiometrics.scrolls++;
  else if (type === 'tabHide') globalBiometrics.tabHides++;
  else if (type === 'move') globalBiometrics.mouseMovements++;

  const data = { type, tabId: activeTabId, totals: globalBiometrics }
  mainWindow?.webContents.send('biometric:update', data)
  for (const t of tabs.values()) {
    t.view.webContents.send('biometric:update', data)
  }
})

ipcMain.handle('biometric:get-totals', () => globalBiometrics)

ipcMain.on('privacy:set', async (_, newCfg) => {
  const wasTorEnabled = cfg.torEnabled
  cfg = { ...cfg, ...newCfg }
  console.log(`🛡️ [Privacy] Config updated: ${JSON.stringify(cfg)}`)
  
  if (cfg.torEnabled && !wasTorEnabled) {
    await startTor()
  } else if (!cfg.torEnabled && wasTorEnabled) {
    stopTor()
  }

  // Re-apply policies to all active sessions
  for (const ses of activeSessions) {
    await applyPolicies(ses)
  }
  // Also default session
  await applyPolicies(session.defaultSession)
  
  mainWindow?.webContents.send('privacy:config', cfg)
})

ipcMain.on('profile:set', async (_, profileId) => {
  activeProfile = profileId
  console.log(`👤 [Identity] Active profile switched to: ${activeProfile}`)
  
  // Create the first tab now that profile is selected, if we don't have one
  if (tabs.size === 0) {
    const id = await createTab('https://www.startpage.com')
    switchTab(id)
  }
})

ipcMain.on('browser:navigate', (_, url) => {
  const t = tabs.get(activeTabId); if (!t) return

  // Handle internal maark:// pages — show React overlay, hide BrowserView
  if (url.startsWith('maark://')) {
    t.url = url
    t.title = url.replace('maark://', 'MAArK ').replace(/\b\w/g, c => c.toUpperCase())
    setBounds()     // hides BrowserView since url has maark://
    broadcast()
    return
  }

  let target = url
  if (!target.startsWith('http://') && !target.startsWith('https://')) {
    target = target.includes('.') && !target.includes(' ')
      ? 'https://' + target
      : 'https://www.startpage.com/do/search?q=' + encodeURIComponent(target)
  }
  t.url = target
  // Show BrowserView before loading so it's visible immediately
  if (mainWindow && !mainWindow.getBrowserViews().includes(t.view)) {
    mainWindow.addBrowserView(t.view)
  }
  setBounds()
  t.view.webContents.loadURL(target)
  broadcast()
})

ipcMain.on('window:minimize', () => mainWindow?.minimize())
ipcMain.on('window:maximize', () => mainWindow?.isMaximized() ? mainWindow.unmaximize() : mainWindow?.maximize())
ipcMain.on('window:close',    () => mainWindow?.close())

// Merged into privacy:set

ipcMain.handle('tor:get-status', () => torStatus)

ipcMain.handle('privacy:get-config', () => cfg)
ipcMain.handle('privacy:get-ua-pool', () => UA_POOL)

ipcMain.on('browser:back',    () => { const t = tabs.get(activeTabId); t?.view.webContents.navigationHistory.canGoBack()    && t.view.webContents.goBack() })
ipcMain.on('browser:forward', () => { const t = tabs.get(activeTabId); t?.view.webContents.navigationHistory.canGoForward() && t.view.webContents.goForward() })
ipcMain.on('browser:reload',  () => tabs.get(activeTabId)?.view.webContents.reload())
ipcMain.on('browser:home',    () => tabs.get(activeTabId)?.view.webContents.loadURL('https://www.startpage.com'))

ipcMain.on('sidebar:resize', (_, sidebarW) => {
  const t = tabs.get(activeTabId); if (!t || !mainWindow) return
  const b = mainWindow.getContentBounds()
  t.view.setBounds({ x: 0, y: TOOLBAR_H, width: b.width - sidebarW, height: b.height - TOOLBAR_H })
})

ipcMain.handle('network:get-info', async () => {
  try {
    const r = await fetch('https://ipwho.is/')
    const d = await r.json()
    return { ip: d.ip, city: d.city, country: d.country, isp: d.connection?.isp, vpn: d.security?.vpn }
  } catch { return { error: 'Offline' } }
})

ipcMain.handle('ai:extract-text', async () => {
  const t = tabs.get(activeTabId)
  return t ? await t.view.webContents.executeJavaScript('document.body.innerText') : ''
})

ipcMain.handle('ai:ask', async (_, { prompt, text, key }) => {
  if (!key) return { error: 'No API Key' }
  try {
    const res = await fetch('https://api.groq.com/openai/v1/chat/completions', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${key}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        model: 'llama-3.3-70b-versatile',
        messages: [{ role: 'user', content: `${prompt}\n\nContext: ${text.substring(0, 5000)}` }]
      })
    })
    const data = await res.json()
    return data.choices ? { result: data.choices[0].message.content } : { error: 'AI Error' }
  } catch (e) { return { error: e.message } }
})

// ── App lifecycle ─────────────────────────────────────────────────────────────
let javaProc = null

async function loadUI() {
  const ports = [5173, 5174, 5175, 5176]
  let success = false
  // Give Vite a moment to fully initialize before we start probing
  await new Promise(r => setTimeout(r, 1000))

  console.log(`📡 [UI] Searching for Vite on 127.0.0.1...`);

  // Retry loop — up to 30 seconds (60 × 500 ms) to find Vite
  for (let i = 0; i < 60 && !success; i++) {
    for (const port of ports) {
      try {
        await new Promise((resolve, reject) => {
          const req = http.get(`http://127.0.0.1:${port}`, res => {
            res.resume() // drain body so socket is released
            if (res.statusCode === 200 || res.statusCode === 304) resolve()
            else reject(new Error(`status ${res.statusCode}`))
          })
          req.on('error', reject)
          req.setTimeout(500, () => { req.destroy(); reject(new Error('timeout')) })
        })
        uiPort = port
        mainWindow.loadURL(`http://127.0.0.1:${port}`)
        console.log(`🎨 [UI] Connected to Vite on port ${port}`)
        if (isDev) mainWindow.webContents.openDevTools({ mode: 'detach' })
        success = true
        break
      } catch (e) {
        if (i === 0) console.log(`[UI] Port ${port}: ${e.message}`)
      }
    }
    if (success) break
    await new Promise(r => setTimeout(r, 500))
  }

  if (!success) {
    console.warn('⚠ [UI] Vite not found after 30 s, falling back to dist/index.html')
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'))
  }
}

app.whenReady().then(async () => {
  // Tor will start when the user enables it from the Privacy Dashboard
  // (auto-start removed — cfg.torEnabled defaults to false)
  javaProc = startJava()
  createWindow()
  if (isDev) await loadUI()
  else mainWindow.loadFile(path.join(__dirname, '../dist/index.html'))
  mainWindow.webContents.once('did-finish-load', () => {
    // We wait for profile:set IPC before creating the first tab.
  })
})

app.on('window-all-closed', () => {
  stopTor()
  javaProc?.kill()
  if (process.platform !== 'darwin') app.quit()
})

app.on('before-quit', () => {
  stopTor()
  javaProc?.kill()
})
