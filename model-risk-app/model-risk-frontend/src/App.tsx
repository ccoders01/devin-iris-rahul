import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { Toaster } from '@/components/ui/sonner'
import Navigation from '@/components/Navigation'
import ModelRegistration from '@/components/ModelRegistration'
import ModelInventory from '@/components/ModelInventory'
import './App.css'

function App() {
  return (
    <Router>
      <div className="min-h-screen bg-gray-50">
        <Navigation />
        <main className="container mx-auto px-4 py-8">
          <Routes>
            <Route path="/" element={<ModelRegistration />} />
            <Route path="/registration" element={<ModelRegistration />} />
            <Route path="/inventory" element={<ModelInventory />} />
          </Routes>
        </main>
        <Toaster />
      </div>
    </Router>
  )
}

export default App
