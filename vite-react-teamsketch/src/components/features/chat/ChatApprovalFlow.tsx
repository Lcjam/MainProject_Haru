import React from 'react';
import { IconMap } from '../../common/Icons';
import ProductImage from '../image/ProductImage';

interface ChatApprovalFlowProps {
  productImageUrl?: string;
  otherUserName?: string;
  mainCategory: string;
  subCategory: string;
  /** 로딩 중에는 헤더만 노출하고 액션 버튼은 숨긴다 */
  showActions?: boolean;
  isOwner?: boolean;
  isApproved?: boolean;
  isDisabled?: boolean;
  transactionType?: string;
  onJoinClick?: () => void;
  onLocationClick?: () => void;
}

/**
 * 채팅방 상단 헤더 + 함께하기/위치보기 승인 플로우.
 * 데이터/상태는 ChatRoom이 소유하고, 여기서는 표시와 클릭 위임만 담당한다.
 */
const ChatApprovalFlow: React.FC<ChatApprovalFlowProps> = ({
  productImageUrl,
  otherUserName,
  mainCategory,
  subCategory,
  showActions = true,
  isOwner = false,
  isApproved = false,
  isDisabled = false,
  transactionType = '',
  onJoinClick,
  onLocationClick
}) => {
  return (
    <div
      className="bg-gradient-to-r from-primary-500 to-primary-600 dark:from-gray-800 dark:to-gray-700
        p-3 flex items-center justify-between shadow-md z-10 flex-shrink-0"
    >
      <div className="flex items-center space-x-3">
        <div className="w-10 h-10 rounded-full overflow-hidden ring-2 ring-white/30 shrink-0">
          <ProductImage imagePath={productImageUrl || ''} />
        </div>
        <div className="flex flex-col">
          <span className="text-base font-medium text-white">
            {otherUserName || '상대방'}
          </span>
          <span className="text-sm text-white/70">
            {mainCategory}
            {subCategory}
          </span>
        </div>
      </div>

      {/* 함께하기 버튼 또는 위치보기 버튼 */}
      {showActions && (
        isOwner && !isApproved ? (
          <button
            onClick={onJoinClick}
            disabled={isDisabled}
            className="
              bg-white text-primary-600 dark:bg-gray-700 dark:text-white
              px-4 py-2 rounded-full shadow-md hover:shadow-lg
              transition-all duration-300 hover:bg-primary-100 dark:hover:bg-gray-600
              disabled:opacity-50 disabled:cursor-not-allowed text-sm font-medium
            "
          >
            함께하기
          </button>
        ) : isApproved && transactionType === '대면' && (
          <button
            onClick={onLocationClick}
            className="
              bg-white text-primary-600 dark:bg-gray-700 dark:text-white
              px-4 py-2 rounded-full shadow-md hover:shadow-lg
              transition-all duration-300 hover:bg-primary-100 dark:hover:bg-gray-600
              text-sm font-medium
            "
          >
            <span className="flex items-center">
              <IconMap className="w-4 h-4 mr-2" onClick={onLocationClick} />
              위치보기
            </span>
          </button>
        )
      )}
    </div>
  );
};

export default ChatApprovalFlow;
