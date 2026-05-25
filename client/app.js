let token = null;
let userId = null;
let maxSlots = 0;
let animationEnabled = false;

let privateKey = null;
let publicKeyBase64 = null;

let statusInterval = null;

const $ = (id) =>
  document.getElementById(id);

const log = (msg) => {
  $('log').textContent =
    `${new Date().toLocaleTimeString()} ${msg}\n`
    + $('log').textContent;
};

const api = (path) =>
  `${$('endpoint').value.replace(/\/$/, '')}${path}`;

const sleep = (ms) =>
  new Promise(resolve => setTimeout(resolve, ms));

function setLight(lightId, textId, state, text) {
  const light = $(lightId);
  const label = $(textId);

  light.classList.remove('green', 'red', 'amber');

  if (state === 'ok') {
    light.classList.add('green');
  } else if (state === 'warning') {
    light.classList.add('amber');
  } else {
    light.classList.add('red');
  }

  label.textContent = text;
}

async function checkServerConnection() {
  try {
    const res = await fetch(api('/health'));

    if (!res.ok) {
      throw new Error();
    }

    setLight(
      'serverLight',
      'serverStatusText',
      'ok',
      'OK'
    );

    return true;
  } catch (e) {
    setLight(
      'serverLight',
      'serverStatusText',
      'error',
      'Disconnected'
    );

    return false;
  }
}

async function checkPufDeviceConnection() {
  try {
    const response = await request('/device/status');

    if (response.response === 'OK READY') {
      setLight(
        'deviceLight',
        'deviceStatusText',
        'ok',
        'OK'
      );

      return true;
    }

    if (response.response === 'NOK POWER_CYCLE_REQUIRED') {
      setLight(
        'deviceLight',
        'deviceStatusText',
        'warning',
        'Connected but needs Repower'
      );

      return true;
    }

    setLight(
      'deviceLight',
      'deviceStatusText',
      'warning',
      response.response
    );

    return true;
  } catch (e) {
    setLight(
      'deviceLight',
      'deviceStatusText',
      'error',
      'Disconnected'
    );

    return false;
  }
}

async function refreshConnectionStatus() {
  const serverOk = await checkServerConnection();

  if (serverOk && token) {
    await checkPufDeviceConnection();
  } else {
    setLight(
      'deviceLight',
      'deviceStatusText',
      'error',
      'Unknown'
    );
  }
}

async function generateClientKeys() {
  const pair =
    await crypto.subtle.generateKey(
      {
        name: 'RSA-OAEP',
        modulusLength: 2048,
        publicExponent:
          new Uint8Array([1, 0, 1]),
        hash: 'SHA-256'
      },
      true,
      ['encrypt', 'decrypt']
    );

  privateKey = pair.privateKey;

  const spki =
    await crypto.subtle.exportKey(
      'spki',
      pair.publicKey
    );

  publicKeyBase64 = btoa(
    String.fromCharCode(
      ...new Uint8Array(spki)
    )
  );
}

async function decryptPassword(encryptedBase64) {
  const bytes =
    Uint8Array.from(
      atob(encryptedBase64),
      c => c.charCodeAt(0)
    );

  const decrypted =
    await crypto.subtle.decrypt(
      { name: 'RSA-OAEP' },
      privateKey,
      bytes
    );

  return new TextDecoder()
    .decode(decrypted);
}

async function request(path, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };

  if (token) {
    headers.Authorization =
      `Bearer ${token}`;
  }

  const res =
    await fetch(api(path), {
      ...options,
      headers
    });

  const body =
    await res.json()
      .catch(() => ({}));

  if (!res.ok) {
    throw new Error(
      body.error || `HTTP ${res.status}`
    );
  }

  return body;
}

function parseServices(lines) {
  return (lines || [])
    .filter(line =>
      line.includes('ACTIVE')
    )
    .map(line => {
      const parts =
        line.split(' ');

      return {
        slot: parts[1],
        serviceId: parts[3]
      };
    });
}

function showPassword(serviceId, password) {
  const target =
    document.querySelector(
      `[data-password-for="${serviceId}"]`
    );

  if (!target) {
    return;
  }

  target.textContent = password;
  target.classList.remove('hidden');
  target.dataset.password = password;
}

function capitalize(s) {
  return s.charAt(0).toUpperCase()
    + s.slice(1);
}

function switchTab(tab) {
  $('servicesTab').classList.add('hidden');
  $('diagnosticsTab').classList.add('hidden');
  $('uartTab').classList.add('hidden');
  $('logsTab').classList.add('hidden');

  $('tabServices').classList.add('secondary');
  $('tabDiagnostics').classList.add('secondary');
  $('tabUart').classList.add('secondary');
  $('tabLogs').classList.add('secondary');

  $(tab + 'Tab')
    .classList.remove('hidden');

  $('tab' + capitalize(tab))
    .classList.remove('secondary');
}

$('tabServices').onclick =
  () => switchTab('services');

$('tabDiagnostics').onclick =
  () => switchTab('diagnostics');

$('tabUart').onclick =
  () => switchTab('uart');

$('tabLogs').onclick =
  () => switchTab('logs');

async function playFlow(steps) {
  if (!animationEnabled) {
    return;
  }

  const packet = $('packet');
  const caption = $('animationCaption');

  const positions = {
    client: '9%',
    server: '48%',
    device: '86%'
  };

  for (const step of steps) {
    packet.classList.remove('encrypted', 'plain');
    packet.classList.add(step.encrypted ? 'encrypted' : 'plain');
    packet.textContent = step.encrypted ? '🔒' : '📦';

    packet.style.left = positions[step.from];
    packet.style.opacity = '1';

    caption.textContent =
      `${step.label} — ${step.encrypted ? 'encrypted' : 'plain/trusted local'}`;

    await sleep(120);

    packet.style.left = positions[step.to];

    await sleep(950);

    packet.style.opacity = '0';

    await sleep(250);
  }

  caption.textContent = '';
}

async function refreshServices() {
  const data =
    await request('/services');

  const services =
    parseServices(data.lines);

  const box =
    $('services');

  box.innerHTML = '';

  if (services.length === 0) {
    box.innerHTML =
      `<p class="muted">No services registered. ${services.length}/${maxSlots} slots used.</p>`;

    return;
  }

  const summary =
    document.createElement('p');

  summary.className = 'muted';
  summary.textContent =
    `${services.length}/${maxSlots} slots used`;

  box.appendChild(summary);

  for (const svc of services) {
    const row =
      document.createElement('div');

    row.className = 'service';

    row.innerHTML = `
      <div>
        <div>${svc.serviceId}</div>

        <div
          class="revealed-password hidden"
          data-password-for="${svc.serviceId}">
        </div>
      </div>

      <div class="service-actions">

        <button data-gen="${svc.serviceId}">
          Reveal
        </button>

        <button
          class="secondary"
          data-rot="${svc.serviceId}">
          Rotate
        </button>

        <button
          class="danger"
          data-del="${svc.serviceId}">
          Delete
        </button>

      </div>
    `;

    box.appendChild(row);
  }
}

async function refreshDiagnostics() {
  const data =
    await request('/device/diagnostics');

  $('diagnostics').textContent =
    JSON.stringify(data, null, 2);
}

async function refreshUartMonitor() {
  const data =
    await request('/device/uart');

  const box =
    $('uartMonitor');

  box.innerHTML = '';

  for (const entry of data.entries) {
    const div =
      document.createElement('div');

    div.className = 'uart-entry';

    div.innerHTML = `
      <div class="uart-meta">
        ${entry.timestamp}
      </div>

      <div class="uart-meta">
        Sender: ${entry.sender}
        →
        Receiver: ${entry.receiver}
      </div>

      <div>
        ${entry.message}
      </div>
    `;

    box.appendChild(div);
  }
}

$('loginBtn').onclick =
  async () => {
    try {
      await generateClientKeys();

      await playFlow([
        {
          from: 'client',
          to: 'server',
          label: 'Login request over HTTPS',
          encrypted: true
        }
      ]);

      const data =
        await request('/login', {
          method: 'POST',
          body: JSON.stringify({
            username:
              $('username').value,
            password:
              $('password').value,
            publicKey:
              publicKeyBase64
          })
        });

      token = data.token;
      userId = data.userId;
      maxSlots = data.maxSlots;
      animationEnabled = data.animationEnabled === true;

      $('who').textContent = userId;
      $('slotLimit').textContent = maxSlots;

      if (animationEnabled) {
        $('animationPanel').classList.remove('hidden');
      } else {
        $('animationPanel').classList.add('hidden');
      }

      $('loginView').classList.add('hidden');
      $('vaultView').classList.remove('hidden');

      log('Logged in');

      await refreshServices();
      await refreshDiagnostics();
      await refreshUartMonitor();
      await refreshConnectionStatus();

      if (statusInterval) {
        clearInterval(statusInterval);
      }

      statusInterval =
        setInterval(
          refreshConnectionStatus,
          5000
        );

    } catch (e) {
      log(
        `Login failed: ${e.message}`
      );
    }
  };

$('refreshBtn').onclick =
  async () => {
    try {
      await refreshServices();
      await refreshDiagnostics();
      await refreshUartMonitor();
      await refreshConnectionStatus();
    } catch (e) {
      log(
        `Refresh failed: ${e.message}`
      );
    }
  };

$('addServiceBtn').onclick =
  async () => {
    try {
      const serviceId =
        $('serviceId')
          .value
          .trim();

      if (!serviceId) {
        return;
      }

      await playFlow([
        {
          from: 'client',
          to: 'server',
          label: 'Add service request over HTTPS',
          encrypted: true
        },
        {
          from: 'server',
          to: 'device',
          label: 'UART add-service command',
          encrypted: false
        },
        {
          from: 'device',
          to: 'server',
          label: 'Arduino confirms slot allocation',
          encrypted: false
        },
        {
          from: 'server',
          to: 'client',
          label: 'HTTPS response',
          encrypted: true
        }
      ]);

      const response =
        await request(
          '/services',
          {
            method: 'POST',
            body: JSON.stringify({
              serviceId
            })
          }
        );

      log(response.response);

      $('serviceId').value = '';

      await refreshServices();
      await refreshDiagnostics();
      await refreshUartMonitor();
      await refreshConnectionStatus();
    } catch (e) {
      log(
        `Add service failed: ${e.message}`
      );
    }
  };

$('diagBtn').onclick =
  async () => {
    try {
      await refreshDiagnostics();
    } catch (e) {
      log(
        `Diagnostics failed: ${e.message}`
      );
    }
  };

$('statusBtn').onclick =
  async () => {
    try {
      const response =
        await request(
          '/device/status'
        );

      log(response.response);

      await refreshDiagnostics();
      await refreshConnectionStatus();
    } catch (e) {
      log(
        `Status failed: ${e.message}`
      );
    }
  };

$('reconnectBtn').onclick =
  async () => {
    try {
      const response =
        await request(
          '/device/reconnect',
          {
            method: 'POST'
          }
        );

      log(response.response);

      await refreshDiagnostics();
      await refreshUartMonitor();
      await refreshConnectionStatus();
    } catch (e) {
      log(
        `Reconnect failed: ${e.message}`
      );
    }
  };

$('refreshUartBtn').onclick =
  async () => {
    try {
      await refreshUartMonitor();
    } catch (e) {
      log(
        `UART refresh failed: ${e.message}`
      );
    }
  };

$('services').onclick =
  async (ev) => {
    const gen =
      ev.target.getAttribute('data-gen');

    const rot =
      ev.target.getAttribute('data-rot');

    const del =
      ev.target.getAttribute('data-del');

    try {
      if (gen) {
        await playFlow([
          {
            from: 'client',
            to: 'server',
            label: 'Reveal request over HTTPS',
            encrypted: true
          },
          {
            from: 'server',
            to: 'device',
            label: 'UART generate-password command',
            encrypted: false
          },
          {
            from: 'device',
            to: 'server',
            label: 'Generated password over trusted UART',
            encrypted: false
          },
          {
            from: 'server',
            to: 'client',
            label: 'RSA encrypted password response',
            encrypted: true
          }
        ]);

        const response =
          await request(
            `/services/${encodeURIComponent(gen)}/generate`,
            {
              method: 'POST'
            }
          );

        const password =
          await decryptPassword(
            response.encryptedPassword
          );

        showPassword(
          gen,
          password
        );

        log(
          `Password revealed for ${gen}`
        );
      }

      if (rot) {
        await playFlow([
          {
            from: 'client',
            to: 'server',
            label: 'Rotate request over HTTPS',
            encrypted: true
          },
          {
            from: 'server',
            to: 'device',
            label: 'UART rotate-service command',
            encrypted: false
          },
          {
            from: 'device',
            to: 'server',
            label: 'Arduino increments version',
            encrypted: false
          },
          {
            from: 'server',
            to: 'client',
            label: 'HTTPS response',
            encrypted: true
          }
        ]);

        const response =
          await request(
            `/services/${encodeURIComponent(rot)}/rotate`,
            {
              method: 'POST'
            }
          );

        log(response.response);

        await refreshServices();
      }

      if (del) {
        const confirmed =
          confirm(
            `Delete ${del}? Device will require repower.`
          );

        if (!confirmed) {
          return;
        }

        await playFlow([
          {
            from: 'client',
            to: 'server',
            label: 'Delete request over HTTPS',
            encrypted: true
          },
          {
            from: 'server',
            to: 'device',
            label: 'UART delete-service command',
            encrypted: false
          },
          {
            from: 'device',
            to: 'server',
            label: 'Device requires repower',
            encrypted: false
          },
          {
            from: 'server',
            to: 'client',
            label: 'HTTPS response',
            encrypted: true
          }
        ]);

        const response =
          await request(
            `/services/${encodeURIComponent(del)}`,
            {
              method: 'DELETE'
            }
          );

        log(response.response);

        await refreshServices();
      }

      await refreshDiagnostics();
      await refreshUartMonitor();
      await refreshConnectionStatus();

    } catch (e) {
      log(
        `Operation failed: ${e.message}`
      );
    }
  };