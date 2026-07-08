export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  email: string;
  password: string;
  nickname: string;
}

export interface LoginData {
  accessToken: string;
}

export interface EmailSendCodeRequest {
  email: string;
}

export interface EmailVerifyCodeRequest {
  email: string;
  code: string;
}
