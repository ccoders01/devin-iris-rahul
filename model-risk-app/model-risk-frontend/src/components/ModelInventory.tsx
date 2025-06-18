import { useState, useEffect } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { RefreshCw, Database } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/services/api'
import { Model } from '@/types/model'

export default function ModelInventory() {
  const [models, setModels] = useState<Model[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isRefreshing, setIsRefreshing] = useState(false)

  const fetchModels = async (showRefreshToast = false) => {
    try {
      setIsRefreshing(true)
      const data = await api.getModels()
      setModels(data)
      if (showRefreshToast) {
        toast.success('Model inventory refreshed')
      }
    } catch (error) {
      toast.error('Failed to fetch models')
    } finally {
      setIsLoading(false)
      setIsRefreshing(false)
    }
  }

  useEffect(() => {
    fetchModels()
  }, [])

  const handleRefresh = () => {
    fetchModels(true)
  }

  const getRiskRatingColor = (rating: string) => {
    switch (rating.toLowerCase()) {
      case 'high':
        return 'destructive'
      case 'medium':
        return 'default'
      case 'low':
        return 'secondary'
      default:
        return 'outline'
    }
  }

  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case 'production':
        return 'default'
      case 'validated':
        return 'secondary'
      case 'in development':
        return 'outline'
      case 'retired':
        return 'destructive'
      default:
        return 'outline'
    }
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  if (isLoading) {
    return (
      <div className="max-w-7xl mx-auto">
        <Card>
          <CardHeader>
            <CardTitle className="text-2xl font-bold flex items-center space-x-2">
              <Database className="h-6 w-6" />
              <span>Model Inventory</span>
            </CardTitle>
            <CardDescription>Loading model inventory...</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex justify-center items-center h-32">
              <RefreshCw className="h-8 w-8 animate-spin text-gray-400" />
            </div>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="max-w-7xl mx-auto">
      <Card>
        <CardHeader>
          <div className="flex justify-between items-start">
            <div>
              <CardTitle className="text-2xl font-bold flex items-center space-x-2">
                <Database className="h-6 w-6" />
                <span>Model Inventory</span>
              </CardTitle>
              <CardDescription>
                {models.length === 0 
                  ? 'No models registered yet.' 
                  : `${models.length} model${models.length === 1 ? '' : 's'} registered in the system.`
                }
              </CardDescription>
            </div>
            <Button 
              onClick={handleRefresh} 
              disabled={isRefreshing}
              variant="outline"
              size="sm"
            >
              <RefreshCw className={`h-4 w-4 mr-2 ${isRefreshing ? 'animate-spin' : ''}`} />
              Refresh
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {models.length === 0 ? (
            <div className="text-center py-12">
              <Database className="h-12 w-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">No models registered yet</h3>
              <p className="text-gray-500 mb-4">
                Get started by registering your first model using the Model Registration form.
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Model ID</TableHead>
                    <TableHead>Model Name</TableHead>
                    <TableHead>Version</TableHead>
                    <TableHead>Sponsor</TableHead>
                    <TableHead>Business Line</TableHead>
                    <TableHead>Model Type</TableHead>
                    <TableHead>Risk Rating</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Created</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {models.map((model) => (
                    <TableRow key={model.model_id}>
                      <TableCell className="font-mono text-xs">
                        {model.model_id.substring(0, 8)}...
                      </TableCell>
                      <TableCell className="font-medium">{model.model_name}</TableCell>
                      <TableCell>{model.model_version}</TableCell>
                      <TableCell>{model.model_sponsor}</TableCell>
                      <TableCell>{model.business_line}</TableCell>
                      <TableCell>{model.model_type}</TableCell>
                      <TableCell>
                        <Badge variant={getRiskRatingColor(model.risk_rating)}>
                          {model.risk_rating}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <Badge variant={getStatusColor(model.status)}>
                          {model.status}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-sm text-gray-500">
                        {formatDate(model.created_at)}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
