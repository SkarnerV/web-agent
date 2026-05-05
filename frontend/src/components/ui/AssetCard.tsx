import React from 'react'
import { Bot, Wand2, Plug, BookOpen, Wrench, Users } from 'lucide-react'
import { Badge } from './Badge'
import { Button } from './Button'

export type AssetIconType = 'agent' | 'skill' | 'mcp' | 'knowledge'

const iconTypeConfig: Record<AssetIconType, {
  icon: React.FC<{ className?: string }>
  bg: string
  color: string
}> = {
  agent: { icon: Bot, bg: 'bg-brand-50', color: 'text-brand-500' },
  skill: { icon: Wand2, bg: 'bg-purple-50', color: 'text-purple-500' },
  mcp: { icon: Plug, bg: 'bg-success-50', color: 'text-success-500' },
  knowledge: { icon: BookOpen, bg: 'bg-warning-50', color: 'text-warning-500' },
}

export interface AssetCardProps {
  id: string
  name: string
  description: string
  iconType?: AssetIconType
  /** @deprecated Use iconType instead */
  iconBg?: string
  /** @deprecated Use iconType instead */
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
  iconType,
  iconBg,
  iconText,
  status,
  toolCount = 0,
  collabCount = 0,
  updatedAt = '2d 前',
  onUse,
  onEdit,
}) => {
  const config = iconType ? iconTypeConfig[iconType] : null
  const resolvedBg = config?.bg ?? iconBg ?? 'bg-brand-50'
  const resolvedColor = config?.color ?? 'text-brand-500'
  const IconComponent = config?.icon ?? null

  return (
    <div className="w-full p-5 bg-white rounded-lg border border-border-subtle flex flex-col gap-3">
      <div className="flex items-center gap-3">
        <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${resolvedBg}`}>
          {IconComponent ? (
            <IconComponent className={`w-5 h-5 ${resolvedColor}`} />
          ) : (
            <span className={`text-sm font-semibold ${resolvedColor}`}>{iconText ?? name[0]}</span>
          )}
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
