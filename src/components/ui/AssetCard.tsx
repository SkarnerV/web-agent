import React from 'react'
import { Badge } from './Badge'
import { Button } from './Button'

// Types
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

// AssetCard Component
export const AssetCard: React.FC<AssetCardProps> = ({
  name,
  description,
  iconBg = 'bg-brand-50',
  iconText = name[0] || 'A',
  status,
  toolCount = 5,
  collabCount = 2,
  updatedAt = '2d 前',
  onUse,
  onEdit,
}) => {
  return (
    <div className="w-[300px] p-5 bg-white rounded-lg border border-border-subtle flex flex-col gap-3">
      {/* Header Row */}
      <div className="flex items-center gap-3">
        <div className={`w-10 h-10 rounded-lg flex items-center justify-center text-lg font-semibold ${iconBg}`}>
          {iconText}
        </div>
        <div className="flex-1 flex flex-col gap-0.5">
          <span className="text-sm font-semibold text-text-primary">{name}</span>
          {status && <Badge variant={status}>{status === 'draft' ? '草稿' : status === 'published' ? '已发布' : status === 'debugging' ? '调试中' : status === 'error' ? '错误' : '进行中'}</Badge>}
        </div>
      </div>

      {/* Description */}
      <p className="text-sm text-text-secondary line-clamp-2 leading-relaxed">
        {description}
      </p>

      {/* Meta */}
      <div className="flex items-center gap-3 text-sm text-text-tertiary">
        <span>🔧 工具 {toolCount}</span>
        <span>🧩 协作 {collabCount}</span>
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