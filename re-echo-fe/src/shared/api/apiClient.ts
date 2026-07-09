import { getAccessToken } from '../../stores/authStore'
import { ApiError, type ApiResponse } from './apiTypes'

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

interface ApiRequestOptions extends RequestInit {
  authenticated?: boolean
  retryUnauthorized?: boolean
}

type AuthenticationRefreshHandler = () => Promise<boolean>

let authenticationRefreshHandler: AuthenticationRefreshHandler | null = null
let authenticationRefreshRequest: Promise<boolean> | null = null

function createHeaders(options: RequestInit, authenticated: boolean) {
  const headers = new Headers(options.headers)

  if (options.body && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }

  if (authenticated) {
    const accessToken = getAccessToken()
    if (accessToken) {
      headers.set('Authorization', `Bearer ${accessToken}`)
    }
  }

  return headers
}

async function parseResponse<T>(response: Response): Promise<ApiResponse<T>> {
  const payload = (await response.json().catch(() => null)) as ApiResponse<T> | null

  if (!payload) {
    throw new ApiError(
      response.status,
      null,
      '서버 응답을 처리할 수 없습니다.',
    )
  }

  if (!response.ok) {
    throw new ApiError(response.status, payload.errorCode, payload.message)
  }

  return payload
}

async function refreshAuthentication() {
  if (!authenticationRefreshHandler) {
    return false
  }

  if (!authenticationRefreshRequest) {
    authenticationRefreshRequest = authenticationRefreshHandler().finally(() => {
      authenticationRefreshRequest = null
    })
  }

  return authenticationRefreshRequest
}

// 공통 응답 envelope와 인증 헤더, 쿠키 전달 방식을 일관되게 적용한다.
export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  const {
    authenticated = false,
    retryUnauthorized = true,
    ...requestOptions
  } = options
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...requestOptions,
    credentials: 'include',
    headers: createHeaders(requestOptions, authenticated),
  })

  if (
    response.status === 401 &&
    authenticated &&
    retryUnauthorized &&
    (await refreshAuthentication())
  ) {
    return apiRequest<T>(path, {
      ...requestOptions,
      authenticated: true,
      retryUnauthorized: false,
    })
  }

  const payload = await parseResponse<T>(response)
  return payload.result
}

export function getApiUrl(path: string) {
  return `${apiBaseUrl}${path}`
}

export function setAuthenticationRefreshHandler(
  handler: AuthenticationRefreshHandler | null,
) {
  authenticationRefreshHandler = handler
}
