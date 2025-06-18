import { Link, useLocation } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { FileText, Database } from 'lucide-react'

export default function Navigation() {
  const location = useLocation()

  return (
    <nav className="bg-white shadow-sm border-b">
      <div className="container mx-auto px-4">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center space-x-4">
            <h1 className="text-xl font-semibold text-gray-900">
              Model Risk Management
            </h1>
          </div>
          
          <div className="flex items-center space-x-4">
            <Link to="/registration">
              <Button
                variant={location.pathname === '/registration' || location.pathname === '/' ? 'default' : 'outline'}
                className="flex items-center space-x-2"
              >
                <FileText className="h-4 w-4" />
                <span>Model Registration</span>
              </Button>
            </Link>
            
            <Link to="/inventory">
              <Button
                variant={location.pathname === '/inventory' ? 'default' : 'outline'}
                className="flex items-center space-x-2"
              >
                <Database className="h-4 w-4" />
                <span>Model Inventory</span>
              </Button>
            </Link>
          </div>
        </div>
      </div>
    </nav>
  )
}
