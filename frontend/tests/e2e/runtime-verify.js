// runtime-verify.js - test real API endpoints on Docker runtime
const http = require('http');

function postJson(path, data) {
  return new Promise((resolve, reject) => {
    const postData = JSON.stringify(data);
    const req = http.request({
      hostname: 'localhost',
      port: 8080,
      path: path,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(postData)
      }
    }, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, data: JSON.parse(body) });
        } catch (e) {
          resolve({ status: res.statusCode, raw: body });
        }
      });
    });
    req.on('error', reject);
    req.write(postData);
    req.end();
  });
}

function getJson(path, token) {
  return new Promise((resolve, reject) => {
    const headers = { 'Accept': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const req = http.request({
      hostname: 'localhost',
      port: 8080,
      path: path,
      method: 'GET',
      headers: headers
    }, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, data: JSON.parse(body) });
        } catch (e) {
          resolve({ status: res.statusCode, raw: body });
        }
      });
    });
    req.on('error', reject);
    req.end();
  });
}

async function main() {
  console.log('=== RUNTIME VERIFICATION SUITE ===');
  
  // 1. Health check
  const health = await getJson('/actuator/health');
  console.log('1. Actuator Health:', health.status, JSON.stringify(health.data));
  
  // 2. Discover Events
  const events = await getJson('/api/v1/events');
  console.log('2. Public Event Discovery:', events.status, 'Results count:', events.data?.content?.length ?? 0);
  
  // 3. User Login
  const loginRes = await postJson('/api/v1/auth/login', {
    email: 'admin@civicpulse.org',
    password: 'Password@2026!'
  });
  console.log('3. Admin Authentication Login:', loginRes.status, 'Has accessToken:', !!loginRes.data?.accessToken);
  const adminToken = loginRes.data?.accessToken;

  // 4. Organizer Login
  const orgLogin = await postJson('/api/v1/auth/login', {
    email: 'organizer@civicpulse.org',
    password: 'Password@2026!'
  });
  console.log('4. Organizer Login:', orgLogin.status, 'Role:', orgLogin.data?.user?.role);
  const orgToken = orgLogin.data?.accessToken;

  // 5. Member Register
  const newEmail = `volunteer_${Date.now()}@civicpulse.org`;
  const registerRes = await postJson('/api/v1/auth/register', {
    email: newEmail,
    password: 'Password@2026!',
    fullName: 'Runtime Test Volunteer'
  });
  console.log('5. New Member Self-Registration:', registerRes.status, 'Created ID:', registerRes.data?.user?.id);
  const memberToken = registerRes.data?.accessToken;

  // 6. Cross-Tenant / Unauthorized Access Rejection Test
  const unauthorizedAdminAction = await getJson('/api/v1/analytics/platform', memberToken);
  console.log('6. Member attempting Admin platform analytics (Expect 403 Forbidden):', unauthorizedAdminAction.status);

  // 7. Security Rate Limiting Test
  console.log('7. Rate Limiting verification on auth routes:');
  let rateLimitHit = false;
  for (let i = 0; i < 20; i++) {
    const res = await postJson('/api/v1/auth/login', { email: 'bad@test.org', password: 'wrong' });
    if (res.status === 429) {
      rateLimitHit = true;
      console.log(`   Rate limit triggered HTTP 429 at attempt ${i + 1}`);
      break;
    }
  }
  console.log('   Rate Limiter Active:', rateLimitHit);

  console.log('=== RUNTIME VERIFICATION COMPLETE ===');
}

main().catch(err => {
  console.error('Test script error:', err);
  process.exit(1);
});
