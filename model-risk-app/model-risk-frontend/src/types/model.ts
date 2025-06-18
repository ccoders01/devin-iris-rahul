export interface Model {
  model_id: string
  model_name: string
  model_version: string
  model_sponsor: string
  business_line: string
  model_type: string
  risk_rating: string
  status: string
  created_at: string
}

export interface ModelCreate {
  model_name: string
  model_version: string
  model_sponsor: string
  business_line: string
  model_type: string
  risk_rating: string
  status: string
}

export const BUSINESS_LINES = [
  'Retail Banking',
  'Wholesale Lending',
  'Investment Banking',
  'Risk Management'
] as const

export const MODEL_TYPES = [
  'Credit Risk',
  'Market Risk',
  'Operational Risk',
  'AML',
  'Capital Calculation',
  'Valuation'
] as const

export const RISK_RATINGS = [
  'High',
  'Medium',
  'Low'
] as const

export const STATUSES = [
  'In Development',
  'Validated',
  'Production',
  'Retired'
] as const
