import React, { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Heart, ArrowRight, Bot, Users, Star, Loader2 } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { Badge } from '../components/ui/Badge'
import { getAgent, listAgentVersions } from '../api/agent'
import { ApiError } from '../api/client'
import type { AgentDetailVO, AssetVersionVO } from '../api/types'

const timeAgo = (iso: string): string => {
  const diff = Date.now() - new Date(iso).getTime()
  const days = Math.floor(diff / 86400000)
  if (days < 1) return '今天'
  if (days === 1) return '1天前'
  if (days < 7) return `${days}天前`
  return `${Math.floor(days / 7)}周前`
}

const StatCard: React.FC<{ label: string; value: string; icon: React.ReactNode }> = ({
  label, value, icon,
}) => (
  <div className="flex items-center gap-3">
    <div className="w-10 h-10 rounded-lg bg-brand-50 flex items-center justify-center text-brand-500">
      {icon}
    </div>
    <div className="flex flex-col">
      <span className="text-xs text-text-tertiary">{label}</span>
      <span className="text-base font-semibold text-text-primary">{value}</span>
    </div>
  </div>
)

const ToolCard: React.FC<{ name: string; description: string; icon: string }> = ({
  name, description, icon,
}) => (
  <div className="flex items-center gap-2 py-2">
    <span className="text-lg">{icon}</span>
    <div className="flex-1 flex flex-col">
      <span className="text-sm font-medium text-text-primary">{name}</span>
      <span className="text-xs text-text-tertiary">{description}</span>
    </div>
  </div>
)

const VersionRow: React.FC<{ version: string; publishedAt: string; releaseNotes?: string }> = ({
  version, publishedAt, releaseNotes,
}) => (
  <div className="flex items-center gap-4 py-3 border-b border-border-subtle last:border-0">
    <span className="text-sm font-medium text-brand-500">{version}</span>
    <span className="text-xs text-text-tertiary">{timeAgo(publishedAt)}</span>
    <span className="text-sm text-text-secondary">{releaseNotes ?? '-'}</span>
  </div>
)

const AgentDetailPage: React.FC = () => {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const [agent, setAgent] = useState<AgentDetailVO | null>(null)
  const [versions, setVersions] = useState<AssetVersionVO[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [activeTab, setActiveTab] = useState('intro')
  const [isFavorite, setIsFavorite] = useState(false)

  useEffect(() => {
    if (!id) return
    ;(async () => {
      try {
        const [a, v] = await Promise.all([
          getAgent(id),
          listAgentVersions(id).catch(() => [] as AssetVersionVO[]),
        ])
        setAgent(a)
        setVersions(v)
      } catch (e) {
        setError(e instanceof ApiError ? e.message : 'Failed to load agent')
      } finally {
        setLoading(false)
      }
    })()
  }, [id])

  if (loading) {
    return (
      <Layout breadcrumb={[{ label: '智能体市场', path: '/market/agents' }, { label: '...' }]}>
        <div className="flex items-center justify-center h-64">
          <Loader2 className="w-6 h-6 animate-spin text-brand-500" />
        </div>
      </Layout>
    )
  }

  if (error || !agent) {
    return (
      <Layout breadcrumb={[{ label: '智能体市场', path: '/market/agents' }]}>
        <div className="p-8 text-center text-error-500">{error ?? 'Agent not found'}</div>
      </Layout>
    )
  }

  const tools = agent.toolBindings ?? []
  const reviewCount = 0

  const tabs = [
    { id: 'intro', label: '简介', active: activeTab === 'intro' },
    { id: 'tools', label: '工具能力', active: activeTab === 'tools' },
    { id: 'versions', label: '版本历史', active: activeTab === 'versions' },
  ]

  return (
    <Layout
      breadcrumb={[
        { label: '智能体市场', path: '/market/agents' },
        { label: agent.name },
      ]}
    >
      <div className="p-8 flex flex-col gap-5 h-full overflow-auto">
        <button
          onClick={() => navigate('/market/agents')}
          className="flex items-center gap-1 text-sm text-text-secondary hover:text-text-primary"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>返回市场</span>
        </button>

        <div className="p-7 rounded-2xl bg-white border border-border-subtle flex items-center gap-5">
          <div className="w-20 h-20 rounded-2xl bg-brand-50 flex items-center justify-center">
            <Bot className="w-10 h-10 text-brand-500" />
          </div>

          <div className="flex-1 flex flex-col gap-1.5">
            <div className="flex items-center gap-2.5">
              <h1 className="text-xl font-bold text-text-primary">{agent.name}</h1>
              {agent.currentVersion && <Badge variant="info">{agent.currentVersion}</Badge>}
              {agent.hasUnpublishedChanges && <Badge variant="info">有未发布变更</Badge>}
            </div>
            <div className="flex items-center gap-2.5 text-xs text-text-tertiary">
              <span>发布于 {timeAgo(agent.createdAt)}</span>
              <span>·</span>
              <span>状态: {agent.status === 'PUBLISHED' ? '已发布' : agent.status === 'DRAFT' ? '草稿' : '已归档'}</span>
            </div>
          </div>

          <div className="flex items-center gap-2.5">
            <Button
              variant="secondary"
              onClick={() => setIsFavorite(!isFavorite)}
              icon={<Heart className={`w-3.5 h-3.5 ${isFavorite ? 'fill-error-500 text-error-500' : ''}`} />}
            >
              收藏
            </Button>
            <Button
              variant="primary"
              onClick={() => navigate(`/chat?agentId=${agent.id}`)}
              icon={<ArrowRight className="w-3.5 h-3.5" />}
            >
              使用
            </Button>
          </div>
        </div>

        <div className="flex items-center gap-1 border-b border-border-subtle">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`px-4 py-2.5 text-sm ${
                tab.active
                  ? 'font-semibold text-brand-500 border-b-2 border-brand-500'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div className="flex gap-5 h-full">
          <div className="flex-1 p-6 rounded-xl bg-white border border-border-subtle flex flex-col gap-4">
            {activeTab === 'intro' && (
              <>
                <h2 className="text-base font-bold text-text-primary">产品简介</h2>
                <p className="text-sm text-text-secondary leading-relaxed">
                  {agent.description || '暂无简介'}
                </p>
                {agent.systemPrompt && (
                  <>
                    <h3 className="text-sm font-bold text-text-primary">系统提示词</h3>
                    <pre className="text-xs text-text-secondary bg-gray-50 p-3 rounded whitespace-pre-wrap">
                      {agent.systemPrompt}
                    </pre>
                  </>
                )}
                <div className="flex items-center gap-4 text-xs text-text-tertiary">
                  <span>最大步数: {agent.maxSteps}</span>
                  <span>模型: {agent.modelId || '默认'}</span>
                </div>
              </>
            )}

            {activeTab === 'tools' && (
              <>
                <h2 className="text-base font-bold text-text-primary">内置工具 ({tools.length})</h2>
                <div className="flex flex-col gap-2">
                  {tools.length === 0 && (
                    <p className="text-sm text-text-tertiary">暂无绑定的工具</p>
                  )}
                  {tools.map((tool) => (
                    <ToolCard
                      key={tool.id}
                      name={tool.toolName}
                      description={tool.sourceType}
                      icon="🔧"
                    />
                  ))}
                </div>
              </>
            )}

            {activeTab === 'versions' && (
              <>
                <h2 className="text-base font-bold text-text-primary">版本历史</h2>
                <div className="flex flex-col">
                  {versions.length === 0 && (
                    <p className="text-sm text-text-tertiary">暂无版本记录</p>
                  )}
                  {versions.map((v) => (
                    <VersionRow
                      key={v.id}
                      version={v.version}
                      publishedAt={v.publishedAt}
                      releaseNotes={v.releaseNotes}
                    />
                  ))}
                </div>
              </>
            )}
          </div>

          <div className="w-[360px] flex flex-col gap-4">
            <div className="p-5 rounded-xl bg-white border border-border-subtle flex flex-col gap-3">
              <h3 className="text-sm font-semibold text-text-primary">数据统计</h3>
              <div className="flex items-center gap-4">
                <StatCard label="使用次数" value="0" icon={<Users className="w-5 h-5" />} />
                <StatCard label="收藏数" value="0" icon={<Heart className="w-5 h-5" />} />
                <StatCard label="评价" value={`${reviewCount}`} icon={<Star className="w-5 h-5" />} />
              </div>
            </div>

            <div className="p-5 rounded-xl bg-white border border-border-subtle flex flex-col gap-2.5">
              <h3 className="text-sm font-semibold text-text-primary">标签</h3>
              <div className="flex items-center gap-1.5 flex-wrap">
                <span className="px-2 py-1 bg-gray-100 rounded text-xs text-text-secondary">
                  {agent.status === 'PUBLISHED' ? '已发布' : agent.status === 'DRAFT' ? '草稿' : '已归档'}
                </span>
                <span className="px-2 py-1 bg-gray-100 rounded text-xs text-text-secondary">
                  最大步数: {agent.maxSteps}
                </span>
              </div>
            </div>

            <div className="p-5 rounded-xl bg-white border border-border-subtle flex flex-col gap-2.5">
              <h3 className="text-sm font-semibold text-text-primary">内置工具 ({tools.length})</h3>
              {tools.slice(0, 3).map((tool) => (
                <div key={tool.id} className="flex items-center gap-2">
                  <span>🔧</span>
                  <span className="text-sm text-text-primary">{tool.toolName}</span>
                </div>
              ))}
              {tools.length === 0 && (
                <span className="text-xs text-text-tertiary">暂无工具</span>
              )}
            </div>
          </div>
        </div>
      </div>
    </Layout>
  )
}

export default AgentDetailPage
