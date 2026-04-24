import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'

// Pages
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import ChatPage from './pages/ChatPage'
import AgentListPage from './pages/AgentListPage'
import AgentCreatePage from './pages/AgentCreatePage'
import AgentEditPage from './pages/AgentEditPage'
import AgentCreateToolsPage from './pages/AgentCreateToolsPage'
import AgentCreateCollabPage from './pages/AgentCreateCollabPage'
import AgentCreatePublishPage from './pages/AgentCreatePublishPage'
import AgentDebugPage from './pages/AgentDebugPage'
import AgentMarketPage from './pages/AgentMarketPage'
import AgentDetailPage from './pages/AgentDetailPage'
import SkillListPage from './pages/SkillListPage'
import SkillCreatePage from './pages/SkillCreatePage'
import SkillMarketPage from './pages/SkillMarketPage'
import MCPListPage from './pages/MCPListPage'
import MCPAddPage from './pages/MCPAddPage'
import MCPMarketPage from './pages/MCPMarketPage'
import KnowledgeBasePage from './pages/KnowledgeBasePage'
import KBDocumentsPage from './pages/KBDocumentsPage'
import CreationWizardPage from './pages/CreationWizardPage'
import UserPermissionPage from './pages/UserPermissionPage'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Level 0 - Entry */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        
        {/* Level 1 - Main Navigation */}
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/chat" element={<ChatPage />} />
        <Route path="/agents" element={<AgentListPage />} />
        <Route path="/skills" element={<SkillListPage />} />
        <Route path="/mcp" element={<MCPListPage />} />
        <Route path="/knowledge" element={<KnowledgeBasePage />} />
        
        {/* Level 1 - Markets */}
        <Route path="/market/agents" element={<AgentMarketPage />} />
        <Route path="/market/agents/:id" element={<AgentDetailPage />} />
        <Route path="/market/skills" element={<SkillMarketPage />} />
        <Route path="/market/mcp" element={<MCPMarketPage />} />
        
        {/* Level 2 - Feature Operations */}
        <Route path="/agents/create" element={<AgentCreatePage />} />
        <Route path="/agents/edit/:id" element={<AgentEditPage />} />
        <Route path="/agents/tools" element={<AgentCreateToolsPage />} />
        <Route path="/agents/collab" element={<AgentCreateCollabPage />} />
        <Route path="/agents/publish" element={<AgentCreatePublishPage />} />
        <Route path="/agents/debug/:id" element={<AgentDebugPage />} />
        <Route path="/agents/:id" element={<AgentDetailPage />} />
        
        <Route path="/skills/create" element={<SkillCreatePage />} />
        <Route path="/mcp/add" element={<MCPAddPage />} />
        <Route path="/knowledge/:id" element={<KBDocumentsPage />} />
        
        {/* Level 3 - Creation Wizard */}
        <Route path="/create" element={<CreationWizardPage />} />
        
        {/* Admin */}
        <Route path="/admin/permissions" element={<UserPermissionPage />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App