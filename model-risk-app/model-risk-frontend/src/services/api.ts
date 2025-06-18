import { Model, ModelCreate } from '@/types/model'

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8000'

export const api = {
  async createModel(model: ModelCreate): Promise<Model> {
    const response = await fetch(`${API_URL}/api/models`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(model),
    })

    if (!response.ok) {
      const error = await response.json()
      throw new Error(error.detail || 'Failed to create model')
    }

    return response.json()
  },

  async getModels(): Promise<Model[]> {
    const response = await fetch(`${API_URL}/api/models`)

    if (!response.ok) {
      throw new Error('Failed to fetch models')
    }

    return response.json()
  },

  async getModelsCount(): Promise<{ count: number }> {
    const response = await fetch(`${API_URL}/api/models/count`)

    if (!response.ok) {
      throw new Error('Failed to fetch models count')
    }

    return response.json()
  }
}
