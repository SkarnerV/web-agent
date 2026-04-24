import React from 'react'

// Types
export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger'
export type ButtonSize = 'sm' | 'md' | 'lg'

export interface ButtonProps {
  variant?: ButtonVariant
  size?: ButtonSize
  children?: React.ReactNode
  icon?: React.ReactNode
  iconOnly?: boolean
  onClick?: () => void
  disabled?: boolean
  className?: string
}

// Variant styles mapping
const variantStyles: Record<ButtonVariant, string> = {
  primary: 'bg-brand-500 text-white hover:bg-brand-600',
  secondary: 'bg-white text-text-primary border border-border-strong hover:bg-gray-50',
  ghost: 'bg-transparent text-brand-500 hover:bg-brand-50',
  danger: 'bg-error-500 text-white hover:bg-red-600',
}

// Size styles mapping
const sizeStyles: Record<ButtonSize, string> = {
  sm: 'px-3 py-1.5 text-sm',
  md: 'px-4 py-2.5 text-sm',
  lg: 'px-5 py-3 text-base',
}

// Button Component
export const Button: React.FC<ButtonProps> = ({
  variant = 'primary',
  size = 'md',
  children,
  icon,
  iconOnly = false,
  onClick,
  disabled = false,
  className = '',
}) => {
  const baseStyles = 'inline-flex items-center justify-center gap-2 rounded-md font-medium transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 focus-visible:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed'
  
  if (iconOnly) {
    return (
      <button
        onClick={onClick}
        disabled={disabled}
        className={`
          ${baseStyles}
          w-9 h-9 rounded-md
          ${variant === 'secondary' ? 'bg-white border border-border-subtle text-gray-600 hover:bg-gray-50' : variantStyles[variant]}
          ${className}
        `}
      >
        {icon}
      </button>
    )
  }

  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`${baseStyles} ${variantStyles[variant]} ${sizeStyles[size]} ${className}`}
    >
      {icon}
      {children}
    </button>
  )
}

export default Button