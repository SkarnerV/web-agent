import React, { useState, useEffect } from 'react'
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
  Sparkles,
  ChevronsLeft,
  ChevronsRight
} from 'lucide-react'

export interface NavItemProps {
  icon: React.ReactNode
  label: string
  active?: boolean
  collapsed?: boolean
  onClick?: () => void
}

export interface NavSectionProps {
  title: string
  collapsed?: boolean
}

export const NavItem: React.FC<NavItemProps> = ({ icon, label, active = false, collapsed = false, onClick }) => {
  return (
    <button
      onClick={onClick}
      title={collapsed ? label : undefined}
      className={`
        flex items-center rounded-md transition-colors duration-150
        ${collapsed ? 'justify-center w-10 h-10 mx-auto' : 'gap-3 w-full px-3 py-2.5'}
        ${active 
          ? 'bg-brand-50 text-brand-500' 
          : 'bg-transparent text-gray-700 hover:bg-gray-50'
        }
      `}
    >
      <span className={`w-[18px] h-[18px] flex-shrink-0 ${active ? 'text-brand-500' : 'text-gray-600'}`}>
        {icon}
      </span>
      {!collapsed && (
        <span className={`flex-1 text-sm ${active ? 'font-semibold text-brand-500' : 'font-medium'}`}>
          {label}
        </span>
      )}
    </button>
  )
}

export const NavSection: React.FC<NavSectionProps> = ({ title, collapsed = false }) => {
  if (collapsed) {
    return <div className="my-2 mx-2 h-px bg-border-subtle" />
  }
  return (
    <div className="px-3 pt-3 pb-1">
      <span className="text-xs font-semibold text-text-tertiary">{title}</span>
    </div>
  )
}

export interface SidebarProps {
  activePath?: string
  onNavigate?: (path: string) => void
  onCreateClick?: () => void
}

export const Sidebar: React.FC<SidebarProps> = ({ 
  activePath = '/dashboard', 
  onNavigate,
  onCreateClick
}) => {
  const [collapsed, setCollapsed] = useState(() => window.innerWidth < 1024)

  useEffect(() => {
    const mql = window.matchMedia('(max-width: 1023px)')
    const handler = (e: MediaQueryListEvent) => setCollapsed(e.matches)
    mql.addEventListener('change', handler)
    return () => mql.removeEventListener('change', handler)
  }, [])

  const handleNavClick = (path: string) => {
    onNavigate?.(path)
  }

  return (
    <aside className={`${collapsed ? 'w-14' : 'w-60'} h-full bg-white border-r border-border-subtle flex flex-col p-3 transition-[width] duration-200`}>
      {/* Logo */}
      <div className="flex items-center gap-2 px-2 pt-2 pb-4">
        <div className="w-7 h-7 bg-brand-500 rounded-md flex items-center justify-center flex-shrink-0">
          <Sparkles className="w-4 h-4 text-white" />
        </div>
        {!collapsed && <span className="text-[15px] font-semibold text-text-primary">Agent Platform</span>}
      </div>

      {/* Main Navigation */}
      <NavItem 
        icon={<LayoutDashboard className="w-[18px] h-[18px]" />}
        label="首页"
        active={activePath === '/dashboard'}
        collapsed={collapsed}
        onClick={() => handleNavClick('/dashboard')}
      />
      <NavItem 
        icon={<MessageSquare className="w-[18px] h-[18px]" />}
        label="对话"
        active={activePath === '/chat'}
        collapsed={collapsed}
        onClick={() => handleNavClick('/chat')}
      />

      {/* My Assets Section */}
      <NavSection title="我的资产" collapsed={collapsed} />
      <NavItem 
        icon={<Bot className="w-[18px] h-[18px]" />}
        label="智能体"
        active={activePath.startsWith('/agents') && !activePath.includes('/market')}
        collapsed={collapsed}
        onClick={() => handleNavClick('/agents')}
      />
      <NavItem 
        icon={<Wand className="w-[18px] h-[18px]" />}
        label="Skill"
        active={activePath.startsWith('/skills') && !activePath.includes('/market')}
        collapsed={collapsed}
        onClick={() => handleNavClick('/skills')}
      />
      <NavItem 
        icon={<Plug className="w-[18px] h-[18px]" />}
        label="MCP"
        active={activePath.startsWith('/mcp') && !activePath.includes('/market')}
        collapsed={collapsed}
        onClick={() => handleNavClick('/mcp')}
      />
      <NavItem 
        icon={<BookOpen className="w-[18px] h-[18px]" />}
        label="知识库"
        active={activePath.startsWith('/knowledge')}
        collapsed={collapsed}
        onClick={() => handleNavClick('/knowledge')}
      />

      {/* Market Section */}
      <NavSection title="市场" collapsed={collapsed} />
      <NavItem 
        icon={<Store className="w-[18px] h-[18px]" />}
        label="智能体市场"
        active={activePath === '/market/agents'}
        collapsed={collapsed}
        onClick={() => handleNavClick('/market/agents')}
      />
      <NavItem 
        icon={<Package className="w-[18px] h-[18px]" />}
        label="Skill 市场"
        active={activePath === '/market/skills'}
        collapsed={collapsed}
        onClick={() => handleNavClick('/market/skills')}
      />
      <NavItem 
        icon={<Blocks className="w-[18px] h-[18px]" />}
        label="MCP 市场"
        active={activePath === '/market/mcp'}
        collapsed={collapsed}
        onClick={() => handleNavClick('/market/mcp')}
      />

      <div className="flex-1" />

      {/* Create Button */}
      <div className="pt-4">
        <button
          onClick={onCreateClick}
          title={collapsed ? '创建' : undefined}
          className="flex items-center justify-center gap-2 w-full py-2.5 px-3 bg-brand-500 text-white rounded-md font-semibold text-sm hover:bg-brand-600 transition-colors"
        >
          <Plus className="w-4 h-4 flex-shrink-0" />
          {!collapsed && <span>创建</span>}
        </button>
      </div>

      {/* Collapse Toggle */}
      <div className="pt-2">
        <button
          onClick={() => setCollapsed(prev => !prev)}
          className="flex items-center justify-center w-full py-2 rounded-md text-gray-500 hover:bg-gray-50 transition-colors"
        >
          {collapsed ? <ChevronsRight className="w-4 h-4" /> : <ChevronsLeft className="w-4 h-4" />}
        </button>
      </div>
    </aside>
  )
}

export default Sidebar
