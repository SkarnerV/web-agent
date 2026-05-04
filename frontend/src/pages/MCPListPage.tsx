import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Plug, Trash2, Zap } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'

interface McpServer {
  id: string
  name: string
  address: string
  protocol: 'SSE' | 'Streamable HTTP' | 'stdio'
  status: 'online' | 'offline' | 'error'
  toolCount: number
  enabled: boolean
}

const mcpData: McpServer[] = [
  { id: '1', name: 'Playwright MCP', address: 'playwright-mcp.internal:8080', protocol: 'SSE', status: 'online', toolCount: 12, enabled: true },
  { id: '2', name: 'GitHub MCP', address: 'github-mcp.internal:8080', protocol: 'Streamable HTTP', status: 'online', toolCount: 8, enabled: true },
  { id: '3', name: 'Puppeteer MCP', address: 'puppeteer-mcp.internal:8080', protocol: 'SSE', status: 'offline', toolCount: 6, enabled: false },
  { id: '4', name: 'Slack MCP', address: 'slack-mcp.internal:8080', protocol: 'Streamable HTTP', status: 'error', toolCount: 5, enabled: true },
  { id: '5', name: 'Filesystem MCP', address: 'fs-mcp.internal:8080', protocol: 'stdio', status: 'online', toolCount: 4, enabled: true },
]

const statusConfig = {
  online: { dot: 'bg-success-500', label: '在线' },
  offline: { dot: 'bg-gray-300', label: '离线' },
  error: { dot: 'bg-error-500', label: '错误' },
}

const MCPListPage: React.FC = () => {
  const navigate = useNavigate()
  const [servers, setServers] = useState(mcpData)
  const [testingId, setTestingId] = useState<string | null>(null)

  const handleToggle = (id: string) => {
    setServers((prev) =>
      prev.map((s) => (s.id === id ? { ...s, enabled: !s.enabled } : s)),
    )
  }

  const handleTestConnection = async (id: string) => {
    setTestingId(id)
    setTimeout(() => {
      setServers((prev) =>
        prev.map((s) => (s.id === id ? { ...s, status: 'online' as const } : s)),
      )
      setTestingId(null)
    }, 1500)
  }

  const handleDelete = (id: string) => {
    setServers((prev) => prev.filter((s) => s.id !== id))
  }

  const onlineCount = servers.filter((s) => s.status === 'online').length
  const totalTools = servers.reduce((sum, s) => sum + s.toolCount, 0)

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
          <div className="grid grid-cols-6 gap-4 px-6 py-3 bg-gray-50 border-b border-border-subtle text-[13px] font-medium text-text-secondary">
            <div>服务器名称</div>
            <div>状态</div>
            <div>协议</div>
            <div>工具</div>
            <div>启用</div>
            <div>操作</div>
          </div>

          {/* Table Body */}
          {servers.map((server) => {
            const st = statusConfig[server.status]
            return (
              <div
                key={server.id}
                className="grid grid-cols-6 gap-4 px-6 py-3.5 border-b border-border-subtle text-sm hover:bg-bg-hover transition-colors items-center"
              >
                {/* Server Name + Address */}
                <div className="flex flex-col gap-0.5 min-w-0">
                  <span className="font-medium text-text-primary truncate">{server.name}</span>
                  <span className="text-[11px] text-text-tertiary truncate">{server.address}</span>
                </div>

                {/* Status: dot + text */}
                <div className="flex items-center gap-2">
                  <div className={`w-2 h-2 rounded-full ${st.dot}`} />
                  <span className="text-text-secondary">{st.label}</span>
                </div>

                {/* Protocol */}
                <div>
                  <span className="px-2 py-0.5 bg-gray-100 rounded text-xs text-text-secondary">
                    {server.protocol}
                  </span>
                </div>

                {/* Tool Count */}
                <div className="text-text-secondary">{server.toolCount} 个工具</div>

                {/* Toggle Switch */}
                <div>
                  <button
                    onClick={() => handleToggle(server.id)}
                    className={`w-11 h-6 rounded-full transition-colors ${
                      server.enabled ? 'bg-brand-500' : 'bg-gray-200'
                    }`}
                  >
                    <div
                      className={`w-5 h-5 rounded-full bg-white shadow-sm transition-transform ${
                        server.enabled ? 'translate-x-5' : 'translate-x-0.5'
                      }`}
                    />
                  </button>
                </div>

                {/* Actions */}
                <div className="flex items-center gap-1.5">
                  <button
                    onClick={() => handleTestConnection(server.id)}
                    disabled={testingId === server.id}
                    className="px-2.5 py-1.5 bg-white border border-border-subtle rounded text-xs text-text-secondary hover:border-border-strong transition-colors disabled:opacity-50"
                  >
                    {testingId === server.id ? (
                      <span className="flex items-center gap-1">
                        <Zap className="w-3 h-3 animate-pulse" />
                        测试中
                      </span>
                    ) : (
                      '测试连接'
                    )}
                  </button>
                  <button className="px-2.5 py-1.5 bg-white border border-border-subtle rounded text-xs text-text-secondary hover:border-border-strong transition-colors">
                    编辑
                  </button>
                  <button
                    onClick={() => handleDelete(server.id)}
                    className="w-8 h-8 rounded-md flex items-center justify-center text-text-tertiary hover:text-error-500 hover:bg-error-50 transition-colors"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            )
          })}

          {/* Empty state */}
          {servers.length === 0 && (
            <div className="flex flex-col items-center justify-center py-16">
              <Plug className="w-12 h-12 text-text-tertiary mb-4" />
              <p className="text-text-secondary mb-4">还没有添加任何 MCP Server</p>
              <Button variant="primary" icon={<Plus className="w-4 h-4" />} onClick={() => navigate('/mcp/add')}>
                添加第一个 MCP
              </Button>
            </div>
          )}
        </div>

        {/* Summary Footer */}
        <div className="mt-4 flex items-center gap-4 text-[13px] text-text-tertiary">
          <span>共 {servers.length} 个 MCP Server</span>
          <span>· {onlineCount} 个已连接</span>
          <span>· {totalTools} 个可用工具</span>
        </div>
      </div>
    </Layout>
  )
}

export default MCPListPage
