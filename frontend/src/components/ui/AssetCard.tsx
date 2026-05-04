import React from 'react'
import { Bot, Sparkles, Zap, MessageSquare, Wrench, Users } from 'lucide-react'
import { Badge } from './Badge'
import { Button } from './Button'

const iconMap: Record<string, React.FC<{ className?: string }>> = {
  bot: Bot,
  sparkles: Sparkles,
  zap: Zap,
  message: MessageSquare,
}

export interface AssetCardProps {
  id: string
  name: string
  description: string
  iconBg?: string
  iconText?: string
  status?: 'draft' | 'published' | 'debugging' | 'error' | 'info'
  toolCount?: number
  collabCount?: number
  updatedAt?: string
  onUse?: () => void
  onEdit?: () => void
}

const statusLabel: Record<string, string> = {
  draft: '草稿',
  published: '已发布',
  debugging: '调试中',
  error: '错误',
  info: '进行中',
}

export const AssetCard: React.FC<AssetCardProps> = ({
  name,
  description,
  iconBg = 'bg-brand-50',
  iconText = 'bot',
  status,
  toolCount = 0,
  collabCount = 0,
  updatedAt = '2d 前',
  onUse,
  onEdit,
}) => {
  const IconComponent = iconMap[iconText] ?? Bot
  const iconColorClass = iconBg === 'bg-brand-50' ? 'text-brand-500'
    : iconBg === 'bg-warning-50' ? 'text-warning-500'
    : iconBg === 'bg-success-50' ? 'text-success-500'
    : iconBg === 'bg-error-50' ? 'text-error-500'
    : 'text-gray-600'

  return (
    <div className="flex-1 p-5 bg-white rounded-lg border border-border-subtle flex flex-col gap-3">
      {/* Header Row */}
      <div className="flex items-center gap-3">
        <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${iconBg}`}>
          <IconComponent className={`w-5 h-5 ${iconColorClass}`} />
        </div>
        <div className="flex-1 flex flex-col gap-0.5">
          <span className="text-sm font-semibold text-text-primary">{name}</span>
          {status && <Badge variant={status}>{statusLabel[status]}</Badge>}
        </div>
      </div>

      {/* Description */}
      <p className="text-sm text-text-secondary line-clamp-2 leading-relaxed">
        {description}
      </p>

      {/* Meta */}
      <div className="flex items-center gap-3 text-sm text-text-tertiary">
        <span className="flex items-center gap-1">
          <Wrench className="w-3.5 h-3.5" />
          工具 {toolCount}
        </span>
        <span className="flex items-center gap-1">
          <Users className="w-3.5 h-3.5" />
          协作 {collabCount}
        </span>
        <span>· {updatedAt}</span>
      </div>

      {/* Divider */}
      <div className="h-px bg-border-subtle" />

      {/* Actions */}
      <div className="flex gap-2">
        <Button variant="primary" onClick={onUse}>使用</Button>
        <Button variant="secondary" onClick={onEdit}>编辑</Button>
      </div>
    </div>
  )
}

export default AssetCard
