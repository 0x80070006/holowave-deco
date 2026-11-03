/* HOLOWAVE, injected after main.js (adapted from NouTube, AGPL-3.0). */
(function () {
  if (window.__holowave) return
  window.__holowave = true

  function send(type, data) {
    try {
      if (window.NouTubeI) window.NouTubeI.onMessage(JSON.stringify({ type: type, data: data }))
    } catch (e) {}
  }

  // 'ended' does not bubble, but it can be caught while it travels down to the element.
  document.addEventListener(
    'ended',
    function (e) {
      if (e.target && e.target.tagName === 'VIDEO') send('hw-ended')
    },
    true
  )

  // ---------------------------------------------------------------- account library
  // YouTube Music's own web client talks to /youtubei/v1/browse with the session cookies and a
  // SAPISIDHASH header; the same request is made here, from the page, so nothing leaves the device
  // but the request YouTube Music itself would send.

  async function authHeader() {
    var m = document.cookie.match(/(?:^|; )(?:SAPISID|__Secure-3PAPISID)=([^;]+)/)
    if (!m) return null
    var ts = Math.floor(Date.now() / 1000)
    var data = new TextEncoder().encode(ts + ' ' + m[1] + ' ' + location.origin)
    var digest = await crypto.subtle.digest('SHA-1', data)
    var hex = Array.from(new Uint8Array(digest), function (b) { return b.toString(16).padStart(2, '0') }).join('')
    return 'SAPISIDHASH ' + ts + '_' + hex
  }

  async function browse(body, continuation) {
    var cfg = (window.ytcfg && window.ytcfg.data_) || {}
    if (!cfg.INNERTUBE_CONTEXT) throw new Error('no-context')
    var auth = await authHeader()
    if (!auth) throw new Error('signed-out')
    var url = '/youtubei/v1/browse?prettyPrint=false' + (continuation ? '&ctoken=' + continuation + '&continuation=' + continuation + '&type=next' : '')
    var r = await fetch(url, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        Authorization: auth,
        'X-Goog-AuthUser': String(cfg.SESSION_INDEX || 0),
        'X-Origin': location.origin,
        'X-Youtube-Client-Name': String(cfg.INNERTUBE_CONTEXT_CLIENT_NAME || 67),
        'X-Youtube-Client-Version': String(cfg.INNERTUBE_CLIENT_VERSION || ''),
      },
      body: JSON.stringify(Object.assign({ context: cfg.INNERTUBE_CONTEXT }, body || {})),
    })
    if (!r.ok) throw new Error('http-' + r.status)
    return r.json()
  }

  function text(t) {
    if (!t) return ''
    if (t.simpleText) return t.simpleText
    return (t.runs || []).map(function (r) { return r.text }).join('')
  }

  function seconds(s) {
    var p = (s || '').split(':').map(Number)
    if (p.some(isNaN)) return 0
    return p.reduce(function (a, b) { return a * 60 + b }, 0)
  }

  // Every song row of a response, with the title of the shelf it sits in ("Aujourd'hui", "Hier"...).
  function rows(node, shelf, out) {
    if (!node || typeof node !== 'object') return out
    if (Array.isArray(node)) { node.forEach(function (n) { rows(n, shelf, out) }); return out }
    if (node.musicShelfRenderer && node.musicShelfRenderer.title) shelf = text(node.musicShelfRenderer.title)
    var r = node.musicResponsiveListItemRenderer
    if (r) {
      var cols = (r.flexColumns || []).map(function (c) { return text((c.musicResponsiveListItemFlexColumnRenderer || {}).text) })
      var id = (r.playlistItemData && r.playlistItemData.videoId) || ''
      if (!id) {
        try { id = r.overlay.musicItemThumbnailOverlayRenderer.content.musicPlayButtonRenderer.playNavigationEndpoint.watchEndpoint.videoId } catch (e) {}
      }
      var thumbs = []
      try { thumbs = r.thumbnail.musicThumbnailRenderer.thumbnail.thumbnails } catch (e) {}
      var fixed = ''
      try { fixed = text(r.fixedColumns[0].musicResponsiveListItemFixedColumnRenderer.text) } catch (e) {}
      if (id && cols[0]) {
        out.push({
          videoId: id,
          title: cols[0],
          artist: (cols[1] || '').split(' • ')[0],
          album: cols[2] || '',
          duration: seconds(fixed),
          thumbnail: thumbs.length ? thumbs[thumbs.length - 1].url : '',
          shelf: shelf || '',
        })
      }
      return out
    }
    for (var k in node) rows(node[k], shelf, out)
    return out
  }

  function nextContinuation(json) {
    var found = ''
    ;(function walk(n) {
      if (found || !n || typeof n !== 'object') return
      if (n.nextContinuationData && n.nextContinuationData.continuation) { found = n.nextContinuationData.continuation; return }
      for (var k in n) walk(n[k])
    })(json)
    return found
  }

  async function collect(browseId, pages) {
    var all = []
    var json = await browse({ browseId: browseId })
    rows(json, '', all)
    var c = nextContinuation(json)
    for (var i = 1; i < pages && c; i++) {
      json = await browse({}, c)
      rows(json, '', all)
      c = nextContinuation(json)
    }
    return all
  }

  // The most played songs of the account. YouTube Music shows no play counter, so a song counts
  // once per day section of the history it appears in (today, yesterday, this week...), plus a
  // bonus when it is liked; ties keep the order of the history (most recent first).
  async function importTop(limit) {
    try {
      var history = await collect('FEmusic_history', 12)
      var liked = []
      try { liked = await collect('FEmusic_liked_videos', 4) } catch (e) {}
      var score = {}
      var first = {}
      var info = {}
      history.forEach(function (s, i) {
        var key = s.videoId
        var seen = (info[key] && info[key].shelves) || {}
        if (!seen[s.shelf]) { score[key] = (score[key] || 0) + 1; seen[s.shelf] = true }
        if (first[key] === undefined) first[key] = i
        info[key] = Object.assign({}, s, { shelves: seen })
      })
      liked.forEach(function (s, i) {
        score[s.videoId] = (score[s.videoId] || 0) + 0.5
        if (first[s.videoId] === undefined) first[s.videoId] = history.length + i
        if (!info[s.videoId]) info[s.videoId] = s
      })
      var top = Object.keys(score).sort(function (a, b) { return score[b] - score[a] || first[a] - first[b] }).slice(0, limit || 100)
      var items = top.map(function (id, rank) {
        var s = info[id]
        return { videoId: id, title: s.title, artist: s.artist, album: s.album, duration: s.duration, thumbnail: s.thumbnail, plays: Math.max(1, Math.round(score[id])), rank: rank }
      })
      for (var i = 0; i < items.length; i += 20) send('hw-library', { items: items.slice(i, i + 20) })
      send('hw-library-done', { count: items.length })
    } catch (e) {
      send('hw-library-done', { count: 0, error: String((e && e.message) || e).slice(0, 80) })
    }
  }

  window.HoloWave = {
    importTop: importTop,
    // Starts a song by id, the way a click on a result would.
    watch: function (id) {
      if (!/^[A-Za-z0-9_-]{6,20}$/.test(id)) return false
      location.assign('https://music.youtube.com/watch?v=' + id)
      return true
    },
    // Same-page start: keeps the document (and the playing audio) alive between songs.
    state: function () {
      var p = document.getElementById('movie_player')
      var v = document.querySelector('video')
      return JSON.stringify({
        videoId: p && p.getVideoData ? (p.getVideoData() || {}).video_id || '' : '',
        playing: v ? !v.paused : false,
        pos: v ? v.currentTime : 0,
        dur: v ? v.duration || 0 : 0,
      })
    },
  }
})()
