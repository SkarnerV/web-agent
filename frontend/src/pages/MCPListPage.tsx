import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Plug, Trash2, Zap, Loader2, AlertCircle } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { listMcps, toggleMcp, testMcpConnection, deleteMcp } from '../api/mcp'
import type { McpSummaryVO } from '../api/types'

type DisplayStatus = 'online' | 'offline' | 'error'

const statusConfig = {
  online: { dot: 'bg-success-500', label: '在线' },
  offline: { dot: 'bg-gray-300', label: '离线' },
  error: { dot: 'bg-error-500', label: '错误' },
}

function mapConnectionStatus(connectionStatus?: string): DisplayStatus {
  switch (connectionStatus) {
    case 'CONNECTED':
      return 'online'
    case 'ERROR':
      return 'error'
    default:
      return 'offline'
  }
}

const MCPListPage: React.FC = () => {
  const navigate = useNavigate()
  const [servers, setServers] = useState<McpSummaryVO[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [testingId, setTestingId] = useState<string | null>(null)

  const fetchServers = async () => {
    try {
      setLoading(true)
      setError(null)
      const result = await listMcps()
      setServers(result.data)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchServers()
  }, [])

  const handleToggle = async (id: string, currentEnabled: boolean) => {
    try {
      const updated = await toggleMcp(id, !currentEnabled)
      setServers((prev) =>
        prev.map((s) => (s.id === id ? { ...s, enabled: updated.enabled } : s)),
      )
    } catch {
      // revert is not needed since we only update on success
    }
  }

  const handleTestConnection = async (id: string) => {
    setTestingId(id)
    try {
      const result = await testMcpConnection(id)
      setServers((prev) =>
        prev.map((s) =>
          s.id === id
            ? { ...s, connectionStatus: result.connectionStatus }
            : s,
        ),
      )
    } catch {
      setServers((prev) =>
        prev.map((s) =>
          s.id === id ? { ...s, connectionStatus: 'ERROR' } : s,
        ),
      )
    } finally {
      setTestingId(null)
    }
  }

  const handleDelete = async (id: string) => {
    if (!window.confirm('确认删除此 MCP Server？此操作不可撤销。')) return
    try {
      await deleteMcp(id)
      setServers((prev) => prev.filter((s) => s.id !== id))
    } catch {
      // deletion failed silently
    }
  }

  const onlineCount = servers.filter((s) => mapConnectionStatus(s.connectionStatus) === 'online').length
  const totalTools = servers.reduce((sum, s) => sum + (s.toolsDiscoveredCount ?? 0), 0)

  if (loading) {
    return (
      <Layout breadcrumb={[{ label: '我的资产' }, { label: 'MCP' }]}>
        <div className="p-8 flex items-center justify-center min-h-[400px]">
          <Loader2 className="w-8 h-8 animate-spin text-text-tertiary" />
        </div>
      </Layout>
    )
  }

  if (error) {
    return (
      <Layout breadcrumb={[{ label: '我的资产' }, { label: 'MCP' }]}>
        <div className="p-8 flex flex-col items-center justify-center min-h-[400px] gap-4">
          <AlertCircle className="w-10 h-10 text-error-500" />
          <p className="text-text-secondary">{error}</p>
          <Button variant="primary" onClick={fetchServers}>
            重试
          </Button>
        </div>
      </Layout>
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
            const displayStatus = mapConnectionStatus(server.connectionStatus)
            const st = statusConfig[displayStatus]
            return (
              <div
                key={server.id}
                className="grid grid-cols-6 gap-4 px-6 py-3.5 border-b border-border-subtle text-sm hover:bg-bg-hover transition-colors items-center"
              >
                {/* Server Name + URL */}
                <div className="flex flex-col gap-0.5 min-w-0">
                  <span className="font-medium text-text-primary truncate">{server.name}</span>
                  <span className="text-[11px] text-text-tertiary truncate">{server.url}</span>
                </div>

                {/* Status: dot + text */}
                <div className="flex items-center gap-2">
                  <div className={`w-2 h-2 rounded-full ${st.dot}`} />
                  <span className="text-text-secondary">{st.label}</span>
                </div>

                {/* Protocol */}
                <div>
                  <span className="px-2 py-0.5 bg-gray-100 rounded text-xs text-text-secondary">
                    {server.protocol ?? '-'}
                  </span>
                </div>

                {/* Tool Count */}
                <div className="text-text-secondary">{server.toolsDiscoveredCount ?? 0} 个工具</div>

                {/* Toggle Switch */}
                <div>
                  <button
                    onClick={() => handleToggle(server.id, server.enabled)}
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
