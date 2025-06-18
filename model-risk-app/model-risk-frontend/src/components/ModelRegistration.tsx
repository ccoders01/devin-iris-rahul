import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Loader2, CheckCircle } from 'lucide-react'
import { api } from '@/services/api'
import { BUSINESS_LINES, MODEL_TYPES, RISK_RATINGS, STATUSES } from '@/types/model'

const formSchema = z.object({
  model_name: z.string().min(1, 'Model name is required'),
  model_version: z.string().min(1, 'Model version is required'),
  model_sponsor: z.string().min(1, 'Model sponsor is required'),
  business_line: z.string().min(1, 'Business line is required'),
  model_type: z.string().min(1, 'Model type is required'),
  risk_rating: z.string().min(1, 'Risk rating is required'),
  status: z.string().min(1, 'Status is required'),
})

type FormData = z.infer<typeof formSchema>

export default function ModelRegistration() {
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [lastRegisteredModel, setLastRegisteredModel] = useState<string | null>(null)

  const form = useForm<FormData>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      model_name: '',
      model_version: '',
      model_sponsor: '',
      business_line: '',
      model_type: '',
      risk_rating: '',
      status: '',
    },
  })

  const onSubmit = async (data: FormData) => {
    setIsSubmitting(true)
    try {
      const result = await api.createModel(data)
      setLastRegisteredModel(result.model_name)
      toast.success(`Model '${result.model_name} ${result.model_version}' successfully registered with ID: ${result.model_id}`)
      form.reset()
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to register model')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto">
      <Card>
        <CardHeader>
          <CardTitle className="text-2xl font-bold">Register New Model</CardTitle>
          <CardDescription>
            Enter the details for a new model to register it in the Model Risk Management system.
          </CardDescription>
        </CardHeader>
        <CardContent>
          {lastRegisteredModel && (
            <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg flex items-center space-x-2">
              <CheckCircle className="h-5 w-5 text-green-600" />
              <span className="text-green-800">
                Model '{lastRegisteredModel}' was successfully registered!
              </span>
            </div>
          )}

          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <FormField
                  control={form.control}
                  name="model_name"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Model Name *</FormLabel>
                      <FormControl>
                        <Input placeholder="Enter model name" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="model_version"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Model Version *</FormLabel>
                      <FormControl>
                        <Input placeholder="e.g., v1.0" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="model_sponsor"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Model Sponsor *</FormLabel>
                      <FormControl>
                        <Input placeholder="Enter sponsor name" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="business_line"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Business Line *</FormLabel>
                      <Select onValueChange={field.onChange} defaultValue={field.value}>
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="Select business line" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {BUSINESS_LINES.map((line) => (
                            <SelectItem key={line} value={line}>
                              {line}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="model_type"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Model Type *</FormLabel>
                      <Select onValueChange={field.onChange} defaultValue={field.value}>
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="Select model type" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {MODEL_TYPES.map((type) => (
                            <SelectItem key={type} value={type}>
                              {type}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="risk_rating"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Risk Rating *</FormLabel>
                      <Select onValueChange={field.onChange} defaultValue={field.value}>
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="Select risk rating" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {RISK_RATINGS.map((rating) => (
                            <SelectItem key={rating} value={rating}>
                              {rating}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="status"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Status *</FormLabel>
                      <Select onValueChange={field.onChange} defaultValue={field.value}>
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="Select status" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {STATUSES.map((status) => (
                            <SelectItem key={status} value={status}>
                              {status}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              <div className="flex justify-end">
                <Button type="submit" disabled={isSubmitting} className="min-w-32">
                  {isSubmitting ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Registering...
                    </>
                  ) : (
                    'Register Model'
                  )}
                </Button>
              </div>
            </form>
          </Form>
        </CardContent>
      </Card>
    </div>
  )
}
