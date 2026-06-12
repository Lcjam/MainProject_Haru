import { format, parseISO } from 'date-fns';
import { ko } from 'date-fns/locale';

/**
 * UTC 시간을 KST로 변환하여 포맷팅
 * @param dateString ISO 형식의 날짜 문자열
 * @param formatString 포맷 문자열 (기본값: 'yyyy-MM-dd HH:mm:ss')
 * @returns 포맷팅된 KST 시간 문자열
 */
export const formatToKST = (dateString: string | number[], formatString: string = 'yyyy-MM-dd HH:mm:ss'): string => {
  try {
    // 배열인 경우 Date 객체로 변환
    const date = Array.isArray(dateString) 
      ? new Date(dateString[0], dateString[1] - 1, dateString[2], dateString[3], dateString[4], dateString[5])
      : parseISO(dateString);

    // KST로 변환 (UTC+9)
    const kstDate = new Date(date.getTime() + (9 * 60 * 60 * 1000));

    return format(kstDate, formatString, { locale: ko });
  } catch (error) {
    console.error('날짜 변환 오류:', error);
    return '날짜 변환 실패';
  }
};

/**
 * 메시지 시간을 상대적 시간으로 변환 (예: '방금 전', '5분 전')
 * @param dateString ISO 형식의 날짜 문자열
 * @returns 상대적 시간 문자열
 */
export const formatRelativeTime = (dateString: string | number[]): string => {
  try {
    const date = Array.isArray(dateString)
      ? new Date(dateString[0], dateString[1] - 1, dateString[2], dateString[3], dateString[4], dateString[5])
      : parseISO(dateString);

    const kstDate = new Date(date.getTime() + (9 * 60 * 60 * 1000));
    const now = new Date();
    const diff = now.getTime() - kstDate.getTime();

    // 1분 미만
    if (diff < 60000) return '방금 전';
    
    // 1시간 미만
    if (diff < 3600000) {
      const minutes = Math.floor(diff / 60000);
      return `${minutes}분 전`;
    }
    
    // 24시간 미만
    if (diff < 86400000) {
      const hours = Math.floor(diff / 3600000);
      return `${hours}시간 전`;
    }
    
    // 7일 미만
    if (diff < 604800000) {
      const days = Math.floor(diff / 86400000);
      return `${days}일 전`;
    }
    
    // 그 이상은 날짜 표시
    return format(kstDate, 'yyyy-MM-dd HH:mm', { locale: ko });
  } catch (error) {
    console.error('상대적 시간 변환 오류:', error);
    return '시간 변환 실패';
  }
}; 