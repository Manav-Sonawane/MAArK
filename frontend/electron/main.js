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
    const lines = text.split('\\n').filter(l => l.trim().length > 0 && l.includes(':'))
    if (lines.length > 0) {
      PROXY_NODES = lines.slice(0, 30).map(p => `http://${p.trim()}`)
      console.log(`[MAArK] Loaded ${PROXY_NODES.length} Elite proxy nodes`)
    }
  }).catch(() => console.log('[MAArK] Failed to fetch fresh proxies, using fallback'))

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

  // 2. Proxy
  if (cfg.proxyMode !== 'direct') {
    // STRICT Proxy mode: No fallback to direct connection to guarantee IP masking
    await ses.setProxy({ proxyRules: randProxy(), proxyBypassRules: 'localhost,127.0.0.1' })
  } else {
    await ses.setProxy({ mode: 'direct' })
  }

  // 3. Tracker + Ad blocking
  ses.webRequest.onBeforeRequest({ urls: ['<all_urls>'] }, (details, cb) => {
    if (!cfg.adBlock) return cb({})
    const url = details.url.toLowerCase()

    // YouTube specific relaxed rules: Don't block essential player components
    if (url.includes('youtube.com') || url.includes('googlevideo.com')) {
      const isAdRequest = url.includes('/ad_') || url.includes('/get_midroll_') || url.includes('pagead') || url.includes('doubleclick.net/pagead');
      return cb({ cancel: isAdRequest });
    }

    // Aggressive Ad & Popup Blocking (Keywords)
    const adKeywords = ['/ads/', '/adserver', '/popunder', '/popup', 'banner_ad', 'tracking.js', 'analytics.js'];
    if (adKeywords.some(kw => url.includes(kw))) {
      return cb({ cancel: true });
    }

    // Comprehensive Tracker & Popup Networks Blocking
    const blocked = BLOCKED.some(p => url.includes(p)) || 
      ['popads.net', 'propellerads.com', 'popcash.net', 'admaven.com', 'onclickads.net', 'mgid.com', 'taboola.com', 'outbrain.com', 'zeropark.com', 'adsterra.com'].some(p => url.includes(p));
    
    cb({ cancel: blocked })
  })

  // 4. HTTPS enforcement
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

  // 5. Permission manager
  ses.setPermissionRequestHandler((wc, permission, callback, details) => {
    if (!cfg.blockPerms) return callback(true)
    const autoBlock = ['geolocation', 'notifications', 'midi', 'mediaKeySystem', 'pointerLock']
    const autoAllow = ['fullscreen', 'clipboard-sanitized-write']
    if (autoBlock.includes(permission)) return callback(false)
    if (autoAllow.includes(permission)) return callback(true)
    // Ask user for camera/mic
    const permId = `perm-${Date.now()}`
    pendingPermissions.set(permId, callback)
    mainWindow?.webContents.send('permission:request', { id: permId, permission, origin: details.requestingUrl })
  })

  // 6. Download protection
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

// ── Fingerprint masking JS ────────────────────────────────────────────────────
const FP_SCRIPT = `(function(){
  Object.defineProperty(screen,'width',{get:()=>1920});
  Object.defineProperty(screen,'height',{get:()=>1080});
  Object.defineProperty(screen,'colorDepth',{get:()=>24});
  const origToDU=HTMLCanvasElement.prototype.toDataURL;
  HTMLCanvasElement.prototype.toDataURL=function(type){
    const ctx=this.getContext('2d');
    if(ctx){const id=ctx.getImageData(0,0,this.width,this.height);
      for(let i=0;i<id.data.length;i+=100)id.data[i]^=Math.floor(Math.random()*3);
      ctx.putImageData(id,0,0);}return origToDU.call(this,type);};
  const origGP=WebGLRenderingContext.prototype.getParameter;
  WebGLRenderingContext.prototype.getParameter=function(p){
    if(p===37445)return 'Intel Inc.';if(p===37446)return 'Intel Iris OpenGL Engine';
    return origGP.call(this,p);};
  console.info('[MAArK] Fingerprint masking active');
})()`

// ── Create tab ────────────────────────────────────────────────────────────────
async function createTab(url = 'https://www.startpage.com') {
  const tabId = nextTabId++
  const partition = cfg.incognito ? `in-memory-${tabId}` : `persist:tab-${tabId}`
  const ses = session.fromPartition(partition)
  await applyPolicies(ses)

  const view = new BrowserView({
    webPreferences: { session: ses, contextIsolation: true, nodeIntegration: false, sandbox: true }
  })

  const data = { view, ses, partition, title: 'New Tab', url: '', loading: false, isSecure: true, canGoBack: false, canGoForward: false }
  tabs.set(tabId, data)

  view.webContents.setWindowOpenHandler(({ url }) => {
    createTab(url).then(id => switchTab(id))
    return { action: 'deny' }
  })

  view.webContents.on('context-menu', (event, params) => {
    if (params.mediaType === 'image') {
      const { Menu } = require('electron')
      const menu = Menu.buildFromTemplate([
        {
          label: '🔍 Analyze Image with MAArK AI',
          click: async () => {
             mainWindow?.webContents.send('image:analyze', params.srcURL)
          }
        }
      ])
      menu.popup()
    }
  })

  view.webContents.on('did-start-loading', () => {
    const t = tabs.get(tabId); if (t) t.loading = true; broadcast()
  })

  view.webContents.on('did-stop-loading', () => {
    const t = tabs.get(tabId); if (!t) return
    const u = view.webContents.getURL()
    t.loading = false; t.url = u
    t.title = view.webContents.getTitle() || u
    t.isSecure = u.startsWith('https://') || u.startsWith('about:') || u.startsWith('chrome:')
    t.canGoBack = view.webContents.canGoBack()
    t.canGoForward = view.webContents.canGoForward()
    broadcast()
    if (!cfg.incognito && u && !u.startsWith('about:') && !u.includes('startpage.com')) {
      fetch(`http://localhost:${JAVA_PORT}/api/history/browse`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: u, title: t.title })
      }).catch(() => {})
    }
    if (isPhishing(u)) mainWindow?.webContents.send('warning:phishing', u)
  })

  view.webContents.on('page-title-updated', (_, title) => {
    const t = tabs.get(tabId); if (t) t.title = title; broadcast()
  })

  view.webContents.on('enter-html-full-screen', () => {
    mainWindow.setFullScreen(true)
    const { width, height } = mainWindow.getBounds()
    view.setBounds({ x: 0, y: 0, width, height })
    mainWindow.webContents.send('browser:fullscreen', true)
  })

  view.webContents.on('leave-html-full-screen', () => {
    mainWindow.setFullScreen(false)
    mainWindow.webContents.send('browser:fullscreen', false)
    setTimeout(setBounds, 100) // Small delay to let window state settle
  })

  view.webContents.on('did-finish-load', () => {
    if (cfg.proxyMode === 'high') view.webContents.executeJavaScript(FP_SCRIPT).catch(() => {})
    
    if (cfg.adBlock) {
      // Cosmetic AdBlock
      view.webContents.insertCSS(`
        .ytd-ad-slot-renderer, .ytd-merch-shelf-renderer, .ytd-rich-metadata-renderer, 
        #masthead-ad, #player-ads, .ad-container, .ad-div, .video-ads, 
        .ytp-ad-module, .ytp-ad-player-overlay { display: none !important; }
      `).catch(() => {})

      // Script Injection for Auto-Skip & Fast-Forward Ads
      view.webContents.executeJavaScript(`
        setInterval(() => {
          const skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern');
          if (skipBtn) skipBtn.click();
          const video = document.querySelector('video');
          if (video && document.querySelector('.ad-showing')) {
            video.currentTime = video.duration - 0.1;
            video.playbackRate = 16;
          }
        }, 500);
      `).catch(() => {})
    }
  })

  view.webContents.loadURL(url)
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
  if (cfg.incognito) { await t.ses.clearStorageData(); await t.ses.clearCache() }
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
  proc.stdout.on('data', d => console.log('[Java]', d.toString().trim()))
  proc.stderr.on('data', d => console.error('[Java]', d.toString().trim()))
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

  // Apply policies to main window session as well
  applyPolicies(session.defaultSession)
  if (isDev) mainWindow.loadURL('http://localhost:5173')
  else mainWindow.loadFile(path.join(__dirname, '../dist/index.html'))
  mainWindow.on('resize', setBounds)
  mainWindow.on('closed', () => { mainWindow = null })
}

// ── IPC handlers ──────────────────────────────────────────────────────────────
ipcMain.handle('tab:create', async (_, url) => { const id = await createTab(url); switchTab(id); return id })
ipcMain.on('tab:switch', (_, id) => switchTab(id))
ipcMain.on('tab:close',  (_, id) => closeTab(id))

ipcMain.on('browser:navigate', (_, url) => {
  const t = tabs.get(activeTabId); if (!t) return
  let target = url
  if (!target.startsWith('http://') && !target.startsWith('https://')) {
    target = target.includes('.') && !target.includes(' ')
      ? 'https://' + target
      : 'https://www.startpage.com/sp/search?query=' + encodeURIComponent(target)
  }
  t.view.webContents.loadURL(target)
  if (!cfg.incognito && !url.includes('startpage.com')) fetch(`http://localhost:${JAVA_PORT}/api/history/search`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query: url })
  }).catch(() => {})
})

ipcMain.on('browser:back',    () => { const t = tabs.get(activeTabId); t?.view.webContents.canGoBack()    && t.view.webContents.goBack() })
ipcMain.on('browser:forward', () => { const t = tabs.get(activeTabId); t?.view.webContents.canGoForward() && t.view.webContents.goForward() })
ipcMain.on('browser:reload',  () => tabs.get(activeTabId)?.view.webContents.reload())
ipcMain.on('browser:home',    () => tabs.get(activeTabId)?.view.webContents.loadURL('https://www.startpage.com'))

ipcMain.on('window:minimize', () => mainWindow?.minimize())
ipcMain.on('window:maximize', () => mainWindow?.isMaximized() ? mainWindow.unmaximize() : mainWindow?.maximize())
ipcMain.on('window:close',    () => mainWindow?.close())

ipcMain.on('sidebar:resize', (_, sidebarW) => {
  const t = tabs.get(activeTabId); if (!t || !mainWindow) return
  const b = mainWindow.getContentBounds()
  t.view.setBounds({ x: 0, y: TOOLBAR_H, width: b.width - sidebarW, height: b.height - TOOLBAR_H })
})

// Privacy config updates
ipcMain.on('privacy:set', async (_, newCfg) => {
  cfg = { ...cfg, ...newCfg }
  // Re-apply policies to all existing sessions
  for (const [, tab] of tabs) { await applyPolicies(tab.ses) }
  mainWindow?.webContents.send('privacy:config', cfg)
})

// Permission answer from UI
ipcMain.on('permission:answer', (_, { id, allow }) => {
  const cb = pendingPermissions.get(id)
  if (cb) { cb(allow); pendingPermissions.delete(id) }
})

// Download answer from UI
ipcMain.on('download:answer', (_, { id, allow, savePath }) => {
  const item = pendingDownloads.get(id)
  if (!item) return
  if (allow && savePath) { item.setSavePath(savePath); item.resume() }
  else item.cancel()
  pendingDownloads.delete(id)
})

ipcMain.handle('privacy:get-config', () => cfg)
ipcMain.handle('privacy:get-ua-pool', () => UA_POOL)

// ── AI Handlers ───────────────────────────────────────────────────────────────
ipcMain.handle('ai:extract-text', async () => {
  const t = tabs.get(activeTabId)
  if (!t) return ''
  try {
    return await t.view.webContents.executeJavaScript('document.body.innerText')
  } catch (e) {
    return ''
  }
})

ipcMain.on('permission:answer', (event, { id, allow }) => {
  const callback = pendingPermissions.get(id)
  if (callback) {
    callback(allow)
    pendingPermissions.delete(id)
  }
})

ipcMain.on('tab-audio:capture', (event) => {
  // Simple implementation for Viva: Redirect to the current active tab
  // Browser audio capture via system mic for demo simplicity
})

ipcMain.handle('ai:ask', async (_, { prompt, text, key }) => {
  if (!key) return { error: 'No API Key provided' }
  try {
    const res = await fetch('https://api.groq.com/openai/v1/chat/completions', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${key}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        model: 'llama-3.3-70b-versatile',
        messages: [
          { role: 'system', content: 'You are MAArK AI, an intelligent browser assistant. Be concise and helpful.' },
          { role: 'user', content: `${prompt}\n\nContext from page:\n${text.substring(0, 8000)}` }
        ]
      })
    })
    const data = await res.json()
    if (data.error) return { error: data.error.message }
    return { result: data.choices[0].message.content }
  } catch (e) {
    return { error: e.message }
  }
})

// ── App lifecycle ─────────────────────────────────────────────────────────────
let javaProc = null
app.whenReady().then(async () => {
  javaProc = startJava()
  try { await waitForJava(); console.log('✅ Java API ready') } catch { console.warn('⚠ Java API unavailable') }
  createWindow()
  mainWindow.webContents.once('did-finish-load', async () => {
    const id = await createTab('https://www.startpage.com')
    switchTab(id)
  })
})

app.on('window-all-closed', () => {
  javaProc?.kill()
  if (process.platform !== 'darwin') app.quit()
})
