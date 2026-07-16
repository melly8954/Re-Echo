// 백엔드 공통 응답 envelope와 화면에서 처리할 오류 정보를 표현한다.
export interface ApiResponse<T> {
  status: number
  errorCode: string | null
  message: string
  result: T
}

export class ApiError extends Error {
  readonly status: number
  readonly errorCode: string | null
  readonly result: unknown

  constructor(
    status: number,
    errorCode: string | null,
    message: string,
    result: unknown = null,
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.errorCode = errorCode
    this.result = result
  }
}
