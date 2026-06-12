// const defaultTheme = require('tailwindcss/defaultTheme');

/** @type {import('tailwindcss').Config} */
export default {
  content: ['./src/**/*.{js,jsx,ts,tsx}'],
  index: ['/index.html'],
  darkMode: 'class',
  theme: {
    // 컨테이너 설정
    container: {
      center: true,
      padding: {
        DEFAULT: '1rem',
        sm: '2rem',
        lg: '4rem',
        xl: '5rem',
        '2xl': '6rem'
      }
    },
    // 타이포그래피 시스템
    fontSize: {
      xs: ['0.75rem', { lineHeight: '1rem' }],
      sm: ['0.875rem', { lineHeight: '1.25rem' }],
      base: ['1rem', { lineHeight: '1.5rem' }],
      lg: ['1.125rem', { lineHeight: '1.75rem' }],
      xl: ['1.25rem', { lineHeight: '1.75rem' }],
      '2xl': ['1.5rem', { lineHeight: '2rem' }],
      '3xl': ['1.875rem', { lineHeight: '2.25rem' }],
      '4xl': ['2.25rem', { lineHeight: '2.5rem' }],
      '5xl': ['3rem', { lineHeight: '1' }]
    },
    extend: {
      // 색상 시스템
      colors: {
        primary: {
          50: '#F8F5FF',
          100: '#E6CCFF',
          200: '#D1A3FF',
          300: '#B980FF',
          400: '#A14EFF',
          500: '#8A2BE2',
          600: '#7A00CC',
          700: '#5700AA',
          800: '#430086',
          900: '#2E0066',
          950: '#190043'
          // #F8F5FF',
          // #E6CCFF',
          // #D1A3FF',
          // #B980FF',
          // #A14EFF',
          // #8A2BE2',
          // #7A00CC',
          // #5700AA',
          // #430086',
          // #2E0066',
          // #190043'
        },
        secondary: {
          50: '#FFF5F8',
          100: '#FFD6E6',
          200: '#FFB3D1',
          300: '#FF8ACF',
          400: '#FF66B2',
          500: '#FF3399',
          600: '#FF007F',
          700: '#CC0066',
          800: '#99004D',
          900: '#660033',
          950: '#330019'
        },
        accent: {
          50: '#F5FFF8',
          100: '#D6F5E6',
          200: '#A3EBCC',
          300: '#75E1B2',
          400: '#4AD799',
          500: '#00CC66',
          600: '#00A653',
          700: '#008040',
          800: '#006633',
          900: '#00331A',
          950: '#001A0D'
        },
        background: {
          light: '#FFFFFF',
          dark: '#1A1A1A'
        },
        surface: {
          light: '#F8F9FA',
          dark: '#2D2D2D'
        },
        text: {
          primary: '#1A1A1A',
          secondary: '#4A4A4A',
          tertiary: '#717171',
          light: '#FFFFFF',
          dark: '#1A1A1A'
        },
        border: {
          light: '#F9B0BA', // 연한 보라
          dark: '#FBCCC5' // 연한 핑크
          // #ECCEF5 (연한 보라 - 연한 보라)
          // #F6CED8 (연한 핑크 - 연핑크 느낌)
        },
        purple: {
          DEFAULT: '#800080', //
          level_1: '#E6CCFF', //
          level_2: '#D1A3FF',
          level_3: '#B980FF',
          level_4: '#A14EFF',
          level_5: '#8A2BE2',
          level_6: '#7A00CC',
          level_7: '#5700AA',
          level_8: '#430086',
          level_9: '#2E0066',
          level_10: '#190043'
          // #800080 (보라색 - 보라색)
          // #E6CCFF (아주 연한 보라 - 라벤더 톤)
          // #D1A3FF (밝은 연보라)
          // #B980FF (연한 보라)
          // #A14EFF (선명한 보라)
          // #8A2BE2 (보라색 - 블루바이올렛)
          // #7A00CC (살짝 어두운 보라)
          // #6600B2 (진한 보라)
          // #4D0080 (깊은 보라)
          // #330066 (어두운 보라 - 거의 검정 느낌)
          // #1A0033 (극한의 어두운 보라)
        },
        pink: {
          DEFAULT: '#FFC0CB', //
          level_1: '#FFD6E6', //
          level_2: '#FFB3D1',
          level_3: '#FF8ACF',
          level_4: '#FF66B2',
          level_5: '#FF3399',
          level_6: '#FF007F',
          level_7: '#CC0066',
          level_8: '#99004D',
          level_9: '#660033',
          level_10: '#330019'
          // #FFC0CB (아주 연한 핑크 - 연핑크 느낌)
          // #FFD6E6 (아주 연한 핑크 - 파스텔톤)
          // #FFB3D1 (밝은 핑크)
          // #FF8ACF (연한 핫핑크)
          // #FF66B2 (선명한 핑크)
          // #FF3399 (핫핑크)
          // #FF007F (진한 핑크)
          // #CC0066 (더 진한 핑크)
          // #99004D (어두운 핑크 - 레드톤 강함)
          // #660033 (매우 어두운 핑크)
          // #330019 (거의 검정에 가까운 핑크)
        },
        purple_color: {
          level_1: '#D6F5E6', //
          level_2: '#D1EBA3', //
          level_3: '#BADD75',
          level_4: '#A3D14A', //
          level_5: '#75D41D',
          level_6: '#5EA815',
          level_7: '#49800F',
          level_8: '#336608',
          level_9: '#1A3304',
          level_10: '#0D1902'
          // #D6F5E6 (아주 연한 연두)
          // #D1EBA3 (밝은 연두)
          // #BADD75 (연한 연두)
          // #A3D14A (선명한 라임그린)
          // #75D41D (기본 라임그린)
          // #5EA815 (살짝 어두운 라임)
          // #49800F (더 진한 연두)
          // #336608 (짙은 녹색)
          // #1A3304 (매우 어두운 녹색)
          // #0D1902 (거의 검정에 가까운 녹색)
        },
        pink_color: {
          level_1: '#D6F5E6', //
          level_2: '#A3EBCC', //
          level_3: '#75E1B2',
          level_4: '#4AD799',
          level_5: '#00CC66',
          level_6: '#00A653',
          level_7: '#008040',
          level_8: '#006633',
          level_9: '#00331A',
          level_10: '#001A0D'
          // #D6F5E6 (아주 연한 민트)
          // #A3EBCC (밝은 민트)
          // #75E1B2 (연한 청록색)
          // #4AD799 (선명한 청록)
          // #00CC66 (기본 청록색)
          // #00A653 (살짝 어두운 청록)
          // #008040 (더 진한 청록)
          // #006633 (짙은 녹색)
          // #00331A (매우 어두운 녹색)
          // #001A0D (거의 검정에 가까운 녹색)
        }
      },
      // 간격 시스템
      spacing: {
        xs: '0.25rem',
        sm: '0.5rem',
        md: '1rem',
        lg: '1.5rem',
        xl: '2rem',
        '2xl': '2.5rem',
        '3xl': '3rem'
      },
      borderRadius: {
        none: '0',
        sm: '0.25rem',
        DEFAULT: '0.375rem',
        md: '0.5rem',
        lg: '0.75rem',
        xl: '1rem',
        '2xl': '1.5rem',
        full: '9999px'
      },
      boxShadow: {
        sm: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
        DEFAULT: '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1)',
        md: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1)',
        lg: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1)',
        xl: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1)',
        inner: 'inset 0 2px 4px 0 rgba(0, 0, 0, 0.05)',
        colored: '0 4px 14px 0 rgba(138, 43, 226, 0.2)'
      },
      animation: {
        'fade-in': 'fadeIn 0.3s ease-in-out',
        'slide-up': 'slideUp 0.3s ease-out',
        'slide-down': 'slideDown 0.3s ease-out',
        'scale-in': 'scaleIn 0.2s ease-out'
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' }
        },
        slideUp: {
          '0%': { transform: 'translateY(10px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' }
        },
        slideDown: {
          '0%': { transform: 'translateY(-10px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' }
        },
        scaleIn: {
          '0%': { transform: 'scale(0.95)', opacity: '0' },
          '100%': { transform: 'scale(1)', opacity: '1' }
        }
      },
      // 레이아웃 시스템
      layout: {
        maxWidth: {
          sm: '640px',
          md: '768px',
          lg: '1024px',
          xl: '1280px'
        },
        gap: {
          xs: '0.5rem',
          sm: '1rem',
          md: '1.5rem',
          lg: '2rem'
        }
      },
      gridTemplateColumns: {
        1: 'repeat(1, minmax(0, 1fr))',
        2: 'repeat(2, minmax(0, 1fr))',
        3: 'repeat(3, minmax(0, 1fr))',
        4: 'repeat(4, minmax(0, 1fr))',
        5: 'repeat(5, minmax(0, 1fr))',
        6: 'repeat(6, minmax(0, 1fr))',
        12: 'repeat(12, minmax(0, 1fr))'
      },
      gridColumn: {
        'span-1': 'span 1 / span 1',
        'span-2': 'span 2 / span 2',
        'span-3': 'span 3 / span 3',
        'span-4': 'span 4 / span 4',
        'span-5': 'span 5 / span 5',
        'span-6': 'span 6 / span 6',
        'span-12': 'span 12 / span 12'
      },
      gridRow: {
        'span-1': 'span 1 / span 1',
        'span-2': 'span 2 / span 2',
        'span-3': 'span 3 / span 3',
        'span-4': 'span 4 / span 4',
        'span-5': 'span 5 / span 5',
        'span-6': 'span 6 / span 6'
      }
    }
  },
  plugins: [],
  safelist: [
    {
      pattern: /^(grid-cols|grid-rows|col-span|row-span|gap)-/,
      variants: ['responsive', 'hover', 'focus', 'active']
    }
  ]
};
