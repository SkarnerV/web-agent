import React from 'react'

// Types
export type BadgeVariant = 'draft' | 'published' | 'debugging' | 'error' | 'info'

export interface BadgeProps {
  variant?: BadgeVariant
  children: React.ReactNode
}

// Variant styles mapping
const variantStyles: Record<BadgeVariant, { bg: string; text: string }> = {
  draft: { bg: 'bg-gray-100', text: 'text-gray-600' },
  published: { bg: 'bg-success-50', text: 'text-success-500' },
  debugging: { bg: 'bg-warning-50', text: 'text-warning-500' },
  error: { bg: 'bg-error-50', text: 'text-error-500' },
  info: { bg: 'bg-brand-50', text: 'text-brand-500' },
}

// Badge Component
export const Badge: React.FC<BadgeProps> = ({
  variant = 'draft',
  children,
}) => {
  const styles = variantStyles[variant]
  
  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${styles.bg} ${styles.text}`}>
      {children}
    </span>
  )
}

// Preset badges for convenience
export const BadgeDraft: React.FC = () => <Badge variant="draft">草稿</Badge>
export const BadgePublished: React.FC = () => <Badge variant="published">已发布</Badge>
export const BadgeDebugging: React.FC = () => <Badge variant="debugging">调试中</Badge>
export const BadgeError: React.FC = () => <Badge variant="error">错误</Badge>
export const BadgeInfo: React.FC = () => <Badge variant="info">进行中</Badge>

export default Badge