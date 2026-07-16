// 새로고침 시 복원 흐름이 완료될 때까지 Access Token을 메모리에만 보관한다.
let accessToken: string | null = null

export function getAccessToken() {
  return accessToken
}

export function setAccessToken(token: string) {
  accessToken = token
}

export function clearAccessToken() {
  accessToken = null
}
