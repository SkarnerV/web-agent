import type { Config } from 'tailwindcss'

const config: Config = {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Brand colors
        brand: {
          50: 'var(--brand-50)',
          100: 'var(--brand-100)',
          400: 'var(--brand-400)',
          500: 'var(--brand-500)',
          600: 'var(--brand-600)',
        },
        // Gray scale
        gray: {
          50: 'var(--gray-50)',
          100: 'var(--gray-100)',
          200: 'var(--gray-200)',
          300: 'var(--gray-300)',
          400: 'var(--gray-400)',
          500: 'var(--gray-500)',
          600: 'var(--gray-600)',
          700: 'var(--gray-700)',
          800: 'var(--gray-800)',
          900: 'var(--gray-900)',
        },
        // Functional colors
        success: {
          50: 'var(--success-50)',
          500: 'var(--success-500)',
        },
        error: {
          50: 'var(--error-50)',
          500: 'var(--error-500)',
        },
        warning: {
          50: 'var(--warning-50)',
          500: 'var(--warning-500)',
        },
        info: {
          50: 'var(--info-50)',
          500: 'var(--info-500)',
        },
        // Text colors
        text: {
          primary: 'var(--text-primary)',
          secondary: 'var(--text-secondary)',
          tertiary: 'var(--text-tertiary)',
          inverse: 'var(--text-inverse)',
        },
        // Border colors
        border: {
          strong: 'var(--border-strong)',
          subtle: 'var(--border-subtle)',
        },
        // Background colors
        bg: {
          canvas: 'var(--bg-canvas)',
          hover: 'var(--bg-hover)',
          surface: 'var(--bg-surface)',
        },
        white: 'var(--white)',
      },
      borderRadius: {
        sm: 'var(--radius-sm)',
        md: 'var(--radius-md)',
        lg: 'var(--radius-lg)',
        xl: 'var(--radius-xl)',
      },
      spacing: {
        '1': 'var(--space-1)',
        '2': 'var(--space-2)',
        '3': 'var(--space-3)',
        '4': 'var(--space-4)',
        '5': 'var(--space-5)',
        '6': 'var(--space-6)',
        '8': 'var(--space-8)',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      fontSize: {
        '11': ['11px', { lineHeight: '1.4' }],
        '13': ['13px', { lineHeight: '1.5' }],
        '15': ['15px', { lineHeight: '1.5' }],
      },
      boxShadow: {
        'card': '0 10px 40px rgba(15, 23, 42, 0.1)',
      },
    },
  },
  plugins: [],
}

export default config