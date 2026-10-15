import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..');
const output = path.join(here, 'haru.postman_collection.json');

const script = (...lines) => [{ listen: 'test', script: { type: 'text/javascript', exec: lines } }];
const pre = (...lines) => [{ listen: 'prerequest', script: { type: 'text/javascript', exec: lines } }];
const jsonBody = (value) => ({ mode: 'raw', raw: JSON.stringify(value, null, 2), options: { raw: { language: 'json' } } });
const request = (name, method, url, options = {}) => ({
  name,
  request: {
    method,
    header: options.headers ?? (options.body ? [{ key: 'Content-Type', value: 'application/json' }] : []),
    ...(options.auth ? { auth: options.auth } : {}),
    ...(options.body ? { body: jsonBody(options.body) } : {}),
    url: { raw: `{{baseUrl}}${url}`, host: ['{{baseUrl}}'], path: url.split('/').filter(Boolean) },
    ...(options.description ? { description: options.description } : {}),
  },
  ...((options.pre?.length || options.tests?.length) ? { event: [...(options.pre ?? []), ...(options.tests ?? [])] } : {}),
});

const withPath = (item, path) => ({ ...item, request: { ...item.request, url: { ...item.request.url, raw: `{{baseUrl}}/${path.join('/')}`, path } } });

const ok = script(
  "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
  "const body = pm.response.json();",
  "pm.test('success response', () => { pm.expect(body.status).to.eql('success'); if (typeof body.data?.success === 'boolean') pm.expect(body.data.success).to.eql(true); });",
);
const auth = { type: 'bearer', bearer: [{ key: 'token', value: '{{token}}', type: 'string' }] };
const sessionGuard = pre(
  "if (!pm.variables.get('token')) { pm.test('authenticated session exists', () => pm.expect.fail('Login did not produce a token; later authenticated requests were not sent.')); pm.execution.skipRequest(); }",
);

function javaFiles(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const full = path.join(directory, entry.name);
    return entry.isDirectory() ? javaFiles(full) : entry.name.endsWith('Controller.java') ? [full] : [];
  });
}

export function controllerRoutes() {
  const directories = [
    path.join(root, 'CoreService/src/main/java/com/example/demo/controller'),
    path.join(root, 'AssistService/src/main/java/com/example/demo/controller'),
  ];
  const samples = {
    boardId: '1', postId: '1', commentId: '1', chatroomId: '1', memberId: '1', productId: '1',
    transactionId: '1', imageId: '1', categoryId: '1', id: '1', email: 'postman@example.com',
    nickname: 'postman', filename: 'missing.png', fileName: 'missing.png', fileId: 'missing.png', fileType: 'posts',
  };
  return directories.flatMap(javaFiles).flatMap((file) => {
    const source = fs.readFileSync(file, 'utf8');
    // Servlet error dispatch (/error) is not a REST API route.
    if (/\bimplements\s+ErrorController\b/.test(source)) return [];
    const base = source.match(/@RequestMapping\("([^"]+)"\)[\s\S]*?public class /)?.[1];
    if (!base) throw new Error(`Class-level @RequestMapping not found: ${path.relative(root, file)}`);
    const routes = [];
    // shortcut: mapping annotations must keep their path on one line, use a Java parser if controllers adopt multiline mappings.
    const mapping = /@(Get|Post|Put|Delete|Patch)Mapping(?:\s*\(([^\n]*)\))?/g;
    for (const match of source.matchAll(mapping)) {
      const method = match[1].toUpperCase();
      const suffix = match[2]?.match(/"([^"]*)"/)?.[1] ?? '';
      const template = `${base}${suffix}`;
      const actualPath = template.replace(/\{([^}:]+)(?::[^}]+)?\}/g, (_, key) => {
        if (!(key in samples)) throw new Error(`No sample for {${key}} in ${template}`);
        return encodeURIComponent(samples[key]);
      });
      routes.push({ method, template, path: actualPath });
    }
    return routes;
  }).sort((a, b) => `${a.path} ${a.method}`.localeCompare(`${b.path} ${b.method}`));
}

const startup = [
  request('Public hobbies', 'GET', '/api/core/hobbies', { tests: ok }),
  request('Public hobbies simple', 'GET', '/api/core/hobbies/simple', { tests: ok }),
  request('Public hobby categories', 'GET', '/api/core/hobbies/categories', { tests: ok }),
  request('Public products', 'GET', '/api/core/market/products/all', { tests: ok }),
  request('OAuth status', 'GET', '/api/core/auth/oauth2/status', {
    tests: script("pm.test('HTTP 200', () => pm.response.to.have.status(200));"),
  }),
  request('Empty login is rejected', 'POST', '/api/core/auth/login', {
    body: { email: '', password: '' },
    tests: script(
      "pm.test('HTTP 400', () => pm.response.to.have.status(400));",
      "pm.test('BAD_REQUEST with message', () => { const body = pm.response.json(); pm.expect(body.status).to.eql('error'); pm.expect(body.code).to.eql('BAD_REQUEST'); pm.expect(body.message).to.be.a('string').and.not.empty; });",
    ),
  }),
  withPath(request('Gateway rejects non-normalized path', 'GET', '/api/core/hobbies', {
    tests: script(
      "pm.test('HTTP 400', () => pm.response.to.have.status(400));",
      "pm.test('INVALID_PATH', () => pm.expect(pm.response.json().code).to.eql('INVALID_PATH'));",
    ),
    description: 'Trailing slash path. The gateway rejects it before routing with the shared error envelope.',
  }), ['api', 'core', 'hobbies', '']),
  request('Gateway unknown route', 'GET', '/api/no-such-service/ping', {
    tests: script(
      "pm.test('HTTP 404', () => pm.response.to.have.status(404));",
      "pm.test('NOT_FOUND', () => pm.expect(pm.response.json().code).to.eql('NOT_FOUND'));",
    ),
  }),
  request('Core requires authentication', 'GET', '/api/core/profiles/me', {
    auth: { type: 'noauth' },
    tests: script(
      "pm.test('HTTP 401', () => pm.response.to.have.status(401));",
      "pm.test('AUTH_REQUIRED', () => pm.expect(pm.response.json().code).to.eql('AUTH_REQUIRED'));",
    ),
  }),
  request('Assist requires authentication', 'GET', '/api/assist/location/search', {
    auth: { type: 'noauth' },
    tests: script(
      "pm.test('HTTP 401', () => pm.response.to.have.status(401));",
      "pm.test('AUTH_REQUIRED', () => pm.expect(pm.response.json().code).to.eql('AUTH_REQUIRED'));",
    ),
  }),
];

const authenticated = [
  request('Login and keep token locally', 'POST', '/api/core/auth/login', {
    body: { email: '{{email}}', password: '{{password}}' },
    pre: pre(
      "pm.variables.set('token', '');",
      "if (!pm.variables.get('email') || !pm.variables.get('password')) throw new Error('Set non-empty email and password before running 02 Authenticated.');",
    ),
    tests: script(
      "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
      "const body = pm.response.json();",
      "pm.test('login succeeded', () => { pm.expect(body.status).to.eql('success'); pm.expect(body.data.token).to.be.a('string').and.not.empty; });",
      "if (pm.response.code === 200 && body.status === 'success' && typeof body.data?.token === 'string' && body.data.token) pm.variables.set('token', body.data.token);",
    ),
  }),
  ...[
    ['My profile', '/api/core/profiles/me'],
    ['My page', '/api/core/profiles/mypage'],
    ['My profile image info', '/api/core/profiles/me/image-info'],
    ['My hobbies', '/api/core/hobbies/user'],
    ['Hosted boards', '/api/core/boards/hosted'],
    ['Joined boards', '/api/core/boards/joined'],
    ['Chat rooms', '/api/core/chat/rooms'],
    ['Active chat rooms', '/api/core/chat/rooms/active'],
    ['Products registered to buy', '/api/core/market/products/users/registers/buy'],
    ['Products registered to sell', '/api/core/market/products/users/registers/sell'],
    ['Product requests to buy', '/api/core/market/products/users/requests/buy'],
    ['Product requests to sell', '/api/core/market/products/users/requests/sell'],
    ['My transactions', '/api/core/market/transactions/user'],
  ].map(([name, url]) => request(name, 'GET', url, { auth, pre: sessionGuard, tests: ok })),
  request('Assist reached without external API call', 'GET', '/api/assist/location/search', {
    auth, pre: sessionGuard,
    tests: script(
      "pm.test('missing query is HTTP 400', () => pm.response.to.have.status(400));",
      "pm.test('request passed authentication', () => pm.expect(['AUTH_REQUIRED', 'AUTH_INVALID', 'AUTH_EXPIRED']).to.not.include(pm.response.json().code));",
      "pm.test('BAD_REQUEST', () => pm.expect(pm.response.json().code).to.eql('BAD_REQUEST'));",
    ),
    description: 'The missing required query parameter is rejected by Assist before its controller can call Kakao.',
  }),
  request('Core unknown path after authentication', 'GET', '/api/core/no-such-endpoint', {
    auth, pre: sessionGuard,
    tests: script(
      "pm.test('HTTP 404', () => pm.response.to.have.status(404));",
      "pm.test('NOT_FOUND', () => pm.expect(pm.response.json().code).to.eql('NOT_FOUND'));",
    ),
  }),
];

const invalidAuthTests = script(
  "pm.test('gateway rejects invalid token with HTTP 401', () => pm.response.to.have.status(401));",
  "pm.test('gateway returns AUTH_INVALID', () => pm.expect(pm.response.json().code).to.eql('AUTH_INVALID'));",
);

const routeAuth = controllerRoutes().map((route, index) => request(
  `${String(index + 1).padStart(3, '0')} ${route.method} ${route.path}`,
  route.method,
  route.path,
  {
    auth: { type: 'noauth' },
    headers: [{ key: 'Authorization', value: 'Bearer invalid-test-token', type: 'text' }],
    ...(route.method === 'POST' ? { body: {} } : {}),
  },
));

const crudGuard = (required, clear = []) => pre(
  ...clear.map((name) => `pm.variables.set('${name}', '');`),
  "if (pm.variables.get('enableCrud') !== 'true') pm.execution.skipRequest();",
  "if (pm.variables.get('enableCrud') === 'true' && !pm.variables.get('token')) { pm.test('CRUD session exists', () => pm.expect.fail('CRUD login did not produce a token; request was not sent.')); pm.execution.skipRequest(); }",
  ...(required ? [`if (pm.variables.get('enableCrud') === 'true' && !/^\\d+$/.test(String(pm.variables.get('${required}') || ''))) throw new Error('Missing ${required}; an earlier CRUD request failed.');`] : []),
);
const idTests = (variable, field, expected) => script(
  "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
  "const body = pm.response.json(); const id = body.data?.id;",
  "pm.test('success response', () => pm.expect(body.status).to.eql('success'));",
  "pm.test('numeric ID returned', () => { pm.expect(id).to.be.a('number'); pm.expect(Number.isInteger(id) && id > 0).to.eql(true); });",
  `pm.test('returned content matches', () => pm.expect(body.data?.${field}).to.eql(${JSON.stringify(expected)}));`,
  `const valid = pm.response.code === 200 && body.status === 'success' && Number.isInteger(id) && id > 0 && body.data?.${field} === ${JSON.stringify(expected)};`,
  `pm.variables.set('${variable}', valid ? String(id) : '');`,
);
const contentTests = (expected) => script(
  "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
  "const body = pm.response.json();",
  "pm.test('success response', () => pm.expect(body.status).to.eql('success'));",
  `pm.test('returned content matches', () => pm.expect(body.data.${expected.field}).to.eql(${JSON.stringify(expected.value)}));`,
);
const deleteTests = (variable) => script(
  "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
  `if (pm.response.code === 200) pm.variables.set('${variable}', '');`,
);
const crudAuth = auth;
const boardCreate = { name: 'Postman CRUD board', description: 'Created only when enableCrud=true' };
const boardUpdate = { name: 'Postman CRUD board updated', description: 'Updated by Postman CRUD folder' };
const postCreate = { title: 'Postman CRUD post', content: 'Created by Postman CRUD folder', imageUrls: [] };
const postUpdate = { title: 'Postman CRUD post updated', content: 'Updated by Postman CRUD folder', imageUrls: [] };

const boardCrud = [
  request('Login for CRUD', 'POST', '/api/core/auth/login', {
    body: { email: '{{email}}', password: '{{password}}' },
    pre: pre(
      "pm.variables.set('token', ''); pm.variables.set('boardId', ''); pm.variables.set('postId', ''); pm.variables.set('commentId', '');",
      "if (pm.variables.get('enableCrud') !== 'true') pm.execution.skipRequest();",
      "if (pm.variables.get('enableCrud') === 'true' && (!pm.variables.get('email') || !pm.variables.get('password'))) throw new Error('Set non-empty email and password before running enabled 04 Board CRUD.');",
    ),
    tests: script(
      "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
      "const body = pm.response.json(); const token = body.data?.token;",
      "pm.test('login succeeded', () => { pm.expect(body.status).to.eql('success'); pm.expect(token).to.be.a('string').and.not.empty; });",
      "pm.variables.set('token', pm.response.code === 200 && body.status === 'success' && typeof token === 'string' && token ? token : '');",
    ),
  }),
  request('Create board', 'POST', '/api/core/boards', {
    auth: crudAuth, body: boardCreate,
    pre: crudGuard(null, ['boardId', 'postId', 'commentId']),
    tests: idTests('boardId', 'name', boardCreate.name),
  }),
  request('Read board', 'GET', '/api/core/boards/{{boardId}}', { auth: crudAuth, pre: crudGuard('boardId'), tests: contentTests({ field: 'name', value: boardCreate.name }) }),
  request('Update board', 'PUT', '/api/core/boards/{{boardId}}', { auth: crudAuth, body: boardUpdate, pre: crudGuard('boardId'), tests: contentTests({ field: 'name', value: boardUpdate.name }) }),
  request('Read updated board', 'GET', '/api/core/boards/{{boardId}}', { auth: crudAuth, pre: crudGuard('boardId'), tests: contentTests({ field: 'name', value: boardUpdate.name }) }),
  request('Create post', 'POST', '/api/core/boards/{{boardId}}/posts', {
    auth: crudAuth, body: postCreate, pre: crudGuard('boardId', ['postId', 'commentId']),
    tests: idTests('postId', 'title', postCreate.title),
  }),
  request('Read post', 'GET', '/api/core/boards/{{boardId}}/posts/{{postId}}', { auth: crudAuth, pre: crudGuard('postId'), tests: contentTests({ field: 'title', value: postCreate.title }) }),
  request('Update post', 'PUT', '/api/core/boards/{{boardId}}/posts/{{postId}}', { auth: crudAuth, body: postUpdate, pre: crudGuard('postId'), tests: contentTests({ field: 'title', value: postUpdate.title }) }),
  request('Read updated post', 'GET', '/api/core/boards/{{boardId}}/posts/{{postId}}', { auth: crudAuth, pre: crudGuard('postId'), tests: contentTests({ field: 'title', value: postUpdate.title }) }),
  request('Create comment', 'POST', '/api/core/boards/posts/{{postId}}/comments', {
    auth: crudAuth, body: { content: 'Postman CRUD comment' }, pre: crudGuard('postId', ['commentId']),
    tests: idTests('commentId', 'content', 'Postman CRUD comment'),
  }),
  request('Read comments', 'GET', '/api/core/boards/posts/{{postId}}/comments', {
    auth: crudAuth, pre: crudGuard('commentId'),
    tests: script(
      "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
      "pm.test('created comment is returned', () => { const comments = pm.response.json().data; pm.expect(comments.some((item) => String(item.id) === pm.variables.get('commentId') && item.content === 'Postman CRUD comment')).to.eql(true); });",
    ),
  }),
  request('Update comment', 'PUT', '/api/core/boards/posts/{{postId}}/comments/{{commentId}}', {
    auth: crudAuth, body: { content: 'Postman CRUD comment updated' }, pre: crudGuard('commentId'),
    tests: contentTests({ field: 'content', value: 'Postman CRUD comment updated' }),
  }),
  request('Read updated comment', 'GET', '/api/core/boards/posts/{{postId}}/comments', {
    auth: crudAuth, pre: crudGuard('commentId'),
    tests: script(
      "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
      "pm.test('updated comment persisted', () => { const comments = pm.response.json().data; pm.expect(comments.some((item) => String(item.id) === pm.variables.get('commentId') && item.content === 'Postman CRUD comment updated')).to.eql(true); });",
    ),
  }),
  request('Delete comment', 'DELETE', '/api/core/boards/posts/{{postId}}/comments/{{commentId}}', { auth: crudAuth, pre: crudGuard('commentId'), tests: deleteTests('commentId') }),
  request('Delete post', 'DELETE', '/api/core/boards/{{boardId}}/posts/{{postId}}', { auth: crudAuth, pre: crudGuard('postId'), tests: deleteTests('postId') }),
  request('Delete board', 'DELETE', '/api/core/boards/{{boardId}}', { auth: crudAuth, pre: crudGuard('boardId'), tests: deleteTests('boardId') }),
];

export function generateCollection() {
  return {
    info: {
      name: 'Haru Backend API Checks',
      description: 'Startup, authenticated read-only checks, gateway route authentication coverage, and explicitly enabled isolated Board CRUD. Route auth proves only gateway rejection, not downstream behavior.',
      schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
    },
    auth: { type: 'noauth' },
    variable: [
      { key: 'baseUrl', value: 'http://127.0.0.1:8080', type: 'string' },
      { key: 'email', value: '', type: 'string' },
      { key: 'password', value: '', type: 'string' },
      { key: 'enableCrud', value: 'false', type: 'string' },
    ],
    item: [
      { name: '01 Startup', item: startup },
      { name: '02 Authenticated', item: authenticated },
      { name: '03 Route auth', description: 'Every current Core and Assist REST mapping with a concrete path and an invalid token. These are gateway checks only.', event: invalidAuthTests, item: routeAuth },
      { name: '04 Board CRUD', description: 'Skipped unless enableCrud is exactly true. Creates and removes its own Board, Post, and Comment; concurrent runs are not isolated from broad cleanup or shared-account side effects.', item: boardCrud },
    ],
  };
}

const generated = `${JSON.stringify(generateCollection())}\n`;
if (process.argv.includes('--check')) {
  if (!fs.existsSync(output) || fs.readFileSync(output, 'utf8') !== generated) {
    console.error('haru.postman_collection.json is stale; run node api-tests/generate.mjs');
    process.exitCode = 1;
  }
} else if (path.resolve(process.argv[1] ?? '') === fileURLToPath(import.meta.url)) {
  fs.mkdirSync(here, { recursive: true });
  fs.writeFileSync(output, generated);
  console.log(`generated ${path.relative(root, output)} with ${routeAuth.length} route-auth requests`);
}
