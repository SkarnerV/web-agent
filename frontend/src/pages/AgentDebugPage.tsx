import React, { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ChevronLeft, Check, Send, AlertTriangle, Clock, Wrench, BarChart3, Bot, User } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { Badge } from '../components/ui/Badge'

// Mock agent data
const mockAgentData = {
  id: '1',
  name: '数据分析助手',
  status: 'debugging' as const,
}

// Mock chat messages
const mockMessages: Array<{ id: string; role: 'user' | 'agent'; content: string }> = [
  { id: '1', role: 'user', content: '帮我分析一下上个月的销售数据' },
  { id: '2', role: 'agent', content: '好的，我来帮您分析上个月的销售数据。请问您需要关注哪些维度？比如：销售额、订单量、客户数、客单价等？' },
  { id: '3', role: 'user', content: '重点关注销售额和订单量的趋势' },
  { id: '4', role: 'agent', content: '根据数据分析，上个月的销售情况如下：\n\n**销售额**: ¥1,234,567，环比增长12.5%\n**订单量**: 8,901单，环比增长8.3%\n\n主要增长来源：\n- 新客户贡献占比35%\n- 老客户复购占比65%\n\n建议：可以继续优化老客户复购策略。' },
]

// Mock tool call records
const mockToolCalls = [
  { id: '1', tool: '数据库查询', time: '2秒前', status: 'success' as const },
  { id: '2', tool: '数据分析', time: '5秒前', status: 'success' as const },
  { id: '3', tool: '图表生成', time: '8秒前', status: 'pending' as const },
]

// Mock performance metrics
const mockMetrics = {
  responseTime: '1.2s',
  toolCalls: 3,
  tokenUsage: 256,
  successRate: '100%',
}

// Chat message component
const ChatMessage: React.FC<{
  role: 'user' | 'agent'
  content: string
}> = ({ role, content }) => {
  return (
    <div className={`flex gap-3 ${role === 'user' ? 'flex-row-reverse' : ''}`}>
      {/* Avatar */}
      <div className={`w-8 h-8 rounded-full flex items-center justify-center ${
        role === 'agent'
          ? 'bg-brand-500'
          : 'bg-gray-200'
      }`}>
        {role === 'agent' ? (
          <Bot className="w-4 h-4 text-white" />
        ) : (
          <User className="w-4 h-4 text-gray-600" />
        )}
      </div>
      {/* Message content */}
      <div className={`max-w-[80%] p-3 rounded-lg ${
        role === 'agent'
          ? 'bg-white border border-border-subtle'
          : 'bg-brand-500 text-white'
      }`}>
        <span className={`text-sm ${
          role === 'user' ? 'text-white' : 'text-text-primary'
        } whitespace-pre-wrap`}>
          {content}
        </span>
      </div>
    </div>
  )
}

// Tool call record component
const ToolCallRecord: React.FC<{
  tool: string
  time: string
  status: 'success' | 'pending' | 'error'
}> = ({ tool, time, status }) => {
  const statusConfig = {
    success: { bg: 'bg-success-50', color: 'text-success-500', icon: Check },
    pending: { bg: 'bg-warning-50', color: 'text-warning-500', icon: Clock },
    error: { bg: 'bg-error-50', color: 'text-error-500', icon: AlertTriangle },
  }

  const { bg, color, icon: IconComponent } = statusConfig[status]

  return (
    <div className="flex items-center gap-3 p-3 rounded-lg bg-gray-50">
      <div className={`w-6 h-6 rounded-full ${bg} flex items-center justify-center`}>
        <IconComponent className={`w-3 h-3 ${color}`} />
      </div>
      <div className="flex-1 flex flex-col gap-0.5">
        <span className="text-sm font-medium text-text-primary">{tool}</span>
        <span className="text-xs text-text-tertiary">{time}</span>
      </div>
      <Badge variant={status === 'success' ? 'published' : status === 'pending' ? 'debugging' : 'error'}>
        {status === 'success' ? '完成' : status === 'pending' ? '进行中' : '失败'}
      </Badge>
    </div>
  )
}

// Metric card component
const MetricCard: React.FC<{
  label: string
  value: string
  icon: React.ReactNode
}> = ({ label, value, icon }) => {
  return (
    <div className="p-3 rounded-lg bg-gray-50 flex items-center gap-3">
      <div className="w-8 h-8 rounded-lg bg-brand-50 flex items-center justify-center">
        {icon}
      </div>
      <div className="flex-1 flex flex-col gap-0.5">
        <span className="text-xs text-text-tertiary">{label}</span>
        <span className="text-sm font-semibold text-text-primary">{value}</span>
      </div>
    </div>
  )
}

// AgentDebugPage Component
const AgentDebugPage: React.FC = () => {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const [messages] = useState(mockMessages)
  const [inputValue, setInputValue] = useState('')
  const [toolCalls] = useState(mockToolCalls)

  const handleBack = () => {
    navigate(`/agents/edit/${id}`)
  }

  const handlePass = () => {
    console.log('Agent passed debug')
    navigate('/agents')
  }

  const handleSendMessage = () => {
    if (inputValue.trim()) {
      console.log('Send message:', inputValue)
      setInputValue('')
    }
  }

  return (
    <Layout breadcrumb={[{ label: '我的资产' }, { label: '智能体' }, { label: '调试' }]}>
      {/* TopBar */}
      <div className="h-14 bg-white border-b border-border-subtle flex items-center justify-between px-6">
        <div className="flex items-center gap-3">
          <button
            onClick={handleBack}
            className="flex items-center gap-1 text-text-secondary hover:text-text-primary transition-colors"
          >
            <ChevronLeft className="w-4 h-4" />
            <span className="text-sm">返回编辑</span>
          </button>
        </div>
        <div className="flex items-center gap-2 flex-1 justify-center">
          <span className="text-sm font-semibold text-text-primary">调试：{mockAgentData.name}</span>
          <Badge variant="debugging">调试中</Badge>
        </div>
        <Button
          variant="primary"
          icon={<Check className="w-4 h-4" />}
          onClick={handlePass}
          className="bg-success-500 hover:bg-green-600"
        >
          通过并发布
        </Button>
      </div>

      {/* Body */}
      <div className="flex-1 flex overflow-hidden">
        {/* Debug Chat */}
        <div className="flex-1 flex flex-col bg-gray-50">
          {/* Warning banner */}
          <div className="p-3 mx-6 my-4 rounded-lg bg-warning-50 border border-warning-500 flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-warning-500" />
            <span className="text-sm text-warning-500">当前处于调试模式，部分功能可能不稳定</span>
          </div>

          {/* Messages area */}
          <div className="flex-1 overflow-auto p-6 flex flex-col gap-4">
            {messages.map((msg) => (
              <ChatMessage key={msg.id} role={msg.role} content={msg.content} />
            ))}
          </div>

          {/* Input area */}
          <div className="p-4 px-6 bg-white border-t border-border-subtle">
            <div className="flex items-center gap-2">
              <input
                type="text"
                placeholder="输入消息进行测试..."
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                className="flex-1 p-2.5 px-3 bg-gray-50 rounded-md text-sm text-text-primary placeholder:text-text-tertiary outline-none border border-border-subtle focus:border-brand-500"
              />
              <Button
                variant="primary"
                icon={<Send className="w-4 h-4" />}
                onClick={handleSendMessage}
                disabled={!inputValue.trim()}
              >
                发送
              </Button>
            </div>
          </div>
        </div>

        {/* Monitor Panel */}
        <div className="w-[400px] bg-white border-l border-border-subtle flex flex-col gap-4 p-5 overflow-auto">
          <span className="text-sm font-semibold text-text-primary">运行过程监控</span>

          {/* Metrics */}
          <div className="flex flex-col gap-2">
            <MetricCard label="响应时间" value={mockMetrics.responseTime} icon={<Clock className="w-4 h-4 text-brand-500" />} />
            <MetricCard label="工具调用" value={`${mockMetrics.toolCalls} 次`} icon={<Wrench className="w-4 h-4 text-brand-500" />} />
            <MetricCard label="Token 使用" value={`${mockMetrics.tokenUsage}`} icon={<BarChart3 className="w-4 h-4 text-brand-500" />} />
            <MetricCard label="成功率" value={mockMetrics.successRate} icon={<Check className="w-4 h-4 text-success-500" />} />
          </div>

          {/* Tool calls */}
          <div className="flex flex-col gap-2">
            <span className="text-xs font-semibold text-text-tertiary">工具调用记录</span>
            {toolCalls.map((call) => (
              <ToolCallRecord
                key={call.id}
                tool={call.tool}
                time={call.time}
                status={call.status}
              />
            ))}
          </div>

          {/* Performance chart placeholder */}
          <div className="flex flex-col gap-2">
            <span className="text-xs font-semibold text-text-tertiary">性能趋势</span>
            <div className="h-[120px] rounded-lg bg-gray-50 border border-border-subtle flex items-center justify-center">
              <span className="text-xs text-text-tertiary">图表占位</span>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  )
}

export default AgentDebugPage