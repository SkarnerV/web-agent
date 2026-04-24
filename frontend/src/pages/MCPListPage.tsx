import React from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Plug, Edit, Trash2, Power, PowerOff } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { Badge } from '../components/ui/Badge'

// Mock data for MCP servers
const mcpData = [
  {
    id: '1',
    name: 'Playwright MCP',
    type: 'npm',
    status: 'connected' as const,
    toolCount: 12,
  },
  {
    id: '2',
    name: 'GitHub MCP',
    type: 'stdio',
    status: 'connected' as const,
    toolCount: 8,
  },
  {
    id: '3',
    name: 'Puppeteer MCP',
    type: 'npm',
    status: 'disconnected' as const,
    toolCount: 6,
  },
  {
    id: '4',
    name: 'Slack MCP',
    type: 'http',
    status: 'error' as const,
    toolCount: 5,
  },
  {
    id: '5',
    name: 'Filesystem MCP',
    type: 'stdio',
    status: 'connected' as const,
    toolCount: 4,
  },
]

// MCPListPage Component
const MCPListPage: React.FC = () => {
  const navigate = useNavigate()

  const getStatusBadge = (status: 'connected' | 'disconnected' | 'error') => {
    switch (status) {
      case 'connected':
        return <Badge variant="published">已连接</Badge>
      case 'disconnected':
        return <Badge variant="draft">未连接</Badge>
      case 'error':
        return <Badge variant="error">错误</Badge>
      default:
        return <Badge variant="draft">未知</Badge>
    }
  }

  const getStatusIcon = (status: 'connected' | 'disconnected' | 'error') => {
    return status === 'connected' ? (
      <Power className="w-4 h-4 text-success-500" />
    ) : (
      <PowerOff className="w-4 h-4 text-text-tertiary" />
    )
  }

  return (
    <Layout breadcrumb={[{ label: '我的资产' }, { label: 'MCP' }]}>
      <div className="p-8">
        {/* Title Row */}
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-xl font-semibold text-text-primary">MCP Servers</h1>
          <Button 
            variant="primary" 
            icon={<Plus className="w-4 h-4" />}
            onClick={() => navigate('/mcp/add')}
          >
            添加 MCP
          </Button>
        </div>

        {/* Table */}
        <div className="bg-white rounded-xl border border-border-subtle overflow-hidden">
          {/* Table Header */}
          <div className="grid grid-cols-5 gap-4 px-6 py-4 bg-gray-50 border-b border-border-subtle text-sm font-medium text-text-secondary">
            <div>名称</div>
            <div>类型</div>
            <div>状态</div>
            <div>工具数</div>
            <div>操作</div>
          </div>

          {/* Table Body */}
          {mcpData.map((mcp) => (
            <div 
              key={mcp.id}
              className="grid grid-cols-5 gap-4 px-6 py-4 border-b border-border-subtle text-sm hover:bg-bg-hover transition-colors items-center"
            >
              {/* Name */}
              <div className="flex items-center gap-3">
                {getStatusIcon(mcp.status)}
                <span className="font-medium text-text-primary">{mcp.name}</span>
              </div>

              {/* Type */}
              <div className="text-text-secondary">
                <span className="px-2 py-0.5 bg-gray-100 rounded text-xs">{mcp.type}</span>
              </div>

              {/* Status */}
              <div>
                {getStatusBadge(mcp.status)}
              </div>

              {/* Tool Count */}
              <div className="text-text-secondary">
                {mcp.toolCount} 个工具
              </div>

              {/* Actions */}
              <div className="flex gap-2">
                <Button 
                  variant="secondary" 
                  size="sm" 
                  iconOnly 
                  icon={<Edit className="w-4 h-4" />}
                />
                <Button 
                  variant="ghost" 
                  size="sm" 
                  iconOnly 
                  icon={<Trash2 className="w-4 h-4" />}
                />
              </div>
            </div>
          ))}

          {/* Empty state */}
          {mcpData.length === 0 && (
            <div className="flex flex-col items-center justify-center py-16">
              <Plug className="w-12 h-12 text-text-tertiary mb-4" />
              <p className="text-text-secondary mb-4">还没有添加任何 MCP Server</p>
              <Button 
                variant="primary" 
                icon={<Plus className="w-4 h-4" />}
                onClick={() => navigate('/mcp/add')}
              >
                添加第一个 MCP
              </Button>
            </div>
          )}
        </div>

        {/* Summary Footer */}
        <div className="mt-4 flex items-center gap-4 text-sm text-text-tertiary">
          <span>共 {mcpData.length} 个 MCP Server</span>
          <span>· {mcpData.filter(m => m.status === 'connected').length} 个已连接</span>
          <span>· {mcpData.reduce((sum, m) => sum + m.toolCount, 0)} 个可用工具</span>
        </div>
      </div>
    </Layout>
  )
}

export default MCPListPage