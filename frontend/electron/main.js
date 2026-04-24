const { app, BrowserWindow, BrowserView, ipcMain, session } = require('electron')
const path = require('path')
const { spawn } = require('child_process')
const http = require('http')

const JAVA_PORT = 7070
const TOOLBAR_H = 132   // toolbar + tab bar combined
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
const pendingDownloads = new Map()
const pendingPermissions = new Map()

// ── Helpers ───────────────────────────────────────────────────────────────────
const randUA    = () => UA_POOL[Math.floor(Math.random() * UA_POOL.length)].ua
const randProxy = () => PROXY_NODES[Math.floor(Math.random() * PROXY_NODES.length)]

function setBounds() {
  if (!mainWindow || !activeTabId) return
  const tab = tabs.get(activeTabId)
  if (!tab) return
  
  if (tab.url && tab.url.startsWith('maark://')) {
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

// ── Apply security policies to a session ──────────────────────────────────────
async function applyPolicies(ses) {
  // 1. User-Agent
  ses.setUserAgent(cfg.uaRandomize ? randUA() : UA_POOL[0].ua)

  // 2. Proxy Rotation
  if (cfg.proxyMode !== 'direct') {
    const proxy = randProxy();
    await ses.setProxy({ proxyRules: proxy, proxyBypassRules: 'localhost,127.0.0.1' })
    console.log(`🛡️ [Policy] Applied Anonymity Proxy: ${proxy}`);
  } else {
    await ses.setProxy({ mode: 'direct' })
  }

  // 3. Tracker + Ad blocking
  ses.webRequest.onBeforeRequest({ urls: ['<all_urls>'] }, (details, cb) => {
    if (!cfg.adBlock) return cb({})
    const url = details.url.toLowerCase()

    // YouTube specific relaxed rules
    if (url.includes('youtube.com') || url.includes('googlevideo.com')) {
      const isAdRequest = url.includes('/ad_') || url.includes('/get_midroll_') || 
                          url.includes('pagead') || url.includes('doubleclick.net/pagead') ||
                          url.includes('adunit') || url.includes('adsense') ||
                          url.includes('ytimg.com/pagead');
      return cb({ cancel: isAdRequest });
    }

    // Comprehensive Tracker & Popup Networks
    const blocked = BLOCKED.some(p => url.includes(p)) || 
      ['popads.net', 'propellerads.com', 'popcash.net', 'admaven.com', 'onclickads.net', 
       'mgid.com', 'taboola.com', 'outbrain.com', 'zeropark.com', 'adsterra.com',
       'exoclick.com', 'juicyads.com', 'ero-advertising.com'].some(p => url.includes(p));
    
    cb({ cancel: blocked })
  })

  // 4. Identity Injection for Backend Isolation
  ses.webRequest.onBeforeSendHeaders((details, callback) => {
    if (details.url.includes(`localhost:${JAVA_PORT}`)) {
      details.requestHeaders['X-MAArK-Profile'] = activeProfile;
    }
    callback({ requestHeaders: details.requestHeaders });
  });

  // 5. HTTPS enforcement
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

  // 6. Permission manager
  ses.setPermissionRequestHandler((wc, permission, callback, details) => {
    if (!cfg.blockPerms) return callback(true)
    const autoBlock = ['geolocation', 'notifications', 'midi', 'mediaKeySystem', 'pointerLock']
    const autoAllow = ['fullscreen', 'clipboard-sanitized-write']
    if (autoBlock.includes(permission)) return callback(false)
    if (autoAllow.includes(permission)) return callback(true)
    const permId = `perm-${Date.now()}`
    pendingPermissions.set(permId, callback)
    mainWindow?.webContents.send('permission:request', { id: permId, permission, origin: details.requestingUrl })
  })

  // 7. Download protection
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
  await applyPolicies(ses)

  // Internal URL handling
  let targetUrl = url
  if (url.startsWith('maark://')) {
    const page = url.split('maark://')[1]
    const devUrl = `http://127.0.0.1:${uiPort || 5173}/#/maark://${page}`
    targetUrl = devUrl
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

  const data = { view, ses, partition, title: 'New Tab', url: '', loading: false, isSecure: true, canGoBack: false, canGoForward: false }
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
    t.loading = false; t.url = u
    t.title = view.webContents.getTitle() || u
    t.isSecure = u.startsWith('https://') || u.startsWith('about:')
    t.canGoBack = view.webContents.canGoBack()
    t.canGoForward = view.webContents.canGoForward()
    broadcast()
    if (!cfg.incognito && u && !u.startsWith('about:') && !u.includes('startpage.com')) {
      fetch(`http://localhost:${JAVA_PORT}/api/history/browse`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: u, title: t.title })
      }).catch(() => {})
    }
  })

  view.webContents.on('did-commit-navigation', (event, u, isMainFrame) => {
    if (isMainFrame && (cfg.proxyMode === 'high' || cfg.fingerprintMock)) {
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
}

// ── IPC handlers ──────────────────────────────────────────────────────────────
ipcMain.handle('tab:create', async (_, url) => { const id = await createTab(url); switchTab(id); return id })
ipcMain.on('tab:switch', (_, id) => switchTab(id))
ipcMain.on('tab:close',  (_, id) => closeTab(id))

ipcMain.on('biometric:event', (event, type) => {
  const data = { type, tabId: activeTabId }
  mainWindow?.webContents.send('biometric:update', data)
  for (const t of tabs.values()) {
    t.view.webContents.send('biometric:update', data)
  }
})

ipcMain.on('profile:set', (_, profileId) => {
  activeProfile = profileId
  console.log(`👤 [Identity] Active profile switched to: ${activeProfile}`)
})

ipcMain.on('browser:navigate', (_, url) => {
  const t = tabs.get(activeTabId); if (!t) return
  let target = url
  if (!target.startsWith('http://') && !target.startsWith('https://')) {
    target = target.includes('.') && !target.includes(' ')
      ? 'https://' + target
      : 'https://www.startpage.com/sp/search?query=' + encodeURIComponent(target)
  }
  t.view.webContents.loadURL(target)
})

ipcMain.on('window:minimize', () => mainWindow?.minimize())
ipcMain.on('window:maximize', () => mainWindow?.isMaximized() ? mainWindow.unmaximize() : mainWindow?.maximize())
ipcMain.on('window:close',    () => mainWindow?.close())

ipcMain.on('privacy:set', async (_, newCfg) => {
  cfg = { ...cfg, ...newCfg }
  for (const [, tab] of tabs) { await applyPolicies(tab.ses) }
  mainWindow?.webContents.send('privacy:config', cfg)
})

ipcMain.handle('privacy:get-config', () => cfg)
ipcMain.handle('privacy:get-ua-pool', () => UA_POOL)

ipcMain.on('browser:back',    () => { const t = tabs.get(activeTabId); t?.view.webContents.canGoBack()    && t.view.webContents.goBack() })
ipcMain.on('browser:forward', () => { const t = tabs.get(activeTabId); t?.view.webContents.canGoForward() && t.view.webContents.goForward() })
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
  
  // Retry loop to wait for Vite to start
  for (let i = 0; i < 10; i++) {
    for (const port of ports) {
      try {
        await new Promise((resolve, reject) => {
          const req = http.get(`http://127.0.0.1:${port}`, res => {
            if (res.statusCode === 200) resolve()
            else reject()
          })
          req.on('error', reject)
          req.setTimeout(200, () => { req.destroy(); reject() })
        })
        mainWindow.loadURL(`http://127.0.0.1:${port}`)
        console.log(`🎨 [UI] Connected to Vite on port ${port}`)
        success = true
        break
      } catch {}
    }
    if (success) break
    await new Promise(r => setTimeout(r, 500)) // Wait 0.5s before next attempt
  }
  
  if (!success) {
    console.warn('⚠ [UI] Vite not found, falling back to dist/index.html')
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'))
  }
}

app.whenReady().then(async () => {
  javaProc = startJava()
  createWindow()
  if (isDev) await loadUI()
  else mainWindow.loadFile(path.join(__dirname, '../dist/index.html'))
  
  mainWindow.webContents.once('did-finish-load', async () => {
    const id = await createTab('https://www.startpage.com')
    switchTab(id)
  })
})

app.on('window-all-closed', () => {
  javaProc?.kill()
  if (process.platform !== 'darwin') app.quit()
})
