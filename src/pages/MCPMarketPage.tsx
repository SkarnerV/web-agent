import React from 'react'

import { Search, Filter, Plug, ArrowRight, CheckCircle } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { Badge } from '../components/ui/Badge'

// Types
interface MCPCardProps {
  id: string
  name: string
  description: string
  provider: string
  status: 'official' | 'community'
  usageCount: number
  rating: number
  features: string[]
}

// Mock data
const categories = [
  { name: '全部', count: 45 },
  { name: '文件系统', count: 8 },
  { name: '数据库', count: 12 },
  { name: '浏览器', count: 6 },
  { name: 'API 集成', count: 15 },
  { name: '工具链', count: 4 },
]

const mcpServers: MCPCardProps[] = [
  { id: '1', name: 'filesystem', description: '文件系统操作：读写、创建、删除文件和目录', provider: 'Anthropic', status: 'official', usageCount: 5000, rating: 4.9, features: ['读写文件', '目录管理', '权限控制'] },
  { id: '2', name: 'postgres', description: 'PostgreSQL 数据库连接和查询操作', provider: 'Anthropic', status: 'official', usageCount: 3000, rating: 4.8, features: ['SQL查询', '事务管理', '数据导入'] },
  { id: '3', name: 'puppeteer', description: '浏览器自动化：截图、点击、填表', provider: 'Community', status: 'community', usageCount: 2000, rating: 4.7, features: ['页面截图', '自动化测试', '表单填充'] },
  { id: '4', name: 'slack', description: 'Slack API 集成：发送消息、读取频道', provider: 'Community', status: 'community', usageCount: 1500, rating: 4.6, features: ['消息发送', '频道管理', '用户信息'] },
  { id: '5', name: 'github', description: 'GitHub API：仓库操作、PR 管理、Issue', provider: 'Anthropic', status: 'official', usageCount: 4000, rating: 4.9, features: ['仓库操作', 'PR管理', 'Issue跟踪'] },
  { id: '6', name: 'sqlite', description: 'SQLite 本地数据库轻量级存储', provider: 'Community', status: 'community', usageCount: 800, rating: 4.5, features: ['本地存储', 'SQL支持', '轻量级'] },
]

// MCPCard Component
const MCPCard: React.FC<MCPCardProps & { onUse?: () => void }> = ({
  name,
  description,
  provider,
  status,
  usageCount,
  rating,
  features,
  onUse,
}) => {
  return (
    <div className="p-5 bg-white rounded-xl border border-border-subtle flex flex-col gap-3">
      {/* Header */}
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-lg bg-brand-50 flex items-center justify-center">
          <Plug className="w-5 h-5 text-brand-500" />
        </div>
        <div className="flex-1 flex flex-col gap-1">
          <div className="flex items-center gap-2">
            <span className="text-sm font-semibold text-text-primary">{name}</span>
            {status === 'official' && (
              <Badge variant="published">
                <CheckCircle className="w-3 h-3 mr-0.5" />
                官方
              </Badge>
            )}
          </div>
          <span className="text-xs text-text-tertiary">{provider}</span>
        </div>
      </div>

      {/* Description */}
      <p className="text-sm text-text-secondary line-clamp-2 leading-relaxed">
        {description}
      </p>

      {/* Features */}
      <div className="flex items-center gap-1.5">
        {features.slice(0, 2).map((feature) => (
          <span key={feature} className="px-2 py-0.5 bg-gray-100 rounded text-xs text-text-tertiary">
            {feature}
          </span>
        ))}
        {features.length > 2 && (
          <span className="text-xs text-text-tertiary">+{features.length - 2}</span>
        )}
      </div>

      {/* Meta */}
      <div className="flex items-center gap-2 text-xs text-text-tertiary">
        <span className="text-warning-500">⭐ {rating}</span>
        <span>·</span>
        <span>{usageCount}+ 使用</span>
      </div>

      {/* Divider */}
      <div className="h-px bg-border-subtle" />

      {/* Actions */}
      <Button variant="primary" onClick={onUse} icon={<ArrowRight className="w-4 h-4" />}>
        接入
      </Button>
    </div>
  )
}

// CategoryItem Component
const CategoryItem: React.FC<{ name: string; count: number; active?: boolean; onClick?: () => void }> = ({
  name,
  count,
  active = false,
  onClick,
}) => {
  return (
    <button
      onClick={onClick}
      className={`p-4 rounded-xl border text-sm ${
        active
          ? 'bg-brand-50 border-brand-500'
          : 'bg-white border-border-subtle hover:bg-gray-50'
      }`}
    >
      <div className="flex flex-col gap-1">
        <span className={`font-semibold ${active ? 'text-brand-500' : 'text-text-primary'}`}>
          {name}
        </span>
        <span className={`text-xs ${active ? 'text-brand-500' : 'text-text-tertiary'}`}>
          {count} 个服务器
        </span>
      </div>
    </button>
  )
}

// MCPMarketPage Component
const MCPMarketPage: React.FC = () => {
  const [searchQuery, setSearchQuery] = React.useState('')
  const [activeCategory, setActiveCategory] = React.useState('全部')

  const handleUseMCP = (id: string) => {
    // TODO: Implement MCP usage
    console.log('Using MCP:', id)
  }

  return (
    <Layout
      breadcrumb={[
        { label: '市场' },
        { label: 'MCP 市场' },
      ]}
    >
      <div className="p-8 flex flex-col gap-5 h-full overflow-auto">
        {/* Header */}
        <div className="flex flex-col gap-1">
          <h1 className="text-lg font-bold text-text-primary">MCP 市场</h1>
          <p className="text-sm text-text-secondary">官方认证的 MCP Server 集合 · 一键接入</p>
        </div>

        {/* Toolbar */}
        <div className="flex items-center gap-2.5">
          {/* Search */}
          <div className="flex-1 flex items-center gap-2 px-3 py-2 bg-white border border-border-subtle rounded-md">
            <Search className="w-4 h-4 text-text-tertiary" />
            <input
              type="text"
              placeholder="搜索 MCP Server..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="flex-1 bg-transparent text-sm outline-none placeholder:text-text-tertiary"
            />
          </div>

          {/* Filter */}
          <Button variant="secondary" icon={<Filter className="w-4 h-4" />} iconOnly />
        </div>

        {/* Body */}
        <div className="flex gap-4 h-full">
          {/* Categories Sidebar */}
          <div className="w-[200px] p-4 rounded-xl bg-white border border-border-subtle flex flex-col gap-2">
            {categories.map((cat) => (
              <CategoryItem
                key={cat.name}
                name={cat.name}
                count={cat.count}
                active={activeCategory === cat.name}
                onClick={() => setActiveCategory(cat.name)}
              />
            ))}
          </div>

          {/* Cards Grid */}
          <div className="flex-1 grid grid-cols-3 gap-4">
            {mcpServers.map((mcp) => (
              <MCPCard
                key={mcp.id}
                {...mcp}
                onUse={() => handleUseMCP(mcp.id)}
              />
            ))}
          </div>
        </div>
      </div>
    </Layout>
  )
}

export default MCPMarketPage