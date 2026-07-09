export interface ApiResponse<T> {
  status: number
  errorCode: string | null
  message: string
  result: T
}

export class ApiError extends Error {
  readonly status: number
  readonly errorCode: string | null

  constructor(status: number, errorCode: string | null, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.errorCode = errorCode
  }
}
