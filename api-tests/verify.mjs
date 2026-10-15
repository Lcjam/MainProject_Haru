import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';
import { controllerRoutes, generateCollection } from './generate.mjs';

const here = path.dirname(fileURLToPath(import.meta.url));
const saved = JSON.parse(fs.readFileSync(path.join(here, 'haru.postman_collection.json'), 'utf8'));
assert.deepEqual(saved, generateCollection(), 'generated collection is stale');
assert.equal(saved.info.schema, 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json');
assert.deepEqual(saved.item.map((folder) => folder.name), ['01 Startup', '02 Authenticated', '03 Route auth', '04 Board CRUD']);

const routeFolder = saved.item[2].item;
const routes = controllerRoutes();
assert.equal(routeFolder.length, routes.length, 'every current controller route must have one auth request');
assert.equal(new Set(routes.map(({ method, path: routePath }) => `${method} ${routePath}`)).size, routes.length, 'concrete REST routes must be unique');
for (const [index, item] of routeFolder.entries()) {
  assert.equal(item.request.url.raw, `{{baseUrl}}${routes[index].path}`);
  assert.ok(!item.request.url.raw.slice('{{baseUrl}}'.length).includes('{'), `unresolved path: ${item.name}`);
  assert.deepEqual(item.request.auth, { type: 'noauth' });
  assert.equal(item.request.header.find((header) => header.key === 'Authorization')?.value, 'Bearer invalid-test-token');
  assert.equal(item.request.method === 'POST' ? item.request.body?.raw : undefined, item.request.method === 'POST' ? '{}' : undefined);
  assert.equal(item.event, undefined, 'route requests inherit their assertions from the folder');
}

const variables = Object.fromEntries(saved.variable.map(({ key, value }) => [key, value]));
assert.deepEqual(variables, { baseUrl: 'http://127.0.0.1:8080', email: '', password: '', enableCrud: 'false' });
assert.ok(!JSON.stringify(saved.variable).includes('token'), 'token must remain a local pm.variables value');

const events = (item, listen) => item.event.find((event) => event.listen === listen).script.exec.join('\n');
const authenticated = saved.item[1].item;
const login = authenticated[0];
const local = new Map([['token', 'stale-token'], ['email', ''], ['password', '']]);
assert.throws(() => vm.runInNewContext(events(login, 'prerequest'), {
  pm: { variables: { get: (key) => local.get(key), set: (key, value) => local.set(key, value) } },
}), /Set non-empty email and password/);
assert.equal(local.get('token'), '', 'blank credentials must shadow any old token before failing');

const crud = saved.item[3].item;
const crudLocal = new Map([['enableCrud', 'true'], ['email', 'postman@example.com'], ['password', 'secret'], ['token', 'stale-token'], ['boardId', '91'], ['postId', '92'], ['commentId', '93']]);
vm.runInNewContext(events(crud[0], 'prerequest'), {
  pm: { variables: { get: (key) => crudLocal.get(key), set: (key, value) => crudLocal.set(key, value) }, execution: { skipRequest: () => assert.fail('enabled CRUD skipped') } },
});
assert.equal(crudLocal.get('token') || crudLocal.get('boardId') || crudLocal.get('postId') || crudLocal.get('commentId'), '', 'CRUD start must shadow stale session values');
for (const item of crud) assert.ok(events(item, 'prerequest').includes("enableCrud') !== 'true'"), `${item.name} lacks opt-in guard`);
const createBoardTest = events(crud.find((item) => item.name === 'Create board'), 'test');
for (const condition of ["pm.response.code === 200", "body.status === 'success'", 'Number.isInteger(id)', "body.data?.name === \"Postman CRUD board\""]) {
  assert.ok(createBoardTest.includes(condition), `create ID storage lacks condition: ${condition}`);
}
assert.ok(createBoardTest.includes("pm.variables.set('boardId', valid ? String(id) : '')"), 'failed create must shadow an old board ID');

function failuresFor(item, code, body) {
  const failures = [];
  const expect = (actual) => ({
    to: {
      eql(expected) { if (actual !== expected) throw new Error(`${actual} !== ${expected}`); },
    },
  });
  vm.runInNewContext(events(item, 'test'), {
    pm: {
      response: { code, json: () => body, to: { have: { status(expected) { if (code !== expected) throw new Error(`${code} !== ${expected}`); } } } },
      expect,
      test(name, fn) { try { fn(); } catch (error) { failures.push(`${name}: ${error.message}`); } },
    },
  });
  return failures;
}

const routeAuthFolder = saved.item[2];
assert.equal(failuresFor(routeAuthFolder, 401, { code: 'AUTH_INVALID' }).length, 0);
assert.ok(failuresFor(routeAuthFolder, 200, { code: 'AUTH_INVALID' }).length > 0, 'wrong status must fail');
assert.ok(failuresFor(routeAuthFolder, 401, { code: 'AUTH_REQUIRED' }).length > 0, 'wrong auth code must fail');
assert.ok(failuresFor(saved.item[0].item[0], 500, { status: 'error' }).length > 0, 'startup error response must fail');

console.log(`verified collection: ${routes.length} REST routes, local token/ID isolation, and negative response assertions`);
