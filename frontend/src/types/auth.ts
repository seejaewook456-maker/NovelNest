export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  email: string;
  password: string;
  passwordConfirm: string;
  nickname: string;
}

export interface LoginData {
  accessToken: string;
  refreshToken: string;
}

export interface TokenReissueData {
  accessToken: string;
  refreshToken: string;
}

export interface EmailSendCodeRequest {
  email: string;
}

export interface EmailVerifyCodeRequest {
  email: string;
  code: string;
}
