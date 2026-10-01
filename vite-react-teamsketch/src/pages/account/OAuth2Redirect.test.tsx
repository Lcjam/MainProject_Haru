import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { Provider } from 'react-redux';
import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import OAuth2Redirect from './OAuth2Redirect';
import { store } from '../../store/store';
import { logout } from '../../store/slices/authSlice';

const api = vi.hoisted(() => ({ exchangeOAuth2Code: vi.fn(), getUserInfoApi: vi.fn() }));

vi.mock('../../services/api/authAPI', () => api);

describe('OAuth2Redirect', () => {
  beforeEach(() => {
    api.exchangeOAuth2Code.mockReset();
    api.getUserInfoApi.mockReset();
    store.dispatch(logout());
  });

  it('exchanges the URL code once and saves the normal login state without reading a token from the URL', async () => {
    api.exchangeOAuth2Code.mockResolvedValue({
      status: 'success',
      data: { token: 'jwt-from-exchange', email: 'user@example.com', nickname: 'Haru' }
    });
    api.getUserInfoApi.mockResolvedValue({
      data: { id: 7, email: 'user@example.com', nickname: 'Haru', hobbies: [] }
    });

    render(
      <Provider store={store}>
        <MemoryRouter initialEntries={['/oauth2/redirect?code=short-lived-code']}>
          <Routes>
            <Route path="/oauth2/redirect" element={<OAuth2Redirect />} />
            <Route path="/my-location" element={<div>location</div>} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    await waitFor(() => expect(api.exchangeOAuth2Code).toHaveBeenCalledWith('short-lived-code'));
    expect(api.getUserInfoApi).toHaveBeenCalledOnce();
    expect(await screen.findByText('location')).toBeInTheDocument();
    expect(localStorage.getItem('token')).toBe('jwt-from-exchange');
  });

  it('shows a failure when the callback has no exchange code', () => {
    render(
      <Provider store={store}>
        <MemoryRouter initialEntries={['/oauth2/redirect?error=authentication_failed']}>
          <Routes><Route path="/oauth2/redirect" element={<OAuth2Redirect />} /></Routes>
        </MemoryRouter>
      </Provider>
    );

    expect(screen.getByRole('status')).toHaveTextContent('소셜 로그인에 실패했습니다. 다시 시도해주세요.');
    expect(api.exchangeOAuth2Code).not.toHaveBeenCalled();
  });
});
