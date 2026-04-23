const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('maark', {
  // Navigation
  navigate: (url)   => ipcRenderer.send('browser:navigate', url),
  back:     ()      => ipcRenderer.send('browser:back'),
  forward:  ()      => ipcRenderer.send('browser:forward'),
  reload:   ()      => ipcRenderer.send('browser:reload'),
  home:     ()      => ipcRenderer.send('browser:home'),

  // Tabs
  newTab:    (url)  => ipcRenderer.invoke('tab:create', url),
  closeTab:  (id)   => ipcRenderer.send('tab:close', id),
  switchTab: (id)   => ipcRenderer.send('tab:switch', id),

  // Window
  minimize: () => ipcRenderer.send('window:minimize'),
  maximize: () => ipcRenderer.send('window:maximize'),
  close:    () => ipcRenderer.send('window:close'),
  resizeSidebar: (w) => ipcRenderer.send('sidebar:resize', w),

  // Privacy
  setPrivacyConfig:  (cfg) => ipcRenderer.send('privacy:set', cfg),
  getPrivacyConfig:  ()    => ipcRenderer.invoke('privacy:get-config'),
  getUaPool:         ()    => ipcRenderer.invoke('privacy:get-ua-pool'),

  // Dialogs
  answerPermission: (id, allow)           => ipcRenderer.send('permission:answer', { id, allow }),
  answerDownload:   (id, allow, savePath) => ipcRenderer.send('download:answer', { id, allow, savePath }),

  // AI
  extractText: () => ipcRenderer.invoke('ai:extract-text'),
  askAI: (prompt, text, key) => ipcRenderer.invoke('ai:ask', { prompt, text, key }),

  // Event listeners (renderer ← main)
  on: (channel, cb) => {
    const allowed = ['tabs:update','browser:url','browser:loading','browser:secure',
      'warning:phishing','warning:insecure','permission:request','download:request','privacy:config']
    if (allowed.includes(channel)) ipcRenderer.on(channel, (_, data) => cb(data))
  },
  off: (channel, cb) => ipcRenderer.removeListener(channel, cb),

  // Java REST API
  api: {
    getSuggestions:   (q)        => fetch(`http://localhost:7070/api/suggestions?q=${encodeURIComponent(q)}`).then(r=>r.json()).catch(()=>[]),
    getSearchHistory: ()         => fetch('http://localhost:7070/api/history/searches?limit=100').then(r=>r.json()).catch(()=>[]),
    getBrowseHistory: ()         => fetch('http://localhost:7070/api/history/browses?limit=100').then(r=>r.json()).catch(()=>[]),
    clearHistory:     ()         => fetch('http://localhost:7070/api/history/clear',{method:'DELETE'}).then(r=>r.json()).catch(()=>{}),
    getPrivacyStatus: ()         => fetch('http://localhost:7070/api/privacy/status').then(r=>r.json()).catch(()=>({})),
    toggleShield:     ()         => fetch('http://localhost:7070/api/privacy/toggle',{method:'POST'}).then(r=>r.json()).catch(()=>({})),
  }
})
