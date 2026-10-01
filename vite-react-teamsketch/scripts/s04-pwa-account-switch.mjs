const origin = 'http://127.0.0.1:4174';
const targets = await fetch('http://127.0.0.1:9224/json').then((response) => response.json());
const target = targets.find((item) => item.type === 'page' && item.url.startsWith(origin));
if (!target) throw new Error('Haru page target not found');

const socket = new WebSocket(target.webSocketDebuggerUrl);
await new Promise((resolve, reject) => {
  socket.addEventListener('open', resolve, { once: true });
  socket.addEventListener('error', reject, { once: true });
});

let id = 0;
let currentEmail = '';
const pending = new Map();
const send = (method, params = {}) => new Promise((resolve, reject) => {
  const messageId = ++id;
  pending.set(messageId, { resolve, reject });
  socket.send(JSON.stringify({ id: messageId, method, params }));
});
const jsonBody = (value) => Buffer.from(JSON.stringify(value)).toString('base64');
const userFixture = (email) => {
  const account = email.startsWith('a@') ? 'A' : 'B';
  return {
    id: account === 'A' ? 1 : 2,
    email,
    name: `${account}_NAME`,
    nickname: `${account}_NICK`,
    phoneNumber: null,
    bio: `${account}_PROFILE_PRIVATE`,
    loginMethod: 'EMAIL',
    socialProvider: 'NONE',
    accountStatus: 'Active',
    authority: 'USER',
    signupDate: '2026-09-29',
    lastUpdateDate: '2026-09-29',
    lastLoginTime: null,
    dopamine: account === 'A' ? 11 : 22,
    points: account === 'A' ? 111 : 222,
    hobbies: []
  };
};

const fulfill = async (requestId, body, status = 200) => {
  await send('Fetch.fulfillRequest', {
    requestId,
    responseCode: status,
    responseHeaders: [
      { name: 'Content-Type', value: 'application/json' },
      { name: 'Cache-Control', value: 'no-store' },
      { name: 'Access-Control-Allow-Origin', value: origin }
    ],
    body: jsonBody(body)
  });
};

const handleApi = async ({ requestId, request }) => {
  const url = new URL(request.url);
  const path = url.pathname;
  if (path === '/api/core/auth/login') {
    currentEmail = JSON.parse(request.postData || '{}').email;
    const user = userFixture(currentEmail);
    await fulfill(requestId, {
      status: 'success',
      data: { email: user.email, nickname: user.nickname, id: user.id, token: `token-${user.id}` }
    });
  } else if (path === '/api/core/profiles/me') {
    await fulfill(requestId, { status: 'success', data: userFixture(currentEmail) });
  } else if (path === '/api/core/chat/rooms/active') {
    const account = currentEmail.startsWith('a@') ? 'A' : 'B';
    await fulfill(requestId, {
      status: 'success',
      data: {
        success: true,
        chatRooms: [{
          chatroomId: account === 'A' ? 101 : 202,
          chatname: `${account}_PRIVATE_CHAT_ROOM`,
          productId: account === 'A' ? 11 : 22,
          registrantEmail: `${account.toLowerCase()}-seller@haru.com`,
          productName: `${account}_PRODUCT`,
          productImageUrl: '',
          sellerEmail: `${account.toLowerCase()}-seller@haru.com`,
          requestEmail: currentEmail,
          otherUserEmail: `${account.toLowerCase()}-other@haru.com`,
          otherUserName: `${account}_CHAT_PARTNER`,
          lastMessage: `${account}_PRIVATE_CHAT_MESSAGE`,
          lastMessageTime: '2026-09-29T00:00:00Z',
          status: 'ACTIVE',
          createdAt: '2026-09-29T00:00:00Z',
          updatedAt: '2026-09-29T00:00:00Z',
          unreadCount: 1,
          messageType: 'TEXT'
        }]
      }
    });
  } else if (path === '/api/core/market/transactions/list') {
    const account = currentEmail.startsWith('a@') ? 'A' : 'B';
    await fulfill(requestId, {
      status: 'success',
      message: 'ok',
      data: [{
        id: account === 'A' ? 301 : 302,
        productId: account === 'A' ? 31 : 32,
        buyerEmail: currentEmail,
        sellerEmail: `${account.toLowerCase()}-seller@haru.com`,
        transactionStatus: '진행중',
        paymentStatus: '미완료',
        price: account === 'A' ? 3100 : 3200,
        description: `${account}_PRIVATE_TRANSACTION`,
        createdAt: [2026, 9, 29, 0, 0]
      }]
    });
  } else if (path === '/api/core/profiles/me/image-info') {
    await fulfill(requestId, { status: 'success', data: { imageUrl: null } });
  } else if (path === '/api/core/hobbies/categories' || path === '/api/core/hobbies') {
    await fulfill(requestId, { status: 'success', data: [] });
  } else {
    await fulfill(requestId, { status: 'success', data: null });
  }
};

socket.addEventListener('message', ({ data }) => {
  const message = JSON.parse(data);
  if (message.method === 'Fetch.requestPaused') {
    handleApi(message.params).catch((error) => console.error(error));
    return;
  }
  if (!message.id || !pending.has(message.id)) return;
  const { resolve, reject } = pending.get(message.id);
  pending.delete(message.id);
  if (message.error) reject(new Error(JSON.stringify(message.error)));
  else resolve(message.result);
});

const evaluate = async (expression) => {
  const result = await send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true });
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.text);
  return result.result.value;
};
const wait = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds));
const waitFor = async (expression, timeout = 6000) => {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) {
    try {
      if (await evaluate(expression)) return;
    } catch {
      // Navigation can replace the execution context between polls.
    }
    await wait(100);
  }
  throw new Error(`Timed out: ${expression}`);
};
const spaNavigate = async (path) => {
  await evaluate(`history.pushState({}, '', ${JSON.stringify(path)}); dispatchEvent(new PopStateEvent('popstate'))`);
  await wait(300);
};
const login = async (email) => {
  await waitFor(`location.pathname === '/login' && Boolean(document.querySelector('[name="email"]'))`);
  await evaluate(`(() => {
    const setValue = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
    const email = document.querySelector('[name="email"]');
    const password = document.querySelector('[name="password"]');
    setValue.call(email, ${JSON.stringify(email)});
    email.dispatchEvent(new Event('input', { bubbles: true }));
    setValue.call(password, 'Password1!');
    password.dispatchEvent(new Event('input', { bubbles: true }));
    document.querySelector('[data-testid="login-button"]').click();
  })()`);
  await waitFor(`localStorage.getItem('user')?.includes(${JSON.stringify(email)})`);
  await waitFor(`location.pathname === '/my-location'`);
  await evaluate(`localStorage.setItem('locationSet', 'true')`);
};

await send('Runtime.enable');
await send('Network.enable');
await send('Fetch.enable', { patterns: [{ urlPattern: `${origin}/api/*`, requestStage: 'Request' }] });
await evaluate(`localStorage.clear(); caches.keys().then((names) => Promise.all(names.map((name) => caches.delete(name))))`);
await send('Page.reload');
await waitFor(`location.pathname === '/login'`);
let registration = await evaluate(`(async () => {
  const registration = await navigator.serviceWorker.ready;
  return { active: registration.active?.state, controlled: Boolean(navigator.serviceWorker.controller) };
})()`);

await login('a@haru.com');
await wait(600);
await send('Page.reload');
await waitFor(`document.body.innerText.includes('A_PROFILE_PRIVATE') || localStorage.getItem('persist:root')?.includes('a@haru.com')`);
await waitFor(`Boolean(navigator.serviceWorker.controller)`);
registration = await evaluate(`(async () => { const registration = await navigator.serviceWorker.ready; return { active: registration.active?.state, controlled: Boolean(navigator.serviceWorker.controller) }; })()`);

await spaNavigate('/mypage');
await waitFor(`document.body.innerText.includes('a@haru.com') && document.body.innerText.includes('A_NICK')`);
const aProfileVisible = await evaluate(`document.body.innerText.includes('a@haru.com') && document.body.innerText.includes('A_NICK')`);

await spaNavigate('/chat-list');
await waitFor(`document.body.innerText.includes('A_PRIVATE_CHAT_MESSAGE')`);
const aChatVisible = await evaluate(`document.body.innerText.includes('A_PRIVATE_CHAT_MESSAGE')`);

await spaNavigate('/transaction-detail');
await waitFor(`document.body.innerText.includes('a@haru.com')`);
const aTransactionVisible = await evaluate(`document.body.innerText.includes('a@haru.com')`);

await evaluate(`(async () => {
  const cache = await caches.open('api-cache');
  await cache.put('/api/core/profiles/me', new Response(JSON.stringify({ email: 'a@haru.com', marker: 'A_PROFILE_PRIVATE' })));
})()`);
await spaNavigate('/setting');
await evaluate(`Array.from(document.querySelectorAll('button')).find((button) => button.textContent?.trim() === '로그아웃')?.click()`);
await wait(100);
await evaluate(`Array.from(document.querySelectorAll('button')).filter((button) => button.textContent?.trim() === '로그아웃').at(-1)?.click()`);
await waitFor(`location.pathname === '/login' && !localStorage.getItem('token')`);
await wait(500);

const afterLogout = await evaluate(`(async () => ({
  persistedHasA: localStorage.getItem('persist:root')?.includes('a@haru.com') || false,
  apiCacheExists: (await caches.keys()).includes('api-cache'),
  apiEntries: (await Promise.all((await caches.keys()).map(async (name) => (await caches.open(name)).keys()))).flat().filter((request) => new URL(request.url).pathname.startsWith('/api/')).map((request) => request.url)
}))()`);

await login('b@haru.com');
await wait(500);
await send('Network.emulateNetworkConditions', { offline: true, latency: 0, downloadThroughput: 0, uploadThroughput: 0 });

await spaNavigate('/mypage');
await waitFor(`document.body.innerText.includes('b@haru.com') && document.body.innerText.includes('B_NICK')`);
const bProfile = await evaluate(`({ body: document.body.innerText, persisted: localStorage.getItem('persist:root') })`);

await spaNavigate('/chat-list');
await wait(2500);
const bChatBody = await evaluate(`document.body.innerText`);

await spaNavigate('/transaction-detail');
await wait(1000);
const bTransactionBody = await evaluate(`document.body.innerText`);
const finalStorage = await evaluate(`(async () => ({
  token: localStorage.getItem('token'),
  user: localStorage.getItem('user'),
  persisted: localStorage.getItem('persist:root'),
  apiCacheExists: (await caches.keys()).includes('api-cache'),
  apiEntries: (await Promise.all((await caches.keys()).map(async (name) => (await caches.open(name)).keys()))).flat().filter((request) => new URL(request.url).pathname.startsWith('/api/')).map((request) => request.url)
}))()`);

await send('Network.emulateNetworkConditions', { offline: false, latency: 0, downloadThroughput: -1, uploadThroughput: -1 });
await send('Fetch.disable');

const forbidden = ['a@haru.com', 'A_PROFILE_PRIVATE', 'A_PRIVATE_CHAT_MESSAGE', 'A_PRIVATE_TRANSACTION', 'A_NICK'];
const bOutputs = [bProfile.body, bProfile.persisted, bChatBody, bTransactionBody, finalStorage.user, finalStorage.persisted].filter(Boolean);
const leakedMarkers = forbidden.filter((marker) => bOutputs.some((output) => output.includes(marker)));
const result = {
  registration,
  aDataLoaded: { profile: aProfileVisible, chat: aChatVisible, transaction: aTransactionVisible },
  afterLogout,
  bOffline: {
    profileShowsB: bProfile.body.includes('b@haru.com') && bProfile.body.includes('B_NICK'),
    chatShowsA: bChatBody.includes('A_PRIVATE_CHAT_MESSAGE'),
    transactionShowsA: bTransactionBody.includes('A_PRIVATE_TRANSACTION') || bTransactionBody.includes('a@haru.com'),
    leakedMarkers,
    apiCacheExists: finalStorage.apiCacheExists,
    apiEntries: finalStorage.apiEntries,
    persistedHasA: finalStorage.persisted?.includes('a@haru.com') || false
  }
};
console.log(JSON.stringify(result, null, 2));
if (!registration.controlled || registration.active !== 'activated') throw new Error('Service worker is not active');
if (!Object.values(result.aDataLoaded).every(Boolean)) throw new Error('A fixtures were not loaded');
if (afterLogout.persistedHasA || afterLogout.apiCacheExists || afterLogout.apiEntries.length) throw new Error('A data survived logout');
if (!result.bOffline.profileShowsB || result.bOffline.leakedMarkers.length || result.bOffline.apiCacheExists || result.bOffline.apiEntries.length) throw new Error('A data leaked to B offline session');
socket.close();
