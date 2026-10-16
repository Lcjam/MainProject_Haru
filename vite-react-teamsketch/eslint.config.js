import js from '@eslint/js';
import globals from 'globals';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import tseslint from 'typescript-eslint';

export default tseslint.config(
  { ignores: ['dist', 'coverage'] },
  {
    linterOptions: {
      reportUnusedDisableDirectives: 'error',
    },
  },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': ['error', { allowConstantExport: true }],
      'no-console': ['error', { allow: ['warn', 'error'] }],
      '@typescript-eslint/no-explicit-any': 'error',
      'react-hooks/exhaustive-deps': 'error',
    },
  },
  {
    files: ['src/**/*.{test,spec}.{ts,tsx}', 'src/test/**/*.{ts,tsx}'],
    rules: {
      'no-restricted-properties': [
        'error',
        { object: 'it', property: 'only', message: 'Do not commit focused tests (it.only).' },
        { object: 'test', property: 'only', message: 'Do not commit focused tests (test.only).' },
        { object: 'describe', property: 'only', message: 'Do not commit focused tests (describe.only).' },
        { object: 'context', property: 'only', message: 'Do not commit focused tests (context.only).' },
        { object: 'it', property: 'skip', message: 'Do not commit skipped tests (it.skip).' },
        { object: 'test', property: 'skip', message: 'Do not commit skipped tests (test.skip).' },
        { object: 'describe', property: 'skip', message: 'Do not commit skipped tests (describe.skip).' },
        { object: 'context', property: 'skip', message: 'Do not commit skipped tests (context.skip).' },
      ],
    },
  }
);
