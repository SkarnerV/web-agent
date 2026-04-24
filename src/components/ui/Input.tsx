import React from 'react'
import { Search } from 'lucide-react'

// Types
export interface InputProps {
  placeholder?: string
  value?: string
  onChange?: (value: string) => void
  type?: 'text' | 'password' | 'email' | 'search'
  disabled?: boolean
  error?: boolean
  icon?: React.ReactNode
  className?: string
}

// Input Component
export const Input: React.FC<InputProps> = ({
  placeholder = 'Placeholder',
  value = '',
  onChange,
  type = 'text',
  disabled = false,
  error = false,
  icon,
  className = '',
}) => {
  return (
    <div className={`flex items-center gap-2 px-3 py-2.5 bg-white border rounded-sm ${error ? 'border-error-500' : 'border-border-strong'} ${className}`}>
      {icon && <span className="w-4 h-4 text-text-tertiary">{icon}</span>}
      {type === 'search' && <Search className="w-4 h-4 text-text-tertiary" />}
      <input
        type={type}
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange?.(e.target.value)}
        disabled={disabled}
        className="flex-1 bg-transparent text-sm text-text-primary placeholder:text-text-tertiary outline-none disabled:opacity-50 disabled:cursor-not-allowed"
      />
    </div>
  )
}

// SearchInput Component (convenience wrapper)
export const SearchInput: React.FC<InputProps> = (props) => (
  <Input {...props} type="search" icon={<Search className="w-4 h-4" />} />
)

export default Input