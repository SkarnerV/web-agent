import React from 'react'
import { 
  LayoutDashboard, 
  MessageSquare, 
  Bot, 
  Wand, 
  Plug, 
  BookOpen,
  Store,
  Package,
  Blocks,
  Plus,
  Sparkles
} from 'lucide-react'

// Types
export interface NavItemProps {
  icon: React.ReactNode
  label: string
  active?: boolean
  onClick?: () => void
}

export interface NavSectionProps {
  title: string
}

// NavItem Component
export const NavItem: React.FC<NavItemProps> = ({ icon, label, active = false, onClick }) => {
  return (
    <button
      onClick={onClick}
      className={`
        flex items-center gap-3 w-[216px] px-3 py-2.5 rounded-md
        transition-colors duration-150
        ${active 
          ? 'bg-brand-50 text-brand-500' 
          : 'bg-transparent text-gray-700 hover:bg-gray-50'
        }
      `}
    >
      <span className={`w-[18px] h-[18px] ${active ? 'text-brand-500' : 'text-gray-600'}`}>
        {icon}
      </span>
      <span className={`flex-1 text-sm ${active ? 'font-semibold text-brand-500' : 'font-medium'}`}>
        {label}
      </span>
    </button>
  )
}

// NavSection Component
export const NavSection: React.FC<NavSectionProps> = ({ title }) => {
  return (
    <div className="px-3 pt-3 pb-1">
      <span className="text-xs font-semibold text-text-tertiary">{title}</span>
    </div>
  )
}

// Sidebar Props
export interface SidebarProps {
  activePath?: string
  onNavigate?: (path: string) => void
  onCreateClick?: () => void
}

// Sidebar Component
export const Sidebar: React.FC<SidebarProps> = ({ 
  activePath = '/dashboard', 
  onNavigate,
  onCreateClick
}) => {
  const handleNavClick = (path: string) => {
    onNavigate?.(path)
  }

  return (
    <aside className="w-[240px] h-full bg-white border-r border-border-subtle flex flex-col p-3">
      {/* Logo */}
      <div className="flex items-center gap-2 px-2 pt-2 pb-4">
        <div className="w-7 h-7 bg-brand-500 rounded-md flex items-center justify-center">
          <Sparkles className="w-4 h-4 text-white" />
        </div>
        <span className="text-[15px] font-semibold text-text-primary">Agent Platform</span>
      </div>

      {/* Main Navigation */}
      <NavItem 
        icon={<LayoutDashboard className="w-[18px] h-[18px]" />}
        label="首页"
        active={activePath === '/dashboard'}
        onClick={() => handleNavClick('/dashboard')}
      />
      <NavItem 
        icon={<MessageSquare className="w-[18px] h-[18px]" />}
        label="对话"
        active={activePath === '/chat'}
        onClick={() => handleNavClick('/chat')}
      />

      {/* My Assets Section */}
      <NavSection title="我的资产" />
      <NavItem 
        icon={<Bot className="w-[18px] h-[18px]" />}
        label="智能体"
        active={activePath.startsWith('/agents') && !activePath.includes('/market')}
        onClick={() => handleNavClick('/agents')}
      />
      <NavItem 
        icon={<Wand className="w-[18px] h-[18px]" />}
        label="Skill"
        active={activePath.startsWith('/skills') && !activePath.includes('/market')}
        onClick={() => handleNavClick('/skills')}
      />
      <NavItem 
        icon={<Plug className="w-[18px] h-[18px]" />}
        label="MCP"
        active={activePath.startsWith('/mcp') && !activePath.includes('/market')}
        onClick={() => handleNavClick('/mcp')}
      />
      <NavItem 
        icon={<BookOpen className="w-[18px] h-[18px]" />}
        label="知识库"
        active={activePath.startsWith('/knowledge')}
        onClick={() => handleNavClick('/knowledge')}
      />

      {/* Market Section */}
      <NavSection title="市场" />
      <NavItem 
        icon={<Store className="w-[18px] h-[18px]" />}
        label="智能体市场"
        active={activePath === '/market/agents'}
        onClick={() => handleNavClick('/market/agents')}
      />
      <NavItem 
        icon={<Package className="w-[18px] h-[18px]" />}
        label="Skill 市场"
        active={activePath === '/market/skills'}
        onClick={() => handleNavClick('/market/skills')}
      />
      <NavItem 
        icon={<Blocks className="w-[18px] h-[18px]" />}
        label="MCP 市场"
        active={activePath === '/market/mcp'}
        onClick={() => handleNavClick('/market/mcp')}
      />

      {/* Create Button */}
      <div className="pt-4">
        <button
          onClick={onCreateClick}
          className="flex items-center justify-center gap-2 w-full py-2.5 px-3 bg-brand-500 text-white rounded-md font-semibold text-sm hover:bg-brand-600 transition-colors"
        >
          <Plus className="w-4 h-4" />
          <span>创建</span>
        </button>
      </div>
    </aside>
  )
}

export default Sidebar