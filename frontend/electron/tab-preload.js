const { contextBridge, ipcRenderer } = require('electron')

// ── Biometric Reporting ──────────────────────────────────────────────────────
window.addEventListener('mousedown', () => ipcRenderer.send('biometric:event', 'click'))
window.addEventListener('keydown',   () => ipcRenderer.send('biometric:event', 'keystroke'))
window.addEventListener('wheel',     () => ipcRenderer.send('biometric:event', 'scroll'), { passive: true })

let mouseMoveCount = 0
window.addEventListener('mousemove', () => {
  mouseMoveCount++
  if (mouseMoveCount % 10 === 0) ipcRenderer.send('biometric:event', 'move')
})

// ── Silent Ad-Killer ────────────────────────────────────────────────────────
// This script runs inside the page to remove UI ads and skip video ads
setInterval(() => {
  // 1. YouTube Video Ad Skipping
  const video = document.querySelector('video')
  const ad = document.querySelector('.ad-showing, .ad-interrupting')
  const skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-skip-ad-button')
  
  if (ad && video) {
    // If it's an ad, fast forward to end and click skip
    video.currentTime = video.duration || 999
    if (skipBtn) skipBtn.click()
  }

  // 2. Element Hiding (General + YouTube)
  const adSelectors = [
    'ytd-ad-slot-renderer', '#player-ads', '.ytd-companion-slot-renderer',
    '#masthead-ad', '.ytp-ad-overlay-container', 'ins.adsbygoogle',
    '[id^="ad-"]', '[class^="ad-"]', '.sponsored-content'
  ]
  adSelectors.forEach(sel => {
    document.querySelectorAll(sel).forEach(el => el.remove())
  })
}, 500)

// ── Global CSS Ad-Blocker ───────────────────────────────────────────────────
// Injects a style sheet to hide ads instantly
window.addEventListener('DOMContentLoaded', () => {
  const style = document.createElement('style')
  style.innerHTML = `
    ytd-ad-slot-renderer, #player-ads, .ytd-companion-slot-renderer,
    #masthead-ad, .ytp-ad-overlay-container, ins.adsbygoogle,
    .ad-container, .ad-unit, .sponsored-banner, .google-ad {
      display: none !important;
      visibility: hidden !important;
      height: 0 !important;
      width: 0 !important;
      overflow: hidden !important;
    }
  `
  document.head.appendChild(style)
})

document.addEventListener('visibilitychange', () => {
  if (document.hidden) ipcRenderer.send('biometric:event', 'tabHide')
})

// ── Bridge for Internal Pages ────────────────────────────────────────────────
// This allows internal tabs (like maark://insights) to communicate with the browser
contextBridge.exposeInMainWorld('maark', {
  on: (channel, cb) => {
    const allowed = ['biometric:update', 'privacy:config']
    if (allowed.includes(channel)) ipcRenderer.on(channel, (_, data) => cb(data))
  },
  off: (channel, cb) => {
    ipcRenderer.removeAllListeners(channel)
  },
  // Basic navigation for internal pages if needed
  closeTab: (id) => ipcRenderer.send('tab:close', id),
  getNetworkInfo: () => ipcRenderer.invoke('network:get-info')
})
