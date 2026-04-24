import React, { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { 
  Plus, 
  Search, 
  MessageSquare, 
  Send, 
  Paperclip,
  ChevronDown,
  Bot,
  BookOpen,
  Settings2,
  Sparkles
} from 'lucide-react'
import { Sidebar } from '../components/layout/Sidebar'
import { Badge } from '../components/ui/Badge'

// Mock data for sessions
const sessionsData = {
  today: [
    { id: 's1', title: '如何优化 Agent 性能?', agent: '客服机器人', time: '10:30' },
    { id: 's2', title: '数据分析报告生成', agent: '数据分析助手', time: '09:15' },
  ],
  yesterday: [
    { id: 's3', title: 'API 接口文档编写', agent: '文档生成器', time: '14:20' },
    { id: 's4', title: '测试用例自动生成', agent: '测试助手', time: '11:45' },
  ],
  earlier: [
    { id: 's5', title: '项目进度跟踪方案', agent: '项目管理助手', time: '2天前' },
    { id: 's6', title: '代码审查反馈', agent: '代码审查专家', time: '3天前' },
  ],
}

// Mock messages
const messagesData: Array<{
  id: string
  type: 'user' | 'agent'
  content: string
  time: string
  avatar: string
  agentName?: string
}> = [
  { id: 'm1', type: 'user', content: '请问如何优化 Agent 的响应速度?', time: '10:30', avatar: '张' },
  { id: 'm2', type: 'agent', content: '优化 Agent 响应速度可以从以下几个方面入手：\n\n1. **减少知识库查询范围** - 精确匹配相关文档，避免全量搜索\n2. **优化 Prompt 设计** - 简化指令，减少不必要的推理步骤\n3. **调整模型参数** - 降低 temperature，使用更快的模型\n4. **缓存常见问题** - 对高频问题建立缓存机制\n\n您想深入了解哪个方面?', time: '10:31', agentName: '客服机器人', avatar: '客' },
  { id: 'm3', type: 'user', content: '我想了解缓存机制的实现方式', time: '10:35', avatar: '张' },
  { id: 'm4', type: 'agent', content: '缓存机制的实现可以分为以下步骤：\n\n### 基础缓存方案\n```typescript\ninterface CacheEntry {\n  query: string\n  response: string\n  timestamp: number\n  ttl: number // 过期时间\n}\n\nconst cache = new Map<string, CacheEntry>()\n```\n\n### 语义相似度匹配\n- 使用向量数据库存储问题向量\n- 通过相似度阈值匹配历史问答\n- 建议阈值设置在 0.85 以上\n\n是否需要我提供完整的代码示例?', time: '10:36', agentName: '客服机器人', avatar: '客' },
]

// Mock agents list
const agentsList = [
  { id: 'a1', name: '客服机器人', status: 'online' },
  { id: 'a2', name: '数据分析助手', status: 'offline' },
  { id: 'a3', name: '代码审查专家', status: 'online' },
]

// Current agent info
const currentAgent = {
  name: '客服机器人',
  description: '智能客服助手，自动处理用户咨询，支持多轮对话和意图识别。',
  status: '在线',
  knowledgeBase: ['产品文档', 'FAQ库', '操作手册'],
  tools: ['知识检索', '意图识别', '情感分析', '自动回复'],
  settings: {
    temperature: 0.7,
    maxTokens: 2048,
    topP: 0.9,
  },
}

// Session Item Component
interface SessionItemProps {
  id: string
  title: string
  agent: string
  time: string
  active?: boolean
  onClick?: () => void
}

const SessionItem: React.FC<SessionItemProps> = ({ title, agent, time, active, onClick }) => (
  <button
    onClick={onClick}
    className={`w-full p-2.5 rounded-lg text-left transition-colors ${
      active ? 'bg-brand-50' : 'hover:bg-gray-50'
    }`}
  >
    <div className="flex flex-col gap-1">
      <span className="text-sm font-medium text-text-primary truncate">{title}</span>
      <div className="flex items-center gap-2">
        <span className="text-xs text-text-tertiary">智能体: {agent}</span>
        <span className="text-xs text-text-tertiary">{time}</span>
      </div>
    </div>
  </button>
)

// Date Section Component
const DateSection: React.FC<{ title: string; children: React.ReactNode }> = ({ title, children }) => (
  <div className="flex flex-col gap-2">
    <span className="text-xs font-semibold text-text-tertiary px-1">{title}</span>
    {children}
  </div>
)

// Message Component
interface MessageProps {
  type: 'user' | 'agent'
  content: string
  time: string
  avatar: string
  agentName?: string
}

const Message: React.FC<MessageProps> = ({ type, content, time, avatar, agentName }) => (
  <div className={`flex gap-3 ${type === 'user' ? 'justify-end' : 'justify-start'}`}>
    {type === 'agent' && (
      <div className="w-8 h-8 rounded-full bg-brand-500 flex items-center justify-center shrink-0">
        <span className="text-sm font-semibold text-white">{avatar}</span>
      </div>
    )}
    <div className={`max-w-[70%] flex flex-col gap-1.5 ${
      type === 'user' ? 'items-end' : 'items-start'
    }`}>
      {type === 'agent' && agentName && (
        <span className="text-xs font-medium text-text-secondary">{agentName}</span>
      )}
      <div className={`p-3 rounded-xl ${
        type === 'user' 
          ? 'bg-brand-50 text-text-primary' 
          : 'bg-white border border-border-subtle text-text-primary'
      }`}>
        <div className="text-sm whitespace-pre-wrap">{content}</div>
      </div>
      <span className="text-xs text-text-tertiary">{time}</span>
    </div>
    {type === 'user' && (
      <div className="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center shrink-0">
        <span className="text-sm font-medium text-text-primary">{avatar}</span>
      </div>
    )}
  </div>
)

// Context Section Component
interface ContextSectionProps {
  icon: React.ReactNode
  title: string
  children: React.ReactNode
}

const ContextSection: React.FC<ContextSectionProps> = ({ icon, title, children }) => (
  <div className="flex flex-col gap-3">
    <div className="flex items-center gap-2">
      <span className="w-4 h-4 text-brand-500">{icon}</span>
      <span className="text-sm font-semibold text-text-primary">{title}</span>
    </div>
    <div className="flex flex-col gap-2 pl-6">{children}</div>
  </div>
)

// ChatPage Component
const ChatPage: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const [activeSession, setActiveSession] = useState('s1')
  const [selectedAgent, setSelectedAgent] = useState('客服机器人')
  const [showAgentDropdown, setShowAgentDropdown] = useState(false)
  const [messageInput, setMessageInput] = useState('')
  const [searchQuery, setSearchQuery] = useState('')

  const handleNavigate = (path: string) => {
    navigate(path)
  }

  const handleCreateClick = () => {
    navigate('/create')
  }

  const handleNewChat = () => {
    setActiveSession('')
  }

  const handleSendMessage = () => {
    if (messageInput.trim()) {
      console.log('Send message:', messageInput)
      setMessageInput('')
    }
  }

  return (
    <div className="h-screen w-screen flex bg-bg-canvas">
      {/* Sidebar */}
      <Sidebar 
        activePath={location.pathname}
        onNavigate={handleNavigate}
        onCreateClick={handleCreateClick}
      />
      
      {/* Main Content - Three Column Layout */}
      <div className="flex-1 flex">
        {/* Session List */}
        <div className="w-[260px] bg-white border-r border-border-subtle p-3 flex flex-col gap-3 overflow-y-auto">
          {/* New Button */}
          <button
            onClick={handleNewChat}
            className="flex items-center justify-center gap-2 w-full py-2.5 bg-brand-500 text-white rounded-lg font-semibold text-sm hover:bg-brand-600 transition-colors"
          >
            <Plus className="w-4 h-4" />
            <span>新建对话</span>
          </button>
          
          {/* Search Box */}
          <div className="flex items-center gap-2 px-2.5 py-2 bg-gray-100 rounded-md">
            <Search className="w-4 h-4 text-text-tertiary" />
            <input
              type="text"
              placeholder="搜索对话..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="flex-1 bg-transparent text-sm text-text-primary placeholder:text-text-tertiary outline-none"
            />
          </div>
          
          {/* Sessions by Date */}
          <div className="flex flex-col gap-4">
            <DateSection title="今天">
              {sessionsData.today.map((session) => (
                <SessionItem
                  key={session.id}
                  id={session.id}
                  title={session.title}
                  agent={session.agent}
                  time={session.time}
                  active={activeSession === session.id}
                  onClick={() => setActiveSession(session.id)}
                />
              ))}
            </DateSection>
            
            <DateSection title="昨天">
              {sessionsData.yesterday.map((session) => (
                <SessionItem
                  key={session.id}
                  id={session.id}
                  title={session.title}
                  agent={session.agent}
                  time={session.time}
                  active={activeSession === session.id}
                  onClick={() => setActiveSession(session.id)}
                />
              ))}
            </DateSection>
            
            <DateSection title="更早">
              {sessionsData.earlier.map((session) => (
                <SessionItem
                  key={session.id}
                  id={session.id}
                  title={session.title}
                  agent={session.agent}
                  time={session.time}
                  active={activeSession === session.id}
                  onClick={() => setActiveSession(session.id)}
                />
              ))}
            </DateSection>
          </div>
        </div>
        
        {/* Chat Area */}
        <div className="flex-1 flex flex-col bg-bg-canvas">
          {/* Top Bar */}
          <div className="h-14 bg-white border-b border-border-subtle px-8 flex items-center justify-between">
            <div className="flex items-center gap-4">
              {/* Agent Selector */}
              <div className="relative">
                <button
                  onClick={() => setShowAgentDropdown(!showAgentDropdown)}
                  className="flex items-center gap-2 px-3 py-2 bg-gray-100 rounded-md hover:bg-gray-50 transition-colors"
                >
                  <Bot className="w-4 h-4 text-brand-500" />
                  <span className="text-sm font-medium text-text-primary">{selectedAgent}</span>
                  <ChevronDown className="w-3.5 h-3.5 text-text-tertiary" />
                </button>
                
                {showAgentDropdown && (
                  <div className="absolute top-full left-0 mt-1 w-[180px] bg-white border border-border-subtle rounded-md shadow-lg z-10">
                    {agentsList.map((agent) => (
                      <button
                        key={agent.id}
                        onClick={() => {
                          setSelectedAgent(agent.name)
                          setShowAgentDropdown(false)
                        }}
                        className={`w-full px-3 py-2.5 text-left text-sm hover:bg-gray-50 ${
                          selectedAgent === agent.name ? 'bg-brand-50 text-brand-500' : 'text-text-primary'
                        }`}
                      >
                        <div className="flex items-center gap-2">
                          <Bot className="w-4 h-4" />
                          <span>{agent.name}</span>
                          {agent.status === 'online' && (
                            <span className="w-2 h-2 rounded-full bg-success-500" />
                          )}
                        </div>
                      </button>
                    ))}
                  </div>
                )}
              </div>
              
              {/* Status Badge */}
              <Badge variant="info">在线</Badge>
            </div>
            
            {/* Right side - optional */}
            <div className="flex items-center gap-2">
              <button className="w-9 h-9 flex items-center justify-center rounded-md hover:bg-gray-50 transition-colors">
                <MessageSquare className="w-4 h-4 text-gray-600" />
              </button>
            </div>
          </div>
          
          {/* Message Area */}
          <div className="flex-1 overflow-y-auto px-8 py-6 flex flex-col gap-4">
            {messagesData.map((msg) => (
              <Message
                key={msg.id}
                type={msg.type}
                content={msg.content}
                time={msg.time}
                avatar={msg.avatar}
                agentName={msg.agentName}
              />
            ))}
          </div>
          
          {/* Input Area */}
          <div className="px-8 pb-6 pt-4 bg-white border-t border-border-subtle">
            <div className="flex items-end gap-3">
              {/* Attachment Button */}
              <button className="w-9 h-9 flex items-center justify-center rounded-md text-gray-600 hover:bg-gray-50 transition-colors">
                <Paperclip className="w-4 h-4" />
              </button>
              
              {/* Input Box */}
              <div className="flex-1 flex items-center gap-2 px-4 py-3 bg-white border border-border-subtle rounded-lg">
                <textarea
                  value={messageInput}
                  onChange={(e) => setMessageInput(e.target.value)}
                  placeholder="输入消息..."
                  rows={1}
                  className="flex-1 bg-transparent text-sm text-text-primary placeholder:text-text-tertiary outline-none resize-none"
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && !e.shiftKey) {
                      e.preventDefault()
                      handleSendMessage()
                    }
                  }}
                />
              </div>
              
              {/* Send Button */}
              <button
                onClick={handleSendMessage}
                disabled={!messageInput.trim()}
                className={`w-9 h-9 flex items-center justify-center rounded-lg transition-colors ${
                  messageInput.trim()
                    ? 'bg-brand-500 text-white hover:bg-brand-600'
                    : 'bg-gray-100 text-gray-400'
                }`}
              >
                <Send className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
        
        {/* Context Panel */}
        <div className="w-[300px] bg-white border-l border-border-subtle p-4 flex flex-col gap-5 overflow-y-auto">
          {/* Current Agent Section */}
          <ContextSection icon={<Bot className="w-4 h-4" />} title="当前智能体">
            <div className="flex items-center gap-3 p-3 bg-brand-50 rounded-lg">
              <div className="w-10 h-10 rounded-full bg-brand-500 flex items-center justify-center">
                <span className="text-lg font-semibold text-white">{currentAgent.name[0]}</span>
              </div>
              <div className="flex flex-col gap-1">
                <span className="text-sm font-semibold text-text-primary">{currentAgent.name}</span>
                <span className="text-xs text-text-tertiary">{currentAgent.status}</span>
              </div>
            </div>
            <p className="text-xs text-text-secondary pl-0">{currentAgent.description}</p>
          </ContextSection>
          
          {/* Context Info Section */}
          <ContextSection icon={<BookOpen className="w-4 h-4" />} title="上下文信息">
            {/* Knowledge Base */}
            <div className="flex flex-col gap-1.5">
              <span className="text-xs font-medium text-text-secondary">关联知识库</span>
              <div className="flex flex-wrap gap-1.5">
                {currentAgent.knowledgeBase.map((kb) => (
                  <span key={kb} className="px-2 py-1 bg-gray-100 rounded-md text-xs text-text-secondary">
                    {kb}
                  </span>
                ))}
              </div>
            </div>
            
            {/* Tools */}
            <div className="flex flex-col gap-1.5">
              <span className="text-xs font-medium text-text-secondary">工具列表</span>
              <div className="flex flex-wrap gap-1.5">
                {currentAgent.tools.map((tool) => (
                  <span key={tool} className="px-2 py-1 bg-brand-50 text-brand-500 rounded-md text-xs font-medium">
                    {tool}
                  </span>
                ))}
              </div>
            </div>
          </ContextSection>
          
          {/* Session Settings Section */}
          <ContextSection icon={<Settings2 className="w-4 h-4" />} title="会话设置">
            <div className="flex flex-col gap-3">
              {/* Temperature */}
              <div className="flex items-center justify-between">
                <span className="text-xs text-text-secondary">Temperature</span>
                <span className="text-xs font-medium text-text-primary">{currentAgent.settings.temperature}</span>
              </div>
              
              {/* Max Tokens */}
              <div className="flex items-center justify-between">
                <span className="text-xs text-text-secondary">最大 Tokens</span>
                <span className="text-xs font-medium text-text-primary">{currentAgent.settings.maxTokens}</span>
              </div>
              
              {/* Top P */}
              <div className="flex items-center justify-between">
                <span className="text-xs text-text-secondary">Top P</span>
                <span className="text-xs font-medium text-text-primary">{currentAgent.settings.topP}</span>
              </div>
            </div>
          </ContextSection>
          
          {/* Footer - Agent Link */}
          <div className="pt-3 border-t border-border-subtle">
            <button
              onClick={() => navigate('/agents/1')}
              className="w-full flex items-center justify-center gap-2 py-2.5 text-brand-500 text-sm font-medium hover:bg-brand-50 rounded-md transition-colors"
            >
              <Sparkles className="w-4 h-4" />
              <span>查看智能体详情</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default ChatPage