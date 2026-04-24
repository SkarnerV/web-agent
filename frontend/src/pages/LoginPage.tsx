import React from 'react'
import { useNavigate } from 'react-router-dom'
import { Sparkles, ArrowRight } from 'lucide-react'

// LoginPage - Standalone login page (no Layout wrapper)
// Design spec from opus4.7.pen node pxMsT
const LoginPage: React.FC = () => {
  const navigate = useNavigate()

  const handleLogin = () => {
    // Navigate to dashboard after login
    navigate('/dashboard')
  }

  return (
    <div 
      className="h-screen w-screen flex flex-col justify-center items-center p-12"
      style={{
        background: 'linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%)'
      }}
    >
      {/* Card - 440px width, 48px padding, 16px rounded corners, shadow */}
      <div className="w-[440px] bg-white rounded-2xl shadow-card flex flex-col gap-6 p-12 items-center">
        {/* Logo Box */}
        <div className="flex items-center gap-2.5">
          {/* Icon - 44x44 brand-500 bg, 10px radius */}
          <div className="w-11 h-11 bg-brand-500 rounded-[10px] flex items-center justify-center">
            <Sparkles className="w-6 h-6 text-white" />
          </div>
          {/* Title - 22px, bold */}
          <span className="text-[22px] font-bold text-text-primary">
            Agent Platform
          </span>
        </div>

        {/* Text Wrapper */}
        <div className="flex flex-col gap-2 items-center">
          {/* Welcome text - 24px, bold */}
          <span className="text-2xl font-bold text-text-primary">
            欢迎使用
          </span>
          {/* Description - 13px, secondary color, centered */}
          <span className="w-[340px] text-[13px] text-text-secondary text-center leading-relaxed">
            构建企业级 AI 工作台，统一管理您的 AI 资产
          </span>
        </div>

        {/* Login Button - brand-500 bg, 10px radius, 14x20 padding */}
        <button
          onClick={handleLogin}
          className="w-full bg-brand-500 rounded-[10px] flex items-center justify-center gap-2.5 py-3.5 px-5 hover:bg-brand-600 transition-colors"
        >
          <span className="text-[15px] font-semibold text-white">
            使用 W3 账号登录
          </span>
          <ArrowRight className="w-4 h-4 text-white" />
        </button>

        {/* Agreement text - 12px, tertiary */}
        <span className="text-xs text-text-tertiary">
          登录即表示同意《使用协议》与《隐私政策》
        </span>
      </div>
    </div>
  )
}

export default LoginPage