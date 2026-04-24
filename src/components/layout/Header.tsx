import React from 'react'
import { Search, Bell, ChevronRight, ChevronDown } from 'lucide-react'

// Types
export interface BreadcrumbItem {
  label: string
  path?: string
}

export interface HeaderProps {
  breadcrumb?: BreadcrumbItem[]
  userName?: string
  userAvatar?: string
  onSearch?: (query: string) => void
  onNotificationClick?: () => void
  onUserClick?: () => void
}

// Header Component
export const Header: React.FC<HeaderProps> = ({
  breadcrumb = [{ label: '首页' }, { label: '当前页面' }],
  userName = '张三',
  userAvatar,
  onSearch,
  onNotificationClick,
  onUserClick
}) => {
  return (
    <header className="h-14 bg-white border-b border-border-subtle flex items-center justify-between px-6">
      {/* Breadcrumb */}
      <div className="flex items-center gap-2">
        {breadcrumb.map((item, index) => (
          <React.Fragment key={index}>
            <span className={`text-sm ${index === breadcrumb.length - 1 ? 'font-medium text-text-primary' : 'text-text-tertiary'}`}>
              {item.label}
            </span>
            {index < breadcrumb.length - 1 && (
              <ChevronRight className="w-3.5 h-3.5 text-text-tertiary" />
            )}
          </React.Fragment>
        ))}
      </div>

      {/* Right Section */}
      <div className="flex items-center gap-3">
        {/* Search */}
        <div className="flex items-center gap-2 px-3 py-2 bg-gray-100 rounded-md w-[240px]">
          <Search className="w-3.5 h-3.5 text-text-tertiary" />
          <input
            type="text"
            placeholder="搜索..."
            className="flex-1 bg-transparent text-sm text-text-primary placeholder:text-text-tertiary outline-none"
            onChange={(e) => onSearch?.(e.target.value)}
          />
        </div>

        {/* Notification */}
        <button
          onClick={onNotificationClick}
          className="w-9 h-9 flex items-center justify-center rounded-md hover:bg-gray-50 transition-colors"
        >
          <Bell className="w-4.5 h-4.5 text-gray-600" />
        </button>

        {/* User */}
        <button
          onClick={onUserClick}
          className="flex items-center gap-2 px-2.5 py-1 bg-gray-100 rounded-full"
        >
          <div className="w-7 h-7 bg-brand-500 rounded-full flex items-center justify-center">
            {userAvatar ? (
              <img src={userAvatar} alt={userName} className="w-full h-full rounded-full" />
            ) : (
              <span className="text-sm font-semibold text-white">{userName[0]}</span>
            )}
          </div>
          <span className="text-sm font-medium text-text-primary">{userName}</span>
          <ChevronDown className="w-3.5 h-3.5 text-text-tertiary" />
        </button>
      </div>
    </header>
  )
}

export default Header