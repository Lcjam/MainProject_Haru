import { useEffect, useState } from 'react';
import { axiosInstance } from '../../../services/api/axiosInstance';
import { apiConfig } from '../../../services/api/apiConfig';

export default function AuthenticatedChatImage({ path, className }: { path: string; className?: string }) {
  const [source, setSource] = useState<string>();

  useEffect(() => {
    const match = /^(?:\/api\/core\/chat\/images\/|\/chat-images\/chatroom_)([1-9]\d*)\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.(?:jpg|jpeg|png|gif|bmp|webp))$/i.exec(path);
    if (!match || match[0] !== path) return;
    const imagePath = `/api/core/chat/images/${match[1]}/${match[2]}`;
    const controller = new AbortController();
    let objectUrl: string | undefined;
    const url = new URL(imagePath, apiConfig.baseURL).toString();
    axiosInstance.get<Blob>(url, { responseType: 'blob', signal: controller.signal })
      .then(({ data }) => {
        if (controller.signal.aborted) return;
        objectUrl = URL.createObjectURL(data);
        setSource(objectUrl);
      })
      .catch(() => setSource(undefined));
    return () => {
      controller.abort();
      if (objectUrl) URL.revokeObjectURL(objectUrl);
      setSource(undefined);
    };
  }, [path]);

  return source ? <img src={source} alt="채팅 이미지" className={className} /> : null;
}
