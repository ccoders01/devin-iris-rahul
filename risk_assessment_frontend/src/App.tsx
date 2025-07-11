import { useState, useEffect } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ResponsiveContainer, PieChart, Pie, Cell, Tooltip, LineChart, Line, XAxis, YAxis, CartesianGrid, BarChart, Bar, Area, AreaChart } from 'recharts'
import { Activity, TrendingUp, TrendingDown, Users, Building2, AlertTriangle, Bot, RefreshCw } from 'lucide-react'

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8000'

interface DashboardData {
  total_borrowers: number
  total_institutions: number
  total_exposure: number
  average_risk_score: number
  risk_distribution: Record<string, number>
  recent_activities: any[]
  agent_status: Record<string, any>
  last_updated: string
}

interface Institution {
  institution_id: string
  name: string
  type: string
  borrower_count: number
  status: string
  last_sync: string
}

interface Borrower {
  borrower_id: string
  name: string
  credit_score: number
  debt_to_income_ratio: number
  annual_income: number
  loan_amount: number
  risk_score?: {
    overall_score: number
    risk_level: string
    confidence: number
    trend: string
  }
}

interface Agent {
  agent_id: string
  status: string
  institutions: string[]
  last_activity: string
}

interface PredictiveModels {
  model_type: string
  accuracy_score: number
  r2_score: number
  rmse: number
  feature_importance: Record<string, number>
  predictions: Array<{
    date: string
    predicted_risk_score: number
    confidence_lower: number
    confidence_upper: number
  }>
  confidence_intervals: Array<{
    period: string
    predictions: Array<{
      date: string
      predicted_risk_score: number
      confidence_lower: number
      confidence_upper: number
    }>
  }>
}

interface StressTestResults {
  stress_test_results: Array<{
    scenario_name: string
    scenario_description: string
    baseline_var: number
    stressed_var: number
    impact_percentage: number
    affected_borrowers: number
    risk_distribution_change: Record<string, number>
  }>
  concentration_analysis: Record<string, number>
  portfolio_metrics: {
    total_exposure: number
    number_of_institutions: number
    largest_exposure_pct: number
  }
}

interface TrendAnalysis {
  historical_trends: Array<{
    date: string
    average_risk_score: number
    risk_distribution: Record<string, number>
    total_exposure: number
    volatility: number
  }>
  seasonal_patterns: Record<string, number>
  forecast_30_days: Array<{
    date: string
    predicted_risk_score: number
    confidence_lower: number
    confidence_upper: number
  }>
  forecast_60_days: Array<{
    date: string
    predicted_risk_score: number
    confidence_lower: number
    confidence_upper: number
  }>
  forecast_90_days: Array<{
    date: string
    predicted_risk_score: number
    confidence_lower: number
    confidence_upper: number
  }>
  trend_direction: string
  volatility_index: number
}

interface RiskFactors {
  factor_importance: Record<string, number>
  correlation_matrix: Record<string, Record<string, number>>
  sensitivity_analysis: Record<string, number>
  concentration_risks: Record<string, number>
}

function App() {
  const [dashboardData, setDashboardData] = useState<DashboardData | null>(null)
  const [institutions, setInstitutions] = useState<Institution[]>([])
  const [borrowers, setBorrowers] = useState<Borrower[]>([])
  const [agents, setAgents] = useState<Record<string, Agent>>({})
  const [predictiveModels, setPredictiveModels] = useState<PredictiveModels | null>(null)
  const [stressTests, setStressTests] = useState<StressTestResults | null>(null)
  const [trendAnalysis, setTrendAnalysis] = useState<TrendAnalysis | null>(null)
  const [riskFactors, setRiskFactors] = useState<RiskFactors | null>(null)
  const [loading, setLoading] = useState(true)
  const [analyticsLoading, setAnalyticsLoading] = useState(false)
  const [forecastPeriod, setForecastPeriod] = useState<'30' | '60' | '90'>('30')
  const [activeTab, setActiveTab] = useState('dashboard')

  const fetchData = async () => {
    try {
      const [dashboardRes, institutionsRes, borrowersRes, agentsRes] = await Promise.all([
        fetch(`${API_BASE_URL}/api/dashboard`),
        fetch(`${API_BASE_URL}/api/institutions`),
        fetch(`${API_BASE_URL}/api/borrowers?limit=50`),
        fetch(`${API_BASE_URL}/api/agents`)
      ])

      const dashboardData = await dashboardRes.json()
      const institutionsData = await institutionsRes.json()
      const borrowersData = await borrowersRes.json()
      const agentsData = await agentsRes.json()

      setDashboardData(dashboardData)
      setInstitutions(institutionsData)
      setBorrowers(borrowersData)
      setAgents(agentsData)
      setLoading(false)
    } catch (error) {
      console.error('Error fetching data:', error)
      setLoading(false)
    }
  }

  const fetchAnalyticsData = async () => {
    try {
      setAnalyticsLoading(true)
      const [predictiveRes, stressTestRes, trendRes, riskFactorsRes] = await Promise.all([
        fetch(`${API_BASE_URL}/api/analytics/predictive-models`),
        fetch(`${API_BASE_URL}/api/analytics/stress-testing`),
        fetch(`${API_BASE_URL}/api/analytics/trend-analysis`),
        fetch(`${API_BASE_URL}/api/analytics/risk-factors`)
      ])

      const predictiveData = await predictiveRes.json()
      const stressTestData = await stressTestRes.json()
      const trendData = await trendRes.json()
      const riskFactorsData = await riskFactorsRes.json()

      setPredictiveModels(predictiveData)
      setStressTests(stressTestData)
      setTrendAnalysis(trendData)
      setRiskFactors(riskFactorsData)
    } catch (error) {
      console.error('Error fetching analytics data:', error)
    } finally {
      setAnalyticsLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
    const interval = setInterval(fetchData, 30000) // Refresh every 30 seconds
    return () => clearInterval(interval)
  }, [])

  useEffect(() => {
    fetchAnalyticsData()
  }, [])

  const triggerAgentCollection = async (agentId: string) => {
    try {
      await fetch(`${API_BASE_URL}/api/agents/${agentId}/trigger-collection`, {
        method: 'POST'
      })
      setTimeout(fetchData, 2000)
    } catch (error) {
      console.error('Error triggering agent collection:', error)
    }
  }

  const getRiskLevelColor = (riskLevel: string) => {
    switch (riskLevel) {
      case 'low': return 'bg-green-500'
      case 'medium': return 'bg-yellow-500'
      case 'high': return 'bg-orange-500'
      case 'critical': return 'bg-red-500'
      default: return 'bg-gray-500'
    }
  }

  const getRiskLevelBadgeVariant = (riskLevel: string) => {
    switch (riskLevel) {
      case 'low': return 'default'
      case 'medium': return 'secondary'
      case 'high': return 'destructive'
      case 'critical': return 'destructive'
      default: return 'outline'
    }
  }

  const getAgentStatusColor = (status: string) => {
    switch (status) {
      case 'idle': return 'text-gray-500'
      case 'collecting': return 'text-blue-500'
      case 'analyzing': return 'text-purple-500'
      case 'active': return 'text-green-500'
      default: return 'text-gray-500'
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <RefreshCw className="h-8 w-8 animate-spin mx-auto mb-4" />
          <p className="text-lg">Loading Investment Banking Risk Portfolio...</p>
        </div>
      </div>
    )
  }

  const riskDistributionData = dashboardData ? Object.entries(dashboardData.risk_distribution).map(([key, value]) => ({
    name: key.charAt(0).toUpperCase() + key.slice(1),
    value,
    color: getRiskLevelColor(key)
  })) : []

  const COLORS = ['#10b981', '#f59e0b', '#f97316', '#ef4444']

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto p-6">
        <div className="mb-8">
          <h1 className="text-4xl font-bold text-gray-900 mb-2">
            Investment Banking Risk Portfolio
          </h1>
          <p className="text-gray-600">
            AI-Powered Risk Assessment and Portfolio Management System
          </p>
        </div>

        <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
          <TabsList className="grid w-full grid-cols-5">
            <TabsTrigger value="dashboard">Dashboard</TabsTrigger>
            <TabsTrigger value="institutions">Institutions</TabsTrigger>
            <TabsTrigger value="borrowers">Borrowers</TabsTrigger>
            <TabsTrigger value="agents">AI Agents</TabsTrigger>
            <TabsTrigger value="analytics">Analytics</TabsTrigger>
          </TabsList>

          <TabsContent value="dashboard" className="space-y-6">
            {/* Key Metrics */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              <Card>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">Total Borrowers</CardTitle>
                  <Users className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">{dashboardData?.total_borrowers || 0}</div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">Institutions</CardTitle>
                  <Building2 className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">{dashboardData?.total_institutions || 0}</div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">Total Exposure</CardTitle>
                  <TrendingUp className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">
                    ${((dashboardData?.total_exposure || 0) / 1000000).toFixed(1)}M
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">Avg Risk Score</CardTitle>
                  <AlertTriangle className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">
                    {((dashboardData?.average_risk_score || 0) * 100).toFixed(1)}%
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* Risk Distribution and Recent Activities */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <Card>
                <CardHeader>
                  <CardTitle>Risk Distribution</CardTitle>
                  <CardDescription>Portfolio risk level breakdown</CardDescription>
                </CardHeader>
                <CardContent>
                  <ResponsiveContainer width="100%" height={300}>
                    <PieChart>
                      <Pie
                        data={riskDistributionData}
                        cx="50%"
                        cy="50%"
                        labelLine={false}
                        label={({ name, value }) => `${name}: ${value}`}
                        outerRadius={80}
                        fill="#8884d8"
                        dataKey="value"
                      >
                        {riskDistributionData.map((_, index) => (
                          <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip />
                    </PieChart>
                  </ResponsiveContainer>
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle>Recent AI Agent Activities</CardTitle>
                  <CardDescription>Latest automated risk assessments</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3 max-h-80 overflow-y-auto">
                    {dashboardData?.recent_activities.slice(0, 8).map((activity, index) => (
                      <div key={index} className="flex items-center space-x-3 p-2 bg-gray-50 rounded">
                        <Activity className="h-4 w-4 text-blue-500" />
                        <div className="flex-1">
                          <p className="text-sm font-medium">{activity.description}</p>
                          <p className="text-xs text-gray-500">
                            Agent {activity.agent_id} • {new Date(activity.timestamp).toLocaleTimeString()}
                          </p>
                        </div>
                        <Badge variant={activity.status === 'completed' ? 'default' : 'secondary'}>
                          {activity.status}
                        </Badge>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            </div>
          </TabsContent>

          <TabsContent value="institutions" className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Financial Institutions</CardTitle>
                <CardDescription>Connected institutions and their status</CardDescription>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Institution</TableHead>
                      <TableHead>Type</TableHead>
                      <TableHead>Borrowers</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Last Sync</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {institutions.map((institution) => (
                      <TableRow key={institution.institution_id}>
                        <TableCell className="font-medium">{institution.name}</TableCell>
                        <TableCell>{institution.type}</TableCell>
                        <TableCell>{institution.borrower_count}</TableCell>
                        <TableCell>
                          <Badge variant={institution.status === 'active' ? 'default' : 'secondary'}>
                            {institution.status}
                          </Badge>
                        </TableCell>
                        <TableCell>{new Date(institution.last_sync).toLocaleString()}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="borrowers" className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Borrower Portfolio</CardTitle>
                <CardDescription>Risk assessment for all borrowers</CardDescription>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Borrower</TableHead>
                      <TableHead>Credit Score</TableHead>
                      <TableHead>Loan Amount</TableHead>
                      <TableHead>Risk Level</TableHead>
                      <TableHead>Risk Score</TableHead>
                      <TableHead>Trend</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {borrowers.slice(0, 20).map((borrower) => (
                      <TableRow key={borrower.borrower_id}>
                        <TableCell className="font-medium">{borrower.name}</TableCell>
                        <TableCell>{borrower.credit_score}</TableCell>
                        <TableCell>${(borrower.loan_amount / 1000).toFixed(0)}K</TableCell>
                        <TableCell>
                          <Badge variant={getRiskLevelBadgeVariant(borrower.risk_score?.risk_level || 'unknown')}>
                            {borrower.risk_score?.risk_level || 'Unknown'}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          {borrower.risk_score ? (borrower.risk_score.overall_score * 100).toFixed(1) + '%' : 'N/A'}
                        </TableCell>
                        <TableCell>
                          {borrower.risk_score?.trend === 'improving' && <TrendingUp className="h-4 w-4 text-green-500" />}
                          {borrower.risk_score?.trend === 'declining' && <TrendingDown className="h-4 w-4 text-red-500" />}
                          {borrower.risk_score?.trend === 'stable' && <div className="h-4 w-4 bg-gray-400 rounded-full" />}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="agents" className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>AI Risk Assessment Agents</CardTitle>
                <CardDescription>Autonomous agents monitoring and assessing risk</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {Object.values(agents).map((agent) => (
                    <Card key={agent.agent_id}>
                      <CardHeader className="pb-3">
                        <div className="flex items-center justify-between">
                          <CardTitle className="text-lg">{agent.agent_id}</CardTitle>
                          <Bot className={`h-5 w-5 ${getAgentStatusColor(agent.status)}`} />
                        </div>
                        <CardDescription>
                          Status: <span className={`font-medium ${getAgentStatusColor(agent.status)}`}>
                            {agent.status}
                          </span>
                        </CardDescription>
                      </CardHeader>
                      <CardContent>
                        <div className="space-y-2">
                          <div>
                            <p className="text-sm font-medium">Assigned Institutions:</p>
                            <div className="flex flex-wrap gap-1 mt-1">
                              {agent.institutions.map((instId) => (
                                <Badge key={instId} variant="outline" className="text-xs">
                                  {instId}
                                </Badge>
                              ))}
                            </div>
                          </div>
                          <div>
                            <p className="text-sm text-gray-500">
                              Last Activity: {new Date(agent.last_activity).toLocaleString()}
                            </p>
                          </div>
                          <Button 
                            size="sm" 
                            onClick={() => triggerAgentCollection(agent.agent_id)}
                            disabled={agent.status !== 'idle'}
                            className="w-full mt-3"
                          >
                            {agent.status === 'idle' ? 'Trigger Collection' : 'Agent Busy'}
                          </Button>
                        </div>
                      </CardContent>
                    </Card>
                  ))}
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="analytics" className="space-y-6">
            {analyticsLoading ? (
              <Card>
                <CardContent className="flex items-center justify-center py-12">
                  <RefreshCw className="h-8 w-8 animate-spin text-blue-500 mr-3" />
                  <span className="text-lg">Loading advanced analytics...</span>
                </CardContent>
              </Card>
            ) : (
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <Card>
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <TrendingUp className="h-5 w-5" />
                      Predictive Risk Models
                    </CardTitle>
                    <CardDescription>ML-based risk forecasting and predictions</CardDescription>
                  </CardHeader>
                  <CardContent>
                    {predictiveModels ? (
                      <div className="space-y-4">
                        <div className="grid grid-cols-3 gap-4 text-sm">
                          <div>
                            <p className="text-gray-500">Model Type</p>
                            <p className="font-semibold">{predictiveModels.model_type}</p>
                          </div>
                          <div>
                            <p className="text-gray-500">R² Score</p>
                            <p className="font-semibold">{(predictiveModels.r2_score * 100).toFixed(1)}%</p>
                          </div>
                          <div>
                            <p className="text-gray-500">RMSE</p>
                            <p className="font-semibold">{predictiveModels.rmse.toFixed(3)}</p>
                          </div>
                        </div>
                        
                        <div className="space-y-2">
                          <div className="flex items-center gap-2">
                            <span className="text-sm font-medium">Forecast Period:</span>
                            <select 
                              value={forecastPeriod} 
                              onChange={(e) => setForecastPeriod(e.target.value as '30' | '60' | '90')}
                              className="text-sm border rounded px-2 py-1"
                            >
                              <option value="30">30 Days</option>
                              <option value="60">60 Days</option>
                              <option value="90">90 Days</option>
                            </select>
                          </div>
                          
                          <ResponsiveContainer width="100%" height={200}>
                            <AreaChart data={
                              forecastPeriod === '30' ? predictiveModels.predictions :
                              forecastPeriod === '60' ? predictiveModels.confidence_intervals.find(ci => ci.period === '60_days')?.predictions || [] :
                              predictiveModels.confidence_intervals.find(ci => ci.period === '90_days')?.predictions || []
                            }>
                              <CartesianGrid strokeDasharray="3 3" />
                              <XAxis 
                                dataKey="date" 
                                tickFormatter={(value) => new Date(value).toLocaleDateString()}
                                fontSize={10}
                              />
                              <YAxis domain={[0, 1]} tickFormatter={(value) => `${(value * 100).toFixed(0)}%`} fontSize={10} />
                              <Tooltip 
                                labelFormatter={(value) => new Date(value).toLocaleDateString()}
                                formatter={(value: number) => [`${(value * 100).toFixed(1)}%`, 'Risk Score']}
                              />
                              <Area 
                                type="monotone" 
                                dataKey="confidence_upper" 
                                stackId="1" 
                                stroke="none" 
                                fill="#e3f2fd" 
                              />
                              <Area 
                                type="monotone" 
                                dataKey="confidence_lower" 
                                stackId="1" 
                                stroke="none" 
                                fill="#ffffff" 
                              />
                              <Line 
                                type="monotone" 
                                dataKey="predicted_risk_score" 
                                stroke="#2196f3" 
                                strokeWidth={2}
                                dot={false}
                              />
                            </AreaChart>
                          </ResponsiveContainer>
                        </div>
                      </div>
                    ) : (
                      <p className="text-gray-500">Loading predictive models...</p>
                    )}
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <AlertTriangle className="h-5 w-5" />
                      Portfolio Stress Testing
                    </CardTitle>
                    <CardDescription>Risk assessment under adverse scenarios</CardDescription>
                  </CardHeader>
                  <CardContent>
                    {stressTests ? (
                      <div className="space-y-4">
                        <div className="grid grid-cols-2 gap-4 text-sm">
                          <div>
                            <p className="text-gray-500">Total Exposure</p>
                            <p className="font-semibold">${(stressTests.portfolio_metrics.total_exposure / 1000000).toFixed(1)}M</p>
                          </div>
                          <div>
                            <p className="text-gray-500">Largest Exposure</p>
                            <p className="font-semibold">{stressTests.portfolio_metrics.largest_exposure_pct.toFixed(1)}%</p>
                          </div>
                        </div>
                        
                        <ResponsiveContainer width="100%" height={200}>
                          <BarChart data={stressTests.stress_test_results.map(test => ({
                            scenario: test.scenario_name.replace('_', ' ').toUpperCase(),
                            baseline: test.baseline_var * 100,
                            stressed: test.stressed_var * 100,
                            impact: test.impact_percentage
                          }))}>
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis dataKey="scenario" fontSize={10} />
                            <YAxis tickFormatter={(value) => `${value}%`} fontSize={10} />
                            <Tooltip formatter={(value: number) => `${value.toFixed(1)}%`} />
                            <Bar dataKey="baseline" fill="#4caf50" name="Baseline VaR" />
                            <Bar dataKey="stressed" fill="#f44336" name="Stressed VaR" />
                          </BarChart>
                        </ResponsiveContainer>
                        
                        <div className="space-y-2">
                          {stressTests.stress_test_results.map((test, index) => (
                            <div key={index} className="text-xs p-2 bg-gray-50 rounded">
                              <p className="font-medium">{test.scenario_name.replace('_', ' ').toUpperCase()}</p>
                              <p className="text-gray-600">{test.scenario_description}</p>
                              <p className="text-red-600">Impact: {test.impact_percentage.toFixed(1)}%</p>
                            </div>
                          ))}
                        </div>
                      </div>
                    ) : (
                      <p className="text-gray-500">Loading stress test results...</p>
                    )}
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <Activity className="h-5 w-5" />
                      Risk Trend Analysis
                    </CardTitle>
                    <CardDescription>Historical patterns and future projections</CardDescription>
                  </CardHeader>
                  <CardContent>
                    {trendAnalysis ? (
                      <div className="space-y-4">
                        <div className="grid grid-cols-3 gap-4 text-sm">
                          <div>
                            <p className="text-gray-500">Trend Direction</p>
                            <div className="flex items-center gap-1">
                              {trendAnalysis.trend_direction === 'improving' && <TrendingUp className="h-4 w-4 text-green-500" />}
                              {trendAnalysis.trend_direction === 'declining' && <TrendingDown className="h-4 w-4 text-red-500" />}
                              {trendAnalysis.trend_direction === 'stable' && <div className="h-4 w-4 bg-gray-400 rounded-full" />}
                              <span className="font-semibold capitalize">{trendAnalysis.trend_direction}</span>
                            </div>
                          </div>
                          <div>
                            <p className="text-gray-500">Volatility Index</p>
                            <p className="font-semibold">{(trendAnalysis.volatility_index * 100).toFixed(1)}%</p>
                          </div>
                          <div>
                            <p className="text-gray-500">Data Points</p>
                            <p className="font-semibold">{trendAnalysis.historical_trends.length}</p>
                          </div>
                        </div>
                        
                        <ResponsiveContainer width="100%" height={200}>
                          <LineChart data={trendAnalysis.historical_trends.slice(-30).map(trend => ({
                            date: trend.date,
                            risk_score: trend.average_risk_score * 100,
                            volatility: trend.volatility * 100
                          }))}>
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis 
                              dataKey="date" 
                              tickFormatter={(value) => new Date(value).toLocaleDateString()}
                              fontSize={10}
                            />
                            <YAxis tickFormatter={(value) => `${value}%`} fontSize={10} />
                            <Tooltip 
                              labelFormatter={(value) => new Date(value).toLocaleDateString()}
                              formatter={(value: number) => `${value.toFixed(1)}%`}
                            />
                            <Line type="monotone" dataKey="risk_score" stroke="#2196f3" strokeWidth={2} dot={false} name="Risk Score" />
                            <Line type="monotone" dataKey="volatility" stroke="#ff9800" strokeWidth={1} dot={false} name="Volatility" />
                          </LineChart>
                        </ResponsiveContainer>
                        
                        <div className="grid grid-cols-4 gap-2 text-xs">
                          {Object.entries(trendAnalysis.seasonal_patterns).map(([quarter, value]) => (
                            <div key={quarter} className="text-center p-2 bg-gray-50 rounded">
                              <p className="font-medium">{quarter}</p>
                              <p className="text-gray-600">{(value * 100).toFixed(1)}%</p>
                            </div>
                          ))}
                        </div>
                      </div>
                    ) : (
                      <p className="text-gray-500">Loading trend analysis...</p>
                    )}
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <Users className="h-5 w-5" />
                      Risk Factor Analysis
                    </CardTitle>
                    <CardDescription>Factor importance and correlations</CardDescription>
                  </CardHeader>
                  <CardContent>
                    {riskFactors ? (
                      <div className="space-y-4">
                        <div>
                          <p className="text-sm font-medium mb-2">Feature Importance</p>
                          <div className="space-y-1">
                            {Object.entries(riskFactors.factor_importance)
                              .sort(([,a], [,b]) => b - a)
                              .slice(0, 5)
                              .map(([factor, importance]) => (
                                <div key={factor} className="flex items-center justify-between text-xs">
                                  <span className="capitalize">{factor.replace('_', ' ')}</span>
                                  <div className="flex items-center gap-2">
                                    <div className="w-16 bg-gray-200 rounded-full h-2">
                                      <div 
                                        className="bg-blue-500 h-2 rounded-full" 
                                        style={{ width: `${importance * 100}%` }}
                                      />
                                    </div>
                                    <span className="w-8 text-right">{(importance * 100).toFixed(0)}%</span>
                                  </div>
                                </div>
                              ))}
                          </div>
                        </div>
                        
                        <div>
                          <p className="text-sm font-medium mb-2">Institution Concentration</p>
                          <div className="space-y-1">
                            {Object.entries(riskFactors.concentration_risks)
                              .sort(([,a], [,b]) => b - a)
                              .map(([institution, percentage]) => (
                                <div key={institution} className="flex items-center justify-between text-xs">
                                  <span>{institution}</span>
                                  <span className="font-medium">{percentage.toFixed(1)}%</span>
                                </div>
                              ))}
                          </div>
                        </div>
                        
                        <div>
                          <p className="text-sm font-medium mb-2">Sensitivity Analysis</p>
                          <div className="grid grid-cols-2 gap-2 text-xs">
                            {Object.entries(riskFactors.sensitivity_analysis).map(([factor, sensitivity]) => (
                              <div key={factor} className="p-2 bg-gray-50 rounded text-center">
                                <p className="font-medium capitalize">{factor.replace('_', ' ')}</p>
                                <p className="text-gray-600">{(sensitivity * 100).toFixed(0)}%</p>
                              </div>
                            ))}
                          </div>
                        </div>
                      </div>
                    ) : (
                      <p className="text-gray-500">Loading risk factors...</p>
                    )}
                  </CardContent>
                </Card>
              </div>
            )}
          </TabsContent>
        </Tabs>
      </div>
    </div>
  )
}

export default App
