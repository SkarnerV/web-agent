import React from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { Header } from './Header'

// Types
export interface LayoutProps {
  children: React.ReactNode
  breadcrumb?: Array<{ label: string; path?: string }>
  hideSidebar?: boolean
}

// Layout Component - wraps pages with sidebar and header
export const Layout: React.FC<LayoutProps> = ({
  children,
  breadcrumb,
  hideSidebar = false,
}) => {
  const navigate = useNavigate()
  const location = useLocation()

  const handleNavigate = (path: string) => {
    navigate(path)
  }

  const handleCreateClick = () => {
    navigate('/create')
  }

  if (hideSidebar) {
    return <div className="h-screen w-screen">{children}</div>
  }

  return (
    <div className="h-screen w-screen flex bg-bg-canvas">
      {/* Sidebar */}
      <Sidebar 
        activePath={location.pathname}
        onNavigate={handleNavigate}
        onCreateClick={handleCreateClick}
      />
      
      {/* Main Content */}
      <div className="flex-1 flex flex-col">
        {/* Header */}
        <Header 
          breadcrumb={breadcrumb}
          userName="张三"
          onSearch={(query) => console.log('Search:', query)}
          onNotificationClick={() => console.log('Notifications')}
          onUserClick={() => console.log('User menu')}
        />
        
        {/* Page Content */}
        <main className="flex-1 overflow-auto">
          {children}
        </main>
      </div>
    </div>
  )
}

export default Layout