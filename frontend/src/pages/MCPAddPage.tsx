import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Check, Package, Code, AlertCircle } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { Badge } from '../components/ui/Badge'

// Mock MCP market data
const mcpMarketData = [
  {
    id: '1',
    name: 'Playwright MCP',
    description: '浏览器自动化和网页测试工具集',
    iconBg: 'bg-brand-50',
    toolCount: 12,
    downloads: '1.2k',
  },
  {
    id: '2',
    name: 'GitHub MCP',
    description: 'GitHub API 集成，支持仓库、Issue、PR 操作',
    iconBg: 'bg-gray-100',
    toolCount: 8,
    downloads: '856',
  },
  {
    id: '3',
    name: 'Puppeteer MCP',
    description: '高级浏览器控制和页面抓取',
    iconBg: 'bg-success-50',
    toolCount: 6,
    downloads: '432',
  },
  {
    id: '4',
    name: 'Slack MCP',
    description: 'Slack 消息发送和频道管理',
    iconBg: 'bg-warning-50',
    toolCount: 5,
    downloads: '298',
  },
  {
    id: '5',
    name: 'Notion MCP',
    description: 'Notion 页面和数据库操作',
    iconBg: 'bg-error-50',
    toolCount: 7,
    downloads: '567',
  },
  {
    id: '6',
    name: 'Filesystem MCP',
    description: '本地文件系统读写操作',
    iconBg: 'bg-gray-100',
    toolCount: 4,
    downloads: '1.5k',
  },
]

// MCP Market Card
const MCPMarketCard: React.FC<{
  name: string
  description: string
  iconBg: string
  toolCount: number
  downloads: string
  onInstall?: () => void
}> = ({ name, description, iconBg, toolCount, downloads, onInstall }) => {
  return (
    <div className="p-5 bg-white rounded-lg border border-border-subtle flex flex-col gap-3 hover:shadow-md transition-shadow">
      {/* Header */}
      <div className="flex items-center gap-3">
        <div className={`w-10 h-10 rounded-lg flex items-center justify-center text-lg font-semibold ${iconBg}`}>
          {name[0]}
        </div>
        <div className="flex-1 flex flex-col gap-0.5">
          <span className="text-sm font-semibold text-text-primary">{name}</span>
          <span className="text-xs text-text-tertiary">下载 {downloads}</span>
        </div>
      </div>

      {/* Description */}
      <p className="text-sm text-text-secondary line-clamp-2 leading-relaxed">
        {description}
      </p>

      {/* Meta */}
      <div className="flex items-center gap-3 text-sm text-text-tertiary">
        <Badge variant="info">{toolCount} 工具</Badge>
      </div>

      {/* Divider */}
      <div className="h-px bg-border-subtle" />

      {/* Actions */}
      <Button variant="primary" size="sm" onClick={onInstall} icon={<Plus className="w-4 h-4" />}>
        安装
      </Button>
    </div>
  )
}

// MCPAddPage Component
const MCPAddPage: React.FC = () => {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<'market' | 'manual'>('market')
  const [jsonConfig, setJsonConfig] = useState('')
  const [isValidJson, setIsValidJson] = useState<boolean | null>(null)

  const handleValidate = () => {
    try {
      JSON.parse(jsonConfig)
      setIsValidJson(true)
    } catch {
      setIsValidJson(false)
    }
  }

  return (
    <Layout breadcrumb={[{ label: 'MCP' }, { label: '添加 MCP Server' }]}>
      <div className="p-10">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-text-primary">添加 MCP Server</h1>
          <p className="text-[13px] text-text-secondary">
            从市场安装预配置的 MCP Server，或手动配置自定义连接
          </p>
        </div>

        {/* Tab Switcher */}
        <div className="inline-flex bg-gray-100 rounded-lg p-1 mt-6 mb-6">
          <button
            onClick={() => setActiveTab('market')}
            className={`flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-colors ${
              activeTab === 'market'
                ? 'bg-white text-text-primary shadow-sm'
                : 'text-text-tertiary hover:text-text-secondary'
            }`}
          >
            <Package className="w-4 h-4" />
            从市场安装
          </button>
          <button
            onClick={() => setActiveTab('manual')}
            className={`flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-colors ${
              activeTab === 'manual'
                ? 'bg-white text-text-primary shadow-sm'
                : 'text-text-tertiary hover:text-text-secondary'
            }`}
          >
            <Code className="w-4 h-4" />
            手动配置
          </button>
        </div>

        {/* Body */}
        {activeTab === 'market' ? (
          /* Market Grid */
          <div className="grid grid-cols-3 gap-6">
            {mcpMarketData.map((mcp) => (
              <MCPMarketCard
                key={mcp.id}
                name={mcp.name}
                description={mcp.description}
                iconBg={mcp.iconBg}
                toolCount={mcp.toolCount}
                downloads={mcp.downloads}
                onInstall={() => navigate('/mcp')}
              />
            ))}
          </div>
        ) : (
          /* Manual Configuration */
          <div className="max-w-[600px]">
            {/* JSON Editor */}
            <div className="flex flex-col gap-2 mb-4">
              <label className="text-sm font-medium text-text-primary">JSON 配置</label>
              <textarea
                placeholder={`{
  "mcpServers": {
    "my-server": {
      "command": "node",
      "args": ["server.js"],
      "env": {}
    }
  }
}`}
                value={jsonConfig}
                onChange={(e) => {
                  setJsonConfig(e.target.value)
                  setIsValidJson(null)
                }}
                className={`w-full h-[300px] px-4 py-3 bg-white border rounded-lg text-sm font-mono text-text-primary placeholder:text-text-tertiary resize-none focus:outline-none ${
                  isValidJson === false
                    ? 'border-error-500'
                    : isValidJson === true
                    ? 'border-success-500'
                    : 'border-border-strong focus:border-brand-500'
                }`}
              />
            </div>

            {/* Validation Status */}
            {isValidJson !== null && (
              <div className={`flex items-center gap-2 mb-4 text-sm ${
                isValidJson ? 'text-success-500' : 'text-error-500'
              }`}>
                {isValidJson ? (
                  <Check className="w-4 h-4" />
                ) : (
                  <AlertCircle className="w-4 h-4" />
                )}
                {isValidJson ? 'JSON 格式正确' : 'JSON 格式错误，请检查语法'}
              </div>
            )}

            {/* Actions */}
            <div className="flex gap-3">
              <Button variant="secondary" onClick={handleValidate}>
                验证配置
              </Button>
              <Button variant="primary" disabled={!isValidJson}>
                添加 Server
              </Button>
            </div>

            {/* Help */}
            <div className="mt-6 p-4 bg-gray-50 rounded-lg border border-border-subtle">
              <p className="text-sm font-medium text-text-primary mb-2">配置示例</p>
              <pre className="text-xs text-text-tertiary font-mono overflow-x-auto">
{`{
  "mcpServers": {
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@anthropic/mcp-server-filesystem", "/path/to/files"]
    }
  }
}`}
              </pre>
            </div>
          </div>
        )}
      </div>
    </Layout>
  )
}

export default MCPAddPage