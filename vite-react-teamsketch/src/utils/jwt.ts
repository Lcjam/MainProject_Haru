export const decodeJWT = (token: string) => {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const payload = JSON.parse(atob(base64));
    return payload;
  } catch (error) {
    console.error('JWT 디코딩 실패:', error);
    return null;
  }
};
