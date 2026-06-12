interface IconProps {
  className?: string;
  onClick?: () => void;
  hasNotification?: boolean;
  notificationCount?: number;
}

export const IconHome = ({ className = '' }: IconProps) => (
  <svg className={className} fill="currentColor" viewBox="0 0 24 24">
    <path d="M12 5.69l5 4.5V18h-2v-6H9v6H7v-7.81l5-4.5M12 3L2 12h3v8h6v-6h2v6h6v-8h3L12 3z" />
  </svg>
);

export const IconChat = ({ className = '' }: IconProps) => (
  <svg className={className} fill="currentColor" viewBox="0 0 24 24">
    <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z" />
  </svg>
);

export const IconList = ({ className = '' }: IconProps) => (
  <svg className={className} fill="currentColor" viewBox="0 0 24 24">
    <path d="M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z" />
  </svg>
);

export const IconMap = ({ className = '' }: IconProps) => (
  <svg className={className} fill="currentColor" viewBox="0 0 24 24">
    <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z" />
  </svg>
);

export const IconUser = ({ className = '' }: IconProps) => (
  <svg className={className} fill="currentColor" viewBox="0 0 24 24">
    <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
  </svg>
);

export const IconSetting = ({ className = '', onClick }: IconProps) => (
  <svg className={className} fill="currentColor" viewBox="0 0 24 24" onClick={onClick}>
    <path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.07.62-.07.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z" />
  </svg>
);

export const IconLogout = ({ className = '', onClick }: IconProps) => (
  <svg className={className} fill="currentColor" viewBox="0 0 24 24" onClick={onClick}>
    <path d="M10 21v-8H3V9h7V2h9v6h7v4h-7v8z" />
  </svg>
);

export const Iconalarm = ({ className = '', onClick, hasNotification = false }: IconProps) => (
  <div className="relative">
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="30"
      height="30"
      viewBox="0 0 24 24"
      fill="none"
      className={`transition-colors duration-300 ${className}`}
      onClick={() => {
        if (onClick) {
          onClick();
        } else {
          alert('알람이 없습니다.');
        }
      }}
    >
      <path
        d="M4 19V17H6V10C6 8.61667 6.41667 7.39167 7.25 6.325C8.08333 5.24167 9.16667 4.53333 10.5 4.2V3.5C10.5 3.08333 10.6417 2.73333 10.925 2.45C11.225 2.15 11.5833 2 12 2C12.4167 2 12.7667 2.15 13.05 2.45C13.35 2.73333 13.5 3.08333 13.5 3.5V4.2C14.8333 4.53333 15.9167 5.24167 16.75 6.325C17.5833 7.39167 18 8.61667 18 10V17H20V19H4ZM12 22C11.45 22 10.975 21.8083 10.575 21.425C10.1917 21.025 10 20.55 10 20H14C14 20.55 13.8 21.025 13.4 21.425C13.0167 21.8083 12.55 22 12 22ZM8 17H16V10C16 8.9 15.6083 7.95833 14.825 7.175C14.0417 6.39167 13.1 6 12 6C10.9 6 9.95833 6.39167 9.175 7.175C8.39167 7.95833 8 8.9 8 10V17Z"
        className={`${
          hasNotification ? 'fill-primary-50' : 'fill-white hover:fill-primary-100'
        }`}
      />
    </svg>
    {hasNotification && (
      <div className="absolute -top-1 -right-1 w-3 h-3 bg-red-500 rounded-full" />
    )}
  </div>
);

export const PlusIcon = ({ className = '' }: IconProps) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width="16"
    height="16"
    viewBox="0 0 16 16"
    fill="none"
    className={className}
  >
    <mask
      id="mask0_254_5851"
      style={{ maskType: 'alpha' }}
      maskUnits="userSpaceOnUse"
      x="0"
      y="0"
      width="16"
      height="16"
    >
      <path
        d="M9.00016 1.33333C9.00016 0.781043 8.55245 0.333328 8.00016 0.333328C7.44788 0.333328 7.00016 0.781043 7.00016 1.33333V6.99999H1.3335C0.781212 6.99999 0.333496 7.44771 0.333496 7.99999C0.333496 8.55228 0.781212 8.99999 1.3335 8.99999H7.00016V14.6667C7.00016 15.2189 7.44788 15.6667 8.00016 15.6667C8.55245 15.6667 9.00016 15.2189 9.00016 14.6667V9H14.6668C15.2191 9 15.6668 8.55228 15.6668 8C15.6668 7.44771 15.2191 7 14.6668 7H9.00016V1.33333Z"
        fill="#006FFD"
      />
    </mask>
    <g mask="url(#mask0_254_5851)">
      <rect width="16" height="16" fill="#6003FF" />
    </g>
  </svg>
);
