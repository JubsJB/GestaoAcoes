'use strict';
const {test} = require('node:test');
const assert = require('node:assert/strict');
const {spawn} = require('node:child_process');
const http = require('node:http');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const browser = process.env.APRESENTACAO_BROWSER_EXE;
test('navegador aceita cookies Secure no HTTP local e só permite ler o CSRF', {skip: !browser}, async () => {
  const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'ga-browser-fixture-'));
  const server = http.createServer((req, res) => {
    if (req.url === '/') {
      res.setHeader('Set-Cookie', [
        'GESTAOACOES_AUTH=fixture; Path=/; HttpOnly; SameSite=Lax; Secure',
        'XSRF-TOKEN=fixture; Path=/; SameSite=Lax; Secure'
      ]);
      res.end(`<body>PENDING<script>
        (async () => {
          const cookies = document.cookie.split('; ');
          const csrf = cookies.includes('XSRF-TOKEN=fixture');
          const response = await fetch('/roundtrip', {method:'POST', headers:{'X-XSRF-TOKEN':csrf?'fixture':''}});
          document.body.textContent = response.ok && !cookies.some(v => v.startsWith('GESTAOACOES_AUTH='))
            ? 'FIXTURE_PASS' : 'FIXTURE_FAIL';
        })().catch(() => document.body.textContent = 'FIXTURE_FAIL');
      </script>`);
      return;
    }
    const auth = (req.headers.cookie || '').split('; ').includes('GESTAOACOES_AUTH=fixture');
    const csrf = req.headers['x-xsrf-token'] === 'fixture';
    res.writeHead(req.url === '/roundtrip' && auth && csrf ? 204 : 403).end();
  });
  try {
    await new Promise((resolve, reject) => {
      server.once('error', reject);
      server.listen(8081, '127.0.0.1', resolve);
    });
    const output = await new Promise((resolve, reject) => {
      const child = spawn(browser, ['--headless=new','--disable-gpu','--no-first-run',
        '--disable-background-networking','--disable-component-update','--disable-sync','--no-proxy-server',
        '--host-resolver-rules=MAP * ~NOTFOUND, EXCLUDE localhost',
        '--user-data-dir='+profile, '--dump-dom','--virtual-time-budget=5000','http://localhost:8081/'],
      {stdio:['ignore','pipe','ignore'],timeout:25000});
      let html = '';
      child.stdout.setEncoding('utf8').on('data', chunk => {html += chunk;});
      child.on('error', reject);
      child.on('close', code => code === 0 ? resolve(html) : reject(new Error('Navegador encerrou com código '+code)));
    });
    assert.match(output, /<body>FIXTURE_PASS<\/body>/);
  } finally {
    if (server.listening) await new Promise(resolve => server.close(resolve));
    fs.rmSync(profile,{recursive:true,force:true,maxRetries:3,retryDelay:200});
  }
});
