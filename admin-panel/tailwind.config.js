import defaultTheme from 'tailwindcss/defaultTheme';

export default {
  content: ['./index.html', './src/**/*.{js,jsx,ts,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', ...defaultTheme.fontFamily.sans],
      },
      colors: {
        primary: '#FF4D8D',
        secondary: '#7B61FF',
        accent: '#FFB547',
        surface: 'rgba(255,255,255,0.08)',
        backdrop: 'rgba(255,255,255,0.25)',
      },
      boxShadow: {
        glow: '0 30px 80px rgba(59, 130, 246, 0.14)',
        card: '0 24px 80px rgba(15, 23, 42, 0.12)',
      },
      backdropBlur: {
        xl: '30px',
      },
      animation: {
        float: 'float 6s ease-in-out infinite',
        shimmer: 'shimmer 1.8s linear infinite',
        pulseHeart: 'pulseHeart 1.4s ease-in-out infinite',
      },
      keyframes: {
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-12px)' },
        },
        shimmer: {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
        pulseHeart: {
          '0%, 100%': { transform: 'scale(1)', opacity: 1 },
          '50%': { transform: 'scale(1.1)', opacity: 0.9 },
        },
      },
    },
  },
  plugins: [],
};
