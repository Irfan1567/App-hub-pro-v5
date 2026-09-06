package com.example.runtime

import android.content.Context
import com.example.data.db.AppDao
import com.example.data.model.AppEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object SampleAppsSeeder {

    suspend fun seedSampleAppsIfEmpty(context: Context, appDao: AppDao) = withContext(Dispatchers.IO) {
        val appsDir = File(context.filesDir, "apps")
        if (!appsDir.exists()) {
            appsDir.mkdirs()
        }

        // 1. CyberCalc Pro Sample App
        val calcDir = File(appsDir, "app_sample_cybercalc")
        if (!calcDir.exists() || calcDir.listFiles().isNullOrEmpty()) {
            calcDir.mkdirs()
            File(calcDir, "index.html").writeText(SAMPLE_CALC_HTML)
            File(calcDir, "styles.css").writeText(SAMPLE_CALC_CSS)
            File(calcDir, "app.js").writeText(SAMPLE_CALC_JS)
            appDao.insertApp(
                AppEntity(
                    id = "sample_cybercalc",
                    name = "CyberCalc Pro",
                    description = "Futuristic scientific calculator with Web Audio synthesis & tape export",
                    iconName = "calculate",
                    entryPoint = "index.html",
                    projectDirName = "app_sample_cybercalc",
                    isSampleApp = true,
                    version = "1.2.0"
                )
            )
        }

        // 2. NoteFlow SPA (Markdown, LocalStorage & JSON Export)
        val notesDir = File(appsDir, "app_sample_noteflow")
        if (!notesDir.exists() || notesDir.listFiles().isNullOrEmpty()) {
            notesDir.mkdirs()
            File(notesDir, "index.html").writeText(SAMPLE_NOTES_HTML)
            File(notesDir, "app.js").writeText(SAMPLE_NOTES_JS)
            appDao.insertApp(
                AppEntity(
                    id = "sample_noteflow",
                    name = "NoteFlow SPA",
                    description = "Responsive markdown notes & task manager with offline localStorage and JSON export",
                    iconName = "edit_note",
                    entryPoint = "index.html",
                    projectDirName = "app_sample_noteflow",
                    isSampleApp = true,
                    version = "2.0.1"
                )
            )
        }

        // 3. Canvas Studio Paint (Blob & Data URL Download)
        val canvasDir = File(appsDir, "app_sample_canvas")
        if (!canvasDir.exists() || canvasDir.listFiles().isNullOrEmpty()) {
            canvasDir.mkdirs()
            File(canvasDir, "index.html").writeText(SAMPLE_CANVAS_HTML)
            appDao.insertApp(
                AppEntity(
                    id = "sample_canvas",
                    name = "Canvas Paint Studio",
                    description = "HTML5 touch canvas for sketching with direct PNG blob export & download",
                    iconName = "palette",
                    entryPoint = "index.html",
                    projectDirName = "app_sample_canvas",
                    isSampleApp = true,
                    version = "1.0.0"
                )
            )
        }

        // 4. Runtime Compatibility & DevBench Test
        val benchDir = File(appsDir, "app_sample_devbench")
        if (!benchDir.exists() || benchDir.listFiles().isNullOrEmpty()) {
            benchDir.mkdirs()
            File(benchDir, "index.html").writeText(SAMPLE_DEVBENCH_HTML)
            appDao.insertApp(
                AppEntity(
                    id = "sample_devbench",
                    name = "Runtime DevBench",
                    description = "Universal test suite for Web APIs, console errors, bridge calls & file input",
                    iconName = "bug_report",
                    entryPoint = "index.html",
                    projectDirName = "app_sample_devbench",
                    isSampleApp = true,
                    version = "1.5.0"
                )
            )
        }

        // 5. Retro Breakout 2D Game (Canvas 60fps & Touch steering)
        val breakoutDir = File(appsDir, "app_sample_breakout")
        if (!breakoutDir.exists() || breakoutDir.listFiles().isNullOrEmpty()) {
            breakoutDir.mkdirs()
            File(breakoutDir, "index.html").writeText(SAMPLE_BREAKOUT_HTML)
            appDao.insertApp(
                AppEntity(
                    id = "sample_breakout",
                    name = "Retro Breakout 2D",
                    description = "60 FPS HTML5 Canvas arcade breakout game with touch steering & sound",
                    iconName = "sports_esports",
                    entryPoint = "index.html",
                    projectDirName = "app_sample_breakout",
                    isSampleApp = true,
                    version = "1.0.0"
                )
            )
        }
    }

    private const val SAMPLE_BREAKOUT_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Retro Breakout</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; touch-action: none; }
        body { background: #020617; color: #f8fafc; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; font-family: monospace; overflow: hidden; }
        .score-board { width: 100%; max-width: 400px; display: flex; justify-content: space-between; font-size: 16px; padding: 10px 16px; color: #00e5ff; }
        canvas { background: #0f172a; border: 2px solid #334155; border-radius: 12px; max-width: 95vw; max-height: 80vh; aspect-ratio: 4 / 5; }
        .hint { margin-top: 8px; font-size: 12px; color: #64748b; }
    </style>
</head>
<body>
    <div class="score-board">
        <span>SCORE: <strong id="score">0</strong></span>
        <span>LIVES: <strong id="lives">3</strong></span>
    </div>
    <canvas id="game-canvas"></canvas>
    <div class="hint">Drag paddle or touch left / right to steer</div>
    <script>
        const canvas = document.getElementById("game-canvas");
        const ctx = canvas.getContext("2d");
        const scoreEl = document.getElementById("score");
        const livesEl = document.getElementById("lives");
        canvas.width = 400; canvas.height = 500;
        let paddleWidth = 84, paddleHeight = 12;
        let paddleX = (canvas.width - paddleWidth) / 2;
        let ballX = canvas.width / 2, ballY = canvas.height - 40;
        let ballDx = 3.5, ballDy = -3.5, ballRadius = 7;
        let score = 0, lives = 3;
        const rowCount = 5, colCount = 6, brickWidth = 56, brickHeight = 16, brickPadding = 8, brickOffsetTop = 40, brickOffsetLeft = 12;
        const colors = ["#ef4444", "#f97316", "#eab308", "#10b981", "#00e5ff"];
        let bricks = [];
        for (let c = 0; c < colCount; c++) {
            bricks[c] = [];
            for (let r = 0; r < rowCount; r++) {
                bricks[c][r] = { x: 0, y: 0, status: 1, color: colors[r] };
            }
        }
        function handleTouch(e) {
            const rect = canvas.getBoundingClientRect();
            const scaleX = canvas.width / rect.width;
            const touchX = (e.touches[0].clientX - rect.left) * scaleX;
            paddleX = Math.max(0, Math.min(canvas.width - paddleWidth, touchX - paddleWidth / 2));
        }
        canvas.addEventListener("touchmove", handleTouch);
        canvas.addEventListener("touchstart", handleTouch);
        function collisionDetection() {
            for (let c = 0; c < colCount; c++) {
                for (let r = 0; r < rowCount; r++) {
                    const b = bricks[c][r];
                    if (b.status === 1) {
                        if (ballX > b.x && ballX < b.x + brickWidth && ballY > b.y && ballY < b.y + brickHeight) {
                            ballDy = -ballDy;
                            b.status = 0;
                            score += 10;
                            scoreEl.innerText = score;
                            if (window.AppHub && window.AppHub.showToast) window.AppHub.showToast("Score +10!");
                        }
                    }
                }
            }
        }
        function draw() {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            for (let c = 0; c < colCount; c++) {
                for (let r = 0; r < rowCount; r++) {
                    if (bricks[c][r].status === 1) {
                        const brickX = c * (brickWidth + brickPadding) + brickOffsetLeft;
                        const brickY = r * (brickHeight + brickPadding) + brickOffsetTop;
                        bricks[c][r].x = brickX; bricks[c][r].y = brickY;
                        ctx.fillStyle = bricks[c][r].color;
                        ctx.fillRect(brickX, brickY, brickWidth, brickHeight);
                    }
                }
            }
            ctx.beginPath();
            ctx.arc(ballX, ballY, ballRadius, 0, Math.PI * 2);
            ctx.fillStyle = "#ffffff";
            ctx.fill();
            ctx.closePath();
            ctx.fillStyle = "#00e5ff";
            ctx.fillRect(paddleX, canvas.height - paddleHeight - 10, paddleWidth, paddleHeight);
            collisionDetection();
            if (ballX + ballDx > canvas.width - ballRadius || ballX + ballDx < ballRadius) ballDx = -ballDx;
            if (ballY + ballDy < ballRadius) ballDy = -ballDy;
            else if (ballY + ballDy > canvas.height - paddleHeight - 10 - ballRadius) {
                if (ballX > paddleX && ballX < paddleX + paddleWidth) ballDy = -ballDy;
                else if (ballY + ballDy > canvas.height - ballRadius) {
                    lives--;
                    livesEl.innerText = lives;
                    if (lives <= 0) {
                        alert("Game Over! Score: " + score);
                        document.location.reload();
                        return;
                    } else {
                        ballX = canvas.width / 2; ballY = canvas.height - 40; ballDx = 3.5; ballDy = -3.5;
                    }
                }
            }
            ballX += ballDx; ballY += ballDy;
            requestAnimationFrame(draw);
        }
        draw();
    </script>
</body>
</html>"""


    private const val SAMPLE_CALC_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>CyberCalc Pro</title>
  <link rel="stylesheet" href="styles.css">
</head>
<body>
  <div class="calc-container">
    <header class="calc-header">
      <div class="logo">⚡ CYBERCALC PRO</div>
      <button id="btnExportTape" class="btn-tape">📥 Export Tape</button>
    </header>
    
    <div class="screen-box">
      <div id="historyTape" class="history-text">Ready</div>
      <div id="display" class="main-display">0</div>
    </div>

    <div class="keypad">
      <button class="btn btn-fn" data-action="clear">AC</button>
      <button class="btn btn-fn" data-action="backspace">⌫</button>
      <button class="btn btn-fn" data-action="percent">%</button>
      <button class="btn btn-op" data-action="divide">÷</button>

      <button class="btn btn-num" data-num="7">7</button>
      <button class="btn btn-num" data-num="8">8</button>
      <button class="btn btn-num" data-num="9">9</button>
      <button class="btn btn-op" data-action="multiply">×</button>

      <button class="btn btn-num" data-num="4">4</button>
      <button class="btn btn-num" data-num="5">5</button>
      <button class="btn btn-num" data-num="6">6</button>
      <button class="btn btn-op" data-action="subtract">−</button>

      <button class="btn btn-num" data-num="1">1</button>
      <button class="btn btn-num" data-num="2">2</button>
      <button class="btn btn-num" data-num="3">3</button>
      <button class="btn btn-op" data-action="add">+</button>

      <button class="btn btn-num btn-zero" data-num="0">0</button>
      <button class="btn btn-num" data-num=".">.</button>
      <button class="btn btn-equal" data-action="equals">=</button>
    </div>
  </div>
  <script src="app.js"></script>
</body>
</html>"""

    private const val SAMPLE_CALC_CSS = """* { box-sizing: border-box; margin: 0; padding: 0; user-select: none; }
body {
  background: #090d16;
  color: #e2e8f0;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 16px;
}
.calc-container {
  width: 100%;
  max-width: 380px;
  background: #111827;
  border: 1px solid #1f2937;
  border-radius: 24px;
  padding: 20px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.6), 0 0 20px rgba(6,182,212,0.15);
}
.calc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.logo {
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 1.5px;
  color: #00e5ff;
}
.btn-tape {
  background: #1f2937;
  border: 1px solid #374151;
  color: #94a3b8;
  font-size: 11px;
  padding: 6px 10px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-tape:active { background: #374151; color: #fff; }
.screen-box {
  background: #030712;
  border: 1px solid #1e293b;
  border-radius: 16px;
  padding: 18px 16px;
  text-align: right;
  margin-bottom: 20px;
  box-shadow: inset 0 2px 8px rgba(0,0,0,0.8);
}
.history-text {
  font-size: 14px;
  color: #64748b;
  min-height: 20px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.main-display {
  font-size: 40px;
  font-weight: 700;
  color: #f8fafc;
  letter-spacing: -1px;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.keypad {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.btn {
  height: 60px;
  border-radius: 16px;
  border: none;
  font-size: 20px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.1s, filter 0.1s;
  display: flex;
  justify-content: center;
  align-items: center;
}
.btn:active { transform: scale(0.94); filter: brightness(1.2); }
.btn-num { background: #1e293b; color: #f1f5f9; }
.btn-fn { background: #334155; color: #94a3b8; }
.btn-op { background: #4f46e5; color: #fff; }
.btn-equal { background: #00e5ff; color: #020617; font-weight: 700; }
.btn-zero { grid-column: span 2; justify-content: flex-start; padding-left: 26px; }"""

    private const val SAMPLE_CALC_JS = """// Web Audio synth beep on key press
let audioCtx = null;
function playBeep(freq = 440) {
  try {
    if (!audioCtx) audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    if (audioCtx.state === 'suspended') audioCtx.resume();
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(freq, audioCtx.currentTime);
    gain.gain.setValueAtTime(0.08, audioCtx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.08);
    osc.connect(gain);
    gain.connect(audioCtx.destination);
    osc.start();
    osc.stop(audioCtx.currentTime + 0.08);
  } catch(e) {}
}

let cur = '0';
let prev = '';
let op = null;
const tape = [];

const display = document.getElementById('display');
const historyTape = document.getElementById('historyTape');

function updateDisplay() {
  display.innerText = cur;
  historyTape.innerText = prev + (op ? ' ' + op : '');
}

document.querySelectorAll('.btn-num').forEach(b => {
  b.addEventListener('click', () => {
    playBeep(520);
    const val = b.dataset.num;
    if (val === '.' && cur.includes('.')) return;
    if (cur === '0' && val !== '.') cur = val;
    else cur += val;
    updateDisplay();
  });
});

document.querySelectorAll('.btn-op').forEach(b => {
  b.addEventListener('click', () => {
    playBeep(640);
    const action = b.dataset.action;
    const symbol = b.innerText;
    if (op && prev !== '') compute();
    prev = cur;
    op = symbol;
    cur = '0';
    updateDisplay();
  });
});

document.querySelectorAll('.btn-fn').forEach(b => {
  b.addEventListener('click', () => {
    playBeep(400);
    const action = b.dataset.action;
    if (action === 'clear') {
      cur = '0'; prev = ''; op = null;
    } else if (action === 'backspace') {
      cur = cur.length > 1 ? cur.slice(0, -1) : '0';
    } else if (action === 'percent') {
      cur = (parseFloat(cur) / 100).toString();
    }
    updateDisplay();
  });
});

function compute() {
  const p = parseFloat(prev);
  const c = parseFloat(cur);
  if (isNaN(p) || isNaN(c) || !op) return;
  let res = 0;
  if (op === '+') res = p + c;
  else if (op === '−') res = p - c;
  else if (op === '×') res = p * c;
  else if (op === '÷') res = c !== 0 ? p / c : 'Error';

  const equation = `${'$'}{p} ${'$'}{op} ${'$'}{c} = ${'$'}{res}`;
  tape.push(equation);
  console.log('[CyberCalc]', equation);
  prev = '';
  op = null;
  cur = res.toString();
  updateDisplay();
}

document.querySelector('.btn-equal').addEventListener('click', () => {
  playBeep(880);
  compute();
});

document.getElementById('btnExportTape').addEventListener('click', () => {
  if (tape.length === 0) {
    if (window.AppHub) window.AppHub.showToast("No calculations yet!");
    else alert("No calculations yet!");
    return;
  }
  const text = "=== CYBERCALC PRO TAPE ===\n" + tape.join("\n") + "\n\nGenerated by App Hub Pro";
  const blob = new Blob([text], { type: 'text/plain' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'cybercalc_tape.txt';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  if (window.AppHub) window.AppHub.showToast("Exporting Tape...");
});
"""

    private const val SAMPLE_NOTES_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>NoteFlow SPA</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { background: #0f172a; color: #f8fafc; font-family: system-ui, sans-serif; padding: 16px; padding-bottom: 80px; }
    header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; border-bottom: 1px solid #334155; padding-bottom: 12px; }
    h1 { font-size: 20px; font-weight: 700; color: #38bdf8; }
    .btn { background: #2563eb; color: #fff; border: none; border-radius: 8px; padding: 8px 14px; font-size: 13px; font-weight: 600; cursor: pointer; }
    .btn:active { opacity: 0.8; }
    .btn-outline { background: transparent; border: 1px solid #475569; color: #cbd5e1; }
    .input-box { width: 100%; padding: 12px; border-radius: 10px; border: 1px solid #334155; background: #1e293b; color: #fff; font-size: 14px; margin-bottom: 10px; }
    .note-card { background: #1e293b; border: 1px solid #334155; border-radius: 12px; padding: 14px; margin-bottom: 12px; display: flex; justify-content: space-between; align-items: flex-start; }
    .note-title { font-size: 16px; font-weight: 600; color: #e2e8f0; margin-bottom: 4px; }
    .note-body { font-size: 13px; color: #94a3b8; white-space: pre-wrap; }
    .note-time { font-size: 10px; color: #64748b; margin-top: 6px; }
    .btn-del { background: #ef4444; color: #fff; border: none; border-radius: 6px; padding: 4px 8px; font-size: 11px; cursor: pointer; }
    .actions { display: flex; gap: 8px; margin-bottom: 16px; }
  </style>
</head>
<body>
  <header>
    <h1>📝 NoteFlow SPA</h1>
    <span id="noteCount" style="font-size: 12px; color: #94a3b8;">0 notes</span>
  </header>

  <div class="actions">
    <button id="btnExport" class="btn btn-outline">⬇️ Export JSON</button>
    <button id="btnImport" class="btn btn-outline">⬆️ Import JSON</button>
    <input type="file" id="fileInput" accept=".json" style="display:none;">
  </div>

  <input type="text" id="noteTitle" class="input-box" placeholder="Note Title...">
  <textarea id="noteContent" class="input-box" style="height: 90px;" placeholder="Write your notes here... (saved to localStorage automatically)"></textarea>
  <button id="btnAdd" class="btn" style="width: 100%; margin-bottom: 20px;">+ Save Note</button>

  <div id="notesList"></div>

  <script src="app.js"></script>
</body>
</html>"""

    private const val SAMPLE_NOTES_JS = """const STORAGE_KEY = 'noteflow_saved_notes';
let notes = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');

function render() {
  const container = document.getElementById('notesList');
  document.getElementById('noteCount').innerText = notes.length + ' notes';
  container.innerHTML = '';
  if (notes.length === 0) {
    container.innerHTML = '<div style="text-align:center; padding: 40px; color: #64748b;">No notes yet. Create your first note above!</div>';
    return;
  }
  notes.forEach((n, idx) => {
    const card = document.createElement('div');
    card.className = 'note-card';
    card.innerHTML = `
      <div>
        <div class="note-title">${'$'}{escapeHtml(n.title)}</div>
        <div class="note-body">${'$'}{escapeHtml(n.body)}</div>
        <div class="note-time">${'$'}{new Date(n.time).toLocaleString()}</div>
      </div>
      <button class="btn-del" onclick="deleteNote(${'$'}{idx})">✕</button>
    `;
    container.appendChild(card);
  });
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.innerText = text;
  return div.innerHTML;
}

window.deleteNote = function(idx) {
  notes.splice(idx, 1);
  localStorage.setItem(STORAGE_KEY, JSON.stringify(notes));
  render();
};

document.getElementById('btnAdd').addEventListener('click', () => {
  const title = document.getElementById('noteTitle').value.trim();
  const body = document.getElementById('noteContent').value.trim();
  if (!title && !body) return;
  notes.unshift({ title: title || 'Untitled', body, time: Date.now() });
  localStorage.setItem(STORAGE_KEY, JSON.stringify(notes));
  document.getElementById('noteTitle').value = '';
  document.getElementById('noteContent').value = '';
  render();
  if (window.AppHub) window.AppHub.showToast('Note saved persistently!');
});

document.getElementById('btnExport').addEventListener('click', () => {
  const jsonStr = JSON.stringify(notes, null, 2);
  const blob = new Blob([jsonStr], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'noteflow_backup.json';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
});

document.getElementById('btnImport').addEventListener('click', () => {
  document.getElementById('fileInput').click();
});

document.getElementById('fileInput').addEventListener('change', (e) => {
  const file = e.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = (event) => {
    try {
      const parsed = JSON.parse(event.target.result);
      if (Array.isArray(parsed)) {
        notes = parsed;
        localStorage.setItem(STORAGE_KEY, JSON.stringify(notes));
        render();
        if (window.AppHub) window.AppHub.showToast('Notes imported successfully!');
      }
    } catch(err) {
      alert('Invalid JSON file');
    }
  };
  reader.readAsText(file);
});

render();
"""

    private const val SAMPLE_CANVAS_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>Canvas Paint Studio</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; touch-action: none; }
    body { background: #0b0f19; color: #fff; font-family: sans-serif; display: flex; flex-direction: column; height: 100vh; overflow: hidden; }
    .toolbar { background: #1e293b; padding: 12px; display: flex; gap: 8px; align-items: center; justify-content: space-between; border-bottom: 1px solid #334155; }
    .colors { display: flex; gap: 8px; }
    .color-pill { width: 28px; height: 28px; border-radius: 50%; border: 2px solid #fff; cursor: pointer; }
    .btn { background: #3b82f6; border: none; color: #fff; padding: 8px 14px; border-radius: 8px; font-weight: 600; cursor: pointer; font-size: 13px; }
    .btn-green { background: #10b981; }
    .btn:active { opacity: 0.8; }
    canvas { flex: 1; background: #ffffff; cursor: crosshair; }
  </style>
</head>
<body>
  <div class="toolbar">
    <div class="colors">
      <div class="color-pill" style="background:#000000;" onclick="setColor('#000000')"></div>
      <div class="color-pill" style="background:#ef4444;" onclick="setColor('#ef4444')"></div>
      <div class="color-pill" style="background:#3b82f6;" onclick="setColor('#3b82f6')"></div>
      <div class="color-pill" style="background:#10b981;" onclick="setColor('#10b981')"></div>
      <div class="color-pill" style="background:#f59e0b;" onclick="setColor('#f59e0b')"></div>
    </div>
    <div style="display:flex; gap:8px;">
      <button class="btn" onclick="clearCanvas()">Clear</button>
      <button class="btn btn-green" onclick="downloadImage()">💾 Download PNG</button>
    </div>
  </div>
  <canvas id="paintCanvas"></canvas>
  <script>
    const canvas = document.getElementById('paintCanvas');
    const ctx = canvas.getContext('2d');
    let painting = false;
    let currentColor = '#000000';

    function resize() {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight - 56;
      ctx.fillStyle = "#ffffff";
      ctx.fillRect(0, 0, canvas.width, canvas.height);
    }
    window.addEventListener('resize', resize);
    resize();

    function setColor(c) { currentColor = c; }
    function clearCanvas() { ctx.fillRect(0, 0, canvas.width, canvas.height); }

    function start(e) { painting = true; draw(e); }
    function end() { painting = false; ctx.beginPath(); }
    function draw(e) {
      if (!painting) return;
      ctx.lineWidth = 6;
      ctx.lineCap = 'round';
      ctx.strokeStyle = currentColor;
      const clientX = e.clientX || (e.touches && e.touches[0].clientX);
      const clientY = (e.clientY || (e.touches && e.touches[0].clientY)) - 56;
      ctx.lineTo(clientX, clientY);
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(clientX, clientY);
    }

    canvas.addEventListener('mousedown', start);
    canvas.addEventListener('mouseup', end);
    canvas.addEventListener('mousemove', draw);
    canvas.addEventListener('touchstart', start);
    canvas.addEventListener('touchend', end);
    canvas.addEventListener('touchmove', draw);

    function downloadImage() {
      canvas.toBlob((blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'canvas_artwork_' + Date.now() + '.png';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        if (window.AppHub) window.AppHub.showToast("Downloading Artwork PNG...");
      }, 'image/png');
    }
  </script>
</body>
</html>"""

    private const val SAMPLE_DEVBENCH_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Runtime DevBench</title>
  <style>
    body { background: #090d16; color: #e2e8f0; font-family: monospace; padding: 16px; font-size: 13px; line-height: 1.5; }
    h1 { color: #00e5ff; font-size: 18px; margin-bottom: 12px; }
    .card { background: #111827; border: 1px solid #1f2937; border-radius: 12px; padding: 14px; margin-bottom: 12px; }
    .btn { background: #2563eb; color: #fff; border: none; border-radius: 6px; padding: 8px 12px; margin: 4px; cursor: pointer; font-size: 12px; }
    .btn-warn { background: #f59e0b; color: #000; }
    .btn-err { background: #ef4444; color: #fff; }
    pre { background: #030712; padding: 10px; border-radius: 6px; overflow-x: auto; color: #34d399; margin-top: 8px; }
  </style>
</head>
<body>
  <h1>🛠️ Universal Runtime DevBench</h1>
  
  <div class="card">
    <strong>Origin & System Info:</strong>
    <div id="originInfo">Loading...</div>
    <div id="bridgeStatus" style="color:#f59e0b; margin-top:4px;">Checking Bridge...</div>
  </div>

  <div class="card">
    <strong>Test Diagnostics Console Logs:</strong><br>
    <button class="btn" onclick="console.log('Test info log at ' + new Date().toISOString())">Log Info</button>
    <button class="btn btn-warn" onclick="console.warn('Test warning log: memory threshold high')">Log Warn</button>
    <button class="btn btn-err" onclick="console.error('Test error log: simulated JS fault!')">Log Error</button>
    <button class="btn btn-err" onclick="simulateUncaughtError()">Trigger Exception</button>
  </div>

  <div class="card">
    <strong>Test Bridge & Native Features:</strong><br>
    <button class="btn" onclick="testBridgeToast()">AppHub.showToast()</button>
    <button class="btn" onclick="testBridgeDownload()">AppHub.download()</button>
    <button class="btn" onclick="testBridgeShare()">AppHub.shareFile()</button>
  </div>

  <div class="card">
    <strong>Test HTML File Input Picker:</strong><br>
    <input type="file" id="benchPicker" multiple style="margin-top:8px;">
    <div id="pickerResult" style="color:#38bdf8; margin-top:6px;"></div>
  </div>

  <script>
    document.getElementById('originInfo').innerHTML = 
      'Location: ' + window.location.href + '<br>' +
      'Origin: ' + window.location.origin + '<br>' +
      'User-Agent: ' + navigator.userAgent;

    if (window.AppHub) {
      document.getElementById('bridgeStatus').innerHTML = '✅ AppHub Native Bridge Connected!';
      document.getElementById('bridgeStatus').style.color = '#34d399';
    } else {
      document.getElementById('bridgeStatus').innerHTML = '⚠️ Standalone Browser Mode';
    }

    function simulateUncaughtError() {
      // Intentionally call undefined function to verify DevConsole exception capture
      nonExistentFunctionCallHere();
    }

    function testBridgeToast() {
      if (window.AppHub && window.AppHub.showToast) {
        window.AppHub.showToast("Greetings from HTML to Android Native!");
      } else {
        alert("AppHub bridge not active in standard browser");
      }
    }

    function testBridgeDownload() {
      const sample = "DevBench Diagnostic Export\nTimestamp: " + new Date().toISOString() + "\nOrigin: " + window.location.origin;
      const b64 = btoa(sample);
      if (window.AppHub && window.AppHub.download) {
        window.AppHub.download("devbench_export.txt", "data:text/plain;base64," + b64, "text/plain");
      } else {
        const a = document.createElement('a');
        a.href = "data:text/plain;base64," + b64;
        a.download = "devbench_export.txt";
        a.click();
      }
    }

    function testBridgeShare() {
      if (window.AppHub && window.AppHub.shareFile) {
        window.AppHub.shareFile("App Hub Pro Universal Runtime is active!");
      } else {
        alert("Share bridge called");
      }
    }

    document.getElementById('benchPicker').addEventListener('change', (e) => {
      const files = e.target.files;
      document.getElementById('pickerResult').innerText = 'Selected ' + files.length + ' file(s): ' + Array.from(files).map(f => f.name).join(', ');
      console.log('File picker success, files:', Array.from(files).map(f => f.name));
    });
  </script>
</body>
</html>"""
}
