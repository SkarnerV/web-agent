import React, { useState } from 'react'
import { Layout } from '../components/layout'
import { Button } from '../components/ui'
import { 
  Search, 
  ChevronDown, 
  Shield, 
  Users, 
  User, 
  Eye,
  MoreHorizontal,
  UserPlus,
} from 'lucide-react'

// Types
interface UserType {
  id: string
  name: string
  email: string
  avatar: string
  avatarBg: string
  department: string
  role: 'admin' | 'developer' | 'user' | 'observer'
  assetLimit: string
  status: 'active' | 'offline' | 'pending'
  lastActive?: string
}

interface StatCardType {
  label: string
  value: string | number
  subtext: string
  subtextColor?: string
}

interface RoleType {
  id: string
  name: string
  icon: React.ReactNode
  iconColor: string
  bgColor: string
  description: string
  userCount: number
}

// Mock data based on design spec
const statsData: StatCardType[] = [
  { label: '总用户', value: '248', subtext: '+12 本月新增', subtextColor: 'text-success-500' },
  { label: '活跃用户', value: '186', subtext: '75% 活跃率' },
  { label: '角色数', value: '4', subtext: '预设角色' },
  { label: '待审批', value: '7', subtext: '需要处理', subtextColor: 'text-warning-500' },
]

const rolesData: RoleType[] = [
  {
    id: 'admin',
    name: '管理员',
    icon: <Shield className="w-4 h-4" />,
    iconColor: 'text-error-500',
    bgColor: 'bg-brand-50',
    description: '全平台管理权限',
    userCount: 5,
  },
  {
    id: 'developer',
    name: '开发者',
    icon: <Users className="w-4 h-4" />,
    iconColor: 'text-info-500',
    bgColor: 'bg-info-50',
    description: '创建和发布资产',
    userCount: 42,
  },
  {
    id: 'user',
    name: '普通用户',
    icon: <User className="w-4 h-4" />,
    iconColor: 'text-success-500',
    bgColor: 'bg-success-50',
    description: '使用已发布资产',
    userCount: 189,
  },
  {
    id: 'observer',
    name: '观察者',
    icon: <Eye className="w-4 h-4" />,
    iconColor: 'text-text-secondary',
    bgColor: 'bg-gray-100',
    description: '仅查看权限',
    userCount: 12,
  },
]

const usersData: UserType[] = [
  {
    id: '1',
    name: '张三',
    email: 'zhangsan@company.com',
    avatar: '张',
    avatarBg: 'bg-brand-500',
    department: 'AI 平台组',
    role: 'admin',
    assetLimit: '不限',
    status: 'active',
  },
  {
    id: '2',
    name: '李四',
    email: 'lisi@company.com',
    avatar: '李',
    avatarBg: 'bg-green-500',
    department: '研发一部',
    role: 'developer',
    assetLimit: '50 智能体',
    status: 'active',
  },
  {
    id: '3',
    name: '王五',
    email: 'wangwu@company.com',
    avatar: '王',
    avatarBg: 'bg-yellow-500',
    department: '产品设计部',
    role: 'user',
    assetLimit: '10 智能体',
    status: 'offline',
  },
  {
    id: '4',
    name: '赵六',
    email: 'zhaoliu@company.com',
    avatar: '赵',
    avatarBg: 'bg-purple-500',
    department: '数据科学部',
    role: 'observer',
    assetLimit: '—',
    status: 'pending',
  },
]

// Role badge colors mapping
const roleStyles: Record<string, { bg: string; text: string; iconColor: string }> = {
  admin: { bg: 'bg-brand-50', text: 'text-error-500', iconColor: 'text-error-500' },
  developer: { bg: 'bg-info-50', text: 'text-info-500', iconColor: 'text-info-500' },
  user: { bg: 'bg-success-50', text: 'text-success-500', iconColor: 'text-success-500' },
  observer: { bg: 'bg-gray-100', text: 'text-text-secondary', iconColor: 'text-text-secondary' },
}

// Role display names
const roleNames: Record<string, string> = {
  admin: '管理员',
  developer: '开发者',
  user: '普通用户',
  observer: '观察者',
}

// Role icons
const roleIcons: Record<string, React.ReactNode> = {
  admin: <Shield className="w-2.75 h-2.75" />,
  developer: <Users className="w-2.75 h-2.75" />,
  user: <User className="w-2.75 h-2.75" />,
  observer: <Eye className="w-2.75 h-2.75" />,
}

// Status styles
const statusStyles: Record<string, { dot: string; text: string }> = {
  active: { dot: 'bg-success-500', text: 'text-text-secondary' },
  offline: { dot: 'bg-gray-400', text: 'text-text-tertiary' },
  pending: { dot: 'bg-warning-500', text: 'text-warning-500' },
}

// Status display names
const statusNames: Record<string, string> = {
  active: '活跃',
  offline: '离线',
  pending: '待审批',
}

// StatCard component
const StatCard: React.FC<{ stat: StatCardType }> = ({ stat }) => {
  return (
    <div className="flex-1 p-5 bg-white rounded-xl border border-border-subtle flex flex-col gap-1.5">
      <span className="text-xs text-text-tertiary">{stat.label}</span>
      <span className="text-[26px] font-bold text-text-primary">{stat.value}</span>
      <span className={`text-[11px] ${stat.subtextColor || 'text-text-tertiary'}`}>{stat.subtext}</span>
    </div>
  )
}

// RolePanel component
const RolePanel: React.FC = () => {
  return (
    <div className="w-[260px] p-[18px] bg-white rounded-xl border border-border-subtle flex flex-col gap-2.5">
      <span className="text-sm font-semibold text-text-primary">角色权限</span>
      {rolesData.map((role) => (
        <div key={role.id} className="flex items-center gap-2.5 p-2 rounded-lg hover:bg-bg-hover cursor-pointer">
          <div className={`w-8 h-8 rounded-lg ${role.bgColor} flex items-center justify-center ${role.iconColor}`}>
            {role.icon}
          </div>
          <div className="flex-1 flex flex-col">
            <span className="text-sm font-medium text-text-primary">{role.name}</span>
            <span className="text-xs text-text-tertiary">{role.userCount} 人</span>
          </div>
        </div>
      ))}
    </div>
  )
}

// UserRow component
const UserRow: React.FC<{ user: UserType }> = ({ user }) => {
  const roleStyle = roleStyles[user.role]
  const statusStyle = statusStyles[user.status]

  return (
    <div className="flex items-center gap-3.5 px-[18px] py-3 border-b border-border-subtle hover:bg-bg-hover">
      {/* User info */}
      <div className="flex-1 flex items-center gap-2.5">
        <div className={`w-8 h-8 rounded-full ${user.avatarBg} flex items-center justify-center text-white text-sm font-medium`}>
          {user.avatar}
        </div>
        <div className="flex flex-col gap-0.5">
          <span className="text-sm font-medium text-text-primary">{user.name}</span>
          <span className="text-xs text-text-tertiary">{user.email}</span>
        </div>
      </div>

      {/* Department */}
      <span className="text-xs text-text-secondary">{user.department}</span>

      {/* Role badge */}
      <div className={`w-[100px] px-2 py-0.5 rounded flex items-center gap-1 ${roleStyle.bg}`}>
        <span className={roleStyle.iconColor}>{roleIcons[user.role]}</span>
        <span className={`text-[11px] font-semibold ${roleStyle.text}`}>{roleNames[user.role]}</span>
      </div>

      {/* Asset limit */}
      <span className="text-xs text-text-secondary">{user.assetLimit}</span>

      {/* Status */}
      <div className="w-[80px] flex items-center gap-1.5">
        <div className={`w-1.5 h-1.5 rounded-sm ${statusStyle.dot}`}></div>
        <span className={`text-xs ${statusStyle.text}`}>
          {user.status === 'pending' ? (
            <span className="font-medium">{statusNames[user.status]}</span>
          ) : (
            statusNames[user.status]
          )}
        </span>
      </div>

      {/* Actions */}
      <div className="w-[80px] flex items-center gap-3">
        {user.status === 'pending' ? (
          <>
            <span className="text-xs font-semibold text-success-500 cursor-pointer hover:underline">通过</span>
            <span className="text-xs font-semibold text-error-500 cursor-pointer hover:underline">拒绝</span>
          </>
        ) : (
          <>
            <span className="text-xs font-medium text-brand-500 cursor-pointer hover:underline">编辑</span>
            <MoreHorizontal className="w-3.5 h-3.5 text-text-secondary cursor-pointer" />
          </>
        )}
      </div>
    </div>
  )
}

// UserPermissionPage component
const UserPermissionPage: React.FC = () => {
  const [searchQuery, setSearchQuery] = useState('')
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [roleFilter, _setRoleFilter] = useState('全部')
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [statusFilter, _setStatusFilter] = useState('全部')

  // Filter users based on search and filters
  const filteredUsers = usersData.filter((user) => {
    const matchesSearch = 
      searchQuery === '' || 
      user.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      user.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
      user.department.toLowerCase().includes(searchQuery.toLowerCase())
    
    const matchesRole = roleFilter === '全部' || roleNames[user.role] === roleFilter
    const matchesStatus = statusFilter === '全部' || statusNames[user.status] === statusFilter
    
    return matchesSearch && matchesRole && matchesStatus
  })

  return (
    <Layout breadcrumb={[
      { label: '管理员', path: '/admin' },
      { label: '用户与权限' },
    ]}>
      <div className="flex flex-col gap-5 h-full p-8">
        {/* Top section: Title + Add button */}
        <div className="flex items-center justify-between">
          <div className="flex flex-col gap-1">
            <span className="text-xl font-bold text-text-primary">用户与权限管理</span>
            <span className="text-sm text-text-secondary">管理平台用户、角色权限和资产配额</span>
          </div>
          <Button icon={<UserPlus className="w-4 h-4" />} variant="primary">
            添加用户
          </Button>
        </div>

        {/* Stats row */}
        <div className="flex gap-4">
          {statsData.map((stat, index) => (
            <StatCard key={index} stat={stat} />
          ))}
        </div>

        {/* Body: Role panel + User table */}
        <div className="flex gap-4 flex-1 min-h-0">
          {/* Role panel */}
          <RolePanel />

          {/* User table */}
          <div className="flex-1 flex flex-col bg-white rounded-xl border border-border-subtle overflow-hidden">
            {/* Toolbar */}
            <div className="flex items-center gap-2.5 p-4 border-b border-border-subtle">
              {/* Search */}
              <div className="flex-1 flex items-center gap-2 px-3 py-2 bg-white rounded-md border border-border-subtle">
                <Search className="w-3.5 h-3.5 text-text-tertiary" />
                <input
                  type="text"
                  placeholder="搜索姓名、邮箱或部门"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="flex-1 text-[13px] text-text-primary placeholder:text-text-tertiary outline-none"
                />
              </div>

              {/* Role filter */}
              <div className="flex items-center gap-1 px-2.5 py-1.5 bg-white rounded-md border border-border-strong cursor-pointer">
                <span className="text-xs text-text-secondary">角色：{roleFilter}</span>
                <ChevronDown className="w-3 h-3 text-text-tertiary" />
              </div>

              {/* Status filter */}
              <div className="flex items-center gap-1 px-2.5 py-1.5 bg-white rounded-md border border-border-strong cursor-pointer">
                <span className="text-xs text-text-secondary">状态：{statusFilter}</span>
                <ChevronDown className="w-3 h-3 text-text-tertiary" />
              </div>
            </div>

            {/* Table header */}
            <div className="flex items-center gap-3.5 px-[18px] py-2.5 bg-gray-50 border-b border-border-subtle">
              <span className="flex-1 text-[11px] font-semibold text-text-tertiary">用户</span>
              <span className="text-[11px] font-semibold text-text-tertiary">部门</span>
              <span className="w-[100px] text-[11px] font-semibold text-text-tertiary">角色</span>
              <span className="text-[11px] font-semibold text-text-tertiary">资产限额</span>
              <span className="w-[80px] text-[11px] font-semibold text-text-tertiary">状态</span>
              <span className="w-[80px] text-[11px] font-semibold text-text-tertiary">操作</span>
            </div>

            {/* Table body */}
            <div className="flex-1 overflow-auto">
              {filteredUsers.map((user) => (
                <UserRow key={user.id} user={user} />
              ))}
            </div>
          </div>
        </div>
      </div>
    </Layout>
  )
}

export default UserPermissionPage