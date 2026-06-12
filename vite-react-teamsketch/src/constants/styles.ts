export const BUTTON_STYLES = {
  variants: {
    primary: 'bg-primary-light hover:bg-primary-dark text-white',
    secondary: 'bg-secondary-light hover:bg-secondary-dark text-white',
    outline: 'border border-primary-light text-primary-light hover:bg-primary-lightest'
  },
  sizes: {
    sm: 'px-3 py-1.5 text-sm',
    md: 'px-4 py-2',
    lg: 'px-6 py-3 text-lg'
  },
  base: `
    rounded-lg
    transition-colors
    disabled:opacity-50
    disabled:cursor-not-allowed
  `
} as const;

export const INPUT_STYLES = {
  variants: {
    default: 'border-gray-300 dark:border-gray-600',
    error: 'border-red-500 dark:border-red-400',
    success: 'border-green-500 dark:border-green-400',
    warning: 'border-yellow-500 dark:border-yellow-400'
  },
  sizes: {
    sm: 'px-2 py-1 text-sm',
    md: 'px-3 py-2 text-base',
    lg: 'px-4 py-3 text-lg'
  },
  base: `
    w-full 
    rounded-lg 
    border 
    text-gray-900 
    dark:text-white 
    dark:bg-gray-800
    focus:outline-none 
    focus:ring-2 
    focus:ring-primary-light
    dark:focus:ring-primary-dark
    disabled:opacity-50
    disabled:cursor-not-allowed
  `
} as const;
