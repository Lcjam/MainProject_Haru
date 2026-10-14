import { render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AuthenticatedChatImage from './AuthenticatedChatImage';

const api = vi.hoisted(() => ({ get: vi.fn() }));
vi.mock('../../../services/api/axiosInstance', () => ({ axiosInstance: api }));
vi.mock('../../../services/api/apiConfig', () => ({ apiConfig: { baseURL: 'http://localhost:8080/api' } }));
const NativeURL = globalThis.URL;
const id = '123e4567-e89b-12d3-a456-426614174000';
const createObjectURL = vi.fn();
const revokeObjectURL = vi.fn();

describe('AuthenticatedChatImage', () => {
  beforeEach(() => {
    api.get.mockReset().mockResolvedValue({ data: new Blob(['image']) });
    createObjectURL.mockReset().mockReturnValue('blob:chat-image');
    revokeObjectURL.mockReset();
    vi.stubGlobal('URL', class extends NativeURL {
      static createObjectURL = createObjectURL;
      static revokeObjectURL = revokeObjectURL;
    });
  });
  afterEach(() => vi.unstubAllGlobals());

  it.each([
    [`/api/core/chat/images/20/${id}.png`, `${id}.png`],
    [`/chat-images/chatroom_20/${id}.png`, `${id}.png`],
    [`/chat-images/chatroom_20/${id}.PNG`, `${id}.PNG`]
  ])('loads %s through the authenticated API without using the static URL', async (path, file) => {
    const view = render(<AuthenticatedChatImage path={path} />);
    expect(await screen.findByAltText('채팅 이미지')).toHaveAttribute('src', 'blob:chat-image');
    expect(api.get).toHaveBeenCalledWith(`http://localhost:8080/api/core/chat/images/20/${file}`,
      expect.objectContaining({ responseType: 'blob', signal: expect.any(AbortSignal) }));
    view.unmount();
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:chat-image');
  });

  it.each([
    `https://evil.example/chat-images/chatroom_20/${id}.png`,
    `/chat-images/chatroom_20/../${id}.png`,
    `/chat-images/chatroom_20/${id}.svg`,
    `/chat-images/chatroom_20/${id}.png\n`,
    '/chat-images/chatroom_20/random.png'
  ])('does not send credentials for invalid image path %s', (path) => {
    render(<AuthenticatedChatImage path={path} />);
    expect(api.get).not.toHaveBeenCalled();
  });

  it('clears the old image when the next message has an invalid path', async () => {
    const view = render(<AuthenticatedChatImage path={`/chat-images/chatroom_20/${id}.png`} />);
    await screen.findByAltText('채팅 이미지');
    view.rerender(<AuthenticatedChatImage path="/untrusted/image.png" />);
    await waitFor(() => expect(screen.queryByAltText('채팅 이미지')).not.toBeInTheDocument());
    expect(api.get).toHaveBeenCalledOnce();
  });
});
