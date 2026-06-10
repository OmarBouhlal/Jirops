const http = require('http');

function get(path) {
  return new Promise((resolve) => {
    const req = http.get({ hostname: 'localhost', port: 8080, path, timeout: 5000 }, (res) => {
      console.log('GET', path, 'status', res.statusCode);
      res.on('data', () => {});
      res.on('end', () => resolve());
    });
    req.on('error', (e) => {
      console.error('GET', path, 'error', e && e.message);
      resolve();
    });
  });
}

function post(path, body) {
  return new Promise((resolve) => {
    const data = JSON.stringify(body || {});
    const req = http.request(
      { hostname: 'localhost', port: 8080, path, method: 'POST', timeout: 5000, headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(data) } },
      (res) => {
        console.log('POST', path, 'status', res.statusCode);
        res.on('data', () => {});
        res.on('end', () => resolve());
      }
    );
    req.on('error', (e) => {
      console.error('POST', path, 'error', e && e.message);
      resolve();
    });
    req.write(data);
    req.end();
  });
}

(async function run() {
  await get('/projects');
  await post('/auth/logout', { refreshToken: 'probe' });
  console.log('probe complete');
})();
